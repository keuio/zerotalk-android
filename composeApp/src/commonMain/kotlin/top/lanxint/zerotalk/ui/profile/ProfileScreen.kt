package top.lanxint.zerotalk.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.AppThemeMode
import top.lanxint.zerotalk.data.notify.SystemNotificationBridge
import top.lanxint.zerotalk.data.model.UserProfile
import top.lanxint.zerotalk.data.repository.NotificationCenterStore
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.AppleHigGroupedCard
import top.lanxint.zerotalk.ui.components.AppleHigGroupedSection
import top.lanxint.zerotalk.ui.components.AppleHigRow
import top.lanxint.zerotalk.ui.components.AppleHigRowAccessory
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
import top.lanxint.zerotalk.ui.components.GenderBadge
import top.lanxint.zerotalk.ui.components.LiquidSegmentedControl
import top.lanxint.zerotalk.ui.components.LiquidToggle
import top.lanxint.zerotalk.ui.components.MbtiCard
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.sheets.ProfileSettingItem
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import com.kashif_e.backdrop.Backdrop

/**
 * 「我的」主界面
 *
 * 全面遵循 Apple HIG Inset Grouped 视觉与尺寸规范：
 * 1. 标准 44dp 列表单元格高度，紧凑优雅，彻底解决纵向松散肥大问题；
 * 2. 12dp Inset Grouped 分组卡片圆角与 0.5dp 细腻边框；
 * 3. 28dp 图标伴随标准 56dp 文本对齐分割线；
 * 4. 紧凑型用户资料卡片，统一 Apple 生态视觉体验。
 */
@Composable
fun ProfileScreen(
    profile: UserProfile,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    /** 兼容渲染模式：开启后液态玻璃降级到 Android 13 以下的渲染能力（关闭 AGSL 类特效） */
    liquidGlassCompat: Boolean,
    onLiquidGlassCompatChange: (Boolean) -> Unit,
    /** 后台接收消息（保活前台服务） */
    backgroundReceiveEnabled: Boolean,
    onBackgroundReceiveChange: (Boolean) -> Unit,
    onRefreshAvatar: () -> Unit,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onLogin: (() -> Unit)? = null,
    onOpenSetting: (ProfileSettingItem) -> Unit,
    /** 打开「通知中心」面板 */
    onOpenNotificationCenter: () -> Unit,
    /** 打开「MBTI 人格测试」面板 */
    onOpenMbtiTest: () -> Unit,
    onDeleteAccount: () -> Unit,
    backdrop: Backdrop? = null,
    isDark: Boolean,
    cardSpacing: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val titleColor = higColors.label
    val subColor = higColors.secondaryLabel

    // 系统通知权限：进入「我的」时刷新一次（用户可能刚从系统设置页返回）
    var notificationsEnabled by remember { mutableStateOf(SystemNotificationBridge.isNotificationEnabled) }
    LaunchedEffect(Unit) { notificationsEnabled = SystemNotificationBridge.isNotificationEnabled }

    // 通知中心未读数：与面板共用同一份 store 状态（进入「我的」时刷新一次）
    val notificationState by NotificationCenterStore.state.collectAsState()
    val notificationUnreadCount = notificationState.unreadCount
    LaunchedEffect(Unit) { NotificationCenterStore.refreshUnread() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 110.dp)
    ) {
        // ============================================================
        // 1. 用户信息卡片 (Apple Inset Grouped 紧凑个人名片)
        // ============================================================
        AppleHigGroupedCard(
            isDark = isDark,
            cornerRadius = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 上半部分：头像 + 姓名 + 身份胶囊
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 头像：真实头像优先，无头像/加载失败时回落为渐变圆环 + 首字母
                    UserAvatar(
                        url = profile.avatarUrl,
                        name = if (profile.isLoggedIn) profile.name else "",
                        size = 46.8.dp,
                        gradient = listOf(
                            Color(0xFF38BDF8),
                            Color(0xFF818CF8),
                            Color(0xFFC084FC)
                        ),
                        fallbackTextStyle = AppleHigTypography.title2,
                        fallbackIconSize = 27.dp
                    )

                    // 昵称与标签
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BasicText(
                            text = profile.name,
                            style = AppleHigTypography.headline.copy(
                                color = titleColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        // 胶囊信息标签：性别 + 年龄 ("18-23" 或 "23以上") + ID + QQ
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 性别
                            GenderBadge(
                                gender = profile.gender,
                                isDark = isDark,
                                fontSize = 11.sp,
                                horizontalPadding = 6.dp,
                                verticalPadding = 2.dp
                            )

                            // 年龄
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (isDark) Color(0xFF2E3440) else Color(0xFFE2E8F0))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    text = profile.ageRange,
                                    style = AppleHigTypography.caption2.copy(
                                        color = subColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }

                            // ID
                            BasicText(
                                text = "ID: ${profile.userId}",
                                style = AppleHigTypography.caption2.copy(color = subColor)
                            )

                            // 绑定的 QQ 状态
                            if (profile.qq.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFF0284C7).copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    BasicText(
                                        text = "QQ:${profile.qq}",
                                        style = AppleHigTypography.caption2.copy(
                                            color = Color(0xFF0284C7),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }

                        // 个人签名
                        BasicText(
                            text = profile.bio,
                            style = AppleHigTypography.footnote.copy(color = subColor),
                            maxLines = 1
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = higColors.separator.copy(alpha = if (isDark) 0.35f else 0.20f)
                )

                // 下半部分：3 个紧凑操作胶囊按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 退出登录 / 登录账号
                    val isLogged = profile.isLoggedIn
                    CapsuleGlassButton(
                        onClick = {
                            if (isLogged) {
                                onLogout()
                            } else {
                                onLogin?.invoke()
                            }
                        },
                        backdrop = backdrop,
                        isDark = isDark,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = if (isLogged) "退出登录" else "登录账号",
                                tint = if (isLogged) higColors.systemRed else higColors.tint,
                                modifier = Modifier.size(12.6.dp)
                            )
                            BasicText(
                                text = if (isLogged) "退出登录" else "登录账号",
                                style = AppleHigTypography.footnote.copy(
                                    color = if (isLogged) higColors.systemRed else higColors.tint,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // 刷新头像 (对应 Web 端移动端视口 <span data-v-058f7f11="">刷新头像</span>，title="刷新 QQ 头像")
                    CapsuleGlassButton(
                        onClick = onRefreshAvatar,
                        backdrop = backdrop,
                        isDark = isDark,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新 QQ 头像",
                                tint = titleColor,
                                modifier = Modifier.size(12.6.dp)
                            )
                            BasicText(
                                text = "刷新头像",
                                style = AppleHigTypography.footnote.copy(
                                    color = titleColor,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    // 修改资料
                    CapsuleGlassButton(
                        onClick = onEditProfile,
                        backdrop = backdrop,
                        isDark = isDark,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "修改资料",
                                tint = titleColor,
                                modifier = Modifier.size(12.6.dp)
                            )
                            BasicText(
                                text = "修改资料",
                                style = AppleHigTypography.footnote.copy(
                                    color = titleColor,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // MBTI 完整信息卡片（未填写 MBTI 时不渲染，保持原有布局）
        profile.mbti?.let { mbtiInfo ->
            Spacer(Modifier.height(cardSpacing))
            MbtiCard(
                mbti = mbtiInfo,
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // MBTI 人格测试入口：未测试过也能进；已测试过则显示「查看报告 / 重新测试」
        Spacer(Modifier.height(cardSpacing))
        AppleHigGroupedSection(
            title = "人格测试",
            footer = "官方 60 题问卷 · 生成你的 16 型人格报告",
            isDark = isDark
        ) {
            AppleHigRow(
                title = "MBTI 人格测试",
                subtitle = if (profile.mbti != null) "查看报告 / 重新测试" else "还没有测试过，去做一次",
                icon = Icons.Default.Psychology,
                iconBgColor = Color(0xFF8B5CF6),
                isDark = isDark,
                onClick = onOpenMbtiTest
            )
        }

        Spacer(Modifier.height(cardSpacing))

        // ============================================================
        // 2. 显示与外观 (44dp 标准单行 Inset Grouped)
        // ============================================================
        AppleHigGroupedSection(
            title = "显示与外观",
            footer = "开启后关闭液态玻璃特效，渲染效果降级",
            isDark = isDark
        ) {
            AppleHigRow(
                title = "主题模式",
                icon = Icons.Default.Palette,
                iconBgColor = Color(0xFF6366F1),
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidSegmentedControl(
                        options = listOf("系统", "浅色", "深色"),
                        selectedIndex = themeMode.ordinal,
                        onOptionSelect = { index ->
                            onThemeModeChange(AppThemeMode.entries[index])
                        },
                        modifier = Modifier.width(153.dp),
                        isDark = isDark
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            AppleHigRow(
                title = "渲染模式",
                icon = Icons.Default.AutoAwesome,
                iconBgColor = Color(0xFF8B5CF6),
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { liquidGlassCompat },
                        onSelect = onLiquidGlassCompatChange,
                        isDark = isDark
                    )
                }
            )
        }

        Spacer(Modifier.height(cardSpacing))

        // ============================================================
        // 2.5 消息与通知
        // ============================================================
        AppleHigGroupedSection(
            title = "消息与通知",
            footer = "关闭后 App 将停止后台保活，退到后台时无法接收系统通知",
            isDark = isDark
        ) {
            // 通知中心：入口行带未读数角标（点击进入官方「通知中心」面板）
            AppleHigRow(
                title = "通知中心",
                subtitle = if (notificationUnreadCount > 0) {
                    "有 $notificationUnreadCount 条未读通知"
                } else {
                    "系统提醒与互动消息"
                },
                icon = Icons.Default.Notifications,
                iconBgColor = Color(0xFFF59E0B),
                isDark = isDark,
                onClick = onOpenNotificationCenter,
                trailingContent = {
                    if (notificationUnreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            BasicText(
                                text = if (notificationUnreadCount > 99) "99+" else notificationUnreadCount.toString(),
                                style = AppleHigTypography.caption2.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = higColors.tertiaryLabel,
                        modifier = Modifier.size(11.7.dp)
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            AppleHigRow(
                title = "后台接收消息",
                icon = Icons.Default.NotificationsActive,
                iconBgColor = Color(0xFF10B981),
                isDark = isDark,
                accessory = AppleHigRowAccessory.Custom {
                    LiquidToggle(
                        selected = { backgroundReceiveEnabled },
                        onSelect = onBackgroundReceiveChange,
                        isDark = isDark
                    )
                }
            )

            AppleHigDivider(isDark = isDark, insetStart = 16.dp)

            AppleHigRow(
                title = "通知权限",
                value = if (notificationsEnabled) "已开启" else "未开启",
                icon = Icons.Default.Notifications,
                iconBgColor = Color(0xFF3B82F6),
                isDark = isDark,
                // 无论当前是否开启都进系统通知设置页：与 iOS「通知」一致，状态看 value、点按去改
                onClick = { SystemNotificationBridge.openNotificationSettings() }
            )
        }

        Spacer(Modifier.height(cardSpacing))

        // ============================================================
        // 3. 账号与安全 (44dp 标准列表项)
        // ============================================================
        AppleHigGroupedSection(
            title = "账号与安全",
            isDark = isDark
        ) {
            AppleHigRow(
                title = ProfileSettingItem.SECURITY_CENTER.title,
                icon = ProfileSettingItem.SECURITY_CENTER.icon,
                iconBgColor = ProfileSettingItem.SECURITY_CENTER.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.SECURITY_CENTER) }
            )
            AppleHigDivider(isDark = isDark)
            AppleHigRow(
                title = ProfileSettingItem.CHANGE_PASSWORD.title,
                icon = ProfileSettingItem.CHANGE_PASSWORD.icon,
                iconBgColor = ProfileSettingItem.CHANGE_PASSWORD.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.CHANGE_PASSWORD) }
            )
        }

        Spacer(Modifier.height(cardSpacing))

        // ============================================================
        // 4. 社交与隐私
        // ============================================================
        AppleHigGroupedSection(
            title = "社交与隐私",
            isDark = isDark
        ) {
            AppleHigRow(
                title = ProfileSettingItem.PRIVACY_SETTINGS.title,
                icon = ProfileSettingItem.PRIVACY_SETTINGS.icon,
                iconBgColor = ProfileSettingItem.PRIVACY_SETTINGS.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.PRIVACY_SETTINGS) }
            )
            AppleHigDivider(isDark = isDark)
            AppleHigRow(
                title = ProfileSettingItem.SEARCH_USERS.title,
                icon = ProfileSettingItem.SEARCH_USERS.icon,
                iconBgColor = ProfileSettingItem.SEARCH_USERS.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.SEARCH_USERS) }
            )
            AppleHigDivider(isDark = isDark)
            AppleHigRow(
                title = ProfileSettingItem.MY_BLOCKLIST.title,
                icon = ProfileSettingItem.MY_BLOCKLIST.icon,
                iconBgColor = ProfileSettingItem.MY_BLOCKLIST.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.MY_BLOCKLIST) }
            )
        }

        Spacer(Modifier.height(cardSpacing))

        // ============================================================
        // 5. 服务与支持
        // ============================================================
        AppleHigGroupedSection(
            title = "服务与支持",
            isDark = isDark
        ) {
            AppleHigRow(
                title = ProfileSettingItem.MY_REPORTS.title,
                icon = ProfileSettingItem.MY_REPORTS.icon,
                iconBgColor = ProfileSettingItem.MY_REPORTS.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.MY_REPORTS) }
            )
            AppleHigDivider(isDark = isDark)
            AppleHigRow(
                title = ProfileSettingItem.PENALTY_RELIEF.title,
                icon = ProfileSettingItem.PENALTY_RELIEF.icon,
                iconBgColor = ProfileSettingItem.PENALTY_RELIEF.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.PENALTY_RELIEF) }
            )
            AppleHigDivider(isDark = isDark)
            AppleHigRow(
                title = ProfileSettingItem.NETEASE_BIND.title,
                icon = ProfileSettingItem.NETEASE_BIND.icon,
                iconBgColor = ProfileSettingItem.NETEASE_BIND.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.NETEASE_BIND) }
            )
            AppleHigDivider(isDark = isDark)
            AppleHigRow(
                title = ProfileSettingItem.FEEDBACK.title,
                icon = ProfileSettingItem.FEEDBACK.icon,
                iconBgColor = ProfileSettingItem.FEEDBACK.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.FEEDBACK) }
            )
            AppleHigDivider(isDark = isDark)
            AppleHigRow(
                title = ProfileSettingItem.CONTACT_US.title,
                icon = ProfileSettingItem.CONTACT_US.icon,
                iconBgColor = ProfileSettingItem.CONTACT_US.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.CONTACT_US) }
            )
            AppleHigDivider(isDark = isDark)
            AppleHigRow(
                title = ProfileSettingItem.DONATION.title,
                icon = ProfileSettingItem.DONATION.icon,
                iconBgColor = ProfileSettingItem.DONATION.iconBgColor,
                isDark = isDark,
                onClick = { onOpenSetting(ProfileSettingItem.DONATION) }
            )
        }

        Spacer(Modifier.height(cardSpacing))

        // ============================================================
        // 6. 危险操作区 (注销账号)
        // ============================================================
        AppleHigGroupedSection(
            isDark = isDark,
            footer = "注销后账号所有数据将无法找回，请谨慎操作"
        ) {
            AppleHigRow(
                title = "注销账号",
                icon = Icons.Default.DeleteForever,
                iconBgColor = higColors.destructive,
                isDestructive = true,
                isDark = isDark,
                onClick = onDeleteAccount
            )
        }
    }
}
