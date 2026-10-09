package top.lanxint.zerotalk.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import top.lanxint.zerotalk.data.model.*
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage

/**
 * 谁是卧底房间全屏/内嵌组件
 */
@Composable
fun UndercoverRoom(
    session: GameSessionDetail,
    currentUserId: String,
    onReady: (Boolean) -> Unit,
    onStart: () -> Unit,
    onLeave: () -> Unit,
    onCancel: () -> Unit,
    onConfirmReveal: () -> Unit,
    onDescribe: (String) -> Unit,
    onVote: (Long) -> Unit,
    onWhiteGuess: (String) -> Unit,
    onKick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = session.phase ?: "WAITING"
    val isHost = session.hostUid == currentUserId || session.creatorUid == currentUserId
    val myPlayer = session.players.find { it.uid == currentUserId }
    val isJoined = myPlayer != null
    val myReady = myPlayer?.ready ?: false

    // 本地倒计时计算 (基于 phaseDeadline 和 serverNow)
    var remainingSeconds by remember(session.phaseDeadline, session.serverNow) {
        val deadline = session.phaseDeadline
        val sNow = session.serverNow
        val initialSec = if (deadline != null && sNow != null && deadline > sNow) {
            (deadline - sNow).toInt()
        } else null
        mutableStateOf(initialSec)
    }

    LaunchedEffect(remainingSeconds) {
        val sec = remainingSeconds
        if (sec != null && sec > 0) {
            delay(1000L)
            remainingSeconds = sec - 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部阶段条 & 倒计时
        PhaseHeader(
            phase = phase,
            remainingSeconds = remainingSeconds,
            round = session.round
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 座位席列表
        PlayersSeatGrid(
            players = session.players,
            maxPlayers = session.settings?.maxPlayers ?: 4,
            currentUserId = currentUserId,
            isHost = isHost,
            phase = phase,
            onKick = onKick,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 阶段交互主操作区
        when (phase) {
            "WAITING" -> {
                WaitingPhaseControls(
                    isHost = isHost,
                    isJoined = isJoined,
                    myReady = myReady,
                    players = session.players,
                    onReady = onReady,
                    onStart = onStart,
                    onLeave = onLeave,
                    onCancel = onCancel
                )
            }
            "ROLE_REVEAL" -> {
                RoleRevealControls(
                    me = session.me,
                    confirmed = myPlayer?.confirmedReveal ?: false,
                    onConfirm = onConfirmReveal
                )
            }
            "DESCRIPTION" -> {
                DescriptionPhaseControls(
                    session = session,
                    currentUserId = currentUserId,
                    onDescribe = onDescribe
                )
            }
            "DISCUSSION" -> {
                Text(
                    text = "自由讨论阶段，请开麦或打字交流发言",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            "VOTING" -> {
                VotingPhaseControls(
                    session = session,
                    currentUserId = currentUserId,
                    onVote = onVote
                )
            }
            "WHITE_GUESS" -> {
                WhiteGuessControls(
                    session = session,
                    currentUserId = currentUserId,
                    onWhiteGuess = onWhiteGuess
                )
            }
            "GAME_OVER" -> {
                GameOverSummary(
                    session = session,
                    onLeave = onLeave
                )
            }
            else -> {
                Text(
                    text = "当前阶段: $phase",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PhaseHeader(
    phase: String,
    remainingSeconds: Int?,
    round: Int
) {
    val phaseName = when (phase) {
        "WAITING" -> "组队准备中"
        "ROLE_REVEAL" -> "查看词语身份"
        "DESCRIPTION" -> "第 $round 轮 · 描述中"
        "DISCUSSION" -> "自由讨论阶段"
        "VOTING" -> "投票放逐阶段"
        "VOTE_RESULT" -> "投票结果公布"
        "WHITE_GUESS" -> "白板猜词中"
        "GAME_OVER" -> "游戏结束"
        else -> phase
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = phaseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (remainingSeconds != null) {
                val isUrgent = remainingSeconds <= 5
                Text(
                    text = "${remainingSeconds}s",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUrgent) Color(0xFFE53E3E) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PlayersSeatGrid(
    players: List<UndercoverPlayer>,
    maxPlayers: Int,
    currentUserId: String,
    isHost: Boolean,
    phase: String,
    onKick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(players) { p ->
                val isMe = p.uid == currentUserId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp)) {
                        AsyncNetworkImage(
                            url = p.avatarUrl.orEmpty(),
                            contentDescription = p.username,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                        // 座位号角标
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${p.seat}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = p.username + if (isMe) " (我)" else "",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!p.alive) {
                            Text(
                                text = "已出局" + if (p.role != null) " · ${p.role}" else "",
                                fontSize = 12.sp,
                                color = Color(0xFFE53E3E)
                            )
                        }
                    }

                    // WAITING 阶段展示准备状态或房主踢人
                    if (phase == "WAITING") {
                        if (p.ready) {
                            Text(
                                text = "已准备",
                                fontSize = 12.sp,
                                color = Color(0xFF38A169),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "未准备",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isHost && !isMe) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onKick(p.userId) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "移出",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 空位提示
            val emptySeats = maxPlayers - players.size
            if (emptySeats > 0 && phase == "WAITING") {
                item {
                    Text(
                        text = "等待其他玩家加入 ($emptySeats 个空位)...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingPhaseControls(
    isHost: Boolean,
    isJoined: Boolean,
    myReady: Boolean,
    players: List<UndercoverPlayer>,
    onReady: (Boolean) -> Unit,
    onStart: () -> Unit,
    onLeave: () -> Unit,
    onCancel: () -> Unit
) {
    val canStart = players.size >= 4 && players.all { it.ready }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "满 4 人且全部准备后，房主可开始游戏",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isJoined) {
                Button(
                    onClick = { onReady(!myReady) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (myReady) Color(0xFF718096) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (myReady) "取消准备" else "准备")
                }
            }

            if (isHost) {
                Button(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("开始游戏")
                }

                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("取消对局")
                }
            } else if (isJoined) {
                OutlinedButton(onClick = onLeave) {
                    Text("离开")
                }
            }
        }
    }
}

@Composable
private fun RoleRevealControls(
    me: UndercoverMe?,
    confirmed: Boolean,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "你的身份与词语",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val wordText = if (me?.role == "blank") "你是白板！没有词语" else me?.word.orEmpty().ifEmpty { "词语加载中..." }
            Text(
                text = wordText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!confirmed) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("我记住了")
                }
            } else {
                Text(
                    text = "已确认，等待其他玩家确认身份...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DescriptionPhaseControls(
    session: GameSessionDetail,
    currentUserId: String,
    onDescribe: (String) -> Unit
) {
    val currentDescribeUid = session.order.getOrNull(session.describeIndex)
    val isMyTurnToDescribe = currentDescribeUid == currentUserId
    var descText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isMyTurnToDescribe) {
            Text(
                text = "轮到你发言描述了：",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    placeholder = { Text("一句话描述你的词语...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (descText.isNotBlank()) {
                            onDescribe(descText.trim())
                            descText = ""
                        }
                    },
                    enabled = descText.isNotBlank()
                ) {
                    Text("发送")
                }
            }
        } else {
            val speaker = session.players.find { it.uid == currentDescribeUid }
            Text(
                text = "等待 ${speaker?.username ?: "玩家"} 描述发言...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VotingPhaseControls(
    session: GameSessionDetail,
    currentUserId: String,
    onVote: (Long) -> Unit
) {
    val myPlayer = session.players.find { it.uid == currentUserId }
    val hasVoted = session.votes?.votedUids?.contains(currentUserId) == true
    val candidates = session.players.filter { it.alive && it.uid != currentUserId }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (hasVoted) "你已完成投票，等待其他人投票结果..." else "请选择你怀疑是卧底的玩家投出：",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (!hasVoted && myPlayer?.alive == true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                candidates.forEach { c ->
                    Button(
                        onClick = { onVote(c.userId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(c.username, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun WhiteGuessControls(
    session: GameSessionDetail,
    currentUserId: String,
    onWhiteGuess: (String) -> Unit
) {
    val isWhite = session.me?.role == "blank"
    var guessText by remember { mutableStateOf("") }

    if (isWhite) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "你已被放逐！猜出平民的词语即可逆转获胜：",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = guessText,
                    onValueChange = { guessText = it },
                    placeholder = { Text("输入你猜测的平民词语...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (guessText.isNotBlank()) {
                            onWhiteGuess(guessText.trim())
                        }
                    },
                    enabled = guessText.isNotBlank()
                ) {
                    Text("提交猜测")
                }
            }
        }
    } else {
        Text(
            text = "白板正在猜词中，若猜中则白板胜利...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GameOverSummary(
    session: GameSessionDetail,
    onLeave: () -> Unit
) {
    val winnerTitle = when (session.winnerCamp) {
        "civilian" -> "平民阵营胜利 🎉"
        "undercover" -> "卧底阵营胜利 🕵️"
        "blank" -> "白板单独胜利 🃏"
        else -> "对局结束"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = winnerTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))

        session.words?.let { words ->
            Text(
                text = "平民词: ${words["civilian_word"].orEmpty()}  |  卧底词: ${words["undercover_word"].orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("退出房间")
        }
    }
}
