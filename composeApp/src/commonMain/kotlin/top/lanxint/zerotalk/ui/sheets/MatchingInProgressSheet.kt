package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.ui.theme.AppleHigColors

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.rotate
import top.lanxint.zerotalk.data.network.WsServerEvent
import top.lanxint.zerotalk.data.repository.MatchStatus
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import kotlinx.coroutines.delay

@Composable
fun SheetMatchingInProgressContent(
    isVoice: Boolean,
    isDark: Boolean,
    onCancel: () -> Unit,
    onMatchSuccess: ((roomId: String, partner: WsServerEvent.UserJoined?) -> Unit)? = null
) {
    val higColors = AppleHigColors.colors(isDark)
    val titleColor = higColors.label
    val subColor = higColors.secondaryLabel

    val matchStatus by ZeroTalkClientManager.matchStatus.collectAsState()

    // 旋转动效
    val infiniteTransition = rememberInfiniteTransition(label = "MatchRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // 匹配成功自动跳转（通过 matchedRoomId 保持平滑倒计时，并在倒计时结束时取得最新 partner）
    // 语音匹配时不跳转：通话覆盖层会自动浮出接管，跳进文字会话反而与对端错位
    val matchedRoomId = (matchStatus as? MatchStatus.Matched)?.roomId
    LaunchedEffect(matchedRoomId) {
        if (matchedRoomId != null && !isVoice) {
            delay(1200L) // 停留 1.2s 展示成功反馈并等待 user_joined 到达
            val currentStatus = ZeroTalkClientManager.matchStatus.value as? MatchStatus.Matched
            val partner = currentStatus?.partner ?: ZeroTalkClientManager.roomPeers.value[matchedRoomId]
            onMatchSuccess?.invoke(matchedRoomId, partner)
        }
    }

    val (currentTitle, currentSubtitle, isSuccess) = when (val s = matchStatus) {
        is MatchStatus.Matched -> {
            val name = s.partner?.username?.ifBlank { null } ?: "对方"
            if (isVoice) {
                Triple("语音匹配成功！", "已连线 $name，正在接通语音...", true)
            } else {
                Triple("匹配成功！", "已连线 $name，正在进入私密聊天...", true)
            }
        }
        is MatchStatus.Timeout -> {
            Triple("匹配超时", "暂未寻得同频零友，请稍后重试", false)
        }
        is MatchStatus.Error -> {
            Triple("匹配异常", s.message, false)
        }
        else -> {
            Triple(
                if (isVoice) "正在寻找同频语音连线..." else "正在为您寻找文字聊天好友...",
                "已进入全网匹配队列，预计约需 3-5 秒",
                false
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isSuccess) Color(0xFF10B981).copy(alpha = 0.15f)
                    else if (isDark) Color(0xFF1E3A5F)
                    else Color(0xFFE0EFFF)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSuccess) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    imageVector = if (isVoice) Icons.Default.GraphicEq else Icons.Default.Refresh,
                    contentDescription = null,
                    tint = higColors.tint,
                    modifier = Modifier
                        .size(32.dp)
                        .then(if (!isVoice) Modifier.rotate(rotation) else Modifier)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        BasicText(
            text = currentTitle,
            style = AppleHigTypography.headline.copy(
                color = titleColor,
                textAlign = TextAlign.Center
            )
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            text = currentSubtitle,
            style = AppleHigTypography.subhead.copy(color = subColor, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(28.dp))

        // 取消匹配按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(if (isDark) Color(0xFF262C38) else Color(0xFFE5E7EB))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        ZeroTalkClientManager.cancelMatching()
                        onCancel()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = if (isSuccess) "进入房间" else "取消匹配",
                style = AppleHigTypography.subhead.copy(
                    color = if (isSuccess) higColors.tint else higColors.destructive,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
