package top.lanxint.zerotalk.data.notify

/**
 * 系统通知桥（Android 通知栏）
 *
 * 约定：App 在前台时新消息只走应用内悬浮提示；退到后台后由本桥投递系统通知，
 * 保证 App 不在前台（甚至被切走）时也能看到新消息。
 */
expect object SystemNotificationBridge {
    /** App 是否处于前台（由平台侧 Activity 生命周期维护） */
    val isAppInForeground: Boolean

    /**
     * 投递一条新消息系统通知
     *
     * @param roomId 会话房间 id，用于点击通知后直达该会话（可为空）
     */
    fun postMessage(title: String, body: String, roomId: String)

    /** 系统通知是否已授权（Android 13+ 为用户授权；低版本恒为 true） */
    val isNotificationEnabled: Boolean

    /** 跳转到系统的「应用通知设置」页，便于用户手动开启 */
    fun openNotificationSettings()
}
