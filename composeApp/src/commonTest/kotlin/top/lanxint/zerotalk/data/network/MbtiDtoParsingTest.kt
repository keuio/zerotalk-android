package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * DTO 层 `mbti` 字段的「对象 / 字符串 / null / 异常形态」兼容回归测试。
 *
 * 这些 DTO 是资料页 MBTI 卡片的数据来源，字段声明为 `JsonElement?`，
 * 必须保证任意形态都能安全解析（不能因为字符串形态就抛异常导致整条响应解析失败）。
 */
class MbtiDtoParsingTest {

    private val gson = Gson()

    @Test
    fun `BootstrapUser 对象形态解析出完整信息`() {
        val json = """
            {"id":1,"username":"kelo","mbti":{"type":"INFP","name":"调停者","role":"diplomat",
            "role_color":"green","keywords":["真诚","理想"],
            "percents":{"ei":{"E":49,"I":51}}}}
        """.trimIndent()
        val user = gson.fromJson(json, BootstrapUser::class.java)
        val mbti = assertNotNull(user.mbtiInfo)
        assertEquals("INFP", mbti.type)
        assertEquals("调停者", mbti.name)
        assertEquals("diplomat", mbti.role)
        assertEquals("green", mbti.roleColor)
        assertEquals(listOf("真诚", "理想"), mbti.keywords)
        assertEquals(1, mbti.dimensions.size)
    }

    @Test
    fun `BootstrapUser 字符串形态只保留类型`() {
        val user = gson.fromJson("""{"mbti":"ENFP"}""", BootstrapUser::class.java)
        assertEquals("ENFP", user.mbtiInfo?.type)
    }

    @Test
    fun `BootstrapUser null 形态`() {
        val user = gson.fromJson("""{"mbti":null}""", BootstrapUser::class.java)
        assertNull(user.mbtiInfo)
    }

    @Test
    fun `BootstrapUser 数组形态不抛异常`() {
        val user = gson.fromJson("""{"mbti":[1,2,3]}""", BootstrapUser::class.java)
        assertNull(user.mbtiInfo)
    }

    @Test
    fun `UserLookupData 优先取 user 内 mbti`() {
        val data = gson.fromJson(
            """{"user":{"mbti":{"type":"INFP","name":"调停者"}},"mbti":"ENFP"}""",
            UserLookupData::class.java
        )
        assertEquals("INFP", data.mbtiInfo?.type)
        assertEquals("INFP", data.mbti)
    }

    @Test
    fun `UserLookupData 缺失 user 内 mbti 时回落根级`() {
        val data = gson.fromJson("""{"user":{},"mbti":"ENFP"}""", UserLookupData::class.java)
        assertEquals("ENFP", data.mbtiInfo?.type)
        assertEquals("ENFP", data.mbti)
    }

    @Test
    fun `UserLookupData 无 mbti 时为 null`() {
        val data = gson.fromJson("""{"user":{}}""", UserLookupData::class.java)
        assertNull(data.mbtiInfo)
        assertNull(data.mbti)
    }

    @Test
    fun `UserProfileDto 对象形态解析`() {
        val dto = gson.fromJson(
            """{"username":"玫瑰","mbti":{"type":"ENFP","role":"diplomat"}}""",
            UserProfileDto::class.java
        )
        assertEquals("ENFP", dto.mbtiInfo?.type)
        assertEquals("diplomat", dto.mbtiInfo?.role)
    }
}
