package top.lanxint.zerotalk.data.model

import com.google.gson.Gson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import top.lanxint.zerotalk.data.network.PeerUserBootstrapDto
import top.lanxint.zerotalk.data.network.PeerUserDto

/**
 * 需求 R2：匿名模式匹配下真实名称泄露与设置同步单元测试
 */
class AnonymousPrivacyTest {

    private val gson = Gson()

    @Test
    fun testPeerUserDto_privacyMode_jsonMapping() {
        // 1. 标准 key: privacy_mode
        val json1 = """{"user_id": 101, "username": "Alice", "privacy_mode": true}"""
        val dto1 = gson.fromJson(json1, PeerUserDto::class.java)
        assertEquals(true, dto1.privacyMode)

        // 2. 驼峰 key: privacyMode
        val json2 = """{"user_id": 102, "username": "Bob", "privacyMode": true}"""
        val dto2 = gson.fromJson(json2, PeerUserDto::class.java)
        assertEquals(true, dto2.privacyMode)

        // 3. 匹配接口常用 key: match_privacy_mode
        val json3 = """{"user_id": 103, "username": "Charlie", "match_privacy_mode": true}"""
        val dto3 = gson.fromJson(json3, PeerUserDto::class.java)
        assertEquals(true, dto3.privacyMode)

        // 4. 未传时缺省为 null
        val json4 = """{"user_id": 104, "username": "Dave"}"""
        val dto4 = gson.fromJson(json4, PeerUserDto::class.java)
        assertNull(dto4.privacyMode)

        // 5. PeerUserBootstrapDto 别名可用
        val bootstrapDto: PeerUserBootstrapDto = dto1
        assertEquals(101L, bootstrapDto.userId)
        assertEquals(true, bootstrapDto.privacyMode)
    }

    @Test
    fun testOtherUserProfile_anonymized_masksSensitiveFields() {
        val original = OtherUserProfile(
            userId = "888",
            uid = "abcdef888",
            name = "真实张三",
            genderText = "男",
            ageRangeText = "21岁",
            location = "上海市 · 浦东新区",
            avatarUrl = "https://oss/zhangsan.jpg",
            avatarFallback = "张",
            bio = "热爱社交，喜欢摄影与旅行",
            title = "社牛达人",
            titleColor = "purple",
            followingCount = 120,
            followerCount = 450,
            likeCount = 888,
            mutualCount = 35,
            privacyMode = false,
            moments = listOf(
                MomentItem(id = "1", authorId = "888", authorName = "真实张三", textContent = "今天天气真好")
            ),
            hasMoreMoments = true,
            mbti = MbtiInfo("ENFP", "竞选者", "外向热情")
        )

        val anonymized = original.anonymized()

        // 验证脱敏结果
        assertEquals("匿名用户", anonymized.name)
        assertEquals("", anonymized.avatarUrl)
        assertEquals("匿", anonymized.avatarFallback)
        assertEquals("这个人很神秘，还没有写简介", anonymized.bio)
        assertEquals("", anonymized.location)
        assertEquals("", anonymized.genderText)
        assertEquals("", anonymized.ageRangeText)
        assertEquals("", anonymized.title)
        assertEquals("", anonymized.titleColor)
        assertEquals(0, anonymized.followingCount)
        assertEquals(0, anonymized.followerCount)
        assertEquals(0, anonymized.likeCount)
        assertEquals(0, anonymized.mutualCount)
        assertTrue(anonymized.privacyMode)
        assertTrue(anonymized.moments.isEmpty())
        assertFalse(anonymized.hasMoreMoments)
        assertNull(anonymized.mbti)

        // 基础身份标识保留（用于举报或拉黑等系统功能）
        assertEquals("888", anonymized.userId)
        assertEquals("abcdef888", anonymized.uid)
    }

    @Test
    fun testAnonymousPeerDetection_rules() {
        fun isAnonymousPeer(
            targetName: String,
            bootstrapPrivacyMode: Boolean?,
            peerPrivacyMode: Boolean?
        ): Boolean {
            return targetName == "匿名用户" ||
                bootstrapPrivacyMode == true ||
                peerPrivacyMode == true
        }

        // 1. 会话目标名为匿名用户
        assertTrue(isAnonymousPeer("匿名用户", false, false))

        // 2. 房间 bootstrap 标记隐私模式
        assertTrue(isAnonymousPeer("李四", true, false))

        // 3. 聚合资料标记隐私模式
        assertTrue(isAnonymousPeer("王五", false, true))

        // 4. 均未标记
        assertFalse(isAnonymousPeer("赵六", false, false))
        assertFalse(isAnonymousPeer("赵六", null, null))
    }
}
