package top.lanxint.zerotalk.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.lanxint.zerotalk.data.settings.UiPreferencesStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
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
import kotlinx.coroutines.delay
import top.lanxint.zerotalk.ui.theme.AppleHigColors

/**
 * 全局软键盘高度追踪器（进程级单例内存记忆）。
 * 兜底 280dp。实时捕获系统软键盘真实完整高度的最高峰值。
 * 表情面板与 [+] 抽屉面板高度对齐此记忆高度，保证键盘 ↔ 面板无缝等高切换，输入栏零高度弹跳。
 */
object KeyboardHeightTracker {
    var lastKeyboardHeightDp by mutableStateOf(260.dp)
        private set

    /** 是否已经成功记录过系统真实软键盘的有效峰值 */
    var hasRecordedPeak: Boolean = false
        private set

    /**
     * 更新键盘最高峰值。
     * - 低于 200dp 的异常/导航栏高度不予记录。
     * - 单调递增最高峰值锁定（Monotonic Peak Holding）：
     *   1. 若尚未记录真实峰值：记录首次有效高度并标记 hasRecordedPeak = true；
     *   2. 若已记录真实峰值：严格只增不减，仅当检测到更高高度（heightDp > lastKeyboardHeightDp）时更新，
     *      防止在键盘收起、过渡帧过程中被较小数值冲刷覆盖。
     */
    fun updatePeak(heightDp: Dp) {
        if (heightDp < 200.dp) return
        if (!hasRecordedPeak) {
            lastKeyboardHeightDp = heightDp
            hasRecordedPeak = true
        } else if (heightDp > lastKeyboardHeightDp) {
            lastKeyboardHeightDp = heightDp
        }
    }

    /**
     * 键盘动画结束且处于完全静止展开态时的确切高度记录。
     * 无论初始默认值是多少，静止态实测数据拥有最高优先级，直接校准为屏幕真实键盘高度，
     * 消除「初始默认高度或过渡动画过度膨胀导致面板比真实键盘高出一截」的冲突。
     */
    fun recordRestingHeight(heightDp: Dp) {
        if (heightDp < 200.dp) return
        lastKeyboardHeightDp = heightDp
        hasRecordedPeak = true
    }

    fun updateIfValid(heightDp: Dp) = updatePeak(heightDp)

    fun resetForTest(heightDp: Dp = 280.dp) {
        lastKeyboardHeightDp = heightDp
        hasRecordedPeak = false
    }
}

/**
 * 表情面板搜索框聚焦状态共享（用于在搜索时将输入栏和表情搜索框整体置于软键盘之上）。
 */
val LocalStickerSearchFocused = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(false) }

/**
 * 表情包面板玻璃表面的不透明度（比 [+] 抽屉的 0.76 / 0.75 更实）。
 *
 * 抽屉里是「图标 + 文案」，透一点没关系；表情面板里是细线条的小图案，
 * 底板太透会让表情和壁纸糊在一起，因此这里单独提高到接近不透明。
 */
private const val STICKER_PANEL_ALPHA_DARK = 0.92f
private const val STICKER_PANEL_ALPHA_LIGHT = 0.94f

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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatComposerBar(
    drawerExpanded: Boolean,
    onDrawerExpandedChange: (Boolean) -> Unit,
    /**
     * 输入框文本。刻意接收 [State] 而不是 String：
     * 若由调用方传值，调用方组合期间就要读取该 state，于是每一次按键都会让整个
     * 调用方 composable（聊天页可达 1800 行）失效重组。改为传 State 后，
     * 读取下沉到本组件内部，按键只会失效本组件。
     */
    inputText: State<String>,
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
        bottom = 12.dp
    ),
    scrimEnabled: Boolean = false,
    drawerHeight: Dp = KeyboardHeightTracker.lastKeyboardHeightDp,
    actionsPerRow: Int = 4,
    drawerContent: (@Composable ColumnScope.() -> Unit)? = null,
    tailModifier: Modifier = Modifier,
    onBottomBarTopChanged: (Float) -> Unit = {},
    stickerPanelExpanded: Boolean = false,
    onToggleStickerPanel: () -> Unit = {},
    onStickerPanelExpandedChange: (Boolean) -> Unit = { expanded ->
        if (expanded != stickerPanelExpanded) onToggleStickerPanel()
    },
    /** 表情包是否处于「已有关键词」的搜索态（由调用方从搜索状态传入；面板高度稳定性需要它） */
    stickerSearchKeywordActive: Boolean = false,
    onStickerPick: ((Long, String) -> Unit)? = null,
    stickerPanelContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val sendOnEnterEnabled by UiPreferencesStore.sendOnEnterEnabled.collectAsStateWithLifecycle()

    val panelVisible = (stickerPanelExpanded && stickerPanelContent != null) || drawerExpanded

    // 系统软键盘动态监听：
    // - imeAnimationTarget: 捕获本次动画的最终目标高度（动画启动瞬间即为真实完整高度，不受中间帧干扰）
    // - ime: 当前实时物理位置
    // - isImeVisible: 键盘是否处于可见/弹出态
    val imeTargetBottomDp = WindowInsets.imeAnimationTarget.asPaddingValues().calculateBottomPadding()
    val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isImeVisible = WindowInsets.isImeVisible
    val navBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(imeTargetBottomDp, imeBottomDp, isImeVisible) {
        if (isImeVisible) {
            // 当动画完成且处于完全静止展开态时（ime == imeAnimationTarget 且处于展开态），确切锁定实测真实静止高度
            if (imeBottomDp >= 200.dp && imeBottomDp == imeTargetBottomDp) {
                KeyboardHeightTracker.recordRestingHeight(imeBottomDp)
            } else if (!KeyboardHeightTracker.hasRecordedPeak && imeBottomDp >= 200.dp) {
                KeyboardHeightTracker.updatePeak(imeBottomDp)
            }
        }
    }

    // 记录最近激活的面板类型，以便收起动画或切键盘接管期间保持正确的面板内容
    var lastActivePanelWasDrawer by remember { mutableStateOf(false) }
    if (drawerExpanded) {
        lastActivePanelWasDrawer = true
    } else if (stickerPanelExpanded) {
        lastActivePanelWasDrawer = false
    }

    // 从面板切键盘的平滑垫底接管期：保持底层面板高度直到软键盘完全升起覆盖，杜绝输入栏回落抖动
    var isHandingOffToKeyboard by remember { mutableStateOf(false) }

    LaunchedEffect(isHandingOffToKeyboard, imeBottomDp, imeTargetBottomDp, isImeVisible, drawerHeight) {
        if (isHandingOffToKeyboard) {
            val reachedFullHeight = imeBottomDp >= drawerHeight
            val reachedAnimationEnd = isImeVisible && imeBottomDp >= 200.dp && imeBottomDp == imeTargetBottomDp
            if (reachedFullHeight || reachedAnimationEnd) {
                isHandingOffToKeyboard = false
            }
        }
    }

    LaunchedEffect(isHandingOffToKeyboard) {
        if (isHandingOffToKeyboard) {
            delay(600)
            isHandingOffToKeyboard = false
        }
    }

    // 面板高度过渡控制：
    // - 仅在「键盘 ↔ 面板」切换时瞬间就位（不跑展开/收起动画，保证输入栏零抖动）
    // - 从无键盘态（Idle）打开或收起面板时，平滑向上升起 / 向下滑落
    val targetPanelHeight = if (panelVisible || isHandingOffToKeyboard) drawerHeight else 0.dp
    val panelHeightAnim = remember { Animatable(targetPanelHeight, Dp.VectorConverter) }
    var isInstantPanelSwitch by remember { mutableStateOf(false) }
    var prevTargetPanelHeight by remember { mutableStateOf(targetPanelHeight) }

    if (targetPanelHeight != prevTargetPanelHeight) {
        isInstantPanelSwitch = isHandingOffToKeyboard || imeBottomDp > 0.dp || isImeVisible
        prevTargetPanelHeight = targetPanelHeight
    }

    LaunchedEffect(targetPanelHeight, isInstantPanelSwitch) {
        if (isInstantPanelSwitch) {
            panelHeightAnim.snapTo(targetPanelHeight)
        } else {
            panelHeightAnim.animateTo(
                targetValue = targetPanelHeight,
                animationSpec = tween(durationMillis = 230, easing = FastOutSlowInEasing)
            )
        }
    }

    val currentPanelHeight = if (isInstantPanelSwitch) {
        targetPanelHeight
    } else {
        panelHeightAnim.value
    }

    // 表情搜索框聚焦状态（搜索场景例外）
    val stickerSearchFocusedState = remember { mutableStateOf(false) }
    // 搜索态面板高度：固定为「抽屉高度 + 搜索栏高度」这一**稳定值**，绝不跟随实时 ime。
    // 旧实现用 imeBottomDp + 48dp 当面板高度，而 ime 在收放键盘时会逐帧变化（部分输入法直接跳到目标值），
    // 于是「点搜索框收键盘 / 再点输入框弹键盘」都会让面板高度瞬间改变 —— 这就是搜索时的面板弹跳。
    // 现在：搜索态高度只与键盘记忆高度有关，键盘开或关都是同一个高度，面板纹丝不动。
    val searchExtra = 48.dp
    val stickerSearchActive = stickerSearchFocusedState.value || stickerSearchKeywordActive
    val searchModeActive = stickerSearchActive && stickerPanelExpanded && stickerPanelContent != null
    val searchPanelHeight = drawerHeight + searchExtra

    val panelOccupiedHeight = if (searchModeActive) {
        searchPanelHeight
    } else {
        currentPanelHeight
    }

    // 输入栏底部的真实间隙：max(面板高度, 键盘高度, 导航栏高度)
    // 搜索态固定用稳定高度（等于键盘高度 + 搜索栏），输入栏不会随键盘收放上下跳动
    val effectiveBottomGap = if (searchModeActive) {
        searchPanelHeight
    } else {
        maxOf(panelOccupiedHeight, imeBottomDp, navBottomDp)
    }

    // 加号按钮 45 度旋转为关闭 × 动画
    val plusRotation by animateFloatAsState(
        targetValue = if (drawerExpanded) 45f else 0f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.72f),
        label = "PlusRotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // ---- 抽屉/表情展开时的全屏透明拦截层（点击聊天背景区域平滑收起面板与键盘）----
        if (scrimEnabled && (drawerExpanded || stickerPanelExpanded)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            isHandingOffToKeyboard = false
                            if (drawerExpanded) onDrawerExpandedChange(false)
                            if (stickerPanelExpanded) onStickerPanelExpandedChange(false)
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
            )
        }

        // ---- 面板层（贴屏幕底部，键盘 z 轴之下）----
        // ---- 面板层（贴屏幕底部，键盘 z 轴之下）----
        // 本层不参与 imePadding，键盘弹出时位置不变，由系统软键盘从上方自然盖住；
        // 外壳使用统一常驻的毛玻璃容器（固定 0.92/0.94 质感与固定投影，不随面板切换而销毁重建），
        // 面板↔面板切换时外壳 100% 保持不动，仅内部内容淡入淡出，彻底消除阴影重新刷新导致的闪烁。
        val showPanelLayer = panelVisible || isHandingOffToKeyboard || currentPanelHeight > 0.dp
        if (showPanelLayer) {
            val showingDrawer = when {
                drawerExpanded -> true
                stickerPanelExpanded && stickerPanelContent != null -> false
                else -> lastActivePanelWasDrawer
            }

            val panelShape = RoundedCornerShape(
                topStart = 32.dp,
                topEnd = 32.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
            val panelModifier = if (backdrop != null) {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { panelShape },
                    effects = {
                        colorControls(
                            brightness = if (isDark) 0.06f else 0.10f,
                            saturation = 1.45f
                        )
                        blur(if (isDark) 16.dp.toPx() else 20.dp.toPx())
                        lens(
                            refractionHeight = 22.dp.toPx(),
                            refractionAmount = 40.dp.toPx(),
                            depthEffect = true
                        )
                    },
                    highlight = { null },
                    shadow = {
                        Shadow(
                            radius = 24.dp,
                            color = Color.Black.copy(alpha = if (isDark) 0.40f else 0.22f)
                        )
                    },
                    onDrawSurface = {
                        drawRect(
                            if (isDark) Color(0xFF161820).copy(alpha = STICKER_PANEL_ALPHA_DARK)
                            else Color(0xFFF6F6F8).copy(alpha = STICKER_PANEL_ALPHA_LIGHT)
                        )
                    }
                )
            } else {
                Modifier
                    .shadow(
                        elevation = 20.dp,
                        shape = panelShape,
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.22f),
                        spotColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.22f)
                    )
                    .background(if (isDark) Color(0xFF1C1F28) else Color(0xFFF6F6F8), panelShape)
            }

            val containerHeight = if (searchModeActive) {
                searchPanelHeight
            } else {
                drawerHeight
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(containerHeight)
                    .graphicsLayer {
                        translationY = (drawerHeight - currentPanelHeight).toPx().coerceAtLeast(0f)
                    }
                    .clip(panelShape)
                    .then(panelModifier)
            ) {
                AnimatedContent(
                    targetState = showingDrawer,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "PanelContentCrossfade"
                ) { isDrawer ->
                    if (!isDrawer && stickerPanelContent != null) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CompositionLocalProvider(LocalStickerSearchFocused provides stickerSearchFocusedState) {
                                stickerPanelContent.invoke(this)
                            }
                        }
                    } else {
                        ChatComposerDrawerContent(
                            actions = actions,
                            isDark = isDark,
                            drawerHeight = drawerHeight,
                            actionsPerRow = actionsPerRow,
                            onDismiss = {
                                isHandingOffToKeyboard = false
                                onDrawerExpandedChange(false)
                            },
                            content = drawerContent
                        )
                    }
                }
            }
        }

        val handleInputBoxClick: () -> Unit = {
            if (drawerExpanded || stickerPanelExpanded) {
                isHandingOffToKeyboard = true
            }
            if (drawerExpanded) onDrawerExpandedChange(false)
            if (stickerPanelExpanded) onStickerPanelExpandedChange(false)
            if (!isVoiceMode) {
                inputFocusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        // ---- 输入栏层（始终在面板与软键盘 z 轴之上）----
        // 底部留白 = effectiveBottomGap = max(面板高度, 键盘高度, 导航栏高度)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = effectiveBottomGap)
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
                            isHandingOffToKeyboard = false
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (stickerPanelExpanded) onStickerPanelExpandedChange(false)
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
                    onClick = handleInputBoxClick,
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
                                        indication = null,
                                        onClick = handleInputBoxClick
                                    ),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = inputText.value,
                                    onValueChange = onInputTextChange,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = controlContentColor,
                                        fontSize = 17.sp,
                                        lineHeight = 21.sp
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = if (sendOnEnterEnabled) ImeAction.Send else ImeAction.Default
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (inputText.value.trim().isNotEmpty()) {
                                                onSend()
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(inputFocusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                if (drawerExpanded || stickerPanelExpanded) {
                                                    isHandingOffToKeyboard = true
                                                }
                                                if (drawerExpanded) onDrawerExpandedChange(false)
                                                if (stickerPanelExpanded) onStickerPanelExpandedChange(false)
                                            }
                                        },
                                    decorationBox = { innerTextField ->
                                        if (inputText.value.isEmpty()) {
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
                        val hasText = inputText.value.trim().isNotEmpty()
                        if (!hasText && onStickerPick != null) {
                            StickerToggleButton(
                                expanded = stickerPanelExpanded,
                                tint = controlContentColor,
                                onClick = {
                                    if (stickerPanelExpanded) {
                                        onStickerPanelExpandedChange(false)
                                    } else {
                                        isHandingOffToKeyboard = false
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        if (drawerExpanded) onDrawerExpandedChange(false)
                                        onStickerPanelExpandedChange(true)
                                    }
                                }
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
                                            .clickable {
                                                isHandingOffToKeyboard = false
                                                if (drawerExpanded) onDrawerExpandedChange(false)
                                                if (stickerPanelExpanded) onStickerPanelExpandedChange(false)
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                onToggleVoiceMode()
                                            },
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
 * 扩展抽屉面板内容：顶部拖拽把手 + 宫格动作项（或自定义内容）。
 * 外层底板与投影已由宿主统一常驻毛玻璃卡片承载。
 */
@Composable
private fun ChatComposerDrawerContent(
    actions: List<ChatComposerAction>,
    isDark: Boolean,
    drawerHeight: Dp,
    actionsPerRow: Int,
    onDismiss: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(drawerHeight)
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
                        .navigationBarsPadding()
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

/**
 * 微信同款交互手势修饰符：
 * 当软键盘或底部面板处于展开态时：
 * 1. 点击聊天区域（含空白处及消息气泡）：立即收起键盘与面板，且在消费该点击后不误触发气泡内的点击动作；
 * 2. 手指滑动列表：立即收起键盘与面板，同时不阻碍滑动，保持列表自然顺畅滚动。
 */
@Composable
fun Modifier.dismissOnTapOrScroll(
    enabled: Boolean,
    onDismiss: () -> Unit
): Modifier {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val nestedScrollConnection = remember(enabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (enabled && source == NestedScrollSource.UserInput && (available.x != 0f || available.y != 0f)) {
                    currentOnDismiss()
                }
                return Offset.Zero
            }
        }
    }

    return this
        .nestedScroll(nestedScrollConnection)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (up != null) {
                        up.consume()
                        currentOnDismiss()
                    }
                }
            }
        }
}

