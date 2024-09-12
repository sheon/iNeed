package lend.borrow.tool.requests

import BorrowRequest
import ToolInApp
import User
import lend.borrow.tool.ToolsRepository
import lend.borrow.tool.UserRepository

suspend fun BorrowRequest.toBorrowRequestUiState(
    userRepo: UserRepository,
    tool: ToolInApp? = null,
    requester: User? = null,
    requestFetchedCallback: (BorrowRequestUiState) -> Unit
) {
    val tmpRequester = requester ?: userRepo.getUserInfo(requesterId)
    tmpRequester?.let { fetchedRequester ->
        if (tool == null)
            ToolsRepository.getInstance(userRepo.application)
                .getTool(toolId, userRepo) { fetchedTool ->
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
        else
            requestFetchedCallback(
                BorrowRequestUiState(
                    tool,
                    fetchedRequester,
                    isAccepted = isAccepted,
                    isRead = isRead,
                    initialRequest = this
                )
            )
    }
}