package lend.borrow.tool

import ToolsRepository
import User
import UserRepository
import android.app.Application

class UserViewModel(private val application: Application): BaseViewModel(application) {
    val toolsRepo by lazy {
        ToolsRepository()
    }

    val userRepo by lazy {
        UserRepository()
    }

    fun updateUserInfo(user: User) {
        launchWithCatchingException {
            userRepo.updateUserInfo(user)
        }
    }

    fun getUserInfo(userId: String, callback: (user: User?) -> Unit){
        launchWithCatchingException {
            callback(userRepo.getUserInfo(userId))
        }
    }
}