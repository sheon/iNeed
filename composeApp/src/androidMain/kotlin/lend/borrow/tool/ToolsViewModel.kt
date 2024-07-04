package lend.borrow.tool

import ToolInApp
import User
import android.app.Application
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

class ToolsViewModel(private val application: Application) : BaseViewModel() {

    var inProgress = MutableStateFlow(false)

    val toolsRepo by lazy {
        ToolsRepository.getInstance(application)
    }

    val userRepo by lazy {
        UserRepository.getInstance(application)
    }

    var _data = mutableListOf<ToolInApp>()
    var data = MutableStateFlow<List<ToolInApp>>(emptyList())

    init {
        getToolsFromRemote()
    }

    fun getToolsFromRemote()  {
        launchWithCatchingException {
            Log.v("Ehsan", "getToolsFromRemote fetching  ${inProgress} ${this@ToolsViewModel}")
            if (!inProgress.value) {
                inProgress.value = true
                toolsRepo.getAvailableTools({
                    _data.addAll(it)
                    data.value = _data
                    inProgress.value = false
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
                inProgress.value = true
                application.getResponseFromAI("what do I need " + iNeedInput + "? send the list of tools name in a kotlin list of strings in one line.") {
                    it.forEach { requiredTool ->
                        tmpToolList.addAll(_data.filter { availableTool ->
                            availableTool.name
                                .replace(" ", "")
                                .equals(requiredTool.replace(" ", ""), true)
                        })
                    }
                    inProgress.value = false
                    data.value = tmpToolList
                }
            }
        } else {
            tmpToolList.addAll(_data)
            data.value = tmpToolList
        }
    }
}