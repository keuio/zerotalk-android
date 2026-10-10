package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 全局通用通知状态控制器
 */
@Stable
class NotificationState(private val coroutineScope: CoroutineScope) {
    var isVisible by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    var message by mutableStateOf("")
        private set
    var subtitle by mutableStateOf("")
        private set
    var icon by mutableStateOf<ImageVector?>(null)
        private set

    private var dismissJob: Job? = null

    /**
     * 在左上角显示全局通知
     *
     * @param message 通知文字内容
     * @param icon 可选图标（默认 CheckCircle 成功对勾）
     * @param durationMs 自动收起时长，默认 2000 毫秒
     */
    fun show(
        message: String,
        icon: ImageVector? = Icons.Default.CheckCircle,
        durationMs: Long = 2000L
    ) {
        this.title = ""
        this.subtitle = ""
        this.message = message
        this.icon = icon
        this.isVisible = true

        scheduleDismiss(durationMs)
    }

    fun showMessageNotification(newCount: Int, totalUnread: Int) {
        title = "新消息"
        message = if (newCount == 1) {
            "收到一条新消息 · 未读 $totalUnread"
        } else {
            "收到 $newCount 条新消息 · 未读 $totalUnread"
        }
        icon = Icons.Default.Forum
        isVisible = true
        scheduleDismiss(4500L)
    }

    fun showIncomingMessage(
        roomName: String,
        senderName: String = "",
        content: String,
        isGroup: Boolean = false
    ) {
        title = roomName
        // 不再使用「新消息」副标题：三排过高，改为两排（标题 + 内容）
        subtitle = ""
        // 群聊 / 大厅带上发送人名字，形如「张三：内容」；
        // 私聊的 title 已经是对方名字，与 senderName 相同，避免重复
        val name = senderName.trim()
        message = if (name.isNotBlank() && name != roomName.trim()) "$name：$content" else content
        icon = Icons.Default.Forum
        isVisible = true
        scheduleDismiss(4500L)
    }

    private fun scheduleDismiss(durationMs: Long) {
        dismissJob?.cancel()
        dismissJob = coroutineScope.launch {
            delay(durationMs)
            isVisible = false
        }
    }

    /**
     * 立即收起当前通知
     */
    fun dismiss() {
        dismissJob?.cancel()
        isVisible = false
    }
}

/**
 * 创建并记住 NotificationState
 */
@Composable
fun rememberNotificationState(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): NotificationState {
    return remember(coroutineScope) {
        NotificationState(coroutineScope)
    }
}

/**
 * 全局通知控制器 CompositionLocal，子组件可通过 `LocalNotificationState.current.show(...)` 直接调用
 */
val LocalNotificationState = compositionLocalOf<NotificationState> {
    error("NotificationState not provided. Wrap your layout in NotificationProvider.")
}

/**
 * 全局通知提供者容器
 */
@Composable
fun NotificationProvider(
    state: NotificationState,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalNotificationState provides state) {
        content()
    }
}

/**
 * 全局通知横幅：从屏幕最顶部向下弹出（导航栏上方起始），绝对水平居中。
 *
 * 动效规范：
 * 1. 位置：Alignment.TopCenter，贴屏幕最顶部、绝对水平居中；
 * 2. 弹出方向：进场用 slideInVertically(-it) 从「顶边之上」完整下滑，
 *    配合外层 clipToBounds()，起始状态被顶边裁掉，因此观感是从最顶部
 *    被推/拉出来，而不是原地淡入；
 * 3. 缩放：高阻尼 spring 从 0.85 放大到 1.0，短暂轻微过冲后干净收敛，
 *    锚点设在顶部中心 (0.5f, 0f)，顶边保持贴合；
 * 4. 淡入只做很短的辅助，避免「直接出现」的观感。
 */
@Composable
fun ZeroTalkNotificationHost(
    state: NotificationState,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // 裁切边压在最顶部：横幅下滑前被顶边裁掉，形成「从顶部出来」的观感
            .clipToBounds(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
        AnimatedVisibility(
            visible = state.isVisible,
            enter = slideInVertically(
                // 从顶部之上完整高度滑入
                initialOffsetY = { -it },
                animationSpec = spring(
                    // 降低阻尼（让弹性更明显，动画过程拉长），降低刚度（让整体速度变缓，出现时间拉长）
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessLow
                )
            ) + scaleIn(
                initialScale = 0.85f,
                // 顶部中心：顶边贴合并向下弹出
                transformOrigin = TransformOrigin(0.5f, 0f),
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 180)
            ) + scaleOut(
                targetScale = 0.94f,
                transformOrigin = TransformOrigin(0.5f, 0f),
                animationSpec = tween(durationMillis = 180)
            ) + fadeOut(animationSpec = tween(durationMillis = 150))
        ) {
            val surface = if (isDark) Color(0xFF1E222B).copy(alpha = 0.96f)
            else Color(0xFFFFFFFF).copy(alpha = 0.97f)
            val borderColor = if (isDark) Color(0xFF333B4A) else Color(0xFFE2E8F0)
            val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            val secondaryColor = if (isDark) Color(0xFFB1BAC7) else Color(0xFF64748B)
            val iconTint = if (isDark) Color(0xFF60A5FA) else Color(0xFF007AFF)
            val shape = RoundedCornerShape(18.dp)

            Row(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 16.dp else 10.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.22f),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .clip(shape)
                    .background(surface)
                    .border(1.dp, borderColor, shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { state.dismiss() }
                    )
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(if (state.title.isBlank()) 18.dp else 24.dp)
                    )
                }
                if (state.title.isBlank()) {
                    BasicText(
                        text = state.message,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        BasicText(
                            text = state.title,
                            style = TextStyle(
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.subtitle.isNotBlank()) {
                            BasicText(
                                text = state.subtitle,
                                style = TextStyle(
                                    color = secondaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicText(
                            text = state.message,
                            style = TextStyle(
                                color = secondaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        }
    }
}
