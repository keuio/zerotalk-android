package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.utils.InteractiveHighlight
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.effects.vibrancy
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow

/**
 * 具有底部导航栏外层胶囊底栏同款视觉与物理质感的独立玻璃胶囊按钮。
 * - 视觉：40% 半透明毛玻璃底槽、40% 高光白边、vibrancy + blur(8dp) + lens(24dp)、柔和悬浮阴影
 * - 交互：iOS 触觉微缩放（点击与长按均完整触发收缩回弹，默认 0.92f）、shimmer 光斑追踪
 */
@Composable
fun CapsuleGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    backdrop: Backdrop? = null,
    isDark: Boolean = isSystemInDarkTheme(),
    pressedScale: Float = 0.92f,
    content: @Composable BoxScope.() -> Unit
) {
    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isDark) Color(0xFF1B1E26) else Color(0xFFF2F4F8))
    }
    val actualBackdrop = backdrop ?: fallbackBackdrop

    val defaultContainerColor =
        if (isDark) Color(0xFF121212).copy(0.4f)
        else Color(0xFFFAFAFA).copy(0.4f)
    val actualContainerColor = containerColor ?: defaultContainerColor

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    scale.animateTo(
                        targetValue = pressedScale,
                        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
                    )
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f)
                    )
                }
            }
        }
    }

    val shape = RoundedCornerShape(100.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .drawBackdrop(
                backdrop = actualBackdrop,
                shape = { shape },
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
                }
            )
            .clip(shape)
            .drawWithContent {
                drawRect(actualContainerColor)
                drawContent()
            }
            .then(interactiveHighlight.modifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

