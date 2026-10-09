package top.lanxint.zerotalk.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Android 平台 MediaRecorder 录音实现
 *
 * 开始后立即录音；停止时把音频写入缓存文件并读回字节。
 * 支持 M4A(AAC) 与 WEBM：聊天语音按实测链路使用 webm，
 * 若设备不支持 webm 编码则自动回退 M4A，并以 [actualFormat] 反映真实格式。
 */
class AndroidMomentAudioRecorder(
    private val context: Context,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val requestedFormat: MomentAudioFormat = MomentAudioFormat.M4A
) : MomentAudioRecorder {

    override val isRecording: State<Boolean> = mutableStateOf(false)

    /** 已录制秒数（仅 UI 展示） */
    override var recordedSeconds by mutableLongStateOf(0L)
        private set

    override var actualFormat: MomentAudioFormat = requestedFormat
        private set

    private var recorder: MediaRecorder? = null
    private var cacheFile: File? = null
    private var tickingJob: Job? = null

    private fun setRecording(recording: Boolean) {
        (isRecording as MutableState<Boolean>).value = recording
    }

    /** 按格式配置编码器；返回 false 表示该组合不可用（由调用方回退） */
    private fun configureCodec(mp: MediaRecorder, format: MomentAudioFormat): Boolean = try {
        when (format) {
            MomentAudioFormat.M4A -> {
                mp.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mp.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }
            MomentAudioFormat.WEBM -> {
                mp.setOutputFormat(MediaRecorder.OutputFormat.WEBM)
                // OPUS 自 API 29 起可用，低版本回退 VORBIS（同为 webm 容器）
                val encoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaRecorder.AudioEncoder.OPUS
                } else {
                    MediaRecorder.AudioEncoder.VORBIS
                }
                mp.setAudioEncoder(encoder)
            }
        }
        true
    } catch (e: Exception) {
        false
    }

    override fun start(onPermissionDenied: (() -> Unit)?) {
        if (isRecording.value) return
        stopQuietly()
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            onPermissionDenied?.invoke()
            return
        }

        recordedSeconds = 0L
        // 先按请求格式尝试，失败则回退 M4A
        val started = startWith(requestedFormat) || startWith(MomentAudioFormat.M4A)
        if (!started) {
            setRecording(false)
        }
    }

    /** 以指定格式尝试开始录音 */
    private fun startWith(format: MomentAudioFormat): Boolean {
        val file = File(context.cacheDir, "chat_audio_${System.currentTimeMillis()}.${format.fileExt}")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            if (!configureCodec(mediaRecorder, format)) {
                runCatching { mediaRecorder.release() }
                return false
            }
            mediaRecorder.setAudioEncodingBitRate(96_000)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            cacheFile = file
            actualFormat = format
            setRecording(true)
            tickingJob?.cancel()
            tickingJob = scope.launch {
                while (isActive) {
                    delay(1000)
                    recordedSeconds += 1
                }
            }
            true
        } catch (e: Exception) {
            runCatching { mediaRecorder.release() }
            recorder = null
            cacheFile = null
            setRecording(false)
            false
        }
    }

    override fun stop(): ByteArray? {
        if (!isRecording.value) return null
        val current = recorder
        setRecording(false)
        recorder = null
        tickingJob?.cancel()
        tickingJob = null
        if (current == null) return null

        return try {
            runCatching { current.stop() }
            runCatching { current.release() }
            val file = cacheFile ?: return null
            if (!file.exists() || file.length() <= 0L) return null
            file.inputStream().use { it.readBytes() }
        } catch (e: Exception) {
            null
        } finally {
            cacheFile?.delete()
            cacheFile = null
        }
    }

    private fun stopQuietly() {
        runCatching { stop() }
    }

    override fun release() {
        stopQuietly()
    }
}

/**
 * Android 平台录音器（Composable 生命周期内持有，权限请求随页面释放）
 */
@Composable
actual fun rememberMomentAudioRecorder(format: MomentAudioFormat): MomentAudioRecorder {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember(format) { AndroidMomentAudioRecorder(context, scope, format) }

    var permissionDeniedCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // 授权成功也不自动开始录音：按住说话场景下手指可能已经松开，
        // 自动开始会让录音无法停止；统一由用户重新按住/点击触发。
        if (!granted) {
            permissionDeniedCallback?.invoke()
        }
        permissionDeniedCallback = null
    }

    DisposableEffect(recorder) {
        onDispose { recorder.release() }
    }

    return object : MomentAudioRecorder by recorder {
        override fun start(onPermissionDenied: (() -> Unit)?) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionDeniedCallback = onPermissionDenied
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                recorder.start(onPermissionDenied)
            }
        }
    }
}