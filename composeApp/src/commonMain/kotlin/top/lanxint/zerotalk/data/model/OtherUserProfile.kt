package top.lanxint.zerotalk.data.model

import top.lanxint.zerotalk.data.network.MomentsSort

/**
 * 他人主页聚合资料（GET /api/moment/user）
 *
 * 服务端一次下发「资料 + 获赞/关注/粉丝计数 + 关系状态 + 动态列表」，
 * 客户端不再逐接口拼装，避免个性签名与动态区出现空窗或「暂时无法获取用户资料」。
 *
 * @param userId 请求与操作使用的数字 user_id（服务端下发时可能为空）
 * @param uid 32 位 hex uid（服务端关注/拉黑接口使用的标识，官方优先取 uid）
 * @param bio 个性签名
 * @param followingCount 关注数
 * @param followerCount 粉丝数
 * @param likeCount 获赞数
 * @param mutualCount 互关数
 * @param canViewFollowing 对方是否允许查看关注列表（缺省视为允许）
 * @param canViewFollowers 对方是否允许查看粉丝列表（缺省视为允许）
 * @param isSelf 是否为当前登录用户本人
 * @param isFollowing 我是否已关注对方
 * @param followedBy 对方是否关注我（互关判定）
 * @param iBlocked 我是否已拉黑对方
 * @param blocked 对方是否拉黑了我（服务端 blocked 字段）
 * @param blockedMessage 被拉黑时服务端下发的说明文案
 * @param canDm 当前是否允许向对方发起私信（缺省视为允许）
 * @param dmBanned 对方是否被封禁私聊
 * @param canReceiveDm 对方是否允许接收私信（缺省视为允许）
 * @param dmDisabledReason 不允许私信时的原因文案
 * @param momentsPublic 动态可见性：null 表示服务端未下发，false 表示明确不可见
 * @param moments 对方动态列表
 * @param hasMoreMoments 是否还有更早的动态
 */
data class OtherUserProfile(
    val userId: String = "",
    val uid: String = "",
    val name: String = "",
    val genderText: String = "",
    val ageRangeText: String = "",
    val location: String = "",
    val avatarUrl: String = "",
    val avatarFallback: String = "",
    val bio: String = "",
    val followingCount: Int = 0,
    val followerCount: Int = 0,
    val likeCount: Int = 0,
    val mutualCount: Int = 0,
    val canViewFollowing: Boolean = true,
    val canViewFollowers: Boolean = true,
    val isSelf: Boolean = false,
    val isFollowing: Boolean = false,
    val followedBy: Boolean = false,
    val iBlocked: Boolean = false,
    val blocked: Boolean = false,
    val blockedMessage: String = "",
    val canDm: Boolean = true,
    val dmBanned: Boolean = false,
    val canReceiveDm: Boolean = true,
    val dmDisabledReason: String = "",
    /** 对方清流模式：null 表示服务端未下发（官方会显示通用的功能说明文案） */
    val cleanStreamMode: Boolean? = null,
    /** 对方隐私模式（为真时官方隐藏「动态」入口） */
    val privacyMode: Boolean = false,
    val momentsPublic: Boolean? = null,
    val moments: List<MomentItem> = emptyList(),
    val hasMoreMoments: Boolean = false,
    val isOnline: Boolean? = null,
    val showOnlineStatus: Boolean? = null
) {
    /** 互相关注 */
    val isMutual: Boolean get() = isFollowing && followedBy

    /** 是否允许向对方发起私信（对齐官方 `can_dm !== false && !dm_banned && can_receive_dm !== false`） */
    val dmAllowed: Boolean get() = canDm && !dmBanned && canReceiveDm

    /** 性别 / 年龄段 / 地区 的紧凑描述行（全部为空时返回空串） */
    val subtitleLine: String
        get() = listOf(genderText, ageRangeText, location)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
}

/**
 * 他人主页 UI 状态（含加载与错误信息，便于页面重试而不是直接显示服务端报错）
 */
data class OtherUserProfileState(
    val userId: String = "",
    val uid: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val profile: OtherUserProfile? = null
)

/**
 * 打开「用户资料面板」的入口参数
 *
 * 群聊点发送者、群聊面板点成员时，调用方手上通常已有昵称/头像（消息或成员列表下发），
 * 先带入作为占位，避免面板在接口返回前出现空白头像与空昵称。
 *
 * @param userId 数字 user_id（可能为空，缺省时按 uid 请求）
 * @param uid 32 位 hex uid
 * @param name 已知昵称（可空）
 * @param avatarUrl 已知头像（可空）
 */
data class UserProfileTarget(
    val userId: String,
    val uid: String,
    val name: String = "",
    val avatarUrl: String = ""
)

/**
 * 资料页层级宿主：每一层资料页有且只有一个渲染点
 *
 * 全站资料页可能被层层下钻（动态作者 → 关注列表 → 次级主页 → 再下钻……），
 * 层级栈见 `ZeroTalkClientManager.userProfileLayers`。
 */
enum class UserProfileLayerHost {
    /** ZeroTalkApp 全屏覆盖层（动态卡片作者、捞取记录） */
    APP,

    /** 大厅内点发送者打开的资料面板 */
    HALL,

    /**
     * 资料页自身内部视差推入的一层（关注 / 粉丝列表点击进入次级主页）。
     * 资料页是递归嵌套的，每层页面实例各持有自己的 owner，据此认领自己要画的那一层。
     */
    PAGE,

    /** 私聊页内推入的一层（群成员资料面板） */
    CHAT
}

/**
 * 资料页被覆盖时的整页快照（资料 + 关系列表 + 动态分页游标）
 *
 * 下钻一层时随该层一起保存，返回时原样恢复：上一层被全局单例覆写后不会出现数据闪烁或列表丢失。
 */
data class UserProfilePageSnapshot(
    val profileState: OtherUserProfileState?,
    val followListState: OtherUserFollowListState?,
    val followCursors: Map<FollowListKind, Long?>,
    val momentsSort: MomentsSort,
    val momentsCursor: Long?,
    val momentsScoreCursor: Double?
)

/**
 * 资料页层级栈元素 —— 全站唯一的「当前打开了哪几层资料页」事实来源
 *
 * @param target 本层展示的用户
 * @param host 本层由哪个渲染点绘制
 * @param owner 渲染本层的不透明实例标识（递归嵌套的 PAGE / CHAT 层据此区分实例；APP / HALL 层为 null）
 * @param restoreSnapshot 压入本层时被覆盖页面的快照；顶层入口压入的第一层为 null（下面不是资料页）
 */
data class UserProfileLayer(
    val target: UserProfileTarget,
    val host: UserProfileLayerHost,
    val owner: Any? = null,
    val restoreSnapshot: UserProfilePageSnapshot? = null
)

/**
 * 关注 / 粉丝 / 互关关系类型（与官方 FollowListView 的 kind 取值一致）
 */
enum class FollowListKind(val param: String, val title: String) {
    FOLLOWING("following", "关注"),
    MUTUAL("mutual", "互关"),
    FOLLOWERS("followers", "粉丝")
}

/**
 * 关注 / 粉丝列表项（GET /api/follow/list）
 */
data class FollowUserItem(
    val uid: String = "",
    val userId: String = "",
    val name: String = "",
    val genderText: String = "",
    val avatarUrl: String = "",
    val isFollowing: Boolean = false,
    val isFollower: Boolean = false,
    val isMutual: Boolean = false,
    val iBlocked: Boolean = false,
    val isSelf: Boolean = false
) {
    /** 列表去重使用的唯一键 */
    val key: String get() = uid.ifBlank { userId }

    /** 是否指向同一用户（uid 与数字 user_id 任一命中即可） */
    fun matches(otherUserId: String, otherUid: String): Boolean {
        val byUid = otherUid.isNotBlank() && uid.isNotBlank() && uid == otherUid
        val byId = otherUserId.isNotBlank() && userId.isNotBlank() && userId == otherUserId
        return byUid || byId
    }
}

/**
 * 关注 / 粉丝 / 互关列表 UI 状态
 *
 * @param allowed 服务端是否允许查看该列表（官方语义 `allowed !== false`，缺省允许）
 * @param message 不可查看时服务端下发的说明文案
 */
data class OtherUserFollowListState(
    val kind: FollowListKind = FollowListKind.FOLLOWING,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val allowed: Boolean = true,
    val message: String = "",
    /** 互关列表仅本人主页可查看（官方 `ne("mutual")` 需要 is_self），非本人主页不提供该分段 */
    val mutualAllowed: Boolean = false,
    val list: List<FollowUserItem> = emptyList(),
    val hasMore: Boolean = false
)
