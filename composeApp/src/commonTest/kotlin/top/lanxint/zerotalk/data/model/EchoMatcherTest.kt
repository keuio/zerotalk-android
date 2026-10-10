package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [EchoMatcher] 的回归测试。
 *
 * 背景：图片/语音的本地回显 content 是占位文案（「［图片］」「[语音 N\"]」），
 * 而 WS 帧与服务端回推的 content 是 OSS URL。此前只比 content，导致永远匹配不上、
 * serverId 恒为 0 —— 表现为发完图片后长按撤回提示「该消息尚未同步到服务端」。
 */
class EchoMatcherTest {

    private fun echo(
        content: String,
        isImage: Boolean = false,
        imageUrl: String = "",
        isVoice: Boolean = false,
        audioUrl: String = "",
        isDice: Boolean = false,
        clientMessageId: String = ""
    ) = ChatMessage(
        id = "local_1",
        senderId = "me",
        content = content,
        timestamp = "12:00",
        isMine = true,
        isImage = isImage,
        imageUrl = imageUrl,
        isVoice = isVoice,
        audioUrl = audioUrl,
        isDice = isDice,
        clientMessageId = clientMessageId
    )

    @Test
    fun `文本按内容匹配`() {
        assertTrue(EchoMatcher.isSameKind(echo("你好"), "text", "你好"))
        assertFalse(EchoMatcher.isSameKind(echo("你好"), "text", "再见"))
    }

    @Test
    fun `骰子按类型匹配（点数由服务端摇出，本地回显 content 为空）`() {
        assertTrue(EchoMatcher.isSameKind(echo("", isDice = true), "dice", "5"))
    }

    @Test
    fun `图片回显按 URL 匹配（回归：此前只比 content 导致永远匹配不上）`() {
        val url = "https://oss.example.com/a.png"
        assertTrue(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = url), "image", url, url))
    }

    @Test
    fun `图片 URL 只在 content 里也能匹配`() {
        val url = "https://oss.example.com/b.png"
        assertTrue(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = url), "image", url, null))
    }

    @Test
    fun `图片 URL 不同则不匹配`() {
        val a = "https://oss.example.com/a.png"
        val b = "https://oss.example.com/b.png"
        assertFalse(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = a), "image", b, b))
    }

    @Test
    fun `图片回显遇文本事件不匹配`() {
        val url = "https://oss.example.com/a.png"
        assertFalse(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = url), "text", url))
    }

    @Test
    fun `语音回显按 content 里的 URL 匹配`() {
        val url = "https://oss.example.com/a.mp3"
        assertTrue(EchoMatcher.isSameKind(echo("[语音 3\"]", isVoice = true, audioUrl = url), "audio", url))
    }

    @Test
    fun `语音 URL 不同则不匹配`() {
        val a = "https://oss.example.com/a.mp3"
        val b = "https://oss.example.com/b.mp3"
        assertFalse(EchoMatcher.isSameKind(echo("[语音 3\"]", isVoice = true, audioUrl = a), "audio", b))
    }

    @Test
    fun `URL 为空不匹配，避免把空回显误判成命中`() {
        assertFalse(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = ""), "image", "", null))
    }

    @Test
    fun `语音事件类型大小写不敏感`() {
        val url = "https://oss.example.com/a.mp3"
        assertTrue(EchoMatcher.isSameKind(echo("[语音 3\"]", isVoice = true, audioUrl = url), "AUDIO", url))
    }

    // ---- 端到端加密：服务端回推的是密文，只能靠 client_message_id 对账 ----

    @Test
    fun `加密消息按 client_message_id 匹配（密文与本地明文不同也算同一条）`() {
        val mine = echo("你好", clientMessageId = "cmid-1")
        assertTrue(EchoMatcher.isSameKind(mine, "text", "Y2lwaGVy", null, "cmid-1"))
    }

    @Test
    fun `client_message_id 不同则不匹配`() {
        val mine = echo("你好", clientMessageId = "cmid-1")
        assertFalse(EchoMatcher.isSameKind(mine, "text", "Y2lwaGVy", null, "cmid-2"))
    }

    @Test
    fun `本地无 client_message_id 时回落到内容比对`() {
        assertTrue(EchoMatcher.isSameKind(echo("你好"), "text", "你好", null, "cmid-1"))
        assertFalse(EchoMatcher.isSameKind(echo("你好"), "text", "Y2lwaGVy", null, "cmid-1"))
    }
}
