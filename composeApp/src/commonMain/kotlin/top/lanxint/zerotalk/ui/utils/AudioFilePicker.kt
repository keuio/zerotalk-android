package top.lanxint.zerotalk.ui.utils

import androidx.compose.runtime.Composable

/**
 * 本地选中的音频文件实体
 */
data class SelectedAudioFile(
    val filename: String,
    val byteArray: ByteArray,
    val mimeType: String,
    val ext: String,
    /** 音频时长（秒）。服务端不下发语音时长，本地回显必须自带，否则气泡显示 0" */
    val durationSec: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectedAudioFile) return false
        return filename == other.filename && ext == other.ext && byteArray.contentEquals(other.byteArray)
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + ext.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        return result
    }
}

/**
 * 平台音频文件选取启动器
 */
interface AudioFilePickerLauncher {
    fun launch()
}

/**
 * 平台音频文件选取器
 */
@Composable
expect fun rememberAudioFilePickerLauncher(
    onAudioSelected: (SelectedAudioFile) -> Unit,
    onPermissionDenied: () -> Unit
): AudioFilePickerLauncher
