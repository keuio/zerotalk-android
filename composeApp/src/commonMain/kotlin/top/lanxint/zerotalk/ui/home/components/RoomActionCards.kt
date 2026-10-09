package top.lanxint.zerotalk.ui.home.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 首页并排独立小卡片：创建房间 & 加入房间
 * 遵循 Apple HIG Grouped List / Card 规范设计，纵向结构包含精致图标、标题与说明文案。
 */
@Composable
fun RoomActionCards(
    onCreateRoomClick: () -> Unit,
    onJoinRoomClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 卡片 1: 创建房间
        SingleRoomActionCard(
            title = "创建房间",
            subtitle = "自定义私密聊天",
            icon = Icons.Default.Add,
            iconBrush = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
            onClick = onCreateRoomClick,
            isDark = isDark,
            modifier = Modifier.weight(1f)
        )

        // 卡片 2: 加入房间
        SingleRoomActionCard(
            title = "加入房间",
            subtitle = "输入暗号进房",
            icon = Icons.Default.Lock,
            iconBrush = Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF06B6D4))),
            onClick = onJoinRoomClick,
            isDark = isDark,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SingleRoomActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBrush: Brush,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
    val titleColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 图标圆圈
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // 标题
            BasicText(
                text = title,
                style = TextStyle(
                    color = titleColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.height(4.dp))

            // 副标题
            BasicText(
                text = subtitle,
                style = TextStyle(
                    color = subtitleColor,
                    fontSize = 11.5.sp
                )
            )
        }
    }
}
