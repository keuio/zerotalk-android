package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * Apple HIG Inset Grouped 辅助端点（Accessory）类型
 */
sealed interface AppleHigRowAccessory {
    /** 标准导航箭头 (>) */
    data object Navigation : AppleHigRowAccessory

    /** 选中对勾 (✓) */
    data object Checkmark : AppleHigRowAccessory

    /** 无后缀 */
    data object None : AppleHigRowAccessory

    /** 自定义 Composable 组件（如开关、输入框、分段控制器等） */
    data class Custom(val content: @Composable () -> Unit) : AppleHigRowAccessory
}

/**
 * Apple HIG Inset Grouped 分组容器 (Section)
 *
 * 遵循 iOS 13+ / iOS 26 系统规范与卡片内嵌标题风格：
 * - 嵌入式 Header (headerInsideCard = true，默认)：Subhead 13.5sp 灰色说明小字，位于卡片内上端，内距 16dp / 14dp；
 * - 外置式 Header (headerInsideCard = false)：Footnote 灰色小字，位于卡片外部上方；
 * - 中部 Card：12dp 圆角，次级系统分组背景；
 * - 底部 Footer：可选的 Footnote 说明文本 (top 6dp, bottom 12dp)。
 */
@Composable
fun AppleHigGroupedSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    isDark: Boolean,
    cardModifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    headerInsideCard: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(modifier = modifier.fillMaxWidth()) {
        // 卡片外部传统 Header (当 headerInsideCard = false 且有标题时展示)
        if (!headerInsideCard && !title.isNullOrBlank()) {
            BasicText(
                text = title,
                style = AppleHigTypography.groupedSectionHeader.copy(
                    color = higColors.secondaryLabel
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
            )
        }

        // 分组卡片 Card
        AppleHigGroupedCard(
            modifier = cardModifier,
            isDark = isDark,
            cornerRadius = cornerRadius
        ) {
            // 卡片内部上端嵌入式 Header (参考 ConversationInfoScreen.kt 规范)
            if (headerInsideCard && !title.isNullOrBlank()) {
                BasicText(
                    text = title,
                    style = TextStyle(
                        fontFamily = AppleHigTypography.defaultFontFamily,
                        color = higColors.secondaryLabel,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        platformStyle = AppleHigTypography.defaultPlatformStyle,
                        lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                    ),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
                )
            }

            content()
        }

        // 分组底部 Footer 说明小字 (Apple HIG 标准 13pt Footnote)
        if (!footer.isNullOrBlank()) {
            BasicText(
                text = footer,
                style = AppleHigTypography.groupedSectionFooter.copy(
                    color = higColors.secondaryLabel
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
            )
        }
    }
}

/**
 * Apple HIG Inset Grouped 标准卡片容器
 */
@Composable
fun AppleHigGroupedCard(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    cornerRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val cardBg = if (isDark) {
        higColors.secondarySystemGroupedBackground.copy(alpha = 0.85f)
    } else {
        higColors.secondarySystemGroupedBackground
    }
    val cardBorder = higColors.separator.copy(alpha = if (isDark) 0.35f else 0.20f)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(0.5.dp, cardBorder, shape)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Apple HIG 扁平填充卡片容器 (Fill Card)
 *
 * 常见于 iMessage 对话详情页、个人动态等场景：
 * - 采用系统填充色 higColors.systemFill；
 * - 无边框描边；
 * - 默认圆角 16dp，默认内边距 horizontal = 16dp, vertical = 14dp；
 * - 外层左右边距由外部页面统一控制 (推荐 16dp)。
 */
@Composable
fun AppleHigFillCard(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    cornerRadius: Dp = 16.dp,
    contentPaddingValues: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    itemSpacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(higColors.systemFill)
            .padding(contentPaddingValues)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = if (itemSpacing > 0.dp) Arrangement.spacedBy(itemSpacing) else Arrangement.Top,
            content = content
        )
    }
}

/**
 * Apple HIG Inset Grouped 标准列表项 (39.6dp 紧凑单行高度，缩小90%)
 *
 * 遵循 iOS Settings 单元格设计：
 * 1. 严格控制单行默认高度在黄金触控高度；
 * 2. 禁用字体 padding 并开启光学垂直对齐，文字基准线与图标完美居中对齐；
 * 3. 左侧支持 25.2dp x 25.2dp 标准 App 图标圆角容器（6.3dp squircle）；
 * 4. 右侧支持辅助状态值（如“已开启”、“系统”等）与标准 Accessory（箭头/对勾/自定义）。
 */
@Composable
fun AppleHigRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconBgColor: Color = Color.Transparent,
    subtitle: String? = null,
    value: String? = null,
    accessory: AppleHigRowAccessory = AppleHigRowAccessory.Navigation,
    isDestructive: Boolean = false,
    isDark: Boolean,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val higColors = AppleHigColors.colors(isDark)
    val titleColor = if (isDestructive) higColors.destructive else higColors.label
    val subColor = higColors.secondaryLabel

    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier
            .defaultMinSize(minHeight = 39.6.dp)
            .padding(horizontal = 16.dp, vertical = if (subtitle != null) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧图标（25.2x25.2dp，6.3dp 圆角 squircle）
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(25.2.dp)
                    .clip(RoundedCornerShape(6.3.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(14.4.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
        }

        // 标题区（单行光学垂直居中；若有副标题则紧凑纵向排布）
        if (subtitle.isNullOrBlank()) {
            BasicText(
                text = title,
                style = AppleHigTypography.groupedRowTitle.copy(
                    color = titleColor,
                    fontWeight = if (isDestructive) FontWeight.Medium else FontWeight.Normal
                ),
                modifier = Modifier.weight(1f)
            )
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                BasicText(
                    text = title,
                    style = AppleHigTypography.groupedRowTitle.copy(
                        color = titleColor,
                        fontWeight = if (isDestructive) FontWeight.Medium else FontWeight.Normal
                    )
                )
                Spacer(Modifier.height(1.dp))
                BasicText(
                    text = subtitle,
                    style = AppleHigTypography.groupedRowSubtitle.copy(color = subColor),
                    maxLines = 1
                )
            }
        }

        // 右侧状态值文本 (Apple HIG 17pt Body，与左侧标题字号对称)
        if (!value.isNullOrBlank()) {
            BasicText(
                text = value,
                style = AppleHigTypography.groupedRowValue.copy(color = subColor)
            )
            Spacer(Modifier.width(6.dp))
        }

        // 自定义尾部内容
        if (trailingContent != null) {
            trailingContent()
        } else {
            // 标准 Accessory
            when (accessory) {
                AppleHigRowAccessory.Navigation -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = if (isDestructive) higColors.destructive.copy(alpha = 0.5f) else higColors.tertiaryLabel,
                        modifier = Modifier.size(11.7.dp)
                    )
                }
                AppleHigRowAccessory.Checkmark -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = higColors.systemBlue,
                        modifier = Modifier.size(16.2.dp)
                    )
                }
                is AppleHigRowAccessory.Custom -> {
                    accessory.content()
                }
                AppleHigRowAccessory.None -> {
                    // 无后缀
                }
            }
        }
    }
}

/**
 * Apple HIG Inset Grouped 标准分割线
 *
 * 遵循 iOS 规范：分割线仅在文字下方延伸，不贯穿左侧图标区域。
 * 默认 insetStart = 56.dp (16dp 左边距 + 28dp 图标 + 12dp 间隙)。若无图标可设为 16.dp。
 */
@Composable
fun AppleHigDivider(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    insetStart: Dp = 56.dp,
    insetEnd: Dp = 0.dp
) {
    val higColors = AppleHigColors.colors(isDark)
    HorizontalDivider(
        modifier = modifier.padding(start = insetStart, end = insetEnd),
        thickness = 0.5.dp,
        color = higColors.separator.copy(alpha = if (isDark) 0.35f else 0.20f)
    )
}
