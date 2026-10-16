package top.lanxint.zerotalk.ui.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.data.model.MomentMusic
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager

/**
 * 正在播放的曲目元数据
 */
data class PlayingTrack(
    val songId: String,
    val name: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String = "",
    val playUrl: String = "",
    val provider: String = "netease"
)

/**
 * 全局统一音乐播放管理器
 *
 * 托管全局所有音乐播放场景（动态网易云卡片、聊天室点歌台、消息分享歌曲等）：
 * - 单例维护全局唯一播放栈（新播放自动顶替旧曲目）；
 * - 维护悬浮窗显隐、吸附与控制器展开/收起状态；
 * - 实时同步播放进度与播放/暂停状态；
 * - 跨页面切换与弹窗关闭保持持续播放。
 */
object GlobalMusicPlayerManager {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private var player: MomentAudioPlayer? = null

    private val _currentTrack = MutableStateFlow<PlayingTrack?>(null)
    val currentTrack: StateFlow<PlayingTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private val _progressSec = MutableStateFlow(0)
    val progressSec: StateFlow<Int> = _progressSec.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    private fun getOrCreatePlayer(): MomentAudioPlayer {
        return player ?: createPlatformAudioPlayer().also { player = it }
    }

    /**
     * 播放单曲（来自 MomentMusic 对象）
     */
    fun playMusic(music: MomentMusic, force: Boolean = false, onError: ((String) -> Unit)? = null) {
        playSong(
            songId = music.songId,
            name = music.name,
            artist = music.artists,
            album = music.album,
            coverUrl = music.coverUrl,
            force = force,
            onError = onError
        )
    }

    /**
     * 播放单曲（根据 songId 向服务端获取播放直链并播放）
     */
    fun playSong(
        songId: String,
        name: String,
        artist: String,
        album: String = "",
        coverUrl: String = "",
        force: Boolean = false,
        onError: ((String) -> Unit)? = null
    ) {
        if (songId.isBlank()) {
            onError?.invoke("歌曲 ID 为空")
            return
        }

        val current = _currentTrack.value
        // 如果点击的是当前正在播放的同一首歌曲且已经就绪
        if (current?.songId == songId && current.playUrl.isNotBlank()) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        stopProgressTracking()
        _isLoading.value = true
        _isVisible.value = true
        _progressSec.value = 0
        _durationSec.value = 0

        val initialTrack = PlayingTrack(
            songId = songId,
            name = name.ifBlank { "未知歌曲" },
            artist = artist.ifBlank { "网易云音乐" },
            album = album,
            coverUrl = coverUrl,
            playUrl = ""
        )
        _currentTrack.value = initialTrack

        scope.launch {
            val res = ZeroTalkClientManager.apiService.getNeteasePlayUrl(songId, force = force)
            val url = res.getOrNull()?.url
            if (url.isNullOrBlank()) {
                _isLoading.value = false
                val errMsg = res.exceptionOrNull()?.message ?: "暂时无法获取该歌曲的播放地址"
                onError?.invoke(errMsg)
                return@launch
            }

            val readyTrack = initialTrack.copy(playUrl = url)
            _currentTrack.value = readyTrack
            startPlayingInternal(readyTrack, onError)
        }
    }

    /**
     * 使用已解析好的直链直接播放
     */
    fun playWithUrl(
        songId: String,
        name: String,
        artist: String,
        coverUrl: String,
        playUrl: String,
        onError: ((String) -> Unit)? = null
    ) {
        val track = PlayingTrack(
            songId = songId,
            name = name,
            artist = artist,
            coverUrl = coverUrl,
            playUrl = playUrl
        )
        _currentTrack.value = track
        _isVisible.value = true
        startPlayingInternal(track, onError)
    }

    private fun startPlayingInternal(track: PlayingTrack, onError: ((String) -> Unit)?) {
        stopProgressTracking()
        val p = getOrCreatePlayer()
        p.stop()
        _isLoading.value = true

        p.play(
            url = track.playUrl,
            onComplete = {
                _isPlaying.value = false
                _progressSec.value = 0
                stopProgressTracking()
            },
            onError = { err ->
                _isPlaying.value = false
                _isLoading.value = false
                stopProgressTracking()
                onError?.invoke(err)
            },
            onDuration = { duration ->
                _durationSec.value = duration
                _isLoading.value = false
                _isPlaying.value = true
                startProgressTracking()
            }
        )
        _isPlaying.value = true
        startProgressTracking()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        player?.pause()
        _isPlaying.value = false
        stopProgressTracking()
    }

    fun resume() {
        val current = _currentTrack.value ?: return
        if (current.playUrl.isNotBlank()) {
            player?.resume()
            _isPlaying.value = true
            startProgressTracking()
        }
    }

    fun stop() {
        player?.stop()
        _isPlaying.value = false
        _isLoading.value = false
        stopProgressTracking()
    }

    /**
     * 关闭并隐藏全局播放器悬浮窗
     */
    fun close() {
        stop()
        _isVisible.value = false
        _isExpanded.value = false
        _currentTrack.value = null
        _progressSec.value = 0
        _durationSec.value = 0
    }

    fun toggleExpanded() {
        _isExpanded.value = !_isExpanded.value
    }

    fun setExpanded(expanded: Boolean) {
        _isExpanded.value = expanded
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                player?.let { p ->
                    val pos = p.getCurrentPosition()
                    if (pos > 0) _progressSec.value = pos
                    val dur = p.getDuration()
                    if (dur > 0) _durationSec.value = dur
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }
}
