package top.lanxint.zerotalk.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.lanxint.zerotalk.data.network.MbtiAnswer
import top.lanxint.zerotalk.data.network.MbtiQuestion
import top.lanxint.zerotalk.data.network.MbtiQuestionsData
import top.lanxint.zerotalk.data.network.MbtiResult
import top.lanxint.zerotalk.data.network.MbtiResultDimension
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.components.AppleHigFillCard
import top.lanxint.zerotalk.ui.components.CapsuleGlassButton
import top.lanxint.zerotalk.ui.components.LiquidButton
import top.lanxint.zerotalk.ui.theme.AppleHigColorTokens
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography

/**
 * 「MBTI 人格测试」模态弹窗类型（官方 60 题问卷）。
 *
 * 入口由「我的」页面统一添加，调用方只需：activeProfileSheet = MbtiTest
 */
data object MbtiTest : ProfileSheetType

/** 失败态重试目标 */
private enum class MbtiRetryTarget { LOAD_ME, LOAD_QUESTIONS, SUBMIT }

/** MBTI 测评 Sheet 的页面阶段 */
private sealed interface MbtiTestPhase {
    /** 首次进入：正在拉取我的测评状态 */
    data object Loading : MbtiTestPhase
    /** 无结果：引导页 */
    data object Guide : MbtiTestPhase
    /** 正在拉取 60 题 */
    data object LoadingQuestions : MbtiTestPhase
    /** 答题中（index 为当前题下标） */
    data class Quiz(val index: Int) : MbtiTestPhase
    /** 正在提交 */
    data object Submitting : MbtiTestPhase
    /** 结果页 */
    data class Done(val result: MbtiResult) : MbtiTestPhase
    /** 失败页（带重试目标） */
    data class Failed(val message: String, val retry: MbtiRetryTarget) : MbtiTestPhase
}

/**
 * MBTI 测评 Sheet 内容（对齐官方 MbtiTestView 的完整流程）。
 *
 * 1. 进入先 GET /api/mbti/me：有结果直接展示结果页，无结果展示引导页；
 * 2. 开始测试 GET /api/mbti/questions（校验 60 题），一次一题、选完自动下一题、最后一题自动提交；
 * 3. 提交 POST /api/mbti/submit（JSON body）成功后展示结果页；
 * 4. 结果页提供「重新测试」与「清除本次测试」（二次确认）。
 *
 * @param isDark 是否暗色模式
 * @param onShowMessage 全局轻提示回调
 */
@Composable
fun SheetMbtiTestContent(
    isDark: Boolean,
    onShowMessage: (String) -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val scope = rememberCoroutineScope()
    val api = ZeroTalkClientManager.apiService

    var phase by remember { mutableStateOf<MbtiTestPhase>(MbtiTestPhase.Loading) }
    var questions by remember { mutableStateOf<MbtiQuestionsData?>(null) }
    var answers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var result by remember { mutableStateOf<MbtiResult?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }

    suspend fun loadMyResult() {
        phase = MbtiTestPhase.Loading
        api.getMyMbtiResult()
            .onSuccess { loaded ->
                result = loaded
                phase = if (loaded == null) MbtiTestPhase.Guide else MbtiTestPhase.Done(loaded)
            }
            .onFailure { error ->
                phase = MbtiTestPhase.Failed(
                    message = error.message ?: "获取测评状态失败",
                    retry = MbtiRetryTarget.LOAD_ME
                )
            }
    }

    suspend fun loadQuestions() {
        phase = MbtiTestPhase.LoadingQuestions
        api.getMbtiQuestions()
            .onSuccess { data ->
                if (data.questions.size != MbtiQuestionsData.EXPECTED_TOTAL) {
                    phase = MbtiTestPhase.Failed(
                        message = "题目数量异常（期望 60 题，实际 " + data.questions.size + " 题），请稍后重试",
                        retry = MbtiRetryTarget.LOAD_QUESTIONS
                    )
                } else {
                    questions = data
                    answers = emptyMap()
                    result = null
                    phase = MbtiTestPhase.Quiz(0)
                }
            }
            .onFailure { error ->
                phase = MbtiTestPhase.Failed(
                    message = error.message ?: "获取题目失败",
                    retry = MbtiRetryTarget.LOAD_QUESTIONS
                )
            }
    }

    suspend fun submitAnswers() {
        val data = questions ?: return
        phase = MbtiTestPhase.Submitting
        val payload = data.questions.map { question ->
            MbtiAnswer(id = question.id, value = answers[question.id] ?: 0)
        }
        api.submitMbtiTest(version = data.version, answers = payload)
            .onSuccess { submitted ->
                result = submitted
                phase = MbtiTestPhase.Done(submitted)
                onShowMessage("测评完成")
            }
            .onFailure { error ->
                phase = MbtiTestPhase.Failed(
                    message = error.message ?: "提交失败",
                    retry = MbtiRetryTarget.SUBMIT
                )
            }
    }

    fun retry(target: MbtiRetryTarget) {
        scope.launch {
            when (target) {
                MbtiRetryTarget.LOAD_ME -> loadMyResult()
                MbtiRetryTarget.LOAD_QUESTIONS -> loadQuestions()
                MbtiRetryTarget.SUBMIT -> submitAnswers()
            }
        }
    }

    LaunchedEffect(Unit) { loadMyResult() }

    when (val current = phase) {
        MbtiTestPhase.Loading -> MbtiLoadingCard(text = "正在加载测评状态…", isDark = isDark)
        MbtiTestPhase.LoadingQuestions -> MbtiLoadingCard(text = "正在获取题目…", isDark = isDark)
        MbtiTestPhase.Submitting -> MbtiLoadingCard(text = "正在计算结果…", isDark = isDark)
        MbtiTestPhase.Guide -> MbtiGuideCard(
            isDark = isDark,
            onStart = { scope.launch { loadQuestions() } }
        )
        is MbtiTestPhase.Failed -> MbtiFailedCard(
            message = current.message,
            isDark = isDark,
            onRetry = { retry(current.retry) }
        )
        is MbtiTestPhase.Quiz -> {
            val data = questions
            val question = data?.questions?.getOrNull(current.index)
            if (data == null || question == null) {
                MbtiFailedCard(
                    message = "题目已失效，请重新开始测试",
                    isDark = isDark,
                    onRetry = { retry(MbtiRetryTarget.LOAD_QUESTIONS) }
                )
            } else {
                MbtiQuizCard(
                    data = data,
                    index = current.index,
                    question = question,
                    selectedValue = answers[question.id],
                    isDark = isDark,
                    onSelect = { value ->
                        answers = answers + (question.id to value)
                        if (current.index < data.questions.size - 1) {
                            phase = MbtiTestPhase.Quiz(current.index + 1)
                        } else {
                            scope.launch { submitAnswers() }
                        }
                    },
                    onPrevious = {
                        if (current.index > 0) phase = MbtiTestPhase.Quiz(current.index - 1)
                    }
                )
            }
        }
        is MbtiTestPhase.Done -> MbtiResultCard(
            result = current.result,
            isDark = isDark,
            isClearing = isClearing,
            onRetest = { scope.launch { loadQuestions() } },
            onClear = { showClearConfirm = true }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isClearing) showClearConfirm = false },
            title = {
                BasicText(
                    text = "清除本次测试？",
                    style = AppleHigTypography.headline.copy(color = higColors.label)
                )
            },
            text = {
                BasicText(
                    text = "清除后测评结果将被删除，需要重新完成 60 题问卷才能再次获得结果。",
                    style = AppleHigTypography.body.copy(color = higColors.secondaryLabel)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isClearing,
                    onClick = {
                        isClearing = true
                        scope.launch {
                            api.clearMbtiTest()
                                .onSuccess {
                                    isClearing = false
                                    showClearConfirm = false
                                    result = null
                                    questions = null
                                    answers = emptyMap()
                                    phase = MbtiTestPhase.Guide
                                    onShowMessage("已清除本次 MBTI 测评结果")
                                }
                                .onFailure { error ->
                                    isClearing = false
                                    showClearConfirm = false
                                    onShowMessage(error.message ?: "清除失败，请稍后重试")
                                }
                        }
                    }
                ) {
                    BasicText(
                        text = if (isClearing) "清除中…" else "确认清除",
                        style = AppleHigTypography.body.copy(color = higColors.destructive)
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = !isClearing, onClick = { showClearConfirm = false }) {
                    BasicText(
                        text = "取消",
                        style = AppleHigTypography.body.copy(color = higColors.secondaryLabel)
                    )
                }
            }
        )
    }
}

// ============================================================
// 加载 / 失败 / 引导
// ============================================================

@Composable
private fun MbtiLoadingCard(text: String, isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier.fillMaxWidth(),
        contentPaddingValues = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = higColors.tint,
                strokeWidth = 2.5.dp
            )
            BasicText(
                text = text,
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
            )
        }
    }
}

@Composable
private fun MbtiFailedCard(message: String, isDark: Boolean, onRetry: () -> Unit) {
    val higColors = AppleHigColors.colors(isDark)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(20.dp),
            itemSpacing = 8.dp
        ) {
            BasicText(
                text = "加载失败",
                style = AppleHigTypography.headline.copy(color = higColors.label)
            )
            BasicText(
                text = message,
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
            )
        }
        LiquidButton(
            onClick = onRetry,
            isDark = isDark,
            surfaceColor = higColors.tint.copy(alpha = if (isDark) 0.85f else 0.90f),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            BasicText(
                text = "重试",
                style = AppleHigTypography.subhead.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun MbtiGuideCard(isDark: Boolean, onStart: () -> Unit) {
    val higColors = AppleHigColors.colors(isDark)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(18.dp),
            itemSpacing = 10.dp
        ) {
            BasicText(
                text = "认识你的 MBTI 人格",
                style = AppleHigTypography.title3.copy(color = higColors.label)
            )
            BasicText(
                text = "共 60 道题，覆盖 E/I、S/N、T/F、J/P 四个维度。请按第一直觉作答，选完自动进入下一题。",
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel)
            )
            MbtiDimensionLegend(isDark = isDark)
        }
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(18.dp),
            itemSpacing = 8.dp
        ) {
            BasicText(
                text = "测试须知",
                style = AppleHigTypography.subhead.copy(color = higColors.secondaryLabel, fontWeight = FontWeight.SemiBold)
            )
            MbtiBulletLine(text = "没有对错之分，选择更接近真实状态的一项", isDark = isDark)
            MbtiBulletLine(text = "中途可点「上一题」回退修改，最后一题选完自动提交", isDark = isDark)
            MbtiBulletLine(text = "结果可随时「清除本次测试」，清除后需重新作答", isDark = isDark)
        }
        LiquidButton(
            onClick = onStart,
            isDark = isDark,
            surfaceColor = higColors.tint.copy(alpha = if (isDark) 0.85f else 0.90f),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            BasicText(
                text = "开始测试",
                style = AppleHigTypography.subhead.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun MbtiDimensionLegend(isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
    val rows = listOf(
        "E 外向" to "I 内向",
        "S 实感" to "N 直觉",
        "T 思考" to "F 情感",
        "J 判断" to "P 感知"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = pair.first,
                    style = AppleHigTypography.caption1.copy(color = higColors.label)
                )
                BasicText(
                    text = "↔",
                    style = AppleHigTypography.caption1.copy(color = higColors.tertiaryLabel)
                )
                BasicText(
                    text = pair.second,
                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                )
            }
        }
    }
}

@Composable
private fun MbtiBulletLine(text: String, isDark: Boolean) {
    val higColors = AppleHigColors.colors(isDark)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = "•",
            style = AppleHigTypography.footnote.copy(color = higColors.tint)
        )
        BasicText(
            text = text,
            style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel),
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================
// 答题页
// ============================================================

@Composable
private fun MbtiQuizCard(
    data: MbtiQuestionsData,
    index: Int,
    question: MbtiQuestion,
    selectedValue: Int?,
    isDark: Boolean,
    onSelect: (Int) -> Unit,
    onPrevious: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val total = data.questions.size
    val progress = ((index + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "第 " + (index + 1) + " 题 / " + total,
                    style = AppleHigTypography.subhead.copy(color = higColors.label, fontWeight = FontWeight.SemiBold)
                )
                BasicText(
                    text = (progress * 100).toInt().toString() + "%",
                    style = AppleHigTypography.caption1.copy(color = higColors.secondaryLabel)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(higColors.tertiarySystemFill)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(higColors.tint)
                )
            }
        }

        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(18.dp)
        ) {
            BasicText(
                text = question.text,
                style = AppleHigTypography.body.copy(color = higColors.label, fontWeight = FontWeight.Medium)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            data.scale.labels.forEachIndexed { optionIndex, label ->
                val value = optionIndex + 1
                MbtiOptionRow(
                    value = value,
                    label = label,
                    selected = selectedValue == value,
                    isDark = isDark,
                    onClick = { onSelect(value) }
                )
            }
        }

        if (index > 0) {
            CapsuleGlassButton(
                onClick = onPrevious,
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = higColors.tint,
                        modifier = Modifier.size(14.dp)
                    )
                    BasicText(
                        text = "上一题",
                        style = AppleHigTypography.subhead.copy(color = higColors.tint, fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun MbtiOptionRow(
    value: Int,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val shape = RoundedCornerShape(14.dp)
    val background = if (selected) {
        higColors.tint.copy(alpha = if (isDark) 0.24f else 0.14f)
    } else {
        higColors.systemFill
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, if (selected) higColors.tint else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) higColors.tint else higColors.tertiarySystemFill),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                text = value.toString(),
                style = AppleHigTypography.caption2.copy(
                    color = if (selected) Color.White else higColors.secondaryLabel,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        BasicText(
            text = label,
            style = AppleHigTypography.body.copy(color = higColors.label),
            modifier = Modifier.weight(1f)
        )
        BasicText(
            text = value.toString() + " 分",
            style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel)
        )
    }
}

// ============================================================
// 结果页
// ============================================================

@Composable
private fun MbtiResultCard(
    result: MbtiResult,
    isDark: Boolean,
    isClearing: Boolean,
    onRetest: () -> Unit,
    onClear: () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    val accent = mbtiResultAccentColor(result, higColors)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppleHigFillCard(
            isDark = isDark,
            modifier = Modifier.fillMaxWidth(),
            contentPaddingValues = PaddingValues(18.dp),
            itemSpacing = 10.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BasicText(
                    text = result.type,
                    style = AppleHigTypography.largeTitle.copy(color = accent, fontWeight = FontWeight.Bold)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (result.name.isNotBlank() && result.name != result.type) {
                        BasicText(
                            text = result.name,
                            style = AppleHigTypography.headline.copy(color = higColors.label)
                        )
                    }
                    if (result.roleLabel.isNotBlank()) {
                        BasicText(
                            text = result.roleLabel,
                            style = AppleHigTypography.caption1.copy(color = accent, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
            if (result.roleDescription.isNotBlank()) {
                BasicText(
                    text = result.roleDescription,
                    style = AppleHigTypography.footnote.copy(color = higColors.secondaryLabel)
                )
            }
            if (result.keywords.isNotEmpty()) {
                MbtiKeywordChips(keywords = result.keywords, color = accent, isDark = isDark)
            }
        }

        if (result.summary.isNotBlank()) {
            MbtiSection(title = "类型摘要", isDark = isDark) {
                BasicText(
                    text = result.summary,
                    style = AppleHigTypography.subhead.copy(color = higColors.label)
                )
            }
        }

        if (result.dimensions.isNotEmpty()) {
            MbtiSection(title = "四维倾向", isDark = isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    result.dimensions.forEach { dimension ->
                        MbtiDimensionRow(
                            dimension = dimension,
                            accent = accent,
                            higColors = higColors
                        )
                    }
                }
            }
        }

        if (result.strengths.isNotBlank()) {
            MbtiSection(title = "优势", isDark = isDark) {
                BasicText(
                    text = result.strengths,
                    style = AppleHigTypography.subhead.copy(color = higColors.label)
                )
            }
        }

        if (result.weaknesses.isNotBlank()) {
            MbtiSection(title = "劣势", isDark = isDark) {
                BasicText(
                    text = result.weaknesses,
                    style = AppleHigTypography.subhead.copy(color = higColors.label)
                )
            }
        }

        if (result.love.isNotBlank()) {
            MbtiSection(title = "恋爱观", isDark = isDark) {
                BasicText(
                    text = result.love,
                    style = AppleHigTypography.subhead.copy(color = higColors.label)
                )
            }
        }

        if (result.social.isNotBlank()) {
            MbtiSection(title = "社交风格", isDark = isDark) {
                BasicText(
                    text = result.social,
                    style = AppleHigTypography.subhead.copy(color = higColors.label)
                )
            }
        }

        if (result.career.isNotBlank()) {
            MbtiSection(title = "职业倾向", isDark = isDark) {
                BasicText(
                    text = result.career,
                    style = AppleHigTypography.subhead.copy(color = higColors.label)
                )
            }
        }

        if (result.testedAt.isNotBlank()) {
            BasicText(
                text = "测评时间：" + result.testedAt,
                style = AppleHigTypography.footnote.copy(
                    color = higColors.tertiaryLabel,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        LiquidButton(
            onClick = onRetest,
            isDark = isDark,
            surfaceColor = accent.copy(alpha = if (isDark) 0.85f else 0.90f),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            BasicText(
                text = "重新测试",
                style = AppleHigTypography.subhead.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
            )
        }

        CapsuleGlassButton(
            onClick = { if (!isClearing) onClear() },
            enabled = !isClearing,
            isDark = isDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = higColors.destructive,
                    modifier = Modifier.size(15.dp)
                )
                BasicText(
                    text = if (isClearing) "清除中…" else "清除本次测试",
                    style = AppleHigTypography.subhead.copy(
                        color = higColors.destructive,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun MbtiSection(
    title: String,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val higColors = AppleHigColors.colors(isDark)
    AppleHigFillCard(
        isDark = isDark,
        modifier = Modifier.fillMaxWidth(),
        contentPaddingValues = PaddingValues(16.dp),
        itemSpacing = 8.dp
    ) {
        BasicText(
            text = title,
            style = AppleHigTypography.subhead.copy(
                color = higColors.secondaryLabel,
                fontWeight = FontWeight.SemiBold
            )
        )
        content()
    }
}

@Composable
private fun MbtiKeywordChips(keywords: List<String>, color: Color, isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keywords.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { keyword ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(color.copy(alpha = if (isDark) 0.22f else 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        BasicText(
                            text = keyword,
                            style = AppleHigTypography.caption1.copy(
                                color = color,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MbtiDimensionRow(
    dimension: MbtiResultDimension,
    accent: Color,
    higColors: AppleHigColorTokens
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BasicText(
            text = dimension.first,
            style = AppleHigTypography.caption1.copy(color = accent, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.width(12.dp)
        )
        BasicText(
            text = dimension.firstPercent.toString() + "%",
            style = AppleHigTypography.caption2.copy(
                color = higColors.secondaryLabel,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.width(32.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(higColors.tertiarySystemFill)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(dimension.firstFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
        }
        BasicText(
            text = dimension.secondPercent.toString() + "%",
            style = AppleHigTypography.caption2.copy(color = higColors.secondaryLabel),
            modifier = Modifier.width(32.dp)
        )
        BasicText(
            text = dimension.second,
            style = AppleHigTypography.caption1.copy(
                color = higColors.secondaryLabel,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.width(12.dp)
        )
    }
}

/**
 * 角色主题色：优先服务端 role_color，缺失时按 role 回落，最后用系统 tint。
 * 与资料页 MbtiCard 保持同一套 16 型标准配色（analyst 紫 / diplomat 绿 / sentinel 蓝 / explorer 黄）。
 */
private fun mbtiResultAccentColor(result: MbtiResult, colors: AppleHigColorTokens): Color =
    when (result.roleColor.lowercase()) {
        "green" -> colors.systemGreen
        "purple", "violet" -> colors.systemPurple
        "blue" -> colors.systemBlue
        "yellow", "gold" -> colors.systemYellow
        "red" -> colors.systemRed
        "orange" -> colors.systemOrange
        "teal" -> colors.systemTeal
        "pink" -> colors.systemPink
        "indigo" -> colors.systemIndigo
        else -> when (result.role.lowercase()) {
            "analyst" -> colors.systemPurple
            "diplomat" -> colors.systemGreen
            "sentinel" -> colors.systemBlue
            "explorer" -> colors.systemYellow
            else -> colors.tint
        }
    }
