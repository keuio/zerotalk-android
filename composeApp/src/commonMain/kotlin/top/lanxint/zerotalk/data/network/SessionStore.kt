package top.lanxint.zerotalk.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.data.security.SECURE_STORE_PREFIX
import top.lanxint.zerotalk.data.security.SecureStore
import java.io.File

/**
 * 本地登录态持久化（双保险）
 *
 * 1. **会话 Cookie**（PHPSID / server_session 等）：冷启动直接复用，[ZeroTalkCookieJar] 每次
 *    收到响应都会自动落盘，因此无需重新登录即可恢复登录态；
 * 2. **最近一次登录成功的账号密码**：服务端会话过期时的静默续登兜底。
 *
 * 存储位置与 [DeviceIdManager] 一致（App 私有 filesDir）。
 * **落盘内容一律经 [SecureStore] 加密**（Android Keystore AES/GCM）：
 * - `zt_session_cookies.txt`    Cookie 快照（JSON）
 * - `zt_session_credential.txt` 账号密码（第一行账号，第二行密码）
 *
 * 历史版本的明文文件会在首次读取时自动原地升级为密文。
 *
 * 注意：调用 [clear] 会同时抹除两者，仅用于「退出登录」与「会话彻底失效」。
 */
object SessionStore {
    private const val COOKIE_FILE = "zt_session_cookies.txt"
    private const val CREDENTIAL_FILE = "zt_session_credential.txt"

    @Volatile
    private var storageDir: File? = null

    /** 最近一次读写的 Cookie 快照，避免每个响应都重复落盘 */
    @Volatile
    private var cachedCookieJson: String? = null

    /**
     * 初始化本地持久化存储目录（如 context.filesDir）
     * 采用后台异步预加载，避免在主线程阻塞磁盘 I/O
     */
    fun init(filesDir: File) {
        storageDir = filesDir
        CoroutineScope(Dispatchers.IO).launch {
            if (cachedCookieJson == null) {
                // 必须走 readSecure：否则会把历史明文直接灌进缓存，loadCookies 命中缓存后
                // 永远不会触发「明文 → 密文」的原地升级
                cachedCookieJson = readSecure(COOKIE_FILE)
            }
        }
    }

    // ---------------- 会话 Cookie ----------------

    /**
     * 写入 Cookie 快照（内容未变化时跳过落盘）
     *
     * 加锁：OkHttp 可能在多个线程同时回调保存响应 Cookie
     */
    @Synchronized
    fun saveCookies(json: String) {
        if (json == cachedCookieJson) return
        cachedCookieJson = json
        writeSecure(COOKIE_FILE, json)
    }

    @Synchronized
    fun loadCookies(): String? {
        cachedCookieJson?.let { return it }
        return readSecure(COOKIE_FILE)?.also { cachedCookieJson = it }
    }

    /**
     * 仅清除会话 Cookie（保留账号密码，用于会话过期后的静默续登）
     */
    @Synchronized
    fun clearCookies() {
        cachedCookieJson = null
        deleteFile(COOKIE_FILE)
    }

    // ---------------- 账号密码 ----------------

    @Synchronized
    fun saveCredential(username: String, password: String) {
        if (username.isBlank()) return
        writeSecure(CREDENTIAL_FILE, "$username\n$password")
    }

    @Synchronized
    fun loadCredential(): Pair<String, String>? {
        val text = readSecure(CREDENTIAL_FILE) ?: return null
        val lines = text.split("\n")
        if (lines.size < 2) return null
        val username = lines[0].trim()
        val password = lines[1]
        if (username.isBlank()) return null
        return username to password
    }

    /**
     * 彻底清除本地登录态（退出登录时调用）
     */
    @Synchronized
    fun clear() {
        clearCookies()
        deleteFile(CREDENTIAL_FILE)
    }

    // ---------------- 文件读写 ----------------

    private fun fileOf(name: String): File? = storageDir?.let { File(it, name) }

    /**
     * 读取敏感文件；历史明文会在读取后**原地升级**为密文（透明迁移）
     */
    private fun readSecure(name: String): String? {
        val raw = readText(name) ?: return null
        if (raw.startsWith(SECURE_STORE_PREFIX)) {
            return SecureStore.decrypt(raw)
        }
        // 历史明文：升级后返回明文内容
        writeSecure(name, raw)
        return raw
    }

    /** 写入敏感文件；加密不可用时退回明文，保证功能不因密钥问题而不可用 */
    private fun writeSecure(name: String, plain: String) {
        writeText(name, SecureStore.encrypt(plain) ?: plain)
    }

    private fun readText(name: String): String? = try {
        fileOf(name)?.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private fun writeText(name: String, content: String) {
        try {
            fileOf(name)?.writeText(content)
        } catch (_: Exception) {
        }
    }

    private fun deleteFile(name: String) {
        try {
            fileOf(name)?.takeIf { it.exists() }?.delete()
        } catch (_: Exception) {
        }
    }
}
