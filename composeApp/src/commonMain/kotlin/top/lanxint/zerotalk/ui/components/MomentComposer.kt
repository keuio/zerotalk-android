package top.lanxint.zerotalk.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.lanxint.zerotalk.data.model.MomentMusic
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import top.lanxint.zerotalk.ui.moments.MomentNeteasePickerSheet
import top.lanxint.zerotalk.ui.moments.MomentUserPickerSheet
import top.lanxint.zerotalk.ui.theme.AppleHigColors
import top.lanxint.zerotalk.ui.theme.AppleHigTypography
import top.lanxint.zerotalk.ui.utils.SelectedPhoto
import top.lanxint.zerotalk.ui.utils.decodeByteArrayToImageBitmap
import top.lanxint.zerotalk.ui.utils.rememberMomentAudioRecorder
import top.lanxint.zerotalk.ui.utils.MomentAudioFormat
import top.lanxint.zerotalk.ui.utils.rememberPhotoPickerLauncher
import com.kashif_e.backdrop.Backdrop

/**
 * 动态发布可见范围
 *
 * 与官方 `POST /moment/create` 入参一一对应：
 * - [Public]  → 不附带任何可见性参数（所有人可见）
 * - [Allow]   → `audience_mode=allow` + `user_ids=JSON数组`（部分人可见）
 * - [Deny]    → `audience_mode=deny` + `user_ids=JSON数组`（不给谁看）
 * - [Mutual]  → `audience_mode=deny` + `audience_mutual=1`（仅互关好友可见）
 * - [Private] → `is_private=1`（仅自己可见）
 *
 * 官方实测「部分可见」与「不给谁看」互斥，后选覆盖先选。
 */
enum class MomentAudience(val label: String) {
    Public("公开"),
    Allow("部分"),
    Deny("不给看"),
    Mutual("互关"),
    Private("仅自己")
}

/** 发布配图上限，与官方发布页保持一致 */
private const val MaxMomentPhotos = 9

/** 可见范围选项（含图标），顺序即展示顺序 */
private val MomentAudienceOptions = listOf(
    Triple(MomentAudience.Public, "公开", Icons.Default.Public),
    Triple(MomentAudience.Allow, "部分可见", Icons.Default.Group),
    Triple(MomentAudience.Deny, "不给谁看", Icons.Default.Lock),
    Triple(MomentAudience.Mutual, "互关好友", Icons.Default.Group),
    Triple(MomentAudience.Private, "仅自己", Icons.Default.Lock)
)

/**
 * 动态发布编辑器 —— 全站唯一的发布实现
 *
 * 「动态」页右下角悬浮「+ 发布动态」与「捞动态」面板内的「发动态」共用本组件，
 * 两处入口不再各自维护一套表单状态、相册选图与提交逻辑。
 *
 * 职责划分：
 * - 组件内部持有正文、配图、可见范围、提交进度等状态，并直接调用
 *   [ZeroTalkClientManager.publishMomentWithPhotos] 落库（`POST /moment/create`）；
 * - 宿主只提供容器外观（卡片 / 模态弹窗），并在 [onPublished] 中关闭面板、刷新列表与提示成功。
 *
 * @param title 面板标题（「动态」页为「发布动态」，「捞动态」面板为「发布新动态」）
 * @param backdrop 传入时提交按钮使用液态毛玻璃 [LiquidButton]；为 null 时退化为 HIG 语义色胶囊按钮
 * @param onDismiss 点击右上角关闭按钮的回调
 * @param onPublished 发布成功回调（图片已上传、动态已创建）
 * @param showTopBar 是否由本组件自绘顶栏（全站唯一 [SheetTopBar]）。默认 false：作为独立模态弹窗时
 *   顶栏由承载它的 [AppleModalBottomSheet] 统一绘制；在「捞动态」面板里内嵌为卡片时传 true。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MomentComposer(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onPublished: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "发布动态",
    backdrop: Backdrop? = null,
    showTopBar: Boolean = false
) {
    val higColors = AppleHigColors.colors(isDark)
    val notificationState = LocalNotificationState.current

    var contentText by remember { mutableStateOf("") }
    var audience by remember { mutableStateOf(MomentAudience.Public) }
    var isSubmitting by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf<String?>(null) }
    var photos by remember { mutableStateOf<List<SelectedPhoto>>(emptyList()) }

    // ---- 语音：点击即开始录音，停止后本地显示时长 + 可移除，发布时才上传 ----
    val audioRecorder = rememberMomentAudioRecorder(MomentAudioFormat.M4A)
    var audioBytes by remember { mutableStateOf<ByteArray?>(null) }
    var audioSeconds by remember { mutableStateOf(0L) }
    var showNeteasePicker by remember { mutableStateOf(false) }
    var selectedMusic by remember { mutableStateOf<MomentMusic?>(null) }
    var showUserPicker by remember { mutableStateOf(false) }
    var selectedUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pickerTitle by remember { mutableStateOf("") }

    fun openUserPicker(mode: MomentAudience) {
        pickerTitle = if (mode == MomentAudience.Allow) "部分人可见" else "不给谁看"
        showUserPicker = true
    }

    val photoPickerLauncher = rememberPhotoPickerLauncher(
        maxItems = MaxMomentPhotos,
        onImagesSelected = { newPhotos ->
            photos = (photos + newPhotos).distinctBy { it.uriString }.take(MaxMomentPhotos)
        },
        onPermissionDenied = {
            notificationState.show("需要相册读取权限以添加动态配图，请在系统设置中允许")
        }
    )

    val canPublish = (contentText.isNotBlank() || photos.isNotEmpty() || audioBytes != null) && !isSubmitting
    val fieldShape = RoundedCornerShape(16.dp)
    val tileShape = RoundedCornerShape(10.dp)

    val onSubmit: () -> Unit = {
        if (canPublish) {
            isSubmitting = true
            progressStatus = if (photos.isNotEmpty()) "正在准备上传图片..." else "正在发布动态..."
            ZeroTalkClientManager.publishMomentWithPhotos(
                content = contentText.trim(),
                photos = photos.map { it.byteArray },
                audioBytes = audioBytes,
                musicJson = selectedMusic?.toJsonString() ?: "",
                isPrivate = audience == MomentAudience.Private,
                audienceMode = when (audience) {
                    MomentAudience.Allow -> "allow"
                    MomentAudience.Deny -> "deny"
                    else -> "all"
                },
                audienceMutual = audience == MomentAudience.Mutual,
                userIds = if (audience == MomentAudience.Allow || audience == MomentAudience.Deny) {
                    selectedUserIds.toList()
                } else emptyList(),
                onProgress = { status -> progressStatus = status },
                onSuccess = {
                    isSubmitting = false
                    progressStatus = null
                    contentText = ""
                    photos = emptyList()
                    audioBytes = null
                    audioSeconds = 0L
                    selectedMusic = null
                    selectedUserIds = emptySet()
                    onPublished()
                },
                onError = { err ->
                    isSubmitting = false
                    progressStatus = null
                    notificationState.show("发布失败: $err")
                }
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // ---- 头部：内嵌卡片时自绘全站唯一 SheetTopBar；独立弹窗时由 AppleModalBottomSheet 绘制 ----
        if (showTopBar) {
            SheetTopBar(
                isDark = isDark,
                title = title,
                leadingAction = SheetAction.Close { onDismiss() }
            )
            Spacer(Modifier.height(12.dp))
        }

        // ---- 正文输入 ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp, max = 140.dp)
                .clip(fieldShape)
                .background(higColors.quaternarySystemFill)
                .border(0.5.dp, higColors.separator, fieldShape)
                .padding(14.dp)
        ) {
            BasicTextField(
                value = contentText,
                onValueChange = { contentText = it },
                modifier = Modifier.fillMaxSize(),
                textStyle = AppleHigTypography.body.copy(color = higColors.label),
                cursorBrush = SolidColor(higColors.tint),
                decorationBox = { innerTextField ->
                    if (contentText.isEmpty()) {
                        BasicText(
                            text = "分享此刻的心情、记录当下的美好... ✨",
                            style = AppleHigTypography.body.copy(color = higColors.placeholderText)
                        )
                    }
                    innerTextField()
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        // ---- 可见范围：直接平铺展示各选项（公开、部分可见等） ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MomentAudienceOptions.forEach { (mode, label, icon) ->
                val isSelected = audience == mode
                SelectableChip(
                    label = label,
                    selected = isSelected,
                    isDark = isDark,
                    icon = icon,
                    textStyle = AppleHigTypography.caption1,
                    horizontalPadding = 10.dp,
                    verticalPadding = 5.dp,
                    onClick = {
                        // 部分可见 / 不给谁看互斥：后选覆盖先选，并打开选人面板
                        if (mode == MomentAudience.Allow || mode == MomentAudience.Deny) {
                            selectedUserIds = emptySet()
                            audience = mode
                            openUserPicker(mode)
                        } else {
                            audience = mode
                            selectedUserIds = emptySet()
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- 配图 ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "动态配图 (${photos.size}/$MaxMomentPhotos)",
                style = AppleHigTypography.caption1.copy(
                    color = higColors.secondaryLabel,
                    fontWeight = FontWeight.Medium
                )
            )
            if (photos.isNotEmpty() && photos.size < MaxMomentPhotos) {
                BasicText(
                    text = "+ 继续添加",
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { photoPickerLauncher.launch() }
                    ),
                    style = AppleHigTypography.caption1.copy(
                        color = higColors.tint,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (photos.isEmpty()) {
            // 空态：整行添加入口
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(tileShape)
                    .background(higColors.quaternarySystemFill)
                    .border(0.5.dp, higColors.separator, tileShape)
                    .clickable { photoPickerLauncher.launch() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "添加图片",
                    tint = higColors.tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicText(
                    text = "添加图片（最多 $MaxMomentPhotos 张）",
                    style = AppleHigTypography.subhead.copy(color = higColors.label)
                )
            }
        } else {
            val chunkedPhotos = remember(photos) { photos.chunked(3) }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunkedPhotos.forEach { rowPhotos ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPhotos.forEach { photo ->
                            val bitmap = remember(photo.byteArray) {
                                decodeByteArrayToImageBitmap(photo.byteArray)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(tileShape)
                                    .background(higColors.tertiarySystemBackground)
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .clickable { photos = photos.filter { it.id != photo.id } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "移除图片",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        // 行内补齐空位与添加入口
                        if (rowPhotos.size < 3 && photos.size < MaxMomentPhotos) {
                            PhotoAddTile(
                                tint = higColors.secondaryLabel,
                                background = higColors.quaternarySystemFill,
                                borderColor = higColors.separator,
                                shape = tileShape,
                                onClick = { photoPickerLauncher.launch() }
                            )
                            repeat(3 - rowPhotos.size - 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        } else if (rowPhotos.size < 3) {
                            repeat(3 - rowPhotos.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 恰好排满整行时补一行添加入口
                if (photos.size % 3 == 0 && photos.size < MaxMomentPhotos) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhotoAddTile(
                            tint = higColors.secondaryLabel,
                            background = higColors.quaternarySystemFill,
                            borderColor = higColors.separator,
                            shape = tileShape,
                            onClick = { photoPickerLauncher.launch() }
                        )
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- 语音 / 音乐 附加内容 ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 语音：点击即开始录音，再点停止
            val isRecording = audioRecorder.isRecording.value
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(tileShape)
                    .background(if (isRecording) Color(0xFFFF3B30).copy(alpha = 0.14f) else higColors.quaternarySystemFill)
                    .border(0.5.dp, higColors.separator, tileShape)
                    .clickable {
                        if (isRecording) {
                            audioBytes = audioRecorder.stop()
                            audioSeconds = audioRecorder.recordedSeconds
                        } else {
                            audioRecorder.start()
                            audioSeconds = 0L
                            if (!audioRecorder.isRecording.value) {
                                // 首次使用需先授予麦克风权限，授权后再次点击即可开始录音
                                notificationState.show("需要麦克风权限，请授权后再次点击录音")
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isRecording) "停止录音" else "录制语音",
                    tint = if (isRecording) Color(0xFFFF3B30) else higColors.tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = if (isRecording) {
                        "停止录音"
                    } else if (audioBytes != null) {
                        "语音 ${audioSeconds}s"
                    } else {
                        "语音"
                    },
                    style = AppleHigTypography.caption2.copy(
                        color = if (isRecording) Color(0xFFFF3B30) else higColors.label,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (audioBytes != null && !isRecording) {
                    BasicText(
                        text = "点击可移除",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                audioBytes = null
                                audioSeconds = 0L
                            }
                        ),
                        style = AppleHigTypography.caption2.copy(
                            color = higColors.tertiaryLabel,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // 音乐：打开网易云选歌
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(tileShape)
                    .background(higColors.quaternarySystemFill)
                    .border(0.5.dp, higColors.separator, tileShape)
                    .clickable { showNeteasePicker = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "选择音乐",
                    tint = higColors.tint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = selectedMusic?.name?.ifBlank { "网易云音乐" } ?: "音乐",
                    style = AppleHigTypography.caption2.copy(
                        color = higColors.label,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                if (selectedMusic != null) {
                    BasicText(
                        text = "点击更换 / 移除",
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { selectedMusic = null }
                        ),
                        style = AppleHigTypography.caption2.copy(
                            color = higColors.tertiaryLabel,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        if ((audience == MomentAudience.Allow || audience == MomentAudience.Deny) && selectedUserIds.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = if (audience == MomentAudience.Allow) "部分人可见：${selectedUserIds.size} 人" else "不给谁看：${selectedUserIds.size} 人",
                style = AppleHigTypography.caption2.copy(
                    color = higColors.tint,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---- 发布 ----
        val submitLabel = if (isSubmitting) (progressStatus ?: "正在发布...") else "立即发布"
        val submitTextStyle = AppleHigTypography.headline.copy(
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        if (backdrop != null) {
            LiquidButton(
                onClick = {
                    if (canPublish) onSubmit()
                    else notificationState.show("请输入动态内容或添加配图")
                },
                backdrop = backdrop,
                isDark = isDark,
                surfaceColor = higColors.tint.copy(alpha = if (isDark) 0.85f else 0.90f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BasicText(text = submitLabel, style = submitTextStyle)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (canPublish) higColors.tint
                        else higColors.secondaryLabel.copy(alpha = 0.3f)
                    )
                    .clickable(enabled = canPublish, onClick = onSubmit),
                contentAlignment = Alignment.Center
            ) {
                BasicText(text = submitLabel, style = submitTextStyle)
            }
        }
    }

    // ---- 网易云选歌弹窗（统一 AppleModalBottomSheet 毛玻璃容器 + 容器顶栏） ----
    if (showNeteasePicker) {
        AppleModalBottomSheet(
            onDismissRequest = { showNeteasePicker = false },
            backdrop = backdrop,
            title = "选择网易云音乐",
            leadingAction = SheetAction.Close { showNeteasePicker = false },
            isDark = isDark
        ) {
            MomentNeteasePickerSheet(
                isDark = isDark,
                onPick = {
                    selectedMusic = it
                    showNeteasePicker = false
                },
                onDismiss = { showNeteasePicker = false }
            )
        }
    }

    // ---- 选人面板（部分可见 / 不给谁看共用） ----
    if (showUserPicker) {
        AppleModalBottomSheet(
            onDismissRequest = { showUserPicker = false },
            backdrop = backdrop,
            title = pickerTitle,
            leadingAction = SheetAction.Close { showUserPicker = false },
            isDark = isDark
        ) {
            MomentUserPickerSheet(
                isDark = isDark,
                selected = selectedUserIds,
                onToggle = { uid, _ ->
                    selectedUserIds = if (selectedUserIds.contains(uid)) {
                        selectedUserIds - uid
                    } else {
                        selectedUserIds + uid
                    }
                },
                onDismiss = { showUserPicker = false }
            )
        }
    }
}

/**
 * 配图网格中的「+」添加入口方块
 */
@Composable
private fun RowScope.PhotoAddTile(
    tint: Color,
    background: Color,
    borderColor: Color,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(shape)
            .background(background)
            .border(0.5.dp, borderColor, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "添加图片",
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
