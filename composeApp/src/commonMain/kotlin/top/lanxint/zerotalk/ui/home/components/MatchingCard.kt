package top.lanxint.zerotalk.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.shadow
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
import top.lanxint.zerotalk.ui.components.LiquidButton
import top.lanxint.zerotalk.ui.components.ZeroTalkBottomTab
import top.lanxint.zerotalk.ui.components.ZeroTalkBottomTabs
import com.kashif_e.backdrop.Backdrop

/**
 * 首页第一个卡片：匹配卡片
 * - 顶部中间：Backdrop 纯文本液态毛玻璃分段切换器（“聊天匹配” / “语音匹配”）
 * - 右上角：设置图标按钮（点击拉起 Apple HIG 模态弹窗）
 * - 中部：极简 HIG 文字层级
 * - 底部：直接复用 Backdrop Catalog 中的 Surface Liquid Button 进行“开始匹配”
 */
@Composable
fun MatchingCard(
    onStartMatch: (isVoice: Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    isDark: Boolean,
    onlineUsers: Int? = null,
    location: String? = null,
    onRefreshOnlineCount: (() -> Unit)? = null,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    /** 递增信号：>0 时把匹配模式强制切回「聊天匹配」（对齐官网 voice_call_banned 后回退） */
    forceChatTick: Int = 0,
    surfaceAlpha: Float = if (isDark) 0.08f else 0.90f
) {
    var selectedModeIndex by remember { mutableIntStateOf(0) }
    val isVoice = selectedModeIndex == 1

    // 语音通话被限制：自动切回文字聊天匹配
    LaunchedEffect(forceChatTick) {
        if (forceChatTick > 0) selectedModeIndex = 0
    }

    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF38383A) else Color(0xFFE5E5EA)
    val titleColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
    val iconColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(21.6.dp))
            .background(cardBg)
            .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(21.6.dp))
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 顶部操作与状态栏：两端排布状态元微胶囊与右上角设置按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：状态元标签微胶囊（直接融入卡片，可点击刷新在线人数）
                val displayLocation = location?.trim()?.takeIf { it.isNotEmpty() } ?: "位置保密"
                val onlineUsersText = if (onlineUsers != null && onlineUsers >= 0) {
                    "在线 $onlineUsers 人"
                } else {
                    "在线 — 人"
                }

                CapsuleGlassButton(
                    onClick = { onRefreshOnlineCount?.invoke() },
                    isDark = isDark,
                    backdrop = backdrop,
                    modifier = Modifier.height(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // 在线状态指示小绿点
                        Box(
                            modifier = Modifier
                                .size(5.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759))
                        )
                        BasicText(
                            text = "$displayLocation · $onlineUsersText",
                            style = TextStyle(
                                color = if (isDark) Color(0xFF8B949E) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }

                // 右侧：设置按钮
                CapsuleGlassButton(
                    onClick = onSettingsClick,
                    isDark = isDark,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "匹配设置",
                        tint = iconColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 2. 居中切换 Tab：直接复用 ZeroTalkBottomTabs
            val tabTitles = listOf("聊天匹配", "语音匹配")
            ZeroTalkBottomTabs(
                selectedTabIndex = { selectedModeIndex },
                onTabSelected = { selectedModeIndex = it },
                tabsCount = tabTitles.size,
                backdrop = backdrop,
                outerHeight = 32.dp,
                innerHeight = 24.dp,
                modifier = Modifier.width(130.dp),
                isDark = isDark
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedModeIndex == index
                    ZeroTalkBottomTab(onClick = { selectedModeIndex = index }) {
                        BasicText(
                            text = title,
                            maxLines = 1,
                            style = TextStyle(
                                color = if (isSelected) {
                                    if (isDark) Color(0xFF60A5FA) else Color(0xFF007AFF)
                                } else {
                                    if (isDark) Color(0xFF8B949E) else Color(0xFF64748B)
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 2. 中部：极简 HIG 排版信息展示
            AnimatedContent(
                targetState = isVoice,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "MatchingModeContent"
            ) { voiceMode ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    BasicText(
                        text = if (voiceMode) "实时语音匹配" else "文字聊天匹配",
                        style = TextStyle(
                            color = titleColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        text = if (voiceMode) "以纯粹的声音，与同频零友即刻连线" else "文字交流，慢下来感受每一句心语",
                        style = TextStyle(
                            color = subtitleColor,
                            fontSize = 11.7.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            // 3. 底部主要行动按钮：Surface Liquid Button（宽度缩短至 88%，优雅居中，纯净柔和悬浮阴影）
            LiquidButton(
                onClick = { onStartMatch(isVoice) },
                backdrop = backdrop,
                isDark = isDark,
                surfaceAlpha = surfaceAlpha,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(43.2.dp)
                    .shadow(
                        elevation = if (isDark) 4.dp else 8.dp,
                        shape = RoundedCornerShape(100.dp),
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.08f),
                        spotColor = Color.Black.copy(alpha = if (isDark) 0.45f else 0.15f)
                    )
            ) {
                BasicText(
                    text = "开始匹配",
                    style = TextStyle(
                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF007AFF),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
