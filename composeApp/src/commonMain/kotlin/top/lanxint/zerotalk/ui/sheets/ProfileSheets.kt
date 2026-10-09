package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import top.lanxint.zerotalk.data.network.AvatarUploadDto
import top.lanxint.zerotalk.ui.utils.rememberPhotoPickerLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.UserProfile
import top.lanxint.zerotalk.data.network.MomentsPrivacyData
import top.lanxint.zerotalk.data.network.ReportItemDto
import top.lanxint.zerotalk.data.network.UserLookupData
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.components.AppleHigGroupedCard
import top.lanxint.zerotalk.ui.components.AppleHigGroupedSection
import top.lanxint.zerotalk.ui.components.AppleHigRow
import top.lanxint.zerotalk.ui.components.AppleHigRowAccessory
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
import top.lanxint.zerotalk.ui.components.GenderBadge
import top.lanxint.zerotalk.ui.components.LiquidSegmentedControl
import top.lanxint.zerotalk.ui.components.LiquidToggle
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 官方解析头像上传受限原因纯函数 (对齐官网 EditProfileView E(reason) 计算逻辑)
 */
fun resolveAvatarUploadReason(reason: String?, upload: AvatarUploadDto?): String {
    val r = reason?.trim().orEmpty()
    val daysRemaining = upload?.daysRemaining ?: 0
    val requiredDays = upload?.requiredDays ?: 7
    return when {
        r.contains("禁止上传头像") -> "已被禁止上传头像"
        r.contains("注销") -> "账号已注销"
        r.contains("封禁") -> "账号已封禁"
        r.contains("违规") -> "账号受限，暂不可改"
        r.contains("注册时间") -> "暂不可上传"
        daysRemaining > 0 || r.contains("注册满") -> {
            if (daysRemaining > 0) "还需 ${daysRemaining} 天可上传" else "注册满 ${requiredDays} 天可上传"
        }
        r.isNotBlank() -> r
        else -> ""
    }
}

/**
 * 官方头像说明副标题 (对齐官网 EditProfileView se 计算属性)
 */
fun resolveAvatarSubtitleHint(upload: AvatarUploadDto?): String {
    val reason = upload?.reason?.trim().orEmpty()
    if (reason.isNotBlank()) {
        val mapped = resolveAvatarUploadReason(reason, upload)
        if (mapped.isNotBlank()) return mapped
    }
    return if (upload?.canUpload == true) {
        "jpg / png / webp，≤ 1MB"
    } else {
        "注册满 7 天且无受限后可改"
    }
}

/**
 * 修改个人资料表单 Sheet 内容 (Apple HIG Inset Grouped)
 */
@Composable
fun SheetEditProfileContent(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    val titleColor = higColors.label

    val uploadDto = profile.avatarUpload
    val canUpload = uploadDto?.canUpload == true
    val hasCustom = profile.hasCustomAvatar || (uploadDto?.hasCustom == true)
    val subtitleHint = resolveAvatarSubtitleHint(uploadDto)

    var isUploadingAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberPhotoPickerLauncher(
        maxItems = 1,
        onImagesSelected = { photos ->
            val photo = photos.firstOrNull() ?: return@rememberPhotoPickerLauncher
            val bytes = photo.byteArray
            if (bytes.size > 1024 * 1024) {
                avatarError = "图片不能超过 1MB"
                return@rememberPhotoPickerLauncher
            }
            val uri = photo.uriString.lowercase()
            if (uri.contains(".") && !uri.endsWith(".jpg") && !uri.endsWith(".jpeg") && !uri.endsWith(".png") && !uri.endsWith(".webp")) {
                avatarError = "仅支持 jpg / png / webp"
                return@rememberPhotoPickerLauncher
            }
            isUploadingAvatar = true
            avatarError = null
            val filename = when {
                uri.endsWith(".png") -> "avatar.png"
                uri.endsWith(".webp") -> "avatar.webp"
                else -> "avatar.jpg"
            }
            ZeroTalkClientManager.uploadAvatar(bytes, filename) { success, err ->
                isUploadingAvatar = false
                if (success) {
                    avatarError = null
                    onProfileChange(ZeroTalkClientManager.userProfile.value)
                } else {
                    avatarError = resolveAvatarUploadReason(err, uploadDto).ifBlank { err ?: "头像上传失败" }
                }
            }
        },
        onPermissionDenied = {
            avatarError = "未授予相册访问权限"
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 头像快速预览与上传管理 (AppleHigFillCard，对齐官网 EditProfileView)
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 头像容器：点击唤起相册（受控于 canUpload），带相机图标遮罩与上传中菊花
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isUploadingAvatar) {
                            if (!canUpload) {
                                avatarError = resolveAvatarUploadReason(uploadDto?.reason, uploadDto).ifBlank { "暂不可上传头像" }
                            } else {
                                avatarError = null
                                photoPicker.launch()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        url = profile.avatarUrl,
                        name = profile.name,
                        size = 54.dp,
                        gradient = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFF818CF8),
                            Color(0xFFC084FC)
                        ),
                        fallbackIconSize = 30.dp
                    )

                    // 相机蒙层与加载指示器
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Color.Black.copy(
                                    alpha = if (isUploadingAvatar) 0.5f else if (canUpload) 0.22f else 0.40f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingAvatar) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = if (canUpload) "更换头像" else "暂不可上传头像",
                                tint = Color.White.copy(alpha = if (canUpload) 0.92f else 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 文案展示：标题 + 限制原因/倒计时/格式规则
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BasicText(
                            text = profile.name.ifBlank { "头像" },
                            style = AppleHigTypography.body.copy(color = titleColor, fontWeight = FontWeight.Medium)
                        )
                        if (!canUpload) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(higColors.systemFill)
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                BasicText(
                                    text = "暂不可上传",
                                    style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel)
                                )
                            }
                        }
                    }
                    BasicText(
                        text = subtitleHint,
                        style = AppleHigTypography.caption1.copy(
                            color = if (!canUpload && uploadDto?.reason?.isNotBlank() == true) higColors.systemRed else higColors.secondaryLabel
                        )
                    )
                }

                // 右侧操作：刷新 QQ 头像 + 恢复默认（仅在已设置自定义头像且允许上传时呈现）
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CapsuleGlassButton(
                        onClick = {
                            ZeroTalkClientManager.refreshAvatar { success, msg ->
                                if (success) {
                                    avatarError = null
                                    onProfileChange(ZeroTalkClientManager.userProfile.value)
                                } else {
                                    avatarError = msg ?: "头像刷新失败"
                                }
                            }
                        },
                        isDark = isDark,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新 QQ 头像",
                                tint = higColors.tint,
                                modifier = Modifier.size(12.dp)
                            )
                            BasicText(
                                text = "刷新头像",
                                style = AppleHigTypography.footnote.copy(color = higColors.tint, fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    if (hasCustom && canUpload) {
                        CapsuleGlassButton(
                            onClick = {
                                if (!isUploadingAvatar) {
                                    ZeroTalkClientManager.clearAvatar { success, msg ->
                                        if (success) {
                                            avatarError = null
                                            onProfileChange(ZeroTalkClientManager.userProfile.value)
                                        } else {
                                            avatarError = msg ?: "恢复默认头像失败"
                                        }
                                    }
                                }
                            },
                            isDark = isDark,
                            modifier = Modifier.height(26.dp)
                        ) {
                            BasicText(
                                text = "恢复默认",
                                style = AppleHigTypography.caption1.copy(color = higColors.systemRed, fontWeight = FontWeight.Normal),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            // 错误/受限提示行 (form-error)
            if (!avatarError.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = higColors.systemRed,
                        modifier = Modifier.size(13.dp)
                    )
                    BasicText(
                        text = avatarError.orEmpty(),
                        style = AppleHigTypography.caption1.copy(color = higColors.systemRed)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 基本信息分组 (AppleHigFillCard)
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 4.dp
        ) {
            BasicText(
                text = "基本信息",
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

            // 昵称输入行 (44dp 标准高度)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "用户昵称",
                    style = AppleHigTypography.groupedRowTitle.copy(color = titleColor),
                    modifier = Modifier.width(80.dp)
                )
                BasicTextField(
                    value = profile.name,
                    onValueChange = { onProfileChange(profile.copy(name = it)) },
                    textStyle = AppleHigTypography.groupedRowTitle.copy(color = titleColor),
                    cursorBrush = SolidColor(higColors.systemBlue),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
            }

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            // 性别选择行
            AppleHigRow(
                title = "性别",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidSegmentedControl(
                        options = UserProfile.GENDER_OPTIONS,
                        selectedIndex = UserProfile.GENDER_OPTIONS.indexOf(profile.gender).coerceAtLeast(0),
                        onOptionSelect = { index ->
                            onProfileChange(profile.copy(gender = UserProfile.GENDER_OPTIONS[index]))
                        },
                        modifier = Modifier.width(130.dp),
                        isDark = isDark
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            // 年龄区间选择（严格仅限 18-23 和 23以上）
            AppleHigRow(
                title = "年龄区间",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidSegmentedControl(
                        options = UserProfile.AGE_OPTIONS,
                        selectedIndex = UserProfile.AGE_OPTIONS.indexOf(profile.ageRange).coerceAtLeast(0),
                        onOptionSelect = { index ->
                            onProfileChange(profile.copy(ageRange = UserProfile.AGE_OPTIONS[index]))
                        },
                        modifier = Modifier.width(148.dp),
                        isDark = isDark
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            // 拍一拍后缀
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "拍一拍后缀",
                    style = AppleHigTypography.groupedRowTitle.copy(color = titleColor),
                    modifier = Modifier.width(80.dp)
                )
                BasicTextField(
                    value = profile.patText,
                    onValueChange = { onProfileChange(profile.copy(patText = it)) },
                    textStyle = AppleHigTypography.groupedRowTitle.copy(color = titleColor),
                    cursorBrush = SolidColor(higColors.systemBlue),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    decorationBox = { innerTextField ->
                        if (profile.patText.isEmpty()) {
                            BasicText(
                                text = "例如：的小脑袋",
                                style = AppleHigTypography.groupedRowTitle.copy(color = higColors.tertiaryLabel)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            // QQ 号码输入行 (用于绑定并同步 QQ 头像)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "QQ 号码",
                    style = AppleHigTypography.groupedRowTitle.copy(color = titleColor),
                    modifier = Modifier.width(80.dp)
                )
                BasicTextField(
                    value = profile.qq,
                    onValueChange = { onProfileChange(profile.copy(qq = it.trim())) },
                    textStyle = AppleHigTypography.groupedRowTitle.copy(color = titleColor),
                    cursorBrush = SolidColor(higColors.systemBlue),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    decorationBox = { innerTextField ->
                        if (profile.qq.isEmpty()) {
                            BasicText(
                                text = "填写 5-20 位 QQ 号绑定头像",
                                style = AppleHigTypography.groupedRowTitle.copy(color = higColors.tertiaryLabel)
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 个人简介分组 (AppleHigFillCard)
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 4.dp
        ) {
            BasicText(
                text = "个人简介",
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                BasicTextField(
                    value = profile.bio,
                    onValueChange = { onProfileChange(profile.copy(bio = it)) },
                    textStyle = TextStyle(
                        fontFamily = AppleHigTypography.defaultFontFamily,
                        color = titleColor,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        platformStyle = AppleHigTypography.defaultPlatformStyle,
                        lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                    ),
                    cursorBrush = SolidColor(higColors.systemBlue),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * 退出登录二次确认 Sheet 内容 (Apple HIG Inset Grouped)
 */
@Composable
fun SheetLogoutConfirmContent(
    profile: UserProfile,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        AppleHigFillCard(
            isDark = isDark,
            cornerRadius = 16.dp,
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 10.dp
        ) {
            BasicText(
                text = "当前登录账号：${profile.name} (ID: ${profile.userId})",
                style = AppleHigTypography.body.copy(
                    color = higColors.label,
                    fontWeight = FontWeight.Medium
                )
            )
            BasicText(
                text = "退出登录后将返回初始状态，您的本地聊天记录与配置已自动加密备份在云端，随时可以重新登录恢复。",
                style = AppleHigTypography.subhead.copy(
                    color = higColors.secondaryLabel,
                    lineHeight = 20.sp
                )
            )
        }
        Spacer(Modifier.height(10.dp))
        BasicText(
            text = "点击右上角「✓」确认退出登录，或点击「×」取消。",
            style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * 注销账号高危二次确认 Sheet 内容 (Apple HIG Inset Grouped 警示风格)
 */
@Composable
fun SheetDeleteAccountConfirmContent(
    profile: UserProfile,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    val cardBg = if (isDark) Color(0xFF2A1515).copy(alpha = 0.85f)
                 else Color(0xFFFFF1F1).copy(alpha = 0.95f)
    val cardBorder = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.6f) else Color(0xFFFECACA)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardBg)
                .border(0.5.dp, cardBorder, RoundedCornerShape(12.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "警示",
                        tint = higColors.systemRed,
                        modifier = Modifier.size(22.dp)
                    )
                    BasicText(
                        text = "注意：此操作将永久删除账号及数据",
                        style = AppleHigTypography.headline.copy(
                            color = higColors.systemRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                BasicText(
                    text = "1. 用户名、唯一数字 ID (${profile.userId}) 将被永久释放；\n2. 全部会话记录、匹配历史、动态内容及点赞将立即彻底清空；\n3. 该过程不可撤销、不可找回，请务必审慎操作。",
                    style = AppleHigTypography.subhead.copy(
                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                        lineHeight = 22.sp
                    )
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        BasicText(
            text = "点击右上角「✓」确认注销账号并清空数据，或点击「×」放弃注销。",
            style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * 通用设置项详情面板 Sheet 内容 (Apple HIG Inset Grouped)
 */
@Composable
fun SheetSettingDetailContent(
    item: ProfileSettingItem,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        AppleHigFillCard(
            isDark = isDark,
            cornerRadius = 16.dp,
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 14.dp
        ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(item.iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        BasicText(
                            text = item.title,
                            style = AppleHigTypography.headline.copy(
                                color = higColors.label,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        BasicText(
                            text = item.subtitle,
                            style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = higColors.separator.copy(alpha = if (isDark) 0.35f else 0.20f)
                )

                BasicText(
                    text = item.summary,
                    style = AppleHigTypography.body.copy(
                        color = higColors.label,
                        lineHeight = 22.sp
                    )
                )
            }
        Spacer(Modifier.height(10.dp))
        BasicText(
            text = "点击右上角「✓」确认已知晓。",
            style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

/**
 * 隐私设置 Sheet 内容 (Apple HIG Inset Grouped)
 */
@Composable
fun SheetPrivacySettingsContent(
    privacy: MomentsPrivacyData,
    onPrivacyChange: (MomentsPrivacyData) -> Unit,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // 1. 动态与社交可见性
        AppleHigGroupedSection(
            title = "动态与社交可见性",
            footer = "精细化配置您的动态在公域大厅与广场流中的展示权限",
            isDark = isDark
        ) {
            AppleHigRow(
                title = "动态全站公开",
                subtitle = "关闭后仅好友与暗号房间内可见",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { privacy.momentsPublic == 1 },
                        onSelect = { onPrivacyChange(privacy.copy(momentsPublic = if (it) 1 else 0)) },
                        isDark = isDark
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            AppleHigRow(
                title = "允许捞取我的动态",
                subtitle = "允许其他用户在「捞动态」功能中捞到此内容",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { privacy.momentsFishable == 1 },
                        onSelect = { onPrivacyChange(privacy.copy(momentsFishable = if (it) 1 else 0)) },
                        isDark = isDark
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            AppleHigRow(
                title = "关注列表公开",
                subtitle = "在个人主页展示您关注的用户及粉丝",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { privacy.followListPublic == 1 },
                        onSelect = { onPrivacyChange(privacy.copy(followListPublic = if (it) 1 else 0)) },
                        isDark = isDark
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // 2. 聊天与私聊权限
        AppleHigGroupedSection(
            title = "私聊来源权限",
            footer = "控制他人通过何种途径向您发起私信会话",
            isDark = isDark
        ) {
            AppleHigRow(
                title = "允许全员私聊",
                subtitle = "允许任何未关注的陌生人直接发起私聊",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { privacy.dmPublic == 1 },
                        onSelect = { onPrivacyChange(privacy.copy(dmPublic = if (it) 1 else 0)) },
                        isDark = isDark
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            AppleHigRow(
                title = "允许从大厅发起私聊",
                subtitle = "公共大厅成员可点击您的头像发起会话",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { privacy.dmFromPublic == 1 },
                        onSelect = { onPrivacyChange(privacy.copy(dmFromPublic = if (it) 1 else 0)) },
                        isDark = isDark
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            AppleHigRow(
                title = "允许从动态发起私聊",
                subtitle = "浏览您动态的用户可直接进入单聊",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { privacy.dmFromMoment == 1 },
                        onSelect = { onPrivacyChange(privacy.copy(dmFromMoment = if (it) 1 else 0)) },
                        isDark = isDark
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            AppleHigRow(
                title = "允许暗号房间发起私聊",
                subtitle = "同一暗号房间成员可互相发起私聊",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { privacy.dmFromPrivate == 1 },
                        onSelect = { onPrivacyChange(privacy.copy(dmFromPrivate = if (it) 1 else 0)) },
                        isDark = isDark
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // 3. 状态显示
        AppleHigGroupedSection(
            title = "状态与活跃度",
            footer = "开启后名片将显示实时绿色在线状态徽标",
            isDark = isDark
        ) {
            AppleHigRow(
                title = "显示在线状态",
                subtitle = "对外展示您当前是否连接在线",
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { privacy.showOnlineStatus == 1 },
                        onSelect = { onPrivacyChange(privacy.copy(showOnlineStatus = if (it) 1 else 0)) },
                        isDark = isDark
                    )
                }
            )
        }
    }
}

/**
 * 查找用户 Sheet 内容 (Apple HIG Inset Grouped)
 */
@Composable
fun SheetSearchUsersContent(
    isDark: Boolean,
    onStartChat: (peerId: String, peerName: String, peerAvatar: String?) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val titleColor = higColors.label
    var loginNameText by remember { mutableStateOf("") }
    var userIdText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var searchResult by remember { mutableStateOf<UserLookupData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        // 搜索输入栏 (双栏：登录用户名 # 用户编号)
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = higColors.secondaryLabel,
                    modifier = Modifier.size(18.dp)
                )

                BasicTextField(
                    value = loginNameText,
                    onValueChange = { input ->
                        if (input.contains("#")) {
                            val parts = input.split("#")
                            loginNameText = parts.getOrNull(0) ?: ""
                            userIdText = parts.getOrNull(1) ?: userIdText
                        } else {
                            loginNameText = input
                        }
                    },
                    textStyle = AppleHigTypography.body.copy(color = titleColor),
                    cursorBrush = SolidColor(higColors.systemBlue),
                    modifier = Modifier.weight(1.2f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (loginNameText.isEmpty()) {
                            BasicText(
                                text = "登录名...",
                                style = AppleHigTypography.body.copy(color = higColors.tertiaryLabel)
                            )
                        }
                        innerTextField()
                    }
                )

                BasicText(
                    text = "#",
                    style = AppleHigTypography.body.copy(color = higColors.secondaryLabel, fontWeight = FontWeight.Bold)
                )

                BasicTextField(
                    value = userIdText,
                    onValueChange = { userIdText = it.filter { ch -> ch.isDigit() } },
                    textStyle = AppleHigTypography.body.copy(color = titleColor),
                    cursorBrush = SolidColor(higColors.systemBlue),
                    modifier = Modifier.weight(0.8f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (userIdText.isEmpty()) {
                            BasicText(
                                text = "编号如 1001",
                                style = AppleHigTypography.body.copy(color = higColors.tertiaryLabel)
                            )
                        }
                        innerTextField()
                    }
                )

                CapsuleGlassButton(
                    onClick = {
                        if (loginNameText.isBlank() && userIdText.isBlank()) return@CapsuleGlassButton
                        isLoading = true
                        errorMessage = null
                        searchResult = null
                        coroutineScope.launch {
                            val res = ZeroTalkClientManager.lookupUser(loginName = loginNameText, userId = userIdText)
                            isLoading = false
                            if (res.isSuccess) {
                                searchResult = res.getOrThrow()
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "未查找到该用户"
                            }
                        }
                    },
                    isDark = isDark,
                    modifier = Modifier.height(30.dp)
                ) {
                    BasicText(
                        text = if (isLoading) "查找中..." else "查找",
                        style = AppleHigTypography.footnote.copy(
                            color = higColors.tint,
                            fontWeight = FontWeight.Medium
                        ),
                        // 胶囊按钮本身没有内边距，不给就会把文字压到贴边
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 结果区域
        if (errorMessage != null) {
            AppleHigFillCard(
                isDark = isDark,
                modifier = Modifier.fillMaxWidth(),
                contentPaddingValues = PaddingValues(16.dp)
            ) {
                BasicText(
                    text = errorMessage ?: "",
                    style = AppleHigTypography.subhead.copy(color = higColors.destructive)
                )
            }
        }

        searchResult?.let { user ->
            AppleHigGroupedCard(
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 查找到的用户头像：真实头像优先，失败回落渐变 + 首字母
                        UserAvatar(
                            url = user.avatarUrl,
                            name = user.username,
                            size = 46.dp,
                            gradient = listOf(Color(0xFF38BDF8), Color(0xFF818CF8)),
                            fallbackIconSize = 26.dp
                        )

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BasicText(
                                text = user.username,
                                style = AppleHigTypography.headline.copy(
                                    color = titleColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GenderBadge(
                                    gender = user.gender,
                                    isDark = isDark,
                                    fontSize = 11.sp,
                                    horizontalPadding = 7.dp,
                                    verticalPadding = 2.dp
                                )
                                BasicText(
                                    text = "ID: ${user.userId}",
                                    style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel)
                                )
                                val userMbti = user.mbti
                                if (!userMbti.isNullOrBlank()) {
                                    BasicText(
                                        text = userMbti,
                                        style = AppleHigTypography.caption2.copy(color = higColors.tint)
                                    )
                                }
                            }
                        }
                    }

                    val userBio = user.bio
                    if (!userBio.isNullOrBlank()) {
                        BasicText(
                            text = userBio,
                            style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                        )
                    }

                    AppleHigDivider(isDark = isDark)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CapsuleGlassButton(
                            onClick = {
                                onStartChat(user.userId.toString(), user.username, user.avatarUrl)
                            },
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            BasicText(
                                text = "发起私聊",
                                style = AppleHigTypography.footnote.copy(
                                    color = higColors.tint,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        CapsuleGlassButton(
                            onClick = {
                                ZeroTalkClientManager.addBlockUser(user.userId) { success, err ->
                                    if (success) {
                                        onShowMessage("已将 ${user.username} 加入黑名单")
                                    } else {
                                        onShowMessage("拉黑失败: $err")
                                    }
                                }
                            },
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            BasicText(
                                text = "屏蔽拉黑",
                                style = AppleHigTypography.footnote.copy(
                                    color = higColors.destructive,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 我的黑名单 Sheet 内容 (Apple HIG Inset Grouped)
 */
@Composable
fun SheetBlockListContent(
    isDark: Boolean,
    onShowMessage: (String) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val titleColor = higColors.label
    val blockList by ZeroTalkClientManager.blockList.collectAsState()

    LaunchedEffect(Unit) {
        ZeroTalkClientManager.fetchBlockList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        if (blockList.isEmpty()) {
            AppleHigFillCard(
                isDark = isDark,
                modifier = Modifier.fillMaxWidth(),
                contentPaddingValues = PaddingValues(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "黑名单",
                        tint = higColors.tertiaryLabel,
                        modifier = Modifier.size(36.dp)
                    )
                    BasicText(
                        text = "暂无被拦截的黑名单用户",
                        style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                    )
                }
            }
        } else {
            AppleHigGroupedSection(
                title = "已屏蔽名单 (${blockList.size})",
                footer = "被屏蔽的用户无法查看您的动态或向您发起任何形式的会话",
                isDark = isDark
            ) {
                blockList.forEachIndexed { index, user ->
                    if (index > 0) {
                        AppleHigDivider(isDark = isDark, insetStart = 16.dp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(higColors.systemFill),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = user.username,
                                tint = higColors.secondaryLabel,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            BasicText(
                                text = user.username,
                                style = AppleHigTypography.body.copy(color = titleColor, fontWeight = FontWeight.Medium)
                            )
                            if (user.createdAt.isNotBlank()) {
                                BasicText(
                                    text = "拉黑时间: ${user.createdAt.take(10)}",
                                    style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel)
                                )
                            }
                        }

                        CapsuleGlassButton(
                            onClick = {
                                ZeroTalkClientManager.unblockUser(user.blockedId) { success, err ->
                                    if (success) {
                                        onShowMessage("已解除拉黑")
                                    } else {
                                        onShowMessage("操作失败: $err")
                                    }
                                }
                            },
                            isDark = isDark,
                            modifier = Modifier.height(28.dp)
                        ) {
                            BasicText(
                                text = "解除拉黑",
                                style = AppleHigTypography.caption1.copy(color = higColors.tint, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 我的举报 Sheet 内容（对齐官网 MyReportsView）
 *
 * 数据源 `GET /api/report/list`（`page` / `per_page=10` / `has_more`）：
 * - 顶部汇总「举报记录 · 共 N 条」；
 * - 每条记录：类型 + 理由标签、被举报对象标题、状态徽章、时间与来源（房间名 / 动态号 / 评论号）、
 *   动态内容预览、补充说明（空则「无补充说明」）、处理结果（空则「平台正在处理中，请耐心等待」）；
 * - 依据 `has_more` 展示底部「加载更多」。
 *
 * 类型 / 理由 / 状态 / 结果文案**全部取服务端下发的 `*_text`**，客户端仅在缺省时兜底。
 */
@Composable
fun SheetMyReportsContent(
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    val reportList by ZeroTalkClientManager.reportList.collectAsState()
    val reportTotal by ZeroTalkClientManager.reportTotal.collectAsState()
    val hasMore by ZeroTalkClientManager.reportHasMore.collectAsState()
    val loadingMore by ZeroTalkClientManager.reportLoadingMore.collectAsState()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        ZeroTalkClientManager.fetchReportList()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        when {
            isLoading && reportList.isEmpty() -> {
                AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(),
                    contentPaddingValues = PaddingValues(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BasicText(
                            text = "正在加载举报记录…",
                            style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                        )
                    }
                }
            }

            reportList.isEmpty() -> {
                AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(),
                    contentPaddingValues = PaddingValues(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = "举报记录",
                            tint = higColors.tertiaryLabel,
                            modifier = Modifier.size(36.dp)
                        )
                        BasicText(
                            text = "暂无举报记录",
                            style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                        )
                        BasicText(
                            text = "在聊天或动态中遇到违规内容时，可通过举报功能提交",
                            style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                        )
                    }
                }
            }

            else -> {
                // 顶部汇总（官网「举报记录 / 共 N 条」）
                AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(),
                    contentPaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    itemSpacing = 4.dp
                ) {
                    BasicText(
                        text = "举报记录",
                        style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                    )
                    BasicText(
                        text = "共 $reportTotal 条",
                        style = AppleHigTypography.title3.copy(color = higColors.label)
                    )
                }

                Spacer(Modifier.height(12.dp))

                reportList.forEach { record ->
                    ReportRecordCard(record = record, isDark = isDark)
                    Spacer(Modifier.height(12.dp))
                }

                if (hasMore) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CapsuleGlassButton(
                            onClick = {
                                if (!loadingMore) {
                                    scope.launch { ZeroTalkClientManager.loadMoreReports() }
                                }
                            },
                            isDark = isDark,
                            enabled = !loadingMore
                        ) {
                            BasicText(
                                text = if (loadingMore) "加载中…" else "加载更多",
                                style = AppleHigTypography.subhead.copy(
                                    color = higColors.tint,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单条举报记录卡片（对齐官网 `record-item`）
 *
 * 结构：标签行（类型 / 理由 / 状态）→ 被举报对象标题 → 元信息 → 动态预览 → 补充说明 → 处理结果。
 */
@Composable
private fun ReportRecordCard(
    record: ReportItemDto,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    val (typeBg, typeFg) = reportTypeColors(record.type)
    val (statusBg, statusFg) = reportStatusColors(record.status)

    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier.fillMaxWidth(),
        contentPaddingValues = PaddingValues(16.dp),
        itemSpacing = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportTag(text = record.typeLabel, background = typeBg, foreground = typeFg)
            ReportTag(
                text = record.reasonText.ifBlank { record.reason.ifBlank { "未填写理由" } },
                background = higColors.quaternarySystemFill,
                foreground = higColors.secondaryLabel
            )
            Spacer(Modifier.weight(1f))
            ReportTag(
                text = record.statusLabel,
                background = statusBg,
                foreground = statusFg,
                emphasized = true
            )
        }

        BasicText(
            text = record.headline,
            style = AppleHigTypography.body.copy(
                color = higColors.label,
                fontWeight = FontWeight.SemiBold
            )
        )

        // 元信息：时间 · 房间名 / 动态号 / 评论号
        val meta = buildList {
            formatReportTime(record.createdAt).takeIf { it.isNotBlank() }?.let { add(it) }
            record.roomName?.takeIf { it.isNotBlank() }?.let { add(it) }
            record.momentId?.takeIf { it > 0 }?.let { add("动态 #$it") }
            record.commentId?.takeIf { it > 0 }?.let { add("评论 #$it") }
        }.joinToString(" · ")
        if (meta.isNotBlank()) {
            BasicText(
                text = meta,
                style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
            )
        }

        // 动态内容预览
        record.momentPreview?.takeIf { it.isNotBlank() }?.let { preview ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(higColors.quaternarySystemFill)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                BasicText(
                    text = preview,
                    style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                )
            }
        }

        BasicText(
            text = record.description?.takeIf { it.isNotBlank() } ?: "无补充说明",
            style = AppleHigTypography.subhead.copy(
                color = if (record.description.isNullOrBlank()) higColors.tertiaryLabel else higColors.secondaryLabel
            )
        )

        // 处理结果（官网 result-box）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(statusBg)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            BasicText(
                text = "处理状态",
                style = AppleHigTypography.caption2.copy(color = statusFg)
            )
            BasicText(
                text = record.resultText?.takeIf { it.isNotBlank() } ?: "平台正在处理中，请耐心等待",
                style = AppleHigTypography.subhead.copy(color = higColors.label)
            )
            if (record.status != "pending") {
                formatReportTime(record.reviewedAt).takeIf { it.isNotBlank() }?.let { reviewed ->
                    BasicText(
                        text = "处理于 $reviewed",
                        style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
                    )
                }
            }
        }
    }
}

/** 举报卡片上的小标签 */
@Composable
private fun ReportTag(
    text: String,
    background: Color,
    foreground: Color,
    emphasized: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        BasicText(
            text = text,
            style = AppleHigTypography.caption2.copy(
                color = foreground,
                fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal
            )
        )
    }
}

/** 举报类型配色（对齐官网 `chat` 与 动态/评论 两种色调） */
private fun reportTypeColors(type: String): Pair<Color, Color> =
    if (type == "chat") {
        Color(0xFF6366F1).copy(alpha = 0.15f) to Color(0xFF6366F1)
    } else {
        Color(0xFF06B6D4).copy(alpha = 0.15f) to Color(0xFF06B6D4)
    }

/** 举报状态配色（对齐官网 pending / approved / rejected / processed 四态） */
private fun reportStatusColors(status: String): Pair<Color, Color> = when (status) {
    "approved" -> Color(0xFF10B981).copy(alpha = 0.15f) to Color(0xFF10B981)
    "rejected" -> Color(0xFFEF4444).copy(alpha = 0.15f) to Color(0xFFEF4444)
    "processed" -> Color(0xFF3B82F6).copy(alpha = 0.15f) to Color(0xFF3B82F6)
    else -> Color(0xFFF59E0B).copy(alpha = 0.15f) to Color(0xFFF59E0B)
}

/** 举报记录时间：官网展示到分钟（`YYYY-MM-DD HH:mm`） */
private fun formatReportTime(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return ""
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(value)
        if (parsed == null) value else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(parsed)
    } catch (_: Exception) {
        value
    }
}
