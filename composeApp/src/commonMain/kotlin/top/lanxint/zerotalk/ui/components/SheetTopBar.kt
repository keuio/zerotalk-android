package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * Apple HIG Sheet 顶部导航动作定义 (Leading / Trailing Actions)
 * - 采用与主卡片右上角「偏好设置」同款 36.dp 圆形液态毛玻璃按钮 (CapsuleGlassButton)
 * - 采用无文字图标形态（Cancel: ×, Back: ←, Done: ✓），对齐极简设计
 */
sealed interface SheetAction {
    /**
     * 前缘取消：表单第一步使用，放弃未保存改动并关闭表单
     */
    data class Cancel(
        val onClick: () -> Unit
    ) : SheetAction

    /**
     * 前缘返回：多步骤表单第二步及后续步骤使用，返回上一步
     */
    data class Back(
        val onClick: () -> Unit
    ) : SheetAction

    /**
     * 前缘关闭：纯浏览/展示类 Sheet 使用
     */
    data class Close(
        val onClick: () -> Unit
    ) : SheetAction

    /**
     * 后缘完成：表单最后确认使用，支持 active/inactive 脏检查与禁用
     * @param enabled 是否活跃可点击（未检测到表单变更时置灰禁用）
     */
    data class Done(
        val enabled: Boolean = true,
        val onClick: () -> Unit
    ) : SheetAction

    /**
     * 无动作
     */
    data object None : SheetAction
}

/**
 * 「内容接管顶栏」声明：由 [AppleModalBottomSheet] 创建并通过 [LocalSheetTopBarClaim] 下发给内容。
 *
 * 用于顶栏内容必须跟随 Sheet 内部状态变化、容器无法代劳的场景（分享面板的两步流程标题、
 * 评论抽屉的评论数、「捞动态 / 捞取历史」里替换外层内容的子页面）：
 * 内容调用 [claim] 后，容器不再绘制自己的顶栏，改由内容在**完全相同的位置与间距**
 * 调用同一个 [SheetTopBar] 绘制 —— 视觉上等价于「容器顶栏直接切换成子页面顶栏」，
 * 不会再出现外层标题与子页面标题上下叠放；子页面关闭时调用 [release] 归还给容器。
 *
 * 写入相同值不会触发重组，因此在 `SideEffect` 中按当前状态重复声明是安全的。
 */
@Stable
class SheetTopBarClaim {
    var claimed by mutableStateOf(false)
        private set

    /** 内容接管顶栏：容器随即隐藏自己的顶栏 */
    fun claim() {
        claimed = true
    }

    /** 内容归还顶栏：容器恢复自己的标题与动作 */
    fun release() {
        claimed = false
    }
}

/** 当前 Sheet 的顶栏接管声明，非 Sheet 内容中为 null */
val LocalSheetTopBarClaim = compositionLocalOf<SheetTopBarClaim?> { null }

/**
 * 全站唯一的 Sheet 顶部栏（Top Bar）—— Apple HIG 模态弹窗头部的单一实现。
 *
 * 组成：居中主标题（可带后缘计数位） + 前缘圆形毛玻璃按钮（取消 × / 返回 ←） + 后缘完成按钮 ✓。
 * 规范：标题居中且使用 `AppleHigTypography.headline`；同一时刻绝不同时出现取消 / 完成 / 返回三个按钮。
 *
 * 两种调用方，渲染结果完全一致：
 * 1. [AppleModalBottomSheet] 容器内部（绝大多数 Sheet 走这条路径）；
 * 2. 顶栏内容随内部状态变化、容器无法代劳的 Sheet —— 例如分享面板的两步流程标题、
 *    评论抽屉需要跟随内部状态显示的评论数；以及「捞动态 / 捞取历史」里替换外层内容的子页面。
 *    这类内容除了调用本组件，还必须通过 [SheetTopBarClaim] 声明接管顶栏（`claim()` / `release()`），
 *    容器随即隐藏自己的顶栏，避免出现上下两层标题；子页面的前缘由关闭 × 变为返回 ←。
 *
 * @param title 居中主标题，为空则不显示
 * @param titleTrailing 主标题右侧的附加内容（如评论数），可为空
 * @param leadingAction 前缘动作
 * @param trailingAction 后缘动作
 */
@Composable
fun SheetTopBar(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    leadingAction: SheetAction = SheetAction.None,
    trailingAction: SheetAction = SheetAction.None
) {
    val higColors = AppleHigColors.colors(isDark)
    val titleColor = higColors.label
    val leadingIconColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        // 居中主标题 (Headline 17sp, 600) 与其后缘计数位
        if (title != null || titleTrailing != null) {
            Row(
                modifier = Modifier.padding(horizontal = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (title != null) {
                    BasicText(
                        text = title,
                        style = AppleHigTypography.headline.copy(
                            color = titleColor,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                if (titleTrailing != null) {
                    Spacer(Modifier.width(6.dp))
                    titleTrailing()
                }
            }
        }

        // 前缘动作 (Leading: Cancel / Back / Close - 36dp CapsuleGlassButton)
        when (leadingAction) {
            is SheetAction.Cancel -> {
                CapsuleGlassButton(
                    onClick = leadingAction.onClick,
                    isDark = isDark,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "取消",
                        tint = leadingIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is SheetAction.Close -> {
                CapsuleGlassButton(
                    onClick = leadingAction.onClick,
                    isDark = isDark,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = leadingIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            is SheetAction.Back -> {
                CapsuleGlassButton(
                    onClick = leadingAction.onClick,
                    isDark = isDark,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "返回",
                        tint = leadingIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            SheetAction.None, is SheetAction.Done -> {}
        }

        // 后缘动作 (Trailing: Done - 36dp CapsuleGlassButton)
        when (trailingAction) {
            is SheetAction.Done -> {
                val isEnabled = trailingAction.enabled
                val disabledColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFF8E8E93)
                val targetBgColor = if (isEnabled) higColors.systemBlue else disabledColor
                val animatedBgColor by animateColorAsState(
                    targetValue = targetBgColor,
                    animationSpec = tween(durationMillis = 250)
                )

                CapsuleGlassButton(
                    onClick = trailingAction.onClick,
                    enabled = isEnabled,
                    containerColor = animatedBgColor,
                    isDark = isDark,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "完成",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            SheetAction.None, is SheetAction.Cancel, is SheetAction.Back, is SheetAction.Close -> {}
        }
    }
}
