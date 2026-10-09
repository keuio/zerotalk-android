package top.lanxint.zerotalk.data.voice

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import top.lanxint.zerotalk.data.network.VoiceCallInfoDto
import top.lanxint.zerotalk.data.network.VoiceIceConfigDto
import top.lanxint.zerotalk.data.network.WsServerEvent
import top.lanxint.zerotalk.data.network.ZeroTalkApiService
import top.lanxint.zerotalk.data.network.ZeroTalkWebSocketClient
import kotlin.random.Random

/**
 * 语音通话控制器（对齐官网 `useVoiceCall` 的状态机）
 *
 * 流程：
 * - **主叫**：[startCall] → `voice_invite` → 收到 `voice_invite_sent`（带 ICE 配置）→ 建连 + 采集 → `voice_offer`
 *   → 收到 `voice_answer` 设远端 → 媒体连通后 `voice_connected`
 * - **被叫**：收到 `voice_ringing`（带 ICE）→ [accept] 建连 + 采集 + `voice_accept` → 收到 `voice_offer`
 *   → 生成 answer 并回 `voice_answer`
 * - **重连**：媒体 `failed/disconnected` 后按 `reconnect_timeout_seconds` 内做 ICE 重启重协商；
 *   超时则 `voice_failed(reconnect_timeout)`
 * - **恢复**：刷新/重连后 [resumeIfNeeded] 走 `GET /api/voice/call/current` + `voice_resume`
 * - **心跳**：通话中每 45 秒 `voice_heartbeat`
 *
 * ICE 服务器**只来自服务端随语音事件下发的 `ice` 字段**（bootstrap 里没有），为空时退化为 host candidate。
 */
class VoiceCallController(
    private val scope: CoroutineScope,
    private val ws: ZeroTalkWebSocketClient,
    private val api: ZeroTalkApiService,
    private val selfUidProvider: () -> String
) {
    private val engine: VoiceEngine = createVoiceEngine()

    private val _uiState = MutableStateFlow(VoiceCallUiState.IDLE)
    val uiState: StateFlow<VoiceCallUiState> = _uiState.asStateFlow()

    private val _session = MutableStateFlow<VoiceCallSession?>(null)
    val session: StateFlow<VoiceCallSession?> = _session.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _speakerOn = MutableStateFlow(false)
    val speakerOn: StateFlow<Boolean> = _speakerOn.asStateFlow()

    private val _events = MutableSharedFlow<VoiceCallEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<VoiceCallEvent> = _events.asSharedFlow()

    private var config = VoiceCallConfig()
    private var ice: VoiceIceConfigDto? = null
    private var heartbeatJob: Job? = null
    private var timeoutJob: Job? = null
    private var reconnectJob: Job? = null
    private var engineStarted = false
    private var selfHungUp = false

    /** 设备是否支持语音通话（WebRTC 可用） */
    val isSupported: Boolean get() = engine.isSupported

    /** 是否有进行中的通话（idle/ended 之外） */
    val isCallActive: Boolean
        get() = _session.value != null &&
            _uiState.value != VoiceCallUiState.IDLE &&
            _uiState.value != VoiceCallUiState.ENDED

    /** 服务端配置回填（bootstrap 的 `voice_call` 块） */
    fun updateConfig(newConfig: VoiceCallConfig) {
        config = newConfig
    }

    // ============================================================
    // 用户动作
    // ============================================================

    /**
     * 发起语音通话（主叫）
     *
     * @param roomId 当前私聊/群聊房间
     * @param toUserId 对方数字 user_id（群聊呼叫指定成员时必填其一）
     * @param toUid 对方 uid（官网 `to_user_id` 也接受 uid 字符串；数字 id 缺失时用这个）
     */
    fun startCall(
        roomId: String,
        toUserId: Long? = null,
        toUid: String? = null,
        peerLabel: String = "",
        peerAvatarUrl: String? = null
    ) {
        if (!config.enabled) {
            toast("语音通话功能暂未开放")
            return
        }
        if (!engine.isSupported) {
            toast("当前设备不支持语音通话")
            return
        }
        if (isCallActive) {
            toast("当前已有进行中的通话")
            return
        }
        if (roomId.isBlank()) {
            toast("房间无效，无法发起通话")
            return
        }

        val peerId = toUserId?.takeIf { it > 0 } ?: 0L
        val uidText = toUid?.trim().orEmpty()
        val callId = clientRequestId()
        selfHungUp = false
        ice = null
        _session.value = VoiceCallSession(
            callId = callId,
            roomId = roomId,
            peerUserId = peerId,
            peerUid = uidText,
            peerLabel = peerLabel,
            peerAvatarUrl = peerAvatarUrl,
            isCaller = true
        )
        _uiState.value = VoiceCallUiState.OUTGOING
        _muted.value = false
        _speakerOn.value = false

        if (!ws.voiceInvite(roomId, callId, peerId.takeIf { it > 0 }, uidText, source = "room")) {
            toast("连接未就绪，请稍后重试")
            resetLocally()
            return
        }
        // 响铃超时（官网 ring_timeout_seconds）
        startTimeout(config.ringTimeoutMs) { onRingTimeout() }
    }

    /** 接听（被叫）；[auto] 为语音匹配自动接通 */
    fun accept() {
        val active = _session.value ?: return
        if (_uiState.value != VoiceCallUiState.INCOMING) return
        cancelTimeout()
        // 本端已接听：此后挂断走 voice_hangup（ringing 阶段才用 voice_cancel）
        _session.value = active.copy(peerAccepted = true)
        _uiState.value = VoiceCallUiState.CONNECTING
        scope.launch {
            if (!startEngine(active.callId)) {
                toast("无法开启麦克风")
                ws.voiceReject(active.callId)
                resetLocally()
                return@launch
            }
            ws.voiceAccept(active.callId)
            startTimeout(config.connectTimeoutMs) { onConnectTimeout() }
        }
    }

    /** 拒接（被叫） */
    fun reject() {
        val active = _session.value ?: return
        if (_uiState.value != VoiceCallUiState.INCOMING) return
        ws.voiceReject(active.callId)
        toast("已拒绝", isError = false)
        resetLocally()
    }

    /** 主叫在对方接听前取消呼叫 */
    fun cancel() {
        val active = _session.value ?: return
        // 对方已接听就不再是「取消」，交由 hangup() 处理
        if (active.peerAccepted) return
        ws.voiceCancel(active.callId)
        toast("已取消呼叫", isError = false)
        resetLocally()
    }

    /** 挂断（通话中 / 建连中）；对方尚未接听时按「取消呼叫」处理 */
    fun hangup() {
        val active = _session.value ?: return
        if (!active.peerAccepted) {
            // ringing 阶段服务端只认 voice_cancel（实测发 voice_hangup 不会结束通话）
            ws.voiceCancel(active.callId)
            toast("已取消呼叫", isError = false)
            resetLocally()
            return
        }
        val usedRelay = runCatching { engine.isRelayUsed() }.getOrDefault(false)
        selfHungUp = true
        ws.voiceHangup(active.callId, usedRelay)
        toast("你已挂断", isError = false)
        resetLocally()
    }

    /** 静音切换（返回切换后的状态） */
    fun toggleMute(): Boolean {
        val next = !_muted.value
        _muted.value = next
        runCatching { engine.setMuted(next) }
        return next
    }

    /** 扬声器切换；返回 false 表示当前设备不支持（官网「当前设备不支持切换扬声器」） */
    fun toggleSpeaker(): Boolean {
        val next = !_speakerOn.value
        val ok = runCatching { engine.setSpeakerOn(next) }.getOrDefault(false)
        if (!ok) {
            toast("当前设备不支持切换扬声器")
            return false
        }
        _speakerOn.value = next
        return true
    }

    // ============================================================
    // 服务端语音事件
    // ============================================================

    fun onWsEvent(event: WsServerEvent.VoiceCall) {
        event.ice?.let { ice = it }
        when (event.kind) {
            // 主叫：服务端已受理邀请，拿到真实 call_id 与 ICE 配置后立刻建连并发 offer
            "invite_sent" -> {
                val active = _session.value ?: return
                if (!active.isCaller) return
                // 关键：本地生成的是 client_request_id，服务端会分配真正的 call_id，
                // 之后所有信令（offer/ice/connected/hangup…）都必须用服务端的 call_id
                val serverCallId = event.callId.takeIf { it.isNotBlank() } ?: active.callId
                if (serverCallId != active.callId) {
                    _session.value = active.copy(callId = serverCallId)
                }
                scope.launch {
                    if (!startEngine(serverCallId)) {
                        toast("无法开启麦克风")
                        ws.voiceFailed(serverCallId, "media_failed")
                        resetLocally()
                        return@launch
                    }
                    val offer = engine.createOffer(iceRestart = false)
                    if (offer.isNullOrBlank()) {
                        toast("发起通话失败，请稍后重试")
                        ws.voiceFailed(serverCallId, "offer_failed")
                        resetLocally()
                        return@launch
                    }
                    ws.voiceOffer(serverCallId, offer)
                    _uiState.value = VoiceCallUiState.CONNECTING
                    startTimeout(config.connectTimeoutMs) { onConnectTimeout() }
                }
            }

            // 被叫：响铃（auto=true 为语音匹配自动接通，直接进入接听流程）
            "ringing" -> {
                if (_session.value != null && _uiState.value != VoiceCallUiState.IDLE) return
                selfHungUp = false
                val incoming = VoiceCallSession(
                    callId = event.callId,
                    roomId = event.roomId.orEmpty(),
                    peerUserId = event.peerUserId ?: 0L,
                    peerUid = event.peerUid.orEmpty(),
                    peerLabel = event.peerUsername?.takeIf { it.isNotBlank() } ?: "对方",
                    peerAvatarUrl = event.peerAvatarUrl,
                    isCaller = false,
                    auto = event.auto,
                    hintMs = event.hintMs ?: 3_000L
                )
                _session.value = incoming
                if (event.auto) {
                    _uiState.value = VoiceCallUiState.INCOMING
                    accept()
                } else {
                    _uiState.value = VoiceCallUiState.INCOMING
                    startTimeout(config.ringTimeoutMs) { onRingTimeout() }
                }
            }

            // 主叫：对方已接听
            "accepted" -> {
                val active = _session.value
                if (active?.isCaller == true) {
                    _session.value = active.copy(peerAccepted = true)
                    _uiState.value = VoiceCallUiState.CONNECTING
                }
            }

            // 被叫：收到主叫 offer → 生成 answer
            "offer" -> {
                val sdp = event.sdp?.takeIf { it.isNotBlank() } ?: return
                val active = _session.value ?: return
                scope.launch {
                    if (!engineStarted && !startEngine(active.callId)) {
                        toast("接听信令失败")
                        ws.voiceFailed(active.callId, "media_failed")
                        resetLocally()
                        return@launch
                    }
                    val answer = engine.setRemoteOfferAndCreateAnswer(sdp)
                    if (answer.isNullOrBlank()) {
                        toast("接听信令失败")
                        ws.voiceFailed(active.callId, "answer_failed")
                        resetLocally()
                        return@launch
                    }
                    ws.voiceAnswer(active.callId, answer)
                    _uiState.value = VoiceCallUiState.CONNECTING
                    startTimeout(config.connectTimeoutMs) { onConnectTimeout() }
                }
            }

            // 主叫：收到 answer
            "answer" -> {
                val sdp = event.sdp?.takeIf { it.isNotBlank() } ?: return
                scope.launch { engine.setRemoteAnswer(sdp) }
            }

            "ice" -> {
                val candidate = event.candidate?.takeIf { it.isNotBlank() } ?: return
                scope.launch { engine.addIceCandidate(candidate) }
            }

            // 服务端确认媒体连通（双通道：本端引擎也会上报一次）
            "connected" -> markConnected(event.usedRelay == true)

            "ended" -> {
                val message = event.message?.takeIf { it.isNotBlank() } ?: reasonText(event.reason)
                if (!selfHungUp) toast(message, isError = false)
                resetLocally()
            }

            "timeout" -> {
                val message = event.message?.takeIf { it.isNotBlank() } ?: "对方未接听"
                toast(message)
                resetLocally()
            }

            "error" -> {
                val message = event.message?.takeIf { it.isNotBlank() } ?: "语音通话失败"
                toast(message)
                resetLocally()
            }

            "peer_reconnecting" -> {
                if (isCallActive) _uiState.value = VoiceCallUiState.RECONNECTING
            }

            "restored" -> {
                if (isCallActive) {
                    _uiState.value = VoiceCallUiState.CONNECTED
                    _session.value?.let { if (it.connectedAtMs == 0L) markConnected(false) }
                }
            }

            "call_banned" -> {
                _events.tryEmit(VoiceCallEvent.Banned)
                toast(event.message?.takeIf { it.isNotBlank() } ?: "你的语音通话功能已被限制")
                resetLocally()
            }
        }
    }

    /**
     * 语音匹配自动接通（官网 `applyMatchVoiceAuto`）
     *
     * 匹配服务端在 `match_success.voice` 里已经把通话建好了，客户端只需按 `voice_resume` 续上：
     * 建立本地会话 → 发 `voice_resume` → 等服务端回 `voice_restored` / 走 offer-answer 建连。
     */
    fun startAutoFromMatch(info: VoiceCallInfoDto, fallbackRoomId: String = "") {
        if (!config.enabled) return
        if (!engine.isSupported) {
            toast("当前设备不支持语音通话")
            return
        }
        if (isCallActive) return
        val callId = info.callId.trim().lowercase()
        if (callId.isBlank()) return

        selfHungUp = false
        ice = info.ice
        _muted.value = false
        _speakerOn.value = false
        _session.value = VoiceCallSession(
            callId = callId,
            roomId = info.roomId.ifBlank { fallbackRoomId },
            peerUserId = info.peerUserId,
            peerUid = info.peerUid.orEmpty(),
            peerLabel = info.peerUsername?.takeIf { it.isNotBlank() } ?: "对方",
            peerAvatarUrl = info.peerAvatarUrl,
            isCaller = info.isCaller,
            auto = true
        )
        _uiState.value = VoiceCallUiState.CONNECTING
        if (!ws.voiceResume(callId)) {
            toast("无法自动接通语音，请手动重试")
            resetLocally()
            return
        }
        startTimeout(config.connectTimeoutMs) { onConnectTimeout() }
    }

    /**
     * WS 重连后为**进行中**的通话续期（官网在 `authenticated` 之后重发 `voice_resume`）
     *
     * 与 [resumeIfNeeded] 的区别：本方法只处理本端已有会话的情况，不查询服务端。
     */
    fun resumeActiveOnReconnect() {
        val active = _session.value ?: return
        if (!config.enabled) return
        ws.voiceResume(active.callId)
    }

    /**
     * 刷新 / 重连后恢复通话（官网 `resume_start source=server`）
     *
     * @param roomIdHint 指定房间时仅在通话属于该房间时恢复
     */
    suspend fun resumeIfNeeded(roomIdHint: String? = null) {
        if (!config.enabled || isCallActive) return
        val data = api.getCurrentVoiceCall().getOrNull() ?: return
        if (!data.hasCall) return
        val info = data.call ?: return
        if (info.callId.isBlank() || info.state == "ended") return
        if (roomIdHint != null && info.roomId.isNotBlank() && info.roomId != roomIdHint) return

        ice = info.ice
        _session.value = VoiceCallSession(
            callId = info.callId.lowercase(),
            roomId = info.roomId,
            peerUserId = info.peerUserId,
            peerUid = info.peerUid.orEmpty(),
            peerLabel = info.peerUsername?.takeIf { it.isNotBlank() } ?: "对方",
            peerAvatarUrl = info.peerAvatarUrl,
            isCaller = info.isCaller
        )
        _uiState.value = VoiceCallUiState.CONNECTING
        _muted.value = false
        _speakerOn.value = false
        if (!ws.voiceResume(info.callId.lowercase())) {
            toast("无法恢复通话")
            resetLocally()
            return
        }
        startTimeout(config.connectTimeoutMs) { onConnectTimeout() }
    }

    /** 主动释放（退出登录 / 进程结束） */
    fun release() {
        cancelTimeout()
        stopHeartbeat()
        reconnectJob?.cancel()
        reconnectJob = null
        runCatching { engine.close() }
        engineStarted = false
        _session.value = null
        _uiState.value = VoiceCallUiState.IDLE
    }

    // ============================================================
    // 内部
    // ============================================================

    /** 建连 + 采集（含录音权限）；ICE 回调直连信令 */
    private suspend fun startEngine(callId: String): Boolean {
        if (engineStarted) return true
        val ok = engine.start(
            ice = ice,
            onIceCandidate = { candidate -> ws.voiceIce(callId, candidate) },
            onConnectionState = { state -> onEngineConnectionState(callId, state) }
        )
        engineStarted = ok
        return ok
    }

    /** 引擎连接状态：连通上报 + 断开自动 ICE 重启 */
    private fun onEngineConnectionState(callId: String, state: String) {
        if (_session.value?.callId != callId) return
        when (state) {
            "connected" -> markConnected(runCatching { engine.isRelayUsed() }.getOrDefault(false))

            "disconnected", "failed" -> {
                if (_uiState.value == VoiceCallUiState.CONNECTED ||
                    _uiState.value == VoiceCallUiState.CONNECTING
                ) {
                    _uiState.value = VoiceCallUiState.RECONNECTING
                    // 官网：重连窗口内做 ICE 重启重新协商，超时则 voice_failed(reconnect_timeout)
                    // 关键：重建任务必须可取消——挂断时若原生 createOffer 仍在飞行中，
                    // 与 close()/dispose() 并发会触发 WebRTC 原生 SIGSEGV
                    reconnectJob?.cancel()
                    reconnectJob = scope.launch {
                        val offer = engine.createOffer(iceRestart = true)
                        // 期间可能已挂断/切会话，必须再确认一次再发信令
                        if (_session.value?.callId != callId) return@launch
                        if (!offer.isNullOrBlank()) ws.voiceOffer(callId, offer)
                        startTimeout(config.reconnectTimeoutMs) { onReconnectTimeout() }
                    }
                }
            }
        }
    }

    private fun markConnected(usedRelay: Boolean) {
        val active = _session.value ?: return
        if (_uiState.value == VoiceCallUiState.CONNECTED) return
        cancelTimeout()
        _uiState.value = VoiceCallUiState.CONNECTED
        _session.value = active.copy(connectedAtMs = System.currentTimeMillis())
        ws.voiceConnected(active.callId, usedRelay)
        startHeartbeat(active.callId)
    }

    private fun startHeartbeat(callId: String) {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                ws.voiceHeartbeat(callId)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun startTimeout(ms: Long, onExpire: () -> Unit) {
        cancelTimeout()
        timeoutJob = scope.launch {
            delay(ms)
            onExpire()
        }
    }

    private fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    private fun onRingTimeout() {
        val active = _session.value ?: return
        // 官网：响铃超时提示「对方未接听」；主叫侧同时取消呼叫
        if (active.isCaller) ws.voiceCancel(active.callId)
        toast("对方未接听")
        resetLocally()
    }

    private fun onConnectTimeout() {
        val active = _session.value ?: return
        ws.voiceFailed(active.callId, "webrtc_failed")
        toast("语音通话失败")
        resetLocally()
    }

    private fun onReconnectTimeout() {
        val active = _session.value ?: return
        ws.voiceFailed(active.callId, "reconnect_timeout")
        toast(reasonText("reconnect_timeout"))
        resetLocally()
    }

    private fun resetLocally() {
        cancelTimeout()
        stopHeartbeat()
        // 先取消可能仍在调用原生引擎的重连任务，再关闭引擎，避免原生层并发释放导致段错误
        reconnectJob?.cancel()
        reconnectJob = null
        runCatching { engine.close() }
        engineStarted = false
        selfHungUp = false
        ice = null
        _session.value = null
        _muted.value = false
        _speakerOn.value = false
        _uiState.value = VoiceCallUiState.IDLE
    }

    private fun toast(message: String, isError: Boolean = true) {
        _events.tryEmit(VoiceCallEvent.Toast(message, isError))
    }

    private fun clientRequestId(): String {
        val rand = Random.nextInt(0, Int.MAX_VALUE).toString(36).take(7)
        return "vc_${System.currentTimeMillis()}_$rand"
    }

    /** 结束原因 → 文案（对齐官网文案表；服务端给了 `message` 时优先用服务端文案） */
    private fun reasonText(reason: String?): String = when (reason?.trim()?.lowercase()) {
        "reconnect_timeout" -> "无法恢复通话"
        "webrtc_failed", "media_failed", "offer_failed", "answer_failed" -> "语音通话失败"
        "voice_call_banned" -> "语音通话功能受限"
        "rejected" -> "已拒绝"
        "cancelled", "canceled" -> "语音会话已取消"
        "user_hangup", "remote_hangup", "hangup" -> "通话已结束"
        "timeout", "expired", "ring_timeout" -> "对方未接听"
        "resume_failed" -> "无法恢复通话"
        else -> "通话已结束"
    }

    private companion object {
        /** 官网心跳间隔 45 秒 */
        const val HEARTBEAT_INTERVAL_MS = 45_000L
    }
}
