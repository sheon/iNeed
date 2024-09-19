
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class Conversation(val conversationId: String, val messages: List<String>)

@Serializable
data class Message(val messageId: String, val fromUserId: String, val message: String, val timeStampInSecond: String = Timestamp.Companion.now().seconds.toString() )

@Serializable
data class BorrowRequest(val requestId: String, val requesterId: String, val ownerId: String, val toolId: String, val isAccepted: Boolean? = null, val isRead: Boolean = false, val conversationId: String? = null, val timeStamp: String = Timestamp.Companion.now().seconds.toString() )