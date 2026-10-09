package top.lanxint.zerotalk.ui.utils

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.io.IOException

/**
 * Android 平台系统 MediaPlayer 实现
 *
 * 与 [MomentAudioPlayer] 约定一致：`play` 非阻塞异步加载，
 * 就绪后自动播放；自然播完回调 [MomentAudioPlayer.onComplete]。
 */
class AndroidMomentAudioPlayer : MomentAudioPlayer {

    private var player: MediaPlayer? = null

    override val isPlaying: Boolean
        get() = player?.isPlaying == true

    override fun play(url: String, onComplete: (() -> Unit)?, onError: ((String) -> Unit)?) {
        stop()
        if (url.isBlank()) {
            onError?.invoke("播放地址为空")
            return
        }
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepareAsync()
        } catch (e: IOException) {
            onError?.invoke("音频加载失败：${e.message ?: "未知错误"}")
            release()
            return
        } catch (e: IllegalArgumentException) {
            onError?.invoke("音频地址无效")
            release()
            return
        }

        mediaPlayer.setOnPreparedListener { it.start() }
        mediaPlayer.setOnCompletionListener {
            it.release()
            if (player === it) player = null
            onComplete?.invoke()
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            mp.release()
            if (player === mp) player = null
            onError?.invoke("播放出错（$what/$extra）")
            true
        }
    }

    override fun stop() {
        val current = player ?: return
        player = null
        runCatching {
            current.setOnCompletionListener(null)
            current.setOnErrorListener(null)
            if (current.isPlaying) current.stop()
            current.release()
        }
    }

    override fun release() = stop()
}

/**
 * Android 平台联网音频播放器（Composable 生命周期内持有，销毁时释放）
 */
@Composable
actual fun rememberMomentAudioPlayer(): MomentAudioPlayer {
    val player = remember { AndroidMomentAudioPlayer() }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}
