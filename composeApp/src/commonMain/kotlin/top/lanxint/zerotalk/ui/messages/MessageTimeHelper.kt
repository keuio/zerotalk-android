package top.lanxint.zerotalk.ui.messages

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.data.model.ChatMessage
import kotlin.math.abs

/**
 * 聊天时间分隔条与消息时间文案计算工具
 *
 * 出现条件：
 * 1. 相邻两条消息时间相差 ≥ 5 分钟；
 * 2. 会话首条有效非系统消息。
 *
 * 文案格式（按自然日跨度）：
 * - 当天：`HH:mm`（例 22:45）
 * - 昨天：`昨天 HH:mm`
 * - 超过 1 天、7 天以内：`周X HH:mm`（周一 ~ 周日）
 * - 超过 7 天：`yyyy-MM-dd HH:mm`（例 2026-10-06 22:45）
 *
 * 插入时间分隔条后，第一条信息算新消息独立成组，其所在组最底部一条需要带上气泡尾巴（仅私聊使用）；
 * 两条连续气泡之间垂直间距为 5dp，切换发送方两组之间间距为 10dp。
 */
object MessageTimeHelper {
    /** 时间分隔条出现阈值：相差 ≥ 5 分钟 */
    const val THRESHOLD_MINUTES = 5
    const val THRESHOLD_MILLIS = THRESHOLD_MINUTES * 60 * 1000L
    const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    /** 周几文案（Calendar.DAY_OF_WEEK: SUNDAY=1） */
    private val WEEK_LABELS = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

    /**
     * 获取系统时区偏移（毫秒）
     */
    fun getTimeZoneOffset(timeMs: Long = System.currentTimeMillis()): Long {
        return try {
            java.util.TimeZone.getDefault().getOffset(timeMs).toLong()
        } catch (_: Throwable) {
            8 * 3600 * 1000L // 兜底东八区
        }
    }

    /**
     * 相邻消息时间差是否达到分隔阈值（≥ 5 分钟）
     */
    fun isGapOverThreshold(t1: Long, t2: Long): Boolean {
        if (t1 <= 0L || t2 <= 0L) return false
        return abs(t2 - t1) >= THRESHOLD_MILLIS
    }

    /**
     * 判断当前消息与其上一条非系统消息之间是否触发时间分隔条（相差 ≥ 5 分钟）
     */
    fun isTimeSeparatorTriggered(prev: ChatMessage, current: ChatMessage): Boolean {
        if (current.isSystem || current.isVoiceCall) return false

        val tPrev = prev.timestampMs
        val tCurr = current.timestampMs

        if (tPrev > 0L && tCurr > 0L) {
            return isGapOverThreshold(tPrev, tCurr)
        }

        // 兼容 customTimeHeader 显式不同的情况
        if (!current.customTimeHeader.isNullOrEmpty() && current.customTimeHeader != prev.customTimeHeader) {
            return true
        }

        return false
    }

    /**
     * 判断在 index 处的消息前是否应该渲染时间分隔条
     */
    fun shouldShowTimeSeparator(messages: List<ChatMessage>, index: Int): Boolean {
        val current = messages.getOrNull(index) ?: return false
        if (current.isSystem || current.isVoiceCall) return false

        // 寻找当前消息之前的上一条有效非系统对话消息
        var prevMsg: ChatMessage? = null
        for (i in (index - 1) downTo 0) {
            val msg = messages[i]
            if (!msg.isSystem && !msg.isVoiceCall) {
                prevMsg = msg
                break
            }
        }

        // 若前序没有非系统消息，说明当前消息为会话首条消息，必须展示时间戳 Header
        if (prevMsg == null) return true

        return isTimeSeparatorTriggered(prevMsg, current)
    }

    /**
     * 获取时间分隔条显示的文字内容
     */
    fun getTimeHeaderText(message: ChatMessage): String {
        if (!message.customTimeHeader.isNullOrEmpty()) {
            return message.customTimeHeader
        }
        val t = message.timestampMs
        if (t <= 0L) {
            return message.timestamp
        }

        return try {
            val now = System.currentTimeMillis()
            val offset = getTimeZoneOffset(now)
            val todayDay = (now + offset) / MILLIS_PER_DAY
            val msgDay = (t + offset) / MILLIS_PER_DAY
            val dayDiff = todayDay - msgDay

            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(t))

            when {
                // 当天（含极端时区/时钟回拨导致 dayDiff 为负的情况）
                dayDiff <= 0L -> timeStr
                // 昨天
                dayDiff == 1L -> "昨天 $timeStr"
                // 超过 1 天、7 天以内
                dayDiff <= 7L -> "${weekdayLabel(t)} $timeStr"
                // 超过 7 天
                else -> java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(t))
            }
        } catch (_: Throwable) {
            message.timestamp
        }
    }

    /**
     * 取指定时间对应的「周X」文案
     */
    private fun weekdayLabel(timeMs: Long): String {
        return try {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = timeMs
            // Calendar.SUNDAY = 1 → 下标 0
            WEEK_LABELS.getOrElse(calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1) { "" }
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * 格式化简短当前时间 HH:mm
     */
    fun formatTimeShort(timeMs: Long = System.currentTimeMillis()): String {
        return try {
            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            timeFormat.format(java.util.Date(timeMs))
        } catch (_: Throwable) {
            "刚刚"
        }
    }

    /**
     * 是否同一发送者（气泡分组：头像、昵称、尾巴与间距都按分组计算）
     *
     * 群聊里 from_uid 可能缺失，senderId 会退化成占位值（"peer"），
     * 若只用 senderId 比较，不同人的消息会被误判为同一组，
     * 表现为「有些消息不显示发送者头像 / 昵称 / 性别」。
     */
    fun isSameSender(a: ChatMessage, b: ChatMessage): Boolean {
        if (a.isMine != b.isMine) return false
        val aId = a.senderId.takeIf { it.isNotBlank() && it != "peer" && it != "me" }
        val bId = b.senderId.takeIf { it.isNotBlank() && it != "peer" && it != "me" }
        if (aId != null && bId != null) return aId == bId
        // senderId 不可用时用昵称兜底
        return a.senderName == b.senderName
    }

    /**
     * 是否为「同一发送者连续消息」的首条
     *
     * 规则：
     * 1. 会话首条或触发时间分隔条时，作为新分组起点；
     * 2. 中间若出现系统消息或拍一拍，直接打断连续分组，后续首条重新展示头像和昵称；
     * 3. 紧邻前一条消息为不同发送者时，作为新分组起点。
     */
    fun isFirstOfSenderGroup(messages: List<ChatMessage>, index: Int): Boolean {
        val current = messages.getOrNull(index) ?: return false
        if (current.isSystem || current.isVoiceCall) return false
        if (index == 0) return true

        // 触发时间分隔条（跨阈值时间）视为新分组起点
        if (shouldShowTimeSeparator(messages, index)) return true

        // 紧邻的前一条消息
        val prev = messages.getOrNull(index - 1) ?: return true
        // 中间若有系统消息或拍一拍，直接打断连续分组，开启新组展示头像与昵称
        if (prev.isSystem || prev.isPat || prev.isVoiceCall) return true

        return !isSameSender(prev, current)
    }

    /**
     * 计算消息气泡下方的垂直间距：
     * - 同一分组内的连续气泡间距：紧凑 4dp；
     * - 换人发送或新分组：10dp；
     * - 触发时间分隔条时气泡与时间条保持间距：4dp；
     * - 系统消息周边留白：6dp。
     */
    fun computeMessageBottomSpacing(messages: List<ChatMessage>, currentIndex: Int): Dp {
        val current = messages.getOrNull(currentIndex) ?: return 0.dp
        val next = messages.getOrNull(currentIndex + 1) ?: return 0.dp

        if (current.isSystem || next.isSystem || current.isPat || next.isPat ||
            current.isVoiceCall || next.isVoiceCall
        ) {
            return 6.dp
        }

        if (isTimeSeparatorTriggered(current, next)) {
            return 4.dp
        }

        return if (isSameSender(current, next)) {
            4.dp  // 同一组两条连续气泡紧凑垂直间距：4dp
        } else {
            10.dp // 切换发送方两组之间间距：10dp
        }
    }

    /**
     * 依据 iMessage 原生规则计算当前消息气泡是否携带尾巴（尖角 / Tail）：
     * 1. 连续同发送方消息，仅该组最底部一条带尾巴；
     * 2. 插入时间分隔条（跨自然日、或同一天间隔 > 60 分钟）后，分隔条前的末条带尾巴，
     *    分隔条后的第一条信息算新消息独立成组（其所在组的底部也带尾巴）；
     * 3. 发送方切换时，上一组末条带尾巴。
     */
    fun computeBubbleHasTail(messages: List<ChatMessage>, index: Int): Boolean {
        val current = messages.getOrNull(index) ?: return false
        if (current.isSystem || current.isVoiceCall) return false

        // 核心规则：插入时间分隔后 第一条信息算新消息，需要带上气泡尾巴（尖角 / Tail）
        if (shouldShowTimeSeparator(messages, index)) {
            return true
        }

        // 寻找后续下一个有效非系统对话消息
        var nextMsg: ChatMessage? = null
        for (i in (index + 1) until messages.size) {
            val next = messages[i]
            if (!next.isSystem && !next.isVoiceCall) {
                nextMsg = next
                break
            }
        }

        // 若后续已无非系统消息，当前即为会话最底部消息 -> 必须带尾巴
        if (nextMsg == null) return true

        // 若后续下一条消息触发了时间分隔条（跨自然日或 > 60 分钟）：
        // 当前消息即为该时间分组的最后一条消息 -> 必须带尾巴！
        if (isTimeSeparatorTriggered(current, nextMsg)) {
            return true
        }

        // 若后续消息发送方不同（我方消息结束，对方发消息） -> 当前组结束，必须带尾巴！
        if (!isSameSender(current, nextMsg)) {
            return true
        }

        // 同发送方且在同一时间段内（未被时间分隔条切断），只有最底部一条带尾巴
        return false
    }
}
