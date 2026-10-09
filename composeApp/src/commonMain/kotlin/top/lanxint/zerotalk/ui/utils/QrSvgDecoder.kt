package top.lanxint.zerotalk.ui.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 网易云登录二维码（服务端下发 SVG data URI）解析与绘制
 *
 * 实测 `POST /api/music/netease/binding/qrcode/start` 返回的 `qr_image` 形如：
 * ```
 * qr_image: "data:image/svg+xml;base64,PHN2Zy..."
 * ```
 * 也就是**二维码图片本身就是 SVG**，`BitmapFactory` 无法解码（这也是之前一直显示占位框的原因）；
 * 工程内也没有 SVG 渲染依赖。而二维码 SVG 的本质就是「一块块方格的矩阵」，
 * 因此这里直接解析出方格矩阵，用 Compose Canvas 画出来：不新增任何依赖，也不需要在本地生成二维码。
 *
 * 兼容两种常见 SVG 结构：
 * 1) `<rect x y width height>` 逐格描述（qrious 等）；
 * 2) `<path stroke="#000" d="M4 4.5h7m2 0h1...">` 用水平线段描述（qrcode 等）。
 */
data class QrModule(val x: Float, val y: Float, val w: Float, val h: Float)

/** 二维码 SVG 解析结果：模块矩阵边长 + 需要填色的方块 */
data class QrSvgSpec(val modules: Float, val rects: List<QrModule>)

private val VIEW_BOX = Regex("""viewBox\s*=\s*["']([^"']+)["']""")
private val WIDTH_ATTR = Regex("""\bwidth\s*=\s*["']([\d.]+)""")
private val RECT_TAG = Regex("""<rect\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
private val PATH_TAG = Regex("""<path\b([^>]*)>""", RegexOption.IGNORE_CASE)
private val ATTR = Regex("""([a-zA-Z-]+)\s*=\s*["']([^"']*)["']""")
private val PATH_TOKEN = Regex("""([MmLlHhVvZz])|(-?\d*\.?\d+)""")

/** 解析 `data:image/svg+xml;base64,...`（也兼容非 base64 与 URL 编码形态），失败返回 null */
fun parseQrSvgDataUri(dataUri: String): QrSvgSpec? {
    val raw = dataUri.trim()
    if (raw.isEmpty()) return null
    val svg = when {
        raw.startsWith("data:", ignoreCase = true) -> {
            val meta = raw.substringAfter("data:", "").substringBefore(',')
            val payload = raw.substringAfter(',', "")
            if (payload.isBlank()) return null
            if (meta.contains("base64", ignoreCase = true)) decodeBase64Text(payload) else urlDecode(payload)
        }
        // 直接给了 SVG 文本
        raw.startsWith("<svg", ignoreCase = true) -> raw
        else -> null
    } ?: return null
    if (!svg.contains("<svg", ignoreCase = true)) return null
    return parseQrSvg(svg)
}

/** 解析 SVG 文本为二维码矩阵 */
fun parseQrSvg(svg: String): QrSvgSpec? {
    val modules = run {
        VIEW_BOX.find(svg)?.groupValues?.getOrNull(1)
            ?.trim()?.split(Regex("""[\s,]+"""))
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size >= 4 }
            ?.let { maxOf(it[2], it[3]) }
            ?: WIDTH_ATTR.find(svg)?.groupValues?.getOrNull(1)?.toFloatOrNull()
            ?: 0f
    }
    if (modules <= 0f) return null

    val rects = mutableListOf<QrModule>()

    // 1) <rect> 逐格描述
    RECT_TAG.findAll(svg).forEach { match ->
        val attrs = attrsOf(match.groupValues[1])
        if (!isDark(attrs)) return@forEach
        val x = attrs["x"]?.toFloatOrNull() ?: 0f
        val y = attrs["y"]?.toFloatOrNull() ?: 0f
        val w = attrs["width"]?.toFloatOrNull() ?: 0f
        val h = attrs["height"]?.toFloatOrNull() ?: 0f
        // 铺满整幅的底色块忽略（白底或整块背景）
        if (w > 0f && h > 0f && (w < modules || h < modules)) {
            rects += QrModule(x, y, w, h)
        }
    }

    // 2) <path> 水平线段描述（qrcode 库默认输出）
    PATH_TAG.findAll(svg).forEach { match ->
        val attrs = attrsOf(match.groupValues[1])
        if (!isDark(attrs)) return@forEach
        val d = attrs["d"] ?: return@forEach
        // 2a) L/l 多边形轮廓 + evenodd 填充（网易云服务端真实下发：
        //     <path fill-rule="evenodd" d="M9 0L9 1L8 1L8 3...Z" fill="#000000"/>，
        //     外层可能还有 <g transform="scale/translate">。用 path 自身坐标网格点采样，
        //     网格数 = 坐标范围，不再依赖 viewBox。）
        if (d.any { it == 'L' || it == 'l' }) {
            parsePathContours(d)?.let { (contourRects, contourModules) ->
                return QrSvgSpec(contourModules, contourRects)
            }
            // L 解析失败则回退下面的线段逻辑
        }
        val strokeWidth = attrs["stroke-width"]?.toFloatOrNull()?.takeIf { it > 0f } ?: 1f
        rects += parsePathRuns(d, strokeWidth)
    }

    if (rects.isEmpty()) return null
    return QrSvgSpec(modules, rects)
}

private fun attrsOf(tagBody: String): Map<String, String> =
    ATTR.findAll(tagBody).associate { it.groupValues[1].lowercase() to it.groupValues[2] }

/**
 * 是否为「深色」图元：白/透明视为底色并忽略
 *
 * 注意：二维码 SVG 的深色 path 常见写法是 `fill="none" stroke="#000"`，
 * 所以有描边时以描边颜色为准，不能再被 `fill="none"` 判成浅色而整段丢掉。
 */
private fun isDark(attrs: Map<String, String>): Boolean {
    val fill = attrs["fill"]?.trim()?.lowercase()
    val stroke = attrs["stroke"]?.trim()?.lowercase()
    fun dark(value: String?): Boolean {
        if (value == null) return false
        if (value == "none" || value == "transparent" || value.isEmpty()) return false
        if (value == "#fff" || value == "#ffffff" || value == "white") return false
        if (value.startsWith("rgba") && value.contains(",0)")) return false
        return true
    }
    if (stroke != null) return if (dark(stroke)) true else dark(fill)
    return dark(fill ?: "black")
}

/**
 * 解析 SVG path 的 `M/m/H/h/V/v/Z/z` 指令，输出等价的矩形集合
 *
 * 二维码 SVG 只用到直线与水平线段，因此无需完整实现 SVG 路径规范：
 * 每个 `h{w}`（或 `v{h}`）就是一条宽度为描边粗细的模块带。
 */
private fun parsePathRuns(d: String, strokeWidth: Float): List<QrModule> {
    val out = mutableListOf<QrModule>()
    var x = 0f
    var y = 0f
    var startX = 0f
    var startY = 0f
    var command = ' '
    var relative = false
    var pending: Float? = null

    fun flush() {
        val value = pending ?: return
        pending = null
        when (command) {
            'h', 'H' -> {
                val target = if (relative) x + value else value
                val left = minOf(x, target)
                val width = kotlin.math.abs(target - x)
                if (width > 0f) out += QrModule(left, y - strokeWidth / 2f, width, strokeWidth)
                x = target
            }
            'v', 'V' -> {
                val target = if (relative) y + value else value
                val top = minOf(y, target)
                val height = kotlin.math.abs(target - y)
                if (height > 0f) out += QrModule(x - strokeWidth / 2f, top, strokeWidth, height)
                y = target
            }
            else -> Unit
        }
    }

    var expectY = false
    PATH_TOKEN.findAll(d).forEach { match ->
        val letter = match.groupValues[1].firstOrNull()
        val number = match.groupValues[2]
        if (letter != null) {
            command = letter
            relative = letter.isLowerCase()
            expectY = false
            pending = null
            if (letter == 'z' || letter == 'Z') {
                x = startX
                y = startY
            }
            return@forEach
        }
        val value = number.toFloatOrNull() ?: return@forEach
        when (command) {
            'M', 'm' -> {
                if (!expectY) {
                    x = if (relative) x + value else value
                    startX = x
                    expectY = true
                } else {
                    y = if (relative) y + value else value
                    startY = y
                    expectY = false
                }
            }
            'h', 'H', 'v', 'V' -> {
                pending = value
                flush()
            }
            else -> Unit
        }
    }
    return out
}

@OptIn(ExperimentalEncodingApi::class)
private fun decodeBase64Text(payload: String): String? = try {
    Base64.decode(payload.filterNot { it.isWhitespace() }).decodeToString()
} catch (_: Exception) {
    null
}

private fun urlDecode(value: String): String {
    if (!value.contains('%')) return value
    val bytes = ArrayList<Byte>(value.length)
    var i = 0
    while (i < value.length) {
        val ch = value[i]
        if (ch == '%' && i + 2 < value.length) {
            val hex = value.substring(i + 1, i + 3).toIntOrNull(16)
            if (hex != null) {
                bytes += hex.toByte()
                i += 3
                continue
            }
        }
        value.substring(i, i + 1).encodeToByteArray().forEach { bytes += it }
        i++
    }
    return bytes.toByteArray().decodeToString()
}

/** path 顶点 */
private data class Pt(val x: Float, val y: Float)

/**
 * 解析 L/l 多边形轮廓 path（`fill-rule="evenodd"` 填充风格），点采样输出模块矩形。
 *
 * 网易云服务端下发的二维码 SVG 是这种结构：
 * ```
 * <g transform="scale(3.860)"><g transform="translate(4.000,4.000)">
 *   <path fill-rule="evenodd" d="M9 0L9 1L8 1L8 3...Z M0 42L1 42L1 43L0 43Z..." fill="#000000"/>
 * </g></g>
 * ```
 * 指令全部是 `M/L/Z`（绝对坐标），描述的是**模块边界多边形轮廓**（非描边线段），
 * 不能用 [parsePathRuns] 的 h/v 描边模型。这里把 d 的本地坐标当作模块网格，
 * 对每个单位格中心做 evenodd 射线判定，落在填充区内的格输出为一个 1x1 模块。
 *
 * 返回 (矩形列表, 网格边长)；无法解析（无 L 指令 / 无子路径 / 无填充格）时返回 null。
 */
private fun parsePathContours(d: String): Pair<List<QrModule>, Float>? {
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE

    val subs = mutableListOf<List<Pt>>()
    var cur = mutableListOf<Pt>()
    var cx = 0f
    var cy = 0f
    var cmd = ' '
    var relative = false
    var needY = false

    fun closeSub() {
        if (cur.size >= 3) subs += cur
        cur = mutableListOf()
    }

    PATH_TOKEN.findAll(d).forEach { match ->
        val letter = match.groupValues[1].firstOrNull()
        val number = match.groupValues[2].toFloatOrNull()
        if (letter != null) {
            cmd = letter
            relative = letter.isLowerCase()
            // M/L 指令后先期待 x，再期待 y（成对消费）
            needY = false
            when (letter) {
                'M', 'm' -> {
                    // 新子路径；绝对 M 会直接用数字覆盖 cx/cy
                    closeSub()
                }
                'Z', 'z' -> {
                    // 闭合：回到子路径起点（Z 后接 m/l 相对坐标以此为准）
                    val start = cur.firstOrNull()
                    closeSub()
                    if (start != null) {
                        cx = start.x
                        cy = start.y
                    }
                }
                else -> Unit
            }
            return@forEach
        }
        if (number == null) return@forEach
        when (cmd) {
            'M', 'm' -> {
                if (!needY) {
                    cx = if (relative) cx + number else number
                    needY = true
                } else {
                    cy = if (relative) cy + number else number
                    cur += Pt(cx, cy)
                    needY = false
                }
            }
            'L', 'l' -> {
                if (!needY) {
                    cx = if (relative) cx + number else number
                    needY = true
                } else {
                    cy = if (relative) cy + number else number
                    cur += Pt(cx, cy)
                    needY = false
                }
            }
            else -> Unit
        }
    }
    closeSub()
    if (subs.isEmpty()) return null

    subs.forEach { sub ->
        sub.forEach { p ->
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
    }
    val gMinX = floor(minX).toInt()
    val gMinY = floor(minY).toInt()
    val gMaxX = ceil(maxX).toInt()
    val gMaxY = ceil(maxY).toInt()
    val modules = maxOf(gMaxX - gMinX, gMaxY - gMinY)
    if (modules <= 0) return null

    val rects = mutableListOf<QrModule>()
    for (gy in gMinY until gMaxY) {
        for (gx in gMinX until gMaxX) {
            val sx = gx + 0.5f
            val sy = gy + 0.5f
            var inside = false
            for (sub in subs) {
                if (pointInPolygonEvenOdd(sx, sy, sub)) inside = !inside
            }
            if (inside) {
                rects += QrModule((gx - gMinX).toFloat(), (gy - gMinY).toFloat(), 1f, 1f)
            }
        }
    }
    if (rects.isEmpty()) return null
    return rects to modules.toFloat()
}

/** 射线法（evenodd 规则）：点是否在多边形内 */
private fun pointInPolygonEvenOdd(px: Float, py: Float, poly: List<Pt>): Boolean {
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val xi = poly[i].x
        val yi = poly[i].y
        val xj = poly[j].x
        val yj = poly[j].y
        if ((yi > py) != (yj > py) &&
            px < (xj - xi) * (py - yi) / (yj - yi) + xi
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

/**
 * 把解析出来的二维码矩阵画到画布上（白底 + 深色模块）
 *
 * @param spec 解析结果
 * @param sizeDp 希望的正方形边长
 * @param moduleColor 模块颜色（默认纯黑，保证识别率）
 */
@Composable
fun QrCodeFromSvg(
    spec: QrSvgSpec,
    sizeDp: Dp,
    modifier: Modifier = Modifier,
    moduleColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val rects = remember(spec) { spec.rects }
    val modules = spec.modules
    Canvas(modifier = modifier.size(sizeDp)) {
        drawRect(color = backgroundColor, size = size)
        if (modules <= 0f) return@Canvas
        val scale = size.minDimension / modules
        rects.forEach { module ->
            drawRect(
                color = moduleColor,
                topLeft = Offset(module.x * scale, module.y * scale),
                size = Size(module.w * scale, module.h * scale)
            )
        }
    }
}
