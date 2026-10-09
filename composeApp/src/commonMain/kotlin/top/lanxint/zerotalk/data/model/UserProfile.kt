package top.lanxint.zerotalk.data.model

import top.lanxint.zerotalk.data.network.AvatarUploadDto

/**
 * 用户个人资料数据模型（预留后续与后端 API 接口对接）
 *
 * @param name 昵称
 * @param avatarUrl 头像链接（后续对接图床或 OSS，当前使用预设/生成式头像）
 * @param gender 性别（"男" / "女"）
 * @param ageRange 年龄区间（严格限定为 "18-23" 和 "23以上"）
 * @param userId 用户唯一数字 ID
 * @param bio 个人签名
 * @param avatarUpload 头像上传限制与状态
 * @param hasCustomAvatar 是否已有自定义头像
 */
data class UserProfile(
    val name: String = "未登录",
    val avatarUrl: String = "",
    val gender: String = "男",
    val ageRange: String = "18-23",
    val userId: String = "--",
    val bio: String = "未登录账号",
    val isLoggedIn: Boolean = false,
    val patText: String = "",
    val loginName: String = "",
    val qq: String = "",
    val avatarUpload: AvatarUploadDto? = null,
    val hasCustomAvatar: Boolean = false,
    val location: String = ""
) {
    companion object {
        val AGE_OPTIONS = listOf("18-23", "23以上")
        val GENDER_OPTIONS = listOf("男", "女")

        val NO_ACCOUNT = UserProfile(
            name = "未登录",
            avatarUrl = "",
            gender = "男",
            ageRange = "18-23",
            userId = "--",
            bio = "未登录账号",
            isLoggedIn = false,
            patText = "",
            loginName = "",
            qq = "",
            avatarUpload = null,
            hasCustomAvatar = false,
            location = ""
        )
    }
}

/**
 * 全局主题模式配置枚举
 */
enum class AppThemeMode(val title: String) {
    SYSTEM("系统"),
    LIGHT("浅色"),
    DARK("深色")
}
