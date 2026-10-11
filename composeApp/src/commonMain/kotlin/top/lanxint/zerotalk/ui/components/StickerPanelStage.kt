package top.lanxint.zerotalk.ui.components

/**
 * 表情包面板当前应展示的阶段
 *
 * 从 [StickerPanel] 的渲染分支中抽出为纯函数，便于回归测试：
 * 曾经因为「搜索无结果」与「我的表情包为空」两个判定混淆，
 * 导致非搜索态下永远命中「没有找到相关表情」，「我的表情包」网格成为死代码。
 */
internal enum class StickerPanelStage {
    /** 搜索中且暂无结果 → 骨架屏 */
    SEARCH_SKELETON,

    /** 已搜索且无结果（含搜索失败）→「没有找到相关表情」/ 错误文案 */
    SEARCH_EMPTY,

    /** 已搜索且有结果 → 搜索结果网格 */
    SEARCH_RESULTS,

    /** 「我的表情包」加载中且暂无内容 →「表情包加载中…」 */
    LOADING,

    /** 「我的表情包」为空 → 空态（可重试） */
    EMPTY,

    /** 「我的表情包」加载失败 → 失败态（可重试，区别于「收藏为空」） */
    ERROR,

    /** 正常态 →「我的表情包」网格（首格为「+」上传格） */
    MINE
}

/**
 * 依据面板入参判定当前阶段。
 *
 * 关键点：**搜索结果为空**只有发生在「搜索态」时才代表没有结果；
 * 非搜索态下搜索结果恒为空，不能据此判定，否则「我的表情包」永远渲染不出来。
 *
 * @param searching 首页搜索进行中（官方 `searching`）
 * @param searched 已进入搜索态（搜索关键词非空）
 * @param searchResultCount 当前累计搜索结果条数
 * @param loading 「我的表情包」是否加载中
 * @param mineCount 「我的表情包」已有条数
 * @param mineFailed 「我的表情包」是否加载失败（服务端返回 null / 请求异常）
 */
internal fun resolveStickerPanelStage(
    searching: Boolean,
    searched: Boolean,
    searchResultCount: Int,
    loading: Boolean,
    mineCount: Int,
    mineFailed: Boolean = false
): StickerPanelStage = when {
    searching && searchResultCount == 0 -> StickerPanelStage.SEARCH_SKELETON
    searched && searchResultCount == 0 -> StickerPanelStage.SEARCH_EMPTY
    searched -> StickerPanelStage.SEARCH_RESULTS
    loading && mineCount == 0 -> StickerPanelStage.LOADING
    mineFailed && mineCount == 0 -> StickerPanelStage.ERROR
    mineCount == 0 -> StickerPanelStage.EMPTY
    else -> StickerPanelStage.MINE
}
