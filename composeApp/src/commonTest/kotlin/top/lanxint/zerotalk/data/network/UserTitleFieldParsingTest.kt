package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 用户称号字段（`title` / `title_color`）在各 DTO 与 WS 事件上的解析回归测试。
 *
 * 官方在消息对象（`msg.title` / `msg.title_color`）、动态（`item.title`）、
 * 用户主页（`user.title`）与成员列表（`member.title`）上都下发这两个字段；
 * 字段缺失时必须保持 null（UI 层据此不渲染徽章），不能解析成 "null" 之类的脏值。
 */
class UserTitleFieldParsingTest {

    private val gson = Gson()

    @Test
    fun `历史消息 DTO 解析 title 与 title_color`() {
        val dto = gson.fromJson(
            """{"id":1,"uid":"u1","content":"hi","title":"银之钥","title_color":"gold"}""",
            ChatMessageDto::class.java
        )
        assertEquals("银之钥", dto.title)
        assertEquals("gold", dto.titleColor)
    }

    @Test
    fun `历史消息 DTO 缺少称号字段时为 null`() {
        val dto = gson.fromJson("""{"id":1,"uid":"u1","content":"hi"}""", ChatMessageDto::class.java)
        assertNull(dto.title)
        assertNull(dto.titleColor)
    }

    @Test
    fun `动态 DTO 解析 title 与 title_color`() {
        val dto = gson.fromJson(
            """{"id":2,"uid":"u1","username":"甲","title":"公共房间审查员","title_color":"red"}""",
            MomentItemDto::class.java
        )
        assertEquals("公共房间审查员", dto.title)
        assertEquals("red", dto.titleColor)
        assertNull(gson.fromJson("""{"id":2}""", MomentItemDto::class.java).title)
    }

    @Test
    fun `用户主页 DTO 解析 title 与 title_color`() {
        val dto = gson.fromJson(
            """{"uid":"u1","username":"甲","title":"银之钥","title_color":"violet"}""",
            UserProfileDto::class.java
        )
        assertEquals("银之钥", dto.title)
        assertEquals("violet", dto.titleColor)
        assertNull(gson.fromJson("""{"uid":"u1"}""", UserProfileDto::class.java).titleColor)
    }

    @Test
    fun `成员与对端 DTO 解析称号字段`() {
        val roomMember = gson.fromJson(
            """{"uid":"u1","username":"甲","title":"管理员","title_color":"cyan"}""",
            RoomMemberDto::class.java
        )
        assertEquals("管理员", roomMember.title)
        assertEquals("cyan", roomMember.titleColor)

        val bootstrapMember = gson.fromJson(
            """{"uid":"u1","username":"甲","title":"管理员","title_color":"cyan"}""",
            BootstrapMemberDto::class.java
        )
        assertEquals("管理员", bootstrapMember.title)
        assertEquals("cyan", bootstrapMember.titleColor)

        val peer = gson.fromJson(
            """{"uid":"u1","username":"甲","title":"银之钥","title_color":"pink"}""",
            PeerUserDto::class.java
        )
        assertEquals("银之钥", peer.title)
        assertEquals("pink", peer.titleColor)
    }

    @Test
    fun `WS message 事件解析 title 与 title_color`() {
        val event = parseMessageEvent(
            JsonParser.parseString(
                """{"event":"message","id":7,"type":"text","content":"hi","title":"银之钥","title_color":"gold"}"""
            ).asJsonObject
        )
        assertEquals("银之钥", event.title)
        assertEquals("gold", event.titleColor)
    }

    @Test
    fun `WS message 事件 data 包裹与缺省`() {
        val wrapped = parseMessageEvent(
            JsonParser.parseString(
                """{"event":"message","data":{"id":8,"title":"公共房间审查员","title_color":"RED"}}"""
            ).asJsonObject
        )
        assertEquals("公共房间审查员", wrapped.title)
        // 大小写不敏感由 UI 层归一化，这里保留原始值
        assertEquals("RED", wrapped.titleColor)

        val missing = parseMessageEvent(JsonParser.parseString("""{"event":"message","id":9}""").asJsonObject)
        assertNull(missing.title)
        assertNull(missing.titleColor)
    }
}
