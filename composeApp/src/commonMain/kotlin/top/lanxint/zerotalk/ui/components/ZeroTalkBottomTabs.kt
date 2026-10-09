package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import top.lanxint.zerotalk.ui.utils.DampedDragAnimation
import top.lanxint.zerotalk.ui.utils.InteractiveHighlight
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberCombinedBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.effects.vibrancy
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.InnerShadow
import com.kashif_e.backdrop.shadow.Shadow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

import androidx.compose.ui.unit.Dp
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop

@Composable
fun ZeroTalkBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    outerHeight: Dp = 56.dp,
    innerHeight: Dp = 48.dp,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable RowScope.() -> Unit
) {
    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isDark) Color(0xFF1B1E26) else Color(0xFFF2F4F8))
    }
    val actualBackdrop = backdrop ?: fallbackBackdrop

    val accentColor =
        if (isDark) Color(0xFF0091FF)
        else Color(0xFF0088FF)
    val containerColor =
        if (isDark) Color(0xFF121212).copy(0.4f)
        else Color(0xFFFAFAFA).copy(0.4f)

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()

        val dampedDragAnimation = remember(animationScope, innerHeight) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = (innerHeight.value + 22f) / innerHeight.value,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    animateToValue(targetIndex.toFloat())
                    onTabSelected(targetIndex)
                    animationScope.launch {
                        offsetAnimation.animateTo(
                            0f,
                            spring(1f, 300f, 0.5f)
                        )
                    }
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            )
        }

        // 监听外部点击或状态变化：当目标索引变更时，驱动阻尼弹簧滑向目标
        val currentTarget = selectedTabIndex()
        LaunchedEffect(currentTarget) {
            if (dampedDragAnimation.targetValue.fastRoundToInt() != currentTarget) {
                dampedDragAnimation.animateToValue(currentTarget.toFloat())
            }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        // Layer 1: 底栏毛玻璃容器（包含真实可点击的 Tab 项目）
        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = actualBackdrop,
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                        lens(24f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = {
                        if (isDark) Highlight.Ambient.copy(alpha = 0.4f)
                        else Highlight.Plain.copy(alpha = 0.4f)
                    },
                    shadow = {
                        Shadow(
                            radius = 12.dp,
                            color = if (isDark) Color.Black.copy(0.35f) else Color.Black.copy(0.06f)
                        )
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(interactiveHighlight.modifier)
                .height(outerHeight)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalZeroTalkTabClickable provides true) {
                content()
            }
        }

        // Layer 2: 染色层（不可见，仅作为 tabsBackdrop 供 Layer 3 光斑采样，禁用点击）
        CompositionLocalProvider(
            LocalZeroTalkBottomTabScale provides {
                lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
            },
            LocalZeroTalkTabClickable provides false
        ) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = actualBackdrop,
                        shape = { RoundedCornerShape(100.dp) },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(
                                24f.dp.toPx() * progress,
                                24f.dp.toPx() * progress
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Default.copy(alpha = progress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(interactiveHighlight.modifier)
                    .height(innerHeight)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        // Layer 3: 动态滑行光斑胶囊（采样 CombinedBackdrop，提供拖拽与物理速度压扁）
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(actualBackdrop, tabsBackdrop),
                    shape = { RoundedCornerShape(100.dp) },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = progress)
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(
                            radius = 24.dp,
                            offset = androidx.compose.ui.unit.DpOffset(0.dp, 6.dp * progress),
                            color = if (isDark) Color.Black.copy(0.4f) else Color(0xFF0F172A).copy(0.15f),
                            alpha = progress
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 8f.dp * progress,
                            offset = androidx.compose.ui.unit.DpOffset(0.dp, 3.dp * progress),
                            color = if (isDark) Color.Black.copy(0.15f) else Color(0xFF0F172A).copy(0.22f),
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
                        // 未按压时的平贴指示层（随按压平滑淡出）
                        drawRect(
                            if (isDark) Color.White.copy(0.1f)
                            else Color.Black.copy(0.07f),
                            alpha = 1f - progress
                        )
                        // 暗色模式保持原有微弱沉浸底色
                        if (isDark) {
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        } else if (progress > 0.001f) {
                            // 亮色模式方案 2：纯白底上的物理菲涅尔暗边与立体厚度折射
                            val cr = CornerRadius(size.height / 2f, size.height / 2f)
                            // 1. 菲涅尔深色折射边框：顶部迎光高亮，侧边与底部深色全反射暗轮廓
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.85f * progress),
                                        Color(0xFF475569).copy(alpha = 0.20f * progress),
                                        Color(0xFF0F172A).copy(alpha = 0.38f * progress)
                                    )
                                ),
                                cornerRadius = cr,
                                style = Stroke(width = 1.2f.dp.toPx())
                            )
                            // 2. 凸透镜内部微聚光：中央通透微泛光，四周折射微暗调，呈现厚玻璃水晶质感
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.16f * progress),
                                        Color(0xFF1E293B).copy(alpha = 0.05f * progress)
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = (size.width + size.height) / 2.5f
                                ),
                                cornerRadius = cr
                            )
                        }
                    }
                )
                .height(innerHeight)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}
