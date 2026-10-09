package top.lanxint.zerotalk.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * MBTI 测评完整信息（服务端 `user.mbti` 的对象形态）
 *
 * 服务端同一字段存在三种历史形态：
 * 1. 对象：`{"type":"INFP","name":"调停者","role":"diplomat",...}`（已实测，字段最全）
 * 2. 字符串：`"INFP"`（早期形态，只有类型代码）
 * 3. `null`（未填写 MBTI 的账号）
 *
 * 因此**不要**把 DTO 字段声明成 [MbtiInfo]?（遇到字符串会直接抛
 * `IllegalStateException: Expected a string but was BEGIN_OBJECT` 之类的解析异常），
 * 而是用 [fromJson] 做容错解析：任何形态、任何异常都降级为 `null`，绝不抛出。
 *
 * 与 WebSocket 侧 `user_joined` 的兼容解析（读 `type` 或 `name`）保持同一口径。
 *
 * @param type 四字母类型代码（如 `INFP`）
 * @param name 中文名（如「调停者」）
 * @param role 角色标识（`analyst` / `diplomat` / `sentinel` / `explorer`）
 * @param roleLabel 角色中文名（如「外交家」）
 * @param roleColor 角色配色标识（`green` / `purple` / `blue` / `yellow`）
 * @param roleDescription 角色整体说明
 * @param summary 类型摘要
 * @param keywords 关键词列表
 * @param strengths 优势
 * @param weaknesses 劣势
 * @param love 恋爱观
 * @param social 社交风格
 * @param career 职业倾向
 * @param letters 四维字母（`{"ei":"I","sn":"N","tf":"F","jp":"P"}`）
 * @param percents 四维百分比（`{"ei":{"E":49,"I":51},...}`）
 * @param scores 四维得分（`{"E":22,"I":23,...}`）
 * @param testedAt 测评时间（服务端原样下发）
 */
data class MbtiInfo(
    val type: String,
    val name: String = "",
    val role: String = "",
    val roleLabel: String = "",
    val roleColor: String = "",
    val roleDescription: String = "",
    val summary: String = "",
    val keywords: List<String> = emptyList(),
    val strengths: String = "",
    val weaknesses: String = "",
    val love: String = "",
    val social: String = "",
    val career: String = "",
    val letters: Map<String, String> = emptyMap(),
    val percents: Map<String, Map<String, Int>> = emptyMap(),
    val scores: Map<String, Int> = emptyMap(),
    val testedAt: String = ""
) {

    /** 类型代码 + 中文名（如「INFP · 调停者」）；中文名缺失或与代码相同时仅返回代码 */
    val displayTitle: String
        get() = if (name.isBlank() || name == type) type else "$type · $name"

    /** 除类型代码外是否还有可展示的丰富信息（字符串形态为 false） */
    val hasDetails: Boolean
        get() = name.isNotBlank() ||
            roleLabel.isNotBlank() ||
            roleDescription.isNotBlank() ||
            summary.isNotBlank() ||
            keywords.isNotEmpty() ||
            percents.isNotEmpty()

    /**
     * 四维百分比（顺序固定 E/I、S/N、T/F、J/P）。
     * 仅返回服务端确实下发了两个字母百分比的维度，缺失的维度自动跳过。
     */
    val dimensions: List<MbtiDimension>
        get() = DIMENSION_SPECS.mapNotNull { spec ->
            val pair = percents[spec.key] ?: return@mapNotNull null
            val first = pair[spec.first] ?: pair[spec.first.lowercase()] ?: return@mapNotNull null
            val second = pair[spec.second] ?: pair[spec.second.lowercase()] ?: return@mapNotNull null
            MbtiDimension(
                key = spec.key,
                first = spec.first,
                second = spec.second,
                firstPercent = first,
                secondPercent = second
            )
        }

    companion object {

        private data class DimensionSpec(val key: String, val first: String, val second: String)

        /** 四维固定顺序：ei / sn / tf / jp */
        private val DIMENSION_SPECS = listOf(
            DimensionSpec("ei", "E", "I"),
            DimensionSpec("sn", "S", "N"),
            DimensionSpec("tf", "T", "F"),
            DimensionSpec("jp", "J", "P")
        )

        private const val KEY_TYPE = "type"
        private const val KEY_NAME = "name"

        /**
         * 字符串形态（早期服务端下发 `"INFP"`）：只有类型代码，其余字段留空。
         * 空白字符串视为未填写，返回 `null`。
         */
        fun fromString(raw: String?): MbtiInfo? {
            val text = raw?.trim().orEmpty()
            return if (text.isEmpty()) null else MbtiInfo(type = text)
        }

        /**
         * 容错解析入口：对象 / 字符串 / `null` / 数组等异常形态一律安全。
         *
         * - 对象：按 [MbtiInfo] 字段逐项取值，缺字段留空；
         * - 字符串：仅类型代码；
         * - `null`、数组或其它非预期形态：返回 `null`；
         * - 任何内部异常都被吞掉并返回 `null`，**绝不向上抛**（避免整条 bootstrap 解析失败）。
         */
        fun fromJson(element: JsonElement?): MbtiInfo? {
            val el = element ?: return null
            return try {
                when {
                    el.isJsonNull -> null
                    el.isJsonObject -> fromObject(el.asJsonObject)
                    el.isJsonPrimitive -> fromString(el.asString)
                    else -> null
                }
            } catch (_: Throwable) {
                null
            }
        }

        private fun fromObject(obj: JsonObject): MbtiInfo? {
            // 与 WebSocket user_joined 同一口径：优先 type，缺失时回落到 name
            val type = obj.text(KEY_TYPE) ?: obj.text(KEY_NAME) ?: return null
            return MbtiInfo(
                type = type,
                name = obj.text(KEY_NAME).orEmpty(),
                role = obj.text("role").orEmpty(),
                roleLabel = obj.text("role_label").orEmpty(),
                roleColor = obj.text("role_color").orEmpty(),
                roleDescription = obj.text("role_description").orEmpty(),
                summary = obj.text("summary").orEmpty(),
                keywords = obj.textList("keywords"),
                strengths = obj.text("strengths").orEmpty(),
                weaknesses = obj.text("weaknesses").orEmpty(),
                love = obj.text("love").orEmpty(),
                social = obj.text("social").orEmpty(),
                career = obj.text("career").orEmpty(),
                letters = obj.textMap("letters"),
                percents = obj.intMatrix("percents"),
                scores = obj.intMap("scores"),
                testedAt = obj.text("tested_at").orEmpty()
            )
        }

        // ------------------------------------------------------------
        // JsonElement 取值工具：形态不符或取值异常一律降级为 null / 空集合
        // ------------------------------------------------------------

        private fun JsonElement?.text(): String? = try {
            val el = this
            if (el == null || el.isJsonNull || !el.isJsonPrimitive) {
                null
            } else {
                el.asString.trim().takeIf { it.isNotEmpty() }
            }
        } catch (_: Throwable) {
            null
        }

        private fun JsonObject.text(key: String): String? = get(key).text()

        private fun JsonObject.textList(key: String): List<String> = try {
            get(key)
                ?.takeIf { it.isJsonArray }
                ?.asJsonArray
                ?.mapNotNull { it.text() }
                .orEmpty()
        } catch (_: Throwable) {
            emptyList()
        }

        private fun JsonObject.textMap(key: String): Map<String, String> = try {
            get(key)
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.entrySet()
                ?.mapNotNull { (k, v) -> v.text()?.let { k to it } }
                ?.toMap()
                .orEmpty()
        } catch (_: Throwable) {
            emptyMap()
        }

        private fun JsonObject.intMap(key: String): Map<String, Int> = try {
            get(key)
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.entrySet()
                ?.mapNotNull { (k, v) -> v.int()?.let { k to it } }
                ?.toMap()
                .orEmpty()
        } catch (_: Throwable) {
            emptyMap()
        }

        private fun JsonObject.intMatrix(key: String): Map<String, Map<String, Int>> = try {
            get(key)
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.entrySet()
                ?.mapNotNull { (k, v) ->
                    val row = v.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
                    val values = row.entrySet()
                        .mapNotNull { (rk, rv) -> rv.int()?.let { rk to it } }
                        .toMap()
                    if (values.isEmpty()) null else k to values
                }
                ?.toMap()
                .orEmpty()
        } catch (_: Throwable) {
            emptyMap()
        }

        private fun JsonElement?.int(): Int? = try {
            val el = this
            when {
                el == null || el.isJsonNull || !el.isJsonPrimitive -> null
                el.asJsonPrimitive.isNumber -> el.asInt
                else -> el.asString.trim().toIntOrNull()
            }
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * MBTI 单个维度的百分比（如 E 49% / I 51%）
 */
data class MbtiDimension(
    val key: String,
    val first: String,
    val second: String,
    val firstPercent: Int,
    val secondPercent: Int
) {
    /** 左侧字母占比（0f~1f）；两端都为 0 时按 0.5 处理，避免出现 0 宽或除零 */
    val firstFraction: Float
        get() {
            val total = firstPercent + secondPercent
            return if (total <= 0) 0.5f else (firstPercent.toFloat() / total).coerceIn(0f, 1f)
        }
}
