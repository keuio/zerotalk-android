package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.ui.theme.AppleHigColors

data class GameOption(
    val type: String,
    val name: String,
    val description: String,
    val tag: String,
    val accentColor: Color
)

private val GAME_OPTIONS = listOf(
    GameOption(
        type = "gobang",
        name = "五子棋",
        description = "15×15 棋盘 · 连成五子即胜",
        tag = "经典棋类",
        accentColor = Color(0xFF007AFF)
    ),
    GameOption(
        type = "go",
        name = "围棋",
        description = "19 路 · 中国规则数子",
        tag = "传统围棋",
        accentColor = Color(0xFF5856D6)
    ),
    GameOption(
        type = "xiangqi",
        name = "中国象棋",
        description = "楚河汉界 · 红方先行",
        tag = "楚汉争霸",
        accentColor = Color(0xFFFF3B30)
    ),
    GameOption(
        type = "chess",
        name = "国际象棋",
        description = "标准规则 · 白方先行",
        tag = "西洋棋",
        accentColor = Color(0xFFFF9500)
    ),
    GameOption(
        type = "undercover",
        name = "谁是卧底",
        description = "4～12 人 · 文字推理",
        tag = "聚会推理",
        accentColor = Color(0xFFAF52DE)
    )
)

/**
 * 经典棋盘/小游戏选择抽屉面板
 *
 * 遵循官方客户端行为规范：
 * - 游戏清单：五子棋 (gobang)、围棋 (go)、中国象棋 (xiangqi)、国际象棋 (chess)、谁是卧底 (undercover)
 * - 发起对战 WS 帧：{"event": "{gameType}_create"}（如 gobang_create）
 */
@Composable
fun GamePickerSheet(
    isDark: Boolean,
    onLaunch: (gameType: String, gameName: String) -> Unit,
    onOpenMySessions: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    var confirmingGame by remember { mutableStateOf<GameOption?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 顶部说明
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = Color(0xFFAF52DE),
                    modifier = Modifier.size(20.dp)
                )
                BasicText(
                    text = "对战小游戏",
                    style = TextStyle(
                        color = higColors.label,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // 关闭按钮
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = higColors.secondaryLabel,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        BasicText(
            text = "发起游戏邀请后，对方同意即可开启实时棋盘对战",
            style = TextStyle(
                color = higColors.secondaryLabel,
                fontSize = 12.sp
            )
        )

        Spacer(Modifier.height(14.dp))

        // 游戏列表
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(GAME_OPTIONS) { game ->
                GameCardItem(
                    game = game,
                    isDark = isDark,
                    onClick = { confirmingGame = game }
                )
            }
        }

        if (onOpenMySessions != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMySessions() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "查看我的游戏对局记录",
                    style = TextStyle(
                        color = Color(0xFF007AFF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // 二次确认发起对战弹窗
    confirmingGame?.let { game ->
        AlertDialog(
            onDismissRequest = { confirmingGame = null },
            title = {
                BasicText(
                    text = "发起「${game.name}」对战",
                    style = TextStyle(
                        color = higColors.label,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = {
                BasicText(
                    text = "是否向对方发起「${game.name}」对战邀请？对方同意后将立即进入棋局。",
                    style = TextStyle(
                        color = higColors.secondaryLabel,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = game
                        confirmingGame = null
                        onLaunch(target.type, target.name)
                        onDismiss()
                    }
                ) {
                    BasicText(
                        text = "立即发起",
                        style = TextStyle(
                            color = Color(0xFF007AFF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingGame = null }) {
                    BasicText(
                        text = "取消",
                        style = TextStyle(
                            color = higColors.secondaryLabel,
                            fontSize = 15.sp
                        )
                    )
                }
            },
            containerColor = if (isDark) Color(0xFF2C2C2E) else Color.White,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun GameCardItem(
    game: GameOption,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val cardBg = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.7f) else Color(0xFFF2F2F7)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标徽章
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(game.accentColor.copy(alpha = 0.15f))
                .border(1.dp, game.accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                tint = game.accentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // 标题与规则描述
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    text = game.name,
                    style = TextStyle(
                        color = higColors.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                // 标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(game.accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    BasicText(
                        text = game.tag,
                        style = TextStyle(
                            color = game.accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            BasicText(
                text = game.description,
                style = TextStyle(
                    color = higColors.secondaryLabel,
                    fontSize = 12.sp
                )
            )
        }

        // 箭头
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = higColors.tertiaryLabel,
            modifier = Modifier.size(18.dp)
        )
    }
}
