package top.lanxint.zerotalk.data.notify

import top.lanxint.zerotalk.notify.AndroidSystemNotifications

actual object SystemNotificationBridge {
    actual val isAppInForeground: Boolean
        get() = AndroidSystemNotifications.isAppInForeground

    actual fun postMessage(title: String, body: String, roomId: String) {
        AndroidSystemNotifications.postMessage(title, body, roomId)
    }

    actual val isNotificationEnabled: Boolean
        get() = AndroidSystemNotifications.isNotificationEnabled()

    actual fun openNotificationSettings() {
        AndroidSystemNotifications.openNotificationSettings()
    }
}
