package top.lanxint.zerotalk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.ChatMessage
import top.lanxint.zerotalk.data.model.REAL_USER_ID_PLACEHOLDERS
import top.lanxint.zerotalk.data.model.UserProfileTarget
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.GenderBadge
import top.lanxint.zerotalk.ui.components.LocalImageViewer
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.ChatDice
import top.lanxint.zerotalk.ui.components.shouldAnimateDice
import androidx.compose.foundation.lazy.itemsIndexed
import top.lanxint.zerotalk.ui.messages.BubbleImageContent
import top.lanxint.zerotalk.ui.messages.MessageTimeHelper
import top.lanxint.zerotalk.ui.messages.MusicPlaylistCard
import top.lanxint.zerotalk.ui.messages.StickerBubble
import top.lanxint.zerotalk.ui.navigation.rememberHasUserProfileLayers
import top.lanxint.zerotalk.ui.utils.BackHandler

/**
 * 零语大厅（公共聊天室）
 * 具备标准的 iOS 风格顶部 NavigationBar、实时公屏流与悬浮输入框。
 *
 * 消息气泡采用 QQ 群聊式布局：对方消息头像独占最左一列，右侧竖排「昵称在上、气泡在下」
 * （昵称/性别仅在同一发送者连续消息的首条展示），气泡为四角同半径的普通圆气泡；
 * 自己的消息不展示头像与昵称，蓝色圆气泡靠右。
 */
@Composable
fun PublicChatroomScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    /** 点击消息里的发送者（头像/昵称）时跳转其个人资料；大厅没有群聊面板，仅此入口 */
    onOpenUserProfile: ((UserProfileTarget) -> Unit)? = null
) {
    val higColors = AppleHigColors.colors(isDark)
    val bgColor = if (isDark) Color(0xFF12141A) else Color(0xFFF3F5F9)
    val navBarBg = if (isDark) Color(0xFF1B1E26) else Color(0xFFFFFFFF)
    val titleColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
    val accentColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF007AFF)

    val hallMessages by ZeroTalkClientManager.hallMessages.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val notificationState = LocalNotificationState.current
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var inputText by remember { mutableStateOf("") }
    var mentionTargets by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var longPressedMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // 资料页层级栈（大厅点发送者打开的资料面板）压在上面时，返回事件属于那一层
    val hasProfileLayers = rememberHasUserProfileLayers()

    // 长按菜单浮层拦截（被资料页层级覆盖时让出返回）
    BackHandler(enabled = !hasProfileLayers && longPressedMessage != null) {
        longPressedMessage = null
    }

    // 大厅主界面返回拦截：退出大厅回到首页（被资料页层级覆盖时让出返回）
    BackHandler(enabled = !hasProfileLayers && longPressedMessage == null) {
        onBack()
    }

    // 进入页面时进入大厅，离开时退出大厅
    DisposableEffect(Unit) {
        ZeroTalkClientManager.enterPublicHall()
        onDispose {
            ZeroTalkClientManager.leavePublicHall()
        }
    }

    // 新消息滚动到底部
    LaunchedEffect(hallMessages.size) {
        if (hallMessages.isNotEmpty()) {
            listState.animateScrollToItem(hallMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .imePadding()
    ) {
        // iOS 风格顶部导航栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(navBarBg)
                .statusBarsPadding()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // 左侧返回按钮
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(7.2.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "返回",
                    tint = accentColor,
                    modifier = Modifier.size(16.2.dp)
                )
                BasicText(
                    text = "首页",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }

            // 中间标题
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(
                    text = "零语大厅",
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    text = "在线畅聊",
                    style = TextStyle(
                        color = subtitleColor,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // 消息列表或空占位
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (hallMessages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.8.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E2430) else Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(32.4.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    BasicText(
                        text = "欢迎来到零语大厅",
                        style = TextStyle(
                            color = titleColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        text = "零语大厅消息全员实时同步，来发一条打个招呼吧～",
                        style = TextStyle(
                            color = subtitleColor,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    itemsIndexed(hallMessages, key = { _, it -> it.id }) { index, msg ->
                        val isFirstInGroup = MessageTimeHelper.isFirstOfSenderGroup(hallMessages, index)
                        val isPeerMessage = !msg.isMine && !msg.isSystem
                        val bottomSpacing = MessageTimeHelper.computeMessageBottomSpacing(hallMessages, index)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            HallMessageBubble(
                                msg = msg,
                                isDark = isDark,
                                showAvatar = isPeerMessage && isFirstInGroup,
                                reserveAvatarSpace = isPeerMessage,
                                showSenderInfo = isPeerMessage && isFirstInGroup,
                                onSenderClick = if (isPeerMessage &&
                                    msg.senderId.isNotBlank() && msg.senderId !in REAL_USER_ID_PLACEHOLDERS
                                ) {
                                    {
                                        onOpenUserProfile?.invoke(
                                            UserProfileTarget(
                                                userId = "",
                                                uid = msg.senderId,
                                                name = msg.senderName,
                                                avatarUrl = msg.senderAvatar
                                            )
                                        )
                                    }
                                } else null,
                                onLongPress = { if (!msg.isMine && !msg.isSystem) longPressedMessage = msg },
                                onPat = if (!msg.isMine && !msg.isSystem && !msg.isImage) {
                                    {
                                        notificationState.show(
                                            ZeroTalkClientManager.sendPat(
                                                ZeroTalkClientManager.hallRoomId.value,
                                                msg.senderId
                                            ).message(msg.senderName)
                                        )
                                    }
                                } else null
                            )

                            if (bottomSpacing.value > 0f) {
                                Spacer(modifier = Modifier.height(bottomSpacing))
                            }
                        }
                    }
                }
            }
        }

        // 底部输入栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF1B1E26) else Color(0xFFFFFFFF))
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 输入框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(if (isDark) Color(0xFF262C38) else Color(0xFFF1F5F9))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputText.isEmpty()) {
                        BasicText(
                            text = "在零语大厅说点什么...",
                            style = TextStyle(
                                color = subtitleColor,
                                fontSize = 15.sp
                            )
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(
                            color = titleColor,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(inputFocusRequester)
                    )
                }

                // 发送按钮
                val canSend = inputText.trim().isNotBlank()
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (canSend) accentColor else (if (isDark) Color(0xFF2E3440) else Color(0xFFE2E8F0)))
                        .clickable(enabled = canSend) {
                            val textToSend = inputText.trim()
                            if (textToSend.isNotBlank()) {
                                val mentionIds = mentionTargets
                                    .map { it.first }
                                    .filter(String::isNotBlank)
                                    .distinct()
                                inputText = ""
                                mentionTargets = emptyList()
                                ZeroTalkClientManager.sendHallMessage(textToSend, mentionIds) { ok ->
                                    if (!ok) {
                                        coroutineScope.launch {
                                            notificationState.show("消息未发送成功，连接已断开，请稍后重试")
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "发送",
                        tint = if (canSend) Color.White else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    longPressedMessage?.let { target ->
        AlertDialog(
            onDismissRequest = { longPressedMessage = null },
            title = { BasicText(target.senderName.ifBlank { "消息操作" }, style = AppleHigTypography.headline.copy(color = titleColor)) },
            text = { BasicText("选择对 ${target.senderName.ifBlank { "这位用户" }} 的操作", style = AppleHigTypography.body.copy(color = subtitleColor)) },
            confirmButton = {
                TextButton(onClick = {
                    notificationState.show(
                        ZeroTalkClientManager.sendPat(ZeroTalkClientManager.hallRoomId.value, target.senderId)
                            .message(target.senderName)
                    )
                    longPressedMessage = null
                }) { BasicText("拍一拍") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val name = target.senderName.ifBlank { target.senderId }
                    val separator = if (inputText.isNotBlank() && !inputText.last().isWhitespace()) " " else ""
                    inputText = "$inputText$separator@$name "
                    if (target.senderId.isNotBlank() && target.senderId != "peer" && target.senderId != "me") {
                        mentionTargets = (mentionTargets + (target.senderId to name)).distinctBy { it.first }
                    }
                    longPressedMessage = null
                    coroutineScope.launch {
                        delay(100)
                        inputFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }) { BasicText("@提及") }
            }
        )
    }
}

/**
 * 大厅单条消息气泡（统一普通圆角气泡，无尖尾）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HallMessageBubble(
    msg: ChatMessage,
    isDark: Boolean,
    showSenderInfo: Boolean = false,
    showAvatar: Boolean = false,
    reserveAvatarSpace: Boolean = false,
    onSenderClick: (() -> Unit)? = null,
    onLongPress: () -> Unit = {},
    /** 双击气泡 = 拍一拍（为 null 时单击保持即时响应） */
    onPat: (() -> Unit)? = null
) {
    val isMine = msg.isMine
    // 大厅统一使用正常的圆气泡（四角同半径），不再使用 iMessage 尖尾
    val bubbleShape = RoundedCornerShape(16.dp)
    val bubbleBg = when {
        isMine -> if (isDark) Color(0xFF007AFF) else Color(0xFF007AFF)
        else -> if (isDark) Color(0xFF262C38) else Color(0xFFE5E7EB)
    }
    val textColor = when {
        isMine -> Color.White
        else -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    }
    val timeColor = when {
        isMine -> Color.White.copy(alpha = 0.7f)
        else -> if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
    }

    // QQ 群聊式布局：对方消息「头像独占最左一列 + 右侧竖排（昵称在上、气泡在下）」；
    // 连续消息折叠头像时保留 44dp 留白占位，确保同一成员连续气泡左边缘对齐整洁；
    // 自己的消息不展示头像与昵称，蓝色气泡靠右。
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (reserveAvatarSpace) {
            if (showAvatar) {
                UserAvatar(
                    url = msg.senderAvatar,
                    name = msg.senderName.ifBlank { msg.senderId },
                    size = 36.dp,
                    fallbackIconSize = 16.dp,
                    modifier = Modifier.then(
                        if (onSenderClick != null) {
                            Modifier
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onSenderClick
                                )
                        } else {
                            Modifier
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(44.dp))
            }
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            // 昵称 + 性别：展示在同一发送者连续消息的首条，位于气泡右上方
            if (!isMine && showSenderInfo) {
                Row(
                    modifier = Modifier
                        .padding(start = 2.dp, bottom = 4.dp)
                        .then(
                            if (onSenderClick != null) {
                                Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onSenderClick
                                    )
                            } else {
                                Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    BasicText(
                        text = msg.senderName.ifBlank { "零友" },
                        style = TextStyle(
                            color = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    GenderBadge(
                        gender = msg.senderGender,
                        isDark = isDark,
                        fontSize = 10.sp,
                        horizontalPadding = 5.dp,
                        verticalPadding = 1.dp
                    )
                }
            }

            val imageViewer = LocalImageViewer.current

            Box(
                modifier = Modifier
                    // 骰子 / 歌单 / 表情包与官网一致：裸媒体块
                    // （`.msg-media-wrap` 背景透明、无内边距、无边框）
                    .then(
                        if (msg.isDice || msg.isMusicPlaylist || msg.isSticker) Modifier
                        else Modifier.clip(bubbleShape).background(bubbleBg)
                    )
                    .combinedClickable(
                        onClick = {
                            if (msg.isImage && msg.imageUrl.isNotBlank()) {
                                imageViewer.open(msg.imageUrl)
                            }
                        },
                        onLongClick = onLongPress,
                        onDoubleClick = onPat
                    )
                    .padding(
                        if (msg.isImage || msg.isDice || msg.isMusicPlaylist || msg.isSticker) {
                            PaddingValues(0.dp)
                        } else {
                            PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                        }
                    )
            ) {
                if (msg.isImage && msg.imageUrl.isNotBlank()) {
                    // 图片消息：复用统一 BubbleImageContent（原比例自适应）
                    BubbleImageContent(
                        imageUrl = msg.imageUrl,
                        isMine = isMine,
                        isDark = isDark,
                        shape = bubbleShape,
                        onClick = { imageViewer.open(msg.imageUrl) }
                    )
                } else if (msg.isDice) {
                    // 骰子：点数由服务端摇出（实测 content 即点数），渲染官网同款骰面
                    ChatDice(
                        value = msg.diceValue,
                        animate = msg.shouldAnimateDice()
                    )
                } else if (msg.isMusicPlaylist) {
                    // 歌单卡片：解析失败（musicPlaylist 为 null）显示「歌单已失效」
                    MusicPlaylistCard(
                        playlist = msg.musicPlaylist,
                        isDark = isDark
                    )
                } else if (msg.isSticker) {
                    // 表情包：优先 image_url，否则按 asset_id 查本地列表，都拿不到显示「表情包已失效」
                    StickerBubble(
                        stickerUrl = msg.stickerUrl,
                        assetId = msg.stickerAssetId,
                        isDark = isDark,
                        onClick = { url -> imageViewer.open(url) }
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicText(
                            text = if (msg.isPat) {
                                msg.patText.ifBlank { if (msg.isMine) "你 拍了拍 对方" else "有人 拍了拍 你" }
                            } else {
                                msg.content
                            },
                            style = TextStyle(
                                color = textColor,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        )
                    BasicText(
                        text = msg.timestamp,
                        style = TextStyle(
                            color = timeColor,
                            fontSize = 10.sp
                        )
                    )
                    }
                }
            }
        }
    }
}

