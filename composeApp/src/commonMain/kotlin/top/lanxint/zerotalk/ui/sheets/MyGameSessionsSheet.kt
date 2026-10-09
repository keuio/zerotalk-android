package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.data.model.GameType
import top.lanxint.zerotalk.data.model.MyGameSessionItem
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.theme.AppleHigColors

/**
 * 我的游戏对局列表抽屉
 *
 * 调用 GET /api/game/my-sessions 获取历史/进行中的全部对局
 */
@Composable
fun MyGameSessionsSheet(
    clientManager: ZeroTalkClientManager,
    isDark: Boolean,
    onSelectSession: (gameType: String, gameId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var sessions by remember { mutableStateOf<List<MyGameSessionItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val res = clientManager.getMyGameSessions()
            if (res.isSuccess) {
                sessions = res.getOrDefault(emptyList())
            } else {
                errorMessage = res.exceptionOrNull()?.message ?: "加载对局记录失败"
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 顶部标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "我的游戏对局",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = higColors.label
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = higColors.secondaryLabel,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }
            }

            sessions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无对局记录",
                        color = higColors.secondaryLabel,
                        fontSize = 14.sp
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessions) { session ->
                        val gameName = GameType.getDisplayName(session.gameType)
                        val isPlaying = session.status == "playing"
                        val isWaiting = session.status == "waiting"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectSession(session.gameType, session.gameId)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = gameName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = higColors.label
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val statusBadgeColor = when (session.status) {
                                            "playing" -> Color(0xFF34C759)
                                            "waiting" -> Color(0xFFFF9500)
                                            "finished" -> Color(0xFF8E8E93)
                                            else -> Color(0xFF8E8E93)
                                        }
                                        val statusText = when (session.status) {
                                            "playing" -> "进行中"
                                            "waiting" -> "等人应战"
                                            "finished" -> "已结束"
                                            "cancelled" -> "已取消"
                                            else -> session.status
                                        }
                                        Surface(
                                            color = statusBadgeColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                color = statusBadgeColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    val vsText = buildString {
                                        val p1 = session.whiteUsername ?: session.redUsername
                                        val p2 = session.blackUsername
                                        if (p1 != null) append(p1)
                                        if (p2 != null) append(" vs $p2")
                                    }
                                    if (vsText.isNotBlank()) {
                                        Text(
                                            text = vsText,
                                            fontSize = 13.sp,
                                            color = higColors.secondaryLabel
                                        )
                                    }

                                    session.createdAt?.let { time ->
                                        Text(
                                            text = time,
                                            fontSize = 11.sp,
                                            color = higColors.tertiaryLabel
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        onSelectSession(session.gameType, session.gameId)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlaying || isWaiting) Color(0xFF007AFF) else Color(0xFF8E8E93)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isPlaying) "进入对局" else if (isWaiting) "查看房间" else "查看棋局",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
