package top.lanxint.zerotalk.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore 实现：AES-256/GCM，密钥由系统密钥库保管，App 只持有别名。
 *
 * 密文格式：`enc1:<base64(iv)>:<base64(ciphertext+tag)>`
 */
actual object SecureStore {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "zt_secure_store_v1"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128

    private fun secretKey(): SecretKey? = try {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        existing ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    } catch (_: Throwable) {
        null
    }

    actual fun encrypt(plain: String): String? = try {
        val key = secretKey() ?: return null
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        SECURE_STORE_PREFIX + Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(body, Base64.NO_WRAP)
    } catch (_: Throwable) {
        null
    }

    actual fun decrypt(cipher: String): String? = try {
        if (!cipher.startsWith(SECURE_STORE_PREFIX)) return null
        val parts = cipher.split(":")
        if (parts.size != 3) return null
        val key = secretKey() ?: return null
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val body = Base64.decode(parts[2], Base64.NO_WRAP)
        val decoder = Cipher.getInstance(TRANSFORM)
        decoder.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        String(decoder.doFinal(body), Charsets.UTF_8)
    } catch (_: Throwable) {
        null
    }
}
