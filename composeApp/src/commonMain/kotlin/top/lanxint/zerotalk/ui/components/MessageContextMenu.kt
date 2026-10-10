package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.colorControls
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.data.model.ChatMessage
import top.lanxint.zerotalk.data.model.canCopy
import top.lanxint.zerotalk.data.model.canRecallOrEdit

/**
 * 消息长按上下文菜单（私聊 / 大厅共用）
 *
 * 从 [top.lanxint.zerotalk.ui.messages.PrivateChatScreen] 抽出，供私聊与公共大厅复用：
 * - [SFSymbolType] / [ContextMenuItem]：菜单项模型；
 * - [buildContextMenuItems]：按消息归属与权限构建菜单项；
 * - [AppleContextMenu]：iOS 26 原生 Liquid Glass 垂直菜单列表；
 * - [MessageContextMenuOverlay]：长按全屏浮层（遮罩 + 1.1 倍放大预览 slot + 智能避让菜单）。
 */

/**
 * 菜单项 SF Symbol 图标类型
 */
enum class SFSymbolType {
    REPLY,
    UNDO_SEND,
    RECALL_AND_EDIT,
    EDIT,
    COPY,
    SPEAK,
    TRANSLATE,
    SAVE_IMAGE,
    TRANSCRIBE,
    PAT,
    MENTION,
    TRASH,
    MORE
}

/**
 * 单个上下文菜单项
 */
data class ContextMenuItem(
    val title: String,
    val icon: SFSymbolType,
    val onClick: () -> Unit
)

/**
 * SF Symbol 风格原生极简矢量图标
 */
@Composable
private fun SFSymbolIcon(
    symbol: SFSymbolType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        when (symbol) {
            SFSymbolType.REPLY -> {
                // SF Symbol: arrowshape.turn.up.left
                val strokeWidth = 1.8.dp.toPx()
                val path = Path().apply {
                    moveTo(w * 0.12f, h * 0.38f)
                    lineTo(w * 0.42f, h * 0.12f)
                    moveTo(w * 0.12f, h * 0.38f)
                    lineTo(w * 0.42f, h * 0.64f)
                    moveTo(w * 0.14f, h * 0.38f)
                    cubicTo(
                        w * 0.50f, h * 0.38f,
                        w * 0.84f, h * 0.46f,
                        w * 0.84f, h * 0.86f
                    )
                }
                drawPath(path, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            SFSymbolType.UNDO_SEND -> {
                // SF Symbol: arrow.uturn.backward.circle
                val strokeWidth = 1.5.dp.toPx()
                drawCircle(
                    color = tint,
                    radius = w * 0.44f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = strokeWidth)
                )
                val arcRect = Rect(w * 0.27f, h * 0.27f, w * 0.73f, h * 0.73f)
                drawArc(
                    color = tint,
                    startAngle = 50f,
                    sweepAngle = 265f,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                val head = Path().apply {
                    moveTo(w * 0.20f, h * 0.46f)
                    lineTo(w * 0.34f, h * 0.30f)
                    lineTo(w * 0.34f, h * 0.58f)
                    close()
                }
                drawPath(head, color = tint, style = Fill)
            }
            SFSymbolType.RECALL_AND_EDIT -> {
                // SF Symbol: 撤回箭头 + 重新编辑铅笔组合
                val strokeWidth = 1.4.dp.toPx()
                // 1. 上半部：撤回弧线与箭头 (从右向左弯曲)
                val arcRect = Rect(w * 0.16f, h * 0.12f, w * 0.68f, h * 0.58f)
                drawArc(
                    color = tint,
                    startAngle = 10f,
                    sweepAngle = -200f,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                val arrowHead = Path().apply {
                    moveTo(w * 0.12f, h * 0.35f)
                    lineTo(w * 0.28f, h * 0.20f)
                    lineTo(w * 0.28f, h * 0.46f)
                    close()
                }
                drawPath(arrowHead, color = tint, style = Fill)

                // 2. 下半部：斜向编辑铅笔
                drawLine(
                    color = tint,
                    start = Offset(w * 0.48f, h * 0.82f),
                    end = Offset(w * 0.82f, h * 0.48f),
                    strokeWidth = strokeWidth * 1.5f,
                    cap = StrokeCap.Round
                )
                val tip = Path().apply {
                    moveTo(w * 0.36f, h * 0.88f)
                    lineTo(w * 0.42f, h * 0.74f)
                    lineTo(w * 0.52f, h * 0.84f)
                    close()
                }
                drawPath(tip, color = tint, style = Fill)
            }
            SFSymbolType.EDIT -> {
                // SF Symbol: pencil
                val strokeWidth = 1.6.dp.toPx()
                drawLine(
                    color = tint,
                    start = Offset(w * 0.32f, h * 0.68f),
                    end = Offset(w * 0.75f, h * 0.25f),
                    strokeWidth = strokeWidth * 1.5f,
                    cap = StrokeCap.Round
                )
                val tip = Path().apply {
                    moveTo(w * 0.18f, h * 0.82f)
                    lineTo(w * 0.25f, h * 0.64f)
                    lineTo(w * 0.36f, h * 0.75f)
                    close()
                }
                drawPath(tip, color = tint, style = Fill)
            }
            SFSymbolType.COPY -> {
                // SF Symbol: doc.on.doc
                val strokeWidth = 1.5.dp.toPx()
                val r = 2.dp.toPx()
                val backPath = Path().apply {
                    moveTo(w * 0.58f, h * 0.16f)
                    lineTo(w * 0.22f + r, h * 0.16f)
                    arcTo(Rect(w * 0.22f, h * 0.16f, w * 0.22f + 2 * r, h * 0.16f + 2 * r), 270f, -90f, false)
                    lineTo(w * 0.22f, h * 0.68f - r)
                    arcTo(Rect(w * 0.22f, h * 0.68f - 2 * r, w * 0.22f + 2 * r, h * 0.68f), 180f, -90f, false)
                    lineTo(w * 0.36f, h * 0.68f)
                }
                drawPath(backPath, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.36f, h * 0.30f),
                    size = Size(w * 0.48f, h * 0.58f),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = strokeWidth)
                )
            }
            SFSymbolType.SPEAK -> {
                // SF Symbol: speaker.wave.2
                val strokeWidth = 1.5.dp.toPx()
                val body = Path().apply {
                    moveTo(w * 0.15f, h * 0.38f)
                    lineTo(w * 0.28f, h * 0.38f)
                    lineTo(w * 0.46f, h * 0.22f)
                    lineTo(w * 0.46f, h * 0.78f)
                    lineTo(w * 0.28f, h * 0.62f)
                    lineTo(w * 0.15f, h * 0.62f)
                    close()
                }
                drawPath(body, color = tint, style = Fill)
                val arc1 = Rect(w * 0.42f, h * 0.34f, w * 0.64f, h * 0.66f)
                drawArc(color = tint, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = arc1.topLeft, size = arc1.size, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                val arc2 = Rect(w * 0.46f, h * 0.24f, w * 0.82f, h * 0.76f)
                drawArc(color = tint, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = arc2.topLeft, size = arc2.size, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            }
            SFSymbolType.TRANSLATE -> {
                // SF Symbol: character.bubble / translate
                val strokeWidth = 1.5.dp.toPx()
                val bubble = Path().apply {
                    moveTo(w * 0.16f, h * 0.22f)
                    lineTo(w * 0.84f, h * 0.22f)
                    lineTo(w * 0.84f, h * 0.64f)
                    lineTo(w * 0.52f, h * 0.64f)
                    lineTo(w * 0.36f, h * 0.80f)
                    lineTo(w * 0.36f, h * 0.64f)
                    lineTo(w * 0.16f, h * 0.64f)
                    close()
                }
                drawPath(bubble, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.38f), end = Offset(w * 0.70f, h * 0.38f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.50f), end = Offset(w * 0.56f, h * 0.50f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            SFSymbolType.SAVE_IMAGE -> {
                // SF Symbol: square.and.arrow.down
                val strokeWidth = 1.6.dp.toPx()
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.5f, h * 0.60f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                val head = Path().apply {
                    moveTo(w * 0.34f, h * 0.45f)
                    lineTo(w * 0.50f, h * 0.60f)
                    lineTo(w * 0.66f, h * 0.45f)
                }
                drawPath(head, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                val tray = Path().apply {
                    moveTo(w * 0.20f, h * 0.50f)
                    lineTo(w * 0.20f, h * 0.82f)
                    lineTo(w * 0.80f, h * 0.82f)
                    lineTo(w * 0.80f, h * 0.50f)
                }
                drawPath(tray, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            SFSymbolType.TRANSCRIBE -> {
                // SF Symbol: waveform
                val strokeWidth = 1.6.dp.toPx()
                val heights = listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.8f, 0.4f)
                heights.forEachIndexed { i, factor ->
                    val x = w * (0.20f + i * 0.12f)
                    val halfH = (h * 0.7f * factor) / 2
                    drawLine(
                        color = tint,
                        start = Offset(x, h * 0.5f - halfH),
                        end = Offset(x, h * 0.5f + halfH),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            SFSymbolType.TRASH -> {
                // SF Symbol: trash
                val strokeWidth = 1.5.dp.toPx()
                val body = Path().apply {
                    moveTo(w * 0.26f, h * 0.32f)
                    lineTo(w * 0.30f, h * 0.86f)
                    lineTo(w * 0.70f, h * 0.86f)
                    lineTo(w * 0.74f, h * 0.32f)
                }
                drawPath(body, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(
                    color = tint,
                    start = Offset(w * 0.18f, h * 0.28f),
                    end = Offset(w * 0.82f, h * 0.28f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                val handle = Path().apply {
                    moveTo(w * 0.40f, h * 0.28f)
                    lineTo(w * 0.40f, h * 0.16f)
                    lineTo(w * 0.60f, h * 0.16f)
                    lineTo(w * 0.60f, h * 0.28f)
                }
                drawPath(handle, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            SFSymbolType.PAT, SFSymbolType.MENTION, SFSymbolType.MORE -> {
                // SF Symbol: ellipsis.circle
                val strokeWidth = 1.5.dp.toPx()
                drawCircle(
                    color = tint,
                    radius = w * 0.44f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = Stroke(width = strokeWidth)
                )
                val dotR = 1.25.dp.toPx()
                val cy = h * 0.5f
                drawCircle(color = tint, radius = dotR, center = Offset(w * 0.32f, cy))
                drawCircle(color = tint, radius = dotR, center = Offset(w * 0.50f, cy))
                drawCircle(color = tint, radius = dotR, center = Offset(w * 0.68f, cy))
            }
        }
    }
}

/**
 * iOS 26 原生 Liquid Glass 垂直上下文菜单列表
 */
@Composable
fun AppleContextMenu(
    items: List<ContextMenuItem>,
    backdrop: Backdrop?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isDark) Color(0xFF1B1E26) else Color(0xFFF2F4F8))
    }
    val actualBackdrop = backdrop ?: fallbackBackdrop

    val surfaceColor = if (isDark) Color(0xFF1E1E22).copy(alpha = 0.68f) else Color(0xFFF9F9FB).copy(alpha = 0.68f)
    val labelColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val secondaryLabelColor = if (isDark) Color(0x99FFFFFF) else Color(0xFF8E8E93)
    val dividerColor = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000)

    Column(
        modifier = modifier
            .width(250.dp)
            .drawBackdrop(
                backdrop = actualBackdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    colorControls(
                        brightness = if (isDark) 0.05f else 0.15f,
                        saturation = 1.4f
                    )
                    blur(if (isDark) 16.dp.toPx() else 20.dp.toPx())
                    lens(16.dp.toPx(), 32.dp.toPx(), depthEffect = true)
                },
                highlight = { Highlight.Plain },
                shadow = { Shadow(radius = 20.dp, color = Color.Black.copy(if (isDark) 0.45f else 0.18f)) },
                onDrawSurface = { drawRect(surfaceColor) }
            )
            .clip(RoundedCornerShape(16.dp))
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = item.onClick
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = item.title,
                    style = TextStyle(
                        color = labelColor,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                SFSymbolIcon(
                    symbol = item.icon,
                    tint = secondaryLabelColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (index < items.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .padding(start = 16.dp)
                        .background(dividerColor)
                )
            }
        }
    }
}

/**
 * 消息长按全屏浮层（私聊 / 大厅共用）：
 * 1. 背景遮罩变暗；
 * 2. 悬浮放大预览（原地 1.1 倍放大 + 柔和外阴影），内容由 [preview] slot 提供，
 *    因此私聊可以传自己的 iMessage 气泡、大厅可以传自己的普通圆气泡；
 * 3. 独立 ContextMenu Popover（智能避让屏幕边缘，自适应位于气泡下方或上方）。
 *
 * @param bounds 被长按气泡在 root 坐标系中的边界（用于定位放大预览与菜单）
 * @param isMine 是否为本人消息（决定菜单左对齐还是右对齐、放大支点方向）
 * @param preview 放大预览内容；调用方负责以 fillMaxSize 渲染自己的气泡
 */
@Composable
fun MessageContextMenuOverlay(
    bounds: Rect,
    isMine: Boolean,
    isShowing: Boolean,
    backdrop: Backdrop?,
    isDark: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    onDismiss: () -> Unit,
    menuItems: List<ContextMenuItem>,
    preview: @Composable () -> Unit
) {
    val overlayAlpha = remember { Animatable(0f) }
    val bubbleScale = remember { Animatable(1.0f) }
    val bubbleOffsetY = remember { Animatable(0f) }
    val menuScale = remember { Animatable(0.75f) }
    val menuAlpha = remember { Animatable(0f) }

    LaunchedEffect(isShowing) {
        if (isShowing) {
            launch {
                overlayAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200)
                )
            }
            launch {
                bubbleScale.animateTo(
                    targetValue = 1.1f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                )
            }
            launch {
                bubbleOffsetY.animateTo(
                    targetValue = -4f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                )
            }
            launch {
                menuScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f)
                )
            }
            launch {
                menuAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 180)
                )
            }
        } else {
            launch {
                overlayAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180)
                )
            }
            launch {
                bubbleScale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                )
            }
            launch {
                bubbleOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                )
            }
            launch {
                menuScale.animateTo(
                    targetValue = 0.75f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f)
                )
            }
            launch {
                menuAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 150)
                )
            }
        }
    }

    val density = LocalDensity.current
    val bubbleLeft = with(density) { bounds.left.toDp() }
    val bubbleTop = with(density) { bounds.top.toDp() }
    val bubbleRight = with(density) { bounds.right.toDp() }
    val bubbleBottom = with(density) { bounds.bottom.toDp() }
    val bubbleWidth = with(density) { bounds.width.toDp() }
    val bubbleHeight = with(density) { bounds.height.toDp() }

    val menuWidth = 250.dp
    val menuHeight = 44.dp * menuItems.size

    // 气泡 1.1 倍放大并上浮 4dp 后的实际视觉边界：
    val scaledExtraHeight = bubbleHeight * 0.1f
    val effectiveBubbleBottom = bubbleBottom - 4.dp + scaledExtraHeight
    val effectiveBubbleTop = bubbleTop - 4.dp - scaledExtraHeight

    val spaceBelow = screenHeight - effectiveBubbleBottom - 88.dp
    val isMenuBelow = spaceBelow >= (menuHeight + 16.dp)

    val menuTop = if (isMenuBelow) {
        (effectiveBubbleBottom + 10.dp).coerceIn(80.dp, screenHeight - menuHeight - 16.dp)
    } else {
        (effectiveBubbleTop - menuHeight - 10.dp).coerceIn(80.dp, screenHeight - menuHeight - 16.dp)
    }

    val menuLeft = if (isMine) {
        (bubbleRight - menuWidth).coerceIn(16.dp, screenWidth - menuWidth - 14.dp)
    } else {
        bubbleLeft.coerceIn(14.dp, screenWidth - menuWidth - 16.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * overlayAlpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // 1. 悬浮放大预览 (原地 1.1 倍放大 + 轻微上浮 -4dp + 柔和外阴影)
        Box(
            modifier = Modifier
                .offset(x = bubbleLeft, y = bubbleTop + bubbleOffsetY.value.dp)
                .size(width = bubbleWidth, height = bubbleHeight)
                .graphicsLayer {
                    scaleX = bubbleScale.value
                    scaleY = bubbleScale.value
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (isMine) 0.85f else 0.15f,
                        pivotFractionY = if (isMenuBelow) 0f else 1f
                    )
                    shadowElevation = (18f * overlayAlpha.value).dp.toPx()
                }
        ) {
            preview()
        }

        // 2. 下方/上方独立 ContextMenu Popover (避让屏幕边缘)
        Box(
            modifier = Modifier
                .offset(x = menuLeft, y = menuTop)
                .graphicsLayer {
                    scaleX = menuScale.value
                    scaleY = menuScale.value
                    alpha = menuAlpha.value
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (isMine) 0.9f else 0.1f,
                        pivotFractionY = if (isMenuBelow) 0.0f else 1.0f
                    )
                }
        ) {
            AppleContextMenu(
                items = menuItems,
                backdrop = backdrop,
                isDark = isDark
            )
        }
    }
}

/**
 * 依据消息归属动态构建 ContextMenu 选项：
 * - 我方：回复、撤回、[撤回并编辑]、[编辑]、拷贝
 * - 对方：回复、拷贝
 * - 房管（viewer_can_delete_message）：额外可删除他人消息
 *
 * @param onReply 为 null 时隐藏「回复」项（例如大厅当前没有引用回复发送通道）
 * @param includeGroupActions 是否展示「@提及」（群聊 / 大厅为 true）
 */
fun buildContextMenuItems(
    message: ChatMessage,
    onReply: (() -> Unit)?,
    onRecall: () -> Unit,
    onRecallAndEdit: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    includeGroupActions: Boolean,
    onPat: () -> Unit,
    onMention: () -> Unit,
    canModerateDelete: Boolean,
    onModerateDelete: () -> Unit
): List<ContextMenuItem> {
    val list = mutableListOf<ContextMenuItem>()

    // 1. 回复（无引用回复通道时隐藏）
    if (onReply != null) {
        list.add(ContextMenuItem("回复", SFSymbolType.REPLY, onReply))
    }

    // 2. 仅我方包含：撤回、撤回并编辑、编辑
    // 已撤回消息不再提供（官网：isOwnMessage && !is_deleted && id）
    if (message.canRecallOrEdit()) {
        list.add(ContextMenuItem("撤回", SFSymbolType.UNDO_SEND, onRecall))
        // 动态分享卡片的 content 是卡片 JSON，不能进输入框编辑（官网同样排除非文本类型）
        if (!message.isImage && !message.isVoice && !message.isDice && !message.isMomentShare &&
            !message.isMusicPlaylist && !message.isSticker
        ) {
            list.add(ContextMenuItem("撤回并编辑", SFSymbolType.RECALL_AND_EDIT, onRecallAndEdit))
            list.add(ContextMenuItem("编辑", SFSymbolType.EDIT, onEdit))
        }
    }

    // 对方消息：拍一拍（私聊 / 群聊均可用，官网亦然），@提及仅群聊
    // 已撤回消息不再提供任何互动项（官网整条替换为占位）
    if (!message.isMine && !message.isDeleted) {
        list.add(ContextMenuItem("拍一拍", SFSymbolType.PAT, onPat))
        if (includeGroupActions) {
            list.add(ContextMenuItem("@提及", SFSymbolType.MENTION, onMention))
        }
    }

    // 3. 拷贝 (我方与对方均支持；已撤回消息内容已被清空，拷贝无意义)
    if (message.canCopy()) {
        list.add(ContextMenuItem("拷贝", SFSymbolType.COPY, onCopy))
    }

    // 4. 房管删除他人消息（仅服务端已同步、且未撤回的消息可删）
    if (!message.isMine && canModerateDelete && message.serverId > 0L && !message.isDeleted) {
        list.add(ContextMenuItem("删除消息", SFSymbolType.TRASH, onModerateDelete))
    }

    return list
}
