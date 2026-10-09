package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.LocalSheetTopBarClaim
import top.lanxint.zerotalk.ui.messages.UserReportDialog
import top.lanxint.zerotalk.ui.moments.MomentCard
import top.lanxint.zerotalk.ui.moments.MomentCommentSheet
import top.lanxint.zerotalk.ui.moments.MomentShareSheet
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.ui.utils.BackHandler

/**
 * 「捞取记录」面板内容
 *
 * 数据来源：`GET /api/moment/fish-history`（分页返回 list / has_more / next_before_id），
 * 每次捞取都会由服务端记入该列表。
 */
@Composable
fun SheetCatchHistoryContent(
    isDark: Boolean,
    /** 内容层已无内部子页面可关时，交回宿主关闭整个 Sheet */
    onRequestClose: () -> Unit = {}
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current

    val records by ZeroTalkClientManager.fishHistory.collectAsState()
    val hasMore by ZeroTalkClientManager.fishHistoryHasMore.collectAsState()
    val isLoading by ZeroTalkClientManager.isFishHistoryLoading.collectAsState()
    var commentsMoment by remember { mutableStateOf<MomentItem?>(null) }
    var shareMoment by remember { mutableStateOf<MomentItem?>(null) }
    // 举报动态目标与提交中标记
    var reportMoment by remember { mutableStateOf<MomentItem?>(null) }
    var reportSubmitting by remember { mutableStateOf(false) }

    // 评论 / 分享子页面接管容器顶栏：外层「捞取历史记录」标题栏让位，
    // 由子页面顶栏在完全相同的位置直接切换为「评论 / 分享」+ 返回 ←
    val topBarClaim = LocalSheetTopBarClaim.current
    val isSubPageVisible = commentsMoment != null || shareMoment != null
    SideEffect {
        if (isSubPageVisible) topBarClaim?.claim() else topBarClaim?.release()
    }

    // 打开面板即拉取最新一页捞取记录
    LaunchedEffect(Unit) {
        ZeroTalkClientManager.fetchFishHistory(
            reset = true,
            onError = { notificationState.show(it) }
        )
    }

    val listState = rememberLazyListState()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val fixedContentHeight = screenHeight * 0.75f

    // 本 Sheet 内容的返回总入口：先关内部子页面（评论 / 分享），没有子页面时交回宿主关闭整个 Sheet。
    // 始终 enabled —— 不再依赖「后注册优先」的组合顺序与宿主抢返回事件（宿主用 sheetContentHandlesBack 显式让位）
    BackHandler(enabled = true) {
        when {
            commentsMoment != null -> commentsMoment = null
            shareMoment != null -> shareMoment = null
            else -> onRequestClose()
        }
    }

    // 历史列表用固定 75% 高度保持分页浏览的稳定观感；评论 / 分享子页面则按自身高度撑开，
    // 这样底部回复输入框才会贴在弹窗底边，而不是悬在固定高度容器中间
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isSubPageVisible) Modifier else Modifier.height(fixedContentHeight))
    ) {
        when {
            commentsMoment != null -> MomentCommentSheet(
                moment = commentsMoment!!,
                isDark = isDark,
                isSubPage = true,
                onClose = { commentsMoment = null }
            )
            shareMoment != null -> MomentShareSheet(
                moment = shareMoment!!,
                isDark = isDark,
                isSubPage = true,
                onDismiss = { shareMoment = null }
            )
            records.isEmpty() && !isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(if (isDark) Color(0xFF222630) else Color(0xFFE8EDF5)),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = "📜",
                                style = TextStyle(fontSize = 28.sp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        BasicText(
                            text = "暂无打捞历史记录",
                            style = AppleHigTypography.headline.copy(
                                color = higColors.label,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.height(6.dp))
                        BasicText(
                            text = "在首页「捞动态」中遇见的心声将汇聚于此",
                            style = AppleHigTypography.footnote.copy(
                                color = higColors.secondaryLabel
                            )
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = records, key = { it.id }) { record ->
                        MomentCard(
                            moment = record,
                            isDark = isDark,
                            horizontalPadding = 0.dp,
                            onLikeClick = {
                                ZeroTalkClientManager.toggleLikeMoment(record.id) { err ->
                                    notificationState.show(err)
                                }
                            },
                            onCommentClick = { commentsMoment = record },
                            onShareClick = { shareMoment = record },
                            onReportClick = { reportMoment = record },
                            onFollowClick = {
                                val following = record.isFollowed
                                ZeroTalkClientManager.setMomentAuthorFollow(
                                    authorId = record.authorId,
                                    authorUid = record.authorUid,
                                    isFollowing = !following,
                                    onSuccess = {
                                        notificationState.show(if (following) "已取消关注" else "已关注 ${record.authorName}")
                                    },
                                    onError = { notificationState.show(it) }
                                )
                            },
                            onPinToggle = {},
                            onVisibilityToggle = {},
                            onDelete = {}
                        )
                    }

                    item {
                        if (hasMore) {
                            LaunchedEffect(records.size) {
                                ZeroTalkClientManager.fetchFishHistory(
                                    reset = false,
                                    onError = { notificationState.show(it) }
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasMore || isLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = higColors.tertiaryLabel,
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    BasicText(
                                        text = "正在捞取更多记录...",
                                        style = AppleHigTypography.footnote.copy(color = higColors.tertiaryLabel)
                                    )
                                }
                            } else {
                                BasicText(
                                    text = "已显示全部历史打捞记录 • 往期心声已封存",
                                    style = AppleHigTypography.footnote.copy(color = higColors.tertiaryLabel)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // 举报动态弹窗（官方六项理由 + 选填说明）
        reportMoment?.let { target ->
            UserReportDialog(
                userName = target.authorName,
                isDark = isDark,
                submitting = reportSubmitting,
                onDismiss = { if (!reportSubmitting) reportMoment = null },
                onSubmit = { reason, description ->
                    reportSubmitting = true
                    ZeroTalkClientManager.reportMoment(
                        momentId = target.id.toLongOrNull() ?: 0L,
                        reason = reason.value,
                        description = description
                    ) { success, err ->
                        reportSubmitting = false
                        if (success) {
                            reportMoment = null
                            notificationState.show("已提交举报，我们会尽快核实")
                        } else {
                            notificationState.show(err ?: "举报提交失败")
                        }
                    }
                }
            )
        }
    }
}

