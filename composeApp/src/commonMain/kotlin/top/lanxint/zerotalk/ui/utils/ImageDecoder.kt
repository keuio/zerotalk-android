package top.lanxint.zerotalk.ui.utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 将图片字节数组解码为 Compose ImageBitmap 的多平台抽象
 *
 * @param maxDimension 解码后长边最大像素（按 2 的幂做降采样），避免大图原图直接解码导致 OOM
 */
expect fun decodeByteArrayToImageBitmap(
    bytes: ByteArray,
    maxDimension: Int = 1280
): ImageBitmap?
