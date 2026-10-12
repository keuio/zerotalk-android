package top.lanxint.zerotalk.ui.moments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.ui.utils.LinkTextHelper

/**
 * 动态富文本组件
 *
 * 核心功能：
 * 1. 动态 Markdown 语法解析与渲染（粗体、斜体、删除线、行内代码、超链接、原生 URL、引用、列表、标题）；
 * 2. 长文本折叠与展开：默认超过 [maxCollapsedLines]（默认 5 行）时截断并显示「全文」按钮，点击展开为完整内容并显示「收起」；
 * 3. 链接精准点击识别与跳转分发（支持自定义 [onUrlClick] 或系统 [LocalUriHandler]）；
 * 4. 依据 [key] 隔离卡片状态，防止 LazyColumn 滚动复用状态错乱。
 */
@Composable
fun MomentRichText(
    text: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    key: Any? = null,
    maxCollapsedLines: Int = 5,
    onUrlClick: ((String) -> Unit)? = null
) {
    if (text.isBlank()) return

    val higColors = AppleHigColors.colors(isDark)
    val uriHandler = LocalUriHandler.current

    val annotatedContent = remember(text, isDark) {
        LinkTextHelper.buildAnnotatedMarkdown(
            text = text,
            linkColor = higColors.link,
            codeBg = if (isDark) Color(0x33FFFFFF) else Color(0x14000000),
            codeColor = if (isDark) Color(0xFFFF9F0A) else Color(0xFFFF3B30),
            quoteColor = higColors.secondaryLabel,
            tag = "URL"
        )
    }

    val hasLinks = remember(annotatedContent) {
        annotatedContent.getStringAnnotations("URL", 0, annotatedContent.length).isNotEmpty()
    }

    val stateKey = key ?: text
    var isExpanded by remember(stateKey) { mutableStateOf(false) }
    var hasOverflow by remember(stateKey) { mutableStateOf(false) }
    var textLayoutResult by remember(stateKey) { mutableStateOf<TextLayoutResult?>(null) }

    Column(modifier = modifier) {
        val clickModifier = if (hasLinks) {
            Modifier.pointerInput(annotatedContent) {
                detectTapGestures { offset ->
                    val layout = textLayoutResult ?: return@detectTapGestures
                    val target = LinkTextHelper.findUrlAt(layout, annotatedContent, offset)
                    if (target != null) {
                        if (onUrlClick != null) {
                            onUrlClick(target)
                        } else {
                            runCatching { uriHandler.openUri(target) }
                        }
                    }
                }
            }
        } else Modifier

        BasicText(
            text = annotatedContent,
            style = AppleHigTypography.body.copy(
                color = higColors.label,
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            maxLines = if (isExpanded) Int.MAX_VALUE else maxCollapsedLines.coerceAtLeast(1),
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout ->
                textLayoutResult = layout
                if (!isExpanded) {
                    val lastLine = layout.lineCount - 1
                    val isEllipsized = lastLine >= 0 && layout.isLineEllipsized(lastLine)
                    val notAllRendered = lastLine >= 0 && layout.getLineEnd(lastLine) < annotatedContent.length
                    if (layout.hasVisualOverflow || layout.didOverflowHeight || isEllipsized || notAllRendered) {
                        hasOverflow = true
                    }
                }
            },
            modifier = clickModifier
        )

        if (hasOverflow) {
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = if (isExpanded) "收起" else "全文",
                style = AppleHigTypography.footnote.copy(
                    color = higColors.tint,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isExpanded = !isExpanded
                    }
                    .padding(vertical = 2.dp)
            )
        }
    }
}
