package top.lanxint.zerotalk.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 中国象棋 9×10 经典棋盘组件
 *
 * 坐标系：9 列 (x ∈ 0..8) × 10 行 (y ∈ 0..9)
 * 字符：90 字符，大写=红方，小写=黑方，'.' 为空
 * 大写：R 車, N 馬, B 相, A 仕, K 帥, C 炮, P 兵
 * 小写：r 車, n 馬, b 象, a 士, k 將, c 砲, p 卒
 */
@Composable
fun XiangqiBoard(
    boardString: String,
    legalMoves: List<List<Int>>,
    inCheck: Boolean,
    lastMove: List<Int>?,
    isMyTurn: Boolean,
    mySide: String, // "red" | "black"
    onMove: (ff: Int, fr: Int, tf: Int, tr: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cols = 9
    val rows = 10
    val normalizedBoard = remember(boardString) {
        boardString.padEnd(cols * rows, '.')
    }

    val flip = mySide == "black"

    var selectedPos by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (boardX, boardY)

    // 获取当前选中格子的所有合法目标格 (boardX, boardY)
    val targetMoves = remember(selectedPos, legalMoves) {
        val sel = selectedPos ?: return@remember emptyList<Pair<Int, Int>>()
        legalMoves.filter { it.size >= 4 && it[0] == sel.first && it[1] == sel.second }
            .map { Pair(it[2], it[3]) }
    }

    val textMeasurer = rememberTextMeasurer()

    val boardBg = Color(0xFFF0D9B5)
    val gridLineColor = Color(0xFF7A583A)
    val redColor = Color(0xFFC8102E)
    val blackColor = Color(0xFF1F1F1F)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 10f)
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(boardBg)
            .border(2.5.dp, Color(0xFF654321), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isMyTurn, normalizedBoard, targetMoves, mySide, flip) {
                    if (!isMyTurn) return@pointerInput
                    detectTapGestures { offset ->
                        val cellW = size.width / (cols - 1)
                        val cellH = size.height / (rows - 1)
                        val screenCol = (offset.x / cellW).roundToInt().coerceIn(0, cols - 1)
                        val screenRow = (offset.y / cellH).roundToInt().coerceIn(0, rows - 1)

                        // 屏幕坐标映射回棋盘逻辑坐标
                        val x = if (flip) 8 - screenCol else screenCol
                        val y = if (flip) screenRow else 9 - screenRow

                        val isTarget = targetMoves.any { it.first == x && it.second == y }
                        val curSel = selectedPos
                        if (curSel != null && isTarget) {
                            onMove(curSel.first, curSel.second, x, y)
                            selectedPos = null
                        } else {
                            val idx = y * cols + x
                            val piece = normalizedBoard.getOrNull(idx) ?: '.'
                            val isMyPiece = when (mySide) {
                                "red" -> piece.isUpperCase()
                                "black" -> piece.isLowerCase()
                                else -> false
                            }
                            if (isMyPiece) {
                                selectedPos = Pair(x, y)
                            } else {
                                selectedPos = null
                            }
                        }
                    }
                }
        ) {
            val cellW = size.width / (cols - 1)
            val cellH = size.height / (rows - 1)

            // 1. 棋盘横线 (10条)
            for (r in 0 until rows) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, r * cellH),
                    end = Offset(size.width, r * cellH),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            // 2. 棋盘竖线 (9条，楚河汉界在屏幕第4排与第5排之间中断)
            for (c in 0 until cols) {
                if (c == 0 || c == cols - 1) {
                    drawLine(
                        color = gridLineColor,
                        start = Offset(c * cellW, 0f),
                        end = Offset(c * cellW, size.height),
                        strokeWidth = 1.2.dp.toPx()
                    )
                } else {
                    // 上半场 (屏幕行 0..4)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(c * cellW, 0f),
                        end = Offset(c * cellW, 4 * cellH),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    // 下半场 (屏幕行 5..9)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(c * cellW, 5 * cellH),
                        end = Offset(c * cellW, 9 * cellH),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }

            // 3. 九宫格斜线 (顶部九宫: 行 0..2, 列 3..5; 底部九宫: 行 7..9, 列 3..5)
            drawLine(gridLineColor, Offset(3 * cellW, 0f), Offset(5 * cellW, 2 * cellH), 1.2.dp.toPx())
            drawLine(gridLineColor, Offset(5 * cellW, 0f), Offset(3 * cellW, 2 * cellH), 1.2.dp.toPx())
            drawLine(gridLineColor, Offset(3 * cellW, 7 * cellH), Offset(5 * cellW, 9 * cellH), 1.2.dp.toPx())
            drawLine(gridLineColor, Offset(5 * cellW, 7 * cellH), Offset(3 * cellW, 9 * cellH), 1.2.dp.toPx())

            // 4. 楚河汉界文字标注 (在屏幕第4至第5排之间的河界区)
            val riverY = 4.5f * cellH
            val riverFontSize = (cellH * 0.38f).toSp()
            val riverTextStyle = TextStyle(
                color = gridLineColor.copy(alpha = 0.55f),
                fontSize = riverFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            val chuheLayout = textMeasurer.measure("楚  河", riverTextStyle)
            drawText(
                textLayoutResult = chuheLayout,
                topLeft = Offset(
                    x = 2 * cellW - chuheLayout.size.width / 2f,
                    y = riverY - chuheLayout.size.height / 2f
                )
            )

            val hanjieLayout = textMeasurer.measure("漢  界", riverTextStyle)
            drawText(
                textLayoutResult = hanjieLayout,
                topLeft = Offset(
                    x = 6 * cellW - hanjieLayout.size.width / 2f,
                    y = riverY - hanjieLayout.size.height / 2f
                )
            )

            // 5. 上一步落子高亮
            if (lastMove != null && lastMove.size >= 4) {
                val fScX = if (flip) 8 - lastMove[0] else lastMove[0]
                val fScY = if (flip) lastMove[1] else 9 - lastMove[1]
                val tScX = if (flip) 8 - lastMove[2] else lastMove[2]
                val tScY = if (flip) lastMove[3] else 9 - lastMove[3]

                val fromCenter = Offset(fScX * cellW, fScY * cellH)
                val toCenter = Offset(tScX * cellW, tScY * cellH)
                drawCircle(Color(0xFF34C759).copy(alpha = 0.25f), cellW * 0.45f, fromCenter)
                drawCircle(Color(0xFF34C759).copy(alpha = 0.35f), cellW * 0.45f, toCenter)
            }

            // 6. 选中棋子指示器
            selectedPos?.let { (bx, by) ->
                val scX = if (flip) 8 - bx else bx
                val scY = if (flip) by else 9 - by
                drawCircle(
                    color = Color(0xFF007AFF),
                    radius = cellW * 0.48f,
                    center = Offset(scX * cellW, scY * cellH),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 7. 合法落子点提示
            targetMoves.forEach { (tx, ty) ->
                val targetIdx = ty * cols + tx
                val targetChar = normalizedBoard.getOrNull(targetIdx) ?: '.'
                val scX = if (flip) 8 - tx else tx
                val scY = if (flip) ty else 9 - ty
                val center = Offset(scX * cellW, scY * cellH)
                if (targetChar == '.') {
                    // 空位画小实心点
                    drawCircle(Color(0xFF34C759), cellW * 0.16f, center)
                } else {
                    // 可吃子画大绿圈
                    drawCircle(Color(0xFF34C759), cellW * 0.46f, center, style = Stroke(width = 2.5.dp.toPx()))
                }
            }

            // 8. 棋子与字体文本绘制
            val pieceRadius = cellW * 0.44f
            val pieceFontSize = (pieceRadius * 1.15f).toSp()

            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    val idx = y * cols + x
                    val char = normalizedBoard.getOrNull(idx) ?: '.'
                    if (char == '.') continue

                    // 棋盘逻辑坐标映射为屏幕坐标
                    val screenX = if (flip) 8 - x else x
                    val screenY = if (flip) y else 9 - y
                    val center = Offset(screenX * cellW, screenY * cellH)

                    val isRed = char.isUpperCase()
                    val pColor = if (isRed) redColor else blackColor
                    val pieceName = getXiangqiPieceName(char)

                    // 棋子外圆盘底色
                    drawCircle(Color(0xFFFBF4E6), pieceRadius, center)
                    // 棋子外圆环边框
                    drawCircle(pColor, pieceRadius, center, style = Stroke(width = 1.6.dp.toPx()))
                    // 棋子内同心细环
                    drawCircle(pColor.copy(alpha = 0.45f), pieceRadius * 0.85f, center, style = Stroke(width = 0.8.dp.toPx()))

                    // 将军高亮红框
                    if (inCheck && (char == 'K' || char == 'k')) {
                        drawCircle(Color(0xFFFF3B30), pieceRadius * 1.16f, center, style = Stroke(width = 2.5.dp.toPx()))
                    }

                    // 绘制棋子上的汉字 (核心修复)
                    if (pieceName.isNotBlank()) {
                        val pieceTextStyle = TextStyle(
                            color = pColor,
                            fontSize = pieceFontSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        val textLayout = textMeasurer.measure(pieceName, pieceTextStyle)
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                x = center.x - textLayout.size.width / 2f,
                                y = center.y - textLayout.size.height / 2f
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 棋子字符转换为中文棋子名称
 */
private fun getXiangqiPieceName(char: Char): String = when (char) {
    'R' -> "車"
    'N' -> "馬"
    'B' -> "相"
    'A' -> "仕"
    'K' -> "帥"
    'C' -> "炮"
    'P' -> "兵"
    'r' -> "車"
    'n' -> "馬"
    'b' -> "象"
    'a' -> "士"
    'k' -> "將"
    'c' -> "砲"
    'p' -> "卒"
    else -> ""
}
