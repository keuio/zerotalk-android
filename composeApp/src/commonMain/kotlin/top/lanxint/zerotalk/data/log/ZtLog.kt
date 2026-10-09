package top.lanxint.zerotalk.data.log

/**
 * 轻量调试日志
 *
 * - [d]：调试信息，仅 debug 包输出（由平台侧在启动时设置 [enabled]）；
 * - [w]：异常 / 告警，始终输出，避免 release 包里关键问题完全无痕；
 * - [fileSink]：平台侧注入的落盘实现。部分国产 ROM（实测 vivo/Android 12）会禁用 logcat，
 *   只能靠私有目录里的日志文件排查（debuggable 包可用 `adb shell run-as` 拉取）。
 */
object ZtLog {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var fileSink: ((String) -> Unit)? = null

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
}
