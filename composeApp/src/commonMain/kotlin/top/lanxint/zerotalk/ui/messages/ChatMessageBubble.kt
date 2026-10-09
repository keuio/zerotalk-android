package top.lanxint.zerotalk.ui.messages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.ChatMessage
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.ChatDice
import top.lanxint.zerotalk.ui.components.NetworkImageLoader
import top.lanxint.zerotalk.ui.components.shouldAnimateDice
import top.lanxint.zerotalk.ui.moments.MomentMusicCard
import top.lanxint.zerotalk.ui.theme.AppleHigColorTokens
import top.lanxint.zerotalk.ui.utils.MomentAudioPlayer
import top.lanxint.zerotalk.ui.utils.rememberMomentAudioPlayer

/**
 * 消息气泡中的引用条预览组件
 */
@Composable
fun BubbleQuotedBox(
    quotedText: String,
    quotedSenderName: String?,
    quotedIsMine: Boolean?,
    isMine: Boolean,
    isDark: Boolean,
    higColors: AppleHigColorTokens,
    modifier: Modifier = Modifier
) {
    val quoteTextColor = if (isMine) {
        Color.White.copy(alpha = 0.70f)
    } else {
        higColors.secondaryLabel
    }
    val quoteBarColor = if (quotedIsMine == true) {
        Color(0xFF007AFF) // 蓝色竖线 (原我方气泡色)
    } else {
        if (isMine) Color(0xFFD1D1D6) else Color(0xFF8E8E93) // 灰色竖线 (原对方气泡色)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isMine) Color(0x26000000)
                else if (isDark) Color(0x33000000) else Color(0x0F000000)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧竖线指示条 (宽度 2dp，原消息气泡同色)
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(quoteBarColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (!quotedSenderName.isNullOrBlank()) {
                BasicText(
                    text = quotedSenderName,
                    style = TextStyle(
                        color = quoteTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 引用预览文字 (15sp Subhead，行高 20sp，Regular，最多 2 行 + 省略号)
            BasicText(
                text = quotedText,
                style = TextStyle(
                    color = quoteTextColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 语音消息播放条组件
 */
@Composable
fun BubbleVoiceContent(
    audioUrl: String,
    durationSec: Int,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = audioUrl.isNotBlank(), onClick = onTogglePlay)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isPlaying) "停止播放" else "播放语音",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        BasicText(
            text = "$durationSec\"",
            style = TextStyle(
                color = Color.White,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * 图片消息展示相框组件
 *
 * 遵循标准比例约束规范：限制最大宽 220dp、最大高 260dp，最小 80dp，
 * 根据图片真实宽高比自适应尺寸，保持原图比例不拉伸变形。
 */
@Composable
fun BubbleImageContent(
    imageUrl: String,
    isMine: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    height: Dp? = null,
    onClick: (() -> Unit)? = null
) {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        bitmap = NetworkImageLoader.loadImage(imageUrl)
        isLoading = false
    }

    // 标准比例约束计算
    val (targetWidth, targetHeight) = remember(bitmap, height) {
        if (height != null) {
            Pair(height, height)
        } else if (bitmap == null) {
            Pair(160.dp, 160.dp)
        } else {
            val bw = bitmap!!.width.toFloat()
            val bh = bitmap!!.height.toFloat()
            val ratio = if (bh > 0f) bw / bh else 1f

            val maxW = 220f
            val maxH = 260f
            val minDim = 80f

            val w: Float
            val h: Float
            if (ratio >= 1f) {
                // 宽图或正方形
                w = maxW
                h = (maxW / ratio).coerceIn(minDim, maxH)
            } else {
                // 竖长图
                h = maxH
                w = (maxH * ratio).coerceIn(minDim, maxW)
            }
            Pair(w.dp, h.dp)
        }
    }

    Box(
        modifier = modifier
            .size(targetWidth, targetHeight)
            .clip(shape)
            .border(
                0.5.dp,
                if (isMine) Color(0x66007AFF) else Color(0x33FFFFFF),
                shape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "聊天图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF007AFF).copy(alpha = 0.6f)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFD1D5DB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Chat Image",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

/**
 * 统一聊天气泡核心内容容器
 *
 * 供私聊 `PrivateChatScreen` 与公共大厅 `PublicChatroomScreen` 全量复用：
 * 统一处理文本、图片、语音播放器、引用前置等富媒体渲染。
 */
@Composable
fun BubbleContentBox(
    message: ChatMessage,
    hasTail: Boolean,
    isDark: Boolean,
    higColors: AppleHigColorTokens,
    modifier: Modifier = Modifier,
    customShape: Shape? = null,
    customBgColor: Color? = null,
    showBorder: Boolean = true,
    onImageClick: ((String) -> Unit)? = null,
    onEnterGame: ((gameType: String, gameId: Long) -> Unit)? = null
) {
    val isMine = message.isMine
    val voicePlayer = rememberMomentAudioPlayer()
    var isVoicePlaying by remember(message.id) { mutableStateOf(false) }

    val bubbleShape = customShape ?: remember(isMine, hasTail) {
        if (hasTail) {
            MessageBubbleTailShape(
                isOutgoing = isMine,
                cornerRadius = 20.dp,
                tailHeight = 6.5.dp
            )
        } else {
            RoundedCornerShape(20.dp)
        }
    }

    if (message.isImage) {
        BubbleImageContent(
            imageUrl = message.imageUrl,
            isMine = isMine,
            isDark = isDark,
            shape = if (customShape != null) customShape else RoundedCornerShape(20.dp),
            modifier = modifier,
            onClick = if (onImageClick != null) { { onImageClick(message.imageUrl) } } else null
        )
    } else if (message.isMusic) {
        val music = message.musicData ?: remember(message.content) {
            ZeroTalkClientManager.parseMusicContent(message.content)
        }
        if (music != null) {
            MomentMusicCard(
                music = music,
                isDark = isDark,
                modifier = modifier
            )
        }
    } else if (message.isGame) {
        val invite = message.gameInvite ?: remember(message.content) {
            ZeroTalkClientManager.parseGameInviteContent(message.content)
        }
        if (invite != null) {
            ChatGameCardBubble(
                invite = invite,
                isMine = isMine,
                isDark = isDark,
                modifier = modifier,
                onEnterGame = { gType, gId ->
                    onEnterGame?.invoke(gType, gId)
                }
            )
        }
    } else if (message.isDice) {
        // 骰子：官网用裸媒体块渲染（`.msg-media-wrap` 背景透明、无内边距、无边框），不套聊天气泡；
        // 点数由服务端摇出（实测 content 即点数），这里渲染官网同款骰面
        ChatDice(
            value = message.diceValue,
            animate = message.shouldAnimateDice(),
            modifier = modifier
        )
    } else {
        val defaultBg = if (isMine) Color(0xFF007AFF) else Color(0xB32C2C2E)
        val bubbleBg = customBgColor ?: defaultBg

        Box(
            modifier = modifier
                .clip(bubbleShape)
                .background(bubbleBg)
                .then(
                    if (showBorder && !isMine && customBgColor == null) {
                        Modifier.border(0.5.dp, Color(0x22FFFFFF), bubbleShape)
                    } else Modifier
                )
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = if (hasTail) 14.5.dp else 8.dp
                )
        ) {
            Column {
                // 如果存在引用前置
                if (!message.quotedText.isNullOrEmpty()) {
                    BubbleQuotedBox(
                        quotedText = message.quotedText,
                        quotedSenderName = message.quotedSenderName,
                        quotedIsMine = message.quotedIsMine,
                        isMine = isMine,
                        isDark = isDark,
                        higColors = higColors
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 语音消息
                if (message.isVoice) {
                    BubbleVoiceContent(
                        audioUrl = message.audioUrl,
                        durationSec = message.voiceDurationSec,
                        isPlaying = isVoicePlaying,
                        onTogglePlay = {
                            if (isVoicePlaying) {
                                voicePlayer.stop()
                                isVoicePlaying = false
                            } else {
                                voicePlayer.play(
                                    url = message.audioUrl,
                                    onComplete = { isVoicePlaying = false },
                                    onError = { isVoicePlaying = false }
                                )
                                isVoicePlaying = true
                            }
                        }
                    )
                } else {
                    // 普通文本 / 拍一拍
                    val textContent = if (message.isPat) message.patText.ifBlank { "拍了拍" } else message.content
                    BasicText(
                        text = textContent,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 17.sp,
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }
    }
}

