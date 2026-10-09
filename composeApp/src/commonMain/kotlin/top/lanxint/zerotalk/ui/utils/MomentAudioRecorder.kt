package top.lanxint.zerotalk.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * 录音输出格式
 *
 * - [M4A]：AAC / MPEG-4，动态语音上传沿用
 * - [WEBM]：聊天语音按实测链路使用 `audio/webm`（presign `content_type=audio/webm`、`ext=webm`）
 */
enum class MomentAudioFormat(val contentType: String, val fileExt: String) {
    M4A("audio/mp4", "m4a"),
    WEBM("audio/webm", "webm")
}

/**
 * 语音录制器抽象
 *
 * 点击/按住开始录音，结束返回音频字节；动态语音在发布时上传，
 * 聊天语音在松手后立即上传（`upload_source=chat_audio`）。
 */
interface MomentAudioRecorder {
    /** 是否正在录音（Compose 可观察状态） */
    val isRecording: State<Boolean>

    /** 已录制时长（秒），录音中实时递增，停止后保留 */
    val recordedSeconds: Long

    /** 本次实际采用的格式（设备不支持请求格式时会回退，需以此为准拼上传参数） */
    val actualFormat: MomentAudioFormat

    /**
     * 开始录音；权限未授予时自动申请，授权后继续开始录音。
     *
     * @param onPermissionDenied 权限被拒绝时回调
     */
    fun start(onPermissionDenied: (() -> Unit)? = null)

    /**
     * 停止录音并返回音频字节（无录音或失败时返回 null）。
     */
    fun stop(): ByteArray?

    /** 释放底层资源（页面销毁时调用） */
    fun release()
}

/**
 * 获取当前页面的录音器（Composable 内使用，随作用域释放）
 *
 * @param format 期望的录音格式；设备不支持时实现会回退到 [MomentAudioFormat.M4A]，
 *               并以 [MomentAudioRecorder.actualFormat] 返回真实格式
 */
@Composable
expect fun rememberMomentAudioRecorder(
    format: MomentAudioFormat
): MomentAudioRecorder
