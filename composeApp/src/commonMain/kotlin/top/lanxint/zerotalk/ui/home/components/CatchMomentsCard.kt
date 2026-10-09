package top.lanxint.zerotalk.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
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
 * 首页第三个卡片：捞动态
 * - 主体区域：点击拉起“捞动态” Apple HIG 底部模态弹窗
 * - 右侧“捞取记录”按钮：点击拉起“捞取记录” Apple HIG 底部模态弹窗
 */
@Composable
fun CatchMomentsCard(
    onCatchMomentsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
    val titleColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
    val btnTextColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCatchMomentsClick
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器
            Box(
                modifier = Modifier
                    .size(41.4.dp)
                    .clip(RoundedCornerShape(11.7.dp))
                    .background(if (isDark) Color(0xFF3B2D54) else Color(0xFFF3E8FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Waves,
                    contentDescription = "捞动态",
                    tint = if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA),
                    modifier = Modifier.size(21.6.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // 文本信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                BasicText(
                    text = "捞动态",
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 14.4.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = "探索此刻零友的心声与动态",
                    style = TextStyle(
                        color = subtitleColor,
                        fontSize = 10.8.sp
                    )
                )
            }

            // 右侧“捞取记录”独立胶囊毛玻璃按钮
            CapsuleGlassButton(
                onClick = onHistoryClick,
                isDark = isDark,
                modifier = Modifier.height(28.8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "捞取记录",
                        tint = btnTextColor,
                        modifier = Modifier.size(12.6.dp)
                    )
                    BasicText(
                        text = "记录",
                        style = TextStyle(
                            color = btnTextColor,
                            fontSize = 10.8.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
