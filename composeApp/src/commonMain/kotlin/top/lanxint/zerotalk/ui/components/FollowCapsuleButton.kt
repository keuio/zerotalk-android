package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 苹果 HIG 风格关注 / 已关注状态切换小胶囊按钮
 */
@Composable
fun FollowCapsuleButton(
    isFollowing: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val bg = if (isFollowing) {
        if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
    } else {
        higColors.tint
    }
    val textColor = if (isFollowing) higColors.secondaryLabel else Color.White

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = if (isFollowing) "已关注" else "关注",
            style = AppleHigTypography.caption1.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
