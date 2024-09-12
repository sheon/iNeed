package lend.borrow.tool

import ToolDetailUiState
import User
import android.app.Application
import android.content.Context
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import lend.borrow.tool.utility.getCurrentLocation
import lend.borrow.tool.utility.toToolDetailUi

open class ToolsViewModel(private val application: Application, open val userId: String?) : BaseViewModel(application) {

    var fetchingToolsInProgress = MutableStateFlow(false)

    var _toolListAroundUser = mutableListOf<ToolDetailUiState>()
    var toolListAroundUser = MutableStateFlow<List<ToolDetailUiState>>(emptyList())

    val loggedInUser by lazy {
        userRepo.currentUser
    }


    init {
        // In case, a registered user logs in then the tools should be fetched as soon as possible.
        // This also helps fetching tools more smoothly when registered user updates their address
        // or search radius. This call cannot be made from composable since it will cause an infinite
        // loop but the viewModel is only created once.
        if(userId != null) {
            loggedInUser
            getToolsFromRemote()
        }
    }
    var anonymousUserLocation: GeoPoint? = null
    fun refreshData() {
        launchWithCatchingException {
            userRepo.fetchUser(userId) {
                _toolListAroundUser.clear()
                userRepo.refreshData()
                getToolsFromRemote(anonymousUserLocation, true)
            }
        }
    }

    fun getAnonymousUserLocation(context: Context, callback: (Double, Double) -> Unit) {
        getCurrentLocation(context){ lat, long ->
            callback(lat, long)
        }
    }

    fun getToolsFromRemote(location: GeoPoint? = null, isRefreshing: Boolean = false)  {
        anonymousUserLocation = location
        launchWithCatchingException {
            if (!fetchingToolsInProgress.value) {
                _toolListAroundUser.clear()
                fetchingToolsInProgress.value = true
                toolsRepo.getAvailableTools(location, {
                    _toolListAroundUser.addAll(it.map { it.toToolDetailUi(application) })
                    toolListAroundUser.value = _toolListAroundUser
                    fetchingToolsInProgress.value = false
                }, userRepo, isRefreshing)
            }
        }
    }

    fun filterDataForTab(showingOwnersTools: Boolean): List<ToolDetailUiState> {
        return _toolListAroundUser.filter { loggedInUser.value == null || loggedInUser.value?.ownTools?.contains(it.id) == showingOwnersTools }
    }

    fun onAddToolToUserFavorites(user: User) {
        launchWithCatchingException {
            userRepo.updateUserFavoriteTools(user)
        }
    }

    fun filterData(iNeedInput: String) {
        val tmpToolList = mutableListOf<ToolDetailUiState>()
        if (iNeedInput.isNotBlank()) {
            if (iNeedInput.split(" ").size == 1) {
                tmpToolList.addAll(_toolListAroundUser.filter {
                    it.name.replace(" ", "").equals(iNeedInput, true)
                })
                toolListAroundUser.value = tmpToolList
            } else {
                fetchingToolsInProgress.value = true
                application.getResponseFromAI("I need " + iNeedInput + "? send the list of tools name in a kotlin list of strings in one line.") {
                    it.forEach { requiredTool ->
                        tmpToolList.addAll(_toolListAroundUser.filter { availableTool ->
                            availableTool.name
                                .replace(" ", "")
                                .equals(requiredTool.replace(" ", ""), true)
                        })
                    }
                    fetchingToolsInProgress.value = false
                    toolListAroundUser.value = tmpToolList
                }
            }
        } else {
            tmpToolList.addAll(_toolListAroundUser)
            toolListAroundUser.value = tmpToolList
        }
    }

}



