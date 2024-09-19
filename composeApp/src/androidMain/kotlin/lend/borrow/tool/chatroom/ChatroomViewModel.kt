package lend.borrow.tool.chatroom

import Conversation
import Message
import User
import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import lend.borrow.tool.BaseViewModel

class ChatroomViewModel(val conversationId: String, val toUserId: String, val application: Application): BaseViewModel(application) {

    val chatRoomUiState = MutableStateFlow(ChatRoomUiState())
    var toUser: User = User()
    private suspend fun getToUserInfo() {
        toUser = userRepo.getUserInfo(toUserId) ?: User()
    }

    private suspend fun getChatroomUiState() = coroutineScope {
        getToUserInfo()
        lateinit var conversation: Conversation
        val conversationRef = userRepo.dbConversations.document(conversationId).get()
        conversation = conversationRef.data<Conversation>()
        val tmpMessages = mutableListOf<Message>()
        conversation.messages.map { messageId ->
                launch {
                    tmpMessages.add(userRepo.getMessage(messageId))
                }
        }.joinAll()
        chatRoomUiState.update {
            it.copy(messages = (it.messages + tmpMessages).sortedBy { it.timeStampInSecond }, toUser = toUser)
        }

    }
    init {
        viewModelScope.launch {
            getChatroomUiState()
        }
    }

    fun sendMessage(message: String, loggedInUser: User) {
        viewModelScope.launch {
            userRepo.sendMessage(message, loggedInUser.id, conversationId)
        }
    }
}

data class ChatRoomUiState(val messages: List<Message> = emptyList(), val toUser: User = User())
