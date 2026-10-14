package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.ui.utils.BackHandler

/**
 * 首次启动「使用说明与须知」浮层
 *
 * 一次性声明（只在首次启动弹一次，确认后写入设备级偏好，不随退出登录重置）：
 * 1. **开发归属**：本 App 是零语 / ZeroTalk 的**非官方第三方客户端**，与官方无任何关联；
 * 2. **使用须知**：需自行遵守官方服务条款，凭据仅加密保存在本机，软件按「现状」提供；
 * 3. **数据归属**：聊天、动态、资料等数据归官方及其权利人所有，本 App 不额外采集或上传；
 * 4. **联系与反馈**：官方渠道属于官方，App 自身问题请联系第三方开发者。
 *
 * 浮层不可点击穿透、拦截系统返回，必须显式选择「我已阅读并知悉」或「退出」。
 *
 * @param onAccept 点击「我已阅读并知悉」
 * @param onExit 点击「退出」（未同意时不进入 App）
 */
@Composable
fun FirstLaunchNoticeOverlay(
    onAccept: () -> Unit,
    onExit: () -> Unit,
    isDark: Boolean
) {
    val higColors = AppleHigColors.colors(isDark)

    // 拦截系统返回：说明未确认前不允许返回键把浮层「绕过去」
    BackHandler(enabled = true) { /* 必须显式选择 */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            // 吞掉所有点击，避免误触到底下的页面
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color(0xFF1C1F28) else Color(0xFFF7F7FA))
                .border(
                    width = 0.5.dp,
                    color = higColors.separator.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = higColors.tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicText(
                    text = "使用说明与须知",
                    style = AppleHigTypography.headline.copy(
                        color = higColors.label,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(Modifier.height(4.dp))

            BasicText(
                text = "首次使用前请阅读以下内容",
                style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NoticeSection(
                    title = "开发归属",
                    body = "本 App 是「零语 / ZeroTalk」（app.zerotalk.cn）的非官方第三方客户端，" +
                        "由社区开发者（kelo）独立开发，与零语官方及其运营方无任何关联，" +
                        "未获得其授权、认可或支持。软件名称、商标、接口与内容均归其各自权利人所有。",
                    isDark = isDark
                )
                NoticeSection(
                    title = "使用须知",
                    body = "· 需自行注册零语账号，并遵守官方的服务条款与社区规范；\n" +
                        "· 账号与密码仅经加密后保存在本机，不会提供给任何第三方；\n" +
                        "· 本软件按「现状」提供，不提供任何明示或暗示的担保，" +
                        "使用过程中产生的风险与后果由使用者自行承担；\n" +
                        "· 请勿用于任何违反法律法规或平台规则的用途。",
                    isDark = isDark
                )
                NoticeSection(
                    title = "数据归属",
                    body = "· 聊天内容、动态、用户资料、房间与游戏数据等均归零语官方及其权利人所有；\n" +
                        "· 本 App 不搭建、不运营任何服务器，也不额外采集或上传你的数据，" +
                        "仅在本机保存登录态、会话与界面偏好；\n" +
                        "· 在系统设置中清除 App 数据或卸载本 App，即可删除本机留存的全部信息。",
                    isDark = isDark
                )
                NoticeSection(
                    title = "联系与反馈",
                    body = "· 「问题反馈」「联系我们」中的官方渠道属于零语官方；\n" +
                        "· 本 App 自身的缺陷与建议（第三方开发，非官方）请走「App反馈」群，" +
                        "或邮件联系开发者 kelo@lanxint.top。",
                    isDark = isDark
                )
            }

            Spacer(Modifier.height(16.dp))

            // 主操作：我已阅读并知悉
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(higColors.tint)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAccept
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "我已阅读并知悉",
                    style = AppleHigTypography.subhead.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(Modifier.height(8.dp))

            // 次操作：不同意则退出 App
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExit
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "不同意，退出",
                    style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                )
            }
        }
    }
}

/** 说明条目：小标题 + 正文 */
@Composable
private fun NoticeSection(title: String, body: String, isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = title,
            style = AppleHigTypography.subhead.copy(
                color = higColors.label,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(3.dp))
        BasicText(
            text = body,
            style = AppleHigTypography.caption1.copy(
                color = higColors.secondaryLabel,
                fontSize = 12.sp
            )
        )
    }
}
