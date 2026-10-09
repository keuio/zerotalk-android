package top.lanxint.zerotalk.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Apple Human Interface Guidelines (HIG) 全场景语义色彩系统 (Semantic System Colors)
 *
 * 体系包含四大核心维度：
 * 1. 系统基础彩色 (Adaptive Colors: systemBlue, systemRed, systemGreen...)
 * 2. 文本与前景色 (Semantic Labels: label, secondaryLabel, tertiaryLabel, quaternaryLabel, link, placeholder, tint, destructive)
 * 3. 背景语义色 (Backgrounds: systemBackground, secondarySystemBackground, systemGroupedBackground...)
 * 4. 填充与分割线 (Fills & Separators: systemFill, separator, opaqueSeparator...)
 */
@Immutable
data class AppleHigColorTokens(
    // ---- 1. 系统自适应彩色 (System Colors) ----
    val systemBlue: Color,
    val systemRed: Color,
    val systemGreen: Color,
    val systemOrange: Color,
    val systemYellow: Color,
    val systemMint: Color,
    val systemTeal: Color,
    val systemIndigo: Color,
    val systemPurple: Color,
    val systemPink: Color,
    val systemBrown: Color,

    // ---- 2. 文本前景色 (Semantic Labels) ----
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val quaternaryLabel: Color,
    val link: Color,
    val placeholderText: Color,
    val tint: Color,
    val destructive: Color,

    // ---- 3. 背景语义色 (Backgrounds) ----
    val systemBackground: Color,
    val secondarySystemBackground: Color,
    val tertiarySystemBackground: Color,
    val systemGroupedBackground: Color,
    val secondarySystemGroupedBackground: Color,
    val tertiarySystemGroupedBackground: Color,

    // ---- 4. 填充与分割线 (Fills & Separators) ----
    val systemFill: Color,
    val secondarySystemFill: Color,
    val tertiarySystemFill: Color,
    val quaternarySystemFill: Color,
    val separator: Color,
    val opaqueSeparator: Color
)

object AppleHigColors {
    /**
     * 明亮模式 (Light Mode) 全场景 HIG 标准色值
     */
    val Light = AppleHigColorTokens(
        // 系统自适应彩色
        systemBlue = Color(0xFF007AFF),
        systemRed = Color(0xFFFF3B30),
        systemGreen = Color(0xFF34C759),
        systemOrange = Color(0xFFFF9500),
        systemYellow = Color(0xFFFFCC00),
        systemMint = Color(0xFF00C7BE),
        systemTeal = Color(0xFF5AC8FA),
        systemIndigo = Color(0xFF5856D6),
        systemPurple = Color(0xFFAF52DE),
        systemPink = Color(0xFFFF2D55),
        systemBrown = Color(0xFFA2845E),

        // 文本前景色 (Labels)
        label = Color(0xFF000000),                              // rgba(0, 0, 0, 1.0)
        secondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.60f), // rgba(60, 60, 67, 0.60)
        tertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.30f),  // rgba(60, 60, 67, 0.30)
        quaternaryLabel = Color(0xFF3C3C43).copy(alpha = 0.18f),// rgba(60, 60, 67, 0.18)
        link = Color(0xFF007AFF),
        placeholderText = Color(0xFF3C3C43).copy(alpha = 0.30f),
        tint = Color(0xFF007AFF),
        destructive = Color(0xFFFF3B30),

        // 背景语义色 (Backgrounds)
        systemBackground = Color(0xFFFFFFFF),
        secondarySystemBackground = Color(0xFFF2F2F7),
        tertiarySystemBackground = Color(0xFFFFFFFF),
        systemGroupedBackground = Color(0xFFF2F2F7),
        secondarySystemGroupedBackground = Color(0xFFFFFFFF),
        tertiarySystemGroupedBackground = Color(0xFFF2F2F7),

        // 填充 & 分割线 (Fills & Separators)
        systemFill = Color(0xFF747480).copy(alpha = 0.20f),          // rgba(116, 116, 128, 0.20)
        secondarySystemFill = Color(0xFF747480).copy(alpha = 0.16f), // rgba(116, 116, 128, 0.16)
        tertiarySystemFill = Color(0xFF747480).copy(alpha = 0.12f),  // rgba(116, 116, 128, 0.12)
        quaternarySystemFill = Color(0xFF747480).copy(alpha = 0.08f),// rgba(116, 116, 128, 0.08)
        separator = Color(0xFF3C3C43).copy(alpha = 0.29f),           // rgba(60, 60, 67, 0.29)
        opaqueSeparator = Color(0xFFC6C6C8)
    )

    /**
     * 暗黑模式 (Dark Mode) 全场景 HIG 标准色值
     */
    val Dark = AppleHigColorTokens(
        // 系统自适应彩色
        systemBlue = Color(0xFF0A84FF),
        systemRed = Color(0xFFFF453A),
        systemGreen = Color(0xFF30D158),
        systemOrange = Color(0xFFFF9F0A),
        systemYellow = Color(0xFFFFD60A),
        systemMint = Color(0xFF63E6E2),
        systemTeal = Color(0xFF64D2FF),
        systemIndigo = Color(0xFF5E5CE6),
        systemPurple = Color(0xFFBF5AF2),
        systemPink = Color(0xFFFF375F),
        systemBrown = Color(0xFFAC8E68),

        // 文本前景色 (Labels)
        label = Color(0xFFFFFFFF),                              // rgba(255, 255, 255, 1.0)
        secondaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.60f), // rgba(235, 235, 245, 0.60)
        tertiaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.30f),  // rgba(235, 235, 245, 0.30)
        quaternaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.18f),// rgba(235, 235, 245, 0.18)
        link = Color(0xFF0A84FF),
        placeholderText = Color(0xFFEBEBF5).copy(alpha = 0.30f),
        tint = Color(0xFF0A84FF),
        destructive = Color(0xFFFF453A),

        // 背景语义色 (Backgrounds)
        systemBackground = Color(0xFF000000),
        secondarySystemBackground = Color(0xFF1C1C1E),
        tertiarySystemBackground = Color(0xFF2C2C2E),
        systemGroupedBackground = Color(0xFF000000),
        secondarySystemGroupedBackground = Color(0xFF1C1C1E),
        tertiarySystemGroupedBackground = Color(0xFF2C2C2E),

        // 填充 & 分割线 (Fills & Separators)
        systemFill = Color(0xFF747480).copy(alpha = 0.24f),          // rgba(116, 116, 128, 0.24)
        secondarySystemFill = Color(0xFF747480).copy(alpha = 0.18f), // rgba(116, 116, 128, 0.18)
        tertiarySystemFill = Color(0xFF747480).copy(alpha = 0.14f),  // rgba(116, 116, 128, 0.14)
        quaternarySystemFill = Color(0xFF747480).copy(alpha = 0.10f),// rgba(116, 116, 128, 0.10)
        separator = Color(0xFF545458).copy(alpha = 0.60f),           // rgba(84, 84, 88, 0.60)
        opaqueSeparator = Color(0xFF48484A)
    )

    /**
     * 根据是否为暗黑模式返回对应的色彩 Token 集
     */
    fun colors(isDark: Boolean): AppleHigColorTokens = if (isDark) Dark else Light
}

/** 兼容旧代码引用别名 */
typealias AppleHigTextColors = AppleHigColors
typealias AppleHigLabelTokens = AppleHigColorTokens
