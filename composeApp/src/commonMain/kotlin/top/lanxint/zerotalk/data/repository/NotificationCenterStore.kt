package top.lanxint.zerotalk.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.data.model.NotificationCategory
import top.lanxint.zerotalk.data.model.NotificationItemDto

/**
 * 通知中心 UI 状态
 */
data class NotificationCenterUiState(
    /** 当前分类下的通知列表（按服务端顺序，未读在前由服务端保证） */
    val items: List<NotificationItemDto> = emptyList(),
    /** 当前分类 Tab */
    val category: NotificationCategory = NotificationCategory.ALL,
    /** 实时未读数（与「我的」入口角标共用同一份） */
    val unreadCount: Int = 0,
    /** 首屏 / 切分类加载中 */
    val isLoading: Boolean = false,
    /** 上拉加载更多中 */
    val isLoadingMore: Boolean = false,
    /** 「全部已读」处理中 */
    val isMarkingAllRead: Boolean = false,
    val hasMore: Boolean = false,
    val nextBeforeId: Long? = null,
    val errorMessage: String? = null
)

/**
 * 通知中心数据仓库（独立于 [ZeroTalkClientManager]，不改动其内部状态）
 *
 * 仅通过 `ZeroTalkClientManager.apiService` 调官方四个接口：
 * - `GET  /api/notifications`         列表（before_id + limit + category）
 * - `GET  /api/notifications/unread`  未读数
 * - `POST /notifications/read`        单条/多条已读（ids 逗号拼接）
 * - `POST /notifications/read-all`    全部已读（空 body）
 *
 * 状态用 StateFlow 暴露，UI 直接 `collectAsState()`。
 */
object NotificationCenterStore {
    /** 官方 NotificationsView 固定 limit=20 */
    const val PAGE_SIZE = 20

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(NotificationCenterUiState())
    val state: StateFlow<NotificationCenterUiState> = _state.asStateFlow()

    private val api get() = ZeroTalkClientManager.apiService

    /**
     * 并发加载代号：切换分类 / 重新刷新会让仍在途的旧请求响应作废
     */
    private var loadToken = 0L

    /**
     * 打开面板：拉第一页 + 刷新未读数（官方 mount 行为）
     */
    fun open() {
        loadPage(append = false)
        refreshUnread()
    }

    /**
     * 切换分类 Tab（官方切分类会清空列表并重新拉第一页）
     */
    fun selectCategory(category: NotificationCategory) {
        if (_state.value.category == category && _state.value.items.isNotEmpty()) return
        loadToken++
        _state.update {
            it.copy(
                category = category,
                items = emptyList(),
                hasMore = false,
                nextBeforeId = null,
                isLoadingMore = false,
                errorMessage = null
            )
        }
        loadPage(append = false)
    }

    /**
     * 重新拉取当前分类第一页（失败重试 / 下拉刷新）
     */
    fun refresh() {
        loadPage(append = false)
    }

    /**
     * 加载更多（官方「加载更多」按钮）
     */
    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return
        loadPage(append = true)
    }

    /**
     * 仅刷新未读数（供「我的」入口角标使用，不触发列表请求）
     */
    fun refreshUnread() {
        scope.launch {
            api.getUnreadNotificationCount().onSuccess { count ->
                _state.update { it.copy(unreadCount = count.coerceAtLeast(0)) }
            }
        }
    }

    /**
     * 标记单条已读（乐观更新 + 服务端未读数校正）
     */
    fun markRead(id: Long) {
        val target = _state.value.items.firstOrNull { it.id == id } ?: return
        if (target.isRead) return
        _state.update { current ->
            current.copy(
                items = current.items.map { if (it.id == id) it.copy(isRead = true) else it },
                unreadCount = (current.unreadCount - 1).coerceAtLeast(0)
            )
        }
        scope.launch {
            api.markNotificationsRead(listOf(id)).onSuccess { serverUnread ->
                if (serverUnread != null) {
                    _state.update { it.copy(unreadCount = serverUnread) }
                }
            }
        }
    }

    /**
     * 全部标为已读（乐观更新 + 失败回滚未读数）
     */
    fun markAllRead() {
        val current = _state.value
        if (current.unreadCount <= 0 || current.isMarkingAllRead) return
        _state.update {
            it.copy(
                isMarkingAllRead = true,
                items = it.items.map { item -> item.copy(isRead = true) },
                unreadCount = 0
            )
        }
        scope.launch {
            api.markAllNotificationsRead()
                .onSuccess { serverUnread ->
                    _state.update { it.copy(isMarkingAllRead = false, unreadCount = serverUnread ?: 0) }
                }
                .onFailure {
                    _state.update { it.copy(isMarkingAllRead = false) }
                    // 失败时以服务端为准回滚未读数
                    refreshUnread()
                }
        }
    }

    /**
     * 退出登录时清空，避免串号
     */
    fun clear() {
        loadToken++
        _state.value = NotificationCenterUiState()
    }

    private fun loadPage(append: Boolean) {
        val token = if (append) loadToken else ++loadToken
        val category = _state.value.category
        val beforeId = if (append) _state.value.nextBeforeId else null

        _state.update { it.copy(isLoading = !append, isLoadingMore = append, errorMessage = null) }

        scope.launch {
            val result = api.getNotifications(
                beforeId = beforeId,
                limit = PAGE_SIZE,
                category = category.key
            )

            // 更晚发起的首屏请求已接管：本次响应作废（不碰任何加载态，避免覆盖新请求）
            if (!append && token != loadToken) return@launch
            // 分类已切换：丢弃本次响应（新分类的首屏请求会自行维护加载态）
            if (_state.value.category != category) return@launch

            result.onSuccess { data ->
                _state.update { current ->
                    current.copy(
                        items = if (append) mergeItems(current.items, data.list) else data.list,
                        hasMore = data.hasMore,
                        nextBeforeId = data.nextBeforeId,
                        unreadCount = data.unread.coerceAtLeast(0),
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = error.message ?: "加载失败"
                    )
                }
            }
        }
    }

    /**
     * 合并分页结果：按 id 去重，避免服务端边界重复返回
     */
    private fun mergeItems(
        old: List<NotificationItemDto>,
        new: List<NotificationItemDto>
    ): List<NotificationItemDto> {
        if (new.isEmpty()) return old
        val seen = old.mapTo(HashSet()) { it.id }
        return old + new.filter { it.id !in seen }
    }
}
