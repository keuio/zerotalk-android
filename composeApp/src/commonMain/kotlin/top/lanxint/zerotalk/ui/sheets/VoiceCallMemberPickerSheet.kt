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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.network.RoomMemberDto
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 群聊语音通话 · 成员选择器（对齐官网 ChatView 的 `voice-picker-panel`）
 *
 * 官网文案：「请选择一位成员发起语音通话」；选中成员后以
 * `voice_invite{room_id, to_user_id, source:"room"}` 呼叫该成员。
 */
@Composable
fun VoiceCallMemberPickerSheet(
    roomId: String,
    isDark: Boolean,
    onPick: (member: RoomMemberDto) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppleHigColors.colors(isDark)
    val membersMap by ZeroTalkClientManager.roomMembers.collectAsState()
    val loadingMap by ZeroTalkClientManager.roomMembersLoading.collectAsState()

    LaunchedEffect(roomId) {
        ZeroTalkClientManager.loadRoomMembers(roomId)
    }

    val members = (membersMap[roomId] ?: emptyList())
        .filter { !it.isSelf && (it.uid.isNotBlank() || it.userId > 0L) }
    val loading = loadingMap[roomId] == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = "语音通话",
            style = AppleHigTypography.title3.copy(color = colors.label, fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = "请选择一位成员发起语音通话",
            style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel)
        )
        Spacer(Modifier.height(12.dp))

        when {
            loading && members.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "正在加载成员…",
                        style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel)
                    )
                }
            }

            members.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "暂无可呼叫的成员",
                        style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel)
                    )
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(members, key = { it.uid.ifBlank { it.userId.toString() } }) { member ->
                        VoiceCallMemberRow(
                            member = member,
                            isDark = isDark,
                            onClick = { onPick(member) }
                        )
                        HorizontalDivider(color = colors.separator.copy(alpha = 0.4f))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VoiceCallMemberRow(
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
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            BasicText(
                text = member.username.ifBlank { "神秘零友" },
                style = AppleHigTypography.subhead.copy(color = colors.label, fontWeight = FontWeight.Medium)
            )
            val role = when {
                member.isCreator -> "群主"
                member.isAdmin -> "管理员"
                else -> ""
            }
            if (role.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = role,
                    style = AppleHigTypography.caption1.copy(color = colors.secondaryLabel, fontSize = 11.sp)
                )
            }
        }
        BasicText(
            text = "呼叫",
            style = AppleHigTypography.footnote.copy(color = Color(0xFF34C759), fontWeight = FontWeight.Medium)
        )
    }
}
