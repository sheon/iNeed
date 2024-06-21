package lend.borrow.tool

import Tool
import ToolsRepository
import User
import UserRepository
import android.app.Application
import kotlinx.coroutines.runBlocking

class ToolsViewModel(private val application: Application) : BaseViewModel(application) {

    val toolsRepo by lazy {
        ToolsRepository()
    }

    val userRepo by lazy {
        UserRepository()
    }

    fun getToolsFromRemote(callback: (List<Tool>) -> Unit) = runBlocking {
        toolsRepo.getAvailableTools(callback)
    }

    fun updateUserFavoriteTools(user: User) {
        launchWithCatchingException {
            userRepo.updateUserFavoriteTools(user)
        }
    }
}