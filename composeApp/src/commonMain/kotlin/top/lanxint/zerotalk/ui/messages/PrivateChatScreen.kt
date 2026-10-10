package top.lanxint.zerotalk.ui.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import top.lanxint.zerotalk.data.model.ChatMessage
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.data.model.MomentShareCardData
import top.lanxint.zerotalk.data.model.toMomentItem
import top.lanxint.zerotalk.ui.moments.MomentSheetsHost
import top.lanxint.zerotalk.data.model.ConversationItem
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.ChatComposerAction
import top.lanxint.zerotalk.ui.components.ChatComposerBar
import top.lanxint.zerotalk.ui.components.ChatComposerQuote
import top.lanxint.zerotalk.ui.components.ChatContentCaptureLayer
import top.lanxint.zerotalk.ui.components.GenderBadge
import top.lanxint.zerotalk.ui.components.UserTitleBadge
import top.lanxint.zerotalk.ui.components.IosChatNavBar
import top.lanxint.zerotalk.ui.components.LiquidButton
import top.lanxint.zerotalk.ui.components.LocalImageViewer
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.ContextMenuItem
import top.lanxint.zerotalk.ui.components.MessageContextMenuOverlay
import top.lanxint.zerotalk.ui.components.buildContextMenuItems
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.navigation.rememberHasUserProfileLayers
import top.lanxint.zerotalk.ui.navigation.rememberUserProfileLayerSlot
import top.lanxint.zerotalk.ui.utils.BackHandler
import top.lanxint.zerotalk.ui.utils.MomentAudioFormat
import top.lanxint.zerotalk.ui.utils.decodeByteArrayToImageBitmap
import top.lanxint.zerotalk.ui.utils.rememberMomentAudioPlayer
import top.lanxint.zerotalk.ui.utils.rememberMomentAudioRecorder
import top.lanxint.zerotalk.ui.utils.rememberPhotoPickerLauncher
import top.lanxint.zerotalk.ui.utils.rememberAudioFilePickerLauncher
import top.lanxint.zerotalk.ui.theme.AppleHigColorTokens
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.CircularProgressIndicator
import top.lanxint.zerotalk.data.model.MessageCategory
import top.lanxint.zerotalk.data.model.REAL_USER_ID_PLACEHOLDERS
import top.lanxint.zerotalk.data.model.UserProfileLayerHost
import top.lanxint.zerotalk.data.model.UserProfileTarget
import top.lanxint.zerotalk.data.model.quotePreviewText
import top.lanxint.zerotalk.data.model.resolveQuotedText
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.NetworkImageLoader
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.lazy.LazyListState
import top.lanxint.zerotalk.ui.components.AppleModalBottomSheet
import top.lanxint.zerotalk.ui.components.SheetAction
import top.lanxint.zerotalk.ui.moments.MomentNeteasePickerSheet
import top.lanxint.zerotalk.ui.sheets.RoomMusicSheet
import top.lanxint.zerotalk.ui.sheets.GamePickerSheet
import top.lanxint.zerotalk.ui.sheets.VoiceCallMemberPickerSheet
import top.lanxint.zerotalk.ui.sheets.MyGameSessionsSheet
import top.lanxint.zerotalk.ui.game.GameSessionScreen

/**
 * 全面复刻 iOS 26 / iMessage 风格的单聊详情页面 (PrivateChatScreen)
 *
 * 核心特性：
 * 1. 沉浸式壁纸体系：默认素雅纯色背景，支持通过顶部设置切换高清雨滴玻璃窗壁纸，自动触发“你更改了背景”系统消息；
 * 2. 顶部原生导航：
 *    - 左侧毛玻璃胶囊 `< 520`（返回并展示未读计数）；
 *    - 居中组合：圆形头像 + 悬浮胶囊昵称 `[姓名] ❯` + “iMessage 信息 / 🔒已加密”；
 *    - 右侧毛玻璃圆形搜索按键（🔍，私聊与群聊行为对齐，语音通话移入底部 [+] 扩展面板）；
 * 3. 经典气泡体系：
 *    - 私聊：带尖尾气泡（MessageBubbleTailShape）——对方来信为带左下尖尾的半透明深灰烟熏毛玻璃气泡（壁纸上通透自然），
 *      我方发出为带右下尖尾的 iOS 系统纯蓝气泡（#007AFF）；
 *    - 群聊（暗号房）：QQ 群聊式布局——头像独占最左一列，右侧竖排「昵称在上、气泡在下」，
 *      昵称/性别仅在同一发送者连续消息的首条展示；气泡为正常的圆气泡（四角同半径，无尖尾）；
 *      自己的消息不展示头像与昵称，蓝色圆气泡靠右；
 *    - 消息长按交互：唤出毛玻璃悬浮菜单（复制、引用、撤回），撤回后就地标记为「该消息已被撤回」（不再移除）；
 * 4. 底部输入与 [+] 扩展栏：
 *    - 左侧独立毛玻璃圆钮 `+`，点击呼出包含“照片”、“文件”、“骰子”、“游戏”的扩展面板；
 *    - 胶囊输入框（CapsuleGlassButton 架构），内置麦克风切换；
 *    - 右侧动态图标：空文本时为经典垂直声波图标（||||||），输入文本时平滑渐变为纯蓝圆形发送箭头（⬆）。
 */
/**
 * 选中的气泡在屏幕中的锚点位置及上下文信息
 */
private data class BubbleAnchorInfo(
    val message: ChatMessage,
    val bounds: Rect,
    val hasTail: Boolean,
    val isLastMineMessage: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatScreen(
    conversation: ConversationItem,
    onBack: () -> Unit,
    backdrop: Backdrop? = null,
    isDark: Boolean,
    onOpenConversation: ((ConversationItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current
    val coroutineScope = rememberCoroutineScope()

    // 进入房间时自动加入 WebSocket 房间并拉取真实历史聊天记录
    LaunchedEffect(conversation.id) {
        ZeroTalkClientManager.enterRoom(conversation.id)
    }

    // 会话消息列表
    var messageList by remember(conversation.id) {
        mutableStateOf<List<ChatMessage>>(emptyList())
    }

    val roomMessagesMap by ZeroTalkClientManager.roomMessages.collectAsState()
    val liveMessages = roomMessagesMap[conversation.id] ?: emptyList()

    // 历史分页状态（上拉到顶继续上拉时拉取更早消息）
    val hasMoreHistoryMap by ZeroTalkClientManager.roomHasMoreHistory.collectAsState()
    val historyLoadingMap by ZeroTalkClientManager.roomHistoryLoading.collectAsState()
    val hasMoreHistory = hasMoreHistoryMap[conversation.id] == true
    val isLoadingHistory = historyLoadingMap[conversation.id] == true

    // 群聊判定：优先后端下发的房间类型，其次回落到会话分类（暗号）
    val groupRoomsMap by ZeroTalkClientManager.groupRooms.collectAsState()
    val isGroupRoom = groupRoomsMap[conversation.id] == true ||
        conversation.category == MessageCategory.CODE

    // 房管权限（viewer_can_delete_message）：可删除本房他人消息
    val roomBootstrapMap by ZeroTalkClientManager.roomBootstrapMap.collectAsState()
    val canModerateDeleteMessage = roomBootstrapMap[conversation.id]?.viewerCanDeleteMessage == true

    // ---- 首聊「对方信息卡片」（对齐官网 PeerProfileCard，私聊 / 匹配房间展示）----
    val roomPeers by ZeroTalkClientManager.roomPeers.collectAsState()
    val peerProfileState by ZeroTalkClientManager.otherUserProfile.collectAsState()
    val myCleanStreamEnabled by ZeroTalkClientManager.myCleanStreamMode.collectAsState()
    val peerProfile = peerProfileState?.profile
    val peerUid = conversation.targetUid.ifBlank { roomPeers[conversation.id]?.uid.orEmpty() }
    val peerUserId = conversation.targetUserId
    val showPeerCard = !isGroupRoom && (peerUid.isNotBlank() || peerUserId.isNotBlank())
    // 举报 / 清流就近开关的临时状态
    var showPeerReport by remember(conversation.id) { mutableStateOf(false) }
    var reportSubmitting by remember(conversation.id) { mutableStateOf(false) }
    var cleanStreamSaving by remember(conversation.id) { mutableStateOf(false) }

    // 卡片需要对方资料（昵称 / 签名 / 清流状态）：进入私聊时按需拉取聚合资料
    LaunchedEffect(conversation.id, peerUserId, peerUid) {
        if (!isGroupRoom && (peerUserId.isNotBlank() || peerUid.isNotBlank())) {
            val current = ZeroTalkClientManager.otherUserProfile.value
            val alreadyLoaded = current?.profile != null && (
                (peerUserId.isNotBlank() && current.userId == peerUserId) ||
                    (peerUid.isNotBlank() && current.uid == peerUid)
                )
            if (!alreadyLoaded) {
                ZeroTalkClientManager.loadOtherUserProfileSilently(peerUserId, peerUid)
            }
        }
    }

    // 实时同步：以管理器中的顺序为准（上拉分页会在列表头部插入更早的消息）。
    // 只保留「管理器尚未回传、且由屏幕侧本地独占生成」的消息（id 以 m_ 或 sys_ 开头），
    // 避免把管理器的旧回显或屏幕侧冗余文本回显复活为重复条目。
    LaunchedEffect(liveMessages) {
        if (liveMessages.isNotEmpty()) {
            val liveIds = liveMessages.map { it.id }.toSet()
            val tailMs = liveMessages.last().timestampMs
            val pendingLocal = messageList.filter {
                it.id !in liveIds &&
                    it.timestampMs >= tailMs &&
                    (it.id.startsWith("m_") || it.id.startsWith("sys_"))
            }
            messageList = liveMessages + pendingLocal
        }
    }

    var inputText by remember { mutableStateOf("") }
    var mentionTargets by remember(conversation.id) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isVoiceMode by remember { mutableStateOf(false) }

    // 聊天语音：实测链路为 webm 录音 → chat_audio 上传 → WS type=audio
    val chatAudioRecorder = rememberMomentAudioRecorder(MomentAudioFormat.WEBM)
    var isVoiceCancelZone by remember { mutableStateOf(false) }

    // 聊天图片：相册选图 → chat_image 上传 → WS type=image
    val chatPhotoPicker = rememberPhotoPickerLauncher(
        maxItems = 1,
        onImagesSelected = { picked ->
            val photo = picked.firstOrNull() ?: return@rememberPhotoPickerLauncher
            ZeroTalkClientManager.sendRoomImage(
                roomId = conversation.id,
                imageBytes = photo.byteArray,
                filename = "chat_image_${System.currentTimeMillis()}.jpg",
                onError = { notificationState.show(it) }
            )
        },
        onPermissionDenied = {
            notificationState.show("需要相册读取权限以发送图片，请在系统设置中允许")
        }
    )

    // 聊天语音文件（选本地音频）：实测链路 presign(upload_source=chat_audio, audio_source=file) → PUT → bind → WS type=audio
    val chatAudioFilePicker = rememberAudioFilePickerLauncher(
        onAudioSelected = { audioFile ->
            ZeroTalkClientManager.sendRoomAudio(
                roomId = conversation.id,
                audioBytes = audioFile.byteArray,
                // 选文件时已由平台读取音频元数据得到时长（服务端不下发语音时长）
                durationSec = audioFile.durationSec.coerceAtLeast(1),
                contentType = audioFile.mimeType,
                fileExt = audioFile.ext,
                audioSource = "file",
                onError = { notificationState.show(it) }
            )
        },
        onPermissionDenied = {
            notificationState.show("无法读取本地音频文件，请检查存储权限")
        }
    )

    // 引用回复状态
    var quotingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // 长按气泡选中的锚点信息及菜单显示动画状态
    var activeBubbleAnchor by remember { mutableStateOf<BubbleAnchorInfo?>(null) }
    var isMenuShowing by remember { mutableStateOf(false) }

    fun dismissContextMenu() {
        isMenuShowing = false
    }

    LaunchedEffect(isMenuShowing) {
        if (!isMenuShowing && activeBubbleAnchor != null) {
            delay(240)
            activeBubbleAnchor = null
        }
    }

    // 壁纸状态（默认使用会话指定壁纸，或 null 纯色背景）
    // 自定义上传背景保存在 ClientManager，跨会话页面返回后仍能保持
    var currentWallpaperKey by remember(conversation.id) {
        mutableStateOf(
            ZeroTalkClientManager.conversationWallpaper(conversation.id)
                ?: conversation.wallpaperKey
        )
    }

    // 背景取值解析：本地文件绝对路径（离线可显示）优先，其次远程 URL，最后素雅纯色。
    // 管理器里的映射先写原始 URL、下载完成后再替换成本地路径，这里订阅它保持一致。
    val wallpaperMap by ZeroTalkClientManager.conversationWallpapers.collectAsState()
    val wallpaperValue = currentWallpaperKey?.trim().orEmpty()
    val localWallpaperFile = remember(wallpaperValue) { resolveLocalWallpaperFile(wallpaperValue) }
    var localWallpaperBitmap by remember(localWallpaperFile) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(localWallpaperFile) {
        val file = localWallpaperFile
        localWallpaperBitmap = if (file == null) null
        else withContext(Dispatchers.IO) { decodeLocalWallpaper(file) }
    }
    val remoteWallpaperUrl = wallpaperValue.takeIf { value ->
        !looksLikeLocalWallpaperPath(value) &&
            (value.startsWith("http://") || value.startsWith("https://"))
    }
    // 本会话背景在管理器里发生变化（如下载完成：远程 URL -> 本地路径）时同步回本地状态
    val globalWallpaper by ZeroTalkClientManager.globalWallpaper.collectAsState()
    LaunchedEffect(conversation.id, wallpaperMap, globalWallpaper) {
        // 已解析值：会话级覆盖 → 全局默认 → 会话自带壁纸
        val mapped = ZeroTalkClientManager.conversationWallpaper(conversation.id)
            ?: conversation.wallpaperKey
        if (mapped != currentWallpaperKey) currentWallpaperKey = mapped
    }

    // 弹窗与详情页状态（按会话隔离：资料页内点「私信」会切换到另一个会话，
    // 若沿用同一个 remember，新会话上会残留上一个房间的资料页 / 群面板）
    var showActionSheet by remember { mutableStateOf(false) }
    var showMusicShareSheet by remember { mutableStateOf(false) }
    var showRoomMusicSheet by remember { mutableStateOf(false) }
    var showGamePickerSheet by remember { mutableStateOf(false) }
    // 群聊语音通话成员选择器
    var showVoiceCallPicker by remember { mutableStateOf(false) }
    var showMyGameSessionsSheet by remember { mutableStateOf(false) }
    val activeGameSession by ZeroTalkClientManager.activeGameSession.collectAsState()
    var showConversationInfo by remember(conversation.id) { mutableStateOf(false) }
    // 点动态卡片评论数打开的动态（复用动态主界面的评论抽屉）
    var momentCardComment by remember(conversation.id) { mutableStateOf<MomentItem?>(null) }
    var showSecretRoomInfo by remember(conversation.id) { mutableStateOf(false) }
    var showMessageSearch by remember { mutableStateOf(false) }
    var messageSearchQuery by remember { mutableStateOf("") }
    // 群成员资料面板：群聊里点发送者、群聊面板里点成员都打开同一面板。
    // 它是资料页层级栈中的 CHAT 层：目标与可见性都由层级栈给出，退场动画期间由层级槽沿用最后一层
    val chatLayerOwner = remember(conversation.id) { Any() }
    val chatProfileSlot = rememberUserProfileLayerSlot(UserProfileLayerHost.CHAT, chatLayerOwner)
    val showMemberProfile = chatProfileSlot.visible
    val hasProfileLayers = rememberHasUserProfileLayers()
    val openMemberProfile: (UserProfileTarget) -> Unit = { target ->
        ZeroTalkClientManager.openUserProfileLayer(target, UserProfileLayerHost.CHAT, chatLayerOwner)
    }

    // 退出聊天页时收掉自己打开的群成员资料层，层级栈里不允许留下没有渲染点的孤儿层
    DisposableEffect(conversation.id) {
        onDispose {
            ZeroTalkClientManager.closeUserProfileLayers(UserProfileLayerHost.CHAT, chatLayerOwner)
        }
    }

    // 系统返回手势 / 物理返回键拦截（依次回退：底部动作菜单 -> 搜索栏 -> 举报弹窗 -> 群聊面板 -> 退出单聊）
    val hasChatSubScreen = showConversationInfo || showMemberProfile

    // 1) 动作菜单浮层拦截（被资料页层级覆盖时让出返回）
    BackHandler(enabled = !hasProfileLayers && showActionSheet) {
        showActionSheet = false
    }

    // 1.1) 音乐与游戏面板浮层拦截
    BackHandler(enabled = !hasProfileLayers && showMusicShareSheet) {
        showMusicShareSheet = false
    }
    BackHandler(enabled = !hasProfileLayers && showRoomMusicSheet) {
        showRoomMusicSheet = false
    }
    BackHandler(enabled = !hasProfileLayers && showGamePickerSheet) {
        showGamePickerSheet = false
    }
    BackHandler(enabled = !hasProfileLayers && showMyGameSessionsSheet) {
        showMyGameSessionsSheet = false
    }
    BackHandler(enabled = !hasProfileLayers && activeGameSession != null) {
        ZeroTalkClientManager.closeGameSession()
    }

    // 2) 消息搜索浮层拦截（被资料页层级覆盖时让出返回）
    BackHandler(enabled = !hasProfileLayers && showMessageSearch) {
        showMessageSearch = false
        messageSearchQuery = ""
    }

    // 3) 举报弹窗拦截：UserReportDialog 是独立窗口、自己消费返回，这里仅作兜底，不随层级让位
    BackHandler(enabled = showPeerReport) {
        if (!reportSubmitting) showPeerReport = false
    }

    // 4) 暗号房间面板拦截（页面内浮层：被群成员资料面板或资料页层级覆盖时让出返回）
    BackHandler(enabled = !hasProfileLayers && !showMemberProfile && showSecretRoomInfo) {
        showSecretRoomInfo = false
    }

    // 5) 聊天页面自身拦截（无子页面/弹窗、且上方没有资料页层级时回退到会话列表）
    BackHandler(
        enabled = !hasChatSubScreen && !hasProfileLayers && !showSecretRoomInfo &&
            !showActionSheet && !showMessageSearch && !showPeerReport
    ) {
        onBack()
    }

    // 列表滚动状态按会话隔离：此前跨会话复用同一个 LazyListState，
    // 进入新会话时会沿用上一个会话的滚动位置，导致「不一定显示最新消息」。
    val listState = remember(conversation.id) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }

    // 被引用消息定位后的高亮闪烁目标：1.2s 后清空（对齐官方 setTimeout(...,1200) 移除 chat-msg--flash）
    var flashedMessageId by remember(conversation.id) { mutableStateOf<String?>(null) }

    // 点击引用条 -> 滚动定位到被引用的那条消息并高亮。
    // 官方行为（私聊与大厅同一实现）：目标不在当前列表时只弹提示，绝不自动补历史。
    val scrollToQuotedMessage: (Long) -> Unit = { replyToId ->
        val quotedIndex = messageList.indexOfFirst { it.serverId == replyToId }
        if (quotedIndex >= 0) {
            // 真实列表结构：index 0 = 常驻的 history_loading_indicator，
            // index 1 = 可选的 peer 资料卡（showPeerCard），其后才是消息本体。
            val targetIndex = 1 + (if (showPeerCard) 1 else 0) + quotedIndex
            val targetId = messageList[quotedIndex].id
            coroutineScope.launch {
                listState.animateScrollToItem(targetIndex)
                flashedMessageId = targetId
                delay(1200)
                if (flashedMessageId == targetId) flashedMessageId = null
            }
        } else {
            notificationState.show("原消息不在当前列表中")
        }
    }

    // 上拉到顶后继续上拉 -> 请求更早的历史消息，并在插入后补偿滚动位置避免跳动
    val currentHasMore by rememberUpdatedState(hasMoreHistory)
    val currentLoadingHistory by rememberUpdatedState(isLoadingHistory)
    val currentMessageCount by rememberUpdatedState(messageList.size)
    LaunchedEffect(conversation.id) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
            .distinctUntilChanged()
            .collect { atTop ->
                // 首页历史尚未载入时不触发分页（否则会与首次定位互相打架）
                if (atTop && currentHasMore && !currentLoadingHistory && currentMessageCount > 0) {
                    val before = currentMessageCount
                    ZeroTalkClientManager.loadOlderMessages(conversation.id)
                    val newSize = withTimeoutOrNull(6000L) {
                        snapshotFlow { messageList.size }.first { it > before }
                    }
                    if (newSize != null && newSize > before) {
                        listState.scrollToItem(newSize - before)
                    }
                }
            }
    }

    // 仅在「首次载入本会话」或「末尾新增消息且用户本就贴近底部」时定位到底部；
    // 顶部前插历史时不滚动，避免与上拉分页互相打架导致页面错乱。
    var anchoredTailId by remember(conversation.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(messageList, showPeerCard, isLoadingHistory) {
        if (messageList.isEmpty()) return@LaunchedEffect
        val tailId = messageList.last().id
        val isInitial = anchoredTailId == null
        // LazyColumn 首项 "history_loading_indicator" 是无条件声明的（非加载态只是空内容），
        // 因此头部项数恒为 1 + (peer 卡片 ? 1 : 0)；旧写法在非加载态少算 1，只是被 coerceAtLeast 掩盖。
        val headerCount = 1 + (if (showPeerCard) 1 else 0)
        val targetIndex = (headerCount + messageList.size - 1).coerceAtLeast(0)
        if (isInitial || anchoredTailId != tailId) {
            anchoredTailId = tailId
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = headerCount + messageList.size
            val nearBottom = lastVisibleIndex >= totalItems - 4
            if (isInitial || nearBottom) {
                listState.scrollToItem(targetIndex)
                if (isInitial) {
                    // 首帧布局测量（尤其是顶部首聊卡片高度）后做二次精准校准，确保稳定停留在最新消息底部
                    delay(80)
                    listState.scrollToItem(targetIndex)
                }
            }
        }
    }

    // 专属壁纸与液态玻璃折射采样 Backdrop
    val chatBackdrop = rememberLayerBackdrop()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val inputFocusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listTopPadding = statusBarTop + 127.dp

    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isImeOpen = imeBottomPadding > 0.dp

    // 记录底部输入控制栏顶部在 Root 中的物理坐标 (像素)
    var bottomBarTopYPx by remember { mutableStateOf<Float?>(null) }

    // 加号按钮 45° 旋转为关闭 × 动画
    val plusRotation by animateFloatAsState(
        targetValue = if (showActionSheet) 45f else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.72f),
        label = "PlusRotation"
    )

    // 监听键盘弹出、抽屉展开、或引用消息变化，平滑且 100% 将最后一条消息顶起
    LaunchedEffect(showActionSheet, isImeOpen, quotingMessage) {
        if ((showActionSheet || isImeOpen || quotingMessage != null) && messageList.isNotEmpty()) {
            // 同上：首项历史加载指示器恒存在，头部项数 = 1 + (peer 卡片 ? 1 : 0)
            val headerCount = 1 + (if (showPeerCard) 1 else 0)
            val targetIndex = (headerCount + messageList.size - 1).coerceAtLeast(0)
            delay(30)
            listState.animateScrollToItem(targetIndex)
            // 等待键盘/抽屉弹起动画完全就绪后，做二次精准微调校准
            delay(320)
            listState.animateScrollToItem(targetIndex)
        }
    }

    // 用户手动拖拽聊天列表时自动收起扩展抽屉面板与软键盘
    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                if (showActionSheet) showActionSheet = false
                if (isImeOpen) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            }
        }
    }

    // 新消息追加到末尾时由上面的 anchoredTailId 逻辑负责定位，这里不再无条件滚到底部

    val pageBg = if (isDark) Color(0xFF000000) else Color(0xFFFEFEFE)
    // 本地背景文件缺失时按纯色处理（浅色背景上控件用深色，保证可读）
    val isWhiteBackground = localWallpaperFile == null && remoteWallpaperUrl == null && !isDark
    val controlContentColor = if (isWhiteBackground) Color(0xFF1C1C1E) else Color.White

    // 亮暗模式自适应玻璃材质表面：
    // 亮色模式：6% 浅烟熏黑微暗遮罩；
    // 暗色模式：6% 半透明乳白微光遮罩（通透极简风格）。
    val buttonSurfaceColor = if (isDark) Color.White else Color.Black
    val buttonSurfaceAlpha = 0.06f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        val rootHeightPx = with(density) { maxHeight.toPx() }

        // 动态计算底部输入控制栏（包括键盘、抽屉、引用条）占据的物理高度
        val bottomOccupiedDp = remember(bottomBarTopYPx, rootHeightPx, showActionSheet) {
            val topY = bottomBarTopYPx
            if (topY != null && rootHeightPx > 0f) {
                val occupiedPx = (rootHeightPx - topY).coerceAtLeast(0f)
                with(density) { occupiedPx.toDp() }
            } else {
                if (showActionSheet) 400.dp else 80.dp
            }
        }

        // 始终保留 16dp 黄金呼吸间隙，且不低于默认悬浮透空内边距 96dp
        val targetBottomPadding = maxOf(96.dp, bottomOccupiedDp + 16.dp)

        val dynamicBottomPadding by animateDpAsState(
            targetValue = targetBottomPadding,
            animationSpec = spring(stiffness = 450f, dampingRatio = 0.82f),
            label = "DynamicBottomPadding"
        )

        // ---- 1. 统一内容捕获层：背景壁纸 + 消息流 LazyColumn，统一录入 chatBackdrop 供 5 个 LiquidButton 光学折射穿透 ----
        // 复用 ui/components/ChatContentCaptureLayer（与公共大厅同一份实现）。
        ChatContentCaptureLayer(
            backdrop = chatBackdrop,
            listState = listState
        ) {
            // 背景层：壁纸与沉浸滤镜
            // 本地背景文件（离线可显示）优先；文件缺失 / 未设置时回退素雅纯色
            val localWallpaper = localWallpaperBitmap
            if (localWallpaperFile != null && localWallpaper != null) {
                // 自定义背景（本地文件）：自适应铺满整个对话界面，超出部分裁掉，不留空白
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBg),
                    contentAlignment = Alignment.Center
                ) {
                    LocalWallpaperImage(
                        bitmap = localWallpaper,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (remoteWallpaperUrl != null) {
                // 自定义上传背景（远程地址）：自适应铺满整个对话界面，超出部分裁掉，不留空白
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBg),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncNetworkImage(
                        url = remoteWallpaperUrl,
                        contentDescription = "自定义对话背景",
                        shape = RectangleShape,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBg)
                )
            }

            // 淡化蒙版在昵称胶囊下方 30dp 处开始 (statusBarTop + 97dp + 30dp = statusBarTop + 127dp)
            val maskFadeEndY = with(density) { (statusBarTop + 127.dp).toPx() }

            // 消息气泡列表流（完全延伸到底部，内容穿透滑过悬浮输入栏与加号）
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Black.copy(alpha = 0.20f),
                                    0.20f to Color.Black.copy(alpha = 0.23f),
                                    0.40f to Color.Black.copy(alpha = 0.32f),
                                    0.60f to Color.Black.copy(alpha = 0.50f),
                                    0.80f to Color.Black.copy(alpha = 0.76f),
                                    0.92f to Color.Black.copy(alpha = 0.91f),
                                    1.00f to Color.Black
                                ),
                                startY = 0f,
                                endY = maskFadeEndY
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    },
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = listTopPadding, // 首条消息精准对齐在联系人胶囊下方 30dp 处
                    bottom = dynamicBottomPadding // 实时精准避让底部输入栏、抽屉、软键盘，并附带 16dp 黄金呼吸空隙
                )
            ) {
                // 顶部历史分页加载指示器
                item(key = "history_loading_indicator") {
                    if (isLoadingHistory) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = Color(0xFF8E8E93)
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicText(
                                text = "正在加载更早的消息...",
                                style = TextStyle(color = Color(0xFF8E8E93), fontSize = 12.sp)
                            )
                        }
                    }
                }

                // 首聊信息卡片（官网 messages 列表的 peer 插槽）：列表最顶部展示对方资料与清流状态
                if (showPeerCard) {
                    item(key = "peer_profile_card") {
                        val peerBootstrap = roomBootstrapMap[conversation.id]
                        // 对方清流状态按数据源可靠性依次取值：本房 bootstrap 的 peer_user（进房即下发）
                        // → 聚合资料的 user.clean_stream_mode → 房间列表缓存的 peer
                        val peerCleanKnown = peerBootstrap?.peerUser?.cleanStreamMode
                            ?: peerProfile?.cleanStreamMode
                            ?: (roomPeers[conversation.id]?.let { it.cleanStreamMode == 1 })
                        PeerProfileCard(
                            peer = peerProfile,
                            isDark = isDark,
                            cleanStatusText = when {
                                peerBootstrap?.cleanStreamBypassed == true -> "本房间类型已绕过违禁词检测"
                                peerCleanKnown == true -> "对方已开启清流模式"
                                peerCleanKnown == false -> "对方已关闭清流模式"
                                peerBootstrap?.cleanStreamForced == true -> "本房间类型已强制清流"
                                else -> "开启后，匹配与私聊房间中自己与对方的文字都会走违禁检测"
                            },
                            myCleanStreamEnabled = myCleanStreamEnabled,
                            cleanSaving = cleanStreamSaving,
                            onToggleCleanStream = { next ->
                                cleanStreamSaving = true
                                ZeroTalkClientManager.setMyCleanStreamMode(next) { success, err ->
                                    cleanStreamSaving = false
                                    notificationState.show(
                                        if (success) {
                                            if (next) "清流模式已开启" else "清流模式已关闭"
                                        } else {
                                            err ?: "清流设置保存失败"
                                        }
                                    )
                                }
                            },
                            onMoments = {
                                openMemberProfile(
                                    UserProfileTarget(
                                        userId = peerUserId,
                                        uid = peerUid,
                                        name = peerProfile?.name ?: conversation.targetName,
                                        avatarUrl = peerProfile?.avatarUrl ?: conversation.targetAvatar.orEmpty()
                                    )
                                )
                            },
                            onReport = { showPeerReport = true },
                            // 官方在私聊 / 匹配房间使用同一条提示文案
                            tip = "可删除本次会话，或拉黑对方阻止再次匹配/私聊",
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                }

                itemsIndexed(
                    items = messageList,
                    key = { _, it -> it.id },
                    contentType = { _, msg ->
                        when {
                            msg.isSystem || msg.isVoiceCall -> 0
                            msg.isPat -> 1
                            msg.isImage -> 2
                            msg.isVoice -> 3
                            msg.isDice -> 4
                            msg.isMusic -> 5
                            msg.isMusicPlaylist -> 6
                            msg.isSticker -> 7
                            msg.isGame -> 8
                            else -> 9
                        }
                    }
                ) { index, msg ->
                    val searchText = if (msg.isVoiceCall) msg.voiceCallText else msg.content
                    val searchMatch = messageSearchQuery.isNotBlank() && searchText.contains(messageSearchQuery, ignoreCase = true)
                    val bottomSpacing = MessageTimeHelper.computeMessageBottomSpacing(messageList, index)

                    // 被引用消息定位后的高亮闪烁：复刻官方 .chat-msg--flash 的 chatMsgFlash 1.1s ease 曲线，
                    // 即 0% 透明 -> 35% #3b82f61a -> 100% 透明（0 -> 385ms 淡入，385ms -> 1100ms 回落）。
                    val isFlashed = flashedMessageId == msg.id
                    val flashAlpha = remember(msg.id) { Animatable(0f) }
                    LaunchedEffect(isFlashed) {
                        if (isFlashed) {
                            flashAlpha.snapTo(0f)
                            flashAlpha.animateTo(1f, animationSpec = tween(durationMillis = 385))
                            flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 715))
                        } else {
                            flashAlpha.snapTo(0f)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isFlashed) {
                                    Modifier.background(
                                        Color(0xFF3B82F6).copy(alpha = 0.1f * flashAlpha.value),
                                        RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .then(if (searchMatch) Modifier.background(Color(0x3348A6FF), RoundedCornerShape(8.dp)) else Modifier)
                    ) {
                        // 触发时间分隔条：与上一条消息相差 ≥ 5 分钟，或为会话首条
                        if (MessageTimeHelper.shouldShowTimeSeparator(messageList, index)) {
                            TimeSeparatorHeader(
                                text = MessageTimeHelper.getTimeHeaderText(msg),
                                isWhiteBackground = isWhiteBackground
                            )
                        }

                        if (msg.isSystem) {
                            // ---- 系统消息 / 时间戳 / 更改背景提示 ----
                            SystemMessageItem(
                                content = msg.content,
                                isWallpaperNotice = msg.content.contains("背景") || msg.content.contains("撤回"),
                                isWhiteBackground = isWhiteBackground
                            )
                        } else if (msg.isVoiceCall) {
                            // ---- 语音通话记录（官网 type:"voice"）：居中弱化文案，取 content 的 text ----
                            SystemMessageItem(
                                content = msg.voiceCallText.ifBlank { "[语音通话]" },
                                isWallpaperNotice = false,
                                isWhiteBackground = isWhiteBackground
                            )
                        } else if (msg.isPat) {
                            // ---- 拍一拍：居中弱化文案；文案由管理器按「发送方 / 被拍方」归因 ----
                            SystemMessageItem(
                                content = msg.patText.ifBlank {
                                    if (msg.isMine) "你 拍了拍 对方" else "有人 拍了拍 你"
                                },
                                isWallpaperNotice = false,
                                isWhiteBackground = isWhiteBackground
                            )
                        } else {
                            // ---- 真实对话气泡 (私聊：iMessage 小尖尾；群聊：普通圆气泡无尾巴) ----
                            val isLastMineMessage = msg.isMine && !msg.isDeleted &&
                                    msg.id == messageList.filter { it.isMine && !it.isSystem && !it.isVoiceCall && !it.isDeleted }.lastOrNull()?.id

                            // 群聊（暗号房）统一使用正常的圆气泡，不携带尖尾；
                            // 私聊仍依据 iMessage 原生规则计算尾巴（连续我方或对方均仅最底部一条带尾巴）
                            val hasTail = !isGroupRoom && computeBubbleHasTail(messageList, index)

                            val isFirstInGroup = MessageTimeHelper.isFirstOfSenderGroup(messageList, index)
                            // 已撤回消息整条替换为居中占位，不再展示头像 / 昵称列
                            val isGroupPeer = !msg.isMine && isGroupRoom && !msg.isDeleted

                            BubbleMessageItem(
                                message = msg,
                                hasTail = hasTail,
                                isLastMineMessage = isLastMineMessage,
                                isDark = isDark,
                                isWhiteBackground = isWhiteBackground,
                                // 群聊（暗号房）：同一发送者连续消息仅在首条展示头像与昵称/性别，
                                // 后续连续消息隐藏头像但保留 44dp 留白占位以对齐气泡；
                                // 自己的消息不展示头像与昵称。
                                showAvatar = isGroupPeer && isFirstInGroup,
                                reserveAvatarSpace = isGroupPeer,
                                showSenderInfo = isGroupPeer && isFirstInGroup,
                                higColors = higColors,
                                // 动态分享卡片：点主体进作者资料页并定位该动态；点评论数打开评论
                                onMomentCardClick = { d ->
                                    ZeroTalkClientManager.openOtherUserProfile(
                                        userId = d.userId.toString(),
                                        uid = d.uid,
                                        name = d.username,
                                        avatarUrl = d.avatarUrl,
                                        momentId = d.momentId
                                    )
                                },
                                onMomentCardCommentClick = { d -> momentCardComment = d.toMomentItem() },
                                // 被引用消息已撤回时，引用条本地兜底显示「该消息已被撤回」
                                quotedTextOverride = messageList.resolveQuotedText(msg),
                                isLifted = isMenuShowing && activeBubbleAnchor?.message?.id == msg.id,
                                // 点群成员头像/昵称 → 打开其资料面板（带已知昵称/头像占位）
                                // 服务端事件缺 from_uid 时 senderId 会退化成 peer/me 占位，这类不可跳转
                                onSenderClick = if (isGroupPeer &&
                                    msg.senderId.isNotBlank() && msg.senderId !in REAL_USER_ID_PLACEHOLDERS
                                ) {
                                    {
                                        openMemberProfile(
                                            UserProfileTarget(
                                                userId = "",
                                                uid = msg.senderId,
                                                name = msg.senderName,
                                                avatarUrl = msg.senderAvatar
                                            )
                                        )
                                    }
                                } else null,
                                // 点击引用条 -> 跳转到被引用的消息（找不到只提示，不补历史）
                                onQuoteClick = scrollToQuotedMessage,
                                onLongPress = { bounds ->
                                    // 已撤回消息整条替换为占位，没有任何可执行操作项，不弹菜单（对齐官网）
                                    if (!msg.isDeleted) {
                                        activeBubbleAnchor = BubbleAnchorInfo(msg, bounds, hasTail, isLastMineMessage)
                                        isMenuShowing = true
                                    }
                                },
                                // 双击对方消息气泡 = 拍一拍（官网同款交互，双击阈值 380ms）；
                                // 图片气泡不接管双击，避免单击预览被双击等待拖慢
                                onDoubleTapPat = if (!msg.isMine && !msg.isSystem && !msg.isImage && !msg.isDeleted) {
                                    {
                                        notificationState.show(
                                            ZeroTalkClientManager.sendPat(conversation.id, msg.senderId)
                                                .message(msg.senderName)
                                        )
                                    }
                                } else null
                            )
                        }

                        if (bottomSpacing.value > 0f) {
                            Spacer(modifier = Modifier.height(bottomSpacing))
                        }
                    }
                }
            }
        }

        // ---- 2. 顶部 iOS 26 原生导航栏（3 个 LiquidButton，悬浮在最上层）----
        // 与公共大厅共用 ui/components/IosChatNavBar（消除重复实现）；
        // 参数与旧的 PrivateChatScreen.TopNavigationBar 一一对应，外观 / 行为保持不变。
        IosChatNavBar(
            onBack = onBack,
            title = conversation.targetName,
            isDark = isDark,
            isWhiteBackground = isWhiteBackground,
            surfaceColor = buttonSurfaceColor,
            surfaceAlpha = buttonSurfaceAlpha,
            backdrop = chatBackdrop,
            unreadCount = 520, // 完美复刻图中 '< 520' 胶囊返回
            showChevron = true,
            onTitleClick = {
                if (isGroupRoom) showSecretRoomInfo = true else showConversationInfo = true
            },
            titleAvatar = {
                // 上层：放大圆形头像 (60dp)，覆盖在胶囊顶部（真实头像优先，失败回落渐变首字母）
                UserAvatar(
                    url = conversation.targetAvatar,
                    name = conversation.targetName,
                    size = 60.dp,
                    gradient = conversation.avatarGradient,
                    fallbackTextStyle = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.border(
                        1.5.dp,
                        if (isWhiteBackground) Color(0x33000000) else Color(0x66FFFFFF),
                        CircleShape
                    )
                )
            },
            trailing = {
                // 右上角统一为搜索按键（私聊 / 群聊行为对齐）：语音通话已移入底部 [+] 扩展面板
                LiquidButton(
                    onClick = {
                        showMessageSearch = !showMessageSearch
                        if (!showMessageSearch) messageSearchQuery = ""
                    },
                    modifier = Modifier.size(41.dp),
                    backdrop = chatBackdrop,
                    isDark = isDark,
                    surfaceColor = buttonSurfaceColor,
                    surfaceAlpha = buttonSurfaceAlpha,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索聊天记录",
                        tint = controlContentColor,
                        modifier = Modifier.size(25.dp)
                    )
                }
            },
            modifier = Modifier.drawWithContent {
                listState.firstVisibleItemScrollOffset
                listState.firstVisibleItemIndex
                drawContent()
            }
        )

        // 私聊 / 群聊统一的聊天记录搜索栏
        if (showMessageSearch) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 62.dp, start = 20.dp, end = 20.dp)
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(if (isDark) Color(0xE61E222B) else Color(0xEFFFFFFF))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = controlContentColor.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = messageSearchQuery,
                    onValueChange = { messageSearchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(color = controlContentColor, fontSize = 14.sp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    decorationBox = { inner ->
                        if (messageSearchQuery.isEmpty()) BasicText("搜索当前已载入的聊天记录", style = TextStyle(color = controlContentColor.copy(alpha = 0.5f), fontSize = 14.sp))
                        inner()
                    }
                )
                BasicText("${messageList.count { it.content.contains(messageSearchQuery, ignoreCase = true) && messageSearchQuery.isNotBlank() }}", style = TextStyle(color = controlContentColor.copy(alpha = 0.65f), fontSize = 12.sp))
            }
        }

        // ---- 3. 底部输入控制栏（公共组件 ChatComposerBar：[+] + 输入框 + 引用条 + 扩展抽屉）----
        ChatComposerBar(
            drawerExpanded = showActionSheet,
            onDrawerExpandedChange = { showActionSheet = it },
            inputText = inputText,
            onInputTextChange = { inputText = it },
            onSend = {
                val contentToSend = inputText.trim()
                val quoted = quotingMessage?.quotePreviewText()
                val quotedIsMine = quotingMessage?.isMine
                val quotedSenderName = quotingMessage?.senderName
                val replyToId = quotingMessage?.serverId?.takeIf { it > 0L }
                val mentionIds = mentionTargets
                    .map { it.first }
                    .filter(String::isNotBlank)
                    .distinct()
                inputText = ""
                mentionTargets = emptyList()
                quotingMessage = null
                // 管理器统一管理乐观回显与服务端引用/@对账
                ZeroTalkClientManager.sendRoomMessage(
                    conversation.id, contentToSend,
                    quotedText = quoted,
                    quotedIsMine = quotedIsMine,
                    quotedSenderName = quotedSenderName,
                    replyToId = replyToId,
                    mentionIds = mentionIds
                )
            },
            quote = quotingMessage?.let { qm ->
                ChatComposerQuote(
                    text = qm.quotePreviewText(),
                    isMine = qm.isMine,
                    cancelContentDescription = "Cancel quote"
                )
            },
            onCancelQuote = { quotingMessage = null },
            // 抽屉扩展项：全集与顺序与重构前完全一致（组件点击时会先收起抽屉，与旧实现一致）
            actions = listOf(
                ChatComposerAction(
                    id = "photo",
                    label = "照片",
                    icon = Icons.Default.Image,
                    color = Color(0xFF34C759)
                ) {
                    // 实测链路：presign(upload_source=chat_image, room_id) → PUT → bind → WS type=image
                    chatPhotoPicker.launch()
                },
                ChatComposerAction(
                    id = "voice_file",
                    label = "语音文件",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    color = Color(0xFF5856D6)
                ) {
                    // 实测链路：presign(upload_source=chat_audio, audio_source=file) → PUT → bind → WS type=audio
                    chatAudioFilePicker.launch()
                },
                ChatComposerAction(
                    id = "music_share",
                    label = "分享歌曲",
                    icon = Icons.Default.MusicNote,
                    color = Color(0xFF007AFF)
                ) {
                    showMusicShareSheet = true
                },
                ChatComposerAction(
                    id = "room_music",
                    label = "房间音乐",
                    icon = Icons.Default.GraphicEq,
                    color = Color(0xFFFF2D55)
                ) {
                    showRoomMusicSheet = true
                },
                ChatComposerAction(
                    id = "dice",
                    label = "摇骰子",
                    icon = Icons.Default.Casino,
                    color = Color(0xFFFF9500)
                ) {
                    ZeroTalkClientManager.sendRoomDice(conversation.id)
                },
                ChatComposerAction(
                    id = "game",
                    label = "游戏",
                    icon = Icons.Default.SportsEsports,
                    color = Color(0xFFAF52DE)
                ) {
                    showGamePickerSheet = true
                },
                ChatComposerAction(
                    id = "voice_call",
                    label = "语音通话",
                    icon = Icons.Default.Call,
                    color = Color(0xFF34C759)
                ) {
                    // 对齐官网 useVoiceCall 的判定：
                    // ① 群聊先选成员（voice-picker-panel「请选择一位成员发起语音通话」）
                    // ② 私聊需双方互发文字后才能发起
                    // 注：to_user_id 可空，服务端会按房间推导对端（官网同样只在已知时携带）
                    val toUserId = conversation.targetUserId.toLongOrNull()?.takeIf { it > 0 }
                    val hasBothSidesText = liveMessages.any { it.isMine } &&
                        liveMessages.any { !it.isMine }
                    when {
                        isGroupRoom -> showVoiceCallPicker = true
                        !hasBothSidesText ->
                            notificationState.show("私聊需双方互发文字后才能发起语音通话")

                        else -> ZeroTalkClientManager.voiceCall.startCall(
                            roomId = conversation.id,
                            toUserId = toUserId,
                            toUid = conversation.targetUid.takeIf { it.isNotBlank() },
                            peerLabel = conversation.targetName,
                            peerAvatarUrl = conversation.targetAvatar
                        )
                    }
                }
            ),
            isDark = isDark,
            backdrop = chatBackdrop,
            controlContentColor = controlContentColor,
            surfaceColor = buttonSurfaceColor,
            surfaceAlpha = buttonSurfaceAlpha,
            isVoiceMode = isVoiceMode,
            onToggleVoiceMode = { isVoiceMode = !isVoiceMode },
            inputFocusRequester = inputFocusRequester,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = if (showActionSheet) 4.dp else 12.dp
            ),
            onBottomBarTopChanged = { bottomBarTopYPx = it },
            // 列表滚动时持续重绘外壳，保证 Backdrop 每帧重新录制
            tailModifier = Modifier.drawWithContent {
                listState.firstVisibleItemScrollOffset
                listState.firstVisibleItemIndex
                drawContent()
            },
            // 语音录制状态机保留在页面内，只把「按住说话」输入区交给公共外壳
            voiceContent = {
                val recordingNow = chatAudioRecorder.isRecording.value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pointerInput(conversation.id) {
                            val cancelThresholdPx = 56.dp.toPx()
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                isVoiceCancelZone = false
                                chatAudioRecorder.start()
                                if (!chatAudioRecorder.isRecording.value) {
                                    // 首次使用需先授予麦克风权限（或录音启动失败），本次不发送
                                    notificationState.show("需要麦克风权限，请授权后重新按住说话")
                                    return@awaitEachGesture
                                }
                                var canceled = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    // 手指上滑超过阈值进入取消区，回落到阈值内可恢复发送
                                    canceled = (down.position.y - change.position.y) > cancelThresholdPx
                                    isVoiceCancelZone = canceled
                                    change.consume()
                                }
                                isVoiceCancelZone = false
                                val bytes = chatAudioRecorder.stop()
                                val durationSec = chatAudioRecorder.recordedSeconds.toInt()
                                when {
                                    canceled -> notificationState.show("已取消发送")
                                    bytes == null || bytes.isEmpty() ->
                                        notificationState.show("录音时间太短")
                                    else -> {
                                        val format = chatAudioRecorder.actualFormat
                                        ZeroTalkClientManager.sendRoomAudio(
                                            roomId = conversation.id,
                                            audioBytes = bytes,
                                            durationSec = durationSec.coerceAtLeast(1),
                                            contentType = format.contentType,
                                            fileExt = format.fileExt,
                                            audioSource = "record",
                                            onError = { notificationState.show(it) }
                                        )
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = when {
                            isVoiceCancelZone -> "松开取消"
                            recordingNow -> "松开发送"
                            else -> "按住说话"
                        },
                        style = TextStyle(
                            color = when {
                                isVoiceCancelZone -> controlContentColor.copy(alpha = 0.45f)
                                recordingNow -> Color(0xFFFF453A)
                                else -> controlContentColor.copy(alpha = 0.45f)
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        )

        // ---- 5. iMessage 原生 ContextMenu 上下文菜单与悬浮放大预览气泡 ----
        activeBubbleAnchor?.let { anchor ->
            val menuItems = remember(anchor.message.id, canModerateDeleteMessage) {
                buildContextMenuItems(
                    message = anchor.message,
                    onReply = {
                        quotingMessage = anchor.message
                        dismissContextMenu()
                    },
                    onRecall = {
                        val msg = anchor.message
                        // 必须发 {event:"recall_message"}：此前只改本地列表，服务端与对端都不知道，
                        // 重进页面消息还会回来（假撤回）
                        ZeroTalkClientManager.recallMessage(conversation.id, msg.serverId) { ok, err ->
                            notificationState.show(if (ok) "已撤回消息" else (err ?: "撤回失败"))
                        }
                        dismissContextMenu()
                    },
                    onRecallAndEdit = {
                        val msg = anchor.message
                        ZeroTalkClientManager.recallMessage(conversation.id, msg.serverId) { ok, err ->
                            if (!ok) notificationState.show(err ?: "撤回失败")
                        }
                        isVoiceMode = false
                        inputText = msg.content
                        notificationState.show("已撤回，可重新编辑")
                        dismissContextMenu()
                    },
                    onEdit = {
                        isVoiceMode = false
                        inputText = anchor.message.content
                        notificationState.show("正在编辑消息")
                        dismissContextMenu()
                    },
                    onCopy = {
                        notificationState.show(if (anchor.message.isImage) "已拷贝图片" else "已拷贝文本")
                        dismissContextMenu()
                    },
                    includeGroupActions = isGroupRoom,
                    onPat = {
                        val target = anchor.message
                        notificationState.show(
                            ZeroTalkClientManager.sendPat(conversation.id, target.senderId)
                                .message(target.senderName)
                        )
                        dismissContextMenu()
                    },
                    onMention = {
                        val target = anchor.message
                        val name = target.senderName.ifBlank { target.senderId }
                        val separator = if (inputText.isNotBlank() && !inputText.last().isWhitespace()) " " else ""
                        inputText = "$inputText$separator@$name "
                        if (target.senderId.isNotBlank() && target.senderId != "peer" && target.senderId != "me") {
                            mentionTargets = (mentionTargets + (target.senderId to name)).distinctBy { it.first }
                        }
                        isVoiceMode = false
                        dismissContextMenu()
                        coroutineScope.launch {
                            delay(100)
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    canModerateDelete = canModerateDeleteMessage,
                    onModerateDelete = {
                        val target = anchor.message
                        ZeroTalkClientManager.moderateDeleteMessage(conversation.id, target.serverId) { success, message ->
                            notificationState.show(if (success) "已删除该消息" else (message ?: "删除消息失败"))
                        }
                        dismissContextMenu()
                    }
                )
            }

            BubbleContextMenuOverlay(
                anchor = anchor,
                isShowing = isMenuShowing,
                backdrop = chatBackdrop,
                isDark = isDark,
                higColors = higColors,
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                onDismiss = { dismissContextMenu() },
                menuItems = menuItems
            )
        }


        AnimatedVisibility(
            visible = showSecretRoomInfo,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            SecretRoomInfoScreen(
                conversation = conversation,
                isDark = isDark,
                backdrop = chatBackdrop,
                onBack = { showSecretRoomInfo = false },
                onMuteChanged = { },
                onOpenUserProfile = { target -> openMemberProfile(target) },
                currentWallpaperKey = currentWallpaperKey,
                onWallpaperApplied = { newKey -> currentWallpaperKey = newKey }
            )
        }

        // ---- 6.1 群成员「资料面板」（层级栈中的 CHAT 层）：群聊点发送者、群聊面板点成员统一跳这里 ----
        AnimatedVisibility(
            visible = chatProfileSlot.visible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            chatProfileSlot.layer?.let { layer ->
                OtherUserProfileScreen(
                    target = layer.target,
                    isDark = isDark,
                    backdrop = chatBackdrop,
                    // 只有本层正好是层级栈栈顶时才允许它拦截返回
                    isTopLayer = chatProfileSlot.isTop,
                    onBack = { ZeroTalkClientManager.popUserProfileLayer() },
                    // 私信落地到私聊房间：先收起资料面板与群面板，再交给外层切换会话
                    onOpenConversation = { target ->
                        ZeroTalkClientManager.closeUserProfileLayers(UserProfileLayerHost.CHAT, chatLayerOwner)
                        showSecretRoomInfo = false
                        onOpenConversation?.invoke(target)
                    }
                )
            }
        }

        // ---- 6.2 举报用户（首聊卡片）：POST /report/submit
        if (showPeerReport) {
            UserReportDialog(
                userName = peerProfile?.name ?: conversation.targetName,
                isDark = isDark,
                submitting = reportSubmitting,
                onDismiss = { if (!reportSubmitting) showPeerReport = false },
                onSubmit = { reason, description ->
                    reportSubmitting = true
                    ZeroTalkClientManager.reportUser(
                        // 官方按 uid 优先、数字 id 兜底解析被举报人
                        reportedId = peerUid.ifBlank { peerUserId },
                        roomId = conversation.id,
                        reason = reason.value,
                        description = description
                    ) { success, roomDeleted, err ->
                        reportSubmitting = false
                        if (success) {
                            showPeerReport = false
                            notificationState.show(
                                if (roomDeleted) "举报提交成功，已拉黑对方" else "举报提交成功，我们会尽快处理"
                            )
                        } else {
                            notificationState.show(err ?: "举报提交失败")
                        }
                    }
                }
            )
        }

        // ---- 6.9 动态评论抽屉（点聊天里动态卡片的评论数打开）----
        MomentSheetsHost(
            commentTarget = momentCardComment,
            shareTarget = null,
            isDark = isDark,
            backdrop = chatBackdrop,
            onDismissComment = { momentCardComment = null },
            onDismissShare = {}
        )

        // ---- 7. 顶部联系人「对话资料详情页」仅用于私聊 ----
        AnimatedVisibility(
            visible = showConversationInfo && !isGroupRoom,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            ConversationInfoScreen(
                conversation = conversation,
                onBack = { showConversationInfo = false },
                backdrop = chatBackdrop,
                isDark = isDark,
                currentWallpaperKey = currentWallpaperKey,
                onOpenConversation = onOpenConversation,
                // 上方还有资料页层级时（页面内下钻），返回事件属于那一层
                backEnabled = !hasProfileLayers,
                onWallpaperApplied = { newKey ->
                    val changed = currentWallpaperKey != newKey
                    currentWallpaperKey = newKey
                    if (changed) {
                        val nowMs = System.currentTimeMillis()
                        messageList = messageList + ChatMessage(
                            id = "sys_bg_$nowMs",
                            senderId = "system",
                            content = "你更改了背景",
                            timestamp = MessageTimeHelper.formatTimeShort(nowMs),
                            isMine = false,
                            isSystem = true,
                            timestampMs = nowMs
                        )
                    }
                }
            )
        }

        // ---- 8. 分享歌曲面板（网易云解析 / 搜索 / 歌单） ----
        if (showMusicShareSheet) {
            AppleModalBottomSheet(
                onDismissRequest = { showMusicShareSheet = false },
                backdrop = chatBackdrop,
                title = "分享网易云音乐",
                leadingAction = SheetAction.Close { showMusicShareSheet = false },
                isDark = isDark
            ) {
                MomentNeteasePickerSheet(
                    isDark = isDark,
                    onPick = { music ->
                        showMusicShareSheet = false
                        ZeroTalkClientManager.sendRoomMusic(conversation.id, music)
                    },
                    onDismiss = { showMusicShareSheet = false }
                )
            }
        }

        // ---- 9. 房间共享音乐面板（上限 500 首，多端实时同步） ----
        if (showRoomMusicSheet) {
            AppleModalBottomSheet(
                onDismissRequest = { showRoomMusicSheet = false },
                backdrop = chatBackdrop,
                title = null,
                leadingAction = SheetAction.None,
                isDark = isDark
            ) {
                RoomMusicSheet(
                    roomId = conversation.id,
                    isDark = isDark,
                    onSendToChat = { music ->
                        ZeroTalkClientManager.sendRoomMusic(conversation.id, music)
                        notificationState.show("已发送到聊天")
                    },
                    onDismiss = { showRoomMusicSheet = false }
                )
            }
        }

        // ---- 10. 经典对战棋盘游戏面板（五子棋、围棋、象棋、国际象棋、谁是卧底） ----
        if (showGamePickerSheet) {
            AppleModalBottomSheet(
                onDismissRequest = { showGamePickerSheet = false },
                backdrop = chatBackdrop,
                title = null,
                leadingAction = SheetAction.None,
                isDark = isDark
            ) {
                GamePickerSheet(
                    isDark = isDark,
                    onLaunch = { gameType, gameName ->
                        showGamePickerSheet = false
                        ZeroTalkClientManager.sendRoomGameInvite(conversation.id, gameType, gameName)
                        notificationState.show("已发起「$gameName」对战")
                    },
                    onOpenMySessions = {
                        showGamePickerSheet = false
                        showMyGameSessionsSheet = true
                    },
                    onDismiss = { showGamePickerSheet = false }
                )
            }
        }

        // ---- 10.5 群聊语音通话 · 成员选择器（对齐官网 voice-picker-panel） ----
        if (showVoiceCallPicker) {
            AppleModalBottomSheet(
                onDismissRequest = { showVoiceCallPicker = false },
                backdrop = chatBackdrop,
                title = null,
                leadingAction = SheetAction.None,
                isDark = isDark
            ) {
                VoiceCallMemberPickerSheet(
                    roomId = conversation.id,
                    isDark = isDark,
                    onPick = { member ->
                        showVoiceCallPicker = false
                        ZeroTalkClientManager.voiceCall.startCall(
                            roomId = conversation.id,
                            toUserId = member.userId.takeIf { it > 0L },
                            toUid = member.uid.takeIf { it.isNotBlank() },
                            peerLabel = member.username,
                            peerAvatarUrl = member.avatarUrl
                        )
                    },
                    onDismiss = { showVoiceCallPicker = false }
                )
            }
        }

        // ---- 11. 我的游戏对局列表抽屉 ----
        if (showMyGameSessionsSheet) {
            AppleModalBottomSheet(
                onDismissRequest = { showMyGameSessionsSheet = false },
                backdrop = chatBackdrop,
                title = null,
                leadingAction = SheetAction.None,
                isDark = isDark
            ) {
                MyGameSessionsSheet(
                    clientManager = ZeroTalkClientManager,
                    isDark = isDark,
                    onSelectSession = { gameType, gameId ->
                        ZeroTalkClientManager.openGameSession(gameType, gameId)
                    },
                    onDismiss = { showMyGameSessionsSheet = false }
                )
            }
        }

        // ---- 12. 沉浸式独立游戏对局页 ----
        if (activeGameSession != null) {
            GameSessionScreen(
                clientManager = ZeroTalkClientManager,
                onBack = {
                    ZeroTalkClientManager.closeGameSession()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 依据 iMessage 原生规则计算当前消息气泡是否携带尾巴（仅私聊使用，群聊统一为无尾巴圆气泡）：
 * 不管连续我方多条消息还是连续对方多条消息，都仅最底部一条带尾巴；
 * 跨自然日或时间间隔 > 60 分钟触发时间分隔条时，分隔条前后消息均按规范携带尾巴。
 */
private fun computeBubbleHasTail(messages: List<ChatMessage>, currentIndex: Int): Boolean {
    return MessageTimeHelper.computeBubbleHasTail(messages, currentIndex)
}

/**
 * 经典 iMessage 带尾巴消息气泡项
 */
@Composable
private fun BubbleMessageItem(
    message: ChatMessage,
    hasTail: Boolean,
    isLastMineMessage: Boolean,
    isDark: Boolean,
    isWhiteBackground: Boolean = false,
    higColors: AppleHigColorTokens,
    isLifted: Boolean = false,
    showSenderInfo: Boolean = false,
    showAvatar: Boolean = false,
    reserveAvatarSpace: Boolean = false,
    onSenderClick: (() -> Unit)? = null,
    /** 点击引用条：回传被引用消息的 serverId，由调用方在 messageList 中定位并滚动高亮 */
    onQuoteClick: ((Long) -> Unit)? = null,
    /** 引用条预览文案覆盖（被引用消息已撤回时显示「该消息已被撤回」） */
    quotedTextOverride: String? = null,
    onLongPress: (Rect) -> Unit,
    /** 双击气泡触发的动作（拍一拍）；为 null 时不接管双击，单击保持即时响应 */
    onDoubleTapPat: (() -> Unit)? = null,
    /** 点动态分享卡片主体 → 作者资料页并定位到该动态 */
    onMomentCardClick: ((MomentShareCardData) -> Unit)? = null,
    /** 点动态分享卡片的评论数 → 打开评论 */
    onMomentCardCommentClick: ((MomentShareCardData) -> Unit)? = null
) {
    val isMine = message.isMine
    // 本地 uid -> 备注 映射（collectAsState 订阅：备注保存 / 清空后气泡上的发言人名字立即刷新）
    val uidRemarks by ZeroTalkClientManager.uidRemarks.collectAsState()
    var bubbleBounds by remember { mutableStateOf(Rect.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isLifted) 0f else 1f
            }
    ) {
        // 群聊对方消息左侧预留头像列（头像 36dp + 间距 8dp = 44dp）
        val avatarColumnWidth = if (reserveAvatarSpace) 44.dp else 0.dp
        val maxBubbleWidth = (maxWidth - avatarColumnWidth) * 0.8f

        // QQ 群聊式布局：对方消息「头像独占最左一列 + 右侧竖排（昵称在上、气泡在下）」；
        // 连续消息折叠头像时保留 44dp 留白占位，确保同一成员所有气泡左边缘对齐整洁；
        // 自己的消息不展示头像与昵称，气泡靠右。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (reserveAvatarSpace) {
                if (showAvatar) {
                    UserAvatar(
                        url = message.senderAvatar,
                        name = message.senderName,
                        size = 36.dp,
                        fallbackIconSize = 16.dp,
                        modifier = Modifier.then(
                            if (onSenderClick != null) {
                                Modifier
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onSenderClick
                                    )
                            } else {
                                Modifier
                            }
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
            ) {
                // 暗号房 / 群聊：同一发送者连续消息的首条，在气泡右上方展示 昵称 + 性别
                if (showSenderInfo && !isMine) {
                    Row(
                        modifier = Modifier
                            .padding(start = 2.dp, bottom = 4.dp)
                            .then(
                                if (onSenderClick != null) {
                                    Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = onSenderClick
                                        )
                                } else {
                                    Modifier
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BasicText(
                            // 群聊 / 暗号房：先查本地 uid -> 备注 映射（senderId 即 uid），
                            // 命中则显示备注，否则保持原逻辑（昵称，空则「零友」）
                            text = uidRemarks[message.senderId]?.ifBlank { null }
                                ?: message.senderName.ifBlank { "零友" },
                            style = TextStyle(
                                color = if (isWhiteBackground) Color(0xFF64748B) else Color(0xB3FFFFFF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            // 昵称过长时省略，把空间让给性别 / 称号徽章（徽章 flex-shrink:0）
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        GenderBadge(
                            gender = message.senderGender,
                            isDark = isDark,
                            fontSize = 10.sp,
                            horizontalPadding = 5.dp,
                            verticalPadding = 1.dp
                        )
                        // 发送者称号（官方消息行顺序：username → gender → UserTitleBadge）
                        UserTitleBadge(
                            title = message.authorTitle,
                            color = message.authorTitleColor,
                            isDark = isDark
                        )
                    }
                }

                val imageViewer = LocalImageViewer.current

                // pointerInput 的 block 只在 key 变化时重启，回调与 message 必须经 rememberUpdatedState
                // 取最新值：同一条消息被原地更新（引用 / 尾巴 / 是否末条变化）时 item.id 不变，
                // 否则长按菜单会拿到旧快照
                val currentMessage by rememberUpdatedState(message)
                val currentOnLongPress by rememberUpdatedState(onLongPress)
                val currentOnDoubleTapPat by rememberUpdatedState(onDoubleTapPat)
                val hasDoubleTap = onDoubleTapPat != null

                BubbleContentBox(
                    message = message,
                    hasTail = hasTail,
                    isDark = isDark,
                    isWhiteBackground = isWhiteBackground,
                    higColors = higColors,
                    onQuoteClick = onQuoteClick,
                    quotedTextOverride = quotedTextOverride,
                    onMomentCardClick = onMomentCardClick,
                    onMomentCardCommentClick = onMomentCardCommentClick,
                    onImageClick = { url ->
                        imageViewer.open(url)
                    },
                    onEnterGame = { gameType, gameId ->
                        ZeroTalkClientManager.openGameSession(gameType, gameId)
                    },
                    modifier = Modifier
                        .then(
                            // 图片 / 游戏卡片 / 骰子 / 歌单 / 表情包为「自撑尺寸」的媒体内容，不施加气泡最小宽度；
                            // 已撤回消息是整条居中占位，同样需要占满整行
                            if (message.isDeleted || message.isImage || message.isGame || message.isDice ||
                                message.isMusicPlaylist || message.isSticker
                            ) {
                                Modifier
                            } else {
                                Modifier.widthIn(min = 52.dp, max = maxBubbleWidth)
                            }
                        )
                        .onGloballyPositioned { coords ->
                            bubbleBounds = coords.boundsInRoot()
                        }
                        // key 用「是否可双击」这个稳定布尔，而不是每次组合都新建的 lambda 实例
                        .pointerInput(message.id, hasDoubleTap) {
                            val handleTap = {
                                val current = currentMessage
                                if (current.isImage && current.imageUrl.isNotBlank()) {
                                    imageViewer.open(current.imageUrl)
                                }
                            }
                            if (hasDoubleTap) {
                                detectTapGestures(
                                    onTap = { handleTap() },
                                    onDoubleTap = { currentOnDoubleTapPat?.invoke() },
                                    onLongPress = { currentOnLongPress(bubbleBounds) }
                                )
                            } else {
                                detectTapGestures(
                                    onTap = { handleTap() },
                                    onLongPress = { currentOnLongPress(bubbleBounds) }
                                )
                            }
                        }
                )

                // 仅在末条我方发出的蓝色气泡下方展示时间戳
                // （私聊带尖尾时上移 7.5dp 贴合尖尾高度并右移避让尖尾；群聊无尖尾，常规内收 12dp）
                if (isLastMineMessage) {
                    val statusColor = if (isWhiteBackground) Color(0xFF8E8E93) else Color(0x99FFFFFF)
                    BasicText(
                        text = message.timestamp,
                        style = TextStyle(
                            color = statusColor,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .padding(end = if (hasTail) 25.dp else 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * iMessage 长按气泡全屏浮层（私聊包装层）：
 * 复用公共 [MessageContextMenuOverlay]，把私聊的 iMessage 气泡作为放大预览 slot 传入，
 * 保证私聊长按交互与抽出前完全一致。
 */
@Composable
private fun BubbleContextMenuOverlay(
    anchor: BubbleAnchorInfo,
    isShowing: Boolean,
    backdrop: Backdrop?,
    isDark: Boolean,
    higColors: AppleHigColorTokens,
    screenWidth: Dp,
    screenHeight: Dp,
    onDismiss: () -> Unit,
    menuItems: List<ContextMenuItem>
) {
    MessageContextMenuOverlay(
        bounds = anchor.bounds,
        isMine = anchor.message.isMine,
        isShowing = isShowing,
        backdrop = backdrop,
        isDark = isDark,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        onDismiss = onDismiss,
        menuItems = menuItems,
        preview = {
            BubbleContentBox(
                message = anchor.message,
                hasTail = anchor.hasTail,
                isDark = isDark,
                higColors = higColors,
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

/**
 * iMessage 原生居中时间分隔条（时间戳 Header）
 */
@Composable
private fun TimeSeparatorHeader(
    text: String,
    isWhiteBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = if (isWhiteBackground) Color(0xFF8E8E93) else Color(0xCCFFFFFF),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * 居中系统消息 / 时间戳 / 更改背景提示
 */
@Composable
private fun SystemMessageItem(
    content: String,
    isWallpaperNotice: Boolean,
    /** 页面底色是否为纯白：有壁纸或深色主题时都为 false，此时系统文案必须用浅色，
     *  否则（例如亮色主题 + 自定义背景）深色字压在图上完全看不清 */
    isWhiteBackground: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isWallpaperNotice) {
            // 你更改了背景 / 你撤回了一条消息
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x44000000))
                    .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                BasicText(
                    text = content,
                    style = TextStyle(
                        color = Color(0xDDFFFFFF),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            // 普通时间戳，如 "今天 10:24"
            BasicText(
                text = content,
                style = TextStyle(
                    color = if (isWhiteBackground) Color(0x73000000) else Color(0xAAFFFFFF),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

/**
 * 背景取值是否形如「本地文件路径」（绝对路径 / file:// 形式）
 *
 * 用于区分本地背景与远程地址：远程图片下载成功后映射里存的是本地绝对路径，
 * 而 http(s) 地址始终按远程地址处理。
 */
internal fun looksLikeLocalWallpaperPath(value: String?): Boolean {
    val raw = value?.trim().orEmpty()
    if (raw.isEmpty()) return false
    if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) return false
    val path = if (raw.startsWith("file://", ignoreCase = true)) raw.substring("file://".length) else raw
    return path.startsWith("/") || path.contains(":\\")
}

/**
 * 解析本地背景文件
 *
 * 只有确实存在的本地文件才返回；文件缺失 / 取值非法返回 null，由调用方回退素雅纯色。
 */
internal fun resolveLocalWallpaperFile(value: String?): File? {
    val raw = value?.trim().orEmpty()
    if (!looksLikeLocalWallpaperPath(raw)) return null
    val path = if (raw.startsWith("file://", ignoreCase = true)) raw.substring("file://".length) else raw
    return File(path).takeIf { it.isFile && it.length() > 0L }
}

/** 读取并解码本地背景文件（失败 / 文件缺失返回 null，由调用方回退纯色） */
internal fun decodeLocalWallpaper(file: File): ImageBitmap? = try {
    decodeByteArrayToImageBitmap(file.readBytes(), NetworkImageLoader.DIMENSION_ORIGINAL)
} catch (_: Throwable) {
    null
}

/**
 * 本地背景图组件
 *
 * 布局与 [AsyncNetworkImage] 保持一致（自适应铺满、超出部分裁掉），
 * 但直接使用本地文件解码出的位图，因此断网 / 重启后依然能显示。
 */
@Composable
internal fun LocalWallpaperImage(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "自定义对话背景",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
