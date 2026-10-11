package top.lanxint.zerotalk.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * OSS 样式拼接的回归测试
 *
 * 官方 `ossImageUrl` 模块导出 5 个样式（thumb / chat / emoji / public / punishment_appeal），
 * 其中表情包（`ChatEmojiPanel` 网格与聊天里 `type:"sticker"` 的消息图）统一用 `style/zerotalk_emoji`。
 * 客户端此前只有 thumb / chat，表情包只能退回 thumb，属于样式错配。
 */
class NetworkImageUrlOssStyleTest {

    private val ossHost = "https://zt-test.oss-cn-hangzhou.aliyuncs.com"

    @Test
    fun `表情包使用 zerotalk_emoji 样式`() {
        assertEquals("style/zerotalk_emoji", NetworkImageUrl.OssStyle.Emoji.value)
    }

    @Test
    fun `原有样式值保持不变`() {
        assertEquals("style/zerotalk_thumb", NetworkImageUrl.OssStyle.Thumb.value)
        assertEquals("style/zerotalk_chat", NetworkImageUrl.OssStyle.Chat.value)
    }

    @Test
    fun `OSS 地址会追加上表情样式`() {
        val styled = NetworkImageUrl.applyOssStyle(
            "$ossHost/stickers/a.png",
            NetworkImageUrl.OssStyle.Emoji
        )
        assertEquals("$ossHost/stickers/a.png?x-oss-process=style/zerotalk_emoji", styled)
    }

    @Test
    fun `已带同款样式时不重复追加`() {
        val already = "$ossHost/stickers/a.png?x-oss-process=style/zerotalk_emoji"
        assertEquals(already, NetworkImageUrl.applyOssStyle(already, NetworkImageUrl.OssStyle.Emoji))
    }

    @Test
    fun `换成别的样式时替换掉旧的 x-oss-process`() {
        val styled = NetworkImageUrl.applyOssStyle(
            "$ossHost/stickers/a.png?x-oss-process=style/zerotalk_thumb",
            NetworkImageUrl.OssStyle.Emoji
        )
        assertEquals("$ossHost/stickers/a.png?x-oss-process=style/zerotalk_emoji", styled)
        assertTrue(styled.count { it == '?' } == 1, "不应产生第二个问号")
    }

    @Test
    fun `保留原有查询参数并追加样式`() {
        val styled = NetworkImageUrl.applyOssStyle(
            "$ossHost/stickers/a.png?v=2",
            NetworkImageUrl.OssStyle.Emoji
        )
        assertEquals("$ossHost/stickers/a.png?v=2&x-oss-process=style/zerotalk_emoji", styled)
    }

    @Test
    fun `非 OSS 地址原样返回`() {
        val cdn = "https://cdn.example.com/stickers/a.png"
        assertEquals(cdn, NetworkImageUrl.applyOssStyle(cdn, NetworkImageUrl.OssStyle.Emoji))
    }

    @Test
    fun `OSS 地址上的 hash 会被保留在末尾`() {
        val styled = NetworkImageUrl.applyOssStyle(
            "$ossHost/stickers/a.png#frag",
            NetworkImageUrl.OssStyle.Emoji
        )
        assertTrue(styled.endsWith("#frag"), "实际: $styled")
        assertTrue(styled.contains("x-oss-process=style/zerotalk_emoji"), "实际: $styled")
    }

    // ---- 候选地址顺序（样式缺失时逐级回退，避免网格整格空白）----

    @Test
    fun `表情包候选顺序为 emoji 到 thumb 到原图`() {
        val candidates = NetworkImageUrl.buildCandidates(
            raw = "$ossHost/stickers/a.png",
            style = NetworkImageUrl.OssStyle.Emoji
        )
        assertEquals(
            listOf(
                "$ossHost/stickers/a.png?x-oss-process=style/zerotalk_emoji",
                "$ossHost/stickers/a.png?x-oss-process=style/zerotalk_thumb",
                "$ossHost/stickers/a.png"
            ),
            candidates
        )
    }

    @Test
    fun `默认样式候选不重复追加 thumb`() {
        val candidates = NetworkImageUrl.buildCandidates(
            raw = "$ossHost/stickers/a.png",
            style = NetworkImageUrl.OssStyle.Thumb
        )
        assertEquals(
            listOf(
                "$ossHost/stickers/a.png?x-oss-process=style/zerotalk_thumb",
                "$ossHost/stickers/a.png"
            ),
            candidates
        )
    }

    @Test
    fun `大图场景优先原图并把样式作为兜底`() {
        val candidates = NetworkImageUrl.buildCandidates(
            raw = "$ossHost/stickers/a.png",
            style = NetworkImageUrl.OssStyle.Emoji,
            preferOriginal = true
        )
        assertEquals(
            listOf(
                "$ossHost/stickers/a.png",
                "$ossHost/stickers/a.png?x-oss-process=style/zerotalk_emoji"
            ),
            candidates
        )
    }

    @Test
    fun `非 OSS 地址候选只有自身`() {
        val cdn = "https://cdn.example.com/stickers/a.png"
        assertEquals(
            listOf(cdn),
            NetworkImageUrl.buildCandidates(cdn, NetworkImageUrl.OssStyle.Emoji)
        )
    }

    @Test
    fun `空地址没有候选`() {
        assertTrue(NetworkImageUrl.buildCandidates("   ").isEmpty())
    }

    @Test
    fun `相对路径会被补全后再拼样式`() {
        val candidates = NetworkImageUrl.buildCandidates(
            raw = "/uploads/stickers/a.png",
            style = NetworkImageUrl.OssStyle.Emoji
        )
        // 站内相对路径不是 OSS，不应追加样式
        assertEquals(listOf("${ZeroTalkApiService.BASE_URL}/uploads/stickers/a.png"), candidates)
    }
}
