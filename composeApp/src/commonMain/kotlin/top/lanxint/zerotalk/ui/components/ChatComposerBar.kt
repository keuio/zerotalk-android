package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.colorControls
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow
import top.lanxint.zerotalk.ui.theme.AppleHigColors

/**
 * 聊天底部输入栏的公共外壳（Composer）。
 *
 * 对齐官网「一个共享 composer 组件 + 标志位控制显示项」的架构：
 * 外壳（悬浮输入栏 + [+] 按钮 + 抽屉开合动画 + 输入框本体 + 引用预览条）完全共用，
 * 抽屉里有哪些扩展项由调用方通过 actions / drawerContent 决定，
 * 语音录制、图片选择等重型状态机留在调用方，只把触发按钮（抽屉项 / 麦克风开关）接进来。
 *
 * 设计要点：
 * - 抽屉开合状态 drawerExpanded 由调用方持有（列表滚动、返回键、列表底部 padding 都需要读它），
 *   组件内部只负责动画与 [+] 的 45 度旋转；
 * - 输入文本 inputText 由调用方持有，发送逻辑由 onSend 回调完成（组件不清空文本，保证与调用方状态一致）；
 * - 输入栏顶部在 Root 中的像素坐标通过 onBottomBarTopChanged 回传，
 *   调用方据此计算消息列表的动态底部避让（私聊的 bottomBarTopYPx / dynamicBottomPadding）。
 *
 * @param drawerExpanded 扩展抽屉是否展开（hoisted state）
 * @param onDrawerExpandedChange 抽屉开合请求（[+] 按钮、点击空白、下滑、抽屉项点击、聚焦输入框都会触发）
 * @param inputText 输入框当前文本
 * @param onInputTextChange 输入框文本变更
 * @param onSend 点击发送箭头时的回调（调用方负责拼装引用 / @提及并清空输入状态）
 * @param actions 抽屉扩展项（默认按 actionsPerRow 个一行排布，不足补空位）
 * @param isDark 暗色模式
 * @param backdrop 液态玻璃折射采样源（可为 null，回退普通阴影表面）
 * @param controlContentColor 输入栏前景色（随背景明暗变化）
 * @param surfaceColor 输入栏玻璃表面基色
 * @param surfaceAlpha 输入栏玻璃表面透明度
 * @param modifier 最外层修饰符
 * @param quote 引用预览条内容，null 表示不显示
 * @param onCancelQuote 取消引用
 * @param placeholder 输入框占位文案
 * @param isVoiceMode 是否处于「按住说话」语音模式
 * @param onToggleVoiceMode 麦克风 / 键盘切换
 * @param voiceContent 语音模式下的输入区内容（RowScope，调用方自行 weight(1f)）；
 *                     为 null 时语音模式下输入区为空
 * @param inputFocusRequester 输入框焦点请求器（调用方可外部持有以便 @提及后主动聚焦）
 * @param contentPadding 外壳内边距（默认左右 16dp，抽屉展开时底部 4dp / 收起时 12dp）
 * @param scrimEnabled 抽屉展开时是否显示点击收起的全屏透明拦截层
 * @param drawerHeight 抽屉面板高度
 * @param actionsPerRow 抽屉每行扩展项个数
 * @param drawerContent 自定义抽屉内容；为 null 时使用 actions 的默认宫格
 * @param tailModifier 追加在外壳最内层的修饰符（调用方可注入随列表滚动重绘的 drawWithContent）
 * @param onBottomBarTopChanged 外壳顶部在 Root 中的像素坐标变化回调
 */
@Composable
fun ChatComposerBar(
    drawerExpanded: Boolean,
    onDrawerExpandedChange: (Boolean) -> Unit,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    actions: List<ChatComposerAction>,
    isDark: Boolean,
    backdrop: Backdrop?,
    controlContentColor: Color,
    surfaceColor: Color,
    surfaceAlpha: Float,
    modifier: Modifier = Modifier,
    quote: ChatComposerQuote? = null,
    onCancelQuote: () -> Unit = {},
    placeholder: String = "发信息",
    isVoiceMode: Boolean = false,
    onToggleVoiceMode: () -> Unit = {},
    voiceContent: (@Composable RowScope.() -> Unit)? = null,
    inputFocusRequester: FocusRequester = remember { FocusRequester() },
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        bottom = if (drawerExpanded) 4.dp else 12.dp
    ),
    scrimEnabled: Boolean = true,
    drawerHeight: Dp = 280.dp,
    actionsPerRow: Int = 4,
    drawerContent: (@Composable ColumnScope.() -> Unit)? = null,
    tailModifier: Modifier = Modifier,
    onBottomBarTopChanged: (Float) -> Unit = {},
    stickerPanelExpanded: Boolean = false,
    onToggleStickerPanel: () -> Unit = {},
    onStickerPick: ((Long, String) -> Unit)? = null,
    stickerPanelContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // 加号按钮 45 度旋转为关闭 × 动画
    val plusRotation by animateFloatAsState(
        targetValue = if (drawerExpanded) 45f else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.72f),
        label = "PlusRotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // ---- 抽屉展开时的全屏透明拦截层（点击聊天背景区域平滑收起抽屉，同时不阻挡视觉）----
        if (scrimEnabled && drawerExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onDrawerExpandedChange(false) }
                    )
            )
        }

        // ---- 底部输入控制栏（2 个 LiquidButton：[+] 与 输入框，完全悬浮透空）----
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(contentPadding)
                .onGloballyPositioned { coordinates ->
                    onBottomBarTopChanged(coordinates.boundsInRoot().top)
                }
                .then(tailModifier)
        ) {
            // 引用回复预览条 (整体高度 36dp，水平内边距 12dp，背景 secondarySystemBackground)
            AnimatedVisibility(
                visible = quote != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                quote?.let { qm ->
                    ChatComposerQuoteBar(
                        quote = qm,
                        isDark = isDark,
                        onCancel = onCancelQuote
                    )
                }
            }

            // 输入栏主体
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 左侧独立 [+] LiquidButton 按钮 (36dp，展开时顺滑旋转 45 度为 ×)
                LiquidButton(
                    onClick = {
                        if (drawerExpanded) {
                            onDrawerExpandedChange(false)
                        } else {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onDrawerExpandedChange(true)
                        }
                    },
                    modifier = Modifier.size(36.dp),
                    backdrop = backdrop,
                    isDark = isDark,
                    surfaceColor = surfaceColor,
                    surfaceAlpha = surfaceAlpha,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (drawerExpanded) "收起扩展" else "展开扩展",
                        tint = controlContentColor,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = plusRotation }
                    )
                }

                // 中央消息输入框 (基于 LiquidButton 架构，高度 36dp)
                LiquidButton(
                    onClick = {
                        if (drawerExpanded) onDrawerExpandedChange(false)
                        if (!isVoiceMode) {
                            inputFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    backdrop = backdrop,
                    isDark = isDark,
                    isInteractive = false,
                    surfaceColor = surfaceColor,
                    surfaceAlpha = surfaceAlpha,
                    contentPadding = PaddingValues(start = 12.dp, end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isVoiceMode) {
                            voiceContent?.invoke(this)
                        } else {
                            // 键盘文本输入模式
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (drawerExpanded) onDrawerExpandedChange(false)
                                        inputFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = inputText,
                                    onValueChange = onInputTextChange,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = controlContentColor,
                                        fontSize = 17.sp,
                                        lineHeight = 21.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(inputFocusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused && drawerExpanded) {
                                                onDrawerExpandedChange(false)
                                            }
                                        },
                                    decorationBox = { innerTextField ->
                                        if (inputText.isEmpty()) {
                                            BasicText(
                                                text = placeholder,
                                                style = TextStyle(
                                                    color = controlContentColor.copy(alpha = 0.45f),
                                                    fontSize = 17.sp
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        // 右侧动态图标：麦克风图标 <-> 蓝色发送箭头（带弹性缩放与淡入淡出转换动画）
                        // 表情包按钮固定在最右，有文字时让位给发送按钮（与官方 composer 一致）
                        val hasText = inputText.trim().isNotEmpty()
                        if (!hasText && onStickerPick != null) {
                            StickerToggleButton(
                                expanded = stickerPanelExpanded,
                                tint = controlContentColor,
                                onClick = onToggleStickerPanel
                            )
                        }
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = hasText,
                                transitionSpec = {
                                    if (targetState) {
                                        // 转换为发送按钮：弹性放大淡入，麦克风缩小淡出
                                        (fadeIn(animationSpec = tween(160)) +
                                                scaleIn(initialScale = 0.35f, animationSpec = spring(dampingRatio = 0.62f, stiffness = 450f)))
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(120)) +
                                                        scaleOut(targetScale = 0.4f, animationSpec = tween(120))
                                            )
                                    } else {
                                        // 转换为语音/键盘：弹性放大淡入，发送按钮缩小淡出
                                        (fadeIn(animationSpec = tween(160)) +
                                                scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = 0.68f, stiffness = 450f)))
                                            .togetherWith(
                                                fadeOut(animationSpec = tween(120)) +
                                                        scaleOut(targetScale = 0.35f, animationSpec = tween(120))
                                            )
                                    }
                                },
                                contentAlignment = Alignment.Center,
                                label = "SendVoiceTransition"
                            ) { targetHasText ->
                                if (targetHasText) {
                                    // 纯蓝圆形发送箭头
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF007AFF))
                                            .clickable { onSend() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else {
                                    // 麦克风图标，点击切换语音录制 / 键盘
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable { onToggleVoiceMode() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedContent(
                                            targetState = isVoiceMode,
                                            transitionSpec = {
                                                (fadeIn(animationSpec = tween(150)) +
                                                        scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 450f)))
                                                    .togetherWith(
                                                        fadeOut(animationSpec = tween(100)) +
                                                                scaleOut(targetScale = 0.6f, animationSpec = tween(100))
                                                    )
                                            },
                                            contentAlignment = Alignment.Center,
                                            label = "VoiceKeyboardTransition"
                                        ) { targetVoiceMode ->
                                            if (targetVoiceMode) {
                                                Icon(
                                                    imageVector = Icons.Default.Keyboard,
                                                    contentDescription = "Keyboard",
                                                    tint = controlContentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Microphone",
                                                    tint = controlContentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 表情包面板（独立槽位，与 [+] 抽屉互斥，由调用方通过 stickerPanelContent 注入）
            AnimatedVisibility(
                visible = stickerPanelExpanded && stickerPanelContent != null,
                enter = expandVertically(
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.78f),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.78f),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(120))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    stickerPanelContent?.invoke(this)
                }
            }

                        // ---- 4. 底部 [+] 扩展抽屉面板（展开时自然将上方输入栏平滑顶起）----
            AnimatedVisibility(
                visible = drawerExpanded,
                enter = expandVertically(
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.78f),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(150)),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = 450f, dampingRatio = 0.78f),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(120))
            ) {
                ChatComposerDrawer(
                    actions = actions,
                    isDark = isDark,
                    backdrop = backdrop,
                    drawerHeight = drawerHeight,
                    actionsPerRow = actionsPerRow,
                    onDismiss = { onDrawerExpandedChange(false) },
                    content = drawerContent
                )
            }
        }
    }
}

/**
 * 引用回复预览条（36dp 高、左竖线、单行截断、取消按钮）
 *
 * 私聊 / 群聊 / 大厅共用同一外观。
 */
@Composable
fun ChatComposerQuoteBar(
    quote: ChatComposerQuote,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {}
) {
    val previewBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val barLineColor = if (quote.isMine) Color(0xFF007AFF) else Color(0xFF8E8E93)
    val labelColor = AppleHigColors.colors(isDark).label

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(previewBg)
            .border(
                0.5.dp,
                if (isDark) Color(0x2EFFFFFF) else Color(0x18000000),
                RoundedCornerShape(8.dp)
            )
            .padding(start = 12.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧竖线指示条 (宽度 2dp，原消息气泡同色)
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(barLineColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 预览文字 (15sp Subhead，单行截断)
        BasicText(
            text = quote.text,
            style = TextStyle(
                color = labelColor,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        // 关闭叉号按钮：视觉 24dp，热区 44dp
        Box(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x14000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = quote.cancelContentDescription,
                    modifier = Modifier.size(13.dp),
                    tint = if (isDark) Color(0xCCFFFFFF) else Color(0x99000000)
                )
            }
        }
    }
}

/**
 * 扩展抽屉里的一个动作项（图标 + 文案），对齐官方 composer-plus-item 风格。
 *
 * @param id 稳定标识
 * @param label 文案
 * @param icon 图标
 * @param color 图标 / 圆底的主色
 * @param textColor 文案颜色，null 时按明暗自动取色
 * @param closeDrawerOnClick 点击时是否先收起抽屉再执行 onClick
 */
data class ChatComposerAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val textColor: Color? = null,
    val closeDrawerOnClick: Boolean = true,
    val onClick: () -> Unit
)

/**
 * 引用预览条的数据模型。
 *
 * @param text 单行预览文案
 * @param isMine 被引用消息是否为我方发出（决定左侧竖线颜色）
 * @param cancelContentDescription 取消按钮的无障碍描述
 */
data class ChatComposerQuote(
    val text: String,
    val isMine: Boolean,
    val cancelContentDescription: String = "取消引用"
)

/**
 * 扩展抽屉面板：毛玻璃表面 + 顶部拖拽把手 + 宫格动作项（或自定义内容）。
 */
@Composable
private fun ChatComposerDrawer(
    actions: List<ChatComposerAction>,
    isDark: Boolean,
    backdrop: Backdrop?,
    drawerHeight: Dp,
    actionsPerRow: Int,
    onDismiss: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(10.dp)) // 输入框与扩展面板之间的 10dp 呼吸悬浮空隙

        val drawerSurfaceShape = RoundedCornerShape(32.dp)
        val drawerModifier = if (backdrop != null) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { drawerSurfaceShape },
                effects = {
                    colorControls(
                        brightness = if (isDark) 0.06f else 0.14f,
                        saturation = 1.45f
                    )
                    blur(if (isDark) 16.dp.toPx() else 20.dp.toPx())
                    lens(
                        refractionHeight = 22.dp.toPx(),
                        refractionAmount = 40.dp.toPx(),
                        depthEffect = true
                    )
                },
                highlight = { Highlight.Plain },
                shadow = { Shadow(radius = 18.dp, color = Color.Black.copy(alpha = if (isDark) 0.35f else 0.16f)) },
                onDrawSurface = {
                    drawRect(
                        if (isDark) Color(0xFF161820).copy(alpha = 0.76f)
                        else Color(0xFFFFFFFF).copy(alpha = 0.75f)
                    )
                }
            )
        } else {
            Modifier
                .shadow(
                    elevation = 18.dp,
                    shape = drawerSurfaceShape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.16f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.16f)
                )
                .background(if (isDark) Color(0xFF1C1F28) else Color(0xFFFFFFFF), drawerSurfaceShape)
                .border(
                    width = 0.5.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                    shape = drawerSurfaceShape
                )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(drawerHeight)
                .then(drawerModifier)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 20f) {
                            onDismiss()
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部拖拽把手指示条 (36dp x 5dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.35f)
                            else Color.Black.copy(alpha = 0.20f)
                        )
                )
            }

            if (content != null) {
                content()
            } else {
                // 功能图标按行等宽排布，每行 actionsPerRow 个（不足补空位）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    actions.chunked(actionsPerRow.coerceAtLeast(1)).forEach { rowActions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowActions.forEach { action ->
                                ChatComposerActionItem(
                                    icon = action.icon,
                                    title = action.label,
                                    color = action.color,
                                    textColor = action.textColor
                                        ?: if (isDark) Color.White else Color(0xFF1C1C1E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (action.closeDrawerOnClick) onDismiss()
                                    action.onClick()
                                }
                            }
                            repeat((actionsPerRow - rowActions.size).coerceAtLeast(0)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * 扩展 Sheet 宫格图标项（图标 + 文案）
 */
@Composable
fun ChatComposerActionItem(
    icon: ImageVector,
    title: String,
    color: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f))
                .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        BasicText(
            text = title,
            style = TextStyle(
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/** 输入栏右侧的表情包开关（与官方 composer 的表情包按钮同语义） */
@Composable
private fun StickerToggleButton(
    expanded: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (expanded) Color(0xFF007AFF).copy(alpha = 0.16f) else Color.Transparent
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEmotions,
            contentDescription = if (expanded) "收起表情包" else "打开表情包",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

