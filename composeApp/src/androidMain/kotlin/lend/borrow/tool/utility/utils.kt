package lend.borrow.tool.utility

import ToolInApp
import ToolInFireStore
import User
import lend.borrow.tool.UserRepository

suspend fun ToolInFireStore.toToolInApp(owner: User, userRepo: UserRepository): ToolInApp {
    return ToolInApp(name, id, description, images, tags, available, owner, borrower?.let { userRepo.getUserInfo(it) })
}