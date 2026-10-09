package top.lanxint.zerotalk.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Apple Human Interface Guidelines (HIG) 标准文本层级体系 (Dynamic Type Typography Scale)
 *
 * 针对移动端屏幕视觉比例与中文字体光学尺寸全面对齐 iOS 原生质感：
 * 1. 禁用 Android 默认的 includeFontPadding，消弭传统平台字符上下不对称内边距；
 * 2. 挂载 LineHeightStyle(Alignment.Center)，文字行内光学垂直居中；
 * 3. 严格校准 Inset Grouped 分组规范的真实视觉字号（主标题 15sp、辅助 12sp），告别臃肿粗大；
 * 4. 指定系统级高质感无衬线体 (FontFamily.SansSerif)。
 */
object AppleHigTypography {
    val defaultFontFamily = FontFamily.SansSerif

    val defaultPlatformStyle = PlatformTextStyle(
        includeFontPadding = false
    )

    val defaultLineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )

    private fun higStyle(
        fontSize: TextUnit,
        lineHeight: TextUnit,
        fontWeight: FontWeight,
        letterSpacing: TextUnit = 0.sp
    ) = TextStyle(
        fontFamily = defaultFontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        platformStyle = defaultPlatformStyle,
        lineHeightStyle = defaultLineHeightStyle
    )

    // ============================================================
    // 1. 系统标题层级 (Display & Titles) (已缩放 90%)
    // ============================================================

    /** Large Title: 32pt / 38pt, Bold 700, 页面顶级大标题 */
    val largeTitle = higStyle(28.8.sp, 34.2.sp, FontWeight.Bold, 0.37.sp)

    /** Title 1: 26pt / 32pt, Regular 400, 模块一级标题、弹窗/卡片核心标题 */
    val title1 = higStyle(23.4.sp, 28.8.sp, FontWeight.Normal, 0.36.sp)

    /** Title 2: 21pt / 26pt, Regular 400, 分组标题、重点内容段落层级起始 */
    val title2 = higStyle(18.9.sp, 23.4.sp, FontWeight.Normal, 0.35.sp)

    /** Title 3: 18pt / 23pt, SemiBold 600, 子分组标题、列表分节 Header */
    val title3 = higStyle(16.2.sp, 20.7.sp, FontWeight.SemiBold, 0.38.sp)

    // ============================================================
    // 2. 内容与重点层级 (Content & Emphasis) (已缩放 90%)
    // ============================================================

    /** Headline: 16pt / 21pt, SemiBold 600, 信息主干重音、Sheet 顶部栏标题 */
    val headline = higStyle(14.4.sp, 18.9.sp, FontWeight.SemiBold, (-0.35).sp)

    /** Body: 16pt / 21pt, Regular 400, 表单内主要正文、列表主文本、主要段落 */
    val body = higStyle(14.4.sp, 18.9.sp, FontWeight.Normal, (-0.35).sp)

    /** Callout: 15pt / 20pt, Regular 400, 引文、呼出强调卡片、状态提示小条 */
    val callout = higStyle(13.5.sp, 18.sp, FontWeight.Normal, (-0.28).sp)

    /** Subhead: 14pt / 19pt, Regular 400, 次要说明文字、辅助描述、副标题 */
    val subhead = higStyle(12.6.sp, 17.1.sp, FontWeight.Normal, (-0.20).sp)

    // ============================================================
    // 3. 辅助与说明层级 (Secondary & Captions) (已缩放 90%)
    // ============================================================

    /** Footnote: 12pt / 16pt, Regular 400, 补充注释、时间戳、次级辅助说明、底部小字提示 */
    val footnote = higStyle(10.8.sp, 14.4.sp, FontWeight.Normal, (-0.08).sp)

    /** Caption 1: 11pt / 15pt, Regular 400, 标签、图标说明文字、紧凑型辅助文本 */
    val caption1 = higStyle(9.9.sp, 13.5.sp, FontWeight.Normal, 0.sp)

    /** Caption 2: 10pt / 13pt, Regular 400, 最小层级标识、角标/徽章 */
    val caption2 = higStyle(9.sp, 11.7.sp, FontWeight.Normal, 0.07.sp)

    // ============================================================
    // 4. Apple HIG Inset Grouped 专用高精排版规范 (对标 iOS 原生「设置」列表紧凑美感) (已缩放 90%)
    // ============================================================

    /** Inset Grouped 分组小节 Header: 12.5sp / 16sp, Regular 400 */
    val groupedSectionHeader = higStyle(13.5.sp, 16.sp, FontWeight.Normal, 0.sp)

    /** Inset Grouped 分组底部说明 Footer: 12sp / 16sp, Regular 400 */
    val groupedSectionFooter = higStyle(10.8.sp, 14.4.sp, FontWeight.Normal, 0.sp)

    /** Inset Grouped 列表项主标题: 14sp / 17sp, Regular 400, Tracking -0.24pt */
    val groupedRowTitle = higStyle(15.3.sp, 17.sp, FontWeight.Normal, (-0.24).sp)

    /** Inset Grouped 列表项右侧状态值: 14sp / 17sp, Regular 400, Tracking -0.24pt */
    val groupedRowValue = higStyle(15.3.sp, 17.sp, FontWeight.Normal, (-0.24).sp)

    /** Inset Grouped 列表项副标题: 11.5sp / 15sp, Regular 400, Tracking 0.00pt */
    val groupedRowSubtitle = higStyle(10.35.sp, 13.5.sp, FontWeight.Normal, 0.sp)
}
