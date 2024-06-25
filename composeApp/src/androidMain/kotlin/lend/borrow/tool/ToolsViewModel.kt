package lend.borrow.tool

import ToolInApp
import ToolsRepository
import User
import UserRepository
import android.app.Application

class ToolsViewModel(private val application: Application) : BaseViewModel(application) {

    val toolsRepo by lazy {
        ToolsRepository()
    }

    val userRepo by lazy {
        UserRepository()
    }

    fun getToolsFromRemote(callback: (List<ToolInApp>) -> Unit)  {
        launchWithCatchingException {
            toolsRepo.getAvailableTools(callback, userRepo)
        }
    }

    fun updateUserFavoriteTools(user: User) {
        launchWithCatchingException {
            userRepo.updateUserFavoriteTools(user)
        }
    }
}