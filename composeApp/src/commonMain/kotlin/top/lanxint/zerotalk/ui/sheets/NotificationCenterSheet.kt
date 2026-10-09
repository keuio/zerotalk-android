package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.data.model.NotificationCategory
import top.lanxint.zerotalk.data.model.NotificationItemDto
import top.lanxint.zerotalk.data.repository.NotificationCenterStore
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
import top.lanxint.zerotalk.ui.components.LiquidSegmentedControl
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 「我的」页面 · 通知中心面板类型
 *
 * 与 [ProfileSheetType] 同包同模块，直接实现该 sealed interface，
 * 因此无需改动 ProfileSheetType.kt 本身（该文件不在本次任务的可改清单内）。
 */
data object ProfileNotificationCenter : ProfileSheetType

// ============================================================
// 通知中心（对齐官方 NotificationsView）
//  1) 分类 Tab：全部 / 互动 / 账号 / 系统
//  2) 顶部「N 条未读」+「全部已读」
//  3) 卡片：类型图标 + 标签 + 标题 + 正文摘要 + 相对时间 + 未读圆点
//  4) 点击单条标记已读；底部「加载更多」
// ============================================================

/**
 * 通知中心内容（由 ZeroTalkApp 的 ProfileSheet 宿主承载，顶栏由 AppleModalBottomSheet 统一绘制）
 */
@Composable
fun SheetNotificationCenterContent(
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    val state by NotificationCenterStore.state.collectAsState()

    // 打开面板即拉第一页并刷新未读数（官方 mount 行为）
    LaunchedEffect(Unit) { NotificationCenterStore.open() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        // ---- 分类 Tab ----
        LiquidSegmentedControl(
            options = NotificationCategory.entries.map { it.label },
            selectedIndex = state.category.ordinal,
            onOptionSelect = { index ->
                NotificationCenterStore.selectCategory(NotificationCategory.entries[index])
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            isDark = isDark
        )

        Spacer(Modifier.height(12.dp))

        // ---- 工具栏：未读统计 + 全部已读 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = if (state.unreadCount > 0) "${state.unreadCount} 条未读" else "暂无未读",
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel),
                modifier = Modifier.weight(1f)
            )

            val canMarkAll = state.unreadCount > 0 && !state.isMarkingAllRead
            CapsuleGlassButton(
                onClick = { NotificationCenterStore.markAllRead() },
                enabled = canMarkAll,
                isDark = isDark
            ) {
                BasicText(
                    text = if (state.isMarkingAllRead) "处理中…" else "全部已读",
                    style = AppleHigTypography.footnote.copy(
                        color = if (canMarkAll) higColors.tint else higColors.tertiaryLabel,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            // ---- 首屏加载态 ----
            state.isLoading && state.items.isEmpty() -> {
                NotificationStateCard(
                    isDark = isDark,
                    icon = null,
                    title = "正在加载通知",
                    hint = "同步最新消息"
                )
            }

            // ---- 空态（区分「全部」与具体分类文案）----
            state.items.isEmpty() && state.errorMessage == null -> {
                NotificationStateCard(
                    isDark = isDark,
                    icon = Icons.Default.Notifications,
                    title = if (state.category == NotificationCategory.ALL) "暂无通知" else "该分类暂无通知",
                    hint = if (state.category == NotificationCategory.ALL) {
                        "有新的系统提醒或互动消息时，会显示在这里"
                    } else {
                        "可切换其他分类查看"
                    }
                )
            }

            // ---- 加载失败且无缓存 ----
            state.items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NotificationStateCard(
                        isDark = isDark,
                        icon = Icons.Default.Notifications,
                        title = "通知加载失败",
                        hint = state.errorMessage ?: "请稍后重试"
                    )
                    Spacer(Modifier.height(12.dp))
                    CapsuleGlassButton(
                        onClick = { NotificationCenterStore.refresh() },
                        isDark = isDark
                    ) {
                        BasicText(
                            text = "重新加载",
                            style = AppleHigTypography.subhead.copy(
                                color = higColors.tint,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ---- 列表 ----
            else -> {
                state.items.forEach { item ->
                    NotificationCard(
                        item = item,
                        isDark = isDark,
                        onClick = { NotificationCenterStore.markRead(item.id) }
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (state.hasMore) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CapsuleGlassButton(
                            onClick = { NotificationCenterStore.loadMore() },
                            enabled = !state.isLoadingMore,
                            isDark = isDark
                        ) {
                            BasicText(
                                text = if (state.isLoadingMore) "加载中…" else "加载更多",
                                style = AppleHigTypography.subhead.copy(
                                    color = if (state.isLoadingMore) higColors.tertiaryLabel else higColors.tint,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // 加载更多失败：轻量提示，不清空已有列表
                val error = state.errorMessage
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    BasicText(
                        text = error,
                        style = AppleHigTypography.caption1.copy(color = higColors.systemRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 单条通知卡片（对齐官方 notify-card）
 */
@Composable
private fun NotificationCard(
    item: NotificationItemDto,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val (icon, accent) = notificationVisual(item.type)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xFF1B1F2A) else Color(0xFFFFFFFF))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 类型图标
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = if (isDark) 0.22f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 标签 + 时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.label.isNotBlank()) {
                        NotificationTag(
                            text = item.label,
                            accent = accent,
                            isDark = isDark
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        text = formatNotificationTime(item.createdAt),
                        style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                    )
                }

                Spacer(Modifier.height(6.dp))

                // 未读圆点 + 标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    BasicText(
                        text = item.title,
                        style = AppleHigTypography.subhead.copy(
                            color = if (item.isRead) higColors.secondaryLabel else higColors.label,
                            fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 正文摘要
                if (item.body.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = item.body,
                        style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel),
                        maxLines = 3
                    )
                }
            }
        }
    }
}

/**
 * 通知标签胶囊（服务端下发 label）
 */
@Composable
private fun NotificationTag(
    text: String,
    accent: Color,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(accent.copy(alpha = if (isDark) 0.20f else 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        BasicText(
            text = text,
            style = AppleHigTypography.caption2.copy(
                color = accent,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * 加载 / 空态卡片（与「我的举报」等 Sheet 的空态视觉一致）
 */
@Composable
private fun NotificationStateCard(
    isDark: Boolean,
    icon: ImageVector?,
    title: String,
    hint: String
) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = higColors.tertiaryLabel,
                    modifier = Modifier.size(36.dp)
                )
            }
            BasicText(
                text = title,
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
            )
            BasicText(
                text = hint,
                style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
            )
        }
    }
}

/**
 * 通知类型 → 图标 / 主题色（覆盖官方 bundle 枚举的全部 type）
 */
private fun notificationVisual(type: String): Pair<ImageVector, Color> = when (type) {
    "chat_dm", "chat_match", "chat_reply", "chat_mention",
    "chat_private_reply", "chat_private_mention" ->
        Icons.Default.Chat to Color(0xFF3B82F6)

    "moment_comment", "moment_comment_reply" ->
        Icons.Default.Forum to Color(0xFF8B5CF6)

    "moment_featured" ->
        Icons.Default.Star to Color(0xFFF59E0B)

    "moment_following_new" ->
        Icons.Default.Favorite to Color(0xFFEC4899)

    "moment_share" ->
        Icons.Default.Share to Color(0xFF06B6D4)

    "user_followed" ->
        Icons.Default.Person to Color(0xFF10B981)

    "feedback_reply" ->
        Icons.Default.Feedback to Color(0xFF0EA5E9)

    "report_result" ->
        Icons.Default.ReportProblem to Color(0xFFF59E0B)

    "penalty_appeal_approved" ->
        Icons.Default.Gavel to Color(0xFF10B981)

    "penalty_appeal_rejected" ->
        Icons.Default.Gavel to Color(0xFFEF4444)

    "ban_public_room" ->
        Icons.Default.Block to Color(0xFFEF4444)

    else ->
        Icons.Default.Notifications to Color(0xFF6366F1)
}

/**
 * 相对时间文案（官方 NotificationsView 同款）：
 * 刚刚 / N 分钟前 / N 小时前 / N 天前 / 更早显示 yyyy-MM-dd HH:mm
 */
private fun formatNotificationTime(raw: String): String {
    if (raw.isBlank()) return ""
    val date = parseNotificationDate(raw) ?: return raw
    val diff = System.currentTimeMillis() - date.time
    val minuteMs = 60_000L
    val hourMs = 60 * minuteMs
    val dayMs = 24 * hourMs
    return when {
        diff < minuteMs -> "刚刚"
        diff < hourMs -> "${diff / minuteMs} 分钟前"
        diff < dayMs -> "${diff / hourMs} 小时前"
        diff < 7 * dayMs -> "${diff / dayMs} 天前"
        else -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
    }
}

private fun parseNotificationDate(raw: String): Date? {
    // 服务端可能下发 "2024-01-01 12:00:00" / ISO8601（带 T、毫秒、时区）
    val normalized = raw.trim()
        .replace('T', ' ')
        .substringBefore('.')
        .substringBefore('+')
        .trim()
    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy-MM-dd"
    )
    for (pattern in patterns) {
        try {
            return SimpleDateFormat(pattern, Locale.getDefault()).parse(normalized)
        } catch (_: Exception) {
            // 尝试下一个格式
        }
    }
    return null
}
