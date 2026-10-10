package top.lanxint.zerotalk.data.security

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 房间 E2EE 纯逻辑测试。
 *
 * 只覆盖不依赖平台 crypto 的函数（AAD 拼接、DEK 布局拆分、长度/版本校验），
 * 不在 commonTest 里调用 Android JCA actual；真实加解密放到 androidUnitTest / 真机。
 */
class RoomCryptoPureLogicTest {

    @Test
    fun `DEK AAD 按官方字符串拼接`() {
        val aad = dekClientAad("12345")
        assertContentEquals("zt|dek_client|v1|12345".encodeToByteArray(), aad)
    }

    /**
     * 回归：DEK AAD 的第四段是**房间 id**，不是账号 user_id。
     *
     * 早期实现误传账号 id，导致正确暗号也认证失败（提示「房间暗号不正确」）。
     * 用真实加密房间实测：roomId 作 AAD 可解出 32 字节 DEK，账号 id 则必定失败。
     */
    @Test
    fun `DEK AAD 使用房间 id 而非账号 id`() {
        val roomId = "4888c3cf5fc5dbd075faa35d771c205d"
        val accountId = "17853"
        assertContentEquals(
            "zt|dek_client|v1|$roomId".encodeToByteArray(),
            dekClientAad(roomId)
        )
        // 两者不能混淆：用账号 id 拼出来的字符串必须与房间 id 的不同，
        // 否则说明拼接退化成「谁传进来都行」
        assertFalse(
            dekClientAad(roomId).contentEquals(dekClientAad(accountId)),
            "房间 id 与账号 id 不能生成相同 AAD"
        )
    }

    @Test
    fun `消息 AAD 按官方新 JS 顺序拼接`() {
        val aad = messageBodyAad(roomId = "room-42", clientMessageId = "msg-uuid", userId = "987", type = "text")
        assertContentEquals("zt|v1|room-42|msg-uuid|987|text".encodeToByteArray(), aad)
    }

    @Test
    fun `消息 AAD 非 text 时保留类型`() {
        val aad = messageBodyAad("r", "msg-cid", "u", "image")
        assertContentEquals("zt|v1|r|msg-cid|u|image".encodeToByteArray(), aad)
    }

    @Test
    fun `旧顺序 AAD 仅保留为历史兼容`() {
        val aad = messageBodyAadEncryptLegacy("room-42", "987", "text", "msg-uuid")
        assertContentEquals("zt|v1|room-42|987|text|msg-uuid".encodeToByteArray(), aad)
    }

    @Test
    fun `官方 base64url 无填充解码兼容标准 base64`() {
        assertContentEquals(byteArrayOf(1, 2, 3), decodeRoomBase64("AQID"))
        assertContentEquals(byteArrayOf(1, 2, 3), decodeRoomBase64("AQID".replace("+", "-").replace("/", "_")))
    }

    @Test
    fun `DEK 布局长度不足时报错`() {
        val payload = byteArrayOf(1, 2, 3)
        assertNotNull(dekLayoutError(payload))
        assertNull(splitDekPayload(payload))
    }

    @Test
    fun `DEK 布局长度等于头部但无密文时报错`() {
        val payload = ByteArray(ROOM_DEK_HEADER_LENGTH) { index -> if (index == 0) ROOM_DEK_LAYOUT_VERSION.toByte() else index.toByte() }
        assertNull(dekLayoutError(payload))
        assertNotNull(splitDekPayload(payload))
        assertTrue(splitDekPayload(payload)!!.cipherText.isEmpty())
    }

    @Test
    fun `DEK 布局 version 非 1 时报错`() {
        val payload = ByteArray(ROOM_DEK_HEADER_LENGTH + 1) { 0 }
        payload[0] = 2
        assertNotNull(dekLayoutError(payload))
        assertNull(splitDekPayload(payload))
    }

    @Test
    fun `DEK 布局拆分位置正确`() {
        val payload = ByteArray(ROOM_DEK_HEADER_LENGTH + 5)
        payload[0] = ROOM_DEK_LAYOUT_VERSION.toByte()
        for (i in 1 until ROOM_DEK_HEADER_LENGTH) payload[i] = i.toByte()
        payload[29] = 0x7a
        payload[30] = 0x7b
        payload[31] = 0x7c
        payload[32] = 0x7d
        payload[33] = 0x7e

        val parts = assertNotNull(splitDekPayload(payload))
        assertEquals(ROOM_DEK_LAYOUT_VERSION, parts.version)
        assertContentEquals(payload.copyOfRange(1, 13), parts.iv)
        assertContentEquals(payload.copyOfRange(13, 29), parts.tag)
        assertContentEquals(payload.copyOfRange(29, payload.size), parts.cipherText)
    }

    @Test
    fun `DEK 明文字节数非 32 时报错`() {
        assertNotNull(dekPlainLengthError(ByteArray(31)))
        assertNotNull(dekPlainLengthError(ByteArray(33)))
        assertNull(dekPlainLengthError(ByteArray(32)))
    }

    @Test
    fun `DEK 布局归位把 tag 放到密文尾部`() {
        val payload = ByteArray(ROOM_DEK_HEADER_LENGTH + 4)
        payload[0] = ROOM_DEK_LAYOUT_VERSION.toByte()
        for (i in 1 until payload.size) payload[i] = i.toByte()

        val reordered = dekCipherTextWithTag(payload)
        val parts = assertNotNull(splitDekPayload(payload))
        assertContentEquals(parts.cipherText + parts.tag, reordered)
    }

    @Test
    fun `unpackDek 布局错误返回失败而不是平台异常`() {
        val result = unpackDek(password = "p", salt = "S2VsbHk=", payload = "AQID", roomId = "1", iterations = 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun `unpackDek base64 无效时返回失败`() {
        val result = unpackDek(password = "p", salt = "not-base64!!!", payload = "AQID", roomId = "1", iterations = 1)
        assertTrue(result.isFailure)
    }
}
