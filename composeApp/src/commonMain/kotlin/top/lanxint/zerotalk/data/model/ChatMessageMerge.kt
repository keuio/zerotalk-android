package top.lanxint.zerotalk.data.model

/**
 * 聊天消息列表的统一合并（逐条对齐官方 `game-*.js` 的 `jn` / `Yn` 与排序键 `gt`）。
 *
 * 官方源码：
 * ```js
 * function jn(e){ const t=new Map;
 *   e.forEach(a=>{ const s=Mn(Cn({...a})), u=s.id!=null&&s.id>0?`id:${s.id}`:`uuid:${s._uuid}`;
 *     t.set(u, In(t.get(u), s)) });
 *   return Array.from(t.values()).sort(Tn) }
 * function Yn(e,t){ return jn([...e,...t]) }
 * function gt(e){ if(e.id!=null&&e.id>0) return e.id;
 *   const t=new Date((e.created_at||"").replace(/-/g,"/")).getTime(); return Number.isNaN(t)?0:t }
 * function Tn(e,t){ const a=gt(e)-gt(t); return a!==0?a:(e._uuid||"").localeCompare(t._uuid||"") }
 * ```
 *
 * 要点：
 * - **去重键**：服务端 id > 0 用 `id`；本地乐观回显（无服务端 id）用本地唯一 id，
 *   **绝不能被历史快照误删**；
 * - **排序键**：服务端 id 优先，本地回显用到达时间（必然排在服务端消息之后，符合「刚发出在底部」）；
 * - **撤回是终态**：任一侧 `isDeleted` 即保持撤回，避免旧分页 / 旧快照把已撤回消息复活。
 *
 * 抽成纯函数是为了可单测：合并顺序 / 去重一旦判错，就会出现「中间一段消息被忽略」或
 * 「列表顺序颠倒（较新的排在较旧的上面）」。
 */
internal object ChatMessageMerge {

    /** 去重键：serverId > 0 用服务端 id；本地乐观回显用本地唯一 id */
    fun dedupeKey(message: ChatMessage): String =
        if (message.serverId > 0L) "id:${message.serverId}" else "local:${message.id}"

    /** 排序键（对齐官方 `gt`）：服务端 id 优先，本地回显用到达时间 */
    fun sortKey(message: ChatMessage): Long =
        if (message.serverId > 0L) message.serverId else message.timestampMs

    private val ORDER: Comparator<ChatMessage> =
        compareBy<ChatMessage> { sortKey(it) }.thenBy { it.id }

    /**
     * 合并两组消息：按 [dedupeKey] 去重、按 [sortKey] 升序排序。
     *
     * 同一 key 时：
     * - 任一侧已撤回 → 采用已撤回副本（正文已清空），撤回不会被复活；
     * - 否则保留已有副本，避免历史快照把实时富字段（游戏卡片 / 引用等）冲掉。
     */
    fun merge(existing: List<ChatMessage>, incoming: List<ChatMessage>): List<ChatMessage> {
        if (incoming.isEmpty()) return existing
        if (existing.isEmpty()) return incoming.sortedWith(ORDER)
        val map = LinkedHashMap<String, ChatMessage>(existing.size + incoming.size)
        existing.forEach { map[dedupeKey(it)] = it }
        incoming.forEach { message ->
            val key = dedupeKey(message)
            val old = map[key]
            map[key] = if (old == null) message else mergeDuplicate(old, message)
        }
        return map.values.sortedWith(ORDER)
    }

    private fun mergeDuplicate(existing: ChatMessage, incoming: ChatMessage): ChatMessage = when {
        existing.isDeleted -> existing
        incoming.isDeleted -> incoming
        else -> existing
    }

    /**
     * 追加一条服务端消息：列表里已有同 serverId 的一条时替换掉旧的，保证只留一条。
     *
     * 补页（bootstrap / loadOlderMessages / catchUpNewerMessages）可能已插入同一条，
     * 随后 WS `message` 推送再到达；若直接 `+` 就会出现两条同样的消息。
     * 本地乐观回显 serverId = 0，不参与去重（由 reconcilePendingEcho 单独对账）。
     */
    fun appendDeduped(list: List<ChatMessage>, message: ChatMessage): List<ChatMessage> {
        if (message.serverId <= 0L) return list + message
        val withoutDuplicate = list.filterNot { it.serverId == message.serverId }
        return if (withoutDuplicate.size == list.size) list + message else withoutDuplicate + message
    }

    /** 服务端 id 最大值（对齐官方 `Xn`）：仅统计 id > 0，无则 null */
    fun maxServerId(list: List<ChatMessage>): Long? =
        list.asSequence().map { it.serverId }.filter { it > 0L }.maxOrNull()

    /** 服务端 id 最小值（对齐官方 `Kn`）：仅统计 id > 0，无则 null */
    fun minServerId(list: List<ChatMessage>): Long? =
        list.asSequence().map { it.serverId }.filter { it > 0L }.minOrNull()
}

/**
 * 「补缺口」增量拉取的页循环决策（对齐官方 `te`：最多 5 页、每页 50）。
 *
 * 官方源码：
 * ```js
 * let se = maxId(list), E = 0;
 * for (; E < 5;) { E += 1; const $e = await loadNewer(S, se, Q);
 *   ... if (!De.length) break; list = Js(list, De);
 *   const Fe = Ua(De); if (!Fe || Fe <= se || (se = Fe, !$e.has_more)) break; }
 * ```
 */
internal object ChatNewerCatchUp {
    /** 官方 `te` 的页数上限 */
    const val MAX_PAGES = 5

    /** 官方默认 `historyPageSize` */
    const val PAGE_SIZE = 50

    /**
     * 计算下一页游标（对齐官方 `if (!Fe || Fe <= se || (se = Fe, !$e.has_more)) break;`）。
     *
     * @param cursor 当前游标（本次请求的 `after_id`）
     * @param pageMaxId 本页返回消息的最大服务端 id
     * @param hasMore 本页响应是否还有更多
     * @return 下一页的游标；null 表示停止补页
     */
    fun nextCursor(cursor: Long, pageMaxId: Long?, hasMore: Boolean): Long? {
        if (pageMaxId == null || pageMaxId <= cursor) return null
        if (!hasMore) return null
        return pageMaxId
    }
}
