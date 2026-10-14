package top.lanxint.zerotalk.data.model

/**
 * 「对方正在输入」的纯规则
 *
 * 抽成纯函数便于单测：展示（收到 `typing` 帧）与上报（发送 `typing` 帧）两侧共用同一口径，
 * 避免出现「群里显示了三点气泡」或「在群里把自己的输入状态广播出去」这类不一致。
 */
object PeerTypingRules {

    /**
     * 是否为「1v1 会话」（私聊 / 匹配）
     *
     * 群聊、暗号房与零语大厅都不算：官方状态名即 `peerIsTyping`（对端），
     * 群聊里无法确定是谁在输入，大厅更不适合展示。
     *
     * @param roomId 事件或当前会话的房间 id
     * @param hallRoomId 当前零语大厅房间 id（与 [roomId] 相同则视为大厅）
     * @param isGroupRoom 服务端下发的房间类型是否为群聊
     * @param category 会话分类（[MessageCategory.CODE] 即暗号房，本客户端按群聊口径处理）
     */
    fun isOneToOneRoom(
        roomId: String?,
        hallRoomId: String?,
        isGroupRoom: Boolean,
        category: MessageCategory?
    ): Boolean {
        val id = roomId?.takeIf { it.isNotBlank() } ?: return false
        if (!hallRoomId.isNullOrBlank() && id == hallRoomId) return false
        if (isGroupRoom) return false
        return category != MessageCategory.CODE
    }

    /**
     * 是否应当发送本次输入状态帧
     *
     * - `active=true`：距上次发送已过 [intervalMs] 才发（按按键节流，避免每次输入都发帧）；
     * - `active=false`：仅在之前确实发过 `true` 时补发一次结束帧（否则发无意义的帧）。
     */
    fun shouldSendTypingFrame(
        active: Boolean,
        lastSentActive: Boolean,
        sinceLastSentMs: Long,
        intervalMs: Long
    ): Boolean = if (active) sinceLastSentMs >= intervalMs else lastSentActive
}
