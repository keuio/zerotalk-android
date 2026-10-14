package top.lanxint.zerotalk.data.network

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 多账号列表（「切换账号」数据源）的内存行为回归测试。
 *
 * 不落盘（未 init 目录时 writeSecure 自动跳过），只验证列表维护逻辑：
 * 最近使用排最前、密码空串保留旧值、展示信息可单独更新、删除即彻底移除。
 */
class AccountStoreTest {

    @AfterTest
    fun tearDown() {
        AccountStore.resetForTest()
    }

    @Test
    fun `保存账号后可按登录名取回并置为当前`() {
        AccountStore.save(loginName = "kelo", password = "p1", nickname = "可洛", userId = "1")
        val account = AccountStore.find("kelo")
        assertEquals("p1", account?.password)
        assertEquals("可洛", account?.displayName)
        assertEquals("kelo", AccountStore.activeLoginName.value)
    }

    @Test
    fun `最近使用的账号排最前`() {
        AccountStore.save(loginName = "a", password = "pa")
        AccountStore.save(loginName = "b", password = "pb")
        assertEquals(listOf("b", "a"), AccountStore.accounts.value.map { it.loginName })
    }

    @Test
    fun `密码传空串时保留原密码`() {
        AccountStore.save(loginName = "a", password = "pa")
        AccountStore.save(loginName = "a", password = "", nickname = "新昵称")
        val account = AccountStore.find("a")
        assertEquals("pa", account?.password)
        assertEquals("新昵称", account?.nickname)
    }

    @Test
    fun `展示信息可单独更新且未知账号被忽略`() {
        AccountStore.save(loginName = "a", password = "pa", nickname = "旧")
        AccountStore.updateProfile("a", nickname = "新", avatarUrl = "https://oss/a.png", userId = "9")
        val account = AccountStore.find("a")
        assertEquals("新", account?.nickname)
        assertEquals("https://oss/a.png", account?.avatarUrl)
        assertEquals("9", account?.userId)
        AccountStore.updateProfile("不存在", nickname = "x", avatarUrl = "y", userId = "z")
        assertEquals(1, AccountStore.accounts.value.size)
    }

    @Test
    fun `删除账号后当前选中被清空`() {
        AccountStore.save(loginName = "a", password = "pa")
        AccountStore.remove("a")
        assertNull(AccountStore.find("a"))
        assertNull(AccountStore.activeLoginName.value)
    }

    @Test
    fun `切换选中不存在的账号时忽略`() {
        AccountStore.save(loginName = "a", password = "pa")
        AccountStore.setActive("不存在")
        assertNull(AccountStore.activeLoginName.value)
    }
}
