package top.lanxint.zerotalk.ui.sheets

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.network.SavedAccount
import top.lanxint.zerotalk.ui.components.AppleHigDivider
import top.lanxint.zerotalk.ui.components.AppleHigGroupedSection
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 「切换账号」面板内容
 *
 * 列出本机保存过的账号（最近使用在最前），点一行即可切换；每行可单独删除（连同本地密码）。
 * 底部提供「添加账号」入口：切到登录表单，登录成功后会自动记入本列表。
 *
 * @param accounts 本地已保存账号（[top.lanxint.zerotalk.data.network.AccountStore]）
 * @param activeLoginName 当前选中账号的登录名
 * @param switching 是否正在切换（切换期间禁用点击，避免并发重登）
 */
@Composable
fun SheetAccountSwitcherContent(
    accounts: List<SavedAccount>,
    activeLoginName: String?,
    switching: Boolean,
    isDark: Boolean,
    onSwitch: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddAccount: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    var pendingDelete by remember { mutableStateOf<SavedAccount?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        if (accounts.isEmpty()) {
            // 没有保存过账号（例如刚退出登录并删除了全部账号）
            AppleHigGroupedSection(
                isDark = isDark,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "本机还没有保存过账号",
                        style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                    )
                }
            }
        } else {
            AppleHigGroupedSection(
                isDark = isDark,
                footer = "账号密码仅加密保存在本机；删除账号会同时清除其本地密码；添加账号后会切换到新账号",
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                accounts.forEachIndexed { index, account ->
                    if (index > 0) AppleHigDivider(isDark = isDark, insetStart = 16.dp)
                    AccountRow(
                        account = account,
                        isActive = account.loginName == activeLoginName,
                        enabled = !switching,
                        isDark = isDark,
                        onSwitch = { onSwitch(account.loginName) },
                        onDelete = { pendingDelete = account }
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // 添加账号：复用「登录账号」表单，登录成功后自动记入列表
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(higColors.tint.copy(alpha = if (isDark) 0.22f else 0.12f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !switching,
                    onClick = onAddAccount
                )
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = higColors.tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            BasicText(
                text = "添加账号",
                style = AppleHigTypography.subhead.copy(
                    color = higColors.tint,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        if (switching) {
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BasicText(
                    text = "正在切换账号…",
                    style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                )
            }
        }
    }

    // 删除二次确认：避免误触把本地密码抹掉
    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                BasicText(
                    text = "删除本地账号？",
                    style = AppleHigTypography.headline.copy(color = higColors.label)
                )
            },
            text = {
                BasicText(
                    text = "将删除「${target.displayName}」在本机保存的登录信息，服务端账号不受影响。",
                    style = AppleHigTypography.body.copy(color = higColors.secondaryLabel)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.loginName)
                    pendingDelete = null
                }) {
                    BasicText(
                        text = "删除",
                        style = AppleHigTypography.body.copy(color = Color(0xFFFF3B30))
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    BasicText(
                        text = "取消",
                        style = AppleHigTypography.body.copy(color = higColors.tint)
                    )
                }
            }
        )
    }
}

/**
 * 单个账号行：头像 + 昵称 + 登录名 + 「当前」标记 + 删除按钮
 */
@Composable
private fun AccountRow(
    account: SavedAccount,
    isActive: Boolean,
    enabled: Boolean,
    isDark: Boolean,
    onSwitch: () -> Unit,
    onDelete: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled && !isActive,
                onClick = onSwitch
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            url = account.avatarUrl.takeIf { it.isNotBlank() },
            name = account.displayName,
            size = 40.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = account.displayName,
                    style = AppleHigTypography.subhead.copy(
                        color = higColors.label,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isActive) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(higColors.tint.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        BasicText(
                            text = "当前",
                            style = AppleHigTypography.caption2.copy(
                                color = higColors.tint,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = account.loginName,
                style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // 删除该账号（连同本地保存的密码）
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    onClick = onDelete
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "删除本地账号",
                tint = higColors.systemRed.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
