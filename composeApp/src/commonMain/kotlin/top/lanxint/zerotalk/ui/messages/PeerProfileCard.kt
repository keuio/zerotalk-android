package top.lanxint.zerotalk.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.data.model.OtherUserProfile
import top.lanxint.zerotalk.ui.components.GenderBadge
import top.lanxint.zerotalk.ui.components.LiquidToggle
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 首聊「对方信息卡片」
 *
 * 1:1 对齐官网 `PeerProfileCard`（chatCore，`peer-card--mobile`）：
 * - 头部：头像 + 昵称 + 性别，下方标签行（年龄段 / 地区）；
 * - 操作：动态 / 举报 两枚胶囊按钮；
 * - 个人简介：官方兜底文案「这个人很神秘，还没有写简介」；
 * - 提示行：可删除本次会话或拉黑对方；
 * - 清流模式行：显示对方状态 + **就近开关我自己的清流模式**。
 *
 * @param peer 对端资料（聚合接口下发；为空时只展示加载占位）
 * @param isDark 暗色主题
 * @param cleanStatusText 清流状态文案（对方已开启/关闭、房间强制/绕过等，由调用方按官方规则拼装）
 * @param myCleanStreamEnabled 我的清流模式开关状态
 * @param cleanSaving 我的清流模式保存中
 * @param onToggleCleanStream 切换我的清流模式
 * @param onMoments 查看动态
 * @param onReport 举报用户
 */
@Composable
fun PeerProfileCard(
    peer: OtherUserProfile?,
    isDark: Boolean,
    cleanStatusText: String,
    myCleanStreamEnabled: Boolean,
    cleanSaving: Boolean,
    onToggleCleanStream: (Boolean) -> Unit,
    onMoments: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
    tip: String = "点击右上角删除按钮，删除后彼此将无法查看该房间"
) {
    val higColors = AppleHigColors.colors(isDark)
    val displayName = peer?.name?.takeIf { it.isNotBlank() } ?: "匿名用户"
    val bio = peer?.bio?.takeIf { it.isNotBlank() } ?: "这个人很神秘，还没有写简介"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(higColors.secondarySystemFill)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // ---- 头部：头像 + 昵称 + 标签 + 操作 ----
        Row(verticalAlignment = Alignment.Top) {
            UserAvatar(
                url = peer?.avatarUrl?.takeIf { it.isNotBlank() },
                name = displayName,
                size = 38.dp,
                fallbackIconSize = 18.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = displayName,
                        style = AppleHigTypography.subhead.copy(
                            color = higColors.label,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                    val genderLabel = genderTextOf(peer)
                    if (genderLabel.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        GenderBadge(
                            gender = genderLabel,
                            isDark = isDark
                        )
                    }
                }

                // 标签行：年龄段 / 地区（官方 peer-card__tags）
                val tags = buildList {
                    peer?.ageRangeText?.takeIf { it.isNotBlank() && it != "保密" }?.let { add(it) }
                }
                val location = peer?.location.orEmpty()
                if (tags.isNotEmpty() || location.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            PillLabel(text = tag, isDark = isDark)
                        }
                        if (location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = higColors.tertiaryLabel,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                BasicText(
                                    text = location,
                                    style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 操作：动态 / 举报（官方 peer-card__actions，隐私模式隐藏动态入口）
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (peer?.privacyMode != true) {
                    CardPillButton(text = "动态", isDark = isDark, onClick = onMoments)
                }
                CardPillButton(text = "举报", isDark = isDark, destructive = true, onClick = onReport)
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- 个人简介 ----
        BasicText(
            text = "个人简介",
            style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            text = bio,
            style = AppleHigTypography.footnote.copy(
                color = if (peer?.bio.isNullOrBlank()) higColors.tertiaryLabel else higColors.secondaryLabel
            )
        )

        if (tip.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = tip,
                style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
            )
        }

        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(higColors.separator.copy(alpha = 0.5f))
        )
        Spacer(Modifier.height(8.dp))

        // ---- 清流模式：对方状态 + 我自己就近开关 ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = "清流模式",
                    style = AppleHigTypography.footnote.copy(
                        color = higColors.label,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = cleanStatusText,
                    style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel)
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.padding(top = 2.dp)) {
                LiquidToggle(
                    selected = { myCleanStreamEnabled },
                    onSelect = { if (!cleanSaving) onToggleCleanStream(it) },
                    isDark = isDark
                )
            }
        }
    }
}

/** 性别文案（服务端已下发中文 gender_text，缺省则不显示） */
private fun genderTextOf(peer: OtherUserProfile?): String = peer?.genderText.orEmpty().trim()

@Composable
private fun PillLabel(text: String, isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(higColors.systemFill)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        BasicText(
            text = text,
            style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel)
        )
    }
}

@Composable
private fun CardPillButton(
    text: String,
    isDark: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val contentColor = if (destructive) higColors.destructive else higColors.tint
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(contentColor.copy(alpha = 0.14f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = AppleHigTypography.caption1.copy(
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// 向后兼容类型别名与委托
typealias UserReportReason = top.lanxint.zerotalk.ui.components.UserReportReason

@Composable
fun UserReportDialog(
    userName: String,
    isDark: Boolean,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (UserReportReason, String) -> Unit
) {
    top.lanxint.zerotalk.ui.components.UserReportDialog(
        userName = userName,
        isDark = isDark,
        submitting = submitting,
        onDismiss = onDismiss,
        onSubmit = onSubmit
    )
}
