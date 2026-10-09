package top.lanxint.zerotalk.ui.sheets

/**
 * 首页模态底栏枚举类型（对齐 Apple HIG Modal Bottom Sheet）
 */
sealed interface HomeSheetType {
    data object MatchingSettings : HomeSheetType
    data class MatchingInProgress(val isVoice: Boolean) : HomeSheetType
    data object CatchMoments : HomeSheetType
    data object CatchHistory : HomeSheetType
    data object CreateRoom : HomeSheetType
    data object JoinRoom : HomeSheetType
}
