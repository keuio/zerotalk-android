package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * 官方 MBTI 测评接口（/api/mbti/...）的数据模型与容错解析。
 *
 * 独立于 top.lanxint.zerotalk.data.model.MbtiInfo：
 * - MbtiInfo 描述「用户资料上已保存的 MBTI 字段」（bootstrap / lookup 下发）；
 * - 本文件的模型描述「测评流程」本身（题目 / 量表 / 提交体 / 测评结果）。
 *
 * 所有解析入口都不依赖 Gson 反射，而是手动读 JsonObject：
 * 服务端字段缺失、类型漂移（字符串 / 数字混用）、整体畸形时一律安全降级，
 * 绝不向上抛异常（避免一条脏响应把整个页面打挂）。
 */

// ============================================================
// 1. 题目与量表 (GET /api/mbti/questions)
// ============================================================

/**
 * 单道测评题目
 *
 * @param id 题目 id（提交答案时原样回传）
 * @param text 题干
 * @param dimension 所属维度，取值 ei / sn / tf / jp
 */
data class MbtiQuestion(
    val id: Int = 0,
    val text: String = "",
    val dimension: String = ""
)

/**
 * 量表定义：labels 为选项文案，下标 0 对应分值 1，依次递增。
 */
data class MbtiScale(
    val labels: List<String> = emptyList()
)

/**
 * 题目接口的 data 对象
 *
 * @param version 量表版本号（提交时必须原样回传）
 * @param total 服务端声明的题目总数
 * @param scale 量表定义
 * @param questions 题目列表
 */
data class MbtiQuestionsData(
    val version: String = "",
    val total: Int = 0,
    val scale: MbtiScale = MbtiScale(),
    val questions: List<MbtiQuestion> = emptyList()
) {
    /** 量表选项数量（选项下标 + 1 即提交分值） */
    val scaleSize: Int get() = scale.labels.size

    /** 是否恰好为官方 60 题（数量不符时 UI 需给出提示而不是硬跑） */
    val isComplete: Boolean get() = questions.size == EXPECTED_TOTAL

    companion object {
        /** 官方固定 60 题 */
        const val EXPECTED_TOTAL = 60

        /** 从 JSON 字符串解析；空白 / 畸形 JSON 返回 null */
        fun fromJson(raw: String?): MbtiQuestionsData? {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return null
            return try {
                fromJsonElement(JsonParser.parseString(text))
            } catch (_: Throwable) {
                null
            }
        }

        /**
         * 从 JsonElement 解析；非对象形态返回 null。
         * 题目数组里非对象的元素会被跳过，其余元素缺字段时用默认值兜底。
         */
        fun fromJsonElement(element: JsonElement?): MbtiQuestionsData? {
            val obj = element.asObjectOrNull() ?: return null
            val questions = obj.array("questions").mapNotNull { item ->
                val q = item.asObjectOrNull() ?: return@mapNotNull null
                MbtiQuestion(
                    id = q.int("id") ?: 0,
                    text = q.string("text").orEmpty(),
                    dimension = q.string("dimension").orEmpty()
                )
            }
            val labels = obj.get("scale").asObjectOrNull()?.stringList("labels").orEmpty()
            return MbtiQuestionsData(
                version = obj.string("version").orEmpty(),
                total = obj.int("total") ?: questions.size,
                scale = MbtiScale(labels),
                questions = questions
            )
        }
    }
}

// ============================================================
// 2. 提交体 (POST /api/mbti/submit)
// ============================================================

/**
 * 单题作答：value 为量表分值（1..scale.labels.size）。
 */
data class MbtiAnswer(
    val id: Int,
    val value: Int
)

/**
 * 提交体（JSON body）：
 * {"version":"...","answers":[{"id":1,"value":3}, ...]}
 *
 * 字段名与官方接口逐字对应，因此这里刻意不写 @SerializedName。
 */
data class MbtiSubmitRequest(
    val version: String,
    val answers: List<MbtiAnswer>
) {
    fun toJson(gson: Gson): String = gson.toJson(this)
}

// ============================================================
// 3. 测评结果 (GET /api/mbti/me, POST /api/mbti/submit)
// ============================================================

/**
 * 官方 MBTI 测评结果
 *
 * @param type 四字母类型代码（如 INFP）
 * @param name 中文名（如「调停者」）
 * @param role 角色标识（analyst / diplomat / sentinel / explorer）
 * @param roleLabel 角色中文名（如「外交家」）
 * @param roleColor 角色配色标识（green / purple / blue / yellow）
 * @param roleDescription 角色整体说明
 * @param summary 类型摘要
 * @param keywords 关键词
 * @param strengths 优势
 * @param weaknesses 劣势
 * @param love 恋爱观
 * @param social 社交风格
 * @param career 职业倾向
 * @param letters 四维字母（{"ei":"I","sn":"N","tf":"F","jp":"P"}）
 * @param percents 四维百分比（{"ei":{"E":49,"I":51}, ...}）
 * @param scores 四维得分（{"E":22,"I":23, ...}）
 * @param testedAt 测评时间（服务端原样下发）
 */
data class MbtiResult(
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

    /** 类型代码 + 中文名（如「INFP · 调停者」）；中文名缺失时仅返回代码 */
    val displayTitle: String
        get() = if (name.isBlank() || name == type) type else type + " · " + name

    /**
     * 四维百分比（顺序固定 E/I、S/N、T/F、J/P）。
     * 只返回服务端确实下发了两个字母百分比的维度，缺失的自动跳过。
     */
    val dimensions: List<MbtiResultDimension>
        get() = DIMENSION_SPECS.mapNotNull { spec ->
            val pair = percents[spec.key] ?: return@mapNotNull null
            val first = pair[spec.first] ?: pair[spec.first.lowercase()] ?: return@mapNotNull null
            val second = pair[spec.second] ?: pair[spec.second.lowercase()] ?: return@mapNotNull null
            MbtiResultDimension(
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

        /** 从 JSON 字符串解析；空白 / 畸形 JSON 返回 null */
        fun fromJson(raw: String?): MbtiResult? {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return null
            return try {
                fromJsonElement(JsonParser.parseString(text))
            } catch (_: Throwable) {
                null
            }
        }

        /**
         * 从 JsonElement 解析；非对象形态返回 null。
         *
         * type 缺失时回落到 name（与 WebSocket user_joined 同一口径）；
         * 两者都没有才判定为无法识别并返回 null。其余字段缺失一律留空，不抛异常。
         */
        fun fromJsonElement(element: JsonElement?): MbtiResult? {
            val obj = element.asObjectOrNull() ?: return null
            val type = obj.string("type") ?: obj.string("name") ?: return null
            return MbtiResult(
                type = type,
                name = obj.string("name").orEmpty(),
                role = obj.string("role").orEmpty(),
                roleLabel = obj.string("role_label").orEmpty(),
                roleColor = obj.string("role_color").orEmpty(),
                roleDescription = obj.string("role_description").orEmpty(),
                summary = obj.string("summary").orEmpty(),
                keywords = obj.stringList("keywords"),
                strengths = obj.string("strengths").orEmpty(),
                weaknesses = obj.string("weaknesses").orEmpty(),
                love = obj.string("love").orEmpty(),
                social = obj.string("social").orEmpty(),
                career = obj.string("career").orEmpty(),
                letters = obj.stringMap("letters"),
                percents = obj.intMatrix("percents"),
                scores = obj.intMap("scores"),
                testedAt = obj.string("tested_at").orEmpty()
            )
        }
    }
}

/**
 * MBTI 单个维度的百分比（如 E 49% / I 51%）
 */
data class MbtiResultDimension(
    val key: String,
    val first: String,
    val second: String,
    val firstPercent: Int,
    val secondPercent: Int
) {
    /** 左侧字母占比（0f~1f）；两端都为 0 时按 0.5 处理，避免 0 宽或除零 */
    val firstFraction: Float
        get() {
            val total = firstPercent + secondPercent
            return if (total <= 0) 0.5f else (firstPercent.toFloat() / total).coerceIn(0f, 1f)
        }
}

/**
 * GET /api/mbti/me 的 data 对象：{"result": <MbtiResult|null>}
 */
data class MbtiMeData(
    val result: MbtiResult? = null
) {
    companion object {
        /**
         * 从 data 对象解析；非对象形态返回 null。
         * result 为 null / 缺失 / 畸形时 result 均为 null（UI 视为「尚未测评」）。
         */
        fun fromJsonElement(element: JsonElement?): MbtiMeData? {
            val obj = element.asObjectOrNull() ?: return null
            return MbtiMeData(result = MbtiResult.fromJsonElement(obj.get("result")))
        }
    }
}

// ============================================================
// 4. JsonElement 取值工具（形态不符或取值异常一律降级）
// ============================================================

private fun JsonElement?.asObjectOrNull(): JsonObject? =
    this?.takeIf { it.isJsonObject }?.asJsonObject

private fun JsonElement?.stringOrNull(): String? = try {
    if (this == null || isJsonNull || !isJsonPrimitive) null
    else asString.trim().takeIf { it.isNotEmpty() }
} catch (_: Throwable) {
    null
}

private fun JsonObject.string(key: String): String? = get(key).stringOrNull()

private fun JsonElement?.intOrNull(): Int? = try {
    when {
        this == null || isJsonNull || !isJsonPrimitive -> null
        asJsonPrimitive.isNumber -> asInt
        else -> asString.trim().toIntOrNull()
    }
} catch (_: Throwable) {
    null
}

private fun JsonObject.int(key: String): Int? = get(key).intOrNull()

private fun JsonObject.array(key: String): List<JsonElement> = try {
    get(key)?.takeIf { it.isJsonArray }?.asJsonArray?.toList().orEmpty()
} catch (_: Throwable) {
    emptyList()
}

/** 只接受 JSON 字符串字面量（数字 / 布尔等异常元素直接跳过） */
private fun JsonElement?.stringLiteralOrNull(): String? = try {
    if (this == null || isJsonNull || !isJsonPrimitive || !asJsonPrimitive.isString) null
    else asString.trim().takeIf { it.isNotEmpty() }
} catch (_: Throwable) {
    null
}

private fun JsonObject.stringList(key: String): List<String> =
    array(key).mapNotNull { it.stringLiteralOrNull() }

private fun JsonObject.stringMap(key: String): Map<String, String> = try {
    get(key).asObjectOrNull()
        ?.entrySet()
        ?.mapNotNull { (k, v) -> v.stringOrNull()?.let { k to it } }
        ?.toMap()
        .orEmpty()
} catch (_: Throwable) {
    emptyMap()
}

private fun JsonObject.intMap(key: String): Map<String, Int> = try {
    get(key).asObjectOrNull()
        ?.entrySet()
        ?.mapNotNull { (k, v) -> v.intOrNull()?.let { k to it } }
        ?.toMap()
        .orEmpty()
} catch (_: Throwable) {
    emptyMap()
}

private fun JsonObject.intMatrix(key: String): Map<String, Map<String, Int>> = try {
    get(key).asObjectOrNull()
        ?.entrySet()
        ?.mapNotNull { (k, v) ->
            val row = v.asObjectOrNull() ?: return@mapNotNull null
            val values = row.entrySet()
                .mapNotNull { (rk, rv) -> rv.intOrNull()?.let { rk to it } }
                .toMap()
            if (values.isEmpty()) null else k to values
        }
        ?.toMap()
        .orEmpty()
} catch (_: Throwable) {
    emptyMap()
}
