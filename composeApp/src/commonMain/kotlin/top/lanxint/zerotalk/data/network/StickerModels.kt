package top.lanxint.zerotalk.data.network

import com.google.gson.annotations.SerializedName

/**
 * 表情包列表项（GET /api/sticker/list 的 `items[]`）
 *
 * 官方 `sticker` store 用 `asset_id` 匹配消息 content 里的 asset_id，
 * 取 `url` 作为表情包图片地址。
 */
data class StickerItemDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("asset_id") val assetId: Long = 0,
    @SerializedName("url") val url: String = ""
)

/**
 * 表情包列表返回（GET /api/sticker/list）
 *
 * @param items 表情包条目
 * @param used 已使用数量（官方 `used`）
 * @param quota 配额（官方 `quota`，缺省 / 非法时官方回落到 15）
 */
data class StickerListData(
    @SerializedName("items") val items: List<StickerItemDto>? = null,
    @SerializedName("used") val used: Int = 0,
    @SerializedName("quota") val quota: Int = 0
) {
    val entries: List<StickerItemDto> get() = items.orEmpty()
}

/** 官方 `sticker` store 的默认配额（`quota` 缺省或为 0 时回落值） */
const val STICKER_DEFAULT_QUOTA = 15

/**
 * 对齐官方 `sticker` store 的 `used` 归一化（官方 `Math.max(0, Number(k.used ?? e.value.length))`）
 *
 * 服务端未下发 `used` 时 Gson 落到默认 0，此时用列表长度兜底，
 * 避免「已添加但 `used` 显示 0」的错位。
 */
fun StickerListData.normalizedUsed(): Int =
    used.takeIf { it > 0 } ?: entries.size.coerceAtLeast(0)

/** 归一化后的配额（恒 >= 1） */
fun StickerListData.normalizedQuota(): Int =
    quota.coerceAtLeast(0).takeIf { it > 0 } ?: STICKER_DEFAULT_QUOTA

/**
 * 网络搜索表情包条目（GET /api/sticker/search 的 `items[]`）
 *
 * 官方搜索返回的是图片直链（没有 asset_id），发送前必须先
 * `POST /api/sticker/prepare-url` 换回 asset_id。
 */
data class StickerSearchItemDto(
    @SerializedName("url") val url: String = "",
    @SerializedName("type") val type: String = ""
)

/**
 * 表情包搜索返回（GET /api/sticker/search?keyword=&page=）
 *
 * @param items 搜索结果（官方只保留 `url` 非空的条目）
 * @param page 服务端回显的页码，缺省回落为请求页码
 * @param hasMore 是否还有下一页（官方 `has_more`）
 */
data class StickerSearchData(
    @SerializedName("items") val items: List<StickerSearchItemDto>? = null,
    @SerializedName("page") val page: Int = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
) {
    val entries: List<StickerSearchItemDto>
        get() = items.orEmpty().filter { it.url.isNotBlank() }
}

/**
 * `POST /api/sticker/prepare-url` 返回：把网络表情图片地址换成可发送的 asset 信息
 */
data class StickerPrepareUrlData(
    @SerializedName("asset_id") val assetId: Long = 0,
    @SerializedName("url") val url: String = ""
)

/**
 * `POST /api/sticker/upload` / `POST /api/sticker/commit` 返回的图片地址
 */
data class StickerUploadData(
    @SerializedName("url") val url: String = ""
)
