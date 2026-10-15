package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.data.security.SECURE_STORE_PREFIX
import top.lanxint.zerotalk.data.security.SecureStore
import java.io.File

/**
 * 本地已登录过的账号（用于「切换账号」）
 *
 * 只保存切换所需的最小信息：登录名 + 密码（加密落盘）+ 展示用的昵称/头像。
 */
data class SavedAccount(
    @SerializedName("loginName") val loginName: String = "",
    @SerializedName("password") val password: String = "",
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("avatarUrl") val avatarUrl: String = "",
    @SerializedName("userId") val userId: String = "",
    @SerializedName("lastUsedAt") val lastUsedAt: Long = 0L
) {
    /** 面板展示名：昵称优先，缺失回落登录名 */
    val displayName: String get() = nickname.ifBlank { loginName }
}

/**
 * 多账号本地存储（「我的 → 切换账号」的数据源）
 *
 * - 文件：`zt_accounts.txt`，内容为 JSON，**经 [SecureStore] 加密**（与 [SessionStore] 同一套密钥）；
 * - 与 [SessionStore] 的分工：[SessionStore] 只负责「当前登录态」（会话 Cookie + 当前账号密码，供冷启动续登），
 *   本对象负责「账号列表」，退出登录**不会**清空它，否则就没法一键切回去了；
 * - 账号列表里的密码同样只在本地加密保存，删除某个账号即彻底抹掉其密码。
 */
object AccountStore {
    private const val ACCOUNT_FILE = "zt_accounts.txt"

    private val gson = Gson()

    @Volatile
    private var storageDir: File? = null

    /** 落盘模型（含当前选中的账号） */
    private data class Persisted(
        @SerializedName("active") val active: String? = null,
        @SerializedName("accounts") val accounts: List<SavedAccount> = emptyList()
    )

    private val _accounts = MutableStateFlow<List<SavedAccount>>(emptyList())
    val accounts: StateFlow<List<SavedAccount>> = _accounts

    private val _activeLoginName = MutableStateFlow<String?>(null)
    val activeLoginName: StateFlow<String?> = _activeLoginName

    /** 是否已经从磁盘读过一次（避免每次查询都读盘） */
    @Volatile
    private var loaded = false

    /**
     * 初始化存储目录（如 context.filesDir）
     *
     * 与 [SessionStore] 一致采用后台异步预加载，避免在主线程阻塞磁盘 I/O 与 Keystore 解密。
     */
    /**
     * 后台预加载专用 scope。
     *
     * 原实现每次 init() 都新建 `CoroutineScope(Dispatchers.IO)`：既重复创建、
     * 又无法取消，且没有任何异常处理 —— ensureLoaded() 抛出（例如 Keystore
     * 解密失败）会作为未捕获异常直接崩溃应用。改为单例 scope + SupervisorJob，
     * 并把预加载失败限制在内部，不影响启动。
     */
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(filesDir: File) {
        storageDir = filesDir
        preloadScope.launch {
            runCatching { ensureLoaded() }
        }
    }

    /** 确保账号列表已从磁盘读入内存（首次访问时同步补读一次） */
    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val raw = readSecure() ?: return
        val persisted = runCatching { gson.fromJson(raw, Persisted::class.java) }.getOrNull() ?: return
        _accounts.value = persisted.accounts.filter { it.loginName.isNotBlank() }
        _activeLoginName.value = persisted.active?.takeIf { name -> _accounts.value.any { it.loginName == name } }
    }

    fun find(loginName: String): SavedAccount? {
        ensureLoaded()
        return _accounts.value.firstOrNull { it.loginName == loginName }
    }

    fun active(): SavedAccount? {
        ensureLoaded()
        return _activeLoginName.value?.let { name -> _accounts.value.firstOrNull { it.loginName == name } }
    }

    /**
     * 新增 / 更新一个账号，并把它置为当前账号
     *
     * @param password 传入空串表示「只更新展示信息，保留原密码」
     */
    @Synchronized
    fun save(
        loginName: String,
        password: String,
        nickname: String = "",
        avatarUrl: String = "",
        userId: String = ""
    ) {
        val name = loginName.trim()
        if (name.isBlank()) return
        ensureLoaded()
        val existing = _accounts.value.firstOrNull { it.loginName == name }
        val merged = SavedAccount(
            loginName = name,
            password = password.ifBlank { existing?.password.orEmpty() },
            nickname = nickname.ifBlank { existing?.nickname.orEmpty() },
            avatarUrl = avatarUrl.ifBlank { existing?.avatarUrl.orEmpty() },
            userId = userId.ifBlank { existing?.userId.orEmpty() },
            lastUsedAt = System.currentTimeMillis()
        )
        // 最近使用的排最前，面板里一眼能看到刚用过的账号
        _accounts.value = (listOf(merged) + _accounts.value.filter { it.loginName != name })
        _activeLoginName.value = name
        persist()
    }

    /** 只更新展示信息（昵称 / 头像 / 数字 ID），账号不存在时忽略 */
    @Synchronized
    fun updateProfile(loginName: String, nickname: String, avatarUrl: String, userId: String) {
        val name = loginName.trim()
        if (name.isBlank()) return
        ensureLoaded()
        val existing = _accounts.value.firstOrNull { it.loginName == name } ?: return
        val updated = existing.copy(
            nickname = nickname.ifBlank { existing.nickname },
            avatarUrl = avatarUrl.ifBlank { existing.avatarUrl },
            userId = userId.ifBlank { existing.userId }
        )
        if (updated == existing) return
        _accounts.value = _accounts.value.map { if (it.loginName == name) updated else it }
        persist()
    }

    @Synchronized
    fun setActive(loginName: String?) {
        ensureLoaded()
        val name = loginName?.trim()?.takeIf { it.isNotBlank() }
        _activeLoginName.value = name?.takeIf { target -> _accounts.value.any { it.loginName == target } }
        persist()
    }

    /** 删除一个账号（连同本地保存的密码一起抹掉） */
    @Synchronized
    fun remove(loginName: String) {
        ensureLoaded()
        val name = loginName.trim()
        if (name.isBlank()) return
        _accounts.value = _accounts.value.filter { it.loginName != name }
        if (_activeLoginName.value == name) _activeLoginName.value = null
        persist()
    }

    /** 仅测试使用：重置内存状态 */
    fun resetForTest() {
        _accounts.value = emptyList()
        _activeLoginName.value = null
        loaded = false
    }

    // ---------------- 落盘 ----------------

    private fun persist() {
        val json = gson.toJson(Persisted(active = _activeLoginName.value, accounts = _accounts.value))
        writeSecure(json)
    }

    private fun fileOf(): File? = storageDir?.let { File(it, ACCOUNT_FILE) }

    /** 读取账号文件；历史明文会在读取后原地升级为密文（与 SessionStore 同策略） */
    private fun readSecure(): String? {
        val raw = try {
            fileOf()?.takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } ?: return null
        if (raw.startsWith(SECURE_STORE_PREFIX)) return SecureStore.decrypt(raw)
        // 历史明文：升级后返回明文内容
        writeSecure(raw)
        return raw
    }

    /** 加密不可用时退回明文，保证功能不因密钥问题而完全不可用 */
    private fun writeSecure(plain: String) {
        try {
            fileOf()?.writeText(SecureStore.encrypt(plain) ?: plain)
        } catch (_: Exception) {
        }
    }
}
