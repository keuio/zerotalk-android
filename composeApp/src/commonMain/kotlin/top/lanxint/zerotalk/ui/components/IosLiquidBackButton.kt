package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.Backdrop

/**
 * 严格遵循 Apple SF Symbols 规范绘制的原生 iOS 圆润返回箭头 (<)
 *
 * 核心设计：
 * - 采用 StrokeCap.Round 确保两端为完全光滑的半球形端点；
 * - 采用 StrokeJoin.Round 确保转折折角（Apex）为平滑圆润曲面，无生硬菱角与突兀尖锐折线；
 * - 精准对齐 iOS 26 原生比例与光学居中。
 */
@Composable
fun IosRoundedChevronBackIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: Dp = 2.4.dp
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidthDp.toPx() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 遵循 Apple SF Symbols 的光学居中法则 (Optical Centering)：
        // 尖角朝左的形状（如 <）在右侧有两支张开的臂膀，视觉墨水权重偏向右侧。
        // 若按纯数学几何居中，人眼会感觉明显偏右。因此向左光学补偿约 1.6dp (0.08w)，
        // 顶点设在 27%，右端设在 57%，使视觉重心（Optical Centroid）与外层圆形/胶囊中心严格吻合。
        val path = Path().apply {
            moveTo(x = w * 0.57f, y = h * 0.22f)
            lineTo(x = w * 0.27f, y = h * 0.50f)
            lineTo(x = w * 0.57f, y = h * 0.78f)
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * 通用液态毛玻璃返回按钮 (IosLiquidBackButton)
 *
 * 特性：
 * 1. 基础尺寸：高度 41dp，与私聊顶部工具栏完全一致；
 * 2. 两种形态：
 *    - 胶囊形态 (unreadCount != null && unreadCount > 0)：展示圆润 `<` 图标与小圆胶囊数字角标；
 *    - 纯图标圆形形态 (unreadCount == null)：41dp × 41dp 正圆形液态毛玻璃按键，内部居中圆润 `<`；
 * 3. 严格对齐：无论胶囊形态还是圆形形态，`<` 图标中心均严格位于距左侧边缘 20.5dp 处，消除任何位置偏移；
 * 4. 完美结合 LiquidButton 物理阻尼与光影折射；
 * 5. 支持明暗自适应及自定义 Backdrop 穿透采样。
 */
@Composable
fun IosLiquidBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int? = null,
    backdrop: Backdrop? = null,
    isDark: Boolean = isSystemInDarkTheme(),
    isWhiteBackground: Boolean = false,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    surfaceAlpha: Float? = null
) {
    val effectiveTint = if (tint.isSpecified) {
        tint
    } else if (isWhiteBackground) {
        Color(0xFF1C1C1E)
    } else {
        Color.White
    }

    val badgeBg = if (isWhiteBackground) Color(0xFF1C1C1E) else Color.White
    val badgeTextColor = if (isWhiteBackground) Color.White else Color(0xFF1C1C1E)

    val buttonHeight = 36.9.dp
    val iconSize = 18.dp

    if (unreadCount != null && unreadCount > 0) {
        // ---- 胶囊形态：< [未读数] ----
        val badgeHeight = 24.3.dp
        LiquidButton(
            onClick = onClick,
            modifier = modifier.height(buttonHeight),
            backdrop = backdrop,
            isDark = isDark,
            surfaceColor = surfaceColor,
            surfaceAlpha = surfaceAlpha,
            contentPadding = PaddingValues(start = buttonHeight * 0.25f, end = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IosRoundedChevronBackIcon(
                    tint = effectiveTint,
                    modifier = Modifier.size(iconSize),
                    strokeWidthDp = 2.16.dp
                )

                // 角标小圆胶囊
                Box(
                    modifier = Modifier
                        .height(badgeHeight)
                        .clip(RoundedCornerShape(100.dp))
                        .background(badgeBg)
                        .padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "$unreadCount",
                        style = TextStyle(
                            color = badgeTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    } else {
        // ---- 纯图标圆形形态 ----
        LiquidButton(
            onClick = onClick,
            modifier = modifier.size(buttonHeight),
            backdrop = backdrop,
            isDark = isDark,
            surfaceColor = surfaceColor,
            surfaceAlpha = surfaceAlpha,
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.size(buttonHeight),
                contentAlignment = Alignment.Center
            ) {
                IosRoundedChevronBackIcon(
                    tint = effectiveTint,
                    modifier = Modifier.size(iconSize),
                    strokeWidthDp = 2.16.dp
                )
            }
        }
    }
}
