package top.lanxint.zerotalk.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 围棋 19×19 传统棋盘组件
 *
 * 坐标系：x ∈ [0..18], y ∈ [0..18]
 * 编码：361 字符，'1'=黑子，'2'=白子，'0'或'.'为空
 */
@Composable
fun GoBoard(
    boardString: String,
    lastX: Int?,
    lastY: Int?,
    captures: Map<String, Int>?,
    isMyTurn: Boolean,
    onMove: (x: Int, y: Int) -> Unit,
    onPass: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boardSize = 19
    val normalizedBoard = remember(boardString) {
        boardString.padEnd(boardSize * boardSize, '0')
    }

    val boardBg = Color(0xFFDCB35C)
    val gridLineColor = Color(0xFF5A3D18)
    val starPoints = listOf(
        Pair(3, 3), Pair(9, 3), Pair(15, 3),
        Pair(3, 9), Pair(9, 9), Pair(15, 9),
        Pair(3, 15), Pair(9, 15), Pair(15, 15)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 提子数统计
        if (captures != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicText(
                    text = "黑方提子: ${captures["black"] ?: 0}",
                    style = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
                BasicText(
                    text = "白方提子: ${captures["white"] ?: 0}",
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(boardBg)
                .border(2.dp, Color(0xFF7A4E1B), RoundedCornerShape(12.dp))
                .padding(6.dp)
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
                            if (idx in normalizedBoard.indices && (normalizedBoard[idx] == '0' || normalizedBoard[idx] == '.')) {
                                onMove(x, y)
                            }
                        }
                    }
            ) {
                val cellWidth = size.width / (boardSize - 1)
                val cellHeight = size.height / (boardSize - 1)

                // 1. 网格线
                for (i in 0 until boardSize) {
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, i * cellHeight),
                        end = Offset(size.width, i * cellHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = gridLineColor,
                        start = Offset(i * cellWidth, 0f),
                        end = Offset(i * cellWidth, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. 星位
                starPoints.forEach { (sx, sy) ->
                    drawCircle(
                        color = gridLineColor,
                        radius = 2.8.dp.toPx(),
                        center = Offset(sx * cellWidth, sy * cellHeight)
                    )
                }

                // 3. 棋子
                val pieceRadius = cellWidth * 0.46f
                for (y in 0 until boardSize) {
                    for (x in 0 until boardSize) {
                        val idx = y * boardSize + x
                        val char = normalizedBoard.getOrNull(idx) ?: '0'
                        val center = Offset(x * cellWidth, y * cellHeight)

                        if (char == '1') {
                            drawCircle(color = Color(0xFF1E1E1E), radius = pieceRadius, center = center)
                            drawCircle(
                                color = Color(0xFF444444),
                                radius = pieceRadius * 0.8f,
                                center = Offset(center.x - pieceRadius * 0.15f, center.y - pieceRadius * 0.15f)
                            )
                        } else if (char == '2') {
                            drawCircle(color = Color(0xFFD4D4D4), radius = pieceRadius, center = center)
                            drawCircle(
                                color = Color.White,
                                radius = pieceRadius * 0.9f,
                                center = Offset(center.x - pieceRadius * 0.1f, center.y - pieceRadius * 0.1f)
                            )
                        }

                        // 最后一步落子红圈高亮
                        if (x == lastX && y == lastY && (char == '1' || char == '2')) {
                            drawCircle(
                                color = Color(0xFFFF3B30),
                                radius = pieceRadius * 0.5f,
                                center = center,
                                style = Stroke(width = 1.8.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 停一手 (Pass) 控制
        if (isMyTurn) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF5856D6))
                    .clickable { onPass() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "停一手 (Pass)",
                    style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
