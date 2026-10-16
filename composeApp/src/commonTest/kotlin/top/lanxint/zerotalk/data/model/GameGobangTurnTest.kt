package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameGobangTurnTest {

    private fun isGobangMyTurn(
        session: GameSessionDetail,
        myUid: String
    ): Boolean {
        if (session.status != "playing") return false
        val isBlack = session.blackUid == myUid
        val isWhite = session.whiteUid == myUid
        return when {
            session.turnSide != null -> {
                (session.turnSide == "black" && isBlack) || (session.turnSide == "white" && isWhite)
            }
            else -> {
                (session.turn == 1 && isBlack) || (session.turn == 2 && isWhite)
            }
        }
    }

    @Test
    fun `五子棋开局turn为1时黑方先行白方等待`() {
        val session = GameSessionDetail(
            id = 1001L,
            gameType = "gobang",
            status = "playing",
            blackUid = "user_black",
            whiteUid = "user_white",
            turn = 1
        )

        assertTrue(isGobangMyTurn(session, "user_black"), "开局 turn=1 时黑方应该可以走子")
        assertFalse(isGobangMyTurn(session, "user_white"), "开局 turn=1 时白方不应该可以走子，避免报错还没轮到你")
        assertFalse(isGobangMyTurn(session, "user_spectator"), "观战者不能走子")
    }

    @Test
    fun `五子棋turn为2时白方行棋黑方等待`() {
        val session = GameSessionDetail(
            id = 1001L,
            gameType = "gobang",
            status = "playing",
            blackUid = "user_black",
            whiteUid = "user_white",
            turn = 2
        )

        assertFalse(isGobangMyTurn(session, "user_black"), "turn=2 时黑方等待")
        assertTrue(isGobangMyTurn(session, "user_white"), "turn=2 时白方行棋")
    }

    @Test
    fun `五子棋下发turnSide时优先以turnSide为准`() {
        val sessionBlackTurn = GameSessionDetail(
            id = 1001L,
            gameType = "gobang",
            status = "playing",
            blackUid = "user_black",
            whiteUid = "user_white",
            turnSide = "black"
        )
        assertTrue(isGobangMyTurn(sessionBlackTurn, "user_black"))
        assertFalse(isGobangMyTurn(sessionBlackTurn, "user_white"))

        val sessionWhiteTurn = GameSessionDetail(
            id = 1001L,
            gameType = "gobang",
            status = "playing",
            blackUid = "user_black",
            whiteUid = "user_white",
            turnSide = "white"
        )
        assertFalse(isGobangMyTurn(sessionWhiteTurn, "user_black"))
        assertTrue(isGobangMyTurn(sessionWhiteTurn, "user_white"))
    }
}
