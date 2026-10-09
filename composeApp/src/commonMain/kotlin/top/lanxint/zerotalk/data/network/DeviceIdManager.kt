package top.lanxint.zerotalk.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * 设备标识管理器
 * 负责管理当前设备的固定 UUID (对应协议中的 zt_reg_did 和 X-Device-Id)
 *
 * 核心机制：
 * 1. 并非直接提取手机硬件 IMEI/MAC（现代 Android 系统限制严苛且属于敏感隐私）；
 * 2. 而是设备首次安装使用时随机生成一个标准 UUID，并在本地私有存储持久化；
 * 3. 后续所有请求与应用冷启动均复用该固定 UUID，保证服务端会话（Session/Cookie）绑定有效（防止被踢下线）。
 */
object DeviceIdManager {
    @Volatile
    private var deviceId: String = ""
    private var storageDir: File? = null

    /**
     * 初始化本地持久化存储目录（如 context.filesDir）
     * 采用后台异步预热，避免冷启动在主线程阻塞磁盘 I/O
     */
    fun init(filesDir: File) {
        storageDir = filesDir
        CoroutineScope(Dispatchers.IO).launch {
            loadFromStorage()
        }
    }

    private fun loadFromStorage() {
        val dir = storageDir ?: return
        try {
            val file = File(dir, "zt_reg_did.txt")
            if (file.exists()) {
                val storedId = file.readText().trim()
                if (storedId.isNotBlank()) {
                    deviceId = storedId
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveToStorage(id: String) {
        val dir = storageDir ?: return
        try {
            val file = File(dir, "zt_reg_did.txt")
            file.writeText(id)
        } catch (_: Exception) {}
    }

    /**
     * 获取当前设备的固定设备 ID
     */
    fun getDeviceId(): String {
        if (deviceId.isEmpty()) {
            synchronized(this) {
                if (deviceId.isEmpty()) {
                    loadFromStorage()
                    if (deviceId.isEmpty()) {
                        deviceId = UUID.randomUUID().toString()
                        saveToStorage(deviceId)
                    }
                }
            }
        }
        return deviceId
    }

    /**
     * 手动设置设备 ID（例如测试或迁移）
     */
    fun setDeviceId(id: String) {
        if (id.isNotBlank()) {
            deviceId = id
            saveToStorage(id)
        }
    }
}
