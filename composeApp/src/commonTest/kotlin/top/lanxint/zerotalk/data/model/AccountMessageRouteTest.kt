package top.lanxint.zerotalk.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 多端场景下服务端消息归属判定的回归测试。
 *
 * 背景：服务端 `is_self` 的语义是「**本账号**发出」，不是「本设备发出」。
 * 双端同时在线（网页端 + App 端）时，网页端发的消息推到 App 端同样带 `is_self=true`，
 * 但 App 端没有对应的本地乐观回显。旧实现把它当作「重复回显」丢弃，
 * 导致消息不显示、不通知、不记未读——即用户反馈的「网页端在线时 App 端消息没有真实发出去」。
 */
class AccountMessageRouteTest {

    @Test
    fun `本账号发出且本机有回显时走核销替换`() {
        assertEquals(
            AccountMessageRoute.RECONCILE_LOCAL_ECHO,
            resolveAccountMessageRoute(isSelf = true, hasPendingLocalEcho = true, isOwnAccount = true)
        )
    }

    @Test
    fun `本账号发出但本机无回显时判定为另一台设备发出（必须入库）`() {
        assertEquals(
            AccountMessageRoute.OWN_OTHER_DEVICE,
            resolveAccountMessageRoute(isSelf = true, hasPendingLocalEcho = false, isOwnAccount = true)
        )
    }

    @Test
    fun `他人消息走常规路径`() {
        assertEquals(
            AccountMessageRoute.OTHER_USER,
            resolveAccountMessageRoute(isSelf = false, hasPendingLocalEcho = false, isOwnAccount = false)
        )
    }

    @Test
    fun `服务端未下发 is_self 时用兜底判定结果`() {
        // 兜底判为本人但无回显 → 另一台设备
        assertEquals(
            AccountMessageRoute.OWN_OTHER_DEVICE,
            resolveAccountMessageRoute(isSelf = null, hasPendingLocalEcho = false, isOwnAccount = true)
        )
        // 兜底判为他人
        assertEquals(
            AccountMessageRoute.OTHER_USER,
            resolveAccountMessageRoute(isSelf = null, hasPendingLocalEcho = false, isOwnAccount = false)
        )
    }

    @Test
    fun `is_self 明确为 false 时忽略兜底判定`() {
        // 服务端说不是本人，即使本地兜底误判为本人，也不能当成自己的消息
        assertEquals(
            AccountMessageRoute.OTHER_USER,
            resolveAccountMessageRoute(isSelf = false, hasPendingLocalEcho = true, isOwnAccount = true)
        )
    }

    // ---- 兜底归属判定 ----

    @Test
    fun `uid 命中本人时判为本账号（大小写不敏感）`() {
        assertTrue(
            isOwnAccountSender(
                fromUid = "AABBCCDD",
                username = "别人",
                myUid = "aabbccdd",
                myUserId = 42L,
                myName = "我"
            )
        )
    }

    @Test
    fun `数字 id 命中本人时判为本账号`() {
        assertTrue(
            isOwnAccountSender(
                fromUid = "42",
                username = null,
                myUid = "aabbccdd",
                myUserId = 42L,
                myName = "我"
            )
        )
    }

    @Test
    fun `有明确发送者标识但不是本人时不再用昵称兜底`() {
        // 同名用户不应被误判成「自己发的」：误判会抑制通知与未读
        assertFalse(
            isOwnAccountSender(
                fromUid = "other-uid",
                username = "我",
                myUid = "aabbccdd",
                myUserId = 42L,
                myName = "我"
            )
        )
    }

    @Test
    fun `发送者标识完全缺失时才用昵称兜底`() {
        assertTrue(
            isOwnAccountSender(
                fromUid = null,
                username = "我",
                myUid = "aabbccdd",
                myUserId = 42L,
                myName = "我"
            )
        )
        assertFalse(
            isOwnAccountSender(
                fromUid = null,
                username = "别人",
                myUid = "aabbccdd",
                myUserId = 42L,
                myName = "我"
            )
        )
    }

    @Test
    fun `空发送者标识与空 uid 都不会误判为本账号`() {
        assertFalse(
            isOwnAccountSender(
                fromUid = "   ",
                username = "别人",
                myUid = "aabbccdd",
                myUserId = 42L,
                myName = "我"
            )
        )
        assertFalse(
            isOwnAccountSender(
                fromUid = null,
                username = null,
                myUid = "",
                myUserId = 0L,
                myName = ""
            )
        )
    }

    @Test
    fun `uid 两侧空白会被裁剪后比较`() {
        assertTrue(
            isOwnAccountSender(
                fromUid = "  aabbccdd  ",
                username = null,
                myUid = " aabbccdd ",
                myUserId = 0L,
                myName = "我"
            )
        )
    }
}
