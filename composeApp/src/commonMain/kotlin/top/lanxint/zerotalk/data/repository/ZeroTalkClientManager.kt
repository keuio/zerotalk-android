package top.lanxint.zerotalk.data.repository

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import top.lanxint.zerotalk.data.model.ChatMessage
import top.lanxint.zerotalk.data.model.ConversationItem
import top.lanxint.zerotalk.data.model.IncomingMessageNotification
import top.lanxint.zerotalk.data.log.ZtLog
import top.lanxint.zerotalk.data.notify.SystemNotificationBridge
import top.lanxint.zerotalk.data.model.MessageCategory
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.data.model.FollowListKind
import top.lanxint.zerotalk.data.model.FollowUserItem
import top.lanxint.zerotalk.data.model.OtherUserFollowListState
import top.lanxint.zerotalk.data.model.OtherUserProfile
import top.lanxint.zerotalk.data.model.OtherUserProfileState
import top.lanxint.zerotalk.data.model.UserProfileLayer
import top.lanxint.zerotalk.data.model.UserProfileLayerHost
import top.lanxint.zerotalk.data.model.UserProfilePageSnapshot
import top.lanxint.zerotalk.data.model.UserProfileTarget
import top.lanxint.zerotalk.data.model.MomentMusic
import top.lanxint.zerotalk.data.model.UserProfile
import top.lanxint.zerotalk.data.model.*
import top.lanxint.zerotalk.data.network.BlockedUserDto
import top.lanxint.zerotalk.data.network.BlockListData
import top.lanxint.zerotalk.data.network.BootstrapData
import top.lanxint.zerotalk.data.network.BootstrapUser
import top.lanxint.zerotalk.data.network.ChatBootstrapData
import top.lanxint.zerotalk.data.network.ChatMessageDto
import top.lanxint.zerotalk.data.network.ContactBootstrapData
import top.lanxint.zerotalk.data.network.CreateMomentData
import top.lanxint.zerotalk.data.network.CreateRoomData
import top.lanxint.zerotalk.data.network.DonateData
import top.lanxint.zerotalk.data.network.DonorDto
import top.lanxint.zerotalk.data.network.FollowListData
import top.lanxint.zerotalk.data.network.FollowListItemDto
import top.lanxint.zerotalk.data.network.JoinRoomData
import top.lanxint.zerotalk.data.network.LoginData
import top.lanxint.zerotalk.data.network.MomentCommentsData
import top.lanxint.zerotalk.data.network.MomentsPrivacyData
import top.lanxint.zerotalk.data.network.PostCommentData
import top.lanxint.zerotalk.data.network.CommentLikeData
import top.lanxint.zerotalk.data.network.MomentItemDto
import top.lanxint.zerotalk.data.network.MomentsCategory
import top.lanxint.zerotalk.data.network.MomentsSort
import top.lanxint.zerotalk.data.network.NetworkImageUrl
import top.lanxint.zerotalk.data.network.NeteaseBindingData
import top.lanxint.zerotalk.data.network.NeteaseQrStartData
import top.lanxint.zerotalk.data.network.NeteaseQrStatusData
import top.lanxint.zerotalk.data.network.PenaltyAppealBootstrapData
import top.lanxint.zerotalk.data.network.ReportItemDto
import top.lanxint.zerotalk.data.network.ReportListData
import top.lanxint.zerotalk.data.network.RoomJoinBanDto
import top.lanxint.zerotalk.data.network.RoomMemberDto
import top.lanxint.zerotalk.data.network.RoomMembersData
import top.lanxint.zerotalk.data.network.SecurityDeviceDto
import top.lanxint.zerotalk.data.network.SecurityLoginLogDto
import top.lanxint.zerotalk.data.network.SessionStore
import top.lanxint.zerotalk.data.network.UpdateProfileData
import top.lanxint.zerotalk.data.network.UserLookupData
import top.lanxint.zerotalk.data.network.UserProfileAggregateData
import top.lanxint.zerotalk.data.network.UserShareCodeData
import top.lanxint.zerotalk.data.network.WsConnectionState
import top.lanxint.zerotalk.data.network.WsServerEvent
import com.google.gson.Gson
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlin.random.Random
import java.util.Calendar
import java.io.File
import java.security.MessageDigest
import okhttp3.Request
import top.lanxint.zerotalk.data.network.ZeroTalkApiService
import top.lanxint.zerotalk.data.network.ZeroTalkWebSocketClient
import top.lanxint.zerotalk.data.voice.VoiceCallConfig
import top.lanxint.zerotalk.data.voice.VoiceCallController
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * 客户端连接状态
 */
sealed class ClientStatus {
    object Idle : ClientStatus()
    object LoggingIn : ClientStatus()
    object Bootstrapping : ClientStatus()
    object ConnectingWs : ClientStatus()
    object Connected : ClientStatus()
    data class Error(val message: String) : ClientStatus()
}

/**
 * 匹配状态
 */
sealed class MatchStatus {
    object Idle : MatchStatus()
    data class Matching(val isVoice: Boolean) : MatchStatus()
    data class Matched(
        val roomId: String,
        val partner: WsServerEvent.UserJoined?
    ) : MatchStatus()
    object Timeout : MatchStatus()
    data class Error(val message: String) : MatchStatus()
}

/**
 * 零语全局核心调度与数据中枢 (单例)
 */
object ZeroTalkClientManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    val apiService = ZeroTalkApiService()
    val wsClient = ZeroTalkWebSocketClient(apiService.client, scope)

    /**
     * 语音通话控制器（官网 useVoiceCall 的移植：信令 + 状态机 + WebRTC 引擎）
     *
     * UI 直接订阅 [VoiceCallController.uiState] / [VoiceCallController.session] / [VoiceCallController.events]。
     */
    val voiceCall: VoiceCallController = VoiceCallController(
        scope = scope,
        ws = wsClient,
        api = apiService,
        selfUidProvider = { _loginData.value?.uid.orEmpty() }
    )

    private val _voiceBannedTick = MutableStateFlow(0)

    /** 匹配超时保护任务 */
    private var matchTimeoutJob: Job? = null

    /**
     * 「语音通话被限制」信号（自增计数）
     *
     * 官网收到 `voice_call_banned` 时会把首页匹配模式由「语音」切回「聊天」，UI 订阅本信号即可复现。
     */
    val voiceBannedTick: StateFlow<Int> = _voiceBannedTick.asStateFlow()

    /** 通知 UI：语音通话被限制 */
    fun notifyVoiceCallBanned() {
        _voiceBannedTick.value = _voiceBannedTick.value + 1
    }

    private val _newMessageNotification = MutableSharedFlow<IncomingMessageNotification>(extraBufferCapacity = 16)
    val newMessageNotification: SharedFlow<IncomingMessageNotification> = _newMessageNotification

    private val _pendingOpenRoomId = MutableStateFlow<String?>(null)

    /** 系统通知点击后请求直达的会话 id（由 UI 消费后置空） */
    val pendingOpenRoomId: StateFlow<String?> = _pendingOpenRoomId.asStateFlow()

    /** 系统通知点击：请求打开指定会话 */
    fun requestOpenRoom(roomId: String) {
        if (roomId.isBlank()) return
        _pendingOpenRoomId.value = roomId
    }

    /** UI 已处理完待打开的会话 */
    fun consumePendingOpenRoom() {
        _pendingOpenRoomId.value = null
    }

    private val _status = MutableStateFlow<ClientStatus>(ClientStatus.Idle)
    val status: StateFlow<ClientStatus> = _status.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile.NO_ACCOUNT)
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _loginData = MutableStateFlow<LoginData?>(null)
    val loginData: StateFlow<LoginData?> = _loginData.asStateFlow()

    private val _bootstrapData = MutableStateFlow<BootstrapData?>(null)
    val bootstrapData: StateFlow<BootstrapData?> = _bootstrapData.asStateFlow()

    /** 全站在线用户数（对齐官网 online_users） */
    private val _onlineUsersCount = MutableStateFlow<Int?>(null)
    val onlineUsersCount: StateFlow<Int?> = _onlineUsersCount.asStateFlow()

    /** 按房间存储聊天 bootstrap 数据（包含成员列表和权限字段） */
    private val _roomBootstrapMap = MutableStateFlow<Map<String, ChatBootstrapData>>(emptyMap())
    val roomBootstrapMap: StateFlow<Map<String, ChatBootstrapData>> = _roomBootstrapMap.asStateFlow()

    /** 各房间成员列表（GET /api/room/members，群聊资料页权威来源） */
    private val _roomMembers = MutableStateFlow<Map<String, List<RoomMemberDto>>>(emptyMap())
    val roomMembers: StateFlow<Map<String, List<RoomMemberDto>>> = _roomMembers.asStateFlow()

    /** 各房间成员总数（服务端 total） */
    private val _roomMemberTotal = MutableStateFlow<Map<String, Int>>(emptyMap())
    val roomMemberTotal: StateFlow<Map<String, Int>> = _roomMemberTotal.asStateFlow()

    /** 各房间成员列表拉取中标记 */
    private val _roomMembersLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val roomMembersLoading: StateFlow<Map<String, Boolean>> = _roomMembersLoading.asStateFlow()

    /** 各房间「禁止加入」名单（GET /api/room/join-bans） */
    private val _roomJoinBans = MutableStateFlow<Map<String, List<RoomJoinBanDto>>>(emptyMap())
    val roomJoinBans: StateFlow<Map<String, List<RoomJoinBanDto>>> = _roomJoinBans.asStateFlow()

    /** 各房间「禁止加入」名单拉取中标记 */
    private val _roomJoinBansLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val roomJoinBansLoading: StateFlow<Map<String, Boolean>> = _roomJoinBansLoading.asStateFlow()

    /** 冷启动恢复登录态是否进行中（用于忽略并发的手动登录） */
    private val _isRestoringSession = MutableStateFlow(false)
    val isRestoringSession: StateFlow<Boolean> = _isRestoringSession.asStateFlow()

    private val _matchStatus = MutableStateFlow<MatchStatus>(MatchStatus.Idle)
    val matchStatus: StateFlow<MatchStatus> = _matchStatus.asStateFlow()

    private val _roomPeers = MutableStateFlow<Map<String, WsServerEvent.UserJoined>>(emptyMap())
    val roomPeers: StateFlow<Map<String, WsServerEvent.UserJoined>> = _roomPeers.asStateFlow()

    private val _hallRoomId = MutableStateFlow("a9bee4a25027ab143de9f313aabbc34a")
    val hallRoomId: StateFlow<String> = _hallRoomId.asStateFlow()

    private val _hallMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val hallMessages: StateFlow<List<ChatMessage>> = _hallMessages.asStateFlow()

    private val _roomMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val roomMessages: StateFlow<Map<String, List<ChatMessage>>> = _roomMessages.asStateFlow()

    /** 各房间是否还有更早的历史消息（上拉分页） */
    private val _roomHasMoreHistory = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val roomHasMoreHistory: StateFlow<Map<String, Boolean>> = _roomHasMoreHistory.asStateFlow()

    /** 各房间历史消息拉取中标记 */
    private val _roomHistoryLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val roomHistoryLoading: StateFlow<Map<String, Boolean>> = _roomHistoryLoading.asStateFlow()

    /** 各房间是否为「群聊房间」（暗号房 / 公共房，需要展示发送者头像昵称性别） */
    private val _groupRooms = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val groupRooms: StateFlow<Map<String, Boolean>> = _groupRooms.asStateFlow()

    /** 各房间当前已知最早一条服务端消息 id（分页游标） */
    private val oldestServerMessageId = mutableMapOf<String, Long>()

    /**
     * 成员资料目录（uid -> 昵称/头像/性别）
     *
     * 群聊里服务端并非每条消息都带 username/avatar_url/gender，
     * 单靠 user_joined 事件又只能记住最后一个人，因此按 uid 累积资料用于回填发送者信息。
     */
    private val memberProfiles = mutableMapOf<String, WsServerEvent.UserJoined>()

    private fun rememberMember(uid: String?, username: String?, gender: String?, avatarUrl: String?) {
        val key = uid?.trim().orEmpty()
        if (key.isBlank()) return
        if (username.isNullOrBlank() && gender.isNullOrBlank() && avatarUrl.isNullOrBlank()) return
        val existing = memberProfiles[key]
        memberProfiles[key] = WsServerEvent.UserJoined(
            uid = key,
            username = username?.takeIf { it.isNotBlank() } ?: existing?.username.orEmpty(),
            gender = gender?.takeIf { it.isNotBlank() } ?: existing?.gender.orEmpty(),
            avatarUrl = avatarUrl?.takeIf { it.isNotBlank() } ?: existing?.avatarUrl,
            mbti = existing?.mbti,
            cleanStreamMode = existing?.cleanStreamMode ?: 0,
            roomId = existing?.roomId
        )
    }

    private fun memberName(uid: String?): String = memberProfiles[uid?.trim().orEmpty()]?.username.orEmpty()
    private fun memberAvatar(uid: String?): String = memberProfiles[uid?.trim().orEmpty()]?.avatarUrl.orEmpty()
    private fun memberGender(uid: String?): String = memberProfiles[uid?.trim().orEmpty()]?.gender.orEmpty()

    /**
     * 判断一条服务端消息是否由本人发出
     *
     * 优先采用服务端显式下发的 `is_self`；否则兼容 `from_uid` 下发 uid 字符串、
     * 数字 user_id、以及缺失发送者（此时用昵称兜底）三种情况。
     */
    private fun isMessageFromMe(fromUid: String?, username: String?, isSelf: Boolean? = null): Boolean {
        if (isSelf != null) return isSelf
        val myUid = _loginData.value?.uid
        val myUserId = _loginData.value?.userId?.toString()
        val myName = _userProfile.value.name
        return when {
            fromUid != null && myUid != null && fromUid == myUid -> true
            fromUid != null && myUserId != null && fromUid == myUserId -> true
            fromUid == null && myName.isNotBlank() && username == myName -> true
            else -> false
        }
    }

    /**
     * 与本地即时回显对账：视为同一条时用服务端消息替换本地回显，避免出现两条。
     * 同时返回被匹配到的本地回显，供调用方将引用回复信息（quotedText / quotedIsMine）
     * 合并到服务端消息中，因为服务端目前不会回传引用字段。
     *
     * 先在时间窗内按内容/类型匹配；**匹配不到时退化为「同类 + FIFO」**，
     * 用来兜两类必然失配的场景：
     * 1. 骰子点数由服务端摇出，本地回显 content 为空，无法按内容比对；
     * 2. 设备时区/时钟与服务端不一致——实测模拟器为 GMT，而服务端 `created_at`
     *    是 GMT+8，`eventMs - timestampMs` 恒定偏 8 小时，时间窗永远打不中。
     *
     * 失配的后果不是「多一条」，而是自己的消息被当成重复消息**丢弃**
     * （见消息分支里的 `isMine -> currentList`），气泡就永远停在本地回显上。
     */
    private fun reconcilePendingEcho(
        list: List<ChatMessage>,
        event: WsServerEvent.Message,
        eventMs: Long
    ): Triple<List<ChatMessage>, Boolean, ChatMessage?> {
        val pendingEchoes = list.filter { it.serverId == 0L && it.isMine }
        if (pendingEchoes.isEmpty()) return Triple(list, false, null)

        val matched = pendingEchoes.firstOrNull { item ->
            (eventMs - item.timestampMs) in -ECHO_MATCH_WINDOW_MS..ECHO_MATCH_WINDOW_MS &&
                isSameEchoKind(item, event)
        } ?: pendingEchoes.firstOrNull { isSameEchoKind(it, event) }
        ?: return Triple(list, false, null)

        return Triple(list.filterNot { it.id == matched.id }, true, matched)
    }

    /** 本地回显与服务端消息的常规时间窗（设备与服务端时钟一致时的首选判据） */
    private const val ECHO_MATCH_WINDOW_MS = 15_000L

    /**
     * 本地回显与服务端消息是否同一条
     *
     * 文本/图片/语音等内容可预知的类型按内容比对；**骰子的点数由服务端摇出**，
     * 本地回显 content 为空、服务端回推 content 是点数（实测 `"5"`），
     * 只按内容比对会匹配失败，进而让自己的骰子消息被当成重复消息丢弃，
     * 气泡永远停在空内容——这正是「骰子消息内容为空」的根因。
     */
    private fun isSameEchoKind(echo: ChatMessage, event: WsServerEvent.Message): Boolean {
        if (echo.content == event.content) return true
        return echo.isDice && event.type.equals("dice", ignoreCase = true)
    }

    private val _conversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversations: StateFlow<List<ConversationItem>> = _conversations.asStateFlow()

    // ---------------- 本地 uid -> 备注 映射（群聊 / 暗号房气泡的发言人名字） ----------------

    /**
     * uid -> 备注 的本地映射
     *
     * 备注接口按 uid 保存（POST /user/remark 的 peer_user_id），而群聊 / 暗号房的消息只有
     * senderId(uid)、拿不到对应的会话项，因此这里维护一份 uid -> 备注 的本地快照。
     * UI 用 collectAsState 订阅本 StateFlow，备注保存 / 清空后气泡上的发言人名字会立即刷新。
     */
    private val _uidRemarks = MutableStateFlow<Map<String, String>>(emptyMap())
    val uidRemarks: StateFlow<Map<String, String>> = _uidRemarks.asStateFlow()

    /**
     * 查询某个 uid 的备注（无备注返回 null）
     *
     * @param uid 32 位 hex uid（消息的 senderId / 会话项的 targetUid）
     */
    fun remarkForUid(uid: String?): String? {
        val key = uid?.trim().orEmpty()
        if (key.isEmpty()) return null
        val snapshot = _uidRemarks.value
        return snapshot[key]?.takeIf { it.isNotBlank() }
            ?: snapshot[key.lowercase()]?.takeIf { it.isNotBlank() }
    }

    /**
     * 用当前会话列表整表重建 uid -> 备注 映射
     *
     * 必须整表重建而不是增量合并：备注被清空（targetRemark 为空）时该 uid 要能从映射里移除，
     * 否则气泡上会一直残留旧备注。
     */
    @Synchronized
    private fun refreshUidRemarks() {
        val next = mutableMapOf<String, String>()
        _conversations.value.forEach { conv ->
            val uid = conv.targetUid.trim()
            val remark = conv.targetRemark.trim()
            if (uid.isNotEmpty() && remark.isNotEmpty()) next[uid] = remark
        }
        if (next != _uidRemarks.value) _uidRemarks.value = next
    }

    // ---------------- 会话自定义背景（全局默认 + 会话级覆盖，本地持久化，可离线显示） ----------------

    /*
     * 会话自定义背景的继承模型
     *
     * 实际生效背景 resolve(会话) = 会话级覆盖 ?: 全局默认 ?: null（素雅纯色）：
     * - 会话级**没有 key** → 跟随全局默认；
     * - 会话级 key 存在且值为 [WALLPAPER_NONE_MARKER] → 该会话显式纯色，不再跟随全局；
     * - 会话级 key 存在且为路径 / URL → 该会话显式背景。
     *
     * 远程图片会先下载到 `filesDir/chat_wallpapers/<hash>.jpg` 再记录本地路径，断网 / 重启后仍能显示；
     * 下载失败时回退记录原始 URL。映射落盘在 `filesDir/zt_chat_wallpapers.txt`（key=value 行格式：
     * 全局默认的 key 是 [WALLPAPER_GLOBAL_KEY]，其余 key 是会话 id；写法与
     * [top.lanxint.zerotalk.data.settings.UiPreferencesStore] 一致）。
     */

    /** 会话级「显式纯色」标记：key 存在且为该值时该会话不跟随全局默认背景 */
    private const val WALLPAPER_NONE_MARKER = "!none"

    /** 全局默认背景在落盘文件 / 内存缓存中的 key */
    private const val WALLPAPER_GLOBAL_KEY = "__global__"

    private val _globalWallpaper = MutableStateFlow<String?>(null)

    /** 全局默认背景（null = 未设置全局背景，未覆盖的会话使用素雅纯色） */
    val globalWallpaper: StateFlow<String?> = _globalWallpaper.asStateFlow()

    private val _conversationWallpapers = MutableStateFlow<Map<String, String>>(emptyMap())

    /**
     * 会话级覆盖的**原始**映射（不含全局 key；值可能是 [WALLPAPER_NONE_MARKER]）
     *
     * UI 订阅它以在会话级背景变化（如下载完成后的 URL → 本地路径替换）时刷新。
     */
    val conversationWallpapers: StateFlow<Map<String, String>> = _conversationWallpapers.asStateFlow()

    private const val WALLPAPER_PREF_FILE = "zt_chat_wallpapers.txt"
    private const val WALLPAPER_DIR_NAME = "chat_wallpapers"

    /** 持久化目录（由 ZeroTalkApplication.onCreate 传入 context.filesDir） */
    @Volatile
    private var wallpaperStorageDir: File? = null

    /** 落盘内容的内存快照（同时镜像全局 key 与会话 key），避免每次读写都做磁盘 IO */
    private val wallpaperCache = mutableMapOf<String, String>()

    /**
     * 初始化会话背景持久化目录（在 ZeroTalkApplication.onCreate 中调用）
     *
     * 与 DeviceIdManager / SessionStore / UiPreferencesStore 的 init 一致：进程被前台服务拉活
     * （无 Activity）时也要能读回上次设置的背景。全局与会话两类 key 一并读回
     * （旧文件里只有会话 id，读出来语义不变）。
     */
    fun initConversationWallpapers(filesDir: File) {
        // 目录先同步落定，保证后续写入立刻可用
        wallpaperStorageDir = filesDir
        // 磁盘读取放后台：与 DeviceIdManager / SessionStore 的预热方式保持一致，避免冷启动在主线程做文件 IO。
        // 结果通过 StateFlow 发布，UI 会自动跟上（背景要到打开会话页才用得上，不会闪）。
        scope.launch(Dispatchers.IO) {
            val loaded = mutableMapOf<String, String>()
            readWallpaperPrefs()?.lineSequence()?.forEach { line ->
                val separator = line.indexOf('=')
                if (separator > 0) {
                    val key = line.substring(0, separator).trim()
                    val value = line.substring(separator + 1).trim()
                    if (key.isNotEmpty() && value.isNotEmpty()) loaded[key] = value
                }
            }
            applyLoadedWallpapers(loaded)
        }
    }

    /** 把磁盘读回的内容并入内存缓存（只补缺、不覆盖读取期间用户新设的值）并发布 */
    @Synchronized
    private fun applyLoadedWallpapers(loaded: Map<String, String>) {
        loaded.forEach { (key, value) -> if (!wallpaperCache.containsKey(key)) wallpaperCache[key] = value }
        publishWallpaperState()
        // 上次下载失败（取值仍是远程 URL）时补一次，避免一直依赖网络；全局与会话两类 key 都要补
        wallpaperCache.toMap()
            .filter { (_, value) -> isRemoteWallpaper(value) }
            .forEach { (key, url) -> downloadAndSwap(key, url) }
    }

    /** 把内存缓存同步到对外 StateFlow（全局 + 会话级两类 key） */
    @Synchronized
    private fun publishWallpaperState() {
        val global = wallpaperCache[WALLPAPER_GLOBAL_KEY]?.takeIf { it.isNotBlank() }
        if (_globalWallpaper.value != global) _globalWallpaper.value = global
        val overrides = wallpaperCache.filterKeys { it != WALLPAPER_GLOBAL_KEY }
        if (overrides != _conversationWallpapers.value) _conversationWallpapers.value = overrides
    }

    /** 读取缓存中某个 key（会话 id 或 [WALLPAPER_GLOBAL_KEY]）的取值，空值视为未设置 */
    @Synchronized
    private fun cachedWallpaper(key: String): String? = wallpaperCache[key]?.takeIf { it.isNotBlank() }

    /**
     * 后台把某个 key（会话 id 或 [WALLPAPER_GLOBAL_KEY]）的远程背景图下载为本地文件并回写
     *
     * 失败保留原始 URL 兜底。
     */
    private fun downloadAndSwap(key: String, url: String) {
        scope.launch(Dispatchers.IO) {
            val local = downloadWallpaperToFile(url)
            if (local == null) {
                ZtLog.w("ZeroTalk", "[Wallpaper] 背景图下载失败，回退原始 URL：" + url)
                return@launch
            }
            if (cachedWallpaper(key) != url) {
                // 期间用户又改了背景：本次下载的文件已无人引用，直接清掉
                deleteLocalWallpaperIfUnused(local)
                return@launch
            }
            updateWallpaperEntry(key, local)
        }
    }

    /**
     * 设置全局默认背景
     *
     * @param wallpaper http(s) 图片地址（会下载为本地文件，映射里存本地绝对路径）、
     *        本地文件绝对路径 / `file://` 形式；传 null / 空串清除全局默认背景
     */
    fun setGlobalWallpaper(wallpaper: String?) {
        setWallpaperValue(WALLPAPER_GLOBAL_KEY, wallpaper, explicitNoneWhenEmpty = false)
    }

    /**
     * 设置会话自定义背景
     *
     * @param wallpaper http(s) 图片地址（会下载为本地文件，映射里存本地绝对路径）、
     *        本地文件绝对路径 / `file://` 形式；传 null / 空串表示该会话**显式纯色**
     *        （写入 [WALLPAPER_NONE_MARKER]，不再跟随全局默认背景）
     */
    fun setConversationWallpaper(conversationId: String, wallpaper: String?) {
        val key = conversationId.trim()
        if (key.isEmpty()) return
        setWallpaperValue(key, wallpaper, explicitNoneWhenEmpty = true)
    }

    /** 移除会话级覆盖（该会话恢复跟随全局默认背景） */
    fun clearConversationWallpaperOverride(conversationId: String) {
        val key = conversationId.trim()
        if (key.isEmpty()) return
        val previous = cachedWallpaper(key)
        updateWallpaperEntry(key, null)
        deleteLocalWallpaperIfUnused(previous)
    }

    /**
     * 写入某个 key 的背景取值（全局默认 / 会话级共用）
     *
     * @param explicitNoneWhenEmpty true 时空值写 [WALLPAPER_NONE_MARKER]（会话级显式纯色，
     *        不再跟随全局）；false 时移除 key（清除全局默认背景）
     */
    private fun setWallpaperValue(key: String, wallpaper: String?, explicitNoneWhenEmpty: Boolean) {
        val previous = cachedWallpaper(key)
        val value = wallpaper?.trim().orEmpty()
        if (value.isEmpty()) {
            updateWallpaperEntry(key, if (explicitNoneWhenEmpty) WALLPAPER_NONE_MARKER else null)
            deleteLocalWallpaperIfUnused(previous)
            return
        }
        if (!isRemoteWallpaper(value)) {
            // 本地路径 / file://：直接记录
            updateWallpaperEntry(key, value)
            if (previous != value) deleteLocalWallpaperIfUnused(previous)
            return
        }
        // 远程 URL：先记录原始地址（UI 立即可见），再后台下载为本地文件后替换为本地路径
        updateWallpaperEntry(key, value)
        if (previous != null && previous != value) deleteLocalWallpaperIfUnused(previous)
        downloadAndSwap(key, value)
    }

    /**
     * 保存「从相册选择的背景图」到本机并返回本地文件绝对路径
     *
     * 背景**完全本地化，不上传服务器**：直接把相册读到的字节写入
     * filesDir/chat_wallpapers/local_<md5>.jpg。
     *
     * **本函数不登记任何范围**：全局默认由调用方接着调 [setGlobalWallpaper]，
     * 会话级由调用方接着调 [setConversationWallpaper]。
     *
     * @return 本地文件绝对路径；写入失败返回 null
     */
    fun saveLocalWallpaperFile(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val dir = wallpaperDir() ?: return null
        val hex = try {
            MessageDigest.getInstance("MD5").digest(bytes)
                .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        } catch (_: Exception) {
            bytes.size.toString()
        }
        return try {
            val file = File(dir, "local_" + hex + ".jpg")
            if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            ZtLog.w("ZeroTalk", "[Wallpaper] 本地背景保存失败：" + e.message)
            null
        }
    }

    /**
     * 读取会话**实际生效**的背景（已按继承模型解析）
     *
     * @return 会话级覆盖（非显式纯色）→ 全局默认 → null（素雅纯色）
     */
    fun conversationWallpaper(conversationId: String): String? {
        val key = conversationId.trim()
        if (key.isEmpty()) return null
        if (_conversationWallpapers.value.containsKey(key)) {
            // 会话级有 key：显式纯色（!none）时不再回落到全局默认
            return conversationWallpaperOverride(key)?.takeIf { it.isNotEmpty() }
        }
        return _globalWallpaper.value?.takeIf { it.isNotBlank() }
    }

    /**
     * 读取会话级**原始覆盖**（未解析继承）
     *
     * @return null = 未单独设置（跟随全局默认）；空串 = 显式纯色（[WALLPAPER_NONE_MARKER]）；
     *         其余为背景取值
     */
    fun conversationWallpaperOverride(conversationId: String): String? {
        val key = conversationId.trim()
        if (key.isEmpty()) return null
        val raw = _conversationWallpapers.value[key] ?: return null
        return if (raw == WALLPAPER_NONE_MARKER) "" else raw
    }

    /** 该会话是否单独设置过背景（true = 不再跟随全局默认背景） */
    fun hasConversationWallpaperOverride(conversationId: String): Boolean {
        val key = conversationId.trim()
        return key.isNotEmpty() && _conversationWallpapers.value.containsKey(key)
    }

    /**
     * 写入背景映射（内存缓存 + 对外 StateFlow + 落盘，值未变化时跳过）
     *
     * @param key 会话 id 或 [WALLPAPER_GLOBAL_KEY]；value 为 null / 空表示移除该 key
     */
    @Synchronized
    private fun updateWallpaperEntry(key: String, value: String?) {
        val current = wallpaperCache.toMutableMap()
        if (value.isNullOrBlank()) {
            current.remove(key)
        } else {
            current[key] = value
        }
        if (current == wallpaperCache) return
        wallpaperCache.clear()
        wallpaperCache.putAll(current)
        publishWallpaperState()
        writeWallpaperPrefs()
    }

    // ---- 会话背景的文件读写（参照 UiPreferencesStore：@Synchronized + 内存缓存） ----

    private fun wallpaperPrefFile(): File? = wallpaperStorageDir?.let { File(it, WALLPAPER_PREF_FILE) }

    private fun readWallpaperPrefs(): String? = try {
        wallpaperPrefFile()?.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun writeWallpaperPrefs() {
        try {
            wallpaperPrefFile()?.writeText(
                wallpaperCache.entries.joinToString("\n") { "${it.key}=${it.value}" }
            )
        } catch (_: Exception) {
        }
    }

    /** 背景图本地缓存目录 filesDir/chat_wallpapers */
    private fun wallpaperDir(): File? {
        val root = wallpaperStorageDir ?: return null
        val dir = File(root, WALLPAPER_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** 是否为需要下载的远程图片地址 */
    private fun isRemoteWallpaper(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)

    /** 把背景取值解析为本地文件（绝对路径 / file:// 形式），非本地形态返回 null */
    private fun localWallpaperFile(value: String?): File? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty() || isRemoteWallpaper(raw)) return null
        val path = if (raw.startsWith("file://", ignoreCase = true)) raw.substring("file://".length) else raw
        return if (path.startsWith("/") || path.contains(":\\")) File(path) else null
    }

    /** 用 URL 的 MD5 作为文件名：同一个 URL 复用同一个本地文件，避免重复下载 */
    private fun wallpaperFileName(url: String): String {
        val hex = try {
            MessageDigest.getInstance("MD5").digest(url.toByteArray())
                .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        } catch (_: Exception) {
            (url.hashCode().toLong() and 0xFFFFFFFFL).toString(16)
        }
        return hex + ".jpg"
    }

    /**
     * 下载远程背景图到 filesDir/chat_wallpapers/<hash>.jpg
     *
     * @return 本地文件绝对路径；下载 / 写入失败返回 null（调用方保留原始 URL 兜底）
     */
    private fun downloadWallpaperToFile(url: String): String? = try {
        val dir = wallpaperDir() ?: return null
        val target = File(dir, wallpaperFileName(url))
        // OSS 对原图直接返回 403（AccessDenied: Forbidden access to the original image），
        // 与 AsyncNetworkImage 一致：优先取 style/zerotalk_thumb 缩略图，再回退原图
        val thumbUrl = NetworkImageUrl.applyOssStyle(url, NetworkImageUrl.OssStyle.Thumb)
        val candidates = if (thumbUrl != url) listOf(thumbUrl, url) else listOf(url)
        var bytes: ByteArray? = null
        for (candidate in candidates) {
            val fetched = fetchWallpaperBytes(candidate)
            if (fetched != null && fetched.isNotEmpty()) {
                bytes = fetched
                break
            }
        }
        if (bytes == null || bytes.isEmpty()) {
            null
        } else {
            // 同名同长度视为同一张图，跳过重复写入
            if (!target.exists() || target.length() != bytes.size.toLong()) target.writeBytes(bytes)
            target.absolutePath
        }
    } catch (e: Exception) {
        ZtLog.w("ZeroTalk", "[Wallpaper] 背景图下载异常：" + e.message)
        null
    }

    /** 拉取背景图字节；非 2xx 返回 null */
    private fun fetchWallpaperBytes(url: String): ByteArray? = try {
        val request = Request.Builder()
            .url(url)
            // 与图片加载保持一致：带上来源页，避免对象存储防盗链拦截
            .header("Referer", ZeroTalkApiService.BASE_URL + "/")
            .build()
        apiService.client.newCall(request).execute().use { response ->
            val body = if (response.isSuccessful) response.body?.bytes() else null
            ZtLog.d("ZeroTalk", "[Wallpaper] fetch code=" + response.code + " bytes=" + (body?.size ?: -1))
            body
        }
    } catch (e: Exception) {
        ZtLog.w("ZeroTalk", "[Wallpaper] 背景图下载异常：" + e.message)
        null
    }

    /**
     * 删除不再被任何范围引用的本地背景文件
     *
     * 引用判定必须同时看全局默认值与会话级覆盖值（两者都镜像在 [wallpaperCache] 里），
     * 否则会把仍被全局默认引用的文件删掉。只删本 App 背景目录内的文件，避免误删用户文件。
     */
    @Synchronized
    private fun deleteLocalWallpaperIfUnused(value: String?) {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return
        if (wallpaperCache.values.any { it == raw }) return
        val file = localWallpaperFile(raw) ?: return
        val dir = wallpaperDir() ?: return
        try {
            if (file.parentFile?.absolutePath == dir.absolutePath && file.exists()) file.delete()
        } catch (_: Exception) {
        }
    }

    private val _moments = MutableStateFlow<List<MomentItem>>(emptyList())
    val moments: StateFlow<List<MomentItem>> = _moments.asStateFlow()

    private val _momentsFeatured = MutableStateFlow<List<MomentItem>>(emptyList())
    val momentsFeatured: StateFlow<List<MomentItem>> = _momentsFeatured.asStateFlow()

    private val _momentsFollowing = MutableStateFlow<List<MomentItem>>(emptyList())
    val momentsFollowing: StateFlow<List<MomentItem>> = _momentsFollowing.asStateFlow()

    private val _momentsMine = MutableStateFlow<List<MomentItem>>(emptyList())
    val momentsMine: StateFlow<List<MomentItem>> = _momentsMine.asStateFlow()

    // ---- 捞动态（POST /moment/fish 捞一条 + GET /api/moment/fish-history 捞取记录） ----

    private val _fishedMoment = MutableStateFlow<MomentItem?>(null)
    val fishedMoment: StateFlow<MomentItem?> = _fishedMoment.asStateFlow()

    private val _isFishing = MutableStateFlow(false)
    val isFishing: StateFlow<Boolean> = _isFishing.asStateFlow()

    private val _fishHistory = MutableStateFlow<List<MomentItem>>(emptyList())
    val fishHistory: StateFlow<List<MomentItem>> = _fishHistory.asStateFlow()

    private val _fishHistoryHasMore = MutableStateFlow(false)
    val fishHistoryHasMore: StateFlow<Boolean> = _fishHistoryHasMore.asStateFlow()

    private val _isFishHistoryLoading = MutableStateFlow(false)
    val isFishHistoryLoading: StateFlow<Boolean> = _isFishHistoryLoading.asStateFlow()

    /** 捞取记录分页游标（服务端下发的 next_before_id） */
    private var fishHistoryCursor: Long? = null

    private val _currentMomentsCategory = MutableStateFlow(MomentsCategory.FEATURED)
    val currentMomentsCategory: StateFlow<MomentsCategory> = _currentMomentsCategory.asStateFlow()

    private val _currentMomentsSort = MutableStateFlow(MomentsSort.LATEST)
    val currentMomentsSort: StateFlow<MomentsSort> = _currentMomentsSort.asStateFlow()

    // ---- 他人主页（GET /api/moment/user 聚合：资料 + 计数 + 关系 + 动态） ----

    private val _otherUserProfile = MutableStateFlow<OtherUserProfileState?>(null)
    val otherUserProfile: StateFlow<OtherUserProfileState?> = _otherUserProfile.asStateFlow()

    private val _otherUserFollowList = MutableStateFlow<OtherUserFollowListState?>(null)
    val otherUserFollowList: StateFlow<OtherUserFollowListState?> = _otherUserFollowList.asStateFlow()

    /** 他人动态分页游标（服务端 next_before_id） */
    private var otherUserMomentsCursor: Long? = null

    /** 他人动态热度游标（服务端 next_before_score，官方 userFeed 与 before_id 同时回传） */
    private var otherUserMomentsScoreCursor: Double? = null

    /**
     * 全站唯一的「资料页层级栈」
     *
     * 资料页可被层层下钻（动态作者 → 关注列表 → 次级主页 → …）。所有入口都只能通过
     * [openUserProfileLayer] / [pushUserProfileLayer] 产生一层，UI 各渲染点只从本栈取自己要画的那一层，
     * 因此「当前打开了哪几层资料页」只有一个事实来源：结构上不可能再出现同一个资料页被叠成两层
     * （历史上「要按两次返回」的根因），返回也只由栈顶那一层拦截。
     */
    private val _userProfileLayers = MutableStateFlow<List<UserProfileLayer>>(emptyList())
    val userProfileLayers: StateFlow<List<UserProfileLayer>> = _userProfileLayers.asStateFlow()

    /** 他人主页动态排序（最新 / 最热），对齐官方 UserMomentsView 的排序胶囊 */
    private val _otherUserMomentsSort = MutableStateFlow(MomentsSort.LATEST)
    val otherUserMomentsSort: StateFlow<MomentsSort> = _otherUserMomentsSort.asStateFlow()

    /** 关注 / 粉丝列表分页游标，按 kind 分别记录 */
    private val otherUserFollowCursor = mutableMapOf<FollowListKind, Long?>()

    /** 标记是否正在回退出栈，防止组件销毁时触发的副作用误清空列表 */
    var isPoppingProfile: Boolean = false
        private set

    /** 主页请求轮次：切换查看对象后据此丢弃过期响应 */
    private var otherUserProfileToken = 0

    private val _privacySettings = MutableStateFlow<MomentsPrivacyData>(MomentsPrivacyData())
    val privacySettings: StateFlow<MomentsPrivacyData> = _privacySettings.asStateFlow()

    private val _blockList = MutableStateFlow<List<BlockedUserDto>>(emptyList())
    val blockList: StateFlow<List<BlockedUserDto>> = _blockList.asStateFlow()

    private val _reportList = MutableStateFlow<List<ReportItemDto>>(emptyList())
    val reportList: StateFlow<List<ReportItemDto>> = _reportList.asStateFlow()

    /** 举报记录总数（官网「共 N 条」） */
    private val _reportTotal = MutableStateFlow(0)
    val reportTotal: StateFlow<Int> = _reportTotal.asStateFlow()

    /** 是否还有下一页（官网「加载更多」按钮显隐） */
    private val _reportHasMore = MutableStateFlow(false)
    val reportHasMore: StateFlow<Boolean> = _reportHasMore.asStateFlow()

    private val _reportLoadingMore = MutableStateFlow(false)
    val reportLoadingMore: StateFlow<Boolean> = _reportLoadingMore.asStateFlow()

    private var _reportPage = 1

    /** 举报记录分页大小（对齐官网 MyReportsView 的 `per_page=10`） */
    private const val REPORT_PAGE_SIZE = 10

    // ---- 安全中心 ----

    private val _securityDevices = MutableStateFlow<List<SecurityDeviceDto>>(emptyList())
    val securityDevices: StateFlow<List<SecurityDeviceDto>> = _securityDevices.asStateFlow()

    private val _securityDevicesLoading = MutableStateFlow(false)
    val securityDevicesLoading: StateFlow<Boolean> = _securityDevicesLoading.asStateFlow()

    private val _securityLoginLogs = MutableStateFlow<List<SecurityLoginLogDto>>(emptyList())
    val securityLoginLogs: StateFlow<List<SecurityLoginLogDto>> = _securityLoginLogs.asStateFlow()

    private val _securityLoginTotal = MutableStateFlow(0)
    val securityLoginTotal: StateFlow<Int> = _securityLoginTotal.asStateFlow()

    /** 安全日志表是否就绪（table_ready === false 时官方展示专门文案） */
    private val _securityLoginTableReady = MutableStateFlow(true)
    val securityLoginTableReady: StateFlow<Boolean> = _securityLoginTableReady.asStateFlow()

    private val _securityLoginLoading = MutableStateFlow(false)
    val securityLoginLoading: StateFlow<Boolean> = _securityLoginLoading.asStateFlow()

    /** 当前登录历史的事件筛选（空串 = 全部事件） */
    private val _securityLoginEventType = MutableStateFlow("")
    val securityLoginEventType: StateFlow<String> = _securityLoginEventType.asStateFlow()

    /** 登录历史已请求到的页码（滚动到底继续 +1） */
    private var securityLoginPage = 1

    // ---- 处罚减免 ----

    private val _penaltyBootstrap = MutableStateFlow<PenaltyAppealBootstrapData?>(null)
    val penaltyBootstrap: StateFlow<PenaltyAppealBootstrapData?> = _penaltyBootstrap.asStateFlow()

    private val _penaltyLoading = MutableStateFlow(false)
    val penaltyLoading: StateFlow<Boolean> = _penaltyLoading.asStateFlow()

    // ---- 网易云绑定 ----

    private val _neteaseBinding = MutableStateFlow<NeteaseBindingData?>(null)
    val neteaseBinding: StateFlow<NeteaseBindingData?> = _neteaseBinding.asStateFlow()

    private val _neteaseLoading = MutableStateFlow(false)
    val neteaseLoading: StateFlow<Boolean> = _neteaseLoading.asStateFlow()

    private var eventListeningJob: Job? = null

    /** WS 自动重连任务（同一时刻只允许一个） */
    private var wsReconnectJob: Job? = null

    /** 连接状态看门狗是否已启动 */
    private var wsWatchdogStarted = false

    /** 主动断开（退出登录）期间抑制自动重连 */
    @Volatile
    private var suppressWsReconnect = false

    /**
     * 用户当前正在查看的会话（由 UI 在进入 / 退出聊天页时设置）
     *
     * 只有「前台 + 正在看这个房间」才抑制横幅（对齐官网 declaredViewingRoomId）。
     * **不能**用 wsClient.currentRoomId：它只在 join/leave 时变，退出聊天页后仍是旧房间，
     * 会让该房间的通知被永久吞掉。
     */
    @Volatile
    private var viewingRoomId: String? = null

    /** 横幅去重键（notification_id / message_id），chat_banner 与 message 两个通道共用 */
    private val recentBannerKeys = LinkedHashSet<String>()

    /** notification_unread 触发的房间列表刷新节流 */
    private var lastUnreadRefreshAtMs = 0L

    /** WS 事件处理累计失败次数（诊断用：连接还在但事件停更时能看出问题） */
    @Volatile
    var wsEventFailures: Int = 0
        private set

    init {
        // 监听 WebSocket 事件
        eventListeningJob = scope.launch {
            wsClient.events.collect { event ->
                // 单条事件处理异常不能拖垮整个事件循环：一旦抛错，
                // 连接还在但后续所有消息/通知/未读都不再更新
                runCatching { handleWsEvent(event) }
                    .onFailure {
                        wsEventFailures++
                        ZtLog.w("ZeroTalk", "handleWsEvent failed(#$wsEventFailures): ${it.message}")
                    }
            }
        }
        ensureWsWatchdog()
        // 系统通知：进程级投递（不依赖 UI 组合）。App 在后台时把新消息送到通知栏，
        // 这样即使 Activity 已销毁、只剩保活服务维持进程，也照样能收到提醒。
        scope.launch {
            _newMessageNotification.collect { notification ->
                if (SystemNotificationBridge.isAppInForeground) return@collect
                val title = if (notification.isGroup) {
                    notification.roomName
                } else {
                    notification.senderName.ifBlank { notification.roomName }
                }
                ZtLog.d("ZeroTalk", "[NOTIFY] post title=$title roomId=${notification.roomId}")
                SystemNotificationBridge.postMessage(
                    title = title,
                    body = notification.content,
                    roomId = notification.roomId
                )
            }
        }
    }

    /**
     * 判断 bootstrap 失败是否为「服务端明确拒绝会话」
     *
     * 只有明确的鉴权失败才允许清理本地会话；网络异常一律保留，
     * 否则一次断网就会把用户登出（真机实测复现）。
     */
    private fun isSessionRejected(error: Throwable?): Boolean {
        val msg = error?.message.orEmpty()
        return msg.contains("HTTP Error: 401") ||
            msg.contains("HTTP Error: 403") ||
            msg.contains("请先登录") ||
            msg.contains("未登录") ||
            msg.contains("登录已失效") ||
            msg.contains("登录状态已失效")
    }

    /**
     * 确保客户端连通（登录 + Bootstrap + WS）
     *
     * 账号密码由登录表单传入（工程内不保留任何默认账号）。
     * 登录成功后会把账号密码写入本地，作为后续冷启动会话过期时的静默续登兜底。
     */
    fun ensureConnected(
        username: String,
        password: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        // 只有 WS 真正认证通过（收到 auth_success）才算「已连接」；
        // 否则继续走登录/重连，避免卡在「显示已连接却收发不了」的假连接状态
        if (_status.value is ClientStatus.Connected &&
            wsClient.connectionState.value == WsConnectionState.AUTHENTICATED
        ) {
            onSuccess?.invoke()
            return
        }
        // 冷启动恢复会话进行中，忽略手动登录，避免并发登录
        if (_isRestoringSession.value) return
        if (username.isBlank() || password.isBlank()) {
            onError?.invoke("请输入账号与密码")
            return
        }

        scope.launch {
            val result = loginAndBootstrap(
                username = username,
                password = password,
                rememberCredential = true
            )
            if (result.isSuccess) {
                onSuccess?.invoke()
            } else {
                onError?.invoke(result.exceptionOrNull()?.message ?: "登录失败")
            }
        }
    }

    /**
     * 冷启动恢复登录态（双保险）
     *
     * 1. 先复用本地持久化的会话 Cookie 直接校验（[ZeroTalkApiService.bootstrap]），
     *    成功即恢复登录态，不需要重新登录；
     * 2. 若服务端会话已失效，则用本地保存的账号密码静默重新登录；
     * 3. 两者都失败时安静地回到未登录状态（不弹错误提示，用户可在「我的」页手动登录）。
     *
     * @param onResult true 表示已恢复登录态
     */
    fun restoreSession(onResult: ((Boolean) -> Unit)? = null) {
        if (_loginData.value != null) {
            onResult?.invoke(true)
            return
        }
        if (_isRestoringSession.value) return
        _isRestoringSession.value = true

        scope.launch(Dispatchers.IO) {
            try {
                val savedCookies = SessionStore.loadCookies()
                val savedCredential = SessionStore.loadCredential()
                if (savedCookies.isNullOrBlank() && savedCredential == null) {
                    onResult?.invoke(false)
                    return@launch
                }

                // 1. 复用本地会话 Cookie
                if (!savedCookies.isNullOrBlank()) {
                    apiService.cookieJar.importFromJson(savedCookies)
                    _status.value = ClientStatus.Bootstrapping
                    val bootstrapResult = apiService.bootstrap()
                    val bootstrap = bootstrapResult.getOrNull()
                    val user = bootstrap?.user
                    if (bootstrap != null && user != null &&
                        applyBootstrap(
                            bootstrap = bootstrap,
                            wsUserId = user.id,
                            fallbackUsername = user.username,
                            fallbackLoginName = user.loginName
                        )
                    ) {
                        onResult?.invoke(true)
                        return@launch
                    }
                    // 关键：bootstrap 失败**不等于**会话失效——断网 / DNS / 超时同样会失败。
                    // 之前无条件清 Cookie，实测真机断一次网就把 zt_session_cookies.txt 删掉、用户被登出。
                    // 只有服务端明确拒绝鉴权时才清理本地会话，其余保留待网络恢复后重试。
                    if (isSessionRejected(bootstrapResult.exceptionOrNull())) {
                        apiService.cookieJar.clear()
                        SessionStore.clearCookies()
                    } else {
                        ZtLog.d("ZeroTalk", "[Auth] bootstrap 失败但非鉴权拒绝，保留本地会话待重试")
                    }
                }

                // 2. 会话失效兜底：用本地账号密码静默续登
                val restored = if (savedCredential != null) {
                    loginAndBootstrap(
                        username = savedCredential.first,
                        password = savedCredential.second,
                        rememberCredential = false,
                        silent = true
                    ).isSuccess
                } else {
                    false
                }

                if (!restored) {
                    // 静默恢复失败：回到未登录状态，且不弹错误提示。
                    // 同样不无条件清 Cookie：网络恢复后 onNetworkAvailable 会再试一次。
                    _status.value = ClientStatus.Idle
                }
                onResult?.invoke(restored)
            } finally {
                _isRestoringSession.value = false
            }
        }
    }

    /**
     * 登录 + Bootstrap + 连接 WebSocket（登录态落地的唯一入口）
     *
     * @param rememberCredential 是否把账号密码写入本地（仅手动登录为 true，静默续登不重复写）
     * @param silent 静默模式：失败时不进入 Error 状态，避免冷启动弹出无意义的错误提示
     */
    private suspend fun loginAndBootstrap(
        username: String,
        password: String,
        rememberCredential: Boolean,
        silent: Boolean = false
    ): Result<Unit> {
        fun fail(message: String): Result<Unit> {
            _status.value = if (silent) ClientStatus.Idle else ClientStatus.Error(message)
            return Result.failure(IOException(message))
        }

        try {
            // 1. 登录
            _status.value = ClientStatus.LoggingIn
            val loginResult = apiService.login(username, password)
            if (loginResult.isFailure) {
                return fail(loginResult.exceptionOrNull()?.message ?: "登录失败")
            }
            val login = loginResult.getOrThrow()
            _loginData.value = login

            // 2. Bootstrap 获取全局配置与 ws_token
            _status.value = ClientStatus.Bootstrapping
            val bootstrapResult = apiService.bootstrap()
            if (bootstrapResult.isFailure) {
                return fail(bootstrapResult.exceptionOrNull()?.message ?: "Bootstrap 失败")
            }

            // 3. 落地登录态并连接 WebSocket
            val connected = applyBootstrap(
                bootstrap = bootstrapResult.getOrThrow(),
                wsUserId = login.userId,
                fallbackUsername = login.username,
                fallbackLoginName = login.loginName
            )
            if (!connected) {
                return fail("未获取到有效 ws_token")
            }

            if (rememberCredential) {
                SessionStore.saveCredential(username, password)
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            return fail(e.message ?: "网络连接异常")
        }
    }

    /**
     * 落地 Bootstrap 结果：刷新个人资料、记录公共大厅、连接 WS 并静默预加载
     *
     * @return 是否成功进入已连接状态（ws_token 缺失时返回 false）
     */
    private fun applyBootstrap(
        bootstrap: BootstrapData,
        wsUserId: Long,
        fallbackUsername: String,
        fallbackLoginName: String
    ): Boolean {
        _bootstrapData.value = bootstrap
        bootstrap.onlineUsers?.let { _onlineUsersCount.value = it }

        // 语音通话服务端配置（是否开放 + 各阶段超时；ICE 服务器在语音事件里逐次下发）
        bootstrap.voiceCall?.let { cfg ->
            voiceCall.updateConfig(
                VoiceCallConfig(
                    enabled = cfg.enabled,
                    forceTurn = cfg.forceTurn,
                    iceTransportPolicy = cfg.iceTransportPolicy,
                    ringTimeoutMs = cfg.ringTimeoutSeconds.coerceAtLeast(MIN_VOICE_TIMEOUT_SECONDS) * 1000L,
                    connectTimeoutMs = cfg.connectTimeoutSeconds.coerceAtLeast(MIN_VOICE_TIMEOUT_SECONDS) * 1000L,
                    reconnectTimeoutMs = cfg.reconnectTimeoutSeconds.coerceAtLeast(MIN_VOICE_TIMEOUT_SECONDS) * 1000L
                )
            )
        }
        // 启动/重连后若服务端仍有进行中的通话，自动恢复（官网 resume_start source=server）
        scope.launch { voiceCall.resumeIfNeeded() }

        // 更新个人资料状态
        val user = bootstrap.user
        if (user != null) {
            // 会话 Cookie 恢复路径没有登录响应，用 bootstrap 用户信息补齐登录态
            // （各业务动作的「请先登录」校验依赖 _loginData，不能为空）
            if (_loginData.value == null) {
                _loginData.value = LoginData(
                    userId = user.id,
                    uid = user.uid,
                    loginName = user.loginName,
                    username = user.username,
                    gender = user.gender,
                    location = user.location
                )
            }

            val rawAvatar = user.avatarUrl ?: ""
            val resolvedAvatar = NetworkImageUrl.resolve(rawAvatar)
            _userProfile.value = UserProfile(
                name = user.username.ifBlank { fallbackUsername },
                avatarUrl = resolvedAvatar,
                gender = if (user.gender == "female") "女" else "男",
                ageRange = user.ageRange ?: "18-23",
                userId = user.id.toString(),
                bio = user.bio ?: "在零语，遇见同频的灵魂",
                isLoggedIn = true,
                patText = user.patText ?: "",
                loginName = user.loginName.ifBlank { fallbackLoginName },
                qq = user.qq ?: "",
                avatarUpload = user.avatarUpload,
                hasCustomAvatar = user.hasCustomAvatar || (user.avatarUpload?.hasCustom == true),
                location = user.location.orEmpty()
            )
        }

        // 提取公共大厅 room_id
        bootstrap.publicRooms?.firstOrNull()?.roomId?.let { id ->
            if (id.isNotBlank()) _hallRoomId.value = id
        }

        // 连接 WebSocket
        val wsToken = bootstrap.wsToken
        if (wsToken.isNullOrBlank()) return false

        _status.value = ClientStatus.ConnectingWs
        wsClient.connect(wsUserId, wsToken)
        // 这里不能直接置 Connected：connect() 只是发起异步握手，
        // 真正可用以服务端回 auth_success 为准（由 ensureWsWatchdog 统一置位），
        // 否则握手/认证失败也会显示「已连接」而实际收发不了

        // 静默预加载动态与真实会话列表
        fetchMoments()
        fetchRoomList()
        fetchBlockList()
        fetchReportList()
        return true
    }

    /**
     * 退出登录并返回无账号初始状态
     */
    fun logout() {
        // 主动断开期间禁止看门狗自动重连
        suppressWsReconnect = true
        wsClient.disconnect()
        apiService.cookieJar.clear()
        // 结束进行中的语音通话并释放 WebRTC 资源
        voiceCall.release()
        // 清除本地持久化登录态（会话 Cookie + 账号密码），确保退出后不会自动续登
        SessionStore.clear()
        _isRestoringSession.value = false
        _status.value = ClientStatus.Idle
        _loginData.value = null
        _bootstrapData.value = null
        _userProfile.value = UserProfile.NO_ACCOUNT
        _matchStatus.value = MatchStatus.Idle
        _roomPeers.value = emptyMap()
        _conversations.value = emptyList()
        _roomMessages.value = emptyMap()
        _privacySettings.value = MomentsPrivacyData()
        _blockList.value = emptyList()
        _reportList.value = emptyList()
        _reportTotal.value = 0
        _reportHasMore.value = false
        _reportLoadingMore.value = false
        _reportPage = 1
        // 「我的」页面新增面板的状态同样清零，避免换账号后串数据
        _securityDevices.value = emptyList()
        _securityLoginLogs.value = emptyList()
        _securityLoginTotal.value = 0
        _securityLoginTableReady.value = true
        _securityLoginEventType.value = ""
        securityLoginPage = 1
        _penaltyBootstrap.value = null
        _neteaseBinding.value = null
        _fishedMoment.value = null
        _fishHistory.value = emptyList()
        _fishHistoryHasMore.value = false
        _isFishHistoryLoading.value = false
        _isFishing.value = false
        fishHistoryCursor = null
        suppressWsReconnect = false
    }

    /**
     * 发起 1v1 随机匹配
     */
    fun startMatching(
        isVoice: Boolean = false,
        oppositeGenderOnly: Boolean = false,
        sameGenderOnly: Boolean = false
    ) {
        if (_loginData.value == null) {
            _matchStatus.value = MatchStatus.Error("当前为无账号状态，请先登录")
            return
        }
        val mode = if (isVoice) "voice" else "chat"
        val opposite = if (oppositeGenderOnly) 1 else 0
        val same = if (sameGenderOnly) 1 else 0

        _matchStatus.value = MatchStatus.Matching(isVoice)
        wsClient.startMatch(mode, opposite, same)
        startMatchTimeout()
    }

    /**
     * 匹配超时保护
     *
     * 对齐官网：按 bootstrap 下发的 `match_timeout_seconds`（不足 10 秒按 10 秒）计时，
     * 超时后自动 `cancel_match` 并切到超时态，避免一直挂在匹配队列里。
     */
    private fun startMatchTimeout() {
        matchTimeoutJob?.cancel()
        val timeoutSec = (_bootstrapData.value?.matchTimeoutSeconds ?: 30).coerceAtLeast(10)
        matchTimeoutJob = scope.launch {
            delay(timeoutSec * 1000L)
            if (_matchStatus.value is MatchStatus.Matching) {
                wsClient.cancelMatch()
                _matchStatus.value = MatchStatus.Timeout
            }
        }
    }

    /**
     * 取消 1v1 匹配
     */
    fun cancelMatching() {
        matchTimeoutJob?.cancel()
        matchTimeoutJob = null
        wsClient.cancelMatch()
        _matchStatus.value = MatchStatus.Idle
    }

    /**
     * 重置匹配状态到闲置
     */
    fun resetMatchStatus() {
        _matchStatus.value = MatchStatus.Idle
    }

    /**
     * 进入零语大厅
     */
    fun enterPublicHall() {
        scope.launch {
            val hallId = _hallRoomId.value
            // 调用 REST 验证房间
            val res = apiService.enterPublicHall(hallId)
            val roomId = res.getOrNull()?.roomId ?: hallId
            _hallRoomId.value = roomId
            // WS 进入大厅房间
            wsClient.joinRoom(roomId)
            // 拉取大厅历史聊天记录（此前进大厅只有实时流）
            loadHallHistory(roomId)
        }
    }

    /**
     * 退出大厅房间
     */
    fun leavePublicHall() {
        wsClient.leaveRoom()
    }

    /**
     * 发送大厅消息
     *
     * @param onResult 发送结果回调（false 表示 WebSocket 未就绪/发送失败，调用方应提示用户）
     */
    fun sendHallMessage(content: String, onResult: ((Boolean) -> Unit)? = null) {
        sendHallMessage(content, emptyList(), onResult)
    }

    fun sendHallMessage(
        content: String,
        mentionIds: List<String>,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        if (content.isBlank()) {
            onResult?.invoke(false)
            return
        }
        scope.launch {
            val currentHall = _hallRoomId.value

            // 1. 连接已断开时先重连（此前大厅表现为「发不出去」且无任何提示）
            val state = wsClient.connectionState.value
            if (state == WsConnectionState.DISCONNECTED || state == WsConnectionState.FAILED) {
                reconnectWebSocket()
            }

            // 2. 确保当前 WS 处于大厅房间，并给服务端留出 join_room 的处理时间
            if (wsClient.currentRoomId != currentHall) {
                wsClient.joinRoom(currentHall)
                delay(150)
            }

            // 3. 发送并在本机即时回显
            val success = wsClient.sendTextMessage(content, mentionIds = mentionIds)
            if (success) {
                val nowTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val myMsg = ChatMessage(
                    id = "hall_my_${System.currentTimeMillis()}",
                    senderId = "me",
                    content = content,
                    timestamp = nowTimeStr,
                    isMine = true,
                    timestampMs = System.currentTimeMillis(),
                    senderName = _userProfile.value.name,
                    senderAvatar = _userProfile.value.avatarUrl,
                    previewText = content
                )
                _hallMessages.value = _hallMessages.value + myMsg
            }
            onResult?.invoke(success)
        }
    }

    /** UI 进入 / 退出聊天页时同步「正在查看的会话」 */
    fun setViewingRoom(roomId: String?) {
        viewingRoomId = roomId?.takeIf { it.isNotBlank() }
    }

    /**
     * 横幅去重：同一条消息可能同时从 `chat_banner` 与 `message` 两个通道到达
     *
     * @return true 表示首次出现（应当弹），false 表示已经弹过
     */
    private fun markBannerSeen(key: String): Boolean {
        if (key.isBlank()) return true
        if (!recentBannerKeys.add(key)) return false
        if (recentBannerKeys.size > 80) {
            val iterator = recentBannerKeys.iterator()
            repeat(recentBannerKeys.size - 80) {
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }
        return true
    }

    /**
     * 全站聊天横幅（官网 `chat_banner`）
     *
     * 对齐官网 `AppLayout` 的 `ws.on("chat_banner", payload => chatBanner.enqueueFromWs(payload, 当前房间))`：
     * 服务端主动推送，不要求已 join 房间，是通知与未读的权威来源。
     */
    private fun handleChatBanner(event: WsServerEvent.ChatBanner) {
        if (event.roomId.isBlank()) return
        ZtLog.d("ZeroTalk", "[BANNER] room=${event.roomId} type=${event.roomType} reason=${event.reason} unread=${event.unread} preview=${event.preview}")

        // 去重：官网按 notification_id / message_id 去重；message 通道共用同一份键
        val dedupeKey = event.notificationId.ifBlank {
            if (event.messageId > 0) "m_${event.messageId}" else ""
        }
        if (!markBannerSeen(dedupeKey)) return
        if (event.messageId > 0) markBannerSeen("m_${event.messageId}")

        // 1) 会话未读本地 +1（随后 notification_unread 会用服务端值校正）
        if (event.unread) {
            _conversations.value = _conversations.value.map { conv ->
                if (conv.id == event.roomId) conv.copy(unreadCount = conv.unreadCount + 1) else conv
            }
        }

        // 2) 只有「前台 + 正在看这个房间」才抑制横幅；后台必须照常通知
        if (SystemNotificationBridge.isAppInForeground && event.roomId == viewingRoomId) return

        // 3) 应用内横幅；后台时由进程级收集器转投系统通知
        val isGroup = event.roomType != "dm" && event.roomType != "normal"
        _newMessageNotification.tryEmit(
            IncomingMessageNotification(
                roomName = event.roomName.ifBlank { "聊天房间" },
                senderName = if (isGroup) "" else event.senderName,
                content = event.preview.ifBlank { "[新消息]" },
                isGroup = isGroup,
                roomId = event.roomId
            )
        )

        // 4) 会话行缺失时补拉，保证会话行与未读出现
        if (_conversations.value.none { it.id == event.roomId }) fetchRoomList()
    }

    /**
     * WS 连接状态看门狗（全局唯一）
     *
     * 修复「打开显示已连接、实际没连上，只能重启 App」：
     * - 只有 [WsConnectionState.AUTHENTICATED]（服务端回过 auth_success）才把状态置为 Connected；
     * - 掉线 / 认证失败时，已登录状态下自动重连，而不是等用户手动操作或重启进程。
     */
    private fun ensureWsWatchdog() {
        if (wsWatchdogStarted) return
        wsWatchdogStarted = true
        scope.launch {
            wsClient.connectionState.collect { state ->
                ZtLog.d("ZeroTalk", "[WS] state=$state")
                when (state) {
                    WsConnectionState.AUTHENTICATED -> {
                        if (_loginData.value != null) _status.value = ClientStatus.Connected
                    }
                    WsConnectionState.FAILED,
                    WsConnectionState.DISCONNECTED -> {
                        if (!suppressWsReconnect && _loginData.value != null) {
                            _status.value = ClientStatus.ConnectingWs
                            scheduleWsReconnect()
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * 网络恢复回调（由平台侧网络监听触发）
     *
     * 断网期间退避会一直拉长，若只等下一次退避，最长要等 30s 才重连。
     * 网络一恢复就立刻打断退避、马上重连，体感与耗电都更好。
     */
    fun onNetworkAvailable() {
        if (suppressWsReconnect) return
        if (_loginData.value == null) {
            // 断网期间可能连「会话恢复」都没成功（本地会话现已保留）：网络恢复后补一次。
            // 注意 registerDefaultNetworkCallback 在**注册时会立刻回调一次**当前网络，
            // 冷启动时恢复流程通常已在跑（restoreSession 内部会静默早退），
            // 这种情形不属于「网络恢复」，不能记日志，否则每次冷启动都会出现误导性的
            // 「网络恢复，重试恢复登录态」。
            if (_isRestoringSession.value) return
            ZtLog.d("ZeroTalk", "[WS] 网络恢复，重试恢复登录态")
            restoreSession()
            return
        }
        if (wsClient.connectionState.value == WsConnectionState.AUTHENTICATED) return
        ZtLog.d("ZeroTalk", "[WS] 网络恢复，立即重连")
        wsReconnectJob?.cancel()
        wsReconnectJob = null
        _status.value = ClientStatus.ConnectingWs
        scheduleWsReconnect()
    }

    /** 自动重连：重新 Bootstrap 取新的 ws_token 再连，带退避；连上后等 auth_success */
    private fun scheduleWsReconnect() {
        if (wsReconnectJob?.isActive == true) return
        wsReconnectJob = scope.launch {
            var attempt = 0
            while (isActive && !suppressWsReconnect && _loginData.value != null) {
                if (wsClient.connectionState.value == WsConnectionState.AUTHENTICATED) return@launch
                attempt++
                ZtLog.d("ZeroTalk", "[WS] reconnect attempt=$attempt")
                reconnectWebSocket()
                // 等认证结果：12s 内没等到 auth_success 视为本次失败，继续退避重试
                val authed = withTimeoutOrNull(12_000L) {
                    wsClient.connectionState.first { it == WsConnectionState.AUTHENTICATED }
                    true
                }
                if (authed == true) return@launch
                // 退避 + 随机抖动：避免多端同时重连打爆服务端，也避免固定节奏被系统限流
                val backoff = minOf(30_000L, 1_500L * attempt)
                delay(backoff + Random.nextLong(0L, 1_000L))
            }
        }
    }

    /**
     * 用当前会话重新 Bootstrap 并重连 WebSocket
     */
    private suspend fun reconnectWebSocket(): Boolean {
        return try {
            val bootstrap = apiService.bootstrap().getOrNull()
            if (bootstrap == null) {
                ZtLog.d("ZeroTalk", "[WS] reconnect: bootstrap 失败（网络不可用或会话失效）")
                return false
            }
            val wsToken = bootstrap.wsToken
            val userId = _loginData.value?.userId ?: bootstrap.user?.id ?: 0L
            if (wsToken.isNullOrBlank() || userId <= 0L) {
                ZtLog.d("ZeroTalk", "[WS] reconnect: 缺少 ws_token/userId")
                return false
            }
            _bootstrapData.value = bootstrap
            bootstrap.onlineUsers?.let { _onlineUsersCount.value = it }
            _status.value = ClientStatus.ConnectingWs
            wsClient.connect(userId, wsToken)
            delay(300)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 刷新当前在线用户数 (GET /api/home/online)
     */
    fun fetchOnlineCount() {
        scope.launch {
            val res = apiService.getOnlineCount()
            res.getOrNull()?.let {
                _onlineUsersCount.value = it.onlineUsers
            }
        }
    }

    /**
     * 拍一拍发送结果，供 UI 给出精确反馈（官网同款分支）
     */
    enum class PatSendResult {
        /** 已发出，等待服务端回推文案 */
        SENT,
        /** 目标标识缺失/是 peer、me 这类占位值 */
        INVALID_TARGET,
        /** 不能拍自己 */
        SELF,
        /** 客户端 3 秒冷却中 */
        COOLDOWN,
        /** WebSocket 未连接 */
        NOT_CONNECTED;

        /**
         * 结果对应的用户提示文案（冷却/未连接两句与官网一致）
         */
        fun message(targetName: String = ""): String = when (this) {
            SENT -> if (targetName.isBlank()) "已拍一拍" else "已拍一拍 $targetName"
            SELF -> "不能拍自己"
            COOLDOWN -> "拍一拍冷却中，请稍后再试"
            INVALID_TARGET -> "对方信息不完整，暂时拍不了"
            NOT_CONNECTED -> "连接未就绪，请稍后再试"
        }
    }

    /** 拍一拍冷却窗口（官网源码 `ka = 3e3`） */
    private const val PAT_COOLDOWN_MS = 3_000L

    private var lastPatAtMs = 0L

    /**
     * 发送拍一拍
     *
     * 帧与守卫完全对齐官网：
     * `{"event":"message","type":"pat","content":"","target_user_id":"<uid|数字id>"}`
     * - 不能拍自己、目标不能是占位标识、3 秒冷却；
     * - **不做本地乐观回显**：文案（含被拍者后缀 `sfx`）由服务端回推，
     *   本地拼不出正确的 `t/sfx`，硬拼会显示成错误的主谓关系。
     *
     * @param targetUserId 目标 uid（32 位）或数字 user_id 字符串
     */
    fun sendPat(roomId: String, targetUserId: String): PatSendResult {
        val target = targetUserId.trim()
        if (roomId.isBlank() || target.isBlank() || target in REAL_USER_ID_PLACEHOLDERS) {
            return PatSendResult.INVALID_TARGET
        }

        val login = _loginData.value
        val myUid = login?.uid?.trim().orEmpty()
        val myUserId = login?.userId ?: 0L
        val targetIsMe = (myUid.isNotBlank() && target.equals(myUid, ignoreCase = true)) ||
            (myUserId > 0L && target == myUserId.toString())
        if (targetIsMe) return PatSendResult.SELF

        val now = System.currentTimeMillis()
        if (now - lastPatAtMs < PAT_COOLDOWN_MS) return PatSendResult.COOLDOWN

        if (wsClient.currentRoomId != roomId) wsClient.joinRoom(roomId)
        if (!wsClient.sendPat(target)) return PatSendResult.NOT_CONNECTED

        lastPatAtMs = now
        return PatSendResult.SENT
    }

    /**
     * 发送 1v1 房间消息
     */
    fun sendRoomMessage(
        roomId: String,
        content: String,
        quotedText: String? = null,
        quotedIsMine: Boolean? = null,
        quotedSenderName: String? = null,
        replyToId: Long? = null,
        mentionIds: List<String> = emptyList()
    ) {
        if (content.isBlank()) return
        if (wsClient.currentRoomId != roomId) {
            wsClient.joinRoom(roomId)
        }
        val success = wsClient.sendTextMessage(content, replyToId, mentionIds)
        if (success) {
            val nowTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val myMsg = ChatMessage(
                id = "room_${roomId}_${System.currentTimeMillis()}",
                senderId = "me",
                content = content,
                timestamp = nowTimeStr,
                isMine = true,
                timestampMs = System.currentTimeMillis(),
                senderName = _userProfile.value.name,
                senderAvatar = _userProfile.value.avatarUrl,
                quotedText = quotedText,
                quotedIsMine = quotedIsMine,
                quotedSenderName = quotedSenderName,
                previewText = content
            )
            val currentList = _roomMessages.value[roomId] ?: emptyList()
            _roomMessages.value = _roomMessages.value + (roomId to (currentList + myMsg))
            val existing = _conversations.value.find { it.id == roomId }
            if (existing != null) {
                val updated = existing.copy(
                    lastMessage = content,
                    timestamp = nowTimeStr
                )
                _conversations.value = listOf(updated) + _conversations.value.filter { it.id != roomId }
            }
        }
    }

    /**
     * 发送聊天图片（实测链路）
     *
     * `presign(upload_source=chat_image, room_id)` → `PUT OSS` → `bind` → WS `type=image`。
     */
    fun sendRoomImage(
        roomId: String,
        imageBytes: ByteArray,
        filename: String = "chat_image.jpg",
        onError: ((String) -> Unit)? = null
    ) {
        if (imageBytes.isEmpty()) {
            onError?.invoke("图片内容为空")
            return
        }
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再发送图片")
            return
        }
        scope.launch {
            val res = apiService.uploadChatImage(imageBytes, filename, roomId)
            val url = res.getOrNull()
            if (url.isNullOrBlank()) {
                onError?.invoke(res.exceptionOrNull()?.message ?: "图片上传失败")
                return@launch
            }
            appendLocalRoomMessage(
                roomId = roomId,
                content = "［图片］",
                isImage = true,
                imageUrl = url,
                preview = "[图片]",
                onError = onError,
                send = { wsClient.sendImageMessage(url) }
            )
        }
    }

    /**
     * 发送聊天语音（实测链路）
     *
     * `presign(upload_source=chat_audio + audio_source=record|file, room_id)` → `PUT OSS` → `bind`
     * → WS `{"type":"audio","audio_source":"record|file"}`。
     *
     * @param audioSource `record`（录音发送）/ `file`（语音文件发送）
     */
    fun sendRoomAudio(
        roomId: String,
        audioBytes: ByteArray,
        durationSec: Int,
        contentType: String = "audio/webm",
        fileExt: String = "webm",
        audioSource: String = "record",
        onError: ((String) -> Unit)? = null
    ) {
        if (audioBytes.isEmpty()) {
            onError?.invoke("录音内容为空")
            return
        }
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再发送语音")
            return
        }
        scope.launch {
            val res = apiService.uploadChatAudio(
                fileBytes = audioBytes,
                filename = "chat_audio_${System.currentTimeMillis()}.$fileExt",
                contentType = contentType,
                roomId = roomId,
                audioSource = audioSource
            )
            val url = res.getOrNull()
            if (url.isNullOrBlank()) {
                onError?.invoke(res.exceptionOrNull()?.message ?: "语音上传失败")
                return@launch
            }
            appendLocalRoomMessage(
                roomId = roomId,
                content = "[语音 ${durationSec}\"]",
                isVoice = true,
                voiceDurationSec = durationSec,
                audioUrl = url,
                audioSource = audioSource,
                preview = "[语音]",
                onError = onError,
                send = { wsClient.sendAudioMessage(url, audioSource) }
            )
        }
    }

    /** 上传成功后发送 WS 帧并写入本地乐观回显（图片 / 语音共用） */
    private fun appendLocalRoomMessage(
        roomId: String,
        content: String,
        isImage: Boolean = false,
        isVoice: Boolean = false,
        voiceDurationSec: Int = 0,
        imageUrl: String = "",
        audioUrl: String = "",
        audioSource: String = "",
        preview: String = content,
        onError: ((String) -> Unit)? = null,
        send: () -> Boolean
    ) {
        if (wsClient.currentRoomId != roomId) {
            wsClient.joinRoom(roomId)
        }
        if (!send()) {
            onError?.invoke("消息发送失败，请检查网络后重试")
            return
        }
        val nowMs = System.currentTimeMillis()
        val nowTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
        val myMsg = ChatMessage(
            id = "room_${roomId}_$nowMs",
            senderId = "me",
            content = content,
            timestamp = nowTimeStr,
            isMine = true,
            timestampMs = nowMs,
            senderName = _userProfile.value.name,
            senderAvatar = _userProfile.value.avatarUrl,
            isImage = isImage,
            imageUrl = imageUrl,
            isVoice = isVoice,
            voiceDurationSec = voiceDurationSec,
            audioUrl = audioUrl,
            audioSource = audioSource,
            previewText = preview
        )
        val currentList = _roomMessages.value[roomId] ?: emptyList()
        _roomMessages.value = _roomMessages.value + (roomId to (currentList + myMsg))
        val existing = _conversations.value.find { it.id == roomId }
        if (existing != null) {
            val updated = existing.copy(lastMessage = preview, timestamp = nowTimeStr)
            _conversations.value = listOf(updated) + _conversations.value.filter { it.id != roomId }
        }
    }

    /**
     * 发送聊天分享歌曲（实测 WS `type: "music"`）
     */
    fun sendRoomMusic(roomId: String, music: MomentMusic) {
        if (wsClient.currentRoomId != roomId) {
            wsClient.joinRoom(roomId)
        }
        val songJson = JsonObject().apply {
            addProperty("provider", "netease")
            addProperty("song_id", music.songId)
            addProperty("name", music.name)
            addProperty("artists", music.artists)
            addProperty("album", music.album)
            addProperty("cover_url", music.coverUrl)
        }.toString()

        val success = wsClient.sendMusicMessage(songJson)
        if (success) {
            val nowMs = System.currentTimeMillis()
            val nowTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
            val preview = "[歌曲] ${music.name}"
            val myMsg = ChatMessage(
                id = "room_${roomId}_$nowMs",
                senderId = "me",
                content = songJson,
                timestamp = nowTimeStr,
                isMine = true,
                timestampMs = nowMs,
                senderName = _userProfile.value.name,
                senderAvatar = _userProfile.value.avatarUrl,
                isMusic = true,
                musicData = music,
                previewText = preview
            )
            val currentList = _roomMessages.value[roomId] ?: emptyList()
            _roomMessages.value = _roomMessages.value + (roomId to (currentList + myMsg))
            val existing = _conversations.value.find { it.id == roomId }
            if (existing != null) {
                val updated = existing.copy(lastMessage = preview, timestamp = nowTimeStr)
                _conversations.value = listOf(updated) + _conversations.value.filter { it.id != roomId }
            }
        }
    }

    /**
     * 发送摇骰子（实测 WS `{"event":"message","type":"dice","content":""}`）
     */
    fun sendRoomDice(roomId: String) {
        if (wsClient.currentRoomId != roomId) {
            wsClient.joinRoom(roomId)
        }
        val success = wsClient.sendDice()
        if (success) {
            val nowMs = System.currentTimeMillis()
            val nowTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
            val preview = "[骰子]"
            val myMsg = ChatMessage(
                id = "room_${roomId}_$nowMs",
                senderId = "me",
                content = "",
                timestamp = nowTimeStr,
                isMine = true,
                timestampMs = nowMs,
                senderName = _userProfile.value.name,
                senderAvatar = _userProfile.value.avatarUrl,
                isDice = true,
                diceValue = 0,
                previewText = preview
            )
            val currentList = _roomMessages.value[roomId] ?: emptyList()
            _roomMessages.value = _roomMessages.value + (roomId to (currentList + myMsg))
            val existing = _conversations.value.find { it.id == roomId }
            if (existing != null) {
                val updated = existing.copy(lastMessage = preview, timestamp = nowTimeStr)
                _conversations.value = listOf(updated) + _conversations.value.filter { it.id != roomId }
            }
        }
    }

    /**
     * 发起棋牌游戏对局邀请（实测 WS `{"event":"{gameType}_create"}`）
     */
    fun sendRoomGameInvite(roomId: String, gameType: String, gameName: String) {
        if (wsClient.currentRoomId != roomId) {
            wsClient.joinRoom(roomId)
        }
        val success = wsClient.sendGameCreate(gameType)
        if (success) {
            val nowMs = System.currentTimeMillis()
            val nowTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
            val contentText = "发起了「$gameName」对战邀请"
            val myMsg = ChatMessage(
                id = "room_${roomId}_$nowMs",
                senderId = "me",
                content = contentText,
                timestamp = nowTimeStr,
                isMine = true,
                timestampMs = nowMs,
                senderName = _userProfile.value.name,
                senderAvatar = _userProfile.value.avatarUrl,
                previewText = contentText
            )
            val currentList = _roomMessages.value[roomId] ?: emptyList()
            _roomMessages.value = _roomMessages.value + (roomId to (currentList + myMsg))
            val existing = _conversations.value.find { it.id == roomId }
            if (existing != null) {
                val updated = existing.copy(lastMessage = contentText, timestamp = nowTimeStr)
                _conversations.value = listOf(updated) + _conversations.value.filter { it.id != roomId }
            }
        }
    }

    // ==================== 对战小游戏全局会话状态与双通道同步 ====================

    private val gson = Gson()
    private var gamePollJob: Job? = null
    private val _activeGameSession = MutableStateFlow<GameSessionDetail?>(null)
    val activeGameSession: StateFlow<GameSessionDetail?> = _activeGameSession.asStateFlow()

    private val _activeGameId = MutableStateFlow<Long?>(null)
    val activeGameId: StateFlow<Long?> = _activeGameId.asStateFlow()

    private val _activeGameType = MutableStateFlow<String?>(null)
    val activeGameType: StateFlow<String?> = _activeGameType.asStateFlow()

    private val _activeGameError = MutableStateFlow<String?>(null)
    val activeGameError: StateFlow<String?> = _activeGameError.asStateFlow()

    val currentUid: String?
        get() = _loginData.value?.uid

    val currentUserId: String?
        get() = _loginData.value?.uid ?: _loginData.value?.userId?.toString()

    suspend fun getMyGameSessions(): Result<List<MyGameSessionItem>> = apiService.getMyGameSessions()

    /**
     * 进入/打开指定游戏对局页，启动双通道状态同步与自适应 HTTP 轮询
     */
    fun openGameSession(gameType: String, gameId: Long) {
        _activeGameType.value = gameType
        _activeGameId.value = gameId
        gamePollJob?.cancel()
        gamePollJob = scope.launch {
            // 立即拉取一次对局详情
            apiService.getGameDetail(gameType, gameId).onSuccess { detail ->
                _activeGameSession.value = detail
            }
            // 依规范设置轮询周期：围棋 1600ms，谁是卧底 2500ms，其他棋类 8000ms
            val intervalMs = when (gameType) {
                GameType.GO -> 1600L
                GameType.UNDERCOVER -> 2500L
                else -> 8000L
            }
            while (isActive && _activeGameId.value == gameId) {
                delay(intervalMs)
                val current = _activeGameSession.value
                if (current != null && current.status !in listOf("waiting", "playing")) {
                    break // 对局已结束或已取消，停止轮询
                }
                apiService.getGameDetail(gameType, gameId).onSuccess { updated ->
                    _activeGameSession.value = resolveGameSession(_activeGameSession.value, updated)
                }
            }
        }
    }

    /**
     * 退出游戏对局页，停止轮询
     */
    fun closeGameSession() {
        gamePollJob?.cancel()
        gamePollJob = null
        _activeGameSession.value = null
        _activeGameId.value = null
        _activeGameType.value = null
        _activeGameError.value = null
    }

    /**
     * 遵循官方规范的棋盘防回滚冲突决议
     */
    fun resolveGameSession(current: GameSessionDetail?, new: GameSessionDetail): GameSessionDetail {
        if (current == null) return new
        // 1. 新 version 比旧 version 小 -> 丢弃，保留旧棋盘
        if (new.version < current.version) return current
        // 2. 旧 version > 0 但新 version 缺失或为 0 -> 丢弃
        if (current.version > 0 && new.version <= 0) return current
        // 3. 旧棋盘有子而新棋盘全空且 move_count > 0 -> 保留旧棋盘
        val curPieces = current.board.count { it != '.' && it != '0' }
        val newPieces = new.board.count { it != '.' && it != '0' }
        if (curPieces > 0 && newPieces == 0 && new.moveCount > 0) {
            return new.copy(board = current.board)
        }
        return new
    }

    /**
     * 接受应战 / 加入游戏对局
     */
    fun joinGame(gameType: String, gameId: Long) {
        wsClient.sendGameJoin(gameType, gameId)
        openGameSession(gameType, gameId)
    }

    /**
     * 五子棋落子
     */
    fun makeGobangMove(gameId: Long, x: Int, y: Int) {
        val ver = _activeGameSession.value?.version ?: 1
        wsClient.sendGobangMove(gameId, x, y, ver)
    }

    /**
     * 围棋落子
     */
    fun makeGoMove(gameId: Long, x: Int, y: Int) {
        val ver = _activeGameSession.value?.version ?: 1
        wsClient.sendGoMove(gameId, x, y, ver)
    }

    /**
     * 围棋停一手
     */
    fun makeGoPass(gameId: Long) {
        val ver = _activeGameSession.value?.version ?: 1
        wsClient.sendGoPass(gameId, ver)
    }

    /**
     * 中国象棋走子
     */
    fun makeXiangqiMove(gameId: Long, ff: Int, fr: Int, tf: Int, tr: Int) {
        val ver = _activeGameSession.value?.version ?: 1
        wsClient.sendXiangqiMove(gameId, ff, fr, tf, tr, ver)
    }

    /**
     * 国际象棋走子
     */
    fun makeChessMove(gameId: Long, ff: Int, fr: Int, tf: Int, tr: Int, promo: Int = 0) {
        val ver = _activeGameSession.value?.version ?: 1
        wsClient.sendChessMove(gameId, ff, fr, tf, tr, promo, ver)
    }

    /**
     * 对局认输
     */
    fun resignGame(gameType: String, gameId: Long) {
        wsClient.sendGameResign(gameType, gameId)
    }

    /**
     * 取消对局邀请
     */
    fun cancelGame(gameType: String, gameId: Long) {
        wsClient.sendGameCancel(gameType, gameId)
    }

    /**
     * 谁是卧底操作
     */
    fun undercoverReady(gameId: Long, ready: Boolean) {
        wsClient.sendUndercoverReady(gameId, ready)
    }

    fun undercoverStart(gameId: Long) {
        wsClient.sendUndercoverStart(gameId)
    }

    fun undercoverConfirm(gameId: Long) {
        wsClient.sendUndercoverConfirm(gameId)
    }

    fun undercoverDescribe(gameId: Long, text: String) {
        wsClient.sendUndercoverDescribe(gameId, text)
    }

    fun undercoverVote(gameId: Long, targetUserId: Long) {
        wsClient.sendUndercoverVote(gameId, targetUserId)
    }

    fun undercoverWhiteGuess(gameId: Long, guess: String) {
        wsClient.sendUndercoverWhiteGuess(gameId, guess)
    }

    fun undercoverLeave(gameId: Long) {
        wsClient.sendUndercoverLeave(gameId)
        closeGameSession()
    }

    fun sendGobangMove(x: Int, y: Int) {
        val gameId = _activeGameId.value ?: return
        makeGobangMove(gameId, x, y)
    }

    fun sendGoMove(x: Int, y: Int) {
        val gameId = _activeGameId.value ?: return
        makeGoMove(gameId, x, y)
    }

    fun sendGoPass() {
        val gameId = _activeGameId.value ?: return
        makeGoPass(gameId)
    }

    fun sendXiangqiMove(ff: Int, fr: Int, tf: Int, tr: Int) {
        val gameId = _activeGameId.value ?: return
        makeXiangqiMove(gameId, ff, fr, tf, tr)
    }

    fun sendChessMove(ff: Int, fr: Int, tf: Int, tr: Int, promo: Int = 0) {
        val gameId = _activeGameId.value ?: return
        makeChessMove(gameId, ff, fr, tf, tr, promo)
    }

    fun sendGameResign() {
        val type = _activeGameType.value ?: return
        val gameId = _activeGameId.value ?: return
        resignGame(type, gameId)
    }

    fun sendGameCancel() {
        val type = _activeGameType.value ?: return
        val gameId = _activeGameId.value ?: return
        cancelGame(type, gameId)
    }

    fun sendUndercoverReady(ready: Boolean) {
        val gameId = _activeGameId.value ?: return
        undercoverReady(gameId, ready)
    }

    fun sendUndercoverStart() {
        val gameId = _activeGameId.value ?: return
        undercoverStart(gameId)
    }

    fun sendUndercoverLeave() {
        val gameId = _activeGameId.value ?: return
        undercoverLeave(gameId)
    }

    fun sendUndercoverConfirm() {
        val gameId = _activeGameId.value ?: return
        undercoverConfirm(gameId)
    }

    fun sendUndercoverDescribe(text: String) {
        val gameId = _activeGameId.value ?: return
        undercoverDescribe(gameId, text)
    }

    fun sendUndercoverVote(targetUserId: Long) {
        val gameId = _activeGameId.value ?: return
        undercoverVote(gameId, targetUserId)
    }

    fun sendUndercoverWhiteGuess(guess: String) {
        val gameId = _activeGameId.value ?: return
        undercoverWhiteGuess(gameId, guess)
    }

    fun sendUndercoverKick(targetUserId: Long) {
        val gameId = _activeGameId.value ?: return
        wsClient.sendUndercoverKick(gameId, targetUserId)
    }

    fun sendUndercoverCancel() {
        val gameId = _activeGameId.value ?: return
        wsClient.sendGameCancel("undercover", gameId)
    }

    private fun mapMomentDtos(dtos: List<MomentItemDto>): List<MomentItem> {
        val myUid = _loginData.value?.uid
        val myUserId = _loginData.value?.userId?.toString() ?: _bootstrapData.value?.user?.id?.toString()
        return dtos.map { dto ->
            val isMine = (myUid != null && dto.uid != null && dto.uid == myUid) ||
                         (myUserId != null && dto.userId.toString() == myUserId)
            val imgList = dto.images?.filter { it.isNotBlank() } ?: emptyList()
            MomentItem(
                id = dto.id.toString(),
                authorId = dto.userId.toString(),
                authorUid = dto.uid.orEmpty(),
                authorName = dto.username.ifBlank { "零语匿友" },
                authorAvatar = dto.avatarUrl ?: "",
                authorGender = if (dto.gender == "female") "女" else "男",
                publishTime = formatRelativeTime(dto.createdAt),
                textContent = dto.content,
                imageUrl = imgList.firstOrNull() ?: "",
                images = imgList,
                audioUrl = NetworkImageUrl.resolve(dto.audioUrl),
                music = parseMomentMusic(dto.music),
                likesCount = dto.likeCount,
                commentsCount = dto.commentCount,
                isLiked = dto.liked,
                isFollowed = dto.isFollowing,
                isPinned = dto.isPinned == true,
                isPrivate = dto.isPrivate == true,
                audienceMode = dto.audienceMode ?: "all",
                audienceMutual = dto.audienceMutual ?: false,
                isMine = isMine,
                fishedAt = dto.fishedAtRaw?.takeIf { it.isNotBlank() }?.let { formatRelativeTime(it) } ?: ""
            )
        }
    }

    /**
     * 解析动态 `music` 字段：兼容服务端下发 JSON 对象 / JSON 字符串两种形态。
     */
    private fun parseMomentMusic(raw: com.google.gson.JsonElement?): MomentMusic? {
        if (raw == null || raw.isJsonNull) return null
        return try {
            val json = if (raw.isJsonPrimitive) {
                JsonParser.parseString(raw.asString)
            } else {
                raw
            }
            if (!json.isJsonObject) return null
            val obj = json.asJsonObject
            MomentMusic(
                provider = obj.get("provider")?.takeIf { it.isJsonPrimitive }?.asString ?: "netease",
                songId = obj.get("song_id")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                name = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                artists = obj.get("artists")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                album = obj.get("album")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                coverUrl = NetworkImageUrl.resolve(
                    obj.get("cover_url")?.takeIf { it.isJsonPrimitive }?.asString
                )
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun updateMomentInAllLists(transform: (MomentItem) -> MomentItem) {
        _moments.value = _moments.value.map(transform)
        _momentsFeatured.value = _momentsFeatured.value.map(transform)
        _momentsFollowing.value = _momentsFollowing.value.map(transform)
        _momentsMine.value = _momentsMine.value.map(transform)
        _fishedMoment.value = _fishedMoment.value?.let(transform)
        _fishHistory.value = _fishHistory.value.map(transform)
        // 他人主页内嵌的动态同样需要跟随点赞 / 关注状态变化
        _otherUserProfile.value = _otherUserProfile.value?.let { state ->
            val profile = state.profile ?: return@let state
            state.copy(profile = profile.copy(moments = profile.moments.map(transform)))
        }
    }

    private fun removeMomentFromAllLists(momentId: String) {
        _moments.value = _moments.value.filter { it.id != momentId }
        _momentsFeatured.value = _momentsFeatured.value.filter { it.id != momentId }
        _momentsFollowing.value = _momentsFollowing.value.filter { it.id != momentId }
        _momentsMine.value = _momentsMine.value.filter { it.id != momentId }
        if (_fishedMoment.value?.id == momentId) _fishedMoment.value = null
        _fishHistory.value = _fishHistory.value.filter { it.id != momentId }
        _otherUserProfile.value = _otherUserProfile.value?.let { state ->
            val profile = state.profile ?: return@let state
            state.copy(profile = profile.copy(moments = profile.moments.filter { it.id != momentId }))
        }
    }

    /**
     * 「调接口 → 成功 / 失败分支」统一封装。
     *
     * 消除各处重复的 `scope.launch { val res = ...; if (res.isSuccess) ... else ... }` 骨架：
     * 成功时把结果交给 [onSuccess]，失败时统一走 [onError]
     * （服务端未给消息时用 [fallbackMessage] 兜底）。
     *
     * 登录校验仍留在各调用方，因为不同操作的提示文案不同。
     */
    private fun <T> launchApiCall(
        fallbackMessage: String,
        onError: ((String) -> Unit)?,
        call: suspend () -> Result<T>,
        onSuccess: (T) -> Unit
    ) {
        scope.launch {
            val res = call()
            if (res.isSuccess) {
                onSuccess(res.getOrThrow())
            } else {
                onError?.invoke(
                    res.exceptionOrNull()?.message?.takeIf { it.isNotBlank() } ?: fallbackMessage
                )
            }
        }
    }

    /**
     * 按分类与排序获取动态列表（精选、关注、我的；最新、最热）
     */
    fun fetchMomentsByCategory(
        category: MomentsCategory = _currentMomentsCategory.value,
        sort: MomentsSort = _currentMomentsSort.value,
        limit: Int = 20,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        _currentMomentsCategory.value = category
        _currentMomentsSort.value = sort
        scope.launch {
            val result = when (category) {
                MomentsCategory.FEATURED -> apiService.getMomentsFeed(limit = limit, sort = sort.paramValue)
                MomentsCategory.FOLLOWING -> apiService.getMomentsFollowing(limit = limit, sort = sort.paramValue)
                MomentsCategory.MINE -> apiService.getMomentsMine(limit = limit, sort = sort.paramValue)
            }
            if (result.isSuccess) {
                val dtos = result.getOrThrow()
                val items = mapMomentDtos(dtos)
                when (category) {
                    MomentsCategory.FEATURED -> {
                        _momentsFeatured.value = items
                        _moments.value = items
                    }
                    MomentsCategory.FOLLOWING -> {
                        _momentsFollowing.value = items
                    }
                    MomentsCategory.MINE -> {
                        _momentsMine.value = items
                    }
                }
                onSuccess?.invoke()
            } else {
                val err = result.exceptionOrNull()?.message ?: "获取动态列表失败"
                onError?.invoke(err)
            }
        }
    }

    /**
     * 获取最新动态 Feed（默认精选）
     */
    fun fetchMoments(limit: Int = 10) {
        fetchMomentsByCategory(MomentsCategory.FEATURED, _currentMomentsSort.value, limit)
    }

    /**
     * 捞一条动态 (POST /moment/fish)
     *
     * 「换一条」与打开捞动态面板都走这里：每次调用即向服务端重新捞取一条，
     * 服务端同时把它记入捞取历史（捞取记录页可见）。
     *
     * @param onEmpty 服务端没有可捞的新动态时回调
     */
    fun fishMoment(
        onSuccess: ((MomentItem) -> Unit)? = null,
        onEmpty: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再捞动态")
            return
        }
        if (_isFishing.value) return
        _isFishing.value = true
        scope.launch {
            val result = apiService.fishMoment()
            _isFishing.value = false
            if (result.isSuccess) {
                val dto = result.getOrThrow().moment
                val item = dto?.let { mapMomentDtos(listOf(it)).firstOrNull() }
                if (item != null) {
                    _fishedMoment.value = item
                    onSuccess?.invoke(item)
                } else {
                    onEmpty?.invoke()
                }
            } else {
                onError?.invoke(result.exceptionOrNull()?.message ?: "捞取失败，请稍后再试")
            }
        }
    }

    /**
     * 捞取记录 (GET /api/moment/fish-history)
     *
     * @param reset true 表示从第一页重新加载（打开面板时使用），false 表示续接下一页
     */
    fun fetchFishHistory(
        reset: Boolean = false,
        limit: Int = 12,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后查看捞取记录")
            return
        }
        if (_isFishHistoryLoading.value) return
        if (!reset && !_fishHistoryHasMore.value) return

        _isFishHistoryLoading.value = true
        scope.launch {
            val result = apiService.getFishHistory(
                beforeId = if (reset) null else fishHistoryCursor,
                limit = limit
            )
            _isFishHistoryLoading.value = false
            if (result.isSuccess) {
                val data = result.getOrThrow()
                val items = mapMomentDtos(data.list ?: emptyList())
                _fishHistory.value = if (reset) items else _fishHistory.value + items
                _fishHistoryHasMore.value = data.hasMore
                fishHistoryCursor = data.nextBeforeId
                onSuccess?.invoke()
            } else {
                onError?.invoke(result.exceptionOrNull()?.message ?: "获取捞取记录失败")
            }
        }
    }

    // ================================================================
    // 他人主页（GET /api/moment/user 聚合：资料 + 计数 + 关系 + 动态）
    // ================================================================

    /**
     * 主页接口的用户标识候选
     *
     * 实测文档：主页聚合与关注/拉黑接口的 user_id 参数取值是「对方 uid」；
     * 而动态卡片等历史实现沿用数字 user_id，这里两个标识依次尝试，避免因标识形态不符
     * 直接落到服务端的「暂时无法获取用户资料」。
     */
    private fun profileIdentifierCandidates(userId: String?, uid: String?): List<String> =
        listOf(uid.orEmpty().trim(), userId.orEmpty().trim())
            .filter { it.isNotBlank() && it != "0" }
            .distinct()

    /** 私信接口 (target_user_id) 优先使用数字 user_id，其次回落 uid */
    private fun dmIdentifierCandidates(userId: String?, uid: String?): List<String> =
        listOf(userId.orEmpty().trim(), uid.orEmpty().trim())
            .filter { it.isNotBlank() && it != "0" }
            .distinct()

    /**
     * 打开他人主页并拉取聚合资料
     *
     * @param userId 数字 user_id（会话项的 targetUserId）
     * @param uid 32 位 hex uid（会话项的 targetUid）
     * @param navigate 是否通知 UI 层导航打开全屏个人资料页面。聊天内部拉取资料时传 false。
     */
    fun openOtherUserProfile(
        userId: String?,
        uid: String? = null,
        name: String = "",
        avatarUrl: String = "",
        navigate: Boolean = true
    ) {
        val numericId = userId.orEmpty().trim()
        val hexUid = uid.orEmpty().trim()

        // navigate = true 表示这是一次「顶层入口打开资料页」：登记为资料页层级栈的第一层（APP 覆盖层），
        // 渲染与返回统一由层级栈决定；资料加载由 openUserProfileLayer 内部继续走下面的静默路径
        if (navigate) {
            // 两个标识都为空时不打开页面，避免叠出一层注定报错的资料页
            if (numericId.isNotBlank() || hexUid.isNotBlank()) {
                openUserProfileLayer(
                    target = UserProfileTarget(numericId, hexUid, name, avatarUrl),
                    host = UserProfileLayerHost.APP
                )
            }
            return
        }

        val candidates = profileIdentifierCandidates(numericId, hexUid)

        // 每次进入新主页时排序回到「最新」（对齐官方 UserMomentsView 的默认排序）
        _otherUserMomentsSort.value = MomentsSort.LATEST
        otherUserMomentsCursor = null
        otherUserMomentsScoreCursor = null
        otherUserProfileToken += 1
        val token = otherUserProfileToken

        if (candidates.isEmpty()) {
            _otherUserProfile.value = OtherUserProfileState(
                userId = numericId,
                uid = hexUid,
                isLoading = false,
                error = "缺少用户标识，暂时无法获取用户资料"
            )
            return
        }

        _otherUserProfile.value = OtherUserProfileState(
            userId = numericId,
            uid = hexUid,
            isLoading = true
        )

        scope.launch {
            val res = fetchProfileAggregateWithFallback(candidates)
            if (token != otherUserProfileToken) return@launch
            val state = _otherUserProfile.value ?: return@launch
            if (res.isSuccess) {
                val data = res.getOrThrow()
                otherUserMomentsCursor = data.nextBeforeId
                otherUserMomentsScoreCursor = data.nextBeforeScore
                val mappedProfile = mapOtherUserProfile(data, numericId, hexUid)
                _otherUserProfile.value = state.copy(
                    isLoading = false,
                    error = null,
                    profile = mappedProfile
                )
                syncUserProfileToConversations(mappedProfile)
            } else {
                _otherUserProfile.value = state.copy(
                    isLoading = false,
                    error = res.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                        ?: "暂时无法获取用户资料"
                )
            }
        }
    }

    /**
     * 静默加载他人聚合资料（用于聊天顶栏或首聊卡片获取对方资料，不触发全局 UI 导航跳转）
     */
    fun loadOtherUserProfileSilently(userId: String?, uid: String? = null) {
        openOtherUserProfile(userId = userId, uid = uid, navigate = false)
    }

    /**
     * 重新拉取当前主页资料（保持页面已有内容，避免闪白）
     *
     * @param showLoading 是否展示整页加载态（首次进入或点击重试时使用）
     */
    fun refreshOtherUserProfile(showLoading: Boolean = false) {
        val state = _otherUserProfile.value ?: return
        val candidates = profileIdentifierCandidates(state.userId, state.uid)
        if (candidates.isEmpty()) return

        otherUserProfileToken += 1
        val token = otherUserProfileToken
        if (showLoading) {
            _otherUserProfile.value = state.copy(isLoading = true, error = null)
        }

        scope.launch {
            val res = fetchProfileAggregateWithFallback(candidates)
            if (token != otherUserProfileToken) return@launch
            val latest = _otherUserProfile.value ?: return@launch
            if (res.isSuccess) {
                val data = res.getOrThrow()
                otherUserMomentsCursor = data.nextBeforeId
                otherUserMomentsScoreCursor = data.nextBeforeScore
                val mappedProfile = mapOtherUserProfile(data, latest.userId, latest.uid)
                _otherUserProfile.value = latest.copy(
                    isLoading = false,
                    error = null,
                    profile = mappedProfile
                )
                syncUserProfileToConversations(mappedProfile)
            } else if (latest.profile == null) {
                _otherUserProfile.value = latest.copy(
                    isLoading = false,
                    error = res.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                        ?: "暂时无法获取用户资料"
                )
            } else {
                _otherUserProfile.value = latest.copy(isLoading = false)
            }
        }
    }

    /** 上拉加载更早的他人动态（沿用主页聚合接口的 before_id 分页） */
    fun loadMoreOtherUserMoments() {
        val state = _otherUserProfile.value ?: return
        val profile = state.profile ?: return
        if (state.isLoadingMore || !profile.hasMoreMoments) return
        val beforeId = otherUserMomentsCursor
        val beforeScore = otherUserMomentsScoreCursor
        if (beforeId == null && beforeScore == null) return
        val candidates = profileIdentifierCandidates(state.userId, state.uid)
        if (candidates.isEmpty()) return

        val token = otherUserProfileToken
        _otherUserProfile.value = state.copy(isLoadingMore = true)

        scope.launch {
            val res = fetchProfileAggregateWithFallback(
                candidates,
                beforeId = beforeId,
                beforeScore = beforeScore
            )
            if (token != otherUserProfileToken) return@launch
            val latest = _otherUserProfile.value ?: return@launch
            val current = latest.profile ?: return@launch
            if (res.isSuccess) {
                val data = res.getOrThrow()
                otherUserMomentsCursor = data.nextBeforeId
                otherUserMomentsScoreCursor = data.nextBeforeScore
                val extra = mapMomentDtos(data.list ?: emptyList())
                _otherUserProfile.value = latest.copy(
                    isLoadingMore = false,
                    profile = current.copy(
                        moments = (current.moments + extra).distinctBy { it.id },
                        hasMoreMoments = data.hasMore
                    )
                )
            } else {
                _otherUserProfile.value = latest.copy(isLoadingMore = false)
            }
        }
    }

    /**
     * 关注 / 取消关注主页用户（/follow/add、/follow/remove）
     *
     * 官方语义：同一按钮 toggle，已关注再点即取消关注。先乐观更新三处状态
     * （主页、关注/粉丝列表、动态卡片），任一步失败即整体回滚。
     */
    fun toggleOtherUserFollow(onResult: ((Boolean, String?) -> Unit)? = null) {
        val state = _otherUserProfile.value
        val profile = state?.profile
        if (state == null || profile == null) {
            onResult?.invoke(false, "资料尚未加载完成，请稍后重试")
            return
        }
        if (profile.isSelf) {
            onResult?.invoke(false, "这是你自己的主页")
            return
        }
        if (profile.iBlocked) {
            onResult?.invoke(false, "已拉黑的用户无法关注，请先解除拉黑")
            return
        }
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录后再关注")
            return
        }
        val numericId = state.userId.ifBlank { profile.userId }
        val hexUid = state.uid.ifBlank { profile.uid }
        val target = !profile.isFollowing
        if (profileIdentifierCandidates(numericId, hexUid).isEmpty()) {
            onResult?.invoke(false, "暂时无法识别该用户")
            return
        }

        applyFollowStateEverywhere(numericId, hexUid, target)

        scope.launch {
            val res = setFollowWithFallback(numericId, hexUid, target)
            if (res.isSuccess) {
                // 关注数 / 粉丝数为服务端权威数据，操作成功后轻量刷新一次
                refreshOtherUserProfile()
                onResult?.invoke(true, null)
            } else {
                applyFollowStateEverywhere(numericId, hexUid, profile.isFollowing)
                onResult?.invoke(false, res.exceptionOrNull()?.message?.takeIf { it.isNotBlank() } ?: "关注操作失败，请稍后重试")
            }
        }
    }

    /**
     * 拉黑 / 解除拉黑主页用户（/block/add、/block/unblock）
     *
     * 官方语义：拉黑会自动取消关注、禁用关注与私信入口；解除拉黑后恢复可私信。
     */
    fun toggleOtherUserBlock(block: Boolean, onResult: ((Boolean, String?) -> Unit)? = null) {
        val state = _otherUserProfile.value
        val profile = state?.profile
        if (state == null || profile == null) {
            onResult?.invoke(false, "资料尚未加载完成，请稍后重试")
            return
        }
        if (profile.isSelf) {
            onResult?.invoke(false, "这是你自己的主页")
            return
        }
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录后再操作")
            return
        }
        val numericId = state.userId.ifBlank { profile.userId }
        val hexUid = state.uid.ifBlank { profile.uid }
        val candidates = profileIdentifierCandidates(numericId, hexUid)
        if (candidates.isEmpty()) {
            onResult?.invoke(false, "暂时无法识别该用户")
            return
        }

        scope.launch {
            var success = false
            var lastError: String? = null
            for (identifier in candidates) {
                val res = apiService.setBlock(identifier, block)
                if (res.isSuccess) {
                    success = true
                    break
                }
                lastError = res.exceptionOrNull()?.message
            }

            if (!success) {
                onResult?.invoke(false, lastError?.takeIf { it.isNotBlank() } ?: "操作失败，请稍后重试")
                return@launch
            }

            if (block) {
                // 拉黑自动取消关注：同步主页与所有列表中的关注状态
                applyFollowStateEverywhere(numericId, hexUid, false)
                _otherUserProfile.value = _otherUserProfile.value?.let { latest ->
                    latest.copy(
                        profile = latest.profile?.copy(
                            iBlocked = true,
                            isFollowing = false,
                            followedBy = false,
                            canDm = false,
                            dmDisabledReason = "已拉黑该用户"
                        )
                    )
                }
            } else {
                _otherUserProfile.value = _otherUserProfile.value?.let { latest ->
                    latest.copy(
                        profile = latest.profile?.copy(
                            iBlocked = false,
                            canDm = true,
                            dmDisabledReason = ""
                        )
                    )
                }
                // 解除后关系状态以服务端为准（对方是否仍关注我等）
                refreshOtherUserProfile()
            }
            fetchBlockList()
            onResult?.invoke(true, null)
        }
    }

    /**
     * 关注状态投放：主页 + 关注/粉丝列表 + 全部动态列表（动态卡片按数字 user_id 匹配作者）
     */
    private fun applyFollowStateEverywhere(numericUserId: String, uid: String, isFollowing: Boolean) {
        _otherUserProfile.value = _otherUserProfile.value?.let { state ->
            state.copy(profile = state.profile?.copy(isFollowing = isFollowing))
        }
        _otherUserFollowList.value = _otherUserFollowList.value?.let { listState ->
            listState.copy(
                list = listState.list.map { item ->
                    if (item.matches(numericUserId, uid)) {
                        item.copy(isFollowing = isFollowing, isMutual = isFollowing && item.isFollower)
                    } else {
                        item
                    }
                }
            )
        }
        // 同步层级栈中每一层快照里的关注/粉丝列表与主页状态，确保逐级返回时关注状态也是最新的
        _userProfileLayers.value = _userProfileLayers.value.map { layer ->
            val snapshot = layer.restoreSnapshot ?: return@map layer
            val updatedFollowList = snapshot.followListState?.let { listState ->
                listState.copy(
                    list = listState.list.map { item ->
                        if (item.matches(numericUserId, uid)) {
                            item.copy(isFollowing = isFollowing, isMutual = isFollowing && item.isFollower)
                        } else {
                            item
                        }
                    }
                )
            }
            val updatedProfile = snapshot.profileState?.let { state ->
                val prof = state.profile
                if (prof != null && (prof.userId == numericUserId || (uid.isNotBlank() && prof.uid == uid))) {
                    state.copy(profile = prof.copy(isFollowing = isFollowing))
                } else {
                    state
                }
            }
            if (updatedFollowList != snapshot.followListState || updatedProfile != snapshot.profileState) {
                layer.copy(
                    restoreSnapshot = snapshot.copy(
                        profileState = updatedProfile,
                        followListState = updatedFollowList
                    )
                )
            } else {
                layer
            }
        }
        // 动态作者匹配：uid 优先，回落数字 user_id（两种标识任一命中即更新）
        updateMomentInAllLists { item ->
            val hit = (uid.isNotBlank() && item.authorUid == uid) ||
                (numericUserId.isNotBlank() && item.authorId == numericUserId)
            if (hit) item.copy(isFollowed = isFollowing) else item
        }
    }

    /** 依次尝试 uid / 数字 user_id 完成关注或取消关注 */
    private suspend fun setFollowWithFallback(
        numericUserId: String,
        uid: String,
        follow: Boolean
    ): Result<Unit> {
        var last: Result<Unit>? = null
        for (identifier in profileIdentifierCandidates(numericUserId, uid)) {
            val res = apiService.setFollow(identifier, follow)
            if (res.isSuccess) return res
            last = res
        }
        return last ?: Result.failure(IOException("暂时无法识别该用户"))
    }

    /**
     * 关注 / 取消关注统一实现：乐观更新 → 调接口 → 失败回滚。
     *
     * 列表项入口（[setFollowForUser]）与动态卡片入口（[setMomentAuthorFollow]）共用本实现，
     * 避免两套乐观更新与回滚逻辑各写一遍。
     *
     * @param refreshProfile 成功后是否轻量刷新主页（列表项入口需要，动态卡片入口不需要）
     */
    private fun setFollowCore(
        userId: String?,
        uid: String?,
        follow: Boolean,
        refreshProfile: Boolean,
        onSuccess: (() -> Unit)?,
        onError: ((String) -> Unit)?
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再关注")
            return
        }
        val numericId = userId.orEmpty()
        val hexUid = uid.orEmpty()
        if (profileIdentifierCandidates(numericId, hexUid).isEmpty()) {
            onError?.invoke("暂时无法识别该用户")
            return
        }
        applyFollowStateEverywhere(numericId, hexUid, follow)
        scope.launch {
            val res = setFollowWithFallback(numericId, hexUid, follow)
            if (res.isSuccess) {
                if (refreshProfile) refreshOtherUserProfile()
                onSuccess?.invoke()
            } else {
                applyFollowStateEverywhere(numericId, hexUid, !follow)
                onError?.invoke(
                    res.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                        ?: "关注操作失败，请稍后重试"
                )
            }
        }
    }

    /**
     * 从关注 / 粉丝列表项切换关注状态
     */
    fun setFollowForUser(
        userId: String?,
        uid: String?,
        follow: Boolean,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        setFollowCore(
            userId = userId,
            uid = uid,
            follow = follow,
            refreshProfile = true,
            onSuccess = { onResult?.invoke(true, null) },
            onError = { onResult?.invoke(false, it) }
        )
    }

    /**
     * 打开关注 / 互关 / 粉丝列表（GET /api/follow/list）
     */
    fun openOtherUserFollowList(kind: FollowListKind) {
        val state = _otherUserProfile.value ?: return
        val profile = state.profile
        // 官方：关注/粉丝受对方隐私设置控制（缺省允许）；互关仅本人主页可见，交由服务端最终裁决
        val allowed = when (kind) {
            FollowListKind.FOLLOWING -> profile?.canViewFollowing ?: true
            FollowListKind.FOLLOWERS -> profile?.canViewFollowers ?: true
            FollowListKind.MUTUAL -> true
        }
        otherUserFollowCursor[kind] = null
        _otherUserFollowList.value = OtherUserFollowListState(
            kind = kind,
            isLoading = true,
            allowed = allowed,
            mutualAllowed = profile?.isSelf == true
        )
        loadOtherUserFollowList(reset = true)
    }

    fun closeOtherUserFollowList() {
        _otherUserFollowList.value = null
    }

    // ---- 资料页层级栈：全站唯一的「打开了哪几层资料页」事实来源 ----

    /** 当前是否有任何资料页层（被覆盖的页面 / 聊天页据此让出返回事件） */
    fun hasUserProfileLayers(): Boolean = _userProfileLayers.value.isNotEmpty()

    /**
     * 顶层入口打开资料页（动态卡片作者 / 捞取记录 / 大厅 / 群成员）：
     * 重置整栈后压入第一层，返回时逐层弹出
     */
    fun openUserProfileLayer(
        target: UserProfileTarget,
        host: UserProfileLayerHost,
        owner: Any? = null
    ) {
        _userProfileLayers.value = listOf(UserProfileLayer(target = target, host = host, owner = owner))
        loadUserProfileLayer(target)
    }

    /**
     * 在当前资料页内下钻一层（关注 / 粉丝列表点击进入次级主页）：
     * 被覆盖页面的整页状态随本层一起快照，返回时原样恢复
     */
    fun pushUserProfileLayer(
        target: UserProfileTarget,
        host: UserProfileLayerHost,
        owner: Any? = null
    ) {
        // 同一目标已在栈顶时不再叠层（重复点击 / 重复回调不会产生第二层）
        val top = _userProfileLayers.value.lastOrNull()
        if (top != null && sameProfileTarget(top.target, target)) return

        _userProfileLayers.value = _userProfileLayers.value + UserProfileLayer(
            target = target,
            host = host,
            owner = owner,
            restoreSnapshot = UserProfilePageSnapshot(
                profileState = _otherUserProfile.value,
                followListState = _otherUserFollowList.value,
                followCursors = otherUserFollowCursor.toMap(),
                momentsSort = _otherUserMomentsSort.value,
                momentsCursor = otherUserMomentsCursor,
                momentsScoreCursor = otherUserMomentsScoreCursor
            )
        )
        // 次级页面初始时关注列表重置为未展开
        _otherUserFollowList.value = null
        otherUserFollowCursor.clear()
        loadUserProfileLayer(target)
    }

    /**
     * 弹出栈顶资料页，并恢复压入它时快照的上一层整页状态
     * @return 是否成功弹出（栈空返回 false）
     */
    fun popUserProfileLayer(): Boolean {
        val layers = _userProfileLayers.value
        val popped = layers.lastOrNull() ?: return false
        isPoppingProfile = true
        _userProfileLayers.value = layers.dropLast(1)
        // 快照挂在层上：无论下面还有没有层（页面内下钻的下面可能是聊天页里的资料页），都要恢复
        popped.restoreSnapshot?.let { restoreUserProfilePage(it) }

        scope.launch {
            delay(500)
            isPoppingProfile = false
        }
        return true
    }

    /** 宿主实例销毁时收掉它自己的层（退出聊天页 / 资料页），避免残留没有渲染点的孤儿层 */
    fun closeUserProfileLayers(host: UserProfileLayerHost, owner: Any? = null) {
        val remaining = _userProfileLayers.value.filterNot {
            it.host == host && (owner == null || it.owner === owner)
        }
        if (remaining.size != _userProfileLayers.value.size) {
            _userProfileLayers.value = remaining
        }
    }

    /** 清空全部资料页层 */
    fun clearUserProfileLayers() {
        _userProfileLayers.value = emptyList()
        isPoppingProfile = false
    }

    /** 恢复一层被覆盖页面的整页状态（资料 / 关系列表 / 动态分页游标） */
    private fun restoreUserProfilePage(snapshot: UserProfilePageSnapshot) {
        _otherUserProfile.value = snapshot.profileState
        _otherUserFollowList.value = snapshot.followListState
        otherUserFollowCursor.clear()
        otherUserFollowCursor.putAll(snapshot.followCursors)
        _otherUserMomentsSort.value = snapshot.momentsSort
        otherUserMomentsCursor = snapshot.momentsCursor
        otherUserMomentsScoreCursor = snapshot.momentsScoreCursor
        otherUserProfileToken += 1
    }

    /** 按目标静默加载资料（不改变层级栈） */
    private fun loadUserProfileLayer(target: UserProfileTarget) {
        openOtherUserProfile(
            userId = target.userId,
            uid = target.uid,
            name = target.name,
            avatarUrl = target.avatarUrl,
            navigate = false
        )
    }

    private fun sameProfileTarget(a: UserProfileTarget, b: UserProfileTarget): Boolean =
        (a.uid.isNotBlank() && a.uid == b.uid) || (a.userId.isNotBlank() && a.userId == b.userId)

    /** 拉取关注 / 粉丝列表；reset = true 表示从第一页重新加载 */
    fun loadOtherUserFollowList(reset: Boolean = false) {
        val listState = _otherUserFollowList.value ?: return
        if (!reset && (!listState.hasMore || listState.isLoading || listState.isLoadingMore)) return
        val state = _otherUserProfile.value ?: return
        val candidates = profileIdentifierCandidates(state.userId, state.uid)
        if (candidates.isEmpty()) {
            _otherUserFollowList.value = listState.copy(isLoading = false, error = "缺少用户标识")
            return
        }

        val kind = listState.kind
        val beforeId = if (reset) null else otherUserFollowCursor[kind]
        _otherUserFollowList.value = if (reset) {
            listState.copy(isLoading = true, isLoadingMore = false, error = null)
        } else {
            listState.copy(isLoadingMore = true)
        }

        scope.launch {
            var data: FollowListData? = null
            var lastError: String? = null
            for (identifier in candidates) {
                val res = apiService.getFollowList(identifier, kind.param, beforeId = beforeId)
                if (res.isSuccess) {
                    data = res.getOrThrow()
                    break
                }
                lastError = res.exceptionOrNull()?.message
            }

            val current = _otherUserFollowList.value ?: return@launch
            if (current.kind != kind) return@launch

            if (data != null) {
                otherUserFollowCursor[kind] = data.nextBeforeId
                val items = data.list.orEmpty().map { mapFollowUserItem(it) }
                _otherUserFollowList.value = current.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    allowed = data.allowed,
                    message = data.message.orEmpty(),
                    list = if (reset) items else (current.list + items).distinctBy { it.key },
                    hasMore = data.hasMore
                )
            } else {
                _otherUserFollowList.value = current.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = lastError?.takeIf { it.isNotBlank() } ?: "获取列表失败"
                )
            }
        }
    }

    /**
     * 发起私信（POST /room/dm/create-from-user）：返回或复用与对方的私聊房间
     */
    fun startDmWithOtherUser(
        onSuccess: ((ConversationItem) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再发起私信")
            return
        }
        val state = _otherUserProfile.value
        val profile = state?.profile
        val numericId = state?.userId.orEmpty().ifBlank { profile?.userId.orEmpty() }
        val hexUid = state?.uid.orEmpty().ifBlank { profile?.uid.orEmpty() }
        val candidates = dmIdentifierCandidates(numericId, hexUid)
        if (candidates.isEmpty()) {
            onError?.invoke("暂时无法识别该用户")
            return
        }
        if (profile?.iBlocked == true) {
            onError?.invoke("已拉黑该用户，请先解除拉黑")
            return
        }
        if (profile?.blocked == true) {
            onError?.invoke("对方已将你拉黑，无法发起私信")
            return
        }
        if (profile != null && !profile.dmAllowed) {
            // 官方同款兜底文案（tmp/assets/dmBan-DYyZ3iqS.js）
            onError?.invoke("对方的私聊功能已被限制，无法发起私聊")
            return
        }
        val disabledReason = profile?.dmDisabledReason.orEmpty()
        if (disabledReason.isNotBlank()) {
            onError?.invoke(disabledReason)
            return
        }

        scope.launch {
            var lastError: String? = null
            for (identifier in candidates) {
                val res = apiService.createDmFromUser(identifier)
                if (res.isSuccess) {
                    val data = res.getOrThrow()
                    val isOnline = if (profile?.showOnlineStatus == false) null else profile?.isOnline
                    val conversation = ConversationItem(
                        id = data.roomId,
                        targetName = profile?.name?.takeIf { it.isNotBlank() }
                            ?: data.roomName.takeIf { it.isNotBlank() }
                            ?: "私聊",
                        targetAvatar = profile?.avatarUrl?.takeIf { it.isNotBlank() },
                        targetUserId = numericId,
                        targetUid = hexUid,
                        lastMessage = "点击查看最新消息",
                        timestamp = formatConversationTime(null),
                        category = MessageCategory.PRIVATE,
                        isOnline = isOnline,
                        tag = resolveConversationStatusTag(MessageCategory.PRIVATE)
                    )
                    upsertConversation(conversation)
                    enterRoom(data.roomId)
                    onSuccess?.invoke(conversation)
                    return@launch
                }
                lastError = res.exceptionOrNull()?.message
            }
            onError?.invoke(lastError?.takeIf { it.isNotBlank() } ?: "发起私信失败，请稍后重试")
        }
    }

    /** 依次尝试 uid / 数字 user_id 拉取主页聚合数据 */
    private suspend fun fetchProfileAggregateWithFallback(
        candidates: List<String>,
        beforeId: Long? = null,
        beforeScore: Double? = null,
        sort: MomentsSort = _otherUserMomentsSort.value
    ): Result<UserProfileAggregateData> {
        var last: Result<UserProfileAggregateData>? = null
        for (identifier in candidates) {
            val res = apiService.getUserProfileAggregate(
                identifier,
                beforeId = beforeId,
                beforeScore = beforeScore,
                sort = sort.paramValue
            )
            if (res.isSuccess) return res
            last = res
        }
        return last ?: Result.failure(IOException("暂时无法获取用户资料"))
    }

    /**
     * 切换他人主页动态排序（最新 / 最热）
     *
     * 对齐官方 UserMomentsView：切换排序后重置分页游标并重新拉取第一页动态，
     * 资料骨架与关系状态保持不变。
     */
    fun setOtherUserMomentsSort(sort: MomentsSort) {
        if (_otherUserMomentsSort.value == sort) return
        _otherUserMomentsSort.value = sort
        otherUserMomentsCursor = null
        otherUserMomentsScoreCursor = null
        val state = _otherUserProfile.value ?: return
        if (state.profile == null) return
        // 清空旧排序的动态，避免「最热」结果里混着「最新」的旧数据
        _otherUserProfile.value = state.copy(
            isLoadingMore = false,
            profile = state.profile.copy(moments = emptyList(), hasMoreMoments = false)
        )
        refreshOtherUserProfile()
    }

    private fun mapOtherUserProfile(
        data: UserProfileAggregateData,
        requestedUserId: String,
        requestedUid: String
    ): OtherUserProfile {
        val user = data.user
        val momentDtos = data.list.orEmpty()
        return OtherUserProfile(
            userId = requestedUserId.takeIf { it.isNotBlank() }
                ?: user?.id?.takeIf { it > 0L }?.toString()
                ?: requestedUid,
            uid = user?.uid?.takeIf { it.isNotBlank() } ?: requestedUid,
            name = user?.username?.takeIf { it.isNotBlank() } ?: "零语零友",
            genderText = user?.genderText?.takeIf { it.isNotBlank() } ?: when (user?.gender) {
                "female" -> "女"
                "male" -> "男"
                else -> ""
            },
            ageRangeText = user?.ageRangeText?.takeIf { it.isNotBlank() } ?: user?.ageRange.orEmpty(),
            location = user?.location.orEmpty(),
            avatarUrl = NetworkImageUrl.resolveWithStyle(user?.avatarUrl),
            avatarFallback = user?.avatarFallback.orEmpty(),
            bio = user?.bio.orEmpty(),
            // 计数与可见权限以 user 对象为准（官方 UserMomentsView 读的就是 data.user.*），根级字段仅作兜底
            followingCount = user?.followingCount ?: data.followingCount,
            followerCount = user?.followerCount ?: data.followerCount,
            likeCount = user?.likeCount ?: data.likeCount,
            mutualCount = user?.mutualCount ?: data.mutualCount,
            canViewFollowing = user?.canViewFollowing ?: data.canViewFollowing,
            canViewFollowers = user?.canViewFollowers ?: data.canViewFollowers,
            isSelf = data.isSelf,
            isFollowing = data.isFollowing,
            followedBy = data.followedBy,
            iBlocked = data.iBlocked,
            blocked = data.blocked == true,
            blockedMessage = data.blockedMessage.orEmpty(),
            canDm = data.canDm,
            dmBanned = user?.dmBanned == true,
            canReceiveDm = user?.canReceiveDm != false,
            dmDisabledReason = data.dmDisabledReason.orEmpty(),
            cleanStreamMode = user?.cleanStreamMode,
            privacyMode = user?.privacyMode == true,
            // 服务端未下发时将保持 null（未知），仅在明确下发 false 时视为不可见
            momentsPublic = data.momentsPublic,
            moments = mapMomentDtos(momentDtos),
            hasMoreMoments = data.hasMore,
            isOnline = if (user?.showOnlineStatus == false) null else user?.isOnline,
            showOnlineStatus = user?.showOnlineStatus
        )
    }

    /**
     * 将加载到的他人资料（清流/在线状态等）实时同步到会话列表
     */
    private fun syncUserProfileToConversations(profile: OtherUserProfile) {
        if (profile.userId.isBlank() && profile.uid.isBlank()) return
        _conversations.value = _conversations.value.map { conv ->
            val matches = (profile.userId.isNotBlank() && conv.targetUserId == profile.userId) ||
                (profile.uid.isNotBlank() && conv.targetUid == profile.uid)
            if (matches && conv.category != MessageCategory.CODE) {
                val isOnline = if (profile.showOnlineStatus == false) null else (profile.isOnline ?: conv.isOnline)
                conv.copy(
                    isOnline = isOnline,
                    tag = resolveConversationStatusTag(conv.category)
                )
            } else conv
        }
    }

    private fun mapFollowUserItem(dto: FollowListItemDto): FollowUserItem = FollowUserItem(
        uid = dto.uid,
        userId = dto.userId.takeIf { it > 0L }?.toString().orEmpty(),
        name = dto.username.takeIf { it.isNotBlank() } ?: "零语零友",
        genderText = when (dto.gender) {
            "female" -> "女"
            "male" -> "男"
            else -> ""
        },
        avatarUrl = NetworkImageUrl.resolveWithStyle(dto.avatarUrl),
        isFollowing = dto.isFollowing,
        isFollower = dto.isFollower,
        isMutual = dto.isMutual,
        iBlocked = dto.iBlocked,
        isSelf = dto.isSelf
    )

    /**
     * 关注或取消关注动态作者，并同步所有动态列表中的关注状态。
     *
     * 与列表项入口共用 [setFollowCore]；官方动态流的关注按钮参数为 `user_id=<uid>`（hex uid），
     * 因此优先使用 [authorUid]，仅当 uid 缺失时回退数字 [authorId]。
     */
    fun setMomentAuthorFollow(
        authorId: String,
        authorUid: String = "",
        isFollowing: Boolean,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        setFollowCore(
            userId = authorId,
            uid = authorUid,
            follow = isFollowing,
            refreshProfile = false,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * 动态点赞
     */
    fun toggleLikeMoment(momentId: String, onError: ((String) -> Unit)? = null) {
        updateMomentInAllLists { item ->
            if (item.id == momentId) {
                val newLiked = !item.isLiked
                val newCount = if (newLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                item.copy(isLiked = newLiked, likesCount = newCount)
            } else item
        }

        val id = momentId.toLongOrNull()
        if (id == null) {
            onError?.invoke("动态标识无效")
            return
        }
        scope.launch {
            val res = apiService.likeMoment(id)
            if (res.isFailure) {
                // 反向切换回滚乐观更新，避免界面停留在错误的点赞状态与数字
                updateMomentInAllLists { item ->
                    if (item.id == momentId) {
                        val revertLiked = !item.isLiked
                        val revertCount = if (revertLiked) item.likesCount + 1
                                          else (item.likesCount - 1).coerceAtLeast(0)
                        item.copy(isLiked = revertLiked, likesCount = revertCount)
                    } else item
                }
                onError?.invoke(res.exceptionOrNull()?.message ?: "点赞失败，请稍后重试")
            }
        }
    }

    /**
     * 获取动态评论列表
     */
    fun fetchMomentComments(
        momentId: Long,
        beforeId: Long? = null,
        limit: Int = 20,
        onSuccess: ((MomentCommentsData) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        launchApiCall(
            fallbackMessage = "获取评论失败",
            onError = onError,
            call = { apiService.getMomentComments(momentId, beforeId, limit) }
        ) {
            onSuccess?.invoke(it)
        }
    }

    /**
     * 发表动态评论 / 二级回复
     */
    fun sendMomentComment(
        momentId: Long,
        content: String,
        parentId: Long? = null,
        replyToUserId: String? = null,
        onSuccess: ((PostCommentData) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再发表评论")
            return
        }
        launchApiCall(
            fallbackMessage = "发表评论失败",
            onError = onError,
            call = { apiService.postMomentComment(momentId, content, parentId, replyToUserId) }
        ) { data ->
            // 同步本地各列表中 moment 的 commentsCount
            updateMomentInAllLists { m ->
                if (m.id == momentId.toString()) {
                    m.copy(commentsCount = data.commentCount.coerceAtLeast(m.commentsCount + 1))
                } else m
            }
            onSuccess?.invoke(data)
        }
    }

    /**
     * 动态评论点赞/取消点赞 (POST /moment/comment/like)
     */
    fun toggleLikeComment(
        momentId: Long,
        commentId: Long,
        onSuccess: ((CommentLikeData) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再操作")
            return
        }
        launchApiCall(
            fallbackMessage = "评论点赞失败",
            onError = onError,
            call = { apiService.likeMomentComment(commentId) }
        ) { data ->
            onSuccess?.invoke(data)
        }
    }

    /**
     * 发布动态（全站唯一发布入口，支持本地多图异步上传并携带进度通知）
     *
     * 由 [top.lanxint.zerotalk.ui.components.MomentComposer] 统一调用，
     * 「动态」页与「捞动态」面板不再各自维护发布逻辑。
     */
    fun publishMomentWithPhotos(
        content: String,
        photos: List<ByteArray>,
        audioBytes: ByteArray? = null,
        musicJson: String = "",
        isPrivate: Boolean = false,
        audienceMode: String = "all",
        audienceMutual: Boolean = false,
        userIds: List<String> = emptyList(),
        onProgress: ((String) -> Unit)? = null,
        onSuccess: ((CreateMomentData) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再发布动态")
            return
        }
        scope.launch {
            try {
                val uploadedUrls = mutableListOf<String>()
                if (photos.isNotEmpty()) {
                    photos.forEachIndexed { index, bytes ->
                        onProgress?.invoke("正在上传第 ${index + 1}/${photos.size} 张配图...")
                        val uploadRes = apiService.uploadImage(bytes, "moment_${System.currentTimeMillis()}_$index.jpg")
                        if (uploadRes.isSuccess) {
                            uploadedUrls.add(uploadRes.getOrThrow())
                        } else {
                            val err = uploadRes.exceptionOrNull()?.message ?: "第 ${index + 1} 张图片上传失败"
                            onError?.invoke(err)
                            return@launch
                        }
                    }
                }

                // 语音：发布时才上传（upload_source=moment_audio）
                var audioUrl = ""
                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    onProgress?.invoke("正在上传语音...")
                    val audioRes = apiService.uploadMomentAudio(audioBytes, "moment_audio_${System.currentTimeMillis()}.m4a")
                    if (audioRes.isSuccess) {
                        audioUrl = audioRes.getOrThrow()
                    } else {
                        onError?.invoke(audioRes.exceptionOrNull()?.message ?: "语音上传失败")
                        return@launch
                    }
                }

                onProgress?.invoke("正在发布动态...")
                val res = apiService.createMoment(
                    content = content,
                    images = uploadedUrls,
                    audioUrl = audioUrl,
                    musicJson = musicJson,
                    isPrivate = isPrivate,
                    audienceMode = audienceMode,
                    audienceMutual = audienceMutual,
                    userIds = userIds
                )
                if (res.isSuccess) {
                    val data = res.getOrThrow()
                    fetchMomentsByCategory()
                    onSuccess?.invoke(data)
                } else {
                    val err = res.exceptionOrNull()?.message ?: "发布动态失败"
                    onError?.invoke(err)
                }
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "发布动态发生异常")
            }
        }
    }

    /**
     * 设置动态置顶/取消置顶 (POST /moment/pin)
     */
    fun setMomentPin(
        momentId: Long,
        isPinned: Boolean,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再操作")
            return
        }
        launchApiCall(
            fallbackMessage = "设置置顶失败",
            onError = onError,
            call = { apiService.setMomentPin(momentId, isPinned) }
        ) {
            updateMomentInAllLists { m ->
                if (m.id == momentId.toString()) m.copy(isPinned = isPinned) else m
            }
            onSuccess?.invoke()
        }
    }

    /**
     * 设置动态可见性（公开或仅自己可见）(POST /moment/visibility)
     */
    fun setMomentVisibility(
        momentId: Long,
        isPrivate: Boolean,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再操作")
            return
        }
        launchApiCall(
            fallbackMessage = "设置可见性失败",
            onError = onError,
            call = { apiService.setMomentVisibility(momentId, isPrivate) }
        ) {
            updateMomentInAllLists { m ->
                if (m.id == momentId.toString()) m.copy(isPrivate = isPrivate) else m
            }
            onSuccess?.invoke()
        }
    }

    /**
     * 设置动态谁可以看 (POST /moment/hide-users)
     */
    fun setMomentAudience(
        momentId: Long,
        audienceMode: String,
        audienceMutual: Boolean,
        userIds: List<String> = emptyList(),
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再操作")
            return
        }
        launchApiCall(
            fallbackMessage = "设置受众失败",
            onError = onError,
            call = { apiService.setMomentHideUsers(momentId, audienceMode, userIds, audienceMutual) }
        ) {
            updateMomentInAllLists { m ->
                if (m.id == momentId.toString()) m.copy(
                    audienceMode = audienceMode,
                    audienceMutual = audienceMutual,
                    isPrivate = false
                ) else m
            }
            onSuccess?.invoke()
        }
    }

    /**
     * 删除动态 (POST /moment/delete)
     */
    fun deleteMoment(
        momentId: Long,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("请先登录后再操作")
            return
        }
        launchApiCall(
            fallbackMessage = "删除动态失败",
            onError = onError,
            call = { apiService.deleteMoment(momentId) }
        ) {
            removeMomentFromAllLists(momentId.toString())
            onSuccess?.invoke()
        }
    }

    /**
     * 获取真实会话房间列表并同步到 conversations
     */
    fun fetchRoomList(type: String = "all") {
        if (_loginData.value == null) return
        scope.launch {
            val res = apiService.getRoomList(offset = 0, perPage = 50, type = type)
            res.getOrNull()?.let { roomData ->
                val roomItems = roomData.rooms.map { dto ->
                    val existingConv = _conversations.value.find { it.id == dto.roomId }
                    val cachedBootstrap = _roomBootstrapMap.value[dto.roomId]
                    val cachedPeer = _roomPeers.value[dto.roomId]

                    // 记录对端成员信息
                    dto.peer?.let { peer ->
                        if (peer.uid.isNotBlank()) {
                            _roomPeers.value = _roomPeers.value + (dto.roomId to WsServerEvent.UserJoined(
                                uid = peer.uid,
                                username = peer.username,
                                gender = peer.gender ?: "unknown",
                                avatarUrl = peer.avatarUrl,
                                mbti = null,
                                cleanStreamMode = if (peer.cleanStreamMode == true) 1 else 0,
                                roomId = dto.roomId
                            ))
                        }
                    }

                    // 官网口径：对端显示名优先取备注（peer.remark || peer.username）
                    val peerDisplayName = dto.peer?.remark?.takeIf { it.isNotBlank() }
                        ?: dto.peer?.username
                    val name = when {
                        dto.type == "private" -> dto.roomName
                        !peerDisplayName.isNullOrBlank() -> peerDisplayName
                        !cachedBootstrap?.peerUser?.username.isNullOrBlank() -> cachedBootstrap.peerUser.username
                        else -> dto.roomName
                    }
                    val cat = when (dto.type) {
                        "private" -> MessageCategory.CODE
                        "dm" -> MessageCategory.PRIVATE
                        else -> MessageCategory.MATCH
                    }

                    // 解析对端在线状态：暗号房不适用在线标记；私聊/匹配依次从 dto.peer、bootstrap、缓存中获取
                    // 隐身模式判定：当对方明确配置 showOnlineStatus == false 时，将 isOnline 置为 null（无状态标识）
                    val isPeerOnline = when {
                        cat == MessageCategory.CODE -> null
                        dto.peer?.showOnlineStatus == false -> null
                        cachedBootstrap?.peerUser?.showOnlineStatus == false -> null
                        dto.peer != null -> dto.peer.isOnline
                        cachedBootstrap?.peerUser != null -> cachedBootstrap.peerUser.isOnline
                        else -> existingConv?.isOnline
                    }

                    val targetUid = dto.peer?.uid?.ifBlank { null } ?: cachedBootstrap?.peerUser?.uid?.ifBlank { null } ?: existingConv?.targetUid.orEmpty()
                    val targetUserId = dto.peer?.userId?.takeIf { it > 0L }?.toString() ?: cachedBootstrap?.peerUser?.userId?.takeIf { it > 0L }?.toString() ?: existingConv?.targetUserId.orEmpty()

                    val tagStr = resolveConversationStatusTag(cat)

                    val lastMsg = existingConv?.lastMessage?.takeIf { it != "点击查看最新消息" } ?: "点击查看最新消息"

                    ConversationItem(
                        id = dto.roomId,
                        targetName = name,
                        // 对齐官网 RoomListSection：仅 dm / 匹配房展示对端头像，
                        // 暗号房（群聊）固定用群组图标，不能落成员头像
                        targetAvatar = if (cat == MessageCategory.CODE) {
                            null
                        } else {
                            dto.peer?.avatarUrl ?: cachedBootstrap?.peerUser?.avatarUrl ?: existingConv?.targetAvatar
                        },
                        targetUserId = targetUserId,
                        targetUid = targetUid,
                        targetLoginName = dto.peer?.loginName?.ifBlank { null } ?: cachedBootstrap?.peerUser?.loginName?.ifBlank { null } ?: existingConv?.targetLoginName.orEmpty(),
                        targetRemark = dto.peer?.remark.orEmpty().ifBlank { existingConv?.targetRemark.orEmpty() },
                        lastMessage = lastMsg,
                        timestamp = formatConversationTime(dto.lastMessageAt ?: dto.createdAt),
                        unreadCount = if (dto.isMuted) 0 else dto.unreadCount,
                        category = cat,
                        isOnline = isPeerOnline,
                        tag = tagStr,
                        isMuted = dto.isMuted,
                        isPinned = dto.isPinned,
                        isRoomCreator = dto.isCreator
                    )
                }

                val existingIds = roomItems.map { it.id }.toSet()
                val leftovers = _conversations.value.filter { it.id !in existingIds && !it.id.startsWith("conv_") }
                _conversations.value = (roomItems + leftovers).sortedByDescending { it.isPinned }
                // 会话列表落地后刷新 uid -> 备注 映射（备注清空的 uid 会被移除）
                refreshUidRemarks()
            }
        }
    }

    /**
     * 新增或置顶会话
     */
    fun upsertConversation(item: ConversationItem) {
        _conversations.value = (listOf(item) + _conversations.value.filter { it.id != item.id })
            .sortedByDescending { it.isPinned }
    }

    /**
     * 创建暗号房间并自动置顶
     *
     * @param encryptionEnabled 是否开启端到端消息加密；官方默认关闭，由创建页开关决定
     */
    fun createSecretRoom(
        roomName: String,
        password: String,
        encryptionEnabled: Boolean = false,
        onSuccess: ((CreateRoomData) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("当前为无账号状态，请先登录")
            return
        }
        scope.launch {
            val res = apiService.createSecretRoom(roomName, password, encryptionEnabled)
            if (res.isSuccess) {
                val data = res.getOrThrow()
                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val newConv = ConversationItem(
                    id = data.roomId,
                    targetName = data.roomName,
                    targetAvatar = null,
                    lastMessage = "暗号房间已创建",
                    timestamp = nowTime,
                    unreadCount = 0,
                    category = MessageCategory.CODE,
                    isOnline = null,
                    tag = "暗号"
                )
                upsertConversation(newConv)
                fetchRoomList()
                onSuccess?.invoke(data)
            } else {
                val err = res.exceptionOrNull()?.message ?: "创建房间失败"
                onError?.invoke(err)
            }
        }
    }

    /**
     * 加入暗号房间并自动置顶
     */
    fun joinSecretRoom(
        creatorUsername: String,
        roomName: String,
        password: String,
        onSuccess: ((JoinRoomData) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onError?.invoke("当前为无账号状态，请先登录")
            return
        }
        scope.launch {
            val res = apiService.joinSecretRoom(creatorUsername, roomName, password)
            if (res.isSuccess) {
                val data = res.getOrThrow()
                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val newConv = ConversationItem(
                    id = data.roomId,
                    targetName = roomName,
                    targetAvatar = null,
                    lastMessage = "已加入暗号房间",
                    timestamp = nowTime,
                    unreadCount = 0,
                    category = MessageCategory.CODE,
                    isOnline = null,
                    tag = "暗号"
                )
                upsertConversation(newConv)
                fetchRoomList()
                onSuccess?.invoke(data)
            } else {
                val err = res.exceptionOrNull()?.message ?: "加入房间失败"
                onError?.invoke(err)
            }
        }
    }

    /**
     * 进入房间：自动加入 WebSocket 房间并拉取真实历史消息
     */
    fun enterRoom(roomId: String) {
        if (wsClient.currentRoomId != roomId) {
            wsClient.joinRoom(roomId)
        }
        loadRoomHistory(roomId)
    }

    fun setConversationPinned(
        roomId: String,
        pinned: Boolean,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录后再操作")
            return
        }
        launchApiCall(
            fallbackMessage = "置顶操作失败",
            onError = { onResult?.invoke(false, it) },
            call = {
                val endpoint = if (pinned) "/room/pin" else "/room/unpin"
                apiService.roomManagementRequest(endpoint, mapOf("room_id" to roomId))
            }
        ) {
            _conversations.value = _conversations.value
                .map { if (it.id == roomId) it.copy(isPinned = pinned) else it }
                .sortedByDescending { it.isPinned }
            fetchRoomList()
            onResult?.invoke(true, null)
        }
    }

    /**
     * 退出房间 (POST /room/leave)
     *
     * 对齐官网 ChatView「退出房间」：仅暗号房/群聊成员可用，确认文案「退出后你将不再是该暗号房成员」。
     */
    fun leaveRoom(
        conversation: ConversationItem,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录后再操作")
            return
        }
        scope.launch {
            val result = apiService.roomManagementRequest(
                "/room/leave",
                mapOf("room_id" to conversation.id)
            )
            if (result.isSuccess) {
                removeRoomLocally(conversation.id)
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, result.exceptionOrNull()?.message ?: "退出失败")
            }
        }
    }

    /**
     * 删除房间 (POST /room/delete)
     *
     * 对齐官网 ChatView「删除房间」：私聊双方均可删除；暗号房/群聊仅创建者可删除。
     *
     * @param blockPeer 为真时同时拉黑对方（对应官网「删除并拉黑」流程的 `block_peer=1`，
     *        服务端会一并拉黑，无需再单独调用 `/block/add`）
     */
    fun deleteRoom(
        conversation: ConversationItem,
        blockPeer: Boolean = false,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录后再操作")
            return
        }
        scope.launch {
            val result = apiService.roomManagementRequest(
                "/room/delete",
                mapOf(
                    "room_id" to conversation.id,
                    "block_peer" to if (blockPeer) "1" else "0"
                )
            )
            if (result.isSuccess) {
                removeRoomLocally(conversation.id)
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, result.exceptionOrNull()?.message ?: "删除房间失败")
            }
        }
    }

    /**
     * 「退出房间 / 删除房间」成功后的本地清理
     *
     * 移出会话列表、清空该房消息与分页游标、必要时退出 WS 房间，并刷新房间列表。
     */
    private fun removeRoomLocally(roomId: String) {
        _conversations.value = _conversations.value.filterNot { it.id == roomId }
        _roomMessages.value = _roomMessages.value - roomId
        _roomHasMoreHistory.value = _roomHasMoreHistory.value - roomId
        _roomHistoryLoading.value = _roomHistoryLoading.value - roomId
        _groupRooms.value = _groupRooms.value - roomId
        if (wsClient.currentRoomId == roomId) wsClient.leaveRoom()
        fetchRoomList()
    }

    /**
     * 拉取房间初始化元数据及历史聊天记录 (/api/chat/bootstrap)
     */
    fun loadRoomHistory(roomId: String) {
        if (_loginData.value == null) return
        // 重新进入房间：重置分页游标与「还有更早消息」标记，避免沿用上一次的过期位置
        oldestServerMessageId.remove(roomId)
        _roomHistoryLoading.value = _roomHistoryLoading.value + (roomId to false)
        _roomHasMoreHistory.value = _roomHasMoreHistory.value + (roomId to false)
        scope.launch {
            val res = apiService.getChatBootstrap(roomId)
            res.getOrNull()?.let { bootstrap ->
                // 1. 同步对端用户信息
                bootstrap.peerUser?.let { peer ->
                    if (peer.uid.isNotBlank()) {
                        rememberMember(peer.uid, peer.username, peer.gender, peer.avatarUrl)
                        _roomPeers.value = _roomPeers.value + (roomId to WsServerEvent.UserJoined(
                            uid = peer.uid,
                            username = peer.username,
                            gender = peer.gender ?: "unknown",
                            avatarUrl = peer.avatarUrl,
                            mbti = null,
                            cleanStreamMode = if (peer.cleanStreamMode == true) 1 else 0,
                            roomId = roomId
                        ))
                        _conversations.value = _conversations.value.map { conv ->
                            if (conv.id == roomId) {
                                val shouldUpdate = conv.targetName == "神秘零友" || conv.targetName == "神秘人" || conv.targetName == "对方" || conv.targetName.startsWith("匹配房间-")
                                val isOnline = when {
                                    conv.category == MessageCategory.CODE -> null
                                    peer.showOnlineStatus == false -> null
                                    else -> peer.isOnline ?: conv.isOnline
                                }
                                val tag = resolveConversationStatusTag(conv.category)
                                conv.copy(
                                    targetName = if (shouldUpdate && peer.username.isNotBlank()) peer.username else conv.targetName,
                                    // 群聊（暗号房）不写对端头像
                                    targetAvatar = if (conv.category == MessageCategory.CODE) conv.targetAvatar else (peer.avatarUrl ?: conv.targetAvatar),
                                    targetUserId = peer.userId.takeIf { it > 0L }?.toString() ?: conv.targetUserId,
                                    targetUid = peer.uid.ifBlank { conv.targetUid },
                                    targetLoginName = peer.loginName.ifBlank { conv.targetLoginName },
                                    isOnline = isOnline,
                                    tag = tag
                                )
                            } else conv
                        }
                    }
                }

                // 3. 同步存储完整的 bootstrap 数据（成员列表 + 权限字段）
                _roomBootstrapMap.value = _roomBootstrapMap.value + (roomId to bootstrap)

                // 4. 映射历史消息
                val msgList = bootstrap.messages.map { dto -> mapChatMessageDto(dto, roomId) }

                // 以服务端下发的房间类型判定「群聊」，比会话列表的 type 映射更可靠
                // （暗号房此前依赖 category == CODE，若服务端 type 不是 private 就会漏判）
                _groupRooms.value = _groupRooms.value + (
                    roomId to (!bootstrap.isDmRoom && !bootstrap.isMatchRoom && !bootstrap.isPublicRoom)
                    )

                _roomHasMoreHistory.value = _roomHasMoreHistory.value + (roomId to bootstrap.hasMoreMessages)
                bootstrap.messages.firstOrNull()?.let { oldestServerMessageId[roomId] = it.id }

                if (msgList.isNotEmpty()) {
                    _roomMessages.value = _roomMessages.value + (roomId to msgList)
                    val lastMsg = msgList.last()
                    _conversations.value = _conversations.value.map { conv ->
                        if (conv.id == roomId) {
                            conv.copy(
                                lastMessage = lastMsg.previewText.ifBlank { lastMsg.content },
                                timestamp = lastMsg.timestamp
                            )
                        } else conv
                    }
                }
            }
        }
    }

    /**
     * 上拉加载更早的历史消息 (/api/chat/messages?before_id=)
     *
     * 供私聊 / 暗号房 / 公共大厅的「顶部继续上拉」调用。
     */
    fun loadOlderMessages(roomId: String) {
        if (_loginData.value == null) return
        if (roomId.isBlank()) return
        if (_roomHistoryLoading.value[roomId] == true) return
        if (_roomHasMoreHistory.value[roomId] != true) return
        val beforeId = oldestServerMessageId[roomId] ?: return

        _roomHistoryLoading.value = _roomHistoryLoading.value + (roomId to true)
        scope.launch {
            val res = apiService.getChatMessages(roomId = roomId, beforeId = beforeId, limit = 50)
            _roomHistoryLoading.value = _roomHistoryLoading.value + (roomId to false)
            val data = res.getOrNull() ?: return@launch

            val older = data.messages.map { dto -> mapChatMessageDto(dto, roomId) }
            if (older.isNotEmpty()) {
                data.messages.firstOrNull()?.let { oldestServerMessageId[roomId] = it.id }
                if (roomId == _hallRoomId.value) {
                    _hallMessages.value = older + _hallMessages.value
                } else {
                    val current = _roomMessages.value[roomId] ?: emptyList()
                    _roomMessages.value = _roomMessages.value + (roomId to (older + current))
                }
            }
            _roomHasMoreHistory.value = _roomHasMoreHistory.value + (roomId to data.hasMore)
        }
    }

    /**
     * 判断某个房间是否为「零语大厅」（公共房）
     *
     * 兼容大厅 room_id 变动或服务端下发 room_id 与本地不一致的情况。
     */
    private fun isHallRoom(roomId: String?): Boolean {
        if (roomId.isNullOrBlank()) return false
        if (roomId == _hallRoomId.value) return true
        return _bootstrapData.value?.publicRooms?.any { it.roomId == roomId } == true
    }

    /**
     * 拉取公共大厅历史聊天记录（进入大厅时调用）
     *
     * 先走与私聊一致的 chat/bootstrap（字段解析久经验证），拿不到再回退到分页接口。
     */
    private fun loadHallHistory(roomId: String) {
        if (_loginData.value == null) return
        scope.launch {
            // 等 WS 入房登记完成再拉历史（服务端可能只对已加入的房间返回消息）
            delay(350)
            val bootstrap = apiService.getChatBootstrap(roomId).getOrNull()
            if (bootstrap != null && bootstrap.messages.isNotEmpty()) {
                applyHallHistory(roomId, bootstrap.messages, bootstrap.hasMoreMessages)
                return@launch
            }
            // 回退：分页接口取最近一页
            val fallback = apiService.getChatMessages(roomId = roomId, limit = 50).getOrNull()
            applyHallHistory(
                roomId = roomId,
                dtos = fallback?.messages ?: emptyList(),
                hasMore = fallback?.hasMore ?: (bootstrap?.hasMoreMessages ?: false)
            )
        }
    }

    private fun applyHallHistory(roomId: String, dtos: List<ChatMessageDto>, hasMore: Boolean) {
        val history = dtos.map { dto -> mapChatMessageDto(dto, roomId) }
        if (history.isNotEmpty()) {
            // 合并已存在的实时消息，避免覆盖刚收到的内容
            val existingIds = _hallMessages.value.map { it.id }.toSet()
            _hallMessages.value = history.filterNot { existingIds.contains(it.id) } + _hallMessages.value
            dtos.firstOrNull()?.let { oldestServerMessageId[roomId] = it.id }
        }
        _roomHasMoreHistory.value = _roomHasMoreHistory.value + (roomId to hasMore)
    }

    /**
     * 「原始消息 → ChatMessage」的归一化输入。
     *
     * 历史消息 DTO 与 WS 实时事件的字段名不同，各自归一化成本结构后交给
     * [buildChatMessage] 统一做类型判定与字段映射，避免两处各写一遍
     * （isImage / isVoice / isPat / patText / preview 与 20 余个字段的拼装）。
     */
    private data class RawChatMessage(
        val id: String,
        val senderId: String,
        val content: String,
        val type: String,
        val timestamp: String,
        val timestampMs: Long,
        /** 本机到达时刻（仅实时推送写入；历史消息留 0，用于骰子掷动等「新鲜度」判定） */
        val arrivedAtMs: Long = 0L,
        val serverId: Long,
        val isMine: Boolean,
        val imageUrl: String? = null,
        val audioUrl: String? = null,
        val audioSource: String? = null,
        val senderName: String = "",
        val senderAvatar: String = "",
        val senderGender: String = "",
        val quotedText: String? = null,
        val quotedIsMine: Boolean? = null,
        val quotedSenderName: String? = null
    )

    /**
     * 历史消息 DTO -> UI 消息模型
     */
    private fun mapChatMessageDto(dto: ChatMessageDto, roomId: String): ChatMessage {
        val myUid = _loginData.value?.uid
        return buildChatMessage(
            RawChatMessage(
                id = "msg_${dto.id}",
                senderId = dto.uid ?: "",
                content = dto.content,
                type = dto.type,
                timestamp = formatConversationTime(dto.createdAt),
                timestampMs = parseTimestampMs(dto.createdAt),
                serverId = dto.id,
                isMine = dto.uid != null && dto.uid == myUid,
                imageUrl = dto.imageUrl,
                audioUrl = dto.audioUrl,
                audioSource = dto.audioSource,
                senderName = dto.username.orEmpty().ifBlank { memberName(dto.uid) },
                senderAvatar = dto.avatarUrl.orEmpty().ifBlank { memberAvatar(dto.uid) },
                senderGender = dto.gender.orEmpty().ifBlank { memberGender(dto.uid) },
                quotedText = dto.replyPreview,
                quotedIsMine = dto.replyToUserId?.let { it == myUid },
                quotedSenderName = dto.replyUsername
            )
        )
    }

    /**
     * 归一化输入 -> ChatMessage（历史消息与 WS 实时消息共用）
     */
    private fun buildChatMessage(raw: RawChatMessage): ChatMessage {
        val msgType = raw.type.lowercase()
        val isImage = msgType == "image" || !raw.imageUrl.isNullOrBlank()
        // 语音消息 type 为 "audio"；"voice" 是语音通话记录（官网 voiceCallDisplay 单独渲染），二者不可混用
        val isVoiceCall = msgType == "voice"
        val isVoice = msgType == "audio" || (!isVoiceCall && !raw.audioUrl.isNullOrBlank())
        val voiceCallText = if (isVoiceCall) parseVoiceCallText(raw.content) else ""
        val isPat = msgType == "pat" || msgType == "pat_pat" || msgType == "poke"
        val isMusic = msgType == "music"
        val musicData = if (isMusic) parseMusicContent(raw.content) else null
        val isGame = msgType in setOf("gobang", "go", "xiangqi", "chess", "undercover") ||
            (raw.content.trim().startsWith("{") && raw.content.contains("\"game_id\""))
        val gameInvite = if (isGame) parseGameInviteContent(raw.content, msgType) else null
        val isDice = msgType == "dice"
        val diceValue = if (isDice) parseDiceValue(raw.content) else 0
        val patText = if (isPat) buildPatDisplayText(raw.content, raw.senderName, raw.isMine) else ""
        val preview = when {
            isGame -> if (gameInvite != null) "[${GameType.getDisplayName(gameInvite.gameType)}] 对战" else "[游戏对战]"
            isMusic -> if (musicData != null && musicData.name.isNotBlank()) "[歌曲] ${musicData.name}" else "[歌曲]"
            isImage -> "[图片]"
            isPat -> patText.ifBlank { "拍了拍" }
            isVoiceCall -> voiceCallText.ifBlank { "[语音通话]" }
            isVoice -> "[语音]"
            isDice -> if (diceValue in 1..6) "[骰子 $diceValue]" else "[骰子]"
            else -> raw.content
        }
        return ChatMessage(
            id = raw.id,
            senderId = raw.senderId,
            content = raw.content,
            timestamp = raw.timestamp,
            isMine = raw.isMine,
            isVoice = isVoice,
            isVoiceCall = isVoiceCall,
            voiceCallText = voiceCallText,
            isDice = isDice,
            diceValue = diceValue,
            isImage = isImage,
            isMusic = isMusic,
            musicData = musicData,
            isGame = isGame,
            gameInvite = gameInvite,
            imageUrl = raw.imageUrl ?: raw.content.takeIf { isImage } ?: "",
            audioUrl = raw.audioUrl?.takeIf { it.isNotBlank() } ?: raw.content.takeIf { isVoice } ?: "",
            audioSource = raw.audioSource.orEmpty(),
            isPat = isPat,
            patText = patText,
            timestampMs = raw.timestampMs,
            arrivedAtMs = raw.arrivedAtMs,
            serverId = raw.serverId,
            senderName = raw.senderName,
            senderAvatar = raw.senderAvatar,
            senderGender = raw.senderGender,
            quotedText = raw.quotedText,
            quotedIsMine = raw.quotedIsMine,
            quotedSenderName = raw.quotedSenderName,
            previewText = preview
        )
    }

    /**
     * 语音通话记录（WS `type:"voice"`）的展示文案。
     *
     * 对齐官网 `voiceCallDisplay` 的 `v()`：`content` 是 JSON（如 `{"text":"语音通话 03:20"}`），
     * 取 `text` 字段；解析失败原样展示；内容为空展示 `[语音通话]`。
     */
    private fun parseVoiceCallText(content: String): String {
        val raw = content.trim()
        if (raw.isEmpty()) return "[语音通话]"
        val text = runCatching {
            JsonParser.parseString(raw).takeIf { it.isJsonObject }
                ?.asJsonObject?.get("text")
                ?.takeIf { it.isJsonPrimitive }
                ?.asString
                ?.trim()
        }.getOrNull().orEmpty()
        return text.ifBlank { raw }
    }

    /**
     * 解析聊天消息中的网易云音乐 JSON
     */
    fun parseMusicContent(content: String): MomentMusic? = try {
        val trimmed = content.trim()
        if (!trimmed.startsWith("{")) null
        else {
            val obj = JsonParser.parseString(trimmed).asJsonObject
            MomentMusic(
                songId = obj.get("song_id")?.asString.orEmpty(),
                name = obj.get("name")?.asString.orEmpty(),
                artists = obj.get("artists")?.asString.orEmpty(),
                album = obj.get("album")?.asString.orEmpty(),
                coverUrl = obj.get("cover_url")?.asString.orEmpty()
            )
        }
    } catch (e: Exception) {
        null
    }

    /**
     * 解析聊天消息中的游戏卡片 JSON
     */
    fun parseGameInviteContent(content: String, fallbackType: String = ""): ChatGameInvite? = try {
        val trimmed = content.trim()
        if (!trimmed.startsWith("{")) null
        else {
            val invite = gson.fromJson(trimmed, ChatGameInvite::class.java)
            if (invite.gameType.isBlank() && fallbackType.isNotBlank()) {
                invite.copy(gameType = fallbackType)
            } else invite
        }
    } catch (e: Exception) {
        null
    }

    /**
     * 拍一拍 content 解析结果（对齐官网 `pat-*.js` 的 `d()`）
     *
     * 服务端下发形态（实测）：
     * `{"v":1,"sfx":"爹地说：想挨法","a":"ok","t":"kelo","tuid":"eefee851…"}`
     * - `a` 拍的人昵称，`t` 被拍的人昵称；
     * - `sfx` 是**被拍者**的拍一拍后缀（官网资料页原文：「朋友拍了拍我」+ 输入框，「双击头像时显示」）；
     * - `tuid`（32 位 hex）/ `tid`（数字 id）标识被拍者，用于判断这一拍是否落在我身上。
     */
    private data class PatContent(
        val from: String = "",
        val to: String = "",
        val suffix: String = "",
        val targetUid: String = "",
        val targetId: Long = 0L
    )

    /** 32 位 hex uid 形态（官网同款校验） */
    private val PAT_UID_REGEX = Regex("^[0-9a-f]{32}$")

    /**
     * 解析拍一拍 content；非 JSON 或缺少可识别的被拍者标识时返回 null
     */
    private fun parsePatContent(content: String): PatContent? {
        val text = content.trim()
        if (!text.startsWith("{")) return null
        return try {
            val obj = JsonParser.parseString(text).asJsonObject
            fun pick(key: String): String =
                obj.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString.orEmpty().trim()

            val targetUid = pick("tuid").lowercase()
            val targetId = pick("tid").toLongOrNull() ?: 0L
            if (!PAT_UID_REGEX.matches(targetUid) && targetId <= 0L) return null
            PatContent(
                from = pick("a"),
                to = pick("t"),
                suffix = pick("sfx"),
                targetUid = targetUid,
                targetId = targetId
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 被拍者是不是本人（tuid 对 uid，tid 对数字 user_id）
     */
    private fun isPatTargetMe(pat: PatContent): Boolean {
        val login = _loginData.value ?: return false
        val myUid = login.uid.trim().lowercase()
        if (pat.targetUid.isNotBlank() && myUid.isNotBlank() && pat.targetUid == myUid) return true
        val myUserId = login.userId
        return pat.targetId > 0L && myUserId > 0L && pat.targetId == myUserId
    }

    /**
     * 拍一拍展示文案：按「发送方 / 被拍方」归因，做「你 / 我」本地化。
     *
     * - 我拍别人 → `你 拍了拍 <对方昵称><后缀>`
     * - 别人拍我 → `<对方昵称> 拍了拍 你<后缀>`
     * - 群聊第三方 → `<A> 拍了拍 <B><后缀>`
     * - content 不可解析 → 官网兜底「有人拍了拍我」的本地化版本
     *
     * 旧实现只看 `from/to/username` 等键名（服务端实际下发的是 `a/t/tuid`），
     * 导致所有拍一拍都退化成「我 拍了拍 对方 / 对方 拍了拍 我」，主谓关系丢失。
     */
    private fun buildPatDisplayText(rawContent: String, senderName: String, isMine: Boolean): String {
        val pat = parsePatContent(rawContent)
        val patFrom = pat?.from.orEmpty()
        val patTo = pat?.to.orEmpty()

        val from = when {
            isMine -> "你"
            patFrom.isNotBlank() -> patFrom
            senderName.isNotBlank() -> senderName
            else -> "有人"
        }
        val to = when {
            pat != null && isPatTargetMe(pat) -> "你"
            patTo.isNotBlank() -> patTo
            // 兜底与官网一致：无法判断目标时按「拍的是我」呈现
            !isMine -> "你"
            else -> "对方"
        }
        val suffix = pat?.suffix.orEmpty()
        return "$from 拍了拍 $to$suffix"
    }

    /**
     * 骰子点数解析（对齐官网 `_o()`）
     *
     * 服务端 content 就是点数本身（实测 `{"type":"dice","content":"5"}`），
     * 1..6 之外一律视为未知；另外兼容 JSON 形态以防协议演进。
     */
    private fun parseDiceValue(content: String): Int {
        val text = content.trim()
        if (text.isEmpty()) return 0
        text.toIntOrNull()?.let { return if (it in 1..6) it else 0 }
        if (text.startsWith("{")) {
            try {
                val obj = JsonParser.parseString(text).asJsonObject
                for (key in arrayOf("value", "dice", "dice_value", "num", "point", "result")) {
                    val element = obj.get(key)?.takeIf { !it.isJsonNull && it.isJsonPrimitive } ?: continue
                    val value = element.asInt
                    if (value in 1..6) return value
                }
            } catch (_: Exception) {
                // 落到未知点数
            }
        }
        return 0
    }

    fun setRoomMuted(roomId: String, muted: Boolean, onResult: ((Boolean, String?) -> Unit)? = null) {
        scope.launch {
            val result = apiService.roomManagementRequest(
                "/room/mute", mapOf("room_id" to roomId, "muted" to if (muted) "1" else "0")
            )
            if (result.isSuccess) {
                _conversations.value = _conversations.value.map { item ->
                    if (item.id == roomId) item.copy(isMuted = muted) else item
                }
                onResult?.invoke(true, null)
            } else onResult?.invoke(false, result.exceptionOrNull()?.message ?: "免打扰设置失败")
        }
    }

    // ---- 群聊成员与房管权限（GET /api/room/members、GET /api/room/join-bans） ----

    /**
     * 判断某个成员是否就是当前登录账号
     */
    fun isSelfMember(uid: String?, userId: Long): Boolean {
        val login = _loginData.value ?: return false
        if (!uid.isNullOrBlank() && login.uid.isNotBlank() && uid == login.uid) return true
        return userId > 0L && userId == login.userId
    }

    /**
     * 拉取房间成员列表（群聊资料页）
     */
    fun loadRoomMembers(roomId: String, query: String = "") {
        if (_loginData.value == null || roomId.isBlank()) return
        if (_roomMembersLoading.value[roomId] == true) return
        _roomMembersLoading.value = _roomMembersLoading.value + (roomId to true)
        scope.launch {
            val res = apiService.getRoomMembers(roomId = roomId, query = query)
            _roomMembersLoading.value = _roomMembersLoading.value + (roomId to false)
            res.getOrNull()?.let { data ->
                _roomMembers.value = _roomMembers.value + (roomId to data.members)
                _roomMemberTotal.value = _roomMemberTotal.value + (roomId to data.total)
            }
        }
    }

    /**
     * 拉取「禁止加入」名单
     */
    fun loadRoomJoinBans(roomId: String) {
        if (_loginData.value == null || roomId.isBlank()) return
        if (_roomJoinBansLoading.value[roomId] == true) return
        _roomJoinBansLoading.value = _roomJoinBansLoading.value + (roomId to true)
        scope.launch {
            val res = apiService.getRoomJoinBans(roomId)
            _roomJoinBansLoading.value = _roomJoinBansLoading.value + (roomId to false)
            res.getOrNull()?.let { data ->
                _roomJoinBans.value = _roomJoinBans.value + (roomId to data.bans)
            }
        }
    }

    /**
     * 移出成员（POST /room/remove-member），可选同时禁止其再加入
     */
    fun removeRoomMember(
        roomId: String,
        targetUserId: String,
        banJoin: Boolean,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val result = apiService.roomManagementRequest(
                "/room/remove-member",
                mapOf(
                    "room_id" to roomId,
                    "target_user_id" to targetUserId,
                    "ban_join" to if (banJoin) "1" else "0"
                )
            )
            if (result.isSuccess) {
                applyRoomMembersFromResponse(roomId, result.getOrNull())
                if (banJoin) loadRoomJoinBans(roomId)
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, result.exceptionOrNull()?.message ?: "移出成员失败")
            }
        }
    }

    /**
     * 设置 / 取消群成员管理员（POST /room/set-member-admin）
     *
     * 参数与官方 `RoomAdminPermModal` 完全一致：`is_admin`、`can_kick`、`can_delete_message`（0/1）。
     * 取消管理员时两项权限一并置 0。成功后服务端直接返回新的 members 列表，优先就地消费。
     *
     * @param canKick 该管理员能否踢人（仅 isAdmin 为 true 时生效）
     * @param canDeleteMessage 该管理员能否删除他人消息（仅 isAdmin 为 true 时生效）
     */
    fun setRoomMemberAdmin(
        roomId: String,
        targetUserId: String,
        isAdmin: Boolean,
        canKick: Boolean = isAdmin,
        canDeleteMessage: Boolean = isAdmin,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val result = apiService.roomManagementRequest(
                "/room/set-member-admin",
                mapOf(
                    "room_id" to roomId,
                    "target_user_id" to targetUserId,
                    "is_admin" to if (isAdmin) "1" else "0",
                    "can_kick" to if (isAdmin && canKick) "1" else "0",
                    "can_delete_message" to if (isAdmin && canDeleteMessage) "1" else "0"
                )
            )
            if (result.isSuccess) {
                applyRoomMembersFromResponse(roomId, result.getOrNull())
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, result.exceptionOrNull()?.message ?: "管理员设置失败")
            }
        }
    }

    /**
     * 房管接口成功后若直接带回新的 members 列表则就地更新，否则回源重新拉取
     */
    private fun applyRoomMembersFromResponse(roomId: String, data: com.google.gson.JsonObject?) {
        val parsed = data?.let { json ->
            runCatching { apiService.gson.fromJson(json, RoomMembersData::class.java) }.getOrNull()
        }
        val members = parsed?.members.orEmpty()
        if (members.isNotEmpty()) {
            _roomMembers.value = _roomMembers.value + (roomId to members)
            parsed?.total?.takeIf { it > 0 }?.let { total ->
                _roomMemberTotal.value = _roomMemberTotal.value + (roomId to total)
            }
        } else {
            loadRoomMembers(roomId)
        }
    }

    /**
     * 解除「禁止加入」（POST /room/unban-join）
     */
    fun unbanRoomJoin(
        roomId: String,
        targetUserId: String,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val result = apiService.roomManagementRequest(
                "/room/unban-join",
                mapOf("room_id" to roomId, "target_user_id" to targetUserId)
            )
            if (result.isSuccess) {
                loadRoomJoinBans(roomId)
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, result.exceptionOrNull()?.message ?: "解除禁止加入失败")
            }
        }
    }

    /**
     * 房管删除他人消息（POST /room/moderate/delete-message）
     */
    fun moderateDeleteMessage(
        roomId: String,
        messageId: Long,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        if (messageId <= 0L) {
            onResult?.invoke(false, "该消息尚未同步到服务端")
            return
        }
        launchApiCall(
            fallbackMessage = "删除消息失败",
            onError = { onResult?.invoke(false, it) },
            call = {
                apiService.roomManagementRequest(
                    "/room/moderate/delete-message",
                    mapOf("room_id" to roomId, "message_id" to messageId.toString())
                )
            }
        ) {
            _roomMessages.value = _roomMessages.value + (
                roomId to (_roomMessages.value[roomId] ?: emptyList()).filterNot { it.serverId == messageId }
                )
            _hallMessages.value = _hallMessages.value.filterNot { it.serverId == messageId }
            onResult?.invoke(true, null)
        }
    }

    /**
     * 将会话标记为已读
     */
    fun markConversationRead(roomId: String) {
        _conversations.value = _conversations.value.map {
            if (it.id == roomId) it.copy(unreadCount = 0) else it
        }
    }

    /**
     * 更新个人资料 (POST /profile/update)
     */
    fun updateUserProfile(
        username: String,
        bio: String,
        ageRange: String,
        gender: String,
        patText: String = "",
        qq: String? = null,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val genderEn = if (gender == "女") "female" else "male"
            val res = apiService.updateProfile(
                username = username,
                bio = bio,
                ageRange = ageRange,
                gender = genderEn,
                patText = patText,
                qq = qq
            )
            if (res.isSuccess) {
                val updatedData = res.getOrThrow()
                val newName = updatedData.username?.ifBlank { username } ?: username
                val newBio = updatedData.bio ?: bio
                val newAge = updatedData.ageRange ?: ageRange
                val newGender = if ((updatedData.gender ?: genderEn) == "female") "女" else "男"
                val newPat = updatedData.patText ?: patText
                val newQq = updatedData.qq ?: qq ?: _userProfile.value.qq

                _userProfile.value = _userProfile.value.copy(
                    name = newName,
                    bio = newBio,
                    ageRange = newAge,
                    gender = newGender,
                    patText = newPat,
                    qq = newQq
                )
                // 同步更新 bootstrap.user 缓存
                _bootstrapData.value?.user?.let { curUser ->
                    _bootstrapData.value = _bootstrapData.value?.copy(
                        user = curUser.copy(
                            username = newName,
                            bio = newBio,
                            ageRange = newAge,
                            gender = if (newGender == "女") "female" else "male",
                            patText = newPat,
                            qq = newQq
                        )
                    )
                }
                onResult?.invoke(true, null)
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "保存资料失败"
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * 提交/更新头像 (POST /profile/avatar/commit)
     */
    fun commitAvatar(
        avatarUrl: String,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.commitAvatar(avatarUrl)
            if (res.isSuccess) {
                val data = res.getOrNull()
                val updatedUrl = NetworkImageUrl.resolve(data?.avatarUrl?.takeIf { it.isNotBlank() } ?: avatarUrl)
                val curUpload = _userProfile.value.avatarUpload?.copy(hasCustom = true, canUpload = true)
                _userProfile.value = _userProfile.value.copy(
                    avatarUrl = updatedUrl,
                    hasCustomAvatar = true,
                    avatarUpload = curUpload
                )
                _bootstrapData.value?.user?.let { curUser ->
                    _bootstrapData.value = _bootstrapData.value?.copy(
                        user = curUser.copy(
                            avatarUrl = updatedUrl,
                            hasCustomAvatar = true,
                            avatarUpload = curUser.avatarUpload?.copy(hasCustom = true, canUpload = true)
                        )
                    )
                }
                onResult?.invoke(true, null)
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "更新头像失败"
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * 上传自定义头像文件 (官方 uploadAvatar 完整流程)
     */
    fun uploadAvatar(
        fileBytes: ByteArray,
        filename: String = "avatar.png",
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        val curUpload = _userProfile.value.avatarUpload
        if (curUpload != null && !curUpload.canUpload) {
            onResult?.invoke(false, "暂不可上传头像")
            return
        }
        scope.launch {
            val res = apiService.uploadAvatar(fileBytes, filename)
            if (res.isSuccess) {
                val data = res.getOrThrow()
                val updatedUrl = NetworkImageUrl.resolve(data.avatarUrl ?: "")
                val newUpload = _userProfile.value.avatarUpload?.copy(
                    hasCustom = true,
                    canUpload = true
                )
                _userProfile.value = _userProfile.value.copy(
                    avatarUrl = updatedUrl,
                    hasCustomAvatar = true,
                    avatarUpload = newUpload
                )
                _bootstrapData.value?.user?.let { curUser ->
                    _bootstrapData.value = _bootstrapData.value?.copy(
                        user = curUser.copy(
                            avatarUrl = updatedUrl,
                            hasCustomAvatar = true,
                            avatarUpload = curUser.avatarUpload?.copy(hasCustom = true, canUpload = true)
                        )
                    )
                }
                onResult?.invoke(true, null)
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "上传头像失败"
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * 对接官方“刷新头像”逻辑 (对应 Web 端移动端视口下的 <span data-v-058f7f11="">刷新头像</span>)
     * 官方原生标题: title="刷新 QQ 头像"
     * 行为：向服务端重新同步个人资料，并对头像 CDN / 代理接口执行带 _refresh 时间戳的无缓存强刷
     */
    fun refreshAvatar(onResult: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.refreshAvatar()
            if (res.isSuccess) {
                val freshAvatarUrl = res.getOrThrow()
                _userProfile.value = _userProfile.value.copy(avatarUrl = freshAvatarUrl)
                _bootstrapData.value?.user?.let { curUser ->
                    _bootstrapData.value = _bootstrapData.value?.copy(
                        user = curUser.copy(avatarUrl = freshAvatarUrl)
                    )
                }
                onResult?.invoke(true, "头像已刷新")
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "头像刷新失败"
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * 清除自定义头像，恢复默认 (POST /profile/avatar/clear)
     */
    fun clearAvatar(onResult: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.clearAvatar()
            if (res.isSuccess) {
                val data = res.getOrNull()
                val rawUrl = data?.avatarUrl ?: ""
                val defaultUrl = if (rawUrl.isNotBlank()) NetworkImageUrl.resolve(rawUrl) else ""
                val newUpload = _userProfile.value.avatarUpload?.copy(hasCustom = false)
                _userProfile.value = _userProfile.value.copy(
                    avatarUrl = defaultUrl,
                    hasCustomAvatar = false,
                    avatarUpload = newUpload
                )
                _bootstrapData.value?.user?.let { curUser ->
                    _bootstrapData.value = _bootstrapData.value?.copy(
                        user = curUser.copy(
                            avatarUrl = defaultUrl,
                            hasCustomAvatar = false,
                            avatarUpload = curUser.avatarUpload?.copy(hasCustom = false)
                        )
                    )
                }
                onResult?.invoke(true, "已恢复默认头像")
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "清除头像失败"
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * 保存隐私设置 (POST /profile/update_moments_privacy)
     */
    fun updateMomentsPrivacy(
        privacy: MomentsPrivacyData,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.updateMomentsPrivacy(privacy)
            if (res.isSuccess) {
                _privacySettings.value = privacy
                onResult?.invoke(true, null)
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "保存隐私设置失败"
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * 查找用户 (POST /user/lookup)
     */
    suspend fun lookupUser(loginName: String = "", userId: String = ""): Result<UserLookupData> {
        val trimmedLogin = loginName.trim()
        val trimmedId = userId.trim()
        if (trimmedLogin.contains("#")) {
            val parts = trimmedLogin.split("#")
            val l = parts.getOrNull(0)?.trim() ?: ""
            val id = parts.getOrNull(1)?.trim() ?: trimmedId
            return apiService.lookupUser(loginName = l, userId = id)
        }
        return apiService.lookupUser(loginName = trimmedLogin, userId = trimmedId)
    }

    // ============================================================
    // 服务与支持：联系我们 / 问题反馈 / 捐赠本站
    // ============================================================

    /**
     * 联系我们 / 问题反馈 · 页初始化 (GET /api/contact/bootstrap)
     */
    suspend fun getContactBootstrap(): Result<ContactBootstrapData> = apiService.getContactBootstrap()

    /**
     * 问题反馈 · 提交 (POST /contact/submit)
     *
     * 提交成功后由调用方重新拉取 [getContactBootstrap] 刷新「我的反馈」。
     */
    suspend fun submitFeedback(type: String, title: String, content: String): Result<Unit> =
        apiService.submitFeedback(type = type, title = title, content = content)

    /**
     * 捐赠本站 · 页初始化 (GET /api/donate)
     */
    suspend fun getDonate(): Result<DonateData> = apiService.getDonate()

    /**
     * 捐赠本站 · 我的分享码 (GET /api/user/share)
     */
    suspend fun getMyShareCode(): Result<UserShareCodeData> = apiService.getMyShareCode()

    /**
     * 捐赠本站 · 私信捐赠者 (POST /api/donate/dm)
     *
     * ⚠️ 当前 UI **不再调用**：捐赠名单行改为点击进入资料页，私信在资料页内完成（避免同一动作两处入口）。
     * 本方法保留为协议实现备用（官网 DonateView 即用此接口）。
     *
     * 与 [startDmWithOtherUser] 的差异：捐赠者列表只下发 uid（无数字 id），
     * 且官网此处走捐赠页专用接口；成功后同样登记会话并进入房间。
     * 失败信息沿用官方文案口径（不能与自己私聊 / 对方私聊受限）。
     */
    suspend fun startDmWithDonor(donor: DonorDto): Result<ConversationItem> {
        if (!donor.hasIdentity) {
            return Result.failure(IOException("暂时无法识别该用户"))
        }
        val me = _loginData.value
        val isSelf = me != null && (
            (donor.uid.isNotBlank() && donor.uid.equals(me.uid, ignoreCase = true)) ||
                (donor.userId > 0 && donor.userId == me.userId)
            )
        if (isSelf) {
            return Result.failure(IOException("不能与自己私聊"))
        }
        if (!donor.dmAllowed) {
            return Result.failure(IOException("对方的私聊功能已被限制，无法发起私聊"))
        }
        val identifier = donor.uid.takeIf { it.isNotBlank() } ?: donor.userId.toString()
        val res = apiService.createDonateDm(identifier)
        val data = res.getOrNull()
            ?: return Result.failure(res.exceptionOrNull() ?: IOException("创建私聊失败"))
        if (data.roomId.isBlank()) {
            return Result.failure(IOException("创建私聊失败"))
        }
        val conversation = ConversationItem(
            id = data.roomId,
            targetName = donor.displayName.takeIf { it.isNotBlank() } ?: "捐赠者",
            targetAvatar = donor.avatarUrl?.takeIf { it.isNotBlank() },
            targetUserId = donor.userId.takeIf { it > 0 }?.toString().orEmpty(),
            targetUid = donor.uid,
            lastMessage = "点击查看最新消息",
            timestamp = formatConversationTime(null),
            category = MessageCategory.PRIVATE,
            isOnline = null,
            tag = resolveConversationStatusTag(MessageCategory.PRIVATE)
        )
        upsertConversation(conversation)
        enterRoom(data.roomId)
        return Result.success(conversation)
    }

    /**
     * 刷新黑名单列表 (GET /api/block/list)
     */
    fun fetchBlockList(onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onComplete?.invoke(false, "未登录")
            return
        }
        scope.launch {
            val res = apiService.getBlockList()
            if (res.isSuccess) {
                _blockList.value = res.getOrThrow().list
                onComplete?.invoke(true, null)
            } else {
                onComplete?.invoke(false, res.exceptionOrNull()?.message)
            }
        }
    }

    /**
     * 解除黑名单 (POST /block/unblock)
     */
    fun unblockUser(blockedId: Long, onResult: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.unblock(blockedId)
            if (res.isSuccess) {
                _blockList.value = _blockList.value.filter { it.blockedId != blockedId }
                onResult?.invoke(true, null)
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "解除拉黑失败"
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * 添加黑名单 (POST /block/add)
     */
    fun addBlockUser(blockedId: Long, onResult: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.addBlock(blockedId)
            if (res.isSuccess) {
                fetchBlockList()
                onResult?.invoke(true, null)
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "添加黑名单失败"
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * 举报记录 · 拉取第一页 (GET /api/report/list)
     *
     * 对齐官网 `MyReportsView`：`page=1`、`per_page=10`，并记录 `total` / `has_more` / 当前页码。
     */
    fun fetchReportList(onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onComplete?.invoke(false, "未登录")
            return
        }
        scope.launch {
            val res = apiService.getReportList(page = 1, perPage = REPORT_PAGE_SIZE)
            if (res.isSuccess) {
                val data = res.getOrThrow()
                _reportList.value = data.list
                _reportTotal.value = data.total
                _reportHasMore.value = data.hasMore
                _reportPage = data.page.coerceAtLeast(1)
                onComplete?.invoke(true, null)
            } else {
                onComplete?.invoke(false, res.exceptionOrNull()?.message)
            }
        }
    }

    /**
     * 举报记录 · 加载更多（官网底部「加载更多」按钮）
     *
     * 追加下一页；已在加载中或已无更多数据时直接返回。
     */
    fun loadMoreReports(onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onComplete?.invoke(false, "未登录")
            return
        }
        if (_reportLoadingMore.value || !_reportHasMore.value) return
        _reportLoadingMore.value = true
        scope.launch {
            val nextPage = _reportPage + 1
            val res = apiService.getReportList(page = nextPage, perPage = REPORT_PAGE_SIZE)
            _reportLoadingMore.value = false
            if (res.isSuccess) {
                val data = res.getOrThrow()
                _reportList.value = _reportList.value + data.list
                _reportTotal.value = data.total
                _reportHasMore.value = data.hasMore
                _reportPage = data.page.coerceAtLeast(nextPage)
                onComplete?.invoke(true, null)
            } else {
                onComplete?.invoke(false, res.exceptionOrNull()?.message)
            }
        }
    }

    /**
     * 举报用户 (POST /report/submit)
     *
     * 官方字段：`reported_id`、`room_id`、`reason`、`description`；
     * 服务端返回 `room_deleted` 为真时，官方会顺手拉黑对方（本次同样处理）。
     *
     * @param onResult (是否成功, 房间是否因举报被删除, 错误信息)
     */
    fun reportUser(
        reportedId: String,
        roomId: String,
        reason: String,
        description: String,
        onResult: ((Boolean, Boolean, String?) -> Unit)? = null
    ) {
        val target = reportedId.trim()
        if (target.isBlank() || target == "0") {
            onResult?.invoke(false, false, "无法获取被举报用户")
            return
        }
        if (_loginData.value == null) {
            onResult?.invoke(false, false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.roomManagementRequest(
                "/report/submit",
                mapOf(
                    "reported_id" to target,
                    "room_id" to roomId,
                    "reason" to reason,
                    "description" to description.trim()
                )
            )
            if (res.isSuccess) {
                // room_deleted 可能下发布尔或 1/0，asBoolean 对两者都成立
                val deletedFlag = res.getOrNull()?.get("room_deleted")
                val roomDeleted = deletedFlag != null && !deletedFlag.isJsonNull && deletedFlag.asBoolean
                if (roomDeleted) {
                    // 官方行为：房间被删除则直接拉黑对方（失败也不影响举报已提交）
                    apiService.setBlock(target, true)
                    _conversations.value = _conversations.value.filterNot { it.id == roomId }
                }
                onResult?.invoke(true, roomDeleted, null)
            } else {
                onResult?.invoke(false, false, res.exceptionOrNull()?.message ?: "举报提交失败")
            }
        }
    }

    /**
     * 设置对端备注 (POST /user/remark)
     *
     * 成功后刷新房间列表，由服务端下发的最新 `peer.remark` 重建显示名（备注优先，其次昵称）。
     */
    fun setPeerRemark(
        peerUserId: String,
        remark: String,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        if (peerUserId.isBlank()) {
            onResult?.invoke(false, "无法确定备注对象")
            return
        }
        val trimmed = remark.trim()
        if (trimmed.length > 20) {
            onResult?.invoke(false, "备注最多 20 个字")
            return
        }
        scope.launch {
            val res = apiService.setUserRemark(peerUserId, trimmed)
            ZtLog.d(
                "ZeroTalk",
                "[Remark] save peer='" + peerUserId + "' draft='" + trimmed +
                    "' ok=" + res.isSuccess + " err='" + res.exceptionOrNull()?.message +
                    "' saved='" + res.getOrNull() + "'"
            )
            if (res.isSuccess) {
                val saved = res.getOrNull().orEmpty()
                _conversations.value = _conversations.value.map { conv ->
                    if (conv.targetUid == peerUserId || conv.targetUserId == peerUserId) {
                        conv.copy(targetRemark = saved)
                    } else conv
                }
                // 备注保存成功后立即刷新 uid -> 备注 映射（清空备注时同步移除）
                refreshUidRemarks()
                fetchRoomList()
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "保存备注失败")
            }
        }
    }

    /**
     * 举报动态 (POST /moment/report)
     *
     * 官方字段：`moment_id`、`reason`、`description`（可空串）。
     */
    fun reportMoment(
        momentId: Long,
        reason: String,
        description: String,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        if (momentId <= 0L) {
            onResult?.invoke(false, "该动态尚未同步到服务端")
            return
        }
        scope.launch {
            val res = apiService.reportMoment(momentId, reason, description)
            if (res.isSuccess) {
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "举报提交失败")
            }
        }
    }

    // ---- 首聊卡片：我的清流模式就近开关 ----
    private val _myCleanStreamMode = MutableStateFlow(false)
    val myCleanStreamMode: StateFlow<Boolean> = _myCleanStreamMode.asStateFlow()

    /** 我的匹配偏好快照（默认值即服务端缺省，匹配设置面板保存成功后回填） */
    private var myMatchPreferences = MyMatchPreferences()

    /** 匹配设置面板保存后回填本地快照，保证清流模式单独开关时携带的其余字段与服务端一致 */
    fun syncMyMatchPreferences(
        ageRange: String,
        oppositeGenderOnly: Boolean,
        sameGenderOnly: Boolean,
        cleanStreamMode: Boolean
    ) {
        myMatchPreferences = MyMatchPreferences(
            ageRange = ageRange,
            oppositeGenderOnly = oppositeGenderOnly,
            sameGenderOnly = sameGenderOnly,
            cleanStreamMode = cleanStreamMode
        )
        _myCleanStreamMode.value = cleanStreamMode
    }

    /**
     * 就近开关清流模式 (POST /profile/update_match_settings)
     *
     * 官方 `PeerProfileCard` 同款动作：乐观更新 + 失败回滚，提示「清流模式已开启/已关闭」。
     */
    fun setMyCleanStreamMode(enabled: Boolean, onResult: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        val previous = _myCleanStreamMode.value
        _myCleanStreamMode.value = enabled
        scope.launch {
            val res = apiService.updateMatchSettings(
                ageRange = myMatchPreferences.ageRange,
                matchOppositeGenderOnly = if (myMatchPreferences.oppositeGenderOnly) 1 else 0,
                matchSameGenderOnly = if (myMatchPreferences.sameGenderOnly) 1 else 0,
                cleanStreamMode = if (enabled) 1 else 0
            )
            if (res.getOrNull() == true) {
                myMatchPreferences = myMatchPreferences.copy(cleanStreamMode = enabled)
                onResult?.invoke(true, null)
            } else {
                _myCleanStreamMode.value = previous
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "清流设置保存失败")
            }
        }
    }

    /**
     * 处理 WebSocket 事件分发
     */
    private fun handleWsEvent(event: WsServerEvent) {
        when (event) {
            is WsServerEvent.AuthSuccess -> {
                _status.value = ClientStatus.Connected
                // WS 重连：为进行中的通话续期；空闲时尝试按服务端状态恢复通话
                voiceCall.resumeActiveOnReconnect()
                scope.launch { voiceCall.resumeIfNeeded() }
            }
            is WsServerEvent.Matching -> {
                val current = _matchStatus.value
                if (current !is MatchStatus.Matching) {
                    _matchStatus.value = MatchStatus.Matching(isVoice = false)
                }
            }
            is WsServerEvent.MatchSuccess -> {
                matchTimeoutJob?.cancel()
                matchTimeoutJob = null
                wsClient.joinRoom(event.roomId)
                val existingPeer = _roomPeers.value[event.roomId]
                _matchStatus.value = MatchStatus.Matched(event.roomId, existingPeer)
                // 语音匹配：服务端随 match_success 下发 voice 载荷 → 自动接通语音（官网 applyMatchVoiceAuto）
                event.voice?.let { voice ->
                    voiceCall.startAutoFromMatch(voice, fallbackRoomId = event.roomId)
                }
            }
            is WsServerEvent.VoiceCall -> {
                voiceCall.onWsEvent(event)
            }
            is WsServerEvent.UserJoined -> {
                // 无论是否本人，都先把资料记入成员目录，供群聊消息回填发送者信息
                rememberMember(event.uid, event.username, event.gender, event.avatarUrl)

                val myUid = _loginData.value?.uid
                // 排除自身进入房间的事件广播
                if (myUid != null && event.uid == myUid) {
                    return
                }
                val targetRoomId = event.roomId ?: wsClient.currentRoomId ?: ""
                if (targetRoomId.isNotBlank()) {
                    _roomPeers.value = _roomPeers.value + (targetRoomId to event)
                    _conversations.value = _conversations.value.map { conv ->
                        if (conv.id == targetRoomId) {
                            val shouldUpdate = conv.targetName == "神秘零友" || conv.targetName == "神秘人" || conv.targetName == "对方" || conv.targetName.startsWith("匹配房间-")
                            val isOnline = if (conv.category == MessageCategory.CODE) null else true
                            val tag = resolveConversationStatusTag(conv.category)
                            conv.copy(
                                targetName = if (shouldUpdate && event.username.isNotBlank()) event.username else conv.targetName,
                                // 群聊（暗号房）：成员进房不得把群头像刷成该成员头像
                                targetAvatar = if (conv.category == MessageCategory.CODE) conv.targetAvatar else (event.avatarUrl ?: conv.targetAvatar),
                                isOnline = isOnline,
                                tag = tag
                            )
                        } else conv
                    }
                }
                val current = _matchStatus.value
                if (current is MatchStatus.Matched) {
                    _matchStatus.value = current.copy(partner = event)
                }
            }
            is WsServerEvent.MatchCancelled -> {
                _matchStatus.value = MatchStatus.Timeout
            }
            is WsServerEvent.MatchError -> {
                _matchStatus.value = MatchStatus.Error(event.msg)
            }
            // 官网 AppLayout：ws.on("chat_banner", …) —— 通知的权威来源
            is WsServerEvent.ChatBanner -> {
                handleChatBanner(event)
            }
            // 官网 AppLayout：ws.on("notification_unread", v => applyRealtimeUnread(v.unread))
            is WsServerEvent.NotificationUnread -> {
                val now = System.currentTimeMillis()
                if (now - lastUnreadRefreshAtMs >= 1_500L) {
                    lastUnreadRefreshAtMs = now
                    fetchRoomList()
                }
            }
            is WsServerEvent.Message -> {
                val isMine = isMessageFromMe(event.fromUid, event.username, event.isSelf)
                val isPatEvent = event.type.equals("pat", ignoreCase = true)
                val timeStr = event.createdAt?.takeLast(8)?.take(5) ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val targetRoom = event.roomId ?: wsClient.currentRoomId
                ZtLog.d("ZeroTalk", "[MSG] type=${event.type} room=$targetRoom isMine=$isMine")
                // 同一条消息可能同时来自 chat_banner 与 message：用 message_id 去重，避免双弹/双记未读
                val msgKey = event.messageId?.takeIf { it > 0 }?.let { "m_$it" }.orEmpty()
                val alreadyBannered = !isMine && msgKey.isNotBlank() && !markBannerSeen(msgKey)
                val eventMs = parseTimestampMs(event.createdAt)

                // 累积成员资料：群聊消息并非每条都带昵称/头像/性别
                rememberMember(event.fromUid, event.username, event.gender, event.avatarUrl)

                val peer = targetRoom?.let { _roomPeers.value[it] }
                val senderName = event.username.orEmpty()
                    .ifBlank { memberName(event.fromUid) }
                    .ifBlank { if (isMine) _userProfile.value.name else peer?.username.orEmpty() }
                val senderAvatar = event.avatarUrl.orEmpty()
                    .ifBlank { memberAvatar(event.fromUid) }
                    .ifBlank { if (isMine) _userProfile.value.avatarUrl else peer?.avatarUrl.orEmpty() }
                val senderGender = event.gender.orEmpty()
                    .ifBlank { memberGender(event.fromUid) }
                    .ifBlank { if (isMine) "" else peer?.gender.orEmpty() }

                // 类型判定（isImage / isVoice / isPat / preview）与字段映射与历史消息共用
                val newMsg = buildChatMessage(
                    RawChatMessage(
                        id = "msg_${event.messageId ?: System.currentTimeMillis()}",
                        senderId = event.fromUid ?: "peer",
                        content = event.content,
                        type = event.type,
                        timestamp = timeStr,
                        timestampMs = eventMs,
                        arrivedAtMs = System.currentTimeMillis(),
                        serverId = event.messageId ?: 0L,
                        isMine = isMine,
                        imageUrl = event.imageUrl,
                        audioSource = event.audioSource,
                        senderName = senderName,
                        senderAvatar = senderAvatar,
                        senderGender = senderGender,
                        quotedText = event.replyPreview,
                        quotedIsMine = event.replyToUserId?.let { it == _loginData.value?.uid },
                        quotedSenderName = event.replyToUsername
                    )
                )
                val preview = newMsg.previewText

                if (isHallRoom(targetRoom)) {
                    // 大厅消息：先与本地回显对账，避免自己发的消息出现两条
                    val (afterReconcile, matchedEcho, pendingEcho) = reconcilePendingEcho(_hallMessages.value, event, eventMs)
                    _hallMessages.value = when {
                        matchedEcho -> {
                            val merged = if (pendingEcho != null) {
                                newMsg.copy(
                                    quotedText = newMsg.quotedText ?: pendingEcho.quotedText,
                                    quotedIsMine = pendingEcho.quotedIsMine ?: newMsg.quotedIsMine,
                                    quotedSenderName = pendingEcho.quotedSenderName ?: newMsg.quotedSenderName
                                )
                            } else newMsg
                            afterReconcile + merged
                        }
                        // 拍一拍不做本地回显（文案/后缀由服务端生成），回推必须入库
                        isMine && !isPatEvent -> _hallMessages.value
                        else -> _hallMessages.value + newMsg
                    }
                    // 大厅非本人消息实时推到全局通知弹窗
                    if (!isMine) {
                        _newMessageNotification.tryEmit(
                            IncomingMessageNotification(
                                roomName = "零语大厅",
                                content = preview,
                                isGroup = true,
                                roomId = _hallRoomId.value
                            )
                        )
                    }
                } else if (targetRoom != null) {
                    // 单聊 / 暗号房消息
                    val currentList = _roomMessages.value[targetRoom] ?: emptyList()
                    val (afterReconcile, matchedEcho, pendingEcho) = reconcilePendingEcho(currentList, event, eventMs)
                    val updatedList = when {
                        matchedEcho -> {
                            val mergedRoom = if (pendingEcho != null) {
                                newMsg.copy(
                                    quotedText = newMsg.quotedText ?: pendingEcho.quotedText,
                                    quotedIsMine = pendingEcho.quotedIsMine ?: newMsg.quotedIsMine,
                                    quotedSenderName = pendingEcho.quotedSenderName ?: newMsg.quotedSenderName
                                )
                            } else newMsg
                            afterReconcile + mergedRoom
                        }
                        // 拍一拍不做本地回显（文案/后缀由服务端生成），回推必须入库
                        isMine && !isPatEvent -> currentList
                        else -> currentList + newMsg
                    }
                    if (updatedList !== currentList) {
                        _roomMessages.value = _roomMessages.value + (targetRoom to updatedList)
                    }
                    // 更新会话最新消息摘要并置顶
                    val existing = _conversations.value.find { it.id == targetRoom }
                    if (existing != null) {
                        val updated = existing.copy(
                            lastMessage = preview,
                            timestamp = timeStr,
                            // 已由 chat_banner 记过未读的同一条消息不再重复 +1
                            unreadCount = if (isMine || existing.isMuted || alreadyBannered) {
                                existing.unreadCount
                            } else {
                                existing.unreadCount + 1
                            }
                        )
                        _conversations.value = (listOf(updated) + _conversations.value.filter { it.id != targetRoom })
                            .sortedByDescending { it.isPinned }
                    } else if (!isMine && !isHallRoom(targetRoom)) {
                        // 会话行还没进列表（例如对方新发起的私聊）：拉一次房间列表补上，
                        // 否则会话行与未读数都不会出现
                        fetchRoomList()
                    }
                }
                // 非本人消息实时推送到全局新消息通知弹窗。
                // 服务端不一定对每条消息都发 chat_banner（实测进入房间后只有 message），
                // 所以这里是必要通道；同一条消息已由 chat_banner 弹过则跳过，
                // 且「前台 + 正在看这个房间」时同样抑制（对齐官网）。
                val viewingThisRoom = SystemNotificationBridge.isAppInForeground && targetRoom == viewingRoomId
                if (!isMine && !alreadyBannered && !viewingThisRoom) {
                    val roomName = if (isHallRoom(targetRoom)) {
                        "零语大厅"
                    } else {
                        _conversations.value.find { it.id == targetRoom }?.targetName
                            ?: peer?.username
                            ?: targetRoom
                            ?: "聊天"
                    }
                    val isGroup = isHallRoom(targetRoom) ||
                        (_groupRooms.value[targetRoom] == true)
                    _newMessageNotification.tryEmit(
                        IncomingMessageNotification(
                            roomName = roomName,
                            senderName = if (!isGroup) senderName else "",
                            content = preview,
                            isGroup = isGroup,
                            roomId = targetRoom.orEmpty()
                        )
                    )
                }
            }
            is WsServerEvent.GameUpdate -> {
                if (event.game != null) {
                    if (_activeGameId.value == event.game.id) {
                        _activeGameSession.value = resolveGameSession(_activeGameSession.value, event.game)
                    }
                }
                // 更新房间卡片状态
                val targetRoom = event.roomId ?: wsClient.currentRoomId
                if (targetRoom != null && event.invite != null) {
                    val currentList = _roomMessages.value[targetRoom] ?: emptyList()
                    val updated = currentList.map { msg ->
                        if (msg.gameInvite?.gameId == event.invite.gameId || (event.messageId != null && msg.serverId == event.messageId)) {
                            msg.copy(gameInvite = event.invite)
                        } else msg
                    }
                    _roomMessages.value = _roomMessages.value + (targetRoom to updated)
                }
            }
            is WsServerEvent.GameJoined -> {
                if (event.game != null) {
                    _activeGameSession.value = event.game
                    _activeGameId.value = event.game.id
                    _activeGameType.value = event.gameType
                }
            }
            is WsServerEvent.GameError -> {
                _activeGameError.value = event.message
                if (event.game != null) {
                    _activeGameSession.value = event.game
                }
            }
            is WsServerEvent.UserLeft, is WsServerEvent.RoomClosed -> {
                val targetRoomId = (event as? WsServerEvent.UserLeft)?.roomId
                    ?: (event as? WsServerEvent.RoomClosed)?.roomId
                    ?: wsClient.currentRoomId
                if (!targetRoomId.isNullOrBlank() && !isHallRoom(targetRoomId)) {
                    _roomPeers.value = _roomPeers.value - targetRoomId
                    _conversations.value = _conversations.value.map { conv ->
                        if (conv.id == targetRoomId && conv.category != MessageCategory.CODE) {
                            conv.copy(
                                isOnline = false,
                                tag = null
                            )
                        } else conv
                    }
                }
            }
            else -> {}
        }
    }

    private fun formatConversationTime(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "刚刚"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = parser.parse(dateStr) ?: return dateStr.takeLast(8).take(5)
            val now = Calendar.getInstance()
            val msgCal = Calendar.getInstance().apply { time = date }

            if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
            ) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            } else if (now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1
            ) {
                "昨天"
            } else {
                SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
            }
        } catch (_: Exception) {
            if (dateStr.length >= 16) dateStr.substring(11, 16) else dateStr
        }
    }

    private fun parseTimestampMs(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateStr)?.time
                ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    // ============================================================
    // 「我的」页面功能动作：安全中心 / 修改密码 / 处罚减免 / 网易云绑定
    // ============================================================

    /**
     * 安全中心 · 拉取登录设备 (GET /api/security/devices)
     */
    fun loadSecurityDevices(onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onComplete?.invoke(false, "请先登录账号")
            return
        }
        if (_securityDevicesLoading.value) return
        _securityDevicesLoading.value = true
        scope.launch {
            val res = apiService.getSecurityDevices()
            _securityDevicesLoading.value = false
            if (res.isSuccess) {
                _securityDevices.value = res.getOrNull()?.devices.orEmpty()
                onComplete?.invoke(true, null)
            } else {
                onComplete?.invoke(false, res.exceptionOrNull()?.message ?: "获取设备列表失败")
            }
        }
    }

    /**
     * 安全中心 · 注销指定设备 (POST /api/security/devices/revoke)
     *
     * 注销本机时服务端会同时作废当前会话，这里直接复用 [logout] 清理本地登录态
     * （断开 WS、清 Cookie 与本地会话），并通过回调把「是否本机」告知 UI。
     *
     * @param onResult (是否成功, 是否为当前设备, 错误信息)
     */
    fun revokeSecurityDevice(
        deviceId: String,
        onResult: ((Boolean, Boolean, String?) -> Unit)? = null
    ) {
        val target = deviceId.trim()
        if (target.isBlank()) {
            onResult?.invoke(false, false, "缺少设备标识")
            return
        }
        if (_loginData.value == null) {
            onResult?.invoke(false, false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.revokeSecurityDevice(target)
            if (res.isSuccess) {
                val isCurrent = res.getOrNull()?.isCurrent == true
                if (isCurrent) {
                    // 本机被注销：立刻断开连接并退出登录
                    logout()
                } else {
                    loadSecurityDevices()
                }
                onResult?.invoke(true, isCurrent, null)
            } else {
                onResult?.invoke(false, false, res.exceptionOrNull()?.message ?: "注销失败")
            }
        }
    }

    /**
     * 安全中心 · 注销其他所有设备 (POST /api/security/devices/revoke-others)
     *
     * 官方 API 具备该端点但未在页面暴露入口，这里保留动作供 UI 主动调用。
     */
    fun revokeOtherSecurityDevices(onResult: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.revokeOtherSecurityDevices()
            if (res.isSuccess) {
                loadSecurityDevices()
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "注销失败")
            }
        }
    }

    /**
     * 安全中心 · 拉取登录历史 (GET /api/security/login-history)
     *
     * 官方的「滚动到底继续加载」语义：每页 5 条，`total` 与已加载条数比较判断是否还有更多。
     *
     * @param reset true 表示从第一页重新拉取（首次进入或切换事件筛选）
     * @param eventType 事件筛选，空串表示全部事件
     */
    fun loadSecurityLoginHistory(
        reset: Boolean = false,
        eventType: String? = null,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onComplete?.invoke(false, "请先登录账号")
            return
        }
        if (_securityLoginLoading.value) return

        val filter = eventType ?: _securityLoginEventType.value
        val nextPage = if (reset) 1 else securityLoginPage + 1
        if (reset) {
            securityLoginPage = 1
            _securityLoginEventType.value = filter
            _securityLoginLogs.value = emptyList()
        }

        _securityLoginLoading.value = true
        scope.launch {
            val res = apiService.getSecurityLoginHistory(
                page = nextPage,
                limit = SECURITY_LOGIN_PAGE_SIZE,
                eventType = filter
            )
            _securityLoginLoading.value = false
            if (res.isSuccess) {
                val data = res.getOrNull()
                // 官方：table_ready !== false，缺省视为就绪
                _securityLoginTableReady.value = data?.tableReady != false
                _securityLoginTotal.value = data?.total ?: 0
                securityLoginPage = data?.page?.takeIf { it > 0 } ?: nextPage
                val incoming = data?.list.orEmpty()
                _securityLoginLogs.value = if (reset) {
                    incoming
                } else {
                    (_securityLoginLogs.value + incoming).distinctBy { log ->
                        if (log.id > 0L) log.id.toString()
                        else "${log.createdAt}|${log.eventType}|${log.ip}"
                    }
                }
                onComplete?.invoke(true, null)
            } else {
                onComplete?.invoke(false, res.exceptionOrNull()?.message ?: "获取登录历史失败")
            }
        }
    }

    /**
     * 修改密码（复用 POST /profile/update）
     *
     * `username` / `qq` 必须带上当前账号已有资料，否则服务端会把昵称一并覆盖为空。
     */
    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        val profile = _userProfile.value
        val username = profile.name.takeIf { it.isNotBlank() && it != "未登录" }
            ?: _loginData.value?.username.orEmpty()
        if (username.isBlank()) {
            onResult?.invoke(false, "无法获取当前昵称，请稍后重试")
            return
        }
        val qq = profile.qq.takeIf { it.isNotBlank() }
        scope.launch {
            val res = apiService.changePassword(
                username = username,
                qq = qq,
                currentPassword = currentPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
            if (res.isSuccess) {
                res.getOrNull()?.let { data ->
                    if (!data.username.isNullOrBlank()) {
                        _userProfile.value = _userProfile.value.copy(name = data.username)
                    }
                    data.qq?.takeIf { it.isNotBlank() }?.let { newQq ->
                        _userProfile.value = _userProfile.value.copy(qq = newQq)
                    }
                }
                onResult?.invoke(true, null)
            } else {
                // 服务端会直接下发「当前密码不正确」这类 msg，原样透出
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "密码修改失败")
            }
        }
    }

    /**
     * 处罚减免 · 页面初始化 (GET /api/penalty-appeal/bootstrap)
     */
    fun loadPenaltyAppeal(onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onComplete?.invoke(false, "请先登录账号")
            return
        }
        _penaltyLoading.value = true
        scope.launch {
            val res = apiService.getPenaltyAppealBootstrap()
            _penaltyLoading.value = false
            if (res.isSuccess) {
                _penaltyBootstrap.value = res.getOrNull()
                onComplete?.invoke(true, null)
            } else {
                onComplete?.invoke(false, res.exceptionOrNull()?.message ?: "加载失败")
            }
        }
    }

    /**
     * 处罚减免 · 提交申请 (POST /api/penalty-appeal/submit)
     *
     * 提交成功后重新拉取 bootstrap，让「我的申请」状态立刻刷新。
     */
    fun submitPenaltyAppeal(
        penaltyKeys: List<String>,
        evidenceUrls: List<String>,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.submitPenaltyAppeal(
                penaltyKeys = penaltyKeys,
                evidenceUrls = evidenceUrls,
                ackGuidelines = true
            )
            if (res.isSuccess) {
                loadPenaltyAppeal()
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "提交失败")
            }
        }
    }

    /**
     * 处罚减免 · 上传手写检讨图片
     *
     * @param onResult (是否成功, 图片地址, 错误信息)
     */
    fun uploadPenaltyEvidence(
        fileBytes: ByteArray,
        filename: String,
        contentType: String,
        onResult: ((Boolean, String?, String?) -> Unit)? = null
    ) {
        if (_loginData.value == null) {
            onResult?.invoke(false, null, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.uploadPenaltyEvidence(fileBytes, filename, contentType)
            if (res.isSuccess) {
                val url = res.getOrNull().orEmpty()
                if (url.isBlank()) {
                    onResult?.invoke(false, null, "上传失败")
                } else {
                    onResult?.invoke(true, url, null)
                }
            } else {
                onResult?.invoke(false, null, res.exceptionOrNull()?.message ?: "上传失败")
            }
        }
    }

    /**
     * 网易云绑定 · 拉取绑定状态 (GET /api/music/netease/binding)
     */
    fun loadNeteaseBinding(onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onComplete?.invoke(false, "请先登录账号")
            return
        }
        _neteaseLoading.value = true
        scope.launch {
            val res = apiService.getNeteaseBinding()
            _neteaseLoading.value = false
            if (res.isSuccess) {
                _neteaseBinding.value = mergeNeteaseBinding(res.getOrNull())
                onComplete?.invoke(true, null)
            } else {
                onComplete?.invoke(false, res.exceptionOrNull()?.message ?: "加载失败")
            }
        }
    }

    /**
     * 网易云绑定 · 生成扫码会话 (POST /api/music/netease/binding/qrcode/start)
     *
     * 轮询由 UI 层协程负责（离开页面/切 Tab 即可取消），这里只发起一次请求。
     */
    fun startNeteaseQrBind(onResult: ((Boolean, NeteaseQrStartData?, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onResult?.invoke(false, null, "请先登录账号")
            return
        }
        scope.launch {
            val res = apiService.startNeteaseQrBind()
            if (res.isSuccess) {
                onResult?.invoke(true, res.getOrNull(), null)
            } else {
                onResult?.invoke(false, null, res.exceptionOrNull()?.message ?: "生成二维码失败")
            }
        }
    }

    /**
     * 网易云绑定 · 查询扫码状态 (GET /api/music/netease/binding/qrcode/status)
     *
     * 挂起版：供 UI 在协程里循环轮询，离开页面 / 切换 Tab 时协程取消即自动停止。
     */
    suspend fun pollNeteaseQrStatus(sessionId: String): Result<NeteaseQrStatusData?> {
        if (sessionId.isBlank()) {
            return Result.failure(IOException("扫码会话已失效"))
        }
        val res = apiService.getNeteaseQrStatus(sessionId)
        if (res.isSuccess) {
            res.getOrNull()?.binding?.let { binding -> _neteaseBinding.value = mergeNeteaseBinding(binding) }
        }
        return res
    }

    /**
     * 网易云绑定 · 作废本次二维码 (POST /api/music/netease/binding/qrcode/cancel)
     */
    fun cancelNeteaseQrBind(sessionId: String) {
        if (sessionId.isBlank()) return
        scope.launch { apiService.cancelNeteaseQrBind(sessionId) }
    }

    /**
     * 网易云绑定 · 粘贴 Cookie 绑定 (POST /api/music/netease/binding/cookie)
     *
     * @param onResult (是否成功, 错误信息, 绑定后的最新状态)
     */
    fun bindNeteaseCookie(
        cookie: String,
        onResult: ((Boolean, String?, NeteaseBindingData?) -> Unit)? = null
    ) {
        val value = cookie.trim()
        if (value.isBlank()) {
            onResult?.invoke(false, "请粘贴网易云 Cookie", null)
            return
        }
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号", null)
            return
        }
        _neteaseLoading.value = true
        scope.launch {
            val res = apiService.bindNeteaseCookie(value)
            _neteaseLoading.value = false
            if (res.isSuccess) {
                val merged = mergeNeteaseBinding(res.getOrNull())
                    ?: _neteaseBinding.value?.copy(bound = true, status = "active")
                _neteaseBinding.value = merged
                onResult?.invoke(true, null, merged)
            } else {
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "绑定失败", null)
            }
        }
    }

    /**
     * 网易云绑定 · 解除绑定 (POST /api/music/netease/binding/unbind)
     */
    fun unbindNetease(onResult: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        _neteaseLoading.value = true
        scope.launch {
            val res = apiService.unbindNetease()
            _neteaseLoading.value = false
            if (res.isSuccess) {
                // 官方解绑后本地状态重置为「未绑定」，但保留须知文案与建表状态
                val previous = _neteaseBinding.value
                _neteaseBinding.value = (res.getOrNull() ?: NeteaseBindingData()).copy(
                    bound = false,
                    status = "none",
                    neteaseUid = 0,
                    nickname = "",
                    avatarUrl = "",
                    vipType = 0,
                    vipHint = "",
                    disclaimer = res.getOrNull()?.disclaimer?.takeIf { it.isNotBlank() }
                        ?: previous?.disclaimer.orEmpty(),
                    tableReady = previous?.tableReady != false
                )
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "解绑失败")
            }
        }
    }

    /**
     * 网易云绑定 · 刷新登录态 (POST /api/music/netease/binding/refresh)
     */
    fun refreshNeteaseBinding(onResult: ((Boolean, String?) -> Unit)? = null) {
        if (_loginData.value == null) {
            onResult?.invoke(false, "请先登录账号")
            return
        }
        _neteaseLoading.value = true
        scope.launch {
            val res = apiService.refreshNeteaseBinding()
            _neteaseLoading.value = false
            if (res.isSuccess) {
                mergeNeteaseBinding(res.getOrNull())?.let { _neteaseBinding.value = it }
                onResult?.invoke(true, null)
            } else {
                onResult?.invoke(false, res.exceptionOrNull()?.message ?: "续期失败")
            }
        }
    }

    /**
     * 合并网易云绑定状态：官方用 `{...旧值, ...新值}` 语义，避免局部返回丢掉须知与建表状态
     */
    private fun mergeNeteaseBinding(incoming: NeteaseBindingData?): NeteaseBindingData? {
        if (incoming == null) return _neteaseBinding.value
        val previous = _neteaseBinding.value
        return incoming.copy(
            disclaimer = incoming.disclaimer.takeIf { it.isNotBlank() }
                ?: previous?.disclaimer.orEmpty(),
            tableReady = incoming.tableReady && previous?.tableReady != false,
            vipHint = incoming.vipHint.takeIf { it.isNotBlank() } ?: previous?.vipHint.orEmpty(),
            nickname = incoming.nickname.takeIf { it.isNotBlank() } ?: previous?.nickname.orEmpty(),
            avatarUrl = incoming.avatarUrl.takeIf { it.isNotBlank() } ?: previous?.avatarUrl.orEmpty()
        )
    }

    /** 安全中心登录历史每页条数（对齐官方 limit=5） */
    private const val SECURITY_LOGIN_PAGE_SIZE = 5

    private fun formatRelativeTime(createdAt: String): String {
        val trimmed = createdAt.trim()
        if (trimmed.isBlank()) return "刚刚"
        return try {
            val epoch = trimmed.toLongOrNull()
            val dateMs: Long = if (epoch != null) {
                if (epoch < 10_000_000_000L) epoch * 1000L else epoch
            } else {
                val hasUtcZ = trimmed.endsWith("Z", ignoreCase = true)
                var parsed: Date? = null
                if (hasUtcZ) {
                    val clean = trimmed.substringBefore("Z").replace("T", " ")
                    val pattern = if (clean.contains(".")) "yyyy-MM-dd HH:mm:ss.SSS" else "yyyy-MM-dd HH:mm:ss"
                    try {
                        val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }
                        parsed = sdf.parse(clean.substringBefore(".").let { if (clean.contains(".")) clean else "$it.000" })
                    } catch (_: Exception) {}
                }

                if (parsed == null) {
                    val normalized = trimmed.replace("T", " ").replace("Z", "")
                    val clean = if (normalized.contains(".")) normalized.substringBefore(".") else normalized
                    val formats = listOf(
                        "yyyy-MM-dd HH:mm:ss",
                        "yyyy-MM-dd HH:mm",
                        "yyyy-MM-dd"
                    )
                    // 服务端如不带时区信息，缺省为北京时间 (GMT+8)
                    val beijingTz = java.util.TimeZone.getTimeZone("GMT+8")
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.getDefault()).apply {
                                timeZone = beijingTz
                            }
                            parsed = sdf.parse(clean)
                            if (parsed != null) break
                        } catch (_: Exception) {}
                    }
                }

                parsed?.time ?: return createdAt
            }

            val now = System.currentTimeMillis()
            val diffMs = now - dateMs

            val minuteMs = 60 * 1000L
            val hourMs = 60 * minuteMs
            val dayMs = 24 * hourMs
            val weekMs = 7 * dayMs

            val calTarget = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8")).apply { timeInMillis = dateMs }
            val calNow = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8")).apply { timeInMillis = now }

            fun formatDate(cTarget: java.util.Calendar, cNow: java.util.Calendar): String {
                val month = cTarget.get(java.util.Calendar.MONTH) + 1
                val day = cTarget.get(java.util.Calendar.DAY_OF_MONTH)
                return if (cTarget.get(java.util.Calendar.YEAR) == cNow.get(java.util.Calendar.YEAR)) {
                    "${month}月${day}日"
                } else {
                    "${cTarget.get(java.util.Calendar.YEAR)}年${month}月${day}日"
                }
            }

            // 时钟微小偏差（如未来 5 分钟内），归为「刚刚」；超过则显示具体日期
            if (diffMs < minuteMs) {
                if (diffMs >= -5 * minuteMs) "刚刚" else formatDate(calTarget, calNow)
            } else {
                when {
                    diffMs < hourMs -> "${diffMs / minuteMs}分钟前"
                    diffMs < dayMs -> "${diffMs / hourMs}小时前"
                    diffMs < weekMs -> "${diffMs / dayMs}天前"
                    else -> formatDate(calTarget, calNow)
                }
            }
        } catch (_: Exception) {
            createdAt
        }
    }
}

/**
 * 我的匹配偏好本地快照
 *
 * 官方 `update_match_settings` 会一次性回传全部偏好字段，而 App 目前只管理其中 4 项
 * （年龄范围、只匹配异性 / 同性、清流模式）。这张快照用于「清流模式」单独开关时
 * 携带其余已知字段，避免把网页端设置的位置 / 隐私 / 筛选等字段覆盖成默认值。
 */
/** 语音通话各阶段超时下限（官网把小于 10 秒的配置按 10 秒处理） */
private const val MIN_VOICE_TIMEOUT_SECONDS = 10

private data class MyMatchPreferences(
    val ageRange: String = "18-23",
    val oppositeGenderOnly: Boolean = false,
    val sameGenderOnly: Boolean = false,
    val cleanStreamMode: Boolean = false
)

