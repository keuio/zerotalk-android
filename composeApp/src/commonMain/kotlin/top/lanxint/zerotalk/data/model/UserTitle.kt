package top.lanxint.zerotalk.data.model

/**
 * 用户称号（官方 `UserTitleBadge-D0s2wMyr.js`）的纯逻辑部分。
 *
 * 官方实现：
 * ```js
 * const COLORS = ["blue","cyan","green","lime","orange","amber","red","pink","purple","violet","gray","gold"]
 * const d = (t) => { const e = String(t || "").toLowerCase(); return COLORS.includes(e) ? e : "blue" }
 * const o = computed(() => { const r = String(props.title || "").trim(); return r ? r.slice(0, 8) : "" })
 * // 渲染：o.value ? <span class="user-title user-title--{d(color)}" title="{o.value}">{o.value}</span> : null
 * ```
 *
 * 即：称号先去首尾空白，空串则**整个徽章不渲染**；渲染文案截断到 8 个字符；
 * 颜色大小写不敏感，白名单之外（含缺失）一律回落 `blue`。
 *
 * 与 UI 解耦成纯函数，便于单元测试覆盖（颜色回落 / 截断规则）。
 */

/** 称号展示的最大字符数（官方 `slice(0, 8)`） */
const val USER_TITLE_MAX_CHARS: Int = 8

/** 官方颜色白名单（顺序与官方一致） */
val USER_TITLE_COLOR_KEYS: List<String> = listOf(
    "blue", "cyan", "green", "lime", "orange", "amber",
    "red", "pink", "purple", "violet", "gray", "gold"
)

/** 颜色缺失 / 非法时的回落值（官方 `"blue"`） */
const val USER_TITLE_FALLBACK_COLOR: String = "blue"

/**
 * 归一化称号展示文案：去首尾空白，超过 [USER_TITLE_MAX_CHARS] 个字符时截断。
 *
 * @return 空串表示「服务端未下发称号」，调用方不应渲染任何占位。
 */
fun normalizeUserTitle(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    return if (trimmed.length <= USER_TITLE_MAX_CHARS) trimmed else trimmed.substring(0, USER_TITLE_MAX_CHARS)
}

/**
 * 归一化称号颜色 key：大小写不敏感，白名单之外（含 null / 空串）回落 [USER_TITLE_FALLBACK_COLOR]。
 */
fun normalizeUserTitleColor(raw: String?): String {
    val key = raw?.trim()?.lowercase().orEmpty()
    return if (key in USER_TITLE_COLOR_KEYS) key else USER_TITLE_FALLBACK_COLOR
}
