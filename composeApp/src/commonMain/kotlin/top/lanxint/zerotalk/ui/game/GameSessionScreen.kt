package top.lanxint.zerotalk.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
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
import top.lanxint.zerotalk.data.model.GameSessionDetail
import top.lanxint.zerotalk.data.model.GameType
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage

/**
 * 沉浸式独立游戏对局页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSessionScreen(
    clientManager: ZeroTalkClientManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session by clientManager.activeGameSession.collectAsState()
    val gameType by clientManager.activeGameType.collectAsState()
    val gameError by clientManager.activeGameError.collectAsState()
    val currentUserId = clientManager.currentUserId ?: ""

    var showResignConfirm by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }

    val currentSession = session
    val resolvedType = gameType ?: currentSession?.gameType ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = GameType.getDisplayName(resolvedType),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val statusText = when (currentSession?.status) {
                            "waiting" -> "等待对手应战"
                            "playing" -> if (currentSession.inCheck) "对局中 · 将军！" else "对局中 · 第 ${currentSession.moveCount} 手"
                            "finished" -> "对局已结束"
                            "cancelled" -> "对局已取消"
                            else -> "连接中..."
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        clientManager.closeGameSession()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (currentSession != null) {
                        val isCreator = currentSession.creatorUid == currentUserId
                        val isPlayer = currentSession.whiteUid == currentUserId ||
                                currentSession.blackUid == currentUserId ||
                                currentSession.redUid == currentUserId

                        if (currentSession.status == "waiting" && isCreator) {
                            TextButton(onClick = { showCancelConfirm = true }) {
                                Text("取消邀请", color = MaterialTheme.colorScheme.error)
                            }
                        } else if (currentSession.status == "playing" && isPlayer && resolvedType != GameType.UNDERCOVER) {
                            TextButton(onClick = { showResignConfirm = true }) {
                                Text("认输", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (currentSession == null) {
                // 加载中状态
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 非卧底游戏展示双方对战玩家信息栏
                    if (resolvedType != GameType.UNDERCOVER) {
                        GameMatchHeader(
                            session = currentSession,
                            gameType = resolvedType,
                            currentUserId = currentUserId
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 错误提示条
                    AnimatedVisibility(visible = gameError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = gameError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // 核心游戏棋盘/房间区域
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (resolvedType) {
                            GameType.GOBANG -> {
                                val isBlack = currentSession.blackUid == currentUserId
                                val isWhite = currentSession.whiteUid == currentUserId
                                val myColor = when {
                                    isBlack -> 2
                                    isWhite -> 1
                                    else -> 0
                                }
                                val isMyTurn = currentSession.status == "playing" && (
                                        (currentSession.turn == 1 && isWhite) ||
                                                (currentSession.turn == 2 && isBlack)
                                        )

                                GobangBoard(
                                    boardString = currentSession.board,
                                    lastX = currentSession.lastX,
                                    lastY = currentSession.lastY,
                                    isMyTurn = isMyTurn,
                                    onMove = { x, y ->
                                        clientManager.sendGobangMove(x, y)
                                    }
                                )
                            }

                            GameType.GO -> {
                                val isBlack = currentSession.blackUid == currentUserId
                                val isWhite = currentSession.whiteUid == currentUserId
                                val isMyTurn = currentSession.status == "playing" && (
                                        (currentSession.turn == 1 && isBlack) || // 围棋黑先
                                                (currentSession.turn == 2 && isWhite)
                                        )

                                GoBoard(
                                    boardString = currentSession.board,
                                    lastX = currentSession.lastX,
                                    lastY = currentSession.lastY,
                                    captures = currentSession.captures,
                                    isMyTurn = isMyTurn,
                                    onMove = { x, y ->
                                        clientManager.sendGoMove(x, y)
                                    },
                                    onPass = {
                                        clientManager.sendGoPass()
                                    }
                                )
                            }

                            GameType.XIANGQI -> {
                                val isRed = currentSession.redUid == currentUserId
                                val isBlack = currentSession.blackUid == currentUserId
                                val mySide = if (isRed) "red" else if (isBlack) "black" else "spectator"
                                val isMyTurn = currentSession.status == "playing" && (
                                        (currentSession.turn == 1 && isRed) ||
                                                (currentSession.turn == 2 && isBlack)
                                        )

                                val lastMoveList = (currentSession.lastMove as? List<*>)?.mapNotNull {
                                    (it as? Number)?.toInt()
                                }

                                XiangqiBoard(
                                    boardString = currentSession.board,
                                    legalMoves = currentSession.legalMoves,
                                    inCheck = currentSession.inCheck,
                                    lastMove = lastMoveList,
                                    isMyTurn = isMyTurn,
                                    mySide = mySide,
                                    onMove = { ff, fr, tf, tr ->
                                        clientManager.sendXiangqiMove(ff, fr, tf, tr)
                                    }
                                )
                            }

                            GameType.CHESS -> {
                                val isWhite = currentSession.whiteUid == currentUserId
                                val isBlack = currentSession.blackUid == currentUserId
                                val mySide = if (isWhite) "white" else if (isBlack) "black" else "spectator"
                                val turnSide = if (currentSession.turn == 1) "white" else "black"
                                val isMyTurn = currentSession.status == "playing" && (
                                        (currentSession.turn == 1 && isWhite) ||
                                                (currentSession.turn == 2 && isBlack)
                                        )

                                val lastMoveList = (currentSession.lastMove as? List<*>)?.mapNotNull {
                                    (it as? Number)?.toInt()
                                }

                                ChessBoard(
                                    boardString = currentSession.board,
                                    legalMoves = currentSession.legalMoves,
                                    inCheck = currentSession.inCheck,
                                    lastMove = lastMoveList,
                                    isMyTurn = isMyTurn,
                                    mySide = mySide,
                                    turnSide = turnSide,
                                    onMove = { ff, fr, tf, tr, promo ->
                                        clientManager.sendChessMove(ff, fr, tf, tr, promo)
                                    }
                                )
                            }

                            GameType.UNDERCOVER -> {
                                UndercoverRoom(
                                    session = currentSession,
                                    currentUserId = currentUserId,
                                    onReady = { ready -> clientManager.sendUndercoverReady(ready) },
                                    onStart = { clientManager.sendUndercoverStart() },
                                    onLeave = { clientManager.sendUndercoverLeave() },
                                    onCancel = { clientManager.sendUndercoverCancel() },
                                    onConfirmReveal = { clientManager.sendUndercoverConfirm() },
                                    onDescribe = { text -> clientManager.sendUndercoverDescribe(text) },
                                    onVote = { targetId -> clientManager.sendUndercoverVote(targetId) },
                                    onWhiteGuess = { guess -> clientManager.sendUndercoverWhiteGuess(guess) },
                                    onKick = { targetId -> clientManager.sendUndercoverKick(targetId) }
                                )
                            }
                        }
                    }

                    // 底部对局状态提示栏
                    if (resolvedType != GameType.UNDERCOVER) {
                        GameTurnFooter(
                            session = currentSession,
                            currentUserId = currentUserId,
                            gameType = resolvedType
                        )
                    }
                }
            }
        }
    }

    // 确认认输弹窗
    if (showResignConfirm) {
        AlertDialog(
            onDismissRequest = { showResignConfirm = false },
            title = { Text("确认认输？") },
            text = { Text("认输后本局将被判定为对手获胜。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResignConfirm = false
                        clientManager.sendGameResign()
                    }
                ) {
                    Text("认输", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResignConfirm = false }) {
                    Text("继续对局")
                }
            }
        )
    }

    // 确认取消弹窗
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("取消对局邀请？") },
            text = { Text("取消后等待中的邀请卡片将关闭。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                        clientManager.sendGameCancel()
                    }
                ) {
                    Text("取消对局", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text("返回")
                }
            }
        )
    }
}

/**
 * 双方玩家信息头栏
 */
@Composable
private fun GameMatchHeader(
    session: GameSessionDetail,
    gameType: String,
    currentUserId: String
) {
    // 双方信息解析
    val isXiangqi = gameType == GameType.XIANGQI
    val player1Name = if (isXiangqi) session.redUsername ?: "红方" else session.whiteUsername ?: "白方"
    val player1Avatar = if (isXiangqi) session.redAvatarUrl else session.whiteAvatarUrl
    val player1Uid = if (isXiangqi) session.redUid else session.whiteUid
    val player1SideName = if (isXiangqi) "红方" else "白方"

    val player2Name = session.blackUsername ?: if (session.status == "waiting") "等待应战..." else "黑方"
    val player2Avatar = session.blackAvatarUrl
    val player2Uid = session.blackUid
    val player2SideName = "黑方"

    val isPlayer1Turn = session.turn == 1
    val isPlayer2Turn = session.turn == 2

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 玩家 1 (白/红)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box {
                    AsyncNetworkImage(
                        url = player1Avatar.orEmpty(),
                        contentDescription = player1Name,
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                    )
                    if (isPlayer1Turn && session.status == "playing") {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38A169))
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = player1Name + if (player1Uid == currentUserId) " (我)" else "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = player1SideName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // VS 标牌
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "VS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 玩家 2 (黑)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = player2Name + if (player2Uid == currentUserId) " (我)" else "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = player2SideName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    AsyncNetworkImage(
                        url = player2Avatar.orEmpty(),
                        contentDescription = player2Name,
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                    )
                    if (isPlayer2Turn && session.status == "playing") {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38A169))
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 底部回合与提示栏
 */
@Composable
private fun GameTurnFooter(
    session: GameSessionDetail,
    currentUserId: String,
    gameType: String
) {
    val isPlayer = session.whiteUid == currentUserId ||
            session.blackUid == currentUserId ||
            session.redUid == currentUserId

    val isMyTurn = when (gameType) {
        GameType.XIANGQI -> {
            val isRed = session.redUid == currentUserId
            val isBlack = session.blackUid == currentUserId
            (session.turn == 1 && isRed) || (session.turn == 2 && isBlack)
        }
        GameType.GO -> {
            val isBlack = session.blackUid == currentUserId
            val isWhite = session.whiteUid == currentUserId
            (session.turn == 1 && isBlack) || (session.turn == 2 && isWhite)
        }
        else -> {
            val isWhite = session.whiteUid == currentUserId
            val isBlack = session.blackUid == currentUserId
            (session.turn == 1 && isWhite) || (session.turn == 2 && isBlack)
        }
    }

    val bannerText = when (session.status) {
        "waiting" -> "邀请已发出，等待对手加入..."
        "playing" -> when {
            session.inCheck && isMyTurn -> "⚠️ 你被将军！请应将"
            session.inCheck -> "对方被将军！"
            isMyTurn -> "轮到你走子了"
            isPlayer -> "等待对手思考中..."
            else -> "观战中"
        }
        "finished" -> {
            val winnerName = when (session.winnerUid) {
                session.whiteUid -> session.whiteUsername
                session.blackUid -> session.blackUsername
                session.redUid -> session.redUsername
                else -> null
            }
            when {
                session.result == "draw" -> "对局结束 · 和棋"
                winnerName != null -> "对局结束 · 胜者: $winnerName"
                else -> "对局结束"
            }
        }
        "cancelled" -> "对局已被取消"
        else -> ""
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = when {
            session.inCheck && isMyTurn -> MaterialTheme.colorScheme.errorContainer
            isMyTurn -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = bannerText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = when {
                session.inCheck && isMyTurn -> MaterialTheme.colorScheme.onErrorContainer
                isMyTurn -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
