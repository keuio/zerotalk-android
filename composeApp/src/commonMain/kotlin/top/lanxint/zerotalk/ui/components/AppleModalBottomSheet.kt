package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.drawPlainBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.colorControls
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow

/**
 * 严格遵循 Apple Human Interface Guidelines (HIG) 的模态底部弹窗 (Modal Bottom Sheet)
 * - 左右悬浮不贴边 (16.dp 呼吸间距，最大宽度 520.dp 居中)
 * - 贴底设计，顶部 28.dp 经典大圆角
 * - 顶部居中药丸形拖拽指示条 (Drag Handle: 36.dp x 5.dp)
 * - 顶部栏统一由全站唯一的 [SheetTopBar] 绘制（居中标题 + 圆形毛玻璃动作按钮），
 *   需要跟随内部状态变化的顶栏（如分享两步流程、评论数、替换外层内容的子页面）由内容自行调用
 *   同一 [SheetTopBar] 并通过 [SheetTopBarClaim] 声明接管，容器随即让出自己的顶栏，不会出现双层标题
 * - 系统级暗色遮罩 (scrim)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    title: String? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    leadingAction: SheetAction = SheetAction.None,
    trailingAction: SheetAction = SheetAction.None,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetContainerColor = if (isDark) Color(0xFF1C1F28) else Color(0xFFFFFFFF)
    val handleColor = if (isDark) Color(0xFF3E4554) else Color(0xFFD1D5DB)
    val surfaceShape = RoundedCornerShape(32.dp)
    // 顶栏接管声明：内容声明接管时容器让出自己的顶栏（由内容在完全相同的位置绘制同一个 SheetTopBar）
    val topBarClaim = remember { SheetTopBarClaim() }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.30f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp)
            .widthIn(max = 520.dp)
    ) {
        val glassSurfaceModifier = if (backdrop != null) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { surfaceShape },
                effects = {
                    colorControls(
                        brightness = if (isDark) 0.05f else 0.15f,
                        saturation = 1.45f
                    )
                    blur(if (isDark) 16.dp.toPx() else 20.dp.toPx())
                    lens(
                        refractionHeight = 24.dp.toPx(),
                        refractionAmount = 48.dp.toPx(),
                        depthEffect = true
                    )
                },
                highlight = { Highlight.Plain },
                shadow = { Shadow(radius = 20.dp, color = Color.Black.copy(alpha = if (isDark) 0.35f else 0.18f)) },
                onDrawSurface = {
                    drawRect(
                        if (isDark) Color(0xFF161820).copy(alpha = 0.78f)
                        else Color(0xFFFFFFFF).copy(alpha = 0.75f)
                    )
                }
            )
        } else {
            Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = surfaceShape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.20f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.20f)
                )
                .background(sheetContainerColor, surfaceShape)
                .border(
                    width = 0.5.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                    shape = surfaceShape
                )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(glassSurfaceModifier)
        ) {
            // 顶部拖拽指示条 (Drag Handle: 遵循 SKILL.md GlassDragHandle 规范)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (backdrop != null) {
                    Box(
                        modifier = Modifier
                            .drawPlainBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(100.dp) },
                                effects = { blur(2.dp.toPx()) },
                                onDrawSurface = {
                                    drawRect(
                                        if (isDark) Color.White.copy(alpha = 0.35f)
                                        else Color.Black.copy(alpha = 0.20f)
                                    )
                                }
                            )
                            .size(width = 36.dp, height = 5.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(handleColor)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val hasTopBar = !topBarClaim.claimed && (
                    title != null || titleTrailing != null ||
                        leadingAction !is SheetAction.None || trailingAction !is SheetAction.None
                    )
                if (hasTopBar) {
                    SheetTopBar(
                        isDark = isDark,
                        title = title,
                        titleTrailing = titleTrailing,
                        leadingAction = leadingAction,
                        trailingAction = trailingAction
                    )
                    Spacer(Modifier.height(18.dp))
                }
                CompositionLocalProvider(LocalSheetTopBarClaim provides topBarClaim) {
                    content()
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        // 底部外部真正透明的浮空隙：键盘收起时按导航栏真实物理高度预留（不足 10dp 则保底 10dp）；
        // 键盘弹起时弹窗窗口已随输入法 resize、导航栏被键盘遮住，只留 10dp 呼吸缝，避免叠出多余空隙
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val bottomMargin = if (imeBottom > 0.dp) 10.dp else maxOf(navBottom, 10.dp)
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomMargin)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        )
    }
}
