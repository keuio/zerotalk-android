package top.lanxint.zerotalk.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 本地界面偏好持久化（设备级，与登录态无关）
 *
 * 存储位置与 [top.lanxint.zerotalk.data.network.DeviceIdManager] 一致（App 私有 filesDir），纯文本 `key=value`：
 * - `zt_ui_preferences.txt`
 *     - `liquid_glass_compat`：兼容渲染模式（`1` = 开启，关闭液态玻璃高性能特效，
 *       渲染能力降级到 Android 13 以下）
 *
 * 与 [top.lanxint.zerotalk.data.network.SessionStore] 的区别：本存储**不随退出登录清除**，
 * 属于「换账号也保留」的设备级界面偏好。
 */
object UiPreferencesStore {
    private const val PREF_FILE = "zt_ui_preferences.txt"
    private const val KEY_LIQUID_GLASS_COMPAT = "liquid_glass_compat"
    private const val KEY_BACKGROUND_RECEIVE = "background_receive"
    private const val KEY_HALL_NOTIFY_AFTER_EXIT = "hall_notify_after_exit"
    private const val KEY_MATCH_AGE_INDEX = "match_age_index"
    private const val KEY_MATCH_GENDER_INDEX = "match_gender_index"
    private const val KEY_MATCH_SHOW_LOCATION = "match_show_location"
    private const val KEY_MATCH_CLEAN_STREAM = "match_clean_stream"
    private const val KEY_MATCH_PRIVACY_MODE = "match_privacy_mode"
    private const val KEY_ENCRYPTED_ROOM_IDS = "encrypted_room_ids"

    @Volatile
    private var storageDir: File? = null

    /** 全量偏好内存快照，避免每次读写都做磁盘 IO */
    private val cache = mutableMapOf<String, String>()
    /** 已知加密房间 ID 缓存 */
    private val encryptedRoomIds = mutableSetOf<String>()

    /**
     * 兼容渲染模式：开启后强制按 Android 13 以下（API 31–32）的渲染能力执行，
     * 关闭全部 AGSL / RuntimeShader 类特效。默认关闭，即保持既有高性能特效。
     */
    @Volatile
    var liquidGlassCompatMode: Boolean = false
        private set

    /**
     * 后台接收消息（保活前台服务）
     *
     * 开启：挂常驻通知并维持后台 WebSocket，退到后台也能收消息；
     * 关闭：不再常驻通知，代价是退到后台一段时间后收不到消息。默认开启。
     */
    private val _backgroundReceiveEnabled = MutableStateFlow(true)
    val backgroundReceiveEnabled: StateFlow<Boolean> = _backgroundReceiveEnabled.asStateFlow()

    /**
     * 退出大厅后继续接收大厅通知
     *
     * 开启：退出大厅页面时**不再**发送 leave_room，保持在大厅房间，退出后仍能收到大厅消息与通知；
     * 关闭：与官方一致，退出页面即离开房间。默认关闭。
     */
    private val _hallNotifyAfterExit = MutableStateFlow(false)
    val hallNotifyAfterExit: StateFlow<Boolean> = _hallNotifyAfterExit.asStateFlow()

    /**
     * 初始化本地持久化存储目录（如 context.filesDir）
     */
    fun init(filesDir: File) {
        storageDir = filesDir
        cache.clear()
        readText(PREF_FILE)?.lineSequence()?.forEach { line ->
            val separator = line.indexOf('=')
            if (separator > 0) {
                cache[line.substring(0, separator).trim()] = line.substring(separator + 1).trim()
            }
        }
        liquidGlassCompatMode = cache[KEY_LIQUID_GLASS_COMPAT] == "1"
        // 缺省视为开启
        _backgroundReceiveEnabled.value = cache[KEY_BACKGROUND_RECEIVE] != "0"
        // 缺省视为关闭（与官方一致）
        _hallNotifyAfterExit.value = cache[KEY_HALL_NOTIFY_AFTER_EXIT] == "1"

        encryptedRoomIds.clear()
        cache[KEY_ENCRYPTED_ROOM_IDS]?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.let { encryptedRoomIds.addAll(it) }
    }

    /**
     * 判断房间是否已知为加密房间
     */
    @Synchronized
    fun isRoomEncrypted(roomId: String): Boolean {
        if (roomId.isBlank()) return false
        return encryptedRoomIds.contains(roomId)
    }

    /**
     * 标记或更新房间是否为加密房间
     */
    @Synchronized
    fun markRoomEncrypted(roomId: String, encrypted: Boolean = true) {
        if (roomId.isBlank()) return
        val changed = if (encrypted) {
            encryptedRoomIds.add(roomId)
        } else {
            encryptedRoomIds.remove(roomId)
        }
        if (changed) {
            cache[KEY_ENCRYPTED_ROOM_IDS] = encryptedRoomIds.joinToString(",")
            writeText(PREF_FILE, cache.entries.joinToString("\n") { "${it.key}=${it.value}" })
        }
    }

    /**
     * 写入「后台接收消息」开关（值未变化时跳过落盘）
     */
    @Synchronized
    fun setBackgroundReceiveEnabled(enabled: Boolean) {
        if (_backgroundReceiveEnabled.value == enabled) return
        _backgroundReceiveEnabled.value = enabled
        cache[KEY_BACKGROUND_RECEIVE] = if (enabled) "1" else "0"
        writeText(PREF_FILE, cache.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    /**
     * 写入「退出大厅后继续接收大厅通知」开关（值未变化时跳过落盘）
     */
    @Synchronized
    fun setHallNotifyAfterExit(enabled: Boolean) {
        if (_hallNotifyAfterExit.value == enabled) return
        _hallNotifyAfterExit.value = enabled
        cache[KEY_HALL_NOTIFY_AFTER_EXIT] = if (enabled) "1" else "0"
        writeText(PREF_FILE, cache.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    /**
     * 写入兼容渲染模式开关（值未变化时跳过落盘）
     */
    @Synchronized
    fun setLiquidGlassCompatMode(enabled: Boolean) {
        if (liquidGlassCompatMode == enabled) return
        liquidGlassCompatMode = enabled
        cache[KEY_LIQUID_GLASS_COMPAT] = if (enabled) "1" else "0"
        writeText(PREF_FILE, cache.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    /**
     * 读取匹配偏好设置
     */
    fun getMatchPreferences(defaultAgeIndex: Int = 0, defaultGenderIndex: Int = 0, defaultShowLocation: Boolean = true, defaultCleanStream: Boolean = false, defaultPrivacyMode: Boolean = true): top.lanxint.zerotalk.ui.sheets.MatchingPreferences {
        return top.lanxint.zerotalk.ui.sheets.MatchingPreferences(
            ageIndex = cache[KEY_MATCH_AGE_INDEX]?.toIntOrNull() ?: defaultAgeIndex,
            genderIndex = cache[KEY_MATCH_GENDER_INDEX]?.toIntOrNull() ?: defaultGenderIndex,
            showLocation = cache[KEY_MATCH_SHOW_LOCATION]?.let { it == "1" } ?: defaultShowLocation,
            cleanStreamMode = cache[KEY_MATCH_CLEAN_STREAM]?.let { it == "1" } ?: defaultCleanStream,
            anonymousMode = cache[KEY_MATCH_PRIVACY_MODE]?.let { it == "1" } ?: defaultPrivacyMode
        )
    }

    /**
     * 保存匹配偏好设置
     */
    @Synchronized
    fun setMatchPreferences(prefs: top.lanxint.zerotalk.ui.sheets.MatchingPreferences) {
        cache[KEY_MATCH_AGE_INDEX] = prefs.ageIndex.toString()
        cache[KEY_MATCH_GENDER_INDEX] = prefs.genderIndex.toString()
        cache[KEY_MATCH_SHOW_LOCATION] = if (prefs.showLocation) "1" else "0"
        cache[KEY_MATCH_CLEAN_STREAM] = if (prefs.cleanStreamMode) "1" else "0"
        cache[KEY_MATCH_PRIVACY_MODE] = if (prefs.anonymousMode) "1" else "0"
        writeText(PREF_FILE, cache.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    // ---------------- 文件读写 ----------------

    private fun fileOf(name: String): File? = storageDir?.let { File(it, name) }

    private fun readText(name: String): String? = try {
        fileOf(name)?.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun writeText(name: String, content: String) {
        try {
            fileOf(name)?.writeText(content)
        } catch (_: Exception) {
        }
    }
}
