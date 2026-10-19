package top.lanxint.zerotalk.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import top.lanxint.zerotalk.data.model.MomentDraft
import top.lanxint.zerotalk.data.model.MomentMusic
import top.lanxint.zerotalk.data.model.MessageContentParser
import top.lanxint.zerotalk.ui.utils.SelectedPhoto

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
    private const val KEY_USAGE_NOTICE_ACCEPTED = "usage_notice_accepted"
    private const val KEY_SEND_ON_ENTER = "send_on_enter"

    @Volatile
    private var storageDir: File? = null

    /** 全量偏好内存快照，避免每次读写都做磁盘 IO */
    private val cache = mutableMapOf<String, String>()
    /** 已知加密房间 ID 缓存 */
    private val encryptedRoomIds = mutableSetOf<String>()

    // ==================== 聊天草稿 (Chat Drafts) ====================
    private const val CHAT_DRAFTS_FILE = "zt_chat_drafts.json"
    private val chatDraftsCache = mutableMapOf<String, String>()

    // ==================== 动态草稿 (Moment Draft) ====================
    private const val MOMENT_DRAFT_FILE = "zt_moment_draft.json"
    private const val MOMENT_DRAFT_PHOTOS_DIR = "moment_draft_photos"

    private data class MomentDraftData(
        val content: String = "",
        val musicJson: String? = null,
        val photoCount: Int = 0
    )

    @Volatile
    private var cachedMomentDraft: MomentDraft? = null

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
     * 首次启动「使用说明与须知」是否已确认
     *
     * 设备级一次性标记（不随退出登录清除）：未确认时启动后会盖一层说明浮层，
     * 用户点「我已阅读并知悉」后写入，之后不再打扰。
     */
    private val _usageNoticeAccepted = MutableStateFlow(false)
    val usageNoticeAccepted: StateFlow<Boolean> = _usageNoticeAccepted.asStateFlow()

    /**
     * 回车发送消息
     * 默认关闭（保持回车换行习惯）。
     */
    private val _sendOnEnterEnabled = MutableStateFlow(false)
    val sendOnEnterEnabled: StateFlow<Boolean> = _sendOnEnterEnabled.asStateFlow()

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
        // 缺省视为未确认：首次启动会弹一次使用说明
        _usageNoticeAccepted.value = cache[KEY_USAGE_NOTICE_ACCEPTED] == "1"
        // 缺省视为关闭
        _sendOnEnterEnabled.value = cache[KEY_SEND_ON_ENTER] == "1"

        encryptedRoomIds.clear()
        cache[KEY_ENCRYPTED_ROOM_IDS]?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.let { encryptedRoomIds.addAll(it) }

        // 恢复聊天草稿缓存
        chatDraftsCache.clear()
        readText(CHAT_DRAFTS_FILE)?.let { json ->
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val map: Map<String, String>? = Gson().fromJson(json, type)
                map?.forEach { (k, v) ->
                    if (k.isNotBlank() && v.isNotEmpty()) chatDraftsCache[k] = v
                }
            } catch (_: Exception) {}
        }
        cachedMomentDraft = null
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
     * 写入「首次启动使用说明已确认」标记（一次性，值未变化时跳过落盘）
     */
    @Synchronized
    fun setUsageNoticeAccepted() {
        if (_usageNoticeAccepted.value) return
        _usageNoticeAccepted.value = true
        cache[KEY_USAGE_NOTICE_ACCEPTED] = "1"
        writeText(PREF_FILE, cache.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }

    /**
     * 写入「回车发送消息」开关（值未变化时跳过落盘）
     */
    @Synchronized
    fun setSendOnEnterEnabled(enabled: Boolean) {
        if (_sendOnEnterEnabled.value == enabled) return
        _sendOnEnterEnabled.value = enabled
        cache[KEY_SEND_ON_ENTER] = if (enabled) "1" else "0"
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

    // ==================== 聊天草稿 (Chat Drafts) ====================

    /**
     * 获取指定房间（私聊、暗号群聊或大厅）的草稿文本
     */
    @Synchronized
    fun getChatDraft(roomId: String): String {
        if (roomId.isBlank()) return ""
        if (chatDraftsCache.isEmpty()) {
            readText(CHAT_DRAFTS_FILE)?.let { json ->
                try {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val map: Map<String, String>? = Gson().fromJson(json, type)
                    map?.forEach { (k, v) ->
                        if (k.isNotBlank() && v.isNotEmpty()) chatDraftsCache[k] = v
                    }
                } catch (_: Exception) {}
            }
        }
        return chatDraftsCache[roomId].orEmpty()
    }

    /**
     * 保存或更新指定房间的草稿文本（若内容为空或空白则自动删除）
     */
    @Synchronized
    fun setChatDraft(roomId: String, draft: String) {
        if (roomId.isBlank()) return
        val trimmed = draft.trim()
        val changed = if (trimmed.isEmpty()) {
            chatDraftsCache.remove(roomId) != null
        } else {
            val prev = chatDraftsCache.put(roomId, draft)
            prev != draft
        }
        if (changed) {
            val json = Gson().toJson(chatDraftsCache)
            writeText(CHAT_DRAFTS_FILE, json)
        }
    }

    /**
     * 清空指定房间的草稿
     */
    @Synchronized
    fun clearChatDraft(roomId: String) {
        setChatDraft(roomId, "")
    }

    // ==================== 已关闭房间公告 (Dismissed Room Announcements) ====================
    private const val DISMISSED_ANNOUNCEMENTS_FILE = "zt_dismissed_announcements.json"
    private val dismissedAnnouncementsCache = mutableMapOf<String, String>()
    private var dismissedAnnouncementsLoaded = false

    /**
     * 获取已被用户手动关闭的群/房间公告映射表（roomId -> announcementText）
     */
    @Synchronized
    fun getDismissedRoomAnnouncements(): Map<String, String> {
        if (!dismissedAnnouncementsLoaded) {
            dismissedAnnouncementsLoaded = true
            val json = readText(DISMISSED_ANNOUNCEMENTS_FILE)
            if (!json.isNullOrBlank()) {
                try {
                    val map = Gson().fromJson<Map<String, String>>(
                        json,
                        object : TypeToken<Map<String, String>>() {}.type
                    )
                    if (map != null) {
                        dismissedAnnouncementsCache.putAll(map)
                    }
                } catch (_: Exception) {}
            }
        }
        return dismissedAnnouncementsCache.toMap()
    }

    /**
     * 记录某个房间已被关闭的公告文本；只有当后续公告内容变更时才会重新弹出
     */
    @Synchronized
    fun setDismissedRoomAnnouncement(roomId: String, announcementText: String) {
        if (roomId.isBlank()) return
        getDismissedRoomAnnouncements() // 确保已加载
        val trimmed = announcementText.trim()
        val changed = if (trimmed.isEmpty()) {
            dismissedAnnouncementsCache.remove(roomId) != null
        } else {
            val prev = dismissedAnnouncementsCache.put(roomId, trimmed)
            prev != trimmed
        }
        if (changed) {
            val json = Gson().toJson(dismissedAnnouncementsCache)
            writeText(DISMISSED_ANNOUNCEMENTS_FILE, json)
        }
    }

    /**
     * 清除指定房间已关闭的公告记录（例如群主清空公告，或发布了新公告）
     */
    @Synchronized
    fun clearDismissedRoomAnnouncement(roomId: String) {
        setDismissedRoomAnnouncement(roomId, "")
    }

    // ==================== 动态草稿 (Moment Draft) ====================

    /**
     * 读取动态草稿（文字、音乐及相册配图）
     */
    @Synchronized
    fun getMomentDraft(): MomentDraft? {
        cachedMomentDraft?.let { return it }
        val json = readText(MOMENT_DRAFT_FILE) ?: return null
        return try {
            val data = Gson().fromJson(json, MomentDraftData::class.java) ?: return null
            val music = data.musicJson?.let {
                MessageContentParser.parseMusic(it) ?: try {
                    Gson().fromJson(it, MomentMusic::class.java)
                } catch (_: Exception) { null }
            }
            val photos = mutableListOf<SelectedPhoto>()
            val photosDir = fileOf(MOMENT_DRAFT_PHOTOS_DIR)
            if (photosDir != null && photosDir.exists()) {
                for (i in 0 until data.photoCount) {
                    val f = File(photosDir, "photo_$i.jpg")
                    if (f.exists() && f.length() > 0) {
                        val bytes = f.readBytes()
                        photos.add(
                            SelectedPhoto(
                                id = "draft_photo_${System.currentTimeMillis()}_$i",
                                uriString = f.toURI().toString(),
                                byteArray = bytes
                            )
                        )
                    }
                }
            }
            val draft = MomentDraft(
                content = data.content,
                music = music,
                photos = photos
            )
            if (draft.isEmpty) {
                null
            } else {
                cachedMomentDraft = draft
                draft
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 保存动态草稿（文字、音乐及相册配图写入专属目录）
     */
    @Synchronized
    fun saveMomentDraft(draft: MomentDraft) {
        if (draft.isEmpty) {
            clearMomentDraft()
            return
        }
        cachedMomentDraft = draft
        val photosDir = fileOf(MOMENT_DRAFT_PHOTOS_DIR)
        photosDir?.mkdirs()
        photosDir?.listFiles()?.forEach { it.delete() }
        draft.photos.forEachIndexed { i, photo ->
            try {
                val f = File(photosDir, "photo_$i.jpg")
                f.writeBytes(photo.byteArray)
            } catch (_: Exception) {}
        }
        val data = MomentDraftData(
            content = draft.content,
            musicJson = draft.music?.toJsonString(),
            photoCount = draft.photos.size
        )
        writeText(MOMENT_DRAFT_FILE, Gson().toJson(data))
    }

    /**
     * 清空动态草稿及残留配图文件
     */
    @Synchronized
    fun clearMomentDraft() {
        cachedMomentDraft = null
        fileOf(MOMENT_DRAFT_FILE)?.delete()
        fileOf(MOMENT_DRAFT_PHOTOS_DIR)?.let { dir ->
            dir.listFiles()?.forEach { it.delete() }
            dir.delete()
        }
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
