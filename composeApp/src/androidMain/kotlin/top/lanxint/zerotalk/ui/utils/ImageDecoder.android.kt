package top.lanxint.zerotalk.ui.utils

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Android 平台原生 BitmapFactory 解码实现
 *
 * 关键点：动态配图往往是手机原图（几千像素、数 MB），若按原尺寸解码，
 * 单张 ARGB_8888 位图可达数十 MB，九宫格场景极易 OOM 且解码极慢（表现为「图片一直不显示」）。
 * 因此先读尺寸算出 inSampleSize 做降采样，并捕获 Throwable（OOM 属于 Error 而非 Exception）。
 */
actual fun decodeByteArrayToImageBitmap(
    bytes: ByteArray,
    maxDimension: Int
): ImageBitmap? {
    return try {
        val target = if (maxDimension > 0) maxDimension else 1280

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

        val longestSide = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
        var sampleSize = 1
        if (longestSide > target && target > 0) {
            val halfLongest = longestSide / 2
            while ((halfLongest / sampleSize) >= target) {
                sampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
        bitmap.asImageBitmap()
    } catch (t: Throwable) {
        // 包含 OutOfMemoryError：宁可这一张不显示，也不能让整个 App 崩掉
        null
    }
}
