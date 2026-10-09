package top.lanxint.zerotalk.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Psychology
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
 * 首页卡片：人格测试（MBTI）
 *
 * - 整卡点击进入官方 60 题问卷（文案与官网首页入口一致：「人格测试 / 60 题 · 看看你是哪种类型」）
 * - 已测出结果时，右侧显示类型代码胶囊（如 INFP），方便一眼看到自己的类型
 * - 视觉沿用首页既有卡片规范（18dp 圆角、1dp 描边、41.4dp 图标容器）
 */
@Composable
fun MbtiTestCard(
    onOpenMbtiTest: () -> Unit,
    isDark: Boolean,
    /** 已测出的类型代码（如 INFP）；为空表示尚未测试 */
    mbtiType: String? = null,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
    val titleColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
    val accent = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    val accentBg = if (isDark) Color(0xFF2E2450) else Color(0xFFF3E8FF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenMbtiTest
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
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "人格测试",
                    tint = accent,
                    modifier = Modifier.size(21.6.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // 文本信息
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = "人格测试",
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 14.4.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = "60 题 · 看看你是哪种类型",
                    style = TextStyle(
                        color = subtitleColor,
                        fontSize = 10.8.sp
                    )
                )
            }

            // 已测出结果时显示类型代码胶囊
            val type = mbtiType?.trim().orEmpty()
            if (type.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(accentBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    BasicText(
                        text = type.uppercase(),
                        style = TextStyle(
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
