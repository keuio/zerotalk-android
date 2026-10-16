package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberBackdrop
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop
import com.kashif_e.backdrop.backdrops.rememberCombinedBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.InnerShadow
import com.kashif_e.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import top.lanxint.zerotalk.ui.utils.DampedDragAnimation

/**
 * 1:1 对齐 KMPLiquidGlass / Backdrop Catalog 的官方液态毛玻璃滑块组件 (LiquidSlider)。
 *
 * 特性：
 * 1. 物理阻尼与速度形变 (DampedDragAnimation)：
 *    - 拖拽过程根据手势加速度产生 scaleX / scaleY 动态挤压拉伸；
 *    - 松手与轻触具有弹簧阻尼回弹特性；
 * 2. 双层折射透镜 (Lens + Chromatic Aberration)：
 *    - Thumb 按下时透镜折射高度由 0 平滑过渡到 10dp / 14dp，并呈现色散边缘；
 *    - 未按下时保持 8dp 高斯模糊并呈现白润磨砂质感；
 * 3. 完美适配深浅色与自定义主题色，兜底使用纯色 CanvasBackdrop 避免空引用。
 */
@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    visibilityThreshold: Float = 0.001f,
    backdrop: Backdrop? = null,
    accentColor: Color? = null,
    trackHeight: Dp = 6.dp,
    thumbWidth: Dp = 36.dp,
    thumbHeight: Dp = 20.dp,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val isLightTheme = !isSystemInDarkTheme()
    val defaultAccentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val actualAccentColor = accentColor ?: defaultAccentColor

    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isLightTheme) Color(0xFFF2F4F8) else Color(0xFF1E222B))
    }
    val actualBackdrop = backdrop ?: fallbackBackdrop

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }

        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.35f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        onValueChange(targetValue)
                        onValueChangeFinished?.invoke()
                    }
                    didDrag = false
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    val next = if (isLtr) (targetValue + delta).coerceIn(valueRange)
                    else (targetValue - delta).coerceIn(valueRange)
                    onValueChange(next)
                }
            )
        }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { v ->
                    if (dampedDragAnimation.targetValue != v) {
                        dampedDragAnimation.updateValue(v)
                    }
                }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layerBackdrop(trackBackdrop)
        ) {
            // 背景槽
            Box(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(trackColor)
                    .pointerInput(animationScope, trackWidth, valueRange) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                            val targetValue =
                                (if (isLtr) valueRange.start + delta
                                else valueRange.endInclusive - delta)
                                    .coerceIn(valueRange)
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue)
                            onValueChangeFinished?.invoke()
                        }
                    }
                    .height(trackHeight)
                    .fillMaxWidth()
            )

            // 已播激活进度槽
            Box(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(actualAccentColor)
                    .height(trackHeight)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                            .coerceIn(0, constraints.maxWidth)
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        // 官方毛玻璃折射流体滑块 (Thumb)
        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        actualBackdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.08f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(thumbWidth, thumbHeight)
        )
    }
}
