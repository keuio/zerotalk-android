package top.lanxint.zerotalk.ui.utils

import androidx.compose.runtime.Composable

/**
 * 本地相册选中的图片实体
 */
data class SelectedPhoto(
    val id: String = "${System.currentTimeMillis()}_${(1000..9999).random()}",
    val uriString: String,
    val byteArray: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelectedPhoto) return false
        return id == other.id && uriString == other.uriString
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uriString.hashCode()
        return result
    }
}

/**
 * 平台相册启动器接口
 */
interface PhotoPickerLauncher {
    fun launch()
}

/**
 * 平台相册权限与多选启动器
 * - 若未获得相册权限，则向系统请求用户的相册权限（Android 13+ READ_MEDIA_IMAGES，低于 13 则 READ_EXTERNAL_STORAGE）
 * - 获得权限后拉起相册选择器（最多 maxItems 张）
 * - 用户拒绝权限时回调 onPermissionDenied
 */
@Composable
expect fun rememberPhotoPickerLauncher(
    maxItems: Int,
    onImagesSelected: (List<SelectedPhoto>) -> Unit,
    onPermissionDenied: () -> Unit
): PhotoPickerLauncher
