package top.lanxint.zerotalk

import android.app.Application
import android.content.pm.ApplicationInfo
import java.io.File
import top.lanxint.zerotalk.data.log.ZtLog
import top.lanxint.zerotalk.data.network.DeviceIdManager
import top.lanxint.zerotalk.data.network.SessionStore
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.data.settings.UiPreferencesStore
import top.lanxint.zerotalk.notify.AndroidSystemNotifications
import top.lanxint.zerotalk.notify.NetworkMonitor

/**
 * 进程级初始化
 *
 * 关键：前台保活服务可能在没有 Activity 的情况下把进程拉活（START_STICKY），
 * 此时也必须能读回本地会话并重建 WebSocket，所以存储与通知渠道放在 Application 里初始化。
 */
class ZeroTalkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 调试日志：debug 包开启，release 包静默（异常类日志走 ZtLog.w 仍会输出）
        ZtLog.enabled = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (ZtLog.enabled) {
            // 部分国产 ROM 禁用 logcat，这里额外落盘，便于 adb run-as 拉取排查
            val logFile = File(filesDir, "zt_log.txt")
            val lock = Any()
            ZtLog.fileSink = { line ->
                synchronized(lock) {
                    runCatching {
                        if (logFile.length() > 512 * 1024) logFile.writeText("")
                        logFile.appendText(line + "\n")
                    }
                }
            }
        }
        DeviceIdManager.init(filesDir)
        SessionStore.init(filesDir)
        UiPreferencesStore.init(filesDir)
        // 会话自定义背景（uid -> 备注 映射不落盘，这里只恢复每会话背景的本地路径映射）
        ZeroTalkClientManager.initConversationWallpapers(filesDir)
        AndroidSystemNotifications.init(this)
        // 网络恢复 → 立即重连（否则要等退避，最长 30s）
        NetworkMonitor.start(this)
    }
}
