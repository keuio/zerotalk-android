package top.lanxint.zerotalk.data.network

/**
 * 图片地址归一化 + OSS 缩略图样式
 *
 * 零语接口返回的图片/头像地址可能是以下任意形态，展示前必须统一成绝对地址：
 * - 绝对地址：`https://xxx.oss-cn-xxx.aliyuncs.com/a.jpg` 或 CDN 地址
 * - 协议相对地址：`//host/a.jpg`
 * - 站内绝对路径：`/uploads/a.jpg`
 * - 无前导斜杠的相对路径：`uploads/a.jpg`
 *
 * 另外，官方 web 端（`ossImageUrl` 模块）会给阿里云 OSS 地址追加 `x-oss-process` 样式参数取缩略图，
 * 动态配图若直接拉原图（手机原图数 MB / 数千像素）会解码极慢甚至 OOM，表现为「图片一直不显示」。
 */
object NetworkImageUrl {

    /** 官方前端使用的 OSS 图片样式 */
    enum class OssStyle(val value: String) {
        /** 列表 / 卡片 / 头像缩略图（style/zerotalk_thumb） */
        Thumb("style/zerotalk_thumb"),

        /** 聊天图片（style/zerotalk_chat） */
        Chat("style/zerotalk_chat"),

        /**
         * 表情包 / Emoji（style/zerotalk_emoji）
         *
         * 官方 `ChatEmojiPanel` 的表情与表情包统一走这个样式，
         * 用 `Thumb` 代替会拉到大尺寸缩略图，网格里清晰度与体积都不对。
         */
        Emoji("style/zerotalk_emoji")
    }

    private const val OSS_PROCESS_PARAM = "x-oss-process"

    fun resolve(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> ""
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> "${ZeroTalkApiService.BASE_URL}$trimmed"
            else -> "${ZeroTalkApiService.BASE_URL}/$trimmed"
        }
    }

    /**
     * 归一化地址，并在地址确属阿里云 OSS 时追加缩略图样式（与官方前端行为一致）
     */
    fun resolveWithStyle(raw: String?, style: OssStyle = OssStyle.Thumb): String =
        applyOssStyle(resolve(raw), style)

    /**
     * 仅对阿里云 OSS 地址追加 `x-oss-process=style/xxx`，其余地址原样返回
     */
    fun applyOssStyle(url: String, style: OssStyle): String {
        if (url.isBlank() || !isAliyunOss(url)) return url
        val styleValue = style.value

        val hashIndex = url.indexOf('#')
        val hash = if (hashIndex >= 0) url.substring(hashIndex) else ""
        val withoutHash = if (hashIndex >= 0) url.substring(0, hashIndex) else url

        val queryIndex = withoutHash.indexOf('?')
        val base = if (queryIndex >= 0) withoutHash.substring(0, queryIndex) else withoutHash
        val query = if (queryIndex >= 0) withoutHash.substring(queryIndex + 1) else ""

        val params = query.split('&').filter { it.isNotBlank() && !it.startsWith("$OSS_PROCESS_PARAM=") }
        if (query.contains("$OSS_PROCESS_PARAM=$styleValue")) return url

        val newQuery = (params + "$OSS_PROCESS_PARAM=$styleValue").joinToString("&")
        return "$base?$newQuery$hash"
    }

    /**
     * 是否为阿里云 OSS 地址（与官方前端判定保持一致）
     */
    private fun isAliyunOss(url: String): Boolean {
        return try {
            val host = java.net.URI(url).host?.lowercase() ?: return false
            if (!host.endsWith(".aliyuncs.com") && !host.endsWith(".aliyun.com")) return false
            host.contains(".oss-") || host.startsWith("oss-") || host.contains("oss-accelerate")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 计算缩略图下载的候选地址顺序（按优先级），供加载器逐个尝试。
     *
     * 官方样式不一定在所有 OSS bucket 上都配置好，样式缺失时 OSS 会返回失败；
     * 而原图又常被防盗链 403。因此非原图加载按
     * `目标样式 → style/zerotalk_thumb → 原图` 逐级回退，避免出现整格空白。
     *
     * 非 OSS 地址不加样式，只返回归一化后的地址本身。
     *
     * @param raw 原始地址（可为相对路径）
     * @param style 期望的样式
     * @param preferOriginal true 表示大图查看场景，优先原图、样式作兜底
     */
    fun buildCandidates(
        raw: String,
        style: OssStyle = OssStyle.Thumb,
        preferOriginal: Boolean = false
    ): List<String> {
        val plainUrl = resolve(raw)
        if (plainUrl.isEmpty()) return emptyList()
        val styledUrl = applyOssStyle(plainUrl, style)
        if (styledUrl == plainUrl) return listOf(plainUrl)

        return if (preferOriginal) {
            listOf(plainUrl, styledUrl).distinct()
        } else {
            buildList {
                add(styledUrl)
                if (style != OssStyle.Thumb) add(applyOssStyle(plainUrl, OssStyle.Thumb))
                add(plainUrl)
            }.distinct()
        }
    }
}
