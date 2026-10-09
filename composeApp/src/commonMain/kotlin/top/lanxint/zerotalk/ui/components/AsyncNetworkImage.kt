package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.data.model.IosContactAvatarGradient
import top.lanxint.zerotalk.data.network.NetworkImageUrl
import top.lanxint.zerotalk.data.network.ZeroTalkApiService
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.ui.utils.decodeByteArrayToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

import java.io.File

/**
 * 内存与磁盘图片缓存与加载单例
 */
object NetworkImageLoader {
    /** 位图缓存上限（按解码后像素估算，约 48MB），避免九宫格大图把内存吃光 */
    private const val MAX_CACHE_BYTES = 48L * 1024 * 1024

    /** 常用解码尺寸档位：头像（低内存）、列表卡片与缩略图、大图原图 */
    const val DIMENSION_AVATAR = 256
    const val DIMENSION_THUMB = 640
    const val DIMENSION_ORIGINAL = 1280

    private val memoryCache = object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {}
    private var cacheBytes = 0L

    @Volatile
    private var imageClient: OkHttpClient? = null

    /**
     * 在 App 启动时注入磁盘缓存目录（配置 100MB 二级磁盘缓存与智能协商）
     */
    fun init(cacheDir: File) {
        try {
            val diskCache = okhttp3.Cache(File(cacheDir, "image_cache"), 100L * 1024 * 1024)
            imageClient = ZeroTalkClientManager.apiService.client.newBuilder()
                .cache(diskCache)
                .addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    val cacheControl = response.header("Cache-Control")
                    // 如果响应成功且未提供强缓存头，赋予 7 天本地磁盘缓存，极大提高列表滑动二次秒开率
                    if (response.isSuccessful && (cacheControl.isNullOrBlank() || cacheControl.contains("no-cache"))) {
                        response.newBuilder()
                            .header("Cache-Control", "public, max-age=604800")
                            .removeHeader("Pragma")
                            .build()
                    } else {
                        response
                    }
                }
                .build()
        } catch (_: Exception) {
            imageClient = null
        }
    }

    /**
     * 复用零语已鉴权的 OkHttpClient（自动携带会话 Cookie、X-Device-Id 与统一 UA），
     * 优先使用配置了 100MB 磁盘二级缓存的图片专用客户端。
     */
    private val client: OkHttpClient
        get() = imageClient ?: ZeroTalkClientManager.apiService.client

    /**
     * 把接口返回的相对路径 / 协议相对路径补全为绝对地址
     */
    fun resolveImageUrl(raw: String): String = NetworkImageUrl.resolve(raw)

    suspend fun loadImage(
        url: String,
        preferOriginal: Boolean = false,
        maxDimension: Int = if (preferOriginal) DIMENSION_ORIGINAL else DIMENSION_THUMB
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return@withContext null

        val cacheKey = if (preferOriginal) "$trimmed#orig" else "$trimmed#dim=$maxDimension"
        synchronized(memoryCache) {
            memoryCache[cacheKey]?.let { return@withContext it }
        }

        val plainUrl = NetworkImageUrl.resolve(trimmed)
        // 与官方 web 端一致：OSS 图片默认优先取 style/zerotalk_thumb 缩略图；大图查看器优先取原图
        val thumbUrl = NetworkImageUrl.applyOssStyle(plainUrl, NetworkImageUrl.OssStyle.Thumb)
        val candidates = if (preferOriginal) {
            if (thumbUrl != plainUrl) listOf(plainUrl, thumbUrl) else listOf(plainUrl)
        } else {
            if (thumbUrl != plainUrl) listOf(thumbUrl, plainUrl) else listOf(plainUrl)
        }

        for (candidate in candidates) {
            val bitmap = fetchBitmap(candidate, maxDimension)
            if (bitmap != null) {
                cacheBitmap(cacheKey, bitmap)
                return@withContext bitmap
            }
        }
        null
    }

    private fun fetchBitmap(resolvedUrl: String, maxDimension: Int): ImageBitmap? = try {
        val request = Request.Builder()
            .url(resolvedUrl)
            // 与官方前端一致，带上来源页，避免图片端点/对象存储的防盗链拦截
            .header("Referer", "${ZeroTalkApiService.BASE_URL}/")
            .build()

        val bytes = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.bytes()
        }
        bytes?.let { decodeByteArrayToImageBitmap(it, maxDimension) }
    } catch (e: Exception) {
        null
    }

    private fun cacheBitmap(key: String, bitmap: ImageBitmap) {
        synchronized(memoryCache) {
            val bytes = bitmap.width.toLong() * bitmap.height * 4
            memoryCache.put(key, bitmap)?.let { previous ->
                cacheBytes -= previous.width.toLong() * previous.height * 4
            }
            cacheBytes += bytes

            val iterator = memoryCache.entries.iterator()
            while (cacheBytes > MAX_CACHE_BYTES && iterator.hasNext()) {
                val entry = iterator.next()
                cacheBytes -= entry.value.width.toLong() * entry.value.height * 4
                iterator.remove()
            }
        }
    }
}

/**
 * 异步网络图片组件
 *
 * @param showPlaceholder 图片加载中/失败时是否展示自带的转圈与占位图标。
 *        为 false 时不遮挡父容器自带的占位（例如 [UserAvatar] 的渐变圆环与首字母）。
 */
@Composable
fun AsyncNetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = Color.Gray.copy(alpha = 0.12f),
    showPlaceholder: Boolean = true,
    maxDimension: Int = NetworkImageLoader.DIMENSION_THUMB
) {
    var bitmap by remember(url, maxDimension) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(url, maxDimension) { mutableStateOf(true) }

    LaunchedEffect(url, maxDimension) {
        if (url.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        val loaded = NetworkImageLoader.loadImage(url, preferOriginal = false, maxDimension = maxDimension)
        bitmap = loaded
        isLoading = false
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading && showPlaceholder) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF007AFF).copy(alpha = 0.6f)
            )
        } else if (!isLoading && showPlaceholder) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "图片预览",
                tint = Color.Gray.copy(alpha = 0.35f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 通用用户头像：真实头像优先，加载中/失败/无头像时回落为渐变圆环 + 首字母
 *
 * 全站头像统一走本组件，避免各页面各自用「渐变 + 首字母」而永远不显示真实头像。
 */
@Composable
fun UserAvatar(
    url: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = CircleShape,
    gradient: List<Color> = IosContactAvatarGradient,
    fallbackTextStyle: TextStyle = AppleHigTypography.headline,
    fallbackIconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center
    ) {
        val initial = name.trim().take(1).uppercase()
        if (initial.isNotBlank()) {
            BasicText(
                text = initial,
                style = fallbackTextStyle.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "用户头像",
                tint = Color.White,
                modifier = Modifier.size(fallbackIconSize)
            )
        }

        if (!url.isNullOrBlank()) {
            AsyncNetworkImage(
                url = url,
                contentDescription = "$name 的头像",
                shape = shape,
                backgroundColor = Color.Transparent,
                showPlaceholder = false,
                maxDimension = NetworkImageLoader.DIMENSION_AVATAR,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
