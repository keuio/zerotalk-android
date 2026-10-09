package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.data.model.MbtiDimension
import top.lanxint.zerotalk.data.model.MbtiInfo
import top.lanxint.zerotalk.ui.theme.AppleHigColorTokens
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * MBTI 完整信息卡片
 *
 * 视觉沿用 Apple HIG：外层复用既有 [AppleHigFillCard]（系统填充色 + 16dp 圆角），
 * 内部分四块：
 * 1. 头部：类型代码（角色色）+ 中文名 + 角色标签胶囊（如「外交家」）；
 * 2. 关键词胶囊（服务端 keywords）；
 * 3. 类型摘要（服务端 summary，最多 3 行）；
 * 4. 四维百分比条（服务端 percents，E/I、S/N、T/F、J/P，主题色表示首字母占比）。
 *
 * 调用方需自行保证 [mbti] 非空：mbti 为 null（未填写 MBTI）时不渲染本卡片。
 *
 * @param mbti 已解析的 MBTI 信息
 * @param isDark 是否暗色模式
 * @param showSummary 是否展示摘要（默认展示）
 */
@Composable
fun MbtiCard(
    mbti: MbtiInfo,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    showSummary: Boolean = true
) {
    val hig = AppleHigColors.colors(isDark)
    val accent = mbtiAccentColor(mbti, hig)
    val dimensions = mbti.dimensions

    AppleHigFillCard(
        modifier = modifier,
        isDark = isDark,
        itemSpacing = 10.dp
    ) {
        // ---- 1. 头部：类型代码 + 中文名 + 角色标签 ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    text = mbti.type,
                    style = AppleHigTypography.title2.copy(
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (mbti.name.isNotBlank() && mbti.name != mbti.type) {
                    BasicText(
                        text = mbti.name,
                        style = AppleHigTypography.subhead.copy(color = hig.secondaryLabel)
                    )
                }
            }

            if (mbti.roleLabel.isNotBlank()) {
                MbtiCapsule(text = mbti.roleLabel, color = accent, isDark = isDark)
            }
        }

        // ---- 2. 关键词胶囊 ----
        if (mbti.keywords.isNotEmpty()) {
            MbtiKeywordRows(keywords = mbti.keywords, color = accent, isDark = isDark)
        }

        // ---- 3. 类型摘要 ----
        if (showSummary && mbti.summary.isNotBlank()) {
            BasicText(
                text = mbti.summary,
                style = AppleHigTypography.footnote.copy(color = hig.secondaryLabel),
                maxLines = 3
            )
        }

        // ---- 4. 四维百分比条 ----
        if (dimensions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                dimensions.forEach { dimension ->
                    MbtiDimensionBar(dimension = dimension, accent = accent, hig = hig)
                }
            }
        }
    }
}

/**
 * 关键词胶囊：每行最多 3 个，避免引入实验性 FlowRow。
 */
@Composable
private fun MbtiKeywordRows(
    keywords: List<String>,
    color: Color,
    isDark: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keywords.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { keyword ->
                    MbtiCapsule(text = keyword, color = color, isDark = isDark)
                }
            }
        }
    }
}

/**
 * 角色 / 关键词胶囊（角色色 12%~22% 透明度底色）
 */
@Composable
private fun MbtiCapsule(
    text: String,
    color: Color,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = if (isDark) 0.22f else 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        BasicText(
            text = text,
            style = AppleHigTypography.caption1.copy(
                color = color,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * 单维百分比条：左侧字母占比用主题色填充，右侧字母占比为系统填充色底。
 */
@Composable
private fun MbtiDimensionBar(
    dimension: MbtiDimension,
    accent: Color,
    hig: AppleHigColorTokens
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = dimension.first,
            style = AppleHigTypography.caption1.copy(
                color = accent,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.width(11.dp)
        )
        BasicText(
            text = "${dimension.firstPercent}%",
            style = AppleHigTypography.caption2.copy(
                color = hig.secondaryLabel,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.width(30.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(hig.tertiarySystemFill)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(dimension.firstFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
        }
        BasicText(
            text = "${dimension.secondPercent}%",
            style = AppleHigTypography.caption2.copy(color = hig.secondaryLabel),
            modifier = Modifier.width(30.dp)
        )
        BasicText(
            text = dimension.second,
            style = AppleHigTypography.caption1.copy(
                color = hig.secondaryLabel,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.width(11.dp)
        )
    }
}

/**
 * 角色主题色：优先服务端 `role_color`，缺失时按 `role` 回落，最后用系统 tint。
 *
 * 16 型标准配色：analyst 紫 / diplomat 绿 / sentinel 蓝 / explorer 黄。
 */
fun mbtiAccentColor(mbti: MbtiInfo, colors: AppleHigColorTokens): Color =
    when (mbti.roleColor.lowercase()) {
        "green" -> colors.systemGreen
        "purple", "violet" -> colors.systemPurple
        "blue" -> colors.systemBlue
        "yellow", "gold" -> colors.systemYellow
        "red" -> colors.systemRed
        "orange" -> colors.systemOrange
        "teal" -> colors.systemTeal
        "pink" -> colors.systemPink
        "indigo" -> colors.systemIndigo
        else -> when (mbti.role.lowercase()) {
            "analyst" -> colors.systemPurple
            "diplomat" -> colors.systemGreen
            "sentinel" -> colors.systemBlue
            "explorer" -> colors.systemYellow
            else -> colors.tint
        }
    }
