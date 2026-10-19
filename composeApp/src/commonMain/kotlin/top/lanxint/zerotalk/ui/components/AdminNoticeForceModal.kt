package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import top.lanxint.zerotalk.data.model.NotificationItemDto
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 全站强制公告通知弹窗 (对接官方 GET /api/notifications/force-popups 与 POST /notifications/force-popup/ack)
 *
 * 阻断式全屏展示：
 * - 倒计时读秒限制，倒计时结束前确认按钮禁用（文案「请认真阅读 (Ns)」）；
 * - 倒计时归零后解锁「我已知晓并确认」按钮；
 * - 点击后调用 ack 回执接口并关闭弹窗。
 */
@Composable
fun AdminNoticeForceModal(
    item: NotificationItemDto,
    readSeconds: Int,
    isAcking: Boolean,
    isDark: Boolean,
    onAck: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val effectiveSeconds = (item.readSeconds ?: item.payload?.readSeconds ?: readSeconds).coerceAtLeast(1)
    var countdown by remember(item.id) { mutableStateOf(effectiveSeconds) }

    LaunchedEffect(item.id) {
        countdown = effectiveSeconds
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    val isAlert = item.level.equals("error", ignoreCase = true) ||
            item.level.equals("danger", ignoreCase = true) ||
            item.level.equals("warning", ignoreCase = true)

    val accentColor = if (isAlert) {
        higColors.destructive
    } else {
        higColors.tint
    }

    AlertDialog(
        onDismissRequest = {}, // 阻断式：不可通过点击外部或返回键跳过
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = higColors.secondarySystemGroupedBackground,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAlert) Icons.Rounded.WarningAmber else Icons.Rounded.Campaign,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val label = item.label?.ifBlank { null }
                    if (label != null) {
                        Text(
                            text = label,
                            style = AppleHigTypography.caption2.copy(
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = item.title?.ifBlank { null } ?: "全站重要公告",
                        style = AppleHigTypography.headline.copy(
                            color = higColors.label,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val bodyText = item.body.orEmpty().ifBlank { "（无通知正文）" }
                Text(
                    text = bodyText,
                    style = AppleHigTypography.body.copy(
                        color = higColors.label,
                        lineHeight = 22.sp
                    )
                )

                if (!item.createdAt.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "发布时间：${item.createdAt}",
                        style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAck,
                enabled = countdown <= 0 && !isAcking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White,
                    disabledContainerColor = higColors.systemFill,
                    disabledContentColor = higColors.secondaryLabel
                )
            ) {
                if (isAcking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else if (countdown > 0) {
                    Text(
                        text = "请认真阅读 (${countdown}s)",
                        style = AppleHigTypography.body.copy(fontWeight = FontWeight.Medium)
                    )
                } else {
                    Text(
                        text = "我已知晓并确认",
                        style = AppleHigTypography.body.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    )
}
