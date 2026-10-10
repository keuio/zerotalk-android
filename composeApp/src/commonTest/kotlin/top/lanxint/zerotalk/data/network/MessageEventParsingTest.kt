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

    // ---- 端到端加密帧（官方 enc / client_message_id）----

    @Test
    fun `加密消息的 enc 与 client_message_id 透传`() {
        val event = parse(
            """{"event":"message","id":11,"type":"text","content":"Y2lwaGVy","uid":"u1",""" +
                """"client_message_id":"cmid-1","enc":{"v":1,"alg":"AES-256-GCM","iv":"aXY","kid":"room_dek_v1","sid":"42"}}"""
        )
        assertEquals("cmid-1", event.clientMessageId)
        val enc = event.enc
        assertTrue(enc != null)
        assertEquals(1, enc.version)
        assertEquals("AES-256-GCM", enc.algorithm)
        assertEquals("aXY", enc.iv)
        assertEquals("room_dek_v1", enc.keyId)
        assertEquals("42", enc.senderUid)
    }

    @Test
    fun `非加密消息 enc 为 null`() {
        val event = parse("""{"event":"message","id":12,"type":"text","content":"hi"}""")
        assertEquals(null, event.enc)
        assertEquals(null, event.clientMessageId)
    }

    @Test
    fun `enc 为脏数据时不抛异常`() {
        assertTrue(parse("""{"event":"message","id":13,"enc":"oops"}""").enc == null)
        assertTrue(parse("""{"event":"message","id":14,"enc":null}""").enc == null)
        // iv 缺失仍能解析出对象，缺字段回落默认值
        val partial = parse("""{"event":"message","id":15,"enc":{"sid":7}}""")
        assertEquals("", partial.enc?.iv)
        assertEquals("7", partial.enc?.senderUid)
        assertEquals(1, partial.enc?.version)
    }

    @Test
    fun `历史 DTO 的加密字段解析`() {
        val dto = Gson().fromJson(
            """{"id":21,"content":"Y2lwaGVy","client_message_id":"cmid-9","enc":{"v":1,"alg":"AES-256-GCM","iv":"aXY"}}""",
            ChatMessageDto::class.java
        )
        assertEquals("cmid-9", dto.clientMessageId)
        assertEquals("aXY", dto.enc?.iv)
        assertEquals(1, dto.enc?.version)
    }
}
