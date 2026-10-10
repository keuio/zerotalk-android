package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.network.STICKER_DEFAULT_QUOTA
import top.lanxint.zerotalk.data.network.StickerItemDto
import top.lanxint.zerotalk.data.network.StickerListData
import top.lanxint.zerotalk.data.network.StickerSearchItemDto
import top.lanxint.zerotalk.data.network.normalizedQuota
import top.lanxint.zerotalk.data.network.normalizedUsed

/** 表情包配额默认值（与官方 `sticker` store 的 15 一致） */
private const val STICKER_QUOTA_FALLBACK = STICKER_DEFAULT_QUOTA

/** 搜索关键词最长字数（官方输入框 `maxlength=20`） */
private const val STICKER_SEARCH_MAX_LENGTH = 20

/**
 * 表情包面板（对齐官方 `ChatEmojiPanel` 的 `sticker` 页）
 *
 * 官方结构（逐项对齐）：
 * - 顶部搜索框：占位「搜索网络表情包…」，右侧放大镜按钮，回车 / 点按钮发起搜索；
 * - 未搜索时 meta 行右侧是 `used/quota`（如 `3/15`），正在上传时追加「上传中…」；
 * - 搜索中 meta 行变为「搜索「关键词」 · 搜索中…」+「取消搜索」；
 * - 网格首格是「+」（点击选本地图片上传到表情包，达上限时禁用）；
 * - 底部提示：未搜索「点击发送 · 长按删除」，搜索中「点击发送 · 长按添加到自己的表情包内」；
 * - 空态：「没有找到相关表情」；图片失败：「已失效」；加载中：骨架 /「表情包加载中…」。
 *
 * 交互：
 * - 我的表情包：单击发送、长按弹「删除表情包」确认；
 * - 搜索结果：单击 `prepare-url` 换到 asset_id 后发送、长按弹「添加到表情包」确认，
 *   滚动到底自动加载下一页。
 *
 * @param state 我的表情包列表（null 表示尚未加载完成）
 * @param loading 是否正在加载（骨架屏）
 * @param uploading 是否正在上传本地图片（「+」禁用 + meta 显示「上传中…」）
 * @param searchResults 网络搜索累计结果
 * @param searchKeyword 当前搜索关键词（空串 = 未搜索）
 * @param searching 是否首页搜索中（网格显示骨架）
 * @param searchLoadingMore 是否正在加载下一页（网格底部显示「加载更多…」）
 * @param searchHasMore 是否还有下一页
 * @param isDark 深色模式
 * @param onPick 点击表情发送回调（assetId, url）
 * @param onUploadLocal 点击「+」选择本地图片
 * @param onSearch 发起 / 取消搜索（keyword 为空表示取消）
 * @param onLoadMore 搜索滚动到底加载下一页
 * @param onDeleteSticker 长按「我的表情包」请求删除（recordId）
 * @param onAddSearchSticker 长按搜索结果请求添加到我的表情包（url）
 * @param onRetry 我的表情包加载失败重试
 */
@Composable
fun StickerPanel(
    state: StickerListData?,
    loading: Boolean,
    isDark: Boolean,
    onPick: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    uploading: Boolean = false,
    searchResults: List<StickerSearchItemDto> = emptyList(),
    searchKeyword: String = "",
    searching: Boolean = false,
    searchLoadingMore: Boolean = false,
    searchHasMore: Boolean = false,
    searchError: String? = null,
    onUploadLocal: (() -> Unit)? = null,
    onSearch: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onDeleteSticker: ((StickerItemDto) -> Unit)? = null,
    /** 点击网络搜索结果：交由调用方走 `prepare-url` → 发送 */
    onSendSearchSticker: ((StickerSearchItemDto) -> Unit)? = null,
    onAddSearchSticker: ((StickerSearchItemDto) -> Unit)? = null
) {
    val items = state?.entries.orEmpty()
    val used = state?.normalizedUsed() ?: 0
    val quota = state?.normalizedQuota() ?: STICKER_QUOTA_FALLBACK
    val isFull = used >= quota
    val searched = searchKeyword.isNotBlank()

    val labelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val brokenColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val fieldBg = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 输入框内容由面板持有：点上「取消搜索」时能立即清空（官方 `resetSearch` 会清 input）
        var searchInput by remember { mutableStateOf("") }
        LaunchedEffect(searchKeyword, searching) {
            if (!searching && searchKeyword.isBlank()) searchInput = ""
        }

        StickerSearchRow(
            text = searchInput,
            onTextChange = { input -> searchInput = input.take(STICKER_SEARCH_MAX_LENGTH) },
            isDark = isDark,
            labelColor = labelColor,
            fieldBg = fieldBg,
            searching = searching,
            onSearch = { keyword ->
                if (keyword.isBlank()) searchInput = ""
                onSearch(keyword)
            }
        )

        if (searching || searched) {
            // 搜索态 meta：「搜索「关键词」 · 搜索中… / 处理中…」+「取消搜索」
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = buildString {
                        append("搜索「")
                        append(searchKeyword)
                        append("」")
                        if (searching) append(" · 搜索中…")
                    },
                    style = TextStyle(color = labelColor, fontSize = 12.sp),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BasicText(
                    text = "取消搜索",
                    style = TextStyle(color = Color(0xFF007AFF), fontSize = 12.sp),
                    modifier = Modifier.clickable { onSearch("") }
                )
            }
        } else {
            // 我的表情包 meta：used/quota（+ 上传中…）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "$used/$quota",
                    style = TextStyle(color = labelColor, fontSize = 12.sp)
                )
                if (uploading) {
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicText(
                        text = "上传中…",
                        style = TextStyle(color = Color(0xFF007AFF), fontSize = 12.sp)
                    )
                }
            }
        }

        when {
            // 官方：搜索中且无结果 → 骨架；非搜索中且无结果 →「没有找到相关表情」
            searching && searchResults.isEmpty() -> StickerSearchSkeleton()
            searchResults.isEmpty() -> StickerStatus(
                text = searchError ?: "没有找到相关表情",
                color = labelColor
            )
            searched -> StickerSearchGrid(
                results = searchResults,
                isDark = isDark,
                loadingMore = searchLoadingMore,
                hasMore = searchHasMore,
                onSend = onSendSearchSticker,
                onLoadMore = onLoadMore,
                onAdd = onAddSearchSticker
            )
            loading && items.isEmpty() -> StickerStatus(text = "表情包加载中…", color = labelColor)
            items.isEmpty() -> EmptyStickerState(labelColor = labelColor, onRetry = onRetry)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 首格「+」：点击选本地图片上传到表情包（官方 `sticker-ui__cell--add`）
                item(key = "sticker-add") {
                    StickerAddCell(
                        isDark = isDark,
                        enabled = onUploadLocal != null && !uploading && !isFull,
                        onClick = { onUploadLocal?.invoke() }
                    )
                }
                items(items, key = { it.id.takeIf { id -> id > 0L } ?: it.assetId }) { item ->
                    StickerCell(
                        item = item,
                        isDark = isDark,
                        brokenColor = brokenColor,
                        onPick = onPick,
                        onLongPress = onDeleteSticker
                    )
                }
            }
        }

        // 底部提示（对齐官方 `sticker-ui__hint`）
        BasicText(
            text = if (searched) "点击发送 · 长按添加到自己的表情包内" else "点击发送 · 长按删除",
            style = TextStyle(color = labelColor, fontSize = 12.sp),
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
        )
    }
}

/** 顶部搜索框：占位「搜索网络表情包…」+ 右侧圆形放大镜按钮（官方 `sticker-ui__search`） */
@Composable
private fun StickerSearchRow(
    text: String,
    onTextChange: (String) -> Unit,
    isDark: Boolean,
    labelColor: Color,
    fieldBg: Color,
    searching: Boolean,
    onSearch: (String) -> Unit
) {
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(fieldBg)
                .border(0.5.dp, if (isDark) Color(0x1FFFFFFF) else Color(0x14000000), RoundedCornerShape(17.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                BasicText(
                    text = "搜索网络表情包…",
                    style = TextStyle(color = labelColor.copy(alpha = 0.75f), fontSize = 13.sp)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(color = textColor, fontSize = 13.sp),
                cursorBrush = SolidColor(Color(0xFF007AFF)),
                // 回车即搜索（对齐官方 `@keydown.enter.prevent`）
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(text) })
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF).copy(alpha = if (searching) 0.5f else 1f))
                .clickable(enabled = !searching) { onSearch(text) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/** 「+」上传格（官方 `sticker-ui__cell--add`，达上限 / 上传中禁用） */
@Composable
private fun StickerAddCell(
    isDark: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
            .then(
                if (enabled) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = "+",
            style = TextStyle(
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
                fontSize = 26.sp
            )
        )
    }
}

/** 单条「我的表情包」格子；url 为空或加载失败时显示「已失效」并不可点（对齐官方 `et` 的判定） */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun StickerCell(
    item: StickerItemDto,
    isDark: Boolean,
    brokenColor: Color,
    onPick: (Long, String) -> Unit,
    onLongPress: ((StickerItemDto) -> Unit)?
) {
    val assetId = item.assetId.takeIf { it > 0L } ?: item.id
    val url = item.url.trim()
    val valid = assetId > 0L && url.isNotEmpty()

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
            .then(
                if (valid) {
                    Modifier.pointerInput(item.id, assetId, url) {
                        detectTapGestures(
                            onTap = { onPick(assetId, url) },
                            onLongPress = { onLongPress?.invoke(item) }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (valid) {
            AsyncNetworkImage(
                url = url,
                contentDescription = "发送表情包 $assetId",
                modifier = Modifier.size(52.dp),
                contentScale = ContentScale.Fit,
                shape = RoundedCornerShape(10.dp),
                backgroundColor = Color.Transparent
            )
        } else {
            BasicText(
                text = "已失效",
                style = TextStyle(color = brokenColor, fontSize = 11.sp)
            )
        }
    }
}

/**
 * 网络搜索表情网格：单击发送（先 prepare-url）、长按添加到我的表情包，
 * 滚动到底自动加载下一页（对齐官方 `onScrollPassive` 的 48px 阈值）。
 */
@Composable
private fun StickerSearchGrid(
    results: List<StickerSearchItemDto>,
    isDark: Boolean,
    loadingMore: Boolean,
    hasMore: Boolean,
    onSend: ((StickerSearchItemDto) -> Unit)?,
    onLoadMore: () -> Unit,
    onAdd: ((StickerSearchItemDto) -> Unit)?
) {
    val gridState = rememberLazyGridState()
    // 不能用 `remember(keys) { derivedStateOf { ... } }`：键一变就换实例，
    // 而下面 `LaunchedEffect` 里的 `snapshotFlow` 仍订阅旧实例，翻页会失效。
    // 这里只建一次 derivedStateOf，让它自己去读最新的 hasMore / loadingMore。
    val shouldLoadMore = remember {
        derivedStateOf {
            hasMore && !loadingMore && gridState.layoutInfo.visibleItemsInfo.isNotEmpty() &&
                gridState.layoutInfo.totalItemsCount -
                gridState.layoutInfo.visibleItemsInfo.last().index <= 4
        }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .collect { if (it) onLoadMore() }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp),
            contentPadding = PaddingValues(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.url }) { item ->
                SearchStickerCell(
                    item = item,
                    isDark = isDark,
                    onTap = onSend,
                    onLongPress = onAdd
                )
            }
        }
        if (loadingMore) {
            BasicText(
                text = "加载更多…",
                style = TextStyle(
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    fontSize = 12.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

/** 搜索结果单格：无 asset_id，单击先 `prepare-url`，长按添加到表情包 */
@Composable
private fun SearchStickerCell(
    item: StickerSearchItemDto,
    isDark: Boolean,
    onTap: ((StickerSearchItemDto) -> Unit)?,
    onLongPress: ((StickerSearchItemDto) -> Unit)?
) {
    val url = item.url.trim()
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
            .pointerInput(url) {
                detectTapGestures(
                    // 搜索结果没有 asset_id：交给调用方走 prepare-url → 发送
                    onTap = { onTap?.invoke(item) },
                    onLongPress = { onLongPress?.invoke(item) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncNetworkImage(
            url = url,
            contentDescription = "网络表情",
            modifier = Modifier.size(52.dp),
            contentScale = ContentScale.Fit,
            shape = RoundedCornerShape(10.dp),
            backgroundColor = Color.Transparent
        )
    }
}

/** 空态（对齐官方「没有找到相关表情」，附重试） */
@Composable
private fun EmptyStickerState(labelColor: Color, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BasicText(
            text = "没有找到相关表情",
            style = TextStyle(color = labelColor, fontSize = 14.sp)
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(8.dp))
            BasicText(
                text = "重新加载",
                style = TextStyle(color = Color(0xFF007AFF), fontSize = 14.sp),
                modifier = Modifier.clickable { onRetry() }
            )
        }
    }
}

/** 居中状态文案（官方 `composer-emoji-panel__status`） */
@Composable
private fun StickerStatus(text: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BasicText(
            text = text,
            style = TextStyle(color = color, fontSize = 13.sp)
        )
    }
}

/** 搜索骨架（官方渲染 8 个格子骨架） */
@Composable
private fun StickerSearchSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1494A3B8))
                    )
                }
            }
        }
    }
}
