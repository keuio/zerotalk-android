package top.lanxint.zerotalk.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import top.lanxint.zerotalk.data.model.UserProfileLayer
import top.lanxint.zerotalk.data.model.UserProfileLayerHost
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager

/**
 * 资料页层级栈中，某个渲染点当前该画的那一层
 *
 * @param visible 本宿主当前是否有层（驱动 `AnimatedVisibility` 的可见性）
 * @param layer 供渲染的层：退场动画期间沿用最后一次的值，避免滑出动画没有内容可画
 * @param isTop 本层是否正好是栈顶 —— 只有栈顶那一层才允许拦截返回
 */
data class UserProfileLayerSlot(
    val visible: Boolean,
    val layer: UserProfileLayer?,
    val isTop: Boolean
)

/**
 * 认领本渲染点要画的资料页层
 *
 * 全站资料页层级只有一个事实来源（`ZeroTalkClientManager.userProfileLayers`），
 * 各渲染点（APP 覆盖层 / 大厅面板 / 资料页内视差层 / 私聊群成员面板）只从栈里认领
 * `host`（必要时再按 `owner` 限定递归嵌套中的具体页面实例）属于自己的那一层，
 * 因此同一个资料页不可能被两个渲染点同时画出来。
 *
 * @param host 本渲染点的宿主类型
 * @param owner 递归嵌套时用于区分实例的不透明标识（见 `UserProfileLayer.owner`）
 */
@Composable
fun rememberUserProfileLayerSlot(
    host: UserProfileLayerHost,
    owner: Any? = null
): UserProfileLayerSlot {
    val layers by ZeroTalkClientManager.userProfileLayers.collectAsState()
    val current = layers.lastOrNull { it.host == host && (owner == null || it.owner === owner) }

    // 记录最后一次非空层：出栈后 AnimatedVisibility 仍在播退场动画，需要它继续渲染内容
    var lastRendered by remember { mutableStateOf<UserProfileLayer?>(null) }
    if (current != null && lastRendered !== current) {
        lastRendered = current
    }

    val top = layers.lastOrNull()
    return UserProfileLayerSlot(
        visible = current != null,
        layer = current ?: lastRendered,
        isTop = top != null && top.host == host && (owner == null || top.owner === owner)
    )
}

/** 当前是否存在任何资料页层（被覆盖的页面据此让出返回事件） */
@Composable
fun rememberHasUserProfileLayers(): Boolean {
    val layers by ZeroTalkClientManager.userProfileLayers.collectAsState()
    return layers.isNotEmpty()
}
