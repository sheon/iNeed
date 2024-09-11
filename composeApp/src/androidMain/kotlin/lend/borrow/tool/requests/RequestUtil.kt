package lend.borrow.tool.requests

import BorrowRequest
import ToolInApp
import User
import lend.borrow.tool.ToolsRepository
import lend.borrow.tool.UserRepository

suspend fun BorrowRequest.toBorrowRequestUiState(userRepo:UserRepository, tool: ToolInApp? = null, requester: User? = null, requestFetchedCallback: (BorrowRequestUiState) -> Unit) {
    val tmpRequester = requester ?: userRepo.getUserInfo(requesterId)
    if (tool == null)
        ToolsRepository.getInstance(userRepo.application).getTool(toolId, userRepo) { fetchedTool ->
            tmpRequester?.let { fetchedRequester ->
                requestFetchedCallback(
                    BorrowRequestUiState(
                        fetchedTool,
                        fetchedRequester,
                        isAccepted = isAccepted,
                        isRead = isRead,
                        initialRequest = this
                    )
                )
            }
        }

}