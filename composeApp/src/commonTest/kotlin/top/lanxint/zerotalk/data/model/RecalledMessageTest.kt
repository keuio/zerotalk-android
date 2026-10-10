package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 「已撤回消息」（`is_deleted`）本地行为的回归测试。
 *
 * 官网 `publicAnnouncementDismiss-*.js` 在类型分发之前执行：
 * `e.msg.is_deleted ? <div class="Xu">该消息已被撤回</div> : …`；
 * 撤回事件也不删除消息，而是 `is_deleted = true` 并清空 `content` / `image_url`。
 * 本客户端此前完全没消费该字段，重进会话后图片变占位框、骰子变 `?`。
 */
class RecalledMessageTest {

    private fun message(
        id: String = "m1",
        content: String = "你好",
        isMine: Boolean = true,
        serverId: Long = 1L,
        isDeleted: Boolean = false,
        isImage: Boolean = false,
        imageUrl: String = "",
        isDice: Boolean = false,
        diceValue: Int = 0,
        quotedText: String? = null,
        replyToId: Long? = null
    ) = ChatMessage(
        id = id,
        senderId = if (isMine) "me" else "peer",
        content = content,
        timestamp = "12:00",
        isMine = isMine,
        serverId = serverId,
        isDeleted = isDeleted,
        isImage = isImage,
        imageUrl = imageUrl,
        isDice = isDice,
        diceValue = diceValue,
        quotedText = quotedText,
        replyToId = replyToId
    )

    @Test
    fun `asRecalled 标记 is_deleted 并清空正文与媒体地址`() {
        val recalled = message(content = "", isImage = true, imageUrl = "https://oss/a.png").asRecalled()
        assertTrue(recalled.isDeleted)
        assertEquals("", recalled.content)
        assertEquals("", recalled.imageUrl)
        assertEquals("", recalled.audioUrl)
        assertEquals("", recalled.stickerUrl)
        assertNull(recalled.quotedText)
        assertEquals(RECALLED_MESSAGE_TEXT, recalled.previewText)
    }

    @Test
    fun `applyRecall 保留消息在列表里而不是移除`() {
        val list = listOf(
            message(id = "a", content = "第一条", serverId = 1L),
            message(id = "b", content = "", isDice = true, serverId = 2L),
            message(id = "c", content = "第三条", serverId = 3L)
        )
        val after = list.applyRecall(2L)
        assertEquals(3, after.size)
        assertEquals(listOf("a", "b", "c"), after.map { it.id })
        assertTrue(after[1].isDeleted)
        assertEquals("", after[1].content)
        assertEquals(RECALLED_MESSAGE_TEXT, after[1].previewText)
        assertFalse(after[0].isDeleted)
        assertFalse(after[2].isDeleted)
    }

    @Test
    fun `applyRecall 把引用该消息的引用摘要改成撤回文案`() {
        val list = listOf(
            message(id = "a", content = "原文", serverId = 1L),
            message(id = "b", content = "回复", serverId = 2L, replyToId = 1L, quotedText = "原文")
        )
        val after = list.applyRecall(1L)
        assertTrue(after[0].isDeleted)
        assertEquals(RECALLED_MESSAGE_TEXT, after[1].quotedText)
    }

    @Test
    fun `applyRecall 对非法 id 原样返回`() {
        val list = listOf(message(id = "a", serverId = 1L))
        assertEquals(list, list.applyRecall(0L))
    }

    @Test
    fun `已撤回消息的会话摘要与引用预览都是撤回文案`() {
        val recalled = message(isImage = true, imageUrl = "https://oss/a.png").asRecalled()
        assertEquals(RECALLED_MESSAGE_TEXT, recalled.previewText)
        assertEquals(RECALLED_MESSAGE_TEXT, recalled.quotePreviewText())
    }

    @Test
    fun `已撤回的我方消息不再提供撤回与编辑菜单项`() {
        assertFalse(message(isMine = true).asRecalled().canRecallOrEdit())
    }

    @Test
    fun `未撤回的我方消息仍提供撤回与编辑菜单项`() {
        assertTrue(message(isMine = true).canRecallOrEdit())
    }

    @Test
    fun `对方消息本就没有撤回菜单项`() {
        assertFalse(message(isMine = false).canRecallOrEdit())
    }

    @Test
    fun `已撤回消息不再提供拷贝菜单项`() {
        assertFalse(message(isMine = true).asRecalled().canCopy())
        assertFalse(message(isMine = false).asRecalled().canCopy())
    }

    @Test
    fun `未撤回消息仍提供拷贝菜单项`() {
        assertTrue(message(isMine = true).canCopy())
        assertTrue(message(isMine = false).canCopy())
    }

    @Test
    fun `resolveQuotedText 在引用目标已撤回时本地兜底`() {
        val list = listOf(
            message(id = "a", serverId = 1L).asRecalled(),
            message(id = "b", serverId = 2L, replyToId = 1L, quotedText = "原文")
        )
        assertEquals(RECALLED_MESSAGE_TEXT, list.resolveQuotedText(list[1]))
    }

    @Test
    fun `resolveQuotedText 目标未撤回时保持服务端下发的引用文案`() {
        val list = listOf(
            message(id = "a", serverId = 1L),
            message(id = "b", serverId = 2L, replyToId = 1L, quotedText = "原文")
        )
        assertEquals("原文", list.resolveQuotedText(list[1]))
    }

    @Test
    fun `resolveQuotedText 引用目标不在列表时保持原文案`() {
        val list = listOf(message(id = "b", serverId = 2L, replyToId = 9L, quotedText = "别处的消息"))
        assertEquals("别处的消息", list.resolveQuotedText(list[0]))
    }
}
