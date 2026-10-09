package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import top.lanxint.zerotalk.data.model.ChatMessage

/**
 * 聊天骰子（对齐官网 `ChatDice` 组件 + `.chat-dice` 样式）
 *
 * 官网视觉规格（`publicAnnouncementDismiss-*.css`）：
 * - 面色 34px、圆角 8px、内边距 5px、点直径 5.5px；
 * - 面底色 `linear-gradient(160deg,#fff,#f4f4f6 48%,#e8e8ec)`，描边 `rgba(148,163,184,.28)`；
 * - 点色 `radial-gradient(circle at 35% 30%,#3f3f46,#18181b 65%,#09090b)`；
 * - 点阵固定 3×3 九宫格（官网 `i` 数组，索引 0..8 行优先）：
 *   1=中心；2=左上+右下；3=左上+中心+右下；4=四角；5=四角+中心；6=左右两列各三点。
 *
 * 官网掷动是 CSS 3D 六面立方体翻转（`Ka = 1050ms`）。这里保留「掷动 → 定格」的节奏
 * （1050ms 内快速换面 + 轻微摆动），**定格结果与官网逐点一致**。
 *
 * @param value 点数 1..6；0 或非法值表示「点数未知」（本地回显尚未收到服务端摇出的点数，
 *              或服务端本身就没存下点数——实测有这种脏数据），此时渲染问号面：
 *              两种都会让气泡显示成空的兜底方案都不采用——官网把非法值当 1 点会伪造点数，
 *              留空面又像是渲染坏了。
 * @param animate 是否播放掷动动画（历史消息、列表复用场景传 false）
 */
@Composable
fun ChatDice(
    value: Int,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    val settleFace = value.takeIf { it in 1..6 }
    var rolling by remember { mutableStateOf(false) }
    var shownFace by remember { mutableStateOf(settleFace ?: 1) }

    LaunchedEffect(settleFace, animate) {
        if (settleFace == null || !animate) {
            shownFace = settleFace ?: 1
            rolling = false
            return@LaunchedEffect
        }
        rolling = true
        val startedAt = System.currentTimeMillis()
        while (System.currentTimeMillis() - startedAt < DICE_ROLL_DURATION_MS) {
            shownFace = Random.nextInt(1, 7)
            delay(DICE_ROLL_TICK_MS)
        }
        shownFace = settleFace
        rolling = false
    }

    // 掷动时轻微摆动，定格后归位（官网为 3D 翻转，这里用轻量近似）
    val rotation by animateFloatAsState(
        targetValue = if (rolling) 12f else 0f,
        animationSpec = tween(durationMillis = if (rolling) 180 else 260),
        label = "chatDiceRotation"
    )

    Box(
        modifier = modifier
            .size(DICE_SIZE)
            .graphicsLayer {
                rotationZ = rotation
                rotationX = -rotation / 2f
            }
            .clip(RoundedCornerShape(DICE_CORNER))
            .background(Brush.linearGradient(colors = listOf(DiceFaceTop, DiceFaceMid, DiceFaceBottom)))
            .border(DICE_BORDER_WIDTH, DiceBorderColor, RoundedCornerShape(DICE_CORNER))
            .padding(DICE_PADDING)
            .semantics {
                contentDescription = if (value in 1..6) "骰子点数 $value" else "骰子"
            },
        contentAlignment = Alignment.Center
    ) {
        if (settleFace == null && !rolling) {
            UnknownValueMark()
        } else {
            DicePips(pips = DICE_FACES.getOrNull(shownFace) ?: DICE_FACES[0])
        }
    }
}

/**
 * 点数未知时的面内容（服务端确实会存出 content 为空的骰子消息，实测 kelo 那边就有两条）。
 *
 * 官网对这种非法值的兜底是**当成 1 点**渲染（`f()` 里 `:1`），那会把「没有点数」
 * 显示成一个真实的 1 点；这里改成问号，既不留空面（像渲染坏了），也不伪造点数。
 */
@Composable
private fun UnknownValueMark() {
    BasicText(
        text = "?",
        style = TextStyle(
            color = DicePipColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    )
}

/**
 * 3×3 点位绘制：九个格子固定占位，只给 `on` 的格子画点，
 * 从而与官网九宫格位置一一对应（不依赖布局间距推算）。
 */
@Composable
private fun DicePips(pips: List<Boolean>) {
    Column {
        for (row in 0 until 3) {
            Row {
                for (column in 0 until 3) {
                    Box(
                        modifier = Modifier.size(DICE_CELL),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pips.getOrNull(row * 3 + column) == true) {
                            Box(
                                modifier = Modifier
                                    .size(DICE_PIP)
                                    .clip(CircleShape)
                                    .background(DicePipBrush)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 面色尺寸（官网 `--dice-size: 34px`） */
private val DICE_SIZE = 34.dp

/** 圆角（官网 8px） */
private val DICE_CORNER = 8.dp

/** 面内边距（官网 5px） */
private val DICE_PADDING = 5.dp

/** 单个点位格边长：34 - 5 × 2 = 24，三等分为 8 */
private val DICE_CELL = 8.dp

/** 点直径（官网 5.5px） */
private val DICE_PIP = 5.5.dp

private val DICE_BORDER_WIDTH = 1.dp

private val DiceFaceTop = Color(0xFFFFFFFF)
private val DiceFaceMid = Color(0xFFF4F4F6)
private val DiceFaceBottom = Color(0xFFE8E8EC)

/** rgba(148,163,184,0.28) */
private val DiceBorderColor = Color(0x471293A1)

/** 点色：官网 radial-gradient(circle at 35% 30%,#3f3f46,#18181b 65%,#09090b) */
private val DicePipBrush = Brush.radialGradient(
    colors = listOf(Color(0xFF3F3F46), Color(0xFF18181B), Color(0xFF09090B)),
    radius = 3.4f
)

/** 点色（问号面复用） */
private val DicePipColor = Color(0xFF18181B)

/** 官网点数点阵（索引 0..8 行优先，true = 画点；下标 n-1 对应点数 n） */
private val DICE_FACES: List<List<Boolean>> = listOf(
    listOf(false, false, false, false, true, false, false, false, false), // 1
    listOf(true, false, false, false, false, false, false, false, true),  // 2
    listOf(true, false, false, false, true, false, false, false, true),   // 3
    listOf(true, false, true, false, false, false, true, false, true),    // 4
    listOf(true, false, true, false, true, false, true, false, true),     // 5
    listOf(true, false, true, true, false, true, true, false, true)       // 6
)

/** 掷动总时长（官网 `Ka = 1050`） */
private const val DICE_ROLL_DURATION_MS = 1050L

/** 掷动换面间隔 */
private const val DICE_ROLL_TICK_MS = 70L

/** 「刚到达」窗口：官网用 4 秒判定是否播放动画，这里同口径 */
private const val DICE_FRESH_WINDOW_MS = 4_000L

/**
 * 骰子是否应播放掷动：只对「刚到达本机」的实时消息播放（官网同为 4 秒窗口），
 * 历史消息与列表滚动复用一律直接定格，避免滚动时满屏掷骰子。
 *
 * 判据用 [ChatMessage.arrivedAtMs]（本机到达时刻）而非服务端 `created_at`，
 * 以免设备时区与服务端不一致时永远判否。
 */
fun ChatMessage.shouldAnimateDice(): Boolean {
    if (arrivedAtMs <= 0L) return false
    val age = System.currentTimeMillis() - arrivedAtMs
    return age in 0..DICE_FRESH_WINDOW_MS
}
