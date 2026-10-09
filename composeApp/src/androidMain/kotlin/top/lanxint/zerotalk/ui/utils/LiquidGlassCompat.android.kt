package top.lanxint.zerotalk.ui.utils

import com.kashif_e.backdrop.platform.PlatformCapabilities

/**
 * Android 实现：直接驱动 KMPLiquidGlass 的运行时渲染能力开关。
 *
 * [PlatformCapabilities.forceLegacyRendering] 是库内所有 AGSL / RuntimeShader 分支的唯一判据，
 * 因此这里一处赋值即可让全 App 的液态玻璃组件同步降级，无需逐组件改造。
 */
actual fun applyLiquidGlassCompatMode(enabled: Boolean) {
    PlatformCapabilities.forceLegacyRendering = enabled
}
