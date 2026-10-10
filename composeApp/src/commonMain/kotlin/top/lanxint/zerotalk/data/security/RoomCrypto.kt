package top.lanxint.zerotalk.data.security

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import top.lanxint.zerotalk.data.model.DEFAULT_KDF_ITERATIONS

/** DEK 封装密文的版本字节值（官方 Fd=1） */
const val ROOM_DEK_LAYOUT_VERSION = 1

/** DEK 封装的固定头部长度：版本 1 + IV 12 + GCM tag 16 = 29 字节 */
const val ROOM_DEK_HEADER_LENGTH = 29

/** 解包出的 DEK 必须是 32 字节 AES-256 密钥 */
const val ROOM_DEK_LENGTH = 32

/**
 * 平台无关的 PBKDF2/AES-GCM 密码学原语。
 *
 * Android 实现在 RoomCrypto.android.kt。除 RoomCrypto 与 randomBytes 外，
 * 本文件里其余函数都是纯函数，不依赖平台 API，commonTest 可以直接覆盖。
 */
@OptIn(ExperimentalEncodingApi::class)
expect object RoomCrypto {
    /** PBKDF2-SHA256 派生 256 位密钥（与官方 WebCrypto PBKDF2 一致） */
    fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, lengthBits: Int = 256): ByteArray

    /** AES-GCM 解密：返回 null 表示认证失败（密钥错误 / AAD 错误 / 密文损坏） */
    fun aesGcmDecrypt(key: ByteArray, iv: ByteArray, cipherText: ByteArray, additionalData: ByteArray): ByteArray?

    /** AES-GCM 加密：返回的字节布局为 ciphertext+tag（与官方 WebCrypto 一致） */
    fun aesGcmEncrypt(key: ByteArray, iv: ByteArray, plainText: ByteArray, additionalData: ByteArray): ByteArray

    /** 生成 [length] 字节安全随机数 */
    fun randomBytes(length: Int): ByteArray
}

/**
 * AAD：DEK 封装使用的 `zt|dek_client|v1|<roomId>`。
 *
 * 注意：这里的标识是**房间 id**，不是账号 user_id。早期注释按 `Nd(userId)` 推测成
 * 账号 id，导致正确暗号也一律认证失败（表现为「房间暗号不正确」）。
 * 已用真实房间实测：AAD 换成 roomId 后 PBKDF2+AES-GCM 恰好解出 32 字节 DEK，
 * 且错误暗号仍被拒绝。
 */
fun dekClientAad(roomId: String): ByteArray = "zt|dek_client|v1|$roomId".encodeToByteArray()

/**
 * AAD：消息正文使用的 zt|v1|<roomId>|<clientMessageId>|<userId>|<type>
 *
 * 与官方最新 JS 的 `Jd(e,s,t,u,a,i)` 完全一致：`Io(e,i,u,a)` 对应
 * roomId|clientMessageId|userId|type。加密入口 [RoomMessageCrypto.encryptTextWithIv]
 * 与解密入口 [RoomMessageCrypto.decryptText] 都必须使用同一顺序。
 *
 * 早期脚本曾臆测 decrypt 端把 userId 放在 type 前，实测官方 minified 代码
 * `Io(e,i,u,a)` 明确是 i=clientMessageId、u=userId、a=type，因此主顺序固定为
 * roomId|clientMessageId|userId|type。旧兼容见 [messageBodyAadEncryptLegacy]。
 */
fun messageBodyAad(
    roomId: String,
    clientMessageId: String,
    userId: String,
    type: String
): ByteArray = "zt|v1|$roomId|$clientMessageId|$userId|$type".encodeToByteArray()

/**
 * 仅用于解密失败时兼容早期历史密文：roomId|userId|type|clientMessageId
 *
 * 不再作为发送侧使用；只有旧客户端留下的历史密文需要一次降级尝试。
 */
fun messageBodyAadEncryptLegacy(
    roomId: String,
    userId: String,
    type: String,
    clientMessageId: String
): ByteArray = "zt|v1|$roomId|$userId|$type|$clientMessageId".encodeToByteArray()

/** 官方 DEK 封装拆分结果 */
data class DekParts(
    val version: Int,
    val iv: ByteArray,
    val tag: ByteArray,
    val cipherText: ByteArray
)

/**
 * 拆分并校验官方 DEK 封装布局：[ver][iv 12][tag 16][ciphertext...]
 *
 * 官方把 GCM tag 从密文前部移到明文头（Gd(ciphertext, tag)），不是 WebCrypto 默认输出顺序。
 * 布局非法返回 null。
 */
fun splitDekPayload(payload: ByteArray): DekParts? {
    if (payload.size < ROOM_DEK_HEADER_LENGTH) return null
    if (payload[0].toInt() and 0xff != ROOM_DEK_LAYOUT_VERSION) return null
    return DekParts(
        version = payload[0].toInt() and 0xff,
        iv = payload.copyOfRange(1, 13),
        tag = payload.copyOfRange(13, 29),
        cipherText = payload.copyOfRange(29, payload.size)
    )
}

/** 纯布局校验错误文案（commonTest 可直接断言） */
fun dekLayoutError(payload: ByteArray): String? {
    if (payload.size < ROOM_DEK_HEADER_LENGTH) return "DEK 密文长度不足"
    if (payload[0].toInt() and 0xff != ROOM_DEK_LAYOUT_VERSION) return "DEK 密文版本不支持"
    return null
}

/**
 * 构造可供平台 AES-GCM 解密的完整密文：ciphertext + tag
 *
 * 布局归位后可直接喂给 RoomCrypto.aesGcmDecrypt。
 */
fun dekCipherTextWithTag(payload: ByteArray): ByteArray {
    val parts = requireNotNull(splitDekPayload(payload)) { "DEK 密文格式非法" }
    return parts.cipherText + parts.tag
}

/** 纯校验：解包出的 DEK 必须恰好 32 字节（commonTest 可直接覆盖） */
fun dekPlainLengthError(plain: ByteArray): String? {
    if (plain.size != ROOM_DEK_LENGTH) return "解包房间密钥失败，请检查暗号"
    return null
}

/**
 * 完整解包 DEK：PBKDF2 派生 + AES-GCM 解密 + 32 字节校验
 *
 * 与官方 Od 一致：
 * - [salt] 与 [payload] 都是 base64 字符串；
 * - [password] 是房间暗号原文；
 * - [roomId] 传入 [dekClientAad]（注意是房间 id，不是账号 id）；
 * - 解密成功后明文必须恰好 32 字节，否则按暗号错误处理。
 */
@OptIn(ExperimentalEncodingApi::class)
fun unpackDek(
    password: String,
    salt: String,
    payload: String,
    roomId: String,
    iterations: Int = DEFAULT_KDF_ITERATIONS
): Result<ByteArray> {
    val saltBytes = decodeRoomBase64(salt) ?: return Result.failure(IllegalArgumentException("盐格式非法"))
    val payloadBytes = decodeRoomBase64(payload) ?: return Result.failure(IllegalArgumentException("DEK 封装格式非法"))
    return unpackDekBytes(password, saltBytes, payloadBytes, roomId, iterations)
}

/**
 * 同上，但接收已解码字节数组。
 *
 * 纯文本测试无法调用 Android actual，因此不提供“不依赖 crypto”的 partial；真实加解密验证
 * 放在 androidUnitTest / 真机，commonTest 只覆盖 AAD 拼接、布局拆分与长度/版本校验。
 */
fun unpackDekBytes(
    password: String,
    salt: ByteArray,
    payload: ByteArray,
    roomId: String,
    iterations: Int = DEFAULT_KDF_ITERATIONS
): Result<ByteArray> {
    val invalid = dekLayoutError(payload)
    if (invalid != null) return Result.failure(IllegalArgumentException(invalid))
    val parts = requireNotNull(splitDekPayload(payload))
    val key = RoomCrypto.pbkdf2(password.encodeToByteArray(), salt, iterations, lengthBits = 256)
    val plain = RoomCrypto.aesGcmDecrypt(
        key = key,
        iv = parts.iv,
        cipherText = dekCipherTextWithTag(payload),
        additionalData = dekClientAad(roomId)
    ) ?: return Result.failure(IllegalArgumentException("解包房间密钥失败，请检查暗号"))
    val lengthError = dekPlainLengthError(plain)
    if (lengthError != null) return Result.failure(IllegalArgumentException(lengthError))
    return Result.success(plain)
}

/** 加密文本消息的结果：密文正文与用于帧 `enc.iv` 的 base64url IV */
data class EncryptedTextResult(
    val cipherText: String,
    val iv: String
)

/**
 * 消息正文加解密 API（只供 text 消息使用）。
 *
 * 收发密文/enc.iv 都按官方 base64url（无填充）格式。
 */
object RoomMessageCrypto {
    /**
     * 加密文本消息并返回密文正文（兼容旧入口，随机生成 IV）。
     *
     * 需要同时取回 IV 时请使用 [encryptTextWithIv]。
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encryptText(
        dek: ByteArray,
        roomId: String,
        userId: String,
        clientMessageId: String,
        plainText: String,
        iv: ByteArray = RoomCrypto.randomBytes(12)
    ): String = encryptTextWithIv(dek, roomId, userId, clientMessageId, plainText, iv).cipherText

    /** 加密文本消息并返回密文 + base64url IV（服务端消息帧 `enc.iv` 需要它） */
    @OptIn(ExperimentalEncodingApi::class)
    fun encryptTextWithIv(
        dek: ByteArray,
        roomId: String,
        userId: String,
        clientMessageId: String,
        plainText: String,
        iv: ByteArray = RoomCrypto.randomBytes(12)
    ): EncryptedTextResult {
        val cipher = RoomCrypto.aesGcmEncrypt(
            key = dek,
            iv = iv,
            plainText = plainText.encodeToByteArray(),
            additionalData = messageBodyAad(roomId, clientMessageId, userId, "text")
        )
        return EncryptedTextResult(
            cipherText = RoomCryptoCodec.encode(cipher),
            iv = RoomCryptoCodec.encode(iv)
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decryptText(
        dek: ByteArray,
        roomId: String,
        userId: String,
        clientMessageId: String,
        ivBase64Url: String,
        cipherTextBase64Url: String
    ): String? {
        val iv = decodeRoomBase64(ivBase64Url) ?: return null
        val cipher = decodeRoomBase64(cipherTextBase64Url) ?: return null

        // 主顺序：官方 `Jd` 使用 Io(e=roomId, i=clientMessageId, u=userId, a=type)
        val plain = RoomCrypto.aesGcmDecrypt(
            key = dek,
            iv = iv,
            cipherText = cipher,
            additionalData = messageBodyAad(roomId, clientMessageId, userId, "text")
        )
        if (plain != null) {
            return try { plain.decodeToString() } catch (_: Exception) { null }
        }

        // 仅兼容早期客户端写出的 roomId|userId|type|clientMessageId 历史密文
        val legacy = RoomCrypto.aesGcmDecrypt(
            key = dek,
            iv = iv,
            cipherText = cipher,
            additionalData = messageBodyAadEncryptLegacy(roomId, userId, "text", clientMessageId)
        )
        if (legacy != null) {
            return try { legacy.decodeToString() } catch (_: Exception) { null }
        }
        return null
    }
}

/** 官方 base64url 无填充解码（对应 index chunk 的 Zy），同时兼容标准 base64 */
internal fun decodeRoomBase64(input: String): ByteArray? = try {
    RoomCryptoCodec.decode(input)
} catch (_: Exception) {
    null
}

/**
 * 官方 base64url 无填充编码/解码（对应 index chunk 的 Qy/Zy）。
 *
 * Base64.decode 默认接受标准 base64，这里兼容无填充 url-safe。
 */
@OptIn(ExperimentalEncodingApi::class)
internal object RoomCryptoCodec {
    fun encode(bytes: ByteArray): String {
        val withPadding = Base64.encode(bytes)
        return withPadding.trimEnd('=').replace('+', '-').replace('/', '_')
    }

    fun decode(input: String): ByteArray {
        val padded = when (val rem = input.length % 4) {
            0 -> input
            else -> input + "=".repeat(4 - rem)
        }.replace('-', '+').replace('_', '/')
        return Base64.decode(padded)
    }
}
