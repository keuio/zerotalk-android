package top.lanxint.zerotalk.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import top.lanxint.zerotalk.MainActivity

/**
 * Android 系统通知与后台保活入口
 *
 * - 新消息：App 退到后台后投递通知栏通知，点击直达对应会话；
 * - 保活：[ZeroTalkKeepAliveService] 前台服务把进程留在后台，WebSocket 才能持续收消息。
 */
object AndroidSystemNotifications {

    /** 通知点击时携带的会话 id */
    const val EXTRA_ROOM_ID = "extra_room_id"

    private const val CHANNEL_MESSAGES = "zt_messages"
    private const val CHANNEL_SERVICE = "zt_service"

    /** 前台保活常驻通知 id */
    const val SERVICE_NOTIFICATION_ID = 0x2A01

    /** 消息通知分组 + 摘要 id（多个会话折叠成一组） */
    private const val GROUP_MESSAGES = "zt_messages_group"
    private const val SUMMARY_NOTIFICATION_ID = 0x2A00

    /** 当前挂在通知栏里的消息会话（回到前台时按 id 清掉） */
    private val activeRoomIds = LinkedHashSet<String>()

    private var appContext: Context? = null

    /** 已 start 未 stop 的 Activity 数量（前台判定） */
    private var startedActivities = 0

    /** App 是否处于前台：前台时消息只走应用内提示，不打扰通知栏 */
    @Volatile
    var isAppInForeground: Boolean = false
        private set

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        createChannels(ctx)
    }

    fun onActivityStarted() {
        startedActivities++
        isAppInForeground = true
        // 回到前台：用户已经能看到消息了，清掉通知栏里的消息提醒（保留前台服务常驻通知）
        clearMessageNotifications()
    }

    /** 清掉所有消息通知；前台服务的常驻通知不受影响 */
    fun clearMessageNotifications() {
        val ctx = appContext ?: return
        val manager = NotificationManagerCompat.from(ctx)
        synchronized(activeRoomIds) {
            activeRoomIds.forEach { roomId -> runCatching { manager.cancel(roomId.hashCode()) } }
            activeRoomIds.clear()
        }
        runCatching { manager.cancel(SUMMARY_NOTIFICATION_ID) }
    }

    fun onActivityStopped() {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        isAppInForeground = startedActivities > 0
    }

    /** 系统通知是否已开启（含用户手动关闭通知的情况） */
    fun isNotificationEnabled(): Boolean {
        val ctx = appContext ?: return false
        return runCatching { NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
            .getOrDefault(false)
    }

    /** 跳到系统的「应用通知设置」页 */
    fun openNotificationSettings() {
        val ctx = appContext ?: return
        runCatching {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        }
    }

    /** Android 13+ 需要 POST_NOTIFICATIONS 授权；低版本恒为 true */
    fun hasNotificationPermission(): Boolean {
        val ctx = appContext ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "新消息",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "私聊 / 群聊 / 大厅的新消息提醒" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                "消息服务",
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = "保持后台在线以接收消息" }
        )
    }

    /** 投递一条新消息通知；[roomId] 用于点击后直达会话 */
    @SuppressLint("MissingPermission")
    fun postMessage(title: String, body: String, roomId: String) {
        val ctx = appContext ?: return
        if (!hasNotificationPermission()) return

        val text = body.ifBlank { "你有一条新消息" }
        val pending = PendingIntent.getActivity(
            ctx,
            roomId.hashCode(),
            openAppIntent(ctx, roomId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title.ifBlank { "零语" })
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_MESSAGES)
            .setContentIntent(pending)
            .build()

        val id = if (roomId.isNotBlank()) {
            roomId.hashCode()
        } else {
            (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        }
        val manager = NotificationManagerCompat.from(ctx)
        runCatching { manager.notify(id, notification) }

        // 分组摘要：多个会话折叠成一组，折叠态显示「N 个会话有新消息」
        if (roomId.isNotBlank()) {
            synchronized(activeRoomIds) { activeRoomIds.add(roomId) }
        }
        val roomCount = synchronized(activeRoomIds) { activeRoomIds.size }
        val summary = NotificationCompat.Builder(ctx, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("零语")
            .setContentText(if (roomCount > 1) "$roomCount 个会话有新消息" else text)
            .setGroup(GROUP_MESSAGES)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()
        runCatching { manager.notify(SUMMARY_NOTIFICATION_ID, summary) }
    }

    /** 开启后台保活（登录后调用） */
    fun startKeepAlive(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        val intent = Intent(ctx, ZeroTalkKeepAliveService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(intent)
            else ctx.startService(intent)
        }
    }

    /** 关闭后台保活（退出登录 / 进程收尾时调用） */
    fun stopKeepAlive(context: Context) {
        val ctx = context.applicationContext
        runCatching { ctx.stopService(Intent(ctx, ZeroTalkKeepAliveService::class.java)) }
    }

    internal fun buildServiceNotification(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("零语正在后台接收消息")
            .setContentText("点按返回应用")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(
                PendingIntent.getActivity(
                    ctx,
                    0,
                    openAppIntent(ctx, ""),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun openAppIntent(ctx: Context, roomId: String): Intent =
        Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (roomId.isNotBlank()) putExtra(EXTRA_ROOM_ID, roomId)
        }
}
