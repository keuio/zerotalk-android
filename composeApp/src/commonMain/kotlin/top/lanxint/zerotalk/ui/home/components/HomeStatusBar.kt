package top.lanxint.zerotalk.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
import top.lanxint.zerotalk.ui.theme.AppleHigTheme
import com.kashif_e.backdrop.Backdrop

/**
 * 首页顶部状态栏组件 (HomeStatusBar) - 方案 A：融合型居中毛玻璃胶囊
 *
 * 将「自身位置」与「全站实时在线人数」整合为单个居中的精致药丸胶囊：
 * 格式：`📍 浙江 · 🟢 在线 128 人`
 * - 纯信息状态指示，取消点击弹窗干扰；
 * - 点击该胶囊轻量触发刷新在线人数（伴随 iOS 触觉弹性微缩放）；
 * - 全面复用 [CapsuleGlassButton] 玻璃胶囊组件与 [AppleHigTheme] 语义设计体系。
 */
@Composable
fun HomeStatusBar(
    onlineUsers: Int?,
    location: String?,
    onRefreshOnlineCount: () -> Unit,
    isDark: Boolean,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigTheme.colors

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CapsuleGlassButton(
            onClick = onRefreshOnlineCount,
            isDark = isDark,
            backdrop = backdrop,
            modifier = Modifier.height(34.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. 自身所在位置（对齐官网 locationLabel）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "我的位置",
                        tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF007AFF),
                        modifier = Modifier.size(13.dp)
                    )
                    val displayLocation = location?.trim()?.takeIf { it.isNotEmpty() } ?: "位置保密"
                    BasicText(
                        text = displayLocation,
                        style = AppleHigTheme.typography.footnote.copy(
                            color = higColors.label,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }

                // 2. 微弱小圆点分隔符
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(higColors.secondaryLabel.copy(alpha = 0.5f))
                )

                // 3. 实时在线状态与人数（对齐官网 overview-stat__value--online）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // 绿色状态圆点
                    Box(
                        modifier = Modifier
                            .size(6.5.dp)
                            .clip(CircleShape)
                            .background(higColors.systemGreen)
                    )
                    val onlineUsersText = if (onlineUsers != null && onlineUsers >= 0) {
                        "在线 $onlineUsers 人"
                    } else {
                        "在线 — 人"
                    }
                    BasicText(
                        text = onlineUsersText,
                        style = AppleHigTheme.typography.footnote.copy(
                            color = higColors.secondaryLabel,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
