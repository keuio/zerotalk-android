package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import top.lanxint.zerotalk.data.log.ZtLog
import top.lanxint.zerotalk.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

enum class WsConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTHENTICATED,
    FAILED
}

/**
 * 零语实时 WebSocket 客户端
 */
class ZeroTalkWebSocketClient(
    private val client: OkHttpClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null

    /** 认证看门狗：onOpen 后迟迟收不到 auth_success 即判失败，交给上层重连 */
    private var authTimeoutJob: Job? = null

    /** 最近一次收到服务端任意帧的时刻 */
    @Volatile
    private var lastServerActivityAtMs = 0L

    /** 最近一次收到 pong 的时刻（仅在服务端确实回过 pong 后才参与半死判定） */
    @Volatile
    private var lastPongAtMs = 0L

    /**
     * 连接代次
     *
     * 旧连接的 onClosing/onClosed/onFailure 可能晚于新连接的 onOpen 到达。
     * 若不区分，这些过期回调会把新连接的状态覆盖成 DISCONNECTED/FAILED，
     * 上层看门狗据此反复重连，表现为「看着连上了却收不到任何消息」。
     */
    private val connectionGeneration = AtomicLong(0)

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<WsServerEvent>(replay = 1, extraBufferCapacity = 64)
    val events: SharedFlow<WsServerEvent> = _events.asSharedFlow()

    @Volatile
    var currentRoomId: String? = null

    private var currentUserId: Long = 0
    private var currentWsToken: String = ""

    companion object {
        const val WS_URL = "wss://app.zerotalk.cn/ws"
        const val ORIGIN = "https://app.zerotalk.cn"
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        /** onOpen 后等待 auth_success 的上限 */
        const val AUTH_TIMEOUT_MS = 12_000L

        /** 已认证后允许的最长静默（3 个心跳周期），超过即判半死连接 */
        const val SERVER_IDLE_TIMEOUT_MS = 90_000L
    }

    /**
     * 发起 WebSocket 连接并执行认证握手
     */
    fun connect(userId: Long, wsToken: String) {
        currentUserId = userId
        currentWsToken = wsToken

        if (webSocket != null) {
            // 静默关闭旧连接：若走 disconnect() 会置 DISCONNECTED，
            // 上层看门狗会把它当成掉线而立刻又发起一次重连（重连风暴）
            connectionGeneration.incrementAndGet()
            stopPingLoop()
            stopAuthWatchdog()
            runCatching { webSocket?.close(1000, "Reconnecting") }
            webSocket = null
        }

        _connectionState.value = WsConnectionState.CONNECTING

        // 本次连接代次：所有回调只认自己这一代，过期回调直接丢弃
        val generation = connectionGeneration.incrementAndGet()
        ZtLog.d("ZeroTalk", "[WS] connect generation=$generation user=$userId")

        val request = Request.Builder()
            .url(WS_URL)
            .header("Origin", ORIGIN)
            .header("User-Agent", USER_AGENT)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (connectionGeneration.get() != generation) return
                ZtLog.d("ZeroTalk", "[WS] onOpen generation=$generation")
                _connectionState.value = WsConnectionState.CONNECTED
                // 连接建立后立即发送首帧认证数据
                sendAuth(userId, wsToken)
                // 启动 25s 心跳定时器
                startPingLoop()
                // 看门狗：WS 握手成功 != 可用，必须等 auth_success 才算连上
                startAuthWatchdog()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (connectionGeneration.get() != generation) return
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (connectionGeneration.get() != generation) return
                _connectionState.value = WsConnectionState.DISCONNECTED
                stopPingLoop()
                stopAuthWatchdog()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (connectionGeneration.get() != generation) return
                ZtLog.d("ZeroTalk", "[WS] onClosed code=$code reason=$reason")
                _connectionState.value = WsConnectionState.DISCONNECTED
                stopPingLoop()
                stopAuthWatchdog()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (connectionGeneration.get() != generation) return
                ZtLog.d("ZeroTalk", "[WS] onFailure: ${t.message}")
                _connectionState.value = WsConnectionState.FAILED
                stopPingLoop()
                stopAuthWatchdog()
            }
        })
    }

    /**
     * 发送认证帧
     */
    private fun sendAuth(userId: Long, wsToken: String) {
        val authPayload = JsonObject().apply {
            addProperty("event", "auth")
            addProperty("user_id", userId)
            addProperty("ws_token", wsToken)
            addProperty("device_id", DeviceIdManager.getDeviceId())
            addProperty("user_agent", USER_AGENT)
        }
        sendRawJson(authPayload.toString())
    }

    /**
     * 启动心跳循环 (每 25 秒一帧)
     */
    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(25_000L)
                // 半死连接判定：已认证却长时间收不到服务端任何帧（连 pong 都没有）时，
                // TCP 往往不报错，界面会一直停在「已连接」。主动判失败交给上层重连。
                // 只在「服务端确实回过 pong」的前提下才判半死：避免服务端不回 pong 时被误杀
                val lastPong = lastPongAtMs
                if (_connectionState.value == WsConnectionState.AUTHENTICATED &&
                    lastPong > 0L &&
                    System.currentTimeMillis() - lastPong > SERVER_IDLE_TIMEOUT_MS
                ) {
                    failAndClose("server idle timeout")
                    return@launch
                }
                val pingPayload = JsonObject().apply {
                    addProperty("event", "ping")
                    addProperty("room_id", currentRoomId ?: "")
                }
                sendRawJson(pingPayload.toString())
            }
        }
    }

    private fun stopPingLoop() {
        pingJob?.cancel()
        pingJob = null
    }

    /** onOpen 后若超时仍未收到 auth_success，判定本次连接不可用 */
    private fun startAuthWatchdog() {
        authTimeoutJob?.cancel()
        authTimeoutJob = scope.launch {
            delay(AUTH_TIMEOUT_MS)
            if (_connectionState.value == WsConnectionState.CONNECTED) {
                failAndClose("auth timeout")
            }
        }
    }

    private fun stopAuthWatchdog() {
        authTimeoutJob?.cancel()
        authTimeoutJob = null
    }

    /** 判失败并关闭当前 socket；上层观察到 FAILED 后会重连 */
    private fun failAndClose(reason: String) {
        ZtLog.d("ZeroTalk", "[WS] failAndClose: $reason")
        connectionGeneration.incrementAndGet()
        stopPingLoop()
        stopAuthWatchdog()
        _connectionState.value = WsConnectionState.FAILED
        runCatching { webSocket?.close(4001, reason) }
        webSocket = null
    }

    /**
     * 解析全站聊天横幅 `chat_banner`（字段与官网 `chatBanner` store 的 `Ky()` 一致）
     */
    private fun parseChatBanner(root: JsonObject): WsServerEvent {
        val data = root.getAsJsonObject("data")
        fun s(key: String): String? =
            root.get(key)?.takeIf { !it.isJsonNull }?.asString
                ?: data?.get(key)?.takeIf { !it.isJsonNull }?.asString
        fun l(key: String): Long = try {
            root.get(key)?.takeIf { !it.isJsonNull }?.asLong
                ?: data?.get(key)?.takeIf { !it.isJsonNull }?.asLong ?: 0L
        } catch (_: Exception) {
            0L
        }
        fun b(key: String, default: Boolean): Boolean = try {
            root.get(key)?.takeIf { !it.isJsonNull }?.asBoolean
                ?: data?.get(key)?.takeIf { !it.isJsonNull }?.asBoolean ?: default
        } catch (_: Exception) {
            default
        }
        return WsServerEvent.ChatBanner(
            roomId = s("room_id").orEmpty(),
            roomType = s("room_type").orEmpty(),
            roomName = s("room_name").orEmpty(),
            messageId = l("message_id"),
            preview = s("preview").orEmpty(),
            senderName = s("sender_name").orEmpty(),
            reason = s("reason").orEmpty(),
            notificationId = s("notification_id").orEmpty(),
            // 官网：unread !== false，即缺省视为计入未读
            unread = b("unread", default = true)
        )
    }

    /**
     * 解析处理服务端下发的消息
     */
    internal fun handleIncomingMessage(text: String) {
        lastServerActivityAtMs = System.currentTimeMillis()
        try {
            val root = JsonParser.parseString(text).asJsonObject
            val event = root.get("event")?.asString ?: return

            val parsedEvent: WsServerEvent = when (event) {
                "auth_success" -> {
                    ZtLog.d("ZeroTalk", "[WS] auth_success")
                    stopAuthWatchdog()
                    lastServerActivityAtMs = System.currentTimeMillis()
                    _connectionState.value = WsConnectionState.AUTHENTICATED
                    WsServerEvent.AuthSuccess
                }
                "pong" -> {
                    // 心跳响应：记录存活时刻，供半死连接判定
                    lastPongAtMs = System.currentTimeMillis()
                    return
                }
                "chat_banner" -> {
                    ZtLog.d("ZeroTalk", "[WS] chat_banner frame")
                    parseChatBanner(root)
                }
                "notification_unread" -> {
                    val data = root.getAsJsonObject("data")
                    val unread = (root.get("unread") ?: data?.get("unread"))
                        ?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                    ZtLog.d("ZeroTalk", "[WS] notification_unread=$unread")
                    WsServerEvent.NotificationUnread(unread.coerceAtLeast(0))
                }
                "matching" -> WsServerEvent.Matching
                "match_success" -> {
                    val dataObj = root.getAsJsonObject("data")
                    val roomId = (root.get("room_id") ?: dataObj?.get("room_id"))?.takeIf { !it.isJsonNull }?.asString ?: ""
                    currentRoomId = roomId
                    val matchMode = (root.get("match_mode") ?: dataObj?.get("match_mode"))
                        ?.takeIf { !it.isJsonNull }?.asString
                    // 语音匹配：服务端随 match_success 下发 voice 自动接通载荷
                    val voiceEl = (root.get("voice") ?: dataObj?.get("voice"))?.takeIf { it.isJsonObject }
                    val voice = if (voiceEl != null) {
                        gson.fromJson(voiceEl, VoiceCallInfoDto::class.java)
                    } else {
                        null
                    }
                    WsServerEvent.MatchSuccess(roomId, matchMode, voice)
                }
                "user_joined" -> {
                    parseUserJoined(root)
                }
                "message" -> {
                    val data = root.getAsJsonObject("data")
                    fun str(key: String): String? =
                        root.get(key)?.takeIf { !it.isJsonNull }?.asString
                            ?: data?.get(key)?.takeIf { !it.isJsonNull }?.asString
                    fun long(key: String): Long? = try {
                        root.get(key)?.takeIf { !it.isJsonNull }?.asLong
                            ?: data?.get(key)?.takeIf { !it.isJsonNull }?.asLong
                    } catch (_: Exception) {
                        null
                    }
                    fun bool(key: String): Boolean? = try {
                        root.get(key)?.takeIf { !it.isJsonNull }?.asBoolean
                            ?: data?.get(key)?.takeIf { !it.isJsonNull }?.asBoolean
                    } catch (_: Exception) {
                        null
                    }
                    WsServerEvent.Message(
                        roomId = str("room_id"),
                        type = str("type") ?: "text",
                        content = str("content") ?: "",
                        // 实测实时推送用 `uid`（不是 `from_uid`），两者都兼容
                        fromUid = str("from_uid") ?: str("uid"),
                        username = str("username"),
                        createdAt = str("created_at"),
                        // 实测实时推送用 `id`（不是 `message_id`），两者都兼容
                        messageId = long("message_id") ?: long("id"),
                        isSelf = bool("is_self"),
                        imageUrl = str("image_url"),
                        audioSource = str("audio_source"),
                        gender = str("gender"),
                        avatarUrl = str("avatar_url"),
                        replyToId = long("reply_to_id"),
                        replyPreview = str("reply_preview"),
                        replyToUserId = str("reply_to_user_id"),
                        replyToUsername = str("reply_username")
                    )
                }
                "typing" -> {
                    val active = root.get("active")?.asBoolean ?: false
                    WsServerEvent.Typing(active)
                }
                "match_cancelled" -> {
                    val type = root.get("type")?.asString ?: "timeout"
                    WsServerEvent.MatchCancelled(type)
                }
                "match_error" -> {
                    val msg = root.get("msg")?.asString ?: "匹配失败"
                    WsServerEvent.MatchError(msg)
                }
                "user_left" -> {
                    val roomId = root.get("room_id")?.takeIf { !it.isJsonNull }?.asString
                    WsServerEvent.UserLeft(roomId)
                }
                "room_closed" -> {
                    val roomId = root.get("room_id")?.takeIf { !it.isJsonNull }?.asString
                    WsServerEvent.RoomClosed(roomId)
                }
                else -> when {
                    // 语音通话全量下行（voice_invite_sent / ringing / offer / answer / ice / …）
                    event.startsWith("voice_") -> parseVoiceCall(event.removePrefix("voice_"), root, text)
                    event.endsWith("_update") -> {
                        val gType = event.removeSuffix("_update")
                        val gameObj = root.get("game")?.takeIf { it.isJsonObject }
                        val game = if (gameObj != null) gson.fromJson(gameObj, GameSessionDetail::class.java) else null
                        val inviteObj = root.get("invite")?.takeIf { it.isJsonObject }
                        val invite = if (inviteObj != null) gson.fromJson(inviteObj, ChatGameInvite::class.java) else null
                        val action = root.get("action")?.takeIf { !it.isJsonNull }?.asString
                        val roomId = root.get("room_id")?.takeIf { !it.isJsonNull }?.asString
                        val messageId = root.get("message_id")?.takeIf { !it.isJsonNull }?.asLong
                        WsServerEvent.GameUpdate(gType, game, invite, action, roomId, messageId, text)
                    }
                    event.endsWith("_joined") -> {
                        val gType = event.removeSuffix("_joined")
                        val gameObj = root.get("game")?.takeIf { it.isJsonObject }
                        val game = if (gameObj != null) gson.fromJson(gameObj, GameSessionDetail::class.java) else null
                        val roomId = root.get("room_id")?.takeIf { !it.isJsonNull }?.asString
                        WsServerEvent.GameJoined(gType, game, roomId, text)
                    }
                    event.endsWith("_error") -> {
                        val gType = event.removeSuffix("_error")
                        val msg = root.get("message")?.takeIf { !it.isJsonNull }?.asString ?: "游戏操作失败"
                        val gameObj = root.get("game")?.takeIf { it.isJsonObject }
                        val game = if (gameObj != null) gson.fromJson(gameObj, GameSessionDetail::class.java) else null
                        WsServerEvent.GameError(gType, msg, game)
                    }
                    else -> WsServerEvent.Unknown(event, text)
                }
            }

            scope.launch {
                _events.emit(parsedEvent)
            }
        } catch (e: Exception) {
            println("[ZeroTalkWS] Error handling incoming message: ${e.message}, raw: $text")
        }
    }

    /**
     * 兼容解析 user_joined 成员进入信息（支持 mbti 对象或字符串、布尔型清流模式以及 data 封装）
     */
    internal fun parseUserJoined(root: JsonObject): WsServerEvent.UserJoined {
        val json = if (root.has("data") && root.get("data").isJsonObject) {
            root.getAsJsonObject("data")
        } else {
            root
        }

        val uid = json.get("uid")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString ?: ""
        val username = json.get("username")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString ?: "神秘人"
        val gender = json.get("gender")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString ?: "unknown"
        val avatarUrl = json.get("avatar_url")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
        val roomId = json.get("room_id")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
            ?: root.get("room_id")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString

        val cleanEl = json.get("clean_stream_mode")
        val cleanStreamMode = when {
            cleanEl == null || cleanEl.isJsonNull -> 0
            cleanEl.isJsonPrimitive && cleanEl.asJsonPrimitive.isBoolean -> if (cleanEl.asBoolean) 1 else 0
            cleanEl.isJsonPrimitive && cleanEl.asJsonPrimitive.isNumber -> cleanEl.asInt
            cleanEl.isJsonPrimitive -> cleanEl.asString.toIntOrNull() ?: 0
            else -> 0
        }

        val mbtiEl = json.get("mbti")
        val mbtiStr = when {
            mbtiEl == null || mbtiEl.isJsonNull -> null
            mbtiEl.isJsonObject -> {
                val obj = mbtiEl.asJsonObject
                obj.get("type")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
                    ?: obj.get("name")?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
            }
            mbtiEl.isJsonPrimitive -> mbtiEl.asString
            else -> null
        }

        return WsServerEvent.UserJoined(
            uid = uid,
            username = username,
            gender = gender,
            avatarUrl = avatarUrl,
            mbti = mbtiStr,
            cleanStreamMode = cleanStreamMode,
            roomId = roomId
        )
    }

    /**
     * 解析语音通话事件（官网 `useVoiceCall` 的 `voice_*` 全量下行）
     *
     * - 载荷既可能平铺也可能包在 `data` 里，两者都查；
     * - ICE 配置来自载荷的 `ice` 字段（服务端随事件下发，是建连的唯一来源）；
     * - `candidate` 可能是字符串也可能是对象，对象形态保留原始 JSON 交给引擎解析。
     */
    internal fun parseVoiceCall(kind: String, root: JsonObject, raw: String): WsServerEvent.VoiceCall {
        val json = if (root.has("data") && root.get("data").isJsonObject) root.getAsJsonObject("data") else root
        fun str(key: String): String? =
            root.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString
                ?: json.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString

        fun long(key: String): Long? =
            root.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asLong
                ?: json.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asLong

        fun bool(key: String): Boolean? =
            root.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asBoolean
                ?: json.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asBoolean

        val iceEl = (root.get("ice") ?: json.get("ice"))?.takeIf { it.isJsonObject }
        val ice = if (iceEl != null) gson.fromJson(iceEl, VoiceIceConfigDto::class.java) else null

        val candEl = root.get("candidate") ?: json.get("candidate")
        val candidate = when {
            candEl == null || candEl.isJsonNull -> null
            candEl.isJsonPrimitive -> candEl.asString
            else -> candEl.toString()
        }

        return WsServerEvent.VoiceCall(
            kind = kind,
            callId = str("call_id")?.trim()?.lowercase().orEmpty(),
            roomId = str("room_id"),
            sdp = str("sdp"),
            candidate = candidate,
            reason = str("reason"),
            message = str("message"),
            usedRelay = bool("used_relay"),
            auto = bool("auto") == true,
            ice = ice,
            peerUserId = long("peer_user_id") ?: long("to_user_id") ?: long("from_user_id"),
            // 实测服务端用的是 from_* / to_* / peer_*，三者都要兜底（被叫侧尤其依赖 from_username）
            peerUid = str("peer_uid") ?: str("from_uid") ?: str("to_uid")
                ?: str("callee_uid") ?: str("caller_uid"),
            peerUsername = str("peer_username") ?: str("from_username") ?: str("to_username"),
            peerAvatarUrl = str("peer_avatar_url") ?: str("from_avatar_url") ?: str("to_avatar_url"),
            isCaller = bool("is_caller"),
            hintMs = long("connecting_hint_ms"),
            rawJson = raw
        )
    }

    /**
     * 发送原始 JSON 字符串
     */
    fun sendRawJson(jsonString: String): Boolean {
        return webSocket?.send(jsonString) ?: false
    }

    /**
     * 发起匹配
     */
    fun startMatch(
        mode: String = "chat",
        oppositeGenderOnly: Int = 0,
        sameGenderOnly: Int = 0
    ): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "start_match")
            addProperty("match_mode", mode)
            addProperty("match_opposite_gender_only", oppositeGenderOnly)
            addProperty("match_same_gender_only", sameGenderOnly)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 取消匹配
     */
    fun cancelMatch(): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "cancel_match")
        }
        return sendRawJson(payload.toString())
    }

    // ============================================================
    // 语音通话信令（官网 useVoiceCall 的 12 个上行帧，除 invite 外均带 call_id）
    // ============================================================

    /**
     * 发起语音通话邀请
     *
     * `client_request_id` 由调用方生成（官网格式 `vc_<时间戳>_<随机>`），用于服务端幂等去重。
     *
     * `to_user_id` 官网同时接受**数字 id** 与 **uid 字符串**
     * （`typeof I === "string" ? I.trim() : Number(I) > 0 ? Number(I) : 0`），
     * 群聊呼叫指定成员时必须携带其一。
     */
    fun voiceInvite(
        roomId: String,
        clientRequestId: String,
        toUserId: Long? = null,
        toUid: String? = null,
        source: String = "room"
    ): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "voice_invite")
            addProperty("room_id", roomId)
            addProperty("client_request_id", clientRequestId)
            addProperty("source", source)
            val numericId = toUserId?.takeIf { it > 0 }
            val uidText = toUid?.trim()?.takeIf { it.isNotBlank() }
            when {
                numericId != null -> addProperty("to_user_id", numericId)
                uidText != null -> addProperty("to_user_id", uidText)
            }
        }
        return sendRawJson(payload.toString())
    }

    /** 接听 */
    fun voiceAccept(callId: String): Boolean = voiceSimple("voice_accept", callId)

    /** 拒接 */
    fun voiceReject(callId: String): Boolean = voiceSimple("voice_reject", callId)

    /** 主叫在对方接听前取消呼叫 */
    fun voiceCancel(callId: String): Boolean = voiceSimple("voice_cancel", callId)

    /** 刷新 / 断线重连后恢复通话 */
    fun voiceResume(callId: String): Boolean = voiceSimple("voice_resume", callId)

    /** 通话中心跳（官网 45s 一次） */
    fun voiceHeartbeat(callId: String): Boolean = voiceSimple("voice_heartbeat", callId)

    /**
     * 挂断
     *
     * @param usedRelay 本次通话是否走了中继（服务端统计用；由引擎的 `isRelayUsed()` 提供）
     */
    fun voiceHangup(callId: String, usedRelay: Boolean): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "voice_hangup")
            addProperty("call_id", callId)
            addProperty("used_relay", usedRelay)
        }
        return sendRawJson(payload.toString())
    }

    /** 发送 SDP offer（`iceRestart=true` 时为 ICE 重启） */
    fun voiceOffer(callId: String, sdp: String): Boolean = voiceWithSdp("voice_offer", callId, sdp)

    /** 发送 SDP answer */
    fun voiceAnswer(callId: String, sdp: String): Boolean = voiceWithSdp("voice_answer", callId, sdp)

    /** 发送本地 ICE candidate（字符串形态） */
    fun voiceIce(callId: String, candidate: String): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "voice_ice")
            addProperty("call_id", callId)
            addProperty("candidate", candidate)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 媒体已连通（触发服务端计费/统计与心跳启动）
     *
     * @param usedRelay 是否走中继
     */
    fun voiceConnected(callId: String, usedRelay: Boolean): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "voice_connected")
            addProperty("call_id", callId)
            addProperty("used_relay", usedRelay)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 建连失败上报
     *
     * @param reason 取值对齐官网：`media_failed` / `offer_failed` / `answer_failed` /
     *        `webrtc_failed` / `reconnect_timeout` 等
     */
    fun voiceFailed(callId: String, reason: String): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "voice_failed")
            addProperty("call_id", callId)
            addProperty("reason", reason)
        }
        return sendRawJson(payload.toString())
    }

    private fun voiceSimple(event: String, callId: String): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", event)
            addProperty("call_id", callId)
        }
        return sendRawJson(payload.toString())
    }

    private fun voiceWithSdp(event: String, callId: String, sdp: String): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", event)
            addProperty("call_id", callId)
            addProperty("sdp", sdp)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 进入/切换房间（多窗关键：发完 join_room 之后发消息不带 room_id）
     */
    fun joinRoom(roomId: String): Boolean {
        currentRoomId = roomId
        val payload = JsonObject().apply {
            addProperty("event", "join_room")
            addProperty("room_id", roomId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 退出当前房间
     */
    fun leaveRoom(): Boolean {
        currentRoomId = null
        val payload = JsonObject().apply {
            addProperty("event", "leave_room")
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 发送指定 type 的聊天消息帧（官网统一入口 `ke({event:"message",type,content,...})`）
     *
     * 帧形如 `{"event":"message","type":"<type>","content":"<content>"}`，
     * 动态分享卡片即 `type:"moment_share"`（content 为卡片 JSON）。
     * 回复与 @ 提及字段对所有类型一致生效。
     *
     * @param type 消息类型（text / moment_share / …），缺省 text
     * @param replyToId 引用回复的服务端消息 id（<=0 不带该字段）
     * @param mentionIds @ 提及的 uid 列表（去空去重后写入 `mention_ids`）
     */
    fun sendMessage(
        content: String,
        type: String = "text",
        replyToId: Long? = null,
        mentionIds: List<String> = emptyList()
    ): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "message")
            addProperty("type", type)
            addProperty("content", content)
            replyToId?.takeIf { it > 0L }?.let { addProperty("reply_to_id", it) }
            val validMentionIds = mentionIds.map(String::trim).filter(String::isNotBlank).distinct()
            if (validMentionIds.isNotEmpty()) {
                add("mention_ids", com.google.gson.JsonArray().apply {
                    validMentionIds.forEach(::add)
                })
            }
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 发送文字消息
     */
    fun sendTextMessage(content: String, replyToId: Long? = null, mentionIds: List<String> = emptyList()): Boolean =
        sendMessage(content = content, type = "text", replyToId = replyToId, mentionIds = mentionIds)

    /**
     * 发送拍一拍
     *
     * 官方帧（实测 + 官网源码一致）：`{"event":"message","type":"pat","content":"","target_user_id":"<uid|数字id>"}`
     *
     * - `content` 必须为**空串**，目标放在顶层 `target_user_id`；
     * - 拍一拍后缀（`sfx`）属于**被拍者**，由服务端从对方资料读取并写进回推的 content，
     *   客户端**不能**自己拼 content（旧实现发 `{"to_uid":…,"to_username":…,"suffix":…}` 与协议不符，服务端不会识别目标）。
     *
     * 服务端回推示例（实测）：
     * ```json
     * {"event":"message","id":3344570,"type":"pat","is_self":true,
     *  "content":"{\"v\":1,\"sfx\":\"爹地说：想挨法\",\"a\":\"ok\",\"t\":\"kelo\",\"tuid\":\"eefee851…\"}"}
     * ```
     */
    fun sendPat(targetUserId: String): Boolean {
        val target = targetUserId.trim()
        if (target.isEmpty()) return false
        val payload = JsonObject().apply {
            addProperty("event", "message")
            addProperty("type", "pat")
            addProperty("content", "")
            addProperty("target_user_id", target)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 发送图片消息
     *
     * 实测帧：`{"event":"message","content":"<图片URL>","type":"image","image_url":"<图片URL>"}`
     */
    fun sendImageMessage(imageUrl: String, content: String = ""): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "message")
            addProperty("type", "image")
            addProperty("content", content.ifBlank { imageUrl })
            addProperty("image_url", imageUrl)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 发送语音消息
     *
     * 实测帧：`{"event":"message","content":"<音频URL>","type":"audio","audio_source":"record|file"}`
     * （旧文档记的 `type:"voice"` 与空 content 均不准确）
     *
     * @param audioSource `record`（录音发送）/ `file`（语音文件发送）
     */
    fun sendAudioMessage(audioUrl: String, audioSource: String = "record"): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "message")
            addProperty("type", "audio")
            addProperty("content", audioUrl)
            addProperty("audio_source", audioSource)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 发送摇骰子
     */
    fun sendDice(): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "message")
            addProperty("type", "dice")
            addProperty("content", "")
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 发送歌曲分享消息
     *
     * 实测帧：`{"event":"message","type":"music","content":"{...song JSON...}"}`
     */
    fun sendMusicMessage(songJsonString: String): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "message")
            addProperty("type", "music")
            addProperty("content", songJsonString)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 发起棋牌游戏对局（五子棋 gobang / 围棋 go / 象棋 xiangqi / 国际象棋 chess / 谁是卧底 undercover）
     *
     * 实测帧：`{"event":"gobang_create"}`
     */
    fun sendGameCreate(gameType: String = "gobang"): Boolean {
        val eventName = if (gameType.endsWith("_create")) gameType else "${gameType}_create"
        val payload = JsonObject().apply {
            addProperty("event", eventName)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 接受应战 / 加入游戏对局
     */
    fun sendGameJoin(gameType: String, gameId: Long): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "${gameType}_join")
            addProperty("game_id", gameId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 五子棋落子
     */
    fun sendGobangMove(gameId: Long, x: Int, y: Int, version: Int): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "gobang_move")
            addProperty("game_id", gameId)
            addProperty("x", x)
            addProperty("y", y)
            addProperty("version", version)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 围棋落子
     */
    fun sendGoMove(gameId: Long, x: Int, y: Int, version: Int): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "go_move")
            addProperty("game_id", gameId)
            addProperty("x", x)
            addProperty("y", y)
            addProperty("version", version)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 围棋停一手 (Pass)
     */
    fun sendGoPass(gameId: Long, version: Int): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "go_pass")
            addProperty("game_id", gameId)
            addProperty("version", version)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 中国象棋走子 [ff, fr, tf, tr]
     */
    fun sendXiangqiMove(gameId: Long, ff: Int, fr: Int, tf: Int, tr: Int, version: Int): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "xiangqi_move")
            addProperty("game_id", gameId)
            addProperty("ff", ff)
            addProperty("fr", fr)
            addProperty("tf", tf)
            addProperty("tr", tr)
            addProperty("version", version)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 国际象棋走子 [ff, fr, tf, tr, promo]
     */
    fun sendChessMove(gameId: Long, ff: Int, fr: Int, tf: Int, tr: Int, promo: Int = 0, version: Int): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "chess_move")
            addProperty("game_id", gameId)
            addProperty("ff", ff)
            addProperty("fr", fr)
            addProperty("tf", tf)
            addProperty("tr", tr)
            addProperty("promo", promo)
            addProperty("version", version)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 对局认输 (不带 version)
     */
    fun sendGameResign(gameType: String, gameId: Long): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "${gameType}_resign")
            addProperty("game_id", gameId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 取消对局邀请
     */
    fun sendGameCancel(gameType: String, gameId: Long): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "${gameType}_cancel")
            addProperty("game_id", gameId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 谁是卧底：准备 / 取消准备
     */
    fun sendUndercoverReady(gameId: Long, ready: Boolean): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "undercover_ready")
            addProperty("game_id", gameId)
            addProperty("ready", ready)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 谁是卧底：房主修改设置
     */
    fun sendUndercoverSettings(
        gameId: Long,
        maxPlayers: Int,
        blankEnabled: Boolean,
        describeSec: Int,
        discussSec: Int,
        voteSec: Int
    ): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "undercover_settings")
            addProperty("game_id", gameId)
            val settings = JsonObject().apply {
                addProperty("max_players", maxPlayers)
                addProperty("blank_enabled", blankEnabled)
                addProperty("describe_sec", describeSec)
                addProperty("discuss_sec", discussSec)
                addProperty("vote_sec", voteSec)
            }
            add("settings", settings)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 谁是卧底：房主踢人
     */
    fun sendUndercoverKick(gameId: Long, targetUserId: Long): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "undercover_kick")
            addProperty("game_id", gameId)
            addProperty("target_user_id", targetUserId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 谁是卧底：房主开始游戏 (≥4人且全员准备)
     */
    fun sendUndercoverStart(gameId: Long): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "undercover_start")
            addProperty("game_id", gameId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 谁是卧底：查看身份确认
     */
    fun sendUndercoverConfirm(gameId: Long): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "undercover_confirm")
            addProperty("game_id", gameId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 谁是卧底：发言描述
     */
    fun sendUndercoverDescribe(gameId: Long, text: String): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "undercover_describe")
            addProperty("game_id", gameId)
            addProperty("text", text)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 谁是卧底：投票
     */
    fun sendUndercoverVote(gameId: Long, targetUserId: Long): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "undercover_vote")
            addProperty("game_id", gameId)
            addProperty("target_user_id", targetUserId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 谁是卧底：白板猜词
     */
    fun sendUndercoverWhiteGuess(gameId: Long, guess: String): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "undercover_white_guess")
            addProperty("game_id", gameId)
            addProperty("guess", guess)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 谁是卧底：离开房间
     */
    fun sendUndercoverLeave(gameId: Long): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "undercover_leave")
            addProperty("game_id", gameId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 发送正在输入状态
     */
    fun sendTyping(active: Boolean): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "typing")
            addProperty("active", active)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 撤回消息
     */
    fun recallMessage(messageId: Long): Boolean {
        val payload = JsonObject().apply {
            addProperty("event", "recall_message")
            addProperty("message_id", messageId)
        }
        return sendRawJson(payload.toString())
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        // 先让本连接的所有回调失效，避免 onClosing/onClosed 回来又改状态
        connectionGeneration.incrementAndGet()
        stopPingLoop()
        stopAuthWatchdog()
        lastServerActivityAtMs = 0L
        lastPongAtMs = 0L
        webSocket?.close(1000, "Client closed")
        webSocket = null
        _connectionState.value = WsConnectionState.DISCONNECTED
    }
}
