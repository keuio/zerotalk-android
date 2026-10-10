package top.lanxint.zerotalk.data.security

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import top.lanxint.zerotalk.data.model.DEFAULT_KDF_ITERATIONS
import top.lanxint.zerotalk.data.model.DEFAULT_ROOM_KEY_ID
import top.lanxint.zerotalk.data.model.EncryptionConfig
import top.lanxint.zerotalk.data.model.RoomUnlockError
import top.lanxint.zerotalk.data.model.RoomUnlockResult

/**
 * 房间端到端加密解锁状态机。
 *
 * 只负责内存中的解锁状态：每个房间保存 [RoomKeyEntry.dek]（32 字节）、密钥 id 与解锁时间。
 * 与仓库/API 解耦；持久化（官方 sessionStorage 24h 语义）由上层选择是否另行接入。
 *
 * 线程安全：所有状态变更都在 [lock] 内完成。
 */
class RoomKeyStore {
    private data class RoomKeyEntry(
        val dek: ByteArray,
        val keyId: String,
        val unlockedAt: Long
    )

    private val lock = Any()
    private val unlockedRooms = LinkedHashMap<String, RoomKeyEntry>()

    /** 房间当前是否已解锁 */
    fun isUnlocked(roomId: String): Boolean = synchronized(lock) {
        unlockedRooms.containsKey(roomId)
    }

    /**
     * 解锁房间：内部完成 DEK 解包并保存。
     *
     * @param config 房间 bootstrap 下发的加密配置
     * @param password 房间暗号
     *
     * DEK 的 AAD 由 [roomId] 参与拼接（`zt|dek_client|v1|<roomId>`），
     * 不接受账号 id —— 传错会导致正确暗号也解包失败。
     */
    fun unlock(
        roomId: String,
        config: EncryptionConfig,
        password: String
    ): RoomUnlockResult {
        val result = unpackDekForConfig(roomId, config, password)
        if (result !is RoomUnlockResult.Success) return result

        val now = System.currentTimeMillis()
        synchronized(lock) {
            unlockedRooms[roomId] = RoomKeyEntry(
                dek = result.dek,
                keyId = config.keyId.ifBlank { DEFAULT_ROOM_KEY_ID },
                unlockedAt = now
            )
        }
        return result
    }

    /** 根据配置选择 DEK 来源：直接下发则导入，否则用暗号解包 */
    @OptIn(ExperimentalEncodingApi::class)
    private fun unpackDekForConfig(
        roomId: String,
        config: EncryptionConfig,
        password: String
    ): RoomUnlockResult {
        if (!config.encryptionEnabled) return RoomUnlockResult.Failure(RoomUnlockError.INVALID_CONFIG)
        val directDek = config.dek?.takeIf { it.isNotBlank() }
        if (directDek != null) {
            val bytes = try {
                Base64.decode(directDek)
            } catch (_: Exception) {
                return RoomUnlockResult.Failure(RoomUnlockError.INVALID_DEK_PAYLOAD)
            }
            if (bytes.size != ROOM_DEK_LENGTH) return RoomUnlockResult.Failure(RoomUnlockError.INVALID_DEK_PAYLOAD)
            return RoomUnlockResult.Success(bytes)
        }

        val salt = config.roomKdfSalt ?: return RoomUnlockResult.Failure(RoomUnlockError.INVALID_CONFIG)
        val payload = config.encryptedDekClient ?: return RoomUnlockResult.Failure(RoomUnlockError.INVALID_CONFIG)
        val iterations = if (config.kdfIterations > 0) config.kdfIterations else DEFAULT_KDF_ITERATIONS

        val result = unpackDek(password, salt, payload, roomId, iterations)
        return result.fold(
            onSuccess = { RoomUnlockResult.Success(it) },
            onFailure = { cause ->
                RoomUnlockResult.Failure(
                    if (cause.message?.contains("盐格式非法") == true ||
                        cause.message?.contains("DEK 封装格式非法") == true ||
                        cause.message?.contains("版本不支持") == true ||
                        cause.message?.contains("长度不足") == true
                    ) {
                        RoomUnlockError.INVALID_DEK_PAYLOAD
                    } else {
                        RoomUnlockError.WRONG_PASSWORD_OR_CORRUPTED
                    }
                )
            }
        )
    }

    /** 移除指定房间的解锁状态 */
    fun lock(roomId: String) {
        synchronized(lock) {
            unlockedRooms.remove(roomId)
        }
    }

    /** 移除全部房间的解锁状态 */
    fun lockAll() {
        synchronized(lock) {
            unlockedRooms.clear()
        }
    }

    /** 房间当前使用的密钥 id；未解锁返回 null */
    fun keyId(roomId: String): String? = synchronized(lock) {
        unlockedRooms[roomId]?.keyId
    }

    /**
     * 取房间 DEK 的副本（32 字节）；未解锁返回 null。
     *
     * 返回副本而不是内部引用：调用方（仓库层加解密）不应持有可被外部改写的同一数组。
     */
    fun dek(roomId: String): ByteArray? = synchronized(lock) {
        unlockedRooms[roomId]?.dek?.copyOf()
    }

    /** 房间解锁时间（epoch ms）；未解锁返回 null */
    fun unlockedAt(roomId: String): Long? = synchronized(lock) {
        unlockedRooms[roomId]?.unlockedAt
    }
}
