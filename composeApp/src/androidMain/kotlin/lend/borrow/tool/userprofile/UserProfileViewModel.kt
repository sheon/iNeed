package lend.borrow.tool.userprofile

import BorrowRequest
import User
import android.app.Application
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lend.borrow.tool.BaseViewModel
import lend.borrow.tool.R

open class UserProfileViewModel( val loggedInUser: User, val application: Application): BaseViewModel(application) {

    val requestsSentByThisUser = MutableStateFlow(mutableMapOf<String, BorrowRequest>())
    val requestsSentToThisUser = MutableStateFlow(mutableMapOf<String, BorrowRequest>())

    var isEditingUserProfile = MutableStateFlow(false)

    val currentUser = userRepo.currentUser

    var provideAddressMessage = application.getString(R.string.provide_address_message)

    val uploadInProgress = MutableStateFlow(false)

    val userProfileUiState = MutableStateFlow(UserProfileUiState(loggedInUser.name, loggedInUser.address, loggedInUser.searchRadius, loggedInUser.availableAtTheMoment, loggedInUser))

    init {
        viewModelScope.launch {
            launch {
                userRepo.fetchRequestsSentByUser(loggedInUser.id) { sentReq ->
                    requestsSentByThisUser.update { sentReq.associateBy { it.requestId }.toMutableMap() }
                }
            }

            launch {
                userRepo.fetchRequestsSentToUser(loggedInUser.id) { receivedReq ->
                    requestsSentToThisUser.update { receivedReq.associateBy { it.requestId }.toMutableMap() }
                }
            }
        }
    }
    fun onAvailabilityUpdated(available: Boolean) {
        userProfileUiState.update { it.copy(isAvailable = available) }
    }

    fun onUserNameUpdated(name: String) {
        userProfileUiState.update { it.copy(name = name) }
    }

    fun onAddressUpdated(address: String) {
        userProfileUiState.update { it.copy(address = address) }
    }

    fun onSearchRadiusUpdated(searchRadius: Int) {
        userProfileUiState.update { it.copy(searchRadius = searchRadius) }
    }


    fun updateUserInfo(newUserInfo: User, oldUserInfo: User) {
        uploadInProgress.value = true
        launchWithCatchingException {
            isEditingUserProfile.update { false }
            userRepo.updateUserInfo(newUserInfo, oldUserInfo) {
                uploadInProgress.value = false
            }
        }
    }

    fun discardChangesInUserInfo() {
        userProfileUiState.value.defaultUser.apply {
            userProfileUiState.update {
                UserProfileUiState(name, address, searchRadius, availableAtTheMoment, this)
            }
        }
        isEditingUserProfile.update { false }
    }

    fun deleteUser(loggedInUser: User) {
        launchWithCatchingException {
            userRepo.deleteAccount(loggedInUser)
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

    fun uploadTool(toolName: String, toolDescription: String, tags: List<String>, images: List<String>, ownerId: String) {
        uploadInProgress.value = true
        launchWithCatchingException {
            val addedTool = toolsRepo.uploadTool(toolName, toolDescription, tags , images , ownerId)
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

data class UserProfileUiState(val name: String, val address: String, val searchRadius: Int , val isAvailable:Boolean, val defaultUser: User)