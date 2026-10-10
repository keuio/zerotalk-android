package top.lanxint.zerotalk.data.model

import com.google.gson.annotations.SerializedName

/**
 * 游戏类型常量
 */
object GameType {
    const val GOBANG = "gobang"           // 五子棋
    const val GO = "go"                   // 围棋
    const val XIANGQI = "xiangqi"         // 中国象棋
    const val CHESS = "chess"             // 国际象棋
    const val UNDERCOVER = "undercover"   // 谁是卧底

    fun getDisplayName(type: String): String = when (type.lowercase()) {
        GOBANG -> "五子棋"
        GO -> "围棋"
        XIANGQI -> "中国象棋"
        CHESS -> "国际象棋"
        UNDERCOVER -> "谁是卧底"
        else -> "对战小游戏"
    }
}

/**
 * 聊天卡片内展示的游戏邀请/对局简要模型（WS type 为游戏类型时的 content 解析结果）
 */
data class ChatGameInvite(
    @SerializedName("game_id") val gameId: Long = 0L,
    @SerializedName("game_type") val gameType: String = "",
    @SerializedName("status") val status: String = "waiting", // waiting | playing | finished | cancelled
    @SerializedName("white_username") val whiteUsername: String? = null,
    @SerializedName("black_username") val blackUsername: String? = null,
    @SerializedName("red_username") val redUsername: String? = null,
    @SerializedName("white_avatar_url") val whiteAvatarUrl: String? = null,
    @SerializedName("black_avatar_url") val blackAvatarUrl: String? = null,
    @SerializedName("red_avatar_url") val redAvatarUrl: String? = null,
    @SerializedName("creator_uid") val creatorUid: String? = null,
    @SerializedName("white_uid") val whiteUid: String? = null,
    @SerializedName("black_uid") val blackUid: String? = null,
    @SerializedName("red_uid") val redUid: String? = null,
    @SerializedName("winner_uid") val winnerUid: String? = null,
    @SerializedName("result") val result: String = "none", // none | draw | resign | white_win | black_win
    @SerializedName("message_id") val messageId: Long = 0L,
    @SerializedName("max_players") val maxPlayers: Int = 4,
    @SerializedName("player_count") val playerCount: Int = 0,
    @SerializedName("phase") val phase: String? = null
)

data class GameDetailResponseData(
    @SerializedName("game") val game: GameSessionDetail
)

/**
 * 权威对局详情数据模型（GET /api/game/<type>?id=... 以及 *_update WS 推送中的 game 对象）
 */
data class GameSessionDetail(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("room_id") val roomId: String = "",
    @SerializedName("game_type") val gameType: String = "",
    @SerializedName("status") val status: String = "waiting", // waiting | playing | finished | cancelled
    @SerializedName("creator_uid") val creatorUid: String? = null,
    @SerializedName("white_username") val whiteUsername: String? = null,
    @SerializedName("black_username") val blackUsername: String? = null,
    @SerializedName("red_username") val redUsername: String? = null,
    @SerializedName("white_avatar_url") val whiteAvatarUrl: String? = null,
    @SerializedName("black_avatar_url") val blackAvatarUrl: String? = null,
    @SerializedName("red_avatar_url") val redAvatarUrl: String? = null,
    @SerializedName("white_uid") val whiteUid: String? = null,
    @SerializedName("black_uid") val blackUid: String? = null,
    @SerializedName("red_uid") val redUid: String? = null,
    @SerializedName("winner_uid") val winnerUid: String? = null,
    @SerializedName("turn") val turn: Int = 1, // 1=红/白, 2=黑
    @SerializedName("turn_side") val turnSide: String? = null, // "white" | "black" | "red"
    @SerializedName("move_count") val moveCount: Int = 0,
    @SerializedName("version") val version: Int = 1,
    @SerializedName("result") val result: String = "none",
    @SerializedName("message_id") val messageId: Long = 0L,
    @SerializedName("board") val board: String = "",
    @SerializedName("legal_moves") val legalMoves: List<List<Int>> = emptyList(),
    @SerializedName("in_check") val inCheck: Boolean = false,
    @SerializedName("last_move") val lastMove: Any? = null,
    @SerializedName("last_x") val lastX: Int? = null,
    @SerializedName("last_y") val lastY: Int? = null,
    @SerializedName("castling") val castling: String? = null,
    @SerializedName("ep") val ep: List<Int>? = null,
    @SerializedName("captures") val captures: Map<String, Int>? = null,
    @SerializedName("score") val score: Any? = null,
    @SerializedName("passes") val passes: Int = 0,
    @SerializedName("sync_chat_to_room") val syncChatToRoom: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("finished_at") val finishedAt: String? = null,

    // 谁是卧底专属字段
    @SerializedName("phase") val phase: String? = null, // WAITING | ROLE_REVEAL | DESCRIPTION | DISCUSSION | VOTING | VOTE_RESULT | WHITE_GUESS | GAME_OVER
    @SerializedName("settings") val settings: UndercoverSettings? = null,
    @SerializedName("players") val players: List<UndercoverPlayer> = emptyList(),
    @SerializedName("order") val order: List<String> = emptyList(),
    @SerializedName("round") val round: Int = 0,
    @SerializedName("describe_index") val describeIndex: Int = 0,
    @SerializedName("phase_deadline") val phaseDeadline: Long? = null,
    @SerializedName("server_now") val serverNow: Long? = null,
    @SerializedName("descriptions") val descriptions: List<UndercoverDescription> = emptyList(),
    @SerializedName("votes") val votes: UndercoverVotes? = null,
    @SerializedName("last_elim") val lastElim: UndercoverElimination? = null,
    @SerializedName("winner_camp") val winnerCamp: String? = null, // civilian | undercover | blank
    @SerializedName("words") val words: Map<String, String>? = null,
    @SerializedName("me") val me: UndercoverMe? = null,
    @SerializedName("host_uid") val hostUid: String? = null
)

data class UndercoverSettings(
    @SerializedName("max_players") val maxPlayers: Int = 4,
    @SerializedName("blank_enabled") val blankEnabled: Boolean = false,
    @SerializedName("describe_sec") val describeSec: Int = 30,
    @SerializedName("discuss_sec") val discussSec: Int = 60,
    @SerializedName("vote_sec") val voteSec: Int = 30
)

data class UndercoverPlayer(
    @SerializedName("username") val username: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("seat") val seat: Int = 1,
    @SerializedName("ready") val ready: Boolean = false,
    @SerializedName("alive") val alive: Boolean = true,
    @SerializedName("confirmed_reveal") val confirmedReveal: Boolean = false,
    @SerializedName("uid") val uid: String = "",
    @SerializedName("user_id") val userId: Long = 0L,
    @SerializedName("role") val role: String? = null // 出局或结束后展示
)

data class UndercoverDescription(
    @SerializedName("seat") val seat: Int = 0,
    @SerializedName("username") val username: String = "",
    @SerializedName("uid") val uid: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("round") val round: Int = 1
)

data class UndercoverVotes(
    @SerializedName("candidates") val candidates: List<String> = emptyList(),
    @SerializedName("voted_uids") val votedUids: List<String> = emptyList(),
    @SerializedName("tie_count") val tieCount: Int = 0
)

data class UndercoverElimination(
    @SerializedName("username") val username: String = "",
    @SerializedName("uid") val uid: String = "",
    @SerializedName("role") val role: String? = null
)

data class UndercoverMe(
    @SerializedName("role") val role: String? = null, // civilian | undercover | blank
    @SerializedName("word") val word: String = "",
    @SerializedName("seat") val seat: Int = 1,
    @SerializedName("alive") val alive: Boolean = true
)

/**
 * 我的游戏对局列表项 (GET /api/game/my-sessions)
 */
data class MyGameSessionItem(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("room_id") val roomId: String = "",
    @SerializedName("game_type") val gameType: String = "",
    @SerializedName("status") val status: String = "waiting", // waiting | playing | finished | cancelled
    @SerializedName("creator_uid") val creatorUid: String? = null,
    @SerializedName("winner_uid") val winnerUid: String? = null,
    @SerializedName("result") val result: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("finished_at") val finishedAt: String? = null,
    @SerializedName("black_username") val blackUsername: String? = null,
    @SerializedName("white_username") val whiteUsername: String? = null,
    @SerializedName("red_username") val redUsername: String? = null
) {
    val gameId: Long get() = id
}

data class GameChatHistoryResponse(
    @SerializedName("messages") val messages: List<ChatGameMessageItem> = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean = false
)

data class ChatGameMessageItem(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("sender_uid") val senderUid: String = "",
    @SerializedName("sender_username") val senderUsername: String = "",
    @SerializedName("content") val content: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

/**
 * 对局详情 -> 聊天卡片模型。
 *
 * 官方 `*_update` 信封通常同时带 `invite`（精简卡）与 `game`（完整对局）；
 * 但文档实测 `xiangqi_update` 只带 `game`，此时用完整对局构造卡片，
 * 否则聊天里的对局卡片会僵死在初始状态直到重进会话。
 *
 * @param fallbackGameType 对局对象未带 `game_type` 时用事件名（`xiangqi` 等）兜底
 */
fun GameSessionDetail.toChatGameInvite(fallbackGameType: String? = null): ChatGameInvite = ChatGameInvite(
    gameId = id,
    gameType = gameType.orEmpty().ifBlank { fallbackGameType.orEmpty() },
    status = status.orEmpty().ifBlank { "waiting" },
    whiteUsername = whiteUsername,
    blackUsername = blackUsername,
    redUsername = redUsername,
    whiteAvatarUrl = whiteAvatarUrl,
    blackAvatarUrl = blackAvatarUrl,
    redAvatarUrl = redAvatarUrl,
    creatorUid = creatorUid,
    whiteUid = whiteUid,
    blackUid = blackUid,
    redUid = redUid,
    winnerUid = winnerUid,
    result = result.orEmpty().ifBlank { "none" },
    messageId = messageId,
    maxPlayers = settings?.maxPlayers ?: 0,
    playerCount = players.size,
    phase = phase
)

/**
 * 用服务端最新下发的卡片覆盖旧卡片，**保留旧卡片已有的身份 / 头像等字段**。
 *
 * 官方 `ge(d)`（`publicAnnouncementDismiss-*.js`）直接用 `invite` 重写整条 content；
 * 本客户端在 `invite` 缺失时用完整对局构造卡片，可能缺少头像等字段，
 * 因此按字段做「新值优先、缺失回落旧值」，只让 status / result / winner 等状态字段被覆盖。
 */
fun ChatGameInvite.overlayOn(old: ChatGameInvite?): ChatGameInvite {
    if (old == null) return this
    return copy(
        gameType = gameType.ifBlank { old.gameType },
        whiteUsername = whiteUsername ?: old.whiteUsername,
        blackUsername = blackUsername ?: old.blackUsername,
        redUsername = redUsername ?: old.redUsername,
        whiteAvatarUrl = whiteAvatarUrl ?: old.whiteAvatarUrl,
        blackAvatarUrl = blackAvatarUrl ?: old.blackAvatarUrl,
        redAvatarUrl = redAvatarUrl ?: old.redAvatarUrl,
        creatorUid = creatorUid ?: old.creatorUid,
        whiteUid = whiteUid ?: old.whiteUid,
        blackUid = blackUid ?: old.blackUid,
        redUid = redUid ?: old.redUid,
        winnerUid = winnerUid ?: old.winnerUid,
        messageId = if (messageId > 0L) messageId else old.messageId,
        maxPlayers = if (maxPlayers > 0) maxPlayers else old.maxPlayers,
        playerCount = if (playerCount > 0) playerCount else old.playerCount,
        phase = phase ?: old.phase
    )
}
