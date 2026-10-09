package top.lanxint.zerotalk.data.voice

import top.lanxint.zerotalk.data.network.VoiceIceConfigDto

/**
 * 语音通话界面状态（对齐官网 `useVoiceCall` 的 `uiState`）
 */
enum class VoiceCallUiState {
    IDLE,
    /** 主叫已发起、等待对方接听 */
    OUTGOING,
    /** 被叫响铃中 */
    INCOMING,
    /** 信令/媒体建连中 */
    CONNECTING,
    /** 通话中 */
    CONNECTED,
    /** 媒体断开、自动重连中 */
    RECONNECTING,
    ENDED
}

/**
 * 一次通话的会话快照（UI 渲染 + 断线恢复用）
 *
 * @param hintMs 服务端 `connecting_hint_ms`（自动接通场景下展示「正在接通」的提示时长）
 */
data class VoiceCallSession(
    val callId: String,
    val roomId: String,
    val peerUserId: Long = 0L,
    val peerUid: String = "",
    val peerLabel: String = "",
    val peerAvatarUrl: String? = null,
    val isCaller: Boolean = false,
    val auto: Boolean = false,
    val hintMs: Long = 3_000L,
    val connectedAtMs: Long = 0L,
    /**
     * 对方是否已接听（服务端 `voice_accepted`）
     *
     * 决定「取消呼叫」与「挂断」的分流：未接听时服务端只认 `voice_cancel`，
     * 已接听后才用 `voice_hangup`（实测 ringing 阶段发 hangup 服务端不处理，通话不会结束）。
     */
    val peerAccepted: Boolean = false
)

/** 通话期间抛给 UI 的一次性事件 */
sealed interface VoiceCallEvent {
    /** 通用提示（失败/结束/受限等） */
    data class Toast(val message: String, val isError: Boolean = true) : VoiceCallEvent

    /** 账号被限制语音通话（官网 `voice_call_banned`） */
    data object Banned : VoiceCallEvent
}

/**
 * 服务端下发的通话配置（bootstrap 的 `voice_call` 块）
 *
 * 实测：`{"enabled":true,"force_turn":false,"ice_transport_policy":"all",
 * "ring_timeout_seconds":45,"connect_timeout_seconds":30,"reconnect_timeout_seconds":40}`
 */
data class VoiceCallConfig(
    val enabled: Boolean = true,
    val forceTurn: Boolean = false,
    val iceTransportPolicy: String? = null,
    val ringTimeoutMs: Long = 45_000L,
    val connectTimeoutMs: Long = 30_000L,
    val reconnectTimeoutMs: Long = 40_000L
)
/**
 * 语音通话引擎：Android 侧用 WebRTC 实现，commonMain 只依赖本接口
 *
 * 与官网 `useVoiceCall` 的 `St()/Ne()/oe()/Ht()/$t()` 一一对应：
 * [start] = 建连 + 采集，[createOffer] = 主叫 offer（可 ICE 重启），
 * [setRemoteOfferAndCreateAnswer] = 被叫 answer，[addIceCandidate] = ICE 交换。
 */
interface VoiceEngine {
    /** 设备与依赖是否具备通话能力（WebRTC 可用） */
    val isSupported: Boolean

    /**
     * 创建 PeerConnection 并采集本地音频（含录音权限申请）
     *
     * @param ice 服务端随语音事件下发的 ICE 配置；为空时退化为 host candidate
     * @param onIceCandidate 本地 candidate（JSON 字符串）
     * @param onConnectionState 连接状态（`connected` / `disconnected` / `failed`）
     */
    suspend fun start(
        ice: VoiceIceConfigDto?,
        onIceCandidate: (String) -> Unit,
        onConnectionState: (String) -> Unit
    ): Boolean

    /** 主叫：生成 SDP offer；[iceRestart] 为真时按 ICE 重启重新协商 */
    suspend fun createOffer(iceRestart: Boolean = false): String?

    /** 被叫：设置远端 offer 并生成 answer */
    suspend fun setRemoteOfferAndCreateAnswer(remoteSdp: String): String?

    /** 主叫：设置远端 answer */
    suspend fun setRemoteAnswer(remoteSdp: String)

    /** 添加远端 ICE candidate（字符串或 JSON 对象串） */
    suspend fun addIceCandidate(candidate: String)

    fun setMuted(muted: Boolean)

    /** 切换扬声器；返回 false 表示当前设备不支持切换（官网「当前设备不支持切换扬声器」） */
    fun setSpeakerOn(on: Boolean): Boolean

    /** 当前选中的 candidate 是否走中继（`voice_connected` / `voice_hangup` 的 `used_relay`） */
    fun isRelayUsed(): Boolean

    fun close()
}

/** 创建语音引擎（Android 用 WebRTC；无实现平台返回不支持的空引擎） */
expect fun createVoiceEngine(): VoiceEngine
