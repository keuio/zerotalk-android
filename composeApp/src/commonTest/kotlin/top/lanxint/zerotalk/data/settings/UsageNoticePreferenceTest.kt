package top.lanxint.zerotalk.data.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 「首次启动使用说明」一次性标记的落盘回归测试。
 *
 * 关键点：默认必须是**未确认**（否则老用户升级后永远看不到声明），
 * 确认后必须落盘且重新 init 仍为已确认（否则每次启动都会重复弹说明）。
 */
class UsageNoticePreferenceTest {

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "zt_usage_notice_${System.nanoTime()}").apply { mkdirs() }

    @Test
    fun `默认未确认_确认后落盘且重新初始化仍为已确认`() {
        val dir = tempDir()
        try {
            UiPreferencesStore.init(dir)
            assertFalse(UiPreferencesStore.usageNoticeAccepted.value, "首次启动应为未确认")

            UiPreferencesStore.setUsageNoticeAccepted()
            assertTrue(UiPreferencesStore.usageNoticeAccepted.value)

            // 模拟下次冷启动：重新 init 后仍应保持已确认
            UiPreferencesStore.init(dir)
            assertTrue(UiPreferencesStore.usageNoticeAccepted.value, "确认状态必须落盘持久化")
        } finally {
            dir.deleteRecursively()
        }
    }
}
