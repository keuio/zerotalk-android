package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 对局聊天 meta 的纯逻辑回归测试（对齐官方 `Mt` / `zn` / `Vn`）。
 *
 * 关键口径：
 * - 只有 `source=game` + `game_id>0` + `game_type` 在白名单内才算对局聊天消息；
 * - `sync_room` **缺省即同步**（照常进房间），只有显式 false 才只在对局面板可见；
 * - 对局面板只展示本局消息，且发送者必须是双方之一（观战者发言不展示）。
 */
class GameChatMetaTest {

    private fun meta(
        source: String = "game",
        gameId: Long = 100L,
        gameType: String = "chess",
        syncRoom: Boolean? = null,
        inviteMessageId: Long = 0L
    ) = GameChatMeta(
        source = source,
        gameId = gameId,
        gameType = gameType,
        inviteMessageId = inviteMessageId,
        syncRoom = syncRoom
    )

    @Test
    fun `合法 meta 判定`() {
        assertTrue(meta().isGameChat)
        assertTrue(meta(gameType = "gobang").isGameChat)
        assertTrue(meta(gameType = "GO").isGameChat, "game_type 大小写不敏感")
    }

    @Test
    fun `非法 meta 判定`() {
        assertFalse(meta(source = "room").isGameChat, "source 必须是 game")
        assertFalse(meta(gameId = 0L).isGameChat, "game_id 必须大于 0")
        assertFalse(meta(gameType = "dice").isGameChat, "白名单外的 game_type 视为普通消息")
        assertFalse(meta(gameType = "").isGameChat)
    }

    @Test
    fun `sync_room 缺省即同步进房间`() {
        assertTrue(meta(syncRoom = null).shouldShowInRoom())
        assertTrue(meta(syncRoom = true).shouldShowInRoom())
        assertFalse(meta(syncRoom = false).shouldShowInRoom())
    }

    @Test
    fun `非对局聊天消息一律进房间`() {
        assertTrue((null as GameChatMeta?).shouldShowInRoom())
        assertTrue(meta(source = "room", syncRoom = false).shouldShowInRoom())
        assertTrue(meta(gameId = 0L, syncRoom = false).shouldShowInRoom())
    }

    private fun message(
        senderId: String = "u1",
        gameId: Long = 100L,
        gameType: String = "chess",
        isDeleted: Boolean = false,
        content: String = "hi"
    ) = ChatMessage(
        id = "m1",
        senderId = senderId,
        content = content,
        timestamp = "10:00",
        isMine = false,
        isDeleted = isDeleted,
        gameMeta = meta(gameId = gameId, gameType = gameType)
    )

    @Test
    fun `对局面板只收本局消息`() {
        val players = setOf("u1", "u2")
        assertTrue(message(gameId = 100L).isGameChatFor(100L, players))
        assertFalse(message(gameId = 101L).isGameChatFor(100L, players), "别的对局不进本局面板")
    }

    @Test
    fun `观战者发言不进对局面板`() {
        assertFalse(message(senderId = "watcher").isGameChatFor(100L, setOf("u1", "u2")))
    }

    @Test
    fun `双方未加载时按本局归集`() {
        assertTrue(message().isGameChatFor(100L, emptySet()), "对局详情未就绪时不做发送者过滤")
    }

    @Test
    fun `撤回消息不进对局面板`() {
        assertFalse(message(isDeleted = true).isGameChatFor(100L, setOf("u1")))
    }

    @Test
    fun `非对局聊天消息不进对局面板`() {
        val plain = ChatMessage(
            id = "m2",
            senderId = "u1",
            content = "普通消息",
            timestamp = "10:00",
            isMine = false
        )
        assertFalse(plain.isGameChatFor(100L, setOf("u1")))
    }

    @Test
    fun `对局双方 uid 汇总白黑红与卧底玩家`() {
        val session = GameSessionDetail(
            id = 1L,
            whiteUid = "w",
            blackUid = "b",
            redUid = "r",
            players = listOf(
                UndercoverPlayer(uid = "p1"),
                UndercoverPlayer(uid = ""),
                UndercoverPlayer(uid = "p2")
            )
        )
        assertEquals(setOf("w", "b", "r", "p1", "p2"), session.playerUids())
    }

    @Test
    fun `历史条目映射保留对局 meta 与发送者`() {
        val item = ChatGameMessageItem(
            id = 42L,
            content = "对局内消息",
            createdAt = "2026-10-08 16:25:11",
            uid = "u1",
            username = "ok",
            type = "text",
            meta = meta()
        )
        val msg = item.toChatMessage(myUid = "me")
        assertEquals(42L, msg.serverId)
        assertEquals("u1", msg.senderId)
        assertEquals("ok", msg.senderName)
        assertEquals("16:25", msg.timestamp)
        assertEquals(100L, msg.gameMeta?.gameId)
        assertFalse(msg.isMine)
        assertNull(item.toChatMessage(myUid = "u1").gameMeta?.syncRoom, "sync_room 缺省保持 null")
    }
}
