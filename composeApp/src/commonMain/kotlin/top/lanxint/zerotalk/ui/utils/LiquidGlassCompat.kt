package top.lanxint.zerotalk.ui.utils

/**
 * 液态玻璃渲染能力总闸（平台桥接）
 *
 * 由「我的 → 显示与外观 → 兼容渲染模式」驱动：开启后强制按 **Android 13 以下（API 31–32）**
 * 的渲染能力执行——关闭全部 AGSL / RuntimeShader 类特效（`lens` 折射与色散、`vibrancy`、
 * `progressiveBlur` 渐隐蒙版、`reflectiveGlass`、SDF、`gamma` / `exposure`），
 * 仅保留模糊、染色、高光与阴影，视觉回落到「经典磨砂玻璃」。
 *
 * 默认关闭，即完全跟随真机能力，不改变既有渲染表现。
 */
expect fun applyLiquidGlassCompatMode(enabled: Boolean)
