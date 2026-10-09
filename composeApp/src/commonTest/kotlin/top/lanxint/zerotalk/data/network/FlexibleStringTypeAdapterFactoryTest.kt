package top.lanxint.zerotalk.data.network

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [FlexibleStringTypeAdapterFactory] 的回归测试。
 *
 * 背景：服务端把 `data.user.mbti` 从字符串改成了对象，DTO 仍声明为 String，
 * Gson 抛 `IllegalStateException: Expected a string but was BEGIN_OBJECT`，
 * 导致 bootstrap 整体解析失败、登录态无法落地（用户侧表现为「服务连接失败」）。
 */
class FlexibleStringTypeAdapterFactoryTest {

    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(FlexibleStringTypeAdapterFactory)
        .create()

    /** 与 `data.user` 同形的最小载体 */
    private data class UserHolder(
        @SerializedName("mbti") val mbti: String? = null,
        @SerializedName("username") val username: String = ""
    )

    @Test
    fun `对象形态优先取 type`() {
        val json = """{"mbti":{"type":"INFP","name":"调停者"},"username":"kelo"}"""
        val user = gson.fromJson(json, UserHolder::class.java)
        assertEquals("INFP", user.mbti)
        assertEquals("kelo", user.username)
    }

    @Test
    fun `对象形态无 type 时回落到 name`() {
        val json = """{"mbti":{"name":"调停者"}}"""
        assertEquals("调停者", gson.fromJson(json, UserHolder::class.java).mbti)
    }

    @Test
    fun `对象形态无可用键时为 null 且不抛异常`() {
        val json = """{"mbti":{"foo":1,"bar":[1,2]}}"""
        assertNull(gson.fromJson(json, UserHolder::class.java).mbti)
    }

    @Test
    fun `字符串形态原样保留`() {
        assertEquals("INFP", gson.fromJson("""{"mbti":"INFP"}""", UserHolder::class.java).mbti)
    }

    @Test
    fun `数字与布尔转为字符串`() {
        assertEquals("42", gson.fromJson("""{"mbti":42}""", UserHolder::class.java).mbti)
        assertEquals("true", gson.fromJson("""{"mbti":true}""", UserHolder::class.java).mbti)
    }

    @Test
    fun `null 保持 null`() {
        assertNull(gson.fromJson("""{"mbti":null}""", UserHolder::class.java).mbti)
    }

    @Test
    fun `数组形态不抛异常且为 null`() {
        assertNull(gson.fromJson("""{"mbti":[1,2,3]}""", UserHolder::class.java).mbti)
    }

    @Test
    fun `同一对象内其它字段不受影响`() {
        val json = """{"mbti":{"type":"ENFP"},"username":"玫瑰"}"""
        val user = gson.fromJson(json, UserHolder::class.java)
        assertEquals("ENFP", user.mbti)
        assertEquals("玫瑰", user.username)
    }
}
