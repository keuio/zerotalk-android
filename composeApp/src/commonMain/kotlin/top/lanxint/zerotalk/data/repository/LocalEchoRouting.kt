package top.lanxint.zerotalk.data.repository

/**
 * 本地乐观回显的落库目标判定（纯逻辑，便于单测）。
 *
 * 大厅（公共房）与普通房间的本地回显落在不同的 StateFlow：
 * - 大厅 -> _hallMessages（大厅界面订阅它，且**不是**会话列表里的会话项）；
 * - 普通房间 -> _roomMessages[roomId] + 会话摘要。
 *
 * 判错会直接表现为「自己发的骰子 / 图片 / 游戏邀请在大厅里看不到」，
 * 或「会话列表里凭空多出一个大厅房间行」，因此抽成纯函数并单测。
 *
 * 判定顺序（与 ZeroTalkClientManager.isHallRoom 完全一致）：
 * 1. roomId 为空 -> 不是大厅；
 * 2. roomId 命中当前大厅 room_id -> 是；
 * 3. roomId 命中 bootstrap 下发的公共房间列表 -> 是（兼容大厅 room_id 变动）；
 * 4. 其余 -> 不是。
 *
 * @param roomId 待判定房间
 * @param hallRoomId 当前已知的大厅 room_id（可为 null / 空）
 * @param publicRoomIds bootstrap 下发的公共房间 room_id 集合
 */
fun isHallEchoTarget(
    roomId: String?,
    hallRoomId: String?,
    publicRoomIds: Collection<String>
): Boolean {
    val id = roomId?.trim().orEmpty()
    if (id.isEmpty()) return false
    if (id == hallRoomId?.trim()) return true
    return publicRoomIds.any { it.trim() == id }
}
