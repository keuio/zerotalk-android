package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 举报理由（1:1 对齐官方 useMomentReport 的 reason 取值）
 */
enum class UserReportReason(val value: String, val label: String) {
    PORNOGRAPHIC("pornographic", "色情低俗"),
    ADVERTISEMENT("advertisement", "广告宣传"),
    POLITICAL("political", "政治敏感"),
    ROBOT("robot", "疑似机器人"),
    AVATAR_NICKNAME("avatar_nickname", "头像/昵称违规"),
    OTHER("other", "其他违规")
}

/**
 * 全局通用举报用户/动态弹窗
 *
 * 官方 `MomentReportModal` 同款：单选理由（默认色情低俗）+ 选填补充说明，
 * 文案「请选择举报理由，平台将人工核实处理。」与「已提交举报，我们会尽快核实」。
 */
@Composable
fun UserReportDialog(
    userName: String,
    isDark: Boolean,
    submitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (UserReportReason, String) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    var reason by remember { mutableStateOf(UserReportReason.PORNOGRAPHIC) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = {
            BasicText(
                text = "举报 ${userName.ifBlank { "该用户" }}",
                style = AppleHigTypography.headline.copy(color = higColors.label)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BasicText(
                    text = "请选择举报理由，平台将人工核实处理。",
                    style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                )
                Spacer(Modifier.height(8.dp))
                UserReportReason.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !submitting,
                                onClick = { reason = option }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (reason == option) higColors.tint
                                    else higColors.systemFill
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (reason == option) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            text = option.label,
                            style = AppleHigTypography.body.copy(color = higColors.label)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                BasicText(
                    text = "补充说明（选填）",
                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(higColors.systemFill)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        enabled = !submitting,
                        textStyle = AppleHigTypography.footnote.copy(color = higColors.label),
                        cursorBrush = SolidColor(higColors.tint),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting,
                onClick = { onSubmit(reason, description) }
            ) {
                BasicText(
                    text = if (submitting) "提交中…" else "提交举报",
                    style = AppleHigTypography.body.copy(color = higColors.tint)
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !submitting,
                onClick = onDismiss
            ) {
                BasicText(
                    text = "取消",
                    style = AppleHigTypography.body.copy(color = higColors.secondaryLabel)
                )
            }
        }
    )
}
