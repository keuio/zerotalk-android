package top.lanxint.zerotalk.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 「join_room 是否已在服务端生效、可以投递消息」的回归测试。
 *
 * 背景：`join_room` 是异步的，本机发帧后立刻把 `currentRoomId` 改成目标房间，
 * 但服务端在处理完该帧前仍按旧房间路由。协议规定 join 之后发消息不带 `room_id`，
 * 所以这期间发出的帧会被**静默丢弃**——表现为「有时候消息发不出去」。
 */
class RoomReadinessTest {

    private val roomA = "room-a"
    private val roomB = "room-b"
    private val settle = RoomReadiness.JOIN_SETTLE_MS

    @Test
    fun `刚发出 join 时尚未就绪（必须等待服务端处理）`() {
        assertFalse(
            RoomReadiness.isReady(roomA, ackedRoom = roomA, joinSentAtMs = 10_000L, nowMs = 10_000L)
        )
    }

    @Test
    fun `超过等待窗口后就绪`() {
        assertTrue(
            RoomReadiness.isReady(roomA, ackedRoom = roomA, joinSentAtMs = 10_000L, nowMs = 10_000L + settle)
        )
    }

    @Test
    fun `目标房间与已确认房间不一致时未就绪`() {
        // 刚要切到 B，但确认记录还停在 A：此时发消息会落在 A
        assertFalse(
            RoomReadiness.isReady(roomB, ackedRoom = roomA, joinSentAtMs = 0L, nowMs = 999_999L)
        )
    }

    @Test
    fun `从未 join 过时未就绪`() {
        assertFalse(RoomReadiness.isReady(roomA, ackedRoom = null, joinSentAtMs = 0L, nowMs = 999_999L))
    }

    @Test
    fun `目标房间为空或空白时未就绪`() {
        assertFalse(RoomReadiness.isReady(null, ackedRoom = roomA, joinSentAtMs = 0L, nowMs = 999_999L))
        assertFalse(RoomReadiness.isReady("", ackedRoom = roomA, joinSentAtMs = 0L, nowMs = 999_999L))
        assertFalse(RoomReadiness.isReady("   ", ackedRoom = roomA, joinSentAtMs = 0L, nowMs = 999_999L))
    }

    @Test
    fun `房间 id 两侧空白会被裁剪后比较`() {
        assertTrue(
            RoomReadiness.isReady(
                targetRoom = "  $roomA  ",
                ackedRoom = " $roomA ",
                joinSentAtMs = 1_000L,
                nowMs = 1_000L + settle
            )
        )
    }

    @Test
    fun `相似但不相同的房间 id 不会误判就绪`() {
        assertFalse(
            RoomReadiness.isReady(
                targetRoom = "$roomA-x",
                ackedRoom = roomA,
                joinSentAtMs = 0L,
                nowMs = 999_999L
            )
        )
    }

    // ---- 剩余等待时长 ----

    @Test
    fun `已就绪时剩余等待为 0`() {
        assertEquals(
            0L,
            RoomReadiness.remainingDelayMs(roomA, roomA, joinSentAtMs = 1_000L, nowMs = 1_000L + settle + 50L)
        )
    }

    @Test
    fun `刚发出 join 时剩余等待接近完整窗口`() {
        assertEquals(
            settle,
            RoomReadiness.remainingDelayMs(roomA, roomA, joinSentAtMs = 1_000L, nowMs = 1_000L)
        )
    }

    @Test
    fun `窗口过半时剩余等待为窗口的一半`() {
        assertEquals(
            settle / 2,
            RoomReadiness.remainingDelayMs(roomA, roomA, joinSentAtMs = 1_000L, nowMs = 1_000L + settle / 2)
        )
    }

    @Test
    fun `未 join 过的房间返回完整窗口作为安全下界`() {
        assertEquals(settle, RoomReadiness.remainingDelayMs(roomB, roomA, joinSentAtMs = 0L, nowMs = 5_000L))
        assertEquals(settle, RoomReadiness.remainingDelayMs(roomA, null, joinSentAtMs = 0L, nowMs = 5_000L))
        assertEquals(settle, RoomReadiness.remainingDelayMs("  ", roomA, joinSentAtMs = 0L, nowMs = 5_000L))
    }

    @Test
    fun `剩余等待永不为负`() {
        assertTrue(
            RoomReadiness.remainingDelayMs(roomA, roomA, joinSentAtMs = 1_000L, nowMs = 1_000_000L) >= 0L
        )
    }
}
