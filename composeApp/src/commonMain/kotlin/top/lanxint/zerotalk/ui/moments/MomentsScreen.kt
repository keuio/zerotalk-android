package top.lanxint.zerotalk.ui.moments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import top.lanxint.zerotalk.ui.components.LocalImageViewer
import top.lanxint.zerotalk.ui.components.NetworkImageLoader
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.data.network.MomentsCategory
import top.lanxint.zerotalk.data.network.MomentsSort
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.AppleModalBottomSheet
import top.lanxint.zerotalk.ui.components.SheetAction
import top.lanxint.zerotalk.ui.components.FollowCapsuleButton
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
import top.lanxint.zerotalk.ui.components.LiquidButton
import top.lanxint.zerotalk.ui.utils.BackHandler
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.MomentComposer
import top.lanxint.zerotalk.ui.components.ZeroTalkBottomTab
import top.lanxint.zerotalk.ui.components.ZeroTalkBottomTabs
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.messages.UserReportDialog
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.drawWithContent
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.Backdrop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * 零语「动态」主界面
 *
 * 核心特性：
 * 1. 顶部集成三大 Tab（精选、关注、我的），采用 Apple HIG 液态毛玻璃胶囊；
 * 2. 顶部右侧紧随轻量级「最新 / 最热」微型切换胶囊；
 * 3. 页面支持 HorizontalPager 左右无级滑动手势，与顶部胶囊指示器双向物理联动；
 * 4. 动态卡片支持点赞（含跳动数）、评论抽屉呼出、作者本人置顶/可见性/删除管理；
 * 5. 评论系统完整支持一键点赞与二级针对性回复（回复 @用户 芯片）；
 * 6. 右下角配备 Apple HIG 磨砂悬浮「+ 发布动态」气泡按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(
    backdrop: Backdrop? = null,
    sheetBackdrop: Backdrop? = null,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current
    val coroutineScope = rememberCoroutineScope()

    val categories = remember { listOf(MomentsCategory.FEATURED, MomentsCategory.FOLLOWING, MomentsCategory.MINE) }
    val pagerState = rememberPagerState { categories.size }

    // 动态页专属折射采样 Backdrop 与各分类列表 LazyListState
    val momentsBackdrop = rememberLayerBackdrop()
    val featuredListState = rememberLazyListState()
    val followingListState = rememberLazyListState()
    val mineListState = rememberLazyListState()

    // 动态列表状态
    val featuredList by ZeroTalkClientManager.momentsFeatured.collectAsState()
    val followingList by ZeroTalkClientManager.momentsFollowing.collectAsState()
    val mineList by ZeroTalkClientManager.momentsMine.collectAsState()

    var currentSort by remember { mutableStateOf(MomentsSort.LATEST) }
    var isRefreshing by remember { mutableStateOf(false) }

    // 活跃查看评论的动态
    var commentMomentTarget by remember { mutableStateOf<MomentItem?>(null) }

    // 活跃分享的动态
    var shareMomentTarget by remember { mutableStateOf<MomentItem?>(null) }

    // 举报动态目标与提交中标记
    var reportTarget by remember { mutableStateOf<MomentItem?>(null) }
    var reportSubmitting by remember { mutableStateOf(false) }

    // 发布动态弹窗状态
    var isPublishSheetVisible by remember { mutableStateOf(false) }

    // 优先拦截动态发布弹窗（评论/分享由 MomentSheetsHost 自动统一拦截）
    BackHandler(enabled = isPublishSheetVisible) {
        isPublishSheetVisible = false
    }

    // 首次进入与 Tab 切换时拉取数据
    LaunchedEffect(pagerState.currentPage, currentSort) {
        val cat = categories[pagerState.currentPage]
        ZeroTalkClientManager.fetchMomentsByCategory(category = cat, sort = currentSort)
    }

    // 顶部悬浮控制栏总高 = 状态栏 + 上下各 8dp + 胶囊 38dp，再留 16dp 呼吸留白：
    // 首条动态整体下移，不再紧贴顶部栏（此前固定 84dp，在状态栏较高的机型上会被顶部栏压住）
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listTopPadding = statusBarTop + 54.dp + 16.dp

    Box(modifier = modifier.fillMaxSize()) {
        // ---- 中间滑动分页动态列表内容区（录入 momentsBackdrop 供发布按钮及顶部栏透空折射）----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val currentListState = when (pagerState.currentPage) {
                        0 -> featuredListState
                        1 -> followingListState
                        else -> mineListState
                    }
                    currentListState.firstVisibleItemScrollOffset
                    currentListState.firstVisibleItemIndex
                    drawContent()
                }
                .layerBackdrop(momentsBackdrop)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val cat = categories[page]
                val list = when (cat) {
                    MomentsCategory.FEATURED -> featuredList
                    MomentsCategory.FOLLOWING -> followingList
                    MomentsCategory.MINE -> mineList
                }
                val currentListState = when (page) {
                    0 -> featuredListState
                    1 -> followingListState
                    else -> mineListState
                }

                if (list.isEmpty()) {
                    EmptyMomentsView(category = cat, isDark = isDark)
                } else {
                    LazyColumn(
                        state = currentListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = listTopPadding,   // 顶部悬浮 Tab / 排序栏高度 + 呼吸留白
                            bottom = 120.dp // 预留底部导航栏高度
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = list,
                            key = { it.id },
                            contentType = { "moment_card" }
                        ) { item ->
                            MomentCard(
                                moment = item,
                                isDark = isDark,
                                onLikeClick = {
                                    ZeroTalkClientManager.toggleLikeMoment(item.id) { err ->
                                        notificationState.show(err)
                                    }
                                },
                                onCommentClick = {
                                    commentMomentTarget = item
                                },
                                onShareClick = {
                                    shareMomentTarget = item
                                },
                                onReportClick = { reportTarget = item },
                                onFollowClick = {
                                    val following = item.isFollowed
                                    ZeroTalkClientManager.setMomentAuthorFollow(
                                        authorId = item.authorId,
                                        authorUid = item.authorUid,
                                        isFollowing = !following,
                                        onSuccess = {
                                            notificationState.show(
                                                if (following) "已取消关注" else "已关注 ${item.authorName}"
                                            )
                                        },
                                        onError = { notificationState.show(it) }
                                    )
                                },
                                onPinToggle = { isPinned ->
                                    item.id.toLongOrNull()?.let { id ->
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
                                    item.id.toLongOrNull()?.let { id ->
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
                                    item.id.toLongOrNull()?.let { id ->
                                        ZeroTalkClientManager.deleteMoment(
                                            momentId = id,
                                            onSuccess = {
                                                notificationState.show("动态已删除")
                                            },
                                            onError = { notificationState.show(it) }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // ---- 顶部悬浮控制栏：三大 Tab 胶囊 + 右侧微型最新/最热切换胶囊 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 三大分类滑动胶囊
            ZeroTalkBottomTabs(
                selectedTabIndex = { pagerState.currentPage },
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                tabsCount = categories.size,
                backdrop = momentsBackdrop,
                outerHeight = 38.dp,
                innerHeight = 32.dp,
                modifier = Modifier.padding(start = 16.dp).width(200.dp),
                isDark = isDark
            ) {
                categories.forEachIndexed { index, cat ->
                    val isSelected = pagerState.currentPage == index
                    ZeroTalkBottomTab(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }) {
                        BasicText(
                            text = cat.title,
                            style = TextStyle(
                                fontFamily = AppleHigTypography.defaultFontFamily,
                                color = if (isSelected) {
                                    if (isDark) Color(0xFF60A5FA) else Color(0xFF007AFF)
                                } else {
                                    if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
                                },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                platformStyle = AppleHigTypography.defaultPlatformStyle,
                                lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                            )
                        )
                    }
                }
            }

            // 右侧「最新 / 最热」玻璃切换胶囊
            MomentsSortCapsule(
                currentSort = currentSort,
                onSortChange = { newSort ->
                    if (currentSort != newSort) {
                        currentSort = newSort
                        val cat = categories[pagerState.currentPage]
                        ZeroTalkClientManager.fetchMomentsByCategory(category = cat, sort = newSort)
                    }
                },
                backdrop = momentsBackdrop,
                isDark = isDark
            )
        }

        // ---- 右下角 Apple HIG 磨砂悬浮「+ 发布动态」按钮 (LiquidButton 采样 momentsBackdrop 强光学折射) ----
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp)
        ) {
            LiquidButton(
                onClick = { isPublishSheetVisible = true },
                backdrop = momentsBackdrop,
                isDark = isDark,
                surfaceColor = if (isDark) Color.White else Color.Black,
                surfaceAlpha = 0.08f,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "发布动态",
                    tint = higColors.label,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // ---- 统一评论抽屉与分享面板承载器 ----
        MomentSheetsHost(
            commentTarget = commentMomentTarget,
            shareTarget = shareMomentTarget,
            isDark = isDark,
            backdrop = sheetBackdrop ?: backdrop,
            onDismissComment = { commentMomentTarget = null },
            onDismissShare = { shareMomentTarget = null }
        )

        // ---- 举报动态弹窗（官方六项理由 + 选填说明）----
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

        // ---- 发布动态弹窗 (AppleModalBottomSheet)：与「捞动态」共用全站唯一的 MomentComposer ----
        if (isPublishSheetVisible) {
            AppleModalBottomSheet(
                onDismissRequest = { isPublishSheetVisible = false },
                backdrop = sheetBackdrop ?: backdrop,
                title = "发布动态",
                leadingAction = SheetAction.Close { isPublishSheetVisible = false },
                isDark = isDark
            ) {
                MomentComposer(
                    isDark = isDark,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    backdrop = sheetBackdrop ?: backdrop,
                    onDismiss = { isPublishSheetVisible = false },
                    onPublished = {
                        isPublishSheetVisible = false
                        notificationState.show("动态发布成功！")
                        val cat = categories[pagerState.currentPage]
                        ZeroTalkClientManager.fetchMomentsByCategory(category = cat, sort = currentSort)
                    }
                )
            }
        }
    }
}

/**
 * 液态毛玻璃「最新 / 最热」双项滑动胶囊
 *
 * 同时供动态主界面（顶部悬浮控制栏）与他人主页动态区复用。
 */
@Composable
internal fun MomentsSortCapsule(
    currentSort: MomentsSort,
    onSortChange: (MomentsSort) -> Unit,
    backdrop: Backdrop? = null,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val sorts = remember { listOf(MomentsSort.LATEST, MomentsSort.HOT) }

    ZeroTalkBottomTabs(
        selectedTabIndex = { if (currentSort == MomentsSort.LATEST) 0 else 1 },
        onTabSelected = { index ->
            val targetSort = sorts.getOrElse(index) { MomentsSort.LATEST }
            onSortChange(targetSort)
        },
        tabsCount = sorts.size,
        backdrop = backdrop,
        outerHeight = 38.dp,
        innerHeight = 32.dp,
        modifier = modifier.width(108.dp),
        isDark = isDark
    ) {
        sorts.forEachIndexed { index, sort ->
            val isSelected = currentSort == sort
            ZeroTalkBottomTab(onClick = {
                onSortChange(sort)
            }) {
                BasicText(
                    text = sort.title,
                    style = TextStyle(
                        fontFamily = AppleHigTypography.defaultFontFamily,
                        color = if (isSelected) {
                            if (isDark) Color(0xFF60A5FA) else Color(0xFF007AFF)
                        } else {
                            if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        platformStyle = AppleHigTypography.defaultPlatformStyle,
                        lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                    )
                )
            }
        }
    }
}

/**
 * 动态列表单张卡片组件
 *
 * 同时供动态主界面（精选 / 关注 / 我的）与他人主页动态区复用：
 * 自带头像 / 昵称 / 性别 / 时间头部、正文、九宫格配图与「点赞 · 评论 · 分享」动作栏；
 * 置顶 / 可见性 / 删除管理菜单仅在 `moment.isMine` 时出现。
 */
@Composable
internal fun MomentCard(
    moment: MomentItem,
    isDark: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onFollowClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onPinToggle: (Boolean) -> Unit,
    onVisibilityToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    /** 是否展示作者性别微标签（资料页里性别已在资料卡展示，避免每条动态重复） */
    showGender: Boolean = true,
    /** 是否展示头像 / 昵称（资料页动态合并进一张卡片时关闭） */
    showAuthor: Boolean = true,
    /** 内嵌模式：不再自带卡片背景与圆角，由外层大卡片提供 */
    embedded: Boolean = false
) {
    val higColors = AppleHigColors.colors(isDark)
    var isMenuOpen by remember { mutableStateOf(false) }

    val cardBg = higColors.systemFill
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (embedded) 0.dp else horizontalPadding)
            .then(if (embedded) Modifier else Modifier.clip(cardShape).background(cardBg))
            .padding(16.dp)
    ) {
        // ---- 头部：头像、昵称、性别标签、时间、状态徽章、菜单 ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形头像：点击打开作者个人资料（非本人时）；资料页合并卡片时隐藏
            if (showAuthor) {
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !moment.isMine && (moment.authorUid.isNotBlank() || moment.authorId.isNotBlank()),
                        onClick = {
                            ZeroTalkClientManager.openOtherUserProfile(moment.authorId, moment.authorUid)
                        }
                    )
                ) {
                    UserAvatar(
                        url = moment.authorAvatar,
                        name = moment.authorName,
                        size = 40.dp,
                        gradient = if (moment.authorGender == "女") {
                            listOf(Color(0xFFF472B6), Color(0xFFFB7185))
                        } else {
                            listOf(Color(0xFF60A5FA), Color(0xFF818CF8))
                        },
                        fallbackTextStyle = AppleHigTypography.headline.copy(fontSize = 16.sp),
                        fallbackIconSize = 20.dp
                    )
                }

                Spacer(Modifier.width(10.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = showAuthor && !moment.isMine && (moment.authorUid.isNotBlank() || moment.authorId.isNotBlank()),
                        onClick = {
                            ZeroTalkClientManager.openOtherUserProfile(moment.authorId, moment.authorUid)
                        }
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showAuthor) {
                        BasicText(
                            text = moment.authorName.ifBlank { "零语匿友" },
                            style = AppleHigTypography.subhead.copy(
                                color = higColors.label,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    if (showGender && moment.authorGender.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))

                        // 性别指示微标签
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (moment.authorGender == "女") Color(0xFFF472B6).copy(alpha = 0.15f)
                                    else Color(0xFF60A5FA).copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            BasicText(
                                text = moment.authorGender,
                                style = AppleHigTypography.caption2.copy(
                                    color = if (moment.authorGender == "女") Color(0xFFF472B6) else Color(0xFF60A5FA),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // 置顶微标签
                    if (moment.isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            BasicText(
                                text = "置顶",
                                style = AppleHigTypography.caption2.copy(
                                    color = Color(0xFFF59E0B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // 私密微标签
                    if (moment.isPrivate) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF6B7280).copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            BasicText(
                                text = "仅自己可见",
                                style = AppleHigTypography.caption2.copy(
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                BasicText(
                    text = if (moment.fishedAt.isNotBlank()) "${moment.publishTime} · 捞于 ${moment.fishedAt}" else moment.publishTime,
                    style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
                )
            }

            // 自身动态管理菜单触发按钮 / 访客操作区（举报 + 关注）
            if (moment.isMine) {
                Box {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "更多操作",
                        tint = higColors.secondaryLabel,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isMenuOpen = !isMenuOpen }
                            )
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onReportClick != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFFEF4444).copy(alpha = 0.16f)
                                    else Color(0xFFEF4444).copy(alpha = 0.10f)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onReportClick
                                )
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = "举报",
                                style = AppleHigTypography.caption1.copy(
                                    color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    if (onFollowClick != null) {
                        FollowCapsuleButton(
                            isFollowing = moment.isFollowed,
                            isDark = isDark,
                            onClick = onFollowClick
                        )
                    }
                }
            }
        }

        // 作者管理下拉抽屉
        AnimatedVisibility(
            visible = isMenuOpen && moment.isMine,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(higColors.secondarySystemBackground)
                    .border(0.5.dp, higColors.separator, RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp)
            ) {
                // 置顶/取消置顶
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isMenuOpen = false
                            onPinToggle(!moment.isPinned)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = if (moment.isPinned) "取消置顶" else "置顶动态",
                        style = AppleHigTypography.footnote.copy(color = higColors.label)
                    )
                }

                // 可见性切换
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isMenuOpen = false
                            onVisibilityToggle(!moment.isPrivate)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (moment.isPrivate) Icons.Default.Public else Icons.Default.Lock,
                        contentDescription = null,
                        tint = higColors.tint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = if (moment.isPrivate) "设为公开" else "设为仅自己可见",
                        style = AppleHigTypography.footnote.copy(color = higColors.label)
                    )
                }

                // 删除
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isMenuOpen = false
                            onDelete()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = "删除动态",
                        style = AppleHigTypography.footnote.copy(color = Color(0xFFFF3B30))
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- 动态正文 ----
        if (moment.textContent.isNotBlank()) {
            BasicText(
                text = moment.textContent,
                style = AppleHigTypography.body.copy(
                    color = higColors.label,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            )
        }

        // ---- 动态语音（audio_url）----
        if (moment.audioUrl.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            MomentVoiceBubble(audioUrl = moment.audioUrl, isDark = isDark)
        }

        // ---- 动态音乐（music 对象，nm-card）----
        val momentMusic = moment.music
        if (momentMusic != null) {
            Spacer(Modifier.height(10.dp))
            MomentMusicCard(music = momentMusic, isDark = isDark)
        }

        // 动态图片（本地资源或远端多图自适应网格）
        if (moment.localImageRes != null) {
            Spacer(Modifier.height(10.dp))
            Image(
                painter = painterResource(moment.localImageRes),
                contentDescription = "动态图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        } else {
            val imageViewer = LocalImageViewer.current
            val displayImages = remember(moment.images, moment.imageUrl) {
                if (moment.images.isNotEmpty()) {
                    moment.images
                } else if (moment.imageUrl.isNotBlank()) {
                    listOf(moment.imageUrl)
                } else {
                    emptyList()
                }
            }

            if (displayImages.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                MomentImagesGrid(
                    images = displayImages,
                    onImageClick = { index ->
                        imageViewer.open(displayImages, index)
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        AppleHigDivider(isDark = isDark)
        Spacer(Modifier.height(10.dp))

        // ---- 底部动作栏：点赞、评论、分享 ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 喜欢/点赞
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLikeClick
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "点赞",
                    tint = if (moment.isLiked) Color(0xFFFF2D55) else higColors.secondaryLabel,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                BasicText(
                    text = if (moment.likesCount > 0) "${moment.likesCount}" else "点赞",
                    style = AppleHigTypography.subhead.copy(
                        color = if (moment.isLiked) Color(0xFFFF2D55) else higColors.secondaryLabel,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // 评论
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCommentClick
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "评论",
                    tint = higColors.secondaryLabel,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                BasicText(
                    text = if (moment.commentsCount > 0) "${moment.commentsCount}" else "评论",
                    style = AppleHigTypography.subhead.copy(
                        color = higColors.secondaryLabel,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // 分享
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onShareClick
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "分享",
                    tint = higColors.secondaryLabel,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                BasicText(
                    text = "分享",
                    style = AppleHigTypography.subhead.copy(
                        color = higColors.secondaryLabel,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * 单张动态图片：按原比例自适应展示（限制最大宽 240dp、最大高 260dp，最小 90dp）
 */
@Composable
private fun SingleMomentImage(
    url: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }

    LaunchedEffect(url) {
        if (url.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        bitmap = NetworkImageLoader.loadImage(url)
        isLoading = false
    }

    val (targetWidth, targetHeight) = remember(bitmap) {
        if (bitmap == null) {
            Pair(200.dp, 180.dp)
        } else {
            val bw = bitmap!!.width.toFloat()
            val bh = bitmap!!.height.toFloat()
            val ratio = if (bh > 0f) bw / bh else 1f

            val maxW = 240f
            val maxH = 260f
            val minDim = 90f

            val w: Float
            val h: Float
            if (ratio >= 1f) {
                w = maxW
                h = (maxW / ratio).coerceIn(minDim, maxH)
            } else {
                h = maxH
                w = (maxH * ratio).coerceIn(minDim, maxW)
            }
            Pair(w.dp, h.dp)
        }
    }

    Box(
        modifier = modifier
            .size(targetWidth, targetHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Gray.copy(alpha = 0.12f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "动态图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF007AFF).copy(alpha = 0.6f)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "动态图片",
                tint = Color.Gray.copy(alpha = 0.35f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * 动态九宫格/自适应图片网格组件
 */
@Composable
private fun MomentImagesGrid(
    images: List<String>,
    modifier: Modifier = Modifier,
    onImageClick: (Int) -> Unit
) {
    when (images.size) {
        1 -> {
            SingleMomentImage(
                url = images[0],
                modifier = modifier,
                onClick = { onImageClick(0) }
            )
        }
        2 -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                images.forEachIndexed { index, url ->
                    AsyncNetworkImage(
                        url = url,
                        contentDescription = "动态图片",
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onImageClick(index) }
                            )
                    )
                }
            }
        }
        3 -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                images.forEachIndexed { index, url ->
                    AsyncNetworkImage(
                        url = url,
                        contentDescription = "动态图片",
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onImageClick(index) }
                            )
                    )
                }
            }
        }
        4 -> {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                images.chunked(2).forEachIndexed { rowIndex, rowImages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowImages.forEachIndexed { colIndex, url ->
                            val index = rowIndex * 2 + colIndex
                            AsyncNetworkImage(
                                url = url,
                                contentDescription = "动态图片",
                                contentScale = ContentScale.Crop,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onImageClick(index) }
                                    )
                            )
                        }
                    }
                }
            }
        }
        else -> {
            val displayedImages = images.take(9)
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                displayedImages.chunked(3).forEachIndexed { rowIndex, rowImages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowImages.forEachIndexed { colIndex, url ->
                            val index = rowIndex * 3 + colIndex
                            AsyncNetworkImage(
                                url = url,
                                contentDescription = "动态图片",
                                contentScale = ContentScale.Crop,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onImageClick(index) }
                                    )
                            )
                        }
                        repeat(3 - rowImages.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 动态列表空状态插画与提示
 */
@Composable
private fun EmptyMomentsView(
    category: MomentsCategory,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    val tip = when (category) {
        MomentsCategory.FEATURED -> "暂无精选动态，来发布第一条吧~"
        MomentsCategory.FOLLOWING -> "你关注的好友还没有发布动态哦~"
        MomentsCategory.MINE -> "你还没有发布过动态，点击右下角写下美好瞬间吧~"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(higColors.secondarySystemBackground),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "✨",
                    style = TextStyle(fontSize = 32.sp)
                )
            }

            Spacer(Modifier.height(14.dp))

            BasicText(
                text = tip,
                style = AppleHigTypography.subhead.copy(
                    color = higColors.secondaryLabel,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
