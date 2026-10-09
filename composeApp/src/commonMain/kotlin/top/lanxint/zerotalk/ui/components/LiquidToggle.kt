package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import top.lanxint.zerotalk.ui.utils.DampedDragAnimation
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

/**
 * 1:1 对齐 Backdrop Catalog 的液态毛玻璃开关组件 (LiquidToggle)（缩小90%尺寸）。
 * - 物理阻尼拖拽形变 (pressedScale 1.5f)、微透镜折射 (lens 5/10dp with chromatic aberration)；
 * - 尺寸几何规范：轨道 57.6x25.2dp，滑块 36x21.6dp，拖动行程 18dp，内边距 1.8dp；
 * - 纯净 DampedDragAnimation 交互，避免外层容器产生点击冲突；
 * - 自适应当前 App 主题 (isDark) 与底层画布兜底 (fallbackBackdrop)。
 */
@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isDark) Color(0xFF222631) else Color(0xFFF2F4F8))
    }
    val actualBackdrop = backdrop ?: fallbackBackdrop

    val isLightTheme = !isDark
    val accentColor =
        if (isLightTheme) Color(0xFF34C759)
        else Color(0xFF30D158)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val trackWidth = 57.6.dp
    val trackHeight = 25.2.dp
    val thumbWidth = 36.dp
    val thumbHeight = 21.6.dp
    val dragTravel = 18.dp
    val dragWidth = with(density) { dragTravel.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }

    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selected)

    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    currentOnSelect(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (currentSelected()) 0f else 1f
                    currentOnSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            }
        )
    }
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(selected) {
        snapshotFlow { selected() }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val trackBackdrop = rememberLayerBackdrop()

    val thumbPadding = 1.8.dp
    val thumbPaddingPx = with(density) { thumbPadding.toPx() }

    Box(
        // 命中区覆盖整条轨道：此前手势只绑在滑块上，点轨道空白处没有反应。
        // 同时补上 Switch 语义与无障碍点击动作（TalkBack 可直接切换）。
        modifier = modifier
            .semantics {
                role = Role.Switch
                onClick(label = "切换") {
                    val next = !currentSelected()
                    fraction = if (next) 1f else 0f
                    currentOnSelect(next)
                    true
                }
            }
            .then(dampedDragAnimation.modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        // 底轨背景
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(RoundedCornerShape(100.dp))
                .drawBehind {
                    val currentFraction = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, currentFraction))
                }
                .size(trackWidth, trackHeight)
        )

        // 毛玻璃滑动指块
        Box(
            Modifier
                .graphicsLayer {
                    val currentFraction = dampedDragAnimation.value
                    val padding = thumbPaddingPx
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, currentFraction)
                        else lerp(-padding, -(padding + dragWidth), currentFraction)
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
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
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
                            color = Color.Black.copy(alpha = 0.05f)
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
                        val velocity = dampedDragAnimation.velocity / 50f
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
