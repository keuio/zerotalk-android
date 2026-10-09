package top.lanxint.zerotalk.data.log

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [AppDiagnostics] 的脱敏与组装回归测试。
 *
 * 重点保证：发送到 App端群的诊断文本里**不得**出现完整 uid / 房间 id 与聊天正文。
 */
class AppDiagnosticsTest {

    private val fullHexUid = "0123456789abcdef0123456789abcdef"

    // ---------------- uid 截断 ----------------

    @Test
    fun `32 位 hex uid 只保留前 8 位`() {
        assertEquals("01234567…", AppDiagnostics.maskId(fullHexUid))
    }

    @Test
    fun `短 id 原样保留`() {
        assertEquals("abc123", AppDiagnostics.maskId("abc123"))
    }

    @Test
    fun `空 id 与未登录占位返回未知`() {
        assertEquals("未知", AppDiagnostics.maskId(null))
        assertEquals("未知", AppDiagnostics.maskId("   "))
        assertEquals("未知", AppDiagnostics.maskId("--"))
    }

    // ---------------- content 省略 ----------------

    @Test
    fun `等号形式的 content 被省略`() {
        val out = AppDiagnostics.sanitizeLine("[WS] send content=这是一条聊天正文")
        assertTrue(out.contains("content=<已省略>"), out)
        assertFalse(out.contains("这是一条聊天正文"), out)
    }

    @Test
    fun `单引号形式的 content 被省略`() {
        assertEquals("content=<已省略>", AppDiagnostics.sanitizeLine("content='hello world'"))
    }

    @Test
    fun `JSON 形式的 content 被省略且其它字段保留`() {
        val out = AppDiagnostics.sanitizeLine("""{"content":"秘密消息","type":"text"}""")
        assertTrue(out.contains("\"content:<已省略>"), out)
        assertFalse(out.contains("秘密消息"), out)
        // type 不是内容字段，应原样保留
        assertTrue(out.contains("\"type\":\"text\""), out)
    }

    @Test
    fun `message 与 text 字段同样被省略`() {
        val a = AppDiagnostics.sanitizeLine("message=对方发来的悄悄话")
        assertTrue(a.contains("message=<已省略>"), a)
        assertFalse(a.contains("悄悄话"), a)

        val b = AppDiagnostics.sanitizeLine("text: 另一段正文")
        assertTrue(b.contains("text:<已省略>"), b)
        assertFalse(b.contains("另一段正文"), b)
    }

    @Test
    fun `相似字段名不被误伤`() {
        assertEquals("contentType=image/png", AppDiagnostics.sanitizeLine("contentType=image/png"))
        assertEquals("context=main", AppDiagnostics.sanitizeLine("context=main"))
        assertEquals("textStyle=body", AppDiagnostics.sanitizeLine("textStyle=body"))
    }

    // ---------------- 32 位 hex / UUID 截断 ----------------

    @Test
    fun `日志中的 32 位 hex 房间 id 被截断`() {
        val out = AppDiagnostics.sanitizeLine("[WS] room $fullHexUid joined")
        assertEquals("[WS] room 01234567… joined", out)
    }

    @Test
    fun `UUID 形态的设备 id 被截断`() {
        val out = AppDiagnostics.sanitizeLine("did=550e8400-e29b-41d4-a716-446655440000")
        assertEquals("did=550e8400…", out)
    }

    @Test
    fun `正常诊断行原样保留`() {
        assertEquals("[WS] connected ok", AppDiagnostics.sanitizeLine("[WS] connected ok"))
        assertEquals(
            "[Wallpaper] 本地背景保存失败：文件不存在",
            AppDiagnostics.sanitizeLine("[Wallpaper] 本地背景保存失败：文件不存在")
        )
    }

    // ---------------- 行筛选 ----------------

    @Test
    fun `关键字行优先且不超过上限`() {
        val lines = (1..10).map { "noise $it" } + listOf("[WS] boom", "Exception: x", "请求失败")
        val picked = AppDiagnostics.selectDiagnosticLines(lines, maxLines = 4)
        assertEquals(4, picked.size)
        assertTrue(picked.contains("[WS] boom"))
        assertTrue(picked.contains("Exception: x"))
        assertTrue(picked.contains("请求失败"))
        // 关键字行 3 行，用 1 行尾部普通行补齐
        assertEquals(1, picked.count { it.startsWith("noise") })
        assertEquals("noise 10", picked.first())
    }

    @Test
    fun `关键字行超过上限时只保留尾部`() {
        val lines = (1..5).map { "error $it" }
        assertEquals(listOf("error 3", "error 4", "error 5"), AppDiagnostics.selectDiagnosticLines(lines, maxLines = 3))
    }

    @Test
    fun `空输入返回空列表`() {
        assertTrue(AppDiagnostics.selectDiagnosticLines(emptyList()).isEmpty())
        assertTrue(AppDiagnostics.selectDiagnosticLines(listOf("  ", ""), maxLines = 3).isEmpty())
    }

    // ---------------- 整体组装 ----------------

    @Test
    fun `build 包含环境信息且不含完整 uid 与正文`() {
        AppDiagnostics.environment = DeviceEnvironment(
            appVersionName = "9.9.9",
            appVersionCode = "42",
            androidVersion = "Android 15（API 35）",
            deviceModel = "vivo V2055A"
        )
        try {
            val text = AppDiagnostics.build(
                uid = fullHexUid,
                nickname = "测试用户",
                now = "2025-01-02 03:04:05",
                rawLogLines = listOf(
                    "[WS] send content=私密正文 room $fullHexUid",
                    "[Wallpaper] ok"
                )
            )
            assertTrue(text.contains("9.9.9"), text)
            assertTrue(text.contains("42"), text)
            assertTrue(text.contains("Android 15（API 35）"), text)
            assertTrue(text.contains("vivo V2055A"), text)
            assertTrue(text.contains("2025-01-02 03:04:05"), text)
            assertTrue(text.contains("账号 uid：01234567…"), text)
            assertTrue(text.contains("账号昵称：测试用户"), text)
            // 脱敏底线：完整 uid 与聊天正文绝不出现
            assertFalse(text.contains(fullHexUid), text)
            assertFalse(text.contains("私密正文"), text)
            assertTrue(text.contains("content=<已省略>"), text)
        } finally {
            AppDiagnostics.environment = null
        }
    }

    @Test
    fun `build 无日志时给出占位`() {
        val text = AppDiagnostics.build(
            uid = null,
            nickname = null,
            now = "2025-01-02 03:04:05",
            rawLogLines = emptyList()
        )
        assertTrue(text.contains("（无可用日志）"), text)
        assertTrue(text.contains("账号 uid：未知"), text)
        assertTrue(text.contains("账号昵称：未知"), text)
    }

    @Test
    fun `build 超长时截断到上限`() {
        // 40 行普通日志，每行足够长，使组装结果超过 MAX_DIAGNOSTIC_CHARS
        val longLog = (1..40).map { "line $it " + "x".repeat(200) }
        val text = AppDiagnostics.build(
            uid = "abc",
            nickname = "n",
            now = "t",
            rawLogLines = longLog
        )
        assertTrue(text.length <= AppDiagnostics.MAX_DIAGNOSTIC_CHARS, "length=" + text.length)
        assertTrue(text.endsWith("（内容过长，已截断）"), text.takeLast(40))
    }

    // ---------------- ZtLog 读取 ----------------

    @Test
    fun `readRecentLines 未注入读取器时返回空`() {
        assertTrue(ZtLog.readRecentLines(10).isEmpty())
    }

    @Test
    fun `readRecentLines 只取尾部并丢弃空行`() {
        ZtLog.logReader = { "a\n\n b \nc\nd" }
        try {
            assertEquals(listOf("a", " b ", "c", "d"), ZtLog.readRecentLines(10))
            assertEquals(listOf("c", "d"), ZtLog.readRecentLines(2))
            assertTrue(ZtLog.readRecentLines(0).isEmpty())
        } finally {
            ZtLog.logReader = null
        }
    }
}
