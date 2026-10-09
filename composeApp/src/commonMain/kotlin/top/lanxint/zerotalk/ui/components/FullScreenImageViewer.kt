package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 全局图片全屏查看器状态控制器
 */
@Stable
class ImageViewerState {
    var images by mutableStateOf<List<String>?>(null)
        private set
    var initialIndex by mutableStateOf(0)
        private set

    val isVisible: Boolean
        get() = !images.isNullOrEmpty()

    fun open(images: List<String>, initialIndex: Int = 0) {
        val valid = images.filter { it.isNotBlank() }
        if (valid.isNotEmpty()) {
            this.images = valid
            this.initialIndex = initialIndex.coerceIn(0, valid.size - 1)
        }
    }

    fun open(imageUrl: String) {
        if (imageUrl.isNotBlank()) {
            open(listOf(imageUrl), 0)
        }
    }

    fun dismiss() {
        images = null
        initialIndex = 0
    }
}

val LocalImageViewer = compositionLocalOf { ImageViewerState() }

/**
 * 全局图片查看器宿主提供者
 */
@Composable
fun ImageViewerProvider(
    state: ImageViewerState = remember { ImageViewerState() },
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalImageViewer provides state) {
        content()
        if (state.isVisible && state.images != null) {
            FullScreenImageViewer(
                images = state.images!!,
                initialIndex = state.initialIndex,
                onDismiss = { state.dismiss() }
            )
        }
    }
}

/**
 * 沉浸式多功能全屏大图查看器
 *
 * 特性：
 * 1. 纯黑沉浸式全屏背景；
 * 2. 多图支持 HorizontalPager 滑动翻页与顶部/顶部页码指示器（如 1/3）；
 * 3. 支持双击放大/还原（1.0x <-> 2.5x）；
 * 4. 支持双指捏合缩放（1.0x ~ 4.5x）及放大后的平移浏览；
 * 5. 未放大时单指左右滑自然交由 Pager 翻页，互不冲突；
 * 6. 单击任意位置退出，右上角附带半透明关闭按钮；
 * 7. 原图自适应展示，原图优先解码加载。
 */
@Composable
fun FullScreenImageViewer(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val validIndex = initialIndex.coerceIn(0, images.size - 1)
            val pagerState = rememberPagerState(
                initialPage = validIndex,
                pageCount = { images.size }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val imageUrl = images.getOrNull(page).orEmpty()
                ZoomableImagePage(
                    imageUrl = imageUrl,
                    isCurrentPage = pagerState.currentPage == page,
                    onDismiss = onDismiss
                )
            }

            // 顶部页码指示器 (多图时展示)
            if (images.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // 右上角半透明关闭按钮
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 14.dp, end = 16.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭预览",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 单页手势缩放图片组件
 */
@Composable
private fun ZoomableImagePage(
    imageUrl: String,
    isCurrentPage: Boolean,
    onDismiss: () -> Unit
) {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        bitmap = NetworkImageLoader.loadImage(imageUrl, preferOriginal = true)
        isLoading = false
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 手势 block 不再以 scale 为 key（见下），这里保证 onDismiss 始终是最新回调
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // 切换离开当前页面时复位缩放与位移
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // 说明：block 内读取的 scale 是 remember 的 MutableState，闭包每次执行都取当前值，
            // 因此不需要把 scale 当 key——把会被本 block 自身修改的状态当 key 会让手势不断重启。
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnDismiss() },
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.size
                        if (pointerCount >= 2) {
                            // 双指缩放
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val targetScale = (scale * zoomChange).coerceIn(1f, 4.5f)
                            scale = targetScale
                            if (targetScale > 1f) {
                                offset += panChange
                            } else {
                                offset = Offset.Zero
                            }
                            event.changes.forEach { it.consume() }
                        } else if (pointerCount == 1 && scale > 1.05f) {
                            // 放大状态下的单指拖拽平移
                            val panChange = event.calculatePan()
                            offset += panChange
                            event.changes.forEach { it.consume() }
                        }
                        // pointerCount == 1 且 scale <= 1.05f 时不拦截，让位给 HorizontalPager
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val viewportW = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()

        // 限制平移范围
        val maxPanX = maxOf(0f, (viewportW * (scale - 1f)) / 2f)
        val maxPanY = maxOf(0f, (viewportH * (scale - 1f)) / 2f)
        val clampedOffset = remember(offset, scale, maxPanX, maxPanY) {
            Offset(
                x = offset.x.coerceIn(-maxPanX, maxPanX),
                y = offset.y.coerceIn(-maxPanY, maxPanY)
            )
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "查看大图",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = clampedOffset.x
                        translationY = clampedOffset.y
                    }
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = Color.White.copy(alpha = 0.85f)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "加载失败",
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
