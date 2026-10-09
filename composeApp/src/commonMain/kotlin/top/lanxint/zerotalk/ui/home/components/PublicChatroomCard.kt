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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 首页第二个卡片：公共聊天室（零语大厅）
 * 遵循 Apple HIG Grouped List / Card 规范设计，点击推入全屏聊天室。
 */
@Composable
fun PublicChatroomCard(
    onEnterChatroom: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
    val titleColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
    val chevronColor = if (isDark) Color(0xFF626B7D) else Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEnterChatroom
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // iOS 风格应用级圆角图标容器
            Box(
                modifier = Modifier
                    .size(41.4.dp)
                    .clip(RoundedCornerShape(11.7.dp))
                    .background(if (isDark) Color(0xFF1E3A5F) else Color(0xFFE0EFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "零语大厅",
                    tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF007AFF),
                    modifier = Modifier.size(21.6.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // 标题与大厅状态
            Column(
                modifier = Modifier.weight(1f)
            ) {
                BasicText(
                    text = "零语大厅",
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 14.4.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = "自由交流的匿名开放空间",
                    style = TextStyle(
                        color = subtitleColor,
                        fontSize = 10.8.sp
                    )
                )
            }

            // iOS 右侧 Chevron 指示箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "进入聊天室",
                tint = chevronColor,
                modifier = Modifier.size(12.6.dp)
            )
        }
    }
}
