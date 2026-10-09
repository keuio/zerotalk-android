package top.lanxint.zerotalk.ui.moments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.data.network.MomentCommentDto
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.components.SheetAction
import top.lanxint.zerotalk.ui.components.SheetTopBar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 二级回复目标元数据
 */
data class ReplyTargetInfo(
    val parentId: Long,
    val replyToUserId: String,
    val replyToUsername: String
)

/**
 * Apple HIG 风格独立动态评论抽屉组件
 *
 * 核心特性：
 * 1. 评论列表展示（支持主评论及嵌套二级回复树）；
 * 2. 评论精准点赞（支持跳动计数与变色动效）；
 * 3. 二级回复：点击任意评论右侧「回复」，输入栏上方滑出「回复 @用户」带关闭芯片，精准定向回复；
 * 4. 极致 Apple HIG 毛玻璃与圆角排版规范。
 *
 * 顶栏由本组件自行调用全站唯一的 `SheetTopBar` 绘制：评论数随内部列表状态实时变化，
 * 宿主容器无法代劳，因此承载它的 `AppleModalBottomSheet` 不传 `title`。
 *
 * @param isSubPage 是否为「捞动态 / 捞取历史」内替换外层内容的子页面；为 true 时前缘动作
 *   由关闭 × 变为返回 ←（返回外层内容），为 false 时为关闭 ×（关闭整个 Sheet）。
 */
@Composable
fun MomentCommentSheet(
    moment: MomentItem,
    isDark: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isSubPage: Boolean = false
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current
    val momentIdLong = moment.id.toLongOrNull() ?: 0L

    var commentsList by remember { mutableStateOf<List<MomentCommentDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<ReplyTargetInfo?>(null) }

    // 加载评论列表函数
    fun loadComments() {
        if (momentIdLong <= 0L) {
            isLoading = false
            return
        }
        isLoading = true
        ZeroTalkClientManager.fetchMomentComments(
            momentId = momentIdLong,
            onSuccess = { data ->
                commentsList = data.list
                isLoading = false
            },
            onError = {
                isLoading = false
            }
        )
    }

    LaunchedEffect(moment.id) {
        loadComments()
    }

    // 递归/深层更新评论点赞状态
    fun updateCommentLike(commentId: Long, isLiked: Boolean, count: Int) {
        fun updateItem(item: MomentCommentDto): MomentCommentDto {
            if (item.id == commentId) {
                return item.copy(liked = isLiked, likeCount = count)
            }
            if (item.replies.isNotEmpty()) {
                return item.copy(replies = item.replies.map { updateItem(it) })
            }
            return item
        }
        commentsList = commentsList.map { updateItem(it) }
    }

    // 底部安全区由承载它的 AppleModalBottomSheet 统一预留（弹窗底部透明浮空隙），此处不再重复让位
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // ---- 顶部标题与关闭按钮栏（全站唯一 SheetTopBar；子页面时前缘为返回 ←） ----
        val totalCommentsCount = commentsList.size + commentsList.sumOf { it.replies.size }
        SheetTopBar(
            isDark = isDark,
            modifier = Modifier.padding(bottom = 18.dp),
            title = "评论",
            leadingAction = if (isSubPage) {
                SheetAction.Back { onClose() }
            } else {
                SheetAction.Close { onClose() }
            },
            titleTrailing = {
                BasicText(
                    text = "$totalCommentsCount",
                    style = AppleHigTypography.subhead.copy(
                        color = higColors.secondaryLabel,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        )

        AppleHigDivider(isDark = isDark)

        // ---- 评论列表内容区 ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(340.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                        color = higColors.tint
                    )
                }
            } else if (commentsList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText(
                            text = "💬",
                            style = TextStyle(fontSize = 32.sp)
                        )
                        Spacer(Modifier.height(8.dp))
                        BasicText(
                            text = "暂无评论，留下第一条脚印吧~",
                            style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(commentsList, key = { it.id }) { rootComment ->
                        CommentItemView(
                            comment = rootComment,
                            isDark = isDark,
                            onReplyClick = { target ->
                                replyTarget = ReplyTargetInfo(
                                    parentId = rootComment.id,
                                    replyToUserId = target.uid,
                                    replyToUsername = target.username
                                )
                            },
                            onLikeClick = { target ->
                                val newLiked = !target.liked
                                val newCount = if (newLiked) target.likeCount + 1 else (target.likeCount - 1).coerceAtLeast(0)
                                updateCommentLike(target.id, newLiked, newCount)

                                ZeroTalkClientManager.toggleLikeComment(
                                    momentId = momentIdLong,
                                    commentId = target.id,
                                    onSuccess = { res ->
                                        updateCommentLike(target.id, res.liked, res.likeCount)
                                    },
                                    onError = { err ->
                                        // 失败回滚
                                        updateCommentLike(target.id, target.liked, target.likeCount)
                                        notificationState.show(err)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        AppleHigDivider(isDark = isDark)

        // ---- 底部发表/回复评论栏 ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(higColors.secondarySystemBackground.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 回复目标芯片（Reply Target Chip）
            AnimatedVisibility(
                visible = replyTarget != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                replyTarget?.let { target ->
                    Row(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(higColors.tint.copy(alpha = 0.12f))
                            .border(0.5.dp, higColors.tint.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            text = "回复 @${target.replyToUsername}",
                            style = AppleHigTypography.caption1.copy(
                                color = higColors.tint,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消回复",
                            tint = higColors.tint,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { replyTarget = null }
                                )
                        )
                    }
                }
            }

            // 输入行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.08f)
                        else Color.Black.copy(alpha = 0.05f)
                    )
                    .border(
                        0.5.dp,
                        higColors.separator.copy(alpha = 0.5f),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = AppleHigTypography.defaultFontFamily,
                        color = higColors.label,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(higColors.tint),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            BasicText(
                                text = if (replyTarget != null) "回复 @${replyTarget?.replyToUsername}..." else "说点什么吧...",
                                style = AppleHigTypography.subhead.copy(
                                    color = higColors.placeholderText,
                                    fontSize = 14.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(Modifier.width(8.dp))

                // 发送按钮
                val canSend = inputText.isNotBlank() && !isSending
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) higColors.tint
                            else higColors.secondaryLabel.copy(alpha = 0.25f)
                        )
                        .clickable(
                            enabled = canSend,
                            onClick = {
                                val contentToSend = inputText.trim()
                                if (contentToSend.isBlank()) return@clickable
                                isSending = true

                                ZeroTalkClientManager.sendMomentComment(
                                    momentId = momentIdLong,
                                    content = contentToSend,
                                    parentId = replyTarget?.parentId,
                                    replyToUserId = replyTarget?.replyToUserId,
                                    onSuccess = { data ->
                                        isSending = false
                                        inputText = ""
                                        replyTarget = null
                                        notificationState.show("回复已发布")
                                        loadComments()
                                    },
                                    onError = { err ->
                                        isSending = false
                                        notificationState.show(err)
                                    }
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "发送",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单条评论卡片项（含二级回复缩进树）
 */
@Composable
private fun CommentItemView(
    comment: MomentCommentDto,
    isDark: Boolean,
    onReplyClick: (MomentCommentDto) -> Unit,
    onLikeClick: (MomentCommentDto) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(modifier = Modifier.fillMaxWidth()) {
        // 主评论行
        CommentRow(
            comment = comment,
            isDark = isDark,
            isSubReply = false,
            onReplyClick = { onReplyClick(comment) },
            onLikeClick = { onLikeClick(comment) }
        )

        // 二级回复树嵌套展示
        if (comment.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 38.dp, top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.04f)
                        else Color.Black.copy(alpha = 0.03f)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comment.replies.forEach { subReply ->
                    CommentRow(
                        comment = subReply,
                        isDark = isDark,
                        isSubReply = true,
                        onReplyClick = { onReplyClick(subReply) },
                        onLikeClick = { onLikeClick(subReply) }
                    )
                }
            }
        }
    }
}

/**
 * 评论/回复内容行组件
 */
@Composable
private fun CommentRow(
    comment: MomentCommentDto,
    isDark: Boolean,
    isSubReply: Boolean,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val avatarSize = if (isSubReply) 22.dp else 28.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 头像：真实头像优先，失败回落渐变 + 首字母（avatar_fallback 为展示字符，不是图片地址）
        UserAvatar(
            url = comment.avatarUrl,
            name = comment.avatarFallback?.takeIf { it.isNotBlank() } ?: comment.username.ifBlank { "匿" },
            size = avatarSize,
            gradient = if (comment.gender == "female") {
                listOf(Color(0xFFF472B6), Color(0xFFFB7185))
            } else {
                listOf(Color(0xFF60A5FA), Color(0xFF818CF8))
            },
            fallbackTextStyle = AppleHigTypography.caption2.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (isSubReply) 10.sp else 12.sp
            ),
            fallbackIconSize = if (isSubReply) 12.dp else 15.dp
        )

        Spacer(Modifier.width(8.dp))

        // 右侧主体：昵称、回复标签、时间、文本、点赞与回复操作
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    BasicText(
                        text = comment.username.ifBlank { "零语匿友" },
                        style = AppleHigTypography.caption1.copy(
                            color = higColors.label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (isSubReply) 12.sp else 13.sp
                        )
                    )

                    // 如果是针对具体用户的回复
                    if (!comment.replyToUsername.isNullOrBlank()) {
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = "▶",
                            style = AppleHigTypography.caption2.copy(
                                color = higColors.tertiaryLabel,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicText(
                            text = "@${comment.replyToUsername}",
                            style = AppleHigTypography.caption1.copy(
                                color = higColors.tint,
                                fontWeight = FontWeight.Medium,
                                fontSize = if (isSubReply) 11.sp else 12.sp
                            )
                        )
                    }
                }

                // 时间
                val timeStr = if (comment.createdAt.length >= 16) {
                    comment.createdAt.substring(5, 16)
                } else comment.createdAt.ifBlank { "刚刚" }
                BasicText(
                    text = timeStr,
                    style = AppleHigTypography.caption2.copy(
                        color = higColors.tertiaryLabel,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(Modifier.height(3.dp))

            // 评论正文
            BasicText(
                text = comment.content,
                style = AppleHigTypography.footnote.copy(
                    color = higColors.label,
                    fontSize = if (isSubReply) 13.sp else 14.sp
                )
            )

            Spacer(Modifier.height(4.dp))

            // 底部操作区：回复按钮 + 点赞心形
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 回复动作
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onReplyClick
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "回复",
                        style = AppleHigTypography.caption2.copy(
                            color = higColors.secondaryLabel,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // 点赞心形与数字
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLikeClick
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "点赞",
                        tint = if (comment.liked) Color(0xFFFF2D55) else higColors.tertiaryLabel,
                        modifier = Modifier.size(13.dp)
                    )
                    if (comment.likeCount > 0) {
                        Spacer(Modifier.width(3.dp))
                        BasicText(
                            text = "${comment.likeCount}",
                            style = AppleHigTypography.caption2.copy(
                                color = if (comment.liked) Color(0xFFFF2D55) else higColors.tertiaryLabel,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
