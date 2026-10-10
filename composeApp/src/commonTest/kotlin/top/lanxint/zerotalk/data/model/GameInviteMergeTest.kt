package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [ChatGameInvite.overlayOn] 与 [GameSessionDetail.toChatGameInvite] 的回归测试。
 *
 * 背景：`xiangqi_update` 等 `*_update` 可能只带 `game` 不带 `invite`，
 * 需要用完整对局构造卡片；同时不能让构造出的卡片把旧卡片的身份 / 头像冲掉。
 */
class GameInviteMergeTest {

    private fun invite(
        gameId: Long = 1L,
        gameType: String = "xiangqi",
        status: String = "waiting",
        result: String = "none",
        whiteUsername: String? = null,
        blackUsername: String? = null,
        whiteAvatarUrl: String? = null,
        winnerUid: String? = null,
        messageId: Long = 0L,
        maxPlayers: Int = 4,
        playerCount: Int = 0
    ) = ChatGameInvite(
        gameId = gameId,
        gameType = gameType,
        status = status,
        result = result,
        whiteUsername = whiteUsername,
        blackUsername = blackUsername,
        whiteAvatarUrl = whiteAvatarUrl,
        winnerUid = winnerUid,
        messageId = messageId,
        maxPlayers = maxPlayers,
        playerCount = playerCount
    )

    @Test
    fun `状态字段被服务端新值覆盖`() {
        val old = invite(status = "waiting", result = "none")
        val new = invite(status = "playing", result = "none")
        assertEquals("playing", new.overlayOn(old).status)
    }

    @Test
    fun `旧卡片的身份与头像被保留`() {
        val old = invite(whiteUsername = "ok", blackUsername = "kelo", whiteAvatarUrl = "https://a.png")
        val merged = invite().overlayOn(old) // 新卡片缺身份字段
        assertEquals("ok", merged.whiteUsername)
        assertEquals("kelo", merged.blackUsername)
        assertEquals("https://a.png", merged.whiteAvatarUrl)
    }

    @Test
    fun `结果与胜者被覆盖`() {
        val old = invite(result = "none", winnerUid = null)
        val new = invite(result = "draw", winnerUid = "u1")
        val merged = new.overlayOn(old)
        assertEquals("draw", merged.result)
        assertEquals("u1", merged.winnerUid)
    }

    @Test
    fun `旧卡片为空时直接采用新卡片`() {
        val new = invite(status = "finished")
        assertEquals(new, new.overlayOn(null))
    }

    @Test
    fun `新卡片缺失的数值字段回落旧卡片`() {
        val old = invite(messageId = 123L, maxPlayers = 4, playerCount = 3)
        val merged = invite(messageId = 0L, maxPlayers = 0, playerCount = 0).overlayOn(old)
        assertEquals(123L, merged.messageId)
        assertEquals(4, merged.maxPlayers)
        assertEquals(3, merged.playerCount)
    }

    @Test
    fun `对局详情构造卡片保留关键状态字段`() {
        val detail = GameSessionDetail(
            id = 9L,
            gameType = "gobang",
            status = "playing",
            whiteUsername = "a",
            blackUsername = "b",
            result = "none",
            messageId = 123L
        )
        val card = detail.toChatGameInvite()
        assertEquals(9L, card.gameId)
        assertEquals("gobang", card.gameType)
        assertEquals("playing", card.status)
        assertEquals("a", card.whiteUsername)
        assertEquals("b", card.blackUsername)
        assertEquals(123L, card.messageId)
    }

    @Test
    fun `对局详情缺 game_type 时用事件名兜底`() {
        val detail = GameSessionDetail(id = 7L, gameType = "", status = "playing")
        assertEquals("xiangqi", detail.toChatGameInvite(fallbackGameType = "xiangqi").gameType)
    }

    @Test
    fun `对局详情缺 status 时兜底为 waiting`() {
        val detail = GameSessionDetail(id = 8L, gameType = "go", status = "")
        assertEquals("waiting", detail.toChatGameInvite().status)
    }
}
