package top.lanxint.zerotalk.data.model

/**
 * 「这条来自服务端的消息，本机应该怎么处理」的判定。
 *
 * ## 为什么需要单独区分「本设备」与「本账号」
 *
 * 服务端下发的 `is_self` 语义是**「这条消息由我这个账号发出」**，而不是
 * 「由我这个设备发出」。两台设备同时在线（网页端 + App 端）时，网页端发的消息
 * 推到 App 端时同样带 `is_self = true`。
 *
 * 旧实现把 `is_self = true` 直接当成「本机乐观回显的核销」，于是走进
 * 「已匹配到本地回显 → 丢弃服务端副本」的分支。但本机**根本没有**这条回显
 * （消息是另一台设备发的），匹配必然失败，消息就被整条丢掉：
 * 不追加、不通知、不记未读。表现就是「网页端在线时，App 端消息没有真实发出去 /
 * 双端不同步」。
 *
 * 现在拆成三条路径：
 * - [AccountMessageRoute.RECONCILE_LOCAL_ECHO]：本账号发出 **且** 本机有待核销回显
 *   → 用服务端版本替换回显（原有的单设备行为，不变）；
 * - [AccountMessageRoute.OWN_OTHER_DEVICE]：本账号发出 **但本机无回显**
 *   → 正常入库，只是标记 `isMine`、不记未读、不弹通知；
 * - [AccountMessageRoute.OTHER_USER]：别人发的 → 常规入库 + 计数 + 通知。
 */
enum class AccountMessageRoute {
    /** 本账号发出，且本机存在对应的待核销乐观回显 */
    RECONCILE_LOCAL_ECHO,

    /** 本账号发出，但本机没有对应回显（另一台设备发出）——必须照常入库 */
    OWN_OTHER_DEVICE,

    /** 其他用户发出 */
    OTHER_USER
}

/**
 * 判定服务端消息的归属路径。
 *
 * @param isSelf 服务端 `is_self`（本**账号**发出）；null 表示服务端未下发
 * @param hasPendingLocalEcho 本机是否存在与该消息匹配的待核销乐观回显
 * @param isOwnAccount 未下发 `is_self` 时，按 uid / 昵称兜底判定的「本账号发出」
 */
fun resolveAccountMessageRoute(
    isSelf: Boolean?,
    hasPendingLocalEcho: Boolean,
    isOwnAccount: Boolean
): AccountMessageRoute {
    val mine = isSelf ?: isOwnAccount
    return when {
        !mine -> AccountMessageRoute.OTHER_USER
        hasPendingLocalEcho -> AccountMessageRoute.RECONCILE_LOCAL_ECHO
        else -> AccountMessageRoute.OWN_OTHER_DEVICE
    }
}

/**
 * 无 `is_self` 时的兜底归属判定（uid 字符串 / 数字 id / 昵称）。
 *
 * 数字 id 与 uid 都参与比较；昵称仅在发送者标识完全缺失时才作为最后手段，
 * 避免同名用户被误判成「自己发的」（自己发的会被抑制通知与未读）。
 */
fun isOwnAccountSender(
    fromUid: String?,
    username: String?,
    myUid: String?,
    myUserId: Long,
    myName: String
): Boolean {
    val uid = fromUid?.trim().orEmpty()
    val myUidTrimmed = myUid?.trim().orEmpty()
    if (uid.isNotEmpty()) {
        if (myUidTrimmed.isNotEmpty() && uid.equals(myUidTrimmed, ignoreCase = true)) return true
        if (myUserId > 0L && uid == myUserId.toString()) return true
        // 有明确发送者标识但都不是本人 → 明确是别人，不再用昵称兜底
        return false
    }
    return myName.isNotBlank() && username?.trim() == myName.trim()
}
