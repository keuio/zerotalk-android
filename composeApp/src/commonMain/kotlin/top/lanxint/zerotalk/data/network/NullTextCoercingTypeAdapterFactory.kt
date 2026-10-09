package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * 服务端 null 文本字段归一化工厂
 *
 * **问题**：服务端会对文本字段显式下发 null（真机实测 `/api/security/login-history` 的
 * `"location": null` 导致进入「登录历史」直接闪退：
 * `NullPointerException: Parameter specified as non-null is null: ... isBlank`）。
 * 原因是 Gson 直接反射写字段、不会走 Kotlin 的默认值，于是运行期出现「非空类型装 null」，
 * 首次 `isBlank()` / `isNotBlank()` / 传给非空形参就会崩。
 *
 * **为什么不用字段级 `@JsonAdapter`**：Gson 会给字段适配器包一层 `nullSafe()`，
 * JSON null 在进入自定义适配器之前就被短路成 null 了（已用本地探针验证：仍然是 null）。
 *
 * **做法**：只在解析完成之后，把目标 DTO 里所有为 null 的字符串字段写回空串（List 字段写回空列表）。
 * 仅作用于「本工程新增、服务端可能显式下发 null」的 DTO，避免影响历史 DTO 的既有语义。
 *
 * ⚠️ 注意：受限于 Kotlin 可空注解是 CLASS 级保留（运行期读不到），这里**无法区分可空与非空**，
 * 因此目标 DTO 内的可空文本字段（如 `eventTypeText`、`adminReply`）也会由 null 变为空串。
 * 这些字段的消费方统一使用 `?.takeIf { it.isNotBlank() } ?: 兜底` 的写法，行为不受影响；
 * 若某个字段需要严格区分「未下发」，请勿放进 [TARGETS]，或改用非字符串类型承载。
 */
object NullTextCoercingTypeAdapterFactory : TypeAdapterFactory {

    private val TARGETS: Set<Class<*>> = setOf(
        SecurityDeviceDto::class.java,
        SecurityDevicesData::class.java,
        SecurityRevokeData::class.java,
        SecurityLoginLogDto::class.java,
        SecurityLoginHistoryData::class.java,
        PenaltyOptionDto::class.java,
        PenaltyAppealDto::class.java,
        PenaltyAppealBootstrapData::class.java,
        NeteaseBindingData::class.java,
        NeteaseQrStartData::class.java,
        NeteaseQrStatusData::class.java
    )

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType !in TARGETS) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) = delegate.write(out, value)

            override fun read(input: JsonReader): T? {
                val value = delegate.read(input) ?: return null
                coerceNullTexts(value)
                return value
            }
        }
    }

    /** 反射遍历（含父类）把 null 的 String 字段写回空串、null 的 List 字段写回空列表 */
    private fun coerceNullTexts(target: Any) {
        var type: Class<*>? = target.javaClass
        while (type != null && type != Any::class.java) {
            type.declaredFields.forEach { field ->
                val isText = field.type == String::class.java
                val isList = List::class.java.isAssignableFrom(field.type)
                if (isText || isList) {
                    try {
                        field.isAccessible = true
                        if (field.get(target) == null) {
                            field.set(target, if (isText) "" else emptyList<Any>())
                        }
                    } catch (_: Exception) {
                        // 单个字段处理失败不影响整体解析
                    }
                }
            }
            type = type.superclass
        }
    }
}
