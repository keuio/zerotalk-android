package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [ChatNewerCatchUp] 的回归测试（官方 `te` 的页循环停止条件）。
 *
 * 官方：`if (!Fe || Fe <= se || (se = Fe, !$e.has_more)) break;`
 * 即：本页无服务端 id、或本页最大 id 不大于游标、或 `has_more` 为 false 时停止。
 */
class ChatNewerCatchUpTest {

    @Test
    fun `正常前进返回本页最大 id`() {
        assertEquals(250L, ChatNewerCatchUp.nextCursor(cursor = 200L, pageMaxId = 250L, hasMore = true))
    }

    @Test
    fun `本页没有服务端 id 时停止`() {
        assertNull(ChatNewerCatchUp.nextCursor(cursor = 200L, pageMaxId = null, hasMore = true))
    }

    @Test
    fun `本页最大 id 不大于游标时停止（避免死循环）`() {
        assertNull(ChatNewerCatchUp.nextCursor(cursor = 200L, pageMaxId = 200L, hasMore = true))
        assertNull(ChatNewerCatchUp.nextCursor(cursor = 200L, pageMaxId = 150L, hasMore = true))
    }

    @Test
    fun `has_more 为 false 时停止`() {
        assertNull(ChatNewerCatchUp.nextCursor(cursor = 200L, pageMaxId = 250L, hasMore = false))
    }

    @Test
    fun `页数上限与每页条数对齐官方`() {
        assertEquals(5, ChatNewerCatchUp.MAX_PAGES)
        assertEquals(50, ChatNewerCatchUp.PAGE_SIZE)
    }
}
