package lend.borrow.tool

import User
import android.app.Application
import android.location.Geocoder
import android.os.Build
import dev.gitlive.firebase.firestore.GeoPoint
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

    fun geocodeAddress(userAddress: String, user: User, addressFoundCallback: (GeoPoint?) -> Unit) {
        when {
            userAddress.equals(
                user.address,
                true
            ) -> addressFoundCallback(user.geoPoint) // No need to run the geoCoder again
            userAddress.isNotEmpty() && userAddress.equals(user.address, true).not() || user.geoPoint == null -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Geocoder(application).getFromLocationName(userAddress, 1) {
                        addressFoundCallback(GeoPoint(it.first().latitude, it.first().longitude))
                    }
                } else {
                    Geocoder(application).getFromLocationName(userAddress, 1)?.let {
                        if (it.size != 0)
                            addressFoundCallback(GeoPoint(it.first().latitude, it.first().longitude))
                        else {
                            null
                        }

                    }
                }
            }
            else -> addressFoundCallback(null)
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