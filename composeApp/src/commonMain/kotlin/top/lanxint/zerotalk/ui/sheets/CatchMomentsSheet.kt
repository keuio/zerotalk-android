package top.lanxint.zerotalk.ui.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.LiquidButton
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.LocalSheetTopBarClaim
import top.lanxint.zerotalk.ui.components.MomentComposer
import top.lanxint.zerotalk.ui.messages.UserReportDialog
import top.lanxint.zerotalk.ui.moments.MomentCard
import top.lanxint.zerotalk.ui.moments.MomentCommentSheet
import top.lanxint.zerotalk.ui.moments.MomentShareSheet
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.ui.utils.BackHandler
import com.kashif_e.backdrop.Backdrop

@Composable
fun SheetCatchMomentsContent(
    isDark: Boolean,
    backdrop: Backdrop,
    /** 内容层已无内部子页面可关时，交回宿主关闭整个 Sheet */
    onRequestClose: () -> Unit = {}
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current

    // 捞到的动态与捞取状态（POST /moment/fish，全局调度管理器持有）
    val fishedMoment by ZeroTalkClientManager.fishedMoment.collectAsState()
    val isFishing by ZeroTalkClientManager.isFishing.collectAsState()

    var commentMomentTarget by remember { mutableStateOf<MomentItem?>(null) }
    var shareMomentTarget by remember { mutableStateOf<MomentItem?>(null) }

    // 发布动态面板开关（表单状态、相册选图与提交逻辑全部内聚在 MomentComposer 中）
    var isPublishVisible by remember { mutableStateOf(false) }

    // 评论 / 分享子页面接管容器顶栏：外层「捞动态」标题栏让位，
    // 由子页面顶栏在完全相同的位置直接切换为「评论 / 分享」+ 返回 ←
    val topBarClaim = LocalSheetTopBarClaim.current
    val isSubPageVisible = commentMomentTarget != null || shareMomentTarget != null
    SideEffect {
        if (isSubPageVisible) topBarClaim?.claim() else topBarClaim?.release()
    }

    // 打开面板即捞一条（与官方 web 端行为一致）
    LaunchedEffect(Unit) {
        ZeroTalkClientManager.fishMoment(
            onEmpty = { notificationState.show("暂时没有可捞的新动态了") },
            onError = { notificationState.show(it) }
        )
    }

    val currentMoment = fishedMoment

    // 举报动态：官方 POST /moment/report（moment_id + reason + description）
    var reportTarget by remember(currentMoment?.id) { mutableStateOf<MomentItem?>(null) }
    var reportSubmitting by remember { mutableStateOf(false) }

    // 本 Sheet 内容的返回总入口：先关内部子页面（评论 / 分享 / 发布卡片），没有子页面时交回宿主关闭整个 Sheet。
    // 始终 enabled —— 不再依赖「后注册优先」的组合顺序与宿主抢返回事件（宿主用 sheetContentHandlesBack 显式让位）
    BackHandler(enabled = true) {
        when {
            commentMomentTarget != null -> commentMomentTarget = null
            shareMomentTarget != null -> shareMomentTarget = null
            isPublishVisible -> isPublishVisible = false
            else -> onRequestClose()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            shareMomentTarget != null -> {
                MomentShareSheet(
                    moment = shareMomentTarget!!,
                    isDark = isDark,
                    isSubPage = true,
                    onDismiss = { shareMomentTarget = null }
                )
            }
            commentMomentTarget != null -> {
                MomentCommentSheet(
                    moment = commentMomentTarget!!,
                    isDark = isDark,
                    isSubPage = true,
                    onClose = { commentMomentTarget = null }
                )
            }
            else -> {
                // 举报动态弹窗（官方六项理由 + 选填说明）
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

                if (currentMoment != null) {
                    // 卡片内容切换动效
                    AnimatedContent(
                        targetState = currentMoment,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "CatchMomentContentTransition"
                    ) { moment ->
                        MomentCard(
                            moment = moment,
                            isDark = isDark,
                            horizontalPadding = 0.dp,
                            onLikeClick = {
                                ZeroTalkClientManager.toggleLikeMoment(moment.id) { err ->
                                    notificationState.show(err)
                                }
                            },
                            onCommentClick = {
                                commentMomentTarget = moment
                            },
                            onShareClick = {
                                shareMomentTarget = moment
                            },
                            onFollowClick = {
                                val following = moment.isFollowed
                                ZeroTalkClientManager.setMomentAuthorFollow(
                                    authorId = moment.authorId,
                                    authorUid = moment.authorUid,
                                    isFollowing = !following,
                                    onSuccess = {
                                        notificationState.show(if (following) "已取消关注" else "已关注 ${moment.authorName}")
                                    },
                                    onError = { notificationState.show(it) }
                                )
                            },
                            onReportClick = {
                                reportTarget = moment
                            },
                            onPinToggle = { isPinned ->
                                moment.id.toLongOrNull()?.let { id ->
                                    ZeroTalkClientManager.setMomentPin(
                                        momentId = id,
                                        isPinned = isPinned,
                                        onSuccess = { notificationState.show(if (isPinned) "动态已置顶" else "已取消置顶") },
                                        onError = { notificationState.show(it) }
                                    )
                                }
                            },
                            onVisibilityToggle = { isPrivate ->
                                moment.id.toLongOrNull()?.let { id ->
                                    ZeroTalkClientManager.setMomentVisibility(
                                        momentId = id,
                                        isPrivate = isPrivate,
                                        onSuccess = { notificationState.show(if (isPrivate) "已设为仅自己可见" else "已设为公开") },
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
                    }
                } else {
                    // 暂无动态时的空状态卡片
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(higColors.systemFill)
                            .padding(vertical = 44.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF222630) else Color(0xFFE8EDF5)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    text = "🎣",
                                    style = TextStyle(fontSize = 26.sp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            BasicText(
                                text = if (isFishing) "正在打捞..." else "暂时没有可捞的动态",
                                style = AppleHigTypography.headline.copy(
                                    color = higColors.label,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(Modifier.height(6.dp))
                            BasicText(
                                text = if (isFishing) "正在为你捞取一条新的心声~"
                                else "点击下方「换一条」再捞一次，或发布自己的动态",
                                style = AppleHigTypography.footnote.copy(
                                    color = higColors.secondaryLabel
                                )
                            )
                        }
                    }
                }

                // 发布动态面板：与「动态」页共用全站唯一的 MomentComposer 发布组件
                AnimatedVisibility(
                    visible = isPublishVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(higColors.systemFill)
                            .padding(14.dp)
                    ) {
                        MomentComposer(
                            isDark = isDark,
                            title = "发布新动态",
                            backdrop = backdrop,
                            showTopBar = true,
                            onDismiss = { isPublishVisible = false },
                            onPublished = {
                                isPublishVisible = false
                                notificationState.show("动态发布成功")
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 5. 换一条捞捞看 / 发动态 双主操作栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidButton(
                        onClick = {
                            if (isFishing) return@LiquidButton
                            // 「换一条」= 重新捞一条：POST /moment/fish
                            ZeroTalkClientManager.fishMoment(
                                onEmpty = { notificationState.show("暂时没有可捞的新动态了") },
                                onError = { notificationState.show(it) }
                            )
                        },
                        backdrop = backdrop,
                        surfaceColor = if (isDark) Color.White.copy(0.12f) else Color.White.copy(0.40f),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        if (isFishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = higColors.tint,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = higColors.tint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        BasicText(
                            text = if (isFishing) "捞取中..." else "换一条",
                            style = AppleHigTypography.subhead.copy(
                                color = higColors.tint,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    LiquidButton(
                        onClick = {
                            isPublishVisible = !isPublishVisible
                        },
                        backdrop = backdrop,
                        surfaceColor = if (isDark) Color.White.copy(0.12f) else Color.White.copy(0.40f),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = higColors.tint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        BasicText(
                            text = "发动态",
                            style = AppleHigTypography.subhead.copy(
                                color = higColors.tint,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}
