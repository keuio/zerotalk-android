package top.lanxint.zerotalk.data.model

/**
 * 本地乐观回显与服务端回推消息的「同一条」判定。
 *
 * 抽成纯函数是为了可单测：这条规则一旦判错，本地回显就永远拿不到服务端 id
 * （`serverId` 恒为 0），消息会永远停在回显上 —— 表现为撤回时提示「该消息尚未同步到服务端」。
 */
internal object EchoMatcher {

    /**
     * @param echo 本地乐观回显
     * @param eventType 服务端消息类型（image / audio / dice / text …）
     * @param eventContent 服务端消息 content
     * @param eventImageUrl 服务端消息 image_url（仅图片可能携带）
     * @param eventClientMessageId 服务端回推的 `client_message_id`；与本地回显一致即同一条
     */
    fun isSameKind(
        echo: ChatMessage,
        eventType: String,
        eventContent: String,
        eventImageUrl: String? = null,
        eventClientMessageId: String? = null
    ): Boolean {
        // 加密消息的服务端回推是密文，明文 content 必然不等；
        // 官方以 client_message_id 判定同一条，这里优先用同一标识精确对账。
        val eventClientId = eventClientMessageId?.trim().orEmpty()
        if (eventClientId.isNotEmpty() && echo.clientMessageId.isNotBlank() &&
            echo.clientMessageId == eventClientId
        ) {
            return true
        }
        if (echo.content == eventContent) return true
        val type = eventType.lowercase()
        if (echo.isDice && type == "dice") return true

        // 图片 / 语音的本地回显 content 是占位文案（「［图片］」「[语音 N\"]」），
        // 而 WS 帧与服务端回推的 content 是 OSS URL —— 只比 content 永远匹配不上。
        val echoUrl = when {
            echo.isImage && type == "image" -> echo.imageUrl
            echo.isVoice && type == "audio" -> echo.audioUrl
            else -> return false
        }
        // 图片的 URL 可能在 image_url 或 content；语音的 URL 就在 content（实测帧）
        val eventUrl = if (echo.isImage) eventImageUrl ?: eventContent else eventContent
        return echoUrl.isNotBlank() && eventUrl.isNotBlank() && echoUrl == eventUrl
    }
}
