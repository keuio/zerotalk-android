package top.lanxint.zerotalk.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.ChatMessage
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.SheetAction
import top.lanxint.zerotalk.ui.components.SheetTopBar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 消息多选转发面板 (ForwardMessageSheet)
 *
 * 长按消息气泡点击「转发」时呼出，交互与体验对标「分享动态卡片」：
 * 1. 顶栏显示当前已选房间数，支持一键完成；
 * 2. 上方展示待转发消息缩略摘要卡片；
 * 3. 房间列表支持零语大厅与全部已有会话多选；
 * 4. 底部支持批量确认转发并给出反馈。
 */
@Composable
fun ForwardMessageSheet(
    message: ChatMessage,
    isDark: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current
    val conversations by ZeroTalkClientManager.conversations.collectAsState()

    var selectedRoomIds by remember { mutableStateOf(emptySet<String>()) }

    val messageSummary = remember(message) {
        when {
            message.isDeleted -> "已撤回消息"
            message.isImage -> "[图片]"
            message.isMomentShare -> {
                val share = message.momentShare
                if (share != null) "[动态] ${share.username}: ${share.excerpt}" else "[动态卡片]"
            }
            message.isMusicPlaylist -> {
                val pl = message.musicPlaylist
                if (pl != null) "[歌单] ${pl.name}" else "[网易云歌单]"
            }
            message.isMusic -> {
                val music = message.musicData
                if (music != null) "[音乐] ${music.name} - ${music.artists}" else "[音乐]"
            }
            message.isSticker -> "[表情包]"
            message.isVoice -> "[语音] ${message.voiceDurationSec}″"
            else -> message.content
        }
    }

    fun dispatchForward() {
        if (selectedRoomIds.isEmpty()) return
        val count = selectedRoomIds.size
        selectedRoomIds.forEach { targetId ->
            ZeroTalkClientManager.forwardMessage(message, targetId)
        }
        notificationState.show(if (count > 1) "消息已转发至 $count 个房间" else "消息已转发")
        onDismiss()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // 顶部标题栏
        SheetTopBar(
            isDark = isDark,
            modifier = Modifier.padding(bottom = 12.dp),
            title = if (selectedRoomIds.isNotEmpty()) "选择转发房间 (${selectedRoomIds.size})" else "转发消息",
            leadingAction = SheetAction.Close { onDismiss() },
            trailingAction = SheetAction.Done(
                enabled = selectedRoomIds.isNotEmpty(),
                onClick = { dispatchForward() }
            )
        )

        // 待转发消息摘要卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(higColors.secondarySystemBackground)
                .border(0.5.dp, higColors.separator.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(higColors.tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    val previewIcon = when {
                        message.isImage -> Icons.Default.Image
                        message.isMomentShare -> Icons.Default.Share
                        message.isMusic || message.isMusicPlaylist -> Icons.Default.MusicNote
                        else -> Icons.Default.FormatQuote
                    }
                    Icon(
                        imageVector = previewIcon,
                        contentDescription = null,
                        tint = higColors.tint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val senderPrefix = if (message.isMine) "我" else message.senderName.ifBlank { "对方" }
                    BasicText(
                        text = "$senderPrefix: $messageSummary",
                        style = AppleHigTypography.subhead.copy(
                            color = higColors.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        AppleHigDivider(isDark = isDark)

        // 目标房间列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 公共大厅选项
            item {
                val isHallSelected = selectedRoomIds.contains("hall")
                ForwardRoomTargetRow(
                    title = "零语大厅",
                    subtitle = "公共聊天室 · 在线匿友畅聊",
                    badgeText = "公共",
                    badgeColor = Color(0xFF10B981),
                    icon = Icons.Default.Public,
                    isSelected = isHallSelected,
                    isDark = isDark,
                    onClick = {
                        selectedRoomIds = if (isHallSelected) {
                            selectedRoomIds - "hall"
                        } else {
                            selectedRoomIds + "hall"
                        }
                    }
                )
            }

            // 当前会话房间列表
            items(conversations, key = { it.id }) { conv ->
                val isSelected = selectedRoomIds.contains(conv.id)
                ForwardRoomTargetRow(
                    title = conv.targetName,
                    subtitle = conv.lastMessage.ifBlank { "暂无最新消息" },
                    badgeText = conv.category.title,
                    badgeColor = when (conv.category.title) {
                        "暗号" -> Color(0xFFF59E0B)
                        "匹配" -> Color(0xFF8B5CF6)
                        else -> Color(0xFF007AFF)
                    },
                    icon = Icons.AutoMirrored.Filled.Chat,
                    isSelected = isSelected,
                    isDark = isDark,
                    onClick = {
                        selectedRoomIds = if (isSelected) {
                            selectedRoomIds - conv.id
                        } else {
                            selectedRoomIds + conv.id
                        }
                    }
                )
            }
        }

        // 底部操作区：已选数量与确认发送按钮
        AnimatedVisibility(
            visible = selectedRoomIds.isNotEmpty(),
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(higColors.tint)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { dispatchForward() }
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "确认转发到 ${selectedRoomIds.size} 个房间",
                    style = AppleHigTypography.body.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/**
 * 房间选择行卡片（HIG 风格）
 */
@Composable
private fun ForwardRoomTargetRow(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean = false,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) higColors.tint.copy(alpha = if (isDark) 0.16f else 0.08f)
                else higColors.secondarySystemBackground
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) higColors.tint else higColors.separator.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
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

        Spacer(Modifier.width(8.dp))

        // 右侧多选 Checkbox 圆圈
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) higColors.tint else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) higColors.tint else higColors.separator.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
