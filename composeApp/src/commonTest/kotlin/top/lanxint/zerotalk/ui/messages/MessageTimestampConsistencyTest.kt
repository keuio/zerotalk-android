package top.lanxint.zerotalk.ui.messages

import com.google.gson.Gson
import top.lanxint.zerotalk.data.network.ChatMessageDto
import top.lanxint.zerotalk.data.repository.ZeroTalkClientManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageTimestampConsistencyTest {

    private val gson = Gson()

    @Test
    fun `测试消息时间在东八区真机下以服务端北京时间为基准显示00点04分`() {
        val originalTz = TimeZone.getDefault()
        try {
            // 模拟东八区真机
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"))

            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val createdAt = "$todayDateStr 00:04:12"

            val dto = gson.fromJson(
                """{"id":101,"uid":"u1","content":"测试时间","created_at":"$createdAt"}""",
                ChatMessageDto::class.java
            )
            val msg = ZeroTalkClientManager.mapChatMessageDto(dto, "room-1")

            // 列表与聊天内页均准确展示为 00:04
            assertEquals("00:04", msg.timestamp)
            assertEquals("00:04", MessageTimeHelper.getTimeHeaderText(msg))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `测试消息时间在UTC模拟器下自适应换算为对应的本地时刻`() {
        val originalTz = TimeZone.getDefault()
        try {
            // 模拟 UTC 模拟器
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

            val sdfBjt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("GMT+8")
            }
            val todayDateStr = sdfBjt.format(Date())
            val createdAt = "$todayDateStr 00:04:12"

            val dto = gson.fromJson(
                """{"id":102,"uid":"u1","content":"测试时间","created_at":"$createdAt"}""",
                ChatMessageDto::class.java
            )
            val msg = ZeroTalkClientManager.mapChatMessageDto(dto, "room-1")

            // 服务端 00:04 (GMT+8) 对应 UTC 16:04 (前一天)
            assertTrue(msg.timestamp == "昨天" || msg.timestamp == "16:04")
            assertEquals("昨天 16:04", MessageTimeHelper.getTimeHeaderText(msg))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }
}
