package top.lanxint.zerotalk.ui.components

import androidx.compose.runtime.Composable

/**
 * 全屏对话框容器（图片查看器等沉浸式覆盖层专用）
 *
 * 与普通 [androidx.compose.ui.window.Dialog] 的区别：内容**延伸到状态栏与导航栏之下**，
 * 真正铺满整屏。Compose 的 `DialogProperties.decorFitsSystemWindows` 是 Android 平台独有参数
 * （common 侧的 expect 声明里没有），因此这里用 expect/actual 把它隔离在平台实现里。
 *
 * 使用方：图片全屏查看器 —— 背景纯黑，系统栏图标需要切成浅色，这些也在 actual 里一并处理。
 *
 * @param onDismissRequest 点击外部 / 返回键 / 关闭时的回调
 * @param content 全屏内容（调用方负责 fillMaxSize 与背景色）
 */
@Composable
expect fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
)
