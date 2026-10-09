package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 规范化性别解析辅助函数
 * 支持常见服务端返回格式："1"/"2", "male"/"female", "m"/"f", "男"/"女"
 * 返回 "男", "女" 或空字符串（未知/保密）
 */
fun parseGenderText(gender: String?): String {
    if (gender.isNullOrBlank()) return ""
    return when (gender.trim().lowercase()) {
        "1", "male", "m", "男", "男士" -> "男"
        "2", "female", "f", "女", "女士" -> "女"
        else -> ""
    }
}

/**
 * Apple HIG 风格通用性别胶囊标签
 *
 * 用于个人主页、搜索结果、聊天消息头像栏、群成员列表等处统一呈现
 */
@Composable
fun GenderBadge(
    gender: String?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 10.sp,
    horizontalPadding: Dp = 5.dp,
    verticalPadding: Dp = 1.dp
) {
    val text = parseGenderText(gender)
    if (text.isEmpty()) return

    val isFemale = text == "女"
    val badgeColor = if (isFemale) Color(0xFFE11D48) else Color(0xFF0284C7)
    val bgColor = badgeColor.copy(alpha = if (isDark) 0.22f else 0.15f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontFamily = AppleHigTypography.defaultFontFamily,
                color = badgeColor,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                platformStyle = AppleHigTypography.defaultPlatformStyle,
                lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
            )
        )
    }
}
