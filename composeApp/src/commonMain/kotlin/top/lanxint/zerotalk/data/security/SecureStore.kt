package top.lanxint.zerotalk.data.security

/**
 * 本地敏感数据加密存储
 *
 * Android 侧用 Android Keystore 里的 AES/GCM 密钥加密（密钥不出安全硬件/系统密钥库）。
 * 解密失败（换机、密钥被清除、内容损坏）返回 null，调用方按「没有本地数据」处理即可，
 * 不影响正常登录流程。
 */
/** 密文前缀（各平台实现必须保持一致） */
const val SECURE_STORE_PREFIX = "enc1:"

expect object SecureStore {
    /** 加密并返回带版本前缀的密文；不可用时返回 null */
    fun encrypt(plain: String): String?

    /** 解密 [SecureStore.encrypt] 产出的密文；失败返回 null */
    fun decrypt(cipher: String): String?
}
