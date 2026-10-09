package top.lanxint.zerotalk.data.model

/**
 * 创建房间请求数据模型（预留后续与后端 API 接口对接）
 *
 * @param roomName 房间名称
 * @param roomSecret 房间暗号/通行密码
 * @param isE2EE 是否开启端到端加密
 */
data class CreateRoomRequest(
    val roomName: String = "",
    val roomSecret: String = "",
    val isE2EE: Boolean = true
)

/**
 * 加入房间请求数据模型（预留后续与后端 API 接口对接）
 *
 * @param ownerName 创建者用户名
 * @param roomName 房间名称
 * @param roomSecret 房间暗号/通行密码
 */
data class JoinRoomRequest(
    val ownerName: String = "",
    val roomName: String = "",
    val roomSecret: String = ""
)

/**
 * 房间信息模型（供前端房间状态或后续接口返回）
 */
data class RoomInfo(
    val roomId: String,
    val roomName: String,
    val ownerName: String,
    val isE2EE: Boolean = true,
    val memberCount: Int = 1,
    val createdAt: Long = 0L
)
