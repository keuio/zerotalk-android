package top.lanxint.zerotalk.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.ui.components.ZeroTalkBottomTab
import top.lanxint.zerotalk.ui.components.ZeroTalkBottomTabs
import com.kashif_e.backdrop.Backdrop

enum class ZeroTalkTab(val title: String, val icon: ImageVector) {
    HOME("首页", Icons.Default.Home),
    MESSAGES("消息", Icons.AutoMirrored.Filled.Chat),
    DYNAMIC("动态", Icons.Default.Explore),
    PROFILE("我的", Icons.Default.Person)
}

@Composable
fun ZeroTalkNavBar(
    selectedTab: ZeroTalkTab,
    onTabSelected: (ZeroTalkTab) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    val tabs = ZeroTalkTab.entries
    val inactiveColor = if (isDark) Color(0xFF8E95A3) else Color(0xFF5A6275)

    ZeroTalkBottomTabs(
        selectedTabIndex = { selectedTab.ordinal },
        onTabSelected = { index -> onTabSelected(tabs[index]) },
        backdrop = backdrop,
        tabsCount = tabs.size,
        outerHeight = 53.2.dp,
        innerHeight = 45.6.dp,
        modifier = modifier.width(266.dp),
        isDark = isDark
    ) {
        tabs.forEach { tab ->
            ZeroTalkBottomTab(onClick = { onTabSelected(tab) }) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    modifier = Modifier.size(20.9.dp),
                    tint = inactiveColor
                )
                BasicText(
                    text = tab.title,
                    style = TextStyle(
                        color = inactiveColor,
                        fontSize = 8.55.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
