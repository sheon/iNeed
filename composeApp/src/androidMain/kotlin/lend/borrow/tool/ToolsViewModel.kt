package lend.borrow.tool

import ToolDetailUiState
import User
import android.app.Application
import android.content.Context
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import lend.borrow.tool.utility.getCurrentLocation
import lend.borrow.tool.utility.toToolDetailUi

open class ToolsViewModel(private val application: Application, open val userId: String?) : BaseViewModel(application) {

    var fetchingToolsInProgress = MutableStateFlow(false)

    var _allToolsAroundUser = mutableListOf<ToolDetailUiState>()
    var allToolsAroundUser = MutableStateFlow<List<ToolDetailUiState>>(emptyList())
    var toolsToShowForChosenTab = MutableStateFlow<List<ToolDetailUiState>>(emptyList())

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
                _allToolsAroundUser.clear()
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
    private var currentTab: Int = 0
    private val isShowingOwnersTools: Boolean
        get() = currentTab == 1

    fun getToolsFromRemote(location: GeoPoint? = null, isRefreshing: Boolean = false)  {
        anonymousUserLocation = location
        launchWithCatchingException {
            if (!fetchingToolsInProgress.value) {
                _allToolsAroundUser.clear()
                fetchingToolsInProgress.value = true
                toolsRepo.getAvailableTools(location, {
                    _allToolsAroundUser.addAll(it.map { it.toToolDetailUi(application) })
                    filterDataForTab(tabIndex = currentTab)
                    fetchingToolsInProgress.value = false
                }, userRepo, isRefreshing)
            }
        }
    }



    fun filterDataForTab(tabIndex: Int) {
        currentTab = tabIndex
        toolsToShowForChosenTab.update {_allToolsAroundUser.filter { loggedInUser.value == null || loggedInUser.value?.ownTools?.contains(it.id) == isShowingOwnersTools }}
    }

    fun onAddToolToUserFavorites(user: User) {
        launchWithCatchingException {
            userRepo.updateUserFavoriteTools(user)
        }
    }

    fun filterData(iNeedInput: String) {
        val tmpToolList = mutableListOf<ToolDetailUiState>()
        if (iNeedInput.isNotBlank()) {
            val searchableTexts = iNeedInput.split(" ").filterNot { it.isEmpty() }
            if (searchableTexts.size == 1) {
                toolsToShowForChosenTab.update {
                    tmpToolList.addAll(it.filter {
                        it.name.replace(" ", "").equals(searchableTexts[0], true)
                    })
                    tmpToolList
                }
            } else {
                fetchingToolsInProgress.value = true
                application.getResponseFromAI(
                    "I need " + iNeedInput + "? send the list of tools name in a kotlin list of strings in one line.",
                    { keyWords ->
                        toolsToShowForChosenTab.update {
                            keyWords.forEach { requiredTool ->
                                tmpToolList.addAll(it.filter { availableTool ->
                                    availableTool.name
                                        .replace(" ", "")
                                        .equals(requiredTool.replace(" ", ""), true)
                                })
                            }
                            tmpToolList
                        }

                        fetchingToolsInProgress.value = false
                    },
                    {
                        fetchingToolsInProgress.value = false
                        toolsToShowForChosenTab.update {
                            tmpToolList.addAll(it)
                            tmpToolList
                        }
                        _latestErrorMessage.value = "We couldn't find the best match for your request, please rephrase your question and try again!"
                    })
            }
        } else {
            filterDataForTab(currentTab)
        }
    }

}



