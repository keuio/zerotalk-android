package top.lanxint.zerotalk.ui.messages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import top.lanxint.zerotalk.ui.theme.AppleHigColorTokens
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.ConversationItem
import top.lanxint.zerotalk.data.model.MessageCategory
import top.lanxint.zerotalk.ui.components.ZeroTalkBottomTab
import top.lanxint.zerotalk.ui.components.ZeroTalkBottomTabs
import top.lanxint.zerotalk.ui.components.UserAvatar
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import com.kashif_e.backdrop.Backdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 「消息」主界面
 *
 * 核心特性：
 * 1. 顶部复用 ZeroTalkBottomTabs 液态毛玻璃滑动胶囊（“全部”、“暗号”、“匹配”、“私聊”）；
 * 2. 页面支持 HorizontalPager 左右无级滑动手势，与顶部胶囊指示器双向物理联动；
 * 3. 全面复刻原生 iOS 信息 (iMessage) 满宽列表：无卡片左右边距、48dp 头像、文字下方 0.5dp 细分割线；
 * 4. 紧凑细腻的 14sp / 17sp 行高排版规范，完全消除视觉臃肿。
 */
@Composable
fun MessagesScreen(
    conversations: List<ConversationItem>,
    onConversationClick: (ConversationItem) -> Unit,
    backdrop: Backdrop? = null,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val categories = MessageCategory.entries
    val pagerState = rememberPagerState { categories.size }
    val coroutineScope = rememberCoroutineScope()

    var activeMenuConversation by remember { mutableStateOf<ConversationItem?>(null) }
    var menuAnchorBounds by remember { mutableStateOf(Rect.Zero) }
    var isMenuShowing by remember { mutableStateOf(false) }
    // 长按菜单的二次确认：对齐官网 ChatView「退出房间 / 删除房间」各自的确认弹窗
    var pendingLeaveRoom by remember { mutableStateOf<ConversationItem?>(null) }
    var pendingDeleteRoom by remember { mutableStateOf<ConversationItem?>(null) }
    var roomActionBusy by remember { mutableStateOf(false) }

    val notificationState = LocalNotificationState.current

    Box(modifier = modifier.fillMaxSize()) {
        // ---- 中间左右滑动分页内容区 ----
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val currentCategory = categories[page]
            val filteredList = remember(conversations, currentCategory) {
                if (currentCategory == MessageCategory.ALL) {
                    conversations
                } else {
                    conversations.filter { it.category == currentCategory }
                }
            }

            if (filteredList.isEmpty()) {
                // 空状态插画与提示 (Apple HIG 风格)
                EmptyMessagesView(category = currentCategory, isDark = isDark)
            } else {
                // 原生 iOS iMessage 满宽列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 100.dp, // 预留顶部悬浮 Tab 区域
                        bottom = 120.dp // 预留底部主导航栏高度
                    )
                ) {
                    items(
                        items = filteredList,
                        key = { it.id },
                        contentType = { "conversation_item" }
                    ) { item ->
                        MessageItemRow(
                            item = item,
                            isDark = isDark,
                            isLifted = isMenuShowing && activeMenuConversation?.id == item.id,
                            onClick = { onConversationClick(item) },
                            onLongPress = { bounds ->
                                menuAnchorBounds = bounds
                                activeMenuConversation = item
                                isMenuShowing = true
                            }
                        )
                        // iMessage 分割线：起始对齐文字左边线 (76dp)，不划穿左侧 48dp 头像
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp),
                            thickness = 0.5.dp,
                            color = higColors.separator.copy(alpha = if (isDark) 0.35f else 0.20f)
                        )
                    }
                }
            }
        }

        // ---- 顶部居中悬浮滑动筛选 Tab (复用 ZeroTalkBottomTabs) ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            ZeroTalkBottomTabs(
                selectedTabIndex = { pagerState.currentPage },
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                tabsCount = categories.size,
                backdrop = backdrop,
                outerHeight = 38.dp,
                innerHeight = 32.dp,
                modifier = Modifier.width(300.dp),
                isDark = isDark
            ) {
                categories.forEachIndexed { index, cat ->
                    val isSelected = pagerState.currentPage == index
                    ZeroTalkBottomTab(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }) {
                        BasicText(
                            text = cat.title,
                            style = TextStyle(
                                fontFamily = AppleHigTypography.defaultFontFamily,
                                color = if (isSelected) {
                                    if (isDark) Color(0xFF60A5FA) else Color(0xFF007AFF)
                                } else {
                                    if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
                                },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                platformStyle = AppleHigTypography.defaultPlatformStyle,
                                lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                            )
                        )
                    }
                }
            }
        }

        // ---- 长按会话菜单 (置顶 / 删除) ----
        activeMenuConversation?.let { menuConv ->
            // 菜单打开期间会话可能已被更新（置顶 / 昵称 / 头像）：始终取列表里的实时状态，
            // 避免菜单文案停留在「打开菜单那一刻」的快照（置顶后仍显示「置顶会话」）
            val selectedConv = conversations.find { it.id == menuConv.id } ?: menuConv
            ListContextMenuOverlay(
                anchorItem = selectedConv,
                anchorBounds = menuAnchorBounds,
                isShowing = isMenuShowing,
                isDark = isDark,
                higColors = AppleHigColors.colors(isDark),
                backdrop = backdrop,
                onDismiss = {
                    isMenuShowing = false
                    // 延迟清空选中的 item，让消失动画能够完整执行
                    coroutineScope.launch {
                        delay(200)
                        activeMenuConversation = null
                    }
                },
                onPin = {
                    ZeroTalkClientManager.setConversationPinned(selectedConv.id, !selectedConv.isPinned) { success, message ->
                        if (success) {
                            notificationState.show(if (selectedConv.isPinned) "已取消 ${selectedConv.targetName} 置顶" else "已将 ${selectedConv.targetName} 置顶")
                        } else notificationState.show(message ?: "置顶操作失败")
                    }
                    isMenuShowing = false
                    coroutineScope.launch { delay(200); activeMenuConversation = null }
                },
                onLeaveRoom = {
                    // 先关闭长按菜单，再弹确认（对齐官网：先关侧栏/菜单再弹「确定退出房间？」）
                    isMenuShowing = false
                    coroutineScope.launch { delay(160); activeMenuConversation = null }
                    roomActionBusy = false
                    pendingLeaveRoom = selectedConv
                },
                onDeleteRoom = {
                    isMenuShowing = false
                    coroutineScope.launch { delay(160); activeMenuConversation = null }
                    roomActionBusy = false
                    pendingDeleteRoom = selectedConv
                }
            )
        }

        // ---- 退出房间确认（官网文案：确定退出房间？/ 退出后你将不再是该暗号房成员） ----
        pendingLeaveRoom?.let { conv ->
            AlertDialog(
                onDismissRequest = { if (!roomActionBusy) pendingLeaveRoom = null },
                title = { Text("确定退出房间？") },
                text = { Text("退出后你将不再是该暗号房成员") },
                confirmButton = {
                    TextButton(
                        enabled = !roomActionBusy,
                        onClick = {
                            roomActionBusy = true
                            ZeroTalkClientManager.leaveRoom(conv) { success, message ->
                                roomActionBusy = false
                                pendingLeaveRoom = null
                                notificationState.show(
                                    if (success) "已退出 ${conv.targetName}" else (message ?: "退出失败")
                                )
                            }
                        }
                    ) {
                        Text(
                            text = if (roomActionBusy) "退出中..." else "确认退出",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(enabled = !roomActionBusy, onClick = { pendingLeaveRoom = null }) {
                        Text("取消")
                    }
                }
            )
        }

        // ---- 删除房间确认（官网文案：群聊「删除后所有成员将无法再进入该房间」/
        //      私聊「删除后彼此将无法查看该房间，此操作不可恢复。」；私聊另提供「删除并拉黑」） ----
        pendingDeleteRoom?.let { conv ->
            val isGroupRoom = conv.category == MessageCategory.CODE
            AlertDialog(
                onDismissRequest = { if (!roomActionBusy) pendingDeleteRoom = null },
                title = { Text("确定删除房间？") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (isGroupRoom) "删除后所有成员将无法再进入该房间"
                            else "删除后彼此将无法查看该房间，此操作不可恢复。"
                        )
                        if (!isGroupRoom) {
                            Text("选择「删除并拉黑」会同时拉黑对方，拉黑后将无法匹配或收到 TA 的私信。")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !roomActionBusy,
                        onClick = {
                            roomActionBusy = true
                            ZeroTalkClientManager.deleteRoom(conv, blockPeer = false) { success, message ->
                                roomActionBusy = false
                                pendingDeleteRoom = null
                                notificationState.show(
                                    if (success) "已删除与 ${conv.targetName} 的房间" else (message ?: "删除房间失败")
                                )
                            }
                        }
                    ) {
                        Text(
                            text = if (roomActionBusy) "删除中..." else "确认删除",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(enabled = !roomActionBusy, onClick = { pendingDeleteRoom = null }) {
                            Text("取消")
                        }
                        if (!isGroupRoom) {
                            TextButton(
                                enabled = !roomActionBusy,
                                onClick = {
                                    roomActionBusy = true
                                    ZeroTalkClientManager.deleteRoom(conv, blockPeer = true) { success, message ->
                                        roomActionBusy = false
                                        pendingDeleteRoom = null
                                        notificationState.show(
                                            if (success) "已删除房间并拉黑对方" else (message ?: "删除房间失败")
                                        )
                                    }
                                }
                            ) {
                                Text("删除并拉黑", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * 列表项长按上下文菜单浮层
 */
@Composable
private fun ListContextMenuOverlay(
    anchorItem: ConversationItem,
    anchorBounds: Rect,
    isShowing: Boolean,
    isDark: Boolean,
    higColors: AppleHigColorTokens,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onLeaveRoom: () -> Unit,
    onDeleteRoom: () -> Unit
) {
    val overlayAlpha = remember { Animatable(0f) }
    val menuScale = remember { androidx.compose.animation.core.Animatable(0.75f) }
    val menuAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

    // pointerInput(Unit) 不会重启，onDismiss 需保持最新
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(isShowing) {
        if (isShowing) {
            launch {
                overlayAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200)
                )
            }
            launch {
                menuScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f)
                )
            }
            launch {
                menuAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 180)
                )
            }
        } else {
            launch {
                overlayAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180)
                )
            }
            launch {
                menuScale.animateTo(
                    targetValue = 0.75f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f)
                )
            }
            launch {
                menuAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 150)
                )
            }
        }
    }

    if (overlayAlpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33000000).copy(alpha = 0.2f * overlayAlpha.value))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { currentOnDismiss() })
                }
        ) {
            // 将菜单定位在所长按的列表项（anchorBounds）的正下方
            val offsetY = with(LocalDensity.current) {
                (anchorBounds.bottom + 8f).toDp()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier
                        .offset(y = offsetY)
                        .graphicsLayer {
                            alpha = menuAlpha.value
                            scaleX = menuScale.value
                            scaleY = menuScale.value
                            transformOrigin = TransformOrigin(0.5f, 0f)
                        }
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xE6252528) else Color(0xFFF2F2F7))
                        .border(0.5.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1A000000), RoundedCornerShape(16.dp))
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 置顶
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onPin)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            tint = higColors.label,
                            modifier = Modifier.size(18.dp)
                        )
                        BasicText(
                            text = if (anchorItem.isPinned) "取消置顶" else "置顶会话",
                            style = TextStyle(color = higColors.label, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                        )
                    }

                    // 退出房间（仅暗号房/群聊成员；对齐官网 ChatView「退出房间」）
                    if (anchorItem.category == MessageCategory.CODE) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(start = 16.dp)
                                .background(if (isDark) Color(0x33FFFFFF) else Color(0x1A000000))
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onLeaveRoom)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(18.dp)
                            )
                            BasicText(
                                text = "退出房间",
                                style = TextStyle(color = Color(0xFFFF9500), fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    // 删除房间（私聊/匹配双方均可删除；暗号房仅创建者可删除；对齐官网 ChatView「删除房间」）
                    if (anchorItem.category != MessageCategory.CODE || anchorItem.isRoomCreator) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(start = 16.dp)
                                .background(if (isDark) Color(0x33FFFFFF) else Color(0x1A000000))
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onDeleteRoom)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFFF453A),
                                modifier = Modifier.size(18.dp)
                            )
                            BasicText(
                                text = "删除房间",
                                style = TextStyle(color = Color(0xFFFF453A), fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 原生 iOS iMessage 满宽列表行组件
 */
@Composable
private fun MessageItemRow(
    item: ConversationItem,
    isDark: Boolean,
    isLifted: Boolean = false,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    var rowBounds by remember { mutableStateOf(Rect.Zero) }
    // 回调保持最新：pointerInput 的 block 只在 key 变化时重启，直接引用 onClick/onLongPress
    // 会让闭包停留在上次 key 变化时的组合（置顶只改 isPinned、item.id 不变），
    // 长按菜单便会一直读到旧状态（置顶后仍显示「置顶会话」）
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    val pinnedBg = if (item.isPinned) {
        if (isDark) Color.White.copy(alpha = 0.045f) else Color.Black.copy(alpha = 0.035f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(pinnedBg)
            .graphicsLayer {
                // 长按弹出菜单时保持列表项可见（此前 alpha=0 会让该行直接「消失」）
                alpha = if (isLifted) 0.55f else 1f
            }
            .onGloballyPositioned { coords ->
                rowBounds = coords.boundsInRoot()
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { currentOnClick() },
                    onLongPress = { currentOnLongPress(rowBounds) }
                )
            }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧 48dp 头像（带渐变色彩与右下角在线状态点）
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // 真实头像优先，无头像/加载失败时回落为渐变 + 首字母
            UserAvatar(
                url = item.targetAvatar,
                name = item.targetName,
                size = 48.dp,
                gradient = item.avatarGradient,
                fallbackTextStyle = TextStyle(
                    fontFamily = AppleHigTypography.defaultFontFamily,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    platformStyle = AppleHigTypography.defaultPlatformStyle,
                    lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                )
            )

            // 在线与离线状态圆点（遵循 Apple HIG：仅适用于 1v1 会话且状态明确；隐身或未知则优雅隐去无标识）
            if (item.category != MessageCategory.CODE && item.isOnline != null) {
                val statusDotColor = if (item.isOnline == true) {
                    higColors.systemGreen
                } else {
                    if (isDark) Color(0xFF48484A) else Color(0xFF8E8E93)
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(statusDotColor)
                        .border(
                            width = 2.dp,
                            color = if (isDark) Color(0xFF12141A) else Color(0xFFF4F2F9),
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 中间文本内容区
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // 顶部行：对方名称 + 场景标签 + 时间戳
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = item.targetName,
                        style = AppleHigTypography.groupedRowTitle.copy(
                            color = higColors.label,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    val displayTag = item.tag?.trim()?.takeIf { it.isNotEmpty() }
                    if (!displayTag.isNullOrBlank() && displayTag != "在线" && displayTag != "清流") {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (item.category) {
                                        MessageCategory.CODE -> higColors.systemGreen.copy(alpha = 0.15f)
                                        MessageCategory.MATCH -> higColors.systemPurple.copy(alpha = 0.15f)
                                        else -> higColors.systemBlue.copy(alpha = 0.12f)
                                    }
                                )
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            BasicText(
                                text = displayTag,
                                style = TextStyle(
                                    fontFamily = AppleHigTypography.defaultFontFamily,
                                    color = when (item.category) {
                                        MessageCategory.CODE -> higColors.systemGreen
                                        MessageCategory.MATCH -> higColors.systemPurple
                                        else -> higColors.systemBlue
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    platformStyle = AppleHigTypography.defaultPlatformStyle,
                                    lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (item.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "置顶",
                            tint = higColors.secondaryLabel.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(11.dp)
                                .padding(end = 3.dp)
                        )
                    }
                    BasicText(
                        text = item.timestamp,
                        style = AppleHigTypography.footnote.copy(
                            color = higColors.secondaryLabel,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            // 底部行：消息摘要与未读红点/蓝点
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isVoice) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = higColors.systemBlue,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 2.dp)
                    )
                }

                BasicText(
                    text = item.lastMessage,
                    style = AppleHigTypography.groupedRowValue.copy(
                        color = higColors.secondaryLabel
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // 未读消息标记 (iOS iMessage 风格纯色小圆形蓝底白字徽标)
                if (item.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(higColors.systemBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = item.unreadCount.toString(),
                            style = TextStyle(
                                fontFamily = AppleHigTypography.defaultFontFamily,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                platformStyle = AppleHigTypography.defaultPlatformStyle,
                                lineHeightStyle = AppleHigTypography.defaultLineHeightStyle
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 空状态展示
 */
@Composable
private fun EmptyMessagesView(
    category: MessageCategory,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isDark) Color(0xFF222630) else Color(0xFFE8EDF5)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = higColors.secondaryLabel,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        BasicText(
            text = "暂无${category.title}会话",
            style = AppleHigTypography.headline.copy(
                color = higColors.label,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(Modifier.height(6.dp))

        BasicText(
            text = when (category) {
                MessageCategory.CODE -> "通过暗号匹配到的新好友会话将汇聚在此"
                MessageCategory.MATCH -> "通过随机语聊或文字连线的对话将展示在此"
                MessageCategory.PRIVATE -> "与好友的私密单聊互动将沉淀在此"
                MessageCategory.ALL -> "在零语探索心语、交换暗号，开启你的第一段对话"
            },
            style = AppleHigTypography.footnote.copy(
                color = higColors.secondaryLabel
            )
        )
    }
}
