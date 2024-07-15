package lend.borrow.tool

import User
import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow

class UserViewModel(private val application: Application): BaseViewModel() {

    val userRepo by lazy {
        UserRepository.getInstance(application)
    }

    val toolReo by lazy {
        ToolsRepository.getInstance(application)
    }
    val currentUser = userRepo.currentUser

    var provideAddressMessage = application.getString(R.string.provide_address_message)

    val uploadInProgress = MutableStateFlow(false)

    fun updateUserInfo(newUserInfo: User, oldUserInfo: User) {
        uploadInProgress.value = true
        launchWithCatchingException {
            userRepo.updateUserInfo(newUserInfo, oldUserInfo) {
                uploadInProgress.value = false
            }
        }
    }

    fun getUserInfo(userId: String, callback: (user: User?) -> Unit){
        launchWithCatchingException {
            callback(userRepo.getUserInfo(userId))
        }
    }

    fun uploadTool(toolName: String, toolDescription: String, tags: List<String>, images: List<String>, ownerId: String) {
        uploadInProgress.value = true
        launchWithCatchingException {
            val addedTool = toolReo.uploadTool(toolName, toolDescription, tags , images , ownerId)
            currentUser.value?.let {
                val tmpOwnTools = it.ownTools
                tmpOwnTools.add(addedTool.id)
                userRepo.updateUserInfo(it.copy(ownTools = tmpOwnTools), it) {
                    uploadInProgress.value = false
                }
            }
        }
    }
}