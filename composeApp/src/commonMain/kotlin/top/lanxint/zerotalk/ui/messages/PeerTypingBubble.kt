package top.lanxint.zerotalk.ui.messages

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 「对方正在输入」三点气泡（私聊 / 匹配等 1v1 会话）
 *
 * 对齐官网 `PeerTypingBanner` 的语义（`peerIsTyping` = 对端正在输入），
 * 表现形式为**对方侧气泡 + 中间三个动态点**：
 * 1. 气泡与对方普通文字气泡完全同形同高同色：
 *    - 尖尾形状：MessageBubbleTailShape(isOutgoing = false, cornerRadius = 20.dp, tailHeight = 6.5.dp)；
 *    - 烟熏深灰底色：0xB32C2C2E + 0.5dp 高光描边；
 *    - 高度对齐：正常单行文本气泡高度（top 8dp + 单行 lineHeight 22dp + 底部尖尾 14.5dp = 44.5dp）；
 * 2. 三个点依次浮现 / 消失（波浪式），循环播放；
 * 3. 气泡本身的出现 / 消失由调用方用 `AnimatedVisibility` 做淡入淡出。
 */
@Composable
fun PeerTypingBubble(
    modifier: Modifier = Modifier
) {
    // 对方气泡形态：左下尖尾（与 BubbleContentBox 的私聊对方带尾气泡完全一致）
    val shape = remember { MessageBubbleTailShape(isOutgoing = false, cornerRadius = 20.dp, tailHeight = 6.5.dp) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xB32C2C2E))
            .border(0.5.dp, Color(0x22FFFFFF), shape)
            .defaultMinSize(minWidth = 64.dp, minHeight = 44.5.dp)
            // 尾巴占 6.5dp，底部 14.5dp = 8dp 视觉内边距 + 6.5dp 尖尾高，与单行气泡 (top 8dp, bottom 14.5dp) 严格一致
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 14.5.dp),
        contentAlignment = Alignment.Center
    ) {
        TypingDots()
    }
}

/** 三个点：依次浮现消失（波浪式），单点周期 420ms、相邻点错开 180ms，高度对齐单行文本 22dp */
@Composable
private fun TypingDots(color: Color = Color(0xE6FFFFFF)) {
    val transition = rememberInfiniteTransition(label = "PeerTypingDots")
    Row(
        modifier = Modifier.height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(5.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offsetMillis = index * 180)
                ),
                label = "TypingDot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer {
                        // 浮现消失：透明度 0.3 → 1，同时轻微上浮，形成波浪观感
                        alpha = 0.3f + 0.7f * progress
                        translationY = -2.dp.toPx() * progress
                    }
                    .background(color, CircleShape)
            )
        }
    }
}
