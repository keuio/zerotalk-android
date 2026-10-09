package top.lanxint.zerotalk.ui.sheets

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.CreateRoomRequest
import top.lanxint.zerotalk.data.model.JoinRoomRequest
import top.lanxint.zerotalk.data.network.CreateRoomData
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.components.LiquidButton
import top.lanxint.zerotalk.ui.components.LiquidToggle
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import com.kashif_e.backdrop.Backdrop

/**
 * 创建房间 Sheet 内容
 */
@Composable
fun SheetCreateRoomContent(
    isDark: Boolean,
    backdrop: Backdrop,
    onCreateSuccess: (CreateRoomData) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current

    var roomName by remember { mutableStateOf("") }
    var roomSecret by remember { mutableStateOf("") }
    var isSecretVisible by remember { mutableStateOf(false) }
    // 对齐官网 RoomEncryptionSwitch（mode="create"）：默认关闭，创建前可自由开关
    var isE2EE by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 分组卡片：房间基础信息
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 2.dp
        ) {
            BasicText(
                text = "房间设置",
                style = TextStyle(
                    fontFamily = AppleHigTypography.defaultFontFamily,
                    color = higColors.secondaryLabel,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    platformStyle = AppleHigTypography.defaultPlatformStyle,
                    lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // 输入行 1: 房间名称
            RoomInputField(
                label = "房间名称",
                value = roomName,
                onValueChange = { roomName = it },
                placeholder = "如：深夜树洞 / 秘密基地",
                isDark = isDark
            )

            AppleHigDivider(isDark = isDark, insetStart = 88.dp)

            // 输入行 2: 房间暗号 (带显隐切换)
            RoomInputField(
                label = "房间暗号",
                value = roomSecret,
                onValueChange = { roomSecret = it },
                placeholder = "设置进入暗号/密码",
                isSecret = true,
                isSecretVisible = isSecretVisible,
                onToggleSecretVisibility = { isSecretVisible = !isSecretVisible },
                isDark = isDark
            )

            AppleHigDivider(isDark = isDark, insetStart = 88.dp)

            // 开关行：端到端消息加密
            // 官网语义（RoomEncryptionSwitch mode="create"）：创建前是普通可交互开关（默认关闭、可反复切换），
            // 但一旦以加密方式创建，房间后续在设置里就无法再关闭 —— 所以这里不做强制锁定。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = "端到端消息加密",
                        style = AppleHigTypography.groupedRowTitle.copy(color = higColors.label)
                    )
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = "消息将加密传输，需用暗号解锁后才能查看和发送，无法关闭",
                        style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
                    )
                    // 开启态才出现的补充说明（官网同款条件文案）
                    if (isE2EE) {
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            text = "开启后无法关闭。解锁状态约 24 小时有效，到期后需重新输入暗号；" +
                                "清除应用数据、更换设备或重新登录账号等操作，" +
                                "也会要求重新验证暗号后才能查看和发送消息。",
                            style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.tertiaryLabel)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // 提交期间置灰并忽略交互（官网 loading 时 disabled）
                Box(modifier = Modifier.alpha(if (isSubmitting) 0.45f else 1f)) {
                    LiquidToggle(
                        selected = { isE2EE },
                        onSelect = { if (!isSubmitting) isE2EE = it },
                        isDark = isDark
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 创建按钮
        LiquidButton(
            onClick = {
                if (isSubmitting) return@LiquidButton
                when {
                    roomName.trim().isBlank() -> {
                        notificationState.show("请输入房间名称")
                    }
                    roomSecret.trim().isBlank() -> {
                        notificationState.show("请输入房间暗号")
                    }
                    else -> {
                        isSubmitting = true
                        notificationState.show("正在创建房间「${roomName.trim()}」...")
                        ZeroTalkClientManager.createSecretRoom(
                            roomName = roomName.trim(),
                            password = roomSecret.trim(),
                            encryptionEnabled = isE2EE,
                            onSuccess = { data ->
                                isSubmitting = false
                                val hint = data.shareHint ?: "房间「${data.roomName}」创建成功"
                                notificationState.show(hint)
                                onCreateSuccess(data)
                            },
                            onError = { err ->
                                isSubmitting = false
                                notificationState.show("创建失败: $err")
                            }
                        )
                    }
                }
            },
            backdrop = backdrop,
            surfaceColor = higColors.tint.copy(alpha = if (isDark) 0.85f else 0.90f),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            BasicText(
                text = if (isSubmitting) "正在创建房间..." else "立即创建房间",
                style = AppleHigTypography.subhead.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * 加入房间 Sheet 内容
 */
@Composable
fun SheetJoinRoomContent(
    isDark: Boolean,
    backdrop: Backdrop,
    onJoinSuccess: (roomId: String, roomName: String) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current

    var ownerName by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    var roomSecret by remember { mutableStateOf("") }
    var isSecretVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 分组卡片：加入信息
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 2.dp
        ) {
            BasicText(
                text = "通行验证",
                style = TextStyle(
                    fontFamily = AppleHigTypography.defaultFontFamily,
                    color = higColors.secondaryLabel,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    platformStyle = AppleHigTypography.defaultPlatformStyle,
                    lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // 输入行 1: 创建者用户名
            RoomInputField(
                label = "创建者",
                value = ownerName,
                onValueChange = { ownerName = it },
                placeholder = "输入房主用户名或 ID",
                isDark = isDark
            )

            AppleHigDivider(isDark = isDark, insetStart = 88.dp)

            // 输入行 2: 房间名称
            RoomInputField(
                label = "房间名称",
                value = roomName,
                onValueChange = { roomName = it },
                placeholder = "输入目标房间名称",
                isDark = isDark
            )

            AppleHigDivider(isDark = isDark, insetStart = 88.dp)

            // 输入行 3: 房间暗号 (带显隐切换)
            RoomInputField(
                label = "房间暗号",
                value = roomSecret,
                onValueChange = { roomSecret = it },
                placeholder = "输入准入通行暗号",
                isSecret = true,
                isSecretVisible = isSecretVisible,
                onToggleSecretVisibility = { isSecretVisible = !isSecretVisible },
                isDark = isDark
            )
        }

        Spacer(Modifier.height(24.dp))

        // 加入按钮
        LiquidButton(
            onClick = {
                if (isSubmitting) return@LiquidButton
                when {
                    ownerName.trim().isBlank() -> {
                        notificationState.show("请输入创建者用户名")
                    }
                    roomName.trim().isBlank() -> {
                        notificationState.show("请输入房间名称")
                    }
                    roomSecret.trim().isBlank() -> {
                        notificationState.show("请输入房间暗号")
                    }
                    else -> {
                        isSubmitting = true
                        notificationState.show("正在验证准入暗号...")
                        ZeroTalkClientManager.joinSecretRoom(
                            creatorUsername = ownerName.trim(),
                            roomName = roomName.trim(),
                            password = roomSecret.trim(),
                            onSuccess = { data ->
                                isSubmitting = false
                                notificationState.show("成功加入房间「${roomName.trim()}」")
                                onJoinSuccess(data.roomId, roomName.trim())
                            },
                            onError = { err ->
                                isSubmitting = false
                                notificationState.show(err)
                            }
                        )
                    }
                }
            },
            backdrop = backdrop,
            surfaceColor = higColors.tint.copy(alpha = if (isDark) 0.85f else 0.90f),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            BasicText(
                text = if (isSubmitting) "正在加入房间..." else "加入房间",
                style = AppleHigTypography.subhead.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * 房间表单通用单行输入项
 */
@Composable
private fun RoomInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isSecret: Boolean = false,
    isSecretVisible: Boolean = false,
    onToggleSecretVisibility: (() -> Unit)? = null,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    val titleColor = higColors.label
    val placeholderColor = higColors.tertiaryLabel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = label,
            style = AppleHigTypography.groupedRowTitle.copy(
                color = titleColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.width(80.dp)
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                BasicText(
                    text = placeholder,
                    style = AppleHigTypography.groupedRowTitle.copy(color = placeholderColor)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = AppleHigTypography.groupedRowTitle.copy(color = titleColor),
                cursorBrush = SolidColor(higColors.systemBlue),
                singleLine = true,
                visualTransformation = if (isSecret && !isSecretVisible) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 密码显隐切换按钮
        if (isSecret && onToggleSecretVisibility != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleSecretVisibility
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = if (isSecretVisible) "隐藏" else "显示",
                    style = AppleHigTypography.caption1.copy(
                        color = higColors.systemBlue,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
