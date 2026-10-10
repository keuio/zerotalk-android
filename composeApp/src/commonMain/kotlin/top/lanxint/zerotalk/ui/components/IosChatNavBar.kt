package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.Backdrop

/**
 * iOS 26 原生导航栏骨架（私聊 / 群聊 / 公共大厅共用）
 *
 * 从 `PrivateChatScreen.TopNavigationBar` 的视觉规范抽出，保证多端一致：
 * - 安全区：[statusBarsPadding]，栏体本身**不**铺底色，内容从状态栏下方开始；
 * - 左右按钮位置：返回键贴 `TopStart`、尾部动作贴 `TopEnd`，均距顶部 8dp；
 * - 标题：`TopCenter` 居中，包在 LiquidButton 液态毛玻璃胶囊里（同私聊联系人胶囊）；
 * - 栏体内边距：水平 21dp、垂直 6dp，与私聊完全一致。
 *
 * @param onBack 返回回调
 * @param title 居中标题主文案
 * @param isDark 当前是否暗色
 * @param subtitle 标题副文案（如大厅「在线畅聊」）；为空时标题单行渲染
 * @param backdrop 液态玻璃采样源；为 null 时 LiquidButton 回退到纯色 canvas backdrop
 * @param isWhiteBackground 浅色底时控件用深色（否则恒用白色）
 * @param surfaceColor / @param surfaceAlpha 玻璃表面烟熏遮罩（与私聊同为 6%）
 * @param backLabel 返回键文案（如大厅「首页」）；为 null 时退化为 [IosLiquidBackButton] 的
 *   圆形 / 未读角标形态（私聊用法）
 * @param unreadCount 仅在 [backLabel] 为 null 时生效：> 0 展示角标胶囊
 * @param showChevron 标题胶囊内是否追加「›」箭头（私聊 / 暗号群聊联系人胶囊同款：
 *   21sp + Bold + 70% 不透明度）；默认 false 保持原行为，仅公共大厅传 true
 * @param onTitleClick 标题胶囊点击回调；为 null 时胶囊不可交互（不注册按压形变）
 * @param titleAvatar 标题胶囊上方的圆形头像槽位（私聊 / 群聊联系人胶囊形态：60dp 头像压在胶囊
 *   上层）；为 null 时保持原「单行标题胶囊」形态。头像本身会复用 [onTitleClick] 作为点击回调
 * @param trailing 右侧尾部动作（如私聊搜索按钮）；为 null 时右侧留空，标题仍严格居中
 */
@Composable
fun IosChatNavBar(
    onBack: () -> Unit,
    title: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backdrop: Backdrop? = null,
    isWhiteBackground: Boolean = false,
    surfaceColor: Color = Color.Unspecified,
    surfaceAlpha: Float? = null,
    backLabel: String? = null,
    unreadCount: Int? = null,
    showChevron: Boolean = false,
    onTitleClick: (() -> Unit)? = null,
    titleAvatar: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    // 浅色底用深色控件、深色底用白色控件（与私聊 TopNavigationBar 同一规则）
    val contentColor = if (isWhiteBackground) Color(0xFF1C1C1E) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 21.dp, vertical = 6.dp)
            .then(modifier)
    ) {
        // ---- 左：液态毛玻璃返回键 ----
        if (backLabel != null) {
            // 胶囊形态：< 首页（高度对齐 IosLiquidBackButton 的 36.9dp）
            LiquidButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp)
                    .height(36.9.dp),
                backdrop = backdrop,
                isDark = isDark,
                surfaceColor = surfaceColor,
                surfaceAlpha = surfaceAlpha,
                contentPadding = PaddingValues(start = 9.dp, end = 13.dp)
            ) {
                IosRoundedChevronBackIcon(
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                    strokeWidthDp = 2.16.dp
                )
                BasicText(
                    text = backLabel,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            IosLiquidBackButton(
                onClick = onBack,
                unreadCount = unreadCount,
                backdrop = backdrop,
                isDark = isDark,
                isWhiteBackground = isWhiteBackground,
                surfaceColor = surfaceColor,
                surfaceAlpha = surfaceAlpha,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp)
            )
        }

        // ---- 中：居中标题胶囊（与私聊联系人胶囊同一材质 / 位置）----
        if (titleAvatar == null) {
            // 无头像形态：胶囊距顶部 8dp（原有布局，标题单行 / 双行）
            LiquidButton(
                onClick = onTitleClick ?: {},
                isInteractive = onTitleClick != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .height(41.dp),
                backdrop = backdrop,
                isDark = isDark,
                surfaceColor = surfaceColor,
                surfaceAlpha = surfaceAlpha,
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                NavBarTitle(
                    title = title,
                    subtitle = subtitle,
                    contentColor = contentColor,
                    showChevron = showChevron
                )
            }
        } else {
            // 有头像形态（对齐私聊 / 群聊 TopNavigationBar）：
            // 下层胶囊下移到 top = 50dp 给 60dp 头像让位，上层头像压在胶囊顶部并复用同一点击回调。
            Box(
                modifier = Modifier.align(Alignment.TopCenter),
                contentAlignment = Alignment.TopCenter
            ) {
                LiquidButton(
                    onClick = onTitleClick ?: {},
                    isInteractive = onTitleClick != null,
                    modifier = Modifier
                        .padding(top = 50.dp)
                        .height(41.dp),
                    backdrop = backdrop,
                    isDark = isDark,
                    surfaceColor = surfaceColor,
                    surfaceAlpha = surfaceAlpha,
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    NavBarTitle(
                        title = title,
                        subtitle = subtitle,
                        contentColor = contentColor,
                        showChevron = showChevron
                    )
                }

                Box(
                    modifier = Modifier.then(
                        if (onTitleClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTitleClick
                            )
                        } else {
                            Modifier
                        }
                    )
                ) {
                    titleAvatar()
                }
            }
        }

        // ---- 右：尾部动作（搜索等）；为 null 时留空，不影响标题居中 ----
        if (trailing != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp)
            ) {
                trailing()
            }
        }
    }
}

/**
 * 标题胶囊内容：副标题为空时单行，否则「主标题 + 弱化副标题」两行。
 *
 * 单行规格与私聊 / 暗号群聊 PrivateChatScreen.TopNavigationBar 的联系人胶囊逐字对齐：
 * 标题 21sp SemiBold；[showChevron] 为 true 时追加 21sp Bold + 70% 不透明度的「›」
 * （LiquidButton 内容行自带 6dp 间距，箭头与私聊一致）。
 */
@Composable
private fun NavBarTitle(
    title: String,
    subtitle: String?,
    contentColor: Color,
    showChevron: Boolean = false
) {
    if (subtitle.isNullOrBlank()) {
        BasicText(
            text = title,
            style = TextStyle(
                color = contentColor,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BasicText(
                text = title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            BasicText(
                text = subtitle,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            )
        }
    }
    if (showChevron) {
        // 私聊 / 暗号群聊同款「›」：21sp + Bold + 70% 不透明度
        BasicText(
            text = "›",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
