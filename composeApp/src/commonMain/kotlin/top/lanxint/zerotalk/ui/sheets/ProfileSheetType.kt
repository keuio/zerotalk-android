package top.lanxint.zerotalk.ui.sheets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import top.lanxint.zerotalk.ui.theme.AppleHigColors

/**
 * 设置项枚举定义
 */
enum class ProfileSettingItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val summary: String
) {
    // 账号与安全
    SECURITY_CENTER(
        title = "安全中心",
        subtitle = "设备管理与登录环境防护",
        icon = Icons.Default.Security,
        iconBgColor = Color(0xFF10B981),
        summary = "已开启全天候异常登录拦截与防盗保护，当前环境安全等级：极高。"
    ),
    CHANGE_PASSWORD(
        title = "修改密码",
        subtitle = "更新您的账号身份凭据",
        icon = Icons.Default.Lock,
        iconBgColor = Color(0xFF3B82F6),
        summary = "定期更新密码有助于保护账号私密安全，推荐包含字母与特殊符号。"
    ),
    SEARCH_USERS(
        title = "查找用户",
        subtitle = "通过 ID 或唯一昵称发现好友",
        icon = Icons.Default.PersonSearch,
        iconBgColor = Color(0xFF8B5CF6),
        summary = "支持精确搜索零语 ID 或模糊查找同城共同兴趣好友。"
    ),

    // 社交与隐私
    PRIVACY_SETTINGS(
        title = "隐私设置",
        subtitle = "黑名单、动态对谁可见及隐身模式",
        icon = Icons.Default.Shield,
        iconBgColor = Color(0xFF0EA5E9),
        summary = "精细化控制他人对您的资料展示权限，杜绝打扰。"
    ),
    MY_REPORTS(
        title = "我的举报",
        subtitle = "违规动态及不良聊天跟进进度",
        icon = Icons.Default.ReportProblem,
        iconBgColor = Color(0xFFF59E0B),
        summary = "零语共建绿色社区，您举报的所有违规事件均在 2 小时内核查处理。"
    ),
    MY_BLOCKLIST(
        title = "我的黑名单",
        subtitle = "已被您屏蔽拦截的用户列表",
        icon = Icons.Default.Block,
        iconBgColor = Color(0xFF64748B),
        summary = "黑名单用户将无法查看您的个人主页、动态或向您发起任何形式的会话。"
    ),
    PENALTY_RELIEF(
        title = "处罚减免",
        subtitle = "信用分恢复与申诉复核通道",
        icon = Icons.Default.Gavel,
        iconBgColor = Color(0xFFEC4899),
        summary = "若存在误判或需要补全合规材料，可通过此通道提交工单申诉。"
    ),
    NETEASE_BIND(
        title = "网易云绑定",
        subtitle = "使用自己的网易云会员在站内听歌",
        icon = Icons.Default.MusicNote,
        // 用 HIG 语义色而非写死色值（网易云品牌红与 systemRed 接近）
        iconBgColor = AppleHigColors.Light.systemRed,
        summary = "绑定你自己的网易云登录态后，站内播放将使用该账号权益；VIP 曲目需账号本身具备会员，本站不提供绕过。"
    ),

    // 服务与支持
    FEEDBACK(
        title = "问题反馈",
        subtitle = "提交功能缺陷或优化建言",
        icon = Icons.Default.Feedback,
        iconBgColor = Color(0xFF06B6D4),
        summary = "您的每一条宝贵建言都将直接同步至产品开发团队。"
    ),
    CONTACT_US(
        title = "联系我们",
        subtitle = "官方客服与开发者交流通道",
        icon = Icons.Default.Email,
        iconBgColor = Color(0xFF6366F1),
        summary = "商务合作与官方支持邮箱：support@zerotalk.app"
    ),
    DONATION(
        title = "捐赠本站",
        subtitle = "支持零语服务器及算法运营",
        icon = Icons.Default.Favorite,
        iconBgColor = Color(0xFFEF4444),
        summary = "零语承诺永久无侵入式广告，您的支持是平台独立健康运营的最大动力。"
    )
}

/**
 * 「我的」页面模态弹窗 Sheet 枚举定义
 */
sealed interface ProfileSheetType {
    /** 登录账号表单 */
    data object Login : ProfileSheetType

    /** 修改个人资料表单 */
    data object EditProfile : ProfileSheetType

    /** 退出登录二次确认 */
    data object LogoutConfirm : ProfileSheetType

    /** 注销账号二次确认 (高危) */
    data object DeleteAccountConfirm : ProfileSheetType

    /** 各功能设置项详情面板 */
    data class SettingDetail(val item: ProfileSettingItem) : ProfileSheetType
}
