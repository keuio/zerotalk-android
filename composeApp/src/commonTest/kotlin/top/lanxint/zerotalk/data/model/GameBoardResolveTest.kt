package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [GameBoardResolve] 的回归测试。
 *
 * 对齐官方 `gameBoardSync-*.js` 的 `resolveBoard`（源码导出为 `p`）四条规则。
 * 这条规则判错会导致「对手走子后棋盘被旧帧覆盖」或「新版棋盘被误判为回滚而永不更新」。
 */
class GameBoardResolveTest {

    private val emptyBoard = ".".repeat(225)
    private val oneStone = "1" + ".".repeat(224)
    private val twoStones = "12" + ".".repeat(223)

    private fun resolve(
        prevBoard: String?,
        nextBoard: String,
        prevVersion: Int = 1,
        nextVersion: Int = 1,
        prevMoveCount: Int = 0,
        nextMoveCount: Int = 0,
        allowFewerStones: Boolean = true
    ): String = GameBoardResolve.resolveBoard(
        prevBoard = prevBoard,
        nextBoard = nextBoard,
        prevVersion = prevVersion,
        nextVersion = nextVersion,
        prevMoveCount = prevMoveCount,
        nextMoveCount = nextMoveCount,
        allowFewerStones = allowFewerStones
    )

    // ---------------- countStones ----------------

    @Test
    fun `计子跳过空位 0 与空格`() {
        assertEquals(2, GameBoardResolve.countStones("1.0 2"))
        assertEquals(0, GameBoardResolve.countStones("000"))
        assertEquals(0, GameBoardResolve.countStones(""))
        assertEquals(0, GameBoardResolve.countStones(null))
        assertEquals(4, GameBoardResolve.countStones("RNBQ"))
        assertEquals(2, GameBoardResolve.countStones("1 2"))
    }

    // ---------------- 规则①：新 version 更小 ----------------

    @Test
    fun `规则1 新 version 更小则保留旧棋盘`() {
        assertEquals(oneStone, resolve(oneStone, twoStones, prevVersion = 5, nextVersion = 4))
    }

    @Test
    fun `规则1 旧棋盘字符串为空时不拦截`() {
        assertEquals(twoStones, resolve("", twoStones, prevVersion = 5, nextVersion = 4))
    }

    // ---------------- 规则②：新 version 缺失 ----------------

    @Test
    fun `规则2 旧 version 大于 0 而新 version 缺失则保留旧棋盘`() {
        assertEquals(oneStone, resolve(oneStone, twoStones, prevVersion = 3, nextVersion = 0))
    }

    @Test
    fun `规则2 旧棋盘字符串为空时不拦截`() {
        assertEquals(twoStones, resolve("", twoStones, prevVersion = 3, nextVersion = 0))
    }

    // ---------------- 规则③：新棋盘全空 ----------------

    @Test
    fun `规则3 新棋盘全空且新 move_count 大于 0 时保留旧棋盘`() {
        assertEquals(oneStone, resolve(oneStone, emptyBoard, nextMoveCount = 2))
    }

    @Test
    fun `规则3 新棋盘全空且仅旧 move_count 大于 0 时也保留旧棋盘`() {
        // 官方口径为 nextMoveCount>0 || prevMoveCount>0，此前只判了新值
        assertEquals(oneStone, resolve(oneStone, emptyBoard, prevMoveCount = 3, nextMoveCount = 0))
    }

    @Test
    fun `规则3 两侧 move_count 都为 0 时采用新棋盘`() {
        assertEquals(emptyBoard, resolve(oneStone, emptyBoard, prevMoveCount = 0, nextMoveCount = 0))
    }

    // ---------------- 规则④：子数变少 ----------------

    @Test
    fun `规则4 不允许子数变少时保留旧棋盘`() {
        assertEquals(twoStones, resolve(twoStones, oneStone, allowFewerStones = false))
    }

    @Test
    fun `规则4 允许子数变少（棋类吃子）时采用新棋盘`() {
        assertEquals(oneStone, resolve(twoStones, oneStone, allowFewerStones = true))
    }

    // ---------------- 正常前进 ----------------

    @Test
    fun `正常前进 version 增加时采用新棋盘`() {
        assertEquals(twoStones, resolve(oneStone, twoStones, prevVersion = 1, nextVersion = 2))
    }

    @Test
    fun `同 version 棋盘变化不被丢弃`() {
        assertEquals(twoStones, resolve(oneStone, twoStones, prevVersion = 2, nextVersion = 2))
    }

    @Test
    fun `首帧旧棋盘为 null 时采用新棋盘`() {
        assertEquals(twoStones, resolve(null, twoStones, prevVersion = 0, nextVersion = 1))
    }
}
