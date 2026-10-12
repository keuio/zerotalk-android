package top.lanxint.zerotalk.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration

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
}
