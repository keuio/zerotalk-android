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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 五子棋 15×15 经典木纹棋盘组件
 *
 * 坐标系：x ∈ [0..14], y ∈ [0..14]
 * 编码：225 字符，'1'=黑子，'2'=白子，'0'或'.'为空
 */
@Composable
fun GobangBoard(
    boardString: String,
    lastX: Int?,
    lastY: Int?,
    isMyTurn: Boolean,
    onMove: (x: Int, y: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val boardSize = 15
    val normalizedBoard = remember(boardString) {
        boardString.padEnd(boardSize * boardSize, '.')
    }

    // 木纹棋盘背景色
    val boardBg = Color(0xFFE2B776)
    val gridLineColor = Color(0xFF6B4E23)
    val starPoints = listOf(
        Pair(3, 3), Pair(11, 3),
        Pair(7, 7),
        Pair(3, 11), Pair(11, 11)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(boardBg)
            .border(2.dp, Color(0xFF8B5A2B), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isMyTurn, normalizedBoard) {
                    if (!isMyTurn) return@pointerInput
                    detectTapGestures { offset ->
                        val cellWidth = size.width / (boardSize - 1)
                        val cellHeight = size.height / (boardSize - 1)
                        val x = (offset.x / cellWidth).roundToInt().coerceIn(0, boardSize - 1)
                        val y = (offset.y / cellHeight).roundToInt().coerceIn(0, boardSize - 1)
                        val idx = y * boardSize + x
                        if (idx in normalizedBoard.indices && (normalizedBoard[idx] == '.' || normalizedBoard[idx] == '0')) {
                            onMove(x, y)
                        }
                    }
                }
        ) {
            val cellWidth = size.width / (boardSize - 1)
            val cellHeight = size.height / (boardSize - 1)

            // 1. 绘制网格线
            for (i in 0 until boardSize) {
                // 横线
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, i * cellHeight),
                    end = Offset(size.width, i * cellHeight),
                    strokeWidth = 1.2.dp.toPx()
                )
                // 竖线
                drawLine(
                    color = gridLineColor,
                    start = Offset(i * cellWidth, 0f),
                    end = Offset(i * cellWidth, size.height),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            // 2. 绘制天元与星位点
            starPoints.forEach { (sx, sy) ->
                drawCircle(
                    color = gridLineColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(sx * cellWidth, sy * cellHeight)
                )
            }

            // 3. 绘制棋子
            val pieceRadius = cellWidth * 0.42f
            for (y in 0 until boardSize) {
                for (x in 0 until boardSize) {
                    val idx = y * boardSize + x
                    val char = normalizedBoard.getOrNull(idx) ?: '.'
                    val center = Offset(x * cellWidth, y * cellHeight)

                    if (char == '1') {
                        // 黑子（带立体微光）
                        drawCircle(
                            color = Color(0xFF1A1A1A),
                            radius = pieceRadius,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFF4A4A4A),
                            radius = pieceRadius * 0.85f,
                            center = Offset(center.x - pieceRadius * 0.15f, center.y - pieceRadius * 0.15f)
                        )
                    } else if (char == '2') {
                        // 白子（带微阴影）
                        drawCircle(
                            color = Color(0xFFCCCCCC),
                            radius = pieceRadius,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = pieceRadius * 0.92f,
                            center = Offset(center.x - pieceRadius * 0.1f, center.y - pieceRadius * 0.1f)
                        )
                    }

                    // 4. 最后一步落子红圈高亮
                    if (x == lastX && y == lastY && (char == '1' || char == '2')) {
                        drawCircle(
                            color = Color(0xFFFF3B30),
                            radius = pieceRadius * 0.5f,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
