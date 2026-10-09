package top.lanxint.zerotalk.data.log

/**
 * 轻量调试日志
 *
 * - [d]：调试信息，仅 debug 包输出（由平台侧在启动时设置 [enabled]）；
 * - [w]：异常 / 告警，始终输出，避免 release 包里关键问题完全无痕；
 * - [fileSink]：平台侧注入的落盘实现。部分国产 ROM（实测 vivo/Android 12）会禁用 logcat，
 *   只能靠私有目录里的日志文件排查（debuggable 包可用 `adb shell run-as` 拉取）。
 * - [logReader]：平台侧注入的读取实现，供「一键发送诊断信息」取日志尾部；
 *   返回日志文件当前文本（实现可只读尾部若干 KB），无日志文件时返回 null。
 */
object ZtLog {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var fileSink: ((String) -> Unit)? = null

    /** 平台侧注入的日志读取实现，见 [readRecentLines] */
    @Volatile
    var logReader: (() -> String?)? = null

    fun d(tag: String, message: String) {
        if (!enabled) return
        emit("[$tag] $message")
    }

    fun w(tag: String, message: String) {
        emit("[$tag] $message")
    }

    private fun emit(line: String) {
        println(line)
        fileSink?.invoke(line)
    }

    /**
     * 读取最近 [maxLines] 行日志（不足则返回全部）。
     *
     * 仅做「取尾部 + 丢弃空行」，不做脱敏 —— 脱敏由 [AppDiagnostics] 负责，
     * 保证调用方拿到的始终是原始文本，便于按需二次处理。
     * 未注入 [logReader]（如单元测试 / 非 Android 环境）或读取失败时返回空列表。
     */
    fun readRecentLines(maxLines: Int): List<String> {
        if (maxLines <= 0) return emptyList()
        val raw = runCatching { logReader?.invoke() }.getOrNull()
        if (raw.isNullOrBlank()) return emptyList()
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        return if (lines.size <= maxLines) lines else lines.takeLast(maxLines)
    }
}
