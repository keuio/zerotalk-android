package top.lanxint.zerotalk.data.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 房间公告关闭状态的持久化及变更判定回归测试。
 */
class DismissedRoomAnnouncementPreferenceTest {

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "zt_announcement_pref_${System.nanoTime()}").apply { mkdirs() }

    @Test
    fun `已关闭公告记录持久化落盘并在重新加载后依然生效`() {
        val dir = tempDir()
        try {
            UiPreferencesStore.init(dir)

            val initial = UiPreferencesStore.getDismissedRoomAnnouncements()
            assertTrue(initial.isEmpty())

            UiPreferencesStore.setDismissedRoomAnnouncement("room_123", "群规第一条：文明发言")
            assertEquals("群规第一条：文明发言", UiPreferencesStore.getDismissedRoomAnnouncements()["room_123"])

            // 模拟冷启动重新初始化
            UiPreferencesStore.init(dir)
            assertEquals("群规第一条：文明发言", UiPreferencesStore.getDismissedRoomAnnouncements()["room_123"])

            // 清除关闭记录（如群主清空公告）
            UiPreferencesStore.clearDismissedRoomAnnouncement("room_123")
            assertTrue(UiPreferencesStore.getDismissedRoomAnnouncements()["room_123"].isNullOrEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }
}
