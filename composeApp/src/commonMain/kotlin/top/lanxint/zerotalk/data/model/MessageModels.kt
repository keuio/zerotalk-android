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
 * 消息类型：动态分享卡片
 *
 * 官方发送帧为 `{"event":"message","type":"moment_share","content":"<卡片 JSON>"}`，
 * 接收端 `JSON.parse(content)` 后渲染动态卡片，而不是把正文当普通文本。
 */
const val MESSAGE_TYPE_MOMENT_SHARE = "moment_share"

/**
 * 消息类型：网易云歌单卡片
 *
 * 官方发送帧为 `{"event":"message","type":"music_playlist","content":"<歌单 JSON>"}`，
 * 接收端 `JSON.parse(content)` 后渲染歌单卡片（与 `music` 同一套 `msg-music-wrap`），
 * 解析失败时气泡显示「歌单已失效」（对齐官方）。
 */
const val MESSAGE_TYPE_MUSIC_PLAYLIST = "music_playlist"

/**
 * 消息类型：表情包
 *
 * 官方发送帧为 `{"event":"message","content":"<asset_id>","asset_id":<id>,"type":"sticker"}`，
 * 接收端优先取 `image_url`，否则用 `content` 里的 asset_id 到本地表情包列表（`/api/sticker/list`）
 * 按 `asset_id` 查 url；都拿不到时气泡降级展示「表情包已失效」（对齐官方；会话列表摘要才是 `[表情包]`）。
 */
const val MESSAGE_TYPE_STICKER = "sticker"

/**
 * 动态分享卡片数据（对齐官方 `moment_share` 的 content JSON，`v:1`）
 *
 * 字段与官方 `momentTime-*.js` 的发送构造 `p()` / 接收解析 `g()` 一一对应：
 * - `moment_id` 必须 > 0，否则官方接收端直接判为失效（本模型由解析层保证）；
 * - `uid` 仅保留合法的 32 位 hex（小写），其余置空；
 * - `gender` 只认 `male` / `female`，其余一律 `other`；
 * - `excerpt` 接收侧最多保留 400 字（发送侧超 280 字截断 + "…"）；
 * - `images` 已过滤空值并截断到 9 张；
 * - `has_audio` / `has_music` 缺省时按 `audio_url` / `music` 是否存在推导。
 *
 * @param momentId 动态 id（官方 `moment_id`）
 * @param userId 作者数字 id（官方 `user_id`，<=0 时官方落 0）
 * @param uid 作者 32 位 hex uid（官方规范化小写；非法则为空串）
 * @param username 作者昵称（最多 32 字，空则「用户」）
 * @param gender male / female / other
 * @param avatarUrl 作者头像地址
 * @param avatarFallback 头像兜底文字（作者名首字，最多 2 字，空则 "?"）
 * @param excerpt 正文摘要
 * @param images 图片地址（最多 9 张）
 * @param audioUrl 语音动态音频地址（无则 null）
 * @param hasAudio 是否含语音（官方 `has_audio`）
 * @param hasMusic 是否含音乐（官方 `has_music`）
 * @param music 已解析的网易云音乐（歌曲形态；歌单仅保留名称/封面）
 * @param likeCount 点赞数（>=0）
 * @param commentCount 评论数（>=0）
 * @param createdAt 原始创建时间字符串（本客户端 MomentItem 未携带，发送时为 null）
 */
data class MomentShareCardData(
    val momentId: Long = 0L,
    val userId: Long = 0L,
    val uid: String = "",
    val username: String = "用户",
    val gender: String = "other",
    val avatarUrl: String = "",
    val avatarFallback: String = "?",
    val excerpt: String = "",
    val images: List<String> = emptyList(),
    val audioUrl: String? = null,
    val hasAudio: Boolean = false,
    val hasMusic: Boolean = false,
    val music: MomentMusic? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: String? = null
)

/**
 * 把动态卡片数据转成 [MomentItem]，用于「看评论」复用 [top.lanxint.zerotalk.ui.moments.MomentCommentSheet]。
 *
 * 注意：卡片数据是服务端下发的截断摘要（正文 ≤280 字、图片 ≤9 张），
 * 因此构造出的对象只适合展示评论，正文可能已被截断。
 */
fun MomentShareCardData.toMomentItem(): MomentItem = MomentItem(
    id = momentId.toString(),
    authorId = userId.toString(),
    authorUid = uid,
    authorName = username,
    authorAvatar = avatarUrl,
    authorGender = when (gender.lowercase()) {
        "female" -> "女"
        "male" -> "男"
        else -> "女"
    },
    publishTime = createdAt.orEmpty(),
    textContent = excerpt,
    imageUrl = images.firstOrNull().orEmpty(),
    images = images,
    audioUrl = audioUrl.orEmpty(),
    music = music,
    likesCount = likeCount,
    commentsCount = commentCount
)

/**
 * 网易云歌单卡片数据（对齐官方 `musicPlayer-*.js` 的 `Yr()` 解析结果）
 *
 * 官方解析规则（逐字段对齐）：
 * - 输入是 JSON 字符串或对象，解析失败 / 非对象直接返回 null；
 * - `playlist_id ?? id` 必须是 1~20 位纯数字，否则 null；
 * - 若 `kind !== "playlist"` 且 `tracks` 不是数组且 `playlist_id` 为假值，则 null；
 * - `name` 空则「未知歌单」；`cover_url` / `creator` 去空白；
 * - `tracks` 逐条按歌曲规则（`Zn()`）解析，最多 500 首；
 * - `track_count` 取 `max(tracks.size, Number(track_count) || 0)`。
 *
 * @param provider 固定 netease
 * @param playlistId 歌单 id（官方 `playlist_id`）
 * @param name 歌单名（空则「未知歌单」）
 * @param coverUrl 封面地址
 * @param creator 创建者昵称
 * @param trackCount 曲目数（已按官方口径归一化）
 * @param tracks 已解析曲目（最多 500 首）
 */
data class MusicPlaylist(
    val provider: String = "netease",
    val playlistId: String = "",
    val name: String = "",
    val coverUrl: String = "",
    val creator: String = "",
    val trackCount: Int = 0,
    val tracks: List<MomentMusic> = emptyList()
)

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
    /**
     * 服务端标记的「已撤回 / 已删除」（官网 `is_deleted`）
     *
     * 撤回后消息**保留在列表里**，但服务端已清空 `content` / `image_url`。
     * 渲染层必须在类型分发之前整条替换为「该消息已被撤回」，否则图片会变占位框、
     * 骰子会变 `?`（此前完全没有消费该字段，重进会话即复现）。
     */
    val isDeleted: Boolean = false,
    val quotedText: String? = null,
    val quotedIsMine: Boolean? = null,
    val quotedSenderName: String? = null,
    /** 被引用消息的服务端 id（点击引用条跳转用；服务端下发 reply_to_id，此前在映射层被丢弃） */
    val replyToId: Long? = null,
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
    /**
     * 发送方称号（官方 UserTitleBadge 的 `title`，服务端在消息对象上下发）。
     *
     * 空串表示未下发 / 无称号，UI 不渲染徽章（不要显示占位）。
     */
    val authorTitle: String = "",
    /** 发送方称号颜色 key（官方 `title_color`，白名单外 UI 回落 blue） */
    val authorTitleColor: String = "",
    /** 会话摘要文案（图片 / 拍一拍等非文本消息用于列表展示） */
    val previewText: String = "",
    /**
     * 是否为动态分享卡片消息（服务端 `type:"moment_share"`）
     *
     * content 是卡片 JSON。解析失败时 [momentShare] 为 null，
     * 气泡按官网口径显示「动态卡片已失效」，而不是把 JSON 原文贴出来。
     */
    val isMomentShare: Boolean = false,
    /** 已解析的动态分享卡片数据（解析失败为 null） */
    val momentShare: MomentShareCardData? = null,
    /**
     * 是否为网易云歌单卡片消息（服务端 `type:"music_playlist"`）
     *
     * content 是歌单 JSON。解析失败时 [musicPlaylist] 为 null，
     * 气泡显示「歌单已失效」，而不是把 JSON 原文贴出来。
     */
    val isMusicPlaylist: Boolean = false,
    /** 已解析的歌单卡片数据（解析失败为 null） */
    val musicPlaylist: MusicPlaylist? = null,
    /**
     * 是否为表情包消息（服务端 `type:"sticker"`）
     *
     * 渲染优先用 [stickerUrl]（来自 `image_url` 或本地表情包列表解析）；
     * 都拿不到时气泡降级显示「表情包已失效」（会话列表摘要为 `[表情包]`）。
     */
    val isSticker: Boolean = false,
    /** 表情包图片地址（image_url 直取，或按 asset_id 查本地列表得到；可能为空） */
    val stickerUrl: String = "",
    /** 表情包 asset_id（content 解析得到；无法解析为 0） */
    val stickerAssetId: Long = 0L
)

/**
 * 官方撤回占位文案。
 *
 * 官网 `publicAnnouncementDismiss-*.js` 在类型分发之前执行：
 * `e.msg.is_deleted ? <div class="Xu">该消息已被撤回</div> : …`，
 * 会话列表摘要与引用摘要也用同一文案。
 */
const val RECALLED_MESSAGE_TEXT = "该消息已被撤回"

/**
 * 把消息归一化为「已撤回」状态（对齐官网 `handleMessageRecall` 的本地标记 + 清空）。
 *
 * 官网撤回时执行：
 * `is_deleted = true; content = ""; image_url = null; reply_preview = null;
 *  mention_ids = []; mention_users = [];`
 * 这里同步清空本模型里等价的正文 / 媒体地址 / 引用摘要，并把会话摘要固定为
 * [RECALLED_MESSAGE_TEXT]，避免撤回后的会话列表仍显示旧内容。
 *
 * 类型标记（[ChatMessage.isImage] / [ChatMessage.isDice] 等）保留不动，
 * 渲染层以 [ChatMessage.isDeleted] 优先短路，不会再出现占位框与 `?`。
 */
fun ChatMessage.asRecalled(): ChatMessage = copy(
    isDeleted = true,
    content = "",
    imageUrl = "",
    audioUrl = "",
    stickerUrl = "",
    quotedText = null,
    quotedSenderName = null,
    quotedIsMine = null,
    previewText = RECALLED_MESSAGE_TEXT
)

/**
 * 是否仍提供「撤回 / 撤回并编辑 / 编辑」操作项。
 *
 * 对齐官网：`e.isOwnMessage(e.msg) && !e.msg.is_deleted && e.msg.id`。
 * 已撤回消息在官网被整条替换为「该消息已被撤回」，不再有操作入口。
 */
fun ChatMessage.canRecallOrEdit(): Boolean = isMine && !isDeleted

/**
 * 是否仍提供「拷贝」操作项。
 *
 * 已撤回消息的 content 已被清空，拷贝没有意义（官网同样不再提供）。
 */
fun ChatMessage.canCopy(): Boolean = !isDeleted

/**
 * 解析引用条应展示的文案。
 *
 * 服务端撤回时会同步把所有引用它的消息 `reply_preview` 改成「该消息已被撤回」；
 * 这里对「被引用消息仍在同一列表且 isDeleted」的情况做本地兜底，
 * 避免服务端漏改时引用条仍显示已被清空的旧内容。
 */
fun List<ChatMessage>.resolveQuotedText(message: ChatMessage): String {
    val target = message.replyToId
        ?.takeIf { it > 0L }
        ?.let { id -> firstOrNull { it.serverId == id } }
    return if (target?.isDeleted == true) RECALLED_MESSAGE_TEXT else message.quotedText.orEmpty()
}

/**
 * 就地应用一次撤回事件（对齐官网 `handleMessageRecall` 对消息列表的改写）。
 *
 * - 命中 [messageId] 的消息改写为 [ChatMessage.asRecalled]；
 * - 引用它的消息把引用摘要改成 [RECALLED_MESSAGE_TEXT]；
 * - **不移除任何元素**：官网撤回后消息保留在列表里，由渲染层整条替换为占位。
 *
 * 抽成纯函数便于单元测试覆盖「标记而非移除」这一行为。
 */
fun List<ChatMessage>.applyRecall(messageId: Long): List<ChatMessage> =
    if (messageId <= 0L) {
        this
    } else {
        map { msg ->
            when {
                msg.serverId == messageId -> msg.asRecalled()
                msg.replyToId == messageId -> msg.copy(quotedText = RECALLED_MESSAGE_TEXT)
                else -> msg
            }
        }
    }

/**
 * 引用回复时展示的「被引用消息」摘要。
 *
 * 骰子按官网口径展示为 `[骰子 N]`；拍一拍直接用已归因文案，
 * 避免把服务端 JSON（`{"a":…,"t":…}`）原样贴进引用条。
 * 已撤回消息（`is_deleted`）固定展示 [RECALLED_MESSAGE_TEXT]（对齐官网）。
 */
fun ChatMessage.quotePreviewText(): String = when {
    isDeleted -> RECALLED_MESSAGE_TEXT
    isVoiceCall -> voiceCallText.ifBlank { "[语音通话]" }
    isVoice -> "[语音 ${voiceDurationSec}\"]"
    isImage -> "[图片]"
    isDice -> if (diceValue in 1..6) "[骰子 $diceValue]" else "[骰子]"
    isPat -> patText.ifBlank { "拍了拍" }
    isMomentShare -> previewText.ifBlank { "[动态]" }
    isMusicPlaylist -> previewText.ifBlank { "[歌单]" }
    isSticker -> "[表情包]"
    isMusic || isGame -> previewText
    else -> content
}

/**
 * 新消息通知（用于全局弹窗展示），由 WebSocket 实时推送触发。
 *
 * @param roomName 房间显示名称
 * @param senderName 发送者昵称（私聊与群聊都传；横幅在群聊里显示为「发送人：内容」）
 * @param content 消息正文
 * @param isGroup 是否为群聊
 */
data class IncomingMessageNotification(
    val roomName: String,
    val senderName: String = "",
    val content: String,
    val isGroup: Boolean = false,
    /** 会话房间 id：系统通知点击后直达该会话（大厅等场景可为空） */
    val roomId: String = ""
)


