package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.lanxint.zerotalk.data.model.*
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 零语 REST API 服务封装
 */
class ZeroTalkApiService(
    val cookieJar: ZeroTalkCookieJar = ZeroTalkCookieJar()
) {
    /**
     * 全站统一 Gson：挂载宽容布尔/数字适配器，兼容服务端 true/false、1/0、\"1\" 混用的下发形态
     */
    val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(FlexibleBooleanTypeAdapterFactory)
        .registerTypeAdapterFactory(FlexibleNumberTypeAdapterFactory)
        .registerTypeAdapterFactory(NullTextCoercingTypeAdapterFactory)
        .create()

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("X-Device-Id", DeviceIdManager.getDeviceId())
                .header("Accept", "application/json, text/plain, */*")
                .build()
            chain.proceed(request)
        }
        .build()

    companion object {
        const val BASE_URL = "https://app.zerotalk.cn"
        const val WS_URL = "wss://app.zerotalk.cn/ws"

        /** 房间成员接口列表字段候选名（按优先级） */
        private val ROOM_MEMBER_LIST_KEYS = arrayOf("list", "members", "items", "users")

        /** 禁止加入名单接口列表字段候选名（按优先级） */
        private val ROOM_BAN_LIST_KEYS = arrayOf("list", "bans", "items", "users")
    }

    /**
     * 账号登录接口
     */
    suspend fun login(username: String, password: String): Result<LoginData> = apiDataRequestRequired(
        path = "/auth/login",
        type = object : TypeToken<ApiResponse<LoginData>>() {}.type,
        params = mapOf(
            "username" to username,
            "password" to password
        ),
        fallbackMessage = "登录失败"
    )

    /**
     * Bootstrap 基础状态初始化接口（获取 ws_token 及公共配置）
     */
    suspend fun bootstrap(): Result<BootstrapData> = apiDataRequestRequired(
        path = "/api/home/bootstrap",
        type = object : TypeToken<ApiResponse<BootstrapData>>() {}.type,
        get = true,
        fallbackMessage = "获取配置失败"
    )

    /**
     * 进入公共大厅接口（换取大厅专属 room_id）
     */
    suspend fun enterPublicHall(roomId: String? = null): Result<EnterRoomData> = apiDataRequestRequired(
        path = "/room/public/enter",
        type = object : TypeToken<ApiResponse<EnterRoomData>>() {}.type,
        params = mutableMapOf<String, String>().apply {
            roomId?.takeIf { it.isNotBlank() }?.let { put("room_id", it) }
        },
        fallbackMessage = "进入大厅失败"
    )

    /**
     * 捞动态 Feed 列表 (GET /api/moment/feed) - 精选
     */
    suspend fun getMomentsFeed(
        limit: Int = 10,
        sort: String = "latest",
        beforeId: Long? = null
    ): Result<List<MomentItemDto>> = fetchMomentFeedList(
        path = "/api/moment/feed",
        limit = limit,
        sort = sort,
        beforeId = beforeId,
        fallbackMessage = "获取动态失败"
    )

    /**
     * 关注动态 Feed 列表 (GET /api/moment/following) - 关注
     */
    suspend fun getMomentsFollowing(
        limit: Int = 10,
        sort: String = "latest",
        beforeId: Long? = null
    ): Result<List<MomentItemDto>> = fetchMomentFeedList(
        path = "/api/moment/following",
        limit = limit,
        sort = sort,
        beforeId = beforeId,
        fallbackMessage = "获取关注动态失败"
    )

    /**
     * 我的动态 Feed 列表 (GET /api/moment/mine) - 我的
     */
    suspend fun getMomentsMine(
        limit: Int = 10,
        sort: String = "latest",
        beforeId: Long? = null
    ): Result<List<MomentItemDto>> = fetchMomentFeedList(
        path = "/api/moment/mine",
        limit = limit,
        sort = sort,
        beforeId = beforeId,
        fallbackMessage = "获取我的动态失败"
    )

    /**
     * 动态 Feed 列表（精选 / 关注 / 我的）共用实现：
     * 三者同构（`ApiResponse<MomentFeedData>`），差异仅在 path 与失败文案。
     */
    private suspend fun fetchMomentFeedList(
        path: String,
        limit: Int,
        sort: String,
        beforeId: Long?,
        fallbackMessage: String
    ): Result<List<MomentItemDto>> = apiDataRequestRequired<MomentFeedData>(
        path = path,
        type = object : TypeToken<ApiResponse<MomentFeedData>>() {}.type,
        params = mutableMapOf(
            "limit" to limit.toString(),
            "sort" to sort
        ).apply { beforeId?.let { put("before_id", it.toString()) } },
        get = true,
        fallbackMessage = fallbackMessage
    ).mapCatching { it.list ?: throw IOException(fallbackMessage) }

    suspend fun getMomentsForUser(
        userId: String,
        limit: Int = 20,
        sort: String = "latest"
    ): Result<MomentFeedData> = apiDataRequestRequired(
        path = "/api/moment/user",
        type = object : TypeToken<ApiResponse<MomentFeedData>>() {}.type,
        params = mapOf(
            "user_id" to userId,
            "limit" to limit.toString(),
            "sort" to sort
        ),
        get = true,
        fallbackMessage = "获取用户动态失败"
    )

    /**
     * 捞一条动态 (POST /moment/fish)
     *
     * 官方语义：无参数随机抽取一条当前可被捞取的动态，并在服务端记入捞取历史。
     * 返回 data.moment 为空表示暂时没有可捞的新动态。
     */
    suspend fun fishMoment(): Result<FishMomentData> = apiDataRequestRequired(
        path = "/moment/fish",
        type = object : TypeToken<ApiResponse<FishMomentData>>() {}.type,
        fallbackMessage = "捞取失败，请稍后再试"
    )

    /**
     * 捞取记录 (GET /api/moment/fish-history)
     *
     * 与动态 Feed 同构：list + has_more + next_before_id，
     * 每项在动态字段基础上附带 fish_log_id 与 fished_at。
     */
    suspend fun getFishHistory(
        beforeId: Long? = null,
        limit: Int = 12
    ): Result<MomentFeedData> = apiDataRequestRequired(
        path = "/api/moment/fish-history",
        type = object : TypeToken<ApiResponse<MomentFeedData>>() {}.type,
        params = mutableMapOf("limit" to limit.toString()).apply {
            beforeId?.let { put("before_id", it.toString()) }
        },
        get = true,
        fallbackMessage = "获取捞取记录失败"
    )

    /**
     * 动态点赞 / 取消点赞 (POST /moment/like)
     *
     * 服务端只回 `code`、无业务数据。失败（HTTP 非 2xx 或 code != 1）一律返回 failure，
     * 调用方据此回滚乐观更新。
     *
     * 此前实现为 `Result.success(apiResponse.isSuccess)`，失败时也返回「成功」，
     * 调用方按 isSuccess 判断会误判为成功而不回滚，导致点赞状态永久错误。
     */
    suspend fun likeMoment(momentId: Long): Result<Unit> = apiDataRequest<Any>(
        path = "/moment/like",
        type = object : TypeToken<ApiResponse<Any>>() {}.type,
        params = mapOf("moment_id" to momentId.toString()),
        fallbackMessage = "点赞失败"
    ).mapCatching { Unit }

    /**
     * 更新匹配偏好设置
     */
    suspend fun updateMatchSettings(
        ageRange: String = "18-23",
        matchOppositeGenderOnly: Int = 0,
        matchSameGenderOnly: Int = 0,
        cleanStreamMode: Int = 0
    ): Result<Boolean> = apiDataRequest<Any>(
        path = "/profile/update_match_settings",
        type = object : TypeToken<ApiResponse<Any>>() {}.type,
        params = mapOf(
            "age_range" to ageRange,
            "match_opposite_gender_only" to matchOppositeGenderOnly.toString(),
            "match_same_gender_only" to matchSameGenderOnly.toString(),
            "clean_stream_mode" to cleanStreamMode.toString()
        ),
        fallbackMessage = "匹配设置保存失败"
    ).mapCatching { true }

    suspend fun roomManagementRequest(
        path: String,
        params: Map<String, String> = emptyMap(),
        get: Boolean = false
    ): Result<com.google.gson.JsonObject> = apiDataRequest<com.google.gson.JsonObject>(
        path = path,
        type = object : TypeToken<ApiResponse<com.google.gson.JsonObject>>() {}.type,
        params = params,
        get = get,
        fallbackMessage = "房间操作失败"
    ).mapCatching { it ?: com.google.gson.JsonObject() }

    /**
     * 房间成员列表 (GET /api/room/members)
     *
     * 服务端可能把列表放在 data 数组本身，或放在 {list|members|items|users: [...]} 里，两种都兼容。
     */
    suspend fun getRoomMembers(
        roomId: String,
        page: Int = 1,
        perPage: Int = 50,
        query: String = ""
    ): Result<RoomMembersData> = withContext(Dispatchers.IO) {
        try {
            val builder = "$BASE_URL/api/room/members".toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(IOException("无效的房间成员接口地址"))
            builder.addQueryParameter("room_id", roomId)
            builder.addQueryParameter("page", page.toString())
            builder.addQueryParameter("per_page", perPage.toString())
            if (query.isNotBlank()) builder.addQueryParameter("q", query)

            val request = Request.Builder().url(builder.build()).get().build()
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error: ${response.code}"))
            }

            val payload = extractListPayload(bodyString, ROOM_MEMBER_LIST_KEYS)
                ?: return@withContext Result.failure(IOException("获取房间成员失败"))
            val list: List<RoomMemberDto> = gson.fromJson(
                payload.first, object : TypeToken<List<RoomMemberDto>>() {}.type
            )
            val meta = payload.second
            Result.success(
                RoomMembersData(
                    members = list,
                    total = meta.intOr("total", list.size),
                    page = meta.intOr("page", page),
                    perPage = meta.intOr("per_page", perPage),
                    hasMore = meta.boolOr("has_more", false)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 禁止加入本房的名单 (GET /api/room/join-bans)
     */
    suspend fun getRoomJoinBans(roomId: String): Result<RoomJoinBansData> = withContext(Dispatchers.IO) {
        try {
            val builder = "$BASE_URL/api/room/join-bans".toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext Result.failure(IOException("无效的禁止加入名单接口地址"))
            builder.addQueryParameter("room_id", roomId)

            val request = Request.Builder().url(builder.build()).get().build()
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error: ${response.code}"))
            }

            val payload = extractListPayload(bodyString, ROOM_BAN_LIST_KEYS)
                ?: return@withContext Result.failure(IOException("获取禁止加入名单失败"))
            val list: List<RoomJoinBanDto> = gson.fromJson(
                payload.first, object : TypeToken<List<RoomJoinBanDto>>() {}.type
            )
            Result.success(
                RoomJoinBansData(bans = list, total = payload.second.intOr("total", list.size))
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取房间共享音乐列表 (GET /api/room/music/playlist?room_id=xxx)
     */
    suspend fun getRoomMusicPlaylist(roomId: String): Result<RoomMusicPlaylistResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/room/music/playlist".toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("room_id", roomId)
                ?.build() ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL"))

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val apiRes = gson.fromJson<ApiResponse<RoomMusicPlaylistResponse>>(
                body,
                object : TypeToken<ApiResponse<RoomMusicPlaylistResponse>>() {}.type
            )
            if (apiRes != null && apiRes.isSuccess && apiRes.data != null) {
                Result.success(apiRes.data)
            } else {
                Result.failure(Exception(apiRes?.msg ?: "获取房间音乐列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 添加歌曲到房间共享列表 (POST /room/music/playlist/add)
     */
    suspend fun addRoomMusic(
        roomId: String,
        songId: String,
        name: String,
        artists: String,
        album: String,
        coverUrl: String,
        version: Int
    ): Result<RoomMusicPlaylistResponse> = apiDataRequestRequired(
        path = "/room/music/playlist/add",
        type = object : TypeToken<ApiResponse<RoomMusicPlaylistResponse>>() {}.type,
        params = mapOf(
            "room_id" to roomId,
            "song_id" to songId,
            "name" to name,
            "artists" to artists,
            "album" to album,
            "cover_url" to coverUrl,
            "version" to version.toString()
        ),
        fallbackMessage = "添加房间音乐失败"
    )

    /**
     * 从房间共享列表移除歌曲 (POST /room/music/playlist/remove)
     */
    suspend fun removeRoomMusic(
        roomId: String,
        songId: String,
        version: Int
    ): Result<RoomMusicPlaylistResponse> = apiDataRequestRequired(
        path = "/room/music/playlist/remove",
        type = object : TypeToken<ApiResponse<RoomMusicPlaylistResponse>>() {}.type,
        params = mapOf(
            "room_id" to roomId,
            "song_id" to songId,
            "version" to version.toString()
        ),
        fallbackMessage = "移除房间音乐失败"
    )

    /**
     * 切换房间音乐播放模式 (POST /room/music/playlist/play-mode)
     */
    suspend fun setRoomMusicPlayMode(
        roomId: String,
        playMode: String,
        version: Int
    ): Result<RoomMusicPlaylistResponse> = apiDataRequestRequired(
        path = "/room/music/playlist/play-mode",
        type = object : TypeToken<ApiResponse<RoomMusicPlaylistResponse>>() {}.type,
        params = mapOf(
            "room_id" to roomId,
            "play_mode" to playMode,
            "version" to version.toString()
        ),
        fallbackMessage = "切换播放模式失败"
    )

    /**
     * 清空房间共享音乐列表 (POST /room/music/playlist/clear)
     */
    suspend fun clearRoomMusic(
        roomId: String,
        version: Int
    ): Result<RoomMusicPlaylistResponse> = apiDataRequestRequired(
        path = "/room/music/playlist/clear",
        type = object : TypeToken<ApiResponse<RoomMusicPlaylistResponse>>() {}.type,
        params = mapOf(
            "room_id" to roomId,
            "version" to version.toString()
        ),
        fallbackMessage = "清空歌单失败"
    )

    /**
     * 获取对局详情 (GET /api/game/<type>?id=<gameId>)
     */
    suspend fun getGameDetail(gameType: String, gameId: Long): Result<GameSessionDetail> =
        apiDataRequestRequired<GameDetailResponseData>(
            path = "/api/game/$gameType",
            type = object : TypeToken<ApiResponse<GameDetailResponseData>>() {}.type,
            params = mapOf("id" to gameId.toString()),
            get = true,
            fallbackMessage = "获取对局状态失败"
        ).map { it.game }

    /**
     * 获取我的对局列表 (GET /api/game/my-sessions)
     */
    suspend fun getMyGameSessions(): Result<List<MyGameSessionItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/game/my-sessions"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error: ${response.code}"))
            }
            val root = JsonParser.parseString(bodyString).asJsonObject
            if (root.get("code")?.asInt != 1) {
                return@withContext Result.failure(IOException(root.get("msg")?.asString ?: "获取对局列表失败"))
            }
            val dataEl = root.get("data")
            val list = when {
                dataEl == null || dataEl.isJsonNull -> emptyList()
                dataEl.isJsonArray -> gson.fromJson<List<MyGameSessionItem>>(dataEl, object : TypeToken<List<MyGameSessionItem>>() {}.type)
                dataEl.isJsonObject && dataEl.asJsonObject.has("sessions") -> {
                    gson.fromJson<List<MyGameSessionItem>>(dataEl.asJsonObject.get("sessions"), object : TypeToken<List<MyGameSessionItem>>() {}.type)
                }
                else -> emptyList()
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取对局聊天历史 (GET /api/game/chat-history?game_id=...&before_id=...&limit=40)
     */
    suspend fun getGameChatHistory(
        gameId: Long,
        beforeId: Long? = null,
        limit: Int = 40
    ): Result<GameChatHistoryResponse> = apiDataRequestRequired<GameChatHistoryResponse>(
        path = "/api/game/chat-history",
        type = object : TypeToken<ApiResponse<GameChatHistoryResponse>>() {}.type,
        params = buildMap {
            put("game_id", gameId.toString())
            put("limit", limit.toString())
            if (beforeId != null) put("before_id", beforeId.toString())
        },
        get = true,
        fallbackMessage = "获取对局聊天记录失败"
    )


    /**
     * 兼容解析「列表型」房间接口：data 可能是数组，也可能是含列表字段的对象。
     *
     * @return 列表数组 与 分页元信息对象（data 为数组时元信息为 null）；code != 1 或结构不符返回 null
     */
    private fun extractListPayload(body: String, keys: Array<String>): Pair<JsonArray, JsonObject?>? {
        val root = try {
            JsonParser.parseString(body).asJsonObject
        } catch (_: Exception) {
            return null
        }
        if (root.intOr("code", 0) != 1) return null
        val dataEl = root.get("data")?.takeIf { !it.isJsonNull } ?: return null
        val meta = dataEl.takeIf { it.isJsonObject }?.asJsonObject
        val array = when {
            dataEl.isJsonArray -> dataEl.asJsonArray
            meta != null -> keys.firstNotNullOfOrNull { key ->
                meta.get(key)?.takeIf { it.isJsonArray }?.asJsonArray
            }
            else -> null
        } ?: return null
        return array to meta
    }

    private fun JsonObject?.intOr(key: String, def: Int): Int {
        val element = this?.get(key)?.takeIf { !it.isJsonNull } ?: return def
        return try {
            element.asInt
        } catch (_: Exception) {
            def
        }
    }

    private fun JsonObject?.boolOr(key: String, def: Boolean): Boolean {
        val element = this?.get(key)?.takeIf { !it.isJsonNull } ?: return def
        return try {
            element.asBoolean
        } catch (_: Exception) {
            def
        }
    }

    /**
     * 获取会话房间列表接口 (/room/list)
     */
    suspend fun getRoomList(
        offset: Int = 0,
        perPage: Int = 50,
        type: String = "all"
    ): Result<RoomListData> = apiDataRequestRequired(
        path = "/room/list",
        type = object : TypeToken<ApiResponse<RoomListData>>() {}.type,
        params = mapOf(
            "offset" to offset.toString(),
            "per_page" to perPage.toString(),
            "type" to type
        ),
        get = true,
        fallbackMessage = "获取房间列表失败"
    )

    /**
     * 进入房间初始化与消息拉取 (/api/chat/bootstrap)
     */
    suspend fun getChatBootstrap(roomId: String): Result<ChatBootstrapData> = apiDataRequestRequired(
        path = "/api/chat/bootstrap",
        type = object : TypeToken<ApiResponse<ChatBootstrapData>>() {}.type,
        params = mapOf("room_id" to roomId),
        get = true,
        fallbackMessage = "获取聊天室信息失败"
    )

    /**
     * 分页拉取历史消息记录 (/api/chat/messages)
     */
    suspend fun getChatMessages(
        roomId: String,
        beforeId: Long? = null,
        afterId: Long? = null,
        limit: Int = 50
    ): Result<ChatMessagesData> = apiDataRequestRequired(
        path = "/api/chat/messages",
        type = object : TypeToken<ApiResponse<ChatMessagesData>>() {}.type,
        params = mutableMapOf(
            "room_id" to roomId,
            "limit" to limit.toString()
        ).apply {
            beforeId?.let { put("before_id", it.toString()) }
            afterId?.let { put("after_id", it.toString()) }
        },
        get = true,
        fallbackMessage = "拉取历史消息失败"
    )

    /**
     * 创建暗号房间 (POST /room/create)
     *
     * 官方 CreateRoomModal 的 `encryption_enabled` 默认为 false（每次打开弹窗重置为关闭），
     * 是否加密完全由创建时的开关决定；加密房间一旦建成便无法再关闭。
     */
    suspend fun createSecretRoom(
        roomName: String,
        password: String,
        encryptionEnabled: Boolean = false
    ): Result<CreateRoomData> = apiDataRequestRequired(
        path = "/room/create",
        type = object : TypeToken<ApiResponse<CreateRoomData>>() {}.type,
        params = mapOf(
            "room_name" to roomName,
            "password" to password,
            "encryption_enabled" to if (encryptionEnabled) "1" else "0"
        ),
        fallbackMessage = "创建房间失败"
    )

    /**
     * 加入暗号房间 (POST /room/join)
     */
    suspend fun joinSecretRoom(
        creatorUsername: String,
        roomName: String,
        password: String
    ): Result<JoinRoomData> = apiDataRequestRequired(
        path = "/room/join",
        type = object : TypeToken<ApiResponse<JoinRoomData>>() {}.type,
        params = mapOf(
            "creator_username" to creatorUsername,
            "room_name" to roomName,
            "password" to password
        ),
        fallbackMessage = "加入房间失败"
    )

    /**
     * 获取动态评论列表 (GET /api/moment/comments)
     */
    suspend fun getMomentComments(
        momentId: Long,
        beforeId: Long? = null,
        limit: Int = 20
    ): Result<MomentCommentsData> = apiDataRequestRequired(
        path = "/api/moment/comments",
        type = object : TypeToken<ApiResponse<MomentCommentsData>>() {}.type,
        params = mutableMapOf(
            "moment_id" to momentId.toString(),
            "limit" to limit.toString()
        ).apply { beforeId?.let { put("before_id", it.toString()) } },
        get = true,
        fallbackMessage = "获取评论列表失败"
    )

    /**
     * 发表动态评论 (POST /moment/comment)
     */
    suspend fun postMomentComment(
        momentId: Long,
        content: String,
        parentId: Long? = null,
        replyToUserId: String? = null
    ): Result<PostCommentData> = apiDataRequestRequired(
        path = "/moment/comment",
        type = object : TypeToken<ApiResponse<PostCommentData>>() {}.type,
        params = mutableMapOf(
            "moment_id" to momentId.toString(),
            "content" to content
        ).apply {
            parentId?.let { put("parent_id", it.toString()) }
            replyToUserId?.takeIf { it.isNotBlank() }?.let { put("reply_to_user_id", it) }
        },
        fallbackMessage = "发表评论失败"
    )

    /**
     * 动态评论点赞/取消点赞 (POST /moment/comment/like)
     */
    suspend fun likeMomentComment(commentId: Long): Result<CommentLikeData> = apiDataRequestRequired(
        path = "/moment/comment/like",
        type = object : TypeToken<ApiResponse<CommentLikeData>>() {}.type,
        params = mapOf("comment_id" to commentId.toString()),
        fallbackMessage = "评论点赞失败"
    )

    /**
     * 发布动态 (POST /moment/create)
     *
     * 参数与官方一致：
     * - `content` 正文
     * - `images` JSON 数组字符串
     * - `audio_url` 语音上传后 URL
     * - `music` JSON 字符串（{provider,song_id,name,artists,album,cover_url}）
     * - `is_private` 1（仅自己可见）
     * - `audience_mode` allow（部分可见）/ deny（不给谁看）
     * - `user_ids` JSON 数组字符串（名单用户 uid）
     * - `audience_mutual` 1/0（互相关注的人可见）
     */
    suspend fun createMoment(
        content: String,
        images: List<String> = emptyList(),
        audioUrl: String = "",
        musicJson: String = "",
        isPrivate: Boolean = false,
        audienceMode: String = "all",
        audienceMutual: Boolean = false,
        userIds: List<String> = emptyList()
    ): Result<CreateMomentData> = apiDataRequestRequired(
        path = "/moment/create",
        type = object : TypeToken<ApiResponse<CreateMomentData>>() {}.type,
        params = mutableMapOf(
            "content" to content,
            "images" to (if (images.isEmpty()) "[]" else gson.toJson(images)),
            "audio_url" to audioUrl,
            "music" to musicJson
        ).apply {
            if (isPrivate) {
                put("is_private", "1")
            } else if (userIds.isNotEmpty()) {
                // 部分可见 / 不给谁看（互斥，由发布器只保留最后选择的一个模式）
                put("audience_mode", if (audienceMode == "all") "allow" else audienceMode)
                put("user_ids", gson.toJson(userIds))
                put("audience_mutual", "0")
            } else if (audienceMutual) {
                put("audience_mode", "deny")
                put("audience_mutual", "1")
                put("user_ids", "[]")
            }
        },
        fallbackMessage = "发布动态失败"
    )

    /**
     * 删除动态 (POST /moment/delete)
     */
    suspend fun deleteMoment(momentId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("moment_id", momentId.toString())
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/moment/delete")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("HTTP Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 设置动态置顶/取消置顶 (POST /moment/pin)
     */
    suspend fun setMomentPin(momentId: Long, isPinned: Boolean): Result<SetMomentPinData> = apiDataRequestRequired(
        path = "/moment/pin",
        type = object : TypeToken<ApiResponse<SetMomentPinData>>() {}.type,
        params = mapOf(
            "moment_id" to momentId.toString(),
            "is_pinned" to if (isPinned) "1" else "0"
        ),
        fallbackMessage = "设置置顶失败"
    )

    /**
     * 设置动态可见性（公开或仅自己可见） (POST /moment/visibility)
     */
    suspend fun setMomentVisibility(momentId: Long, isPrivate: Boolean): Result<SetMomentVisibilityData> = apiDataRequestRequired(
        path = "/moment/visibility",
        type = object : TypeToken<ApiResponse<SetMomentVisibilityData>>() {}.type,
        params = mapOf(
            "moment_id" to momentId.toString(),
            "is_private" to if (isPrivate) "1" else "0"
        ),
        fallbackMessage = "设置可见性失败"
    )

    /**
     * 获取动态受众设置 (GET /api/moment/hide-users)
     */
    suspend fun getMomentHideUsers(momentId: Long): Result<MomentHideUsersData> = apiDataRequestRequired(
        path = "/api/moment/hide-users",
        type = object : TypeToken<ApiResponse<MomentHideUsersData>>() {}.type,
        params = mapOf("moment_id" to momentId.toString()),
        get = true,
        fallbackMessage = "获取受众设置失败"
    )

    /**
     * 设置动态受众谁可以看 (POST /moment/hide-users)
     */
    suspend fun setMomentHideUsers(
        momentId: Long,
        audienceMode: String = "deny",
        userIds: List<String> = emptyList(),
        audienceMutual: Boolean = false,
        releasePrivate: Boolean = true
    ): Result<Unit> = apiDataRequest<Any>(
        path = "/moment/hide-users",
        type = object : TypeToken<ApiResponse<Any>>() {}.type,
        params = mapOf(
            "moment_id" to momentId.toString(),
            "audience_mode" to (if (audienceMode == "all") "deny" else audienceMode),
            "user_ids" to (if (userIds.isEmpty()) "[]" else gson.toJson(userIds)),
            "audience_mutual" to (if (audienceMutual) "1" else "0"),
            "release_private" to (if (releasePrivate) "1" else "0")
        ),
        fallbackMessage = "设置受众失败"
    ).mapCatching { Unit }

    /**
     * 修改个人资料 (POST /profile/update)
     */
    suspend fun updateProfile(
        username: String,
        bio: String,
        ageRange: String? = null,
        gender: String? = null,
        patText: String? = null,
        loginName: String? = null,
        qq: String? = null
    ): Result<UpdateProfileData> = apiDataRequestRequired(
        path = "/profile/update",
        type = object : TypeToken<ApiResponse<UpdateProfileData>>() {}.type,
        params = mutableMapOf(
            "username" to username.trim(),
            "bio" to bio.trim()
        ).apply {
            ageRange?.takeIf { it.isNotBlank() }?.let { put("age_range", it) }
            gender?.takeIf { it.isNotBlank() }?.let { put("gender", if (it == "女") "female" else "male") }
            patText?.takeIf { it.isNotBlank() }?.let { put("pat_text", it.trim()) }
            loginName?.takeIf { it.isNotBlank() }?.let { put("login_name", it.trim()) }
            qq?.takeIf { it.isNotBlank() }?.let { put("qq", it.trim()) }
        },
        fallbackMessage = "更新资料失败"
    )

    /**
     * 上传头像 (官方优先 uploadSource: "user_avatar" 直传 + /profile/avatar/commit，回退 POST /profile/avatar)
     */
    suspend fun uploadAvatar(
        fileBytes: ByteArray,
        filename: String = "avatar.png"
    ): Result<AvatarData> = withContext(Dispatchers.IO) {
        val contentType = when {
            filename.endsWith(".jpg", ignoreCase = true) || filename.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/png"
        }
        try {
            // 1. 优先尝试三段式直传（upload_source = user_avatar）
            val directResult = uploadFile(
                fileBytes = fileBytes,
                filename = filename,
                contentType = contentType,
                uploadSource = "user_avatar",
                legacyType = "image"
            )
            if (directResult.isSuccess) {
                val directUrl = directResult.getOrThrow()
                if (directUrl.isNotBlank()) {
                    val commitRes = commitAvatar(directUrl)
                    if (commitRes.isSuccess) {
                        val commitData = commitRes.getOrNull()
                        val finalData = commitData?.copy(
                            avatarUrl = commitData.avatarUrl?.takeIf { it.isNotBlank() } ?: directUrl,
                            hasCustom = true
                        ) ?: AvatarData(avatarUrl = directUrl, hasCustom = true)
                        return@withContext Result.success(finalData)
                    }
                }
            }
        } catch (_: Exception) {
            // 直传异常时继续尝试 legacy 上传
        }

        // 2. 回退到标准 multipart 上传 (POST /profile/avatar)
        legacyAvatarUpload(fileBytes, filename, contentType)
    }

    /**
     * 旧版/回退头像 multipart 上传 (POST /profile/avatar)
     */
    private suspend fun legacyAvatarUpload(
        fileBytes: ByteArray,
        filename: String,
        contentType: String
    ): Result<AvatarData> = withContext(Dispatchers.IO) {
        try {
            val mediaType = contentType.toMediaTypeOrNull()
            val fileBody = fileBytes.toRequestBody(mediaType)
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, fileBody)
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/profile/avatar")
                .post(multipartBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error: ${response.code}"))
            }

            val typeToken = object : TypeToken<ApiResponse<AvatarData>>() {}.type
            val apiResponse: ApiResponse<AvatarData> = gson.fromJson(bodyString, typeToken)
            if (apiResponse.isSuccess && apiResponse.data != null) {
                Result.success(apiResponse.data.copy(hasCustom = true))
            } else {
                Result.failure(IOException(apiResponse.msg ?: "上传头像失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 上传动态配图 (官方 /api/upload 系列)
     *
     * 说明：此前本方法误用了头像接口 `POST /profile/avatar`，导致发布带图动态时
     * 图片地址完全不对，动态里看不到配图。
     *
     * 官方前端（moment.uploadImage → uploadFileDirect）优先走三段式直传：
     * 1. POST /api/upload/presign（upload_source / content_type / bytes / ext）
     * 2. PUT upload_url 直传对象存储
     * 3. POST /api/upload/bind（ticket_id）取回最终 url
     * 服务端未启用 OSS 时回退到旧版 multipart `POST /api/upload`（file + type + upload_source）。
     */
    suspend fun uploadImage(
        fileBytes: ByteArray,
        filename: String = "moment_image.jpg"
    ): Result<String> = uploadFile(
        fileBytes = fileBytes,
        filename = filename,
        contentType = if (filename.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg",
        uploadSource = "moment_image",
        legacyType = "image"
    )

    /**
     * 上传聊天图片（实测链路）
     *
     * 1. `POST /api/upload/presign`：`upload_source=chat_image`、`content_type`、`bytes`、`ext`、`room_id`
     * 2. `PUT <OSS 签名 URL>`：body 为图片字节
     * 3. `POST /api/upload/bind`：`ticket_id`
     * 4. WS 帧：`{"event":"message","content":"<图片URL>","type":"image","image_url":"<图片URL>"}`
     */
    suspend fun uploadChatImage(
        fileBytes: ByteArray,
        filename: String = "chat_image.jpg",
        roomId: String
    ): Result<String> = uploadFile(
        fileBytes = fileBytes,
        filename = filename,
        contentType = when {
            filename.endsWith(".png", ignoreCase = true) -> "image/png"
            filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/jpeg"
        },
        uploadSource = "chat_image",
        legacyType = "image",
        roomId = roomId
    )

    /**
     * 上传聊天语音（实测链路）
     *
     * 1. `POST /api/upload/presign`：`upload_source=chat_audio`、`content_type`、`bytes`、`ext`、
     *    `room_id`、`audio_source=record|file`
     * 2. `PUT <OSS 签名 URL>`：body 为音频字节
     * 3. `POST /api/upload/bind`：`ticket_id`
     * 4. WS 帧：`{"event":"message","content":"<音频URL>","type":"audio","audio_source":"record|file"}`
     *
     * @param audioSource `record`（录音发送）/ `file`（语音文件发送）
     */
    suspend fun uploadChatAudio(
        fileBytes: ByteArray,
        filename: String,
        contentType: String,
        roomId: String,
        audioSource: String
    ): Result<String> = uploadFile(
        fileBytes = fileBytes,
        filename = filename,
        contentType = contentType,
        uploadSource = "chat_audio",
        legacyType = "audio",
        roomId = roomId,
        audioSource = audioSource
    )

    /**
     * 上传动态语音
     *
     * 复用通用上传链路：`POST /api/upload/presign` + PUT + bind（`upload_source=moment_audio`），
     * 服务端未启用 OSS 时回退旧版 multipart `POST /api/upload_audio`
     * （字段 `file`、`type=audio`、`upload_source=moment_audio`）。
     */
    suspend fun uploadMomentAudio(
        fileBytes: ByteArray,
        filename: String = "moment_audio.m4a"
    ): Result<String> = uploadFile(
        fileBytes = fileBytes,
        filename = filename,
        contentType = "audio/mp4",
        uploadSource = "moment_audio",
        legacyType = "audio",
        legacyPath = "/api/upload_audio"
    )

    /**
     * 网易云歌曲解析（链接 / 歌曲 ID）：POST /api/music/netease/resolve
     */
    suspend fun resolveNeteaseSong(input: String): Result<NeteaseResolveData?> = apiDataRequest(
        path = "/api/music/netease/resolve",
        type = object : TypeToken<ApiResponse<NeteaseResolveData>>() {}.type,
        params = mapOf("input" to input),
        fallbackMessage = "解析网易云歌曲失败"
    )

    /**
     * 网易云搜索：POST /api/music/netease/search
     */
    suspend fun searchNeteaseSongs(keyword: String, offset: Int = 0, limit: Int = 20): Result<NeteaseSearchData?> = apiDataRequest(
        path = "/api/music/netease/search",
        type = object : TypeToken<ApiResponse<NeteaseSearchData>>() {}.type,
        params = mapOf(
            "keyword" to keyword,
            "offset" to offset.toString(),
            "limit" to limit.toString()
        ),
        fallbackMessage = "搜索网易云歌曲失败"
    )

    /**
     * 网易云歌单解析：POST /api/music/netease/playlist/resolve
     */
    suspend fun resolveNeteasePlaylist(input: String): Result<NeteasePlaylistData?> = apiDataRequest(
        path = "/api/music/netease/playlist/resolve",
        type = object : TypeToken<ApiResponse<NeteasePlaylistData>>() {}.type,
        params = mapOf("input" to input),
        fallbackMessage = "解析网易云歌单失败"
    )

    /**
     * 获取网易云歌曲播放地址：POST /api/music/netease/play-url
     *
     * body：`song_id`、`force`（0 用缓存 / 1 强制刷新）。
     */
    suspend fun getNeteasePlayUrl(songId: String, force: Boolean = false): Result<NeteasePlayUrlData?> = apiDataRequest(
        path = "/api/music/netease/play-url",
        type = object : TypeToken<ApiResponse<NeteasePlayUrlData>>() {}.type,
        params = mapOf(
            "song_id" to songId,
            "force" to if (force) "1" else "0"
        ),
        fallbackMessage = "获取音乐播放地址失败"
    )

    /**
     * 通用上传：三段式直传 + 旧版 multipart 回退，返回可直接访问的文件地址
     *
     * @param legacyPath 直传不可用时回退的旧版 multipart 端点
     *                   （图片/头像为 `/api/upload`，动态语音为 `/api/upload_audio`）
     */
    suspend fun uploadFile(
        fileBytes: ByteArray,
        filename: String,
        contentType: String,
        uploadSource: String,
        legacyType: String? = null,
        roomId: String? = null,
        audioSource: String? = null,
        legacyPath: String = "/api/upload"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (fileBytes.isEmpty()) {
            return@withContext Result.failure(IOException("文件大小无效"))
        }

        // 1. 取直传凭证
        val ext = filename.substringAfterLast('.', "").ifBlank {
            when (contentType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "audio/mpeg" -> "mp3"
                "audio/wav" -> "wav"
                "audio/ogg" -> "ogg"
                "audio/webm" -> "webm"
                else -> "jpg"
            }
        }

        val presignForm = FormBody.Builder()
            .add("upload_source", uploadSource)
            .add("content_type", contentType)
            .add("bytes", fileBytes.size.toString())
            .add("ext", ext)
            .apply {
                roomId?.takeIf { it.isNotBlank() }?.let { add("room_id", it) }
                // 聊天语音专属：record（录音）/ file（语音文件），实测由 presign 与 WS 帧同时携带
                audioSource?.takeIf { it.isNotBlank() }?.let { add("audio_source", it) }
            }
            .build()

        val presignResult = try {
            val request = Request.Builder()
                .url("$BASE_URL/api/upload/presign")
                .post(presignForm)
                .build()
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            val type = object : TypeToken<ApiResponse<UploadPresignData>>() {}.type
            val apiResponse: ApiResponse<UploadPresignData> = gson.fromJson(bodyString, type)
            if (apiResponse.isSuccess && apiResponse.data != null) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(IOException(apiResponse.msg ?: "获取上传凭证失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        val presign = presignResult.getOrNull()
        if (presign == null || presign.uploadUrl.isNullOrBlank() || presign.ticketId.isBlank()) {
            if (uploadSource.startsWith("chat_")) {
                val errMsg = presignResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                    ?: if (uploadSource == "chat_audio") "获取语音上传凭证失败" else "获取图片上传凭证失败"
                return@withContext Result.failure(IOException(errMsg))
            }
            // 2. 其它非聊天渠道回退：旧版 multipart /api/upload
            return@withContext legacyUpload(fileBytes, filename, contentType, uploadSource, legacyType, legacyPath)
        }

        try {
            // 3. PUT 直传到对象存储 (OSS)
            val putHeaders = presign.headers ?: emptyMap()
            val putBody = fileBytes.toRequestBody(contentType.toMediaTypeOrNull())
            val putBuilder = Request.Builder().url(presign.uploadUrl).put(putBody)
            putHeaders.forEach { (key, value) ->
                if (key.isNotBlank() && value.isNotBlank() && !key.equals("Content-Length", ignoreCase = true)) {
                    putBuilder.header(key, value)
                }
            }
            putBuilder.header("Content-Type", contentType)
            val putResponse = client.newCall(putBuilder.build()).execute()
            val putSuccess = putResponse.isSuccessful
            val putCode = putResponse.code
            putResponse.close()
            if (!putSuccess) {
                if (uploadSource.startsWith("chat_")) {
                    return@withContext Result.failure(IOException("文件存储直传失败 (HTTP $putCode)"))
                }
                return@withContext legacyUpload(fileBytes, filename, contentType, uploadSource, legacyType, legacyPath)
            }

            // 4. 绑定凭证并取回最终地址 (POST /api/upload/bind)
            val bindForm = FormBody.Builder()
                .add("ticket_id", presign.ticketId)
                .build()
            val bindRequest = Request.Builder()
                .url("$BASE_URL/api/upload/bind")
                .post(bindForm)
                .build()
            val bindResponse = client.newCall(bindRequest).execute()
            val bindBody = bindResponse.body?.string() ?: ""
            val bindType = object : TypeToken<ApiResponse<UploadResultData>>() {}.type
            val bindApi: ApiResponse<UploadResultData> = gson.fromJson(bindBody, bindType)
            if (bindApi.isSuccess) {
                val directUrl = presign.uploadUrl.substringBefore('?')
                val url = bindApi.data?.url?.takeIf { it.isNotBlank() } ?: directUrl
                Result.success(NetworkImageUrl.resolve(url))
            } else {
                Result.failure(IOException(bindApi.msg ?: "上传校验绑定失败"))
            }
        } catch (e: Exception) {
            if (uploadSource.startsWith("chat_")) {
                Result.failure(e)
            } else {
                legacyUpload(fileBytes, filename, contentType, uploadSource, legacyType, legacyPath)
            }
        }
    }

    /**
     * 旧版 multipart 上传（图片/头像走 `/api/upload`，动态语音走 `/api/upload_audio`）
     */
    private suspend fun legacyUpload(
        fileBytes: ByteArray,
        filename: String,
        contentType: String,
        uploadSource: String,
        legacyType: String?,
        path: String = "/api/upload"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileBody = fileBytes.toRequestBody(contentType.toMediaTypeOrNull())
            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, fileBody)
            legacyType?.let { multipartBuilder.addFormDataPart("type", it) }
            multipartBuilder.addFormDataPart("upload_source", uploadSource)

            val request = Request.Builder()
                .url("$BASE_URL$path")
                .post(multipartBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error: ${response.code}"))
            }

            val type = object : TypeToken<ApiResponse<UploadResultData>>() {}.type
            val apiResponse: ApiResponse<UploadResultData> = gson.fromJson(bodyString, type)
            val url = apiResponse.data?.url ?: apiResponse.data?.avatarUrl
            if (apiResponse.isSuccess && !url.isNullOrBlank()) {
                Result.success(NetworkImageUrl.resolve(url))
            } else {
                val defaultFailMsg = if (uploadSource.contains("audio")) "上传语音失败" else "上传图片失败"
                Result.failure(IOException(apiResponse.msg ?: defaultFailMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 提交/确认头像 URL (POST /profile/avatar/commit)
     */
    suspend fun commitAvatar(avatarUrl: String): Result<AvatarData> = apiDataRequest<AvatarData>(
        path = "/profile/avatar/commit",
        type = object : TypeToken<ApiResponse<AvatarData>>() {}.type,
        params = mapOf("url" to avatarUrl),
        fallbackMessage = "更新头像失败"
    ).mapCatching { data ->
        // 服务端可能不回 data，此时用提交的地址兜底
        (data ?: AvatarData(avatarUrl = avatarUrl, hasCustom = true)).copy(hasCustom = true)
    }

    /**
     * 清除头像 (POST /profile/avatar/clear)
     */
    suspend fun clearAvatar(): Result<AvatarData> = apiDataRequest<AvatarData>(
        path = "/profile/avatar/clear",
        type = object : TypeToken<ApiResponse<AvatarData>>() {}.type,
        fallbackMessage = "清除头像失败"
    ).mapCatching { data ->
        // 服务端可能不回 data，此时按「已清除」兜底
        (data ?: AvatarData(avatarUrl = "", hasCustom = false)).copy(hasCustom = false)
    }

    /**
     * 官方刷新头像逻辑 (对应 Web 端移动端视口下的 <span data-v-058f7f11="">刷新头像</span>)
     * 官方行为：悬浮提示为“刷新 QQ 头像”，重新从服务端拉取个人资料并带 _refresh 时间戳强刷头像 CDN / 代理接口
     */
    suspend fun refreshAvatar(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bootRes = bootstrap()
            if (bootRes.isFailure) {
                return@withContext Result.failure(bootRes.exceptionOrNull() ?: IOException("获取个人资料失败"))
            }
            val user = bootRes.getOrThrow().user
            val rawAvatarUrl = user?.avatarUrl?.trim()
            val qq = user?.qq?.trim()
            val targetUrl = when {
                !rawAvatarUrl.isNullOrBlank() -> {
                    val fullUrl = NetworkImageUrl.resolve(rawAvatarUrl)
                    val separator = if (fullUrl.contains("?")) "&" else "?"
                    "$fullUrl${separator}_refresh=${System.currentTimeMillis()}"
                }
                !qq.isNullOrBlank() -> {
                    "https://q.qlogo.cn/g?b=qq&nk=$qq&s=640&_refresh=${System.currentTimeMillis()}"
                }
                else -> null
            }

            if (targetUrl == null) {
                return@withContext Result.failure(IOException("未绑定 QQ 号或未设置头像"))
            }

            // 执行带凭据的静默请求校验与预热 (对应 Web 端 Cy(cacheKey, url, true))
            val req = Request.Builder()
                .url(targetUrl)
                .get()
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                Result.success(targetUrl)
            } else {
                Result.failure(IOException("头像刷新失败: HTTP ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 修改隐私设置 (POST /profile/update_moments_privacy)
     */
    suspend fun updateMomentsPrivacy(privacy: MomentsPrivacyData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("moments_public", privacy.momentsPublic.toString())
                .add("moments_fishable", privacy.momentsFishable.toString())
                .add("dm_public", privacy.dmPublic.toString())
                .add("dm_from_public", privacy.dmFromPublic.toString())
                .add("dm_from_moment", privacy.dmFromMoment.toString())
                .add("dm_from_private", privacy.dmFromPrivate.toString())
                .add("follow_list_public", privacy.followListPublic.toString())
                .add("show_online_status", privacy.showOnlineStatus.toString())
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/profile/update_moments_privacy")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("HTTP Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取他人主页聚合信息 (GET /api/moment/user)
     *
     * 一次返回：资料 + 获赞/关注/粉丝计数 + 关系状态 + 该用户动态列表（分页）。
     *
     * @param userId 服务端要求的用户标识（实测为对方 uid，同时兼容数字 user_id）
     * @param beforeId 动态分页游标（上一页返回的 next_before_id）
     */
    suspend fun getUserProfileAggregate(
        userId: String,
        beforeId: Long? = null,
        beforeScore: Double? = null,
        limit: Int = 6,
        sort: String = "latest"
    ): Result<UserProfileAggregateData> = apiDataRequestRequired(
        path = "/api/moment/user",
        type = object : TypeToken<ApiResponse<UserProfileAggregateData>>() {}.type,
        params = mutableMapOf(
            "user_id" to userId,
            "limit" to limit.toString(),
            "sort" to sort
        ).apply {
            if (beforeId != null && beforeId > 0L) put("before_id", beforeId.toString())
            // 官方 userFeed 同时回传 before_score（热度排序游标）
            beforeScore?.let { put("before_score", it.toString()) }
        },
        get = true,
        fallbackMessage = "获取用户主页失败"
    )

    /**
     * 获取他人关注 / 粉丝 / 互关列表 (GET /api/follow/list)
     *
     * @param kind following(关注) / followers(粉丝) / mutual(互关)，与官方 `FollowListView` 一致
     */
    suspend fun getFollowList(
        userId: String,
        kind: String,
        beforeId: Long? = null,
        limit: Int = 20
    ): Result<FollowListData> = apiDataRequestRequired(
        path = "/api/follow/list",
        type = object : TypeToken<ApiResponse<FollowListData>>() {}.type,
        params = mutableMapOf(
            "user_id" to userId,
            "kind" to kind,
            "limit" to limit.toString()
        ).apply {
            if (beforeId != null && beforeId > 0L) put("before_id", beforeId.toString())
        },
        get = true,
        fallbackMessage = "获取关注列表失败"
    )

    /**
     * 关注 / 取消关注（POST /follow/add 或 /follow/remove）
     *
     * @param follow true = 关注（/follow/add），false = 取消关注（/follow/remove）
     */
    suspend fun setFollow(userId: String, follow: Boolean): Result<Unit> = apiDataRequest<Any>(
        path = if (follow) "/follow/add" else "/follow/remove",
        type = object : TypeToken<ApiResponse<Any>>() {}.type,
        params = mapOf("user_id" to userId),
        fallbackMessage = "关注操作失败"
    ).mapCatching { Unit }

    /**
     * 拉黑 / 解除拉黑（POST /block/add 或 /block/unblock）
     *
     * 官方语义：拉黑会自动取消关注，并同时禁用关注与私信入口。
     *
     * @param block true = 拉黑（/block/add），false = 解除拉黑（/block/unblock）
     */
    suspend fun setBlock(userId: String, block: Boolean): Result<Unit> = apiDataRequest<Any>(
        path = if (block) "/block/add" else "/block/unblock",
        type = object : TypeToken<ApiResponse<Any>>() {}.type,
        params = mapOf("blocked_id" to userId),
        fallbackMessage = "拉黑操作失败"
    ).mapCatching { Unit }

    /**
     * 发起私聊 (POST /room/dm/create-from-user)
     */
    suspend fun createDmFromUser(targetUserId: String): Result<EnterRoomData> = apiDataRequestRequired(
        path = "/room/dm/create-from-user",
        type = object : TypeToken<ApiResponse<EnterRoomData>>() {}.type,
        params = mapOf("target_user_id" to targetUserId),
        fallbackMessage = "发起私聊失败"
    )

    /**
     * 查找用户 (POST /user/lookup)
     */
    suspend fun lookupUser(loginName: String = "", userId: String = ""): Result<UserLookupData> = apiDataRequestRequired(
        path = "/user/lookup",
        type = object : TypeToken<ApiResponse<UserLookupData>>() {}.type,
        params = mapOf(
            "login_name" to loginName.trim(),
            "user_id" to userId.trim()
        ),
        fallbackMessage = "未找到该用户"
    )

    /**
     * 获取黑名单列表 (GET /api/block/list)
     */
    suspend fun getBlockList(page: Int = 1, perPage: Int = 15): Result<BlockListData> = apiDataRequestRequired(
        path = "/api/block/list",
        type = object : TypeToken<ApiResponse<BlockListData>>() {}.type,
        params = mapOf(
            "page" to page.toString(),
            "per_page" to perPage.toString()
        ),
        get = true,
        fallbackMessage = "获取黑名单失败"
    )

    /**
     * 添加黑名单 (POST /block/add)
     */
    suspend fun addBlock(blockedId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("blocked_id", blockedId.toString())
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/block/add")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("HTTP Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解除黑名单 (POST /block/unblock)
     */
    suspend fun unblock(blockedId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("blocked_id", blockedId.toString())
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/block/unblock")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("HTTP Error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 设置对端备注 (POST /user/remark)
     *
     * 官方口径（`publicAnnouncementDismiss` 里的 `Id.set`）：form `peer_user_id` + `remark`，
     * **最多 20 字、留空即清除**；返回体带最新的 `remark`。
     */
    suspend fun setUserRemark(peerUserId: String, remark: String): Result<String?> =
        apiDataRequest<com.google.gson.JsonObject>(
            path = "/user/remark",
            type = object : TypeToken<ApiResponse<com.google.gson.JsonObject>>() {}.type,
            params = mapOf(
                "peer_user_id" to peerUserId,
                "remark" to remark
            ),
            fallbackMessage = "保存备注失败"
        ).mapCatching { obj ->
            obj?.get("remark")?.takeIf { it.isJsonPrimitive }?.asString
        }

    /**
     * 获取未读通知数 (/api/notifications/unread)
     */
    suspend fun getUnreadNotificationCount(): Result<Int> = apiDataRequestRequired<NotificationUnreadData>(
        path = "/api/notifications/unread",
        type = object : TypeToken<ApiResponse<NotificationUnreadData>>() {}.type,
        get = true,
        fallbackMessage = "获取未读通知数失败"
    ).mapCatching { it.unread.coerceAtLeast(0) }

    /**
     * 举报记录列表 (GET /api/report/list)
     *
     * 官网 `MyReportsView` 固定按 `page` + `per_page=10` 分页，并用 `has_more` 控制「加载更多」。
     */
    suspend fun getReportList(page: Int = 1, perPage: Int = 10): Result<ReportListData> = apiDataRequestRequired(
        path = "/api/report/list",
        type = object : TypeToken<ApiResponse<ReportListData>>() {}.type,
        params = mapOf(
            "page" to page.toString(),
            "per_page" to perPage.toString()
        ),
        get = true,
        fallbackMessage = "获取举报记录失败"
    )

    // ============================================================
    // 「我的」页面功能接口
    //  1) 安全中心  /api/security/*
    //  2) 修改密码  /profile/update（与资料更新同一端点）
    //  3) 处罚减免  /api/penalty-appeal/*
    //  4) 网易云绑定 /api/music/netease/binding/*
    // GET 走 query，POST 走 form-urlencoded；X-Device-Id 与登录 Cookie 由 OkHttp 拦截器统一附带。
    // ============================================================

    /**
     * 通用 JSON 接口请求：解析 `ApiResponse<T>` 并返回其中的 `data`
     *
     * @param type 形如 `object : TypeToken<ApiResponse<XxxData>>() {}.type`
     * @param get true 走 GET（参数拼 query），false 走 POST（参数进 form）
     * @return data 为空时成功仍返回 null（部分写操作没有业务数据）
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> apiDataRequest(
        path: String,
        type: java.lang.reflect.Type,
        params: Map<String, String> = emptyMap(),
        get: Boolean = false,
        fallbackMessage: String = "请求失败"
    ): Result<T?> = withContext(Dispatchers.IO) {
        try {
            val url = if (get) {
                val builder = "$BASE_URL$path".toHttpUrlOrNull()?.newBuilder()
                    ?: return@withContext Result.failure(IOException("无效的接口地址"))
                params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
                builder.build().toString()
            } else {
                "$BASE_URL$path"
            }

            val formBody = FormBody.Builder().apply {
                if (!get) params.forEach { (key, value) -> add(key, value) }
            }.build()

            val request = Request.Builder().url(url).apply {
                if (get) get() else post(formBody)
            }.build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP Error: ${response.code}"))
            }

            val apiResponse = gson.fromJson(bodyString, type) as? ApiResponse<T>
                ?: return@withContext Result.failure(IOException(fallbackMessage))
            if (apiResponse.isSuccess) {
                Result.success(apiResponse.data)
            } else {
                Result.failure(IOException(apiResponse.msg ?: fallbackMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 与 [apiDataRequest] 相同，但要求服务端必须返回 `data`：
     * `data` 为空时按失败处理（沿用同一 [fallbackMessage]）。
     *
     * 这样「返回非空模型」的既有方法可以只替换函数体、保持签名不变，
     * 调用方（`getOrThrow()` / `getOrNull()!!` 等）无需任何改动。
     */
    private suspend fun <T> apiDataRequestRequired(
        path: String,
        type: java.lang.reflect.Type,
        params: Map<String, String> = emptyMap(),
        get: Boolean = false,
        fallbackMessage: String = "请求失败"
    ): Result<T> = apiDataRequest<T>(path, type, params, get, fallbackMessage)
        .mapCatching { data -> data ?: throw IOException(fallbackMessage) }

    /**
     * 安全中心 · 登录设备列表 (GET /api/security/devices)
     */
    suspend fun getSecurityDevices(): Result<SecurityDevicesData?> = apiDataRequest(
        path = "/api/security/devices",
        type = object : TypeToken<ApiResponse<SecurityDevicesData>>() {}.type,
        get = true,
        fallbackMessage = "获取设备列表失败"
    )

    /**
     * 安全中心 · 注销指定设备 (POST /api/security/devices/revoke)
     *
     * 返回 `is_current` 为真表示注销的是本机，调用方需断开连接并退出登录。
     */
    suspend fun revokeSecurityDevice(deviceId: String): Result<SecurityRevokeData?> = apiDataRequest(
        path = "/api/security/devices/revoke",
        type = object : TypeToken<ApiResponse<SecurityRevokeData>>() {}.type,
        params = mapOf("device_id" to deviceId),
        fallbackMessage = "注销失败"
    )

    /**
     * 安全中心 · 注销其他所有设备 (POST /api/security/devices/revoke-others)
     *
     * 官方 API 具备该端点但页面未暴露入口。
     */
    suspend fun revokeOtherSecurityDevices(): Result<Unit> = apiDataRequest<com.google.gson.JsonObject>(
        path = "/api/security/devices/revoke-others",
        type = object : TypeToken<ApiResponse<com.google.gson.JsonObject>>() {}.type,
        fallbackMessage = "注销失败"
    ).map { }

    /**
     * 安全中心 · 登录/安全事件历史 (GET /api/security/login-history)
     *
     * @param eventType 为空表示全部事件；取值见 `SecurityEventType`
     */
    suspend fun getSecurityLoginHistory(
        page: Int = 1,
        limit: Int = 5,
        eventType: String? = null
    ): Result<SecurityLoginHistoryData?> = apiDataRequest(
        path = "/api/security/login-history",
        type = object : TypeToken<ApiResponse<SecurityLoginHistoryData>>() {}.type,
        params = buildMap {
            put("page", page.toString())
            put("limit", limit.toString())
            if (!eventType.isNullOrBlank()) put("event_type", eventType)
        },
        get = true,
        fallbackMessage = "获取登录历史失败"
    )

    /**
     * 修改密码（复用 POST /profile/update）
     *
     * 官方没有独立的改密端点：同一表单里带上 `username`（昵称，必须原样回填，
     * 留空会把昵称改没）、`qq`（可空）与三个密码字段；服务端校验失败会直接
     * 返回 `{"code":0,"msg":"当前密码不正确"}`，由调用方原样提示。
     */
    suspend fun changePassword(
        username: String,
        qq: String?,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<UpdateProfileData?> = apiDataRequest(
        path = "/profile/update",
        type = object : TypeToken<ApiResponse<UpdateProfileData>>() {}.type,
        params = buildMap {
            put("username", username.trim())
            if (!qq.isNullOrBlank()) put("qq", qq.trim())
            put("current_password", currentPassword)
            put("new_password", newPassword)
            put("confirm_password", confirmPassword)
        },
        fallbackMessage = "密码修改失败"
    )

    /**
     * 处罚减免 · 页面初始化 (GET /api/penalty-appeal/bootstrap)
     */
    suspend fun getPenaltyAppealBootstrap(): Result<PenaltyAppealBootstrapData?> = apiDataRequest(
        path = "/api/penalty-appeal/bootstrap",
        type = object : TypeToken<ApiResponse<PenaltyAppealBootstrapData>>() {}.type,
        get = true,
        fallbackMessage = "加载失败"
    )

    /**
     * 处罚减免 · 提交申请 (POST /api/penalty-appeal/submit)
     *
     * 官方把所有参数放进 form，其中两个数组以 **JSON 字符串** 提交，`ack_guidelines` 固定为 1。
     * 注意：接口不收检讨正文，正文即上传的手写照片内容。
     */
    suspend fun submitPenaltyAppeal(
        penaltyKeys: List<String>,
        evidenceUrls: List<String>,
        ackGuidelines: Boolean = true
    ): Result<Unit> = apiDataRequest<com.google.gson.JsonObject>(
        path = "/api/penalty-appeal/submit",
        type = object : TypeToken<ApiResponse<com.google.gson.JsonObject>>() {}.type,
        params = mapOf(
            "penalty_keys" to gson.toJson(penaltyKeys),
            "evidence_urls" to gson.toJson(evidenceUrls),
            "ack_guidelines" to if (ackGuidelines) "1" else "0"
        ),
        fallbackMessage = "提交失败"
    ).map { }

    /**
     * 处罚减免 · 上传手写检讨图片 (POST /api/upload，upload_source=penalty_appeal_image)
     */
    suspend fun uploadPenaltyEvidence(
        fileBytes: ByteArray,
        filename: String,
        contentType: String
    ): Result<String> = uploadFile(
        fileBytes = fileBytes,
        filename = filename,
        contentType = contentType,
        uploadSource = "penalty_appeal_image",
        legacyType = "image"
    )

    /**
     * 网易云绑定 · 绑定状态 (GET /api/music/netease/binding)
     */
    suspend fun getNeteaseBinding(): Result<NeteaseBindingData?> = apiDataRequest(
        path = "/api/music/netease/binding",
        type = object : TypeToken<ApiResponse<NeteaseBindingData>>() {}.type,
        get = true,
        fallbackMessage = "加载失败"
    )

    /**
     * 网易云绑定 · 生成扫码会话 (POST /api/music/netease/binding/qrcode/start)
     */
    suspend fun startNeteaseQrBind(): Result<NeteaseQrStartData?> = apiDataRequest(
        path = "/api/music/netease/binding/qrcode/start",
        type = object : TypeToken<ApiResponse<NeteaseQrStartData>>() {}.type,
        params = mapOf("ack_disclaimer" to "1"),
        fallbackMessage = "生成二维码失败"
    )

    /**
     * 网易云绑定 · 轮询扫码状态 (GET /api/music/netease/binding/qrcode/status)
     */
    suspend fun getNeteaseQrStatus(sessionId: String): Result<NeteaseQrStatusData?> = apiDataRequest(
        path = "/api/music/netease/binding/qrcode/status",
        type = object : TypeToken<ApiResponse<NeteaseQrStatusData>>() {}.type,
        params = mapOf("session_id" to sessionId),
        get = true,
        fallbackMessage = "扫码状态查询失败"
    )

    /**
     * 网易云绑定 · 作废本次二维码 (POST /api/music/netease/binding/qrcode/cancel)
     */
    suspend fun cancelNeteaseQrBind(sessionId: String): Result<Unit> = apiDataRequest<com.google.gson.JsonObject>(
        path = "/api/music/netease/binding/qrcode/cancel",
        type = object : TypeToken<ApiResponse<com.google.gson.JsonObject>>() {}.type,
        params = mapOf("session_id" to sessionId),
        fallbackMessage = "取消失败"
    ).map { }

    /**
     * 网易云绑定 · 粘贴 Cookie 绑定 (POST /api/music/netease/binding/cookie)
     */
    suspend fun bindNeteaseCookie(cookie: String): Result<NeteaseBindingData?> = apiDataRequest(
        path = "/api/music/netease/binding/cookie",
        type = object : TypeToken<ApiResponse<NeteaseBindingData>>() {}.type,
        params = mapOf("cookie" to cookie, "ack_disclaimer" to "1"),
        fallbackMessage = "绑定失败"
    )

    /**
     * 网易云绑定 · 解除绑定 (POST /api/music/netease/binding/unbind)
     */
    suspend fun unbindNetease(): Result<NeteaseBindingData?> = apiDataRequest(
        path = "/api/music/netease/binding/unbind",
        type = object : TypeToken<ApiResponse<NeteaseBindingData>>() {}.type,
        fallbackMessage = "解绑失败"
    )

    /**
     * 网易云绑定 · 刷新登录态 (POST /api/music/netease/binding/refresh)
     */
    suspend fun refreshNeteaseBinding(): Result<NeteaseBindingData?> = apiDataRequest(
        path = "/api/music/netease/binding/refresh",
        type = object : TypeToken<ApiResponse<NeteaseBindingData>>() {}.type,
        fallbackMessage = "续期失败"
    )

    /**
     * 举报动态 (POST /moment/report)
     *
     * 官方字段：`moment_id`、`reason`、`description`（可选，空串即可）；
     * reason 取值见 `top.lanxint.zerotalk.ui.messages.UserReportReason`。
     */
    suspend fun reportMoment(
        momentId: Long,
        reason: String,
        description: String
    ): Result<Unit> = apiDataRequest<com.google.gson.JsonObject>(
        path = "/moment/report",
        type = object : TypeToken<ApiResponse<com.google.gson.JsonObject>>() {}.type,
        params = mapOf(
            "moment_id" to momentId.toString(),
            "reason" to reason,
            "description" to description
        ),
        fallbackMessage = "举报提交失败"
    ).map { }

    /**
     * 获取当前全站在线用户数 (GET /api/home/online)
     */
    suspend fun getOnlineCount(): Result<OnlineCountData> = apiDataRequestRequired<OnlineCountData>(
        path = "/api/home/online",
        type = object : TypeToken<ApiResponse<OnlineCountData>>() {}.type,
        get = true,
        fallbackMessage = "获取在线人数失败"
    )

    // ============================================================
    // 服务与支持：联系我们 / 问题反馈 / 捐赠本站
    // （依据官网 contact-Qek2ePzN.js 与 DonateView，勿与站内举报混用）
    // ============================================================

    /**
     * 联系我们 · 页初始化 (GET /api/contact/bootstrap)
     *
     * 同一个接口同时服务两个页面：
     * - 「联系我们」取 `contact_config`（群二维码 / 群名 / 邮箱 / QQ / 微信）与 `other_contact`；
     * - 「问题反馈」取 `contact_config.suggestion_intro` 与 `user_feedbacks`（我的反馈记录）。
     */
    suspend fun getContactBootstrap(): Result<ContactBootstrapData> = apiDataRequestRequired(
        path = "/api/contact/bootstrap",
        type = object : TypeToken<ApiResponse<ContactBootstrapData>>() {}.type,
        get = true,
        fallbackMessage = "获取联系方式失败"
    )

    /**
     * 问题反馈 · 提交 (POST /contact/submit)
     *
     * ⚠️ 官网此路径**不带 `/api` 前缀**（见 `contact-Qek2ePzN.js`）；服务端只回 `code/msg` 不返回 `data`，
     * 故用 [apiDataRequest] 再 `map { }` 收敛为 [Unit]。
     */
    suspend fun submitFeedback(
        type: String,
        title: String,
        content: String
    ): Result<Unit> = apiDataRequest<com.google.gson.JsonObject>(
        path = "/contact/submit",
        type = object : TypeToken<ApiResponse<com.google.gson.JsonObject>>() {}.type,
        params = mapOf(
            "type" to type,
            "title" to title,
            "content" to content
        ),
        fallbackMessage = "提交失败，请稍后重试"
    ).map { }

    /**
     * 捐赠本站 · 页初始化 (GET /api/donate)
     */
    suspend fun getDonate(): Result<DonateData> = apiDataRequestRequired(
        path = "/api/donate",
        type = object : TypeToken<ApiResponse<DonateData>>() {}.type,
        get = true,
        fallbackMessage = "加载失败"
    )

    /**
     * 捐赠本站 · 私信捐赠者 (POST /api/donate/dm)
     *
     * 返回或复用与捐赠者的私聊房间（官网 DonateView 的「私信」动作专用接口）。
     */
    suspend fun createDonateDm(targetUserId: String): Result<EnterRoomData> = apiDataRequestRequired(
        path = "/api/donate/dm",
        type = object : TypeToken<ApiResponse<EnterRoomData>>() {}.type,
        params = mapOf("target_user_id" to targetUserId),
        fallbackMessage = "创建私聊失败"
    )

    /**
     * 捐赠本站 · 我的分享码 (GET /api/user/share)
     *
     * 付款备注用，形如 `login_name#零填充 user_id`（官网优先取本接口，失败才本地拼）。
     */
    suspend fun getMyShareCode(): Result<UserShareCodeData> = apiDataRequestRequired(
        path = "/api/user/share",
        type = object : TypeToken<ApiResponse<UserShareCodeData>>() {}.type,
        get = true,
        fallbackMessage = "获取分享码失败"
    )

    /**
     * 语音通话 · 当前通话 (GET /api/voice/call/current)
     *
     * 刷新 / 断线重连后恢复通话用（官网 `resume_start source=server`）：
     * 返回 `has_call` 与 `call{call_id, room_id, state, is_caller, peer_*, ice}`。
     */
    suspend fun getCurrentVoiceCall(): Result<VoiceCurrentCallData> = apiDataRequestRequired(
        path = "/api/voice/call/current",
        type = object : TypeToken<ApiResponse<VoiceCurrentCallData>>() {}.type,
        get = true,
        fallbackMessage = "获取通话状态失败"
    )
}
