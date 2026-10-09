package top.lanxint.zerotalk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 全局 Apple HIG 组合本地变量 (CompositionLocal)
 */
val LocalAppleHigColors = staticCompositionLocalOf<AppleHigColorTokens> { AppleHigColors.Light }

/**
 * 外部便捷调用的单例入口
 *
 * 用法：
 * - 颜色：`AppleHigTheme.colors.label`
 * - 排版：`AppleHigTheme.typography.headline`
 */
object AppleHigTheme {
    val colors: AppleHigColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppleHigColors.current

    val typography: AppleHigTypography
        get() = AppleHigTypography
}

/**
 * Apple HIG 设计体系主题组合根入口
 *
 * 职责：
 * 1. 挂载 [LocalAppleHigColors]，使得子组件无需逐级透传 `isDark` 即可获取当前模式下的语义色彩；
 * 2. 桥接并挂载至 [MaterialTheme]，防止 Material 3 组件、水波纹（Ripple）及输入指示器回退为默认的 Material 原生紫色。
 */
@Composable
fun AppleHigTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val higColors = AppleHigColors.colors(darkTheme)

    val m3ColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = higColors.systemBlue,
            background = higColors.systemBackground,
            surface = higColors.secondarySystemBackground,
            error = higColors.systemRed,
            onPrimary = Color.White,
            onBackground = higColors.label,
            onSurface = higColors.label,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = higColors.systemBlue,
            background = higColors.systemBackground,
            surface = higColors.secondarySystemBackground,
            error = higColors.systemRed,
            onPrimary = Color.White,
            onBackground = higColors.label,
            onSurface = higColors.label,
            onError = Color.White
        )
    }

    CompositionLocalProvider(
        LocalAppleHigColors provides higColors
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            content = content
        )
    }
}
