package top.lanxint.zerotalk.data.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import top.lanxint.zerotalk.data.network.VoiceIceConfigDto
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * 语音引擎的上下文与录音权限桥
 *
 * [init] 由 MainActivity 注入 Application Context 与「申请录音权限」的启动器；
 * 权限结果由 MainActivity 的 `registerForActivityResult` 回填到 [onPermissionResult]。
 */
object VoiceEngineHolder {
    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var requestPermission: (() -> Unit)? = null

    @Volatile
    private var pendingPermission: ((Boolean) -> Unit)? = null

    fun init(context: Context, requestRecordAudio: () -> Unit) {
        appContext = context.applicationContext
        requestPermission = requestRecordAudio
    }

    internal fun context(): Context? = appContext

    /** MainActivity 权限回调回填 */
    fun onPermissionResult(granted: Boolean) {
        pendingPermission?.invoke(granted)
        pendingPermission = null
    }

    /** 确保已获得录音权限（未授权则拉起系统弹窗并挂起等待结果） */
    internal suspend fun ensureRecordPermission(): Boolean {
        val ctx = appContext ?: return false
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        val launch = requestPermission ?: return false
        return suspendCancellableCoroutine { cont ->
            pendingPermission = { granted -> if (cont.isActive) cont.resume(granted) }
            launch()
        }
    }
}

/** Android：WebRTC 音频通话引擎 */
actual fun createVoiceEngine(): VoiceEngine = WebRtcVoiceEngine()

/**
 * 基于 Google WebRTC（`io.github.webrtc-sdk:android`）的纯音频通话引擎
 *
 * - 只采集/发送音频（对齐官网 `getUserMedia({audio:{echoCancellation,noiseSuppression,autoGainControl},video:false})`）
 * - ICE 服务器与传输策略来自服务端随语音事件下发的 [VoiceIceConfigDto]
 * - 通过 `onSelectedCandidatePairChanged` 判定是否走中继（`used_relay`）
 */
internal class WebRtcVoiceEngine : VoiceEngine {

    private var factory: PeerConnectionFactory? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null

    private var speakerOn = false
    private var muted = false

    @Volatile
    private var relayUsed = false

    /**
     * 原生引擎生命周期门闸（挂断段错误的根因修复）
     *
     * WebRTC 的 `createOffer` / `restartIce` / `addIceCandidate` 等原生调用一旦与
     * `PeerConnection.dispose()` 并发，就会在原生层直接触发 SIGSEGV
     * （实测挂断瞬间：`Process 31815 exited due to signal 11 (Segmentation fault)`）。
     *
     * 三层防护：
     * 1. [closed] 门闸——挂断后所有原生入口与 Observer 回调立即短路，不再产生新的原生调用；
     * 2. [nativeGate] 串行化——原生调用与资源释放互斥，释放必须等飞行中的调用结束；
     * 3. 延迟释放——真正的 `close()/dispose()` 排到 [disposeDispatcher] 执行，避开原生回调栈。
     *
     * [engineLock] 只保护字段可见性（引用与门闸），临界区很短；跨挂起点的互斥交给 [nativeGate]。
     */
    private val engineLock = Any()
    private val nativeGate = Mutex()
    private val lifecycleScope = CoroutineScope(SupervisorJob() + disposeDispatcher)

    /** true 表示当前没有可用的原生连接，所有原生入口短路 */
    @Volatile
    private var closed = true

    /**
     * 连接代次：每次 start()/close() 自增。
     *
     * Observer 回调捕获自己所属的代次；代次不符说明这是上一通电话的残留回调，直接丢弃，
     * 避免旧 PeerConnection 的候选/状态在挂断后污染新的一通电话。
     */
    @Volatile
    private var generation = 0L

    override val isSupported: Boolean
        get() = VoiceEngineHolder.context() != null

    override suspend fun start(
        ice: VoiceIceConfigDto?,
        onIceCandidate: (String) -> Unit,
        onConnectionState: (String) -> Unit
    ): Boolean {
        val ctx = VoiceEngineHolder.context() ?: return false
        // 权限弹窗会让用户交互挂起，此时不要占用门闸
        if (!VoiceEngineHolder.ensureRecordPermission()) return false

        return nativeGate.withLock {
            // 新的一通电话：重新开门并领取新代次。上一通遗留的释放任务只持有它自己的资源快照，
            // 不会误伤本次连接；旧 Observer 也会因代次不符而静默。
            val myGeneration = synchronized(engineLock) {
                closed = false
                relayUsed = false
                ++generation
            }

            try {
                val factory = ensureFactory(ctx)
                val rtcConfig = buildRtcConfig(ice)
                val observer = object : PeerConnection.Observer {
                    override fun onIceCandidate(candidate: IceCandidate) {
                        if (closed || generation != myGeneration) return
                        // 服务端与网页端都按 {candidate, sdpMid, sdpMLineIndex} 对象形态交换
                        val json = JSONObject().apply {
                            put("candidate", candidate.sdp)
                            put("sdpMid", candidate.sdpMid)
                            put("sdpMLineIndex", candidate.sdpMLineIndex)
                        }.toString()
                        onIceCandidate(json)
                    }

                    override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                        if (closed || generation != myGeneration) return
                        val state = when (newState) {
                            PeerConnection.PeerConnectionState.CONNECTED -> "connected"
                            PeerConnection.PeerConnectionState.DISCONNECTED -> "disconnected"
                            PeerConnection.PeerConnectionState.FAILED -> "failed"
                            PeerConnection.PeerConnectionState.CLOSED -> "closed"
                            else -> "other"
                        }
                        onConnectionState(state)
                    }

                    override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) {
                        if (closed || generation != myGeneration) return
                        relayUsed = isRelayCandidate(event.local) || isRelayCandidate(event.remote)
                    }

                    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                        if (closed || generation != myGeneration) return
                        // 部分设备只回调 ICE 状态，需要一并映射
                        when (newState) {
                            PeerConnection.IceConnectionState.CONNECTED,
                            PeerConnection.IceConnectionState.COMPLETED -> onConnectionState("connected")
                            PeerConnection.IceConnectionState.FAILED -> onConnectionState("failed")
                            PeerConnection.IceConnectionState.DISCONNECTED -> onConnectionState("disconnected")
                            else -> Unit
                        }
                    }

                    override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
                    override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                    override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit
                    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) = Unit
                    override fun onAddStream(stream: MediaStream) = Unit
                    override fun onRemoveStream(stream: MediaStream) = Unit
                    override fun onDataChannel(channel: DataChannel) = Unit
                    override fun onRenegotiationNeeded() = Unit
                    override fun onAddTrack(receiver: RtpReceiver, streams: Array<MediaStream>) = Unit
                    override fun onTrack(transceiver: RtpTransceiver) = Unit
                }

                // 创建 PeerConnection：与 close() 的快照在同一把锁下，二者必有一个先拿到
                val pc = synchronized(engineLock) {
                    if (closed) return@synchronized null
                    factory.createPeerConnection(rtcConfig, observer)?.also { peerConnection = it }
                } ?: return@withLock false

                val prepared = synchronized(engineLock) {
                    if (closed) {
                        false
                    } else {
                        val source = factory.createAudioSource(MediaConstraints())
                        audioSource = source
                        val track = factory.createAudioTrack("zt-voice", source)
                        localTrack = track
                        track.setEnabled(!muted)
                        pc.addTrack(track, listOf("zt-voice-stream"))
                        true
                    }
                }
                if (!prepared) {
                    // close() 可能已抢先收走刚建好的 PC：只回收仍挂在字段上的那一份，避免重复 dispose
                    val orphan = synchronized(engineLock) {
                        if (peerConnection === pc) {
                            val snapshot = NativeResources(peerConnection, audioSource, localTrack)
                            peerConnection = null
                            audioSource = null
                            localTrack = null
                            snapshot
                        } else {
                            NativeResources(null, null, null)
                        }
                    }
                    if (!orphan.isEmpty) enqueueTeardown(orphan)
                    return@withLock false
                }

                audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                applyAudioRoute(speakerOn)
                true
            } catch (t: Throwable) {
                println("[VoiceEngine] start failed: ${t.message}")
                close()
                false
            }
        }
    }

    override suspend fun createOffer(iceRestart: Boolean): String? = nativeGate.withLock {
        val pc = synchronized(engineLock) { if (closed) null else peerConnection } ?: return@withLock null
        if (iceRestart) {
            runCatching { pc.restartIce() }
        }
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        val sdp = runCatching { pc.createOfferSuspend(constraints) }.getOrNull() ?: return@withLock null
        sdp.description
    }

    override suspend fun setRemoteOfferAndCreateAnswer(remoteSdp: String): String? = nativeGate.withLock {
        val pc = synchronized(engineLock) { if (closed) null else peerConnection } ?: return@withLock null
        runCatching { pc.setRemoteDescriptionSuspend(SessionDescription.Type.OFFER, remoteSdp) }
            .getOrNull() ?: return@withLock null
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        val answer = runCatching { pc.createAnswerSuspend(constraints) }.getOrNull() ?: return@withLock null
        answer.description
    }

    override suspend fun setRemoteAnswer(remoteSdp: String) {
        nativeGate.withLock {
            val pc = synchronized(engineLock) { if (closed) null else peerConnection } ?: return@withLock
            runCatching { pc.setRemoteDescriptionSuspend(SessionDescription.Type.ANSWER, remoteSdp) }
            Unit
        }
    }

    override suspend fun addIceCandidate(candidate: String) {
        val parsed = parseIceCandidate(candidate) ?: return
        nativeGate.withLock {
            val pc = synchronized(engineLock) { if (closed) null else peerConnection } ?: return@withLock
            runCatching { pc.addIceCandidate(parsed) }
            Unit
        }
    }

    /** 远端 candidate 既可能是 `{candidate,sdpMid,sdpMLineIndex}` 对象串，也可能是裸 candidate 字符串 */
    private fun parseIceCandidate(raw: String): IceCandidate? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (text.startsWith("{")) {
            return try {
                val obj = JSONObject(text)
                val sdp = obj.optString("candidate").takeIf { it.isNotBlank() } ?: return null
                val mid = obj.optString("sdpMid").takeIf { it.isNotBlank() }
                IceCandidate(mid, obj.optInt("sdpMLineIndex", 0), sdp)
            } catch (_: Exception) {
                null
            }
        }
        return IceCandidate(null, 0, text)
    }

    /** 中继候选在 SDP 里带 `typ relay`（用于 `used_relay` 上报） */
    private fun isRelayCandidate(candidate: IceCandidate?): Boolean =
        candidate?.sdp?.contains("typ relay", ignoreCase = true) == true

    override fun setMuted(muted: Boolean) {
        this.muted = muted
        val track = synchronized(engineLock) { if (closed) null else localTrack }
        runCatching { track?.setEnabled(!muted) }
    }

    override fun setSpeakerOn(on: Boolean): Boolean {
        val manager = audioManager ?: return false
        return try {
            speakerOn = on
            applyAudioRoute(on)
            true
        } catch (t: Throwable) {
            println("[VoiceEngine] setSpeakerOn failed: ${t.message}")
            false
        }
    }

    override fun isRelayUsed(): Boolean = relayUsed

    /**
     * 关闭引擎：立即落下 [closed] 门闸并摘走原生资源，真正的 `dispose()` 交给 [enqueueTeardown]。
     *
     * 必须是非阻塞的——它由主线程上的挂断 / 结束流程调用。
     */
    override fun close() {
        val snapshot = synchronized(engineLock) {
            closed = true
            // 代次自增：让所有在飞的 Observer 回调立即失效
            generation++
            val resources = NativeResources(peerConnection, audioSource, localTrack)
            peerConnection = null
            audioSource = null
            localTrack = null
            resources
        }
        relayUsed = false
        // 音频路由与原生 PC 无关，可以立刻复位
        runCatching {
            audioManager?.let { manager ->
                manager.mode = AudioManager.MODE_NORMAL
                manager.isSpeakerphoneOn = false
            }
        }
        if (!snapshot.isEmpty) enqueueTeardown(snapshot)
    }

    // ---------------- 内部 ----------------

    /**
     * 延迟释放：先等 [nativeGate] 上飞行中的原生调用结束，再调用 `close()/dispose()`。
     *
     * 释放动作跑在 [disposeDispatcher]（单线程）上，既避开原生回调栈，也保证多次释放串行。
     */
    private fun enqueueTeardown(resources: NativeResources) {
        lifecycleScope.launch {
            val drained = withTimeoutOrNull(TEARDOWN_WAIT_MS) {
                nativeGate.withLock { disposeResources(resources) }
                true
            }
            if (drained != true) {
                // 极端情况下原生回调迟迟不返回：先 close() 让底层停止工作，再延迟释放兜底
                runCatching { resources.peerConnection?.close() }
                delay(TEARDOWN_FALLBACK_DELAY_MS)
                disposeResources(resources)
            }
        }
    }

    private fun disposeResources(resources: NativeResources) {
        runCatching { resources.localTrack?.dispose() }
        runCatching { resources.audioSource?.dispose() }
        runCatching { resources.peerConnection?.close() }
        runCatching { resources.peerConnection?.dispose() }
    }

    private fun ensureFactory(ctx: Context): PeerConnectionFactory {
        factory?.let { return it }
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(ctx)
                .createInitializationOptions()
        )
        val adm = JavaAudioDeviceModule.builder(ctx)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .createAudioDeviceModule()
        audioDeviceModule = adm
        val created = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setAudioDeviceModule(adm)
            .createPeerConnectionFactory()
        factory = created
        return created
    }

    private fun buildRtcConfig(ice: VoiceIceConfigDto?): PeerConnection.RTCConfiguration {
        // 一个 iceServer 条目可能带多个地址（数组形态），逐个建 IceServer
        val servers = ice?.iceServers.orEmpty().flatMap { server ->
            server.urls.mapNotNull { url ->
                if (url.isBlank()) return@mapNotNull null
                val builder = PeerConnection.IceServer.builder(url)
                server.username?.takeIf { it.isNotBlank() }?.let { builder.setUsername(it) }
                server.credential?.takeIf { it.isNotBlank() }?.let { builder.setPassword(it) }
                builder.createIceServer()
            }
        }
        return PeerConnection.RTCConfiguration(servers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            iceTransportsType = if (ice?.relayOnly == true) {
                PeerConnection.IceTransportsType.RELAY
            } else {
                PeerConnection.IceTransportsType.ALL
            }
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
    }

    private fun applyAudioRoute(speaker: Boolean) {
        val manager = audioManager ?: return
        @Suppress("DEPRECATION")
        run {
            manager.mode = AudioManager.MODE_IN_COMMUNICATION
            manager.isSpeakerphoneOn = speaker
        }
    }

    private suspend fun PeerConnection.createOfferSuspend(
        constraints: MediaConstraints
    ): SessionDescription? = suspendCancellableCoroutine { cont ->
        createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) {
                    if (cont.isActive) cont.resume(null)
                    return
                }
                setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) = Unit
                    override fun onSetSuccess() {
                        if (cont.isActive) cont.resume(sdp)
                    }

                    override fun onCreateFailure(p0: String?) {
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onSetFailure(p0: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                }, sdp)
            }

            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String?) {
                if (cont.isActive) cont.resume(null)
            }

            override fun onSetFailure(error: String?) {
                if (cont.isActive) cont.resume(null)
            }
        }, constraints)
    }

    private suspend fun PeerConnection.createAnswerSuspend(
        constraints: MediaConstraints
    ): SessionDescription? = suspendCancellableCoroutine { cont ->
        createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) {
                    if (cont.isActive) cont.resume(null)
                    return
                }
                setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) = Unit
                    override fun onSetSuccess() {
                        if (cont.isActive) cont.resume(sdp)
                    }

                    override fun onCreateFailure(p0: String?) {
                        if (cont.isActive) cont.resume(null)
                    }

                    override fun onSetFailure(p0: String?) {
                        if (cont.isActive) cont.resume(null)
                    }
                }, sdp)
            }

            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String?) {
                if (cont.isActive) cont.resume(null)
            }

            override fun onSetFailure(error: String?) {
                if (cont.isActive) cont.resume(null)
            }
        }, constraints)
    }

    private suspend fun PeerConnection.setRemoteDescriptionSuspend(
        type: SessionDescription.Type,
        sdp: String
    ): SessionDescription? = suspendCancellableCoroutine { cont ->
        val description = SessionDescription(type, sdp)
        setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) = Unit
            override fun onSetSuccess() {
                if (cont.isActive) cont.resume(description)
            }

            override fun onCreateFailure(p0: String?) {
                if (cont.isActive) cont.resume(null)
            }

            override fun onSetFailure(error: String?) {
                println("[VoiceEngine] setRemoteDescription failed: $error")
                if (cont.isActive) cont.resume(null)
            }
        }, description)
    }

    /** 一次释放所需的原生句柄快照（同一时刻只属于一个持有者，避免重复 dispose） */
    private class NativeResources(
        val peerConnection: PeerConnection?,
        val audioSource: AudioSource?,
        val localTrack: AudioTrack?
    ) {
        val isEmpty: Boolean
            get() = peerConnection == null && audioSource == null && localTrack == null
    }

    private companion object {
        /**
         * 原生释放专用单线程，全部引擎实例共用：保证 dispose 串行且不占用主线程。
         * 守护线程，进程存活期间常驻。
         */
        val disposeDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "zt-voice-dispose").apply { isDaemon = true }
        }.asCoroutineDispatcher()

        /** 等待飞行中原生调用的上限；超时后退化为 close() + 延迟释放 */
        const val TEARDOWN_WAIT_MS = 2_000L
        const val TEARDOWN_FALLBACK_DELAY_MS = 300L
    }
}
