package top.lanxint.zerotalk.ui.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 平台实现：解析远程网络音频的时长（秒）
 */
expect suspend fun extractRemoteAudioDurationSec(audioUrl: String): Int

/**
 * 全局语音时长异步解析器与双层缓存
 *
 * 针对服务端下发的语音消息缺少 duration 的问题：
 * 1. 内存高速缓存 (Memory Cache)：首次解析后常驻内存，保证列表滑动与重组零延迟；
 * 2. 磁盘持久化 (Disk Storage)：落盘到私有目录 `zt_voice_durations.json`，App 重启后直接命中；
 * 3. 并发去重 (In-flight Request Deduplication)：相同 URL 的请求合并，避免并发重复嗅探。
 */
object VoiceDurationResolver {
    private const val CACHE_FILE_NAME = "zt_voice_durations.json"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val memoryCache = mutableMapOf<String, Int>()
    private val inFlight = mutableMapOf<String, Deferred<Int>>()

    @Volatile
    private var storageDir: File? = null
    private val gson = Gson()

    /**
     * 初始化本地持久化存储目录（在 Application 中注入 filesDir）
     */
    fun init(filesDir: File) {
        storageDir = filesDir
        scope.launch {
            loadFromDisk()
        }
    }

    private suspend fun loadFromDisk() = mutex.withLock {
        runCatching {
            val dir = storageDir ?: return@runCatching
            val file = File(dir, CACHE_FILE_NAME)
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<Map<String, Int>>() {}.type
                val map: Map<String, Int> = gson.fromJson(json, type) ?: emptyMap()
                synchronized(memoryCache) {
                    memoryCache.putAll(map)
                }
            }
        }
    }

    private suspend fun saveToDisk() = mutex.withLock {
        runCatching {
            val dir = storageDir ?: return@runCatching
            val file = File(dir, CACHE_FILE_NAME)
            val snapshot = synchronized(memoryCache) { HashMap(memoryCache) }
            val json = gson.toJson(snapshot)
            file.writeText(json)
        }
    }

    /**
     * 同步直接查询内存缓存中的时长（无耗时、零等待；未命中或小于等于 0 返回 0）
     */
    fun getCached(url: String?): Int {
        if (url.isNullOrBlank()) return 0
        return synchronized(memoryCache) { memoryCache[url] ?: 0 }
    }

    /**
     * 写入已知时长（例如录音发出后、或者播放器准备就绪时回传）
     */
    fun put(url: String?, durationSec: Int) {
        if (url.isNullOrBlank() || durationSec <= 0) return
        val changed = synchronized(memoryCache) {
            val old = memoryCache[url]
            if (old != durationSec) {
                memoryCache[url] = durationSec
                true
            } else {
                false
            }
        }
        if (changed) {
            scope.launch {
                saveToDisk()
            }
        }
    }

    /**
     * 异步解析音频时长（带缓存、并发去重与轻量静默网络嗅探）
     */
    suspend fun resolveDuration(url: String?): Int {
        if (url.isNullOrBlank()) return 0
        val cleanUrl = url.trim()

        val cached = getCached(cleanUrl)
        if (cached > 0) return cached

        val deferred: Deferred<Int> = mutex.withLock {
            val existing = inFlight[cleanUrl]
            if (existing != null) {
                existing
            } else {
                val newDeferred = scope.async {
                    runCatching {
                        extractRemoteAudioDurationSec(cleanUrl)
                    }.getOrDefault(0)
                }
                inFlight[cleanUrl] = newDeferred
                newDeferred
            }
        }

        val result = deferred.await()
        mutex.withLock {
            inFlight.remove(cleanUrl)
        }

        if (result > 0) {
            put(cleanUrl, result)
        }
        return result
    }
}
