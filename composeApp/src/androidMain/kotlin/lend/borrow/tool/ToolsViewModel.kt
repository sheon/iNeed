package lend.borrow.tool

import ToolInApp
import User
import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import lend.borrow.tool.utility.getCurrentLocation

class ToolsViewModel(private val application: Application, val user: User?) : BaseViewModel() {

    var fetchingToolsInProgress = MutableStateFlow(false)
    var fetchingLocationInProgress = MutableStateFlow(false)

    var anythingInProgress = merge(fetchingToolsInProgress, fetchingLocationInProgress)

    val toolsRepo by lazy {
        ToolsRepository.getInstance(application)
    }

    val userRepo by lazy {
        UserRepository.getInstance(application)
    }
    val favorites: MutableStateFlow<List<String>>
        get() = MutableStateFlow( userRepo.currentUser.value?.favoriteTools?: emptyList())

    var _data = mutableListOf<ToolInApp>()
    var data = MutableStateFlow<List<ToolInApp>>(emptyList())

    var tool = MutableStateFlow<ToolInApp?>(null)
    init {
        // In case, a registered user logs in then the tools should be fetched as soon as possible.
        // This also helps fetching tools more smoothly when registered user updates their address
        // or search radius. This call cannot be made from composable since it will cause an infinite
        // loop but the viewModel is only created once.
        if(userRepo.currentUser.value != null)
            getToolsFromRemote()
    }
    var anonymousUserLocation: GeoPoint? = null
    fun refreshData() {
        _data.clear()
        userRepo.refreshData()
        getToolsFromRemote(anonymousUserLocation)
    }

    fun getAnonymousUserLocation(context: Context, callback: (Double, Double) -> Unit) {
        fetchingLocationInProgress.value = true
        getCurrentLocation(context){ lat, long ->
            fetchingLocationInProgress.value = false
            callback(lat, long)
        }
    }
    fun getToolsFromRemote(location: GeoPoint? = null, isOwnTools: Boolean = false)  {
        anonymousUserLocation = location
        launchWithCatchingException {
            if (!fetchingToolsInProgress.value) {
                _data.clear()
                fetchingToolsInProgress.value = true
                toolsRepo.getAvailableTools(location, {
                    _data.addAll(it.filter { user == null || user.ownTools.contains(it.id) == isOwnTools })
                    data.value = _data
                    fetchingToolsInProgress.value = false
                }, userRepo)
            }
        }
    }

    fun updateUserFavoriteTools(user: User) {
        launchWithCatchingException {
            userRepo.updateUserFavoriteTools(user)
        }
    }

    fun filterData(iNeedInput: String) {
        val tmpToolList = mutableListOf<ToolInApp>()
        if (iNeedInput.isNotBlank()) {
            if (iNeedInput.split(" ").size == 1) {
                tmpToolList.addAll(_data.filter {
                    it.name.equals(iNeedInput, true)
                })
                data.value = tmpToolList
            } else {
                fetchingToolsInProgress.value = true
                application.getResponseFromAI("what do I need " + iNeedInput + "? send the list of tools name in a kotlin list of strings in one line.") {
                    it.forEach { requiredTool ->
                        tmpToolList.addAll(_data.filter { availableTool ->
                            availableTool.name
                                .replace(" ", "")
                                .equals(requiredTool.replace(" ", ""), true)
                        })
                    }
                    fetchingToolsInProgress.value = false
                    data.value = tmpToolList
                }
            }
        } else {
            tmpToolList.addAll(_data)
            data.value = tmpToolList
        }
    }

     fun getTool(toolId: String, ) {
         viewModelScope.launch {
             toolsRepo.getTool(toolId, {
                 tool.value = it
             }, userRepo)
         }
    }
}