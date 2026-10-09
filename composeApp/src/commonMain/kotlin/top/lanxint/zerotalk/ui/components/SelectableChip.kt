package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 可选中胶囊 chip —— 「一排可选项」的通用实现。
 *
 * 选中态：tint 18% 底 + 1dp tint 描边 + tint 文字（SemiBold）；
 * 未选中态：quaternarySystemFill 底 + 次要色文字（Medium）。
 *
 * 动态发布的「谁可以看」与网易云选歌的「模式切换」此前各写一份，
 * 且未选中字重已经漂移（Normal 与 Medium 并存），统一到这里。
 *
 * @param icon 可选前置图标（传 null 则只显示文字）
 * @param textStyle 文字基准样式（各处字号不同，由调用方指定）
 */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSize: Dp = 11.dp,
    textStyle: TextStyle = AppleHigTypography.caption1,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 6.dp
) {
    val higColors = AppleHigColors.colors(isDark)
    val shape = RoundedCornerShape(100.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) higColors.tint.copy(alpha = 0.18f)
                else higColors.quaternarySystemFill
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) higColors.tint else Color.Transparent,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) higColors.tint else higColors.secondaryLabel,
                modifier = Modifier.size(iconSize)
            )
            Spacer(Modifier.width(3.dp))
        }
        BasicText(
            text = label,
            style = textStyle.copy(
                color = if (selected) higColors.tint else higColors.secondaryLabel,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        )
    }
}
