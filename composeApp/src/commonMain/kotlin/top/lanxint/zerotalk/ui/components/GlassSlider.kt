package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
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
import kotlin.math.roundToInt

/**
 * 遵循 KMPLiquidGlass 设计哲学的液态毛玻璃滑块组件 (GlassSlider / LiquidSlider)
 *
 * 特性：
 * 1. 轨道层 Backdrop 采样：通过 [layerBackdrop] 捕获底轨与进度色彩；
 * 2. 14dp x 14dp 水滴胶囊滑块：
 *    - 常态为正圆水滴胶囊；
 *    - 按下/拖拽时通过 [DampedDragAnimation] 产生受压扁平与速度拉伸阻尼形变；
 * 3. 完整多层折射：通过 [rememberCombinedBackdrop]、[lens] 折射与高光内阴影，呈现生动流体质感；
 * 4. 触控支持：单击轨道即达、无级平滑拖动与松手回弹。
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
    isDark: Boolean = isSystemInDarkTheme()
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val animationScope = rememberCoroutineScope()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isDark) Color(0xFF1E222B) else Color(0xFFF1F5F9))
    }
    val actualBackdrop = backdrop ?: fallbackBackdrop

    val trackHeight = 4.dp
    val thumbBaseSize = 14.dp

    val initialFraction = if (valueRange.endInclusive > valueRange.start) {
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).fastCoerceIn(0f, 1f)
    } else 0f

    var fraction by remember { mutableFloatStateOf(initialFraction) }
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .height(28.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbRadiusPx = with(density) { (thumbBaseSize / 2).toPx() }
        val usableWidthPx = (widthPx - thumbRadiusPx * 2).coerceAtLeast(1f)

        val dampedDragAnimation = remember(animationScope, usableWidthPx) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = fraction,
                valueRange = 0f..1f,
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.25f,
                onDragStarted = { position ->
                    isDragging = true
                    val rawX = if (isLtr) position.x else (widthPx - position.x)
                    val newF = ((rawX - thumbRadiusPx) / usableWidthPx).fastCoerceIn(0f, 1f)
                    fraction = newF
                    val newVal = valueRange.start + newF * (valueRange.endInclusive - valueRange.start)
                    currentOnValueChange(newVal)
                },
                onDragStopped = {
                    isDragging = false
                    currentOnValueChangeFinished?.invoke()
                },
                onDrag = { _, dragAmount ->
                    val delta = if (isLtr) dragAmount.x / usableWidthPx else -dragAmount.x / usableWidthPx
                    val newF = (fraction + delta).fastCoerceIn(0f, 1f)
                    fraction = newF
                    val newVal = valueRange.start + newF * (valueRange.endInclusive - valueRange.start)
                    currentOnValueChange(newVal)
                }
            )
        }

        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { fraction }
                .collectLatest { f ->
                    dampedDragAnimation.updateValue(f)
                }
        }

        LaunchedEffect(value, isDragging) {
            if (!isDragging) {
                val f = if (valueRange.endInclusive > valueRange.start) {
                    ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).fastCoerceIn(0f, 1f)
                } else 0f
                if (f != fraction) {
                    fraction = f
                    dampedDragAnimation.updateValue(f)
                }
            }
        }

        val trackBackdrop = rememberLayerBackdrop()

        val gestureModifier = if (enabled) dampedDragAnimation.modifier else Modifier

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier),
            contentAlignment = Alignment.CenterStart
        ) {
            // 1. 轨道层（挂载 LayerBackdrop 用于滑块采样内部色彩与折射）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .layerBackdrop(trackBackdrop)
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                    .border(
                        0.5.dp,
                        if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000),
                        RoundedCornerShape(trackHeight / 2)
                    )
            )

            // 2. 已播进度槽（鲜活玫瑰色渐变）
            val currentAnimatedFraction = dampedDragAnimation.value
            val activeWidthPx = (thumbRadiusPx + usableWidthPx * currentAnimatedFraction)
            Box(
                modifier = Modifier
                    .size(
                        width = with(density) { activeWidthPx.toDp() },
                        height = trackHeight
                    )
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFE11D48), Color(0xFFFB7185))
                        )
                    )
            )

            // 3. KMPLiquidGlass 液态毛玻璃水滴胶囊滑块 (Thumb)
            val thumbOffsetXPx = usableWidthPx * currentAnimatedFraction

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = if (isLtr) thumbOffsetXPx else (widthPx - thumbOffsetXPx - thumbRadiusPx * 2)
                    }
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(
                            actualBackdrop,
                            rememberBackdrop(trackBackdrop) { drawBackdrop ->
                                val progress = dampedDragAnimation.pressProgress
                                val scaleX = lerp(2f / 3f, 0.75f, progress)
                                val scaleY = lerp(0f, 0.75f, progress)
                                scale(scaleX, scaleY) {
                                    drawBackdrop()
                                }
                            }
                        ),
                        shape = { RoundedCornerShape(100.dp) },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            blur(6f.dp.toPx() * (1f - progress))
                            lens(
                                4f.dp.toPx() * progress,
                                8f.dp.toPx() * progress,
                                chromaticAberration = true
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Ambient.copy(
                                width = Highlight.Ambient.width / 1.5f,
                                blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                alpha = 0.5f + progress * 0.4f
                            )
                        },
                        shadow = {
                            Shadow(
                                radius = 3.dp,
                                color = Color.Black.copy(alpha = 0.22f)
                            )
                        },
                        innerShadow = {
                            val progress = dampedDragAnimation.pressProgress
                            InnerShadow(
                                radius = 3.dp * progress,
                                alpha = progress * 0.8f
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            // 按压时水滴轻微压扁受力形变，拖动时随速度横向流体拉伸
                            scaleY = dampedDragAnimation.scaleY * (1f - dampedDragAnimation.pressProgress * 0.15f)
                            val velocity = dampedDragAnimation.velocity / 50f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            // 表面清亮玻璃水滴高光
                            drawRect(Color.White.copy(alpha = 0.92f - progress * 0.3f))
                        }
                    )
                    .size(thumbBaseSize)
                    .border(0.75.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(100.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 水珠顶部微弧高光亮点
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .offset(y = (-2.5).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }
        }
    }
}
