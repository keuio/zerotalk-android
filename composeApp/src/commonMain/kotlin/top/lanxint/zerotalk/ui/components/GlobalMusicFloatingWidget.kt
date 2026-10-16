package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.utils.GlobalMusicPlayerManager
import kotlin.math.roundToInt

/**
 * 全局音乐播放器自由拖拽吸附悬浮窗
 *
 * 遵循官方与 Apple HIG 设计语言：
 * 1. 自由拖拽与屏幕边缘平滑吸附；
 * 2. 状态双模切换：
 *    - 折叠微球态：黑胶唱片旋转封面 + 音符呼吸律动 + 加载指示器；
 *    - 展开药丸态：曲名、歌手、播放/暂停、关闭与微型进度条；
 * 3. 跨页面常驻，统一接管全局所有动态与聊天室音乐播放。
 */
@Composable
fun GlobalMusicFloatingWidget(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val isVisible by GlobalMusicPlayerManager.isVisible.collectAsState()
    val isExpanded by GlobalMusicPlayerManager.isExpanded.collectAsState()
    val isPlaying by GlobalMusicPlayerManager.isPlaying.collectAsState()
    val isLoading by GlobalMusicPlayerManager.isLoading.collectAsState()
    val currentTrack by GlobalMusicPlayerManager.currentTrack.collectAsState()
    val progressSec by GlobalMusicPlayerManager.progressSec.collectAsState()
    val durationSec by GlobalMusicPlayerManager.durationSec.collectAsState()

    if (!isVisible || currentTrack == null) return

    val higColors = AppleHigColors.colors(isDark)
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        val collapsedSizeDp = 54.dp
        val expandedWidthDp = 280.dp
        val expandedHeightDp = 64.dp

        val collapsedSizePx = with(density) { collapsedSizeDp.toPx() }
        val expandedWidthPx = with(density) { expandedWidthDp.toPx() }
        val expandedHeightPx = with(density) { expandedHeightDp.toPx() }

        val currentWidthPx = if (isExpanded) expandedWidthPx else collapsedSizePx
        val currentHeightPx = if (isExpanded) expandedHeightPx else collapsedSizePx

        val edgeMarginPx = with(density) { 16.dp.toPx() }
        val topMarginPx = with(density) { 70.dp.toPx() }
        val bottomMarginPx = with(density) { 100.dp.toPx() }

        // 默认停靠在右侧中上部
        var targetOffsetX by remember {
            mutableFloatStateOf(screenWidthPx - currentWidthPx - edgeMarginPx)
        }
        var targetOffsetY by remember {
            mutableFloatStateOf(screenHeightPx * 0.35f)
        }

        // 平滑吸附过渡动画
        val animatedOffsetX by animateFloatAsState(
            targetValue = targetOffsetX,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
        val animatedOffsetY by animateFloatAsState(
            targetValue = targetOffsetY,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )

        // 拖拽手势临时偏移
        var dragDeltaX by remember { mutableFloatStateOf(0f) }
        var dragDeltaY by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        val effectiveX = if (isDragging) targetOffsetX + dragDeltaX else animatedOffsetX
        val effectiveY = if (isDragging) targetOffsetY + dragDeltaY else animatedOffsetY

        // 黑胶唱片旋转动效
        val infiniteTransition = rememberInfiniteTransition()
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        val discRotation = if (isPlaying) rotationAngle else 0f

        val safeX = effectiveX.coerceIn(edgeMarginPx, (screenWidthPx - currentWidthPx - edgeMarginPx).coerceAtLeast(edgeMarginPx))
        val safeY = effectiveY.coerceIn(topMarginPx, (screenHeightPx - currentHeightPx - bottomMarginPx).coerceAtLeast(topMarginPx))

        Box(
            modifier = Modifier
                .offset { IntOffset(safeX.roundToInt(), safeY.roundToInt()) }
                .pointerInput(screenWidthPx, screenHeightPx, isExpanded) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragDeltaX = 0f
                            dragDeltaY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDeltaX += dragAmount.x
                            dragDeltaY += dragAmount.y
                        },
                        onDragEnd = {
                            val finalX = targetOffsetX + dragDeltaX
                            val finalY = (targetOffsetY + dragDeltaY).coerceIn(
                                topMarginPx,
                                screenHeightPx - currentHeightPx - bottomMarginPx
                            )
                            // 边缘吸附：判断在中线偏左还是偏右
                            val centerX = finalX + currentWidthPx / 2
                            targetOffsetX = if (centerX < screenWidthPx / 2) {
                                edgeMarginPx
                            } else {
                                screenWidthPx - currentWidthPx - edgeMarginPx
                            }
                            targetOffsetY = finalY
                            dragDeltaX = 0f
                            dragDeltaY = 0f
                            isDragging = false
                        },
                        onDragCancel = {
                            dragDeltaX = 0f
                            dragDeltaY = 0f
                            isDragging = false
                        }
                    )
                }
        ) {
            val track = currentTrack ?: return@Box

            if (isExpanded) {
                // ==================== 展开态：迷你药丸控制器 ====================
                val pillShape = RoundedCornerShape(32.dp)
                val pillBg = if (isDark) Color(0xF01C1C1E) else Color(0xF5FFFFFF)
                val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

                Row(
                    modifier = Modifier
                        .size(width = expandedWidthDp, height = expandedHeightDp)
                        .shadow(elevation = 12.dp, shape = pillShape, ambientColor = Color(0x33000000))
                        .clip(pillShape)
                        .background(pillBg)
                        .border(1.dp, borderColor, pillShape)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 黑胶封面（可点击折叠回微球）
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF111111))
                            .border(1.5.dp, Color(0xFFE11D48), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                GlobalMusicPlayerManager.setExpanded(false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncNetworkImage(
                            url = track.coverUrl,
                            contentDescription = "唱片封面",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .rotate(discRotation)
                        )
                        // 黑胶中心孔
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(pillBg)
                                .border(1.dp, Color(0x66FFFFFF), CircleShape)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // 歌名 + 歌手 + 微型进度条
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                GlobalMusicPlayerManager.togglePlayPause()
                            }
                    ) {
                        BasicText(
                            text = track.name,
                            style = TextStyle(
                                color = higColors.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(1.dp))
                        BasicText(
                            text = track.artist,
                            style = TextStyle(
                                color = higColors.secondaryLabel,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))

                        // 线性进度槽
                        val progressFraction = if (durationSec > 0) {
                            (progressSec.toFloat() / durationSec).coerceIn(0f, 1f)
                        } else 0f

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(2.5.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFE11D48), Color(0xFFFB7185))
                                        )
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // 控制按钮行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 播放 / 暂停 / 加载中按钮
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    GlobalMusicPlayerManager.togglePlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "暂停" else "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 收起为微球按钮
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    GlobalMusicPlayerManager.setExpanded(false)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "收起",
                                tint = higColors.secondaryLabel,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 关闭并停止播放按钮
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    GlobalMusicPlayerManager.close()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = higColors.secondaryLabel,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // ==================== 折叠态：微型黑胶悬浮球 ====================
                Box(
                    modifier = Modifier
                        .size(collapsedSizeDp)
                        .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = Color(0x44000000))
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E222B) else Color(0xFFF8FAFC))
                        .border(
                            1.5.dp,
                            if (isPlaying) Color(0xFFE11D48) else (if (isDark) Color(0x33FFFFFF) else Color(0x22000000)),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            GlobalMusicPlayerManager.setExpanded(true)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 黑胶封面
                    AsyncNetworkImage(
                        url = track.coverUrl,
                        contentDescription = "唱片封面",
                        modifier = Modifier
                            .size(collapsedSizeDp - 6.dp)
                            .clip(CircleShape)
                            .rotate(discRotation)
                    )

                    // 唱片中心微孔
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                            .border(1.dp, Color(0x55FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = Color(0xFFE11D48)
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isPlaying) Color(0xFFE11D48) else Color.White,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
