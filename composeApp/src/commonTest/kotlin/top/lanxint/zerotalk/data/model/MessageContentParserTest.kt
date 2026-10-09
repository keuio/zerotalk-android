package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [MessageContentParser] 的回归测试。
 *
 * 覆盖官方两种「支持但此前未渲染」的消息类型：
 * - `music_playlist`（官方 `Yr()`）正常 / 异常 JSON；
 * - `sticker`（官方 `Su()` / `ko()`）有 / 无 `image_url`，content 为数字或 JSON。
 */
class MessageContentParserTest {

    // ==================== music_playlist ====================

    @Test
    fun `歌单 JSON 正常时逐字段解析`() {
        val json = """
            {
              "provider": "netease",
              "kind": "playlist",
              "playlist_id": "123456789",
              "name": "深夜电台",
              "cover_url": "https://p1.music.126.net/cover.jpg",
              "creator": "零友",
              "track_count": 3,
              "tracks": [
                {"song_id": "111", "name": "歌一", "artists": "歌手A", "album": "专辑", "cover_url": "https://c/1.jpg"},
                {"song_id": "222", "name": "歌二", "artists": "歌手B"},
                {"song_id": "333", "name": "歌三", "artists": "歌手C"}
              ]
            }
        """.trimIndent()

        val playlist = MessageContentParser.parseMusicPlaylist(json)
        assertNotNull(playlist)
        assertEquals("netease", playlist.provider)
        assertEquals("123456789", playlist.playlistId)
        assertEquals("深夜电台", playlist.name)
        assertEquals("https://p1.music.126.net/cover.jpg", playlist.coverUrl)
        assertEquals("零友", playlist.creator)
        assertEquals(3, playlist.trackCount)
        assertEquals(3, playlist.tracks.size)
        assertEquals("111", playlist.tracks[0].songId)
        assertEquals("歌一", playlist.tracks[0].name)
        assertEquals("歌手A", playlist.tracks[0].artists)
        assertEquals("https://c/1.jpg", playlist.tracks[0].coverUrl)
    }

    @Test
    fun `歌单 kind 为 playlist 时即使没有 tracks 也解析成功`() {
        val json = """{"kind":"playlist","playlist_id":"42","name":"空歌单"}"""
        val playlist = MessageContentParser.parseMusicPlaylist(json)
        assertNotNull(playlist)
        assertEquals("42", playlist.playlistId)
        assertEquals("空歌单", playlist.name)
        assertEquals(0, playlist.trackCount)
        assertTrue(playlist.tracks.isEmpty())
    }

    @Test
    fun `歌单仅有 id 且 tracks 为数组时回落到 id`() {
        val json = """{"id":"987","tracks":[{"song_id":"1","name":"单曲"}]}"""
        val playlist = MessageContentParser.parseMusicPlaylist(json)
        assertNotNull(playlist)
        assertEquals("987", playlist.playlistId)
        assertEquals(1, playlist.trackCount)
    }

    @Test
    fun `歌单 track_count 取曲目数与字段值的较大者`() {
        val json = """{"kind":"playlist","playlist_id":"1","track_count":"50","tracks":[{"song_id":"1"}]}"""
        val playlist = MessageContentParser.parseMusicPlaylist(json)
        assertNotNull(playlist)
        assertEquals(50, playlist.trackCount)
        assertEquals(1, playlist.tracks.size)
    }

    @Test
    fun `歌单名缺失时回落为未知歌单`() {
        val json = """{"kind":"playlist","playlist_id":"1"}"""
        assertEquals("未知歌单", MessageContentParser.parseMusicPlaylist(json)?.name)
    }

    @Test
    fun `歌单 tracks 里的非法歌曲被过滤`() {
        val json = """
            {"kind":"playlist","playlist_id":"1","tracks":[
              {"song_id":"1","name":"合法"},
              {"kind":"playlist","playlist_id":"9"},
              {"song_id":"abc"},
              "not-an-object",
              {"id":"2","name":"用 id 的合法歌曲"}
            ]}
        """.trimIndent()
        val playlist = MessageContentParser.parseMusicPlaylist(json)
        assertNotNull(playlist)
        assertEquals(2, playlist.tracks.size)
        assertEquals("1", playlist.tracks[0].songId)
        assertEquals("2", playlist.tracks[1].songId)
    }

    @Test
    fun `歌单 JSON 异常时返回 null`() {
        assertNull(MessageContentParser.parseMusicPlaylist(""))
        assertNull(MessageContentParser.parseMusicPlaylist("not json"))
        assertNull(MessageContentParser.parseMusicPlaylist("""[1,2,3]"""))
        assertNull(MessageContentParser.parseMusicPlaylist("""{"kind":"playlist"}"""))
        assertNull(MessageContentParser.parseMusicPlaylist("""{"kind":"playlist","playlist_id":"abc"}"""))
        assertNull(MessageContentParser.parseMusicPlaylist("""{"kind":"playlist","playlist_id":"0x10"}"""))
        assertNull(MessageContentParser.parseMusicPlaylist("""{"name":"没有 id","tracks":[]}"""))
        // playlist_id 为真值时官方不再校验 kind / tracks，仍解析为歌单
        assertNotNull(MessageContentParser.parseMusicPlaylist("""{"playlist_id":"1","kind":"song"}"""))
    }

    @Test
    fun `歌单解析不会把歌曲误判成歌单`() {
        assertNull(MessageContentParser.parseMusicPlaylist("""{"song_id":"1","name":"单曲"}"""))
        assertNull(MessageContentParser.parseMusic("""{"kind":"playlist","playlist_id":"1"}"""))
    }

    @Test
    fun `歌单摘要文案`() {
        val playlist = MessageContentParser.parseMusicPlaylist(
            """{"kind":"playlist","playlist_id":"1","name":"深夜电台"}"""
        )
        assertEquals("[歌单] 《深夜电台》", MessageContentParser.musicPlaylistPreview(playlist))
        assertEquals("[歌单]", MessageContentParser.musicPlaylistPreview(null))

        val longName = MessageContentParser.parseMusicPlaylist(
            """{"kind":"playlist","playlist_id":"1","name":"长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长长"}"""
        )
        val preview = MessageContentParser.musicPlaylistPreview(longName)
        assertTrue(preview.startsWith("[歌单] 《"))
        assertTrue(preview.endsWith("…"))
    }

    // ==================== sticker ====================

    @Test
    fun `表情包有 image_url 时直接使用`() {
        val ref = MessageContentParser.resolveSticker(
            imageUrl = " https://cdn.example.com/a.png ",
            content = "12",
            lookup = { "https://cdn.example.com/from-list.png" }
        )
        assertEquals("https://cdn.example.com/a.png", ref.url)
        assertEquals(12L, ref.assetId)
        assertTrue(ref.hasImage)
    }

    @Test
    fun `表情包无 image_url 时按 asset_id 查列表`() {
        val ref = MessageContentParser.resolveSticker(
            imageUrl = null,
            content = "12",
            lookup = { assetId -> if (assetId == 12L) "https://cdn.example.com/from-list.png" else "" }
        )
        assertEquals("https://cdn.example.com/from-list.png", ref.url)
        assertEquals(12L, ref.assetId)
    }

    @Test
    fun `表情包无 image_url 且列表未命中时降级为空地址`() {
        val ref = MessageContentParser.resolveSticker(
            imageUrl = "   ",
            content = "99",
            lookup = { "" }
        )
        assertEquals("", ref.url)
        assertEquals(99L, ref.assetId)
        assertTrue(!ref.hasImage)
    }

    @Test
    fun `表情包 content 为数字`() {
        assertEquals(123L, MessageContentParser.parseStickerAssetId("123"))
        assertEquals(123L, MessageContentParser.parseStickerAssetId(" 123 "))
        assertEquals(12L, MessageContentParser.parseStickerAssetId("12abc"))
    }

    @Test
    fun `表情包 content 为 JSON`() {
        assertEquals(456L, MessageContentParser.parseStickerAssetId("""{"asset_id":456}"""))
        assertEquals(789L, MessageContentParser.parseStickerAssetId("""{"id":789}"""))
        assertEquals(5L, MessageContentParser.parseStickerAssetId("""{"asset_id":"5"}"""))
        assertNull(MessageContentParser.parseStickerAssetId("""{"asset_id":0}"""))
        assertNull(MessageContentParser.parseStickerAssetId("""{"foo":"bar"}"""))
    }

    @Test
    fun `表情包 content 非法时返回 null`() {
        assertNull(MessageContentParser.parseStickerAssetId(null))
        assertNull(MessageContentParser.parseStickerAssetId(""))
        assertNull(MessageContentParser.parseStickerAssetId("abc"))
        assertNull(MessageContentParser.parseStickerAssetId("0"))
        assertNull(MessageContentParser.parseStickerAssetId("-5"))
        assertNull(MessageContentParser.parseStickerAssetId("{"))
    }

    @Test
    fun `表情包 content 非法且无 image_url 时地址为空`() {
        val ref = MessageContentParser.resolveSticker(imageUrl = null, content = "abc")
        assertEquals(0L, ref.assetId)
        assertEquals("", ref.url)
    }
}
