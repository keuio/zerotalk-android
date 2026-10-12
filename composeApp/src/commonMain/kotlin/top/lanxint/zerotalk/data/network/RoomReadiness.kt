package top.lanxint.zerotalk.data.network

/**
 * 「房间上下文是否已就绪、可以投递不带 `room_id` 的消息帧」的纯判定。
 *
 * ## 为什么需要它
 *
 * `join_room` 是**异步**的：本机发出该帧后立即把 `currentRoomId` 改成目标房间，
 * 但服务端在收到并处理该帧之前，仍会把消息路由到**旧房间**。协议规定 join 之后
 * 发消息**不带** `room_id`，因此这期间发出的帧会被服务端静默丢弃——
 * 这就是「有时候消息发不出去」的根因。
 *
 * 抽成纯函数是为了让「何时算就绪」可被单测覆盖，而不必真的连线上服务端
 * （项目规范禁止对线上接口做自动化测试）。
 */
internal object RoomReadiness {

    /**
     * `join_room` 后等待服务端切换房间上下文的最短时间。
     *
     * 服务端实测多数情况不回 `join_room` 的确认帧，无法靠回包同步，
     * 只能用保守延迟覆盖一次「服务端处理 + 消息路由」的往返。
     */
    const val JOIN_SETTLE_MS = 150L

    /**
     * 是否已可安全发送。
     *
     * @param targetRoom 目标房间（调用方传入，可能为空 / 含空白）
     * @param ackedRoom 最近一次成功发出的 join_room 目标房间
     * @param joinSentAtMs 该次 join_room 发出的时刻（0 表示从未发出）
     * @param nowMs 当前时刻（由调用方注入，便于测试）
     */
    fun isReady(
        targetRoom: String?,
        ackedRoom: String?,
        joinSentAtMs: Long,
        nowMs: Long
    ): Boolean {
        val target = targetRoom?.trim().orEmpty()
        if (target.isEmpty()) return false
        if (ackedRoom?.trim() != target) return false
        if (joinSentAtMs <= 0L) return false
        return nowMs - joinSentAtMs >= JOIN_SETTLE_MS
    }

    /**
     * 距离可以安全发送还需等待的毫秒数（已就绪返回 0）。
     *
     * 目标房间未被 join 过时返回完整窗口：调用方通常紧接着就会发出 join_room，
     * 等待一个完整窗口是安全的下界。
     */
    fun remainingDelayMs(
        targetRoom: String?,
        ackedRoom: String?,
        joinSentAtMs: Long,
        nowMs: Long
    ): Long {
        val target = targetRoom?.trim().orEmpty()
        if (target.isEmpty()) return JOIN_SETTLE_MS
        if (ackedRoom?.trim() != target) return JOIN_SETTLE_MS
        if (joinSentAtMs <= 0L) return JOIN_SETTLE_MS
        return (JOIN_SETTLE_MS - (nowMs - joinSentAtMs)).coerceAtLeast(0L)
    }
}
