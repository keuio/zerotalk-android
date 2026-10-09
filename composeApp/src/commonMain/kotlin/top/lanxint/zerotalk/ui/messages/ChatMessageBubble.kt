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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.ChatMessage
import top.lanxint.zerotalk.data.model.MomentShareCardData
import top.lanxint.zerotalk.data.model.MusicPlaylist
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.ChatDice
import top.lanxint.zerotalk.ui.components.NetworkImageLoader
import top.lanxint.zerotalk.ui.components.shouldAnimateDice
import top.lanxint.zerotalk.ui.moments.MomentMusicCard
import top.lanxint.zerotalk.ui.moments.MomentVoiceBubble
import top.lanxint.zerotalk.ui.theme.AppleHigColorTokens
import top.lanxint.zerotalk.ui.theme.AppleHigColors
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
    higColors: AppleHigColorTokens,
    modifier: Modifier = Modifier
) {
    // 引用条位于气泡内部，颜色必须跟随**气泡底色**、而不是主题：
    // 对方气泡恒为深灰 Color(0xB32C2C2E)、我方气泡恒为蓝 Color(0xFF007AFF)（两种主题下都一样），
    // 气泡主文字也恒为白色。因此引用文字必须恒为浅色 —— 此前用 higColors.secondaryLabel，
    // 亮色模式下那是深灰字，压在深灰气泡上几乎看不见。
    val quoteTextColor = Color.White.copy(alpha = if (isMine) 0.80f else 0.72f)
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
                // 气泡底色两种主题下一致，遮罩也保持一致
                // （原先亮色用 6%，引用块几乎看不出边界）
                if (isMine) Color(0x26000000) else Color(0x2E000000)
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
    } else if (message.isMusicPlaylist) {
        // 歌单卡片：官网与 music 共用 `.msg-music-wrap`（裸媒体块，不套聊天气泡）；
        // 解析失败（musicPlaylist 为 null）显示「歌单已失效」（对齐官方）
        MusicPlaylistCard(
            playlist = message.musicPlaylist,
            isDark = isDark,
            modifier = modifier
        )
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
    } else if (message.isMomentShare) {
        // 动态分享卡片：官网同样是裸媒体块（`.msg-media-wrap msg-moment-share-wrap`），
        // 自带主题化底色、不套聊天气泡；解析失败（momentShare 为 null）显示「动态卡片已失效」
        MomentShareCardBubble(
            data = message.momentShare,
            isDark = isDark,
            modifier = modifier
        )
    } else if (message.isSticker) {
        // 表情包：官网裸媒体块（`.msg-media-wrap--sticker`），有图渲染图片，
        // 拿不到地址（image_url 缺失且本地列表未命中）降级显示「表情包已失效」（对齐官方）
        StickerBubble(
            stickerUrl = message.stickerUrl,
            assetId = message.stickerAssetId,
            isDark = isDark,
            modifier = modifier,
            onClick = onImageClick
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

/**
 * 网易云歌单卡片（服务端 `type:"music_playlist"`）
 *
 * 对齐官方 `NeteasePlaylistCard`（`.nm-pl-card`，聊天内嵌于 `.msg-music-wrap`）：
 * 封面 + 歌单名 + 「创建者 · N 首」+「歌单」徽标，与 [MomentMusicCard] 同一套视觉语言。
 * [playlist] 为 null（JSON 解析失败 / playlist_id 非法）时显示「歌单已失效」（对齐官方）。
 */
@Composable
fun MusicPlaylistCard(
    playlist: MusicPlaylist?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val cardShape = RoundedCornerShape(14.dp)
    val cardBg = if (isDark) Color(0xFF1F2430) else Color(0xFFF2F4F8)
    val subtitleColor = higColors.secondaryLabel

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBg)
            .border(0.5.dp, higColors.separator.copy(alpha = 0.5f), cardShape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncNetworkImage(
            url = playlist?.coverUrl.orEmpty(),
            contentDescription = "歌单封面",
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = when {
                    playlist == null -> "歌单已失效"
                    playlist.name.isBlank() -> "未知歌单"
                    else -> playlist.name
                },
                style = TextStyle(
                    color = if (playlist == null) subtitleColor else higColors.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = musicPlaylistSubtitle(playlist),
                    style = TextStyle(color = subtitleColor, fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (playlist != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE11D48))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                BasicText(
                    text = "歌单",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

/** 歌单卡片副标题：官方 `creator ? creator + " · " : ""` + `N 首` */
private fun musicPlaylistSubtitle(playlist: MusicPlaylist?): String {
    if (playlist == null) return "网易云音乐"
    val count = "${playlist.trackCount} 首"
    return if (playlist.creator.isNotBlank()) "${playlist.creator} · $count" else "网易云歌单 · $count"
}

/**
 * 表情包气泡（服务端 `type:"sticker"`）
 *
 * 对齐官方 `ko()`：优先 `image_url`，否则按 content 的 asset_id 查本地表情包列表
 * （`GET /api/sticker/list`，结果缓存在 [ZeroTalkClientManager]）；都拿不到时显示「表情包已失效」（对齐官方）。
 */
@Composable
fun StickerBubble(
    stickerUrl: String,
    assetId: Long,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: ((String) -> Unit)? = null
) {
    val resolvedUrl by produceState(initialValue = stickerUrl, stickerUrl, assetId) {
        value = stickerUrl
        if (value.isBlank() && assetId > 0L) {
            value = ZeroTalkClientManager.resolveStickerUrl(assetId)
            if (value.isBlank()) {
                ZeroTalkClientManager.ensureStickerListLoaded()
                value = ZeroTalkClientManager.resolveStickerUrl(assetId)
            }
        }
    }

    if (resolvedUrl.isBlank()) {
        BasicText(
            text = "表情包已失效",
            style = TextStyle(
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                fontSize = 14.sp
            ),
            modifier = modifier
        )
        return
    }

    AsyncNetworkImage(
        url = resolvedUrl,
        contentDescription = "表情包",
        modifier = modifier
            .size(120.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClick(resolvedUrl) }
                } else {
                    Modifier
                }
            ),
        contentScale = ContentScale.Fit,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.Transparent
    )
}

/**
 * 动态分享卡片气泡（服务端 `type:"moment_share"`）
 *
 * 对齐官网 `MomentShareChatCard`（`.msg-moment-share`）：
 * 作者头像 + 昵称 + 性别 + 正文摘要（最多 5 行）+ 语音 / 音乐 + 图片九宫格（最多 9 张）
 * + 底部点赞 / 评论数。
 *
 * 与官网一致，卡片**不套聊天气泡**（官网是裸媒体块 `.msg-media-wrap msg-moment-share-wrap`），
 * 而是自带主题化底色：亮色白卡深字、暗色深卡浅字。因此卡片内部文字使用
 * [AppleHigColors] 是安全的 —— 它们落在卡片自己的底色上，而不是固定在
 * 蓝 / 深灰的聊天气泡上（后者才必须恒用白色文字，参见 [BubbleQuotedBox] 的修复）。
 *
 * @param data 已解析的卡片数据；为 null（JSON 解析失败 / moment_id 非法）时
 *   按官网口径显示「动态卡片已失效」
 */
@Composable
fun MomentShareCardBubble(
    data: MomentShareCardData?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val cardShape = RoundedCornerShape(16.dp)
    // 对齐官网 .msg-moment-share 的亮 / 暗两套底色
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0x6194A3B8) else Color(0x4794A3B8)
    val dividerColor = if (isDark) Color(0x8C475569) else Color(0x2494A3B8)
    val excerptColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val statColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBg)
            .border(0.5.dp, borderColor, cardShape)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        if (data == null) {
            BasicText(
                text = "动态卡片已失效",
                style = TextStyle(color = statColor, fontSize = 13.sp)
            )
            return@Column
        }

        // ---- 头部：头像 + 昵称 + 性别（+ 时间） ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            MomentShareAvatar(
                avatarUrl = data.avatarUrl,
                fallback = data.avatarFallback,
                size = 34.dp
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = data.username,
                        style = TextStyle(
                            color = higColors.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    MomentShareGenderBadge(gender = data.gender)
                }
                data.createdAt?.takeIf { it.isNotBlank() }?.let { time ->
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        text = time,
                        style = TextStyle(color = statColor, fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ---- 正文摘要（官网最多 5 行） ----
        if (data.excerpt.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = data.excerpt,
                style = TextStyle(color = excerptColor, fontSize = 13.sp, lineHeight = 19.sp),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ---- 语音 / 音乐 ----
        if (data.audioUrl != null) {
            Spacer(Modifier.height(8.dp))
            MomentVoiceBubble(audioUrl = data.audioUrl, isDark = isDark)
        }
        data.music?.let { music ->
            Spacer(Modifier.height(8.dp))
            MomentMusicCard(music = music, isDark = isDark)
        }

        // ---- 图片九宫格（最多 9 张） ----
        if (data.images.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            MomentShareImageGrid(images = data.images)
        }

        // ---- 底部：点赞 / 评论 ----
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(dividerColor)
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MomentShareStat(
                icon = Icons.Default.Favorite,
                count = data.likeCount,
                color = statColor
            )
            Spacer(Modifier.width(12.dp))
            MomentShareStat(
                icon = Icons.AutoMirrored.Filled.Chat,
                count = data.commentCount,
                color = statColor
            )
        }
    }
}

/**
 * 动态卡片作者头像：真实头像优先，缺失 / 加载失败时回落为渐变底 + 兜底文字
 * （对齐官网 `.msg-moment-share__avatar--fb`）。
 */
@Composable
private fun MomentShareAvatar(
    avatarUrl: String,
    fallback: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    if (avatarUrl.isBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF334155)))),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = fallback.ifBlank { "?" },
                style = TextStyle(
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    } else {
        AsyncNetworkImage(
            url = avatarUrl,
            contentDescription = "动态作者头像",
            modifier = modifier.size(size),
            shape = shape,
            showPlaceholder = false
        )
    }
}

/** 动态卡片性别角标（对齐官网 `.msg-moment-share__gender`） */
@Composable
private fun MomentShareGenderBadge(gender: String) {
    val (symbol, color) = when (gender) {
        "male" -> "♂" to Color(0xFF3B82F6)
        "female" -> "♀" to Color(0xFFEC4899)
        else -> return
    }
    BasicText(
        text = symbol,
        style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    )
}

/** 动态卡片底部计数项（点赞 / 评论） */
@Composable
private fun MomentShareStat(
    icon: ImageVector,
    count: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(3.dp))
        BasicText(
            text = count.toString(),
            style = TextStyle(color = color, fontSize = 12.sp)
        )
    }
}

/**
 * 动态图片九宫格
 *
 * 单图按官网限制放大（约 180×128dp），多图 3 列正方裁切；
 * 不足一行的末行用等宽 Spacer 占位，保持网格对齐。
 */
@Composable
private fun MomentShareImageGrid(
    images: List<String>,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    if (images.size == 1) {
        AsyncNetworkImage(
            url = images.first(),
            contentDescription = "动态图片",
            modifier = modifier
                .width(180.dp)
                .height(128.dp),
            shape = shape
        )
        return
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        images.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { url ->
                    AsyncNetworkImage(
                        url = url,
                        contentDescription = "动态图片",
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = shape
                    )
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

