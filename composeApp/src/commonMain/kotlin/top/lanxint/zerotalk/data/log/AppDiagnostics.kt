package top.lanxint.zerotalk.data.log

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 平台环境信息（App 版本 / 系统 / 机型）。
 *
 * 由 Android 侧在 [top.lanxint.zerotalk.ZeroTalkApplication.onCreate] 注入到
 * [AppDiagnostics.environment]；commonMain 不直接依赖 android.os.Build。
 */
data class DeviceEnvironment(
    val appVersionName: String = "未知",
    val appVersionCode: String = "未知",
    val androidVersion: String = "未知",
    val deviceModel: String = "未知"
)

/**
 * 「一键发送诊断信息到 App端群」的信息组装与**脱敏**。
 *
 * ## 脱敏规则（发送前必定执行，顺序即代码顺序）
 * 1. **消息正文 / 用户内容字段**：`content=…`、`content='…'`、`"content":"…"`，
 *    以及 `message=`、`text=`、`msg:` 等同类字段，值一律替换为 `<已省略>`，
 *    仅保留字段名与分隔符（JSON 形态输出 `"content":<已省略>`）。
 *    - 只匹配完整的字段名（`\bcontent\b`），因此 `contentType`、`context`、`textStyle` 不受影响。
 * 2. **32 位十六进制 id**（uid / room_id / 会话 id 等）：只保留前 8 位，其余用 `…` 替代。
 * 3. **带连字符的 UUID**（本地随机设备 id）：同样只保留前 8 位。
 * 4. 头部账号 uid 同样只保留前 8 位（[maskId]）。
 *
 * ## 行筛选规则
 * [selectDiagnosticLines] 优先保留含诊断关键字的行（`[WS]` / `[Wallpaper]` / `[Remark]` /
 * `Exception` / `error` / `ERROR` / `失败` / `错误` / `异常` / `超时` / `timeout` /
 * `断开` / `重连` 等）；关键字行不足 [DEFAULT_LOG_LINES] 行时，用日志尾部的普通行补齐，
 * 普通行同样已脱敏。超出上限时保留最靠近当前时刻（尾部）的行。
 *
 * ## 约束
 * 本对象**不得**调用 [ZtLog.d] / [ZtLog.w]，避免把诊断文本写回日志造成自我放大。
 */
object AppDiagnostics {

    /** 默认随诊断一起发送的日志行数（尾部） */
    const val DEFAULT_LOG_LINES: Int = 40

    /** 最终诊断文本的软上限，避免超出房间消息长度限制导致发送失败 */
    const val MAX_DIAGNOSTIC_CHARS: Int = 4000

    /** 十六进制 id 对外可见的前缀长度 */
    private const val VISIBLE_ID_PREFIX: Int = 8

    private const val ELLIPSIS = "…"
    private const val REDACTED = "<已省略>"

    /** 平台环境信息，由 Android 侧注入；未注入时各项显示「未知」 */
    @Volatile
    var environment: DeviceEnvironment? = null

    // ---- 脱敏用正则 ----

    /** 32 位十六进制 id：uid / room_id / 会话 id 等 */
    private val HEX32 = Regex("[0-9a-fA-F]{32}")

    /** 标准 UUID（含连字符），如本地随机生成的设备 id */
    private val UUID_RE = Regex(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    )

    /**
     * 消息正文 / 用户内容字段。
     *
     * 匹配 `content=值`、`content:'值'`、`"content":"值"`、`message: 值`、`text=值` 等；
     * 值支持双引号、单引号、裸值三种形态。字段名后允许一个可选的收尾引号，以覆盖 JSON 的 `"content":`。
     */
    private val CONTENT_FIELD = Regex(
        "(?i)\\b(content|message|text|msg)\"?\\s*([=:])\\s*(\"[^\"]*\"|'[^']*'|[^\\s,;)\\]}{]+)"
    )

    /** 诊断关键字：命中即视为高价值行，优先保留 */
    private val DIAGNOSTIC_KEYWORDS = listOf(
        "[WS]", "[Wallpaper]", "[Remark]",
        "Exception", "exception", "Throwable",
        "ERROR", "Error", "error",
        "失败", "错误", "异常",
        "timeout", "Timeout", "超时",
        "refused", "unreachable", "disconnect", "断开", "重连"
    )

    // ---- 对外 API ----

    /**
     * 脱敏单个 id：保留前 [VISIBLE_ID_PREFIX] 位，其余用 `…` 替代。
     * 空串 / `--`（未登录占位）返回「未知」。
     */
    fun maskId(id: String?): String {
        val value = id?.trim().orEmpty()
        if (value.isEmpty() || value == "--") return "未知"
        return if (value.length <= VISIBLE_ID_PREFIX) value else value.take(VISIBLE_ID_PREFIX) + ELLIPSIS
    }

    /**
     * 对单行日志脱敏：先抹掉消息正文字段，再截断 32 位 hex id 与 UUID。
     */
    fun sanitizeLine(line: String): String {
        var result = CONTENT_FIELD.replace(line) { match ->
            // 保留字段名与分隔符，值统一替换为占位符
            match.groupValues[1] + match.groupValues[2] + REDACTED
        }
        result = HEX32.replace(result) { it.value.take(VISIBLE_ID_PREFIX) + ELLIPSIS }
        result = UUID_RE.replace(result) { it.value.take(VISIBLE_ID_PREFIX) + ELLIPSIS }
        return result
    }

    /** 对多行文本逐行脱敏（换行符保留） */
    fun sanitizeText(text: String): String =
        text.split('\n').joinToString("\n") { sanitizeLine(it) }

    /** 是否为高诊断价值行 */
    fun isDiagnosticLine(line: String): Boolean =
        DIAGNOSTIC_KEYWORDS.any { line.contains(it) }

    /**
     * 从原始日志行中挑出要发送的部分：关键字行优先，不足 [maxLines] 行时用尾部普通行补齐，
     * 最终按原始顺序返回（先补齐的普通行，再关键字行）。所有返回行均已脱敏。
     */
    fun selectDiagnosticLines(lines: List<String>, maxLines: Int = DEFAULT_LOG_LINES): List<String> {
        if (maxLines <= 0) return emptyList()
        val cleaned = lines.map { sanitizeLine(it) }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return emptyList()

        val important = cleaned.filter { isDiagnosticLine(it) }.takeLast(maxLines)
        if (important.size >= maxLines) return important

        val filler = cleaned
            .filterNot { isDiagnosticLine(it) }
            .takeLast(maxLines - important.size)
        return filler + important
    }

    /** 当前时间文本（诊断头部用） */
    fun currentTimeText(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    /**
     * 组装完整诊断文本。
     *
     * @param uid 账号 id（可为 32 位 hex），仅前 8 位对外可见
     * @param nickname 账号昵称（用户本人的展示名，原样发送）
     * @param now 时间文本，默认取当前时间
     * @param rawLogLines 原始日志行，默认从 [ZtLog.readRecentLines] 取日志尾部
     */
    fun build(
        uid: String?,
        nickname: String?,
        now: String = currentTimeText(),
        rawLogLines: List<String> = ZtLog.readRecentLines(DEFAULT_LOG_LINES * 4)
    ): String {
        val env = environment
        val logLines = selectDiagnosticLines(rawLogLines, DEFAULT_LOG_LINES)
        val text = buildString {
            appendLine("【零语 App 诊断信息】")
            appendLine("时间：$now")
            appendLine("App 版本：${env?.appVersionName ?: "未知"}（${env?.appVersionCode ?: "未知"}）")
            appendLine("系统版本：${env?.androidVersion ?: "未知"}")
            appendLine("设备机型：${env?.deviceModel ?: "未知"}")
            appendLine("账号 uid：${maskId(uid)}")
            appendLine("账号昵称：${nickname?.trim().orEmpty().ifBlank { "未知" }}")
            appendLine()
            appendLine("【日志尾部（已脱敏，最多 $DEFAULT_LOG_LINES 行）】")
            if (logLines.isEmpty()) {
                appendLine("（无可用日志）")
            } else {
                logLines.forEach { appendLine(it) }
            }
        }.trimEnd()

        return if (text.length <= MAX_DIAGNOSTIC_CHARS) {
            text
        } else {
            text.take(MAX_DIAGNOSTIC_CHARS - "\n…（内容过长，已截断）".length) + "\n…（内容过长，已截断）"
        }
    }
}
