package lend.borrow.tool.chatroom

import Conversation
import Message
import User
import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import lend.borrow.tool.BaseViewModel

class ChatroomViewModel(val conversationId: String, val toUserId: String, val application: Application): BaseViewModel(application) {

    val messageListState = mutableStateListOf<Message>()
    var toUser: User = User()
    private suspend fun getToUserInfo() {
        toUser = userRepo.getUserInfo(toUserId) ?: User()
    }

    private fun getChatroomUiState(): Flow<Conversation> {
        return userRepo.dbConversations.document(conversationId)
            .snapshots
            .map { querySnapshot ->
                querySnapshot.data<Conversation>()
            }
    }

    init {
        viewModelScope.launch {
            getToUserInfo()
            getChatroomUiState().collect { conversation ->
                val tmpMessages = mutableListOf<Message>()
                conversation.messages.mapNotNull { messageId ->
                    if (messageListState.map { it.messageId }.contains(messageId)
                            .not()
                    ) launch {
                            tmpMessages.add(userRepo.getMessage(messageId))
                        }
                    else
                        null

                }.joinAll()
                messageListState.addAll(tmpMessages)
            }
        }
    }

    fun sendMessage(message: String, loggedInUser: User) {
        viewModelScope.launch {
            userRepo.sendMessage(message, loggedInUser.id, conversationId)
        }
    }
}

