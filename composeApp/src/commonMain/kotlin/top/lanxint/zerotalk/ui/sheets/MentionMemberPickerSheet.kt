package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.lanxint.zerotalk.data.network.RoomMemberDto
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 群聊 @ 成员选择器弹窗面板
 *
 * 在暗号群聊中输入「@」或点击 @ 扩展时弹出，支持快速搜索与点选群成员。
 */
@Composable
fun MentionMemberPickerSheet(
    roomId: String,
    isDark: Boolean,
    onPick: (member: RoomMemberDto) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppleHigColors.colors(isDark)
    val membersMap by ZeroTalkClientManager.roomMembers.collectAsStateWithLifecycle()
    val loadingMap by ZeroTalkClientManager.roomMembersLoading.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(roomId) {
        ZeroTalkClientManager.loadRoomMembers(roomId)
    }

    val allMembers = (membersMap[roomId] ?: emptyList())
        .filter { !it.isSelf && (it.uid.isNotBlank() || it.userId > 0L) }
    val loading = loadingMap[roomId] == true

    val filteredMembers = remember(allMembers, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            allMembers
        } else {
            allMembers.filter { member ->
                member.username.contains(query, ignoreCase = true) ||
                    member.uid.contains(query, ignoreCase = true) ||
                    (member.title?.contains(query, ignoreCase = true) == true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                BasicText(
                    text = "选择要 @ 的成员",
                    style = AppleHigTypography.title3.copy(color = colors.label, fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = if (allMembers.isNotEmpty()) "共 ${allMembers.size} 位群成员" else "成员将收到 @ 提及提醒",
                    style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel)
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.tertiarySystemGroupedBackground)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "取消",
                    style = AppleHigTypography.subhead.copy(color = colors.tint, fontWeight = FontWeight.Medium)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 搜索框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.quaternarySystemFill)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colors.tertiaryLabel,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = AppleHigTypography.body.copy(color = colors.label),
                cursorBrush = SolidColor(colors.tint),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (searchQuery.isEmpty()) {
                        BasicText(
                            text = "搜索成员昵称",
                            style = AppleHigTypography.body.copy(color = colors.tertiaryLabel)
                        )
                    }
                    inner()
                }
            )
            if (searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.tertiaryLabel)
                        .clickable { searchQuery = "" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清空",
                        tint = colors.systemBackground,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            loading && allMembers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "正在加载群成员…",
                        style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel)
                    )
                }
            }

            allMembers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "群内暂无可 @ 的其他成员",
                        style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel)
                    )
                }
            }

            filteredMembers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "未找到匹配的群成员",
                        style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel)
                    )
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(filteredMembers, key = { it.uid.ifBlank { it.userId.toString() } }) { member ->
                        MentionMemberRow(
                            member = member,
                            isDark = isDark,
                            onClick = { onPick(member) }
                        )
                        HorizontalDivider(color = colors.separator.copy(alpha = 0.35f))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MentionMemberRow(
    member: RoomMemberDto,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val colors = AppleHigColors.colors(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            url = member.avatarUrl,
            name = member.username,
            size = 40.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = member.username.ifBlank { "零友" },
                    style = AppleHigTypography.subhead.copy(color = colors.label, fontWeight = FontWeight.Medium)
                )

                if (member.isCreator) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF9500).copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(
                            text = "房主",
                            style = AppleHigTypography.caption2.copy(
                                color = Color(0xFFFF9500),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                } else if (member.isAdmin) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.tint.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(
                            text = "管理员",
                            style = AppleHigTypography.caption2.copy(
                                color = colors.tint,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                val titleText = member.title?.takeIf { it.isNotBlank() }
                if (titleText != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.secondarySystemFill)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(
                            text = titleText,
                            style = AppleHigTypography.caption2.copy(
                                color = colors.secondaryLabel,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.tint.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "@",
                style = AppleHigTypography.subhead.copy(
                    color = colors.tint,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
