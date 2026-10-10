package top.lanxint.zerotalk.ui.utils

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android 平台音频文件选取实现
 * 基于 SAF (ActivityResultContracts.GetContent) 实现安全的本地音频文件读取
 */
@Composable
actual fun rememberAudioFilePickerLauncher(
    onAudioSelected: (SelectedAudioFile) -> Unit,
    onPermissionDenied: () -> Unit
): AudioFilePickerLauncher {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // 1. 获取文件名
                var displayName: String? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }
                val rawName = displayName?.takeIf { it.isNotBlank() } ?: "audio_${System.currentTimeMillis()}.mp3"

                // 2. 获取 MIME 类型与扩展名
                val rawMime = context.contentResolver.getType(uri)?.takeIf { it.isNotBlank() }
                val ext = rawName.substringAfterLast('.', "").ifBlank {
                    when (rawMime) {
                        "audio/wav", "audio/x-wav" -> "wav"
                        "audio/webm" -> "webm"
                        "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
                        "audio/ogg" -> "ogg"
                        else -> "mp3"
                    }
                }
                val mimeType = rawMime ?: when (ext.lowercase()) {
                    "wav" -> "audio/wav"
                    "webm" -> "audio/webm"
                    "m4a", "mp4" -> "audio/mp4"
                    "ogg" -> "audio/ogg"
                    else -> "audio/mpeg"
                }

                // 3. 读取音频字节
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    // 3.5 读音频时长：服务端不下发语音时长（官方同样靠音频元数据现算），
                    // 本地回显必须自带，否则气泡会显示 0"
                    val durationSec = runCatching {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, uri)
                            val ms = retriever
                                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toLongOrNull() ?: 0L
                            (ms / 1000L).toInt()
                        } finally {
                            runCatching { retriever.release() }
                        }
                    }.getOrDefault(0)

                    val result = SelectedAudioFile(
                        filename = rawName,
                        byteArray = bytes,
                        mimeType = mimeType,
                        ext = ext,
                        durationSec = durationSec
                    )
                    withContext(Dispatchers.Main) {
                        onAudioSelected(result)
                    }
                }
            } catch (e: Exception) {
                // 读取失败时不崩溃
            }
        }
    }

    return remember {
        object : AudioFilePickerLauncher {
            override fun launch() {
                try {
                    filePickerLauncher.launch("audio/*")
                } catch (e: Exception) {
                    onPermissionDenied()
                }
            }
        }
    }
}
