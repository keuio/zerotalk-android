package top.lanxint.zerotalk.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import top.lanxint.zerotalk.data.network.ChatMessageDto
import top.lanxint.zerotalk.data.network.parseMessageEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 用户称号从「历史 DTO / WS 事件」透传到 `ChatMessage.authorTitle` 的回归测试。
 *
 * 官方消息行是 `title: e.msg.title, color: e.msg.title_color`；本客户端两条链路
 * 共用 [ZeroTalkClientManager.buildChatMessage]，称号必须原样落到 UI 模型，
 * 服务端未下发时保持空串（UI 不渲染空徽章）。
 */
class UserTitleMappingTest {

    private val gson = Gson()

    @Test
    fun `历史 DTO 的称号透传到 ChatMessage`() {
        val dto = gson.fromJson(
            """{"id":42,"uid":"u1","type":"text","content":"hi","title":"银之钥","title_color":"gold","created_at":"2024-01-01 10:00:00"}""",
            ChatMessageDto::class.java
        )
        val msg = ZeroTalkClientManager.mapChatMessageDto(dto, "room-1")
        assertEquals("银之钥", msg.authorTitle)
        assertEquals("gold", msg.authorTitleColor)
    }

    @Test
    fun `历史 DTO 未下发称号时为空串`() {
        val dto = gson.fromJson(
            """{"id":43,"uid":"u1","type":"text","content":"hi","created_at":"2024-01-01 10:00:00"}""",
            ChatMessageDto::class.java
        )
        val msg = ZeroTalkClientManager.mapChatMessageDto(dto, "room-1")
        assertEquals("", msg.authorTitle)
        assertEquals("", msg.authorTitleColor)
    }

    @Test
    fun `WS 事件的称号透传到 ChatMessage`() {
        val event = parseMessageEvent(
            JsonParser.parseString(
                """{"event":"message","id":7,"type":"text","content":"hi","uid":"u2","username":"甲","title":"公共房间审查员","title_color":"red"}"""
            ).asJsonObject
        )
        val msg = ZeroTalkClientManager.buildChatMessage(
            ZeroTalkClientManager.RawChatMessage(
                id = "msg_7",
                senderId = event.fromUid ?: "peer",
                content = event.content,
                type = event.type,
                timestamp = "12:00",
                timestampMs = 0L,
                serverId = event.messageId ?: 0L,
                isMine = false,
                senderName = event.username.orEmpty(),
                authorTitle = event.title.orEmpty(),
                authorTitleColor = event.titleColor.orEmpty()
            )
        )
        assertEquals("公共房间审查员", msg.authorTitle)
        assertEquals("red", msg.authorTitleColor)
    }
}
