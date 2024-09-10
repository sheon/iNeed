package lend.borrow.tool.requests

import BorrowRequest
import ToolInApp
import User
import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lend.borrow.tool.BaseViewModel
import lend.borrow.tool.ToolsRepository
import lend.borrow.tool.UserRepository

class RequestsViewModel(application: Application, userId: String?, toolId: String? ): BaseViewModel() {


    val toolsRepo by lazy {
        ToolsRepository.getInstance(application)
    }

    val userRepo by lazy {
        UserRepository.getInstance(application)
    }
    val requests = MutableStateFlow(emptyList<BorrowRequestUiState>())


    fun getRequests(toolId: String? = null, userId: String? = null) {
        val tmpRequests = mutableListOf<BorrowRequestUiState>()
        viewModelScope.launch {
            when {
                toolId != null -> {
                    toolsRepo.getToolWithRequests(toolId, userRepo) { fetchedTool, fetchedRequests ->
                        fetchedRequests.forEach { request ->
                            tmpRequests.add(request)
                        }
                    }
                }
                userId != null -> {
                    userRepo.fetchRequestsSentByUser(userId).forEach { request ->
                        tmpRequests.add(request)
                    }
                }


            }
                        requests.update { tmpRequests.toList() }
        }
    }




    init {
        getRequests(toolId, userId)
    }

    fun onRequestReadUpdated(request: BorrowRequest) {
        viewModelScope.launch {
            userRepo.updateRequest(request.copy(isRead = true)) {
                getRequests(request.toolId)
            }
        }
    }

    fun onRequestAccepted(accepted: Boolean, request: BorrowRequest) {
        viewModelScope.launch {
            userRepo.updateRequest(request.copy(isAccepted = accepted)) {
                getRequests(request.toolId)
            }
        }
    }

}


data class BorrowRequestUiState (val tool: ToolInApp, val borrower: User, val isAccepted: Boolean? = null, val isRead: Boolean = false, val initialRequest: BorrowRequest)