package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.data.log.AppDiagnostics
import top.lanxint.zerotalk.data.network.NeteaseBindingData
import top.lanxint.zerotalk.data.network.PenaltyAppealDto
import top.lanxint.zerotalk.data.network.PenaltyOptionDto
import top.lanxint.zerotalk.data.network.SecurityDeviceDto
import top.lanxint.zerotalk.data.network.SecurityLoginLogDto
import top.lanxint.zerotalk.data.repository.ClientStatus
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.components.AppleHigGroupedSection
import top.lanxint.zerotalk.ui.components.AppleHigRow
import top.lanxint.zerotalk.ui.components.AppleHigRowAccessory
import top.lanxint.zerotalk.ui.components.AppleModalBottomSheet
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.utils.decodeByteArrayToImageBitmap
import top.lanxint.zerotalk.ui.utils.parseQrSvgDataUri
import top.lanxint.zerotalk.ui.utils.QrCodeFromSvg
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
import top.lanxint.zerotalk.ui.components.LiquidSegmentedControl
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.data.network.ContactBootstrapData
import top.lanxint.zerotalk.data.network.ContactConfigDto
import top.lanxint.zerotalk.data.network.DonateData
import top.lanxint.zerotalk.data.network.DonorDto
import top.lanxint.zerotalk.data.network.FeedbackRecordDto
import top.lanxint.zerotalk.ui.components.LiquidButton
import top.lanxint.zerotalk.ui.components.SelectableChip
import top.lanxint.zerotalk.ui.utils.rememberPhotoPickerLauncher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import zerotalk.composeapp.generated.resources.Res
import zerotalk.composeapp.generated.resources.donate_app_alipay
import zerotalk.composeapp.generated.resources.donate_app_wechat
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ============================================================
// 「我的」页面四个功能的 Sheet 内容
//  1) 安全中心   SheetSecurityCenterContent
//  2) 修改密码   SheetChangePasswordContent
//  3) 处罚减免   SheetPenaltyReliefContent
//  4) 网易云绑定 SheetNeteaseBindContent
//
// 文案与取值全部对齐官方实现（SecurityCenterView / PenaltyAppealView / NeteaseBindView/ music 模块）。
// 视觉一律消费 AppleHigColors / AppleHigTypography 语义 token，不写死字号与颜色。
// ============================================================

// ------------------------------------------------------------
// 1) 安全中心
// ------------------------------------------------------------

/** 登录历史事件筛选项（取值与中文文案均取自官方 select） */
private val SECURITY_EVENT_FILTERS: List<Pair<String, String>> = listOf(
    "" to "全部事件",
    "login_success" to "登录成功",
    "login_failed" to "登录失败",
    "ip_anomaly" to "异常 IP",
    "device_new" to "新增设备",
    "device_revoke" to "设备注销",
    "force_logout" to "强制下线"
)

/**
 * 安全中心 Sheet 内容：设备管理 + 登录历史（两个分页 Tab）
 *
 * @param onShowMessage 顶部通知（成功/失败提示）
 * @param onLoggedOut 本机被注销后需要关闭面板（此时已完成退出登录）
 */
@Composable
fun SheetSecurityCenterContent(
    isDark: Boolean,
    onShowMessage: (String) -> Unit,
    onLoggedOut: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val devices by ZeroTalkClientManager.securityDevices.collectAsState()
    val devicesLoading by ZeroTalkClientManager.securityDevicesLoading.collectAsState()
    val logs by ZeroTalkClientManager.securityLoginLogs.collectAsState()
    val logsTotal by ZeroTalkClientManager.securityLoginTotal.collectAsState()
    val logsTableReady by ZeroTalkClientManager.securityLoginTableReady.collectAsState()
    val logsLoading by ZeroTalkClientManager.securityLoginLoading.collectAsState()
    val eventType by ZeroTalkClientManager.securityLoginEventType.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var pendingRevoke by remember { mutableStateOf<SecurityDeviceDto?>(null) }
    var revokingDeviceId by remember { mutableStateOf("") }
    var showRevokeOthersConfirm by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        ZeroTalkClientManager.loadSecurityDevices()
    }

    // 切到「登录历史」时按当前筛选拉取第一页
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && logs.isEmpty() && !logsLoading) {
            ZeroTalkClientManager.loadSecurityLoginHistory(reset = true)
        }
    }

    // 滚动到底部附近继续加载（对齐官方 IntersectionObserver + rootMargin 的语义）：
    // 只在「已加载条数 / 滚动位置」发生实际变化时判断一次，避免每帧重启协程
    LaunchedEffect(selectedTab) {
        if (selectedTab != 1) return@LaunchedEffect
        snapshotFlow {
            Triple(
                ZeroTalkClientManager.securityLoginLogs.value.size,
                scrollState.value,
                scrollState.maxValue
            )
        }
            .distinctUntilChanged()
            .collect { (loaded, scrollValue, maxValue) ->
                val nearBottom = maxValue > 0 && scrollValue >= maxValue - 240
                val total = ZeroTalkClientManager.securityLoginTotal.value
                if (nearBottom && !ZeroTalkClientManager.securityLoginLoading.value && loaded < total) {
                    ZeroTalkClientManager.loadSecurityLoginHistory(reset = false)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 20.dp)
    ) {
        LiquidSegmentedControl(
            options = listOf("设备管理", "登录历史"),
            selectedIndex = selectedTab,
            onOptionSelect = { selectedTab = it },
            outerHeight = 34.dp,
            innerHeight = 28.dp,
            isDark = isDark,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        if (selectedTab == 0) {
            when {
                devicesLoading && devices.isEmpty() -> SecurityStateCard(
                    isDark = isDark,
                    icon = Icons.Default.Security,
                    title = "正在加载安全信息...",
                    subtitle = ""
                )

                devices.isEmpty() -> SecurityStateCard(
                    isDark = isDark,
                    icon = Icons.Default.Devices,
                    title = "暂无设备记录",
                    subtitle = "连接成功后，活跃设备将显示在这里"
                )

                else -> {
                    // 设备数量（官方展示在「设备管理」Tab 的数量徽标上）
                    BasicText(
                        text = "登录设备 (${devices.size})",
                        style = AppleHigTypography.groupedSectionHeader.copy(color = higColors.secondaryLabel),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    devices.forEach { device ->
                        SecurityDeviceCard(
                            device = device,
                            isDark = isDark,
                            revoking = revokingDeviceId == device.deviceId,
                            onRevoke = { pendingRevoke = device }
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // 官方 API 有「注销其他所有设备」但页面未暴露入口，这里补一个
                    if (devices.any { !it.isCurrent }) {
                        SecondaryActionRow(
                            text = "注销其他所有设备",
                            isDark = isDark,
                            destructive = true,
                            onClick = { showRevokeOthersConfirm = true }
                        )
                    }
                }
            }
        } else {
            // 事件筛选（官方为 select，这里用可横向滚动的胶囊筛选条保持 HIG 观感）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SECURITY_EVENT_FILTERS.forEach { (value, label) ->
                    val selected = eventType == value
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (selected) higColors.tint else higColors.systemFill)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !logsLoading || selected,
                                onClick = {
                                    if (!selected) {
                                        ZeroTalkClientManager.loadSecurityLoginHistory(
                                            reset = true,
                                            eventType = value
                                        )
                                    }
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        BasicText(
                            text = label,
                            style = AppleHigTypography.footnote.copy(
                                color = if (selected) Color.White else higColors.secondaryLabel,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                !logsTableReady -> SecurityStateCard(
                    isDark = isDark,
                    icon = Icons.Default.Security,
                    title = "暂无历史记录",
                    subtitle = "安全日志表尚未就绪"
                )

                logs.isEmpty() && logsLoading -> SecurityStateCard(
                    isDark = isDark,
                    icon = Icons.Default.Security,
                    title = "加载中...",
                    subtitle = ""
                )

                logs.isEmpty() -> SecurityStateCard(
                    isDark = isDark,
                    icon = Icons.Default.Security,
                    title = "暂无记录",
                    subtitle = "登录或安全事件发生后将在此显示"
                )

                else -> {
                    AppleHigFillCard(
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth(),
                        contentPaddingValues = PaddingValues(0.dp)
                    ) {
                        logs.forEachIndexed { index, log ->
                            if (index > 0) {
                                AppleHigDivider(isDark = isDark, insetStart = 16.dp)
                            }
                            SecurityLogRow(log = log, isDark = isDark)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    BasicText(
                        text = when {
                            logsLoading -> "加载中..."
                            logs.size >= logsTotal && logsTotal > 0 -> "已加载全部"
                            else -> ""
                        },
                        style = AppleHigTypography.caption1.copy(
                            color = higColors.tertiaryLabel,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // 注销设备二次确认
    pendingRevoke?.let { device ->
        val isCurrent = device.isCurrent
        AlertDialog(
            onDismissRequest = { if (revokingDeviceId.isBlank()) pendingRevoke = null },
            title = {
                BasicText(
                    text = if (isCurrent) "注销当前设备？" else "注销该设备？",
                    style = AppleHigTypography.headline.copy(color = higColors.label)
                )
            },
            text = {
                BasicText(
                    text = if (isCurrent) {
                        "将立即断开连接并退出登录，需重新登录后才能继续使用。"
                    } else {
                        "该设备将被强制下线，且无法继续使用当前会话。"
                    },
                    style = AppleHigTypography.body.copy(color = higColors.secondaryLabel)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = revokingDeviceId.isBlank(),
                    onClick = {
                        val target = device
                        revokingDeviceId = target.deviceId
                        ZeroTalkClientManager.revokeSecurityDevice(target.deviceId) { success, wasCurrent, err ->
                            revokingDeviceId = ""
                            pendingRevoke = null
                            when {
                                !success -> onShowMessage(err ?: "注销失败")
                                wasCurrent -> {
                                    onShowMessage("当前设备已注销")
                                    onLoggedOut()
                                }
                                else -> onShowMessage("已注销该设备，对方将强制下线")
                            }
                        }
                    }
                ) {
                    BasicText(
                        text = if (revokingDeviceId.isNotBlank()) "处理中..." else "确认注销",
                        style = AppleHigTypography.body.copy(color = higColors.destructive)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = revokingDeviceId.isBlank(),
                    onClick = { pendingRevoke = null }
                ) {
                    BasicText("取消", style = AppleHigTypography.body.copy(color = higColors.secondaryLabel))
                }
            }
        )
    }

    // 注销其他所有设备二次确认
    if (showRevokeOthersConfirm) {
        AlertDialog(
            onDismissRequest = { showRevokeOthersConfirm = false },
            title = {
                BasicText("注销其他所有设备？", style = AppleHigTypography.headline.copy(color = higColors.label))
            },
            text = {
                BasicText(
                    text = "其他设备将被强制下线，且无法继续使用当前会话。本机登录状态不受影响。",
                    style = AppleHigTypography.body.copy(color = higColors.secondaryLabel)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRevokeOthersConfirm = false
                    ZeroTalkClientManager.revokeOtherSecurityDevices { success, err ->
                        onShowMessage(if (success) "已注销其他所有设备" else (err ?: "注销失败"))
                    }
                }) {
                    BasicText("确认注销", style = AppleHigTypography.body.copy(color = higColors.destructive))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeOthersConfirm = false }) {
                    BasicText("取消", style = AppleHigTypography.body.copy(color = higColors.secondaryLabel))
                }
            }
        )
    }
}

/**
 * 单台登录设备卡片（标题 / 平台 / IP / 活跃 / ID + 注销按钮）
 */
@Composable
private fun SecurityDeviceCard(
    device: SecurityDeviceDto,
    isDark: Boolean,
    revoking: Boolean,
    onRevoke: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (device.isCurrent) {
                    Modifier.border(1.dp, higColors.tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ),
        contentPaddingValues = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (device.isCurrent) higColors.tint.copy(alpha = 0.14f) else higColors.secondarySystemFill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "设备",
                    tint = if (device.isCurrent) higColors.tint else higColors.secondaryLabel,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicText(
                        text = securityDeviceTitle(device),
                        style = AppleHigTypography.groupedRowTitle.copy(
                            color = higColors.label,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (device.isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(higColors.tint.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                text = "本机",
                                style = AppleHigTypography.caption2.copy(
                                    color = higColors.tint,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
                val platformText = securityDevicePlatform(device)
                if (platformText.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = platformText,
                        style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        SecurityFactRow(
            label = "IP",
            value = device.ip.ifBlank { "未知" } +
                if (device.location.isNotBlank()) " · ${device.location}" else "",
            isDark = isDark
        )
        SecurityFactRow(label = "活跃", value = formatSecurityDeviceSeen(device.lastSeenAt), isDark = isDark)
        SecurityFactRow(label = "ID", value = truncateDeviceId(device.deviceId), isDark = isDark)

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(higColors.destructive.copy(alpha = 0.12f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !revoking,
                    onClick = onRevoke
                ),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = when {
                    revoking -> "处理中..."
                    device.isCurrent -> "注销并退出"
                    else -> "注销此设备"
                },
                style = AppleHigTypography.footnote.copy(
                    color = higColors.destructive,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/** 设备信息行（左标签 + 右值） */
@Composable
private fun SecurityFactRow(label: String, value: String, isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        BasicText(
            text = label,
            style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel),
            modifier = Modifier.width(44.dp)
        )
        BasicText(
            text = value,
            style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel),
            modifier = Modifier.weight(1f)
        )
    }
}

/** 登录历史单行：事件类型 + 时间 + IP/地区 */
@Composable
private fun SecurityLogRow(log: SecurityLoginLogDto, isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
    val eventColor = when (log.eventType) {
        "ip_anomaly", "login_failed" -> higColors.systemOrange
        "login_success", "device_new" -> higColors.systemGreen
        "device_revoke", "force_logout" -> higColors.destructive
        else -> higColors.secondaryLabel
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    text = log.eventTypeText?.takeIf { it.isNotBlank() } ?: log.eventType,
                    style = AppleHigTypography.footnote.copy(color = eventColor, fontWeight = FontWeight.Medium)
                )
                if (log.isAnomaly) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "异常",
                        tint = higColors.systemOrange,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            BasicText(
                text = formatSecurityDateTime(log.createdAt),
                style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
            )
        }

        BasicText(
            text = log.ip.ifBlank { "未知 IP" } +
                if (log.location.isNotBlank()) " · ${log.location}" else "",
            style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
        )
    }
}

/** 通用空态 / 状态卡片 */
@Composable
private fun SecurityStateCard(
    isDark: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val higColors = AppleHigColors.colors(isDark)
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
                imageVector = icon,
                contentDescription = title,
                tint = higColors.tertiaryLabel,
                modifier = Modifier.size(32.dp)
            )
            BasicText(
                text = title,
                style = AppleHigTypography.subhead.copy(
                    color = higColors.secondaryLabel,
                    textAlign = TextAlign.Center
                )
            )
            if (subtitle.isNotBlank()) {
                BasicText(
                    text = subtitle,
                    style = AppleHigTypography.caption1.copy(
                        color = higColors.tertiaryLabel,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

/** 次级整宽动作行（用于「注销其他所有设备」等） */
@Composable
private fun SecondaryActionRow(
    text: String,
    isDark: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(higColors.systemFill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = AppleHigTypography.footnote.copy(
                color = if (destructive) higColors.destructive else higColors.tint,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/** 设备标题：browser → device_name 中「 · 」之后 → platform · browser → 当前设备/其他设备 */
private fun securityDeviceTitle(device: SecurityDeviceDto): String {
    val browser = device.browser.trim()
    if (browser.isNotEmpty()) return browser
    val name = device.deviceName.trim()
    if (name.isNotEmpty() && name != "Unknown Device") {
        val separatorIndex = name.indexOf(" · ")
        if (separatorIndex >= 0) {
            val tail = name.substring(separatorIndex + 3).trim()
            if (tail.isNotEmpty()) return tail
        }
        return name
    }
    val combined = listOf(device.platform, device.browser).filter { it.isNotBlank() }.joinToString(" · ")
    return combined.ifBlank { if (device.isCurrent) "当前设备" else "其他设备" }
}

/** 平台行：platform 非 Web 时用它，否则取 device_name 的第一段 */
private fun securityDevicePlatform(device: SecurityDeviceDto): String {
    val platform = device.platform.trim()
    if (platform.isNotEmpty() && platform != "Web") return platform
    val name = device.deviceName.trim()
    if (name.contains(" · ")) {
        val head = name.substringBefore(" · ").trim()
        if (head.isNotEmpty() && head != "Web") return head
    }
    return ""
}

/** 设备 ID 展示：超过 16 字符显示「前 8…后 8」 */
private fun truncateDeviceId(deviceId: String): String {
    val value = deviceId.trim()
    if (value.isEmpty()) return "未知"
    return if (value.length <= 16) value else "${value.take(8)}…${value.takeLast(8)}"
}

/**
 * 设备「活跃」时间。
 *
 * 官方 SecurityCenterView 的取值与格式化（tmp/assets/SecurityCenterView-*.js）：
 * ```
 * oe = a => { if (!a) return "未知"; const e = new Date(a * 1e3);
 *             return Number.isNaN(e.getTime()) ? "未知" : e.toLocaleString() }
 * ```
 * 即 `last_seen_at` 是 **Unix 秒**，且 JS 的 `*` 会把**任何可数字化字面量**
 * （`1759238400`、`1759238400.0`、`1.7592384e9`、纯数字字符串）隐式转成 number。
 *
 * 该字段经 Gson 落进 [SecurityDeviceDto.lastSeenAt]（String），实际可能是：
 *  - `"1759238400"`：整数，旧实现能处理；
 *  - `"1759238400.0"` / `"1.7592384e9"`：PHP `json_encode` 对浮点时间戳的输出。
 *    旧实现只做 `toLongOrNull()`，这类值直接失败并落到「原样回显」分支，
 *    卡片上于是出现裸时间戳 —— 这正是「活跃时间显示不对」的根因；
 *  - 少数情况下是 `"2025-09-30 12:00:00"` 这类时间字符串（DTO 注释声明要兼容）。
 *
 * 这里按官方语义统一处理：能数字化的都按 Unix 秒换算成本地时区展示，
 * 否则再按时间字符串解析，两者都失败才回退原文。
 */
private fun formatSecurityDeviceSeen(raw: String): String {
    val value = raw.trim()
    if (value.isEmpty()) return "未知"
    val seconds = value.toJsNumberOrNull()
    if (seconds != null) {
        // 官方 `if (!a) return "未知"`：0 表示无记录
        if (seconds <= 0.0) return "未知"
        val millis = seconds * 1000.0
        // 官方 new Date(...) 的有效区间是 ±8.64e15 毫秒，超出即 Invalid Date →「未知」
        if (!millis.isFinite() || millis < -8.64e15 || millis > 8.64e15) return "未知"
        return try {
            SimpleDateFormat(SECURITY_DISPLAY_PATTERN, Locale.getDefault()).format(Date(millis.toLong()))
        } catch (_: Exception) {
            "未知"
        }
    }
    parseSecurityDateTime(value)?.let { return it }
    // 数字形态却无法解释为 Unix 秒（Infinity / NaN 等）：官方 new Date(...) 只会得到
    // Invalid Date →「未知」，不要回显裸值
    if (value.toDoubleOrNull() != null) return "未知"
    return value
}

/**
 * 按 JS `Number()` 的十进制语义解析（对齐官方 `a * 1e3` 的隐式转换）：
 * 接受 `[+-]?digits[.digits][e±digits]`；拒绝 `Infinity` / `NaN` / 十六进制等
 * `Double.parseDouble` 会放行、但时间戳场景不存在的写法。
 */
private fun String.toJsNumberOrNull(): Double? =
    if (JS_DECIMAL_NUMBER_REGEX.matches(this)) toDoubleOrNull() else null

private val JS_DECIMAL_NUMBER_REGEX = Regex("^[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?$")

/** 时间展示格式（官方为 `toLocaleString()`，App 侧统一为等价的本地时间文本） */
private const val SECURITY_DISPLAY_PATTERN = "yyyy-MM-dd HH:mm"

/** 时间字符串尾部时区偏移：`+08:00` / `+0800` */
private val SECURITY_UTC_OFFSET_REGEX = Regex("([+-])(\\d{2}):?(\\d{2})$")

/** 时间字符串可接受的日期时间形态（按「精确 → 宽松」顺序尝试） */
private val SECURITY_DATE_PATTERNS = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd'T'HH:mm",
    "yyyy-MM-dd"
)

/**
 * 时间字符串 → 本地时区「yyyy-MM-dd HH:mm」；解析失败返回 null，由调用方决定回退文案。
 *
 * 官方 `ie(created_at)`：`new Date(a.replace(" ", "T"))`，解析失败原样回显。
 * 浏览器 `Date` 的时区语义：无时区标记按本地时间，尾部 `Z` 按 UTC，
 * 尾部 `±HH:mm` / `±HHmm` 按该偏移。旧实现直接 `removeSuffix("Z")`，
 * 把 UTC 当成本地时间，东八区会整整差 8 小时。
 */
private fun parseSecurityDateTime(raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty()) return null

    // 与官方一致：只把首个空格换成 T，交给 Date 语义解析
    var text = value.replaceFirst(' ', 'T')

    var zone: TimeZone? = null
    if (text.endsWith("Z") || text.endsWith("z")) {
        zone = TimeZone.getTimeZone("UTC")
        text = text.dropLast(1)
    } else {
        val offset = SECURITY_UTC_OFFSET_REGEX.find(text)
        // 只在「日期时间」之后紧跟偏移时才按偏移解析，避免误伤 "2025-09-30" 这类纯日期
        if (offset != null && text.substring(0, offset.range.first).contains('T')) {
            val sign = offset.groupValues[1]
            val hours = offset.groupValues[2]
            val minutes = offset.groupValues[3]
            zone = TimeZone.getTimeZone("GMT$sign$hours:$minutes")
            text = text.substring(0, offset.range.first)
        }
    }

    SECURITY_DATE_PATTERNS.forEach { pattern ->
        try {
            val formatter = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                isLenient = false
                zone?.let { timeZone = it }
            }
            val parsed = formatter.parse(text) ?: return@forEach
            return SimpleDateFormat(SECURITY_DISPLAY_PATTERN, Locale.getDefault()).format(parsed)
        } catch (_: Exception) {
            // 尝试下一个模式
        }
    }
    return null
}

/**
 * 时间字符串 → 本地时区「yyyy-MM-dd HH:mm」，解析失败原样回显（官方 `ie` 的回退语义）。
 */
private fun formatSecurityDateTime(raw: String): String {
    val value = raw.trim()
    if (value.isEmpty()) return "未知"
    return parseSecurityDateTime(value) ?: value
}

// ------------------------------------------------------------
// 2) 修改密码
// ------------------------------------------------------------

/**
 * 修改密码 Sheet 内容（复用 POST /profile/update）
 *
 * 昵称与 QQ 自动回填当前账号资料，避免服务端把昵称覆盖为空。
 */
@Composable
fun SheetChangePasswordContent(
    isDark: Boolean,
    onShowMessage: (String) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    val canSubmit = currentPassword.isNotBlank() &&
        newPassword.isNotBlank() &&
        confirmPassword.isNotBlank() &&
        !submitting

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 4.dp
        ) {
            BasicText(
                text = "密码修改",
                style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
            )

            PasswordField(
                label = "当前密码",
                value = currentPassword,
                onValueChange = { currentPassword = it },
                placeholder = "请输入当前登录密码",
                isDark = isDark
            )
            AppleHigDivider(isDark = isDark, insetStart = 0.dp)
            PasswordField(
                label = "新密码",
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = "至少 6 位，建议字母与数字组合",
                isDark = isDark
            )
            AppleHigDivider(isDark = isDark, insetStart = 0.dp)
            PasswordField(
                label = "确认新密码",
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "请再次输入新密码",
                isDark = isDark
            )
        }

        Spacer(Modifier.height(10.dp))

        BasicText(
            text = "修改成功后请使用新密码重新登录其他设备。",
            style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(Modifier.height(16.dp))

        PrimaryActionButton(
            text = if (submitting) "提交中…" else "确认修改",
            enabled = canSubmit,
            isDark = isDark,
            onClick = {
                // 前端校验：新密码至少 6 位、两次输入一致
                val trimmedNew = newPassword.trim()
                when {
                    trimmedNew.length < 6 -> onShowMessage("新密码至少 6 位")
                    trimmedNew != confirmPassword.trim() -> onShowMessage("两次输入的新密码不一致")
                    else -> {
                        submitting = true
                        ZeroTalkClientManager.changePassword(
                            currentPassword = currentPassword,
                            newPassword = trimmedNew,
                            confirmPassword = confirmPassword.trim()
                        ) { success, err ->
                            submitting = false
                            if (success) {
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                onShowMessage("密码已更新")
                            } else {
                                // 服务端 msg 原样提示（例如「当前密码不正确」）
                                onShowMessage(err ?: "密码修改失败")
                            }
                        }
                    }
                }
            }
        )
    }
}

/** 密码输入行（左右标签 + 密码框） */
@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = label,
            style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel),
            modifier = Modifier.width(72.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AppleHigTypography.groupedRowTitle.copy(color = higColors.label),
            cursorBrush = SolidColor(higColors.systemBlue),
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.weight(1f),
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

/** 主行动按钮（tint 底 + 白字，禁用态置灰） */
@Composable
private fun PrimaryActionButton(
    text: String,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) higColors.tint else higColors.systemFill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = AppleHigTypography.headline.copy(
                color = if (enabled) Color.White else higColors.tertiaryLabel
            )
        )
    }
}

// ------------------------------------------------------------
// 3) 处罚减免
// ------------------------------------------------------------

/** 手写检讨字数要求：1 项 200 字，n≥2 项 150 + 200*(n-1)，0 项 0 */
private fun penaltyRequiredWords(selectedCount: Int): Int {
    val count = selectedCount.coerceAtLeast(0)
    return when {
        count <= 0 -> 0
        count == 1 -> 200
        else -> 150 + 200 * (count - 1)
    }
}

/** 最多可上传张数：官方 min(9, 选中项数)，未选时至少 1 */
private fun penaltyMaxUpload(selectedCount: Int): Int =
    if (selectedCount <= 0) 1 else minOf(9, selectedCount)

private const val PENALTY_MAX_BYTES = 5 * 1024 * 1024

/** 通过文件头识别图片类型（相册选择器只给了字节流，没有 mime） */
private fun sniffImageContentType(bytes: ByteArray): String? {
    if (bytes.size < 12) return null
    fun at(index: Int): Int = bytes[index].toInt() and 0xFF
    return when {
        at(0) == 0xFF && at(1) == 0xD8 -> "image/jpeg"
        at(0) == 0x89 && at(1) == 0x50 && at(2) == 0x4E && at(3) == 0x47 -> "image/png"
        at(0) == 0x47 && at(1) == 0x49 && at(2) == 0x46 -> "image/gif"
        at(0) == 0x52 && at(1) == 0x49 && at(2) == 0x46 && at(3) == 0x46 &&
            at(8) == 0x57 && at(9) == 0x45 && at(10) == 0x42 && at(11) == 0x50 -> "image/webp"
        else -> null
    }
}

private fun imageExtensionFor(contentType: String): String = when (contentType) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    else -> "jpg"
}

/**
 * 已有的手写检讨证据（含旧版 evidence_map 兼容）
 */
private data class PenaltyEvidence(val label: String, val url: String)

/** 展开官方「我的申请」里的证据列表：优先 evidence_urls，其次平铺 evidence_map 的值并去重 */
private fun penaltyEvidenceList(appeal: PenaltyAppealDto): List<PenaltyEvidence> {
    val direct = appeal.evidenceUrls.orEmpty().filter { it.isNotBlank() }
    val flattened = if (direct.isNotEmpty()) {
        emptyList()
    } else {
        appeal.evidenceMap.orEmpty().values.flatMap { element ->
            when {
                element.isJsonNull -> emptyList()
                element.isJsonPrimitive -> element.asString.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
                element.isJsonArray -> element.asJsonArray.mapNotNull { item ->
                    if (item.isJsonPrimitive) item.asString.takeIf { it.isNotBlank() } else null
                }
                else -> emptyList()
            }
        }
    }
    return direct.ifEmpty { flattened }.distinct().mapIndexed { index, url ->
        PenaltyEvidence(label = if (index == 0) "手写检讨" else "手写检讨 ${index + 1}", url = url)
    }
}

/**
 * 处罚减免 Sheet 内容（手写检讨申请，账号终身仅一次）
 */
@Composable
fun SheetPenaltyReliefContent(
    isDark: Boolean,
    onShowMessage: (String) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val bootstrap by ZeroTalkClientManager.penaltyBootstrap.collectAsState()
    val loading by ZeroTalkClientManager.penaltyLoading.collectAsState()

    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var evidenceUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var ackGuidelines by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }

    val activePenalties = bootstrap?.activePenalties.orEmpty()
    val guidelines = bootstrap?.guidelines.orEmpty()
    val appeal = bootstrap?.appeal
    val canApply = bootstrap?.canApply == true
    val maxUpload = penaltyMaxUpload(selectedKeys.size)
    val requiredWords = penaltyRequiredWords(selectedKeys.size)

    // 已选项目数变少时同步裁剪已上传图片（官方 maxUpload 收紧后截断）
    LaunchedEffect(maxUpload) {
        if (evidenceUrls.size > maxUpload) evidenceUrls = evidenceUrls.take(maxUpload)
    }

    LaunchedEffect(Unit) {
        ZeroTalkClientManager.loadPenaltyAppeal()
    }

    val photoPicker = rememberPhotoPickerLauncher(
        maxItems = (maxUpload - evidenceUrls.size).coerceAtLeast(2),
        onImagesSelected = { photos ->
            val remaining = (maxUpload - evidenceUrls.size).coerceAtLeast(0)
            if (remaining <= 0) {
                onShowMessage("当前已选 ${selectedKeys.size} 项，最多上传 $maxUpload 张图片")
                return@rememberPhotoPickerLauncher
            }
            val accepted = mutableListOf<Pair<ByteArray, String>>()
            for (photo in photos.take(remaining)) {
                val contentType = sniffImageContentType(photo.byteArray)
                val displayName = photo.uriString.substringAfterLast('/').ifBlank { "图片" }
                when {
                    contentType == null || contentType == "image/gif" -> onShowMessage("仅支持 jpg / png / webp")
                    photo.byteArray.size > PENALTY_MAX_BYTES -> onShowMessage("$displayName 超过 5MB")
                    else -> accepted += photo.byteArray to contentType
                }
            }
            if (accepted.isEmpty()) return@rememberPhotoPickerLauncher

            uploading = true
            // 逐张串行上传，成功后立即追加（与官方逐张上传 + 进度条一致）
            var index = 0
            fun uploadNext() {
                val current = accepted.getOrNull(index)
                if (current == null) {
                    uploading = false
                    return
                }
                val (bytes, contentType) = current
                ZeroTalkClientManager.uploadPenaltyEvidence(
                    fileBytes = bytes,
                    filename = "penalty_appeal_${System.currentTimeMillis()}.${imageExtensionFor(contentType)}",
                    contentType = contentType
                ) { success, url, err ->
                    if (success && !url.isNullOrBlank()) {
                        evidenceUrls = evidenceUrls + url
                    } else {
                        onShowMessage(err ?: "上传失败")
                    }
                    index += 1
                    uploadNext()
                }
            }
            uploadNext()
        },
        onPermissionDenied = { onShowMessage("需要相册权限才能上传手写检讨图片") }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        BasicText(
            text = "手写检讨申请解除处罚，账号终身仅一次",
            style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        when {
            loading && bootstrap == null -> SecurityStateCard(
                isDark = isDark,
                icon = Icons.Default.Security,
                title = "加载中",
                subtitle = ""
            )

            activePenalties.isEmpty() && appeal == null -> SecurityStateCard(
                isDark = isDark,
                icon = Icons.Default.Check,
                title = "状态很好",
                subtitle = "你目前没有生效中的处罚，继续保持就好。若日后有限制，可在此申请一次减免。"
            )

            else -> {
                // 我的申请（已提交过则隐藏申请表单，终身仅一次）
                appeal?.let { myAppeal ->
                    PenaltyAppealCard(appeal = myAppeal, isDark = isDark)
                    Spacer(Modifier.height(12.dp))
                }

                if (activePenalties.isNotEmpty() && appeal == null) {
                    // 申请须知
                    AppleHigFillCard(
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth(),
                        contentPaddingValues = PaddingValues(16.dp),
                        itemSpacing = 6.dp
                    ) {
                        BasicText(
                            text = "申请须知",
                            style = AppleHigTypography.groupedRowTitle.copy(
                                color = higColors.label,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        guidelines.forEachIndexed { index, text ->
                            BasicText(
                                text = "${index + 1}. $text",
                                style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (canApply) {
                        AppleHigFillCard(
                            isDark = isDark,
                            modifier = Modifier.fillMaxWidth(),
                            contentPaddingValues = PaddingValues(16.dp),
                            itemSpacing = 10.dp
                        ) {
                            Column {
                                BasicText(
                                    text = "提交申请",
                                    style = AppleHigTypography.groupedRowTitle.copy(
                                        color = higColors.label,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(Modifier.height(2.dp))
                                BasicText(
                                    text = "已选 ${selectedKeys.size} 项 · 手写检讨合计不少于 $requiredWords 字",
                                    style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
                                )
                            }

                            // 选择减免项目
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                BasicText(
                                    text = "选择减免项目",
                                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                                )
                                activePenalties.forEach { option ->
                                    PenaltyOptionRow(
                                        option = option,
                                        selected = selectedKeys.contains(option.key),
                                        isDark = isDark,
                                        onToggle = {
                                            selectedKeys = if (selectedKeys.contains(option.key)) {
                                                selectedKeys - option.key
                                            } else {
                                                selectedKeys + option.key
                                            }
                                        }
                                    )
                                }
                            }

                            // 手写检讨图片（选择项目后才出现）
                            if (selectedKeys.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            BasicText(
                                                text = "纸张手写检讨",
                                                style = AppleHigTypography.groupedRowTitle.copy(color = higColors.label)
                                            )
                                            BasicText(
                                                text = "拍照清晰完整，内容覆盖本次申请的违规事项",
                                                style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                                            )
                                        }
                                        CapsuleGlassButton(
                                            onClick = { if (!uploading && !submitting) photoPicker.launch() },
                                            isDark = isDark,
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            BasicText(
                                                text = if (uploading) "上传中…"
                                                else if (evidenceUrls.isNotEmpty()) "继续添加" else "上传图片",
                                                style = AppleHigTypography.caption1.copy(
                                                    color = higColors.tint,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }

                                    if (evidenceUrls.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            evidenceUrls.forEachIndexed { index, url ->
                                                Box {
                                                    AsyncNetworkImage(
                                                        url = url,
                                                        contentDescription = "手写检讨 ${index + 1}",
                                                        modifier = Modifier.size(72.dp),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(2.dp)
                                                            .size(18.dp)
                                                            .clip(CircleShape)
                                                            .background(higColors.label.copy(alpha = 0.55f))
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null,
                                                                enabled = !uploading && !submitting,
                                                                onClick = {
                                                                    evidenceUrls = evidenceUrls.filterIndexed { i, _ -> i != index }
                                                                }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "移除图片",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    BasicText(
                                        text = "${evidenceUrls.size}/$maxUpload",
                                        style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                                    )
                                }
                            }

                            // 手写确认
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { ackGuidelines = !ackGuidelines }
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HigCheckBox(checked = ackGuidelines, isDark = isDark)
                                BasicText(
                                    text = "确认检讨为本人纸张手写与违规相关，不得使用电子字、打印件或代写、AI生图",
                                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        PrimaryActionButton(
                            text = if (submitting) "提交中…" else "提交申请",
                            enabled = !submitting && !uploading,
                            isDark = isDark,
                            onClick = {
                                when {
                                    selectedKeys.isEmpty() -> onShowMessage("请至少选择一项处罚")
                                    evidenceUrls.isEmpty() -> onShowMessage("请至少上传一张手写检讨图片")
                                    !ackGuidelines -> onShowMessage("请先勾选手写检讨确认")
                                    else -> {
                                        submitting = true
                                        ZeroTalkClientManager.submitPenaltyAppeal(
                                            penaltyKeys = selectedKeys.toList(),
                                            evidenceUrls = evidenceUrls
                                        ) { success, err ->
                                            submitting = false
                                            if (success) {
                                                selectedKeys = emptySet()
                                                evidenceUrls = emptyList()
                                                ackGuidelines = false
                                                onShowMessage("申请已提交")
                                            } else {
                                                onShowMessage(err ?: "提交失败")
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        AppleHigFillCard(
                            isDark = isDark,
                            modifier = Modifier.fillMaxWidth(),
                            contentPaddingValues = PaddingValues(16.dp),
                            itemSpacing = 6.dp
                        ) {
                            BasicText(
                                text = "暂不可申请",
                                style = AppleHigTypography.groupedRowTitle.copy(
                                    color = higColors.label,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            BasicText(
                                text = bootstrap?.reason?.takeIf { it.isNotBlank() }
                                    ?: "当前无法提交处罚减免申请",
                                style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 我的申请卡片（状态 / 申请项目 / 要求字数 / 审核回复 / 证据缩略图） */
@Composable
private fun PenaltyAppealCard(appeal: PenaltyAppealDto, isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
    val statusText = when (appeal.status) {
        "pending" -> "待审核"
        "approved" -> "已通过"
        "rejected" -> "已驳回"
        else -> appeal.status
    }
    val statusColor = when (appeal.status) {
        "pending" -> higColors.systemOrange
        "approved" -> higColors.systemGreen
        "rejected" -> higColors.secondaryLabel
        else -> higColors.secondaryLabel
    }
    val evidence = penaltyEvidenceList(appeal)

    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier.fillMaxWidth(),
        contentPaddingValues = PaddingValues(16.dp),
        itemSpacing = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = "我的申请",
                    style = AppleHigTypography.groupedRowTitle.copy(
                        color = higColors.label,
                        fontWeight = FontWeight.Medium
                    )
                )
                BasicText(
                    text = "提交于 ${formatSecurityDateTime(appeal.createdAt)}",
                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(statusColor.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                BasicText(
                    text = statusText,
                    style = AppleHigTypography.caption2.copy(
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        SecurityFactRow(
            label = "申请项目",
            value = appeal.penaltyLabels.orEmpty().joinToString("、").ifBlank { "—" },
            isDark = isDark
        )
        SecurityFactRow(
            label = "要求字数",
            value = "不少于 ${appeal.requiredWordCount} 字",
            isDark = isDark
        )

        appeal.adminReply?.takeIf { it.isNotBlank() }?.let { reply ->
            AppleHigDivider(isDark = isDark, insetStart = 0.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = "审核回复",
                    style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                )
                BasicText(
                    text = reply,
                    style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.label)
                )
                appeal.reviewedAt?.takeIf { it.isNotBlank() }?.let { reviewedAt ->
                    BasicText(
                        text = formatSecurityDateTime(reviewedAt),
                        style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
                    )
                }
            }
        }

        if (evidence.isNotEmpty()) {
            AppleHigDivider(isDark = isDark, insetStart = 0.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                evidence.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AsyncNetworkImage(
                            url = item.url,
                            contentDescription = item.label,
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
                        BasicText(
                            text = item.label,
                            style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel)
                        )
                    }
                }
            }
        }
    }
}

/** 减免项目勾选行 */
@Composable
private fun PenaltyOptionRow(
    option: PenaltyOptionDto,
    selected: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) higColors.tint.copy(alpha = 0.10f) else higColors.systemFill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HigCheckBox(checked = selected, isDark = isDark)
        BasicText(
            text = option.label,
            style = AppleHigTypography.groupedRowTitle.copy(
                color = if (selected) higColors.label else higColors.secondaryLabel
            )
        )
    }
}

/** HIG 风格方形勾选框 */
@Composable
private fun HigCheckBox(checked: Boolean, isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) higColors.tint else higColors.secondarySystemFill)
            .border(
                width = 1.dp,
                color = if (checked) higColors.tint else higColors.separator,
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

// ------------------------------------------------------------
// 4) 网易云绑定
// ------------------------------------------------------------

/**
 * 网易云音乐绑定 Sheet 内容
 *
 * 前置必须勾选「本站不提供 VIP 绕过」确认（对应 ack_disclaimer=1）；
 * 扫码流程：start → 轮询 status（1.8s 首查，之后 2s）→ success/expired 停止 → 取消/离开页面即停。
 */
@Composable
fun SheetNeteaseBindContent(
    isDark: Boolean,
    onShowMessage: (String) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val binding by ZeroTalkClientManager.neteaseBinding.collectAsState()
    val loading by ZeroTalkClientManager.neteaseLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    /** 已绑定时是否展开「更换账号」抽屉 */
    var drawerOpen by remember { mutableStateOf(false) }
    var ackDisclaimer by remember { mutableStateOf(false) }
    var cookieText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    // 扫码会话
    var sessionId by remember { mutableStateOf("") }
    var qrImage by remember { mutableStateOf("") }
    var qrMessage by remember { mutableStateOf("") }
    var qrStatus by remember { mutableStateOf("") }
    var polling by remember { mutableStateOf(false) }

    val bound = binding?.bound == true
    val disclaimer = binding?.disclaimer?.takeIf { it.isNotBlank() }
        ?: "绑定后仅使用你自己的网易云登录态。VIP 歌曲需账号本身有会员；本站不提供绕过方案。"
    val drawerVisible = !bound || drawerOpen

    LaunchedEffect(Unit) {
        ZeroTalkClientManager.loadNeteaseBinding { success, err ->
            if (!success) onShowMessage(err ?: "加载失败")
        }
    }

    // 绑定成功后收起抽屉、清空输入
    LaunchedEffect(bound) {
        if (bound) {
            drawerOpen = false
            cookieText = ""
            ackDisclaimer = false
            stopQrPolling(
                sessionId = sessionId,
                onReset = {
                    sessionId = ""
                    qrImage = ""
                    qrMessage = ""
                    qrStatus = ""
                }
            )
            polling = false
        }
    }

    // 扫码轮询：离开页面或停止时协程自动取消
    LaunchedEffect(sessionId, polling) {
        if (!polling || sessionId.isBlank()) return@LaunchedEffect
        var first = true
        while (isActive && polling && sessionId.isNotBlank()) {
            delay(if (first) 1800L else 2000L)
            first = false
            val res = ZeroTalkClientManager.pollNeteaseQrStatus(sessionId)
            val data = res.getOrNull()
            if (!res.isSuccess) {
                polling = false
                onShowMessage(res.exceptionOrNull()?.message ?: "扫码状态查询失败")
                break
            }
            qrStatus = data?.status.orEmpty()
            qrMessage = data?.message.orEmpty()
            when (qrStatus) {
                "success" -> {
                    polling = false
                    sessionId = ""
                    qrImage = ""
                    onShowMessage("扫码绑定成功")
                    break
                }
                "expired", "failed" -> {
                    polling = false
                    break
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        when {
            loading && binding == null -> SecurityStateCard(
                isDark = isDark,
                icon = Icons.Default.MusicNote,
                title = "加载中…",
                subtitle = ""
            )

            bound -> NeteaseBoundCard(
                binding = binding!!,
                isDark = isDark,
                busy = busy,
                drawerOpen = drawerOpen,
                onRefresh = {
                    busy = true
                    ZeroTalkClientManager.refreshNeteaseBinding { success, err ->
                        busy = false
                        onShowMessage(if (success) "已尝试续期" else (err ?: "续期失败"))
                    }
                },
                onToggleDrawer = { drawerOpen = !drawerOpen },
                onUnbind = {
                    busy = true
                    ZeroTalkClientManager.unbindNetease { success, err ->
                        busy = false
                        onShowMessage(if (success) "已解除绑定" else (err ?: "解绑失败"))
                    }
                }
            )

            else -> SecurityStateCard(
                isDark = isDark,
                icon = Icons.Default.MusicNote,
                title = "尚未绑定网易云账号",
                subtitle = "绑定后站内播放将使用你的登录态。VIP 曲目需你的网易云账号本身具备会员。"
            )
        }

        if (drawerVisible) {
            Spacer(Modifier.height(12.dp))

            // 使用须知 + 前置确认
            AppleHigFillCard(
                isDark = isDark,
                modifier = Modifier.fillMaxWidth(),
                contentPaddingValues = PaddingValues(16.dp),
                itemSpacing = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "使用须知",
                        tint = higColors.systemOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    BasicText(
                        text = "使用须知",
                        style = AppleHigTypography.groupedRowTitle.copy(
                            color = higColors.label,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                BasicText(
                    text = disclaimer,
                    style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { ackDisclaimer = !ackDisclaimer }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HigCheckBox(checked = ackDisclaimer, isDark = isDark)
                    BasicText(
                        text = "我已确认：本站不提供 VIP 绕过，仅使用我自己的网易云会员权益",
                        style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LiquidSegmentedControl(
                options = listOf("扫码绑定", "粘贴 Cookie"),
                selectedIndex = selectedTab,
                onOptionSelect = { index ->
                    selectedTab = index
                    // 切换方式即作废当前二维码并停止轮询
                    if (sessionId.isNotBlank()) {
                        ZeroTalkClientManager.cancelNeteaseQrBind(sessionId)
                    }
                    polling = false
                    sessionId = ""
                    qrImage = ""
                    qrMessage = ""
                    qrStatus = ""
                },
                outerHeight = 34.dp,
                innerHeight = 28.dp,
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            if (selectedTab == 0) {
                AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(),
                    contentPaddingValues = PaddingValues(16.dp),
                    itemSpacing = 10.dp
                ) {
                    BasicText(
                        text = "使用网易云音乐 App 扫描二维码并确认登录",
                        style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val qrSvgSpec = remember(qrImage) { parseQrSvgDataUri(qrImage) }
                        val qrInlineBitmap = remember(qrImage) { decodeInlineQrImage(qrImage) }
                        val qrImageUrl = remember(qrImage) { normalizeQrImageUrl(qrImage) }
                        if (qrSvgSpec != null) {
                            // 实测服务端下发的是 SVG data URI（BitmapFactory 解不了），解析矩阵后用 Canvas 画
                            QrCodeFromSvg(
                                spec = qrSvgSpec,
                                sizeDp = 160.dp,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            )
                        } else if (qrInlineBitmap != null) {
                            // 服务端把二维码以 data URI / base64 内联下发时本地解码绘制
                            androidx.compose.foundation.Image(
                                bitmap = qrInlineBitmap,
                                contentDescription = "网易云登录二维码",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else if (qrImageUrl.isNotBlank()) {
                            AsyncNetworkImage(
                                url = qrImageUrl,
                                contentDescription = "网易云登录二维码",
                                modifier = Modifier.size(160.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                shape = RoundedCornerShape(12.dp),
                                showPlaceholder = true
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "二维码",
                                    tint = higColors.tertiaryLabel,
                                    modifier = Modifier.size(48.dp)
                                )
                                BasicText(
                                    text = "点击下方按钮生成二维码",
                                    style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                                )
                            }
                        }
                    }

                    if (qrMessage.isNotBlank()) {
                        BasicText(
                            text = qrMessage,
                            style = AppleHigTypography.caption1.copy(
                                color = when (qrStatus) {
                                    "success" -> higColors.systemGreen
                                    "failed", "expired" -> higColors.destructive
                                    else -> higColors.secondaryLabel
                                },
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PrimaryActionButton(
                        text = when {
                            polling -> "等待扫码…"
                            sessionId.isNotBlank() -> "刷新二维码"
                            else -> "生成二维码"
                        },
                        enabled = ackDisclaimer && !busy && !polling,
                        isDark = isDark,
                        onClick = {
                            if (!ackDisclaimer) {
                                onShowMessage("请先勾选使用须知确认")
                                return@PrimaryActionButton
                            }
                            busy = true
                            if (sessionId.isNotBlank()) {
                                ZeroTalkClientManager.cancelNeteaseQrBind(sessionId)
                            }
                            ZeroTalkClientManager.startNeteaseQrBind { success, data, err ->
                                busy = false
                                if (!success || data == null) {
                                    onShowMessage(err ?: "生成二维码失败")
                                    return@startNeteaseQrBind
                                }
                                sessionId = data.sessionId
                                qrImage = data.qrImage
                                qrStatus = data.status
                                // 对齐官方：没有二维码图片但有二维码内容时，内容提示**覆盖** message
                                // （官方 `!qr_image && qr_content && (message = "请用网易云 App 打开：…")`）
                                qrMessage = if (data.qrImage.isBlank() && data.qrContent.isNotBlank()) {
                                    "请用网易云 App 打开：${data.qrContent}"
                                } else {
                                    data.message.ifBlank { "请扫码" }
                                }
                                polling = data.sessionId.isNotBlank()
                            }
                        }
                    )

                    if (sessionId.isNotBlank()) {
                        SecondaryActionRow(
                            text = "取消",
                            isDark = isDark,
                            onClick = {
                                ZeroTalkClientManager.cancelNeteaseQrBind(sessionId)
                                polling = false
                                sessionId = ""
                                qrImage = ""
                                qrMessage = ""
                                qrStatus = ""
                            }
                        )
                    }
                }
            } else {
                AppleHigFillCard(
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth(),
                    contentPaddingValues = PaddingValues(16.dp),
                    itemSpacing = 10.dp
                ) {
                    BasicText(
                        text = "在电脑浏览器登录 music.163.com，从开发者工具复制 Cookie，至少包含 MUSIC_U",
                        style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(higColors.systemFill)
                            .padding(10.dp)
                    ) {
                        if (cookieText.isEmpty()) {
                            BasicText(
                                text = "MUSIC_U=xxx; __csrf=yyy; …",
                                style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                            )
                        }
                        BasicTextField(
                            value = cookieText,
                            onValueChange = { if (it.length <= 12000) cookieText = it },
                            textStyle = AppleHigTypography.caption1.copy(color = higColors.label),
                            cursorBrush = SolidColor(higColors.systemBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PrimaryActionButton(
                        text = if (busy) "提交中…" else "确认绑定",
                        enabled = ackDisclaimer && cookieText.isNotBlank() && !busy,
                        isDark = isDark,
                        onClick = {
                            if (!ackDisclaimer) {
                                onShowMessage("请先勾选使用须知确认")
                                return@PrimaryActionButton
                            }
                            busy = true
                            ZeroTalkClientManager.bindNeteaseCookie(cookieText) { success, err, bound ->
                                busy = false
                                if (success) {
                                    onShowMessage("绑定成功")
                                    // 官方：账号可能无 VIP 时额外提示 vip_hint
                                    if ((bound?.vipType ?: 0) <= 0) {
                                        onShowMessage(
                                            bound?.vipHint?.takeIf { it.isNotBlank() }
                                                ?: "账号可能无 VIP，VIP 歌曲或仅试听"
                                        )
                                    }
                                } else {
                                    onShowMessage(err ?: "绑定失败")
                                }
                            }
                        }
                    )
                }
            }

            if (binding?.tableReady == false) {
                Spacer(Modifier.height(10.dp))
                BasicText(
                    text = "数据库未升级：请执行 upgrade_netease_binding.sql 后再使用绑定功能。",
                    style = AppleHigTypography.caption1.copy(color = higColors.destructive)
                )
            }
        }
    }
}

/** 已绑定账号卡片 */
@Composable
private fun NeteaseBoundCard(
    binding: NeteaseBindingData,
    isDark: Boolean,
    busy: Boolean,
    drawerOpen: Boolean,
    onRefresh: () -> Unit,
    onToggleDrawer: () -> Unit,
    onUnbind: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val statusText = when (binding.status) {
        "active" -> "有效"
        "invalid" -> "已失效"
        else -> binding.status.ifBlank { "未知" }
    }
    val statusColor = if (binding.status == "active") higColors.systemGreen else higColors.destructive

    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier.fillMaxWidth(),
        contentPaddingValues = PaddingValues(16.dp),
        itemSpacing = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserAvatar(
                url = binding.avatarUrl.takeIf { it.isNotBlank() },
                name = binding.nickname.ifBlank { "网易云" },
                size = 48.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicText(
                        text = binding.nickname.ifBlank { "已绑定账号" },
                        style = AppleHigTypography.groupedRowTitle.copy(
                            color = higColors.label,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(statusColor.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(
                            text = statusText,
                            style = AppleHigTypography.caption2.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = "网易云 UID ${if (binding.neteaseUid > 0L) binding.neteaseUid.toString() else "—"}",
                    style = AppleHigTypography.groupedRowSubtitle.copy(color = higColors.secondaryLabel)
                )
                if (binding.vipHint.isNotBlank()) {
                    BasicText(
                        text = binding.vipHint,
                        style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SecondaryActionRow(
                    text = "续期登录态",
                    isDark = isDark,
                    onClick = { if (!busy) onRefresh() }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SecondaryActionRow(
                    text = if (drawerOpen) "收起" else "更换账号",
                    isDark = isDark,
                    onClick = { if (!busy) onToggleDrawer() }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SecondaryActionRow(
                    text = "解除绑定",
                    isDark = isDark,
                    destructive = true,
                    onClick = { if (!busy) onUnbind() }
                )
            }
        }
    }
}

/** 停止扫码轮询并作废会话（离开/绑定成功时调用） */
private fun stopQrPolling(sessionId: String, onReset: () -> Unit) {
    if (sessionId.isNotBlank()) {
        ZeroTalkClientManager.cancelNeteaseQrBind(sessionId)
    }
    onReset()
}

/**
 * 网易云扫码二维码图片兼容处理
 *
 * 服务端的 `qr_image` 可能是三种形态，逐一体贴处理，避免「有图却显示占位框」：
 * 1. `data:image/png;base64,xxx` 或纯 base64 → 本地解码为位图（离线可用，无需下载）；
 * 2. 相对路径 `/uploads/qr/xxx.png` → 补全为站点绝对地址；
 * 3. 完整 URL → 原样交给 AsyncNetworkImage 下载。
 */
private fun decodeInlineQrImage(raw: String): ImageBitmap? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    val base64 = when {
        value.startsWith("data:", ignoreCase = true) -> value.substringAfter(',', "")
        // 裸 base64：既不是 data URI 也不是 http(s) 链接，且长度足够（二维码图不会太短）
        !value.startsWith("http", ignoreCase = true) && value.length > 200 -> value
        else -> return null
    }
    if (base64.isBlank()) return null
    return try {
        @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
        val bytes = kotlin.io.encoding.Base64.decode(base64.filterNot { it.isWhitespace() })
        decodeByteArrayToImageBitmap(bytes, 512)
    } catch (_: Exception) {
        null
    }
}

/** 二维码图片地址：相对路径补全为站点绝对地址，其余原样返回 */
private fun normalizeQrImageUrl(raw: String): String {
    val value = raw.trim()
    if (value.isEmpty()) return ""
    if (value.startsWith("data:", ignoreCase = true)) return ""
    if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
        return value
    }
    if (value.startsWith("//")) return "https:$value"
    if (value.startsWith("/")) return "${top.lanxint.zerotalk.data.network.ZeroTalkApiService.BASE_URL}$value"
    // 既不是链接也不是 base64 的裸串：交给下载层尝试（可能服务端直接给了文件名）
    return value
}

// ============================================================
// 5) 联系我们   SheetContactUsContent   —— 对齐官网 ContactUsView
// 6) 问题反馈   SheetFeedbackContent    —— 对齐官网 FeedbackView
// 7) 捐赠本站   SheetDonateContent      —— 对齐官网 DonateView + 新增「捐献 App」内置收款码
//
// 参考依据（tmp/assets/）：
//   contact-Qek2ePzN.js        GET /api/contact/bootstrap、POST /contact/submit
//   ContactUsView-B0dCPMDm.js  群二维码 + 邮箱 / QQ / 微信 / other_contact 列表
//   FeedbackView-CeBvwI2E.js   类型 / 标题 / 内容表单 + 我的反馈记录（含官方回复）
//   DonateView-BgtTdtCv.js     GET /api/donate、POST /api/donate/dm、分享码备注、捐赠名单
// ============================================================

/** 联系我们 · 单行联系方式（官方顺序：邮箱 → QQ → 微信 → other_contact） */
private data class ContactEntry(
    val name: String,
    val value: String,
    val icon: ImageVector,
    val iconBgColor: Color
)

/** 加载中占位卡片（联系我们 / 捐赠页共用） */
@Composable
private fun SheetLoadingCard(isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
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
                text = "正在加载…",
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
            )
        }
    }
}

/**
 * 联系我们 Sheet 内容（对齐官网 ContactUsView）
 *
 * 顶部为官方交流群二维码（`group_name` + `group_qrcode`），下方为联系方式列表。
 * 官网此处为纯展示，按需求增加「点击任意一项复制」（复制后 toast 提示）。
 */
@Composable
fun SheetContactUsContent(
    isDark: Boolean,
    onShowMessage: (String) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val clipboard = LocalClipboardManager.current

    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var bootstrap by remember { mutableStateOf<ContactBootstrapData?>(null) }

    LaunchedEffect(Unit) {
        val res = ZeroTalkClientManager.getContactBootstrap()
        if (res.isSuccess) {
            bootstrap = res.getOrNull()
        } else {
            errorText = res.exceptionOrNull()?.message ?: "加载失败"
        }
        isLoading = false
    }

    val config = bootstrap?.contactConfig
    val entries = remember(config, bootstrap?.otherContact) {
        buildList {
            config?.contactEmail?.takeIf { it.isNotBlank() }?.let {
                add(ContactEntry("邮箱", it, Icons.Default.Email, Color(0xFF3B82F6)))
            }
            config?.contactQq?.takeIf { it.isNotBlank() }?.let {
                add(ContactEntry("QQ", it, Icons.Default.Chat, Color(0xFF06B6D4)))
            }
            config?.contactWechat?.takeIf { it.isNotBlank() }?.let {
                add(ContactEntry("微信", it, Icons.Default.Forum, Color(0xFF10B981)))
            }
            bootstrap?.otherContact.orEmpty()
                .filter { it.name.isNotBlank() && it.value.isNotBlank() }
                .forEach {
                    add(ContactEntry(it.name, it.value, Icons.Default.Info, Color(0xFF8B5CF6)))
                }
        }
    }

    val copyValue: (String) -> Unit = { value ->
        clipboard.setText(AnnotatedString(value))
        onShowMessage("已复制：$value")
    }

    val groupQr = normalizeQrImageUrl(config?.groupQrcode.orEmpty())
    val isEmpty = config == null || (groupQr.isBlank() && entries.isEmpty())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        when {
            isLoading -> SheetLoadingCard(isDark = isDark)

            isEmpty -> {
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
                            imageVector = Icons.Default.Email,
                            contentDescription = "联系方式",
                            tint = higColors.tertiaryLabel,
                            modifier = Modifier.size(36.dp)
                        )
                        BasicText(
                            text = errorText?.takeIf { it.isNotBlank() } ?: "暂未配置联系方式",
                            style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                        )
                        BasicText(
                            text = "请稍后再试，或通过「问题反馈」留言",
                            style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                        )
                    }
                }
            }

            else -> {
                if (groupQr.isNotBlank()) {
                    AppleHigFillCard(
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth(),
                        contentPaddingValues = PaddingValues(16.dp),
                        itemSpacing = 12.dp
                    ) {
                        BasicText(
                            text = config?.groupName?.takeIf { it.isNotBlank() } ?: "官方交流群",
                            style = AppleHigTypography.headline.copy(
                                color = higColors.label,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        AsyncNetworkImage(
                            url = groupQr,
                            contentDescription = "群聊二维码",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Fit,
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = Color.White
                        )
                        BasicText(
                            text = "扫码加入群聊",
                            style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                if (entries.isNotEmpty()) {
                    AppleHigGroupedSection(
                        title = "联系方式",
                        footer = "如需人工协助，可通过以上方式找到我们；点击任意一项即可复制",
                        isDark = isDark
                    ) {
                        entries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                AppleHigDivider(isDark = isDark, insetStart = 16.dp)
                            }
                            AppleHigRow(
                                title = entry.name,
                                value = entry.value,
                                icon = entry.icon,
                                iconBgColor = entry.iconBgColor,
                                isDark = isDark,
                                accessory = AppleHigRowAccessory.None,
                                onClick = { copyValue(entry.value) }
                            )
                        }
                    }
                }
            }
        }

        // App 专属：开发者联系邮箱（服务端 contact_config 未下发，客户端固定展示）
        Spacer(Modifier.height(16.dp))

        AppleHigGroupedSection(
            title = "开发者",
            footer = "App 相关的问题与建议可发送至该邮箱；点击即可复制",
            isDark = isDark
        ) {
            AppleHigRow(
                title = "联系App开发者",
                value = "noreply@lanxint.top",
                icon = Icons.Default.Email,
                iconBgColor = Color(0xFF3B82F6),
                isDark = isDark,
                accessory = AppleHigRowAccessory.None,
                onClick = { copyValue("noreply@lanxint.top") }
            )
        }
    }
}

/** 「App反馈」一键加入的官方暗语群聊（固定参数） */
private const val APP_FEEDBACK_ROOM_OWNER = "kelo"
private const val APP_FEEDBACK_ROOM_NAME = "App端群"
private const val APP_FEEDBACK_ROOM_PASSWORD = "1"

/**
 * 问题反馈 Sheet 内容（对齐官网 FeedbackView）
 *
 * 上半部分为提交表单（类型 / 标题 / 详细内容），下半部分为「我的反馈」记录；
 * 提交成功后自动刷新记录列表。
 *
 * 顶部另有「App反馈」入口：一键加入官方暗语群聊（创建者 kelo / 房间 App端群 / 暗号 1），
 * 走 [ZeroTalkClientManager.joinSecretRoom] 的 `POST /room/join`。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetFeedbackContent(
    isDark: Boolean,
    onShowMessage: (String) -> Unit,
    onOpenRoom: (roomId: String, roomName: String) -> Unit = { _, _ -> }
) {
    val higColors = AppleHigColors.colors(isDark)
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var config by remember { mutableStateOf<ContactConfigDto?>(null) }
    var records by remember { mutableStateOf<List<FeedbackRecordDto>>(emptyList()) }

    var feedbackType by remember { mutableStateOf("suggestion") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var isJoiningAppRoom by remember { mutableStateOf(false) }

    // 「发送诊断信息到 App端群」：预览文本 / 预览弹窗 / 发送中
    var diagnosticsText by remember { mutableStateOf("") }
    var showDiagnosticsPreview by remember { mutableStateOf(false) }
    var isSendingDiagnostics by remember { mutableStateOf(false) }

    // 官方同款：内容上限 2000 字，剩余不足 50 字时提示转警示色
    val contentLimit = 2000
    val remaining = (contentLimit - content.length).coerceAtLeast(0)

    suspend fun reload() {
        val res = ZeroTalkClientManager.getContactBootstrap()
        if (res.isSuccess) {
            val data = res.getOrNull()
            config = data?.contactConfig
            records = data?.userFeedbacks.orEmpty()
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { reload() }

    val onSubmit: () -> Unit = {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        when {
            isSubmitting -> Unit
            trimmedTitle.isEmpty() -> formError = "请填写标题"
            trimmedContent.isEmpty() -> formError = "请填写内容"
            else -> {
                isSubmitting = true
                formError = null
                scope.launch {
                    val res = ZeroTalkClientManager.submitFeedback(
                        type = feedbackType,
                        title = trimmedTitle,
                        content = trimmedContent
                    )
                    if (res.isSuccess) {
                        onShowMessage("提交成功")
                        title = ""
                        content = ""
                        reload()
                    } else {
                        formError = res.exceptionOrNull()?.message ?: "提交失败，请稍后重试"
                    }
                    isSubmitting = false
                }
            }
        }
    }

    // ---- 诊断信息发送链路：已加入则直接进房发送，未加入则先 joinSecretRoom ----

    /** 进入目标房间并发送（发送前校验连接状态，避免消息静默丢失） */
    suspend fun deliverDiagnostics(roomId: String, text: String) {
        if (ZeroTalkClientManager.status.value !is ClientStatus.Connected) {
            isSendingDiagnostics = false
            onShowMessage("发送失败：连接未就绪，请稍后重试")
            return
        }
        // 先让 WebSocket 进入该房间，再发送，避免 join 与 send 抢跑导致消息被服务端丢弃
        ZeroTalkClientManager.enterRoom(roomId)
        delay(600)
        ZeroTalkClientManager.sendRoomMessage(roomId, text)
        isSendingDiagnostics = false
        showDiagnosticsPreview = false
        onShowMessage("已发送到「$APP_FEEDBACK_ROOM_NAME」")
    }

    /** 尚未加入 App端群：复用「App反馈」同一套常量与 joinSecretRoom */
    fun joinAndSendDiagnostics(text: String) {
        ZeroTalkClientManager.joinSecretRoom(
            creatorUsername = APP_FEEDBACK_ROOM_OWNER,
            roomName = APP_FEEDBACK_ROOM_NAME,
            password = APP_FEEDBACK_ROOM_PASSWORD,
            onSuccess = { data ->
                scope.launch { deliverDiagnostics(data.roomId, text) }
            },
            onError = { err ->
                isSendingDiagnostics = false
                onShowMessage("加入「$APP_FEEDBACK_ROOM_NAME」失败：$err")
            }
        )
    }

    val onSendDiagnostics: () -> Unit = {
        if (!isSendingDiagnostics) {
            val text = diagnosticsText.trim()
            when {
                text.isEmpty() -> onShowMessage("诊断信息为空，无法发送")
                ZeroTalkClientManager.loginData.value == null ->
                    onShowMessage("请先登录后再发送诊断信息")
                else -> {
                    isSendingDiagnostics = true
                    scope.launch {
                        val existing = ZeroTalkClientManager.conversations.value
                            .firstOrNull { it.targetName == APP_FEEDBACK_ROOM_NAME }
                        if (existing != null) {
                            deliverDiagnostics(existing.id, text)
                        } else {
                            joinAndSendDiagnostics(text)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        // ---- App反馈：一键加入官方暗语群聊（创建者 kelo / 房间 App端群 / 暗号 1） ----
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 12.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "App反馈",
                    style = AppleHigTypography.headline.copy(
                        color = higColors.label,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    text = "一键加入官方暗语群聊「$APP_FEEDBACK_ROOM_NAME」，直接在群里反馈问题",
                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                )
            }

            LiquidButton(
                onClick = {
                    if (isJoiningAppRoom) return@LiquidButton
                    isJoiningAppRoom = true
                    onShowMessage("正在加入「$APP_FEEDBACK_ROOM_NAME」…")
                    ZeroTalkClientManager.joinSecretRoom(
                        creatorUsername = APP_FEEDBACK_ROOM_OWNER,
                        roomName = APP_FEEDBACK_ROOM_NAME,
                        password = APP_FEEDBACK_ROOM_PASSWORD,
                        onSuccess = { data ->
                            isJoiningAppRoom = false
                            onShowMessage("已加入「$APP_FEEDBACK_ROOM_NAME」")
                            onOpenRoom(data.roomId, APP_FEEDBACK_ROOM_NAME)
                        },
                        onError = { err ->
                            isJoiningAppRoom = false
                            onShowMessage(err)
                        }
                    )
                },
                isDark = isDark,
                isInteractive = !isJoiningAppRoom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
                BasicText(
                    text = if (isJoiningAppRoom) "正在加入…" else "一键加入群聊",
                    style = AppleHigTypography.subhead.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // ---- 一键发送诊断信息：先弹预览（可编辑），确认后再发到 App端群 ----
            LiquidButton(
                onClick = {
                    if (isSendingDiagnostics) return@LiquidButton
                    // 组装时即完成脱敏：uid/房间 id 截断、聊天正文省略、日志按关键字筛选
                    diagnosticsText = AppDiagnostics.build(
                        // 优先用账号 uid（32 位 hex，会截断到前 8 位）；无则退回数字 user_id
                        uid = ZeroTalkClientManager.loginData.value?.uid
                            ?.takeIf { it.isNotBlank() }
                            ?: ZeroTalkClientManager.userProfile.value.userId,
                        nickname = ZeroTalkClientManager.userProfile.value.name
                    )
                    showDiagnosticsPreview = true
                },
                isDark = isDark,
                isInteractive = !isSendingDiagnostics,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
                BasicText(
                    text = "发送诊断信息到 App端群",
                    style = AppleHigTypography.subhead.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            BasicText(
                text = "自动附带 App 版本 / 系统 / 机型 / 账号信息与日志尾部；发送前可预览编辑，" +
                    "uid 与房间 id 已截断、聊天正文已省略",
                style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
            )
        }

        Spacer(Modifier.height(16.dp))

        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 14.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "提交反馈",
                    style = AppleHigTypography.headline.copy(
                        color = higColors.label,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    text = config?.suggestionIntro?.takeIf { it.isNotBlank() }
                        ?: "提交后可在页面下方查看处理进度",
                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                )
            }

            // 反馈类型（官网为 select 四项，此处改为胶囊 chip 以适配触屏）
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    text = "反馈类型",
                    style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "suggestion" to "功能建议",
                        "bug" to "问题反馈",
                        "complaint" to "投诉建议",
                        "other" to "其他"
                    ).forEach { (value, label) ->
                        SelectableChip(
                            label = label,
                            selected = feedbackType == value,
                            isDark = isDark,
                            onClick = { feedbackType = value },
                            textStyle = AppleHigTypography.caption1
                        )
                    }
                }
            }

            // 标题（上限 255）
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    text = "标题",
                    style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(higColors.quaternarySystemFill)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { if (it.length <= 255) title = it },
                        textStyle = AppleHigTypography.body.copy(color = higColors.label),
                        cursorBrush = SolidColor(higColors.systemBlue),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (title.isEmpty()) {
                                BasicText(
                                    text = "简要说明问题或建议",
                                    style = AppleHigTypography.body.copy(color = higColors.tertiaryLabel)
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            // 详细内容（上限 2000）
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    text = "详细内容",
                    style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(higColors.quaternarySystemFill)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = content,
                        onValueChange = { if (it.length <= contentLimit) content = it },
                        textStyle = AppleHigTypography.body.copy(color = higColors.label),
                        cursorBrush = SolidColor(higColors.systemBlue),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (content.isEmpty()) {
                                BasicText(
                                    text = "请描述具体情况，便于我们跟进",
                                    style = AppleHigTypography.body.copy(color = higColors.tertiaryLabel)
                                )
                            }
                            inner()
                        }
                    )
                }
                BasicText(
                    text = "还可输入 $remaining 字",
                    style = AppleHigTypography.caption2.copy(
                        color = if (remaining < 50) higColors.systemOrange else higColors.tertiaryLabel
                    )
                )
            }

            formError?.takeIf { it.isNotBlank() }?.let {
                BasicText(
                    text = it,
                    style = AppleHigTypography.footnote.copy(color = higColors.systemRed)
                )
            }

            LiquidButton(
                onClick = onSubmit,
                isDark = isDark,
                isInteractive = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                BasicText(
                    text = if (isSubmitting) "提交中…" else "提交反馈",
                    style = AppleHigTypography.headline.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (records.isEmpty()) {
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
                        imageVector = Icons.Default.Feedback,
                        contentDescription = "我的反馈",
                        tint = higColors.tertiaryLabel,
                        modifier = Modifier.size(36.dp)
                    )
                    BasicText(
                        text = if (isLoading) "正在加载…" else "暂无反馈记录",
                        style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                    )
                    BasicText(
                        text = "提交后将显示在这里",
                        style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                    )
                }
            }
        } else {
            AppleHigGroupedSection(
                title = "我的反馈（${records.size}）",
                footer = "流转状态：待处理 → 处理中 → 已解决 / 已关闭",
                isDark = isDark
            ) {
                records.forEachIndexed { index, record ->
                    if (index > 0) {
                        AppleHigDivider(isDark = isDark, insetStart = 16.dp)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BasicText(
                                text = record.title,
                                style = AppleHigTypography.body.copy(
                                    color = higColors.label,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FeedbackStatusBadge(status = record.status, label = record.statusLabel)
                        }

                        BasicText(
                            text = "${record.typeLabel} · ${record.createdAt}",
                            style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
                        )

                        BasicText(
                            text = record.content,
                            style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                        )

                        record.adminReply?.takeIf { it.isNotBlank() }?.let { reply ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(higColors.tint.copy(alpha = 0.10f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                BasicText(
                                    text = "官方回复",
                                    style = AppleHigTypography.caption2.copy(color = higColors.tint)
                                )
                                BasicText(
                                    text = reply,
                                    style = AppleHigTypography.subhead.copy(color = higColors.label)
                                )
                                record.repliedAt?.takeIf { it.isNotBlank() }?.let { repliedAt ->
                                    BasicText(
                                        text = repliedAt,
                                        style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- 诊断信息预览：完整文本可编辑，确认后发送 ----
    if (showDiagnosticsPreview) {
        AppleModalBottomSheet(
            onDismissRequest = { if (!isSendingDiagnostics) showDiagnosticsPreview = false },
            title = "诊断信息预览",
            isDark = isDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BasicText(
                    text = "以下内容将发送到「$APP_FEEDBACK_ROOM_NAME」，可编辑后再发送。" +
                        "账号 uid 与房间 id 已截断，聊天正文已省略。",
                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 300.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(higColors.quaternarySystemFill)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = diagnosticsText,
                        onValueChange = { diagnosticsText = it },
                        textStyle = AppleHigTypography.footnote.copy(color = higColors.label),
                        cursorBrush = SolidColor(higColors.systemBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LiquidButton(
                    onClick = onSendDiagnostics,
                    isDark = isDark,
                    isInteractive = !isSendingDiagnostics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    BasicText(
                        text = if (isSendingDiagnostics) "发送中…" else "发送到「$APP_FEEDBACK_ROOM_NAME」",
                        style = AppleHigTypography.headline.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** 反馈状态徽章（对齐官网 pending / processing / resolved / closed 四色） */
@Composable
private fun FeedbackStatusBadge(status: String, label: String) {
    val (bg, fg) = when (status) {
        "resolved" -> Color(0xFF10B981).copy(alpha = 0.15f) to Color(0xFF10B981)
        "processing" -> Color(0xFF3B82F6).copy(alpha = 0.15f) to Color(0xFF3B82F6)
        "closed" -> Color(0xFF6B7280).copy(alpha = 0.15f) to Color(0xFF6B7280)
        else -> Color(0xFFF59E0B).copy(alpha = 0.15f) to Color(0xFFF59E0B)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        BasicText(
            text = label,
            style = AppleHigTypography.caption2.copy(color = fg, fontWeight = FontWeight.Medium)
        )
    }
}

/** 捐赠收款方式选择（禁用态点击给出「暂未开放」提示，对齐官网 `R()` 行为） */
@Composable
private fun DonateChannelChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    onDisabled: () -> Unit
) {
    SelectableChip(
        label = label,
        selected = selected && enabled,
        isDark = isDark,
        onClick = { if (enabled) onClick() else onDisabled() },
        textStyle = AppleHigTypography.caption1
    )
}

/**
 * 捐赠 Sheet 内容（对齐官网 DonateView，并新增「捐献 App」内置收款码）
 *
 * 1. **捐赠本站**：说明 + 付款备注（分享码，一键复制）+ 微信/支付宝收款码 + 捐赠名单；
 * 2. **捐献 App**：直接赞赏 App 开发者，使用随包内置的两张收款码（与站点捐赠相互独立）；
 * 3. 捐赠名单：点击整行打开资料页（私信等操作在资料页内完成，故名单行不再单设私聊入口）。
 */
@Composable
fun SheetDonateContent(
    isDark: Boolean,
    onShowMessage: (String) -> Unit,
    onOpenDonorProfile: (DonorDto) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val clipboard = LocalClipboardManager.current
    val loginData by ZeroTalkClientManager.loginData.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var donate by remember { mutableStateOf<DonateData?>(null) }
    var shareCode by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var siteChannel by remember { mutableStateOf("wechat") }
    var appChannel by remember { mutableStateOf("wechat") }

    LaunchedEffect(Unit) {
        val res = ZeroTalkClientManager.getDonate()
        if (res.isSuccess) {
            donate = res.getOrNull()
        } else {
            errorText = res.exceptionOrNull()?.message ?: "加载失败"
        }
        // 分享码：优先服务端下发；失败则按官网规则本地拼 `login_name#零填充 user_id`
        val remote = ZeroTalkClientManager.getMyShareCode().getOrNull()?.shareCode?.trim().orEmpty()
        shareCode = remote.ifBlank {
            val name = loginData?.loginName.orEmpty().trim()
            val id = loginData?.userId ?: 0L
            if (name.isNotEmpty() && id > 0) {
                val padded = id.toString().let { if (it.length >= 3) it else it.padStart(3, '0') }
                "$name#$padded"
            } else {
                ""
            }
        }
        isLoading = false
    }

    val siteQr = normalizeQrImageUrl(
        if (siteChannel == "wechat") donate?.wechatQrcode.orEmpty() else donate?.alipayQrcode.orEmpty()
    )
    val siteChannelName = if (siteChannel == "wechat") "微信" else "支付宝"
    val donors = donate?.donors.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        if (isLoading) {
            SheetLoadingCard(isDark = isDark)
            return@Column
        }

        // ---------------- 捐赠本站 ----------------
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 12.dp
        ) {
            BasicText(
                text = "捐赠本站",
                style = AppleHigTypography.headline.copy(
                    color = higColors.label,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                text = donate?.intro?.takeIf { it.isNotBlank() }
                    ?: errorText?.takeIf { it.isNotBlank() }
                    ?: "捐赠完全出于自愿，不会获得任何特权、会员或额外功能。",
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
            )

            // 付款备注（分享码）
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    text = "如希望在捐赠名单中留下名字，付款时请备注站内用户名 + ID",
                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(higColors.quaternarySystemFill)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            text = shareCode.ifBlank { "加载中…" },
                            style = AppleHigTypography.footnote.copy(color = higColors.label)
                        )
                    }
                    CapsuleGlassButton(
                        onClick = {
                            if (shareCode.isBlank() || copied) return@CapsuleGlassButton
                            clipboard.setText(AnnotatedString(shareCode))
                            copied = true
                            onShowMessage("已复制，付款时粘贴到备注即可")
                        },
                        isDark = isDark,
                        enabled = shareCode.isNotBlank()
                    ) {
                        BasicText(
                            text = if (copied) "已复制" else "一键复制",
                            style = AppleHigTypography.caption1.copy(
                                color = if (shareCode.isBlank()) higColors.tertiaryLabel else higColors.tint,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 收款方式：对应二维码为空时不可选（对齐官网 pay-btn 的 disabled）
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DonateChannelChip(
                    label = "微信支付",
                    selected = siteChannel == "wechat",
                    enabled = !donate?.wechatQrcode.isNullOrBlank(),
                    isDark = isDark,
                    onClick = { siteChannel = "wechat" },
                    onDisabled = { onShowMessage("该收款方式暂未开放") }
                )
                DonateChannelChip(
                    label = "支付宝",
                    selected = siteChannel == "alipay",
                    enabled = !donate?.alipayQrcode.isNullOrBlank(),
                    isDark = isDark,
                    onClick = { siteChannel = "alipay" },
                    onDisabled = { onShowMessage("该收款方式暂未开放") }
                )
            }

            if (siteQr.isNotBlank()) {
                AsyncNetworkImage(
                    url = siteQr,
                    contentDescription = "${siteChannelName}收款二维码",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentScale = ContentScale.Fit,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color.White
                )
                BasicText(
                    text = "请使用 $siteChannelName 扫码，捐赠全凭心意",
                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                )
            } else {
                BasicText(
                    text = "该收款方式暂未开放",
                    style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- 捐献 App（随包内置收款码） ----------------
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(16.dp),
            itemSpacing = 12.dp
        ) {
            BasicText(
                text = "捐献 App",
                style = AppleHigTypography.headline.copy(
                    color = higColors.label,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                text = "直接赞赏 App 开发者。该通道与站点捐赠相互独立，同样不附带任何特权；" +
                    "感谢你为零语客户端的持续维护添一份力。",
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DonateChannelChip(
                    label = "微信",
                    selected = appChannel == "wechat",
                    enabled = true,
                    isDark = isDark,
                    onClick = { appChannel = "wechat" },
                    onDisabled = {}
                )
                DonateChannelChip(
                    label = "支付宝",
                    selected = appChannel == "alipay",
                    enabled = true,
                    isDark = isDark,
                    onClick = { appChannel = "alipay" },
                    onDisabled = {}
                )
            }

            Image(
                painter = painterResource(
                    if (appChannel == "wechat") Res.drawable.donate_app_wechat
                    else Res.drawable.donate_app_alipay
                ),
                contentDescription = if (appChannel == "wechat") "微信收款码" else "支付宝收款码",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(6.dp)
            )
            BasicText(
                text = "请使用${if (appChannel == "wechat") "微信" else "支付宝"}扫码，感谢支持",
                style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- 捐赠名单 ----------------
        if (donors.isEmpty()) {
            AppleHigFillCard(
                isDark = isDark,
                modifier = Modifier.fillMaxWidth(),
                contentPaddingValues = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "捐赠名单",
                        tint = higColors.tertiaryLabel,
                        modifier = Modifier.size(36.dp)
                    )
                    BasicText(
                        text = "暂时还没有公开的捐赠记录",
                        style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
                    )
                }
            }
        } else {
            AppleHigGroupedSection(
                title = "捐赠名单",
                footer = "已记录 ${donors.size} 位热心支持，排名不分先后",
                isDark = isDark
            ) {
                donors.forEachIndexed { index, donor ->
                    if (index > 0) {
                        AppleHigDivider(isDark = isDark, insetStart = 16.dp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (donor.hasIdentity) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onOpenDonorProfile(donor) }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UserAvatar(
                            url = donor.avatarUrl,
                            name = donor.displayName,
                            size = 40.dp
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BasicText(
                                text = donor.displayName.ifBlank { "匿名用户" },
                                style = AppleHigTypography.subhead.copy(
                                    color = higColors.label,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            donor.message?.takeIf { it.isNotBlank() }?.let { message ->
                                BasicText(
                                    text = message,
                                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                                )
                            }
                        }

                        if (donor.amount.isNotBlank()) {
                            BasicText(
                                text = "¥${donor.amount}",
                                style = AppleHigTypography.subhead.copy(
                                    color = higColors.systemOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
