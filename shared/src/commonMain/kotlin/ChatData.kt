import kotlinx.serialization.Serializable

@Serializable
data class ChatRoom(val messages: List<Message>, val name:String)

@Serializable
data class Message(val from: User, val to: User, val message: String)

@Serializable
data class BorrowRequest(val borrowerId: String, val ownerId: String, val toolId: String, val isAccepted: Boolean? = null)