package top.lanxint.zerotalk.ui.utils

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
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
    @Volatile
    private var isPrepared = false

    override val isPlaying: Boolean
        get() = runCatching { isPrepared && player?.isPlaying == true }.getOrDefault(false)

    override fun play(
        url: String,
        onComplete: (() -> Unit)?,
        onError: ((String) -> Unit)?,
        onDuration: ((Int) -> Unit)?
    ) {
        stop()
        if (url.isBlank()) {
            onError?.invoke("播放地址为空")
            return
        }
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        isPrepared = false

        // 统一配置媒体音频通道与属性，确保声音路由至媒体音量（非通话/铃声音道）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }

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

        mediaPlayer.setOnPreparedListener { mp ->
            if (player !== mp) return@setOnPreparedListener
            isPrepared = true
            val durationMs = runCatching { mp.duration }.getOrDefault(0)
            if (durationMs > 0) onDuration?.invoke(durationMs / 1000)
            runCatching { mp.start() }
        }
        mediaPlayer.setOnCompletionListener { mp ->
            isPrepared = false
            runCatching { mp.release() }
            if (player === mp) player = null
            onComplete?.invoke()
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            isPrepared = false
            runCatching { mp.reset() }
            runCatching { mp.release() }
            if (player === mp) player = null
            onError?.invoke("播放出错（$what/$extra）")
            true
        }
    }

    override fun pause() {
        runCatching {
            val current = player ?: return
            if (isPrepared && current.isPlaying) {
                current.pause()
            }
        }
    }

    override fun resume() {
        runCatching {
            val current = player ?: return
            if (isPrepared && !current.isPlaying) {
                current.start()
            }
        }
    }

    override fun getCurrentPosition(): Int = runCatching {
        if (!isPrepared) return 0
        (player?.currentPosition ?: 0) / 1000
    }.getOrDefault(0)

    override fun getDuration(): Int = runCatching {
        if (!isPrepared) return 0
        (player?.duration ?: 0) / 1000
    }.getOrDefault(0)

    override fun seekTo(positionSec: Int) {
        runCatching {
            val current = player ?: return
            if (isPrepared) {
                val targetMs = (positionSec * 1000).coerceAtLeast(0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    current.seekTo(targetMs.toLong(), MediaPlayer.SEEK_CLOSEST)
                } else {
                    current.seekTo(targetMs)
                }
            }
        }
    }

    override fun stop() {
        val current = player ?: return
        player = null
        isPrepared = false
        runCatching {
            current.setOnCompletionListener(null)
            current.setOnErrorListener(null)
            current.setOnPreparedListener(null)
            if (current.isPlaying) current.stop()
            current.reset()
            current.release()
        }
    }

    override fun release() = stop()
}

/**
 * 创建独立平台音频播放器（可脱离 Composable 生命周期跨页面常驻）
 */
actual fun createPlatformAudioPlayer(): MomentAudioPlayer = AndroidMomentAudioPlayer()

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
