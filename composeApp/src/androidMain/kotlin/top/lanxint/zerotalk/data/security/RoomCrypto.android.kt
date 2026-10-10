package top.lanxint.zerotalk.data.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android JCA 实现。
 *
 * PBKDF2：API 23-25 不保证提供 PBKDF2WithHmacSHA256，因此这里直接基于 HmacSHA256
 * 实现标准 PBKDF2。PRF 固定为 HmacSHA256，与官方 WebCrypto PBKDF2-SHA256 完全一致；
 * 不使用 PBKDF2WithHmacSHA1 回退（两者派生结果不同，会导致与服务端不兼容）。
 *
 * AES-GCM：API 23+ 系统提供 AES/GCM/NoPadding，GCMParameterSpec 自 API 19 起可用。
 * 使用 128 位认证标签，与官方 WebCrypto 默认一致。
 */
actual object RoomCrypto {
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val TAG_BITS = 128
    private const val HMAC_SHA256 = "HmacSHA256"
    private const val KEY_LENGTH_BYTES = 32

    private val secureRandom = SecureRandom()

    actual fun pbkdf2(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        lengthBits: Int
    ): ByteArray {
        require(iterations > 0) { "PBKDF2 迭代次数必须大于 0" }
        require(lengthBits > 0 && lengthBits % 8 == 0) { "派生长度必须是 8 的倍数" }

        val hLen = KEY_LENGTH_BYTES
        val dkLen = lengthBits / 8
        val blockCount = (dkLen + hLen - 1) / hLen
        val out = ByteArray(blockCount * hLen)
        val mac = Mac.getInstance(HMAC_SHA256)
        val keySpec = SecretKeySpec(password, HMAC_SHA256)

        for (block in 1..blockCount) {
            mac.init(keySpec)
            mac.update(salt)
            val blockIndex = byteArrayOf(
                (block ushr 24).toByte(),
                (block ushr 16).toByte(),
                (block ushr 8).toByte(),
                block.toByte()
            )
            var u = mac.doFinal(blockIndex)
            val t = u.copyOf()
            repeat(iterations - 1) {
                mac.init(keySpec)
                u = mac.doFinal(u)
                for (i in u.indices) {
                    t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
                }
            }
            t.copyInto(out, destinationOffset = (block - 1) * hLen)
        }
        return out.copyOfRange(0, dkLen)
    }

    actual fun aesGcmDecrypt(
        key: ByteArray,
        iv: ByteArray,
        cipherText: ByteArray,
        additionalData: ByteArray
    ): ByteArray? = try {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_BITS, iv)
        )
        cipher.updateAAD(additionalData)
        cipher.doFinal(cipherText)
    } catch (_: Exception) {
        null
    }

    actual fun aesGcmEncrypt(
        key: ByteArray,
        iv: ByteArray,
        plainText: ByteArray,
        additionalData: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(additionalData)
        return cipher.doFinal(plainText)
    }

    actual fun randomBytes(length: Int): ByteArray {
        require(length >= 0)
        return ByteArray(length).also { secureRandom.nextBytes(it) }
    }
}
