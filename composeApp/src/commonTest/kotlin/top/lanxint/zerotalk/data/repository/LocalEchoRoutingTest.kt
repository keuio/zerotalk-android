package top.lanxint.zerotalk.data.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 「大厅 vs 普通房间」本地乐观回显落库判定 [isHallEchoTarget] 的回归测试。
 *
 * 这条判定决定本地回显写进 _hallMessages 还是 _roomMessages[roomId]：
 * 判成房间会让大厅发骰子 / 图片 / 游戏邀请时本地看不到（只能等服务端广播），
 * 判成大厅则会让会话列表凭空多出一行。
 */
class LocalEchoRoutingTest {

    private val hallId = "a9bee4a25027ab143de9f313aabbc34a"
    private val publicRooms = listOf("a9bee4a25027ab143de9f313aabbc34a", "public-2")

    @Test
    fun `roomId 命中当前大厅 room_id 时判为大厅`() {
        assertTrue(isHallEchoTarget(hallId, hallId, publicRooms))
    }

    @Test
    fun `roomId 命中 bootstrap 公共房间列表时判为大厅`() {
        // 大厅 room_id 变动 / 本地与服务端不一致时，仍能靠公共房间列表识别
        assertTrue(isHallEchoTarget("public-2", hallId, publicRooms))
    }

    @Test
    fun `普通私聊房间判为房间`() {
        assertFalse(isHallEchoTarget("room-private-1", hallId, publicRooms))
    }

    @Test
    fun `roomId 为空或空白时判为房间`() {
        assertFalse(isHallEchoTarget(null, hallId, publicRooms))
        assertFalse(isHallEchoTarget("", hallId, publicRooms))
        assertFalse(isHallEchoTarget("   ", hallId, publicRooms))
    }

    @Test
    fun `大厅 room_id 未知时仍按公共房间列表判定`() {
        assertTrue(isHallEchoTarget("public-2", null, publicRooms))
        assertFalse(isHallEchoTarget("room-private-1", null, publicRooms))
    }

    @Test
    fun `公共房间列表为空时只有当前大厅命中`() {
        assertTrue(isHallEchoTarget(hallId, hallId, emptyList()))
        assertFalse(isHallEchoTarget("public-2", hallId, emptyList()))
    }

    @Test
    fun `两侧空白会被裁剪后比较`() {
        assertTrue(isHallEchoTarget("  $hallId  ", hallId, emptyList()))
        assertTrue(isHallEchoTarget("public-2", "  $hallId  ", listOf("  public-2  ")))
    }

    @Test
    fun `相似但不相同的 id 不会误判为大厅`() {
        assertFalse(isHallEchoTarget(hallId + "x", hallId, publicRooms))
        assertFalse(isHallEchoTarget("public-2x", hallId, publicRooms))
    }
}
