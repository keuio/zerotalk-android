package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.shadow.Shadow
import kotlin.math.roundToInt

/**
 * 遵循 KMPLiquidGlass 设计哲学的液态毛玻璃滑块组件 (GlassSlider / LiquidSlider)
 *
 * 特性：
 * 1. 磨砂质感轨道：半透明凹槽衬底配合圆角几何，暗色/浅色自适应；
 * 2. 高光渐变已播槽：玫瑰粉到珊瑚红高质感渐变；
 * 3. 流体毛玻璃微胶囊滑块 (Thumb)：
 *    - 未交互时呈现精致白色圆珠 (12dp) 与微阴影；
 *    - 按下/拖拽时触发弹性形变膨胀 (16dp) 与柔和高光投影；
 * 4. 触控支持：支持沿轨道单击即达、无级流畅平滑拖动，松手触发 [onValueChangeFinished]。
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    val trackHeight = 4.dp
    val normalThumbSize = 12.dp
    val pressedThumbSize = 16.dp

    var isInteracting by remember { mutableStateOf(false) }

    val thumbScale by animateFloatAsState(
        targetValue = if (isInteracting) 1.33f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f)
    )

    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isDark) Color(0xFF1E222B) else Color(0xFFF1F5F9))
    }

    BoxWithConstraints(
        modifier = modifier
            .height(28.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbRadiusPx = with(density) { (normalThumbSize / 2).toPx() }
        val usableWidthPx = (widthPx - thumbRadiusPx * 2).coerceAtLeast(1f)

        val clampedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
        val fraction = if (valueRange.endInclusive > valueRange.start) {
            ((clampedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        } else 0f

        fun updateFromX(rawX: Float) {
            val adjustedX = (rawX - thumbRadiusPx).coerceIn(0f, usableWidthPx)
            val newFraction = (adjustedX / usableWidthPx).coerceIn(0f, 1f)
            val computedValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
            currentOnValueChange(computedValue)
        }

        val gestureModifier = if (enabled) {
            Modifier.pointerInput(widthPx, valueRange) {
                detectTapGestures(
                    onPress = { offset ->
                        isInteracting = true
                        updateFromX(offset.x)
                        val released = tryAwaitRelease()
                        isInteracting = false
                        if (released) {
                            currentOnValueChangeFinished?.invoke()
                        }
                    }
                )
            }.pointerInput(widthPx, valueRange) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isInteracting = true
                        updateFromX(offset.x)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        updateFromX(change.position.x)
                    },
                    onDragEnd = {
                        isInteracting = false
                        currentOnValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isInteracting = false
                    }
                )
            }
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier),
            contentAlignment = Alignment.CenterStart
        ) {
            // 背景底槽（毛玻璃磨砂凹槽）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                    .border(
                        0.5.dp,
                        if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000),
                        RoundedCornerShape(trackHeight / 2)
                    )
            )

            // 已播进度槽（鲜活玫瑰色渐变）
            val activeWidth = (thumbRadiusPx + usableWidthPx * fraction)
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, 0) }
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(
                            width = with(density) { activeWidth.toDp() },
                            height = trackHeight
                        )
                        .clip(RoundedCornerShape(trackHeight / 2))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFE11D48), Color(0xFFFB7185))
                            )
                        )
                )
            }

            // 流体毛玻璃微胶囊滑块 (Thumb)
            val thumbOffsetXPx = usableWidthPx * fraction
            val currentThumbSizeDp = normalThumbSize * thumbScale

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = thumbOffsetXPx.roundToInt(),
                            y = 0
                        )
                    }
                    .align(Alignment.CenterStart)
                    .size(currentThumbSizeDp)
                    .shadow(
                        elevation = if (isInteracting) 6.dp else 2.5.dp,
                        shape = CircleShape,
                        ambientColor = Color(0x40000000),
                        spotColor = Color(0x50000000)
                    )
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // 中心微核
                Box(
                    modifier = Modifier
                        .size(currentThumbSizeDp * 0.45f)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48))
                )
            }
        }
    }
}
