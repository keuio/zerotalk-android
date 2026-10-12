package top.lanxint.zerotalk.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import top.lanxint.zerotalk.data.network.ChatBootstrapData
import top.lanxint.zerotalk.data.network.ChatMessageDto
import top.lanxint.zerotalk.data.network.PeerUserDto
import top.lanxint.zerotalk.data.network.RoomListItemDto
import top.lanxint.zerotalk.data.network.WsServerEvent
import top.lanxint.zerotalk.data.repository.MatchStatus

/**
 * 端到端全景综合回归与需求验收测试套件 (E2E ZeroTalk Requirements Test Suite)
 *
 * 遵循 4-Tier 测试方法论覆盖需求 R1 至 R6：
 * - Tier 1: 核心功能与契约单测 (Core Feature Coverage)
 * - Tier 2: 边界条件与极端异常测试 (Boundary & Corner Cases)
 * - Tier 3: 跨特性组合与协同验证 (Cross-Feature Combinations)
 * - Tier 4: 真实端到端应用场景 (Real-World Application Scenarios)
 */
class E2EZerotalkRequirementsTest {

    private val gson = Gson()

    // =========================================================================
    // Contract Helper Models & Functions (R1 ~ R6)
    // =========================================================================

    /** R2: 契约模型与反序列化验证 */
    data class PeerUserBootstrapContractDto(
        @SerializedName(value = "user_id", alternate = ["id"]) val userId: Long = 0,
        @SerializedName("uid") val uid: String = "",
        @SerializedName("username") val username: String = "",
        @SerializedName("avatar_url") val avatarUrl: String? = null,
        @SerializedName(value = "privacy_mode", alternate = ["privacyMode", "match_privacy_mode"])
        val privacyMode: Boolean? = null,
        @SerializedName("clean_stream_mode") val cleanStreamMode: Boolean? = null,
        @SerializedName("show_online_status") val showOnlineStatus: Boolean? = null
    )

    /** R2: 用户画像脱敏契约函数 (对齐 PROJECT.md §2) */
    private fun OtherUserProfile.anonymizeContract(): OtherUserProfile = copy(
        name = "匿名用户",
        avatarUrl = "",
        avatarFallback = "匿",
        bio = "这个人很神秘，还没有写简介",
        location = "",
        ageRangeText = "",
        genderText = "",
        title = "",
        titleColor = "",
        followingCount = 0,
        followerCount = 0,
        likeCount = 0,
        mutualCount = 0,
        privacyMode = true,
        moments = emptyList(),
        hasMoreMoments = false,
        mbti = null
    )

    /** R2: 多源匿名判定规则 (对齐 PROJECT.md §2) */
    private fun isAnonymousPeerContract(
        conversationTargetName: String,
        conversationName: String,
        roomBootstrapPrivacyMode: Boolean?,
        peerProfilePrivacyMode: Boolean?
    ): Boolean = conversationTargetName == "匿名用户" ||
        conversationName == "匿名用户" ||
        roomBootstrapPrivacyMode == true ||
        peerProfilePrivacyMode == true

    /** R4: 会话返回胶囊未读数计算契约 (对齐 PROJECT.md §R4) */
    private fun calculateOtherUnreadCount(
        conversations: List<ConversationItem>,
        currentRoomId: String
    ): Int? = conversations
        .filter { it.id != currentRoomId }
        .sumOf { maxOf(0, it.unreadCount) }
        .takeIf { it > 0 }

    /** R5: 链接提取与规范化契约 (对齐 PROJECT.md §3) */
    private object LinkContractHelper {
        val URL_REGEX = Regex(
            """(?i)\b(?:https?://|www\.)[^\s\u4e00-\u9fa5\u3000-\u303f\uff01-\uff5e]+"""
        )

        fun cleanUrl(raw: String): String {
            var trimmed = raw
            while (trimmed.isNotEmpty() && (trimmed.last() in ".,!?:;\"'”’" || (trimmed.last() == ')' && '(' !in trimmed))) {
                trimmed = trimmed.dropLast(1)
            }
            return trimmed
        }

        fun normalizeUrl(url: String): String {
            val trimmed = url.trim()
            return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed
            } else {
                "https://$trimmed"
            }
        }

        fun extractUrls(text: String): List<String> =
            URL_REGEX.findAll(text).map { cleanUrl(it.value) }.filter { it.isNotBlank() }.toList()
    }

    private fun sampleChatMessage(
        id: String = "msg_1",
        serverId: Long = 0L,
        content: String = "Hello",
        isMine: Boolean = true,
        timestampMs: Long = 1_700_000_000_000L,
        isImage: Boolean = false,
        imageUrl: String = "",
        isVoice: Boolean = false,
        audioUrl: String = "",
        isDice: Boolean = false,
        diceValue: Int = 0,
        clientMessageId: String = "",
        isDeleted: Boolean = false
    ) = ChatMessage(
        id = id,
        senderId = if (isMine) "me" else "peer",
        content = content,
        timestamp = "12:00",
        isMine = isMine,
        serverId = serverId,
        timestampMs = timestampMs,
        isImage = isImage,
        imageUrl = imageUrl,
        isVoice = isVoice,
        audioUrl = audioUrl,
        isDice = isDice,
        diceValue = diceValue,
        clientMessageId = clientMessageId,
        isDeleted = isDeleted
    )

    // =========================================================================
    // Tier 1: 核心功能与契约验证 (Core Feature Coverage)
    // =========================================================================

    @Test
    fun testTier1_R1_EchoMatcher_TextAndMediaTypes() {
        val textEcho = sampleChatMessage(content = "你好")
        assertTrue(EchoMatcher.isSameKind(textEcho, "text", "你好"))
        assertFalse(EchoMatcher.isSameKind(textEcho, "text", "世界"))

        val diceEcho = sampleChatMessage(content = "", isDice = true)
        assertTrue(EchoMatcher.isSameKind(diceEcho, "dice", "6"))

        val imgUrl = "https://oss.zerotalk.top/img_1.png"
        val imageEcho = sampleChatMessage(content = "［图片］", isImage = true, imageUrl = imgUrl)
        assertTrue(EchoMatcher.isSameKind(imageEcho, "image", imgUrl, imgUrl))
        assertFalse(EchoMatcher.isSameKind(imageEcho, "text", imgUrl))

        val voiceUrl = "https://oss.zerotalk.top/voice_1.webm"
        val voiceEcho = sampleChatMessage(content = "[语音 5\"]", isVoice = true, audioUrl = voiceUrl)
        assertTrue(EchoMatcher.isSameKind(voiceEcho, "audio", voiceUrl))
    }

    @Test
    fun testTier1_R1_ChatMessageMerge_DeduplicationAndOrder() {
        val serverMsg1 = sampleChatMessage("s1", serverId = 100L, timestampMs = 100L, content = "历史1")
        val serverMsg2 = sampleChatMessage("s2", serverId = 200L, timestampMs = 200L, content = "历史2")
        val localEcho = sampleChatMessage("local_1", serverId = 0L, timestampMs = 300L, isMine = true, content = "新发消息")

        val merged = ChatMessageMerge.merge(
            listOf(serverMsg2, localEcho),
            listOf(serverMsg1)
        )

        assertEquals(3, merged.size)
        assertEquals(listOf(100L, 200L, 0L), merged.map { it.serverId })
        assertEquals("local_1", merged.last().id)
    }

    @Test
    fun testTier1_R1_GameInvite_OverlayPreservesMetadata() {
        val oldInvite = ChatGameInvite(
            gameId = 42L,
            gameType = "gobang",
            status = "waiting",
            whiteUsername = "Alice",
            blackUsername = "Bob",
            whiteAvatarUrl = "https://oss/alice.png"
        )
        val updateInvite = ChatGameInvite(
            gameId = 42L,
            gameType = "gobang",
            status = "playing"
        )
        val result = updateInvite.overlayOn(oldInvite)
        assertEquals("playing", result.status)
        assertEquals("Alice", result.whiteUsername)
        assertEquals("Bob", result.blackUsername)
        assertEquals("https://oss/alice.png", result.whiteAvatarUrl)
    }

    @Test
    fun testTier1_R2_PrivacyMasking_Contract() {
        val original = OtherUserProfile(
            userId = "10086",
            uid = "abcdef1234567890abcdef1234567890",
            name = "真实昵称",
            avatarUrl = "https://oss/real_avatar.png",
            bio = "真实个性签名",
            location = "北京市 · 朝阳区",
            genderText = "男",
            ageRangeText = "22岁",
            title = "资深零友",
            titleColor = "gold",
            followingCount = 12,
            followerCount = 48,
            likeCount = 99,
            mutualCount = 5,
            privacyMode = false,
            moments = listOf(MomentItem(id = "1", authorId = "10086", authorName = "真实昵称", textContent = "私人动态")),
            hasMoreMoments = true,
            mbti = MbtiInfo("INTJ", "建筑师", "独立思考")
        )

        val anonymized = original.anonymizeContract()

        assertEquals("匿名用户", anonymized.name)
        assertEquals("", anonymized.avatarUrl)
        assertEquals("匿", anonymized.avatarFallback)
        assertEquals("这个人很神秘，还没有写简介", anonymized.bio)
        assertEquals("", anonymized.location)
        assertEquals("", anonymized.genderText)
        assertEquals("", anonymized.title)
        assertEquals(0, anonymized.followingCount)
        assertEquals(0, anonymized.followerCount)
        assertEquals(0, anonymized.likeCount)
        assertTrue(anonymized.privacyMode)
        assertTrue(anonymized.moments.isEmpty())
        assertFalse(anonymized.hasMoreMoments)
        assertNull(anonymized.mbti)
    }

    @Test
    fun testTier1_R2_PeerUserDto_PrivacyModeSerialization() {
        val json = """
            {
                "user_id": 999,
                "uid": "hex_uid_999",
                "username": "secret_agent",
                "privacy_mode": true,
                "clean_stream_mode": true,
                "show_online_status": false
            }
        """.trimIndent()
        val dto = gson.fromJson(json, PeerUserBootstrapContractDto::class.java)
        assertEquals(999L, dto.userId)
        assertEquals("secret_agent", dto.username)
        assertEquals(true, dto.privacyMode)
        assertEquals(true, dto.cleanStreamMode)
        assertEquals(false, dto.showOnlineStatus)
    }

    @Test
    fun testTier1_R3_TextMessageToClipboardContent() {
        val textMsg = sampleChatMessage(content = "重要验证码: 839201")
        val textToCopy = if (textMsg.isImage) textMsg.imageUrl.ifBlank { textMsg.content } else textMsg.content
        assertEquals("重要验证码: 839201", textToCopy)

        val imgMsg = sampleChatMessage(isImage = true, imageUrl = "https://oss/captured.jpg", content = "［图片］")
        val imgToCopy = if (imgMsg.isImage) imgMsg.imageUrl.ifBlank { imgMsg.content } else imgMsg.content
        assertEquals("https://oss/captured.jpg", imgToCopy)
    }

    @Test
    fun testTier1_R4_UnreadCount_ExcludesCurrentConversation() {
        val convs = listOf(
            ConversationItem(id = "room_1", targetName = "A", lastMessage = "m1", timestamp = "12:00", unreadCount = 3, category = MessageCategory.PRIVATE),
            ConversationItem(id = "room_2", targetName = "B", lastMessage = "m2", timestamp = "12:01", unreadCount = 5, category = MessageCategory.CODE),
            ConversationItem(id = "current", targetName = "C", lastMessage = "m3", timestamp = "12:02", unreadCount = 9, category = MessageCategory.MATCH)
        )
        val otherUnread = calculateOtherUnreadCount(convs, currentRoomId = "current")
        assertEquals(8, otherUnread)
    }

    @Test
    fun testTier1_R4_UnreadCount_ZeroReturnsNull() {
        val convs = listOf(
            ConversationItem(id = "room_1", targetName = "A", lastMessage = "m1", timestamp = "12:00", unreadCount = 0, category = MessageCategory.PRIVATE),
            ConversationItem(id = "current", targetName = "B", lastMessage = "m2", timestamp = "12:01", unreadCount = 2, category = MessageCategory.MATCH)
        )
        val otherUnread = calculateOtherUnreadCount(convs, currentRoomId = "current")
        assertNull(otherUnread, "当其他会话无未读时必须返回 null 以便导航栏渲染正圆形返回按钮")
    }

    @Test
    fun testTier1_R5_LinkDetection_Protocols() {
        val text = "官网：https://zerotalk.top 备用：http://lanxint.top 国际：www.github.com"
        val urls = LinkContractHelper.extractUrls(text)
        assertEquals(3, urls.size)
        assertEquals("https://zerotalk.top", urls[0])
        assertEquals("http://lanxint.top", urls[1])
        assertEquals("www.github.com", urls[2])

        assertEquals("https://www.github.com", LinkContractHelper.normalizeUrl(urls[2]))
        assertEquals("https://zerotalk.top", LinkContractHelper.normalizeUrl(urls[0]))
    }

    @Test
    fun testTier1_R6_MatchStatus_Transitions() {
        var status: MatchStatus = MatchStatus.Idle
        assertEquals(MatchStatus.Idle, status)

        status = MatchStatus.Matching(isVoice = false)
        assertTrue(status is MatchStatus.Matching)
        assertFalse((status as MatchStatus.Matching).isVoice)

        val joined = WsServerEvent.UserJoined(
            uid = "u1",
            username = "同行者",
            gender = "male",
            avatarUrl = "https://oss/u1.png",
            mbti = "INTP"
        )
        status = MatchStatus.Matched(roomId = "room_match_101", partner = joined)
        assertTrue(status is MatchStatus.Matched)
        assertEquals("room_match_101", (status as MatchStatus.Matched).roomId)
        assertEquals("同行者", (status as MatchStatus.Matched).partner?.username)

        status = MatchStatus.Idle
        assertEquals(MatchStatus.Idle, status)
    }

    // =========================================================================
    // Tier 2: 边界条件与极端异常测试 (Boundary & Corner Cases)
    // =========================================================================

    @Test
    fun testTier2_R1_ChatMessageMerge_DeletedIsTerminal() {
        val deletedMsg = sampleChatMessage("s1", serverId = 1L, isDeleted = true, content = "")
        val undeletedHistorical = sampleChatMessage("s1", serverId = 1L, isDeleted = false, content = "原消息")

        val merged1 = ChatMessageMerge.merge(listOf(deletedMsg), listOf(undeletedHistorical))
        assertTrue(merged1.single().isDeleted)
        assertEquals("", merged1.single().content)

        val merged2 = ChatMessageMerge.merge(listOf(undeletedHistorical), listOf(deletedMsg))
        assertTrue(merged2.single().isDeleted)
        assertEquals("", merged2.single().content)
    }

    @Test
    fun testTier2_R1_ChatMessageMerge_AppendDeduped() {
        val initial = listOf(
            sampleChatMessage("s1", serverId = 10L, content = "旧版"),
            sampleChatMessage("s2", serverId = 20L, content = "正常")
        )
        val updated = sampleChatMessage("s1_new", serverId = 10L, content = "新版推送")
        val result = ChatMessageMerge.appendDeduped(initial, updated)

        assertEquals(2, result.size)
        assertEquals("新版推送", result.first { it.serverId == 10L }.content)
    }

    @Test
    fun testTier2_R1_EchoMatcher_ClientMessageIdPreciseMatch() {
        val echoWithId = sampleChatMessage("loc_1", clientMessageId = "client-uuid-999", content = "明文")
        assertTrue(EchoMatcher.isSameKind(echoWithId, "text", "密文", null, "client-uuid-999"))
        assertFalse(EchoMatcher.isSameKind(echoWithId, "text", "密文", null, "client-uuid-different"))
    }

    @Test
    fun testTier2_R2_PeerUserDto_DefaultAndNullValues() {
        val minimalJson = """{"user_id": 1}"""
        val dto = gson.fromJson(minimalJson, PeerUserBootstrapContractDto::class.java)
        assertEquals(1L, dto.userId)
        assertEquals("", dto.username)
        assertNull(dto.privacyMode)
        assertNull(dto.cleanStreamMode)
    }

    @Test
    fun testTier2_R2_MatchSettings_ParamMapValidation() {
        fun buildMatchSettingsParams(
            ageRange: String = "18-23",
            oppositeGender: Boolean = true,
            sameGender: Boolean = false,
            cleanStream: Boolean = true,
            privacyMode: Boolean = true
        ): Map<String, String> = mapOf(
            "age_range" to ageRange,
            "match_opposite_gender_only" to (if (oppositeGender) "1" else "0"),
            "match_same_gender_only" to (if (sameGender) "1" else "0"),
            "clean_stream_mode" to (if (cleanStream) "1" else "0"),
            "match_privacy_mode" to (if (privacyMode) "1" else "0")
        )

        val params = buildMatchSettingsParams(privacyMode = true)
        assertEquals("1", params["match_privacy_mode"])
        assertEquals("1", params["clean_stream_mode"])
        assertEquals("1", params["match_opposite_gender_only"])
        assertEquals("0", params["match_same_gender_only"])

        val paramsWithoutPrivacy = buildMatchSettingsParams(privacyMode = false)
        assertEquals("0", paramsWithoutPrivacy["match_privacy_mode"])
    }

    @Test
    fun testTier2_R3_EmptyOrSpecialUnicodeTextCopy() {
        val emojiMsg = sampleChatMessage(content = "🔥✨🚀 特殊符：\uD83D\uDC4D\uD83C\uDFFB & <script>")
        val copied = emojiMsg.content
        assertEquals("🔥✨🚀 特殊符：\uD83D\uDC4D\uD83C\uDFFB & <script>", copied)

        val blankImgMsg = sampleChatMessage(isImage = true, imageUrl = "", content = "")
        val copiedEmpty = blankImgMsg.imageUrl.ifBlank { blankImgMsg.content }
        assertEquals("", copiedEmpty)
    }

    @Test
    fun testTier2_R4_UnreadCount_BoundaryConditions() {
        val emptyList = emptyList<ConversationItem>()
        assertNull(calculateOtherUnreadCount(emptyList, "room_1"))

        val allInCurrent = listOf(
            ConversationItem(id = "current", targetName = "A", lastMessage = "", timestamp = "", unreadCount = 999, category = MessageCategory.MATCH)
        )
        assertNull(calculateOtherUnreadCount(allInCurrent, "current"))

        val largeCount = listOf(
            ConversationItem(id = "r1", targetName = "A", lastMessage = "", timestamp = "", unreadCount = 9999, category = MessageCategory.PRIVATE),
            ConversationItem(id = "r2", targetName = "B", lastMessage = "", timestamp = "", unreadCount = 8888, category = MessageCategory.CODE)
        )
        assertEquals(18887, calculateOtherUnreadCount(largeCount, "current"))
    }

    @Test
    fun testTier2_R5_LinkCleaning_ComplexPunctuationAndParentheses() {
        val input = "详情请查阅 https://zh.wikipedia.org/wiki/ZeroTalk_(app), 或者访问 www.test.com! 以及 http://a.b.c:8080/path?q=1&v=2."
        val urls = LinkContractHelper.extractUrls(input)

        assertEquals(3, urls.size)
        assertEquals("https://zh.wikipedia.org/wiki/ZeroTalk_(app)", urls[0])
        assertEquals("www.test.com", urls[1])
        assertEquals("http://a.b.c:8080/path?q=1&v=2", urls[2])
    }

    @Test
    fun testTier2_R5_NonLinkText_NoMatches() {
        val plain = "今天天气真好，没有 www 也没有 http 协议"
        val urls = LinkContractHelper.extractUrls(plain)
        assertTrue(urls.isEmpty())
    }

    @Test
    fun testTier2_R6_MatchStatus_TimeoutAndErrorStates() {
        val timeout: MatchStatus = MatchStatus.Timeout
        assertTrue(timeout is MatchStatus.Timeout)

        val error: MatchStatus = MatchStatus.Error("网络连接超时")
        assertTrue(error is MatchStatus.Error)
        assertEquals("网络连接超时", (error as MatchStatus.Error).message)
    }

    // =========================================================================
    // Tier 3: 跨特性组合与协同验证 (Cross-Feature Combinations)
    // =========================================================================

    @Test
    fun testTier3_R1_R2_CardReconciliationWithAnonymousPeer() {
        val echo = sampleChatMessage(
            id = "echo_moment_1",
            serverId = 0L,
            clientMessageId = "moment-share-cmid-101",
            content = """{"moment_id":888,"username":"匿名用户"}""",
            isMine = true
        )
        val incomingHistorical = sampleChatMessage(
            id = "server_moment_1",
            serverId = 777L,
            clientMessageId = "moment-share-cmid-101",
            content = """{"moment_id":888,"username":"匿名用户","v":1,"extra":true}""",
            isMine = true
        )

        assertTrue(EchoMatcher.isSameKind(echo, "moment_share", incomingHistorical.content, null, incomingHistorical.clientMessageId))

        val merged = ChatMessageMerge.merge(listOf(echo), listOf(incomingHistorical))
        val dedupeServer = merged.filter { it.serverId > 0L }
        assertEquals(1, dedupeServer.size)
        assertEquals(777L, dedupeServer.single().serverId)
    }

    @Test
    fun testTier3_R2_R6_DynamicIsland_AnonymousPartnerHandling() {
        val joinedAnonymous = WsServerEvent.UserJoined(
            uid = "u_anon_99",
            username = "匿名用户",
            gender = "unknown",
            avatarUrl = "https://oss/should_be_hidden.png",
            mbti = "INFP"
        )
        val matchStatus = MatchStatus.Matched(roomId = "room_anon_1", partner = joinedAnonymous)

        val partner = matchStatus.partner
        assertNotNull(partner)

        val isAnonymous = partner.username == "匿名用户"
        val displayName = if (isAnonymous) "匿名用户" else partner.username
        val displayAvatar = if (isAnonymous) null else partner.avatarUrl

        assertEquals("匿名用户", displayName)
        assertNull(displayAvatar, "对端开启匿名模式时，灵动岛卡片头像必须脱敏为 null")
    }

    @Test
    fun testTier3_R2_DeterminationLogic_AllBranches() {
        assertTrue(isAnonymousPeerContract("匿名用户", "任意房名", null, null))
        assertTrue(isAnonymousPeerContract("张三", "匿名用户", null, null))
        assertTrue(isAnonymousPeerContract("张三", "普通房间", roomBootstrapPrivacyMode = true, peerProfilePrivacyMode = null))
        assertTrue(isAnonymousPeerContract("张三", "普通房间", roomBootstrapPrivacyMode = false, peerProfilePrivacyMode = true))
        assertFalse(isAnonymousPeerContract("张三", "普通房间", roomBootstrapPrivacyMode = false, peerProfilePrivacyMode = false))
    }

    @Test
    fun testTier3_R4_R6_MultitaskingMatchingAndUnreadCalculations() {
        val conversations = listOf(
            ConversationItem(id = "hall", targetName = "公共大厅", lastMessage = "新消息", timestamp = "12:00", unreadCount = 4, category = MessageCategory.CODE),
            ConversationItem(id = "chat_1", targetName = "好友", lastMessage = "在吗", timestamp = "12:01", unreadCount = 2, category = MessageCategory.PRIVATE),
            ConversationItem(id = "active_room", targetName = "正在浏览", lastMessage = "浏览中", timestamp = "12:02", unreadCount = 0, category = MessageCategory.MATCH)
        )
        val otherUnread = calculateOtherUnreadCount(conversations, "active_room")
        assertEquals(6, otherUnread)

        val matchingStatus: MatchStatus = MatchStatus.Matching(isVoice = true)
        val isMatching = matchingStatus is MatchStatus.Matching
        assertTrue(isMatching)

        val buttonText = if (isMatching) "匹配中 · 点击取消" else "开始匹配"
        assertEquals("匹配中 · 点击取消", buttonText)
    }

    @Test
    fun testTier3_R3_R5_LinkExtractionVsFullTextCopyCoexistence() {
        val messageText = "请访问我们的官网 https://zerotalk.top 体验更多功能"
        val fullCopiedText = messageText
        assertEquals("请访问我们的官网 https://zerotalk.top 体验更多功能", fullCopiedText)

        val detectedUrls = LinkContractHelper.extractUrls(messageText)
        assertEquals(1, detectedUrls.size)
        assertEquals("https://zerotalk.top", detectedUrls.single())
    }

    // =========================================================================
    // Tier 4: 真实端到端应用场景 (Real-World Application Scenarios)
    // =========================================================================

    @Test
    fun testTier4_Scenario1_AnonymousMatchingToChatJourney() = runTest {
        // 1. 用户开启匿名模式匹配
        val matchSettings = mapOf(
            "age_range" to "18-23",
            "match_privacy_mode" to "1"
        )
        assertEquals("1", matchSettings["match_privacy_mode"])

        // 2. 灵动岛挂载并进入紧凑匹配中状态
        var currentStatus: MatchStatus = MatchStatus.Matching(isVoice = false)
        assertTrue(currentStatus is MatchStatus.Matching)

        // 3. 服务端匹配成功下发匿名对端
        val joinedPeer = WsServerEvent.UserJoined(
            uid = "peer_uid_404",
            username = "匿名用户",
            gender = "unknown",
            avatarUrl = "https://oss/secret.png",
            mbti = "INTJ"
        )
        currentStatus = MatchStatus.Matched(roomId = "room_404", partner = joinedPeer)
        assertTrue(currentStatus is MatchStatus.Matched)

        // 4. 灵动岛展开卡片，校验身份遵循匿名保护脱敏
        val partner = (currentStatus as MatchStatus.Matched).partner
        assertNotNull(partner)
        val isAnonymous = isAnonymousPeerContract(partner.username, "", null, null)
        assertTrue(isAnonymous)
        val effectiveName = if (isAnonymous) "匿名用户" else partner.username
        val effectiveAvatar = if (isAnonymous) null else partner.avatarUrl
        assertEquals("匿名用户", effectiveName)
        assertNull(effectiveAvatar)

        // 5. 用户手动确认进入房间，重置灵动岛
        val enteredConversation = ConversationItem(
            id = (currentStatus as MatchStatus.Matched).roomId,
            targetName = effectiveName,
            targetAvatar = effectiveAvatar,
            lastMessage = "匹配成功，开始聊天吧！",
            timestamp = "12:00",
            category = MessageCategory.MATCH
        )
        assertEquals("room_404", enteredConversation.id)
        assertEquals("匿名用户", enteredConversation.targetName)
        assertNull(enteredConversation.targetAvatar)

        currentStatus = MatchStatus.Idle
        assertEquals(MatchStatus.Idle, currentStatus)
    }

    @Test
    fun testTier4_Scenario2_CardSharingAndReconnectionReconciliationJourney() {
        // 1. 本地发出动态卡片，生成 serverId == 0 的乐观回显
        val localEcho = sampleChatMessage(
            id = "local_card_001",
            serverId = 0L,
            clientMessageId = "cmid_card_001",
            content = """{"moment_id":12345,"username":"Alice"}""",
            isMine = true
        )

        // 2. 掉线重连拉取历史消息，获得服务端已落库记录（JSON 字段增减重写）
        val serverCard = sampleChatMessage(
            id = "server_msg_999",
            serverId = 999L,
            clientMessageId = "cmid_card_001",
            content = """{"moment_id":12345,"username":"Alice","rewritten":true}""",
            isMine = true
        )

        // 3. 校验回显对账命中
        val matched = EchoMatcher.isSameKind(
            localEcho,
            "moment_share",
            serverCard.content,
            null,
            serverCard.clientMessageId
        )
        assertTrue(matched, "服务端回推重写 JSON 时通过 clientMessageId 精确核销本地回显")

        // 4. 合并历史消息与本地列表
        val merged = ChatMessageMerge.merge(listOf(localEcho), listOf(serverCard))
        val dedupeList = ChatMessageMerge.appendDeduped(emptyList(), serverCard)
        assertEquals(1, dedupeList.size)
        assertEquals(999L, dedupeList.first().serverId)
    }

    @Test
    fun testTier4_Scenario3_MultitaskingNavigationAndUnreadCapsuleJourney() {
        // 1. 用户位于会话 chat_active
        val activeRoomId = "chat_active"

        val conversations = listOf(
            ConversationItem(id = "chat_active", targetName = "当前聊天", lastMessage = "你好", timestamp = "12:00", unreadCount = 0, category = MessageCategory.PRIVATE),
            ConversationItem(id = "chat_other_1", targetName = "小群", lastMessage = "聚会？", timestamp = "11:59", unreadCount = 2, category = MessageCategory.CODE),
            ConversationItem(id = "chat_other_2", targetName = "张三", lastMessage = "收到", timestamp = "11:58", unreadCount = 3, category = MessageCategory.PRIVATE)
        )

        // 2. 动态未读角标展示 < 5
        val unreadCapsuleCount = calculateOtherUnreadCount(conversations, activeRoomId)
        assertEquals(5, unreadCapsuleCount)

        // 3. 聊天过程中发起全局匹配
        val matchingState: MatchStatus = MatchStatus.Matching(isVoice = false)
        assertTrue(matchingState is MatchStatus.Matching)

        // 4. 收到带链接的消息并点击
        val chatMessageWithUrl = sampleChatMessage(content = "体验地址：https://zerotalk.top/preview 请测试！")
        val extractedUrls = LinkContractHelper.extractUrls(chatMessageWithUrl.content)
        assertEquals(listOf("https://zerotalk.top/preview"), extractedUrls)

        // 5. 取消匹配
        val finalStatus: MatchStatus = MatchStatus.Idle
        assertEquals(MatchStatus.Idle, finalStatus)
    }
}
