package lend.borrow.tool

import User
import android.app.Application

class UserViewModel(private val application: Application): BaseViewModel() {

    val userRepo by lazy {
        UserRepository.getInstance(application)
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