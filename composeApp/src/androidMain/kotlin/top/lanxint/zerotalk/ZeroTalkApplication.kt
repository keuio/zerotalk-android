package top.lanxint.zerotalk

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.RandomAccessFile
import top.lanxint.zerotalk.data.log.AppDiagnostics
import top.lanxint.zerotalk.data.log.DeviceEnvironment
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
        // 调试日志：debug 包开启详细日志（ZtLog.d）；release 包仅保留 ZtLog.w 告警，
        // 避免 release 包关键问题完全无痕。
        ZtLog.enabled = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        // 日志落盘 + 读取（同一个 zt_log.txt，共用一把锁）。
        //
        // 落盘：部分国产 ROM 禁用 logcat，这里额外写私有目录，便于 adb run-as 拉取排查。
        // 该实现不再限定 debug 包 —— 「一键发送诊断信息到 App端群」是面向真实用户的反馈入口，
        // release 包同样需要可读日志；release 下 ZtLog.d 静默，只有 ZtLog.w 会落盘，
        // 配合下方 512KB 轮转，开销可忽略。
        //
        // 读取：供 AppDiagnostics 取日志尾部，只读文件末尾 256KB，避免整文件读入内存。
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
        ZtLog.logReader = {
            runCatching {
                synchronized(lock) {
                    if (!logFile.exists()) {
                        null
                    } else {
                        val maxBytes = 256 * 1024
                        val length = logFile.length()
                        val start = (length - maxBytes).coerceAtLeast(0L)
                        val bytes = ByteArray((length - start).toInt())
                        RandomAccessFile(logFile, "r").use { raf ->
                            raf.seek(start)
                            raf.readFully(bytes)
                        }
                        val text = String(bytes, Charsets.UTF_8)
                        // 从任意字节位置起读可能截断首行（多字节字符也可能残缺），丢弃残缺首行
                        if (start > 0) text.substringAfter('\n', "") else text
                    }
                }
            }.getOrNull()
        }

        // 「一键诊断」头部所需的环境信息（App 版本名/版本号、Android 版本、机型厂商+型号）
        AppDiagnostics.environment = runCatching {
            val pkg = packageManager.getPackageInfo(packageName, 0)
            DeviceEnvironment(
                appVersionName = pkg.versionName ?: "未知",
                appVersionCode = PackageInfoCompat.getLongVersionCode(pkg).toString(),
                androidVersion = "Android ${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）",
                deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
                    .filter { !it.isNullOrBlank() }
                    .joinToString(" ")
                    .ifBlank { "未知" }
            )
        }.getOrNull()

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
