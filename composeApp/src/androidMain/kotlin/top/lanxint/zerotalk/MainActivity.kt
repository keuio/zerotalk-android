package top.lanxint.zerotalk

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.data.settings.UiPreferencesStore
import top.lanxint.zerotalk.data.voice.VoiceEngineHolder
import top.lanxint.zerotalk.notify.AndroidSystemNotifications
import top.lanxint.zerotalk.ui.ZeroTalkApp
import top.lanxint.zerotalk.ui.components.NetworkImageLoader
import top.lanxint.zerotalk.ui.utils.applyLiquidGlassCompatMode

class MainActivity : ComponentActivity() {

    /** 语音通话录音权限（引擎按需触发，结果回填给 VoiceEngineHolder） */
    private val recordAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        VoiceEngineHolder.onPermissionResult(granted)
    }

    /** 系统通知权限（Android 13+）：未授权只影响通知栏，不影响应用内提示与消息接收 */
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 结果无需处理，未授权时静默降级为仅应用内提示 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 设备 ID / 会话存储 / 通知渠道已在 ZeroTalkApplication 中完成进程级初始化
        // 本地界面偏好（设备级）：冷启动即恢复「兼容渲染模式」，避免首帧先渲染高性能特效再降级
        applyLiquidGlassCompatMode(UiPreferencesStore.liquidGlassCompatMode)
        // 初始化网络图片磁盘二级缓存 (100MB)，极大提升二次加载秒开速度并减少网络流量
        NetworkImageLoader.init(cacheDir)
        // 语音通话引擎：注入 Context 与录音权限启动器
        VoiceEngineHolder.init(this) {
            recordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        // 系统通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !AndroidSystemNotifications.hasNotificationPermission()
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // 冷启动若由通知点击拉起，直接请求进入对应会话
        handleNotificationIntent(intent)
        // 保活条件：已登录 且 用户开启了「后台接收消息」。
        // 关闭后立即停掉前台服务（常驻通知随之消失），代价是退到后台一段时间后收不到消息。
        lifecycleScope.launch {
            combine(
                ZeroTalkClientManager.loginData,
                UiPreferencesStore.backgroundReceiveEnabled
            ) { data, backgroundReceive -> data != null && backgroundReceive }
                .collect { shouldKeepAlive ->
                    if (shouldKeepAlive) {
                        AndroidSystemNotifications.startKeepAlive(this@MainActivity)
                    } else {
                        AndroidSystemNotifications.stopKeepAlive(this@MainActivity)
                    }
                }
        }
        setContent {
            ZeroTalkApp()
        }
    }

    override fun onStart() {
        super.onStart()
        AndroidSystemNotifications.onActivityStarted()
    }

    override fun onStop() {
        AndroidSystemNotifications.onActivityStopped()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    /** 系统通知点击：请求直达对应会话（由 UI 消费后打开） */
    private fun handleNotificationIntent(intent: Intent?) {
        val roomId = intent
            ?.getStringExtra(AndroidSystemNotifications.EXTRA_ROOM_ID)
            ?.takeIf { it.isNotBlank() }
            ?: return
        ZeroTalkClientManager.requestOpenRoom(roomId)
    }
}
