package top.lanxint.zerotalk.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * 文本超链接识别与富文本样式构建工具
 */
object LinkTextHelper {
    // 匹配 http://、https:// 与 www. 开头的链接，排斥中文、全角字符及空白符。
    // 注意：不能在 https?:// 前使用 \b，因为 Android ICU 正则引擎将中文汉字视为 \w（词字符），
    // 会导致「下载链接附内https://...」在汉字与 h 之间没有词边界 \b 而匹配失败。
    val URL_REGEX = Regex(
        """(?i)(?:https?://|(?<![a-zA-Z0-9_/@.])www\.)[^\s\u4e00-\u9fff\u3400-\u4dbf\u3000-\u303f\uff00-\uffef\u2000-\u206f<>"{}|\\^`]+"""
    )

    /**
     * 判断文本中是否包含 URL
     */
    fun hasLinks(text: String): Boolean = URL_REGEX.containsMatchIn(text)

    /**
     * 剥离句子末尾可能误吞的英文字符与标点（如 . , ! ? ; : ) ] } 等及未配对括号）
     */
    fun cleanUrl(raw: String): String {
        var trimmed = raw
        while (trimmed.isNotEmpty()) {
            val last = trimmed.last()
            if (last in ".,!?:;\"'”’`~，。？！；：“”‘’") {
                trimmed = trimmed.dropLast(1)
            } else if (last == ')' && '(' !in trimmed) {
                trimmed = trimmed.dropLast(1)
            } else if (last == '）' && '（' !in trimmed) {
                trimmed = trimmed.dropLast(1)
            } else if (last == ']' && '[' !in trimmed) {
                trimmed = trimmed.dropLast(1)
            } else if (last == '}' && '{' !in trimmed) {
                trimmed = trimmed.dropLast(1)
            } else if (last == '>' && '<' !in trimmed) {
                trimmed = trimmed.dropLast(1)
            } else if (last == '》' && '《' !in trimmed) {
                trimmed = trimmed.dropLast(1)
            } else {
                break
            }
        }
        return trimmed
    }

    /**
     * 为 www. 补齐 https:// 协议头，确保浏览器能正确解析跳转
     */
    fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    /**
     * 构造带 URL 下划线与范围注解的 AnnotatedString（默认不改变文字颜色，仅添加下划线）
     */
    fun buildAnnotatedTextWithLinks(
        text: String,
        linkColor: Color = Color.Unspecified,
        tag: String = "URL"
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val builder = AnnotatedString.Builder()
        var lastIndex = 0
        URL_REGEX.findAll(text).forEach { matchResult ->
            val start = matchResult.range.first
            val rawMatch = matchResult.value
            val cleaned = cleanUrl(rawMatch)
            if (cleaned.isNotEmpty()) {
                val end = start + cleaned.length
                if (start > lastIndex) {
                    builder.append(text.substring(lastIndex, start))
                }
                builder.pushStringAnnotation(tag = tag, annotation = cleaned)
                builder.pushStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                )
                builder.append(cleaned)
                builder.pop()
                builder.pop()
                lastIndex = end
            }
        }
        if (lastIndex < text.length) {
            builder.append(text.substring(lastIndex))
        }
        return builder.toAnnotatedString()
    }

    /**
     * 根据点击坐标（相对于 BasicText 自身坐标系）在 TextLayoutResult 中精准命中 URL 注解
     */
    fun findUrlAt(
        layout: TextLayoutResult,
        annotatedText: AnnotatedString,
        offset: Offset,
        tag: String = "URL"
    ): String? {
        if (annotatedText.isEmpty()) return null
        val width = layout.size.width.toFloat()
        val height = layout.size.height.toFloat()
        if (offset.x < -8f || offset.x > width + 8f || offset.y < -8f || offset.y > height + 8f) {
            return null
        }
        val clampedY = offset.y.coerceIn(0f, (height - 1f).coerceAtLeast(0f))
        val line = layout.getLineForVerticalPosition(clampedY)
        val lineTop = layout.getLineTop(line)
        val lineBottom = layout.getLineBottom(line)
        if (offset.y < lineTop - 6f || offset.y > lineBottom + 6f) {
            return null
        }
        val lineLeft = layout.getLineLeft(line)
        val lineRight = layout.getLineRight(line)
        if (offset.x < lineLeft - 8f || offset.x > lineRight + 8f) {
            return null
        }

        val lineStart = layout.getLineStart(line)
        val lineEndExclusive = layout.getLineEnd(line, visibleEnd = true)
            .coerceAtMost(annotatedText.length)
        if (lineStart >= lineEndExclusive) return null

        val clampedX = offset.x.coerceIn(lineLeft, lineRight)
        val rawOffset = layout.getOffsetForPosition(Offset(clampedX, clampedY))

        // getOffsetForPosition 返回光标边界 (0..length)，点击字符右半侧会返回 charIndex + 1，
        // 因此同时检查 rawOffset 与 rawOffset - 1，并按水平距离优先选择命中的字符。
        val candidates = listOf(rawOffset, rawOffset - 1)
            .distinct()
            .filter { it in lineStart until lineEndExclusive }
            .sortedBy { idx ->
                val box = layout.getBoundingBox(idx)
                when {
                    offset.x < box.left -> box.left - offset.x
                    offset.x > box.right -> offset.x - box.right
                    else -> 0f
                }
            }

        for (idx in candidates) {
            val box = layout.getBoundingBox(idx)
            if (offset.x in (box.left - 10f)..(box.right + 10f)) {
                val hit = annotatedText.getStringAnnotations(tag, idx, idx + 1).firstOrNull()
                if (hit != null) {
                    return normalizeUrl(hit.item)
                }
            }
        }
        return null
    }

    // ============================================================
    // Markdown 语法解析与 AnnotatedString 渲染构建器
    // ============================================================

    private val CODE_REGEX = Regex("""`([^`\r\n]+)`""")
    private val MD_LINK_REGEX = Regex("""\[([^\]\r\n]+)\]\(\s*([^\s\)\r\n]+)\s*\)""")
    private val BOLD_ITALIC_REGEX = Regex("""(?:\*\*\*(?!\s)([^\r\n]+?)(?<!\s)\*\*\*|___(?!\s)([^\r\n]+?)(?<!\s)___)""")
    private val BOLD_REGEX = Regex("""(?:\*\*(?!\s)([^\r\n]+?)(?<!\s)\*\*|__(?!\s)([^\r\n]+?)(?<!\s)__)""")
    private val STRIKE_REGEX = Regex("""~~(?!\s)([^\r\n]+?)(?<!\s)~~""")
    private val ITALIC_REGEX = Regex("""(?:(?<!\*)\*(?!\s|\*)([^\*\r\n]+?)(?<!\s|\*)\*(?!\*)|(?<!_)_(?!\s|_)([^_\r\n]+?)(?<!\s|_)_(?!_))""")

    private val HEADER_REGEX = Regex("""^(#{1,6})\s+(.+)$""")
    private val QUOTE_REGEX = Regex("""^>\s*(.*)$""")
    private val UNORDERED_LIST_REGEX = Regex("""^(\s*)[-*]\s+(.+)$""")
    private val ORDERED_LIST_REGEX = Regex("""^(\s*)(\d+)\.\s+(.+)$""")

    private enum class InlineTokenType {
        INLINE_CODE,
        MARKDOWN_LINK,
        RAW_URL,
        BOLD_ITALIC,
        BOLD,
        STRIKE,
        ITALIC
    }

    private data class InlineMatch(
        val range: IntRange,
        val type: InlineTokenType,
        val text: String,
        val extra: String? = null
    )

    /**
     * 构建支持 Markdown 基础语法的 AnnotatedString
     *
     * 支持的语法：
     * - 粗体：`**文字**`、`__文字__`
     * - 斜体：`*文字*`、`_文字_`
     * - 粗斜体：`***文字***`、`___文字___`
     * - 删除线：`~~文字~~`
     * - 行内代码：`` `代码` ``
     * - Markdown 超链接：`[显示文本](url)`
     * - 原生 URL 自动识别
     * - 引用排版：`> 引用`
     * - 列表排版：`- 项目` / `* 项目` / `1. 项目`
     * - 标题排版：`# 一级标题` ~ `### 三级标题`
     */
    fun buildAnnotatedMarkdown(
        text: String,
        linkColor: Color = Color.Unspecified,
        codeBg: Color = Color.Unspecified,
        codeColor: Color = Color.Unspecified,
        quoteColor: Color = Color.Unspecified,
        tag: String = "URL"
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val effectiveLinkColor = if (linkColor != Color.Unspecified) linkColor else Color(0xFF007AFF)
        val effectiveCodeBg = if (codeBg != Color.Unspecified) codeBg else Color(0x1F8E8E93)
        val effectiveQuoteColor = if (quoteColor != Color.Unspecified) quoteColor else Color(0xFF8E8E93)

        val builder = AnnotatedString.Builder()
        val lines = text.split("\r\n", "\n")

        for (i in lines.indices) {
            val line = lines[i]
            renderMarkdownLine(
                builder = builder,
                line = line,
                linkColor = effectiveLinkColor,
                codeBg = effectiveCodeBg,
                codeColor = codeColor,
                quoteColor = effectiveQuoteColor,
                tag = tag
            )
            if (i < lines.lastIndex) {
                builder.append("\n")
            }
        }

        return builder.toAnnotatedString()
    }

    private fun renderMarkdownLine(
        builder: AnnotatedString.Builder,
        line: String,
        linkColor: Color,
        codeBg: Color,
        codeColor: Color,
        quoteColor: Color,
        tag: String
    ) {
        val trimmed = line.trim()
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            builder.pushStyle(SpanStyle(color = quoteColor))
            builder.append("───────────────")
            builder.pop()
            return
        }

        val headerMatch = HEADER_REGEX.matchEntire(line)
        if (headerMatch != null) {
            val level = headerMatch.groupValues[1].length
            val content = headerMatch.groupValues[2]
            val fontSize = when (level) {
                1 -> 18.sp
                2 -> 16.5.sp
                3 -> 15.5.sp
                else -> 15.sp
            }
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize))
            appendInlineMarkdown(builder, content, linkColor, codeBg, codeColor, quoteColor, tag)
            builder.pop()
            return
        }

        val quoteMatch = QUOTE_REGEX.matchEntire(line)
        if (quoteMatch != null) {
            val content = quoteMatch.groupValues[1]
            builder.pushStyle(SpanStyle(color = quoteColor, fontStyle = FontStyle.Italic))
            builder.append("▎ ")
            if (content.isNotEmpty()) {
                appendInlineMarkdown(builder, content, linkColor, codeBg, codeColor, quoteColor, tag)
            }
            builder.pop()
            return
        }

        val uListMatch = UNORDERED_LIST_REGEX.matchEntire(line)
        if (uListMatch != null) {
            val indent = uListMatch.groupValues[1]
            val content = uListMatch.groupValues[2]
            builder.append("$indent• ")
            appendInlineMarkdown(builder, content, linkColor, codeBg, codeColor, quoteColor, tag)
            return
        }

        val oListMatch = ORDERED_LIST_REGEX.matchEntire(line)
        if (oListMatch != null) {
            val indent = oListMatch.groupValues[1]
            val number = oListMatch.groupValues[2]
            val content = oListMatch.groupValues[3]
            builder.append("$indent$number. ")
            appendInlineMarkdown(builder, content, linkColor, codeBg, codeColor, quoteColor, tag)
            return
        }

        appendInlineMarkdown(builder, line, linkColor, codeBg, codeColor, quoteColor, tag)
    }

    private fun findNextInlineMatch(text: String, startIndex: Int, allowLinks: Boolean): InlineMatch? {
        if (startIndex >= text.length) return null

        var bestMatch: InlineMatch? = null

        fun consider(match: InlineMatch?) {
            if (match == null || match.range.first < startIndex) return
            if (bestMatch == null) {
                bestMatch = match
            } else {
                val current = bestMatch!!
                if (match.range.first < current.range.first) {
                    bestMatch = match
                } else if (match.range.first == current.range.first) {
                    if (match.range.last > current.range.last) {
                        bestMatch = match
                    } else if (match.range.last == current.range.last && match.type.ordinal < current.type.ordinal) {
                        bestMatch = match
                    }
                }
            }
        }

        CODE_REGEX.find(text, startIndex)?.let { m ->
            consider(InlineMatch(m.range, InlineTokenType.INLINE_CODE, m.groupValues[1]))
        }

        if (allowLinks) {
            MD_LINK_REGEX.find(text, startIndex)?.let { m ->
                val display = m.groupValues[1]
                val url = m.groupValues[2].removeSurrounding("<", ">")
                consider(InlineMatch(m.range, InlineTokenType.MARKDOWN_LINK, display, url))
            }

            URL_REGEX.find(text, startIndex)?.let { m ->
                val cleaned = cleanUrl(m.value)
                if (cleaned.isNotEmpty()) {
                    val start = m.range.first
                    val end = start + cleaned.length - 1
                    consider(InlineMatch(start..end, InlineTokenType.RAW_URL, cleaned))
                }
            }
        }

        BOLD_ITALIC_REGEX.find(text, startIndex)?.let { m ->
            val content = m.groupValues[1].ifEmpty { m.groupValues[2] }
            consider(InlineMatch(m.range, InlineTokenType.BOLD_ITALIC, content))
        }

        BOLD_REGEX.find(text, startIndex)?.let { m ->
            val content = m.groupValues[1].ifEmpty { m.groupValues[2] }
            consider(InlineMatch(m.range, InlineTokenType.BOLD, content))
        }

        STRIKE_REGEX.find(text, startIndex)?.let { m ->
            consider(InlineMatch(m.range, InlineTokenType.STRIKE, m.groupValues[1]))
        }

        ITALIC_REGEX.find(text, startIndex)?.let { m ->
            val content = m.groupValues[1].ifEmpty { m.groupValues[2] }
            consider(InlineMatch(m.range, InlineTokenType.ITALIC, content))
        }

        return bestMatch
    }

    private fun appendInlineMarkdown(
        builder: AnnotatedString.Builder,
        text: String,
        linkColor: Color,
        codeBg: Color,
        codeColor: Color,
        quoteColor: Color,
        tag: String,
        allowLinks: Boolean = true
    ) {
        if (text.isEmpty()) return

        var currentIndex = 0
        while (currentIndex < text.length) {
            val nextMatch = findNextInlineMatch(text, currentIndex, allowLinks)
            if (nextMatch == null) {
                builder.append(text.substring(currentIndex))
                break
            }

            if (nextMatch.range.first > currentIndex) {
                builder.append(text.substring(currentIndex, nextMatch.range.first))
            }

            when (nextMatch.type) {
                InlineTokenType.INLINE_CODE -> {
                    builder.pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBg,
                            color = if (codeColor != Color.Unspecified) codeColor else Color.Unspecified
                        )
                    )
                    builder.append(nextMatch.text)
                    builder.pop()
                }

                InlineTokenType.MARKDOWN_LINK -> {
                    val normalizedUrl = normalizeUrl(nextMatch.extra.orEmpty())
                    builder.pushStringAnnotation(tag = tag, annotation = normalizedUrl)
                    builder.pushStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                    appendInlineMarkdown(
                        builder = builder,
                        text = nextMatch.text,
                        linkColor = linkColor,
                        codeBg = codeBg,
                        codeColor = codeColor,
                        quoteColor = quoteColor,
                        tag = tag,
                        allowLinks = false
                    )
                    builder.pop()
                    builder.pop()
                }

                InlineTokenType.RAW_URL -> {
                    val normalizedUrl = normalizeUrl(nextMatch.text)
                    builder.pushStringAnnotation(tag = tag, annotation = normalizedUrl)
                    builder.pushStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                    builder.append(nextMatch.text)
                    builder.pop()
                    builder.pop()
                }

                InlineTokenType.BOLD_ITALIC -> {
                    builder.pushStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )
                    )
                    appendInlineMarkdown(
                        builder = builder,
                        text = nextMatch.text,
                        linkColor = linkColor,
                        codeBg = codeBg,
                        codeColor = codeColor,
                        quoteColor = quoteColor,
                        tag = tag,
                        allowLinks = allowLinks
                    )
                    builder.pop()
                }

                InlineTokenType.BOLD -> {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    appendInlineMarkdown(
                        builder = builder,
                        text = nextMatch.text,
                        linkColor = linkColor,
                        codeBg = codeBg,
                        codeColor = codeColor,
                        quoteColor = quoteColor,
                        tag = tag,
                        allowLinks = allowLinks
                    )
                    builder.pop()
                }

                InlineTokenType.STRIKE -> {
                    builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    appendInlineMarkdown(
                        builder = builder,
                        text = nextMatch.text,
                        linkColor = linkColor,
                        codeBg = codeBg,
                        codeColor = codeColor,
                        quoteColor = quoteColor,
                        tag = tag,
                        allowLinks = allowLinks
                    )
                    builder.pop()
                }

                InlineTokenType.ITALIC -> {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    appendInlineMarkdown(
                        builder = builder,
                        text = nextMatch.text,
                        linkColor = linkColor,
                        codeBg = codeBg,
                        codeColor = codeColor,
                        quoteColor = quoteColor,
                        tag = tag,
                        allowLinks = allowLinks
                    )
                    builder.pop()
                }
            }

            currentIndex = nextMatch.range.last + 1
        }
    }
}
