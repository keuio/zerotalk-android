package top.lanxint.zerotalk.ui.messages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.ConversationItem
import top.lanxint.zerotalk.data.model.FollowListKind
import top.lanxint.zerotalk.data.model.FollowUserItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.lanxint.zerotalk.data.model.MessageCategory
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.data.model.OtherUserFollowListState
import top.lanxint.zerotalk.data.model.OtherUserProfile
import top.lanxint.zerotalk.data.model.OtherUserProfileState
import top.lanxint.zerotalk.data.model.UserProfileLayerHost
import top.lanxint.zerotalk.data.model.UserProfileTarget
import top.lanxint.zerotalk.data.network.MomentsSort
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.AppleModalBottomSheet
import top.lanxint.zerotalk.ui.components.SheetAction
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.components.MbtiCard
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
import top.lanxint.zerotalk.ui.components.FollowCapsuleButton
import top.lanxint.zerotalk.ui.components.IosLiquidBackButton
import top.lanxint.zerotalk.ui.components.LiquidSegmentedControl
import top.lanxint.zerotalk.ui.components.LiquidToggle
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.moments.MomentCard
import top.lanxint.zerotalk.ui.moments.MomentSheetsHost
import top.lanxint.zerotalk.ui.moments.MomentsSortCapsule
import top.lanxint.zerotalk.ui.navigation.rememberUserProfileLayerSlot
import top.lanxint.zerotalk.ui.utils.BackHandler
import top.lanxint.zerotalk.ui.utils.rememberPhotoPickerLauncher
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.drawPlainBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.colorControls
import kotlinx.coroutines.launch

/**
 * 他人主页（对话资料详情页，iOS iMessage Conversation Info 形态）
 *
 * 数据全部来自实测接口，不再使用任何写死的演示数据：
 * - 聚合资料 GET /api/moment/user：昵称/性别/年龄段/地区/头像/个性签名 + 获赞/关注/粉丝计数
 *   + 关系状态（是否关注、是否互关、是否拉黑、能否私信）+ 对方动态列表（分页）；
 * - 关注/粉丝列表 GET /api/follow/list?kind=following|followers；
 * - 关注切换 /follow/add、/follow/remove；拉黑 /block/add、/block/unblock（拉黑自动取消关注）；
 * - 私信 /room/dm/create-from-user（返回或复用私聊房间）。
 *
 * 个性签名与动态区在接口失败时给出可重试的错误卡片，而不是停留在「暂时无法获取用户资料」。
 *
 * @param onOpenConversation 私信落地到另一个房间时通知外层切换会话
 * @param profileOnly 纯资料面板模式：群聊 / 群聊面板点成员打开时使用，隐藏「背景」等会话专属设置
 * @param backEnabled 本实例是否允许拦截返回：作为资料页层级栈中的一层时，只有栈顶那一层传 true
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationInfoScreen(
    conversation: ConversationItem,
    onBack: () -> Unit,
    backdrop: Backdrop? = null,
    isDark: Boolean,
    currentWallpaperKey: String? = null,
    /** 背景已写入管理器后回调（仅做本地状态刷新 / 系统提示，不再重复落库） */
    onWallpaperApplied: (String?) -> Unit = {},
    onOpenConversation: ((ConversationItem) -> Unit)? = null,
    profileOnly: Boolean = false,
    backEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current

    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isDark) Color(0xFF12141A) else Color(0xFFF4F2F9))
    }
    val actualBackdrop = backdrop ?: fallbackBackdrop

    // 详情页专属全屏磨砂玻璃背景 Backdrop（供顶部返回按键采样）
    val infoScreenBackdrop = rememberLayerBackdrop()

    val globalProfileState by ZeroTalkClientManager.otherUserProfile.collectAsState()
    val globalFollowListState by ZeroTalkClientManager.otherUserFollowList.collectAsState()
    val roomPeers by ZeroTalkClientManager.roomPeers.collectAsState()
    val roomBootstrapMap by ZeroTalkClientManager.roomBootstrapMap.collectAsState()
    // 主页动态排序（最新 / 最热），与动态主界面共用 MomentsSort
    val momentsSort by ZeroTalkClientManager.otherUserMomentsSort.collectAsState()

    // 会话项可能还没带上对端 uid（房间列表未刷新 / 匹配房刚建立），用 WS 房间成员资料兜底
    val requestUserId = conversation.targetUserId
    val requestUid = conversation.targetUid.ifBlank { roomPeers[conversation.id]?.uid.orEmpty() }

    // 局部状态快照：本页面的资料与关系列表独立持有，避免次级主页推入/弹出时由于全局单例覆写导致正在执行动效的旧视图数据闪烁
    var localProfileState by remember(conversation.id, requestUserId, requestUid) {
        mutableStateOf<OtherUserProfileState?>(null)
    }
    var localFollowListState by remember(conversation.id) {
        mutableStateOf<OtherUserFollowListState?>(null)
    }
    // 本页面实例在资料页层级栈中的身份：页面内下钻出来的每一层都归本实例渲染
    val pageLayerOwner = remember { Any() }
    val pageProfileSlot = rememberUserProfileLayerSlot(UserProfileLayerHost.PAGE, pageLayerOwner)
    // 本实例是否正压着一层下钻出来的次级用户主页（支持逐级下钻与回退）
    val isSubProfilePushed = pageProfileSlot.visible

    LaunchedEffect(globalProfileState, requestUserId, requestUid) {
        val current = globalProfileState ?: return@LaunchedEffect
        val matchesUser = (requestUserId.isNotBlank() && (current.userId == requestUserId || current.profile?.userId == requestUserId)) ||
            (requestUid.isNotBlank() && (current.uid == requestUid || current.profile?.uid == requestUid))
        if (matchesUser || localProfileState == null) {
            localProfileState = current
        }
    }

    LaunchedEffect(globalFollowListState, isSubProfilePushed) {
        if (!isSubProfilePushed) {
            localFollowListState = globalFollowListState
        }
    }

    val profileState = localProfileState ?: globalProfileState
    val followListState = if (isSubProfilePushed) (localFollowListState ?: globalFollowListState) else globalFollowListState

    // 分段标签索引 (0: 资料, 1: 背景)
    var selectedTab by remember { mutableStateOf(0) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    // 主页动态的评论抽屉 / 分享面板目标（复用动态主界面同一套 Sheet）
    var commentMomentTarget by remember { mutableStateOf<MomentItem?>(null) }
    var shareMomentTarget by remember { mutableStateOf<MomentItem?>(null) }
    // 举报动态目标与提交中标记（宿主在本页末尾）
    var reportTarget by remember { mutableStateOf<MomentItem?>(null) }
    var reportSubmitting by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(conversation.id, requestUserId, requestUid) {
        // 已经持有该用户的资料或正在加载时，不重复拉取，避免动态分页被重置
        val current = ZeroTalkClientManager.otherUserProfile.value
        val isCurrentTarget = (requestUserId.isNotBlank() && current?.userId == requestUserId) ||
            (requestUid.isNotBlank() && current?.uid == requestUid)
        val alreadyLoaded = current?.profile != null && isCurrentTarget
        val isCurrentlyLoading = current?.isLoading == true && isCurrentTarget
        if (!alreadyLoaded && !isCurrentlyLoading) {
            // 本页自身就是资料页目的地，只静默加载数据，不再请求全局资料页导航（否则会叠出第二层同一页面）
            ZeroTalkClientManager.loadOtherUserProfileSilently(requestUserId, requestUid)
        }
    }

    // 离开页面时收起本实例推入的下钻层与下级列表，避免下次进入残留
    DisposableEffect(conversation.id) {
        onDispose {
            // 本实例拥有的页面内下钻层随实例一起收掉，层级栈里不允许留下没有渲染点的孤儿层
            ZeroTalkClientManager.closeUserProfileLayers(UserProfileLayerHost.PAGE, pageLayerOwner)
            if (!profileOnly && !ZeroTalkClientManager.isPoppingProfile && !ZeroTalkClientManager.hasUserProfileLayers()) {
                ZeroTalkClientManager.closeOtherUserFollowList()
            }
        }
    }
    // 系统返回手势 / 物理返回键拦截（依次回退：评论/分享Sheet -> 拉黑弹窗 -> 关注粉丝列表 -> 退出当前主页）
    // backEnabled = false 表示本实例不是层级栈栈顶（已被更深一层覆盖），此时不得抢返回事件
    val hasSubOverlay = isSubProfilePushed || followListState != null || showBlockConfirm ||
        commentMomentTarget != null || shareMomentTarget != null

    // 1) 评论/分享抽屉拦截：ModalBottomSheet 是独立窗口、位于层级栈之上，因此不随 backEnabled 让位
    //    （与其承载者 MomentSheetsHost 的同类拦截互为兜底）
    BackHandler(enabled = commentMomentTarget != null || shareMomentTarget != null) {
        commentMomentTarget = null
        shareMomentTarget = null
    }

    // 2) 拉黑确认弹窗拦截
    BackHandler(enabled = backEnabled && showBlockConfirm) {
        showBlockConfirm = false
    }

    // 3) 关注 / 粉丝列表拦截：当没有更深层的次级主页时，关闭关注列表
    BackHandler(enabled = backEnabled && !isSubProfilePushed && followListState != null) {
        ZeroTalkClientManager.closeOtherUserFollowList()
        localFollowListState = null
    }

    // 4) 资料页自身拦截：当没有任一次级弹窗/列表/次级主页时，调用 onBack() 回退
    BackHandler(enabled = backEnabled && !hasSubOverlay) {
        onBack()
    }

    val profile = profileState?.profile
    val displayName = profile?.name?.takeIf { it.isNotBlank() } ?: conversation.targetName
    val displayAvatar = profile?.avatarUrl?.takeIf { it.isNotBlank() } ?: conversation.targetAvatar
    val isLoading = profileState?.isLoading == true && profile == null

    val opaqueBg = if (isDark) Color(0xFF12141A) else Color(0xFFF2F2F7)

    val parallaxOffset by animateFloatAsState(
        targetValue = if (isSubProfilePushed) -0.25f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "UnderlyingParallax"
    )
    val dimAlpha by animateFloatAsState(
        targetValue = if (isSubProfilePushed) 0.35f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "UnderlyingDim"
    )

    Box(
        modifier = modifier.fillMaxSize().background(opaqueBg)
    ) {
        // ---- 1. 当前页面（用户1及关系列表）：带有 GPU 硬件加速的 -25% 视差位移 ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = size.width * parallaxOffset
                }
        ) {
        // ---- 1. 详情页专属磨砂背景层 (录入 infoScreenBackdrop，供返回键等上层组件采样) ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawPlainBackdrop(
                    backdrop = actualBackdrop,
                    shape = { RectangleShape },
                    effects = {
                        colorControls(
                            brightness = if (!isDark) 0.12f else 0.04f,
                            saturation = 1.35f
                        )
                        blur(if (!isDark) 24.dp.toPx() else 20.dp.toPx())
                    }
                )
                .background(
                    if (isDark) Color(0xFF12141A).copy(alpha = 0.75f)
                    else Color(0xFFF2F2F7).copy(alpha = 0.72f)
                )
                .layerBackdrop(infoScreenBackdrop)
        )

        // ---- 2. 详情页主体滚动内容 ----
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部留白避让悬浮返回键
            Spacer(modifier = Modifier.height(58.dp))

            // 中心大尺寸圆形头像 (80dp)：真实头像优先，失败回落渐变首字母
            UserAvatar(
                url = displayAvatar,
                name = displayName,
                size = 80.dp,
                gradient = conversation.avatarGradient,
                fallbackTextStyle = TextStyle(
                    color = Color.White,
                    fontSize = 30.6.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 昵称 (Title 2 级别加粗)
            BasicText(
                text = displayName,
                style = TextStyle(
                    color = higColors.label,
                    fontSize = 19.8.sp,
                    lineHeight = 25.2.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // 性别 · 年龄段 · 地区 统一只在下方「个性签名」卡片内展示，此处不再重复

            // 关系徽标：互相关注 / 对方关注了你 / 已拉黑
            val relationBadges = buildList {
                profile?.let { user ->
                    if (user.isMutual) add("互相关注" to higColors.systemGreen)
                    else if (user.followedBy) add("对方关注了你" to higColors.tint)
                    if (user.iBlocked) add("已拉黑" to higColors.destructive)
                    if (user.blocked) add("对方已拉黑你" to higColors.destructive)
                }
            }
            if (relationBadges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    relationBadges.forEach { (label, color) ->
                        BasicText(
                            text = label,
                            style = AppleHigTypography.caption2.copy(color = color, fontWeight = FontWeight.Medium),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(color.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3 个圆形操作图标：关注、私信、拉黑
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isSelf = profile?.isSelf == true
                val isBlocked = profile?.iBlocked == true
                val isBlockedByPeer = profile?.blocked == true
                val isFollowing = profile?.isFollowing == true
                val actionsEnabled = !isLoading && profile != null && !isSelf
                // 官方判定（UserMomentsView）：can_dm !== false && can_receive_dm !== false && !dm_banned
                // 且非拉黑关系；任一不满足则私信入口置灰并显示「无法私信」
                val dmUnavailable = profile != null &&
                    (!profile.dmAllowed || isBlocked || isBlockedByPeer)

                if (!isSelf) {
                    // 关注 / 取消关注（同一按钮 toggle，官方文案：关注 / 已关注）
                    CircularActionButton(
                        icon = if (isFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                        label = if (isFollowing) "已关注" else "关注",
                        tint = if (isFollowing) higColors.tint else higColors.label,
                        containerColor = higColors.systemFill,
                        enabled = actionsEnabled,
                        onClick = {
                            val target = !isFollowing
                            ZeroTalkClientManager.toggleOtherUserFollow { success, err ->
                                notificationState.show(
                                    when {
                                        !success -> err ?: "关注操作失败，请稍后重试"
                                        target -> "关注成功"
                                        else -> "已取消关注"
                                    }
                                )
                            }
                        }
                    )
                }

                // 私信（POST /room/dm/create-from-user：返回或复用私聊房间）
                CircularActionButton(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = if (dmUnavailable) "无法私信" else "私信",
                    tint = if (dmUnavailable) higColors.tertiaryLabel else higColors.label,
                    containerColor = higColors.systemFill,
                    enabled = !isLoading && profile != null &&
                        !isBlocked && !isBlockedByPeer && profile.dmAllowed,
                    onClick = {
                        ZeroTalkClientManager.startDmWithOtherUser(
                            onSuccess = { target ->
                                notificationState.show("已进入与 ${target.targetName} 的私聊")
                                if (target.id == conversation.id) onBack() else onOpenConversation?.invoke(target)
                            },
                            onError = { message -> notificationState.show(message) }
                        )
                    }
                )

                if (!isSelf) {
                    // 拉黑 / 解除拉黑（拉黑前二次确认，拉黑自动取消关注）
                    CircularActionButton(
                        icon = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                        label = if (isBlocked) "解除拉黑" else "拉黑",
                        tint = if (isBlocked) higColors.tint else higColors.destructive,
                        containerColor = higColors.systemFill,
                        enabled = !isLoading && profile != null,
                        onClick = {
                            if (isBlocked) {
                                ZeroTalkClientManager.toggleOtherUserBlock(block = false) { success, err ->
                                    notificationState.show(
                                        if (success) "已解除拉黑" else err ?: "操作失败，请稍后重试"
                                    )
                                }
                            } else {
                                showBlockConfirm = true
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 分段标签：资料 / 设置 (LiquidSegmentedControl)
            // 纯资料面板模式（群成员）不提供设置等会话专属选项
            if (!profileOnly) {
                LiquidSegmentedControl(
                    options = listOf("资料", "设置"),
                    selectedIndex = selectedTab,
                    onOptionSelect = { selectedTab = it },
                    outerHeight = 34.dp,
                    innerHeight = 28.dp,
                    isDark = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 备注接口需要的对端数字 user_id：会话字段常常为空，按「资料 → 会话 → 房间 bootstrap」依次兜底
    val remarkPeerUid = profileState?.profile?.uid?.takeIf { it.isNotBlank() }
        ?: conversation.targetUid.takeIf { it.isNotBlank() }
        ?: roomBootstrapMap[conversation.id]?.peerUser?.uid?.takeIf { it.isNotBlank() }


    // ---- 3. 内容区块 (卡片式分组，HIG Grouped List) ----
            AnimatedContent(
                targetState = if (profileOnly) 0 else selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TabContentTransition"
            ) { currentTab ->
                if (currentTab == 0) {
                    ProfileTabContent(
                        state = profileState,
                        isDark = isDark,
                        currentMomentSort = momentsSort,
                        onRetry = {
                            // 重试同样只加载数据，不触发全局资料页导航
                            ZeroTalkClientManager.loadOtherUserProfileSilently(requestUserId, requestUid)
                        },
                        onOpenFollowList = { kind ->
                            ZeroTalkClientManager.openOtherUserFollowList(kind)
                        },
                        onMomentSortChange = { sort ->
                            ZeroTalkClientManager.setOtherUserMomentsSort(sort)
                        },
                        onToggleMomentLike = { momentId ->
                            ZeroTalkClientManager.toggleLikeMoment(momentId) { err ->
                                notificationState.show(err)
                            }
                        },
                        onOpenMomentComments = { moment ->
                            commentMomentTarget = moment
                        },
                        onShareMoment = { moment ->
                            shareMomentTarget = moment
                        },
                        onReportMoment = { moment ->
                            reportTarget = moment
                        },
                        onLoadMoreMoments = {
                            ZeroTalkClientManager.loadMoreOtherUserMoments()
                        }
                    )
                } else {
                    ChatSettingsTabContent(
                        currentWallpaperKey = currentWallpaperKey,
                        isDark = isDark,
                        conversation = conversation,
                        onWallpaperApplied = onWallpaperApplied,
                        // 会话列表未必下发数字 user_id：备注接口走 uid（官网也是 uid 优先）
                        peerUid = remarkPeerUid
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // ---- 顶部返回导航条 (尺寸、位置与私聊页完全一致，悬浮固定在最上层) ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 21.dp, vertical = 6.dp)
        ) {
            IosLiquidBackButton(
                onClick = onBack,
                backdrop = infoScreenBackdrop,
                isDark = isDark,
                isWhiteBackground = !isDark,
                // 与私聊页一致：只在毛玻璃上叠一层极淡的底，避免暗色下白底 + 白图标糊成一片
                surfaceColor = if (isDark) Color.White else Color.Black,
                surfaceAlpha = 0.06f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp)
            )
        }

        // ---- 4. 关注 / 粉丝列表（推入式二级页面） ----
        AnimatedVisibility(
            visible = followListState != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            followListState?.let { listState ->
                OtherUserFollowListPane(
                    state = listState,
                    isDark = isDark,
                    backdrop = infoScreenBackdrop,
                    onBack = {
                        ZeroTalkClientManager.closeOtherUserFollowList()
                        localFollowListState = null
                    },
                    onKindChange = { kind ->
                        if (kind != listState.kind) ZeroTalkClientManager.openOtherUserFollowList(kind)
                    },
                    onToggleFollow = { item ->
                        val target = !item.isFollowing
                        ZeroTalkClientManager.setFollowForUser(
                            userId = item.userId,
                            uid = item.uid,
                            follow = target
                        ) { success, err ->
                            notificationState.show(
                                when {
                                    !success -> err ?: "关注操作失败，请稍后重试"
                                    target -> "已关注 ${item.name}"
                                    else -> "已取消关注"
                                }
                            )
                        }
                    },
                    onOpenUser = { item ->
                        // 下钻一层：压入本实例拥有的 PAGE 层（同一目标已在栈顶时不会重复叠层）
                        ZeroTalkClientManager.pushUserProfileLayer(
                            target = UserProfileTarget(
                                userId = item.userId,
                                uid = item.uid,
                                name = item.name,
                                avatarUrl = item.avatarUrl
                            ),
                            host = UserProfileLayerHost.PAGE,
                            owner = pageLayerOwner
                        )
                    },
                    onLoadMore = { ZeroTalkClientManager.loadOtherUserFollowList(reset = false) }
                )
            }
        }

        // ---- 5. 拉黑二次确认 ----
        if (showBlockConfirm) {
            BlockConfirmOverlay(
                isDark = isDark,
                onCancel = { showBlockConfirm = false },
                onConfirm = {
                    showBlockConfirm = false
                    ZeroTalkClientManager.toggleOtherUserBlock(block = true) { success, err ->
                        notificationState.show(
                            if (success) "已将 $displayName 加入黑名单" else err ?: "拉黑失败，请稍后重试"
                        )
                    }
                }
            )
        }
        // ---- 6. 主页动态的评论抽屉 / 分享面板（统一由 MomentSheetsHost 承载）----
        MomentSheetsHost(
            commentTarget = commentMomentTarget,
            shareTarget = shareMomentTarget,
            isDark = isDark,
            backdrop = actualBackdrop,
            onDismissComment = { commentMomentTarget = null },
            onDismissShare = { shareMomentTarget = null }
        )

        // ---- 6.1 举报动态弹窗（官方六项理由 + 选填说明）----
        reportTarget?.let { target ->
            UserReportDialog(
                userName = target.authorName,
                isDark = isDark,
                submitting = reportSubmitting,
                onDismiss = { if (!reportSubmitting) reportTarget = null },
                onSubmit = { reason, description ->
                    reportSubmitting = true
                    ZeroTalkClientManager.reportMoment(
                        momentId = target.id.toLongOrNull() ?: 0L,
                        reason = reason.value,
                        description = description
                    ) { success, err ->
                        reportSubmitting = false
                        if (success) {
                            reportTarget = null
                            notificationState.show("已提交举报，我们会尽快核实")
                        } else {
                            notificationState.show(err ?: "举报提交失败")
                        }
                    }
                }
            )
        }
    }

        // 柔和暗色遮罩（当次级主页推入时平滑变暗 35%，返回时淡出恢复）
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = dimAlpha }
                    .background(Color.Black)
            )
        }

        // ---- 2. 页面内下钻的次级用户主页（层级栈中的 PAGE 层）：从右侧平滑推入与滑出 ----
        AnimatedVisibility(
            visible = pageProfileSlot.visible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        ) {
            pageProfileSlot.layer?.let { layer ->
                OtherUserProfileScreen(
                    target = layer.target,
                    isDark = isDark,
                    backdrop = actualBackdrop,
                    // 只有本层正好是层级栈栈顶时才允许它拦截返回
                    isTopLayer = pageProfileSlot.isTop,
                    onBack = { ZeroTalkClientManager.popUserProfileLayer() },
                    onOpenConversation = { conv ->
                        ZeroTalkClientManager.clearUserProfileLayers()
                        ZeroTalkClientManager.closeOtherUserFollowList()
                        localFollowListState = null
                        onOpenConversation?.invoke(conv)
                    }
                )
            }
        }
    }
}

/**
 * 「资料」页签：个性签名 + 资料计数 + 关系 + 对方动态
 */
@Composable
private fun ProfileTabContent(
    state: OtherUserProfileState?,
    isDark: Boolean,
    currentMomentSort: MomentsSort,
    onRetry: () -> Unit,
    onOpenFollowList: (FollowListKind) -> Unit,
    onMomentSortChange: (MomentsSort) -> Unit,
    onToggleMomentLike: (String) -> Unit,
    onOpenMomentComments: (MomentItem) -> Unit,
    onShareMoment: (MomentItem) -> Unit,
    onReportMoment: (MomentItem) -> Unit,
    onLoadMoreMoments: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val profile = state?.profile

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            // state 为 null 表示首次拉取尚未开始（LaunchedEffect 还未执行），此时应展示加载态而不是错误
            profile == null && (state == null || state.isLoading) -> ProfileLoadingCard(isDark = isDark)

            profile == null -> ProfileErrorCard(
                isDark = isDark,
                message = state.error?.takeIf { it.isNotBlank() } ?: "暂时无法获取用户资料",
                onRetry = onRetry
            )

            else -> {
                ProfileSignatureCard(profile = profile, isDark = isDark)
                // MBTI 卡片：未设置 MBTI 时为 null，不渲染
                profile.mbti?.let { mbti ->
                    MbtiCard(
                        mbti = mbti,
                        isDark = isDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
                ProfileStatsCard(
                    profile = profile,
                    isDark = isDark,
                    onOpenFollowList = onOpenFollowList
                )
                ProfileMomentsCard(
                    profile = profile,
                    isLoadingMore = state.isLoadingMore,
                    isDark = isDark,
                    currentSort = currentMomentSort,
                    onSortChange = onMomentSortChange,
                    onToggleLike = onToggleMomentLike,
                    onComment = onOpenMomentComments,
                    onShare = onShareMoment,
                    onReport = onReportMoment,
                    onLoadMore = onLoadMoreMoments
                )
            }
        }
    }
}

/**
 * 第一组：个性签名（含性别 / 年龄段 / 地区）
 */
@Composable
private fun ProfileSignatureCard(profile: OtherUserProfile, isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)

    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPaddingValues = PaddingValues(16.dp)
    ) {
        BasicText(
            text = "签名",
            style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
        )

        Spacer(modifier = Modifier.height(6.dp))

        BasicText(
            text = profile.bio.takeIf { it.isNotBlank() } ?: "这个人很神秘，还没有写简介",
            style = AppleHigTypography.body.copy(
                color = if (profile.bio.isNotBlank()) higColors.label else higColors.tertiaryLabel
            )
        )

        if (profile.subtitleLine.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            AppleHigDivider(isDark = isDark, insetStart = 0.dp)
            Spacer(modifier = Modifier.height(10.dp))
            BasicText(
                text = profile.subtitleLine,
                style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
            )
        }
    }
}

/**
 * 第二组：获赞 / 互关 / 关注 / 粉丝 计数（互关、关注与粉丝可点开列表，与官方主页统计项一致）
 */
@Composable
private fun ProfileStatsCard(
    profile: OtherUserProfile,
    isDark: Boolean,
    onOpenFollowList: (FollowListKind) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPaddingValues = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCell(
                value = profile.likeCount,
                label = "获赞",
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
            // 互关统计项仅在本人主页展示（与官方 UserMomentsView 一致）
            if (profile.isSelf) {
                StatCell(
                    value = profile.mutualCount,
                    label = "互关",
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenFollowList(FollowListKind.MUTUAL) }
                )
            }
            StatCell(
                value = profile.followingCount,
                label = "关注",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onOpenFollowList(FollowListKind.FOLLOWING) }
            )
            StatCell(
                value = profile.followerCount,
                label = "粉丝",
                isDark = isDark,
                modifier = Modifier.weight(1f),
                onClick = { onOpenFollowList(FollowListKind.FOLLOWERS) }
            )
        }

        if (!profile.canViewFollowing || !profile.canViewFollowers) {
            Spacer(modifier = Modifier.height(10.dp))
            BasicText(
                text = "对方设置了部分关系列表不可见",
                style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
            )
        }
    }
}

@Composable
private fun StatCell(
    value: Int,
    label: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .background(higColors.secondarySystemFill)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicText(
            text = formatCount(value),
            style = AppleHigTypography.title3.copy(color = higColors.label)
        )
        Spacer(modifier = Modifier.height(2.dp))
        BasicText(
            text = label,
            style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
        )
    }
}

/**
 * 第三组：对方动态（GET /api/moment/user 的 list 字段）
 *
 * 对齐官网 UserMomentsView：支持「最新 / 最热」排序（sort=latest|hot）与
 * before_id / before_score 分页；动态卡片直接复用动态主界面的 MomentCard
 * （头像 / 昵称 / 正文 / 配图 / 点赞 · 评论 · 分享）。
 */
@Composable
private fun ProfileMomentsCard(
    profile: OtherUserProfile,
    isLoadingMore: Boolean,
    isDark: Boolean,
    currentSort: MomentsSort,
    onSortChange: (MomentsSort) -> Unit,
    onToggleLike: (String) -> Unit,
    onComment: (MomentItem) -> Unit,
    onShare: (MomentItem) -> Unit,
    onReport: (MomentItem) -> Unit,
    onLoadMore: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current
    val moments = profile.moments
    // 官方状态卡：blocked 为真（对方拉黑了我）时展示「动态不可见」+ blocked_message；
    // moments_public 明确下发 false 且无动态时同样按不可见处理。
    val invisible = profile.blocked || (profile.momentsPublic == false && moments.isEmpty())

    when {
            invisible -> AppleHigFillCard(
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPaddingValues = PaddingValues(16.dp)
            ) {
                Column {
                    BasicText(
                        text = "动态不可见",
                        style = AppleHigTypography.body.copy(color = higColors.label)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicText(
                        text = profile.blockedMessage.takeIf { it.isNotBlank() }
                            ?: "对方关闭了动态查看功能",
                        style = AppleHigTypography.footnote.copy(color = higColors.tertiaryLabel)
                    )
                }
            }

            moments.isEmpty() -> AppleHigFillCard(
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPaddingValues = PaddingValues(16.dp)
            ) {
                BasicText(
                    text = "暂无公开动态",
                    style = AppleHigTypography.footnote.copy(color = higColors.tertiaryLabel)
                )
            }

            else -> {
                // 合并成一张大卡片：表头（动态 / 共 N 条 / 最新·最热）挪进卡片内，
                // 条目是同一作者，不再重复头像昵称
                AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPaddingValues = PaddingValues(0.dp)
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "动态",
                        style = AppleHigTypography.subhead.copy(
                            color = higColors.secondaryLabel,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    BasicText(
                        text = if (profile.hasMoreMoments) "已加载 ${moments.size} 条" else "共 ${moments.size} 条",
                        style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    MomentsSortCapsule(
                        currentSort = currentSort,
                        onSortChange = onSortChange,
                        isDark = isDark
                    )
                }
                AppleHigDivider(isDark = isDark, insetStart = 16.dp)
                moments.forEach { moment ->
                    MomentCard(
                        moment = moment,
                        isDark = isDark,
                        onLikeClick = { onToggleLike(moment.id) },
                        onCommentClick = { onComment(moment) },
                        onShareClick = { onShare(moment) },
                        onFollowClick = null,
                        // 资料页里性别已在「签名」卡展示，动态卡片不再重复
                        showGender = false,
                        // 同一作者：不重复头像昵称，且不再自带卡片背景
                        showAuthor = false,
                        embedded = true,
                        onReportClick = { onReport(moment) },
                        // 置顶 / 可见性 / 删除仅在 moment.isMine 时由卡片露出管理菜单
                        onPinToggle = { isPinned ->
                            moment.id.toLongOrNull()?.let { id ->
                                ZeroTalkClientManager.setMomentPin(
                                    momentId = id,
                                    isPinned = isPinned,
                                    onSuccess = {
                                        notificationState.show(if (isPinned) "动态已置顶" else "已取消置顶")
                                    },
                                    onError = { notificationState.show(it) }
                                )
                            }
                        },
                        onVisibilityToggle = { isPrivate ->
                            moment.id.toLongOrNull()?.let { id ->
                                ZeroTalkClientManager.setMomentVisibility(
                                    momentId = id,
                                    isPrivate = isPrivate,
                                    onSuccess = {
                                        notificationState.show(if (isPrivate) "已设为仅自己可见" else "已设为公开")
                                    },
                                    onError = { notificationState.show(it) }
                                )
                            }
                        },
                        onDelete = {
                            moment.id.toLongOrNull()?.let { id ->
                                ZeroTalkClientManager.deleteMoment(
                                    momentId = id,
                                    onSuccess = { notificationState.show("动态已删除") },
                                    onError = { notificationState.show(it) }
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
                }

                if (profile.hasMoreMoments) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !isLoadingMore,
                                onClick = onLoadMore
                            )
                            .background(higColors.secondarySystemFill)
                            .padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 1.6.dp,
                                color = higColors.tint
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        BasicText(
                            text = if (isLoadingMore) "加载更多…" else "加载更多",
                            style = AppleHigTypography.footnote.copy(
                                color = higColors.tint,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                } else {
                    BasicText(
                        text = "已阅尽此刻",
                        style = AppleHigTypography.caption1.copy(
                            color = higColors.tertiaryLabel,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
}

@Composable
private fun ProfileLoadingCard(isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)

    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPaddingValues = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = higColors.tint
            )
            BasicText(
                text = "正在获取用户资料…",
                style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
            )
        }
    }
}

/**
 * 资料拉取失败：明确展示服务端原因并给出重试入口，避免「暂时无法获取用户资料」成为死胡同
 */
@Composable
private fun ProfileErrorCard(
    isDark: Boolean,
    message: String,
    onRetry: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPaddingValues = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "资料加载失败",
                tint = higColors.tertiaryLabel,
                modifier = Modifier.size(30.dp)
            )
            BasicText(
                text = message,
                style = AppleHigTypography.footnote.copy(
                    color = higColors.secondaryLabel,
                    textAlign = TextAlign.Center
                )
            )
            CapsuleGlassButton(
                onClick = onRetry,
                isDark = isDark,
                modifier = Modifier.height(30.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重新加载",
                        tint = higColors.tint,
                        modifier = Modifier.size(12.6.dp)
                    )
                    BasicText(
                        text = "重新加载",
                        style = AppleHigTypography.footnote.copy(
                            color = higColors.tint,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

/**
 * 关注 / 粉丝列表二级页面（GET /api/follow/list）
 */
@Composable
private fun OtherUserFollowListPane(
    state: OtherUserFollowListState,
    isDark: Boolean,
    backdrop: Backdrop?,
    onBack: () -> Unit,
    onKindChange: (FollowListKind) -> Unit,
    onToggleFollow: (FollowUserItem) -> Unit,
    onOpenUser: (FollowUserItem) -> Unit,
    onLoadMore: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val scrollState = rememberScrollState()
    // 互关分段仅在本人主页出现（与官方一致）
    val kinds = remember(state.mutualAllowed) {
        listOf(FollowListKind.FOLLOWING, FollowListKind.MUTUAL, FollowListKind.FOLLOWERS)
            .filter { it != FollowListKind.MUTUAL || state.mutualAllowed }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) Color(0xFF12141A)
                else Color(0xFFF2F2F7)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(58.dp))

            BasicText(
                text = "关系列表",
                style = AppleHigTypography.title2.copy(
                    color = higColors.label,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            LiquidSegmentedControl(
                options = kinds.map { it.title },
                selectedIndex = kinds.indexOf(state.kind).coerceAtLeast(0),
                onOptionSelect = { index -> kinds.getOrNull(index)?.let(onKindChange) },
                outerHeight = 34.dp,
                innerHeight = 28.dp,
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading && state.list.isEmpty() -> ProfileLoadingCard(isDark = isDark)

                !state.allowed -> AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPaddingValues = PaddingValues(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "不可见",
                            tint = higColors.tertiaryLabel,
                            modifier = Modifier.size(30.dp)
                        )
                        BasicText(
                            text = state.message.takeIf { it.isNotBlank() }
                                ?: "对方设置了${state.kind.title}列表不可见",
                            style = AppleHigTypography.footnote.copy(
                                color = higColors.secondaryLabel,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }

                state.error != null && state.list.isEmpty() -> AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPaddingValues = PaddingValues(20.dp)
                ) {
                    BasicText(
                        text = state.error,
                        style = AppleHigTypography.footnote.copy(color = higColors.destructive)
                    )
                }

                state.list.isEmpty() -> AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPaddingValues = PaddingValues(24.dp)
                ) {
                    BasicText(
                        text = "还没有${state.kind.title}的用户",
                        style = AppleHigTypography.footnote.copy(
                            color = higColors.tertiaryLabel,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPaddingValues = PaddingValues(vertical = 6.dp)
                ) {
                    state.list.forEachIndexed { index, item ->
                        if (index > 0) {
                            AppleHigDivider(isDark = isDark, insetStart = 62.dp)
                        }
                        FollowUserRow(
                            item = item,
                            kind = state.kind,
                            isDark = isDark,
                            onOpen = { onOpenUser(item) },
                            onToggleFollow = { onToggleFollow(item) }
                        )
                    }

                    if (state.hasMore) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = !state.isLoadingMore,
                                    onClick = onLoadMore
                                )
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 1.6.dp,
                                    color = higColors.tint
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            BasicText(
                                text = if (state.isLoadingMore) "加载更多…" else "加载更多",
                                style = AppleHigTypography.footnote.copy(
                                    color = higColors.tint,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 21.dp, vertical = 6.dp)
        ) {
            IosLiquidBackButton(
                onClick = onBack,
                backdrop = backdrop,
                isDark = isDark,
                isWhiteBackground = !isDark,
                // 与私聊页一致：只在毛玻璃上叠一层极淡的底，避免暗色下白底 + 白图标糊成一片
                surfaceColor = if (isDark) Color.White else Color.Black,
                surfaceAlpha = 0.06f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun FollowUserRow(
    item: FollowUserItem,
    kind: FollowListKind,
    isDark: Boolean,
    onOpen: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpen
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        UserAvatar(
            url = item.avatarUrl.takeIf { it.isNotBlank() },
            name = item.name,
            size = 36.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = item.name,
                style = AppleHigTypography.groupedRowTitle.copy(
                    color = higColors.label,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            // 关系副标题（对齐官方 FollowListView 文案）
            val subtitle = buildList {
                if (item.genderText.isNotBlank()) add(item.genderText)
                when {
                    item.isMutual -> add("互相关注")
                    item.isFollower && kind == FollowListKind.FOLLOWING -> add("回关了你")
                    item.isFollowing && kind == FollowListKind.FOLLOWERS -> add("已关注")
                    item.isFollower -> add("关注了你")
                }
                if (item.iBlocked) add("已拉黑")
            }.joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                BasicText(
                    text = subtitle,
                    style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel),
                    maxLines = 1
                )
            }
        }

        if (item.isSelf) {
            BasicText(
                text = "自己",
                style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
            )
        } else {
            FollowCapsuleButton(
                isFollowing = item.isFollowing,
                isDark = isDark,
                onClick = onToggleFollow
            )
        }
    }
}

/**
 * 拉黑二次确认浮层（对齐官方 Web 端「有确认弹窗」的行为）
 */
@Composable
private fun BlockConfirmOverlay(
    isDark: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0xFF1C1F28) else Color.White)
                // 吞掉卡片区域的点击，避免误触蒙层关闭
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText(
                    text = "确认拉黑对方？",
                    style = AppleHigTypography.headline.copy(color = higColors.label)
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = "拉黑后，你们将无法再相互匹配，对方也无法向你发起私聊。",
                    style = AppleHigTypography.footnote.copy(
                        color = higColors.secondaryLabel,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicText(
                    text = "可在「我的黑名单」中随时解除。",
                    style = AppleHigTypography.footnote.copy(
                        color = higColors.tertiaryLabel,
                        textAlign = TextAlign.Center
                    )
                )
            }

            AppleHigDivider(isDark = isDark, insetStart = 0.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCancel
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "取消",
                        style = AppleHigTypography.groupedRowTitle.copy(color = higColors.tint)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(higColors.separator)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onConfirm
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "确认拉黑",
                        style = AppleHigTypography.groupedRowTitle.copy(
                            color = higColors.destructive,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

/**
 * 头部圆形操作按钮（50dp 圆形图标底座 + 下方 12sp 标签文字）
 */
@Composable
private fun CircularActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    containerColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val contentColor = if (enabled) tint else tint.copy(alpha = 0.35f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(containerColor)
                .border(0.5.dp, Color.White.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        BasicText(
            text = label,
            style = TextStyle(
                color = contentColor,
                fontSize = 10.8.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * 「设置」页签：对话专属壁纸（本地偏好）+ 消息免打扰（线上 /room/mute）
 *
 * 会话资料页与暗号房资料页共用（后者把它放进「设置」页签）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatSettingsTabContent(
    currentWallpaperKey: String?,
    isDark: Boolean,
    conversation: ConversationItem,
    /** 背景已按范围写入管理器后回调（宿主据此刷新本地状态 / 追加系统提示） */
    onWallpaperApplied: (String?) -> Unit = {},
    /** 内容左右留白：会话资料页自带 16dp；暗号房资料页的容器已留白，需传 0.dp 以免双份缩进 */
    horizontalPadding: Dp = 16.dp,
    /** 对端 uid（备注接口的 peer_user_id，uid 优先）；群聊 / 未知时传 null */
    peerUid: String? = null
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current
    val coroutineScope = rememberCoroutineScope()

    // 消息免打扰本地状态（服务端为权威，成功后回写；与暗号房资料页一致）
    var muted by remember(conversation.id) { mutableStateOf(conversation.isMuted) }
    val cardBackdrop = rememberCanvasBackdrop { drawRect(higColors.systemFill) }

    // 自定义背景保存状态（图片对齐宽度铺满，上下如有留空露出默认背景色）
    var isUploading by remember { mutableStateOf(false) }
    // 待应用背景：选完图 / 选完纯色后，询问生效范围（仅当前会话 / 作为默认背景）
    var pendingWallpaper by remember { mutableStateOf<WallpaperPending?>(null) }
    val wallpaperMapState by ZeroTalkClientManager.conversationWallpapers.collectAsState()
    val globalWallpaper by ZeroTalkClientManager.globalWallpaper.collectAsState()
    val hasWallpaperOverride = wallpaperMapState.containsKey(conversation.id)
    // 远程图下载成功后会落盘为本地文件，因此这里 http(s) 与本地路径都要认
    val customWallpaperUrl = currentWallpaperKey?.takeIf {
        it.startsWith("http://") || it.startsWith("https://") || looksLikeLocalWallpaperPath(it)
    }
    val customWallpaperLocalFile = remember(currentWallpaperKey) {
        resolveLocalWallpaperFile(currentWallpaperKey)
    }
    var customWallpaperBitmap by remember(currentWallpaperKey) {
        mutableStateOf<ImageBitmap?>(null)
    }
    LaunchedEffect(customWallpaperLocalFile) {
        customWallpaperBitmap = customWallpaperLocalFile?.let {
            withContext(Dispatchers.IO) { decodeLocalWallpaper(it) }
        }
    }
    val photoPickerLauncher = rememberPhotoPickerLauncher(
        maxItems = 1,
        onImagesSelected = { photos ->
            val photo = photos.firstOrNull() ?: return@rememberPhotoPickerLauncher
            isUploading = true
            coroutineScope.launch {
                // 背景只存本机：不上传服务器，直接把相册图片写入本地文件
                val path = withContext(Dispatchers.IO) {
                    // 只落盘，不登记范围：范围由随后的「应用背景」弹窗决定
                    ZeroTalkClientManager.saveLocalWallpaperFile(photo.byteArray)
                }
                isUploading = false
                if (path != null) pendingWallpaper = WallpaperPending(path, "自定义")
                else notificationState.show("背景图保存失败")
            }
        },
        onPermissionDenied = {
            notificationState.show("需要相册读取权限以上传背景图，请在系统设置中允许")
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- 通知：消息免打扰 ----
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp)
        ) {
            BasicText(
                text = "通知",
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "隐藏提醒",
                    style = TextStyle(color = higColors.label, fontSize = 15.3.sp, fontWeight = FontWeight.Normal),
                    modifier = Modifier.weight(1f)
                )
                LiquidToggle(
                    selected = { muted },
                    onSelect = { next ->
                        ZeroTalkClientManager.setRoomMuted(conversation.id, next) { success, message ->
                            if (success) {
                                muted = next
                                notificationState.show(if (next) "已开启消息免打扰" else "已关闭消息免打扰")
                            } else {
                                notificationState.show(message ?: "免打扰设置失败")
                            }
                        }
                    },
                    backdrop = cardBackdrop,
                    isDark = isDark
                )
            }
        }

        // ---- 备注名（仅 1v1 对端；群聊没有单一对端，不显示）----
        if (conversation.category != MessageCategory.CODE && !peerUid.isNullOrBlank()) {
            var remarkDraft by remember(conversation.id, conversation.targetRemark) {
                mutableStateOf(conversation.targetRemark)
            }
            var savingRemark by remember { mutableStateOf(false) }
            AppleHigFillCard(
                isDark = isDark,
                modifier = Modifier.fillMaxWidth(),
                contentPaddingValues = PaddingValues(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BasicText(
                        text = "备注名",
                        style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(higColors.quaternarySystemFill)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = remarkDraft,
                                // 官方 maxlength=20
                                onValueChange = { if (it.length <= 20) remarkDraft = it },
                                textStyle = AppleHigTypography.body.copy(color = higColors.label),
                                cursorBrush = SolidColor(higColors.systemBlue),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (remarkDraft.isEmpty()) {
                                        BasicText(
                                            text = "留空则清除备注",
                                            style = AppleHigTypography.body.copy(color = higColors.tertiaryLabel)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                        CapsuleGlassButton(
                            onClick = {
                                if (savingRemark) return@CapsuleGlassButton
                                savingRemark = true
                                ZeroTalkClientManager.setPeerRemark(peerUid, remarkDraft) { success, err ->
                                    savingRemark = false
                                    if (success) {
                                        notificationState.show(
                                            if (remarkDraft.isBlank()) "已清除备注" else "备注已保存"
                                        )
                                    } else {
                                        notificationState.show(err ?: "保存失败")
                                    }
                                }
                            },
                            isDark = isDark,
                            modifier = Modifier.height(34.dp)
                        ) {
                            BasicText(
                                text = if (savingRemark) "保存中…" else "保存",
                                style = AppleHigTypography.footnote.copy(
                                    color = higColors.tint,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BasicText(
                    text = "背景",
                    style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 选项 1：默认（素雅纯色）
                    WallpaperCardItem(
                        title = "默认",
                        isSelected = currentWallpaperKey == null,
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { pendingWallpaper = WallpaperPending(null, "默认") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isDark) Color(0xFF161820) else Color(0xFFF4F2F9))
                        )
                    }

                    // 选项 2：自定义背景图（从相册选择）
                    WallpaperCardItem(
                        title = "自定义",
                        isSelected = customWallpaperUrl != null,
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { photoPickerLauncher.launch() }
                    ) {
                        val localPreview = customWallpaperBitmap
                        if (localPreview != null) {
                            LocalWallpaperImage(
                                bitmap = localPreview,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else if (customWallpaperUrl != null && !looksLikeLocalWallpaperPath(customWallpaperUrl)) {
                            AsyncNetworkImage(
                                url = customWallpaperUrl,
                                contentDescription = "自定义背景预览",
                                shape = RoundedCornerShape(8.dp),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (customWallpaperUrl == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = higColors.tint
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "添加背景",
                                            tint = higColors.tint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        BasicText(
                                            text = "上传背景图",
                                            style = AppleHigTypography.caption2.copy(
                                                color = higColors.secondaryLabel
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 当前生效范围：仅在与「默认纯色」不同时才提示（顺带给出恢复跟随默认的出口）
                if (hasWallpaperOverride || globalWallpaper != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = if (hasWallpaperOverride) "当前：已单独设置" else "当前：跟随自定义背景",
                            style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel),
                            modifier = Modifier.weight(1f)
                        )
                        if (hasWallpaperOverride) {
                            BasicText(
                                text = "恢复跟随默认",
                                style = AppleHigTypography.footnote.copy(
                                    color = higColors.tint,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        ZeroTalkClientManager.clearConversationWallpaperOverride(conversation.id)
                                        notificationState.show("已恢复跟随默认背景")
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        BasicText(
            text = "自定义背景图将自适应铺满整个对话界面，超出部分会被裁掉。",
            style = AppleHigTypography.footnote.copy(
                color = higColors.secondaryLabel,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }

    // ---- 应用背景：询问生效范围（对齐 iOS「用作墙纸」范式）----
    pendingWallpaper?.let { pending ->
        AppleModalBottomSheet(
            onDismissRequest = { pendingWallpaper = null },
            title = "应用背景",
            leadingAction = SheetAction.Close { pendingWallpaper = null },
            isDark = isDark
        ) {
            BasicText(
                text = "已选择「" + pending.label + "」，选择生效范围",
                style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(6.dp))
            WallpaperScopeRow(
                title = "仅当前会话",
                subtitle = "只对「" + conversation.targetName + "」生效",
                isDark = isDark
            ) {
                ZeroTalkClientManager.setConversationWallpaper(conversation.id, pending.value)
                onWallpaperApplied(pending.value)
                pendingWallpaper = null
                notificationState.show(if (pending.value == null) "已设为默认" else "背景已应用")
            }
            AppleHigDivider(isDark = isDark, insetStart = 20.dp)
            WallpaperScopeRow(
                title = "作为默认背景",
                subtitle = "所有未单独设置背景的会话都会使用",
                isDark = isDark
            ) {
                ZeroTalkClientManager.setGlobalWallpaper(pending.value)
                onWallpaperApplied(pending.value)
                pendingWallpaper = null
                notificationState.show(if (pending.value == null) "已清除默认背景" else "已设为默认背景")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 待应用背景（value 为 null 表示素雅纯色） */
private data class WallpaperPending(val value: String?, val label: String)

/** 应用背景弹窗里的一行 */
@Composable
private fun WallpaperScopeRow(
    title: String,
    isDark: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val colors = AppleHigColors.colors(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = AppleHigTypography.body.copy(color = colors.label)
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = subtitle,
                    style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel)
                )
            }
        }
    }
}

/**
 * 对话壁纸卡片选择项
 */
@Composable
private fun WallpaperCardItem(
    title: String,
    isSelected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    previewContent: @Composable () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) higColors.tint else (if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)),
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        // 标题留在预览图下方、卡片内，水平居中
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            previewContent()

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(higColors.tint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        BasicText(
            text = title,
            style = TextStyle(
                color = higColors.label,
                fontSize = 12.6.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/** 计数展示：过万折算为「x.x万」 */
private fun formatCount(value: Int): String = when {
    value >= 10000 -> {
        val scaled = value / 1000 / 10.0
        "${scaled}万"
    }
    else -> value.toString()
}

/**
 * 独立的「用户资料面板」
 *
 * 群聊（点消息里的发送者）与群聊面板（点成员）都跳转到此面板：
 * 复用他人主页的资料区 —— 头像 / 昵称 / 关系徽标 / 关注·私信·拉黑 / 个性签名 / 获赞·关注·粉丝 / 动态，
 * 但不含会话背景等会话专属设置，也不再依赖会话项是否存在（群成员没有独立会话）。
 *
 * @param target 入口参数（数字 user_id / hex uid，以及已知昵称与头像作为加载期占位）
 */
@Composable
fun OtherUserProfileScreen(
    target: UserProfileTarget,
    isDark: Boolean,
    onBack: () -> Unit,
    backdrop: Backdrop? = null,
    onOpenConversation: ((ConversationItem) -> Unit)? = null,
    isTopLayer: Boolean = true,
    modifier: Modifier = Modifier
) {
    val syntheticConversation = remember(target.userId, target.uid, target.name, target.avatarUrl) {
        ConversationItem(
            id = "",
            // 已知昵称/头像先占位，接口返回后由主页数据覆盖
            targetName = target.name,
            targetAvatar = target.avatarUrl.takeIf { it.isNotBlank() },
            lastMessage = "",
            timestamp = "",
            category = MessageCategory.PRIVATE,
            targetUserId = target.userId,
            targetUid = target.uid
        )
    }

    ConversationInfoScreen(
        conversation = syntheticConversation,
        onBack = onBack,
        backdrop = backdrop,
        isDark = isDark,
        onOpenConversation = onOpenConversation,
        profileOnly = true,
        // 本面板是资料页层级栈中的一层：只有栈顶那一层才允许拦截返回
        backEnabled = isTopLayer,
        modifier = modifier
    )
}
