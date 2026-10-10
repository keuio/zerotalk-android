package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [normalizeUserTitle] / [normalizeUserTitleColor] 回归测试。
 *
 * 对齐官方 `UserTitleBadge-D0s2wMyr.js`：
 * ```js
 * const o = computed(() => { const r = String(props.title || "").trim(); return r ? r.slice(0, 8) : "" })
 * const d = (t) => { const e = String(t || "").toLowerCase(); return COLORS.includes(e) ? e : "blue" }
 * ```
 */
class UserTitleTest {

    // ---- 文案：去空白 / 空串不渲染 / 截断 8 字 ----

    @Test
    fun blankTitleNormalizesToEmpty() {
        assertEquals("", normalizeUserTitle(null))
        assertEquals("", normalizeUserTitle(""))
        assertEquals("", normalizeUserTitle("   "))
        assertEquals("", normalizeUserTitle("\t\n "))
    }

    @Test
    fun titleIsTrimmed() {
        assertEquals("银之钥", normalizeUserTitle("  银之钥  "))
        assertEquals("公共房间审查员", normalizeUserTitle("\n公共房间审查员\t"))
    }

    @Test
    fun titleIsTruncatedToEightChars() {
        // 6 字：不截断
        assertEquals("公共房间审查员", normalizeUserTitle("公共房间审查员"))
        // 9 字：截到 8 字
        assertEquals("一二三四五六七八", normalizeUserTitle("一二三四五六七八九"))
        // 先 trim 再截断：前导空白不计入长度
        assertEquals("一二三四五六七八", normalizeUserTitle("   一二三四五六七八九"))
        assertEquals(8, normalizeUserTitle("abcdefghij").length)
        assertEquals("abcdefgh", normalizeUserTitle("abcdefghij"))
    }

    @Test
    fun exactlyEightCharsIsKept() {
        val eight = "12345678"
        assertEquals(eight, normalizeUserTitle(eight))
    }

    // ---- 颜色：白名单 / 大小写不敏感 / 非法回落 blue ----

    @Test
    fun allWhitelistColorsAreAccepted() {
        val expected = listOf(
            "blue", "cyan", "green", "lime", "orange", "amber",
            "red", "pink", "purple", "violet", "gray", "gold"
        )
        assertEquals(expected, USER_TITLE_COLOR_KEYS)
        expected.forEach { key ->
            assertEquals(key, normalizeUserTitleColor(key))
        }
    }

    @Test
    fun colorIsCaseInsensitive() {
        assertEquals("red", normalizeUserTitleColor("RED"))
        assertEquals("gold", normalizeUserTitleColor("Gold"))
        assertEquals("violet", normalizeUserTitleColor("VIOLET"))
    }

    @Test
    fun unknownOrMissingColorFallsBackToBlue() {
        assertEquals("blue", normalizeUserTitleColor(null))
        assertEquals("blue", normalizeUserTitleColor(""))
        assertEquals("blue", normalizeUserTitleColor("   "))
        assertEquals("blue", normalizeUserTitleColor("crimson"))
        assertEquals("blue", normalizeUserTitleColor("#ff0000"))
        assertEquals("blue", normalizeUserTitleColor("蓝"))
    }

    @Test
    fun fallbackColorIsInWhitelist() {
        assertTrue(USER_TITLE_FALLBACK_COLOR in USER_TITLE_COLOR_KEYS)
    }
}
