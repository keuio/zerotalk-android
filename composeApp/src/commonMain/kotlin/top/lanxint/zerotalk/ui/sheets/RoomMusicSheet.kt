package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.MomentMusic
import top.lanxint.zerotalk.data.network.RoomMusicPlaylistItem
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.moments.MomentNeteasePickerSheet
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.ui.utils.rememberMomentAudioPlayer
import kotlinx.coroutines.launch

/**
 * 房间共享音乐播放列表面板（成员共享 · 多端同步，上限 500 首）
 *
 * 遵循官方客户端行为规范：
 * - GET /api/room/music/playlist?room_id=xxx
 * - POST /room/music/playlist/add
 * - POST /room/music/playlist/remove
 * - POST /room/music/playlist/clear
 * - POST /room/music/playlist/play-mode
 * - version 乐观锁递增机制
 */
@Composable
fun RoomMusicSheet(
    roomId: String,
    isDark: Boolean,
    onSendToChat: (MomentMusic) -> Unit,
    onDismiss: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current
    val coroutineScope = rememberCoroutineScope()
    val audioPlayer = rememberMomentAudioPlayer()

    var playlist by remember { mutableStateOf<List<RoomMusicPlaylistItem>>(emptyList()) }
    var currentVersion by remember { mutableStateOf(0) }
    var playMode by remember { mutableStateOf("loop-all") }
    var isLoading by remember { mutableStateOf(true) }
    var isAddingSong by remember { mutableStateOf(false) }

    var playingSongId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    fun refreshList() {
        isLoading = true
        coroutineScope.launch {
            val res = ZeroTalkClientManager.apiService.getRoomMusicPlaylist(roomId)
            isLoading = false
            res.onSuccess { data ->
                playlist = data.allSongs
                currentVersion = data.version
                playMode = data.playMode
            }.onFailure { err ->
                notificationState.show(err.message ?: "拉取歌单失败")
            }
        }
    }

    LaunchedEffect(roomId) {
        refreshList()
    }

    if (isAddingSong) {
        MomentNeteasePickerSheet(
            isDark = isDark,
            onPick = { music ->
                isAddingSong = false
                coroutineScope.launch {
                    val res = ZeroTalkClientManager.apiService.addRoomMusic(
                        roomId = roomId,
                        songId = music.songId,
                        name = music.name,
                        artists = music.artists,
                        album = music.album,
                        coverUrl = music.coverUrl,
                        version = currentVersion
                    )
                    res.onSuccess { updated ->
                        currentVersion = updated.version
                        playlist = updated.allSongs
                        notificationState.show("已加入房间歌单")
                    }.onFailure { err ->
                        notificationState.show(err.message ?: "添加歌曲失败")
                    }
                }
            },
            onDismiss = { isAddingSong = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 顶部控制行：标题 + 播放模式 + 清空
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(20.dp)
                )
                BasicText(
                    text = "房间音乐 (${playlist.size}/500)",
                    style = TextStyle(
                        color = higColors.label,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 循环模式切换
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val nextMode = if (playMode == "loop-all") "loop-one" else "loop-all"
                            coroutineScope.launch {
                                val res = ZeroTalkClientManager.apiService.setRoomMusicPlayMode(
                                    roomId = roomId,
                                    playMode = nextMode,
                                    version = currentVersion
                                )
                                res.onSuccess {
                                    playMode = nextMode
                                    currentVersion = it.version
                                    notificationState.show(if (nextMode == "loop-one") "单曲循环" else "列表循环")
                                }
                            }
                        }
                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playMode == "loop-one") Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "播放模式",
                        tint = higColors.label,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 清空按钮
                if (playlist.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                coroutineScope.launch {
                                    val res = ZeroTalkClientManager.apiService.clearRoomMusic(roomId, currentVersion)
                                    res.onSuccess {
                                        playlist = emptyList()
                                        currentVersion = it.version
                                        notificationState.show("已清空歌单")
                                    }.onFailure { err ->
                                        notificationState.show(err.message ?: "清空失败")
                                    }
                                }
                            }
                            .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "清空",
                            style = TextStyle(
                                color = Color(0xFFFF3B30),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // 关闭按钮
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = higColors.secondaryLabel,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 操作栏：添加歌曲 & 播放全部
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 添加歌曲按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF007AFF))
                    .clickable { isAddingSong = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    BasicText(
                        text = "添加歌曲",
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // 播放全部
            if (playlist.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                        .clickable {
                            val first = playlist.firstOrNull() ?: return@clickable
                            coroutineScope.launch {
                                val res = ZeroTalkClientManager.apiService.getNeteasePlayUrl(first.songId)
                                val playUrl = res.getOrNull()?.url.orEmpty()
                                if (playUrl.isNotBlank()) {
                                    playingSongId = first.songId
                                    isPlaying = true
                                    audioPlayer.play(playUrl)
                                } else {
                                    notificationState.show("无法获取歌曲播放直链")
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = higColors.label,
                            modifier = Modifier.size(18.dp)
                        )
                        BasicText(
                            text = "播放全部",
                            style = TextStyle(color = higColors.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 歌曲列表主体
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF007AFF)
                )
            }
        } else if (playlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "暂无共享音乐，点击上方按钮添加歌曲",
                    style = TextStyle(
                        color = higColors.secondaryLabel,
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(playlist, key = { _, item -> item.songId }) { index, item ->
                    val isCurrent = playingSongId == item.songId && isPlaying
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) Color(0xFF007AFF).copy(alpha = 0.12f) else if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 封面
                        AsyncNetworkImage(
                            url = item.coverUrl,
                            contentDescription = item.name,
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(Modifier.width(10.dp))

                        // 歌名与歌手
                        Column(modifier = Modifier.weight(1f)) {
                            BasicText(
                                text = item.name.ifBlank { "未知歌曲" },
                                style = AppleHigTypography.subhead.copy(
                                    color = if (isCurrent) Color(0xFF007AFF) else higColors.label,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            BasicText(
                                text = item.artists.ifBlank { "未知歌手" },
                                style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 动作按钮组
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 播放 / 暂停
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                                    .clickable {
                                        if (isCurrent) {
                                            audioPlayer.stop()
                                            isPlaying = false
                                        } else {
                                            coroutineScope.launch {
                                                val res = ZeroTalkClientManager.apiService.getNeteasePlayUrl(item.songId)
                                                val playUrl = res.getOrNull()?.url.orEmpty()
                                                if (playUrl.isNotBlank()) {
                                                    playingSongId = item.songId
                                                    isPlaying = true
                                                    audioPlayer.play(playUrl)
                                                } else {
                                                    notificationState.show("无法获取歌曲播放直链")
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "播放",
                                    tint = if (isCurrent) Color(0xFF007AFF) else higColors.label,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 发送到聊天
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34C759).copy(alpha = 0.15f))
                                    .clickable {
                                        onSendToChat(
                                            MomentMusic(
                                                songId = item.songId,
                                                name = item.name,
                                                artists = item.artists,
                                                album = item.album,
                                                coverUrl = item.coverUrl
                                            )
                                        )
                                        notificationState.show("已发送歌曲到聊天")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "发送到聊天",
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // 删除
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                                    .clickable {
                                        coroutineScope.launch {
                                            val res = ZeroTalkClientManager.apiService.removeRoomMusic(
                                                roomId = roomId,
                                                songId = item.songId,
                                                version = currentVersion
                                            )
                                            res.onSuccess { updated ->
                                                currentVersion = updated.version
                                                playlist = updated.allSongs
                                                notificationState.show("已从歌单移除")
                                            }.onFailure { err ->
                                                notificationState.show(err.message ?: "移除失败")
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = Color(0xFFFF3B30),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
