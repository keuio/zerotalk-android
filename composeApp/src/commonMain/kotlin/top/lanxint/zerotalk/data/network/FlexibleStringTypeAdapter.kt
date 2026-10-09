package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * 宽容字符串适配器：同一个「文本」字段，服务端可能下发成字符串、数字、布尔，甚至对象。
 *
 * 实例：`data.user.mbti` 早期是 `"INFP"`，后来变成 `{"type":"INFP","name":"调停者"}`。
 * DTO 声明成 `String` 时 Gson 会抛
 * `IllegalStateException: Expected a string but was BEGIN_OBJECT at ... path $.data.user.mbti`，
 * 导致整条 bootstrap 解析失败、登录态无法落地，用户侧只看到「服务连接失败」——**已真实发生过**。
 *
 * 与 [FlexibleBooleanTypeAdapterFactory] / [FlexibleNumberTypeAdapterFactory] 同一思路：
 * 对**所有 String 字段**生效，按下列顺序取值：
 * 1. JSON null → null
 * 2. 字符串 / 数字 / 布尔 → 对应的字符串形式
 * 3. 对象 → 依次尝试 type / name / value / text / label / title 的原始值，都没有则 null
 * 4. 数组等其它形态 → null（跳过，不再抛异常）
 *
 * 与 WebSocket 侧 `user_joined` 的 mbti 兼容解析（读 `type` 或 `name`）保持同一口径。
 */
object FlexibleStringTypeAdapterFactory : TypeAdapterFactory {

    /** 对象形态下按优先级尝试的文本键（与 WS 侧保持一致，另补充常见命名） */
    private val TEXT_KEYS = listOf("type", "name", "value", "text", "label", "title")

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != String::class.java) return null
        @Suppress("UNCHECKED_CAST")
        return StringAdapter as TypeAdapter<T>
    }

    private object StringAdapter : TypeAdapter<String>() {

        override fun write(out: JsonWriter, value: String?) {
            if (value == null) out.nullValue() else out.value(value)
        }

        override fun read(input: JsonReader): String? = when (input.peek()) {
            JsonToken.NULL -> {
                input.nextNull()
                null
            }
            // 注意：严格模式下 JsonReader.nextString() 对 NUMBER 可用，但对 BOOLEAN 会抛
            // 「Expected a string but was BOOLEAN」，必须显式走 nextBoolean()
            JsonToken.STRING, JsonToken.NUMBER -> input.nextString()
            JsonToken.BOOLEAN -> input.nextBoolean().toString()
            JsonToken.BEGIN_OBJECT -> readFromObject(input)
            else -> {
                input.skipValue()
                null
            }
        }

        /** 对象形态：取第一个命中的文本键，其余键一律跳过 */
        private fun readFromObject(input: JsonReader): String? {
            var found: String? = null
            input.beginObject()
            while (input.hasNext()) {
                val name = input.nextName()
                val isCandidate = TEXT_KEYS.any { it.equals(name, ignoreCase = true) }
                if (found == null && isCandidate) {
                    found = when (input.peek()) {
                        JsonToken.STRING, JsonToken.NUMBER -> input.nextString()
                        JsonToken.BOOLEAN -> input.nextBoolean().toString()
                        else -> {
                            input.skipValue()
                            null
                        }
                    }
                } else {
                    input.skipValue()
                }
            }
            input.endObject()
            return found
        }
    }
}
