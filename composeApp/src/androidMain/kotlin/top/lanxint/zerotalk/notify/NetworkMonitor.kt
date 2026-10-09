package top.lanxint.zerotalk.notify

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager

/**
 * 默认网络监听
 *
 * 网络恢复时立刻通知管理器重连，避免退避最长要等 30s 才恢复。
 */
object NetworkMonitor {
    private var registered = false

    fun start(context: Context) {
        if (registered) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runCatching { ZeroTalkClientManager.onNetworkAvailable() }
            }
        }
        runCatching {
            cm.registerDefaultNetworkCallback(callback)
            registered = true
        }
    }
}
