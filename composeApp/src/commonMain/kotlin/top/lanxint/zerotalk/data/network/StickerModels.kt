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
 * @param quota 配额（官方 `quota`）
 */
data class StickerListData(
    @SerializedName("items") val items: List<StickerItemDto>? = null,
    @SerializedName("used") val used: Int = 0,
    @SerializedName("quota") val quota: Int = 0
) {
    val entries: List<StickerItemDto> get() = items.orEmpty()
}
