package top.lanxint.zerotalk.data.network

import com.google.gson.JsonParser
import top.lanxint.zerotalk.data.model.shouldShowInRoom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `message` 事件里的对局聊天 meta 解析回归测试。
 *
 * 对齐官方 `Mt`：只有 `source=game` + `game_id>0` + `game_type` 在白名单内才认；
 * 且 `sync_room` **缺省必须是 null 而不是 false**（官方按「缺省即同步到房间」处理，
 * 若误解析成 false，对局内聊天就会从房间列表里凭空消失）。
 */
class GameChatMetaParsingTest {

    private fun parse(json: String) = parseMessageEvent(JsonParser.parseString(json).asJsonObject)

    @Test
    fun `对局聊天 meta 解析`() {
        val event = parse(
            """{"event":"message","id":1,"type":"text","content":"hi","meta":""" +
                """{"source":"game","game_id":20860,"game_type":"chess","invite_message_id":3328429,"sync_room":false}}"""
        )
        val meta = event.meta
        assertEquals("game", meta?.source)
        assertEquals(20860L, meta?.gameId)
        assertEquals("chess", meta?.gameType)
        assertEquals(3328429L, meta?.inviteMessageId)
        assertEquals(false, meta?.syncRoom)
        assertTrue(meta!!.isGameChat)
        assertFalse(meta.shouldShowInRoom())
    }

    @Test
    fun `sync_room 缺省保持 null`() {
        val event = parse(
            """{"event":"message","id":2,"meta":{"source":"game","game_id":1,"game_type":"go"}}"""
        )
        assertNull(event.meta?.syncRoom)
        assertTrue(event.meta!!.shouldShowInRoom(), "缺省即同步到房间")
    }

    @Test
    fun `非 game 来源的 meta 视为普通消息`() {
        val event = parse(
            """{"event":"message","id":3,"meta":{"source":"room","game_id":1,"game_type":"go"}}"""
        )
        assertNull(event.meta)
    }

    @Test
    fun `白名单外的 game_type 视为普通消息`() {
        val event = parse(
            """{"event":"message","id":4,"meta":{"source":"game","game_id":1,"game_type":"dice"}}"""
        )
        assertNull(event.meta)
    }

    @Test
    fun `缺少 meta 字段时为 null`() {
        assertNull(parse("""{"event":"message","id":5,"type":"text","content":"x"}""").meta)
    }

    @Test
    fun `脏 meta 载荷不抛异常`() {
        val event = parse("""{"event":"message","id":6,"meta":"not-an-object"}""")
        assertNull(event.meta)
    }

    @Test
    fun `data 包裹的 meta 同样解析`() {
        val event = parse(
            """{"event":"message","data":{"id":7,"meta":{"source":"game","game_id":9,"game_type":"xiangqi"}}}"""
        )
        assertEquals(9L, event.meta?.gameId)
    }
}
