package top.lanxint.zerotalk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.ChatMessage
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.data.model.MomentShareCardData
import top.lanxint.zerotalk.data.model.toMomentItem
import top.lanxint.zerotalk.ui.moments.MomentSheetsHost
import top.lanxint.zerotalk.data.model.ConversationItem
import top.lanxint.zerotalk.data.model.IosContactAvatarGradient
import top.lanxint.zerotalk.data.model.MessageCategory
import top.lanxint.zerotalk.data.model.REAL_USER_ID_PLACEHOLDERS
import top.lanxint.zerotalk.data.model.quotePreviewText
import top.lanxint.zerotalk.data.model.resolveQuotedText
import top.lanxint.zerotalk.data.model.UserProfileTarget
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.data.settings.UiPreferencesStore
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.components.GenderBadge
import top.lanxint.zerotalk.ui.components.UserTitleBadge
import top.lanxint.zerotalk.ui.components.IosChatNavBar
import top.lanxint.zerotalk.ui.components.LocalImageViewer
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.ContextMenuItem
import top.lanxint.zerotalk.ui.components.MessageContextMenuOverlay
import top.lanxint.zerotalk.ui.components.buildContextMenuItems
import top.lanxint.zerotalk.ui.components.AppleModalBottomSheet
import top.lanxint.zerotalk.ui.components.ChatComposerAction
import top.lanxint.zerotalk.ui.components.ChatComposerBar
import top.lanxint.zerotalk.ui.components.ChatComposerQuote
import top.lanxint.zerotalk.ui.components.ChatContentCaptureLayer
import top.lanxint.zerotalk.ui.components.SheetAction
import androidx.compose.foundation.lazy.itemsIndexed
import top.lanxint.zerotalk.ui.messages.BubbleContentBox
import top.lanxint.zerotalk.ui.messages.ConversationInfoScreen
import top.lanxint.zerotalk.ui.messages.LocalWallpaperImage
import top.lanxint.zerotalk.ui.messages.MessageTimeHelper
import top.lanxint.zerotalk.ui.messages.decodeLocalWallpaper
import top.lanxint.zerotalk.ui.messages.looksLikeLocalWallpaperPath
import top.lanxint.zerotalk.ui.messages.resolveLocalWallpaperFile
import top.lanxint.zerotalk.ui.moments.MomentNeteasePickerSheet
import top.lanxint.zerotalk.ui.navigation.rememberHasUserProfileLayers
import top.lanxint.zerotalk.ui.sheets.GamePickerSheet
import top.lanxint.zerotalk.ui.utils.BackHandler
import top.lanxint.zerotalk.ui.utils.rememberPhotoPickerLauncher

/**
 * 零语大厅（公共聊天室）
 * 具备标准的 iOS 风格顶部 NavigationBar、实时公屏流与悬浮输入框。
 *
 * 消息气泡采用 QQ 群聊式布局：对方消息头像独占最左一列，右侧竖排「昵称在上、气泡在下」
 * （昵称/性别仅在同一发送者连续消息的首条展示），气泡为四角同半径的普通圆气泡；
 * 自己的消息不展示头像与昵称，蓝色圆气泡靠右。
 *
 * 渲染链对齐官方 PublicChatView：
 * - type === "system" / "voice" / "pat" → 居中提示（不套气泡）；
 * - 图片 / 骰子 / 歌单 / 表情包 / 游戏 / 音乐 / 动态分享 → 裸媒体块（.msg-media-wrap，无气泡底色）；
 * - 其余 → 普通圆角气泡。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicChatroomScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    /** 点击消息里的发送者（头像/昵称）时跳转其个人资料；大厅没有群聊面板，仅此入口 */
    onOpenUserProfile: ((UserProfileTarget) -> Unit)? = null
) {
    val higColors = AppleHigColors.colors(isDark)
    val bgColor = if (isDark) Color(0xFF12141A) else Color(0xFFF3F5F9)
    // 亮暗自适应玻璃材质表面（与私聊 TopNavigationBar 完全一致）：6% 烟熏 / 乳白遮罩
    val buttonSurfaceColor = if (isDark) Color.White else Color.Black
    val buttonSurfaceAlpha = 0.06f
    val titleColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)

    val hallMessages by ZeroTalkClientManager.hallMessages.collectAsState()
    val hallRoomId by ZeroTalkClientManager.hallRoomId.collectAsState()
    // 房管权限（viewer_can_delete_message）：可删除大厅他人消息
    // 注：大厅历史目前未写入 roomBootstrapMap（见汇报），接线后即可生效
    val roomBootstrapMap by ZeroTalkClientManager.roomBootstrapMap.collectAsState()
    val canModerateDeleteHallMessage = roomBootstrapMap[hallRoomId]?.viewerCanDeleteMessage == true

    // 大厅不是真实会话项（不写入 _conversations）：这里只合成一个用于「详细资料页」的会话对象，
    // 让顶栏昵称 / 头像胶囊可点开资料页并在其中设置聊天背景（背景按 id = hallRoomId 存在管理器里）。
    val hallConversation = remember(hallRoomId) {
        ConversationItem(
            id = hallRoomId,
            targetName = "零语大厅",
            // 空头像 → UserAvatar 走「名称首字」兜底，显示「零」（与暗号群聊同一套规则）
            targetAvatar = "",
            // 与群聊兜底一致的一组渐变（IosContactAvatarGradient 也是 ConversationItem 的默认值）
            avatarGradient = IosContactAvatarGradient,
            lastMessage = "",
            timestamp = "",
            // 群聊 / 房间语义：设置页不显示「备注名」（1v1 专属）
            category = MessageCategory.CODE
        )
    }

    // 大厅背景：与私聊共用管理器（key = hallRoomId），在资料页里设置后立刻生效
    val wallpaperMap by ZeroTalkClientManager.conversationWallpapers.collectAsState()
    val globalWallpaper by ZeroTalkClientManager.globalWallpaper.collectAsState()
    val hallWallpaperValue = remember(hallRoomId, wallpaperMap, globalWallpaper) {
        ZeroTalkClientManager.conversationWallpaper(hallRoomId)
    }
    val hallWallpaperRaw = hallWallpaperValue?.trim().orEmpty()
    val hallLocalWallpaperFile = remember(hallWallpaperRaw) { resolveLocalWallpaperFile(hallWallpaperRaw) }
    var hallLocalWallpaperBitmap by remember(hallLocalWallpaperFile) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(hallLocalWallpaperFile) {
        val file = hallLocalWallpaperFile
        hallLocalWallpaperBitmap = if (file == null) null
        else withContext(Dispatchers.IO) { decodeLocalWallpaper(file) }
    }
    val hallRemoteWallpaperUrl = hallWallpaperRaw.takeIf { value ->
        !looksLikeLocalWallpaperPath(value) &&
            (value.startsWith("http://") || value.startsWith("https://"))
    }
    // 浅色底 → 顶栏 / 气泡控件用深色；一旦设置了背景（本地或远程）恒用白色控件（与私聊同一规则）
    val isWhiteBackground = hallLocalWallpaperFile == null && hallRemoteWallpaperUrl == null && !isDark

    val listState = rememberLazyListState()
    // 液态玻璃折射采样 Backdrop：把「壁纸层 + 消息流」统一录进这一层，
    // 供悬浮顶栏（圆形返回键 / 标题胶囊）与底部输入栏（[+] / 输入框）的 LiquidButton 光学折射穿透，
    // 得到真正的液态玻璃（而非 backdrop = null 的纯色兜底画布）。
    // 顶栏 / 输入栏 / 长按菜单 / 资料页全部声明在该捕获层**之外**（z 序在上），
    // 玻璃控件自身不会被录进 Backdrop，杜绝循环采样引发的 RenderThread 崩溃。
    val hallBackdrop = rememberLayerBackdrop()
    val coroutineScope = rememberCoroutineScope()
    val notificationState = LocalNotificationState.current
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputText by remember { mutableStateOf("") }
    var mentionTargets by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    // 底部 [+] 扩展抽屉开合（hoisted：返回键、列表底部避让都要读它）
    var showComposerDrawer by remember { mutableStateOf(false) }
    // 分享歌曲 / 游戏选择面板
    var showMusicShareSheet by remember { mutableStateOf(false) }
    var showGamePickerSheet by remember { mutableStateOf(false) }
    // 顶栏昵称 / 头像胶囊 → 详细资料页（内含聊天背景设置）
    var showHallInfo by remember { mutableStateOf(false) }
    // 退出大厅后继续接收通知（设备级偏好，放在大厅设置页里，与私聊「隐藏提醒」同位置）
    val hallNotifyAfterExit by UiPreferencesStore.hallNotifyAfterExit.collectAsState()
    // 点动态卡片评论数打开的动态（复用动态主界面的评论抽屉）
    var momentCardComment by remember { mutableStateOf<MomentItem?>(null) }
    // 悬浮输入栏顶部在 Root 中的像素坐标（据此计算消息列表的动态底部避让）
    var bottomBarTopYPx by remember { mutableStateOf<Float?>(null) }
    // 悬浮顶栏实测高度（px）：消息列表 / 空态据此从顶栏下方穿过
    var navBarHeightPx by remember { mutableStateOf<Float?>(null) }
    // 键盘是否弹出：抽屉 / 键盘 / 引用条出现时把最后一条消息顶到输入栏之上
    val isImeOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    // 相册选图 → chat_image 上传 → WS type=image（与私聊同链路；
    // 大厅房间由管理器 appendLocalOutgoing 落到 _hallMessages，不会污染会话列表）
    val hallPhotoPicker = rememberPhotoPickerLauncher(
        maxItems = 1,
        onImagesSelected = { picked ->
            val photo = picked.firstOrNull() ?: return@rememberPhotoPickerLauncher
            ZeroTalkClientManager.sendRoomImage(
                roomId = hallRoomId,
                imageBytes = photo.byteArray,
                filename = "hall_image_${System.currentTimeMillis()}.jpg",
                onError = { notificationState.show(it) }
            )
        },
        onPermissionDenied = {
            notificationState.show("需要相册读取权限以发送图片，请在系统设置中允许")
        }
    )
    // 引用回复：被长按「回复」选中的消息；发送后清空
    var quotingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    // 引用跳转进行中：期间跳过「新消息自动滚底」，避免与跳转动画互相抢
    var isJumping by remember { mutableStateOf(false) }
    // 正在高亮闪烁的被引用消息 serverId（1.2s 后清空）
    var flashingMessageId by remember { mutableStateOf<Long?>(null) }

    // 长按菜单锚点（消息 + 气泡在 root 坐标系中的边界，用于放大预览与菜单避让）
    var menuAnchor by remember { mutableStateOf<HallMenuAnchor?>(null) }
    var isMenuShowing by remember { mutableStateOf(false) }
    // 撤回已由 ZeroTalkClientManager.recallMessage 发事件 + 服务端 message_recalled 广播就地标记为已撤回，无需本地兜底
    val visibleMessages = hallMessages

    fun dismissContextMenu() {
        isMenuShowing = false
    }

    LaunchedEffect(isMenuShowing) {
        if (!isMenuShowing && menuAnchor != null) {
            delay(240)
            menuAnchor = null
        }
    }

    // 资料页层级栈（大厅点发送者打开的资料面板）压在上面时，返回事件属于那一层
    val hasProfileLayers = rememberHasUserProfileLayers()

    // 长按菜单浮层拦截（被资料页层级覆盖时让出返回）
    BackHandler(enabled = !hasProfileLayers && menuAnchor != null) {
        dismissContextMenu()
    }

    // [+] 扩展抽屉 / 分享歌曲 / 游戏面板浮层拦截（优先于「退出大厅」）
    BackHandler(enabled = !hasProfileLayers && showComposerDrawer) {
        showComposerDrawer = false
    }
    BackHandler(enabled = !hasProfileLayers && showMusicShareSheet) {
        showMusicShareSheet = false
    }
    BackHandler(enabled = !hasProfileLayers && showGamePickerSheet) {
        showGamePickerSheet = false
    }

    // 详细资料页浮层拦截：先关资料页，再谈退出大厅（页面内下钻的资料层由资料页自己拦）
    BackHandler(enabled = !hasProfileLayers && showHallInfo) {
        showHallInfo = false
    }

    // 大厅主界面返回拦截：退出大厅回到首页（被资料页层级 / 浮层覆盖时让出返回）
    BackHandler(
        enabled = !hasProfileLayers && !showHallInfo && menuAnchor == null &&
            !showComposerDrawer && !showMusicShareSheet && !showGamePickerSheet
    ) {
        onBack()
    }

    // 进入页面时进入大厅；离开时是否退出房间取决于「退出大厅后继续接收通知」开关：
    // - 开启：不发送 leave_room，保持在大厅房间，退出页面后仍能收到大厅消息与通知
    // - 关闭（默认）：与官方一致，退出页面即离开房间
    DisposableEffect(Unit) {
        ZeroTalkClientManager.enterPublicHall()
        onDispose {
            if (!UiPreferencesStore.hallNotifyAfterExit.value) {
                ZeroTalkClientManager.leavePublicHall()
            }
        }
    }

    // 新消息滚动到底部
    // 守卫：引用跳转期间（isJumping）跳过自动滚底，否则新消息到达会把跳转结果顶走；
    // 闪烁结束后 isJumping 复位，后续新消息恢复正常滚底
    LaunchedEffect(visibleMessages.size) {
        if (isJumping) return@LaunchedEffect
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.size - 1)
        }
    }

    // 抽屉展开 / 键盘弹出 / 引用条出现时，把最后一条消息顶到悬浮输入栏之上
    // （与私聊同一套处理：首帧后等动画就绪再做一次精准校准）
    LaunchedEffect(showComposerDrawer, isImeOpen, quotingMessage) {
        if ((showComposerDrawer || isImeOpen || quotingMessage != null) && visibleMessages.isNotEmpty()) {
            delay(30)
            listState.animateScrollToItem(visibleMessages.size - 1)
            delay(320)
            listState.animateScrollToItem(visibleMessages.size - 1)
        }
    }

    // 点击引用条跳转到被引用消息（对齐官方 scrollToQuotedMessage）：
    // - 找到：animateScrollToItem 到该条（大厅 LazyColumn 无 header item，index 可直接用）+ 高亮闪烁 1.2s
    // - 找不到：只提示「原消息不在当前列表中」，不补拉历史（官方口径）
    fun jumpToQuotedMessage(replyToId: Long) {
        if (replyToId <= 0L || isJumping) return
        val targetIndex = visibleMessages.indexOfFirst { it.serverId == replyToId }
        if (targetIndex < 0) {
            coroutineScope.launch { notificationState.show("原消息不在当前列表中") }
            return
        }
        coroutineScope.launch {
            isJumping = true
            try {
                listState.animateScrollToItem(targetIndex)
                flashingMessageId = replyToId
                delay(1200)
            } finally {
                // 正常路径与中途取消都复位，避免闪烁状态卡住或自动滚底守卫永远生效
                flashingMessageId = null
                isJumping = false
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val rootHeightPx = with(density) { maxHeight.toPx() }

        // 悬浮输入栏（含抽屉 / 软键盘）占据的物理高度 -> 消息列表动态底部避让
        val bottomOccupiedDp = remember(bottomBarTopYPx, rootHeightPx, showComposerDrawer) {
            val topY = bottomBarTopYPx
            if (topY != null && rootHeightPx > 0f) {
                with(density) { (rootHeightPx - topY).coerceAtLeast(0f).toDp() }
            } else {
                if (showComposerDrawer) 400.dp else 80.dp
            }
        }
        val dynamicBottomPadding by animateDpAsState(
            targetValue = maxOf(96.dp, bottomOccupiedDp + 16.dp),
            animationSpec = spring(stiffness = 450f, dampingRatio = 0.82f),
            label = "HallDynamicBottomPadding"
        )

        // 顶部避让悬浮顶栏：实测顶栏高度（含状态栏安全区）+ 12dp 呼吸间距；
        // 测量到之前用 100.dp 兜底，避免首帧跳动（顶栏自身高度约 103dp，兜底取近似值）。
        val navBarTopPadding = remember(navBarHeightPx, density) {
            val measured = navBarHeightPx
            val navBarHeightDp =
                if (measured != null && measured > 0f) with(density) { measured.toDp() } else 100.dp
            navBarHeightDp + 12.dp
        }

        // 顶部渐隐蒙层终点：悬浮顶栏底边（实测高度）+ 30dp，与私聊 maskFadeEndY 同一口径
        // （私聊 = statusBarTop + 127dp = 顶栏底边 + 30dp）；未测量到前用 100.dp 兜底。
        val maskFadeEndY = remember(navBarHeightPx, density) {
            val measured = navBarHeightPx
            val navBarHeightDp =
                if (measured != null && measured > 0f) with(density) { measured.toDp() } else 100.dp
            with(density) { (navBarHeightDp + 30.dp).toPx() }
        }

        // ---- 1. 统一内容捕获层：壁纸层 + 消息流 LazyColumn 统一录入 hallBackdrop ----
        // 供悬浮顶栏（圆形返回键 / 标题胶囊）与底部输入栏（[+] / 输入框）的 LiquidButton
        // 光学折射穿透，得到真正的液态玻璃（而非 backdrop = null 的纯色兜底画布）。
        // 顶栏 / 输入栏 / 长按菜单 / 资料页全部声明在该捕获层**之外**（z 序在上），
        // 玻璃控件自身不会被录进 Backdrop，杜绝循环采样引发的 RenderThread 崩溃。
        // 内容层：仅消息流。顶栏已移出流内，改为下方声明的悬浮覆盖层（z 序在内容层之上），
        // 消息从顶栏下面穿过；列表 / 空态顶部用实测顶栏高度做避让。
        // 不再用 imePadding 压缩高度，悬浮输入栏自带 imePadding，
        // 消息列表改为按 bottomBarTopYPx 动态避让。
        // 复用 ui/components/ChatContentCaptureLayer（与私聊同一份实现）。
        ChatContentCaptureLayer(
            backdrop = hallBackdrop,
            listState = listState
        ) {
            // ---- 背景层：大厅会话壁纸（在详细资料页里设置后立即生效）----
            // 本地文件优先（离线可显示），其次远程地址；都没有时铺页面纯色底。
            // 必须位于捕获层内部，玻璃才能折射出壁纸 / 页面底色。
            val localHallWallpaper = hallLocalWallpaperBitmap
            if (hallLocalWallpaperFile != null && localHallWallpaper != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    LocalWallpaperImage(
                        bitmap = localHallWallpaper,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (hallRemoteWallpaperUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncNetworkImage(
                        url = hallRemoteWallpaperUrl,
                        contentDescription = "自定义对话背景",
                        shape = RectangleShape,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // 无自定义背景：捕获层内铺页面纯色底（与私聊 pageBg 兜底同一写法），
                // 保证玻璃折射的是页面底色而不是透明画布
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                )
            }

            // 消息列表或空占位（列表视口顶边与根顶边对齐，顶部避让交给 contentPadding / padding）
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (visibleMessages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            // 空态同样避开悬浮顶栏：顶部 = 实测顶栏高度 + 12dp 呼吸间距
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                bottom = 24.dp,
                                top = navBarTopPadding
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.8.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E2430) else Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(32.4.dp)
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        BasicText(
                            text = "欢迎来到零语大厅",
                            style = TextStyle(
                                // 壁纸底上白色可读（与顶栏 / 气泡控件的 isWhiteBackground 同一判定）
                                color = if (isWhiteBackground) titleColor else Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        BasicText(
                            text = "零语大厅消息全员实时同步，来发一条打个招呼吧～",
                            style = TextStyle(
                                color = if (isWhiteBackground) subtitleColor else Color.White.copy(alpha = 0.78f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            // 顶部渐隐蒙层（与私聊同一套渐变参数 + BlendMode.DstIn）：
                            // 消息滑到悬浮顶栏下方时渐隐；顶栏声明在捕获层之外，不受蒙层影响、保持清晰。
                            // Offscreen 合成保证 DstIn 只作用于本列表图层，不波及壁纸 / 顶栏。
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
                            start = 16.dp,
                            end = 16.dp,
                            // 顶部避让悬浮顶栏：实测顶栏高度 + 12dp 呼吸间距
                            // （测量到之前用 100.dp 兜底，避免首帧跳动）
                            top = navBarTopPadding,
                            // 悬浮输入栏 + 抽屉 + 软键盘的动态避让（下方按 bottomBarTopYPx 实时计算）
                            bottom = dynamicBottomPadding
                        )
                    ) {
                        itemsIndexed(visibleMessages, key = { _, it -> it.id }) { index, msg ->
                            val isFirstInGroup = MessageTimeHelper.isFirstOfSenderGroup(visibleMessages, index)
                            // 已撤回消息整条替换为居中占位，不再展示头像 / 昵称列
                            val isPeerMessage = !msg.isMine && !msg.isSystem && !msg.isDeleted
                            val bottomSpacing = MessageTimeHelper.computeMessageBottomSpacing(visibleMessages, index)

                            // 引用跳转命中：背景高亮闪烁（对齐官方 .chat-msg--flash，峰值 #3b82f61a）
                            val isQuotedTarget = flashingMessageId != null && msg.serverId == flashingMessageId
                            val flashColor by animateColorAsState(
                                targetValue = if (isQuotedTarget) Color(0x1A3B82F6) else Color.Transparent,
                                animationSpec = tween(durationMillis = if (isQuotedTarget) 140 else 320),
                                label = "HallQuotedMessageFlash"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(flashColor, RoundedCornerShape(13.6.dp))
                            ) {
                                val centeredNoticeText = when {
                                    msg.isPat -> msg.patText.ifBlank {
                                        if (msg.isMine) "你 拍了拍 对方" else "有人 拍了拍 你"
                                    }
                                    msg.isVoiceCall -> msg.voiceCallText.ifBlank { "[语音通话]" }
                                    else -> msg.content
                                }

                                // 官方渲染链：type === "system" / "voice" / "pat" → 居中提示（不套气泡）
                                if (msg.isSystem || msg.isVoiceCall || msg.isPat) {
                                    HallCenteredNotice(
                                        text = centeredNoticeText,
                                        isDark = isDark,
                                        isWhiteBackground = isWhiteBackground
                                    )
                                } else {
                                    HallMessageBubble(
                                        msg = msg,
                                        isDark = isDark,
                                        isWhiteBackground = isWhiteBackground,
                                        showAvatar = isPeerMessage && isFirstInGroup,
                                        reserveAvatarSpace = isPeerMessage,
                                        showSenderInfo = isPeerMessage && isFirstInGroup,
                                        // 被引用消息已撤回时，引用条本地兜底显示「该消息已被撤回」
                                        quotedTextOverride = visibleMessages.resolveQuotedText(msg),
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
                                        onSenderClick = if (isPeerMessage &&
                                            msg.senderId.isNotBlank() && msg.senderId !in REAL_USER_ID_PLACEHOLDERS
                                        ) {
                                            {
                                                onOpenUserProfile?.invoke(
                                                    UserProfileTarget(
                                                        userId = "",
                                                        uid = msg.senderId,
                                                        name = msg.senderName,
                                                        avatarUrl = msg.senderAvatar
                                                    )
                                                )
                                            }
                                        } else null,
                                        // 自己的消息也支持长按（系统消息仍排除），对齐官方大厅
                                        onLongPress = { bounds ->
                                            // 已撤回消息没有任何可执行操作项，不弹菜单（对齐官网）
                                            if (!msg.isSystem && !msg.isDeleted) {
                                                menuAnchor = HallMenuAnchor(msg, bounds)
                                                isMenuShowing = true
                                            }
                                        },
                                        // 引用条点击 → 跳转被引用消息
                                        onQuoteClick = { replyId -> jumpToQuotedMessage(replyId) },
                                        onPat = if (!msg.isMine && !msg.isSystem && !msg.isImage) {
                                            {
                                                notificationState.show(
                                                    ZeroTalkClientManager.sendPat(
                                                        ZeroTalkClientManager.hallRoomId.value,
                                                        msg.senderId
                                                    ).message(msg.senderName)
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
            }
        }

        // ---- 顶部 iOS 26 原生导航栏（悬浮覆盖层：z 序在内容层之上、长按菜单 / 资料页之下）----
        // 与私聊 / 群聊共用同一套 LiquidButton 材质、居中标题与安全区处理
        // （抽到 ui/components/IosChatNavBar.kt）。
        // 与暗号群聊（PrivateChatScreen.TopNavigationBar）对齐：圆形返回键（无「首页」文字）、
        // 无副标题、昵称胶囊上方压 60dp 头像（胶囊顶部重叠约 10dp 的效果保持不变），
        // 点胶囊 / 头像打开详细资料页（可设置聊天背景）。
        // 栏体本身不铺任何底色（IosChatNavBar 只做 statusBarsPadding + 内边距），
        // 壁纸 / 消息从栏体下方与两侧透出；圆形返回键与标题胶囊走 hallBackdrop 真实液态玻璃。
        // 标题胶囊补齐私聊同款「›」箭头（showChevron = true）。
        // 实测栏体高度（含状态栏安全区）：消息列表 / 空态据此避让。
        IosChatNavBar(
            onBack = onBack,
            title = hallConversation.targetName,
            isDark = isDark,
            isWhiteBackground = isWhiteBackground,
            surfaceColor = buttonSurfaceColor,
            surfaceAlpha = buttonSurfaceAlpha,
            backdrop = hallBackdrop,
            showChevron = true,
            onTitleClick = { showHallInfo = true },
            titleAvatar = {
                UserAvatar(
                    url = hallConversation.targetAvatar,
                    name = hallConversation.targetName,
                    size = 60.dp,
                    gradient = hallConversation.avatarGradient,
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
            modifier = Modifier
                .align(Alignment.TopCenter)
                .drawWithContent {
                    // 列表滚动时持续重绘顶栏，保证玻璃每帧重新采样已更新的 hallBackdrop
                    listState.firstVisibleItemScrollOffset
                    listState.firstVisibleItemIndex
                    drawContent()
                }
                .onGloballyPositioned { coords ->
                    // 取栏体自身高度（含 statusBarsPadding），与列表视口顶边对齐，避免 root 偏移误差
                    navBarHeightPx = coords.size.height.toFloat()
                }
        )

        // ---- 底部输入控制栏（公共组件 ChatComposerBar：[+] + 输入框 + 引用条 + 4 项扩展抽屉）----
        // 根是 fillMaxSize 的悬浮外壳（scrim + 底部悬浮输入栏），必须作为聊天内容的**兄弟**声明，
        // 且长按菜单 / 资料页等浮层要声明在它之后（z 序）。
        ChatComposerBar(
            drawerExpanded = showComposerDrawer,
            onDrawerExpandedChange = { showComposerDrawer = it },
            inputText = inputText,
            onInputTextChange = { inputText = it },
            onSend = {
                val textToSend = inputText.trim()
                if (textToSend.isNotBlank()) {
                    val mentionIds = mentionTargets
                        .map { it.first }
                        .filter(String::isNotBlank)
                        .distinct()
                    // 引用快照必须在清空 quotingMessage 之前取
                    val quotedText = quotingMessage?.quotePreviewText()
                    val quotedIsMine = quotingMessage?.isMine
                    val quotedSenderName = quotingMessage?.senderName
                    val replyToId = quotingMessage?.serverId?.takeIf { it > 0L }
                    inputText = ""
                    mentionTargets = emptyList()
                    quotingMessage = null
                    ZeroTalkClientManager.sendHallMessage(
                        content = textToSend,
                        mentionIds = mentionIds,
                        messageType = "text",
                        replyToId = replyToId,
                        quotedText = quotedText,
                        quotedIsMine = quotedIsMine,
                        quotedSenderName = quotedSenderName
                    ) { ok ->
                        if (!ok) {
                            coroutineScope.launch {
                                notificationState.show("消息未发送成功，连接已断开，请稍后重试")
                            }
                        }
                    }
                }
            },
            quote = quotingMessage?.let { qm ->
                ChatComposerQuote(
                    text = qm.quotePreviewText(),
                    isMine = qm.isMine,
                    cancelContentDescription = "取消引用"
                )
            },
            onCancelQuote = { quotingMessage = null },
            // 大厅只有官网的 4 项：发送图片 / 分享歌曲 / 摇骰子 / 游戏
            // （私聊抽屉里的「语音文件 / 房间音乐 / 语音通话」大厅一律不要）
            actions = listOf(
                ChatComposerAction(
                    id = "photo",
                    label = "发送图片",
                    icon = Icons.Default.Image,
                    color = Color(0xFF34C759)
                ) {
                    // 实测链路：presign(upload_source=chat_image, room_id=大厅) → PUT → bind → WS type=image
                    hallPhotoPicker.launch()
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
                    id = "dice",
                    label = "摇骰子",
                    icon = Icons.Default.Casino,
                    color = Color(0xFFFF9500)
                ) {
                    // 大厅感知：本地回显写入 _hallMessages，会话列表不动
                    ZeroTalkClientManager.sendRoomDice(hallRoomId)
                },
                ChatComposerAction(
                    id = "game",
                    label = "游戏",
                    icon = Icons.Default.SportsEsports,
                    color = Color(0xFFAF52DE)
                ) {
                    showGamePickerSheet = true
                }
            ),
            isDark = isDark,
            // 输入栏的 [+] 与输入框走 hallBackdrop 真实液态玻璃（与顶栏同一个内容捕获层）
            backdrop = hallBackdrop,
            // 浅色纯色底用深色控件；深色底 / 壁纸底用白色控件（与私聊 controlContentColor 同一规则）
            controlContentColor = if (isWhiteBackground) Color(0xFF1C1C1E) else Color.White,
            surfaceColor = buttonSurfaceColor,
            surfaceAlpha = buttonSurfaceAlpha,
            placeholder = "发信息",
            inputFocusRequester = inputFocusRequester,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = if (showComposerDrawer) 4.dp else 12.dp
            ),
            onBottomBarTopChanged = { bottomBarTopYPx = it },
            // 列表滚动时持续重绘输入栏外壳，保证玻璃每帧重新采样已更新的 hallBackdrop
            tailModifier = Modifier.drawWithContent {
                listState.firstVisibleItemScrollOffset
                listState.firstVisibleItemIndex
                drawContent()
            }
        )

        // ---- 分享歌曲面板（网易云解析 / 搜索 / 歌单，复用私聊同一个 sheet）----
        if (showMusicShareSheet) {
            AppleModalBottomSheet(
                onDismissRequest = { showMusicShareSheet = false },
                backdrop = null,
                title = "分享网易云音乐",
                leadingAction = SheetAction.Close { showMusicShareSheet = false },
                isDark = isDark
            ) {
                MomentNeteasePickerSheet(
                    isDark = isDark,
                    onPick = { music ->
                        showMusicShareSheet = false
                        // 既有音乐发送链路 + 大厅感知落库
                        ZeroTalkClientManager.sendRoomMusic(hallRoomId, music)
                    },
                    onDismiss = { showMusicShareSheet = false }
                )
            }
        }

        // ---- 游戏选择面板（与私聊同一套棋牌 / 谁是卧底入口）----
        if (showGamePickerSheet) {
            AppleModalBottomSheet(
                onDismissRequest = { showGamePickerSheet = false },
                backdrop = null,
                title = null,
                leadingAction = SheetAction.None,
                isDark = isDark
            ) {
                GamePickerSheet(
                    isDark = isDark,
                    onLaunch = { gameType, gameName ->
                        showGamePickerSheet = false
                        ZeroTalkClientManager.sendRoomGameInvite(hallRoomId, gameType, gameName)
                        notificationState.show("已发起「$gameName」对战")
                    },
                    onDismiss = { showGamePickerSheet = false }
                )
            }
        }

        // ---- 长按上下文菜单（与私聊共用公共组件，毛玻璃菜单 + 气泡 1.1 倍放大预览）----
        menuAnchor?.let { anchor ->
            val target = anchor.message
            val menuItems = remember(target.id, canModerateDeleteHallMessage) {
                buildContextMenuItems(
                    message = target,
                    // 大厅引用回复：sendHallMessage 已支持 reply_to_id，接上「回复」项
                    onReply = {
                        quotingMessage = target
                        dismissContextMenu()
                        coroutineScope.launch {
                            delay(100)
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    onRecall = {
                        val msg = target
                        // 官方走 WS recall_message 事件；服务端广播 message_recalled 后由管理器就地标记为已撤回。
                        // 本地由管理器即时处理一次，故无需再做屏幕级兜底隐藏
                        ZeroTalkClientManager.recallMessage(hallRoomId, msg.serverId) { ok, err ->
                            notificationState.show(if (ok) "已撤回消息" else (err ?: "撤回失败"))
                        }
                        dismissContextMenu()
                    },
                    onRecallAndEdit = {
                        val msg = target
                        ZeroTalkClientManager.recallMessage(hallRoomId, msg.serverId) { ok, err ->
                            if (!ok) notificationState.show(err ?: "撤回失败")
                        }
                        inputText = msg.content
                        notificationState.show("已撤回，可重新编辑")
                        dismissContextMenu()
                    },
                    onEdit = {
                        inputText = target.content
                        notificationState.show("正在编辑消息")
                        dismissContextMenu()
                    },
                    onCopy = {
                        notificationState.show(if (target.isImage) "已拷贝图片" else "已拷贝文本")
                        dismissContextMenu()
                    },
                    includeGroupActions = true,
                    onPat = {
                        notificationState.show(
                            ZeroTalkClientManager.sendPat(hallRoomId, target.senderId)
                                .message(target.senderName)
                        )
                        dismissContextMenu()
                    },
                    onMention = {
                        val name = target.senderName.ifBlank { target.senderId }
                        val separator = if (inputText.isNotBlank() && !inputText.last().isWhitespace()) " " else ""
                        inputText = inputText + separator + "@" + name + " "
                        if (target.senderId.isNotBlank() && target.senderId != "peer" && target.senderId != "me") {
                            mentionTargets = (mentionTargets + (target.senderId to name)).distinctBy { it.first }
                        }
                        dismissContextMenu()
                        coroutineScope.launch {
                            delay(100)
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    canModerateDelete = canModerateDeleteHallMessage,
                    onModerateDelete = {
                        ZeroTalkClientManager.moderateDeleteMessage(hallRoomId, target.serverId) { success, message ->
                            notificationState.show(if (success) "已删除该消息" else (message ?: "删除消息失败"))
                        }
                        dismissContextMenu()
                    }
                )
            }

            MessageContextMenuOverlay(
                bounds = anchor.bounds,
                isMine = target.isMine,
                isShowing = isMenuShowing,
                backdrop = null,
                isDark = isDark,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                onDismiss = { dismissContextMenu() },
                menuItems = menuItems,
                preview = {
                    // 放大预览复用同一套气泡内容组件（与私聊预览一致）
                    BubbleContentBox(
                        message = target,
                        hasTail = false,
                        isDark = isDark,
                        higColors = higColors,
                        customShape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        }

        // ---- 详细资料页（点顶栏昵称 / 头像胶囊打开；内含聊天背景设置）----
        // 大厅没有单一对端对象：走 settingsOnly，只展示会话设置（聊天背景），
        // 背景以 id = hallRoomId 写入管理器，本页背景层随即生效。
        AnimatedVisibility(
            visible = showHallInfo,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            ConversationInfoScreen(
                conversation = hallConversation,
                onBack = { showHallInfo = false },
                backdrop = hallBackdrop,
                isDark = isDark,
                currentWallpaperKey = hallWallpaperValue,
                settingsOnly = true,
                // 大厅专属设置：退出大厅后继续接收通知（放在大厅设置页，与私聊「隐藏提醒」同位置）
                hallNotifyAfterExit = hallNotifyAfterExit,
                onHallNotifyAfterExitChange = { UiPreferencesStore.setHallNotifyAfterExit(it) },
                // 页面内下钻的资料层压在上面时，返回事件属于那一层
                backEnabled = !hasProfileLayers
            )
        }

        // ---- 动态评论抽屉（点聊天里动态卡片的评论数打开）----
        MomentSheetsHost(
            commentTarget = momentCardComment,
            shareTarget = null,
            isDark = isDark,
            backdrop = null,
            onDismissComment = { momentCardComment = null },
            onDismissShare = {}
        )
    }
}

/**
 * 大厅长按菜单锚点：被长按的消息 + 气泡在 root 坐标系中的边界
 */
private data class HallMenuAnchor(
    val message: ChatMessage,
    val bounds: Rect
)

/**
 * 大厅居中提示（system / voice / pat）
 *
 * 对齐官方：这三类消息均为居中弱化文案，不套气泡、不展示头像与昵称。
 */
@Composable
private fun HallCenteredNotice(text: String, isDark: Boolean, isWhiteBackground: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                color = hallSecondaryTextColor(isDark, isWhiteBackground),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        )
    }
}

/**
 * 大厅弱化文字色（居中提示 / 昵称）
 *
 * 浅色纯色底沿用原有灰色；壁纸底（浅色页 + 已设背景）改用白色，避免深色文字压在深色壁纸上。
 */
private fun hallSecondaryTextColor(isDark: Boolean, isWhiteBackground: Boolean): Color = when {
    isWhiteBackground -> Color(0xFF64748B)
    isDark -> Color(0xFF8B949E)
    else -> Color.White.copy(alpha = 0.85f)
}

/**
 * 大厅单条消息气泡（统一普通圆角气泡，无尖尾）
 */
@Composable
private fun HallMessageBubble(
    msg: ChatMessage,
    isDark: Boolean,
    isWhiteBackground: Boolean = false,
    showSenderInfo: Boolean = false,
    showAvatar: Boolean = false,
    reserveAvatarSpace: Boolean = false,
    onSenderClick: (() -> Unit)? = null,
    onLongPress: (Rect) -> Unit = {},
    /** 双击气泡 = 拍一拍（为 null 时单击保持即时响应） */
    onPat: (() -> Unit)? = null,
    /** 点击气泡内引用条：回传被引用消息 serverId（大厅用于跳转 + 高亮） */
    onQuoteClick: ((Long) -> Unit)? = null,
    /** 引用条预览文案覆盖（被引用消息已撤回时显示「该消息已被撤回」） */
    quotedTextOverride: String? = null,
    /** 点动态分享卡片主体 → 作者资料页并定位到该动态 */
    onMomentCardClick: ((MomentShareCardData) -> Unit)? = null,
    /** 点动态分享卡片的评论数 → 打开评论 */
    onMomentCardCommentClick: ((MomentShareCardData) -> Unit)? = null
) {
    val isMine = msg.isMine
    val higColors = AppleHigColors.colors(isDark)
    val imageViewer = LocalImageViewer.current
    // 气泡在 root 坐标系中的边界：长按菜单定位 + 1.1 倍放大预览用
    var bubbleBounds by remember { mutableStateOf(Rect.Zero) }
    // pointerInput 的 block 只在 key 变化时重启，回调与 msg 必须经 rememberUpdatedState 取最新值：
    // 同一条消息被原地更新（撤回 / 引用变化）时 item.id 不变，否则长按菜单会拿到旧快照
    val currentMessage by rememberUpdatedState(msg)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnPat by rememberUpdatedState(onPat)
    val hasDoubleTap = onPat != null

    // QQ 群聊式布局：对方消息「头像独占最左一列 + 右侧竖排（昵称在上、气泡在下）」；
    // 连续消息折叠头像时保留 44dp 留白占位，确保同一成员连续气泡左边缘对齐整洁；
    // 自己的消息不展示头像与昵称，蓝色气泡靠右。
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (reserveAvatarSpace) {
            if (showAvatar) {
                UserAvatar(
                    url = msg.senderAvatar,
                    name = msg.senderName.ifBlank { msg.senderId },
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
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            // 昵称 + 性别：展示在同一发送者连续消息的首条，位于气泡右上方
            if (!isMine && showSenderInfo) {
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
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    BasicText(
                        text = msg.senderName.ifBlank { "零友" },
                        style = TextStyle(
                            color = hallSecondaryTextColor(isDark, isWhiteBackground),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        // 昵称过长时省略，把空间让给性别 / 称号徽章（徽章 flex-shrink:0）
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    GenderBadge(
                        gender = msg.senderGender,
                        isDark = isDark,
                        fontSize = 10.sp,
                        horizontalPadding = 5.dp,
                        verticalPadding = 1.dp
                    )
                    // 发送者称号（官方消息行顺序：username → gender → UserTitleBadge）
                    UserTitleBadge(
                        title = msg.authorTitle,
                        color = msg.authorTitleColor,
                        isDark = isDark
                    )
                }
            }

            // 与私聊 / 群聊共用同一套气泡内容组件（BubbleContentBox）：
            // 对方气泡恒为深灰 0xB32C2C2E + 白字，我方恒为蓝色 0xFF007AFF；
            // 大厅为 QQ 群聊式普通圆气泡（16dp 同半径、无尖尾）。
            BubbleContentBox(
                message = msg,
                hasTail = false,
                isDark = isDark,
                higColors = higColors,
                customShape = RoundedCornerShape(16.dp),
                // 浅色纯色底 → 深色占位文案；深色底 / 壁纸底 → 白色（与顶栏同一判定）
                isWhiteBackground = isWhiteBackground,
                onImageClick = { url -> imageViewer.open(url) },
                onEnterGame = { gameType, gameId ->
                    ZeroTalkClientManager.openGameSession(gameType, gameId)
                },
                onQuoteClick = onQuoteClick,
                quotedTextOverride = quotedTextOverride,
                onMomentCardClick = onMomentCardClick,
                onMomentCardCommentClick = onMomentCardCommentClick,
                // 大厅原有的「气泡内右下角时间戳」：通过行尾槽位保留
                trailingContent = {
                    BasicText(
                        text = msg.timestamp,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        ),
                        // 时间戳必须单行：多行正文下不能被挤成竖排
                        maxLines = 1,
                        softWrap = false
                    )
                },
                modifier = Modifier
                    // 长按菜单锚点：取气泡在 root 坐标系中的边界
                    .onGloballyPositioned { coords -> bubbleBounds = coords.boundsInRoot() }
                    // 点击 / 长按 / 双击（拍一拍）逻辑与私聊同款，从原大厅气泡实现整体搬到容器上：
                    // - 单击：图片消息打开大图（图片组件自身不注册指针手势，不会双重触发）
                    // - 长按：弹上下文菜单（撤回 / 引用 / 拷贝…）
                    // - 双击：拍一拍（仅对方非图片消息，由 onPat 是否为 null 决定）
                    .pointerInput(msg.id, hasDoubleTap) {
                        val handleTap = {
                            val current = currentMessage
                            if (current.isImage && current.imageUrl.isNotBlank()) {
                                imageViewer.open(current.imageUrl)
                            }
                        }
                        if (hasDoubleTap) {
                            detectTapGestures(
                                onTap = { handleTap() },
                                onDoubleTap = { currentOnPat?.invoke() },
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
        }
    }
}
