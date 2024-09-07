package lend.borrow.tool

import ToolDetailUiState
import User
import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lend.borrow.tool.utility.getCurrentLocation
import lend.borrow.tool.utility.toToolDetailUi

open class ToolsViewModel(private val application: Application, open val userId: String?) : BaseViewModel() {

    var fetchingToolsInProgress = MutableStateFlow(false)
    var fetchingLocationInProgress = MutableStateFlow(false)

    var anythingInProgress = MutableStateFlow(false)

    val toolsRepo by lazy {
        ToolsRepository.getInstance(application)
    }

    val userRepo by lazy {
        UserRepository.getInstance(application)
    }

    var _toolListAroundUser = mutableListOf<ToolDetailUiState>()
    var toolListAroundUser = MutableStateFlow<List<ToolDetailUiState>>(emptyList())

    val loggedInUser by lazy {
        userRepo.currentUser
    }

    init {
        viewModelScope.launch {
            combine(
                fetchingToolsInProgress,
                fetchingLocationInProgress
            ) { fetchingTools, fetchingLocatio ->
                anythingInProgress.update { fetchingTools || fetchingLocatio }
            }.collect()
        }
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
    fun refreshData(isOwnerOfTools: Boolean = false) {
        launchWithCatchingException {
            userRepo.fetchUser(userId) {
                _toolListAroundUser.clear()
                userRepo.refreshData()
                getToolsFromRemote(anonymousUserLocation, isOwnerOfTools)
            }
        }
    }

    fun getAnonymousUserLocation(context: Context, callback: (Double, Double) -> Unit) {
        fetchingLocationInProgress.value = true
        getCurrentLocation(context){ lat, long ->
            fetchingLocationInProgress.value = false
            callback(lat, long)
        }
    }

    private var showingOwnersTools = false
    fun getToolsFromRemote(location: GeoPoint? = null, isOwnerOfTools: Boolean = false)  {
        showingOwnersTools = isOwnerOfTools
        anonymousUserLocation = location
        launchWithCatchingException {
            if (!fetchingToolsInProgress.value) {
                _toolListAroundUser.clear()
                fetchingToolsInProgress.value = true
                toolsRepo.getAvailableTools(location, {
                    _toolListAroundUser.addAll(it.filter { loggedInUser.value == null || loggedInUser.value?.ownTools?.contains(it.id) == showingOwnersTools }.map { it.toToolDetailUi(application) })
                    toolListAroundUser.value = _toolListAroundUser
                    fetchingToolsInProgress.value = false
                }, userRepo)
            }
        }
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



