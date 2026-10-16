package top.lanxint.zerotalk.ui.utils

import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.lanxint.zerotalk.data.log.ZtLog

actual suspend fun extractRemoteAudioDurationSec(audioUrl: String): Int = withContext(Dispatchers.IO) {
    val cleanUrl = audioUrl.trim()
    if (cleanUrl.isBlank()) return@withContext 0

    val fullUrl = when {
        cleanUrl.startsWith("//") -> "https:$cleanUrl"
        cleanUrl.startsWith("/") -> "https://app.zerotalk.cn$cleanUrl"
        else -> cleanUrl
    }

    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(fullUrl, HashMap<String, String>())
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L
        if (durationMs > 0L) {
            // 四舍五入到最近的秒数，最短计为 1 秒
            val sec = ((durationMs + 500L) / 1000L).toInt().coerceAtLeast(1)
            sec
        } else {
            0
        }
    } catch (e: Throwable) {
        ZtLog.w("VoiceDurationResolver", "Failed to extract duration for $fullUrl: ${e.message}")
        0
    } finally {
        runCatching { retriever.release() }
    }
}
