package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `message` 事件解析回归测试（重点是 `is_deleted` 撤回标记）。
 *
 * 官网 `ws.on("message", …)` 把它归一化为 `is_deleted: !!e.is_deleted`；
 * 此前本客户端的解析完全没读该字段，导致带 `is_deleted` 的实时消息
 * 被当成普通消息渲染。
 */
class MessageEventParsingTest {

    private fun parse(json: String): WsServerEvent.Message =
        parseMessageEvent(JsonParser.parseString(json).asJsonObject)

    @Test
    fun `is_deleted 透传`() {
        val event = parse("""{"event":"message","id":7,"type":"image","is_deleted":true}""")
        assertTrue(event.isDeleted)
    }

    @Test
    fun `缺省 is_deleted 为 false`() {
        assertFalse(parse("""{"event":"message","id":7}""").isDeleted)
    }

    @Test
    fun `data 包裹的 is_deleted 也能解析`() {
        val event = parse("""{"event":"message","data":{"id":8,"is_deleted":true}}""")
        assertTrue(event.isDeleted)
        assertEquals(8L, event.messageId)
    }

    @Test
    fun `is_deleted 为 null 时为 false`() {
        assertFalse(parse("""{"event":"message","id":7,"is_deleted":null}""").isDeleted)
    }

    @Test
    fun `常规字段仍然解析`() {
        val event = parse(
            """{"event":"message","id":9,"type":"text","content":"hi","uid":"u1","is_self":true,"image_url":"https://oss/a.png"}"""
        )
        assertEquals(9L, event.messageId)
        assertEquals("text", event.type)
        assertEquals("hi", event.content)
        assertEquals("u1", event.fromUid)
        assertEquals(true, event.isSelf)
        assertEquals("https://oss/a.png", event.imageUrl)
        assertFalse(event.isDeleted)
    }

    @Test
    fun `历史 DTO 的 is_deleted 字段解析`() {
        val gson = Gson()
        assertTrue(
            gson.fromJson("""{"id":1,"is_deleted":true}""", ChatMessageDto::class.java).isDeleted
        )
        assertFalse(
            gson.fromJson("""{"id":1}""", ChatMessageDto::class.java).isDeleted
        )
    }
}
