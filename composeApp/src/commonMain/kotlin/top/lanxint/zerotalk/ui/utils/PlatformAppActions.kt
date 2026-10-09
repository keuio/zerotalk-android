package top.lanxint.zerotalk.ui.utils

import androidx.compose.runtime.Composable

/**
 * 平台层应用操作接口（原生 Toast 提示、退出应用等）
 */
interface PlatformAppActions {
    fun showToast(message: String)
    fun exitApp()
}

@Composable
expect fun rememberPlatformAppActions(): PlatformAppActions
