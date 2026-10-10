package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [ChatMessageMerge] 的回归测试。
 *
 * 背景：大厅历史此前是 `history.filterNot{...} + existing`（只前插、不判重、不打乱顺序也不排序），
 * 一旦本地已有较新一批、bootstrap 只回最近 N 条，中间漏段就永远补不上，且顺序会颠倒成
 * 「较新的在上面、较旧的下面」。这里覆盖官方 `jn` / `Yn` 的去重 + 排序语义。
 */
class ChatMessageMergeTest {

    private fun msg(
        id: String,
        serverId: Long = 0L,
        timestampMs: Long = 0L,
        isDeleted: Boolean = false,
        isMine: Boolean = false,
        content: String = ""
    ) = ChatMessage(
        id = id,
        senderId = if (isMine) "me" else "peer",
        content = content,
        timestamp = "12:00",
        isMine = isMine,
        serverId = serverId,
        timestampMs = timestampMs,
        isDeleted = isDeleted
    )

    // ---------------- 去重 ----------------

    @Test
    fun `同一 serverId 只保留一条`() {
        val merged = ChatMessageMerge.merge(
            listOf(msg("msg_1", serverId = 1L, content = "updated")),
            listOf(msg("msg_1", serverId = 1L, content = "old"))
        )
        assertEquals(1, merged.size)
        // 同一条消息内容不变（撤回除外），保留已有副本避免历史快照冲掉实时富字段
        assertEquals("updated", merged.single().content)
    }

    @Test
    fun `本地乐观回显 serverId 为 0 不会被误删`() {
        val local = msg("room_r_1", serverId = 0L, timestampMs = 1_700_000_000_000L, isMine = true)
        val merged = ChatMessageMerge.merge(listOf(local), listOf(msg("msg_100", serverId = 100L)))
        assertEquals(2, merged.size)
        assertTrue(merged.any { it.id == "room_r_1" })
    }

    @Test
    fun `空 incoming 直接返回原列表实例`() {
        val existing = listOf(msg("msg_1", serverId = 1L))
        assertSame(existing, ChatMessageMerge.merge(existing, emptyList()))
    }

    @Test
    fun `空 existing 返回排序后的 incoming`() {
        val merged = ChatMessageMerge.merge(
            emptyList(),
            listOf(msg("msg_300", serverId = 300L), msg("msg_100", serverId = 100L))
        )
        assertEquals(listOf(100L, 300L), merged.map { it.serverId })
    }

    // ---------------- 排序 ----------------

    @Test
    fun `乱序拼接后按服务端 id 升序`() {
        val merged = ChatMessageMerge.merge(
            listOf(msg("msg_300", serverId = 300L), msg("msg_100", serverId = 100L)),
            listOf(msg("msg_200", serverId = 200L))
        )
        assertEquals(listOf(100L, 200L, 300L), merged.map { it.serverId })
    }

    @Test
    fun `本地回显排在服务端消息之后`() {
        val local = msg("room_r_1", serverId = 0L, timestampMs = 1_700_000_000_000L, isMine = true)
        val merged = ChatMessageMerge.merge(listOf(local), listOf(msg("msg_1", serverId = 1L)))
        assertEquals(listOf("msg_1", "room_r_1"), merged.map { it.id })
    }

    // ---------------- 中间缺口 ----------------

    @Test
    fun `中间缺口补上后列表连续`() {
        // 本地 [100..150]，bootstrap 只回最近 [201..250] -> 合并后中间空洞
        val gapped = ChatMessageMerge.merge(
            listOf(msg("msg_100", serverId = 100L), msg("msg_150", serverId = 150L)),
            listOf(msg("msg_201", serverId = 201L), msg("msg_250", serverId = 250L))
        )
        assertEquals(listOf(100L, 150L, 201L, 250L), gapped.map { it.serverId })

        // after_id 补页拿到 [151..200] -> 合并后连续
        val filled = ChatMessageMerge.merge(
            gapped,
            listOf(msg("msg_151", serverId = 151L), msg("msg_200", serverId = 200L))
        )
        assertEquals(listOf(100L, 150L, 151L, 200L, 201L, 250L), filled.map { it.serverId })
    }

    // ---------------- 撤回不复活 ----------------

    @Test
    fun `已有已撤回不会被未撤回的历史快照复活`() {
        val merged = ChatMessageMerge.merge(
            listOf(msg("msg_1", serverId = 1L, isDeleted = true, content = "")),
            listOf(msg("msg_1", serverId = 1L, isDeleted = false, content = "旧内容"))
        )
        assertTrue(merged.single().isDeleted)
        assertEquals("", merged.single().content)
    }

    @Test
    fun `服务端撤回覆盖本地未撤回副本`() {
        val merged = ChatMessageMerge.merge(
            listOf(msg("msg_1", serverId = 1L, isDeleted = false, content = "内容")),
            listOf(msg("msg_1", serverId = 1L, isDeleted = true, content = ""))
        )
        assertTrue(merged.single().isDeleted)
        assertEquals("", merged.single().content)
    }

    // ---------------- 追加去重（WS 推送 vs 补页双写） ----------------

    @Test
    fun `追加同 serverId 时替换旧的一条`() {
        val list = listOf(msg("msg_1", serverId = 1L, content = "旧"))
        val appended = ChatMessageMerge.appendDeduped(list, msg("msg_1", serverId = 1L, content = "新"))
        assertEquals(1, appended.size)
        assertEquals("新", appended.single().content)
    }

    @Test
    fun `追加不同 serverId 正常追加`() {
        val list = listOf(msg("msg_1", serverId = 1L))
        val appended = ChatMessageMerge.appendDeduped(list, msg("msg_2", serverId = 2L))
        assertEquals(listOf(1L, 2L), appended.map { it.serverId })
    }

    @Test
    fun `追加本地回显 serverId 为 0 不参与去重`() {
        val list = listOf(msg("room_a", serverId = 0L, isMine = true))
        val appended = ChatMessageMerge.appendDeduped(list, msg("room_b", serverId = 0L, isMine = true))
        assertEquals(2, appended.size)
    }

    // ---------------- 游标 ----------------

    @Test
    fun `最大最小服务端 id 忽略本地回显`() {
        val list = listOf(
            msg("local_1", serverId = 0L),
            msg("msg_100", serverId = 100L),
            msg("msg_300", serverId = 300L)
        )
        assertEquals(300L, ChatMessageMerge.maxServerId(list))
        assertEquals(100L, ChatMessageMerge.minServerId(list))
        assertNull(ChatMessageMerge.maxServerId(listOf(msg("local_1", serverId = 0L))))
        assertNull(ChatMessageMerge.minServerId(emptyList()))
    }
}
