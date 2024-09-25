package lend.borrow.tool.conversations

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import lend.borrow.tool.BaseViewModel

class ConversationsViewModel(val application: Application, userId: String): BaseViewModel(application) {
    val conversationsMap = MutableStateFlow(mutableMapOf<String, ConversationUiState>())

    init {
        getConversations()
    }

    fun getConversations() {
        viewModelScope.launch {
//            if (conversationId != null) {
//                userRepo.dbConversations.document(conversationId).get().also {
//                    val conversation = it.data<Conversation>()
//                    conversationsMap.update {
//                        it[conversationId] = ConversationUiState(conversationId, conversation.messages)
//                        it
//                    }
//                }
//            }
        }


    }

}

data class ConversationUiState(val conversationId: String, val messages: List<String> = emptyList())