package top.lanxint.zerotalk.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.components.AppleModalBottomSheet
import top.lanxint.zerotalk.ui.components.LiquidButton
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.components.sheetContentHeight
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/** 加密房间弹窗模式：unlock 为解锁已启用加密的房间，enable 为首次启用端到端加密 */
enum class UnlockRoomMode {
    UNLOCK,
    ENABLE
}

/**
 * 加密房间解锁弹窗。
 *
 * 对标官网 UnlockRoomModal：
 * 1. 标题与说明文案随 [titleMode] 切换；
 * 2. 暗号输入框为密码态，最大 128 位；
 * 3. 解锁/启用失败的错误文案由 [errorMessage] 回显；
 * 4. 「忘记暗号？」折叠区支持房主重置暗号、非房主联系房主；
 * 5. 组件本身不访问网络，所有动作通过回调交给外部接线。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockRoomSheet(
    visible: Boolean,
    loading: Boolean,
    titleMode: UnlockRoomMode,
    isRoomCreator: Boolean,
    roomCreatorName: String?,
    roomCreatorAvatar: String?,
    errorMessage: String?,
    onSubmit: (String) -> Unit,
    onClose: () -> Unit,
    onContactCreator: () -> Unit,
    onResetPassword: (String) -> Unit,
    resettingPassword: Boolean,
    isDark: Boolean,
    contactingCreator: Boolean = false
) {
    val higColors = AppleHigColors.colors(isDark)
    var password by remember { mutableStateOf("") }
    var displayedError by remember { mutableStateOf<String?>(errorMessage) }
    var forgotExpanded by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordConfirm by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf<String?>(null) }

    if (!visible) return

    // 官网在弹窗关闭时清空暗号、错误与重新展开状态
    LaunchedEffect(visible) {
        if (!visible) {
            password = ""
            displayedError = null
            forgotExpanded = false
            newPassword = ""
            newPasswordConfirm = ""
            resetError = null
        }
    }

    // 父层通过更换 errorMessage 回显解锁失败文案
    LaunchedEffect(errorMessage) {
        displayedError = errorMessage
    }

    fun submitPassword() {
        if (password.isNotBlank() && !loading) {
            displayedError = null
            onSubmit(password.trim())
        }
    }

    fun submitResetPassword() {
        if (resettingPassword) return
        when {
            newPassword.isBlank() -> resetError = "请输入新暗号"
            newPassword != newPasswordConfirm -> resetError = "两次输入的暗号不一致"
            else -> {
                resetError = null
                onResetPassword(newPassword.trim())
            }
        }
    }

    AppleModalBottomSheet(
        onDismissRequest = { if (!loading && !resettingPassword) onClose() },
        title = if (titleMode == UnlockRoomMode.ENABLE) "启用端到端消息加密" else "解锁消息",
        isDark = isDark
    ) {
        // 固定高度后内部滚动，避免展开「忘记暗号」时弹窗高度突变
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .sheetContentHeight(0.82f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            if (titleMode == UnlockRoomMode.ENABLE) {
                BasicText(
                    text = "启用后，新发送的消息将加密传输；历史消息保持可见。",
                    style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                )
                Spacer(Modifier.height(6.dp))
                BasicText(
                    text = "开启后无法关闭；你本人及全部成员均需输入房间暗号方可查看与发送消息，且每 24 小时需重新验证暗号。请提前告知成员并牢记暗号。",
                    style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                )
                Spacer(Modifier.height(14.dp))
            } else {
                BasicText(
                    text = "请输入房间暗号以查看和发送加密消息",
                    style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                )
                Spacer(Modifier.height(6.dp))
                BasicText(
                    text = "解锁状态将在 24 小时后失效；若更换浏览器、清除站点数据或重新登录，也需重新验证暗号。",
                    style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                )
                Spacer(Modifier.height(14.dp))
            }

            AppleHigFillCard(
                isDark = isDark,
                modifier = Modifier.fillMaxWidth(),
                contentPaddingValues = PaddingValues(16.dp),
                itemSpacing = 2.dp
            ) {
                UnlockPasswordField(
                    value = password,
                    onValueChange = { password = it.take(128) },
                    placeholder = "请输入房间暗号",
                    enabled = !loading,
                    onSubmit = ::submitPassword,
                    isDark = isDark
                )
            }

            Spacer(Modifier.height(10.dp))

            when {
                loading -> {
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
                            text = if (titleMode == UnlockRoomMode.ENABLE) "启用中…" else "解锁中…",
                            style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                        )
                    }
                }
                displayedError != null -> {
                    BasicText(
                        text = displayedError.orEmpty(),
                        style = AppleHigTypography.footnote.copy(color = higColors.destructive)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidButton(
                    onClick = onClose,
                    isDark = isDark,
                    isInteractive = !loading && !resettingPassword,
                    surfaceColor = higColors.secondarySystemFill,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    BasicText(
                        text = "取消",
                        style = AppleHigTypography.headline.copy(
                            color = higColors.label,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                LiquidButton(
                    onClick = ::submitPassword,
                    isDark = isDark,
                    isInteractive = !loading,
                    tint = higColors.tint,
                    surfaceColor = higColors.tint.copy(alpha = if (isDark) 0.85f else 0.90f),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        text = when {
                            loading && titleMode == UnlockRoomMode.ENABLE -> "启用中…"
                            loading -> "解锁中…"
                            titleMode == UnlockRoomMode.ENABLE -> "确认启用"
                            else -> "解锁"
                        },
                        style = AppleHigTypography.headline.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 忘记暗号折叠区只在官方解锁模式下出现
            if (titleMode == UnlockRoomMode.UNLOCK) {
                Spacer(Modifier.height(20.dp))
                ForgotPasswordSection(
                    isRoomCreator = isRoomCreator,
                    roomCreatorName = roomCreatorName,
                    roomCreatorAvatar = roomCreatorAvatar,
                    contactingCreator = contactingCreator,
                    resettingPassword = resettingPassword,
                    resetError = resetError,
                    expanded = forgotExpanded,
                    onToggleExpanded = { forgotExpanded = !forgotExpanded },
                    onContactCreator = onContactCreator,
                    onResetPassword = ::submitResetPassword,
                    onNewPasswordChange = { newPassword = it.take(128) },
                    onNewPasswordConfirmChange = { newPasswordConfirm = it.take(128) },
                    newPassword = newPassword,
                    newPasswordConfirm = newPasswordConfirm,
                    isDark = isDark
                )
            }
        }
    }
}

/** 「忘记暗号？」折叠区：房主重置暗号 / 非房主查看房主信息并发消息 */
@Composable
private fun ForgotPasswordSection(
    isRoomCreator: Boolean,
    roomCreatorName: String?,
    roomCreatorAvatar: String?,
    contactingCreator: Boolean,
    resettingPassword: Boolean,
    resetError: String?,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onContactCreator: () -> Unit,
    onResetPassword: () -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onNewPasswordConfirmChange: (String) -> Unit,
    newPassword: String,
    newPasswordConfirm: String,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    val chevronAngle by animateFloatAsState(if (expanded) 180f else 0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(higColors.quaternarySystemFill)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleExpanded
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        text = "忘记暗号？",
                        style = AppleHigTypography.footnote.copy(
                            color = higColors.tint,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = higColors.tint,
                        modifier = Modifier.size(16.dp).rotate(chevronAngle)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AppleHigDivider(isDark = isDark, insetStart = 0.dp)

                    Spacer(Modifier.height(12.dp))

                    if (isRoomCreator) {
                        BasicText(
                            text = "你是房主，可直接重置房间暗号。重置后所有成员需使用新暗号重新解锁，旧暗号将立即失效。",
                            style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                        )
                        Spacer(Modifier.height(14.dp))

                        PasswordLabeledField(
                            label = "新暗号",
                            value = newPassword,
                            onValueChange = onNewPasswordChange,
                            placeholder = "请输入新暗号",
                            enabled = !resettingPassword,
                            isDark = isDark
                        )
                        AppleHigDivider(isDark = isDark, insetStart = 88.dp)
                        PasswordLabeledField(
                            label = "确认新暗号",
                            value = newPasswordConfirm,
                            onValueChange = onNewPasswordConfirmChange,
                            placeholder = "请再次输入新暗号",
                            enabled = !resettingPassword,
                            isDark = isDark
                        )

                        if (resetError != null) {
                            Spacer(Modifier.height(8.dp))
                            BasicText(
                                text = resetError.orEmpty(),
                                style = AppleHigTypography.footnote.copy(color = higColors.destructive)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (resettingPassword) {
                                        higColors.systemFill
                                    } else {
                                        higColors.tint.copy(alpha = if (isDark) 0.14f else 0.12f)
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = !resettingPassword,
                                    onClick = onResetPassword
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                text = if (resettingPassword) "重置中…" else "重置暗号",
                                style = AppleHigTypography.headline.copy(
                                    color = if (resettingPassword) higColors.tertiaryLabel else higColors.tint,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    } else {
                        BasicText(
                            text = "请联系房主获取房间暗号。",
                            style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                        )
                        if (roomCreatorName.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            BasicText(
                                text = "房主信息加载中…",
                                style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                            )
                        } else {
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(higColors.systemFill)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                UserAvatar(
                                    url = roomCreatorAvatar,
                                    name = roomCreatorName,
                                    modifier = Modifier.size(40.dp),
                                    size = 40.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    BasicText(
                                        text = "房主",
                                        style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
                                    )
                                    BasicText(
                                        text = roomCreatorName,
                                        style = AppleHigTypography.subhead.copy(
                                            color = higColors.label,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (contactingCreator) {
                                                higColors.systemFill
                                            } else {
                                                higColors.tint.copy(alpha = if (isDark) 0.14f else 0.12f)
                                            }
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            enabled = !contactingCreator,
                                            onClick = onContactCreator
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = null,
                                            tint = if (contactingCreator) higColors.tertiaryLabel else higColors.tint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        BasicText(
                                            text = if (contactingCreator) "跳转中…" else "发送消息",
                                            style = AppleHigTypography.footnote.copy(
                                                color = if (contactingCreator) higColors.tertiaryLabel else higColors.tint,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

/** 解锁暗号输入框（密码态，最大 128 位） */
@Composable
private fun UnlockPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    onSubmit: () -> Unit,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = "房间暗号",
            style = AppleHigTypography.groupedRowTitle.copy(
                color = higColors.label,
                fontWeight = FontWeight.Medium
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(higColors.quaternarySystemFill)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                BasicText(
                    text = placeholder,
                    style = AppleHigTypography.groupedRowTitle.copy(color = higColors.tertiaryLabel),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = AppleHigTypography.groupedRowTitle.copy(color = higColors.label),
                cursorBrush = SolidColor(higColors.systemBlue),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField -> innerTextField() }
            )
        }
    }
}

/** 重置暗号输入行 */
@Composable
private fun PasswordLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = label,
            style = AppleHigTypography.groupedRowTitle.copy(
                color = higColors.label,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.width(88.dp)
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                BasicText(
                    text = placeholder,
                    style = AppleHigTypography.groupedRowTitle.copy(color = higColors.tertiaryLabel),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = AppleHigTypography.groupedRowTitle.copy(color = higColors.label),
                cursorBrush = SolidColor(higColors.systemBlue),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField -> innerTextField() }
            )
        }
    }
}
