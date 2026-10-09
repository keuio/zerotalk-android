package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import top.lanxint.zerotalk.ui.utils.DampedDragAnimation
import com.kashif_e.backdrop.backdrops.CanvasBackdrop
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.shadow.Shadow
import kotlin.math.abs
import kotlin.math.sign

/**
 * 支持拖动与物理阻尼形变的分段选择器 (LiquidSegmentedControl)。
 * - 采用与底部导航栏同款 DampedDragAnimation 物理阻尼与手势跟随；
 * - 双层磨砂结构：外层微磨砂底槽 + 内层阻尼滑块，均只使用 blur + 纯色填充（滑块另有 3dp 阴影）；
 * - 🚫 刻意不使用 AGSL 高性能特效：本组件采样的是自绘纯色画布（fallbackBackdrop），
 *   lens 折射 / vibrancy / Highlight 渐变高光在此没有可见信息量，去掉可省每帧两趟 RuntimeShader；
 * - 支持手势无级拖拽、松手吸附以及点击瞬间弹簧滑动。
 */
@Composable
fun LiquidSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    outerHeight: Dp = 30.6.dp,
    innerHeight: Dp = 25.2.dp,
    isDark: Boolean = isSystemInDarkTheme()
) {
    if (options.isEmpty()) return

    val tabsCount = options.size
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    val isDarkState = rememberUpdatedState(isDark)

    val fallbackBackdrop = rememberCanvasBackdrop {
        val currentIsDark = isDarkState.value
        drawRect(if (currentIsDark) Color(0xFF222631) else Color(0xFFF2F4F8))
    }

    BoxWithConstraints(
        modifier = modifier
            .height(outerHeight)
            .clip(RoundedCornerShape(100.dp))
            .drawBackdrop(
                backdrop = fallbackBackdrop,
                shape = { RoundedCornerShape(100.dp) },
                effects = {
                    blur(6f.dp.toPx())
                },
                onDrawSurface = {
                    val currentIsDark = isDarkState.value
                    drawRect(if (currentIsDark) Color(0xFF161922).copy(alpha = 0.5f) else Color(0xFFE2E6EE).copy(alpha = 0.5f))
                }
            )
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val tabWidthPx = totalWidthPx / tabsCount
        val tabWidthDp = with(density) { tabWidthPx.toDp() }

        val currentOnOptionSelect by rememberUpdatedState(onOptionSelect)

        val dampedDragAnimation = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1.08f,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    animateToValue(targetIndex.toFloat())
                    currentOnOptionSelect(targetIndex)
                },
                onDrag = { _, _ -> }
            )
        }

        // 外部选中索引变化时触发弹簧动画
        LaunchedEffect(selectedIndex) {
            if (dampedDragAnimation.targetValue.fastRoundToInt() != selectedIndex) {
                dampedDragAnimation.animateToValue(selectedIndex.toFloat())
            }
        }

        // ---- Layer 1: 可随动画与手势位移的液态毛玻璃滑块 ----
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidthPx
                        else totalWidthPx - (dampedDragAnimation.value + 1f) * tabWidthPx
                }
                .width(tabWidthDp)
                .height(innerHeight)
                .drawBackdrop(
                    backdrop = fallbackBackdrop,
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        blur(4f.dp.toPx())
                    },
                    shadow = {
                        val currentIsDark = isDarkState.value
                        Shadow(
                            radius = 3.dp,
                            color = Color.Black.copy(alpha = if (currentIsDark) 0.30f else 0.08f)
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val currentIsDark = isDarkState.value
                        drawRect(if (currentIsDark) Color(0xFF2E3442).copy(alpha = 0.85f) else Color(0xFFFFFFFF).copy(alpha = 0.90f))
                    }
                )
        )

        // ---- Layer 2: 选项文字层 (无事件阻断，纯视觉呈现) ----
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, title ->
                val currentFraction = dampedDragAnimation.value
                val distance = abs(currentFraction - index)
                val isSelected = distance < 0.5f

                val textColor = if (isSelected) {
                    if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                } else {
                    if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = title,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 10.8.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    )
                }
            }
        }

        // ---- Layer 3: 顶层统一手势交互层 (处理点击与流畅拖拽跟随) ----
        val viewConfig = LocalViewConfiguration.current
        val touchSlop = viewConfig.touchSlop

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tabsCount, isLtr, totalWidthPx, tabWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        var hasMoved = false
                        var totalDragX = 0f
                        var totalDragY = 0f

                        val currentFraction = dampedDragAnimation.value
                        val thumbCenter = if (isLtr) (currentFraction + 0.5f) * tabWidthPx else totalWidthPx - (currentFraction + 0.5f) * tabWidthPx
                        val isTouchingThumb = abs(startX - thumbCenter) <= tabWidthPx * 0.75f
                        val startTargetValue = if (isTouchingThumb) {
                            dampedDragAnimation.value
                        } else {
                            if (isLtr) (startX / tabWidthPx - 0.5f).fastCoerceIn(0f, (tabsCount - 1).toFloat())
                            else ((totalWidthPx - startX) / tabWidthPx - 0.5f).fastCoerceIn(0f, (tabsCount - 1).toFloat())
                        }

                        dampedDragAnimation.press()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.changedToUpIgnoreConsumed()) {
                                change.consume()
                                dampedDragAnimation.release()
                                if (hasMoved) {
                                    val velocity = dampedDragAnimation.velocity
                                    val predictedValue = dampedDragAnimation.targetValue + (velocity * 0.25f).fastCoerceIn(-0.6f, 0.6f)
                                    val targetIndex = predictedValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                                    dampedDragAnimation.animateToValue(targetIndex.toFloat())
                                    currentOnOptionSelect(targetIndex)
                                } else {
                                    // 未发生显著拖拽位移，判定为单点轻触选项
                                    val tappedIndex = if (isLtr) {
                                        (startX / tabWidthPx).toInt().fastCoerceIn(0, tabsCount - 1)
                                    } else {
                                        ((totalWidthPx - startX) / tabWidthPx).toInt().fastCoerceIn(0, tabsCount - 1)
                                    }
                                    dampedDragAnimation.animateToValue(tappedIndex.toFloat())
                                    currentOnOptionSelect(tappedIndex)
                                }
                                break
                            }

                            if (change.isConsumed) {
                                dampedDragAnimation.release()
                                val targetIndex = dampedDragAnimation.targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                                dampedDragAnimation.animateToValue(targetIndex.toFloat())
                                break
                            }

                            val dragDeltaX = change.position.x - change.previousPosition.x
                            val dragDeltaY = change.position.y - change.previousPosition.y
                            totalDragX += dragDeltaX
                            totalDragY += dragDeltaY

                            if (!hasMoved) {
                                if (abs(totalDragX) > touchSlop && abs(totalDragX) > abs(totalDragY)) {
                                    hasMoved = true
                                    val initialDelta = (totalDragX / tabWidthPx) * if (isLtr) 1f else -1f
                                    dampedDragAnimation.updateValue(
                                        (startTargetValue + initialDelta).fastCoerceIn(0f, (tabsCount - 1).toFloat())
                                    )
                                } else if (abs(totalDragY) > touchSlop && abs(totalDragY) > abs(totalDragX)) {
                                    // 识别为垂直滚动，将事件交由外层滚动容器
                                    dampedDragAnimation.release()
                                    break
                                }
                            } else {
                                change.consume()
                                val deltaFraction = (dragDeltaX / tabWidthPx) * if (isLtr) 1f else -1f
                                dampedDragAnimation.updateValue(
                                    (dampedDragAnimation.targetValue + deltaFraction).fastCoerceIn(0f, (tabsCount - 1).toFloat())
                                )
                            }
                        }
                    }
                }
        )
    }
}
