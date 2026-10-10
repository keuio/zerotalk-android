package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [LocalEchoMerge] 的回归测试。
 *
 * 背景：对账命中后用服务端消息整体替换本地回显，而服务端语音帧
 * （`{"type":"audio","content":"<OSS 直链>","audio_source":"record"}`）**不含时长字段**，
 * 官网也是由播放器读音频元数据现算的。此前合并只保留引用字段，
 * `voiceDurationSec` 被服务端的 0 覆盖 —— 自己发出去的语音一直显示 `0"`。
 */
class LocalEchoMergeTest {

    private fun msg(
        id: String,
        content: String = "",
        isVoice: Boolean = false,
        voiceDurationSec: Int = 0,
        audioUrl: String = "",
        audioSource: String = "",
        imageUrl: String = "",
        isImage: Boolean = false,
        stickerUrl: String = "",
        stickerAssetId: Long = 0L,
        isSticker: Boolean = false,
        isDeleted: Boolean = false,
        quotedText: String? = null,
        quotedIsMine: Boolean? = null,
        quotedSenderName: String? = null,
        replyToId: Long? = null,
        previewText: String = "",
        senderName: String = "",
        senderAvatar: String = "",
        senderGender: String = ""
    ) = ChatMessage(
        id = id,
        senderId = "me",
        content = content,
        timestamp = "12:00",
        isMine = true,
        isVoice = isVoice,
        voiceDurationSec = voiceDurationSec,
        audioUrl = audioUrl,
        audioSource = audioSource,
        imageUrl = imageUrl,
        isImage = isImage,
        stickerUrl = stickerUrl,
        stickerAssetId = stickerAssetId,
        isSticker = isSticker,
        isDeleted = isDeleted,
        quotedText = quotedText,
        quotedIsMine = quotedIsMine,
        quotedSenderName = quotedSenderName,
        replyToId = replyToId,
        previewText = previewText,
        senderName = senderName,
        senderAvatar = senderAvatar,
        senderGender = senderGender
    )

    // ==================== 语音时长（本次修复的核心） ====================

    @Test
    fun `服务端无时长时保留本地回显的语音时长`() {
        val server = msg("s1", content = "https://oss/a.webm", isVoice = true, audioUrl = "https://oss/a.webm")
        val echo = msg("e1", content = "[语音 12\"]", isVoice = true, voiceDurationSec = 12, audioUrl = "https://oss/a.webm")
        assertEquals(12, LocalEchoMerge.merge(server, echo).voiceDurationSec)
    }

    @Test
    fun `服务端有时长时以服务端为准`() {
        val server = msg("s1", isVoice = true, voiceDurationSec = 9)
        val echo = msg("e1", isVoice = true, voiceDurationSec = 12)
        assertEquals(9, LocalEchoMerge.merge(server, echo).voiceDurationSec)
    }

    @Test
    fun `两侧都没有时长时保持 0`() {
        val server = msg("s1", isVoice = true)
        val echo = msg("e1", isVoice = true)
        assertEquals(0, LocalEchoMerge.merge(server, echo).voiceDurationSec)
    }

    // ==================== 媒体地址 / 来源 ====================

    @Test
    fun `服务端音频地址为空时回落本地回显`() {
        val server = msg("s1", isVoice = true)
        val echo = msg("e1", isVoice = true, audioUrl = "https://oss/a.webm", audioSource = "record")
        val merged = LocalEchoMerge.merge(server, echo)
        assertEquals("https://oss/a.webm", merged.audioUrl)
        assertEquals("record", merged.audioSource)
    }

    @Test
    fun `服务端音频字段有值时不被本地覆盖`() {
        val server = msg("s1", isVoice = true, audioUrl = "https://oss/server.webm", audioSource = "file")
        val echo = msg("e1", isVoice = true, audioUrl = "https://oss/echo.webm", audioSource = "record")
        val merged = LocalEchoMerge.merge(server, echo)
        assertEquals("https://oss/server.webm", merged.audioUrl)
        assertEquals("file", merged.audioSource)
    }

    @Test
    fun `服务端图片地址为空时回落本地回显`() {
        val server = msg("s1", isImage = true)
        val echo = msg("e1", isImage = true, imageUrl = "https://oss/a.png")
        assertEquals("https://oss/a.png", LocalEchoMerge.merge(server, echo).imageUrl)
    }

    @Test
    fun `表情包地址与 asset_id 回落本地回显`() {
        val server = msg("s1", isSticker = true)
        val echo = msg("e1", isSticker = true, stickerUrl = "https://oss/s.png", stickerAssetId = 77L)
        val merged = LocalEchoMerge.merge(server, echo)
        assertEquals("https://oss/s.png", merged.stickerUrl)
        assertEquals(77L, merged.stickerAssetId)
    }

    // ==================== 引用 / 撤回 / 摘要 / 发送者 ====================

    @Test
    fun `引用字段服务端优先且 quotedIsMine 回显优先`() {
        val server = msg("s1", quotedText = "服务端引用", quotedIsMine = null, replyToId = 5L)
        val echo = msg("e1", quotedText = "本地引用", quotedIsMine = true, quotedSenderName = "甲")
        val merged = LocalEchoMerge.merge(server, echo)
        assertEquals("服务端引用", merged.quotedText)
        assertEquals(true, merged.quotedIsMine)
        assertEquals("甲", merged.quotedSenderName)
        assertEquals(5L, merged.replyToId)
    }

    @Test
    fun `服务端缺引用字段时回落本地`() {
        val server = msg("s1")
        val echo = msg("e1", quotedText = "本地引用", replyToId = 8L)
        val merged = LocalEchoMerge.merge(server, echo)
        assertEquals("本地引用", merged.quotedText)
        assertEquals(8L, merged.replyToId)
        assertNull(merged.quotedIsMine)
    }

    @Test
    fun `任一侧标记撤回即视为已撤回`() {
        assertTrue(LocalEchoMerge.merge(msg("s1", isDeleted = true), msg("e1")).isDeleted)
        assertTrue(LocalEchoMerge.merge(msg("s1"), msg("e1", isDeleted = true)).isDeleted)
        assertFalse(LocalEchoMerge.merge(msg("s1"), msg("e1")).isDeleted)
    }

    @Test
    fun `摘要与发送者资料在服务端为空时回落本地`() {
        val server = msg("s1", previewText = "", senderName = "", senderAvatar = "", senderGender = "")
        val echo = msg("e1", previewText = "[语音]", senderName = "我", senderAvatar = "https://a.png", senderGender = "female")
        val merged = LocalEchoMerge.merge(server, echo)
        assertEquals("[语音]", merged.previewText)
        assertEquals("我", merged.senderName)
        assertEquals("https://a.png", merged.senderAvatar)
        assertEquals("female", merged.senderGender)
    }

    @Test
    fun `服务端摘要与发送者资料有值时不被本地覆盖`() {
        val server = msg("s1", previewText = "[语音]", senderName = "服务端名", senderAvatar = "https://s.png", senderGender = "male")
        val echo = msg("e1", previewText = "本地摘要", senderName = "本地名", senderAvatar = "https://e.png", senderGender = "female")
        val merged = LocalEchoMerge.merge(server, echo)
        assertEquals("[语音]", merged.previewText)
        assertEquals("服务端名", merged.senderName)
        assertEquals("https://s.png", merged.senderAvatar)
        assertEquals("male", merged.senderGender)
    }

    @Test
    fun `服务端 id 与内容仍以服务端为准`() {
        val server = msg("s1", content = "https://oss/a.webm", isVoice = true)
        val echo = msg("e1", content = "[语音 12\"]", isVoice = true, voiceDurationSec = 12)
        val merged = LocalEchoMerge.merge(server, echo)
        assertEquals("s1", merged.id)
        assertEquals("https://oss/a.webm", merged.content)
        assertEquals(12, merged.voiceDurationSec)
    }
}
