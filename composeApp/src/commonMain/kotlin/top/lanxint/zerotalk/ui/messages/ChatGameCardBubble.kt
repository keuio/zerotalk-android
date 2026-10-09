package top.lanxint.zerotalk.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.ChatGameInvite
import top.lanxint.zerotalk.data.model.GameType
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.theme.AppleHigColors

/**
 * 聊天流中的互动对战游戏卡片气泡
 *
 * 遵循官方客户端行为规范：
 * - 游戏类型：五子棋 (gobang)、围棋 (go)、中国象棋 (xiangqi)、国际象棋 (chess)、谁是卧底 (undercover)
 * - 状态：waiting (等待应战) | playing (对局中) | finished (对局结束) | cancelled (已取消)
 * - 交互按钮：接受应战 (*_join) / 进入对局 / 查看棋局
 */
@Composable
fun ChatGameCardBubble(
    invite: ChatGameInvite,
    isMine: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onEnterGame: (gameType: String, gameId: Long) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val gameName = GameType.getDisplayName(invite.gameType)

    val accentColor = when (invite.gameType.lowercase()) {
        GameType.GOBANG -> Color(0xFF007AFF)
        GameType.GO -> Color(0xFF5856D6)
        GameType.XIANGQI -> Color(0xFFFF3B30)
        GameType.CHESS -> Color(0xFFFF9500)
        GameType.UNDERCOVER -> Color(0xFFAF52DE)
        else -> Color(0xFF007AFF)
    }

    val (statusLabel, statusColor) = when (invite.status) {
        "waiting" -> "等人应战" to Color(0xFFFF9500)
        "playing" -> "对局中" to Color(0xFF34C759)
        "finished" -> {
            val resText = when (invite.result) {
                "draw" -> "和棋"
                "resign" -> "认输结束"
                else -> "对局结束"
            }
            resText to Color(0xFF8E8E93)
        }
        "cancelled" -> "已取消" to Color(0xFF8E8E93)
        else -> invite.status to Color(0xFF8E8E93)
    }

    val cardBg = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.95f)
    val borderCol = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(0.5.dp, borderCol, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 顶部信息：图标 + 游戏名称 + 状态药丸
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    BasicText(
                        text = gameName,
                        style = TextStyle(
                            color = higColors.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // 状态胶囊
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    BasicText(
                        text = statusLabel,
                        style = TextStyle(
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 对战信息区域（双方头像与昵称展示）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.5f) else Color(0xFFF2F2F7))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 蓝方/白方/发起方
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val p1Name = invite.whiteUsername ?: invite.redUsername ?: "发起人"
                    val p1Avatar = invite.whiteAvatarUrl ?: invite.redAvatarUrl
                    PlayerMiniAvatar(avatarUrl = p1Avatar, name = p1Name, color = accentColor)
                    BasicText(
                        text = p1Name,
                        style = TextStyle(
                            color = higColors.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // VS 标识
                BasicText(
                    text = "VS",
                    style = TextStyle(
                        color = higColors.tertiaryLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // 黑方/应战方
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val p2Name = invite.blackUsername ?: if (invite.status == "waiting") "等待应战..." else "对手"
                    val p2Avatar = invite.blackAvatarUrl
                    PlayerMiniAvatar(avatarUrl = p2Avatar, name = p2Name, color = Color(0xFF8E8E93))
                    BasicText(
                        text = p2Name,
                        style = TextStyle(
                            color = if (invite.blackUsername != null) higColors.label else higColors.tertiaryLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 底部操作按钮
            val isWaiting = invite.status == "waiting"
            val buttonText = when {
                isWaiting && !isMine -> "接受应战"
                isWaiting && isMine -> "等待加入中..."
                invite.status == "playing" -> "进入对局"
                else -> "查看棋局"
            }

            val canClick = !(isWaiting && isMine) && invite.status != "cancelled"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (canClick) accentColor else Color(0xFF8E8E93).copy(alpha = 0.2f))
                    .then(
                        if (canClick) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isWaiting && !isMine) {
                                    ZeroTalkClientManager.joinGame(invite.gameType, invite.gameId)
                                }
                                onEnterGame(invite.gameType, invite.gameId)
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        text = buttonText,
                        style = TextStyle(
                            color = if (canClick) Color.White else higColors.secondaryLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (canClick) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerMiniAvatar(
    avatarUrl: String?,
    name: String,
    color: Color
) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncNetworkImage(
            url = avatarUrl,
            contentDescription = name,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = name.take(1).uppercase(),
                style = TextStyle(
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
