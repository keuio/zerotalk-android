package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 「登录账号」表单内容
 *
 * 账号密码完全由用户输入（不再使用内置默认账号）。点击 Sheet 顶部「完成」提交，
 * 由 [top.lanxint.zerotalk.data.repository.ZeroTalkClientManager.ensureConnected] 完成
 * 登录 + Bootstrap + WebSocket 连接；成功后账号密码与会话 Cookie 会写入本地，
 * 供后续冷启动自动恢复登录态。
 */
@Composable
fun SheetLoginContent(
    username: String,
    password: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 4.dp
        ) {
            BasicText(
                text = "账号信息",
                style = AppleHigTypography.caption1.copy(
                    color = higColors.secondaryLabel,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 2.dp)
            )

            LoginField(
                label = "账号",
                value = username,
                placeholder = "零语账号 / 登录名",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                isPassword = false,
                enabled = !isSubmitting,
                onValueChange = onUsernameChange,
                isDark = isDark
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            LoginField(
                label = "密码",
                value = password,
                placeholder = "登录密码",
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                isPassword = true,
                enabled = !isSubmitting,
                onValueChange = onPasswordChange,
                isDark = isDark
            )
        }

        Spacer(Modifier.height(12.dp))

        // 状态区：登录中 / 错误 / 说明
        when {
            isSubmitting -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = higColors.tint,
                        strokeWidth = 1.5.dp
                    )
                    BasicText(
                        text = "正在登录并连接零语云服务...",
                        style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                    )
                }
            }
            errorMessage != null -> {
                BasicText(
                    text = errorMessage,
                    style = AppleHigTypography.footnote.copy(color = higColors.destructive)
                )
            }
            else -> {
                BasicText(
                    text = "登录成功后会自动在本机保存登录态，下次打开无需重复登录；如需切换账号，可在本页退出登录。",
                    style = AppleHigTypography.footnote.copy(color = higColors.tertiaryLabel)
                )
            }
        }
    }
}

/**
 * 登录表单的单行输入项（左侧标签 + 右侧输入框，行高对齐 HIG Inset Grouped 44dp）
 */
@Composable
private fun LoginField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isPassword: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = label,
            style = AppleHigTypography.groupedRowTitle.copy(color = higColors.label),
            modifier = Modifier.width(80.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = AppleHigTypography.groupedRowTitle.copy(color = higColors.label),
            cursorBrush = SolidColor(higColors.systemBlue),
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = AppleHigTypography.groupedRowTitle.copy(color = higColors.tertiaryLabel)
                    )
                }
                innerTextField()
            }
        )
    }
}
