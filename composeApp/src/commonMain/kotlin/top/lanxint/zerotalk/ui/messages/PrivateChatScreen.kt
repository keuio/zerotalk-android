package top.lanxint.zerotalk.ui.messages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.graphics.SolidColor
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
import top.lanxint.zerotalk.data.model.ConversationItem
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.GenderBadge
import top.lanxint.zerotalk.ui.components.IosLiquidBackButton
import top.lanxint.zerotalk.ui.components.LiquidButton
import top.lanxint.zerotalk.ui.components.LocalImageViewer
import top.lanxint.zerotalk.ui.components.LocalNotificationState
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
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.colorControls
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow
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
 *    - 消息长按交互：唤出毛玻璃悬浮菜单（复制、引用、撤回），撤回直接移除并回填输入框；
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

private enum class SFSymbolType {
    REPLY,
    UNDO_SEND,
    RECALL_AND_EDIT,
    EDIT,
    COPY,
    SPEAK,
    TRANSLATE,
    SAVE_IMAGE,
    TRANSCRIBE,
    PAT,
    MENTION,
    TRASH,
    MORE
}

private data class ContextMenuItem(
    val title: String,
    val icon: SFSymbolType,
    val onClick: () -> Unit
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
                durationSec = 0,
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
        val headerCount = (if (isLoadingHistory) 1 else 0) + (if (showPeerCard) 1 else 0)
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
            val headerCount = (if (isLoadingHistory) 1 else 0) + (if (showPeerCard) 1 else 0)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    // 实时感知 LazyListState 滚动偏移，确保滑动的每一帧都触发统一图层重绘与 Backdrop 录制
                    listState.firstVisibleItemScrollOffset
                    listState.firstVisibleItemIndex
                    drawContent()
                }
                .layerBackdrop(chatBackdrop)
        ) {
            // 背景层：壁纸与沉浸滤镜
            // 本地背景文件（离线可显示）优先；文件缺失 / 未设置时回退素雅纯色
            val localWallpaper = localWallpaperBitmap
            if (localWallpaperFile != null && localWallpaper != null) {
                // 自定义背景（本地文件）：图片对齐宽度铺满，上下如有留空露出 pageBg 背景色
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBg),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LocalWallpaperImage(
                        bitmap = localWallpaper,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (remoteWallpaperUrl != null) {
                // 自定义上传背景（远程地址）：图片对齐宽度铺满，上下如有留空露出 pageBg 背景色
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageBg),
                    contentAlignment = Alignment.TopCenter
                ) {
                    AsyncNetworkImage(
                        url = remoteWallpaperUrl,
                        contentDescription = "自定义对话背景",
                        shape = RectangleShape,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
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
                            msg.isGame -> 6
                            else -> 7
                        }
                    }
                ) { index, msg ->
                    val searchText = if (msg.isVoiceCall) msg.voiceCallText else msg.content
                    val searchMatch = messageSearchQuery.isNotBlank() && searchText.contains(messageSearchQuery, ignoreCase = true)
                    val bottomSpacing = MessageTimeHelper.computeMessageBottomSpacing(messageList, index)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                isDark = isDark
                            )
                        } else if (msg.isVoiceCall) {
                            // ---- 语音通话记录（官网 type:"voice"）：居中弱化文案，取 content 的 text ----
                            SystemMessageItem(
                                content = msg.voiceCallText.ifBlank { "[语音通话]" },
                                isWallpaperNotice = false,
                                isDark = isDark
                            )
                        } else if (msg.isPat) {
                            // ---- 拍一拍：居中弱化文案；文案由管理器按「发送方 / 被拍方」归因 ----
                            SystemMessageItem(
                                content = msg.patText.ifBlank {
                                    if (msg.isMine) "你 拍了拍 对方" else "有人 拍了拍 你"
                                },
                                isWallpaperNotice = false,
                                isDark = isDark
                            )
                        } else {
                            // ---- 真实对话气泡 (私聊：iMessage 小尖尾；群聊：普通圆气泡无尾巴) ----
                            val isLastMineMessage = msg.isMine &&
                                    msg.id == messageList.filter { it.isMine && !it.isSystem && !it.isVoiceCall }.lastOrNull()?.id

                            // 群聊（暗号房）统一使用正常的圆气泡，不携带尖尾；
                            // 私聊仍依据 iMessage 原生规则计算尾巴（连续我方或对方均仅最底部一条带尾巴）
                            val hasTail = !isGroupRoom && computeBubbleHasTail(messageList, index)

                            val isFirstInGroup = MessageTimeHelper.isFirstOfSenderGroup(messageList, index)
                            val isGroupPeer = !msg.isMine && isGroupRoom

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
                                onLongPress = { bounds ->
                                    activeBubbleAnchor = BubbleAnchorInfo(msg, bounds, hasTail, isLastMineMessage)
                                    isMenuShowing = true
                                },
                                // 双击对方消息气泡 = 拍一拍（官网同款交互，双击阈值 380ms）；
                                // 图片气泡不接管双击，避免单击预览被双击等待拖慢
                                onDoubleTapPat = if (!msg.isMine && !msg.isSystem && !msg.isImage) {
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
        TopNavigationBar(
            conversation = conversation,
            unreadCount = 520, // 完美复刻图中 '< 520' 胶囊返回
            onBack = onBack,
            onTitleClick = {
                if (isGroupRoom) showSecretRoomInfo = true else showConversationInfo = true
            },
            // 右上角统一为搜索按键（私聊 / 群聊行为对齐）：语音通话已移入底部 [+] 扩展面板
            onSearchClick = {
                showMessageSearch = !showMessageSearch
                if (!showMessageSearch) messageSearchQuery = ""
            },
            backdrop = chatBackdrop,
            isDark = isDark,
            isWhiteBackground = isWhiteBackground,
            surfaceColor = buttonSurfaceColor,
            surfaceAlpha = buttonSurfaceAlpha,
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

        // ---- 2.5 抽屉展开时的全屏透明拦截层（点击聊天背景区域平滑收起抽屉，同时不阻挡视觉）----
        if (showActionSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showActionSheet = false }
                    )
            )
        }

        // ---- 3. 底部输入控制栏（2 个 LiquidButton：[+] 与 输入框，完全悬浮透空）----
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = if (showActionSheet) 4.dp else 12.dp)
                .onGloballyPositioned { coordinates ->
                    bottomBarTopYPx = coordinates.boundsInRoot().top
                }
                .drawWithContent {
                    listState.firstVisibleItemScrollOffset
                    listState.firstVisibleItemIndex
                    drawContent()
                }
        ) {
            // 引用回复预览条 (整体高度 36dp，水平内边距 12dp，背景 secondarySystemBackground)
            AnimatedVisibility(
                visible = quotingMessage != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                quotingMessage?.let { qm ->
                    val previewBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                    val barLineColor = if (qm.isMine) Color(0xFF007AFF) else Color(0xFF8E8E93)
                    val previewText = qm.quotePreviewText()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewBg)
                            .border(
                                0.5.dp,
                                if (isDark) Color(0x2EFFFFFF) else Color(0x18000000),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(start = 12.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧竖线指示条 (宽度 2dp，原消息气泡同色)
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(barLineColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // 预览文字 (15sp Subhead，单行截断)
                        BasicText(
                            text = previewText,
                            style = TextStyle(
                                color = higColors.label,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // 关闭叉号按钮：视觉 24dp，热区 44dp
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { quotingMessage = null }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x14000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel quote",
                                    modifier = Modifier.size(13.dp),
                                    tint = if (isDark) Color(0xCCFFFFFF) else Color(0x99000000)
                                )
                            }
                        }
                    }
                }
            }

            // 输入栏主体
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 左侧独立 [+] LiquidButton 按钮 (36dp，展开时顺滑旋转 45° 为 ×)
                LiquidButton(
                    onClick = {
                        if (showActionSheet) {
                            showActionSheet = false
                        } else {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            showActionSheet = true
                        }
                    },
                    modifier = Modifier.size(36.dp),
                    backdrop = chatBackdrop,
                    isDark = isDark,
                    surfaceColor = buttonSurfaceColor,
                    surfaceAlpha = buttonSurfaceAlpha,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (showActionSheet) "收起扩展" else "展开扩展",
                        tint = controlContentColor,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = plusRotation }
                    )
                }

                // 中央消息输入框 (基于 LiquidButton 架构，高度 36dp)
                LiquidButton(
                    onClick = {
                        if (showActionSheet) showActionSheet = false
                        if (!isVoiceMode) {
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    backdrop = chatBackdrop,
                    isDark = isDark,
                    isInteractive = false,
                    surfaceColor = buttonSurfaceColor,
                    surfaceAlpha = buttonSurfaceAlpha,
                    contentPadding = PaddingValues(start = 12.dp, end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isVoiceMode) {
                            // 按住说话：上滑取消，松开发送
                            // 实测链路：webm 录音 → presign(upload_source=chat_audio, audio_source=record) → PUT → bind → WS type=audio
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
                        } else {
                            // 键盘文本输入模式
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (showActionSheet) showActionSheet = false
                                        inputFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = controlContentColor,
                                        fontSize = 17.sp,
                                        lineHeight = 21.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(inputFocusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused && showActionSheet) {
                                                showActionSheet = false
                                            }
                                        },
                                    decorationBox = { innerTextField ->
                                        if (inputText.isEmpty()) {
                                            BasicText(
                                                text = "发信息",
                                                style = TextStyle(
                                                    color = controlContentColor.copy(alpha = 0.45f),
                                                    fontSize = 17.sp
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        // 右侧动态图标：麦克风图标 <-> 蓝色发送箭头（带弹性缩放与淡入淡出转换动画）
                        val hasText = inputText.trim().isNotEmpty()
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = hasText,
                                transitionSpec = {
                                    if (targetState) {
                                        // 转换为发送按钮：弹性放大淡入，麦克风缩小淡出
                                        (fadeIn(animationSpec = tween(160)) +
                                                scaleIn(initialScale = 0.35f, animationSpec = spring(dampingRatio = 0.62f, stiffness = 450f)))
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(120)) +
                                                        scaleOut(targetScale = 0.4f, animationSpec = tween(120))
                                            )
                                    } else {
                                        // 转换为语音/键盘：弹性放大淡入，发送按钮缩小淡出
                                        (fadeIn(animationSpec = tween(160)) +
                                                scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = 0.68f, stiffness = 450f)))
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(120)) +
                                                        scaleOut(targetScale = 0.35f, animationSpec = tween(120))
                                            )
                                    }
                                },
                                contentAlignment = Alignment.Center,
                                label = "SendVoiceTransition"
                            ) { targetHasText ->
                                if (targetHasText) {
                                    // 纯蓝圆形发送箭头
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF007AFF))
                                            .clickable {
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
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else {
                                    // 麦克风图标，点击切换语音录制 / 键盘
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable { isVoiceMode = !isVoiceMode },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedContent(
                                            targetState = isVoiceMode,
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(150)) +
                                                        scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 450f)))
                                                    .togetherWith(
                                                        fadeOut(animationSpec = tween(100)) +
                                                                scaleOut(targetScale = 0.6f, animationSpec = tween(100))
                                                    )
                                            },
                                            contentAlignment = Alignment.Center,
                                            label = "VoiceKeyboardTransition"
                                        ) { targetVoiceMode ->
                                            if (targetVoiceMode) {
                                                Icon(
                                                    imageVector = Icons.Default.Keyboard,
                                                    contentDescription = "Keyboard",
                                                    tint = controlContentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Microphone",
                                                    tint = controlContentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---- 4. 底部 [+] 扩展抽屉面板（展开时自然将上方输入栏平滑顶起）----
            AnimatedVisibility(
                visible = showActionSheet,
                enter = expandVertically(
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.78f),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.78f),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(120))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(10.dp)) // 输入框与扩展面板之间的 10dp 呼吸悬浮空隙

                    val drawerSurfaceShape = RoundedCornerShape(32.dp)
                    val drawerModifier = if (chatBackdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = chatBackdrop,
                            shape = { drawerSurfaceShape },
                            effects = {
                                colorControls(
                                    brightness = if (isDark) 0.06f else 0.14f,
                                    saturation = 1.45f
                                )
                                blur(if (isDark) 16.dp.toPx() else 20.dp.toPx())
                                lens(
                                    refractionHeight = 22.dp.toPx(),
                                    refractionAmount = 40.dp.toPx(),
                                    depthEffect = true
                                )
                            },
                            highlight = { Highlight.Plain },
                            shadow = { Shadow(radius = 18.dp, color = Color.Black.copy(alpha = if (isDark) 0.35f else 0.16f)) },
                            onDrawSurface = {
                                drawRect(
                                    if (isDark) Color(0xFF161820).copy(alpha = 0.76f)
                                    else Color(0xFFFFFFFF).copy(alpha = 0.75f)
                                )
                            }
                        )
                    } else {
                        Modifier
                            .shadow(
                                elevation = 18.dp,
                                shape = drawerSurfaceShape,
                                ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.16f),
                                spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.16f)
                            )
                            .background(if (isDark) Color(0xFF1C1F28) else Color(0xFFFFFFFF), drawerSurfaceShape)
                            .border(
                                width = 0.5.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                                shape = drawerSurfaceShape
                            )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .then(drawerModifier)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 20f) {
                                        showActionSheet = false
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 顶部拖拽把手指示条 (36dp x 5dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.35f)
                                        else Color.Black.copy(alpha = 0.20f)
                                    )
                            )
                        }

                        // 功能图标两排布局，每排4个等宽竖向对齐（第1排：照片、语音文件、分享歌曲、房间音乐；第2排：摇骰子、游戏、语音通话、留白占位）
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 第一排 (照片、语音文件、分享歌曲、房间音乐)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ActionSheetGridItem(
                                    icon = Icons.Default.Image,
                                    title = "照片",
                                    color = Color(0xFF34C759),
                                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    showActionSheet = false
                                    // 实测链路：presign(upload_source=chat_image, room_id) → PUT → bind → WS type=image
                                    chatPhotoPicker.launch()
                                }

                                ActionSheetGridItem(
                                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                                    title = "语音文件",
                                    color = Color(0xFF5856D6),
                                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    showActionSheet = false
                                    // 实测链路：presign(upload_source=chat_audio, audio_source=file) → PUT → bind → WS type=audio
                                    chatAudioFilePicker.launch()
                                }

                                ActionSheetGridItem(
                                    icon = Icons.Default.MusicNote,
                                    title = "分享歌曲",
                                    color = Color(0xFF007AFF),
                                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    showActionSheet = false
                                    showMusicShareSheet = true
                                }

                                ActionSheetGridItem(
                                    icon = Icons.Default.GraphicEq,
                                    title = "房间音乐",
                                    color = Color(0xFFFF2D55),
                                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    showActionSheet = false
                                    showRoomMusicSheet = true
                                }
                            }

                            // 第二排 (摇骰子、游戏、语音通话、留白占位)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ActionSheetGridItem(
                                    icon = Icons.Default.Casino,
                                    title = "摇骰子",
                                    color = Color(0xFFFF9500),
                                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    showActionSheet = false
                                    ZeroTalkClientManager.sendRoomDice(conversation.id)
                                }

                                ActionSheetGridItem(
                                    icon = Icons.Default.SportsEsports,
                                    title = "游戏",
                                    color = Color(0xFFAF52DE),
                                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    showActionSheet = false
                                    showGamePickerSheet = true
                                }

                                ActionSheetGridItem(
                                    icon = Icons.Default.Call,
                                    title = "语音通话",
                                    color = Color(0xFF34C759),
                                    textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    showActionSheet = false
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

                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

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
                        messageList = messageList.filter { it.id != msg.id }
                        val nowMs = System.currentTimeMillis()
                        messageList = messageList + ChatMessage(
                            id = "sys_recall_$nowMs",
                            senderId = "system",
                            content = "你撤回了一条消息",
                            timestamp = MessageTimeHelper.formatTimeShort(nowMs),
                            isMine = false,
                            isSystem = true,
                            timestampMs = nowMs
                        )
                        notificationState.show("已撤回消息")
                        dismissContextMenu()
                    },
                    onRecallAndEdit = {
                        val msg = anchor.message
                        messageList = messageList.filter { it.id != msg.id }
                        val nowMs = System.currentTimeMillis()
                        messageList = messageList + ChatMessage(
                            id = "sys_recall_$nowMs",
                            senderId = "system",
                            content = "你撤回了一条消息",
                            timestamp = MessageTimeHelper.formatTimeShort(nowMs),
                            isMine = false,
                            isSystem = true,
                            timestampMs = nowMs
                        )
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
 * 顶部原生 iOS 26 导航栏
 */
@Composable
private fun TopNavigationBar(
    conversation: ConversationItem,
    unreadCount: Int,
    onBack: () -> Unit,
    onTitleClick: () -> Unit,
    onSearchClick: () -> Unit,
    backdrop: Backdrop?,
    isDark: Boolean,
    isWhiteBackground: Boolean = false,
    surfaceColor: Color = if (isDark) Color.White else Color.Black,
    surfaceAlpha: Float = 0.06f,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isWhiteBackground) Color(0xFF1C1C1E) else Color.White
    val badgeBg = if (isWhiteBackground) Color(0xFF1C1C1E) else Color.White
    val badgeTextColor = if (isWhiteBackground) Color.White else Color(0xFF1C1C1E)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 21.dp, vertical = 6.dp)
            .then(modifier)
    ) {
        // 左侧通用液态毛玻璃胶囊返回键：< 520 (高度 41dp，圆润无棱角 < 图标，角标小圆胶囊 27dp 高)
        IosLiquidBackButton(
            onClick = onBack,
            unreadCount = unreadCount,
            backdrop = backdrop,
            isDark = isDark,
            isWhiteBackground = isWhiteBackground,
            surfaceColor = surfaceColor,
            surfaceAlpha = surfaceAlpha,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp)
        )

        // 居中联系人组合：头像在名字胶囊上层 (先声明胶囊昵称在下层，后声明圆形头像在上层)
        Box(
            modifier = Modifier.align(Alignment.TopCenter),
            contentAlignment = Alignment.TopCenter
        ) {
            // 下层：悬浮 LiquidButton 胶囊昵称 [陈默 ›] (高度 41dp，文字 21sp)
            LiquidButton(
                onClick = onTitleClick,
                modifier = Modifier
                    .padding(top = 50.dp)
                    .height(41.dp),
                backdrop = backdrop,
                isDark = isDark,
                surfaceColor = surfaceColor,
                surfaceAlpha = surfaceAlpha,
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                BasicText(
                    text = conversation.targetName,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    text = "›",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

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
                modifier = Modifier
                    .border(1.5.dp, if (isWhiteBackground) Color(0x33000000) else Color(0x66FFFFFF), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTitleClick
                    )
            )
        }

        // 右侧 LiquidButton 圆形搜索按键 (尺寸 41dp，图标 25dp)
        LiquidButton(
            onClick = onSearchClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
                .size(41.dp),
            backdrop = backdrop,
            isDark = isDark,
            surfaceColor = surfaceColor,
            surfaceAlpha = surfaceAlpha,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索聊天记录",
                tint = contentColor,
                modifier = Modifier.size(25.dp)
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
    onLongPress: (Rect) -> Unit,
    /** 双击气泡触发的动作（拍一拍）；为 null 时不接管双击，单击保持即时响应 */
    onDoubleTapPat: (() -> Unit)? = null
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
                            )
                        )
                        GenderBadge(
                            gender = message.senderGender,
                            isDark = isDark,
                            fontSize = 10.sp,
                            horizontalPadding = 5.dp,
                            verticalPadding = 1.dp
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
                    higColors = higColors,
                    onImageClick = { url ->
                        imageViewer.open(url)
                    },
                    onEnterGame = { gameType, gameId ->
                        ZeroTalkClientManager.openGameSession(gameType, gameId)
                    },
                    modifier = Modifier
                        .then(
                            // 图片 / 游戏卡片 / 骰子为「自撑尺寸」的媒体内容，不施加气泡最小宽度
                            if (message.isImage || message.isGame || message.isDice) Modifier
                            else Modifier.widthIn(min = 52.dp, max = maxBubbleWidth)
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
 * SF Symbol 风格原生极简矢量图标
 */
@Composable
private fun SFSymbolIcon(
    symbol: SFSymbolType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        when (symbol) {
            SFSymbolType.REPLY -> {
                // SF Symbol: arrowshape.turn.up.left
                val strokeWidth = 1.8.dp.toPx()
                val path = Path().apply {
                    moveTo(w * 0.12f, h * 0.38f)
                    lineTo(w * 0.42f, h * 0.12f)
                    moveTo(w * 0.12f, h * 0.38f)
                    lineTo(w * 0.42f, h * 0.64f)
                    moveTo(w * 0.14f, h * 0.38f)
                    cubicTo(
                        w * 0.50f, h * 0.38f,
                        w * 0.84f, h * 0.46f,
                        w * 0.84f, h * 0.86f
                    )
                }
                drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            SFSymbolType.UNDO_SEND -> {
                // SF Symbol: arrow.uturn.backward.circle
                val strokeWidth = 1.5.dp.toPx()
                drawCircle(
                    color = tint,
                    radius = w * 0.44f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = strokeWidth)
                )
                val arcRect = Rect(w * 0.27f, h * 0.27f, w * 0.73f, h * 0.73f)
                drawArc(
                    color = tint,
                    startAngle = 50f,
                    sweepAngle = 265f,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                val head = Path().apply {
                    moveTo(w * 0.20f, h * 0.46f)
                    lineTo(w * 0.34f, h * 0.30f)
                    lineTo(w * 0.34f, h * 0.58f)
                    close()
                }
                drawPath(head, color = tint, style = Fill)
            }
            SFSymbolType.RECALL_AND_EDIT -> {
                // SF Symbol: 撤回箭头 + 重新编辑铅笔组合
                val strokeWidth = 1.4.dp.toPx()
                // 1. 上半部：撤回弧线与箭头 (从右向左弯曲)
                val arcRect = Rect(w * 0.16f, h * 0.12f, w * 0.68f, h * 0.58f)
                drawArc(
                    color = tint,
                    startAngle = 10f,
                    sweepAngle = -200f,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                val arrowHead = Path().apply {
                    moveTo(w * 0.12f, h * 0.35f)
                    lineTo(w * 0.28f, h * 0.20f)
                    lineTo(w * 0.28f, h * 0.46f)
                    close()
                }
                drawPath(arrowHead, color = tint, style = Fill)

                // 2. 下半部：斜向编辑铅笔
                drawLine(
                    color = tint,
                    start = Offset(w * 0.48f, h * 0.82f),
                    end = Offset(w * 0.82f, h * 0.48f),
                    strokeWidth = strokeWidth * 1.5f,
                    cap = StrokeCap.Round
                )
                val tip = Path().apply {
                    moveTo(w * 0.36f, h * 0.88f)
                    lineTo(w * 0.42f, h * 0.74f)
                    lineTo(w * 0.52f, h * 0.84f)
                    close()
                }
                drawPath(tip, color = tint, style = Fill)
            }
            SFSymbolType.EDIT -> {
                // SF Symbol: pencil
                val strokeWidth = 1.6.dp.toPx()
                drawLine(
                    color = tint,
                    start = Offset(w * 0.32f, h * 0.68f),
                    end = Offset(w * 0.75f, h * 0.25f),
                    strokeWidth = strokeWidth * 1.5f,
                    cap = StrokeCap.Round
                )
                val tip = Path().apply {
                    moveTo(w * 0.18f, h * 0.82f)
                    lineTo(w * 0.25f, h * 0.64f)
                    lineTo(w * 0.36f, h * 0.75f)
                    close()
                }
                drawPath(tip, color = tint, style = Fill)
            }
            SFSymbolType.COPY -> {
                // SF Symbol: doc.on.doc
                val strokeWidth = 1.5.dp.toPx()
                val r = 2.dp.toPx()
                val backPath = Path().apply {
                    moveTo(w * 0.58f, h * 0.16f)
                    lineTo(w * 0.22f + r, h * 0.16f)
                    arcTo(Rect(w * 0.22f, h * 0.16f, w * 0.22f + 2 * r, h * 0.16f + 2 * r), 270f, -90f, false)
                    lineTo(w * 0.22f, h * 0.68f - r)
                    arcTo(Rect(w * 0.22f, h * 0.68f - 2 * r, w * 0.22f + 2 * r, h * 0.68f), 180f, -90f, false)
                    lineTo(w * 0.36f, h * 0.68f)
                }
                drawPath(backPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.36f, h * 0.30f),
                    size = Size(w * 0.48f, h * 0.58f),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = strokeWidth)
                )
            }
            SFSymbolType.SPEAK -> {
                // SF Symbol: speaker.wave.2
                val strokeWidth = 1.5.dp.toPx()
                val body = Path().apply {
                    moveTo(w * 0.15f, h * 0.38f)
                    lineTo(w * 0.28f, h * 0.38f)
                    lineTo(w * 0.46f, h * 0.22f)
                    lineTo(w * 0.46f, h * 0.78f)
                    lineTo(w * 0.28f, h * 0.62f)
                    lineTo(w * 0.15f, h * 0.62f)
                    close()
                }
                drawPath(body, color = tint, style = Fill)
                val arc1 = Rect(w * 0.42f, h * 0.34f, w * 0.64f, h * 0.66f)
                drawArc(color = tint, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = arc1.topLeft, size = arc1.size, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                val arc2 = Rect(w * 0.46f, h * 0.24f, w * 0.82f, h * 0.76f)
                drawArc(color = tint, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = arc2.topLeft, size = arc2.size, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            }
            SFSymbolType.TRANSLATE -> {
                // SF Symbol: character.bubble / translate
                val strokeWidth = 1.5.dp.toPx()
                val bubble = Path().apply {
                    moveTo(w * 0.16f, h * 0.22f)
                    lineTo(w * 0.84f, h * 0.22f)
                    lineTo(w * 0.84f, h * 0.64f)
                    lineTo(w * 0.52f, h * 0.64f)
                    lineTo(w * 0.36f, h * 0.80f)
                    lineTo(w * 0.36f, h * 0.64f)
                    lineTo(w * 0.16f, h * 0.64f)
                    close()
                }
                drawPath(bubble, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.38f), end = Offset(w * 0.70f, h * 0.38f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.50f), end = Offset(w * 0.56f, h * 0.50f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            SFSymbolType.SAVE_IMAGE -> {
                // SF Symbol: square.and.arrow.down
                val strokeWidth = 1.6.dp.toPx()
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.5f, h * 0.60f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                val head = Path().apply {
                    moveTo(w * 0.34f, h * 0.45f)
                    lineTo(w * 0.50f, h * 0.60f)
                    lineTo(w * 0.66f, h * 0.45f)
                }
                drawPath(head, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val tray = Path().apply {
                    moveTo(w * 0.20f, h * 0.50f)
                    lineTo(w * 0.20f, h * 0.82f)
                    lineTo(w * 0.80f, h * 0.82f)
                    lineTo(w * 0.80f, h * 0.50f)
                }
                drawPath(tray, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            SFSymbolType.TRANSCRIBE -> {
                // SF Symbol: waveform
                val strokeWidth = 1.6.dp.toPx()
                val heights = listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.8f, 0.4f)
                heights.forEachIndexed { i, factor ->
                    val x = w * (0.20f + i * 0.12f)
                    val halfH = (h * 0.7f * factor) / 2
                    drawLine(
                        color = tint,
                        start = Offset(x, h * 0.5f - halfH),
                        end = Offset(x, h * 0.5f + halfH),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            SFSymbolType.TRASH -> {
                // SF Symbol: trash
                val strokeWidth = 1.5.dp.toPx()
                val body = Path().apply {
                    moveTo(w * 0.26f, h * 0.32f)
                    lineTo(w * 0.30f, h * 0.86f)
                    lineTo(w * 0.70f, h * 0.86f)
                    lineTo(w * 0.74f, h * 0.32f)
                }
                drawPath(body, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(
                    color = tint,
                    start = Offset(w * 0.18f, h * 0.28f),
                    end = Offset(w * 0.82f, h * 0.28f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                val handle = Path().apply {
                    moveTo(w * 0.40f, h * 0.28f)
                    lineTo(w * 0.40f, h * 0.16f)
                    lineTo(w * 0.60f, h * 0.16f)
                    lineTo(w * 0.60f, h * 0.28f)
                }
                drawPath(handle, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            SFSymbolType.PAT, SFSymbolType.MENTION, SFSymbolType.MORE -> {
                // SF Symbol: ellipsis.circle
                val strokeWidth = 1.5.dp.toPx()
                drawCircle(
                    color = tint,
                    radius = w * 0.44f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = strokeWidth)
                )
                val dotR = 1.25.dp.toPx()
                val cy = h * 0.5f
                drawCircle(color = tint, radius = dotR, center = Offset(w * 0.32f, cy))
                drawCircle(color = tint, radius = dotR, center = Offset(w * 0.50f, cy))
                drawCircle(color = tint, radius = dotR, center = Offset(w * 0.68f, cy))
            }
        }
    }
}

/**
 * iOS 26 原生 Liquid Glass 垂直上下文菜单列表
 */
@Composable
private fun AppleContextMenu(
    items: List<ContextMenuItem>,
    backdrop: Backdrop?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isDark) Color(0xFF1B1E26) else Color(0xFFF2F4F8))
    }
    val actualBackdrop = backdrop ?: fallbackBackdrop

    val surfaceColor = if (isDark) Color(0xFF1E1E22).copy(alpha = 0.68f) else Color(0xFFF9F9FB).copy(alpha = 0.68f)
    val labelColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val secondaryLabelColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF8E8E93)
    val dividerColor = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000)

    Column(
        modifier = modifier
            .width(250.dp)
            .drawBackdrop(
                backdrop = actualBackdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    colorControls(
                        brightness = if (isDark) 0.05f else 0.15f,
                        saturation = 1.4f
                    )
                    blur(if (isDark) 16.dp.toPx() else 20.dp.toPx())
                    lens(16.dp.toPx(), 32.dp.toPx(), depthEffect = true)
                },
                highlight = { Highlight.Plain },
                shadow = { Shadow(radius = 20.dp, color = Color.Black.copy(if (isDark) 0.45f else 0.18f)) },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .clip(RoundedCornerShape(16.dp))
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = item.onClick
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = item.title,
                    style = TextStyle(
                        color = labelColor,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                SFSymbolIcon(
                    symbol = item.icon,
                    tint = secondaryLabelColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (index < items.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .padding(start = 16.dp)
                        .background(dividerColor)
                )
            }
        }
    }
}

/**
 * iMessage 长按气泡全屏浮层：
 * 1. 背景遮罩变暗；
 * 2. 悬浮放大预览气泡（原地 1.1 倍放大 + 柔和外阴影 + 保持尾巴与样式）；
 * 3. 独立 ContextMenu Popover（智能避让屏幕边缘，自适应位于气泡下方或上方）。
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
    val overlayAlpha = remember { Animatable(0f) }
    val bubbleScale = remember { Animatable(1.0f) }
    val bubbleOffsetY = remember { Animatable(0f) }
    val menuScale = remember { Animatable(0.75f) }
    val menuAlpha = remember { Animatable(0f) }

    LaunchedEffect(isShowing) {
        if (isShowing) {
            launch {
                overlayAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200)
                )
            }
            launch {
                bubbleScale.animateTo(
                    targetValue = 1.1f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                )
            }
            launch {
                bubbleOffsetY.animateTo(
                    targetValue = -4f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                )
            }
            launch {
                menuScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f)
                )
            }
            launch {
                menuAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 180)
                )
            }
        } else {
            launch {
                overlayAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180)
                )
            }
            launch {
                bubbleScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                )
            }
            launch {
                bubbleOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                )
            }
            launch {
                menuScale.animateTo(
                    targetValue = 0.75f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f)
                )
            }
            launch {
                menuAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 150)
                )
            }
        }
    }

    val density = LocalDensity.current
    val bubbleLeft = with(density) { anchor.bounds.left.toDp() }
    val bubbleTop = with(density) { anchor.bounds.top.toDp() }
    val bubbleRight = with(density) { anchor.bounds.right.toDp() }
    val bubbleBottom = with(density) { anchor.bounds.bottom.toDp() }
    val bubbleWidth = with(density) { anchor.bounds.width.toDp() }
    val bubbleHeight = with(density) { anchor.bounds.height.toDp() }

    val menuWidth = 250.dp
    val menuHeight = 44.dp * menuItems.size

    // 气泡 1.1 倍放大并上浮 4dp 后的实际视觉边界：
    val scaledExtraHeight = bubbleHeight * 0.1f
    val effectiveBubbleBottom = bubbleBottom - 4.dp + scaledExtraHeight
    val effectiveBubbleTop = bubbleTop - 4.dp - scaledExtraHeight

    val spaceBelow = screenHeight - effectiveBubbleBottom - 88.dp
    val isMenuBelow = spaceBelow >= (menuHeight + 16.dp)

    val menuTop = if (isMenuBelow) {
        (effectiveBubbleBottom + 10.dp).coerceIn(80.dp, screenHeight - menuHeight - 16.dp)
    } else {
        (effectiveBubbleTop - menuHeight - 10.dp).coerceIn(80.dp, screenHeight - menuHeight - 16.dp)
    }

    val menuLeft = if (anchor.message.isMine) {
        (bubbleRight - menuWidth).coerceIn(16.dp, screenWidth - menuWidth - 14.dp)
    } else {
        bubbleLeft.coerceIn(14.dp, screenWidth - menuWidth - 16.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * overlayAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // 1. 悬浮放大预览气泡 (原地 1.1 倍放大 + 轻微上浮 -4dp + 柔和外阴影)
        Box(
            modifier = Modifier
                .offset(x = bubbleLeft, y = bubbleTop + bubbleOffsetY.value.dp)
                .size(width = bubbleWidth, height = bubbleHeight)
                .graphicsLayer {
                    scaleX = bubbleScale.value
                    scaleY = bubbleScale.value
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (anchor.message.isMine) 0.85f else 0.15f,
                        pivotFractionY = if (isMenuBelow) 0f else 1f
                    )
                    shadowElevation = (18f * overlayAlpha.value).dp.toPx()
                }
        ) {
            BubbleContentBox(
                message = anchor.message,
                hasTail = anchor.hasTail,
                isDark = isDark,
                higColors = higColors,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. 下方/上方独立 ContextMenu Popover (避让屏幕边缘)
        Box(
            modifier = Modifier
                .offset(x = menuLeft, y = menuTop)
                .graphicsLayer {
                    scaleX = menuScale.value
                    scaleY = menuScale.value
                    alpha = menuAlpha.value
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (anchor.message.isMine) 0.9f else 0.1f,
                        pivotFractionY = if (isMenuBelow) 0.0f else 1.0f
                    )
                }
        ) {
            AppleContextMenu(
                items = menuItems,
                backdrop = backdrop,
                isDark = isDark
            )
        }
    }
}

/**
 * 依据消息归属动态构建 ContextMenu 选项：
 * - 我方：回复、撤回、[撤回并编辑]、[编辑]、拷贝
 * - 对方：回复、拷贝
 * - 房管（viewer_can_delete_message）：额外可删除他人消息
 */
private fun buildContextMenuItems(
    message: ChatMessage,
    onReply: () -> Unit,
    onRecall: () -> Unit,
    onRecallAndEdit: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    includeGroupActions: Boolean,
    onPat: () -> Unit,
    onMention: () -> Unit,
    canModerateDelete: Boolean,
    onModerateDelete: () -> Unit
): List<ContextMenuItem> {
    val list = mutableListOf<ContextMenuItem>()

    // 1. 回复
    list.add(ContextMenuItem("回复", SFSymbolType.REPLY, onReply))

    // 2. 仅我方包含：撤回、撤回并编辑、编辑
    if (message.isMine) {
        list.add(ContextMenuItem("撤回", SFSymbolType.UNDO_SEND, onRecall))
        if (!message.isImage && !message.isVoice && !message.isDice) {
            list.add(ContextMenuItem("撤回并编辑", SFSymbolType.RECALL_AND_EDIT, onRecallAndEdit))
            list.add(ContextMenuItem("编辑", SFSymbolType.EDIT, onEdit))
        }
    }

    // 对方消息：拍一拍（私聊 / 群聊均可用，官网亦然），@提及仅群聊
    if (!message.isMine) {
        list.add(ContextMenuItem("拍一拍", SFSymbolType.PAT, onPat))
        if (includeGroupActions) {
            list.add(ContextMenuItem("@提及", SFSymbolType.MENTION, onMention))
        }
    }

    // 3. 拷贝 (我方与对方均支持)
    list.add(ContextMenuItem("拷贝", SFSymbolType.COPY, onCopy))

    // 4. 房管删除他人消息（仅服务端已同步的消息可删）
    if (!message.isMine && canModerateDelete && message.serverId > 0L) {
        list.add(ContextMenuItem("删除消息", SFSymbolType.TRASH, onModerateDelete))
    }

    return list
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
    isDark: Boolean
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
                    color = if (isDark) Color(0xAAFFFFFF) else Color(0x73000000), // 45% black for light mode
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

/**
 * 扩展 Sheet 宫格图标项
 */
@Composable
private fun ActionSheetGridItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f))
                .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        BasicText(
            text = title,
            style = TextStyle(
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        )
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
 * 布局与 [AsyncNetworkImage] 保持一致（按宽度铺满，上下留空露出父容器背景色），
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
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxSize()
        )
    }
}
