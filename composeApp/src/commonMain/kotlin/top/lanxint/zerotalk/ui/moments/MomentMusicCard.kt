package top.lanxint.zerotalk.ui.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.MomentMusic
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.messages.tapPreservingLongPress
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.ui.utils.rememberMomentAudioPlayer
import kotlinx.coroutines.launch

/**
 * 动态内嵌网易云音乐卡片（nm-card）
 *
 * 展示封面（网易云 CDN）+ 歌名 + 歌手 / 专辑；
 * 点击播放时按官方流程先 `POST /api/music/netease/play-url`（`song_id`、`force=0`）
 * 取回直链再交给平台播放器播放，再次点击暂停。
 */
@Composable
internal fun MomentMusicCard(
    music: MomentMusic,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val player = rememberMomentAudioPlayer()
    val coroutineScope = rememberCoroutineScope()
    val notificationState = LocalNotificationState.current

    var isPlaying by remember(music.songId) { mutableStateOf(false) }
    var isLoading by remember(music.songId) { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(14.dp)
    val cardBg = if (isDark) Color(0xFF1F2430) else Color(0xFFF2F4F8)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(cardBg)
            .border(0.5.dp, higColors.separator.copy(alpha = 0.5f), cardShape)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面：直连网易云 CDN
        AsyncNetworkImage(
            url = music.coverUrl,
            contentDescription = "音乐封面",
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = music.name.ifBlank { "未知歌曲" },
                style = AppleHigTypography.subhead.copy(
                    color = higColors.label,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(4.dp))
                BasicText(
                    text = listOf(music.artists, music.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                        .ifBlank { "网易云音乐" },
                    style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // 播放 / 暂停
        val canTogglePlay = music.isPlayable && !isLoading
        // 抽成命名 lambda：手势助手与无障碍 onClick 共用同一份回调
        // （原 `return@clickable` 在此改为 else 分支）
        val togglePlay: () -> Unit = {
            if (isPlaying) {
                player.stop()
                isPlaying = false
            } else {
                isLoading = true
                coroutineScope.launch {
                    val res = ZeroTalkClientManager.apiService.getNeteasePlayUrl(music.songId, force = false)
                    isLoading = false
                    val url = res.getOrNull()?.url
                    if (url.isNullOrBlank()) {
                        notificationState.show(
                            res.exceptionOrNull()?.message ?: "暂时无法播放该歌曲"
                        )
                        return@launch
                    }
                    player.play(
                        url = url,
                        onComplete = { isPlaying = false },
                        onError = { err ->
                            isPlaying = false
                            notificationState.show(err)
                        }
                    )
                    isPlaying = true
                }
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFE11D48))
                .then(
                    if (canTogglePlay) {
                        // 用 tapPreservingLongPress 而非 clickable：clickable 会消费 down，
                        // 长按落在按钮上时父级气泡的消息菜单收不到事件
                        // （官方 DOM 里按钮的 @click 不消费长按，整块都能长按）。
                        Modifier
                            .tapPreservingLongPress(key = music.songId, onTap = togglePlay)
                            .semantics {
                                onClick(label = if (isPlaying) "暂停" else "播放") {
                                    togglePlay()
                                    true
                                }
                            }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 动态内嵌语音条
 *
 * 语音动态的 `audio_url` 由发布页 `/api/upload_audio` 上传得到；
 * 点击按需播放 / 暂停，样式与音乐卡片区分（更轻的胶囊）。
 */
@Composable
internal fun MomentVoiceBubble(
    audioUrl: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val player = rememberMomentAudioPlayer()
    var isPlaying by remember(audioUrl) { mutableStateOf(false) }
    val notificationState = LocalNotificationState.current
    val shape = RoundedCornerShape(100.dp)

    // 抽成命名 lambda：手势助手与无障碍 onClick 共用同一份回调
    val togglePlay: () -> Unit = {
        if (isPlaying) {
            player.stop()
            isPlaying = false
        } else {
            player.play(
                url = audioUrl,
                onComplete = { isPlaying = false },
                onError = { err ->
                    isPlaying = false
                    notificationState.show(err)
                }
            )
            isPlaying = true
        }
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(if (isDark) Color(0xFF2A2F3A) else Color(0xFFEDEFF4))
            .border(0.5.dp, higColors.separator.copy(alpha = 0.5f), shape)
            // 用 tapPreservingLongPress 而非 clickable：clickable 会消费 down，
            // 长按落在语音条上时父级气泡的消息菜单收不到事件
            // （官方 DOM 里控件的 @click 不消费长按，整卡都能长按）。
            .tapPreservingLongPress(key = audioUrl, onTap = togglePlay)
            .semantics {
                onClick(label = if (isPlaying) "暂停语音" else "播放语音") {
                    togglePlay()
                    true
                }
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "暂停语音" else "播放语音",
            tint = higColors.tint,
            modifier = Modifier.size(16.dp)
        )
        BasicText(
            text = if (isPlaying) "正在播放语音…" else "语音",
            style = AppleHigTypography.subhead.copy(
                color = higColors.label,
                fontSize = 14.sp
            )
        )
    }
}
