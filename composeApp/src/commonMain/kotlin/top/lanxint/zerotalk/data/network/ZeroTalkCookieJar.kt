package top.lanxint.zerotalk.data.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * 零语专属 CookieJar 实现
 * 负责在 OkHttp HTTP 交互与 WebSocket 握手期间自动保存与注入 Cookie（包括 PHPSID 与 server_session）
 *
 * 同时把会话 Cookie 落盘到 [SessionStore]，使 App 冷启动后无需重新登录即可保持登录态。
 */
class ZeroTalkCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, MutableMap<String, Cookie>>()
    private val gson = Gson()

    /**
     * Cookie 持久化快照
     */
    private data class CookieSnapshot(
        @SerializedName("host") val host: String = "",
        @SerializedName("name") val name: String = "",
        @SerializedName("value") val value: String = "",
        @SerializedName("expiresAt") val expiresAt: Long = 0,
        @SerializedName("domain") val domain: String = "",
        @SerializedName("path") val path: String = "/",
        @SerializedName("hostOnly") val hostOnly: Boolean = false,
        @SerializedName("secure") val secure: Boolean = false,
        @SerializedName("httpOnly") val httpOnly: Boolean = false
    )

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val hostCookies = cookieStore.getOrPut(host) { ConcurrentHashMap() }
        for (cookie in cookies) {
            hostCookies[cookie.name] = cookie
        }
        // 会话 Cookie 落盘：保证下次冷启动直接复用登录态
        SessionStore.saveCookies(exportToJson())
    }

    /**
     * 导出当前全部 Cookie 为 JSON（用于本地持久化）
     */
    fun exportToJson(): String {
        val snapshots = mutableListOf<CookieSnapshot>()
        cookieStore.forEach { (host, cookies) ->
            cookies.values.forEach { cookie ->
                snapshots += CookieSnapshot(
                    host = host,
                    name = cookie.name,
                    value = cookie.value,
                    expiresAt = cookie.expiresAt,
                    domain = cookie.domain,
                    path = cookie.path,
                    hostOnly = cookie.hostOnly,
                    secure = cookie.secure,
                    httpOnly = cookie.httpOnly
                )
            }
        }
        return gson.toJson(snapshots)
    }

    /**
     * 从本地持久化内容恢复 Cookie（自动丢弃已过期与非法项）
     */
    fun importFromJson(json: String) {
        if (json.isBlank()) return
        val snapshots = try {
            gson.fromJson(json, Array<CookieSnapshot>::class.java)
        } catch (_: Exception) {
            null
        } ?: return

        val now = System.currentTimeMillis()
        snapshots.forEach { snapshot ->
            if (snapshot.name.isBlank() || snapshot.domain.isBlank() || snapshot.expiresAt < now) return@forEach
            try {
                val builder = Cookie.Builder()
                    .name(snapshot.name)
                    .value(snapshot.value)
                    .path(snapshot.path.ifBlank { "/" })
                    .expiresAt(snapshot.expiresAt)
                if (snapshot.hostOnly) builder.hostOnlyDomain(snapshot.domain) else builder.domain(snapshot.domain)
                if (snapshot.secure) builder.secure()
                if (snapshot.httpOnly) builder.httpOnly()

                cookieStore.getOrPut(snapshot.host) { ConcurrentHashMap() }[snapshot.name] = builder.build()
            } catch (_: Exception) {
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val result = mutableListOf<Cookie>()
        val currentTime = System.currentTimeMillis()

        // 匹配当前 host 以及父域名
        cookieStore.forEach { (storedHost, cookies) ->
            if (host == storedHost || host.endsWith(".$storedHost") || storedHost.endsWith(".$host")) {
                val iterator = cookies.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val cookie = entry.value
                    if (cookie.expiresAt < currentTime) {
                        iterator.remove() // 清理过期 Cookie
                    } else if (cookie.matches(url)) {
                        result.add(cookie)
                    }
                }
            }
        }

        // 确保注入零语前端协议必需的设备标识 Cookie (zt_reg_did)
        if (result.none { it.name == "zt_reg_did" }) {
            try {
                val did = DeviceIdManager.getDeviceId()
                if (did.isNotBlank()) {
                    val didCookie = Cookie.Builder()
                        .domain(url.host)
                        .path("/")
                        .name("zt_reg_did")
                        .value(did)
                        .build()
                    result.add(didCookie)
                }
            } catch (_: Exception) {}
        }

        return result
    }

    /**
     * 获取指定名称的 Cookie 字符串值（例如 PHPSID 或 server_session）
     */
    fun getCookieValue(name: String): String? {
        cookieStore.values.forEach { map ->
            map.entries.forEach { (key, cookie) ->
                if (key.equals(name, ignoreCase = true) || key.startsWith(name, ignoreCase = true)) {
                    return cookie.value
                }
            }
        }
        return null
    }

    /**
     * 获取指定 URL 的 Cookie 请求头拼接字符串（格式：name1=val1; name2=val2）
     */
    fun getCookieHeaderString(url: HttpUrl): String {
        val cookies = loadForRequest(url)
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    /**
     * 清空存储的 Cookie
     */
    fun clear() {
        cookieStore.clear()
    }
}
