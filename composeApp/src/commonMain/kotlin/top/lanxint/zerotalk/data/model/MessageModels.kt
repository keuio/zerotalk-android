package top.lanxint.zerotalk.data.model

import androidx.compose.ui.graphics.Color

/**
 * 消息分类标签枚举
 */
enum class MessageCategory(val title: String) {
    ALL("全部"),
    CODE("暗号"),
    MATCH("匹配"),
    PRIVATE("私聊")
}

/**
 * iOS 联系人头像标准灰紫渐变背景规范
 * - 主底色：偏深的灰紫 / 雾霾紫 (0xFF534B66)
 * - 上方渐变：更亮的浅紫、淡蓝紫高光 (0xFF8C83A4)
 * - 下方渐变：更深的靛蓝、灰蓝紫阴影 (0xFF221E34)
 * - 整体质感：低饱和、偏暗、柔和磨砂感，复刻 iOS 联系人头像原型
 */
val IosContactAvatarGradient = listOf(
    Color(0xFF8C83A4), // 上方渐变：浅紫、淡蓝紫高光
    Color(0xFF534B66), // 主底色：偏深的灰紫 / 雾霾紫
    Color(0xFF221E34)  // 下方渐变：更深的靛蓝、灰蓝紫阴影
)

/**
 * 消息会话项模型 (全面对标 iOS iMessage 列表项规范)
 */
data class ConversationItem(
    val id: String,
    val targetName: String,
    val targetAvatar: String? = null,
    val avatarGradient: List<Color> = IosContactAvatarGradient,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val category: MessageCategory,
    val isOnline: Boolean? = null,
    val tag: String? = null,
    val isVoice: Boolean = false,
    val voiceDurationSec: Int = 0,
    val wallpaperKey: String? = null,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val targetUserId: String = "",
    val targetUid: String = "",
    val targetLoginName: String = "",
    /** 对端备注名（官网 `peer.remark`，为空表示未设置）；显示名优先取它 */
    val targetRemark: String = "",
    /**
     * 该房间是否由当前账号创建（来源 `/room/list` 的 `is_creator`）
     *
     * 用于长按菜单：私聊双方均可「删除房间」，而暗号房/群聊仅创建者可「删除房间」，
     * 非创建者只提供「退出房间」（对齐官网 ChatView 的 canLeave / canDelete 判定）。
     */
    val isRoomCreator: Boolean = false
)

/**
 * 解析会话项的场景标签 (Tag)
 *
 * 规则：
 * 1. 暗号房间 / 群聊（[MessageCategory.CODE]）：展示 "暗号"
 * 2. 1v1 会话（私聊 [MessageCategory.PRIVATE] / 匹配 [MessageCategory.MATCH]）：
 *    - 名字右侧不需要显示在线/清流标识（状态由头像右下角圆点表达），返回 null 优雅隐去
 */
fun resolveConversationStatusTag(
    category: MessageCategory,
    isOnline: Boolean? = null,
    cleanStreamMode: Boolean? = null
): String? {
    if (category == MessageCategory.CODE) {
        return "暗号"
    }
    return null
}

/**
 * 非真实用户标识的占位值
 *
 * WebSocket 消息事件缺少 `from_uid` 时发送者会退化成 `peer`，本地乐观回显用 `me`，
 * 系统消息用 `system`，未知数字 id 为 `0`。这些值不能用于请求用户资料等接口。
 */
val REAL_USER_ID_PLACEHOLDERS = setOf("peer", "me", "system", "0")

/**
 * 单条聊天消息模型
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val content: String,
    val timestamp: String,
    val isMine: Boolean,
    val isVoice: Boolean = false,
    val voiceDurationSec: Int = 0,
    /**
     * 语音通话记录（服务端 `type:"voice"`）
     *
     * 与 [isVoice]（语音消息，`type:"audio"`）不同：官网 `voiceCallDisplay` 把通话记录
     * 按 `content` JSON 的 `text` 字段展示（如「语音通话 03:20」「语音通话失败」），
     * 并居中弱化渲染，不参与气泡分组。
     */
    val isVoiceCall: Boolean = false,
    /** 语音通话记录展示文案（已从 `content` 的 `text` 解析） */
    val voiceCallText: String = "",
    val isSystem: Boolean = false,
    val quotedText: String? = null,
    val quotedIsMine: Boolean? = null,
    val quotedSenderName: String? = null,
    val isDice: Boolean = false,
    val diceValue: Int = 0,
    val isImage: Boolean = false,
    val imageResKey: String? = null,
    val isMusic: Boolean = false,
    val musicData: MomentMusic? = null,
    val isGame: Boolean = false,
    val gameInvite: ChatGameInvite? = null,
    val timestampMs: Long = 0L,
    /**
     * 消息在本机「到达」的时刻（实时推送写入，历史消息为 0）。
     *
     * 骰子掷动动画的新鲜度判定用它而不是 [timestampMs]：服务端 `created_at` 是
     * 服务端本地时间（GMT+8），设备时区不一致时（实测模拟器为 GMT）两者相差数小时，
     * 用服务端时间判「是否刚到达」会永远判否。
     */
    val arrivedAtMs: Long = 0L,
    val customTimeHeader: String? = null,
    /** 服务端消息自增 id（历史分页游标，本地即时回显消息为 0） */
    val serverId: Long = 0L,
    /** 图片消息的真实图片地址 */
    val imageUrl: String = "",
    /** 语音消息的真实音频地址（WS `type:"audio"` 的 content） */
    val audioUrl: String = "",
    /** 语音来源：record（录音）/ file（语音文件），对应 WS `audio_source` */
    val audioSource: String = "",
    /** 拍一拍消息 */
    val isPat: Boolean = false,
    /** 拍一拍展示文案（已格式化，如「张三 拍了拍 李四的小脑袋」） */
    val patText: String = "",
    /** 发送方昵称（暗号房 / 公共大厅用于展示发送者信息） */
    val senderName: String = "",
    /** 发送方头像 */
    val senderAvatar: String = "",
    /** 发送方性别：male / female / unknown */
    val senderGender: String = "",
    /** 会话摘要文案（图片 / 拍一拍等非文本消息用于列表展示） */
    val previewText: String = ""
)

/**
 * 引用回复时展示的「被引用消息」摘要。
 *
 * 骰子按官网口径展示为 `[骰子 N]`；拍一拍直接用已归因文案，
 * 避免把服务端 JSON（`{"a":…,"t":…}`）原样贴进引用条。
 */
fun ChatMessage.quotePreviewText(): String = when {
    isVoiceCall -> voiceCallText.ifBlank { "[语音通话]" }
    isVoice -> "[语音 ${voiceDurationSec}\"]"
    isImage -> "[图片]"
    isDice -> if (diceValue in 1..6) "[骰子 $diceValue]" else "[骰子]"
    isPat -> patText.ifBlank { "拍了拍" }
    isMusic || isGame -> previewText
    else -> content
}

/**
 * 新消息通知（用于全局弹窗展示），由 WebSocket 实时推送触发。
 *
 * @param roomName 房间显示名称
 * @param senderName 发送者昵称（仅 DM 场景需要显示，群聊置空）
 * @param content 消息正文
 * @param isGroup 是否为群聊（群聊不显示发送者昵称）
 */
data class IncomingMessageNotification(
    val roomName: String,
    val senderName: String = "",
    val content: String,
    val isGroup: Boolean = false,
    /** 会话房间 id：系统通知点击后直达该会话（大厅等场景可为空） */
    val roomId: String = ""
)


