package top.lanxint.zerotalk.ui.moments

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import top.lanxint.zerotalk.data.model.MomentItem
import top.lanxint.zerotalk.ui.components.AppleModalBottomSheet
import top.lanxint.zerotalk.ui.utils.BackHandler
import com.kashif_e.backdrop.Backdrop

/**
 * 统一承载动态评论抽屉与分享面板的宿主组件
 *
 * 自动处理 BackHandler 拦截与 AppleModalBottomSheet 模态包裹，
 * 消除在各大包含动态交互的页面中重复编写数十行样板代码。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentSheetsHost(
    commentTarget: MomentItem?,
    shareTarget: MomentItem?,
    isDark: Boolean,
    backdrop: Backdrop? = null,
    onDismissComment: () -> Unit,
    onDismissShare: () -> Unit
) {
    BackHandler(enabled = commentTarget != null || shareTarget != null) {
        if (commentTarget != null) onDismissComment()
        if (shareTarget != null) onDismissShare()
    }

    commentTarget?.let { moment ->
        AppleModalBottomSheet(
            onDismissRequest = onDismissComment,
            backdrop = backdrop,
            isDark = isDark
        ) {
            MomentCommentSheet(
                moment = moment,
                isDark = isDark,
                onClose = onDismissComment
            )
        }
    }

    shareTarget?.let { moment ->
        AppleModalBottomSheet(
            onDismissRequest = onDismissShare,
            backdrop = backdrop,
            isDark = isDark
        ) {
            MomentShareSheet(
                moment = moment,
                isDark = isDark,
                onDismiss = onDismissShare
            )
        }
    }
}
