package top.lanxint.zerotalk.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * 聊天消息 `content` 的纯解析器（无副作用，便于单元测试）。
 *
 * 逐字段对齐官方 bundle：
 * - 歌曲 `Zn()`（`musicPlayer-*.js` 导出的 `a`）
 * - 歌单 `Yr()`（`musicPlayer-*.js` 导出的 `b`）
 * - 表情包 `Su()`（`publicAnnouncementDismiss-*.js`）
 *
 * 解析器只做「字符串 / JSON 元素 -> 模型」的转换，不依赖
 * `ZeroTalkClientManager` 单例，因此可以直接在 JVM 单元测试里调用。
 */
object MessageContentParser {

    /** 官方 `/^\d{1,20}$/`：纯数字 id（歌单 / 歌曲） */
    private val NUMERIC_ID_REGEX = Regex("^\\d{1,20}$")

    /** 官方歌单最多保留 500 首曲目 */
    private const val MAX_PLAYLIST_TRACKS = 500

    /** 歌曲名 / 歌单名缺失时的官方兜底文案 */
    private const val UNKNOWN_SONG = "未知歌曲"
    private const val UNKNOWN_PLAYLIST = "未知歌单"

    /**
     * 表情包解析结果。
     *
     * @param assetId 从 content 解析出的 asset_id（无法解析为 0）
     * @param url 最终可展示的图片地址（可能为空）
     */
    data class StickerRef(
        val assetId: Long,
        val url: String
    ) {
        val hasImage: Boolean get() = url.isNotBlank()
    }

    // ==================== 歌曲 ====================

    /**
     * 解析 `type:"music"` 的 content（官方 `Zn()`）。
     *
     * @return 解析成功返回歌曲；JSON 非法 / 非对象 / id 非纯数字 / 是歌单形态时返回 null
     */
    fun parseMusic(content: String): MomentMusic? = parseObjectText(content)?.let(::musicFromObject)

    /** 解析 JSON 元素形态的歌曲（歌单 tracks 复用） */
    fun parseMusicElement(element: JsonElement?): MomentMusic? = parseObjectElement(element)?.let(::musicFromObject)

    private fun musicFromObject(obj: JsonObject): MomentMusic? {
        // 官方：kind === "playlist" || playlist_id 为真值 -> 不是歌曲
        if (obj.str("kind") == "playlist" || jsTruthy(obj.get("playlist_id"))) return null
        val songId = rawOr(obj, "song_id", "id").asStringOrNull()?.trim().orEmpty()
        if (!NUMERIC_ID_REGEX.matches(songId)) return null
        return MomentMusic(
            provider = "netease",
            songId = songId,
            name = obj.str("name").ifBlank { UNKNOWN_SONG },
            artists = obj.str("artists"),
            album = obj.str("album"),
            coverUrl = obj.str("cover_url")
        )
    }

    // ==================== 歌单 ====================

    /**
     * 解析 `type:"music_playlist"` 的 content（官方 `Yr()`）。
     *
     * @return 解析成功返回歌单；JSON 非法 / 非对象 / playlist_id 非法 / 形态不匹配时返回 null
     */
    fun parseMusicPlaylist(content: String): MusicPlaylist? = parseObjectText(content)?.let(::playlistFromObject)

    /** 解析 JSON 元素形态的歌单 */
    fun parseMusicPlaylistElement(element: JsonElement?): MusicPlaylist? =
        parseObjectElement(element)?.let(::playlistFromObject)

    private fun playlistFromObject(obj: JsonObject): MusicPlaylist? {
        // 官方：String(playlist_id ?? id).trim()
        val playlistId = rawOr(obj, "playlist_id", "id").asStringOrNull()?.trim().orEmpty()
        if (!NUMERIC_ID_REGEX.matches(playlistId)) return null

        // 官方：kind !== "playlist" && !Array.isArray(tracks) && !playlist_id -> null
        val kind = obj.str("kind")
        val tracksElement = obj.get("tracks")?.takeIf { !it.isJsonNull && it.isJsonArray }
        if (kind != "playlist" && tracksElement == null && !jsTruthy(obj.get("playlist_id"))) return null

        val tracks = mutableListOf<MomentMusic>()
        if (tracksElement != null) {
            for (element in tracksElement.asJsonArray) {
                parseMusicElement(element)?.let { tracks.add(it) }
                if (tracks.size >= MAX_PLAYLIST_TRACKS) break
            }
        }

        val trackCount = maxOf(tracks.size, numberOrZero(obj.get("track_count")))
        return MusicPlaylist(
            provider = "netease",
            playlistId = playlistId,
            name = obj.str("name").ifBlank { UNKNOWN_PLAYLIST },
            coverUrl = obj.str("cover_url"),
            creator = obj.str("creator"),
            trackCount = trackCount,
            tracks = tracks
        )
    }

    // ==================== 表情包 ====================

    /**
     * 从表情包 content 解析 asset_id（官方 `Su()` 的 `Number.parseInt` 语义）。
     *
     * 官方只接受纯数字；这里额外兼容服务端可能下发的 JSON 形态
     * （`{"asset_id":123}` / `{"id":123}`），两者都拿不到时返回 null。
     */
    fun parseStickerAssetId(content: String?): Long? {
        val text = content?.trim().orEmpty()
        if (text.isEmpty()) return null
        if (text.startsWith("{")) {
            val obj = parseObjectText(text) ?: return null
            val id = jsToLongOrNull(obj.get("asset_id")) ?: jsToLongOrNull(obj.get("id"))
            return id?.takeIf { it > 0 }
        }
        return jsParseIntPositive(text)
    }

    /**
     * 解析表情包展示地址（官方 `ko()`）。
     *
     * 1. `image_url` 非空 -> 直接用；
     * 2. 否则用 content 的 asset_id 调 [lookup]（本地表情包列表）；
     * 3. 都拿不到 -> url 为空，渲染层气泡降级展示「表情包已失效」（对齐官方）。
     */
    fun resolveSticker(
        imageUrl: String?,
        content: String?,
        lookup: (Long) -> String = { "" }
    ): StickerRef {
        val direct = imageUrl?.trim().orEmpty()
        val assetId = parseStickerAssetId(content) ?: 0L
        if (direct.isNotEmpty()) return StickerRef(assetId, direct)
        if (assetId <= 0L) return StickerRef(0L, "")
        return StickerRef(assetId, lookup(assetId).trim())
    }

    // ==================== 会话摘要 ====================

    /** 官方 `go()`：歌单摘要 `[歌单] 《歌单名》`（超长截断） */
    fun musicPlaylistPreview(playlist: MusicPlaylist?): String {
        if (playlist == null) return "[歌单]"
        val text = "《${playlist.name.ifBlank { UNKNOWN_PLAYLIST }}》"
        return if (text.length > 36) "[歌单] ${text.take(36)}…" else "[歌单] $text"
    }

    // ==================== 内部工具 ====================

    /** 字符串 -> JSON 对象（官方 `JSON.parse` 语义：解析失败 / 非对象返回 null） */
    private fun parseObjectText(text: String): JsonObject? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val parsed = try {
            JsonParser.parseString(trimmed)
        } catch (_: Exception) {
            return null
        }
        return parsed.takeIf { it.isJsonObject }?.asJsonObject
    }

    /** JSON 元素 -> JSON 对象（对象直取；字符串再解析一层，与官方 `typeof e === "string"` 分支一致） */
    private fun parseObjectElement(element: JsonElement?): JsonObject? {
        if (element == null || element.isJsonNull) return null
        if (element.isJsonObject) return element.asJsonObject
        if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            return parseObjectText(element.asString)
        }
        return null
    }

    /** `a ?? b`（仅 null / 缺失回落，空字符串不回落） */
    private fun rawOr(obj: JsonObject, primary: String, fallback: String): JsonElement? =
        obj.get(primary)?.takeIf { !it.isJsonNull } ?: obj.get(fallback)?.takeIf { !it.isJsonNull }

    /** `String(x ?? "")` 的近似：非原始值返回 null */
    private fun JsonElement?.asStringOrNull(): String? {
        if (this == null || this.isJsonNull || !this.isJsonPrimitive) return null
        return try {
            this.asString
        } catch (_: Exception) {
            null
        }
    }

    /** `String(obj[key] ?? "").trim()` */
    private fun JsonObject.str(key: String): String {
        val element = get(key) ?: return ""
        if (element.isJsonNull || !element.isJsonPrimitive) return ""
        return try {
            element.asString.trim()
        } catch (_: Exception) {
            ""
        }
    }

    /** JavaScript 真值判定（`if (x)`），数组 / 对象恒为真 */
    private fun jsTruthy(element: JsonElement?): Boolean {
        if (element == null || element.isJsonNull) return false
        if (!element.isJsonPrimitive) return true
        val primitive = element.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> {
                val value = primitive.asDouble
                value != 0.0 && !value.isNaN()
            }
            primitive.isString -> primitive.asString.isNotEmpty()
            else -> false
        }
    }

    /** `Number(x) || 0`（NaN / 非数字 -> 0），再取整 */
    private fun numberOrZero(element: JsonElement?): Int {
        if (element == null || element.isJsonNull || !element.isJsonPrimitive) return 0
        val primitive = element.asJsonPrimitive
        val value = when {
            primitive.isNumber -> primitive.asDouble
            primitive.isString -> primitive.asString.trim().toDoubleOrNull() ?: return 0
            primitive.isBoolean -> if (primitive.asBoolean) 1.0 else 0.0
            else -> return 0
        }
        if (value.isNaN() || value.isInfinite()) return 0
        return value.toInt()
    }

    /** JSON 元素 -> Long（数字直接取；字符串走 parseInt 语义） */
    private fun jsToLongOrNull(element: JsonElement?): Long? {
        if (element == null || element.isJsonNull || !element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return when {
            primitive.isNumber -> {
                val value = primitive.asDouble
                if (value.isFinite()) value.toLong() else null
            }
            primitive.isString -> jsParseIntPositive(primitive.asString)
            else -> null
        }
    }

    /** 官方 `Number.parseInt(text, 10)` 语义，仅返回 > 0 的结果 */
    private fun jsParseIntPositive(text: String): Long? {
        var index = 0
        while (index < text.length && text[index].isWhitespace()) index++
        var negative = false
        if (index < text.length && (text[index] == '+' || text[index] == '-')) {
            negative = text[index] == '-'
            index++
        }
        val start = index
        while (index < text.length && text[index] in '0'..'9') index++
        if (index == start) return null
        val magnitude = text.substring(start, index).toLongOrNull() ?: return null
        val signed = if (negative) -magnitude else magnitude
        return signed.takeIf { it > 0 }
    }
}
