package top.lanxint.zerotalk.data.model

/**
 * 本地乐观回显与服务端回推消息的字段合并。
 *
 * 对账命中（[EchoMatcher.isSameKind]）后用服务端消息替换本地回显，但**服务端并不回传所有字段**，
 * 直接整体替换会把只有本地才知道的值覆盖成 0 / 空串。典型的就是语音时长：
 *
 * - 发送链路（`_private/聊天图片与语音发送接口.md`）语音帧只有
 *   `{"event":"message","type":"audio","content":"<OSS 直链>","audio_source":"record|file"}`，
 *   **没有任何时长字段**；
 * - 官网 `VoicePlayer`（`publicAnnouncementDismiss-*.js` 里 `de(mo,{url:e.msg.content,...})`）
 *   只接收 `url`，时长是播放器读音频元素元数据（`loadedmetadata` → `audio.duration`）现算的。
 *
 * 因此服务端回推的语音消息 `voiceDurationSec` 恒为 0；若不保留本地回显的时长，
 * 自己发出去的录音就会一直显示 `0"`。
 *
 * 这里统一做「服务端优先、缺失时回落本地回显」的合并，避免大厅 / 房间两条分支各写一份、
 * 后续再漏字段。
 */
internal object LocalEchoMerge {

    /**
     * @param server 服务端回推构建出的消息
     * @param echo 被对账命中的本地乐观回显
     */
    fun merge(server: ChatMessage, echo: ChatMessage): ChatMessage = server.copy(
        // 引用回复：服务端当前不回传这些字段，沿用本地回显（quotedIsMine 服务端从不回传，回显优先）
        quotedText = server.quotedText ?: echo.quotedText,
        quotedIsMine = echo.quotedIsMine ?: server.quotedIsMine,
        quotedSenderName = server.quotedSenderName ?: echo.quotedSenderName,
        replyToId = server.replyToId ?: echo.replyToId,
        // 撤回：任一侧标记即视为已撤回（服务端可能已清空正文）
        isDeleted = server.isDeleted || echo.isDeleted,
        // 语音时长：服务端无该字段，本地已知则保留（服务端 >0 时仍以服务端为准）
        voiceDurationSec = if (server.voiceDurationSec > 0) {
            server.voiceDurationSec
        } else {
            echo.voiceDurationSec
        },
        // 媒体地址 / 来源：服务端为空时回落本地，避免本地已知的直链或 record/file 来源被清空
        audioUrl = server.audioUrl.ifBlank { echo.audioUrl },
        audioSource = server.audioSource.ifBlank { echo.audioSource },
        imageUrl = server.imageUrl.ifBlank { echo.imageUrl },
        // 表情包：image_url 缺失时本地已解析出的地址 / asset_id 不能被清掉
        stickerUrl = server.stickerUrl.ifBlank { echo.stickerUrl },
        stickerAssetId = if (server.stickerAssetId > 0L) server.stickerAssetId else echo.stickerAssetId,
        // 会话摘要与发送者资料：服务端缺失时回落本地
        previewText = server.previewText.ifBlank { echo.previewText },
        senderName = server.senderName.ifBlank { echo.senderName },
        senderAvatar = server.senderAvatar.ifBlank { echo.senderAvatar },
        senderGender = server.senderGender.ifBlank { echo.senderGender },
        // 称号：服务端缺失时回落本地回显（本地回显通常也没有，保持空串即可）
        authorTitle = server.authorTitle.ifBlank { echo.authorTitle },
        authorTitleColor = server.authorTitleColor.ifBlank { echo.authorTitleColor }
    )
}
