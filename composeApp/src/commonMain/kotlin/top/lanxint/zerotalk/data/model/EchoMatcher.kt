package top.lanxint.zerotalk.data.model

import com.google.gson.JsonParser

/**
 * 本地乐观回显与服务端回推消息的「同一条」判定。
 *
 * 抽成纯函数是为了可单测：这条规则一旦判错，本地回显就永远拿不到服务端 id
 * （`serverId` 恒为 0），消息会永远停在回显上 —— 表现为撤回时提示「该消息尚未同步到服务端」。
 */
internal object EchoMatcher {

    private val MOMENT_ID_REGEX = Regex(""""(?:moment_id|momentId)"\s*:\s*"?(\d+)"?""")
    private val SONG_ID_REGEX = Regex(""""(?:song_id|songId)"\s*:\s*"?(\d+)"?""")
    private val PLAYLIST_ID_REGEX = Regex(""""(?:playlist_id|playlistId)"\s*:\s*"?(\d+)"?""")

    fun extractMomentId(content: String): Long? {
        val regexMatch = MOMENT_ID_REGEX.find(content)?.groupValues?.get(1)?.toLongOrNull()
        if (regexMatch != null && regexMatch > 0L) return regexMatch
        val trimmed = content.trim()
        if (trimmed.startsWith("{")) {
            try {
                val obj = JsonParser.parseString(trimmed).asJsonObject
                val el = obj.get("moment_id") ?: obj.get("momentId")
                val id = el?.asLong?.takeIf { it > 0L }
                if (id != null) return id
            } catch (_: Exception) {}
        }
        return null
    }

    fun extractSongId(content: String): String? {
        val fromParser = MessageContentParser.parseMusic(content)?.songId?.takeIf { it.isNotBlank() }
        if (fromParser != null) return fromParser
        val regexMatch = SONG_ID_REGEX.find(content)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
        if (regexMatch != null) return regexMatch
        val trimmed = content.trim()
        if (trimmed.startsWith("{")) {
            try {
                val obj = JsonParser.parseString(trimmed).asJsonObject
                val el = obj.get("song_id") ?: obj.get("songId") ?: obj.get("id")
                val id = el?.asString?.trim()?.takeIf { it.isNotBlank() }
                if (id != null) return id
            } catch (_: Exception) {}
        }
        return null
    }

    fun extractPlaylistId(content: String): String? {
        val fromParser = MessageContentParser.parseMusicPlaylist(content)?.playlistId?.takeIf { it.isNotBlank() }
        if (fromParser != null) return fromParser
        val regexMatch = PLAYLIST_ID_REGEX.find(content)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
        if (regexMatch != null) return regexMatch
        val trimmed = content.trim()
        if (trimmed.startsWith("{")) {
            try {
                val obj = JsonParser.parseString(trimmed).asJsonObject
                val el = obj.get("playlist_id") ?: obj.get("playlistId") ?: obj.get("id")
                val id = el?.asString?.trim()?.takeIf { it.isNotBlank() }
                if (id != null) return id
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * @param echo 本地乐观回显
     * @param eventType 服务端消息类型（image / audio / dice / text …）
     * @param eventContent 服务端消息 content
     * @param eventImageUrl 服务端消息 image_url（仅图片可能携带）
     * @param eventClientMessageId 服务端回推的 `client_message_id`；与本地回显一致即同一条
     */
    fun isSameKind(
        echo: ChatMessage,
        eventType: String,
        eventContent: String,
        eventImageUrl: String? = null,
        eventClientMessageId: String? = null
    ): Boolean {
        // 加密消息的服务端回推是密文，明文 content 必然不等；
        // 官方以 client_message_id 判定同一条，这里优先用同一标识精确对账。
        val eventClientId = eventClientMessageId?.trim().orEmpty()
        if (eventClientId.isNotEmpty() && echo.clientMessageId.isNotBlank() &&
            echo.clientMessageId == eventClientId
        ) {
            return true
        }
        if (echo.content.isNotBlank() && echo.content == eventContent) return true
        val type = eventType.lowercase()
        if (echo.isDice && type == "dice") return true

        // 1. 动态分享卡片 (moment_share)
        if (echo.isMomentShare && (type == "moment_share" || type == "text" || type.isEmpty())) {
            val echoMomentId = echo.momentShare?.momentId?.takeIf { it > 0L }
                ?: extractMomentId(echo.content)
            val eventMomentId = extractMomentId(eventContent)
            if (echoMomentId != null && eventMomentId != null && echoMomentId == eventMomentId) {
                return true
            }
        }

        // 2. 单曲音乐 (music)
        if (echo.isMusic && (type == "music" || type == "text" || type.isEmpty())) {
            val echoSongId = echo.musicData?.songId?.takeIf { it.isNotBlank() }
                ?: extractSongId(echo.content)
            val eventSongId = extractSongId(eventContent)
            if (!echoSongId.isNullOrBlank() && echoSongId == eventSongId) {
                return true
            }
        }

        // 3. 网易云歌单 (music_playlist)
        if (echo.isMusicPlaylist && (type == "music_playlist" || type == "text" || type.isEmpty())) {
            val echoPlaylistId = echo.musicPlaylist?.playlistId?.takeIf { it.isNotBlank() }
                ?: extractPlaylistId(echo.content)
            val eventPlaylistId = extractPlaylistId(eventContent)
            if (!echoPlaylistId.isNullOrBlank() && echoPlaylistId == eventPlaylistId) {
                return true
            }
        }

        // 4. 表情包 (sticker)
        if (echo.isSticker && (type == "sticker" || type == "image" || type == "text" || type.isEmpty())) {
            val echoAssetId = echo.stickerAssetId.takeIf { it > 0L }
                ?: MessageContentParser.parseStickerAssetId(echo.content) ?: 0L
            val eventAssetId = MessageContentParser.parseStickerAssetId(eventContent) ?: 0L
            if (echoAssetId > 0L && eventAssetId > 0L && echoAssetId == eventAssetId) {
                return true
            }
            val echoUrl = echo.stickerUrl.trim().ifBlank { echo.imageUrl.trim() }
            val eventUrl = eventImageUrl?.trim().orEmpty().ifBlank { eventContent.trim() }
            if (echoUrl.isNotBlank() && eventUrl.isNotBlank() && echoUrl == eventUrl) {
                return true
            }
        }

        // 5. 图片 / 语音的本地回显 content 是占位文案（「［图片］」「[语音 N\"]」），
        // 而 WS 帧与服务端回推的 content 是 OSS URL —— 只比 content 永远匹配不上。
        val echoUrl = when {
            echo.isImage && type == "image" -> echo.imageUrl
            echo.isVoice && type == "audio" -> echo.audioUrl
            else -> return false
        }
        // 图片的 URL 可能在 image_url 或 content；语音的 URL 就在 content（实测帧）
        val eventUrl = if (echo.isImage) eventImageUrl ?: eventContent else eventContent
        return echoUrl.isNotBlank() && eventUrl.isNotBlank() && echoUrl == eventUrl
    }

    /**
     * 重载：判定两条 ChatMessage 是否为同一条业务消息（用于历史合并核销）
     */
    fun isSameKind(echo: ChatMessage, server: ChatMessage): Boolean {
        val serverClientId = server.clientMessageId.trim()
        if (serverClientId.isNotEmpty() && echo.clientMessageId.isNotBlank() &&
            echo.clientMessageId == serverClientId
        ) {
            return true
        }

        // 优先尝试直接模型数据比对
        if (echo.isMomentShare && server.isMomentShare) {
            val echoId = echo.momentShare?.momentId?.takeIf { it > 0L } ?: extractMomentId(echo.content)
            val serverId = server.momentShare?.momentId?.takeIf { it > 0L } ?: extractMomentId(server.content)
            if (echoId != null && serverId != null && echoId == serverId) {
                return true
            }
        }

        if (echo.isMusic && server.isMusic) {
            val echoSongId = echo.musicData?.songId?.takeIf { it.isNotBlank() } ?: extractSongId(echo.content)
            val serverSongId = server.musicData?.songId?.takeIf { it.isNotBlank() } ?: extractSongId(server.content)
            if (!echoSongId.isNullOrBlank() && echoSongId == serverSongId) {
                return true
            }
        }

        if (echo.isMusicPlaylist && server.isMusicPlaylist) {
            val echoPlaylistId = echo.musicPlaylist?.playlistId?.takeIf { it.isNotBlank() } ?: extractPlaylistId(echo.content)
            val serverPlaylistId = server.musicPlaylist?.playlistId?.takeIf { it.isNotBlank() } ?: extractPlaylistId(server.content)
            if (!echoPlaylistId.isNullOrBlank() && echoPlaylistId == serverPlaylistId) {
                return true
            }
        }

        if (echo.isSticker && server.isSticker) {
            val echoAssetId = echo.stickerAssetId.takeIf { it > 0L } ?: MessageContentParser.parseStickerAssetId(echo.content) ?: 0L
            val serverAssetId = server.stickerAssetId.takeIf { it > 0L } ?: MessageContentParser.parseStickerAssetId(server.content) ?: 0L
            if (echoAssetId > 0L && serverAssetId > 0L && echoAssetId == serverAssetId) {
                return true
            }
            val echoUrl = echo.stickerUrl.trim().ifBlank { echo.imageUrl.trim() }
            val serverUrl = server.stickerUrl.trim().ifBlank { server.imageUrl.trim() }
            if (echoUrl.isNotBlank() && serverUrl.isNotBlank() && echoUrl == serverUrl) {
                return true
            }
        }

        val serverType = when {
            server.isMomentShare -> "moment_share"
            server.isMusicPlaylist -> "music_playlist"
            server.isMusic -> "music"
            server.isSticker -> "sticker"
            server.isDice -> "dice"
            server.isImage -> "image"
            server.isVoice -> "audio"
            server.isGame -> "game"
            else -> "text"
        }

        val eventImageUrl = when {
            server.isSticker -> server.stickerUrl.ifBlank { server.imageUrl }.takeIf { it.isNotBlank() }
            server.isImage -> server.imageUrl.takeIf { it.isNotBlank() }
            else -> null
        }

        val eventContent = when {
            server.isVoice -> server.audioUrl.ifBlank { server.content }
            else -> server.content
        }

        return isSameKind(
            echo = echo,
            eventType = serverType,
            eventContent = eventContent,
            eventImageUrl = eventImageUrl,
            eventClientMessageId = server.clientMessageId
        )
    }
}

