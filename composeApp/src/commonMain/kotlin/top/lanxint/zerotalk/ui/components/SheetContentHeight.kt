package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 底部弹窗内容的固定高度（默认屏高的 75%）。
 *
 * 内容若按自身高度撑开，会在「数据加载完成 / 切换 Tab」时改变高度，弹窗随之重新做展开动画，
 * 观感上像是被重新打开；固定高度可消除这种跳动（「捞取历史记录」原本就是这么做的）。
 */
@Composable
fun rememberSheetContentHeight(fraction: Float = 0.75f): Dp =
    LocalConfiguration.current.screenHeightDp.dp * fraction

/** 固定高度的弹窗内容修饰符（见 [rememberSheetContentHeight]） */
@Composable
fun Modifier.sheetContentHeight(fraction: Float = 0.75f): Modifier =
    this.height(rememberSheetContentHeight(fraction))
