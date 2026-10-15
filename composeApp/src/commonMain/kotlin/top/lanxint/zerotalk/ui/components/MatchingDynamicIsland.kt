package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import top.lanxint.zerotalk.data.network.WsServerEvent
import top.lanxint.zerotalk.data.repository.MatchStatus
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager

/**
 * 全局顶部灵动岛匹配交互组件 (Apple Dynamic Island 风格)
 *
 * 挂载于根布局顶部，完全非阻塞 (无 Scrim 模态遮罩)，允许在匹配中自由切换 Tab 与浏览页面。
 * - 紧凑胶囊态 (Matching)：展示文字/语音匹配标签、动效呼吸/旋转、实时秒表计时、取消按钮
 * - 展开卡片态 (Matched)：弹簧过渡展开，严格遵循匿名保护展示昵称与头像，提供「进入房间」按钮，不自动切屏打断
 * - 超时/异常态 (Timeout/Error)：简要状态提示，延迟后自动收起
 */
@Composable
fun MatchingDynamicIsland(
    matchStatus: MatchStatus,
    isDark: Boolean,
    onEnterRoom: (roomId: String, partner: WsServerEvent.UserJoined?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onAutoDismissMatched: ((roomId: String, partner: WsServerEvent.UserJoined?) -> Unit)? = null
) {
    // 超时与异常状态自动收起
    LaunchedEffect(matchStatus) {
        when (matchStatus) {
            is MatchStatus.Timeout, is MatchStatus.Error -> {
                delay(2500L)
                ZeroTalkClientManager.resetMatchStatus()
            }
            is MatchStatus.Matched -> {
                delay(5000L)
                if (onAutoDismissMatched != null) {
                    onAutoDismissMatched(matchStatus.roomId, matchStatus.partner)
                } else {
                    ZeroTalkClientManager.resetMatchStatus()
                }
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = matchStatus !is MatchStatus.Idle,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessLow
                )
            ) + scaleIn(
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(0.5f, 0f),
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(180)
            ) + scaleOut(
                targetScale = 0.94f,
                transformOrigin = TransformOrigin(0.5f, 0f),
                animationSpec = tween(180)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                contentAlignment = Alignment.TopCenter
            ) {
                when (matchStatus) {
                    is MatchStatus.Matching -> {
                        MatchingCapsule(
                            isVoice = matchStatus.isVoice,
                            isDark = isDark,
                            onCancel = onCancel
                        )
                    }
                    is MatchStatus.Matched -> {
                        MatchedCard(
                            roomId = matchStatus.roomId,
                            partner = matchStatus.partner,
                            isDark = isDark,
                            onEnterRoom = onEnterRoom
                        )
                    }
                    is MatchStatus.Timeout -> {
                        StatusPill(
                            icon = Icons.Default.Info,
                            iconTint = Color(0xFFF59E0B),
                            borderColor = Color(0xFFD97706).copy(alpha = 0.5f),
                            message = "匹配超时，暂未寻得同频零友",
                            isDark = isDark,
                            onClick = { ZeroTalkClientManager.resetMatchStatus() }
                        )
                    }
                    is MatchStatus.Error -> {
                        StatusPill(
                            icon = Icons.Default.Warning,
                            iconTint = Color(0xFFEF4444),
                            borderColor = Color(0xFFDC2626).copy(alpha = 0.5f),
                            message = matchStatus.message.ifBlank { "匹配遇到异常，请稍后重试" },
                            isDark = isDark,
                            onClick = { ZeroTalkClientManager.resetMatchStatus() }
                        )
                    }
                    is MatchStatus.Idle -> {
                        // 闲置不渲染
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchingCapsule(
    isVoice: Boolean,
    isDark: Boolean,
    onCancel: () -> Unit
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        elapsedSeconds = 0
        while (isActive) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "IslandMatchingAnim")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VoiceWaveScale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TextMatchRotate"
    )

    val mins = (elapsedSeconds / 60).toString().padStart(2, '0')
    val secs = (elapsedSeconds % 60).toString().padStart(2, '0')
    val timerText = "$mins:$secs"

    val shape = RoundedCornerShape(100.dp)
    val surfaceColor = if (isDark) Color(0xFF0F1117).copy(alpha = 0.95f) else Color(0xFF14171F).copy(alpha = 0.95f)
    val borderColor = if (isDark) Color(0xFF333B4A) else Color(0xFF2E3440).copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .shadow(
                elevation = if (isDark) 10.dp else 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(surfaceColor)
            .border(0.8.dp, borderColor, shape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isVoice) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "语音匹配",
                tint = Color(0xFF34C759),
                modifier = Modifier
                    .size(17.dp)
                    .scale(waveScale)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "文字匹配",
                tint = Color(0xFF0A84FF),
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotation)
            )
        }

        Spacer(Modifier.width(8.dp))

        BasicText(
            text = if (isVoice) "语音匹配中" else "文字匹配中",
            style = TextStyle(
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.width(8.dp))

        BasicText(
            text = timerText,
            style = TextStyle(
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
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
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "取消匹配",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun MatchedCard(
    roomId: String,
    partner: WsServerEvent.UserJoined?,
    isDark: Boolean,
    onEnterRoom: (roomId: String, partner: WsServerEvent.UserJoined?) -> Unit
) {
    val roomPeers by ZeroTalkClientManager.roomPeers.collectAsStateWithLifecycle()
    val roomBootstraps by ZeroTalkClientManager.roomBootstrapMap.collectAsStateWithLifecycle()
    val effectivePartner = partner ?: roomPeers[roomId] ?: roomBootstraps[roomId]?.peerUser?.let { peer ->
        WsServerEvent.UserJoined(
            uid = peer.uid,
            username = peer.username,
            gender = peer.gender ?: "unknown",
            avatarUrl = peer.avatarUrl,
            mbti = null,
            roomId = roomId
        )
    }
    val isAnonymous = effectivePartner?.username == "匿名用户" ||
        roomBootstraps[roomId]?.peerUser?.privacyMode == true
    val displayName = if (isAnonymous) "匿名用户" else (effectivePartner?.username?.ifBlank { null } ?: "神秘零友")
    val displayAvatar = if (isAnonymous) null else effectivePartner?.avatarUrl

    val shape = RoundedCornerShape(22.dp)
    val surfaceColor = if (isDark) Color(0xFF0F1117).copy(alpha = 0.98f) else Color(0xFF141720).copy(alpha = 0.98f)
    val borderColor = Color(0xFF334155).copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 16.dp else 12.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(surfaceColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onEnterRoom(roomId, partner) }
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            url = displayAvatar,
            name = displayName,
            size = 42.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(14.dp)
                )
                BasicText(
                    text = "匹配成功！已连线 $displayName",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicText(
                text = "点击进入房间聊天",
                style = TextStyle(
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF0A84FF))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onEnterRoom(roomId, partner) }
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = "进入房间",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun StatusPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    borderColor: Color,
    message: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(100.dp)
    val surfaceColor = if (isDark) Color(0xFF1E2028).copy(alpha = 0.96f) else Color(0xFF262C38).copy(alpha = 0.96f)

    Row(
        modifier = Modifier
            .shadow(
                elevation = 6.dp,
                shape = pillShape,
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(pillShape)
            .background(surfaceColor)
            .border(0.8.dp, borderColor, pillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text = message,
            style = TextStyle(
                color = Color(0xFFF3F4F6),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
