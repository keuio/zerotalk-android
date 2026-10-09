package com.kashif_e.backdrop.platform

import android.graphics.BlurMaskFilter
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.MaskFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Paint

/**
 * Platform abstraction shim to pave the path to Compose Multiplatform/iOS.
 * Currently implemented in terms of Android classes so existing behavior is preserved.
 */
object PlatformCapabilities {
    /**
     * 强制降级渲染开关（由宿主 App 的「兼容渲染模式」驱动，见 [forceLegacyRendering]）。
     *
     * 置为 `true` 时，无论真机 API 等级如何，一律按 Android 12 及以下（API 31–32）的渲染能力执行：
     * 关闭全部 AGSL / RuntimeShader 类特效——[com.kashif_e.backdrop.effects.lens] 折射与色散、
     * vibrancy、progressiveBlur 的渐隐蒙版、reflectiveGlass、SDF 以及 gamma / exposure 调整；
     * 仅保留模糊（blur）、染色（colorControls / opacity）、高光与阴影，
     * 视觉回落到「经典磨砂玻璃」，可显著降低 RenderThread 的 GPU 开销。
     *
     * 默认为 `false`，即完全跟随真机能力，不改变既有渲染行为。
     */
    @Volatile
    var forceLegacyRendering: Boolean = false

    /** 真机自身是否具备 AGSL RuntimeShader 能力（Android 13 / API 33 起） */
    private val runtimeShaderSupportedByDevice: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val supportsRuntimeShader: Boolean
        get() = !forceLegacyRendering && runtimeShaderSupportedByDevice

    /**
     * 模糊 / 色彩滤镜 / RenderEffect 均为 Android 12（API 31）起具备的能力，
     * 属于「安卓 13 以下」效果中依然保留的部分，故不受 [forceLegacyRendering] 影响。
     */
    val supportsBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supportsColorFilter: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supportsRenderEffect: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supportsBlurMaskFilter: Boolean = true // Always available on Android
}

/**
 * Platform abstraction for blur mask filters used in shadow/highlight effects.
 */
sealed class PlatformBlurMaskFilter {
    data class Android(val maskFilter: MaskFilter) : PlatformBlurMaskFilter()
    data object None : PlatformBlurMaskFilter()

    companion object {
        fun create(radius: Float, style: BlurStyle = BlurStyle.NORMAL): PlatformBlurMaskFilter {
            if (radius <= 0f) return None
            val blurType = when (style) {
                BlurStyle.NORMAL -> BlurMaskFilter.Blur.NORMAL
                BlurStyle.SOLID -> BlurMaskFilter.Blur.SOLID
                BlurStyle.OUTER -> BlurMaskFilter.Blur.OUTER
                BlurStyle.INNER -> BlurMaskFilter.Blur.INNER
            }
            return Android(BlurMaskFilter(radius, blurType))
        }
    }
}

enum class BlurStyle {
    NORMAL, SOLID, OUTER, INNER
}

/**
 * Extension to apply a platform blur mask filter to a Compose Paint.
 */
fun Paint.setPlatformMaskFilter(filter: PlatformBlurMaskFilter?) {
    val frameworkPaint = this.asFrameworkPaint()
    frameworkPaint.maskFilter = when (filter) {
        is PlatformBlurMaskFilter.Android -> filter.maskFilter
        PlatformBlurMaskFilter.None, null -> null
    }
}

sealed class PlatformRenderEffect {
    data class Android(val renderEffect: RenderEffect) : PlatformRenderEffect()
    data object None : PlatformRenderEffect()

    companion object {
        fun blur(
            @FloatRange(from = 0.0) radius: Float,
            tileMode: Shader.TileMode = Shader.TileMode.CLAMP
        ): PlatformRenderEffect {
            if (!PlatformCapabilities.supportsBlur || radius <= 0f) return None
            val effect =
                RenderEffect.createBlurEffect(
                    radius,
                    radius,
                    tileMode
                )
            return Android(effect)
        }

        fun colorFilter(colorFilter: ColorFilter): PlatformRenderEffect {
            if (!PlatformCapabilities.supportsColorFilter) return None
            val effect = RenderEffect.createColorFilterEffect(colorFilter)
            return Android(effect)
        }

        fun colorMatrix(matrix: FloatArray): PlatformRenderEffect {
            val cf: ColorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
            return colorFilter(cf)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun runtimeShader(shader: PlatformRuntimeShader): PlatformRenderEffect {
            val androidShader = shader.android ?: return None
            val effect = RenderEffect.createRuntimeShaderEffect(androidShader, "content")
            return Android(effect)
        }

        @RequiresApi(Build.VERSION_CODES.S)
        fun chain(outer: PlatformRenderEffect, inner: PlatformRenderEffect): PlatformRenderEffect {
            val outerAndroid = outer.asAndroid() ?: return inner
            val innerAndroid = inner.asAndroid() ?: return outer
            return Android(RenderEffect.createChainEffect(outerAndroid, innerAndroid))
        }
    }
}

class PlatformRuntimeShader private constructor(val android: RuntimeShader?) {
    companion object {
        fun compile(source: String): PlatformRuntimeShader {
            return if (PlatformCapabilities.supportsRuntimeShader) {
                PlatformRuntimeShader(RuntimeShader(source))
            } else {
                PlatformRuntimeShader(null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setFloatUniform(name: String, vararg values: Float) {
        val shader = android ?: return
        if (values.isEmpty()) return
        if (values.size == 1) {
            shader.setFloatUniform(name, values[0])
        } else {
            shader.setFloatUniform(name, values)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setColorUniform(name: String, argb: Int) {
        android?.setColorUniform(name, argb)
    }
}

fun PlatformRenderEffect.asAndroid(): RenderEffect? =
    when (this) {
        is PlatformRenderEffect.Android -> renderEffect
        PlatformRenderEffect.None -> null
    }