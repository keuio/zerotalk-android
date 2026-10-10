package top.lanxint.zerotalk.data.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * 解析服务端 `message_recalled` 事件（官网 `handleMessageRecall` 的入参）。
 *
 * 官方聊天页 `case "message_recalled": p.handleMessageRecall(d, p.addSystemMessage)`
 * 只消费以下字段，本函数逐一对齐（并兼容 `root` / `root.data` 两级，与 `message` 事件一致）：
 * - `message_id`（兼容 `id`）——缺失或非法时整个事件丢弃（官网 `if (!X) return`）；
 * - `room_id`；
 * - `username`；
 * - `by_moderator` / `by_room_admin`。
 *
 * 抽成独立纯函数是为了让「字段名与容错」可被单元测试覆盖，避免像早期 `message` 事件
 * 那样把 `id` 误当 `message_id`（或把非数字值解析成 0 后静默丢事件）。
 */
internal fun parseMessageRecalled(root: JsonObject): WsServerEvent.MessageRecalled? {
    // 与 game / invite 等事件一致：data 只在确实是对象时才下钻，避免脏载荷直接抛异常丢帧
    val data = root.get("data")?.takeIf { it.isJsonObject }?.asJsonObject

    fun raw(key: String) =
        root.get(key)?.takeIf { !it.isJsonNull } ?: data?.get(key)?.takeIf { !it.isJsonNull }

    fun str(key: String): String? = try {
        raw(key)?.asString
    } catch (_: Exception) {
        null
    }

    fun long(key: String): Long? = try {
        raw(key)?.asLong
    } catch (_: Exception) {
        null
    }

    fun bool(key: String): Boolean = try {
        raw(key)?.asBoolean ?: false
    } catch (_: Exception) {
        false
    }

    val messageId = long("message_id") ?: long("id") ?: 0L
    if (messageId <= 0L) return null

    return WsServerEvent.MessageRecalled(
        roomId = str("room_id")?.takeIf { it.isNotBlank() },
        messageId = messageId,
        username = str("username")?.takeIf { it.isNotBlank() },
        byModerator = bool("by_moderator"),
        byRoomAdmin = bool("by_room_admin")
    )
}

/**
 * 解析服务端 `message` 事件（官网 `ws.on("message", …)` 的入参）。
 *
 * 字段与 [ZeroTalkWebSocketClient] 内的历史实现完全一致，抽成独立纯函数是为了让
 * 「字段名与容错」可被单元测试覆盖（尤其是 `is_deleted` 撤回标记的透传，
 * 此前漏掉该字段导致重进会话后已撤回消息被当成普通消息渲染）。
 *
 * - 兼容 `root` / `root.data` 两级载荷；
 * - 兼容 `uid` / `from_uid`、`id` / `message_id` 两组字段名；
 * - `is_deleted` 归一化为 `Boolean`（缺省 / 非法值一律 false，对齐官网 `!!e.is_deleted`）。
 */
internal fun parseMessageEvent(root: JsonObject): WsServerEvent.Message {
    // data 只在确实是对象时才下钻，避免脏载荷直接抛异常丢帧
    val data = root.get("data")?.takeIf { it.isJsonObject }?.asJsonObject

    fun raw(key: String) =
        root.get(key)?.takeIf { !it.isJsonNull } ?: data?.get(key)?.takeIf { !it.isJsonNull }

    fun str(key: String): String? = try {
        raw(key)?.asString
    } catch (_: Exception) {
        null
    }

    fun long(key: String): Long? = try {
        raw(key)?.asLong
    } catch (_: Exception) {
        null
    }

    fun bool(key: String): Boolean? = try {
        raw(key)?.asBoolean
    } catch (_: Exception) {
        null
    }

    return WsServerEvent.Message(
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
        replyToUsername = str("reply_username"),
        // 官网：is_deleted: !!e.is_deleted
        isDeleted = bool("is_deleted") ?: false,
        // 端到端加密参数（官方消息行 enc：{v,alg,iv,kid,sid}）
        enc = parseMessageEnc(raw("enc")),
        clientMessageId = str("client_message_id"),
        // 发送者称号（官方消息行 title / title_color）
        title = str("title"),
        titleColor = str("title_color")
    )
}

/**
 * 解析消息事件里的端到端加密参数（官方 `enc`：`{v,alg,iv,kid,sid}`）。
 *
 * 非对象 / 脏字段一律返回 null，交由映射层按普通消息处理；
 * 解析抽到顶层是为了让 `message` 事件解析保持单一返回点，便于单测。
 */
internal fun parseMessageEnc(element: JsonElement?): MessageEncryptionDto? =
    element?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
        runCatching {
            MessageEncryptionDto(
                version = obj.get("v")?.takeIf { it.isJsonPrimitive }?.asInt ?: 1,
                algorithm = obj.get("alg")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                iv = obj.get("iv")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                keyId = obj.get("kid")?.takeIf { it.isJsonPrimitive }?.asString,
                senderUid = obj.get("sid")?.takeIf { it.isJsonPrimitive }?.asString
            )
        }.getOrNull()
    }
