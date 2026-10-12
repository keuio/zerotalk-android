package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 通用聊天室/群聊/大厅公告横幅条（对齐官方 public-announcement-bar）
 *
 * 规格特性：
 * - Apple HIG 磨砂玻璃卡片视觉，亮暗模式自适应
 * - 支持长文本就地展开 / 折叠查看完整公告
 * - 支持点击胶囊标签查看详情与资料页
 * - 支持一键关闭并记录到已忽略集合
 */
@Composable
fun RoomAnnouncementBanner(
    announcement: String,
    isDark: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "公告",
    onBannerClick: (() -> Unit)? = null
) {
    val higColors = AppleHigColors.colors(isDark)
    var isExpanded by remember(announcement) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0xCC1E222B) else Color(0xE6FFFFFF))
            .border(
                0.5.dp,
                if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .clickable {
                isExpanded = !isExpanded
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = if (isExpanded) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(higColors.systemBlue.copy(alpha = 0.14f))
                    .then(
                        if (onBannerClick != null) {
                            Modifier.clickable { onBannerClick() }
                        } else Modifier
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                BasicText(
                    text = label,
                    style = AppleHigTypography.caption2.copy(
                        color = higColors.systemBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            BasicText(
                text = announcement,
                style = AppleHigTypography.footnote.copy(
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                ),
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // 展开/折叠图标与关闭按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { isExpanded = !isExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起公告" else "展开公告",
                        tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.width(2.dp))

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭公告",
                        tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
