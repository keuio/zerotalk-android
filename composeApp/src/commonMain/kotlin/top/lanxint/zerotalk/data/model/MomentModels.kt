package top.lanxint.zerotalk.data.model

import org.jetbrains.compose.resources.DrawableResource

/**
 * 捞动态数据模型（预留后续与后端 API 接口对接）
 *
 * @param id 动态唯一 ID
 * @param authorId 作者用户 ID
 * @param authorName 作者昵称
 * @param authorAvatar 作者头像
 * @param authorGender 作者性别（"男" / "女"）
 * @param publishTime 相对发布时间（例如："1小时前"、"5分钟前"）
 * @param textContent 正文文本内容（可为空）
 * @param imageUrl 网络图片链接（供后续远程 API 接入）
 * @param localImageRes 本地可绘制图片资源（供本地前端演示）
 * @param likesCount 点赞数
 * @param commentsCount 评论数
 * @param isLiked 是否已点赞
 * @param isFollowed 是否已关注
 */
data class MomentItem(
    val id: String,
    val authorId: String,
    /** 作者 32 位 hex uid：关注 / 拉黑接口的权威标识（官方 `user_id=<uid>`） */
    val authorUid: String = "",
    val authorName: String,
    val authorAvatar: String = "",
    val authorGender: String = "女",
    /**
     * 作者称号（官方 MomentCard `item.title`）。
     *
     * 空串表示未下发 / 无称号，UI 不渲染徽章。
     */
    val authorTitle: String = "",
    /** 作者称号颜色 key（官方 `item.title_color`，白名单外 UI 回落 blue） */
    val authorTitleColor: String = "",
    val publishTime: String = "1小时前",
    val textContent: String = "",
    val imageUrl: String = "",
    val images: List<String> = emptyList(),
    val localImageRes: DrawableResource? = null,
    /** 语音动态的音频地址（`audio_url`），发布时由 `/api/upload_audio` 上传得到 */
    val audioUrl: String = "",
    /** 动态附带的网易云音乐（`music` 对象） */
    val music: MomentMusic? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val isFollowed: Boolean = false,
    val isPinned: Boolean = false,
    val isPrivate: Boolean = false,
    val audienceMode: String = "all",
    val audienceMutual: Boolean = false,
    val isMine: Boolean = false,
    /** 捞取时间（相对时间，如「1小时前」；仅捞取记录与捞到的动态携带） */
    val fishedAt: String = ""
)

/**
 * 动态附带的网易云音乐
 *
 * 与官方 `POST /moment/create` 的 `music` 字段结构一致：
 * `{ provider, song_id, name, artists, album, cover_url }`；
 * 封面直连网易云 CDN（`p3/p4.music.126.net/...jpg`），播放走
 * `POST /api/music/netease/play-url`（body：`song_id`、`force`）。
 */
data class MomentMusic(
    val provider: String = "netease",
    val songId: String = "",
    val name: String = "",
    val artists: String = "",
    val album: String = "",
    val coverUrl: String = ""
) {
    /** 是否为可解析播放的网易云歌曲 */
    val isPlayable: Boolean
        get() = provider.equals("netease", ignoreCase = true) && songId.isNotBlank()

    /** 发布动态时 `music` 字段的 JSON 字符串（与官方结构一致） */
    fun toJsonString(): String {
        fun plain(value: String): String = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return """{"provider":"${plain(provider)}","song_id":"${plain(songId)}","name":"${plain(name)}","artists":"${plain(artists)}","album":"${plain(album)}","cover_url":"${plain(coverUrl)}"}"""
    }
}
