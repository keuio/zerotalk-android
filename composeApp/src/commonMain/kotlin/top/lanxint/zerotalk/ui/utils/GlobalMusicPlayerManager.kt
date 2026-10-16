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

    private val _playlist = MutableStateFlow<List<PlayingTrack>>(emptyList())
    val playlist: StateFlow<List<PlayingTrack>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

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

    private fun addToPlaylist(track: PlayingTrack): Int {
        val list = _playlist.value.toMutableList()
        val existingIndex = list.indexOfFirst { it.songId == track.songId }
        return if (existingIndex != -1) {
            val existing = list[existingIndex]
            val merged = if (track.playUrl.isNotBlank() || existing.playUrl.isBlank()) track else track.copy(playUrl = existing.playUrl)
            list[existingIndex] = merged
            _playlist.value = list
            existingIndex
        } else {
            list.add(track)
            _playlist.value = list
            list.size - 1
        }
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
        // 如果点击的是当前正在播放的同一首歌曲且底层播放器实例仍有效
        if (current?.songId == songId && current.playUrl.isNotBlank() && player != null) {
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
        val idx = addToPlaylist(initialTrack)
        _currentIndex.value = idx
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
            addToPlaylist(readyTrack)
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
        val idx = addToPlaylist(track)
        _currentIndex.value = idx
        _currentTrack.value = track
        _isVisible.value = true
        startPlayingInternal(track, onError)
    }

    /**
     * 播放播放列表中的指定索引歌曲
     */
    fun playAt(index: Int, onError: ((String) -> Unit)? = null) {
        val list = _playlist.value
        if (index !in list.indices) return
        val track = list[index]
        _currentIndex.value = index
        _isVisible.value = true
        if (track.playUrl.isNotBlank()) {
            _currentTrack.value = track
            startPlayingInternal(track, onError)
        } else {
            playSong(
                songId = track.songId,
                name = track.name,
                artist = track.artist,
                album = track.album,
                coverUrl = track.coverUrl,
                onError = onError
            )
        }
    }

    /**
     * 切上一首
     */
    fun playPrevious(onError: ((String) -> Unit)? = null) {
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIdx = _currentIndex.value
        val prevIdx = if (currentIdx > 0) currentIdx - 1 else list.size - 1
        playAt(prevIdx, onError)
    }

    /**
     * 切下一首
     */
    fun playNext(onError: ((String) -> Unit)? = null) {
        val list = _playlist.value
        if (list.isEmpty()) return
        val currentIdx = _currentIndex.value
        val nextIdx = if (currentIdx + 1 < list.size) currentIdx + 1 else 0
        playAt(nextIdx, onError)
    }

    /**
     * 从播放列表中移除某一项
     */
    fun removeFromPlaylist(index: Int) {
        val list = _playlist.value.toMutableList()
        if (index !in list.indices) return
        val isCurrent = index == _currentIndex.value
        list.removeAt(index)
        _playlist.value = list
        if (list.isEmpty()) {
            close()
            _currentIndex.value = -1
        } else if (isCurrent) {
            val nextIdx = index.coerceAtMost(list.size - 1)
            playAt(nextIdx)
        } else if (index < _currentIndex.value) {
            _currentIndex.value = _currentIndex.value - 1
        }
    }

    /**
     * 清空播放列表并停止播放
     */
    fun clearPlaylist() {
        close()
        _playlist.value = emptyList()
        _currentIndex.value = -1
    }

    /**
     * 跳转至指定秒数
     */
    fun seekTo(positionSec: Int) {
        _progressSec.value = positionSec
        player?.seekTo(positionSec)
    }

    private fun startPlayingInternal(track: PlayingTrack, onError: ((String) -> Unit)?) {
        stopProgressTracking()
        val p = getOrCreatePlayer()
        p.stop()
        _isLoading.value = true
        _isPlaying.value = false

        p.play(
            url = track.playUrl,
            onComplete = {
                val list = _playlist.value
                if (list.size > 1) {
                    playNext()
                } else {
                    _isPlaying.value = false
                    _isLoading.value = false
                    _progressSec.value = 0
                    stopProgressTracking()
                }
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
            val p = player
            if (p != null) {
                p.resume()
                _isPlaying.value = true
                startProgressTracking()
            } else {
                startPlayingInternal(current, null)
            }
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
