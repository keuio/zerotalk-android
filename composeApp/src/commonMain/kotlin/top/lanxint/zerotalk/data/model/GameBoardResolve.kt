package top.lanxint.zerotalk.data.model

/**
 * 棋盘防回滚冲突决议（逐条对齐官方 `gameBoardSync-*.js` 的 `resolveBoard`，源码导出为 `p`）：
 *
 * ```js
 * function A(e){ const t=e.prevBoard??"", i=Number(e.nextVersion), n=Number(e.prevVersion),
 *   r=Number(e.nextMoveCount), v=Number(e.prevMoveCount);
 *   if(t&&isFinite(n)&&isFinite(i)&&i<n) return {ignore:true,board:t};            // ①
 *   if(t&&isFinite(n)&&n>0&&!isFinite(i)) return {ignore:true,board:t};          // ②
 *   const o=k(t),d=k(e.nextBoard);                                              // k() 跳过 ''/'0'/'.'/' '
 *   return o>0&&d===0&&(r>0||v>0) ? {ignore:false,board:t}                       // ③
 *        : (!e.allowFewerStones&&d<o) ? {ignore:false,board:t}                   // ④
 *        : {ignore:false,board:e.nextBoard}; }
 * ```
 *
 * 抽成纯函数是为了可单测：这条规则一旦判错，会出现「对手走子后棋盘被服务端旧帧覆盖」
 * 或「新版棋盘被误判为回滚而永远不更新」。
 */
internal object GameBoardResolve {

    /**
     * 棋盘上的子数（对齐官方 `k(e)`：跳过空字符 / `'0'` / `'.'` / `' '`）。
     */
    fun countStones(board: String?): Int {
        var count = 0
        val text = board.orEmpty()
        for (ch in text) {
            if (ch == '\u0000' || ch == '0' || ch == '.' || ch == ' ') continue
            count++
        }
        return count
    }

    /**
     * @param prevBoard 旧棋盘（本地已渲染的权威棋盘）
     * @param nextBoard 服务端新下发的棋盘
     * @param prevVersion 旧 version
     * @param nextVersion 新 version
     * @param prevMoveCount 旧 move_count
     * @param nextMoveCount 新 move_count
     * @param allowFewerStones 是否允许新棋盘子数变少（棋类吃子为 true；官方棋类均传 true）
     * @return 应当采用的棋盘：保留旧棋盘时返回 [prevBoard]，否则返回 [nextBoard]
     */
    fun resolveBoard(
        prevBoard: String?,
        nextBoard: String,
        prevVersion: Int,
        nextVersion: Int,
        prevMoveCount: Int,
        nextMoveCount: Int,
        allowFewerStones: Boolean
    ): String {
        val prev = prevBoard.orEmpty()
        // ① 旧棋盘非空且新 version 比旧 version 小 -> 保留旧棋盘
        if (prev.isNotEmpty() && nextVersion < prevVersion) return prev
        // ② 旧棋盘非空、旧 version > 0，但新 version 缺失 / 非法（Kotlin Int 下为 <= 0）-> 保留旧棋盘
        if (prev.isNotEmpty() && prevVersion > 0 && nextVersion <= 0) return prev
        val prevStones = countStones(prev)
        val nextStones = countStones(nextBoard)
        // ③ 旧棋盘有子而新棋盘全空，且任一侧已有落子记录 -> 保留旧棋盘
        if (prevStones > 0 && nextStones == 0 && (nextMoveCount > 0 || prevMoveCount > 0)) return prev
        // ④ 不允许子数变少时，新棋盘子数更少 -> 保留旧棋盘
        if (!allowFewerStones && nextStones < prevStones) return prev
        return nextBoard
    }
}
