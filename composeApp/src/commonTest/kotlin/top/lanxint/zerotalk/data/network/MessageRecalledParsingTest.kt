package top.lanxint.zerotalk.data.network

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `message_recalled` 事件解析回归测试。
 *
 * 字段名以官网 `publicAnnouncementDismiss-*.js` 的
 * `case "message_recalled": p.handleMessageRecall(d, p.addSystemMessage)` 为准：
 * 官方只消费 `message_id` / `username` / `by_moderator` / `by_room_admin`，
 * 其中 `message_id` 缺失时直接 return（本实现返回 null，事件丢弃）。
 *
 * 该解析此前完全缺失，导致服务端广播的撤回事件被丢进 `Unknown`，
 * 重进大厅后已撤回消息复原。
 */
class MessageRecalledParsingTest {

    private fun parse(json: String): WsServerEvent.MessageRecalled? =
        parseMessageRecalled(JsonParser.parseString(json).asJsonObject)

    @Test
    fun `顶层字段完整解析`() {
        val event = assertNotNull(
            parse(
                """{"event":"message_recalled","message_id":123,"room_id":"room-1","username":"kelo"}"""
            )
        )
        assertEquals(123L, event.messageId)
        assertEquals("room-1", event.roomId)
        assertEquals("kelo", event.username)
        assertFalse(event.byModerator)
        assertFalse(event.byRoomAdmin)
    }

    @Test
    fun `data 包裹字段可解析`() {
        val event = assertNotNull(
            parse("""{"event":"message_recalled","data":{"message_id":42,"room_id":"r2","username":"零友"}}""")
        )
        assertEquals(42L, event.messageId)
        assertEquals("r2", event.roomId)
        assertEquals("零友", event.username)
    }

    @Test
    fun `id 作为 message_id 的兼容字段`() {
        val event = assertNotNull(parse("""{"event":"message_recalled","id":99}"""))
        assertEquals(99L, event.messageId)
    }

    @Test
    fun `message_id 优先于 id`() {
        val event = assertNotNull(parse("""{"message_id":7,"id":8}"""))
        assertEquals(7L, event.messageId)
    }

    @Test
    fun `顶层为 null 时回落到 data`() {
        val event = assertNotNull(
            parse("""{"message_id":null,"data":{"message_id":5,"username":"x"}}""")
        )
        assertEquals(5L, event.messageId)
        assertEquals("x", event.username)
    }

    @Test
    fun `审核员删除标记解析`() {
        val event = assertNotNull(
            parse("""{"message_id":1,"username":"审核君","by_moderator":true}""")
        )
        assertTrue(event.byModerator)
        assertFalse(event.byRoomAdmin)
    }

    @Test
    fun `房管删除标记解析`() {
        val event = assertNotNull(
            parse("""{"message_id":2,"username":"房管","by_room_admin":true}""")
        )
        assertTrue(event.byRoomAdmin)
        assertFalse(event.byModerator)
    }

    @Test
    fun `缺少 message_id 返回 null`() {
        assertNull(parse("""{"event":"message_recalled","username":"kelo"}"""))
    }

    @Test
    fun `message_id 为 0 返回 null`() {
        assertNull(parse("""{"message_id":0,"username":"kelo"}"""))
    }

    @Test
    fun `message_id 非数字不抛异常返回 null`() {
        assertNull(parse("""{"message_id":"abc"}"""))
    }

    @Test
    fun `message_id 为对象不抛异常返回 null`() {
        assertNull(parse("""{"message_id":{"nested":1}}"""))
    }

    @Test
    fun `data 为非对象时仍能解析顶层字段`() {
        val event = assertNotNull(parse("""{"data":5,"message_id":9,"username":"kelo"}"""))
        assertEquals(9L, event.messageId)
        assertEquals("kelo", event.username)
    }

    @Test
    fun `room_id 与 username 缺失时为空`() {
        val event = assertNotNull(parse("""{"message_id":3}"""))
        assertNull(event.roomId)
        assertNull(event.username)
    }

    @Test
    fun `room_id 与 username 为空白串时归一化为空`() {
        val event = assertNotNull(parse("""{"message_id":4,"room_id":"  ","username":""}"""))
        assertNull(event.roomId)
        assertNull(event.username)
    }
}
