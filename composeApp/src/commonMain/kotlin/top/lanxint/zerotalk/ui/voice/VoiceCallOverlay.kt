package top.lanxint.zerotalk.ui.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.delay
import top.lanxint.zerotalk.data.model.ConversationItem
import top.lanxint.zerotalk.data.voice.VoiceCallSession
import top.lanxint.zerotalk.data.voice.VoiceCallUiState
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 语音通话全屏覆盖层（对齐官网 useVoiceCall 的通话界面）
 *
 * - 来电：接听 / 拒接
 * - 去电：呼叫中 / 取消
 * - 建连中：正在接通…
 * - 通话中：通话计时 + 静音 / 扬声器 / 挂断
 * - 重连中：网络异常，正在重连… + 挂断
 */
@Composable
fun VoiceCallOverlay(
    uiState: VoiceCallUiState,
    session: VoiceCallSession?,
    muted: Boolean,
    speakerOn: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    if (session == null || uiState == VoiceCallUiState.IDLE) return

    // 通话计时（每秒刷新）
    var elapsedSec by remember(session.callId, session.connectedAtMs) {
        mutableStateOf(
            if (session.connectedAtMs > 0L) {
                ((System.currentTimeMillis() - session.connectedAtMs) / 1000L).toInt().coerceAtLeast(0)
            } else {
                0
            }
        )
    }
    LaunchedEffect(session.callId, session.connectedAtMs) {
        if (session.connectedAtMs <= 0L) return@LaunchedEffect
        while (true) {
            delay(1_000L)
            elapsedSec = ((System.currentTimeMillis() - session.connectedAtMs) / 1000L).toInt().coerceAtLeast(0)
        }
    }

    val statusText = when (uiState) {
        VoiceCallUiState.INCOMING -> "邀请你语音通话"
        VoiceCallUiState.OUTGOING -> "正在呼叫…"
        VoiceCallUiState.CONNECTING -> if (session.auto) "正在接通语音…" else "正在接通…"
        VoiceCallUiState.CONNECTED -> formatDuration(elapsedSec)
        VoiceCallUiState.RECONNECTING -> "网络异常，正在重连…"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF21C1C1E), Color(0xF2000000), Color(0xF21C1C1E))
                )
            )
            .clickable(enabled = false) { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            UserAvatar(
                url = session.peerAvatarUrl,
                name = session.peerLabel.ifBlank { "对方" },
                size = 108.dp,
                modifier = Modifier.border(2.dp, Color(0x33FFFFFF), CircleShape)
            )

            Spacer(Modifier.height(20.dp))

            BasicText(
                text = session.peerLabel.ifBlank { "对方" },
                style = TextStyle(
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    platformStyle = AppleHigTypography.defaultPlatformStyle
                )
            )

            Spacer(Modifier.height(10.dp))

            BasicText(
                text = statusText,
                style = TextStyle(
                    color = if (uiState == VoiceCallUiState.RECONNECTING) Color(0xFFFF9F0A) else Color(0xB3FFFFFF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.weight(1f))

            when (uiState) {
                VoiceCallUiState.INCOMING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallActionButton(
                            icon = Icons.Default.CallEnd,
                            label = "拒接",
                            containerColor = Color(0xFFFF453A),
                            onClick = onReject
                        )
                        CallActionButton(
                            icon = Icons.Default.Call,
                            label = "接听",
                            containerColor = Color(0xFF30D158),
                            onClick = onAccept
                        )
                    }
                }

                VoiceCallUiState.OUTGOING -> {
                    CallActionButton(
                        icon = Icons.Default.CallEnd,
                        label = "取消",
                        containerColor = Color(0xFFFF453A),
                        onClick = onCancel
                    )
                }

                VoiceCallUiState.CONNECTED -> {
                    CallControlsRow(
                        muted = muted,
                        speakerOn = speakerOn,
                        onToggleMute = onToggleMute,
                        onToggleSpeaker = onToggleSpeaker,
                        onHangup = onHangup
                    )
                }

                VoiceCallUiState.CONNECTING -> {
                    // 对方尚未接听前仍是「取消呼叫」（服务端 ringing 阶段只认 voice_cancel）
                    if (session.peerAccepted) {
                        CallControlsRow(
                            muted = muted,
                            speakerOn = speakerOn,
                            onToggleMute = onToggleMute,
                            onToggleSpeaker = onToggleSpeaker,
                            onHangup = onHangup
                        )
                    } else {
                        CallActionButton(
                            icon = Icons.Default.CallEnd,
                            label = "取消",
                            containerColor = Color(0xFFFF453A),
                            onClick = onCancel
                        )
                    }
                }

                VoiceCallUiState.RECONNECTING -> {
                    CallActionButton(
                        icon = Icons.Default.CallEnd,
                        label = "挂断",
                        containerColor = Color(0xFFFF453A),
                        onClick = onHangup
                    )
                }

                else -> Unit
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/** 通话控制行：静音 / 扬声器 / 挂断 */
@Composable
private fun CallControlsRow(
    muted: Boolean,
    speakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onHangup: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CallActionButton(
            icon = if (muted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (muted) "已静音" else "静音",
            containerColor = if (muted) Color(0xFF8E8E93) else Color(0x33FFFFFF),
            onClick = onToggleMute
        )
        CallActionButton(
            icon = if (speakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
            label = if (speakerOn) "扬声器" else "听筒",
            containerColor = if (speakerOn) Color(0xFFFFFFFF) else Color(0x33FFFFFF),
            iconTint = if (speakerOn) Color(0xFF1C1C1E) else Color.White,
            onClick = onToggleSpeaker
        )
        CallActionButton(
            icon = Icons.Default.CallEnd,
            label = "挂断",
            containerColor = Color(0xFFFF453A),
            onClick = onHangup
        )
    }
}

/** 通话操作圆钮（图标 + 文案） */
@Composable
private fun CallActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    onClick: () -> Unit,
    iconTint: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = label,
            style = TextStyle(
                color = Color(0xCCFFFFFF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/** 通话时长 mm:ss（超过 1 小时显示 h:mm:ss） */
private fun formatDuration(totalSec: Int): String {
    val safe = totalSec.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}
