package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import com.kashif_e.backdrop.backdrops.LayerBackdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop

/**
 * 聊天内容统一捕获层（私聊 / 群聊 / 公共大厅共用）
 *
 * 把「壁纸层 + 消息流 LazyColumn」录进同一个 [LayerBackdrop]，供悬浮顶栏（返回键 / 标题胶囊）与
 * 底部输入栏（[+] / 输入框）等 LiquidButton 光学折射穿透，得到真正的液态玻璃。
 *
 * 约束（防循环采样 / 防 RenderThread 崩溃）：
 * - 本层内**只**放背景与消息流，所有玻璃控件必须声明在本层**之外**（z 序在上），
 *   否则会把玻璃自身录进 Backdrop，造成循环采样；
 * - 全屏只此一层 layerBackdrop，不要嵌套自采样层。
 *
 * [listState] 在 draw 阶段被读取，是私聊同款的「状态闭包防护」：列表滚动时每帧触发统一图层
 * 重绘与 Backdrop 录制，保证玻璃折射内容实时跟随。
 *
 * @param backdrop 采样目标 LayerBackdrop（由 [com.kashif_e.backdrop.backdrops.rememberLayerBackdrop] 创建）
 * @param listState 消息流 LazyListState，用于在 draw 阶段建立滚动失效依赖
 * @param modifier 追加在外层的修饰符（默认仅 fillMaxSize）
 * @param content 被捕获的内容（壁纸层 + 消息流），按声明顺序叠放
 */
@Composable
fun ChatContentCaptureLayer(
    backdrop: LayerBackdrop,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                // 实时感知 LazyListState 滚动偏移，确保滑动的每一帧都触发统一图层重绘与 Backdrop 录制
                listState.firstVisibleItemScrollOffset
                listState.firstVisibleItemIndex
                drawContent()
            }
            .layerBackdrop(backdrop)
    ) {
        content()
    }
}
