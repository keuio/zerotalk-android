package top.lanxint.zerotalk.notify

import android.app.Service
import android.content.Intent
import android.os.IBinder
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager

/**
 * 后台保活前台服务
 *
 * 唯一职责：把进程留在后台，让 [top.lanxint.zerotalk.data.network.ZeroTalkWebSocketClient]
 * 的 WebSocket 持续存活以接收消息。本服务不发起任何网络请求。
 *
 * 注意：Android 12+ 只允许从「前台」启动前台服务；这里在登录 / 冷启动恢复会话时启动。
 */
class ZeroTalkKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须在 5s 内调用，否则系统会抛 ANR / 直接杀进程
        runCatching {
            startForeground(
                AndroidSystemNotifications.SERVICE_NOTIFICATION_ID,
                AndroidSystemNotifications.buildServiceNotification(this)
            )
        }
        // 进程被系统按 START_STICKY 拉活时（没有 Activity），主动恢复会话并重建 WebSocket，
        // 否则保活只留住了空进程，消息依旧收不到。restoreSession 内部有幂等保护。
        runCatching { ZeroTalkClientManager.restoreSession() }
        return START_STICKY
    }
}
