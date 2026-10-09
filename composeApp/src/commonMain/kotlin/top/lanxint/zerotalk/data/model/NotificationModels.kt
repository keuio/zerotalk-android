package top.lanxint.zerotalk.data.model

import com.google.gson.annotations.SerializedName

/**
 * 通知中心 · 单条通知 (GET /api/notifications)
 *
 * 字段与官方 `NotificationsView` 完全对齐：
 * - `is_read`：服务端可能下发 true/false、1/0（全站 Gson 已挂宽容布尔适配器）；
 * - `label` / `level`：官方用于卡片标签与配色（level 默认 info）；
 * - `ref_type` / `ref_id` / `payload`：官方据此决定点击后跳转的目标页（本客户端仅用于展示与已读）。
 */
data class NotificationItemDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("type") val type: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("label") val label: String = "",
    @SerializedName("level") val level: String = "info",
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("ref_type") val refType: String = "",
    @SerializedName("ref_id") val refId: Long? = null,
    @SerializedName("actor_uid") val actorUid: String? = null,
    @SerializedName("actor_id") val actorId: Long? = null,
    @SerializedName("payload") val payload: NotificationPayloadDto? = null
)

/**
 * 通知附带的业务载荷（官方 `item.payload`）
 */
data class NotificationPayloadDto(
    @SerializedName("moment_id") val momentId: Long? = null,
    @SerializedName("comment_id") val commentId: Long? = null,
    @SerializedName("room_id") val roomId: String? = null,
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("user_id") val userId: Long? = null,
    @SerializedName("follower_id") val followerId: Long? = null
)

/**
 * 通知列表响应 (GET /api/notifications)
 *
 * 官方一次返回 `{ list, has_more, next_before_id, unread }`，其中 `unread` 会顺带回写实时未读数。
 */
data class NotificationListData(
    @SerializedName("list") val list: List<NotificationItemDto> = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean = false,
    @SerializedName("next_before_id") val nextBeforeId: Long? = null,
    @SerializedName("unread") val unread: Int = 0
)

/**
 * 通知分类 Tab（官方 NotificationsView 的四个 key 与文案）
 */
enum class NotificationCategory(val key: String, val label: String) {
    ALL("all", "全部"),
    INTERACTION("interaction", "互动"),
    ACCOUNT("account", "账号"),
    SYSTEM("system", "系统")
}
