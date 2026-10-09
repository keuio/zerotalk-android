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
 * 宽容数字类型适配器工厂（Int / Long）
 *
 * 与 [FlexibleBooleanTypeAdapterFactory] 对称：零语服务端同一字段可能在
 * `true/false`、`1/0`、`"1"/"0"` 之间摇摆。官方前端对此有统一兜底函数
 * （见 `tmp/assets/PrivacySettingsView-Da9PtCkC.js`）：
 * ```
 * n = (v, fallback) => v == null || v === "" ? fallback
 *      : typeof v === "boolean" ? v
 *      : typeof v === "number"  ? v !== 0
 *      : typeof v === "string"  ? (v === "1" || v === "true")
 *      : fallback
 * ```
 * 隐私设置字段（`moments_public`、`dm_public`…）正是按此兼容消费，且保存时**统一回传 0/1**。
 *
 * Gson 默认遇到类型不符会直接抛 `IllegalStateException: Expected an int but was BOOLEAN`
 * 并让整个接口解析失败，因此这里让 Int / Long 字段同时接受：
 * - 数字字面量（含 `12.5` 这类小数，取整；用原始字面量解析以保住大整数精度）
 * - `true` / `false` → `1` / `0`
 * - 数字字符串 `"12"` / `"12.5"`，空串按 `0`
 *
 * JSON null：原始 int/long 取 0，可空 Int?/Long? 保持 null。
 */
object FlexibleNumberTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val adapter: TypeAdapter<*> = when (type.rawType) {
            Int::class.javaPrimitiveType -> FlexibleIntAdapter(nullValue = 0)
            Int::class.javaObjectType -> FlexibleIntAdapter(nullValue = null)
            Long::class.javaPrimitiveType -> FlexibleLongAdapter(nullValue = 0L)
            Long::class.javaObjectType -> FlexibleLongAdapter(nullValue = null)
            else -> return null
        }
        @Suppress("UNCHECKED_CAST")
        return adapter as TypeAdapter<T>
    }
}

/** 取出数字字面量：整数字面量走 Long 以免大整数掉精度，小数再退回 Double */
private fun JsonReader.readNumberLiteral(): String = nextString().trim()

private fun parseNumber(raw: String): Double? =
    raw.toLongOrNull()?.toDouble() ?: raw.toDoubleOrNull()

class FlexibleIntAdapter(private val nullValue: Int? = null) : TypeAdapter<Int?>() {

    override fun write(out: JsonWriter, value: Int?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(input: JsonReader): Int? = when (input.peek()) {
        JsonToken.NUMBER -> (parseNumber(input.readNumberLiteral())
            ?: throw JsonSyntaxException("无法识别的数字")).toInt()
        JsonToken.BOOLEAN -> if (input.nextBoolean()) 1 else 0
        JsonToken.STRING -> input.nextString().trim().let { raw ->
            if (raw.isEmpty()) 0
            else (parseNumber(raw) ?: throw JsonSyntaxException("无法识别的数字：$raw")).toInt()
        }
        JsonToken.NULL -> {
            input.nextNull()
            nullValue
        }
        else -> throw JsonSyntaxException("期望数字，实际为 ${input.peek()}")
    }
}

class FlexibleLongAdapter(private val nullValue: Long? = null) : TypeAdapter<Long?>() {

    override fun write(out: JsonWriter, value: Long?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(input: JsonReader): Long? = when (input.peek()) {
        JsonToken.NUMBER -> (parseNumber(input.readNumberLiteral())
            ?: throw JsonSyntaxException("无法识别的数字")).toLong()
        JsonToken.BOOLEAN -> if (input.nextBoolean()) 1L else 0L
        JsonToken.STRING -> input.nextString().trim().let { raw ->
            if (raw.isEmpty()) 0L
            else raw.toLongOrNull()
                ?: (parseNumber(raw) ?: throw JsonSyntaxException("无法识别的数字：$raw")).toLong()
        }
        JsonToken.NULL -> {
            input.nextNull()
            nullValue
        }
        else -> throw JsonSyntaxException("期望数字，实际为 ${input.peek()}")
    }
}
