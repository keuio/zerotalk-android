package top.lanxint.zerotalk.data.network

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import top.lanxint.zerotalk.data.model.GameSessionDetail
import top.lanxint.zerotalk.data.model.ChatGameInvite
import top.lanxint.zerotalk.data.model.MbtiInfo

/**
 * 接口通用基础响应模型
 */
data class ApiResponse<T>(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("data") val data: T? = null
) {
    val isSuccess: Boolean get() = code == 1
}

/**
 * 登录成功返回数据模型
 */
data class LoginData(
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName("uid") val uid: String = "",
    @SerializedName("login_name") val loginName: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("gender") val gender: String = "male",
    @SerializedName("location") val location: String? = null
)

/**
 * 头像上传限制与状态数据 (user.avatar_upload)
 */
data class AvatarUploadDto(
    @SerializedName("can_upload") val canUpload: Boolean = false,
    @SerializedName("days_remaining") val daysRemaining: Int = 0,
    @SerializedName("required_days") val requiredDays: Int = 7,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("has_custom") val hasCustom: Boolean = false,
    @SerializedName("has_qq") val hasQq: Boolean = false
)

/**
 * Bootstrap 返回的用户详情模型
 */
data class BootstrapUser(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("uid") val uid: String = "",
    @SerializedName("login_name") val loginName: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("gender") val gender: String = "male",
    @SerializedName("gender_label") val genderLabel: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("location") val location: String? = null,
    /**
     * MBTI 原始 JSON：对象 `{"type":"INFP","name":"调停者",...}` 或早期字符串 `"INFP"`，
     * 未填写时为 null。保留 [JsonElement] 交给 [MbtiInfo.fromJson] 容错解析，
     * 避免声明成对象类型时遇到字符串形态抛异常、导致整条 bootstrap 解析失败。
     */
    @SerializedName("mbti") val mbti: JsonElement? = null,
    @SerializedName("age_range") val ageRange: String? = null,
    @SerializedName("pat_text") val patText: String? = null,
    @SerializedName("qq") val qq: String? = null,
    @SerializedName("avatar_upload") val avatarUpload: AvatarUploadDto? = null,
    @SerializedName("has_custom_avatar") val hasCustomAvatar: Boolean = false
) {
    /** MBTI 完整信息（服务端下发对象或字符串；缺失/异常形态为 null） */
    val mbtiInfo: MbtiInfo? get() = MbtiInfo.fromJson(mbti)
}

/**
 * Bootstrap 返回的公共房间模型
 */
data class PublicRoomDto(
    @SerializedName("room_id") val roomId: String = "",
    @SerializedName("room_name") val roomName: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("member_count") val memberCount: Int = 0
)

/**
 * Bootstrap 返回的数据模型
 */
data class BootstrapData(
    @SerializedName("user") val user: BootstrapUser? = null,
    @SerializedName("ws_url") val wsUrl: String? = null,
    @SerializedName("ws_token") val wsToken: String? = null,
    @SerializedName("match_timeout_seconds") val matchTimeoutSeconds: Int? = 30,
    @SerializedName("online_users") val onlineUsers: Int? = 0,
    @SerializedName("public_rooms") val publicRooms: List<PublicRoomDto>? = null,
    /** 语音通话服务端配置（含是否开放、ICE 传输策略、各阶段超时秒数） */
    @SerializedName("voice_call") val voiceCall: VoiceCallConfigDto? = null
)

/**
 * 语音通话服务端配置（bootstrap 的 `voice_call` 块）
 *
 * 实测：`{"enabled":true,"force_turn":false,"ice_transport_policy":"all",
 * "ring_timeout_seconds":45,"connect_timeout_seconds":30,"reconnect_timeout_seconds":40}`
 *
 * 注意：**ICE 服务器不在这里**，而是服务端随 `voice_invite_sent` / `voice_ringing` / `voice_restored`
 * 等语音事件的 `ice` 字段逐次下发。
 */
data class VoiceCallConfigDto(
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("force_turn") val forceTurn: Boolean = false,
    @SerializedName("ice_transport_policy") val iceTransportPolicy: String? = null,
    @SerializedName("ring_timeout_seconds") val ringTimeoutSeconds: Int = 45,
    @SerializedName("connect_timeout_seconds") val connectTimeoutSeconds: Int = 30,
    @SerializedName("reconnect_timeout_seconds") val reconnectTimeoutSeconds: Int = 40
)
/**
 * 在线用户数响应模型 (GET /api/home/online)
 */
data class OnlineCountData(
    @SerializedName("online_users") val onlineUsers: Int = 0
)

/**
 * 进入房间返回数据
 */
data class EnterRoomData(
    @SerializedName("room_id") val roomId: String = "",
    @SerializedName("room_name") val roomName: String = ""
)

/**
 * 动态列表项 DTO
 */
data class MomentItemDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("username") val username: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("gender") val gender: String = "female",
    @SerializedName("content") val content: String = "",
    @SerializedName("images") val images: List<String>? = null,
    @SerializedName("audio_url") val audioUrl: String? = null,
    /**
     * 动态附带的音乐（官方 `music` 对象）。
     *
     * 列表接口下发 JSON 对象，发布接口回显时可能下发 JSON 字符串，
     * 因此保留原始 [JsonElement] 由映射层兼容两种形态。
     */
    @SerializedName("music") val music: JsonElement? = null,
    @SerializedName("like_count") val likeCount: Int = 0,
    @SerializedName("comment_count") val commentCount: Int = 0,
    @SerializedName("liked") val liked: Boolean = false,
    @SerializedName("is_following") val isFollowing: Boolean = false,
    @SerializedName("is_pinned") val isPinned: Boolean = false,
    @SerializedName("is_private") val isPrivate: Boolean = false,
    @SerializedName("audience_mode") val audienceMode: String? = null,
    @SerializedName("audience_mutual") val audienceMutual: Boolean? = null,
    @SerializedName("created_at") val createdAt: String = "",
    /** 作者称号（官方 MomentCard `item.title`）；未下发为 null */
    @SerializedName("title") val title: String? = null,
    /** 作者称号颜色 key（官方 `item.title_color`） */
    @SerializedName("title_color") val titleColor: String? = null,
    // ---- 捞取相关字段（仅 /moment/fish 与 /api/moment/fish-history 返回，其余接口为空） ----
    @SerializedName("fish_log_id") val fishLogId: Long = 0,
    @SerializedName("fished_at") val fishedAt: JsonElement? = null
) {
    /** 兼容服务端下发数字时间戳（Long）、ISO-8601 或普通字符串 */
    val fishedAtRaw: String?
        get() {
            val elem = fishedAt ?: return null
            if (elem.isJsonNull) return null
            return if (elem.isJsonPrimitive) {
                val prim = elem.asJsonPrimitive
                if (prim.isNumber) prim.asLong.toString() else prim.asString
            } else elem.toString()
        }
}

/**
 * 直传凭证 (POST /api/upload/presign)
 */
data class UploadPresignData(
    @SerializedName("ticket_id") val ticketId: String = "",
    @SerializedName("upload_url") val uploadUrl: String? = null,
    @SerializedName("headers") val headers: Map<String, String>? = null,
    @SerializedName("content_length") val contentLength: Long = 0
)

/**
 * 上传结果 (POST /api/upload/bind 与旧版 POST /api/upload)
 */
data class UploadResultData(
    @SerializedName("url") val url: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("upload_file_id") val uploadFileId: Long = 0
)

/**
 * 动态 Feed 分页返回
 */
data class MomentFeedData(
    @SerializedName("list") val list: List<MomentItemDto>? = null,
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("next_before_id") val nextBeforeId: Long? = null
)

/**
 * 捞取一条动态返回 (POST /moment/fish)
 *
 * 无可捞内容时服务端返回成功但 data.moment 为空，此时应提示「暂时没有可捞的新动态了」
 */
data class FishMomentData(
    @SerializedName("moment") val moment: MomentItemDto? = null
)

/**
 * WebSocket 服务端下发事件密封类
 */
sealed class WsServerEvent {
    object AuthSuccess : WsServerEvent()
    object Matching : WsServerEvent()
    /**
     * 匹配成功
     *
     * @param matchMode 服务端回传的匹配模式（`voice` / `chat`）
     * @param voice 语音匹配时的自动接通载荷（官网 `applyMatchVoiceAuto(F.voice)`），
     *        含 `call_id` / `room_id` / 对端信息；非语音匹配为 null
     */
    data class MatchSuccess(
        val roomId: String,
        val matchMode: String? = null,
        val voice: VoiceCallInfoDto? = null
    ) : WsServerEvent()
    data class UserJoined(
        val uid: String,
        val username: String,
        val gender: String,
        val avatarUrl: String?,
        val mbti: String?,
        val cleanStreamMode: Int = 0,
        val roomId: String? = null
    ) : WsServerEvent()
    data class Message(
        val roomId: String?,
        val type: String,
        val content: String,
        val fromUid: String?,
        val username: String?,
        val createdAt: String?,
        val messageId: Long? = null,
        val imageUrl: String? = null,
        /** 语音来源：record（录音）/ file（语音文件），仅 `type="audio"` 携带 */
        val audioSource: String? = null,
        val gender: String? = null,
        val avatarUrl: String? = null,
        /** 引用回复相关（reply_to_id / reply_preview / reply_to_user_id / reply_username） */
        val replyToId: Long? = null,
        val replyPreview: String? = null,
        val replyToUserId: String? = null,
        val replyToUsername: String? = null,
        /**
         * 服务端显式下发的「是否本人发出」（实测实时推送带 `is_self`）。
         *
         * 该字段比「按 from_uid / 昵称推断」更可靠：实时推送里发送者字段是 `uid`，
         * 而缺失时的昵称兜底会在改名后失效。
         */
        val isSelf: Boolean? = null,
        /**
         * 服务端标记的「已撤回 / 已删除」（官网 `is_deleted`，WS 实时帧同样携带）。
         *
         * 官网 `ws.on("message", …)` 把它归一化为 `is_deleted: !!e.is_deleted`；
         * 消息可能以「已撤回」状态实时到达，映射层必须透传到 ChatMessage。
         */
        val isDeleted: Boolean = false,
        /** 发送者称号（官方消息行 `msg.title`）；未下发为 null */
        val title: String? = null,
        /** 发送者称号颜色 key（官方消息行 `msg.title_color`） */
        val titleColor: String? = null
    ) : WsServerEvent()

    /**
     * 消息撤回 / 房管删除（官网 `message_recalled`）
     *
     * 官网 `handleMessageRecall(event, addSystemMessage)` 的行为（逐字段对齐）：
     * - 用 `message_id` 定位消息，标记 `is_deleted=true` 并清空 `content` / `image_url` /
     *   `reply_preview` / `mention_ids` / `mention_users`；
     * - 所有 `reply_to_id == message_id` 的消息，引用摘要改为「该消息已被撤回」；
     * - `username` 非空且该消息此前未被标记删除时追加系统提示：
     *   `by_moderator` → 「审核员 X 删除了一条消息」；
     *   `by_room_admin` → 「管理员 X 删除了一条消息」；否则「X 撤回了一条消息」。
     *
     * 本客户端与官网一致：命中消息**保留在列表里**，由仓库层标记
     * `ChatMessage.isDeleted = true` 并清空正文 / 媒体地址（[top.lanxint.zerotalk.data.model.asRecalled]），
     * 引用它的消息改写成「该消息已被撤回」，再按官网口径补系统提示（幂等，已撤回的不重复补）。
     *
     * @param roomId 服务端下发的房间 id（官网聊天页是房间作用域、不依赖该字段；
     *        本客户端多房间共享同一 WS，缺失时按消息归属房间兜底）
     * @param messageId 被撤回 / 删除的消息服务端 id（`message_id`，兼容 `id`）
     * @param username 操作者昵称（用于生成系统提示；缺失则只移除消息）
     * @param byModerator 审核员删除（`by_moderator`）
     * @param byRoomAdmin 房管删除（`by_room_admin`）
     */
    data class MessageRecalled(
        val roomId: String?,
        val messageId: Long,
        val username: String? = null,
        val byModerator: Boolean = false,
        val byRoomAdmin: Boolean = false
    ) : WsServerEvent()

    /**
     * 全站聊天横幅（官网 `chat_banner`）
     *
     * 官网 `AppLayout` 里是**独立订阅**的：`ws.on("chat_banner", payload => chatBanner.enqueueFromWs(payload, 当前房间))`。
     * 该事件由服务端主动推送，**不要求客户端已 join 该房间**，因此通知与未读必须以它为准，
     * 而不是只靠 `message`（后者只在已进入该房间时才有）。
     */
    data class ChatBanner(
        val roomId: String,
        /** normal(匹配) / dm(私聊) / private(暗号) / public(大厅) */
        val roomType: String,
        val roomName: String,
        val messageId: Long,
        val preview: String,
        val senderName: String,
        /** message / mention(@你) / reply(回复了你) */
        val reason: String,
        val notificationId: String,
        /** 是否计入未读（官网 `unread !== false`） */
        val unread: Boolean
    ) : WsServerEvent()

    /** 实时未读总数（官网 `notification_unread`：`{unread:N}`），用于刷新会话未读 */
    data class NotificationUnread(val unread: Int) : WsServerEvent()

    data class Typing(val active: Boolean) : WsServerEvent()
    data class MatchCancelled(val type: String) : WsServerEvent()
    data class MatchError(val msg: String) : WsServerEvent()
    data class UserLeft(val roomId: String?) : WsServerEvent()
    data class RoomClosed(val roomId: String?) : WsServerEvent()
    data class GameUpdate(
        val gameType: String,
        val game: GameSessionDetail?,
        val invite: ChatGameInvite?,
        val action: String?,
        val roomId: String?,
        val messageId: Long?,
        val rawJson: String
    ) : WsServerEvent()
    data class GameJoined(
        val gameType: String,
        val game: GameSessionDetail?,
        val roomId: String?,
        val rawJson: String
    ) : WsServerEvent()
    data class GameError(
        val gameType: String,
        val message: String,
        val game: GameSessionDetail?
    ) : WsServerEvent()

    /**
     * 语音通话事件（官网 `useVoiceCall` 的 `voice_*` 全量下行）
     *
     * 载荷字段随事件而异，除常用字段外保留 [rawJson] 兜底：
     * - `kind`：去掉 `voice_` 前缀的事件名（`invite_sent` / `ringing` / `accepted` / `offer` /
     *   `answer` / `ice` / `connected` / `ended` / `timeout` / `error` / `peer_reconnecting` /
     *   `restored` / `call_banned`）
     * - `ice`：服务端随事件下发的 ICE 配置（`ice_servers` + `ice_transport_policy`），**通话建连的唯一来源**
     * - `auto`：语音匹配自动接通标记；`hintMs`：`connecting_hint_ms`（默认 3000）
     */
    data class VoiceCall(
        val kind: String,
        val callId: String,
        val roomId: String?,
        val sdp: String?,
        val candidate: String?,
        val reason: String?,
        val message: String?,
        val usedRelay: Boolean?,
        val auto: Boolean,
        val ice: VoiceIceConfigDto?,
        val peerUserId: Long?,
        val peerUid: String?,
        val peerUsername: String?,
        val peerAvatarUrl: String?,
        val isCaller: Boolean?,
        val hintMs: Long?,
        val rawJson: String
    ) : WsServerEvent()

    data class Unknown(val rawEvent: String, val rawJson: String) : WsServerEvent()
}

/**
 * 语音通话 · ICE 服务器条目
 *
 * 实测服务端下发形态（`voice_invite_sent.ice.ice_servers[]`）：
 * - `{"urls":"stun:ztturn.avrinbai.cn:3478"}` —— 单个地址是**字符串**
 * - `{"urls":["turn:...udp","turn:...tcp","turns:...tcp"],"username":"...","credential":"..."}` —— 多个地址是**数组**
 *
 * 因此 [urls] 与 [url] 都按“字符串或字符串数组”宽松解析。
 */
data class VoiceIceServerDto(
    @SerializedName(value = "urls", alternate = ["url"]) val rawUrls: JsonElement? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("credential") val credential: String? = null
) {
    /** 归一化后的地址列表（数组或单串均可） */
    val urls: List<String>
        get() {
            val element = rawUrls ?: return emptyList()
            return when {
                element.isJsonArray -> element.asJsonArray
                    .mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asString }
                    .filter { it.isNotBlank() }

                element.isJsonPrimitive -> listOf(element.asString).filter { it.isNotBlank() }
                else -> emptyList()
            }
        }
}

/**
 * 语音通话 · ICE 配置（服务端语音事件里的 `ice` 字段）
 *
 * `ice_transport_policy` 为 `relay` 时只走中继（对应 WebRTC 的 `IceTransportPolicy.RELAY`），否则 `ALL`。
 */
data class VoiceIceConfigDto(
    @SerializedName("ice_servers") val iceServers: List<VoiceIceServerDto>? = null,
    @SerializedName("ice_transport_policy") val iceTransportPolicy: String? = null
) {
    val relayOnly: Boolean get() = iceTransportPolicy.equals("relay", ignoreCase = true)
}

/**
 * 语音通话 · 当前通话 (GET /api/voice/call/current)
 *
 * 用于刷新/重连后恢复通话（官网 `resume_start source=server`）。
 */
data class VoiceCurrentCallData(
    @SerializedName("has_call") val hasCall: Boolean = false,
    @SerializedName("call") val call: VoiceCallInfoDto? = null
)

/** 语音通话 · 通话信息 (GET /api/voice/call/current 的 `call`) */
data class VoiceCallInfoDto(
    @SerializedName("call_id") val callId: String = "",
    @SerializedName("room_id") val roomId: String = "",
    @SerializedName("state") val state: String = "",
    @SerializedName("is_caller") val isCaller: Boolean = false,
    @SerializedName("caller_uid") val callerUid: String? = null,
    @SerializedName("callee_uid") val calleeUid: String? = null,
    @SerializedName("peer_user_id") val peerUserId: Long = 0,
    @SerializedName("peer_uid") val peerUid: String? = null,
    @SerializedName("peer_username") val peerUsername: String? = null,
    @SerializedName("peer_avatar_url") val peerAvatarUrl: String? = null,
    @SerializedName("ice") val ice: VoiceIceConfigDto? = null
)

/**
 * 房间对端用户信息模型
 */
data class PeerUserDto(
    @SerializedName(value = "user_id", alternate = ["id"]) val userId: Long = 0,
    @SerializedName("uid") val uid: String = "",
    @SerializedName("login_name") val loginName: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("remark") val remark: String? = null,
    @SerializedName(value = "is_online", alternate = ["online", "online_status", "isOnline", "onlineStatus"]) val isOnline: Boolean? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName(value = "clean_stream_mode", alternate = ["clean_stream", "cleanStreamMode", "cleanStream", "clean_stream_enabled"]) val cleanStreamMode: Boolean? = null,
    /** 对方是否开启在线状态展示（为 0/false 时即隐身模式） */
    @SerializedName(value = "show_online_status", alternate = ["showOnlineStatus", "online_status_visible"]) val showOnlineStatus: Boolean? = null,
    /** 对端称号（官方 peer 对象 `title`） */
    @SerializedName("title") val title: String? = null,
    /** 对端称号颜色 key（官方 `title_color`） */
    @SerializedName("title_color") val titleColor: String? = null
)

/**
 * 房间列表项 DTO (/room/list)
 */
data class RoomListItemDto(
    @SerializedName("room_id") val roomId: String = "",
    @SerializedName("room_name") val roomName: String = "",
    @SerializedName("type") val type: String = "normal", // normal: 匹配, private: 暗号, dm: 私聊
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("last_message_at") val lastMessageAt: String? = null,
    @SerializedName("created_by_uid") val createdByUid: String? = null,
    @SerializedName("is_creator") val isCreator: Boolean = false,
    @SerializedName("member_count") val memberCount: Int = 0,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("is_pinned") val isPinned: Boolean = false,
    @SerializedName("is_muted") val isMuted: Boolean = false,
    @SerializedName("peer") val peer: PeerUserDto? = null
)

/**
 * 房间列表分页返回数据
 */
data class RoomListData(
    @SerializedName("rooms") val rooms: List<RoomListItemDto> = emptyList(),
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("per_page") val perPage: Int = 20,
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("type") val type: String = "all",
    @SerializedName("tab_alerts") val tabAlerts: Map<String, Int>? = null
)

/**
 * 单聊消息 DTO (/api/chat/bootstrap, /api/chat/messages)
 */
data class ChatMessageDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("content") val content: String = "",
    @SerializedName("type") val type: String = "text",
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("audio_url") val audioUrl: String? = null,
    @SerializedName("audio_source") val audioSource: String? = null,
    @SerializedName("reply_to_id") val replyToId: Long? = null,
    @SerializedName("reply_preview") val replyPreview: String? = null,
    @SerializedName("reply_to_user_id") val replyToUserId: String? = null,
    @SerializedName("reply_username") val replyUsername: String? = null,
    @SerializedName("is_deleted") val isDeleted: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("media_locked") val mediaLocked: Boolean = false,
    /** 发送者称号（官方 UserTitleBadge 的 `title`）；未下发为 null */
    @SerializedName("title") val title: String? = null,
    /** 发送者称号颜色 key（官方 `title_color`，白名单外 UI 回落 blue） */
    @SerializedName("title_color") val titleColor: String? = null
)

/**
 * 聊天室初始化元数据 DTO (/api/chat/bootstrap)
 */
data class ChatBootstrapData(
    @SerializedName("room_id") val roomId: String = "",
    @SerializedName("room") val room: RoomListItemDto? = null,
    @SerializedName("messages") val messages: List<ChatMessageDto> = emptyList(),
    @SerializedName("has_more_messages") val hasMoreMessages: Boolean = false,
    @SerializedName("peer_user") val peerUser: PeerUserDto? = null,
    @SerializedName("is_match_room") val isMatchRoom: Boolean = false,
    @SerializedName("is_dm_room") val isDmRoom: Boolean = false,
    @SerializedName("is_public_room") val isPublicRoom: Boolean = false,
    @SerializedName("encryption_enabled") val encryptionEnabled: Boolean = false,
    @SerializedName("encryption_unlocked") val encryptionUnlocked: Boolean = true,
    @SerializedName("ws_token") val wsToken: String? = null,
    // ---- 群聊权限与成员（服务端权威下发，客户端不自算）----
    @SerializedName("members") val members: List<BootstrapMemberDto>? = null,
    @SerializedName("viewer_is_admin") val viewerIsAdmin: Boolean = false,
    @SerializedName("viewer_can_kick") val viewerCanKick: Boolean = false,
    @SerializedName("viewer_can_delete_message") val viewerCanDeleteMessage: Boolean = false,
    /** 清流策略：本房间是否强制 / 已绕过违禁词检测（官方 PeerProfileCard 文案分支） */
    @SerializedName("clean_stream_forced") val cleanStreamForced: Boolean = false,
    @SerializedName("clean_stream_bypassed") val cleanStreamBypassed: Boolean = false,
    @SerializedName("is_muted") val isMuted: Boolean = false,
    @SerializedName("announcement") val announcement: String? = null,
    @SerializedName("status") val status: String? = null
)

/**
 * 历史消息分页加载返回 DTO (/api/chat/messages)
 */
data class ChatMessagesData(
    @SerializedName("room_id") val roomId: String = "",
    @SerializedName("messages") val messages: List<ChatMessageDto> = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean = false
)

/**
 * 创建暗号房间返回数据 (/room/create)
 */
data class CreateRoomData(
    @SerializedName("room_id") val roomId: String = "",
    @SerializedName("room_name") val roomName: String = "",
    @SerializedName("creator_username") val creatorUsername: String? = null,
    /** 未下发时按「未加密」处理：官方创建弹窗默认不加密 */
    @SerializedName("encryption_enabled") val encryptionEnabled: Boolean? = false,
    @SerializedName("share_hint") val shareHint: String? = null
)

/**
 * 加入暗号房间返回数据 (/room/join)
 */
data class JoinRoomData(
    @SerializedName("room_id") val roomId: String = ""
)

/**
 * 动态评论项 DTO (/api/moment/comments)
 */
data class MomentCommentDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("moment_id") val momentId: Long = 0,
    @SerializedName("uid") val uid: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("avatar_fallback") val avatarFallback: String? = null,
    @SerializedName("content") val content: String = "",
    @SerializedName("like_count") val likeCount: Int = 0,
    @SerializedName("liked") val liked: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("parent_id") val parentId: Long? = null,
    @SerializedName("reply_to_username") val replyToUsername: String? = null,
    @SerializedName("reply_to_user_id") val replyToUserId: Long? = null,
    @SerializedName("replies") val replies: List<MomentCommentDto> = emptyList()
)

/**
 * 评论点赞返回数据 (/moment/comment/like)
 */
data class CommentLikeData(
    @SerializedName("liked") val liked: Boolean = false,
    @SerializedName("like_count") val likeCount: Int = 0
)

/**
 * 动态板块分类（精选、关注、我的）
 */
enum class MomentsCategory(val title: String) {
    FEATURED("精选"),
    FOLLOWING("关注"),
    MINE("我的")
}

/**
 * 动态排序模式（最新、最热）
 */
enum class MomentsSort(val title: String, val paramValue: String) {
    LATEST("最新", "latest"),
    HOT("最热", "hot")
}

/**
 * 动态评论列表数据 DTO
 */
data class MomentCommentsData(
    @SerializedName("list") val list: List<MomentCommentDto> = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("next_before_id") val nextBeforeId: Long? = null
)

/**
 * 发表评论返回数据 (/moment/comment)
 */
data class PostCommentData(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("comment_count") val commentCount: Int = 0,
    @SerializedName("comment") val comment: MomentCommentDto? = null
)

/**
 * 发布动态返回数据 (/moment/create)
 */
data class CreateMomentData(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null
)

/**
 * 动态置顶返回数据 (/moment/pin)
 */
data class SetMomentPinData(
    @SerializedName("moment_id") val momentId: Long = 0,
    @SerializedName("is_pinned") val isPinned: Boolean = false,
    @SerializedName("pinned_at") val pinnedAt: String? = null,
    @SerializedName("message") val message: String? = null
)

/**
 * 动态可见性返回数据 (/moment/visibility)
 */
data class SetMomentVisibilityData(
    @SerializedName("is_private") val isPrivate: Boolean = false,
    @SerializedName("message") val message: String? = null
)

/**
 * 动态受众设置数据 (/api/moment/hide-users)
 */
data class MomentHideUsersData(
    @SerializedName("moment_id") val momentId: Long = 0,
    @SerializedName("audience_mode") val audienceMode: String = "all", // all, include, exclude
    @SerializedName("audience_mutual") val audienceMutual: Boolean = false,
    @SerializedName("hidden_user_count") val hiddenUserCount: Int = 0
)

/**
 * 资料更新返回数据 (/profile/update)
 */
data class UpdateProfileData(
    @SerializedName("login_name") val loginName: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("age_range") val ageRange: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("pat_text") val patText: String? = null,
    @SerializedName("qq") val qq: String? = null
)

/**
 * 头像上传/提交返回数据 (/profile/avatar, /profile/avatar/commit)
 */
data class AvatarData(
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("has_custom") val hasCustom: Boolean = false,
    @SerializedName("has_qq") val hasQq: Boolean = false
)

/**
 * 隐私设置配置数据 (/profile/update_moments_privacy)
 */
data class MomentsPrivacyData(
    @SerializedName("moments_public") val momentsPublic: Int = 1,
    @SerializedName("moments_fishable") val momentsFishable: Int = 1,
    @SerializedName("dm_public") val dmPublic: Int = 1,
    @SerializedName("dm_from_public") val dmFromPublic: Int = 1,
    @SerializedName("dm_from_moment") val dmFromMoment: Int = 1,
    @SerializedName("dm_from_private") val dmFromPrivate: Int = 1,
    @SerializedName("follow_list_public") val followListPublic: Int = 1,
    @SerializedName("show_online_status") val showOnlineStatus: Int = 1
)

/**
 * 查找用户返回的用户对象 (/user/lookup)
 */
data class UserLookupUserDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("uid") val uid: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("login_name") val loginName: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("bio") val bio: String? = null,
    /** MBTI 原始 JSON（对象 / 字符串，未填写为 null），经 [MbtiInfo.fromJson] 容错解析 */
    @SerializedName("mbti") val mbti: JsonElement? = null,
    @SerializedName("age_range_text") val ageRangeText: String? = null,
    @SerializedName("location") val location: String? = null
) {
    /** MBTI 完整信息（服务端下发对象或字符串；缺失/异常形态为 null） */
    val mbtiInfo: MbtiInfo? get() = MbtiInfo.fromJson(mbti)
}

/**
 * 查找用户返回数据 (/user/lookup)
 */
data class UserLookupData(
    @SerializedName("user") val user: UserLookupUserDto? = null,
    @SerializedName("is_self") val isSelf: Boolean = false,
    @SerializedName("can_dm") val canDm: Boolean = true,
    @SerializedName("can_view_moments") val canViewMoments: Boolean = true,
    @SerializedName("dm_disabled_reason") val dmDisabledReason: String? = null,
    @SerializedName("user_id") val fallbackUserId: Long = 0,
    @SerializedName("uid") val fallbackUid: String = "",
    @SerializedName("username") val fallbackUsername: String = "",
    @SerializedName("avatar_url") val fallbackAvatarUrl: String? = null,
    @SerializedName("gender") val fallbackGender: String? = null,
    @SerializedName("bio") val fallbackBio: String? = null,
    /** 根级兜底 MBTI 原始 JSON（对象 / 字符串） */
    @SerializedName("mbti") val fallbackMbti: JsonElement? = null
) {
    val userId: Long get() = user?.id ?: fallbackUserId
    val uid: String get() = user?.uid ?: fallbackUid
    val username: String get() = (user?.username ?: fallbackUsername).ifBlank { user?.loginName ?: "" }
    val loginName: String get() = user?.loginName ?: ""
    val avatarUrl: String? get() = user?.avatarUrl ?: fallbackAvatarUrl
    val gender: String? get() = user?.gender ?: fallbackGender
    val bio: String? get() = user?.bio ?: fallbackBio
    /** MBTI 完整信息：优先 user 对象内，缺失时回落根级字段 */
    val mbtiInfo: MbtiInfo? get() = user?.mbtiInfo ?: MbtiInfo.fromJson(fallbackMbti)

    /** 兼容旧调用点：仅类型代码字符串（无 MBTI 时为 null） */
    val mbti: String? get() = mbtiInfo?.type
}

/**
 * 黑名单用户列表项 (/api/block/list)
 */
data class BlockedUserDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("blocked_id") val blockedId: Long = 0,
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("username") val username: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

data class BlockListData(
    @SerializedName("list") val list: List<BlockedUserDto> = emptyList(),
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("per_page") val perPage: Int = 15
)

/**
 * 举报记录项 (GET /api/report/list)
 *
 * 字段 1:1 对齐官网 `MyReportsView`（`tmp/assets/MyReportsView-DM6V7TiH.js`）实际消费的键名：
 * 类型 / 理由 / 状态 / 处理结果**均由服务端下发 `*_text`**，客户端不再自行映射文案。
 */
data class ReportItemDto(
    @SerializedName("id") val id: Long = 0,
    /** `chat`（举报用户）｜`moment`（举报动态）｜`moment_comment`（举报评论） */
    @SerializedName("type") val type: String = "chat",
    /** 类型文案（服务端下发，如「聊天举报」「动态举报」「评论举报」） */
    @SerializedName("type_text") val typeText: String = "",
    /** 举报理由 */
    @SerializedName("reason") val reason: String = "",
    /** 举报理由文案（服务端下发） */
    @SerializedName("reason_text") val reasonText: String = "",
    /** 被举报者昵称 */
    @SerializedName("reported_username") val reportedUsername: String = "",
    /** `pending` 待处理｜`approved` 已通过｜`rejected` 已驳回｜`processed` 已处理 */
    @SerializedName("status") val status: String = "pending",
    /** 状态文案（服务端下发） */
    @SerializedName("status_text") val statusText: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    /** 聊天举报所属房间名（仅 `chat`） */
    @SerializedName("room_name") val roomName: String? = null,
    @SerializedName("moment_id") val momentId: Long? = null,
    @SerializedName("comment_id") val commentId: Long? = null,
    /** 动态内容预览（仅动态/评论举报且有内容时） */
    @SerializedName("moment_preview") val momentPreview: String? = null,
    /** 补充说明（可为空，空时展示「无补充说明」） */
    @SerializedName("description") val description: String? = null,
    /** 处理结果文案（为空时展示「平台正在处理中，请耐心等待」） */
    @SerializedName("result_text") val resultText: String? = null,
    /** 处理时间（仅非 `pending` 时展示） */
    @SerializedName("reviewed_at") val reviewedAt: String? = null
) {
    /** 记录标题（对齐官网：按类型区分前缀） */
    val headline: String
        get() = when (type) {
            "moment" -> "举报动态 · $reportedUsername"
            "moment_comment" -> "举报评论 · $reportedUsername"
            else -> "举报用户：$reportedUsername"
        }

    /** 类型标签文案（服务端未下发时按类型兜底） */
    val typeLabel: String
        get() = typeText.ifBlank {
            when (type) {
                "moment" -> "动态举报"
                "moment_comment" -> "评论举报"
                else -> "聊天举报"
            }
        }

    /** 状态标签文案（服务端未下发时按状态兜底） */
    val statusLabel: String
        get() = statusText.ifBlank {
            when (status) {
                "approved" -> "已通过"
                "rejected" -> "已驳回"
                "processed" -> "已处理"
                else -> "待处理"
            }
        }
}

/**
 * 举报列表响应 (GET /api/report/list)
 */
data class ReportListData(
    @SerializedName("list") val list: List<ReportItemDto> = emptyList(),
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("per_page") val perPage: Int = 10,
    @SerializedName("has_more") val hasMore: Boolean = false
)

// ============================================================
// 以下为本轮新增接口模型（他人主页 / 房间成员 / 我的页面功能）
// 说明：服务端对布尔字段存在 true/false 与 1/0 混用，可空布尔交给
// FlexibleBooleanTypeAdapterFactory 宽容解析；文本字段可能显式下发 null，
// 由 NullTextCoercingTypeAdapterFactory 在解析后归一化为空串。
// ============================================================

/**
 * 用户主页资料 DTO
 */
data class UserProfileDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("uid") val uid: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("gender_text") val genderText: String? = null,
    @SerializedName("age_range") val ageRange: String? = null,
    @SerializedName("age_range_text") val ageRangeText: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("avatar_fallback") val avatarFallback: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("dm_banned") val dmBanned: Boolean = false,
    /** 官方语义：仅在显式下发 false 时视为不可接收私信 */
    @SerializedName("can_receive_dm") val canReceiveDm: Boolean = true,
    // 计数与列表可见权限实际下挂在 user 对象内（官方 `s.value = t.user`），可空以便根级兜底
    @SerializedName("following_count") val followingCount: Int? = null,
    @SerializedName("follower_count") val followerCount: Int? = null,
    @SerializedName("like_count") val likeCount: Int? = null,
    @SerializedName("mutual_count") val mutualCount: Int? = null,
    @SerializedName("can_view_following") val canViewFollowing: Boolean? = null,
    @SerializedName("can_view_followers") val canViewFollowers: Boolean? = null,
    /** 对方清流模式开关状态（官方 PeerProfileCard 显示「对方已开启/关闭清流模式」） */
    @SerializedName(value = "clean_stream_mode", alternate = ["clean_stream", "cleanStreamMode", "cleanStream", "clean_stream_enabled"]) val cleanStreamMode: Boolean? = null,
    /** 对方是否在线 */
    @SerializedName(value = "is_online", alternate = ["online", "online_status", "isOnline", "onlineStatus"]) val isOnline: Boolean? = null,
    /** 对方是否开启在线状态展示（为 0/false 时即隐身模式） */
    @SerializedName(value = "show_online_status", alternate = ["showOnlineStatus", "online_status_visible"]) val showOnlineStatus: Boolean? = null,
    /** 对方是否开启隐私模式（为真时官方隐藏「动态」入口） */
    @SerializedName("privacy_mode") val privacyMode: Boolean? = null,
    /** 用户称号（官方 UserMomentsView 身份行 `user.title`）；未下发为 null */
    @SerializedName("title") val title: String? = null,
    /** 用户称号颜色 key（官方 `user.title_color`） */
    @SerializedName("title_color") val titleColor: String? = null,
    /** MBTI 原始 JSON（对象 / 字符串，未填写为 null），经 [MbtiInfo.fromJson] 容错解析 */
    @SerializedName("mbti") val mbti: JsonElement? = null
) {
    /** MBTI 完整信息（服务端下发对象或字符串；缺失/异常形态为 null） */
    val mbtiInfo: MbtiInfo? get() = MbtiInfo.fromJson(mbti)
}

/**
 * 他人主页聚合接口返回 (GET /api/moment/user)
 */
data class UserProfileAggregateData(
    @SerializedName("user") val user: UserProfileDto? = null,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("follower_count") val followerCount: Int = 0,
    @SerializedName("like_count") val likeCount: Int = 0,
    @SerializedName("mutual_count") val mutualCount: Int = 0,
    /** 官方语义：字段缺失按「允许」处理（`x !== false`） */
    @SerializedName("can_view_following") val canViewFollowing: Boolean = true,
    @SerializedName("can_view_followers") val canViewFollowers: Boolean = true,
    @SerializedName("is_self") val isSelf: Boolean = false,
    @SerializedName("i_blocked") val iBlocked: Boolean = false,
    @SerializedName("is_following") val isFollowing: Boolean = false,
    @SerializedName("followed_by") val followedBy: Boolean = false,
    @SerializedName("can_dm") val canDm: Boolean = true,
    @SerializedName("dm_disabled_reason") val dmDisabledReason: String? = null,
    /** 动态可见性：实测下发 BOOLEAN，官方未消费该字段 */
    @SerializedName("moments_public") val momentsPublic: Boolean? = null,
    /** 对方是否拉黑了我 */
    @SerializedName("blocked") val blocked: Boolean? = null,
    @SerializedName("blocked_message") val blockedMessage: String? = null,
    @SerializedName("list") val list: List<MomentItemDto>? = null,
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("next_before_id") val nextBeforeId: Long? = null,
    @SerializedName("next_before_score") val nextBeforeScore: Double? = null
)

/**
 * 关注/粉丝列表项 (GET /api/follow/list)
 */
data class FollowListItemDto(
    @SerializedName("uid") val uid: String = "",
    @SerializedName("follow_id") val followId: Long = 0,
    /** 部分返回会带上数字 user_id，缺失时以 uid 作为操作标识 */
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName("username") val username: String = "",
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("avatar_fallback") val avatarFallback: String? = null,
    @SerializedName("is_following") val isFollowing: Boolean = false,
    @SerializedName("is_follower") val isFollower: Boolean = false,
    @SerializedName("is_mutual") val isMutual: Boolean = false,
    @SerializedName("i_blocked") val iBlocked: Boolean = false,
    @SerializedName("is_self") val isSelf: Boolean = false
)

/**
 * 关注/粉丝列表返回 (GET /api/follow/list)
 */
data class FollowListData(
    @SerializedName("allowed") val allowed: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("is_self") val isSelf: Boolean = false,
    @SerializedName("user") val user: UserProfileDto? = null,
    @SerializedName("list") val list: List<FollowListItemDto>? = null,
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("next_before_id") val nextBeforeId: Long? = null
)

/**
 * 房间成员项 DTO (GET /api/room/members)
 *
 * 服务端可能下发 uid 字符串或数字 user_id，成员操作统一走 [targetId]。
 */
data class RoomMemberDto(
    @SerializedName("uid") val uid: String = "",
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName(value = "username", alternate = ["nickname", "name"]) val username: String = "",
    @SerializedName(value = "avatar_url", alternate = ["avatar"]) val avatarUrl: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName(value = "is_creator", alternate = ["is_owner"]) val isCreator: Boolean = false,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    /** 该成员（管理员）是否被授予踢人权限，由 /room/set-member-admin 设置 */
    @SerializedName("can_kick") val canKick: Boolean = false,
    /** 该成员（管理员）是否被授予删除消息权限 */
    @SerializedName("can_delete_message") val canDeleteMessage: Boolean = false,
    @SerializedName(value = "is_self", alternate = ["is_me"]) val isSelf: Boolean = false,
    @SerializedName("is_muted") val isMuted: Boolean = false,
    @SerializedName("joined_at") val joinedAt: String? = null,
    /** 成员称号（官方成员列表 `member.title`） */
    @SerializedName("title") val title: String? = null,
    /** 成员称号颜色 key（官方 `member.title_color`） */
    @SerializedName("title_color") val titleColor: String? = null
) {
    /** 成员操作目标：优先数字 user_id，缺失时回落到 uid */
    val targetId: String get() = if (userId > 0L) userId.toString() else uid
}

/**
 * 房间成员分页返回 DTO (GET /api/room/members)
 */
data class RoomMembersData(
    @SerializedName(value = "list", alternate = ["members", "items", "users"]) val members: List<RoomMemberDto> = emptyList(),
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("per_page") val perPage: Int = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
)

/**
 * 禁止加入名单项 DTO (GET /api/room/join-bans)
 */
data class RoomJoinBanDto(
    @SerializedName("uid") val uid: String = "",
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName(value = "username", alternate = ["nickname", "name"]) val username: String = "",
    @SerializedName(value = "avatar_url", alternate = ["avatar"]) val avatarUrl: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName(value = "banned_at", alternate = ["created_at"]) val bannedAt: String? = null
) {
    /** 解禁操作目标：优先数字 user_id，缺失时回落到 uid */
    val targetId: String get() = if (userId > 0L) userId.toString() else uid
}

/**
 * 禁止加入名单返回 DTO (GET /api/room/join-bans)
 */
data class RoomJoinBansData(
    @SerializedName(value = "list", alternate = ["bans", "items", "users"]) val bans: List<RoomJoinBanDto> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

/**
 * 聊天室成员项 DTO（bootstrap.members[]）
 */
data class BootstrapMemberDto(
    @SerializedName(value = "user_id", alternate = ["id"]) val userId: Long = 0,
    @SerializedName("uid") val uid: String = "",
    @SerializedName(value = "username", alternate = ["nickname", "name"]) val username: String = "",
    @SerializedName(value = "avatar_url", alternate = ["avatar"]) val avatarUrl: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("is_creator") val isCreator: Boolean = false,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("can_kick") val canKick: Boolean = false,
    @SerializedName("can_delete_message") val canDeleteMessage: Boolean = false,
    /** 成员称号（官方成员列表 `member.title`） */
    @SerializedName("title") val title: String? = null,
    /** 成员称号颜色 key（官方 `member.title_color`） */
    @SerializedName("title_color") val titleColor: String? = null
)

/**
 * 未读通知数 (GET /api/notifications/unread 之类)
 */
data class NotificationUnreadData(
    @SerializedName("unread") val unread: Int = 0
)

/**
 * 安全中心 · 登录设备项 (GET /api/security/devices)
 *
 * `last_seen_at` 官方按 Unix 秒处理（`new Date(v * 1000)`），这里用 String 兼容时间字符串。
 */
data class SecurityDeviceDto(
    @SerializedName("device_id") val deviceId: String = "",
    @SerializedName("device_name") val deviceName: String = "",
    @SerializedName("platform") val platform: String = "",
    @SerializedName("browser") val browser: String = "",
    @SerializedName("is_current") val isCurrent: Boolean = false,
    @SerializedName("ip") val ip: String = "",
    @SerializedName("location") val location: String = "",
    @SerializedName("last_seen_at") val lastSeenAt: String = ""
)

data class SecurityDevicesData(
    @SerializedName(value = "devices", alternate = ["list", "items"]) val devices: List<SecurityDeviceDto>? = null
)

/**
 * 安全中心 · 注销单设备返回 (POST /api/security/devices/revoke)
 *
 * `is_current` 为真表示注销的是本机，需要立刻断开连接并退出登录。
 */
data class SecurityRevokeData(
    @SerializedName("is_current") val isCurrent: Boolean = false
)

/**
 * 安全中心 · 登录/安全事件项 (GET /api/security/login-history)
 */
data class SecurityLoginLogDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("event_type") val eventType: String = "",
    @SerializedName("event_type_text") val eventTypeText: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("ip") val ip: String = "",
    @SerializedName("location") val location: String = "",
    @SerializedName("is_anomaly") val isAnomaly: Boolean = false
)

/**
 * 安全中心 · 登录历史分页 (GET /api/security/login-history)
 *
 * 官方语义：`table_ready !== false`（缺省视为就绪），`total` 用于判断是否还有下一页。
 */
data class SecurityLoginHistoryData(
    @SerializedName("table_ready") val tableReady: Boolean = true,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName(value = "list", alternate = ["items", "logs"]) val list: List<SecurityLoginLogDto>? = null
)

/**
 * 处罚减免 · 可申请的处罚项 (GET /api/penalty-appeal/bootstrap → active_penalties[])
 */
data class PenaltyOptionDto(
    @SerializedName("key") val key: String = "",
    @SerializedName("label") val label: String = ""
)

/**
 * 处罚减免 · 我的申请（同上接口的 `appeal` 字段，可能为 null）
 *
 * `evidence_map` 为旧版结构，其值可能是字符串或字符串数组。
 */
data class PenaltyAppealDto(
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("penalty_labels") val penaltyLabels: List<String>? = null,
    @SerializedName("required_word_count") val requiredWordCount: Int = 0,
    @SerializedName("admin_reply") val adminReply: String? = null,
    @SerializedName("reviewed_at") val reviewedAt: String? = null,
    @SerializedName("evidence_urls") val evidenceUrls: List<String>? = null,
    @SerializedName("evidence_map") val evidenceMap: Map<String, com.google.gson.JsonElement>? = null
)

/**
 * 处罚减免 · 页面初始化 (GET /api/penalty-appeal/bootstrap)
 */
data class PenaltyAppealBootstrapData(
    @SerializedName("active_penalties") val activePenalties: List<PenaltyOptionDto>? = null,
    @SerializedName("guidelines") val guidelines: List<String>? = null,
    @SerializedName(value = "can_apply", alternate = ["canApply"]) val canApply: Boolean = false,
    @SerializedName("appeal") val appeal: PenaltyAppealDto? = null,
    /** can_apply 为 false 时服务端给出的原因 */
    @SerializedName("reason") val reason: String? = null
)

/**
 * 网易云绑定 · 绑定状态 (GET /api/music/netease/binding)
 *
 * `status`：active(有效) / invalid(已失效) / none(未绑定)。
 */
data class NeteaseBindingData(
    @SerializedName("bound") val bound: Boolean = false,
    @SerializedName("status") val status: String = "",
    @SerializedName("netease_uid") val neteaseUid: Long = 0,
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("avatar_url") val avatarUrl: String = "",
    @SerializedName("vip_type") val vipType: Int = 0,
    @SerializedName("vip_hint") val vipHint: String = "",
    @SerializedName("disclaimer") val disclaimer: String = "",
    @SerializedName("table_ready") val tableReady: Boolean = true
)

/**
 * 网易云绑定 · 扫码会话 (POST /api/music/netease/binding/qrcode/start)
 */
data class NeteaseQrStartData(
    @SerializedName("session_id") val sessionId: String = "",
    @SerializedName("qr_image") val qrImage: String = "",
    @SerializedName("qr_content") val qrContent: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("status") val status: String = ""
)

/**
 * 网易云绑定 · 扫码状态 (GET /api/music/netease/binding/qrcode/status)
 *
 * `status`：waiting / scanned / success / expired / failed；成功后 `binding` 带回新的绑定信息。
 */
data class NeteaseQrStatusData(
    @SerializedName("status") val status: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("binding") val binding: NeteaseBindingData? = null
)

// ============================================================
//  动态音乐（网易云）· /api/music/netease/*
// ============================================================

/**
 * 网易云歌曲统一结构
 *
 * `/api/music/netease/resolve`、`/search`、`/playlist/resolve` 均返回该结构，
 * 客户端包装成动态的 `music` 对象（provider/song_id/name/artists/album/cover_url）。
 */
data class NeteaseSongDto(
    @SerializedName("song_id") val songId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("artists") val artists: String = "",
    @SerializedName("album") val album: String = "",
    @SerializedName("cover_url") val coverUrl: String = ""
) {
    /** 是否为有效歌曲（缺 song_id 的记录无法播放，直接丢弃） */
    val isValid: Boolean get() = songId.isNotBlank()
}

/**
 * 网易云链接 / 歌曲 ID 解析返回 (POST /api/music/netease/resolve)
 *
 * 兼容两种下发形态：直接返回歌曲对象，或包一层 `song`。
 */
data class NeteaseResolveData(
    @SerializedName("song") val song: NeteaseSongDto? = null,
    @SerializedName("song_id") val songId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("artists") val artists: String = "",
    @SerializedName("album") val album: String = "",
    @SerializedName("cover_url") val coverUrl: String = ""
) {
    /** 归一化为歌曲对象（两种形态都为空时返回 null） */
    fun toSong(): NeteaseSongDto? {
        song?.takeIf { it.isValid }?.let { return it }
        return NeteaseSongDto(songId, name, artists, album, coverUrl).takeIf { it.isValid }
    }
}

/**
 * 网易云搜索返回 (POST /api/music/netease/search)
 *
 * `keyword`、`offset`、`limit`（默认 20）；列表字段兼容 `list` / `songs`。
 */
data class NeteaseSearchData(
    @SerializedName("list") val list: List<NeteaseSongDto>? = null,
    @SerializedName("songs") val songs: List<NeteaseSongDto>? = null,
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("total") val total: Int = 0
) {
    val items: List<NeteaseSongDto> get() = (list ?: songs).orEmpty().filter { it.isValid }
}

/**
 * 网易云歌单解析返回 (POST /api/music/netease/playlist/resolve)
 */
data class NeteasePlaylistData(
    @SerializedName("playlist_id") val playlistId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("cover_url") val coverUrl: String = "",
    @SerializedName("track_count") val trackCount: Int = 0,
    @SerializedName("list") val list: List<NeteaseSongDto>? = null,
    @SerializedName("songs") val songs: List<NeteaseSongDto>? = null
) {
    val items: List<NeteaseSongDto> get() = (list ?: songs).orEmpty().filter { it.isValid }
}

/**
 * 网易云播放地址 (POST /api/music/netease/play-url)
 *
 * body：`song_id`、`force`（0 用缓存 / 1 强制刷新）。
 */
data class NeteasePlayUrlData(
    @SerializedName("url") val url: String = "",
    @SerializedName("song_id") val songId: String = "",
    @SerializedName("expires_in") val expiresIn: Long = 0,
    @SerializedName("reason") val reason: String? = null
)

/**
 * 房间共享音乐条目模型
 */
data class RoomMusicPlaylistItem(
    @SerializedName("song_id") val songId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("artists") val artists: String = "",
    @SerializedName("album") val album: String = "",
    @SerializedName("cover_url") val coverUrl: String = "",
    @SerializedName("added_by") val addedBy: String? = null,
    @SerializedName("added_at") val addedAt: Long = 0L
)

/**
 * 房间共享音乐歌单列表响应模型（乐观锁版本号机制）
 */
data class RoomMusicPlaylistResponse(
    @SerializedName("version") val version: Int = 0,
    @SerializedName("play_mode") val playMode: String = "loop-all",
    @SerializedName("current_song_id") val currentSongId: String? = null,
    @SerializedName("items") val items: List<RoomMusicPlaylistItem>? = null,
    @SerializedName("playlist") val playlist: List<RoomMusicPlaylistItem>? = null
) {
    val allSongs: List<RoomMusicPlaylistItem>
        get() = (items ?: playlist).orEmpty()
}

/**
 * 联系我们 · 站点联系配置 (GET /api/contact/bootstrap → `contact_config`)
 *
 * 真机实测（2026-10-09）：`contact_email` 可能为空串、`group_qrcode` 为 OSS 直链；
 * 各字段均需调用方自行判空后再展示。
 */
data class ContactConfigDto(
    @SerializedName("group_qrcode") val groupQrcode: String? = null,
    @SerializedName("group_name") val groupName: String? = null,
    @SerializedName("contact_email") val contactEmail: String? = null,
    @SerializedName("contact_qq") val contactQq: String? = null,
    @SerializedName("contact_wechat") val contactWechat: String? = null,
    /** 问题反馈页的提交区说明文案 */
    @SerializedName("suggestion_intro") val suggestionIntro: String? = null
)

/** 联系我们 · 额外联系方式条目 (GET /api/contact/bootstrap → `other_contact[]`) */
data class OtherContactDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("value") val value: String = ""
)

/**
 * 问题反馈 · 我的反馈记录 (GET /api/contact/bootstrap → `user_feedbacks[]`)
 */
data class FeedbackRecordDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("type") val type: String = "suggestion",
    @SerializedName("title") val title: String = "",
    @SerializedName("content") val content: String = "",
    @SerializedName("status") val status: String = "pending",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("admin_reply") val adminReply: String? = null,
    @SerializedName("replied_at") val repliedAt: String? = null
) {
    /** 状态文案（1:1 对齐官网 FeedbackView 的映射表） */
    val statusLabel: String
        get() = when (status) {
            "pending" -> "待处理"
            "processing" -> "处理中"
            "resolved" -> "已解决"
            "closed" -> "已关闭"
            else -> "待处理"
        }

    /** 反馈类型文案（1:1 对齐官网 FeedbackView 的映射表） */
    val typeLabel: String
        get() = when (type) {
            "suggestion" -> "功能建议"
            "bug" -> "问题反馈"
            "complaint" -> "投诉建议"
            else -> "其他"
        }
}

/** 联系我们 / 问题反馈页初始化数据 (GET /api/contact/bootstrap) */
data class ContactBootstrapData(
    @SerializedName("contact_config") val contactConfig: ContactConfigDto? = null,
    @SerializedName("other_contact") val otherContact: List<OtherContactDto>? = null,
    @SerializedName("user_feedbacks") val userFeedbacks: List<FeedbackRecordDto>? = null
)

/**
 * 捐赠 · 捐赠者条目 (GET /api/donate → `donors[]`)
 *
 * 注意：`amount` 服务端下发的是**字符串**（实测 `"8.80"`、`"420"`）；
 * 匿名捐赠者 `uid` 为空串，此时不可点头像、不可私信。
 */
data class DonorDto(
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("amount") val amount: String = "",
    @SerializedName("message") val message: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("avatar_fallback") val avatarFallback: String? = null,
    @SerializedName("uid") val uid: String = "",
    @SerializedName("user_id") val userId: Long = 0,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("dm_banned") val dmBanned: Boolean = false,
    @SerializedName("can_receive_dm") val canReceiveDm: Boolean = true
) {
    /** 是否具备可识别的用户身份（匿名捐赠者 uid 为空） */
    val hasIdentity: Boolean get() = uid.isNotBlank() || userId > 0

    /** 是否可发起私信：沿用官方的 `!dm_banned && can_receive_dm` 判定 */
    val dmAllowed: Boolean get() = hasIdentity && !dmBanned && canReceiveDm

    /** 首字兜底（对齐官网 `avatar_fallback` → 昵称首字） */
    val fallbackLetter: String
        get() = avatarFallback?.trim()?.takeIf { it.isNotEmpty() }
            ?: displayName.trim().takeIf { it.isNotEmpty() }?.first()?.toString()
            ?: "善"
}

/** 捐赠页数据 (GET /api/donate) */
data class DonateData(
    @SerializedName("intro") val intro: String = "",
    @SerializedName("wechat_qrcode") val wechatQrcode: String? = null,
    @SerializedName("alipay_qrcode") val alipayQrcode: String? = null,
    @SerializedName("donors") val donors: List<DonorDto>? = null
)

/** 我的分享码 (GET /api/user/share)：`login_name#零填充 user_id` */
data class UserShareCodeData(
    @SerializedName("share_code") val shareCode: String = "",
    @SerializedName("login_name") val loginName: String = "",
    @SerializedName("id") val id: Long = 0
)

