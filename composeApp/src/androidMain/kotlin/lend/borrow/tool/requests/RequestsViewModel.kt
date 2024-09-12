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

class RequestsViewModel(
    application: Application,
    val owner: User? = null,
    val requester: User? = null,
    val toolId: String?
) : BaseViewModel(application) {
    val requestsForThisUser = MutableStateFlow(mutableMapOf<String, BorrowRequestUiState>())


    fun getRequests() {
        val tmpRequestsMap = mutableMapOf<String, BorrowRequestUiState>()
        requestsForThisUser.value.map {
            tmpRequestsMap[it.key] = it.value
        }
        viewModelScope.launch {
            when {
                toolId != null -> {
                    toolsRepo.getToolWithRequests(toolId, requester?.id, userRepo) { _, fetchedRequests ->
                        fetchedRequests.forEach { request ->
                            request.toBorrowRequestUiState(userRepo) { borrowRequestUiState ->
                                tmpRequestsMap[request.requestId] = borrowRequestUiState
                            }
                        }
                        requestsForThisUser.update { tmpRequestsMap }
                    }
                }

                requester != null -> {
                    userRepo.fetchRequestsSentByUser(requester.id) { sentReqs ->
                        launch {
                            sentReqs.forEach { request ->
                                request.toBorrowRequestUiState(
                                    userRepo,
                                    requester = requester
                                ) { borrowRequestUiState ->
                                    tmpRequestsMap[request.requestId] = borrowRequestUiState
                                }
                            }
                            requestsForThisUser.update { tmpRequestsMap }
                        }
                    }
                }

                owner != null -> {
                    userRepo.fetchRequestsSentToUser(owner.id) { receivedReqs ->
                        launch {
                            receivedReqs.forEach { request ->
                                request.toBorrowRequestUiState(userRepo) { borrowRequestUiState ->
                                    tmpRequestsMap[request.requestId] = borrowRequestUiState
                                }
                            }
                            requestsForThisUser.update { tmpRequestsMap }
                        }
                    }
                }
            }
        }
    }

    suspend fun getRequest(request: BorrowRequestUiState) {
            userRepo.fetchARequest(request.initialRequest.requestId).also { fetchedRequest ->
                fetchedRequest.toBorrowRequestUiState(
                    tool = request.tool,
                    requester = request.borrower,
                    userRepo = userRepo
                ) { borrowRequestUiState ->
                    val tmpRequestsMap = mutableMapOf<String, BorrowRequestUiState>()
                    requestsForThisUser.value.map {
                        tmpRequestsMap[it.key] = it.value
                    }
                    tmpRequestsMap[fetchedRequest.requestId] = borrowRequestUiState
                    requestsForThisUser.update { tmpRequestsMap }
                }
            }
    }

    init {
        getRequests()
    }

    fun onRequestReadUpdated(request: BorrowRequestUiState) {
        viewModelScope.launch {
            userRepo.updateRequest(request.initialRequest.copy(isRead = true)) {
                getRequest(request)
            }
        }
    }


    fun onRequestAccepted(accepted: Boolean, request: BorrowRequestUiState) {
        viewModelScope.launch {
            userRepo.updateRequest(request.initialRequest.copy(isAccepted = accepted)) {
                getRequest(request)
            }
        }
    }

}


data class BorrowRequestUiState (val tool: ToolInApp, val borrower: User, val isAccepted: Boolean? = null, val isRead: Boolean = false, val initialRequest: BorrowRequest)