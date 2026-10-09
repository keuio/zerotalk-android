package top.lanxint.zerotalk.ui.utils

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android 平台相册权限与图片选择器实现
 */
@Composable
actual fun rememberPhotoPickerLauncher(
    maxItems: Int,
    onImagesSelected: (List<SelectedPhoto>) -> Unit,
    onPermissionDenied: () -> Unit
): PhotoPickerLauncher {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val handleUris: (List<Uri>) -> Unit = { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                val photos = uris.mapNotNull { uri ->
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        if (bytes != null && bytes.isNotEmpty()) {
                            SelectedPhoto(uriString = uri.toString(), byteArray = bytes)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    onImagesSelected(photos)
                }
            }
        }
    }

    // 单选照片 Launcher (maxItems <= 1 时使用，避免系统契约抛出 IllegalArgumentException)
    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) handleUris(listOf(uri))
    }

    // 多选照片 Launcher (仅当 maxItems > 1 时实际消费，占位构造使用 2 保证不抛异常)
    val multiPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = if (maxItems > 1) maxItems else 2)
    ) { uris: List<Uri> ->
        handleUris(uris)
    }

    val launchPicker = {
        val req = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        if (maxItems <= 1) {
            singlePickerLauncher.launch(req)
        } else {
            multiPickerLauncher.launch(req)
        }
    }

    // 2. 根据系统版本获取对应相册权限
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // 3. 权限请求 Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限授予成功，立即拉起相册选择
            launchPicker()
        } else {
            onPermissionDenied()
        }
    }

    return remember(singlePickerLauncher, multiPickerLauncher, permissionLauncher, context, permissionToRequest, maxItems) {
        object : PhotoPickerLauncher {
            override fun launch() {
                val isGranted = ContextCompat.checkSelfPermission(
                    context,
                    permissionToRequest
                ) == PackageManager.PERMISSION_GRANTED

                if (isGranted) {
                    // 已有相册权限，直接拉起相册
                    launchPicker()
                } else {
                    // 未获得权限，向系统请求相册权限
                    permissionLauncher.launch(permissionToRequest)
                }
            }
        }
    }
}
