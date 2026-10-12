package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [EchoMatcher] 的回归测试。
 *
 * 背景：图片/语音的本地回显 content 是占位文案（「［图片］」「[语音 N\"]」），
 * 而 WS 帧与服务端回推的 content 是 OSS URL。此前只比 content，导致永远匹配不上、
 * serverId 恒为 0 —— 表现为发完图片后长按撤回提示「该消息尚未同步到服务端」。
 */
class EchoMatcherTest {

    private fun echo(
        content: String,
        isImage: Boolean = false,
        imageUrl: String = "",
        isVoice: Boolean = false,
        audioUrl: String = "",
        isDice: Boolean = false,
        clientMessageId: String = ""
    ) = ChatMessage(
        id = "local_1",
        senderId = "me",
        content = content,
        timestamp = "12:00",
        isMine = true,
        isImage = isImage,
        imageUrl = imageUrl,
        isVoice = isVoice,
        audioUrl = audioUrl,
        isDice = isDice,
        clientMessageId = clientMessageId
    )

    @Test
    fun `文本按内容匹配`() {
        assertTrue(EchoMatcher.isSameKind(echo("你好"), "text", "你好"))
        assertFalse(EchoMatcher.isSameKind(echo("你好"), "text", "再见"))
    }

    @Test
    fun `骰子按类型匹配（点数由服务端摇出，本地回显 content 为空）`() {
        assertTrue(EchoMatcher.isSameKind(echo("", isDice = true), "dice", "5"))
    }

    @Test
    fun `图片回显按 URL 匹配（回归：此前只比 content 导致永远匹配不上）`() {
        val url = "https://oss.example.com/a.png"
        assertTrue(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = url), "image", url, url))
    }

    @Test
    fun `图片 URL 只在 content 里也能匹配`() {
        val url = "https://oss.example.com/b.png"
        assertTrue(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = url), "image", url, null))
    }

    @Test
    fun `图片 URL 不同则不匹配`() {
        val a = "https://oss.example.com/a.png"
        val b = "https://oss.example.com/b.png"
        assertFalse(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = a), "image", b, b))
    }

    @Test
    fun `图片回显遇文本事件不匹配`() {
        val url = "https://oss.example.com/a.png"
        assertFalse(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = url), "text", url))
    }

    @Test
    fun `语音回显按 content 里的 URL 匹配`() {
        val url = "https://oss.example.com/a.mp3"
        assertTrue(EchoMatcher.isSameKind(echo("[语音 3\"]", isVoice = true, audioUrl = url), "audio", url))
    }

    @Test
    fun `语音 URL 不同则不匹配`() {
        val a = "https://oss.example.com/a.mp3"
        val b = "https://oss.example.com/b.mp3"
        assertFalse(EchoMatcher.isSameKind(echo("[语音 3\"]", isVoice = true, audioUrl = a), "audio", b))
    }

    @Test
    fun `URL 为空不匹配，避免把空回显误判成命中`() {
        assertFalse(EchoMatcher.isSameKind(echo("［图片］", isImage = true, imageUrl = ""), "image", "", null))
    }

    @Test
    fun `语音事件类型大小写不敏感`() {
        val url = "https://oss.example.com/a.mp3"
        assertTrue(EchoMatcher.isSameKind(echo("[语音 3\"]", isVoice = true, audioUrl = url), "AUDIO", url))
    }

    // ---- 端到端加密：服务端回推的是密文，只能靠 client_message_id 对账 ----

    @Test
    fun `加密消息按 client_message_id 匹配（密文与本地明文不同也算同一条）`() {
        val mine = echo("你好", clientMessageId = "cmid-1")
        assertTrue(EchoMatcher.isSameKind(mine, "text", "Y2lwaGVy", null, "cmid-1"))
    }

    @Test
    fun `client_message_id 不同则不匹配`() {
        val mine = echo("你好", clientMessageId = "cmid-1")
        assertFalse(EchoMatcher.isSameKind(mine, "text", "Y2lwaGVy", null, "cmid-2"))
    }

    @Test
    fun `本地无 client_message_id 时回落到内容比对`() {
        assertTrue(EchoMatcher.isSameKind(echo("你好"), "text", "你好", null, "cmid-1"))
        assertFalse(EchoMatcher.isSameKind(echo("你好"), "text", "Y2lwaGVy", null, "cmid-1"))
    }

    // ---- 富媒体卡片对账测试 ----

    @Test
    fun `动态分享按 moment_id 匹配，服务端重写 JSON 亦可命中`() {
        val localJson = """{"moment_id":12345,"username":"本地用户","excerpt":"动态摘要"}"""
        val serverJson = """{"moment_id":12345,"username":"服务端重写昵称","images":["https://oss.example.com/1.jpg"],"like_count":10}"""
        val local = echo(localJson).copy(isMomentShare = true, momentShare = MomentShareCardData(momentId = 12345L))
        assertTrue(EchoMatcher.isSameKind(local, "moment_share", serverJson))
    }

    @Test
    fun `动态分享 moment_id 不同时不匹配`() {
        val local = echo("""{"moment_id":100}""").copy(isMomentShare = true, momentShare = MomentShareCardData(momentId = 100L))
        assertFalse(EchoMatcher.isSameKind(local, "moment_share", """{"moment_id":200}"""))
    }

    @Test
    fun `单曲音乐按 song_id 匹配`() {
        val local = echo("""{"song_id":"123","name":"歌曲A"}""").copy(
            isMusic = true,
            musicData = MomentMusic(provider = "netease", songId = "123", name = "歌曲A")
        )
        val serverJson = """{"song_id":"123","name":"重写歌曲名","cover_url":"https://oss.example.com/cover.jpg"}"""
        assertTrue(EchoMatcher.isSameKind(local, "music", serverJson))
    }

    @Test
    fun `单曲音乐 song_id 不同则不匹配`() {
        val local = echo("""{"song_id":"123"}""").copy(
            isMusic = true,
            musicData = MomentMusic(provider = "netease", songId = "123", name = "A")
        )
        assertFalse(EchoMatcher.isSameKind(local, "music", """{"song_id":"456"}"""))
    }

    @Test
    fun `歌单按 playlist_id 匹配`() {
        val local = echo("""{"playlist_id":"999","name":"歌单A"}""").copy(
            isMusicPlaylist = true,
            musicPlaylist = MusicPlaylist(provider = "netease", playlistId = "999", name = "歌单A")
        )
        val serverJson = """{"playlist_id":"999","name":"新歌单名","track_count":50}"""
        assertTrue(EchoMatcher.isSameKind(local, "music_playlist", serverJson))
    }

    @Test
    fun `歌单 playlist_id 不同则不匹配`() {
        val local = echo("""{"playlist_id":"999"}""").copy(
            isMusicPlaylist = true,
            musicPlaylist = MusicPlaylist(provider = "netease", playlistId = "999", name = "歌单A")
        )
        assertFalse(EchoMatcher.isSameKind(local, "music_playlist", """{"playlist_id":"888"}"""))
    }

    @Test
    fun `表情包按 asset_id 匹配`() {
        val local = echo("101").copy(isSticker = true, stickerAssetId = 101L)
        assertTrue(EchoMatcher.isSameKind(local, "sticker", "101"))
        assertTrue(EchoMatcher.isSameKind(local, "sticker", """{"asset_id":101}"""))
    }

    @Test
    fun `表情包按 URL 匹配`() {
        val url = "https://oss.example.com/sticker_101.gif"
        val local = echo("").copy(isSticker = true, stickerUrl = url)
        assertTrue(EchoMatcher.isSameKind(local, "sticker", url, url))
    }

    @Test
    fun `表情包 asset_id 不同且 URL 不同则不匹配`() {
        val local = echo("101").copy(isSticker = true, stickerAssetId = 101L, stickerUrl = "https://oss.example.com/101.gif")
        assertFalse(EchoMatcher.isSameKind(local, "sticker", "102", "https://oss.example.com/102.gif"))
    }

    @Test
    fun `重载 isSameKind(echo, server) 各种富媒体卡片匹配`() {
        // 动态卡片
        val localMoment = echo("").copy(isMomentShare = true, momentShare = MomentShareCardData(momentId = 888L))
        val serverMoment = ChatMessage(id = "srv_1", serverId = 1L, senderId = "me", content = """{"moment_id":888}""", timestamp = "12:01", isMine = true, isMomentShare = true, momentShare = MomentShareCardData(momentId = 888L))
        assertTrue(EchoMatcher.isSameKind(localMoment, serverMoment))

        // 音乐卡片
        val localMusic = echo("").copy(isMusic = true, musicData = MomentMusic(provider = "netease", songId = "777", name = "M"))
        val serverMusic = ChatMessage(id = "srv_2", serverId = 2L, senderId = "me", content = """{"song_id":"777"}""", timestamp = "12:01", isMine = true, isMusic = true, musicData = MomentMusic(provider = "netease", songId = "777", name = "M"))
        assertTrue(EchoMatcher.isSameKind(localMusic, serverMusic))

        // 歌单卡片
        val localPlaylist = echo("").copy(isMusicPlaylist = true, musicPlaylist = MusicPlaylist(provider = "netease", playlistId = "666", name = "P"))
        val serverPlaylist = ChatMessage(id = "srv_3", serverId = 3L, senderId = "me", content = """{"playlist_id":"666"}""", timestamp = "12:01", isMine = true, isMusicPlaylist = true, musicPlaylist = MusicPlaylist(provider = "netease", playlistId = "666", name = "P"))
        assertTrue(EchoMatcher.isSameKind(localPlaylist, serverPlaylist))

        // 表情包卡片
        val localSticker = echo("").copy(isSticker = true, stickerAssetId = 555L)
        val serverSticker = ChatMessage(id = "srv_4", serverId = 4L, senderId = "me", content = "555", timestamp = "12:01", isMine = true, isSticker = true, stickerAssetId = 555L)
        assertTrue(EchoMatcher.isSameKind(localSticker, serverSticker))
    }
}
