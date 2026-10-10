package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 表情包配额与搜索结果解析的回归测试（对齐官方 `sticker` store）
 *
 * 官方口径：
 * - `used = Math.max(0, Number(k.used ?? items.length))`
 * - `quota = Math.max(0, Number(k.quota ?? 15)) || 15`
 * - 搜索结果只保留 `url` 非空的条目
 */
class StickerModelsTest {

    private val gson = Gson()

    @Test
    fun `quota 缺省时回落到 15`() {
        val data = StickerListData(quota = 0)
        assertEquals(15, data.normalizedQuota())
    }

    @Test
    fun `quota 为负数时回落到 15`() {
        val data = StickerListData(quota = -3)
        assertEquals(15, data.normalizedQuota())
    }

    @Test
    fun `quota 有效时原样保留`() {
        val data = StickerListData(quota = 30)
        assertEquals(30, data.normalizedQuota())
    }

    @Test
    fun `used 为负数时归零`() {
        val data = StickerListData(used = -5)
        assertEquals(0, data.normalizedUsed())
    }

    @Test
    fun `used 有效时原样保留`() {
        val data = StickerListData(used = 7)
        assertEquals(7, data.normalizedUsed())
    }

    @Test
    fun `服务端未下发 used 时用列表长度兜底`() {
        val json = """
            {"items":[{"id":1,"asset_id":101,"url":"https://cdn.example/a.png"},
                      {"id":2,"asset_id":102,"url":"https://cdn.example/b.png"}]}
        """.trimIndent()
        val data = gson.fromJson(json, StickerListData::class.java)
        assertEquals(2, data.normalizedUsed())
    }

    @Test
    fun `列表缺少 items 时 entries 为空`() {
        val data = gson.fromJson("""{"used":2,"quota":15}""", StickerListData::class.java)
        assertTrue(data.entries.isEmpty())
        assertEquals(2, data.normalizedUsed())
        assertEquals(15, data.normalizedQuota())
    }

    @Test
    fun `条目按 asset_id 与 url 解析`() {
        val json = """
            {"items":[{"id":11,"asset_id":22,"url":"https://cdn.example/a.png"}],
             "used":1,"quota":15}
        """.trimIndent()
        val data = gson.fromJson(json, StickerListData::class.java)
        val item = data.entries.single()
        assertEquals(11L, item.id)
        assertEquals(22L, item.assetId)
        assertEquals("https://cdn.example/a.png", item.url)
    }

    @Test
    fun `搜索结果过滤掉 url 为空的条目`() {
        val json = """
            {"items":[{"url":"https://cdn.example/a.png","type":"png"},
                      {"url":"","type":"png"},
                      {"url":"https://cdn.example/b.webp","type":"webp"}],
             "page":1,"has_more":true}
        """.trimIndent()
        val data = gson.fromJson(json, StickerSearchData::class.java)
        assertEquals(2, data.entries.size)
        assertEquals("https://cdn.example/a.png", data.entries[0].url)
        assertEquals("https://cdn.example/b.webp", data.entries[1].url)
        assertTrue(data.hasMore)
        assertEquals(1, data.page)
    }

    @Test
    fun `搜索结果缺少 has_more 时视为没有下一页`() {
        val data = gson.fromJson("""{"items":[]}""", StickerSearchData::class.java)
        assertFalse(data.hasMore)
        assertTrue(data.entries.isEmpty())
    }

    @Test
    fun `prepare-url 解析 asset_id 与最终地址`() {
        val json = """{"asset_id":99,"url":"https://cdn.example/prepared.png"}"""
        val data = gson.fromJson(json, StickerPrepareUrlData::class.java)
        assertEquals(99L, data.assetId)
        assertEquals("https://cdn.example/prepared.png", data.url)
    }

    @Test
    fun `quota 与 used 相等时属于满额`() {
        val data = StickerListData(used = 15, quota = 15)
        assertTrue(data.normalizedUsed() >= data.normalizedQuota())
    }
}
