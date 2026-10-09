package top.lanxint.zerotalk.ui.utils

import androidx.compose.runtime.Composable

/**
 * 动态音乐 / 语音的联网音频播放器抽象
 *
 * 播放动态音乐时：先 `POST /api/music/netease/play-url` 取播放地址，
 * 再调用 [MomentAudioPlayer.play]；语音动态直接播放 [top.lanxint.zerotalk.data.model.MomentItem.audioUrl]。
 *
 * 仅暴露播放 / 暂停 / 释放最小集合，平台实现负责具体音频栈
 * （Android 使用系统 MediaPlayer）。
 */
interface MomentAudioPlayer {
    /** 当前是否正在播放 */
    val isPlaying: Boolean

    /**
     * 开始播放 [url]。
     *
     * @param onComplete 播放自然结束时回调（用于 UI 恢复未播放状态）
     * @param onError 播放失败时回调，携带原因
     */
    fun play(url: String, onComplete: (() -> Unit)? = null, onError: ((String) -> Unit)? = null)

    /** 暂停 / 停止当前播放 */
    fun stop()

    /** 释放底层资源（页面销毁时调用） */
    fun release()
}

/**
 * 获取当前页面的联网音频播放器（Composable 内使用，随作用域释放）
 */
@Composable
expect fun rememberMomentAudioPlayer(): MomentAudioPlayer