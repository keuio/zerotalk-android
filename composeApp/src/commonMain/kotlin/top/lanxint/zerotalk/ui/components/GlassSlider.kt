package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.Backdrop

/**
 * 液态毛玻璃滑块组件 (GlassSlider)，由 1:1 对齐 KMPLiquidGlass / Backdrop Catalog 的 [LiquidSlider] 驱动。
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    backdrop: Backdrop? = null,
    accentColor: Color? = null,
    trackHeight: Dp = 6.dp,
    thumbWidth: Dp = 36.dp,
    thumbHeight: Dp = 20.dp,
    isDark: Boolean = isSystemInDarkTheme()
) {
    LiquidSlider(
        value = { value },
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        backdrop = backdrop,
        accentColor = accentColor,
        trackHeight = trackHeight,
        thumbWidth = thumbWidth,
        thumbHeight = thumbHeight,
        onValueChangeFinished = onValueChangeFinished
    )
}
