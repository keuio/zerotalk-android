package top.lanxint.zerotalk.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * [FullScreenDialog] 的 Android 实现：真正全屏 + 沉浸式
 *
 * 1. `decorFitsSystemWindows = false` —— 内容延伸到状态栏 / 导航栏之下。默认值是 true，
 *    对话框窗口会被系统栏「切掉」上下两条，图片查看器只铺满中间一块（此前点击放大图片
 *    看起来没有真正全屏，正是这个原因）。
 * 2. 打开期间**隐藏状态栏与导航栏**（沉浸式看图），用户从边缘滑动可临时唤出，
 *    关闭对话框时恢复显示。
 * 3. 内容通常是纯黑背景：系统栏图标切成浅色，临时唤出时也能看清。
 */
@Composable
actual fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // 平台独有参数：内容铺到系统栏之下（真正的全屏）
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val view = LocalView.current
        DisposableEffect(Unit) {
            // 对话框是独立窗口：系统栏的显隐与外观都要在它自己的窗口上设置
            val window = (view.parent as? DialogWindowProvider)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.let {
                it.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                it.isAppearanceLightStatusBars = false
                it.isAppearanceLightNavigationBars = false
                it.hide(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                // 关闭时显式恢复系统栏：正常路径下 Activity 窗口重新获得焦点后会自动恢复，
                // 这里再兜一次，避免个别机型/系统版本上系统栏留在隐藏态。
                runCatching { controller?.show(WindowInsetsCompat.Type.systemBars()) }
            }
        }
        content()
    }
}
