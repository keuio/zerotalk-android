package top.lanxint.zerotalk.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 表情包面板阶段判定的回归测试
 *
 * 背景：面板曾经用 `searchResults.isEmpty()` 直接判定「没有找到相关表情」，
 * 但非搜索态下搜索结果恒为空，于是「我的表情包」网格、加载骨架、空态全部成为死代码，
 * 表现就是「收藏的表情怎么都显示不出来」。这里锁死官方 `ChatEmojiPanel` 的分支语义。
 */
class StickerPanelStageTest {

    @Test
    fun `未搜索且已有收藏时进入我的表情包网格`() {
        // 这正是此前被误判成「没有找到相关表情」的场景
        val stage = resolveStickerPanelStage(
            searching = false,
            searched = false,
            searchResultCount = 0,
            loading = false,
            mineCount = 3
        )
        assertEquals(StickerPanelStage.MINE, stage)
    }

    @Test
    fun `未搜索且正在加载时显示加载中而不是网格`() {
        val stage = resolveStickerPanelStage(
            searching = false,
            searched = false,
            searchResultCount = 0,
            loading = true,
            mineCount = 0
        )
        assertEquals(StickerPanelStage.LOADING, stage)
    }

    @Test
    fun `未搜索且收藏为空时进入空态`() {
        val stage = resolveStickerPanelStage(
            searching = false,
            searched = false,
            searchResultCount = 0,
            loading = false,
            mineCount = 0
        )
        // EMPTY 仍会渲染「我的表情包」网格（含首个「+」上传格），
        // 只是在数据语义上与 MINE 区分；不能退回空态文案。
        assertEquals(StickerPanelStage.EMPTY, stage)
    }

    @Test
    fun `加载失败且无收藏时进入失败态`() {
        val stage = resolveStickerPanelStage(
            searching = false,
            searched = false,
            searchResultCount = 0,
            loading = false,
            mineCount = 0,
            mineFailed = true
        )
        assertEquals(StickerPanelStage.ERROR, stage)
    }

    @Test
    fun `失败但仍有上次成功结果时继续显示网格`() {
        val stage = resolveStickerPanelStage(
            searching = false,
            searched = false,
            searchResultCount = 0,
            loading = false,
            mineCount = 2,
            mineFailed = true
        )
        assertEquals(StickerPanelStage.MINE, stage)
    }

    @Test
    fun `搜索中且暂无结果时显示骨架`() {
        val stage = resolveStickerPanelStage(
            searching = true,
            searched = true,
            searchResultCount = 0,
            loading = false,
            mineCount = 5
        )
        assertEquals(StickerPanelStage.SEARCH_SKELETON, stage)
    }

    @Test
    fun `搜索结束且无结果时提示没有找到相关表情`() {
        val stage = resolveStickerPanelStage(
            searching = false,
            searched = true,
            searchResultCount = 0,
            loading = false,
            mineCount = 5
        )
        assertEquals(StickerPanelStage.SEARCH_EMPTY, stage)
    }

    @Test
    fun `搜索有结果时优先展示搜索结果`() {
        val stage = resolveStickerPanelStage(
            searching = false,
            searched = true,
            searchResultCount = 4,
            loading = false,
            mineCount = 5
        )
        assertEquals(StickerPanelStage.SEARCH_RESULTS, stage)
    }

    @Test
    fun `搜索态下我的表情包为空也仍然显示搜索结果`() {
        // 搜索态与「我的表情包」互不影响：收藏为空不能把搜索结果挡掉
        val stage = resolveStickerPanelStage(
            searching = false,
            searched = true,
            searchResultCount = 2,
            loading = false,
            mineCount = 0
        )
        assertEquals(StickerPanelStage.SEARCH_RESULTS, stage)
    }

    @Test
    fun `搜索中即使已有翻页结果也保持搜索结果网格`() {
        // 翻页搜索（append）期间 searching 为 false；首页搜索但已有缓存结果时不应回到骨架
        val stage = resolveStickerPanelStage(
            searching = true,
            searched = true,
            searchResultCount = 8,
            loading = false,
            mineCount = 5
        )
        assertEquals(StickerPanelStage.SEARCH_RESULTS, stage)
    }

    @Test
    fun `未搜索时搜索结果为空不影响我的表情包加载态`() {
        val stage = resolveStickerPanelStage(
            searching = false,
            searched = false,
            searchResultCount = 0,
            loading = true,
            mineCount = 0,
            mineFailed = true
        )
        // 加载中的优先级高于失败标记：先看「加载中」，避免闪烁失败文案
        assertEquals(StickerPanelStage.LOADING, stage)
    }
}
