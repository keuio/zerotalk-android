package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 「对方正在输入」纯规则回归测试。
 *
 * 两条口径必须锁死：
 * 1. 只有 1v1 会话（私聊 / 匹配）参与显示与上报 —— 群聊、暗号房、零语大厅都不行；
 * 2. 上报节流：`active=true` 按间隔节流，`active=false` 只在报过 `true` 之后补发一次。
 */
class PeerTypingRulesTest {

    private fun isOneToOne(
        roomId: String? = "dm_1",
        hallRoomId: String? = "hall_1",
        isGroupRoom: Boolean = false,
        category: MessageCategory? = MessageCategory.PRIVATE
    ) = PeerTypingRules.isOneToOneRoom(roomId, hallRoomId, isGroupRoom, category)

    @Test
    fun `私聊与匹配房间算 1v1`() {
        assertTrue(isOneToOne(category = MessageCategory.PRIVATE))
        assertTrue(isOneToOne(category = MessageCategory.MATCH))
    }

    @Test
    fun `群聊与暗号房不算 1v1`() {
        assertFalse(isOneToOne(isGroupRoom = true), "服务端标记的群聊房间")
        assertFalse(isOneToOne(category = MessageCategory.CODE), "暗号房按群聊口径处理")
    }

    @Test
    fun `零语大厅不算 1v1`() {
        assertFalse(isOneToOne(roomId = "hall_1", hallRoomId = "hall_1", category = null))
    }

    @Test
    fun `房间号缺失或空白不算 1v1`() {
        assertFalse(isOneToOne(roomId = null))
        assertFalse(isOneToOne(roomId = "  "))
    }

    @Test
    fun `未进过大厅时大厅判定不误伤普通房间`() {
        assertTrue(isOneToOne(roomId = "dm_1", hallRoomId = null))
        assertTrue(isOneToOne(roomId = "dm_1", hallRoomId = ""))
    }

    @Test
    fun `输入中帧按间隔节流`() {
        assertFalse(
            PeerTypingRules.shouldSendTypingFrame(
                active = true, lastSentActive = false, sinceLastSentMs = 500L, intervalMs = 2000L
            ),
            "刚发过不足 2 秒不再发"
        )
        assertTrue(
            PeerTypingRules.shouldSendTypingFrame(
                active = true, lastSentActive = false, sinceLastSentMs = 2000L, intervalMs = 2000L
            )
        )
    }

    @Test
    fun `结束帧仅在报过输入中之后补发`() {
        assertFalse(
            PeerTypingRules.shouldSendTypingFrame(
                active = false, lastSentActive = false, sinceLastSentMs = 10_000L, intervalMs = 2000L
            ),
            "从未上报过输入中，不必发结束帧"
        )
        assertTrue(
            PeerTypingRules.shouldSendTypingFrame(
                active = false, lastSentActive = true, sinceLastSentMs = 100L, intervalMs = 2000L
            ),
            "结束帧不受节流限制，必须立刻发出"
        )
    }
}
