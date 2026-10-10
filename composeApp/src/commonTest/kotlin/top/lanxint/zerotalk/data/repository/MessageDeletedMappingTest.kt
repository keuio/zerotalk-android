package top.lanxint.zerotalk.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import top.lanxint.zerotalk.data.model.RECALLED_MESSAGE_TEXT
import top.lanxint.zerotalk.data.model.quotePreviewText
import top.lanxint.zerotalk.data.network.ChatMessageDto
import top.lanxint.zerotalk.data.network.parseMessageEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `is_deleted` 从历史 DTO / WS 实时事件映射到 `ChatMessage` 的回归测试。
 *
 * 两条链路共用 [ZeroTalkClientManager.buildChatMessage]：撤回消息必须
 * 保留在列表里、标记 `isDeleted` 并清空正文 / 媒体地址，
 * 会话摘要与引用预览都固定为「该消息已被撤回」。
 */
class MessageDeletedMappingTest {

    private val gson = Gson()

    @Test
    fun `历史 DTO 的 is_deleted 透传并清空内容`() {
        val dto = gson.fromJson(
            """{"id":42,"uid":"u1","type":"image","content":"","image_url":"https://oss/a.png","is_deleted":true,"created_at":"2024-01-01 10:00:00"}""",
            ChatMessageDto::class.java
        )
        val msg = ZeroTalkClientManager.mapChatMessageDto(dto, "room-1")
        assertTrue(msg.isDeleted)
        assertEquals("", msg.content)
        assertEquals("", msg.imageUrl)
        assertEquals(RECALLED_MESSAGE_TEXT, msg.previewText)
        assertEquals(RECALLED_MESSAGE_TEXT, msg.quotePreviewText())
    }

    @Test
    fun `历史 DTO 未撤回时保持普通消息`() {
        val dto = gson.fromJson(
            """{"id":43,"uid":"u1","type":"text","content":"你好","is_deleted":false,"created_at":"2024-01-01 10:00:00"}""",
            ChatMessageDto::class.java
        )
        val msg = ZeroTalkClientManager.mapChatMessageDto(dto, "room-1")
        assertFalse(msg.isDeleted)
        assertEquals("你好", msg.content)
        assertEquals("你好", msg.previewText)
    }

    @Test
    fun `WS 事件的 is_deleted 透传并清空内容`() {
        val event = parseMessageEvent(
            JsonParser.parseString("""{"event":"message","id":7,"type":"dice","content":"5","is_deleted":true}""").asJsonObject
        )
        assertTrue(event.isDeleted)
        val msg = ZeroTalkClientManager.buildChatMessage(
            ZeroTalkClientManager.RawChatMessage(
                id = "msg_7",
                senderId = "peer",
                content = event.content,
                type = event.type,
                timestamp = "12:00",
                timestampMs = 0L,
                serverId = event.messageId ?: 0L,
                isMine = false,
                isDeleted = event.isDeleted
            )
        )
        assertTrue(msg.isDeleted)
        assertEquals("", msg.content)
        assertEquals(RECALLED_MESSAGE_TEXT, msg.previewText)
    }
}
