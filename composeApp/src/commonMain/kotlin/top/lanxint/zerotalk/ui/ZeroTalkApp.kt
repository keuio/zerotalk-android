package top.lanxint.zerotalk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import top.lanxint.zerotalk.data.model.AppThemeMode
import top.lanxint.zerotalk.data.model.MessageCategory
import top.lanxint.zerotalk.data.model.UserProfile
import top.lanxint.zerotalk.data.model.UserProfileLayerHost
import top.lanxint.zerotalk.data.model.UserProfileTarget
import top.lanxint.zerotalk.data.model.ConversationItem
import top.lanxint.zerotalk.data.model.resolveConversationStatusTag
import top.lanxint.zerotalk.data.voice.VoiceCallEvent
import top.lanxint.zerotalk.ui.voice.VoiceCallOverlay
import top.lanxint.zerotalk.data.repository.ClientStatus
import top.lanxint.zerotalk.data.repository.NotificationCenterStore
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.data.settings.UiPreferencesStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.lanxint.zerotalk.ui.components.AppleModalBottomSheet
import top.lanxint.zerotalk.ui.components.ImageViewerProvider
import top.lanxint.zerotalk.ui.components.NotificationProvider
import top.lanxint.zerotalk.ui.components.SheetAction
import top.lanxint.zerotalk.ui.components.ZeroTalkNotificationHost
import top.lanxint.zerotalk.ui.components.rememberNotificationState
import top.lanxint.zerotalk.ui.home.HomeScreen
import top.lanxint.zerotalk.ui.messages.MessagesScreen
import top.lanxint.zerotalk.ui.messages.OtherUserProfileScreen
import top.lanxint.zerotalk.ui.messages.PrivateChatScreen
import top.lanxint.zerotalk.ui.navigation.ZeroTalkNavBar
import top.lanxint.zerotalk.ui.navigation.ZeroTalkTab
import top.lanxint.zerotalk.ui.navigation.rememberHasUserProfileLayers
import top.lanxint.zerotalk.ui.navigation.rememberUserProfileLayerSlot
import top.lanxint.zerotalk.ui.moments.MomentsScreen
import top.lanxint.zerotalk.ui.profile.ProfileScreen
import top.lanxint.zerotalk.ui.screens.PublicChatroomScreen
import top.lanxint.zerotalk.data.network.MomentsPrivacyData
import top.lanxint.zerotalk.ui.utils.BackHandler
import top.lanxint.zerotalk.ui.utils.applyLiquidGlassCompatMode
import top.lanxint.zerotalk.ui.utils.rememberPlatformAppActions
import top.lanxint.zerotalk.ui.sheets.HomeSheetType
import top.lanxint.zerotalk.ui.sheets.MatchingPreferences
import top.lanxint.zerotalk.ui.sheets.MbtiTest
import top.lanxint.zerotalk.ui.sheets.ProfileNotificationCenter
import top.lanxint.zerotalk.ui.sheets.ProfileSettingItem
import top.lanxint.zerotalk.ui.sheets.ProfileSheetType
import top.lanxint.zerotalk.ui.sheets.SheetBlockListContent
import top.lanxint.zerotalk.ui.sheets.SheetCatchHistoryContent
import top.lanxint.zerotalk.ui.sheets.SheetCatchMomentsContent
import top.lanxint.zerotalk.ui.sheets.SheetChangePasswordContent
import top.lanxint.zerotalk.ui.sheets.SheetContactUsContent
import top.lanxint.zerotalk.ui.sheets.SheetCreateRoomContent
import top.lanxint.zerotalk.ui.sheets.SheetDeleteAccountConfirmContent
import top.lanxint.zerotalk.ui.sheets.SheetDonateContent
import top.lanxint.zerotalk.ui.sheets.SheetEditProfileContent
import top.lanxint.zerotalk.ui.sheets.SheetFeedbackContent
import top.lanxint.zerotalk.ui.sheets.SheetJoinRoomContent
import top.lanxint.zerotalk.ui.sheets.SheetLoginContent
import top.lanxint.zerotalk.ui.sheets.SheetLogoutConfirmContent
import top.lanxint.zerotalk.ui.sheets.SheetMatchingInProgressContent
import top.lanxint.zerotalk.ui.sheets.SheetMatchingSettingsContent
import top.lanxint.zerotalk.ui.sheets.SheetMbtiTestContent
import top.lanxint.zerotalk.ui.sheets.SheetMyReportsContent
import top.lanxint.zerotalk.ui.sheets.SheetNeteaseBindContent
import top.lanxint.zerotalk.ui.sheets.SheetNotificationCenterContent
import top.lanxint.zerotalk.ui.sheets.SheetPenaltyReliefContent
import top.lanxint.zerotalk.ui.sheets.SheetPrivacySettingsContent
import top.lanxint.zerotalk.ui.sheets.SheetSearchUsersContent
import top.lanxint.zerotalk.ui.sheets.SheetSecurityCenterContent
import top.lanxint.zerotalk.ui.sheets.SheetSettingDetailContent
import top.lanxint.zerotalk.ui.theme.AppleHigTheme
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroTalkApp() {
    val systemDark = isSystemInDarkTheme()
    var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    // 兼容渲染模式（设备级本地偏好）：开启后液态玻璃降级到 Android 13 以下的渲染能力
    var liquidGlassCompat by remember { mutableStateOf(UiPreferencesStore.liquidGlassCompatMode) }
    // 后台接收消息（保活前台服务）：设备级偏好，默认开启
    val backgroundReceiveEnabled by UiPreferencesStore.backgroundReceiveEnabled.collectAsState()
    // 渲染能力总闸是 KMPLiquidGlass 的全局状态，随开关即时下发（无需重启即生效）
    SideEffect { applyLiquidGlassCompatMode(liquidGlassCompat) }
    var selectedTab by remember { mutableStateOf(ZeroTalkTab.HOME) }
    // 仅记录切换后前一个 Tab 作为根页面回退目标（例如：首页->消息，按返回回到首页；动态->我的，按返回回到动态）
    var previousTab by remember { mutableStateOf<ZeroTalkTab?>(null) }
    var inChatroom by remember { mutableStateOf(false) }

    var lastBackPressTime by remember { mutableStateOf(0L) }
    val platformActions = rememberPlatformAppActions()

    val notificationState = rememberNotificationState()
    val coroutineScope = rememberCoroutineScope()

    // 用户资料状态（默认无账号状态）
    var userProfile by remember { mutableStateOf(UserProfile.NO_ACCOUNT) }
    var editingProfile by remember { mutableStateOf(userProfile) }

    // 观察全局零语后端连接与资料状态
    val clientStatus by ZeroTalkClientManager.status.collectAsState()
    val managerUserProfile by ZeroTalkClientManager.userProfile.collectAsState()
    val managerLoginData by ZeroTalkClientManager.loginData.collectAsState()
    val roomPeers by ZeroTalkClientManager.roomPeers.collectAsState()

    LaunchedEffect(managerUserProfile) {
        userProfile = managerUserProfile
    }

    // 冷启动恢复登录态：优先复用本地会话 Cookie，失效则用本地保存的账号密码静默续登
    LaunchedEffect(Unit) {
        ZeroTalkClientManager.restoreSession()
    }

    var privacySettings by remember { mutableStateOf(MomentsPrivacyData()) }
    val managerPrivacy by ZeroTalkClientManager.privacySettings.collectAsState()
    LaunchedEffect(managerPrivacy) {
        privacySettings = managerPrivacy
    }

    LaunchedEffect(clientStatus) {
        when (clientStatus) {
            is ClientStatus.Connected -> {
                notificationState.show("已连接零语云服务")
            }
            is ClientStatus.Error -> {
                notificationState.show("服务连接失败: ${(clientStatus as ClientStatus.Error).message}")
            }
            else -> {}
        }
    }

    LaunchedEffect(managerLoginData) {
        if (managerLoginData == null) return@LaunchedEffect

        var previousUnread: Int? = null
        while (true) {
            val unread = ZeroTalkClientManager.apiService.getUnreadNotificationCount().getOrNull()
            if (unread != null) {
                val previous = previousUnread
                if (previous != null && unread > previous) {
                    notificationState.showMessageNotification(unread - previous, unread)
                }
                previousUnread = unread
            }
            delay(10_000L)
        }
    }

    LaunchedEffect(managerLoginData) {
        if (managerLoginData == null) return@LaunchedEffect

        ZeroTalkClientManager.newMessageNotification.collect { notification ->
            // 系统通知已由 ZeroTalkClientManager 进程级投递（不依赖 UI 组合），此处只负责应用内提示
            notificationState.showIncomingMessage(
                roomName = notification.roomName,
                senderName = notification.senderName,
                content = notification.content,
                isGroup = notification.isGroup
            )
        }
    }

    // 当前弹出的模态底栏状态 (首页)
    var activeSheet by remember { mutableStateOf<HomeSheetType?>(null) }
    // 个人资料与设置模态底栏状态 (我的)
    var activeProfileSheet by remember { mutableStateOf<ProfileSheetType?>(null) }

    // 登录表单状态（提升到顶层，供 Sheet 顶部「完成」按钮消费；工程内不保留任何默认账号）
    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // 消息列表与当前活跃单聊会话 (消息) - 直接观察 ZeroTalkClientManager 真实会话数据流
    val conversations by ZeroTalkClientManager.conversations.collectAsState()
    var activeChatConversation by remember { mutableStateOf<ConversationItem?>(null) }

    // 同步「正在查看的会话」：进入聊天页设置、退出清空（供横幅抑制判定）
    LaunchedEffect(activeChatConversation?.id) {
        ZeroTalkClientManager.setViewingRoom(activeChatConversation?.id)
    }

    // 系统通知点击：等会话列表就绪后直达对应会话
    val pendingOpenRoomId by ZeroTalkClientManager.pendingOpenRoomId.collectAsState()
    LaunchedEffect(pendingOpenRoomId, conversations) {
        val roomId = pendingOpenRoomId ?: return@LaunchedEffect
        val conv = conversations.find { it.id == roomId } ?: return@LaunchedEffect
        ZeroTalkClientManager.markConversationRead(conv.id)
        activeChatConversation = conv.copy(unreadCount = 0)
        ZeroTalkClientManager.consumePendingOpenRoom()
    }

    // 语音通话状态（全局覆盖层：任何页面都能浮出来电/去电/通话中）
    val voiceUiState by ZeroTalkClientManager.voiceCall.uiState.collectAsState()
    val voiceSession by ZeroTalkClientManager.voiceCall.session.collectAsState()
    val voiceMuted by ZeroTalkClientManager.voiceCall.muted.collectAsState()
    val voiceSpeakerOn by ZeroTalkClientManager.voiceCall.speakerOn.collectAsState()

    // 通话过程提示 + 被限制时把匹配模式切回「聊天」（对齐官网 voice_call_banned 行为）
    LaunchedEffect(Unit) {
        ZeroTalkClientManager.voiceCall.events.collect { event ->
            when (event) {
                is VoiceCallEvent.Toast -> notificationState.show(event.message)
                VoiceCallEvent.Banned -> {
                    notificationState.show("你的语音通话功能已被限制")
                    ZeroTalkClientManager.notifyVoiceCallBanned()
                }
            }
        }
    }
    val voiceBannedTick by ZeroTalkClientManager.voiceBannedTick.collectAsState()

    // 语音匹配自动接通时收起匹配进度面板：ModalBottomSheet 是独立窗口，会盖住通话覆盖层
    LaunchedEffect(voiceSession?.callId) {
        if (voiceSession != null && activeSheet is HomeSheetType.MatchingInProgress) {
            activeSheet = null
        }
    }

    // 资料页层级栈的两个渲染点：APP（动态作者 / 捞取记录）与 HALL（大厅点发送者）
    // 层级栈是全站唯一事实来源，这里只认领属于自己宿主的栈顶那一层
    val appProfileSlot = rememberUserProfileLayerSlot(UserProfileLayerHost.APP)
    val hallProfileSlot = rememberUserProfileLayerSlot(UserProfileLayerHost.HALL)

    // 资料页是主窗口内的覆盖层，而 ModalBottomSheet 是独立窗口、永远盖在它之上。
    // 因此从 Sheet 里的动态卡片点头像打开资料页时，必须先把 Sheet 收起来，否则资料页会被压在下面看不见。
    // （APP 层是唯一可能从 Sheet 内部打开的宿主：捞动态 / 捞取记录的动态卡片都在 Sheet 里）
    LaunchedEffect(appProfileSlot.visible) {
        if (appProfileSlot.visible) {
            activeSheet = null
        }
    }

    // 切换到消息 Tab 时自动触发真实房间列表刷新
    LaunchedEffect(selectedTab) {
        if (selectedTab == ZeroTalkTab.MESSAGES) {
            ZeroTalkClientManager.fetchRoomList()
        }
    }

    // 实时监听对端真实成员信息或会话更新，秒级更新单聊标题与状态
    LaunchedEffect(roomPeers, conversations) {
        activeChatConversation?.let { active ->
            val updated = conversations.find { it.id == active.id }
            if (updated != null && (updated.targetName != active.targetName || updated.targetAvatar != active.targetAvatar || updated.tag != active.tag || updated.isOnline != active.isOnline)) {
                activeChatConversation = active.copy(
                    targetName = updated.targetName,
                    targetAvatar = updated.targetAvatar,
                    isOnline = updated.isOnline,
                    tag = updated.tag
                )
            } else {
                val peer = roomPeers[active.id]
                if (peer != null) {
                    val shouldUpdate = active.targetName == "神秘零友" || active.targetName == "神秘人" || active.targetName == "对方" || active.targetName.startsWith("匹配房间-")
                    if (shouldUpdate && peer.username.isNotBlank()) {
                        val isOnline = if (active.category == MessageCategory.CODE) null else (active.isOnline ?: true)
                        activeChatConversation = active.copy(
                            targetName = peer.username,
                            targetAvatar = peer.avatarUrl ?: active.targetAvatar,
                            isOnline = isOnline,
                            tag = resolveConversationStatusTag(active.category)
                        )
                    }
                }
            }
        }
    }

    // 匹配偏好设置状态（支持保存与脏检查）
    var savedMatchingPreferences by remember { mutableStateOf(MatchingPreferences()) }
    var currentMatchingPreferences by remember { mutableStateOf(savedMatchingPreferences) }

    LaunchedEffect(activeSheet) {
        if (activeSheet is HomeSheetType.MatchingSettings) {
            currentMatchingPreferences = savedMatchingPreferences
        }
    }
    LaunchedEffect(activeProfileSheet) {
        if (activeProfileSheet is ProfileSheetType.EditProfile) {
            editingProfile = userProfile
        }
    }

    // 纯色背景方案 (遵循 Apple HIG Inset Grouped 底色规范)：
    // 亮色：systemGroupedBackground (#F2F2F7)
    // 暗色：systemGroupedBackground (#000000)
    val backgroundColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7),
        animationSpec = tween(250)
    )
    val subTextColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)

    // 创建 NavBar 和 Sheet 需要的 Backdrop（折射整个主界面）
    val mainContentBackdrop = rememberLayerBackdrop()
    // 创建卡片内部组件需要的 Backdrop（仅折射最底层的背景，避免循环依赖引发 RenderThread 崩溃）
    val backgroundBackdrop = rememberLayerBackdrop()

    AppleHigTheme(darkTheme = isDark) {
        NotificationProvider(state = notificationState) {
            ImageViewerProvider {
                // ---- 1. 优先关闭展开的底部模态弹窗（Bottom Sheet） ----
            // 「捞动态 / 捞取历史」的内容自己管理返回（内部子页面优先），无子页面时回调 onRequestClose 关闭 Sheet；
            // 其余 Sheet 没有内容级返回处理，由这里直接关闭。两侧互为显式门控，不依赖「后注册优先」的组合顺序
            val sheetContentHandlesBack = activeSheet is HomeSheetType.CatchMoments ||
                activeSheet is HomeSheetType.CatchHistory
            BackHandler(enabled = activeSheet != null && !sheetContentHandlesBack) {
                if (activeSheet is HomeSheetType.MatchingSettings) {
                    currentMatchingPreferences = savedMatchingPreferences
                }
                activeSheet = null
            }

            BackHandler(enabled = activeProfileSheet != null) {
                if (activeProfileSheet is ProfileSheetType.EditProfile) {
                    editingProfile = userProfile
                }
                activeProfileSheet = null
            }

            // ---- 2. 根页面层级拦截：主 Tab 单步回退与双击退出软件 ----
            // 资料页层级栈非空时返回事件属于栈顶那一层，根页面不参与（覆盖 APP / HALL / PAGE / CHAT 四类宿主）
            val hasProfileLayers = rememberHasUserProfileLayers()
            val isAtRoot = activeSheet == null &&
                activeProfileSheet == null &&
                !inChatroom &&
                activeChatConversation == null &&
                !hasProfileLayers

            BackHandler(enabled = isAtRoot) {
                if (previousTab != null && previousTab != selectedTab) {
                    // 回退到切换前的前一个 Tab 根页面，回退后清空记录（仅保留一次前驱记忆）
                    selectedTab = previousTab!!
                    previousTab = null
                } else {
                    // 已经在根页面，执行双击退出逻辑
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000L) {
                        platformActions.exitApp()
                    } else {
                        lastBackPressTime = now
                        platformActions.showToast("再按一次退出应用")
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
            // 主界面（被 mainContentBackdrop 采样，供 NavBar 和 BottomSheet 折射）
            Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(mainContentBackdrop)
        ) {
            // 最底层的纯色背景（被 backgroundBackdrop 采样，供页面内的 Liquid 组件折射）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .layerBackdrop(backgroundBackdrop)
            )

            // 页面内容
            when (selectedTab) {
                ZeroTalkTab.HOME -> {
                    HomeScreen(
                        backdrop = backgroundBackdrop,
                        onStartMatch = { isVoice ->
                            val opposite = savedMatchingPreferences.genderIndex == 1
                            val same = savedMatchingPreferences.genderIndex == 2
                            ZeroTalkClientManager.startMatching(
                                isVoice = isVoice,
                                oppositeGenderOnly = opposite,
                                sameGenderOnly = same
                            )
                            activeSheet = HomeSheetType.MatchingInProgress(isVoice)
                        },
                        onOpenMatchingSettings = {
                            activeSheet = HomeSheetType.MatchingSettings
                        },
                        onEnterChatroom = {
                            inChatroom = true
                        },
                        onOpenCreateRoom = {
                            activeSheet = HomeSheetType.CreateRoom
                        },
                        onOpenJoinRoom = {
                            activeSheet = HomeSheetType.JoinRoom
                        },
                        onOpenCatchMoments = {
                            activeSheet = HomeSheetType.CatchMoments
                        },
                        onOpenCatchHistory = {
                            activeSheet = HomeSheetType.CatchHistory
                        },
                        onOpenMbtiTest = {
                            activeProfileSheet = MbtiTest
                        },
                        isDark = isDark
                    )
                }
                ZeroTalkTab.PROFILE -> {
                    ProfileScreen(
                        profile = userProfile,
                        themeMode = themeMode,
                        onThemeModeChange = { themeMode = it },
                        liquidGlassCompat = liquidGlassCompat,
                        onLiquidGlassCompatChange = {
                            liquidGlassCompat = it
                            UiPreferencesStore.setLiquidGlassCompatMode(it)
                        },
                        backgroundReceiveEnabled = backgroundReceiveEnabled,
                        onBackgroundReceiveChange = { UiPreferencesStore.setBackgroundReceiveEnabled(it) },
                        onRefreshAvatar = {
                            notificationState.show("正在刷新头像...")
                            ZeroTalkClientManager.refreshAvatar { success, msg ->
                                notificationState.show(msg ?: if (success) "头像已刷新" else "头像刷新失败")
                            }
                        },
                        onEditProfile = {
                            editingProfile = userProfile
                            activeProfileSheet = ProfileSheetType.EditProfile
                        },
                        onLogout = {
                            activeProfileSheet = ProfileSheetType.LogoutConfirm
                        },
                        onLogin = {
                            // 打开登录表单，账号密码由用户输入
                            loginUsername = ""
                            loginPassword = ""
                            loginError = null
                            isLoggingIn = false
                            activeProfileSheet = ProfileSheetType.Login
                        },
                        onOpenSetting = { item ->
                            if (item == ProfileSettingItem.PRIVACY_SETTINGS) {
                                privacySettings = ZeroTalkClientManager.privacySettings.value
                            }
                            activeProfileSheet = ProfileSheetType.SettingDetail(item)
                        },
                        onOpenNotificationCenter = {
                            activeProfileSheet = ProfileNotificationCenter
                        },
                        onDeleteAccount = {
                            activeProfileSheet = ProfileSheetType.DeleteAccountConfirm
                        },
                        backdrop = backgroundBackdrop,
                        isDark = isDark
                    )
                }
                ZeroTalkTab.MESSAGES -> {
                    MessagesScreen(
                        conversations = conversations,
                        onConversationClick = { item ->
                            ZeroTalkClientManager.markConversationRead(item.id)
                            activeChatConversation = item.copy(unreadCount = 0)
                        },
                        backdrop = backgroundBackdrop,
                        isDark = isDark
                    )
                }
                ZeroTalkTab.DYNAMIC -> {
                    MomentsScreen(
                        backdrop = backgroundBackdrop,
                        sheetBackdrop = mainContentBackdrop,
                        isDark = isDark
                    )
                }
            }
        }

        // 底部液态玻璃导航栏（266dp 紧凑胶囊，处于最外层，折射 mainContentBackdrop）
        ZeroTalkNavBar(
            selectedTab = selectedTab,
            onTabSelected = { newTab ->
                if (newTab != selectedTab) {
                    previousTab = selectedTab
                    selectedTab = newTab
                }
            },
            backdrop = mainContentBackdrop,
            isDark = isDark,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )

        // 全屏公共聊天室（零语大厅）
        AnimatedVisibility(
            visible = inChatroom,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            PublicChatroomScreen(
                onBack = {
                    inChatroom = false
                    // 退出大厅时收掉大厅打开的资料页层，避免层级残留
                    ZeroTalkClientManager.closeUserProfileLayers(UserProfileLayerHost.HALL)
                },
                isDark = isDark,
                onOpenUserProfile = { target ->
                    ZeroTalkClientManager.openUserProfileLayer(target, UserProfileLayerHost.HALL)
                }
            )
        }

        // 大厅点发送者 → 用户资料面板（层级栈中的 HALL 层；大厅本身没有群聊面板，仅此跳转入口）
        AnimatedVisibility(
            visible = hallProfileSlot.visible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            hallProfileSlot.layer?.let { layer ->
                OtherUserProfileScreen(
                    target = layer.target,
                    isDark = isDark,
                    backdrop = mainContentBackdrop,
                    isTopLayer = hallProfileSlot.isTop,
                    onBack = { ZeroTalkClientManager.popUserProfileLayer() },
                    // 资料面板内发起私信：先收起面板，再切到私聊会话
                    onOpenConversation = { target ->
                        ZeroTalkClientManager.closeUserProfileLayers(UserProfileLayerHost.HALL)
                        activeChatConversation = target
                    }
                )
            }
        }

        // 全屏单聊页面（iMessage 风格私聊）
        AnimatedVisibility(
            visible = activeChatConversation != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            activeChatConversation?.let { conversation ->
                PrivateChatScreen(
                    conversation = conversation,
                    onBack = { activeChatConversation = null },
                    backdrop = mainContentBackdrop,
                    isDark = isDark,
                    onOpenConversation = { activeChatConversation = it }
                )
            }
        }

        // Apple HIG 模态底部弹窗 (Modal Bottom Sheet)
        val currentSheet = activeSheet
        if (currentSheet != null) {
            val isHistory = currentSheet is HomeSheetType.CatchHistory
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = !isHistory)

            val sheetTitle = when (currentSheet) {
                is HomeSheetType.MatchingSettings -> "匹配偏好设置"
                is HomeSheetType.CatchMoments -> "捞动态"
                is HomeSheetType.CatchHistory -> "捞取历史记录"
                is HomeSheetType.MatchingInProgress -> null
                is HomeSheetType.CreateRoom -> "创建房间"
                is HomeSheetType.JoinRoom -> "加入房间"
            }

            val hasSettingsChanges = currentMatchingPreferences != savedMatchingPreferences

            val leadingAction = when (currentSheet) {
                is HomeSheetType.MatchingSettings -> SheetAction.Cancel {
                    currentMatchingPreferences = savedMatchingPreferences
                    activeSheet = null
                }
                is HomeSheetType.CatchMoments,
                is HomeSheetType.CatchHistory,
                is HomeSheetType.MatchingInProgress,
                is HomeSheetType.CreateRoom,
                is HomeSheetType.JoinRoom -> SheetAction.Close {
                    activeSheet = null
                }
            }

            val trailingAction = when (currentSheet) {
                is HomeSheetType.MatchingSettings -> SheetAction.Done(
                    enabled = hasSettingsChanges,
                    onClick = {
                        savedMatchingPreferences = currentMatchingPreferences
                        activeSheet = null
                        val ageRangeStr = if (currentMatchingPreferences.ageIndex == 0) "18-23" else "23以上"
                        val opposite = currentMatchingPreferences.genderIndex == 1
                        val same = currentMatchingPreferences.genderIndex == 2
                        // 回填本地快照：首聊卡片的「清流模式」开关需要携带其余字段一起提交
                        ZeroTalkClientManager.syncMyMatchPreferences(
                            ageRange = ageRangeStr,
                            oppositeGenderOnly = opposite,
                            sameGenderOnly = same,
                            cleanStreamMode = currentMatchingPreferences.cleanStreamMode
                        )
                        coroutineScope.launch {
                            ZeroTalkClientManager.apiService.updateMatchSettings(
                                ageRange = ageRangeStr,
                                matchOppositeGenderOnly = if (opposite) 1 else 0,
                                matchSameGenderOnly = if (same) 1 else 0,
                                cleanStreamMode = if (currentMatchingPreferences.cleanStreamMode) 1 else 0
                            )
                        }
                    }
                )
                else -> SheetAction.None
            }

            AppleModalBottomSheet(
                onDismissRequest = {
                    if (currentSheet is HomeSheetType.MatchingSettings) {
                        currentMatchingPreferences = savedMatchingPreferences
                    }
                    activeSheet = null
                },
                backdrop = mainContentBackdrop,
                title = sheetTitle,
                leadingAction = leadingAction,
                trailingAction = trailingAction,
                sheetState = sheetState,
                isDark = isDark
            ) {
                when (currentSheet) {
                    is HomeSheetType.MatchingSettings -> {
                        SheetMatchingSettingsContent(
                            preferences = currentMatchingPreferences,
                            onPreferencesChange = { currentMatchingPreferences = it },
                            isDark = isDark
                        )
                    }
                    is HomeSheetType.MatchingInProgress -> {
                        SheetMatchingInProgressContent(
                            isVoice = currentSheet.isVoice,
                            isDark = isDark,
                            onCancel = {
                                ZeroTalkClientManager.cancelMatching()
                                activeSheet = null
                            },
                            onMatchSuccess = { roomId, partner ->
                                activeSheet = null
                                val effectivePartner = partner ?: ZeroTalkClientManager.roomPeers.value[roomId]
                                val partnerName = effectivePartner?.username?.ifBlank { null } ?: "神秘零友"
                                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                val matchedConv = ConversationItem(
                                    id = roomId,
                                    targetName = partnerName,
                                    targetAvatar = effectivePartner?.avatarUrl,
                                    lastMessage = "匹配成功，开始聊天吧！",
                                    timestamp = nowTime,
                                    category = MessageCategory.MATCH,
                                    isOnline = true,
                                    tag = resolveConversationStatusTag(MessageCategory.MATCH)
                                )
                                ZeroTalkClientManager.upsertConversation(matchedConv)
                                activeChatConversation = matchedConv
                            }
                        )
                    }
                    is HomeSheetType.CatchMoments -> {
                        SheetCatchMomentsContent(
                            isDark = isDark,
                            backdrop = mainContentBackdrop,
                            onRequestClose = { activeSheet = null }
                        )
                    }
                    is HomeSheetType.CatchHistory -> {
                        SheetCatchHistoryContent(
                            isDark = isDark,
                            onRequestClose = { activeSheet = null }
                        )
                    }
                    is HomeSheetType.CreateRoom -> {
                        SheetCreateRoomContent(
                            isDark = isDark,
                            backdrop = mainContentBackdrop,
                            onCreateSuccess = { roomData ->
                                activeSheet = null
                                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                val createdConv = ConversationItem(
                                    id = roomData.roomId,
                                    targetName = roomData.roomName,
                                    targetAvatar = null,
                                    lastMessage = "暗号房间已创建",
                                    timestamp = nowTime,
                                    category = MessageCategory.CODE,
                                    tag = "暗号"
                                )
                                activeChatConversation = createdConv
                            }
                        )
                    }
                    is HomeSheetType.JoinRoom -> {
                        SheetJoinRoomContent(
                            isDark = isDark,
                            backdrop = mainContentBackdrop,
                            onJoinSuccess = { roomId, roomName ->
                                activeSheet = null
                                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                val joinedConv = ConversationItem(
                                    id = roomId,
                                    targetName = roomName,
                                    targetAvatar = null,
                                    lastMessage = "已加入暗号房间",
                                    timestamp = nowTime,
                                    category = MessageCategory.CODE,
                                    tag = "暗号"
                                )
                                activeChatConversation = joinedConv
                            }
                        )
                    }
                }
            }
        }

        // 资料页层级栈的 APP 层（动态作者 / 捞取记录），全屏覆盖在底栏弹窗上方
        AnimatedVisibility(
            visible = appProfileSlot.visible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            appProfileSlot.layer?.let { layer ->
                OtherUserProfileScreen(
                    target = layer.target,
                    isDark = isDark,
                    backdrop = mainContentBackdrop,
                    isTopLayer = appProfileSlot.isTop,
                    onBack = { ZeroTalkClientManager.popUserProfileLayer() },
                    onOpenConversation = { conv ->
                        ZeroTalkClientManager.clearUserProfileLayers()
                        activeSheet = null
                        activeChatConversation = conv
                    }
                )
            }
        }

        // 个人资料与设置模态底栏 (Profile & Settings Sheets)
        val currentProfileSheet = activeProfileSheet
        if (currentProfileSheet != null) {
            // 安全中心内容较长，默认半屏（PartiallyExpanded）展开、可上滑拖至全屏 —— 与「捞取历史记录」一致；
            // 其余资料 / 设置类 Sheet 内容较短，仍直接全屏展开
            val isSecurityCenter = currentProfileSheet is ProfileSheetType.SettingDetail &&
                currentProfileSheet.item == ProfileSettingItem.SECURITY_CENTER
            val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = !isSecurityCenter)

            val pTitle = when (currentProfileSheet) {
                is ProfileSheetType.Login -> "登录账号"
                is ProfileSheetType.EditProfile -> "修改资料"
                is ProfileSheetType.LogoutConfirm -> "退出登录"
                is ProfileSheetType.DeleteAccountConfirm -> "注销账号"
                is ProfileSheetType.SettingDetail -> currentProfileSheet.item.title
                is MbtiTest -> "MBTI 人格测试"
                is ProfileNotificationCenter -> "通知中心"
            }

            val pLeadingAction = when (currentProfileSheet) {
                is ProfileSheetType.EditProfile,
                is ProfileSheetType.Login,
                is ProfileSheetType.LogoutConfirm,
                is ProfileSheetType.DeleteAccountConfirm -> SheetAction.Cancel {
                    if (currentProfileSheet is ProfileSheetType.EditProfile) {
                        editingProfile = userProfile
                    }
                    if (currentProfileSheet is ProfileSheetType.Login) {
                        // 取消时丢弃已输入的账号密码与错误提示
                        loginError = null
                        isLoggingIn = false
                    }
                    activeProfileSheet = null
                }
                is ProfileSheetType.SettingDetail -> SheetAction.Close {
                    activeProfileSheet = null
                }
                is MbtiTest -> SheetAction.Close {
                    activeProfileSheet = null
                }
                is ProfileNotificationCenter -> SheetAction.Close {
                    activeProfileSheet = null
                }
            }

            val isProfileDirty = editingProfile != userProfile && editingProfile.name.isNotBlank()

            val pTrailingAction = when (currentProfileSheet) {
                is ProfileSheetType.Login -> SheetAction.Done(
                    enabled = loginUsername.isNotBlank() && loginPassword.isNotBlank() && !isLoggingIn,
                    onClick = {
                        loginError = null
                        isLoggingIn = true
                        ZeroTalkClientManager.ensureConnected(
                            username = loginUsername.trim(),
                            password = loginPassword,
                            onSuccess = {
                                isLoggingIn = false
                                loginPassword = ""
                                activeProfileSheet = null
                                notificationState.show("登录成功，已连接零语云服务")
                            },
                            onError = { err ->
                                isLoggingIn = false
                                loginError = err
                            }
                        )
                    }
                )
                is ProfileSheetType.EditProfile -> SheetAction.Done(
                    enabled = isProfileDirty,
                    onClick = {
                        ZeroTalkClientManager.updateUserProfile(
                            username = editingProfile.name,
                            bio = editingProfile.bio,
                            ageRange = editingProfile.ageRange,
                            gender = editingProfile.gender,
                            patText = editingProfile.patText,
                            qq = editingProfile.qq,
                            onResult = { success, err ->
                                if (success) {
                                    if (editingProfile.avatarUrl != userProfile.avatarUrl && editingProfile.avatarUrl.isNotBlank()) {
                                        ZeroTalkClientManager.commitAvatar(editingProfile.avatarUrl)
                                    }
                                    if (editingProfile.qq != userProfile.qq && editingProfile.qq.isNotBlank()) {
                                        ZeroTalkClientManager.refreshAvatar()
                                    }
                                    userProfile = editingProfile
                                    activeProfileSheet = null
                                    notificationState.show("资料已成功同步保存")
                                } else {
                                    notificationState.show("保存失败: $err")
                                }
                            }
                        )
                    }
                )
                is ProfileSheetType.LogoutConfirm -> SheetAction.Done(
                    enabled = true,
                    onClick = {
                        ZeroTalkClientManager.logout()
                        // 退出登录时清空通知中心状态，避免串号
                        NotificationCenterStore.clear()
                        userProfile = UserProfile.NO_ACCOUNT
                        activeProfileSheet = null
                        notificationState.show("已退出登录")
                    }
                )
                is ProfileSheetType.DeleteAccountConfirm -> SheetAction.Done(
                    enabled = true,
                    onClick = {
                        activeProfileSheet = null
                        notificationState.show("账号已注销")
                    }
                )
                is ProfileSheetType.SettingDetail -> {
                    if (currentProfileSheet.item == ProfileSettingItem.PRIVACY_SETTINGS) {
                        SheetAction.Done(
                            enabled = true,
                            onClick = {
                                ZeroTalkClientManager.updateMomentsPrivacy(privacySettings) { success, err ->
                                    if (success) {
                                        activeProfileSheet = null
                                        notificationState.show("隐私配置已保存")
                                    } else {
                                        notificationState.show("保存失败: $err")
                                    }
                                }
                            }
                        )
                    } else {
                        // 安全中心、查找用户、我的黑名单、我的举报、问题反馈、联系我们、捐赠本站、修改密码、处罚减免、网易云绑定等，
                        // 右上角均不需要 确认[√] 按钮（左上角已提供关闭[×]按钮，表单类由卡片内专用操作触发）
                        SheetAction.None
                    }
                }
                // MBTI 测评：操作按钮在内容里（重新测试 / 清除本次测试），右上角无需确认按钮
                is MbtiTest -> SheetAction.None
                // 通知中心：顶部工具栏自带「全部已读」，右上角无需确认按钮
                is ProfileNotificationCenter -> SheetAction.None
            }

            AppleModalBottomSheet(
                onDismissRequest = {
                    if (currentProfileSheet is ProfileSheetType.EditProfile) {
                        editingProfile = userProfile
                    }
                    if (currentProfileSheet is ProfileSheetType.Login) {
                        loginError = null
                        isLoggingIn = false
                    }
                    activeProfileSheet = null
                },
                backdrop = mainContentBackdrop,
                title = pTitle,
                leadingAction = pLeadingAction,
                trailingAction = pTrailingAction,
                sheetState = profileSheetState,
                isDark = isDark
            ) {
                when (currentProfileSheet) {
                    is ProfileSheetType.Login -> {
                        SheetLoginContent(
                            username = loginUsername,
                            password = loginPassword,
                            isSubmitting = isLoggingIn,
                            errorMessage = loginError,
                            onUsernameChange = { loginUsername = it },
                            onPasswordChange = { loginPassword = it },
                            isDark = isDark
                        )
                    }
                    is ProfileSheetType.EditProfile -> {
                        SheetEditProfileContent(
                            profile = editingProfile,
                            onProfileChange = { editingProfile = it },
                            isDark = isDark
                        )
                    }
                    is ProfileSheetType.LogoutConfirm -> {
                        SheetLogoutConfirmContent(
                            profile = userProfile,
                            isDark = isDark
                        )
                    }
                    is ProfileSheetType.DeleteAccountConfirm -> {
                        SheetDeleteAccountConfirmContent(
                            profile = userProfile,
                            isDark = isDark
                        )
                    }
                    is ProfileSheetType.SettingDetail -> {
                        when (currentProfileSheet.item) {
                            ProfileSettingItem.PRIVACY_SETTINGS -> {
                                SheetPrivacySettingsContent(
                                    privacy = privacySettings,
                                    onPrivacyChange = { privacySettings = it },
                                    isDark = isDark
                                )
                            }
                            ProfileSettingItem.SEARCH_USERS -> {
                                SheetSearchUsersContent(
                                    isDark = isDark,
                                    onStartChat = { peerId, peerName, peerAvatar ->
                                        activeProfileSheet = null
                                        val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                        activeChatConversation = ConversationItem(
                                            id = "dm_$peerId",
                                            targetName = peerName,
                                            targetAvatar = peerAvatar,
                                            lastMessage = "你好！很高兴认识你",
                                            timestamp = nowTime,
                                            category = MessageCategory.PRIVATE,
                                            isOnline = null,
                                            tag = null
                                        )
                                    },
                                    onShowMessage = { notificationState.show(it) }
                                )
                            }
                            ProfileSettingItem.MY_BLOCKLIST -> {
                                SheetBlockListContent(
                                    isDark = isDark,
                                    onShowMessage = { notificationState.show(it) }
                                )
                            }
                            ProfileSettingItem.MY_REPORTS -> {
                                SheetMyReportsContent(
                                    isDark = isDark
                                )
                            }
                            // 服务与支持：问题反馈（提交表单 + 我的反馈记录）
                            ProfileSettingItem.FEEDBACK -> {
                                SheetFeedbackContent(
                                    isDark = isDark,
                                    onShowMessage = { notificationState.show(it) },
                                    // 「App反馈」加入暗语群聊成功后：关闭资料页并直接进入该群聊
                                    onOpenRoom = { roomId, roomName ->
                                        activeProfileSheet = null
                                        val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                        activeChatConversation = ConversationItem(
                                            id = roomId,
                                            targetName = roomName,
                                            targetAvatar = null,
                                            lastMessage = "已加入暗号房间",
                                            timestamp = nowTime,
                                            category = MessageCategory.CODE,
                                            isOnline = null,
                                            tag = "暗号"
                                        )
                                    }
                                )
                            }
                            // 服务与支持：联系我们（群二维码 + 联系方式，点击复制）
                            ProfileSettingItem.CONTACT_US -> {
                                SheetContactUsContent(
                                    isDark = isDark,
                                    onShowMessage = { notificationState.show(it) }
                                )
                            }
                            // 服务与支持：捐赠本站（本站双码 + 备注分享码 + 捐赠名单）与捐献 App
                            ProfileSettingItem.DONATION -> {
                                SheetDonateContent(
                                    isDark = isDark,
                                    onShowMessage = { notificationState.show(it) },
                                    onOpenDonorProfile = { donor ->
                                        // 先关面板再打开全局资料层，避免资料页被 Sheet 遮挡
                                        activeProfileSheet = null
                                        ZeroTalkClientManager.openUserProfileLayer(
                                            target = UserProfileTarget(
                                                userId = donor.userId.takeIf { it > 0 }?.toString().orEmpty(),
                                                uid = donor.uid,
                                                name = donor.displayName,
                                                avatarUrl = donor.avatarUrl.orEmpty()
                                            ),
                                            host = UserProfileLayerHost.APP
                                        )
                                    }
                                )
                            }
                            ProfileSettingItem.SECURITY_CENTER -> {
                                SheetSecurityCenterContent(
                                    isDark = isDark,
                                    onShowMessage = { notificationState.show(it) },
                                    onLoggedOut = {
                                        userProfile = UserProfile.NO_ACCOUNT
                                        activeProfileSheet = null
                                    }
                                )
                            }
                            ProfileSettingItem.CHANGE_PASSWORD -> {
                                SheetChangePasswordContent(
                                    isDark = isDark,
                                    onShowMessage = { notificationState.show(it) }
                                )
                            }
                            ProfileSettingItem.PENALTY_RELIEF -> {
                                SheetPenaltyReliefContent(
                                    isDark = isDark,
                                    onShowMessage = { notificationState.show(it) }
                                )
                            }
                            ProfileSettingItem.NETEASE_BIND -> {
                                SheetNeteaseBindContent(
                                    isDark = isDark,
                                    onShowMessage = { notificationState.show(it) }
                                )
                            }
                            else -> {
                                SheetSettingDetailContent(
                                    item = currentProfileSheet.item,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                    is MbtiTest -> {
                        SheetMbtiTestContent(
                            isDark = isDark,
                            onShowMessage = { notificationState.show(it) }
                        )
                    }
                    is ProfileNotificationCenter -> {
                        SheetNotificationCenterContent(
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // 顶部居中展开式全局动态通知胶囊 (Apple Dynamic Island / Pill 风格)
        ZeroTalkNotificationHost(
            state = notificationState,
            isDark = isDark
        )

        // 语音通话全屏覆盖层（最顶层：任何页面都能浮出来电/去电/通话中）
        VoiceCallOverlay(
            uiState = voiceUiState,
            session = voiceSession,
            muted = voiceMuted,
            speakerOn = voiceSpeakerOn,
            onAccept = { ZeroTalkClientManager.voiceCall.accept() },
            onReject = { ZeroTalkClientManager.voiceCall.reject() },
            onCancel = { ZeroTalkClientManager.voiceCall.cancel() },
            onHangup = { ZeroTalkClientManager.voiceCall.hangup() },
            onToggleMute = { ZeroTalkClientManager.voiceCall.toggleMute() },
            onToggleSpeaker = { ZeroTalkClientManager.voiceCall.toggleSpeaker() }
        )
    }
    }
    }
    }
}
