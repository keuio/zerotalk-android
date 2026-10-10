package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.normalizeUserTitle
import top.lanxint.zerotalk.data.model.normalizeUserTitleColor

/**
 * 用户称号标识（严格对齐官方 `UserTitleBadge` + `UserTitleBadge.css`）
 *
 * 官方组件行为：
 * - `title` 去首尾空白后为空 → **不渲染任何东西**（不占位）；
 * - 渲染文案 = 截断到 8 个字符的 `title`；
 * - `color` 大小写不敏感，白名单外回落 `blue`；
 * - 官方 CSS 只有一套色值，**亮 / 暗色下颜色完全相同**，因此 [isDark] 仅保留签名兼容，
 *   不参与取色（不要按主题改色）。
 *
 * 尺寸换算：官方用 `rem`（根字号 16px），本仓库既有约定是 CSS px → dp/sp 1:1：
 * - `max-width: 5.5rem` = 88dp；`min-height: 1.05rem` = 16.8dp；
 * - `padding: 0 .35rem` = 左右 5.6dp；`border-radius: 5px`、`border: 1px solid`；
 * - `font-size: .55rem` = 8.8sp；`line-height: 1.2` = 10.56sp；`letter-spacing: .01em`。
 *
 * 布局语义：`flex-shrink: 0`（不参与压缩）——调用方必须把**昵称**放在
 * `Modifier.weight(1f, fill = false)` + `maxLines = 1` + 省略号上，本组件自身不加 weight，
 * 这样昵称过长时被截断而徽章保持完整（不会像行尾时间戳那样被挤成竖排）。
 *
 * @param title 服务端下发的称号原文；为空 / 空白时不渲染
 * @param color 服务端下发的 `title_color`；白名单外回落 `blue`
 * @param isDark 仅保留签名兼容（官方亮 / 暗色同色，不参与取色）
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun UserTitleBadge(
    title: String?,
    color: String?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val text = normalizeUserTitle(title)
    // 官方：title 为空时整个 span 不渲染
    if (text.isEmpty()) return

    val palette = userTitlePalette(normalizeUserTitleColor(color))
    val shape = RoundedCornerShape(5.dp)
    // 官方 background / border-color 都取「surface 色 + 透明度」，gold 的 surface 与文字色不同
    val surface = palette.surface ?: palette.text

    Box(
        modifier = modifier
            .heightIn(min = 16.8.dp)
            .widthIn(max = 88.dp)
            .clip(shape)
            .background(surface.copy(alpha = palette.backgroundAlpha))
            .border(1.dp, surface.copy(alpha = palette.borderAlpha), shape)
            .padding(horizontal = 5.6.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = palette.text,
                fontSize = 8.8.sp,
                lineHeight = 10.56.sp,
                // letter-spacing: .01em ≈ 0.088sp
                letterSpacing = 0.09.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * 单个称号色板
 *
 * 官方 CSS 的 `background` / `border-color` 都是「文字色 + 透明度」，
 * 只有 `gold` 例外（文字 #b45309，背景 / 边框取 #f59e0b 的不同透明度），
 * 因此这里显式记录文字色与两个透明度，gold 单独给出覆盖色。
 */
private data class UserTitlePalette(
    val text: Color,
    val backgroundAlpha: Float,
    val borderAlpha: Float,
    /** 非 null 时背景 / 边框改用该色（官方 gold 用 #f59e0b） */
    val surface: Color? = null
)

/**
 * 颜色 key → 色板。
 *
 * 色值逐条对齐 `UserTitleBadge.css`（`#RRGGBBAA` 转 Compose 的 `0xAARRGGBB`）。
 * key 已由 [normalizeUserTitleColor] 归一化，这里仍对未知值回落 blue 以防被直接调用。
 */
private fun userTitlePalette(key: String): UserTitlePalette = when (key) {
    "blue" -> UserTitlePalette(Color(0xFF2563EB), 0x14 / 255f, 0x2E / 255f)
    "cyan" -> UserTitlePalette(Color(0xFF0891B2), 0x14 / 255f, 0x2E / 255f)
    "green" -> UserTitlePalette(Color(0xFF059669), 0x14 / 255f, 0x2E / 255f)
    "lime" -> UserTitlePalette(Color(0xFF65A30D), 0x1A / 255f, 0x33 / 255f)
    "orange" -> UserTitlePalette(Color(0xFFEA580C), 0x14 / 255f, 0x2E / 255f)
    "amber" -> UserTitlePalette(Color(0xFFD97706), 0x1A / 255f, 0x33 / 255f)
    "red" -> UserTitlePalette(Color(0xFFE11D48), 0x14 / 255f, 0x2E / 255f)
    "pink" -> UserTitlePalette(Color(0xFFDB2777), 0x14 / 255f, 0x2E / 255f)
    "purple" -> UserTitlePalette(Color(0xFF7C3AED), 0x14 / 255f, 0x2E / 255f)
    "violet" -> UserTitlePalette(Color(0xFF6D28D9), 0x14 / 255f, 0x2E / 255f)
    "gray" -> UserTitlePalette(Color(0xFF475569), 0x14 / 255f, 0x29 / 255f)
    // gold：文字 #b45309，背景 #f59e0b1f，边框 #f59e0b47
    "gold" -> UserTitlePalette(Color(0xFFB45309), 0x1F / 255f, 0x47 / 255f, surface = Color(0xFFF59E0B))
    else -> UserTitlePalette(Color(0xFF2563EB), 0x14 / 255f, 0x2E / 255f)
}
