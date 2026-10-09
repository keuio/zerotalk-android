package top.lanxint.zerotalk.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * 国际象棋 8×8 棋盘组件
 *
 * 坐标系：
 * row 0 = 白方底线 (Rank 1), row 7 = 黑方底线 (Rank 8)
 * file 0 = a 列, file 7 = h 列
 * index = row * 8 + file (总长 64)
 *
 * 棋子字符：
 * 大写=白方 (K=王, Q=后, R=车, B=象, N=马, P=兵)
 * 小写=黑方 (k=王, q=后, r=车, b=象, n=马, p=兵)
 * '.' 为空格
 */
@Composable
fun ChessBoard(
    boardString: String,
    legalMoves: List<List<Int>>,
    inCheck: Boolean,
    lastMove: List<Int>?,
    isMyTurn: Boolean,
    mySide: String, // "white" | "black"
    turnSide: String?, // "white" | "black"
    onMove: (ff: Int, fr: Int, tf: Int, tr: Int, promo: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedBoard = remember(boardString) {
        boardString.padEnd(64, '.')
    }

    val flip = mySide == "black"

    var selectedPos by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (file, row)
    var pendingPromotionMove by remember { mutableStateOf<Pair<Pair<Int, Int>, Pair<Int, Int>>?>(null) } // ((ff, fr), (tf, tr))

    // 选中格子的所有候选走法
    val movesFromSelected = remember(selectedPos, legalMoves) {
        val sel = selectedPos ?: return@remember emptyList<List<Int>>()
        legalMoves.filter { it.size >= 4 && it[0] == sel.first && it[1] == sel.second }
    }

    // 提取目标格坐标列表
    val targetSquares = remember(movesFromSelected) {
        movesFromSelected.map { Pair(it[2], it[3]) }.distinct()
    }

    // 找到被将军的王所在格
    val kingCheckPos = remember(inCheck, normalizedBoard, turnSide) {
        if (!inCheck) null
        else {
            val targetChar = if (turnSide == "black") 'k' else 'K'
            val idx = normalizedBoard.indexOf(targetChar)
            if (idx >= 0) Pair(idx % 8, idx / 8) else null
        }
    }

    val lightColor = Color(0xFFF0D9B5)
    val darkColor = Color(0xFFB58863)
    val selectedBg = Color(0x66BACA2B)
    val lastMoveBg = Color(0x55F6E05E)
    val checkBg = Color(0x77E53E3E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, Color(0xFF5D4037), RoundedCornerShape(12.dp))
            .background(Color(0xFF5D4037))
            .padding(4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (screenR in 0 until 8) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (screenC in 0 until 8) {
                        // 视角映射到 board 坐标
                        val file = if (flip) 7 - screenC else screenC
                        val row = if (flip) screenR else 7 - screenR
                        val idx = row * 8 + file
                        val pieceChar = normalizedBoard.getOrNull(idx) ?: '.'

                        val isDarkSquare = (file + row) % 2 == 0
                        val baseColor = if (isDarkSquare) darkColor else lightColor

                        val isSelected = selectedPos?.first == file && selectedPos?.second == row
                        val isTarget = targetSquares.any { it.first == file && it.second == row }
                        val isLastMove = lastMove != null && lastMove.size >= 4 && (
                                (lastMove[0] == file && lastMove[1] == row) ||
                                        (lastMove[2] == file && lastMove[3] == row)
                                )
                        val isKingInCheck = kingCheckPos != null && kingCheckPos.first == file && kingCheckPos.second == row

                        val cellBg = when {
                            isKingInCheck -> checkBg
                            isSelected -> selectedBg
                            isLastMove -> lastMoveBg
                            else -> baseColor
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(cellBg)
                                .clickable(enabled = isMyTurn) {
                                    val curSel = selectedPos
                                    if (curSel != null && isTarget) {
                                        // 检查该目标格是否有升变走法
                                        val promoMoves = movesFromSelected.filter {
                                            it[2] == file && it[3] == row && it.size >= 5 && it[4] > 0
                                        }
                                        if (promoMoves.isNotEmpty()) {
                                            pendingPromotionMove = Pair(curSel, Pair(file, row))
                                        } else {
                                            onMove(curSel.first, curSel.second, file, row, 0)
                                            selectedPos = null
                                        }
                                    } else {
                                        // 检查是否点中了己方棋子
                                        val isMyPiece = when (mySide) {
                                            "white" -> pieceChar.isUpperCase()
                                            "black" -> pieceChar.isLowerCase()
                                            else -> false
                                        }
                                        if (isMyPiece) {
                                            selectedPos = Pair(file, row)
                                        } else {
                                            selectedPos = null
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // 渲染棋盘坐标标记 (角落微标)
                            if (screenC == 0) {
                                Text(
                                    text = (row + 1).toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkSquare) lightColor else darkColor,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 2.dp, top = 1.dp)
                                )
                            }
                            if (screenR == 7) {
                                Text(
                                    text = ('a' + file).toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkSquare) lightColor else darkColor,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 2.dp, bottom = 1.dp)
                                )
                            }

                            // 棋子渲染
                            if (pieceChar != '.') {
                                val symbol = getChessSymbol(pieceChar)
                                val isWhitePiece = pieceChar.isUpperCase()
                                Text(
                                    text = symbol,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWhitePiece) Color.White else Color(0xFF1A1A1A),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 目标格指示器（可走小圆点，可吃圆环）
                            if (isTarget) {
                                if (pieceChar == '.') {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x8848BB78))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.85f)
                                            .border(3.dp, Color(0xAA48BB78), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 升变弹窗选择 (Queen=81, Rook=82, Bishop=66, Knight=78)
    pendingPromotionMove?.let { (from, to) ->
        PromotionPickerModal(
            isWhite = mySide == "white",
            onSelect = { promoCode ->
                onMove(from.first, from.second, to.first, to.second, promoCode)
                pendingPromotionMove = null
                selectedPos = null
            },
            onDismiss = {
                pendingPromotionMove = null
            }
        )
    }
}

/**
 * 棋子字符转换为 Unicode 国际象棋符号
 */
private fun getChessSymbol(c: Char): String = when (c) {
    'K' -> "♔"
    'Q' -> "♕"
    'R' -> "♖"
    'B' -> "♗"
    'N' -> "♘"
    'P' -> "♙"
    'k' -> "♚"
    'q' -> "♛"
    'r' -> "♜"
    'b' -> "♝"
    'n' -> "♞"
    'p' -> "♟"
    else -> ""
}

/**
 * 升变选择模态弹窗
 */
@Composable
private fun PromotionPickerModal(
    isWhite: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    // 升变选项：后 Q(81), 车 R(82), 象 B(66), 马 N(78)
    val options = listOf(
        Triple(if (isWhite) "♕" else "♛", "皇后 (Q)", 81),
        Triple(if (isWhite) "♖" else "♜", "城堡 (R)", 82),
        Triple(if (isWhite) "♗" else "♝", "主教 (B)", 66),
        Triple(if (isWhite) "♘" else "♞", "骑士 (N)", 78)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "兵升变选择",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    options.forEach { (symbol, label, code) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelect(code) }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = symbol,
                                fontSize = 36.sp,
                                color = if (isWhite) Color.Black else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
