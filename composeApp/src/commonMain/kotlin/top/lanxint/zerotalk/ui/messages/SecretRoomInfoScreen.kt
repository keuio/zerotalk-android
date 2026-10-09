package top.lanxint.zerotalk.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberCanvasBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.drawPlainBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.colorControls
import top.lanxint.zerotalk.ui.components.LiquidSegmentedControl
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import top.lanxint.zerotalk.ui.components.UserAvatar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.components.IosLiquidBackButton
import top.lanxint.zerotalk.ui.components.LiquidToggle
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.parseGenderText
import top.lanxint.zerotalk.data.model.ConversationItem
import top.lanxint.zerotalk.data.model.UserProfileTarget
import top.lanxint.zerotalk.data.network.BootstrapMemberDto
import top.lanxint.zerotalk.data.network.ChatBootstrapData
import top.lanxint.zerotalk.data.network.RoomMemberDto
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

private data class RoomMemberUi(
    val userId: String,
    val uid: String,
    val name: String,
    val avatar: String,
    val gender: String,
    val isAdmin: Boolean,
    val isOwner: Boolean,
    val canKick: Boolean = false,
    val canDeleteMessage: Boolean = false,
    val isSelf: Boolean = false
)

/**
 * 成员行待确认的房管操作（破坏性动作统一先弹确认框）
 */
private sealed interface MemberAction {
    val member: RoomMemberUi

    data class Remove(override val member: RoomMemberUi) : MemberAction
    data class Unban(override val member: RoomMemberUi) : MemberAction
}

@Composable
fun SecretRoomInfoScreen(
    conversation: ConversationItem,
    isDark: Boolean,
    backdrop: Backdrop? = null,
    onBack: () -> Unit,
    onMuteChanged: (Boolean) -> Unit,
    /** 点击成员 / 禁止加入名单中的用户时跳转其资料面板 */
    onOpenUserProfile: ((UserProfileTarget) -> Unit)? = null,
    /** 对话专属壁纸（与私聊资料页同一套本地偏好，由 PrivateChatScreen 持有） */
    currentWallpaperKey: String? = null,
    /** 背景已写入管理器后回调（仅做本地状态刷新） */
    onWallpaperApplied: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppleHigColors.colors(isDark)

    val bootstrapMap by ZeroTalkClientManager.roomBootstrapMap.collectAsState()
    val bootstrap: ChatBootstrapData? = bootstrapMap[conversation.id]

    // 成员与「禁止加入」名单以服务端接口为准（bootstrap.members 仅作兜底）
    val membersMap by ZeroTalkClientManager.roomMembers.collectAsState()
    val memberTotalMap by ZeroTalkClientManager.roomMemberTotal.collectAsState()
    val membersLoadingMap by ZeroTalkClientManager.roomMembersLoading.collectAsState()
    val bansMap by ZeroTalkClientManager.roomJoinBans.collectAsState()
    val bansLoadingMap by ZeroTalkClientManager.roomJoinBansLoading.collectAsState()

    fun mapMember(dto: BootstrapMemberDto): RoomMemberUi {
        return RoomMemberUi(
            userId = dto.userId.takeIf { it > 0L }?.toString() ?: dto.uid,
            uid = dto.uid,
            name = dto.username.ifBlank { "零语用户" },
            avatar = dto.avatarUrl.orEmpty(),
            gender = dto.gender.orEmpty(),
            isAdmin = dto.isAdmin,
            isOwner = dto.isCreator,
            canKick = dto.canKick,
            canDeleteMessage = dto.canDeleteMessage,
            isSelf = ZeroTalkClientManager.isSelfMember(dto.uid, dto.userId)
        )
    }

    fun mapRoomMember(dto: RoomMemberDto): RoomMemberUi {
        val role = dto.role.orEmpty().lowercase()
        return RoomMemberUi(
            userId = dto.targetId,
            uid = dto.uid.ifBlank { dto.userId.takeIf { it > 0L }?.toString().orEmpty() },
            name = dto.username.ifBlank { "零语用户" },
            avatar = dto.avatarUrl.orEmpty(),
            gender = dto.gender.orEmpty(),
            isAdmin = dto.isAdmin || role == "admin" || role == "administrator",
            isOwner = dto.isCreator || role == "creator" || role == "owner",
            // 管理员的两项授权由 /room/set-member-admin 下发，用于回填「管理员权限」面板
            canKick = dto.canKick,
            canDeleteMessage = dto.canDeleteMessage,
            isSelf = dto.isSelf || ZeroTalkClientManager.isSelfMember(dto.uid, dto.userId)
        )
    }

    val apiMembers = membersMap[conversation.id].orEmpty().map(::mapRoomMember)
    val members = apiMembers.ifEmpty { bootstrap?.members?.map(::mapMember).orEmpty() }
    val bans = bansMap[conversation.id].orEmpty().map { dto ->
        RoomMemberUi(
            userId = dto.targetId,
            uid = dto.uid.ifBlank { dto.userId.takeIf { it > 0L }?.toString().orEmpty() },
            name = dto.username.ifBlank { "零语用户" },
            avatar = dto.avatarUrl.orEmpty(),
            gender = dto.gender.orEmpty(),
            isAdmin = false,
            isOwner = false,
            isSelf = ZeroTalkClientManager.isSelfMember(dto.uid, dto.userId)
        )
    }
    val membersTotal = memberTotalMap[conversation.id]?.takeIf { it > 0 } ?: members.size

    var pendingAction by remember { mutableStateOf<MemberAction?>(null) }
    // 「管理员权限」面板对象与保存中标记（官方 RoomAdminPermModal：保存期间保持打开并显示「保存中…」）
    var adminEditor by remember { mutableStateOf<RoomMemberUi?>(null) }
    var adminSaving by remember { mutableStateOf(false) }
    // 顶层页签：0 成员 / 1 设置
    var selectedTab by remember { mutableStateOf(0) }
    // 成员页内的二级切换：0 成员 / 1 禁止加入（普通成员看不到「禁止加入」）
    var memberTab by remember { mutableStateOf(0) }
    var muted by remember(conversation.id) { mutableStateOf(conversation.isMuted) }
    val membersLoading = membersLoadingMap[conversation.id] == true
    val loading = members.isEmpty() && bootstrap == null && membersLoading
    val bansLoading = bansLoadingMap[conversation.id] == true
    var error by remember { mutableStateOf<String?>(null) }
    val notificationState = LocalNotificationState.current

    // 房管能力：viewer_can_kick 为权威位，管理员身份兜底。
    // 服务端 bootstrap 未必下发 viewer_is_admin（实测为空），此时用成员列表里自身的角色兜底，
    // 否则管理员会被误判成普通成员、「禁止加入」入口被隐藏。
    val viewerCanKick = bootstrap?.viewerCanKick ?: false
    val viewerIsAdmin = (bootstrap?.viewerIsAdmin ?: false) || members.any { it.isSelf && it.isAdmin }
    // 官方 ChatView 权限模型：仅创建者（群主）可设置管理员/取消管理员；创建者与管理员均可移出成员
    val viewerIsOwner = members.any { it.isSelf && it.isOwner }
    val canSetAdmin = viewerIsOwner
    val canManageMembers = viewerIsOwner || viewerIsAdmin || viewerCanKick

    LaunchedEffect(conversation.id) {
        ZeroTalkClientManager.loadRoomHistory(conversation.id)
        ZeroTalkClientManager.loadRoomMembers(conversation.id)
        ZeroTalkClientManager.loadRoomJoinBans(conversation.id)
    }

    /**
     * 执行房管动作（成员/封禁名单变更），失败时把服务端 msg 落到 error 上提示
     */
    fun performMemberAction(action: MemberAction, banJoin: Boolean = false) {
        val onResult: (Boolean, String?) -> Unit = { success, message ->
            error = if (success) null else (message ?: "操作失败")
        }
        when (action) {
            is MemberAction.Remove -> ZeroTalkClientManager.removeRoomMember(
                conversation.id, action.member.userId, banJoin, onResult
            )
            is MemberAction.Unban -> ZeroTalkClientManager.unbanRoomJoin(
                conversation.id, action.member.userId, onResult
            )
        }
    }

    val fallbackBackdrop = rememberCanvasBackdrop {
        drawRect(if (isDark) Color(0xFF12141A) else Color(0xFFF4F2F9))
    }
    val actualBackdrop = backdrop ?: fallbackBackdrop
    val infoScreenBackdrop = rememberLayerBackdrop()
    val cardBackdrop = rememberCanvasBackdrop { drawRect(colors.systemFill) }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawPlainBackdrop(
                    backdrop = actualBackdrop,
                    shape = { RectangleShape },
                    effects = {
                        colorControls(
                            brightness = if (!isDark) 0.12f else 0.04f,
                            saturation = 1.35f
                        )
                        blur(if (!isDark) 24.dp.toPx() else 20.dp.toPx())
                    }
                )
                .background(
                    if (isDark) Color(0xFF12141A).copy(alpha = 0.75f)
                    else Color(0xFFF2F2F7).copy(alpha = 0.72f)
                )
                .layerBackdrop(infoScreenBackdrop)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(58.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserAvatar(
                url = conversation.targetAvatar,
                name = conversation.targetName,
                size = 80.dp,
                gradient = conversation.avatarGradient,
                fallbackTextStyle = TextStyle(color = Color.White, fontSize = 30.6.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )
            Spacer(Modifier.height(12.dp))
            BasicText(
                text = conversation.targetName,
                style = TextStyle(color = colors.label, fontSize = 19.8.sp, lineHeight = 25.2.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(4.dp))
            BasicText("${membersTotal} 位成员 · 暗号房", style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel))
        }

        LiquidSegmentedControl(
            options = listOf("成员", "设置"),
            selectedIndex = selectedTab,
            onOptionSelect = { selectedTab = it },
            outerHeight = 38.dp,
            innerHeight = 32.dp,
            isDark = isDark,
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        )

        if (selectedTab == 1) {
            // ---- 设置：通知（隐藏提醒）+ 对话专属壁纸，复用会话资料页同一套内容 ----
            ChatSettingsTabContent(
                currentWallpaperKey = currentWallpaperKey,
                onWallpaperApplied = onWallpaperApplied,
                isDark = isDark,
                conversation = conversation,
                // 外层滚动容器已 padding(horizontal = 16.dp)，这里再留白会让卡片被缩进两次
                horizontalPadding = 0.dp
            )
        } else {
        if (canManageMembers) {
            // 成员页内的二级切换：成员 / 禁止加入
            LiquidSegmentedControl(
                options = listOf("成员", "禁止加入"),
                selectedIndex = memberTab,
                onOptionSelect = { memberTab = it },
                outerHeight = 32.dp,
                innerHeight = 26.dp,
                isDark = isDark,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }

        error?.takeIf { members.isNotEmpty() || bans.isNotEmpty() }?.let { message ->
            BasicText(
                text = message,
                style = AppleHigTypography.footnote.copy(color = colors.destructive),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 4.dp)
            )
        }

        if (loading) {
            BasicText("正在加载…", style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel), modifier = Modifier.padding(20.dp))
        } else if (error != null && members.isEmpty() && bans.isEmpty()) {
            BasicText(error ?: "加载失败", style = AppleHigTypography.footnote.copy(color = colors.destructive), modifier = Modifier.padding(20.dp))
        } else {
            val users = if (memberTab == 0) members else bans
            AppleHigFillCard(
                isDark = isDark,
                modifier = Modifier.fillMaxWidth(),
                contentPaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                // 分组标题放进卡片内（与列表同卡，不再悬在卡片外面）
                BasicText(
                    text = if (memberTab == 0) "成员 (${users.size})" else "禁止加入 (${users.size})",
                    style = AppleHigTypography.groupedSectionHeader.copy(color = colors.secondaryLabel),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .height(0.5.dp)
                        .background(colors.separator.copy(alpha = 0.45f))
                )
                if (users.isEmpty()) {
                    val emptyText = when {
                        (memberTab == 0 && (loading || membersLoading)) || (memberTab == 1 && bansLoading) -> "正在加载…"
                        memberTab == 0 -> "暂无成员资料"
                        else -> "暂无禁止加入的用户"
                    }
                    BasicText(
                        emptyText,
                        style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    // 一排两个：长名单一屏能看更多人；奇数个时最后一行靠左
                    users.chunked(2).forEachIndexed { rowIndex, rowMembers ->
                        if (rowIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp)
                                    .height(0.5.dp)
                                    .background(colors.separator.copy(alpha = 0.45f))
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowMembers.forEach { member ->
                                MemberGridCell(
                                    member = member,
                                    isDark = isDark,
                                    showMemberActions = memberTab == 0,
                                    showBanAction = memberTab == 1,
                                    canSetAdmin = canSetAdmin,
                                    canManageMembers = canManageMembers,
                                    viewerIsOwner = viewerIsOwner,
                                    onOpenProfile = {
                                        // 点击成员/被禁用户 → 跳转其资料面板（群聊与群聊面板行为一致）
                                        onOpenUserProfile?.invoke(
                                            UserProfileTarget(
                                                userId = member.userId,
                                                uid = member.uid,
                                                name = member.name,
                                                avatarUrl = member.avatar
                                            )
                                        )
                                    },
                                    onEditAdmin = { adminEditor = member },
                                    onRemove = { pendingAction = MemberAction.Remove(member) },
                                    onUnban = { pendingAction = MemberAction.Unban(member) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowMembers.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        } // 成员 tab 分支结束
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 21.dp, vertical = 6.dp)
    ) {
        IosLiquidBackButton(
            onClick = onBack,
            backdrop = infoScreenBackdrop,
            isDark = isDark,
            isWhiteBackground = !isDark,
            // 与私聊页一致：只在毛玻璃上叠一层极淡的底，避免暗色下白底 + 白图标糊成一片
            surfaceColor = if (isDark) Color.White else Color.Black,
            surfaceAlpha = 0.06f,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp)
        )
    }
    }

    // 「管理员权限」面板（对齐官方 RoomAdminPermModal：三项授权 + 保存/保存中…）
    adminEditor?.let { member ->
        AdminPermissionDialog(
            member = member,
            saving = adminSaving,
            isDark = isDark,
            backdrop = cardBackdrop,
            onDismiss = {
                if (!adminSaving) adminEditor = null
            },
            onSave = { nextIsAdmin, nextCanKick, nextCanDeleteMessage ->
                adminSaving = true
                ZeroTalkClientManager.setRoomMemberAdmin(
                    roomId = conversation.id,
                    targetUserId = member.userId,
                    isAdmin = nextIsAdmin,
                    canKick = nextCanKick,
                    canDeleteMessage = nextCanDeleteMessage
                ) { success, message ->
                    adminSaving = false
                    if (success) {
                        adminEditor = null
                        error = null
                        notificationState.show(if (nextIsAdmin) "已更新管理员" else "已取消管理员")
                    } else {
                        // 面板保持打开便于重试，失败原因走顶部通知（面板会遮住页面内错误文案）
                        val reason = message?.takeIf { it.isNotBlank() } ?: "管理员设置失败"
                        error = reason
                        notificationState.show(reason)
                    }
                }
            }
        )
    }

    // 房管动作统一二次确认，避免误踢 / 误解除禁止加入
    pendingAction?.let { action ->
        val member = action.member
        val title = when (action) {
            is MemberAction.Remove -> "移出成员"
            is MemberAction.Unban -> "解除禁止加入"
        }
        val message = when (action) {
            is MemberAction.Remove -> "将「${member.name}」移出本群？移出后对方将无法继续在本群发言。"
            is MemberAction.Unban -> "允许「${member.name}」重新加入本群？"
        }
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { BasicText(title, style = AppleHigTypography.headline.copy(color = colors.label)) },
            text = { BasicText(message, style = AppleHigTypography.body.copy(color = colors.secondaryLabel)) },
            confirmButton = {
                if (action is MemberAction.Remove) {
                    Row {
                        TextButton(onClick = {
                            val current = action
                            pendingAction = null
                            performMemberAction(current, banJoin = false)
                        }) {
                            BasicText("仅移出", style = AppleHigTypography.body.copy(color = colors.destructive))
                        }
                        TextButton(onClick = {
                            val current = action
                            pendingAction = null
                            performMemberAction(current, banJoin = true)
                        }) {
                            BasicText("移出并禁止加入", style = AppleHigTypography.body.copy(color = colors.destructive))
                        }
                    }
                } else {
                    TextButton(onClick = {
                        val current = action
                        pendingAction = null
                        performMemberAction(current)
                    }) {
                        BasicText("确定", style = AppleHigTypography.body.copy(color = colors.tint))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    BasicText("取消", style = AppleHigTypography.body.copy(color = colors.secondaryLabel))
                }
            }
        )
    }
}

/**
 * 管理员权限面板
 *
 * 1:1 对齐官方 `RoomAdminPermModal`（ChatView）：
 * - 标题「管理员权限」，副标题「为「昵称」设置本房间管理员」；
 * - 三项授权：设为管理员（关闭后将同时收回下方两项权限）/ 允许踢人 / 允许删除消息；
 * - 关闭管理员时下方两项一并置 0，保存时提交 `is_admin && can_kick`、`is_admin && can_delete_message`。
 */
@Composable
private fun AdminPermissionDialog(
    member: RoomMemberUi,
    saving: Boolean,
    isDark: Boolean,
    backdrop: Backdrop?,
    onDismiss: () -> Unit,
    onSave: (isAdmin: Boolean, canKick: Boolean, canDeleteMessage: Boolean) -> Unit
) {
    val colors = AppleHigColors.colors(isDark)
    // 以成员当前授权回填（官方 watch([visible, member]) 的等价实现）
    var isAdmin by remember(member.userId, member.isAdmin) { mutableStateOf(member.isAdmin) }
    var canKick by remember(member.userId, member.canKick) { mutableStateOf(member.canKick) }
    var canDeleteMessage by remember(member.userId, member.canDeleteMessage) {
        mutableStateOf(member.canDeleteMessage)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            BasicText("管理员权限", style = AppleHigTypography.headline.copy(color = colors.label))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BasicText(
                    text = "为「${member.name}」设置本房间管理员",
                    style = AppleHigTypography.footnote.copy(color = colors.secondaryLabel)
                )
                Spacer(Modifier.height(10.dp))
                AdminPermissionRow(
                    label = "设为管理员",
                    hint = "关闭后将同时收回下方两项权限",
                    selected = isAdmin,
                    enabled = !saving,
                    backdrop = backdrop,
                    isDark = isDark,
                    onSelect = { next ->
                        isAdmin = next
                        if (!next) {
                            canKick = false
                            canDeleteMessage = false
                        }
                    }
                )
                AdminPermissionRow(
                    label = "允许踢人",
                    hint = "可将普通成员移出本房间，并可勾选禁止其再次用暗号加入",
                    selected = canKick,
                    enabled = isAdmin && !saving,
                    backdrop = backdrop,
                    isDark = isDark,
                    onSelect = { canKick = it }
                )
                AdminPermissionRow(
                    label = "允许删除消息",
                    hint = "可删除他人发送的消息",
                    selected = canDeleteMessage,
                    enabled = isAdmin && !saving,
                    backdrop = backdrop,
                    isDark = isDark,
                    onSelect = { canDeleteMessage = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    // 关闭管理员时两项权限一并置 0（服务端同样要求，这里前置收敛）
                    onSave(isAdmin, isAdmin && canKick, isAdmin && canDeleteMessage)
                }
            ) {
                BasicText(
                    text = if (saving) "保存中…" else "保存",
                    style = AppleHigTypography.body.copy(color = colors.tint)
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                BasicText("取消", style = AppleHigTypography.body.copy(color = colors.secondaryLabel))
            }
        }
    )
}

/**
 * 权限开关行；未授权时置灰且不可切换（对齐官方 `room-admin-row--disabled`）
 */
@Composable
private fun AdminPermissionRow(
    label: String,
    hint: String,
    selected: Boolean,
    enabled: Boolean,
    backdrop: Backdrop?,
    isDark: Boolean,
    onSelect: (Boolean) -> Unit
) {
    val colors = AppleHigColors.colors(isDark)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            BasicText(
                text = label,
                style = AppleHigTypography.subhead.copy(
                    color = if (enabled) colors.label else colors.tertiaryLabel,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = hint,
                style = AppleHigTypography.caption1.copy(color = colors.secondaryLabel)
            )
        }
        Box(modifier = Modifier.alpha(if (enabled) 1f else 0.45f)) {
            LiquidToggle(
                selected = { selected },
                onSelect = { if (enabled) onSelect(it) },
                backdrop = backdrop,
                isDark = isDark
            )
        }
    }
}

/**
 * 成员网格单元格（一排两个）
 *
 * 头像 + 昵称 + 「角色 · 性别」；**昵称过长时以「…」省略**（maxLines = 1）。
 * 右侧为房管操作图标：管理员权限 / 移出成员 / 解除禁止加入。
 */
@Composable
private fun MemberGridCell(
    member: RoomMemberUi,
    isDark: Boolean,
    showMemberActions: Boolean,
    showBanAction: Boolean,
    canSetAdmin: Boolean,
    canManageMembers: Boolean,
    viewerIsOwner: Boolean,
    onOpenProfile: () -> Unit,
    onEditAdmin: () -> Unit,
    onRemove: () -> Unit,
    onUnban: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppleHigColors.colors(isDark)
    val genderLabel = parseGenderText(member.gender)
    val roleLabel = when {
        member.isOwner -> "群主"
        member.isAdmin -> "管理员"
        else -> ""
    }
    // 只展示「角色 · 性别」，不再暴露 uid（32 位十六进制串）
    val metaParts = listOfNotNull(
        roleLabel.takeIf { it.isNotBlank() },
        genderLabel.takeIf { it.isNotBlank() }
    )

    Row(
        modifier = modifier
            .clickable(onClick = onOpenProfile)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(member.avatar, member.name, size = 36.dp)

        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            BasicText(
                text = member.name,
                style = AppleHigTypography.footnote.copy(
                    color = colors.label,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (metaParts.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                BasicText(
                    text = metaParts.joinToString(" · "),
                    style = AppleHigTypography.caption2.copy(color = colors.secondaryLabel),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showMemberActions && !member.isOwner && !member.isSelf) {
            // 官方：仅创建者（群主）可设置/取消管理员，且不针对自己与创建者
            if (canSetAdmin) {
                Icon(
                    Icons.Default.AdminPanelSettings,
                    "管理员权限",
                    tint = if (member.isAdmin) colors.tint else colors.secondaryLabel,
                    modifier = Modifier.size(20.dp).clickable { onEditAdmin() }
                )
            }
            // 官方提示文案：创建者可移出成员，管理员只能移出普通成员
            if (canManageMembers && (viewerIsOwner || !member.isAdmin)) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.PersonRemove,
                    "移出成员",
                    tint = colors.destructive,
                    modifier = Modifier.size(19.dp).clickable { onRemove() }
                )
            }
        } else if (showBanAction && canManageMembers) {
            Icon(
                Icons.Default.Block,
                "解除禁止加入",
                tint = colors.destructive,
                modifier = Modifier.size(19.dp).clickable { onUnban() }
            )
        }
    }
}
