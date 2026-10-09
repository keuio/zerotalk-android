package top.lanxint.zerotalk.data.model

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [MbtiInfo] 容错解析回归测试。
 *
 * 服务端 `user.mbti` 存在三种形态：完整对象 / 早期字符串 / null；
 * 解析任何形态都不得抛异常（否则整条 bootstrap 会解析失败）。
 */
class MbtiInfoTest {

    /** 真实账号实测下发的完整对象（字段逐项照抄服务端结构） */
    private val fullObjectJson = """
        {
          "type": "INFP",
          "name": "调停者",
          "role": "diplomat",
          "role_label": "外交家",
          "role_color": "green",
          "role_description": "外交家乐于助人，善于共情。",
          "summary": "安静而神秘，却鼓舞人心且不知疲倦的理想主义者。",
          "keywords": ["真诚", "理想", "温柔", "创意", "独特"],
          "strengths": "共情、洞察、理想主义",
          "weaknesses": "过于理想化、容易内耗",
          "love": "渴望灵魂共鸣",
          "social": "慢热但真诚",
          "career": "适合创作与助人型工作",
          "letters": {"ei": "I", "sn": "N", "tf": "F", "jp": "P"},
          "percents": {
            "ei": {"E": 49, "I": 51},
            "sn": {"S": 27, "N": 73},
            "tf": {"T": 30, "F": 70},
            "jp": {"J": 35, "P": 65}
          },
          "scores": {"E": 22, "I": 23, "S": 12, "N": 33, "T": 13, "F": 31, "J": 16, "P": 29},
          "tested_at": "2026-10-10 00:27:24"
        }
    """.trimIndent()

    private fun parse(json: String): MbtiInfo? = MbtiInfo.fromJson(JsonParser.parseString(json))

    // ------------------------------------------------------------
    // 1. 对象形态
    // ------------------------------------------------------------

    @Test
    fun `对象形态逐字段解析`() {
        val mbti = assertNotNull(parse(fullObjectJson))
        assertEquals("INFP", mbti.type)
        assertEquals("调停者", mbti.name)
        assertEquals("diplomat", mbti.role)
        assertEquals("外交家", mbti.roleLabel)
        assertEquals("green", mbti.roleColor)
        assertEquals("外交家乐于助人，善于共情。", mbti.roleDescription)
        assertEquals("安静而神秘，却鼓舞人心且不知疲倦的理想主义者。", mbti.summary)
        assertEquals(listOf("真诚", "理想", "温柔", "创意", "独特"), mbti.keywords)
        assertEquals("共情、洞察、理想主义", mbti.strengths)
        assertEquals("过于理想化、容易内耗", mbti.weaknesses)
        assertEquals("渴望灵魂共鸣", mbti.love)
        assertEquals("慢热但真诚", mbti.social)
        assertEquals("适合创作与助人型工作", mbti.career)
        assertEquals(mapOf("ei" to "I", "sn" to "N", "tf" to "F", "jp" to "P"), mbti.letters)
        assertEquals("2026-10-10 00:27:24", mbti.testedAt)
        assertTrue(mbti.hasDetails)
        assertEquals("INFP · 调停者", mbti.displayTitle)
    }

    @Test
    fun `对象形态解析 percents 与 scores`() {
        val mbti = assertNotNull(parse(fullObjectJson))
        assertEquals(49, mbti.percents["ei"]?.get("E"))
        assertEquals(51, mbti.percents["ei"]?.get("I"))
        assertEquals(73, mbti.percents["sn"]?.get("N"))
        assertEquals(23, mbti.scores["I"])
        assertEquals(29, mbti.scores["P"])
    }

    @Test
    fun `四维百分比按固定顺序且占比正确`() {
        val mbti = assertNotNull(parse(fullObjectJson))
        val dimensions = mbti.dimensions
        assertEquals(listOf("ei", "sn", "tf", "jp"), dimensions.map { it.key })
        val ei = dimensions[0]
        assertEquals("E", ei.first)
        assertEquals("I", ei.second)
        assertEquals(49, ei.firstPercent)
        assertEquals(51, ei.secondPercent)
        assertEquals(0.49f, ei.firstFraction, 0.0001f)
    }

    @Test
    fun `对象形态无 type 时回落到 name`() {
        val mbti = assertNotNull(parse("""{"name":"调停者"}"""))
        assertEquals("调停者", mbti.type)
        // name 与 type 相同时不重复拼接
        assertEquals("调停者", mbti.displayTitle)
    }

    @Test
    fun `维度缺失时只返回服务端确实下发的维度`() {
        val mbti = assertNotNull(parse("""{"type":"INFP","percents":{"ei":{"E":40,"I":60},"sn":{"S":10}}}"""))
        assertEquals(listOf("ei"), mbti.dimensions.map { it.key })
    }

    @Test
    fun `percents 与两端均为 0 时占比取一半不除零`() {
        val mbti = assertNotNull(parse("""{"type":"INFP","percents":{"ei":{"E":0,"I":0}}}"""))
        assertEquals(0.5f, mbti.dimensions[0].firstFraction, 0.0001f)
    }

    // ------------------------------------------------------------
    // 2. 字符串形态
    // ------------------------------------------------------------

    @Test
    fun `字符串形态只保留类型代码`() {
        val mbti = assertNotNull(parse(""""INFP""""))
        assertEquals("INFP", mbti.type)
        assertEquals("", mbti.name)
        assertEquals("INFP", mbti.displayTitle)
        assertFalse(mbti.hasDetails)
        assertTrue(mbti.keywords.isEmpty())
        assertTrue(mbti.dimensions.isEmpty())
    }

    @Test
    fun `字符串形态首尾空白被裁剪`() {
        assertEquals("ENFP", MbtiInfo.fromString("  ENFP  ")?.type)
        assertNull(MbtiInfo.fromString("   "))
        assertNull(MbtiInfo.fromString(null))
    }

    // ------------------------------------------------------------
    // 3. null
    // ------------------------------------------------------------

    @Test
    fun `null 形态返回 null`() {
        assertNull(MbtiInfo.fromJson(null))
        assertNull(parse("null"))
    }

    // ------------------------------------------------------------
    // 4. 异常 JSON（一律不抛异常）
    // ------------------------------------------------------------

    @Test
    fun `数组形态不抛异常且为 null`() {
        assertNull(parse("[1,2,3]"))
    }

    @Test
    fun `对象无 type 与 name 时为 null`() {
        assertNull(parse("""{"foo":1,"bar":[1,2]}"""))
    }

    @Test
    fun `type 与 name 均为空白时为 null`() {
        assertNull(parse("""{"type":"","name":"   "}"""))
    }

    @Test
    fun `对象内字段类型异常时降级为空且不抛异常`() {
        val mbti = assertNotNull(
            parse(
                """{"type":"INFP","keywords":"not-array","percents":[],"scores":"x","letters":42}"""
            )
        )
        assertEquals("INFP", mbti.type)
        assertTrue(mbti.keywords.isEmpty())
        assertTrue(mbti.percents.isEmpty())
        assertTrue(mbti.scores.isEmpty())
        assertTrue(mbti.letters.isEmpty())
    }

    @Test
    fun `percents 中非数字值被跳过`() {
        val mbti = assertNotNull(
            parse("""{"type":"INFP","percents":{"ei":{"E":"49","I":51},"sn":{"S":true,"N":null}}}""")
        )
        // "49" 可转数字保留，布尔/ null 被跳过
        assertEquals(49, mbti.percents["ei"]?.get("E"))
        assertEquals(listOf("ei"), mbti.dimensions.map { it.key })
    }

    @Test
    fun `数字与布尔等原始形态按字符串处理`() {
        assertEquals("42", parse("42")?.type)
        assertEquals("true", parse("true")?.type)
    }
}
