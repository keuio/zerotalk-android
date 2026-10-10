package top.lanxint.zerotalk.data.model

/**
 * 房间端到端加密配置（从 bootstrap `encryption` 对象映射而来）
 *
 * 字段口径与官方网页前端一致：
 * - [roomKdfSalt] 是 PBKDF2 盐（base64）；[encryptedDekClient] 是 DEK 封装（base64）；
 * - [kdfIterations] 缺省按 150000 处理；[keyId] 缺省为 `room_dek_v1`；
 * - [dek] 在服务端部分场景直接下发已解封 DEK（官方 `Hd()` 会直接导入）。
 */
data class EncryptionConfig(
    val encryptionEnabled: Boolean = false,
    val roomKdfSalt: String? = null,
    val encryptedDekClient: String? = null,
    val kdfIterations: Int = DEFAULT_KDF_ITERATIONS,
    val keyId: String = DEFAULT_ROOM_KEY_ID,
    val dek: String? = null
)

/** 官方 PBKDF2 默认迭代次数 */
const val DEFAULT_KDF_ITERATIONS = 150000

/** 官方默认密钥 id */
const val DEFAULT_ROOM_KEY_ID = "room_dek_v1"

/** 房间端到端加密解锁结果 */
sealed class RoomUnlockResult {
    /** 解锁成功，[dek] 为 32 字节数据密钥 */
    data class Success(val dek: ByteArray) : RoomUnlockResult()

    /** 解锁失败，[reason] 为可展示给用户的错误原因 */
    data class Failure(val reason: RoomUnlockError) : RoomUnlockResult()
}

/** 房间端到端加密解锁错误 */
enum class RoomUnlockError {
    /** 房间加密配置缺失或字段不完整 */
    INVALID_CONFIG,

    /** DEK 封装格式非法（长度不足 / 版本非 1 / base64 无效） */
    INVALID_DEK_PAYLOAD,

    /** PBKDF2 派生失败（平台不支持等） */
    KDF_FAILED,

    /** AES-GCM 解密失败 / 解出的明文不是 32 字节，说明暗号可能错误 */
    WRONG_PASSWORD_OR_CORRUPTED,

    /** 服务端返回了解锁失败 */
    SERVER_REJECTED
}
