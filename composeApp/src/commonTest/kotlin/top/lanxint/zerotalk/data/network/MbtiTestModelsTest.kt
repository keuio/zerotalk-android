package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 官方 MBTI 测评数据模型的解析 / 组装回归测试。
 *
 * 覆盖三块：
 * 1. 题目 JSON 解析（正常 / 缺字段 / 非对象元素 / 畸形）；
 * 2. 提交体组装（version + answers[{id,value}]，字段名必须与官方逐字一致）；
 * 3. 结果 JSON 解析（正常 / 缺字段 / 异常形态）与 GET /api/mbti/me 的 data 包装。
 */
class MbtiTestModelsTest {

    private val gson = Gson()

    // ========================================================
    // 1. 题目 JSON 解析
    // ========================================================

    private val questionsJson = """
        {
          "version": "v1",
          "total": 2,
          "scale": {"labels": ["非常不同意", "不同意", "同意", "非常同意"]},
          "questions": [
            {"id": 1, "text": "你享受社交聚会吗？", "dimension": "ei"},
            {"id": 2, "text": "你更相信直觉还是事实？", "dimension": "sn"}
          ]
        }
    """.trimIndent()

    @Test
    fun `题目 JSON 正常解析`() {
        val data = assertNotNull(MbtiQuestionsData.fromJson(questionsJson))
        assertEquals("v1", data.version)
        assertEquals(2, data.total)
        assertEquals(4, data.scale.labels.size)
        assertEquals(4, data.scaleSize)
        assertEquals(listOf("非常不同意", "不同意", "同意", "非常同意"), data.scale.labels)
        assertEquals(2, data.questions.size)
        assertEquals(1, data.questions[0].id)
        assertEquals("你享受社交聚会吗？", data.questions[0].text)
        assertEquals("ei", data.questions[0].dimension)
        assertEquals("sn", data.questions[1].dimension)
    }

    @Test
    fun `题目 JSON 缺字段时用默认值兜底`() {
        val json = """
            {"questions": [{"id": 7}, {"id": 8, "text": "第二题"}]}
        """.trimIndent()
        val data = assertNotNull(MbtiQuestionsData.fromJson(json))
        assertEquals("", data.version)
        assertEquals(2, data.total)
        assertEquals(0, data.scale.labels.size)
        assertEquals("", data.questions[0].text)
        assertEquals("", data.questions[0].dimension)
        assertEquals("第二题", data.questions[1].text)
    }

    @Test
    fun `题目 JSON 非对象元素被跳过`() {
        val json = """
            {"total": 3, "questions": [1, "x", {"id": 9, "text": "有效题"}]}
        """.trimIndent()
        val data = assertNotNull(MbtiQuestionsData.fromJson(json))
        assertEquals(1, data.questions.size)
        assertEquals(9, data.questions[0].id)
        assertEquals(3, data.total)
    }

    @Test
    fun `题目 JSON 数字字符串的 total 也能解析`() {
        val data = assertNotNull(MbtiQuestionsData.fromJson("{\"total\":\"60\",\"questions\":[]}"))
        assertEquals(60, data.total)
    }

    @Test
    fun `题目 JSON 畸形或非对象返回 null`() {
        assertNull(MbtiQuestionsData.fromJson(null))
        assertNull(MbtiQuestionsData.fromJson(""))
        assertNull(MbtiQuestionsData.fromJson("   "))
        assertNull(MbtiQuestionsData.fromJson("{不是 JSON"))
        assertNull(MbtiQuestionsData.fromJson("[]"))
        assertNull(MbtiQuestionsData.fromJson("\"INFP\""))
        assertNull(MbtiQuestionsData.fromJson("null"))
    }

    @Test
    fun `题目 JSON 通过 JsonElement 入口解析`() {
        val data = assertNotNull(MbtiQuestionsData.fromJsonElement(JsonParser.parseString(questionsJson)))
        assertEquals(2, data.questions.size)
    }

    @Test
    fun `题目数量与官方 60 题校验`() {
        assertEquals(60, MbtiQuestionsData.EXPECTED_TOTAL)
        assertTrue(MbtiQuestionsData.fromJson(questionsJson)!!.isComplete.not())
    }

    // ========================================================
    // 2. 提交体组装
    // ========================================================

    @Test
    fun `提交体组装字段名与官方一致`() {
        val body = MbtiSubmitRequest(
            version = "v1",
            answers = listOf(MbtiAnswer(id = 1, value = 3), MbtiAnswer(id = 2, value = 4))
        ).toJson(gson)
        assertEquals(
            "{\"version\":\"v1\",\"answers\":[{\"id\":1,\"value\":3},{\"id\":2,\"value\":4}]}",
            body
        )
    }

    @Test
    fun `提交体空答案仍可组装`() {
        val body = MbtiSubmitRequest(version = "", answers = emptyList()).toJson(gson)
        assertEquals("{\"version\":\"\",\"answers\":[]}", body)
    }

    @Test
    fun `提交体可由服务端 JSON 反解析回模型`() {
        val json = "{\"version\":\"v2\",\"answers\":[{\"id\":5,\"value\":2}]}"
        val parsed = gson.fromJson(json, MbtiSubmitRequest::class.java)
        assertEquals("v2", parsed.version)
        assertEquals(1, parsed.answers.size)
        assertEquals(5, parsed.answers[0].id)
        assertEquals(2, parsed.answers[0].value)
    }

    // ========================================================
    // 3. 结果 JSON 解析
    // ========================================================

    private val resultJson = """
        {
          "type": "INFP",
          "name": "调停者",
          "role": "diplomat",
          "role_label": "外交家",
          "role_color": "green",
          "role_description": "外交家乐于助人，善于共情。",
          "summary": "安静而神秘，却鼓舞人心。",
          "keywords": ["真诚", "理想", "温柔"],
          "strengths": "共情、洞察",
          "weaknesses": "过于理想化",
          "love": "渴望灵魂共鸣",
          "social": "慢热但真诚",
          "career": "适合创作",
          "letters": {"ei": "I", "sn": "N", "tf": "F", "jp": "P"},
          "percents": {
            "ei": {"E": 49, "I": 51},
            "sn": {"S": 27, "N": 73},
            "tf": {"T": 30, "F": 70},
            "jp": {"J": 35, "P": 65}
          },
          "scores": {"E": 22, "I": 23},
          "tested_at": "2026-10-10 00:27:24"
        }
    """.trimIndent()

    @Test
    fun `结果 JSON 正常解析`() {
        val result = assertNotNull(MbtiResult.fromJson(resultJson))
        assertEquals("INFP", result.type)
        assertEquals("调停者", result.name)
        assertEquals("diplomat", result.role)
        assertEquals("外交家", result.roleLabel)
        assertEquals("green", result.roleColor)
        assertEquals("安静而神秘，却鼓舞人心。", result.summary)
        assertEquals(listOf("真诚", "理想", "温柔"), result.keywords)
        assertEquals("共情、洞察", result.strengths)
        assertEquals("过于理想化", result.weaknesses)
        assertEquals("渴望灵魂共鸣", result.love)
        assertEquals("慢热但真诚", result.social)
        assertEquals("适合创作", result.career)
        assertEquals("I", result.letters["ei"])
        assertEquals(51, result.percents["ei"]?.get("I"))
        assertEquals(22, result.scores["E"])
        assertEquals("2026-10-10 00:27:24", result.testedAt)
        assertEquals("INFP · 调停者", result.displayTitle)
    }

    @Test
    fun `结果四维百分比按固定顺序且缺失维度跳过`() {
        val result = assertNotNull(MbtiResult.fromJson(resultJson))
        val dimensions = result.dimensions
        assertEquals(4, dimensions.size)
        assertEquals(listOf("ei", "sn", "tf", "jp"), dimensions.map { it.key })
        assertEquals("E", dimensions[0].first)
        assertEquals(49, dimensions[0].firstPercent)
        assertEquals(51, dimensions[0].secondPercent)
        assertEquals(0.49f, dimensions[0].firstFraction, 0.001f)

        val partial = assertNotNull(
            MbtiResult.fromJson("{\"type\":\"X\",\"percents\":{\"jp\":{\"J\":10,\"P\":90}}}")
        )
        assertEquals(1, partial.dimensions.size)
        assertEquals("jp", partial.dimensions[0].key)
    }

    @Test
    fun `结果百分比为数字字符串时也能解析`() {
        val result = assertNotNull(
            MbtiResult.fromJson("{\"type\":\"X\",\"percents\":{\"ei\":{\"E\":\"60\",\"I\":\"40\"}}}")
        )
        assertEquals(60, result.dimensions[0].firstPercent)
        assertEquals(40, result.dimensions[0].secondPercent)
    }

    @Test
    fun `结果 JSON 缺字段时安全降级`() {
        val onlyType = assertNotNull(MbtiResult.fromJson("{\"type\":\"ENFP\"}"))
        assertEquals("ENFP", onlyType.type)
        assertEquals("", onlyType.name)
        assertEquals("ENFP", onlyType.displayTitle)
        assertTrue(onlyType.keywords.isEmpty())
        assertTrue(onlyType.percents.isEmpty())
        assertTrue(onlyType.dimensions.isEmpty())
        assertEquals("", onlyType.testedAt)

        val onlyName = assertNotNull(MbtiResult.fromJson("{\"name\":\"调停者\"}"))
        assertEquals("调停者", onlyName.type)
        assertEquals("调停者", onlyName.displayTitle)

        assertNull(MbtiResult.fromJson("{}"))
        assertNull(MbtiResult.fromJson("{\"summary\":\"只有摘要\"}"))
    }

    @Test
    fun `结果 JSON 异常形态返回 null`() {
        assertNull(MbtiResult.fromJson(null))
        assertNull(MbtiResult.fromJson(""))
        assertNull(MbtiResult.fromJson("{不是 JSON"))
        assertNull(MbtiResult.fromJson("[]"))
        assertNull(MbtiResult.fromJson("\"INFP\""))
        assertNull(MbtiResult.fromJson("null"))
        assertNull(MbtiResult.fromJsonElement(null))
    }

    @Test
    fun `结果 keywords 非字符串元素被跳过`() {
        val result = assertNotNull(
            MbtiResult.fromJson("{\"type\":\"X\",\"keywords\":[\"a\",1,null,\"b\"]}")
        )
        assertEquals(listOf("a", "b"), result.keywords)
    }

    // ========================================================
    // 4. GET /api/mbti/me 的 data 包装
    // ========================================================

    @Test
    fun `me 接口 data 正常解析出结果`() {
        val data = assertNotNull(
            MbtiMeData.fromJsonElement(JsonParser.parseString("{\"result\":" + resultJson + "}"))
        )
        assertEquals("INFP", data.result?.type)
    }

    @Test
    fun `me 接口 result 为 null 或缺失时为未测评`() {
        val nullResult = assertNotNull(MbtiMeData.fromJsonElement(JsonParser.parseString("{\"result\":null}")))
        assertNull(nullResult.result)
        val missing = assertNotNull(MbtiMeData.fromJsonElement(JsonParser.parseString("{}")))
        assertNull(missing.result)
        val malformedResult = assertNotNull(
            MbtiMeData.fromJsonElement(JsonParser.parseString("{\"result\":\"oops\"}"))
        )
        assertNull(malformedResult.result)
    }

    @Test
    fun `me 接口非对象 data 返回 null`() {
        assertNull(MbtiMeData.fromJsonElement(null))
        assertNull(MbtiMeData.fromJsonElement(JsonParser.parseString("[]")))
        assertNull(MbtiMeData.fromJsonElement(JsonParser.parseString("\"x\"")))
    }
}