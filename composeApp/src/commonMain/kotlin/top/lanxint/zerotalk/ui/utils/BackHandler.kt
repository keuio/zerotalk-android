package top.lanxint.zerotalk.ui.utils

import androidx.compose.runtime.Composable

/**
 * 跨平台的系统返回键 / 侧滑手势返回拦截器
 *
 * @param enabled 是否启用拦截，默认为 true
 * @param onBack 拦截后的回调逻辑
 */
@Composable
expect fun BackHandler(enabled: Boolean, onBack: () -> Unit)
