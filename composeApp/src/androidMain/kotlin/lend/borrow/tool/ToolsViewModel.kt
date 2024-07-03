package lend.borrow.tool

import ToolInApp
import User
import android.app.Application

class ToolsViewModel(private val application: Application) : BaseViewModel() {

    val toolsRepo by lazy {
        ToolsRepository.getInstance(application)
    }

    val userRepo by lazy {
        UserRepository.getInstance(application)
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