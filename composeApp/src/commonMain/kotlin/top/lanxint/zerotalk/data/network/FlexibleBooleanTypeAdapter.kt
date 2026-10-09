package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * 宽容布尔类型适配器工厂
 *
 * 零语服务端对同一语义的布尔字段存在 **true/false 与 1/0 两种下发形态**，官方前端同样是按
 * 兼容写法消费的（见 `tmp/assets/dmBan-DYyZ3iqS.js`：`dm_banned === true || dm_banned === 1`、
 * `can_receive_dm === false`），例如：
 * - `GET /api/moment/user` 的 `moments_public` 实测下发 BOOLEAN，而旧 DTO 声明为 Int，
 *   直接导致 `IllegalStateException: Expected an int but was BOOLEAN ... $.data.moments_public`
 *   令整页资料解析失败；
 * - 房间加密字段里 `encryption_enabled:false` 与 `room.encryption_enabled:0` 并存。
 *
 * 此适配器让所有 Boolean 字段同时接受：`true`/`false`、`1`/`0`（含小数）、
 * `"1"`/`"0"`/`"true"`/`"false"`/`"yes"`/`"no"`/`"on"`/`"off"`/`""`。
 *
 * JSON null 的处理按字段可空性区分（Kotlin 可空 Boolean 编译为包装类型，非空为原始 boolean）：
 * - 原始 `boolean`：取 `false`，避免解包崩溃；
 * - 包装 `Boolean?`：保持 `null`，与 Gson 内建行为一致，不改变「未知 / 未下发」语义。
 */
object FlexibleBooleanTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val adapter: TypeAdapter<*> = when (type.rawType) {
            Boolean::class.javaPrimitiveType -> FlexibleBooleanAdapter(nullValue = false)
            Boolean::class.javaObjectType -> FlexibleBooleanAdapter(nullValue = null)
            else -> return null
        }
        @Suppress("UNCHECKED_CAST")
        return adapter as TypeAdapter<T>
    }
}

/**
 * 宽容布尔读取实现（写出仍为标准 JSON 布尔）
 *
 * @param nullValue JSON null 时的取值：原始 boolean 传 false，可空 Boolean 传 null
 */
class FlexibleBooleanAdapter(private val nullValue: Boolean? = null) : TypeAdapter<Boolean?>() {

    override fun write(out: JsonWriter, value: Boolean?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(input: JsonReader): Boolean? = when (input.peek()) {
        JsonToken.BOOLEAN -> input.nextBoolean()
        JsonToken.NUMBER -> input.nextDouble() != 0.0
        JsonToken.STRING -> when (input.nextString().trim().lowercase()) {
            "1", "true", "yes", "on" -> true
            "0", "false", "no", "off", "" -> false
            else -> throw JsonSyntaxException("无法识别的布尔值")
        }
        JsonToken.NULL -> {
            input.nextNull()
            nullValue
        }
        else -> throw JsonSyntaxException("期望布尔值，实际为 ${input.peek()}")
    }
}
