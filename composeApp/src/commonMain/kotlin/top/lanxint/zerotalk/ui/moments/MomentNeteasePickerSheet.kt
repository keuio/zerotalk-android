package top.lanxint.zerotalk.ui.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.MomentMusic
import top.lanxint.zerotalk.data.network.NeteaseSongDto
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AsyncNetworkImage
import top.lanxint.zerotalk.ui.components.LocalNotificationState
import top.lanxint.zerotalk.ui.components.SelectableChip
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import kotlinx.coroutines.launch

/**
 * 网易云选歌弹窗（对齐官方 NeteaseResolveModal 三入口）
 *
 * 1. 链接 / 歌曲 ID：POST /api/music/netease/resolve（input=歌曲ID或163分享链接）
 * 2. 搜索：POST /api/music/netease/search（keyword / offset / limit）
 * 3. 歌单：POST /api/music/netease/playlist/resolve（input=歌单链接）
 *
 * 选中后通过 [onPick] 返回包装好的动态 music 对象。
 */
@Composable
fun MomentNeteasePickerSheet(
    isDark: Boolean,
    onPick: (MomentMusic) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current
    val coroutineScope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(NeteasePickMode.RESOLVE) }
    var input by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var playlistInput by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<NeteaseSongDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun doResolve(value: String) {
        if (value.isBlank()) return
        isLoading = true
        coroutineScope.launch {
            val res = ZeroTalkClientManager.apiService.resolveNeteaseSong(value.trim())
            isLoading = false
            val song = res.getOrNull()?.toSong()
            if (song != null) {
                onPick(song.toMomentMusic())
            } else {
                notificationState.show(res.exceptionOrNull()?.message ?: "未找到该歌曲")
            }
        }
    }

    fun doSearch(value: String) {
        if (value.isBlank()) return
        isLoading = true
        coroutineScope.launch {
            val res = ZeroTalkClientManager.apiService.searchNeteaseSongs(value.trim(), offset = 0, limit = 20)
            isLoading = false
            val songs = res.getOrNull()?.items ?: emptyList()
            if (songs.isEmpty()) {
                notificationState.show(res.exceptionOrNull()?.message ?: "没有找到相关歌曲")
            } else {
                results = songs
            }
        }
    }

    fun doResolvePlaylist(value: String) {
        if (value.isBlank()) return
        isLoading = true
        coroutineScope.launch {
            val res = ZeroTalkClientManager.apiService.resolveNeteasePlaylist(value.trim())
            isLoading = false
            val songs = res.getOrNull()?.items ?: emptyList()
            if (songs.isEmpty()) {
                notificationState.show(res.exceptionOrNull()?.message ?: "歌单解析失败或为空")
            } else {
                results = songs
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
    ) {
        // 顶栏由承载它的 AppleModalBottomSheet 统一绘制（标题「选择网易云音乐」）

        // 三个入口 Tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectableChip(
                label = "链接 / ID",
                selected = mode == NeteasePickMode.RESOLVE,
                isDark = isDark,
                onClick = { mode = NeteasePickMode.RESOLVE }
            )
            SelectableChip(
                label = "搜索",
                selected = mode == NeteasePickMode.SEARCH,
                isDark = isDark,
                onClick = { mode = NeteasePickMode.SEARCH }
            )
            SelectableChip(
                label = "歌单",
                selected = mode == NeteasePickMode.PLAYLIST,
                isDark = isDark,
                onClick = { mode = NeteasePickMode.PLAYLIST }
            )
        }

        Spacer(Modifier.height(12.dp))

        when (mode) {
            NeteasePickMode.RESOLVE -> NeteaseInputRow(
                placeholder = "粘贴 163 分享链接或输入歌曲 ID",
                value = input,
                onValueChange = { input = it },
                onSubmit = { doResolve(input) },
                isDark = isDark
            )

            NeteasePickMode.SEARCH -> NeteaseInputRow(
                placeholder = "输入歌名 / 歌手 / 专辑关键词",
                value = keyword,
                onValueChange = { keyword = it },
                onSubmit = { doSearch(keyword) },
                isDark = isDark
            )

            NeteasePickMode.PLAYLIST -> NeteaseInputRow(
                placeholder = "粘贴网易云歌单链接",
                value = playlistInput,
                onValueChange = { playlistInput = it },
                onSubmit = { doResolvePlaylist(playlistInput) },
                isDark = isDark
            )
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.dp,
                    color = higColors.tint
                )
            }
        } else if (results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.songId }) { song ->
                    NeteaseSongRow(
                        song = song,
                        isDark = isDark,
                        onClick = { onPick(song.toMomentMusic()) }
                    )
                }
            }
        }
    }
}

private enum class NeteasePickMode { RESOLVE, SEARCH, PLAYLIST }

private fun NeteaseSongDto.toMomentMusic(): MomentMusic = MomentMusic(
    provider = "netease",
    songId = songId,
    name = name,
    artists = artists,
    album = album,
    coverUrl = coverUrl
)

@Composable
private fun NeteaseInputRow(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(higColors.quaternarySystemFill)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = higColors.label, fontSize = 14.sp),
                cursorBrush = SolidColor(higColors.tint),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = TextStyle(color = higColors.placeholderText, fontSize = 13.sp)
                        )
                    }
                    inner()
                }
            )
        }
        Box(
            modifier = Modifier
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(higColors.tint)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSubmit
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "查询",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun NeteaseSongRow(
    song: NeteaseSongDto,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(higColors.quaternarySystemFill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncNetworkImage(
            url = song.coverUrl,
            contentDescription = "歌曲封面",
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = song.name.ifBlank { "未知歌曲" },
                style = AppleHigTypography.subhead.copy(
                    color = higColors.label,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = song.artists.ifBlank { "网易云音乐" },
                style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
