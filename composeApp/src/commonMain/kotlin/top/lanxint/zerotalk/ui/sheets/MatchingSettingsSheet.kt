package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.components.LiquidSegmentedControl
import top.lanxint.zerotalk.ui.components.LiquidToggle
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 匹配偏好设置数据模型
 */
data class MatchingPreferences(
    val ageIndex: Int = 0,
    val genderIndex: Int = 0,
    val showLocation: Boolean = true,
    val cleanStreamMode: Boolean = false,
    val anonymousMode: Boolean = true
)

@Composable
fun SheetMatchingSettingsContent(
    preferences: MatchingPreferences,
    onPreferencesChange: (MatchingPreferences) -> Unit,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 分组一：基础偏好 (对齐 ConversationInfoScreen：无分割线，由留白自然分隔)
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 6.dp
        ) {
            BasicText(
                text = "基础偏好",
                style = TextStyle(
                    fontFamily = AppleHigTypography.defaultFontFamily,
                    color = higColors.secondaryLabel,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    platformStyle = AppleHigTypography.defaultPlatformStyle,
                    lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // 年龄范围
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "年龄范围",
                    style = TextStyle(
                        color = higColors.label,
                        fontSize = 15.3.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )

                LiquidSegmentedControl(
                    options = listOf("18-23", "23以上"),
                    selectedIndex = preferences.ageIndex,
                    onOptionSelect = { onPreferencesChange(preferences.copy(ageIndex = it)) },
                    modifier = Modifier.width(138.dp),
                    isDark = isDark
                )
            }

            // 匹配性别
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "匹配性别",
                    style = TextStyle(
                        color = higColors.label,
                        fontSize = 15.3.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )

                LiquidSegmentedControl(
                    options = listOf("无", "异性", "同性"),
                    selectedIndex = preferences.genderIndex,
                    onOptionSelect = { onPreferencesChange(preferences.copy(genderIndex = it)) },
                    modifier = Modifier.width(168.dp),
                    isDark = isDark
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 分组二：体验控制 (彻底对齐 ConversationInfoScreen：纯平填充卡片、无分割线、纯留白呼吸间距)
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 6.dp
        ) {
            BasicText(
                text = "体验控制",
                style = TextStyle(
                    fontFamily = AppleHigTypography.defaultFontFamily,
                    color = higColors.secondaryLabel,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    platformStyle = AppleHigTypography.defaultPlatformStyle,
                    lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // 开关 1: 我的位置
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "我的位置",
                    style = TextStyle(
                        color = higColors.label,
                        fontSize = 15.3.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )

                LiquidToggle(
                    selected = { preferences.showLocation },
                    onSelect = { onPreferencesChange(preferences.copy(showLocation = it)) },
                    isDark = isDark
                )
            }

            // 开关 2: 清流模式
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "清流模式",
                    style = TextStyle(
                        color = higColors.label,
                        fontSize = 15.3.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )

                LiquidToggle(
                    selected = { preferences.cleanStreamMode },
                    onSelect = { onPreferencesChange(preferences.copy(cleanStreamMode = it)) },
                    isDark = isDark
                )
            }

            // 开关 3: 匿名模式
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "匿名模式",
                    style = TextStyle(
                        color = higColors.label,
                        fontSize = 15.3.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )

                LiquidToggle(
                    selected = { preferences.anonymousMode },
                    onSelect = { onPreferencesChange(preferences.copy(anonymousMode = it)) },
                    isDark = isDark
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
