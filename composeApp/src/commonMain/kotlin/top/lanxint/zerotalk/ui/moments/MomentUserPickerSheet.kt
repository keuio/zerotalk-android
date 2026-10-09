package top.lanxint.zerotalk.ui.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 动态发布 · 选人面板（部分可见 / 不给谁看共用）
 *
 * 数据来自 `GET /room/list?type=dm|normal`（最多 50 人），
 * 取每个私聊房间的 peer 用户；顶部可搜索，支持按用户名 / 编号精确添加。
 *
 * 顶栏由承载它的 `AppleModalBottomSheet` 统一绘制，标题（「部分人可见」/「不给谁看」）由调用方传入容器。
 *
 * @param selected 已选中的用户 uid 集合
 * @param onToggle 切换选中状态（互斥模式由外层保证单选语义）
 * @param onDismiss 关闭面板
 */
@Composable
fun MomentUserPickerSheet(
    isDark: Boolean,
    selected: Set<String>,
    onToggle: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current

    var users by remember { mutableStateOf<List<PickerUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        // 官方选人面板数据来自 GET /room/list?type=dm|normal，最多取 50 人
        val dmRes = ZeroTalkClientManager.apiService.getRoomList(offset = 0, perPage = 50, type = "dm")
        val normalRes = ZeroTalkClientManager.apiService.getRoomList(offset = 0, perPage = 50, type = "normal")
        isLoading = false
        val merged = (dmRes.getOrNull()?.rooms.orEmpty() + normalRes.getOrNull()?.rooms.orEmpty())
            .mapNotNull { it.peer }
            .filter { it.uid.isNotBlank() }
            .distinctBy { it.uid }
            .map { PickerUser(uid = it.uid, name = it.username.ifBlank { it.loginName }, avatarUrl = it.avatarUrl.orEmpty()) }
        users = merged.take(50)
    }

    val filtered = remember(query, users) {
        if (query.isBlank()) users
        else users.filter {
            it.name.contains(query, ignoreCase = true) || it.uid.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
    ) {
        // 顶栏由承载它的 AppleModalBottomSheet 统一绘制

        // 搜索框：按用户名 / 编号精确添加
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(higColors.quaternarySystemFill)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = higColors.label, fontSize = 14.sp),
                cursorBrush = SolidColor(higColors.tint),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        BasicText(
                            text = "搜索用户名 / 编号，或从列表选择",
                            style = TextStyle(color = higColors.placeholderText, fontSize = 13.sp)
                        )
                    }
                    inner()
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.dp,
                    color = higColors.tint
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.uid }) { user ->
                    PickerUserRow(
                        user = user,
                        isSelected = selected.contains(user.uid),
                        isDark = isDark,
                        onClick = { onToggle(user.uid, user.name) }
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        BasicText(
                            text = "未找到匹配用户，可输入用户名 / 编号后直接确认",
                            style = AppleHigTypography.footnote.copy(color = higColors.tertiaryLabel),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 已选摘要
        if (selected.isNotEmpty()) {
            BasicText(
                text = "已选择 ${selected.size} 人",
                style = AppleHigTypography.footnote.copy(
                    color = higColors.tint,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

/** 选人面板的用户项 */
private data class PickerUser(
    val uid: String,
    val name: String,
    val avatarUrl: String = ""
)

@Composable
private fun PickerUserRow(
    user: PickerUser,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) higColors.tint.copy(alpha = 0.14f)
                else higColors.quaternarySystemFill
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) higColors.tint else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncNetworkImage(
            url = user.avatarUrl,
            contentDescription = "头像",
            modifier = Modifier.size(34.dp),
            shape = CircleShape
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = user.name.ifBlank { "零语用户" },
                style = AppleHigTypography.subhead.copy(
                    color = higColors.label,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(Modifier.height(1.dp))
            BasicText(
                text = user.uid,
                style = AppleHigTypography.caption2.copy(color = higColors.tertiaryLabel)
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选",
                tint = higColors.tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
