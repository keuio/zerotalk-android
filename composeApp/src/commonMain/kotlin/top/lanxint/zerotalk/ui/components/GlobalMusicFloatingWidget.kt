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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.utils.GlobalMusicPlayerManager
import kotlin.math.roundToInt

/**
 * 格式化播放时间（秒 -> mm:ss）
 */
private fun formatTime(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    val m = s / 60
    val rem = s % 60
    val mStr = if (m < 10) "0$m" else "$m"
    val sStr = if (rem < 10) "0$rem" else "$rem"
    return "$mStr:$sStr"
}

/**
 * 全局音乐播放器自由拖拽吸附悬浮窗
 *
 * 遵循官方与 Apple HIG 设计语言：
 * 1. 自由拖拽与屏幕边缘平滑吸附；
 * 2. 状态双模切换：
 *    - 折叠微球态：黑胶唱片旋转封面 + 音符呼吸律动 + 加载指示器；
 *    - 展开精致卡片面板：
 *      * 黑胶封面、曲名、歌手与折叠/关闭按钮；
 *      * KMP Glass Slider 玻璃质感拖动进度条与已播/总时长；
 *      * 上一首 / 播放(暂停) / 下一首 控制按钮；
 *      * 播放列表抽屉开关与曲目管理（点击播放、移除单曲、一键清空）；
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
    val playlist by GlobalMusicPlayerManager.playlist.collectAsState()
    val currentIndex by GlobalMusicPlayerManager.currentIndex.collectAsState()
    val progressSec by GlobalMusicPlayerManager.progressSec.collectAsState()
    val durationSec by GlobalMusicPlayerManager.durationSec.collectAsState()

    var showPlaylistDrawer by remember { mutableStateOf(false) }
    var localSeekValue by remember { mutableFloatStateOf(-1f) }

    if (!isVisible || currentTrack == null) return

    val higColors = AppleHigColors.colors(isDark)
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        val collapsedSizeDp = 54.dp
        val expandedWidthDp = 300.dp
        val baseExpandedHeightDp = 142.dp
        val drawerHeightDp = if (showPlaylistDrawer) 180.dp else 0.dp
        val currentEstimatedHeightDp = if (isExpanded) baseExpandedHeightDp + drawerHeightDp else collapsedSizeDp

        val collapsedSizePx = with(density) { collapsedSizeDp.toPx() }
        val expandedWidthPx = with(density) { expandedWidthDp.toPx() }
        val estimatedHeightPx = with(density) { currentEstimatedHeightDp.toPx() }

        val currentWidthPx = if (isExpanded) expandedWidthPx else collapsedSizePx
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
        val safeY = effectiveY.coerceIn(topMarginPx, (screenHeightPx - estimatedHeightPx - bottomMarginPx).coerceAtLeast(topMarginPx))

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
                                screenHeightPx - estimatedHeightPx - bottomMarginPx
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
                // ==================== 展开态：卡片式控制面板 ====================
                val cardShape = RoundedCornerShape(22.dp)
                val cardBg = if (isDark) Color(0xF21C1E26) else Color(0xF7FFFFFF)
                val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

                Column(
                    modifier = Modifier
                        .width(expandedWidthDp)
                        .shadow(elevation = 16.dp, shape = cardShape, ambientColor = Color(0x33000000))
                        .clip(cardShape)
                        .background(cardBg)
                        .border(1.dp, borderColor, cardShape)
                ) {
                    // 1. 顶部栏：黑胶封面 + 歌曲信息 + 收起/关闭按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, start = 12.dp, end = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 唱片旋转微缩封面
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .rotate(discRotation)
                            )
                            // 黑胶中心轴孔
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(cardBg)
                                    .border(1.dp, Color(0x66FFFFFF), CircleShape)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // 歌名与歌手
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
                            Spacer(Modifier.height(2.dp))
                            BasicText(
                                text = track.artist,
                                style = TextStyle(
                                    color = higColors.secondaryLabel,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(6.dp))

                        // 收起为微球按钮
                        Box(
                            modifier = Modifier
                                .size(28.dp)
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

                        // 关闭按钮
                        Box(
                            modifier = Modifier
                                .size(28.dp)
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
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // 2. 进度滑块区域 (KMP GlassSlider + 时间标签)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        val displayProgress = if (localSeekValue >= 0) localSeekValue else progressSec.toFloat()
                        val maxDuration = durationSec.coerceAtLeast(1).toFloat()

                        GlassSlider(
                            value = displayProgress,
                            valueRange = 0f..maxDuration,
                            onValueChange = { localSeekValue = it },
                            onValueChangeFinished = {
                                if (localSeekValue >= 0) {
                                    GlobalMusicPlayerManager.seekTo(localSeekValue.roundToInt())
                                    localSeekValue = -1f
                                }
                            },
                            isDark = isDark
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BasicText(
                                text = formatTime(displayProgress.toInt()),
                                style = TextStyle(
                                    color = higColors.tertiaryLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            BasicText(
                                text = formatTime(durationSec),
                                style = TextStyle(
                                    color = higColors.tertiaryLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // 3. 核心控制栏：上一首 / 播放(暂停) / 下一首 / 播放列表开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 播放列表抽屉开关
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (showPlaylistDrawer) Color(0xFFE11D48).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showPlaylistDrawer = !showPlaylistDrawer
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = "播放列表",
                                    tint = if (showPlaylistDrawer) Color(0xFFE11D48) else higColors.secondaryLabel,
                                    modifier = Modifier.size(19.dp)
                                )
                                if (playlist.isNotEmpty()) {
                                    BasicText(
                                        text = "${playlist.size}",
                                        style = TextStyle(
                                            color = if (showPlaylistDrawer) Color(0xFFE11D48) else higColors.secondaryLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // 上一首
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    GlobalMusicPlayerManager.playPrevious()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "上一首",
                                tint = higColors.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // 播放 / 暂停 / 加载中
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0xFFE11D48))
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
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "暂停" else "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // 下一首
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    GlobalMusicPlayerManager.playNext()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "下一首",
                                tint = higColors.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // 右侧配重占位（保持播放键居中）
                        Spacer(Modifier.width(36.dp))
                    }

                    // 4. 展开式播放列表抽屉
                    AnimatedVisibility(
                        visible = showPlaylistDrawer,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDark) Color(0x1A000000) else Color(0x0A000000))
                        ) {
                            // 分隔线
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            )

                            // 列表头部：曲目数量与清空操作
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    text = "播放队列 (${playlist.size})",
                                    style = TextStyle(
                                        color = higColors.secondaryLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(Modifier.weight(1f))
                                if (playlist.isNotEmpty()) {
                                    BasicText(
                                        text = "清空",
                                        style = TextStyle(
                                            color = Color(0xFFE11D48),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            GlobalMusicPlayerManager.clearPlaylist()
                                        }
                                    )
                                }
                            }

                            if (playlist.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        text = "队列暂无曲目",
                                        style = TextStyle(
                                            color = higColors.tertiaryLabel,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 160.dp)
                                        .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                                ) {
                                    itemsIndexed(playlist, key = { index, item -> "${item.songId}_$index" }) { index, item ->
                                        val isCurrent = index == currentIndex
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isCurrent) {
                                                        if (isDark) Color(0x22E11D48) else Color(0x14E11D48)
                                                    } else Color.Transparent
                                                )
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    GlobalMusicPlayerManager.playAt(index)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = "正在播放",
                                                    tint = Color(0xFFE11D48),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            } else {
                                                BasicText(
                                                    text = "${index + 1}",
                                                    style = TextStyle(
                                                        color = higColors.tertiaryLabel,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    modifier = Modifier.width(14.dp)
                                                )
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                BasicText(
                                                    text = item.name,
                                                    style = TextStyle(
                                                        color = if (isCurrent) Color(0xFFE11D48) else higColors.label,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                BasicText(
                                                    text = item.artist,
                                                    style = TextStyle(
                                                        color = higColors.secondaryLabel,
                                                        fontSize = 10.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(Modifier.width(6.dp))

                                            // 移除单曲按钮
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        GlobalMusicPlayerManager.removeFromPlaylist(index)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "移除",
                                                    tint = higColors.tertiaryLabel,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
