package top.lanxint.zerotalk.ui.moments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.SheetAction
import top.lanxint.zerotalk.ui.components.SheetTopBar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 分享面板页面阶段
 */
private enum class ShareSheetStep {
    OPTIONS,
    SELECT_ROOM
}

/**
 * Apple HIG 风格动态分享面板
 *
 * 核心特性：
 * 1. 复制链接：完全遵循官方前端格式生成 https://app.zerotalk.cn/app/moments/{id} 并复制至剪贴板；
 * 2. 分享到房间：前置安全可见性校验，展示会话房间列表，选定后自动构建官方卡片摘要并分发至对应房间；
 * 3. 极致细腻的 Apple HIG 毛玻璃与圆角微动效卡片设计。
 *
 * 顶栏由本组件自行调用全站唯一的 `SheetTopBar` 绘制：两步流程的标题与返回动作随内部步骤切换，
 * 宿主容器无法代劳，因此承载它的 `AppleModalBottomSheet` 不传 `title`。
 *
 * @param isSubPage 是否为「捞动态 / 捞取历史」内替换外层内容的子页面；为 true 时第一步前缘动作
 *   由关闭 × 变为返回 ←（返回外层内容），为 false 时为关闭 ×（关闭整个 Sheet）。
 */
@Composable
fun MomentShareSheet(
    moment: MomentItem,
    isDark: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isSubPage: Boolean = false
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current
    val clipboardManager = LocalClipboardManager.current
    val conversations by ZeroTalkClientManager.conversations.collectAsState()
    val hallRoomId by ZeroTalkClientManager.hallRoomId.collectAsState()

    var currentStep by remember { mutableStateOf(ShareSheetStep.OPTIONS) }

    val momentLink = remember(moment.id) {
        "https://app.zerotalk.cn/app/moments/${moment.id}"
    }

    // 格式化官方动态卡片分享文本
    fun buildOfficialShareText(): String {
        val excerpt = if (moment.textContent.length > 280) {
            "${moment.textContent.take(280)}…"
        } else moment.textContent.trim()
        val author = moment.authorName.ifBlank { "用户" }
        return if (excerpt.isNotBlank()) {
            "[动态] $author：$excerpt $momentLink"
        } else {
            "[动态] $author 的动态 $momentLink"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // ---- 顶部标题栏（全站唯一 SheetTopBar；标题随两步流程切换，子页面时前缘为返回 ←） ----
        SheetTopBar(
            isDark = isDark,
            modifier = Modifier.padding(bottom = 18.dp),
            title = when (currentStep) {
                ShareSheetStep.OPTIONS -> "分享动态"
                ShareSheetStep.SELECT_ROOM -> "选择分享房间"
            },
            leadingAction = when (currentStep) {
                ShareSheetStep.OPTIONS -> if (isSubPage) {
                    SheetAction.Back { onDismiss() }
                } else {
                    SheetAction.Close { onDismiss() }
                }
                ShareSheetStep.SELECT_ROOM -> SheetAction.Back { currentStep = ShareSheetStep.OPTIONS }
            }
        )

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState == ShareSheetStep.SELECT_ROOM) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "ShareStepTransition"
        ) { step ->
            when (step) {
                ShareSheetStep.OPTIONS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        BasicText(
                            text = "复制链接，或发到房间里以卡片展示",
                            style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                        )

                        Spacer(Modifier.height(18.dp))

                        // 选项一：复制链接
                        ShareActionItem(
                            icon = Icons.Default.ContentCopy,
                            iconGradient = listOf(Color(0xFF007AFF), Color(0xFF60A5FA)),
                            title = "复制链接",
                            subtitle = momentLink,
                            isDark = isDark,
                            onClick = {
                                clipboardManager.setText(AnnotatedString(momentLink))
                                notificationState.show("链接已复制")
                                onDismiss()
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        // 选项二：分享到房间
                        ShareActionItem(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            iconGradient = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA)),
                            title = "分享到房间",
                            subtitle = "以卡片形式发送到房间或大厅",
                            isDark = isDark,
                            hasArrow = true,
                            onClick = {
                                // 前置可见性校验：对标官方规范
                                if (moment.isPrivate || moment.audienceMutual ||
                                    moment.audienceMode == "allow" || moment.audienceMode == "deny"
                                ) {
                                    notificationState.show("设置了可见范围的动态无法分享到房间")
                                    return@ShareActionItem
                                }
                                currentStep = ShareSheetStep.SELECT_ROOM
                            }
                        )

                        Spacer(Modifier.height(10.dp))
                    }
                }

                ShareSheetStep.SELECT_ROOM -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        AppleHigDivider(isDark = isDark)

                        // 房间列表
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 公共大厅选项
                            item {
                                RoomTargetRow(
                                    title = "零语大厅",
                                    subtitle = "公共聊天室 · 在线匿友畅聊",
                                    badgeText = "公共",
                                    badgeColor = Color(0xFF10B981),
                                    icon = Icons.Default.Public,
                                    isDark = isDark,
                                    onClick = {
                                        val shareText = buildOfficialShareText()
                                        ZeroTalkClientManager.sendHallMessage(shareText)
                                        notificationState.show("动态卡片已发送")
                                        onDismiss()
                                    }
                                )
                            }

                            // 当前会话房间列表
                            items(conversations, key = { it.id }) { conv ->
                                RoomTargetRow(
                                    title = conv.targetName,
                                    subtitle = conv.lastMessage.ifBlank { "暂无最新消息" },
                                    badgeText = conv.category.title,
                                    badgeColor = when (conv.category.title) {
                                        "暗号" -> Color(0xFFF59E0B)
                                        "匹配" -> Color(0xFF8B5CF6)
                                        else -> Color(0xFF007AFF)
                                    },
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    isDark = isDark,
                                    onClick = {
                                        val shareText = buildOfficialShareText()
                                        ZeroTalkClientManager.sendRoomMessage(conv.id, shareText)
                                        notificationState.show("动态卡片已发送")
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分享动作项组件
 */
@Composable
private fun ShareActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconGradient: List<Color>,
    title: String,
    subtitle: String,
    isDark: Boolean,
    hasArrow: Boolean = false,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(higColors.secondarySystemBackground)
            .border(0.5.dp, higColors.separator.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(iconGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = title,
                style = AppleHigTypography.subhead.copy(
                    color = higColors.label,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = subtitle,
                style = AppleHigTypography.caption2.copy(
                    color = higColors.secondaryLabel
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (hasArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = higColors.tertiaryLabel,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 房间选择行卡片
 */
@Composable
private fun RoomTargetRow(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(higColors.secondarySystemBackground)
            .border(0.5.dp, higColors.separator.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = title,
                    style = AppleHigTypography.subhead.copy(
                        color = higColors.label,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    BasicText(
                        text = badgeText,
                        style = AppleHigTypography.caption2.copy(
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            BasicText(
                text = subtitle,
                style = AppleHigTypography.caption2.copy(
                    color = higColors.secondaryLabel
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
