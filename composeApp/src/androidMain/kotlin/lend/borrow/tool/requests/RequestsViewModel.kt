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

class RequestsViewModel(application: Application, val loggedInUser: User, private val toolId: String?): BaseViewModel(application) {
    val requestsSentByThisUser = MutableStateFlow(mutableMapOf<String, BorrowRequestUiState>())
    val requestsSentToThisUser = MutableStateFlow(mutableMapOf<String, BorrowRequestUiState>())


    fun getRequests(toolId: String? = null, userId: String) {
        viewModelScope.launch {
            when {
                toolId != null -> {
                    val tmpMap = mutableMapOf<String, BorrowRequestUiState>()
                    tmpMap.putAll(requestsSentToThisUser.value)
                    toolsRepo.getToolWithRequests(toolId, userRepo) { _, fetchedRequests ->
                        println("Ehsan: RequestsVM getRequests getToolWithRequests receivedReqs: ${fetchedRequests.map { it.requestId }}")
                        fetchedRequests.forEach { request ->
                            println("Ehsan: RequestsVM getRequests fetchedRequests.forEach: ${request.requestId}")
                            request.toBorrowRequestUiState(userRepo) { borrowRequestUiState ->
                                println("Ehsan: RequestsVM getRequests borrowRequestUiState: ${borrowRequestUiState.initialRequest.requestId}")
                                tmpMap[request.requestId] = borrowRequestUiState
                                println("Ehsan: RequestsVM getRequests tmpMap: ${tmpMap.keys}")
                            }
                        }
                    }
                    requestsSentToThisUser.update { tmpMap }
                }
                else -> {
                    userRepo.fetchAllRequestsForUser(userId) { receivedReqs, sentReqs ->
                        println("Ehsan: RequestsVM getRequests fetchAllRequestsForUser receivedReqs: ${receivedReqs.size}")
                        requestsSentToThisUser.update { receivedReqs.associateBy { it.initialRequest.requestId }.toMutableMap() }
                        requestsSentByThisUser.update { sentReqs.associateBy { it.initialRequest.requestId }.toMutableMap()}
                    }
                }
            }
        }
    }




    init {
        getRequests(toolId, loggedInUser.id)
    }

    fun onRequestReadUpdated(request: BorrowRequest) {
        viewModelScope.launch {
            userRepo.updateRequest(request.copy(isRead = true)) {
                getRequests(request.toolId, loggedInUser.id)
            }
        }
    }

    fun onRequestAccepted(accepted: Boolean, request: BorrowRequest) {
        viewModelScope.launch {
            userRepo.updateRequest(request.copy(isAccepted = accepted)) {
                getRequests(request.toolId, loggedInUser.id)
            }
        }
    }

}


data class BorrowRequestUiState (val tool: ToolInApp, val borrower: User, val isAccepted: Boolean? = null, val isRead: Boolean = false, val initialRequest: BorrowRequest)