package top.lanxint.zerotalk.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.home.components.CatchMomentsCard
import top.lanxint.zerotalk.ui.home.components.MatchingCard
import top.lanxint.zerotalk.ui.home.components.MbtiTestCard
import top.lanxint.zerotalk.ui.home.components.PublicChatroomCard
import top.lanxint.zerotalk.ui.home.components.RoomActionCards
import com.kashif_e.backdrop.Backdrop

/**
 * 零语首页视图 (HomeScreen)
 * 承载四大核心功能卡片：
 * 1. 匹配卡片 (MatchingCard - 融入自身位置与在线人数、聊天/语音模式切换与开始匹配)
 * 2. 公共聊天室卡片 (PublicChatroomCard - 零语大厅)
 * 3. 房间操作卡片 (RoomActionCards - 创建房间与加入房间并排卡片)
 * 4. 捞动态卡片 (CatchMomentsCard - 捞动态与记录入口)
 */
@Composable
fun HomeScreen(
    backdrop: Backdrop,
    onStartMatch: (isVoice: Boolean) -> Unit,
    onOpenMatchingSettings: () -> Unit,
    onEnterChatroom: () -> Unit,
    onOpenCreateRoom: () -> Unit,
    onOpenJoinRoom: () -> Unit,
    onOpenCatchMoments: () -> Unit,
    onOpenCatchHistory: () -> Unit,
    onOpenMbtiTest: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // 监听全站在线人数与自身所在位置
    val onlineUsers by ZeroTalkClientManager.onlineUsersCount.collectAsState()
    val userProfile by ZeroTalkClientManager.userProfile.collectAsState()
    val bootstrapData by ZeroTalkClientManager.bootstrapData.collectAsState()
    val loginData by ZeroTalkClientManager.loginData.collectAsState()
    // 语音通话被限制的信号：驱动匹配卡片回退到「聊天匹配」
    val voiceBannedTick by ZeroTalkClientManager.voiceBannedTick.collectAsState()

    val myLocation = userProfile.location.ifBlank {
        bootstrapData?.user?.location.orEmpty()
    }.ifBlank {
        loginData?.location.orEmpty()
    }

    // 每次进入首页刷新在线人数
    LaunchedEffect(Unit) {
        ZeroTalkClientManager.fetchOnlineCount()
    }

    // 获取状态栏高度并加上适度的呼吸留白
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listTopPadding = statusBarTop + 16.dp

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = listTopPadding, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 第一个卡片：匹配卡片（内部整合状态元胶囊 + Backdrop 纯文本分段滑块 + 设置 + 状态文案 + Surface Liquid 按钮）
        item {
            MatchingCard(
                onStartMatch = onStartMatch,
                onSettingsClick = onOpenMatchingSettings,
                onlineUsers = onlineUsers,
                location = myLocation,
                onRefreshOnlineCount = { ZeroTalkClientManager.fetchOnlineCount() },
                backdrop = backdrop,
                isDark = isDark,
                forceChatTick = voiceBannedTick
            )
        }

        // 第二个卡片：公共聊天室（零语大厅）
        item {
            PublicChatroomCard(
                onEnterChatroom = onEnterChatroom,
                isDark = isDark
            )
        }

        // 房间操作卡片（创建房间 & 加入房间，并排独立小卡片）
        item {
            RoomActionCards(
                onCreateRoomClick = onOpenCreateRoom,
                onJoinRoomClick = onOpenJoinRoom,
                isDark = isDark
            )
        }

        // 第三个卡片：捞动态（含右侧捞取记录按键）
        item {
            CatchMomentsCard(
                onCatchMomentsClick = onOpenCatchMoments,
                onHistoryClick = onOpenCatchHistory,
                isDark = isDark
            )
        }

        // 第五个卡片：人格测试（MBTI 60 题问卷，已测出结果时右侧显示类型代码）
        item {
            MbtiTestCard(
                onOpenMbtiTest = onOpenMbtiTest,
                isDark = isDark,
                mbtiType = userProfile.mbti?.type
            )
        }

        // 底部 Spacer 确保页面滚动时三大卡片能够平滑滚入底栏下方，呈现折射效果
        item {
            Spacer(Modifier.height(88.dp))
        }
    }
}
