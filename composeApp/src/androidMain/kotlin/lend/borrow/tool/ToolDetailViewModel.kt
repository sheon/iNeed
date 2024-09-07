package lend.borrow.tool

import BorrowRequest
import ToolDetailUiState
import ToolInApp
import User
import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lend.borrow.tool.utility.toToolDetailUi
import lend.borrow.tool.utility.toToolInApp

class ToolDetailViewModel(private val application: Application, val toolId: String, override val userId: String? = null) : ToolsViewModel(application, userId) {
    val isSavingChanges = MutableStateFlow(false)
    val isFetchingTool = MutableStateFlow(false)

    val requestsReceivedForThisTool = MutableStateFlow(emptyList<BorrowRequest>())

    private lateinit var _toolDetailUiState: MutableStateFlow<ToolDetailUiState>
    private val _takingAPicture = MutableStateFlow(false)
    val takingAPicture = _takingAPicture
    val toolDetailUiState by lazy {
        _toolDetailUiState.asStateFlow()
    }

    fun getRequestsForTool(tool: ToolInApp, borrowRequestsCallback: (List<BorrowRequest>) -> Unit) {
        viewModelScope.launch {
            borrowRequestsCallback(userRepo.fetchReceivedRequestsForTool(tool.id))
        }
    }

    fun onRequestToBorrow(user: User, toolToBorrow: ToolDetailUiState, callback: (ToolInApp) -> Unit) {
        launchWithCatchingException {
            userRepo.onRequestToBorrow(user, toolToBorrow.defaultTool) {
                getTool(toolToBorrow.id, callback)
            }
        }
    }

    fun getRequestsReceivedForTool(tool: ToolInApp, borrowRequestsCallback: (List<BorrowRequest>) -> Unit) {
        viewModelScope.launch {
            borrowRequestsCallback(userRepo.fetchReceivedRequestsForTool(tool.id))
        }
    }

    var tool = MutableStateFlow<ToolDetailUiState?>(null)
    var isEditingToolInfo = MutableStateFlow(false)
    init {
        viewModelScope.launch {
            combine(isSavingChanges, isFetchingTool) { saving, fetching ->
                _isProcessing.update { saving || fetching }
                _progressMessage.value = when{
                    saving -> application.getString(R.string.saving)
                    fetching -> application.getString(R.string.fetching)
                    else -> ""
                }
            }.collect()
        }


        // In case, a registered user logs in then the tools should be fetched as soon as possible.
        // This also helps fetching tools more smoothly when registered user updates their address
        // or search radius. This call cannot be made from composable since it will cause an infinite
        // loop but the viewModel is only created once.
            getTool(toolId) {
                getRequestsReceivedForTool(it) {
                    requestsReceivedForThisTool.update { it }
                }
            }
    }



    fun updateUserFavoriteTools(user: User) {
        launchWithCatchingException {
            userRepo.updateUserFavoriteTools(user)
        }
    }

    fun startCamera(start: Boolean) {
        _takingAPicture.value = start
    }

    private fun getTool(toolId: String, callback: (ToolInApp) -> Unit = {}) {
        if (!isFetchingTool.value)
            viewModelScope.launch {
                isFetchingTool.update {true}
                try {
                    _latestErrorMessage.value = null
                    toolsRepo.getTool(toolId, {

                        getRequestsForTool(it) { requestList ->
                            requestsReceivedForThisTool.update { requestList }
                            callback(it)
                        }
                        it.toToolDetailUi(application).also {toolDetailUiState ->
                            tool.value = toolDetailUiState
                        initiateOwnerToolDetailEditingUiState(toolDetailUiState)
                        }

                    }, userRepo)
                } catch (e: Exception) {
                    _latestErrorMessage.value = "Couldn't find your tool"
                    tool.value = null
                } finally {
                    isEditingToolInfo.value = false
                    isFetchingTool.update { false }
                }
            }
    }

    fun onToolNameChanged(newValue: String){
        _toolDetailUiState.update { it.copy(name = newValue) }
    }
    fun onToolDescriptionChanged(newValue: String){
        _toolDetailUiState.update { it.copy(description = newValue) }
    }
    fun onToolInstructionChanged(newValue: String){
        _toolDetailUiState.update { it.copy(instruction = newValue) }
    }
    fun onToolTagsChanged(newValue: String){
        _toolDetailUiState.update { it.copy(tags = newValue) }
    }
    fun onToolImageAdded(newValue: String){
        _toolDetailUiState.update {
            it.newImages.add(newValue)
            it.copy()
        }
    }
    fun onToolImageDeleted(newValue: String){
        _toolDetailUiState.update { affectedTool ->
            val tmpImages = affectedTool.images.toMutableMap()
            tmpImages.remove(newValue)?.let {
                affectedTool.deletedImages.add(newValue)
            }
            affectedTool.newImages.remove(newValue)
            val newToolUi = affectedTool.copy(images = tmpImages)
            newToolUi.newImages.addAll(affectedTool.newImages)
            newToolUi.deletedImages.addAll(affectedTool.deletedImages)
            newToolUi

        }
    }

    fun initiateOwnerToolDetailEditingUiState(tool: ToolDetailUiState){
        if (::_toolDetailUiState.isInitialized)
            _toolDetailUiState.update { tool }
        else
            _toolDetailUiState = MutableStateFlow(tool)
    }

    fun deleteTool(tool: ToolDetailUiState) {
        if (isProcessing.value.not())
            viewModelScope.launch {
                _isProcessing.value = true
                val owner = tool.owner
                toolsRepo.deleteTool(tool.toToolInApp(), owner.copy(ownTools = owner.ownTools.apply { remove(toolId) })) {
                    getTool(toolId)
                    isEditingToolInfo.value = false
                    _isProcessing.value = false
                }
            }
    }
    fun saveChangesInToolDetail() {
        if (isSavingChanges.value.not())
            viewModelScope.launch {
                isSavingChanges.update {true}
                toolsRepo.updateToolDetail(toolDetailUiState.value.toToolInApp()) {
                    getTool(toolDetailUiState.value.id)
                    isSavingChanges.update {false}
                }
            }
    }

    fun discardChangesInToolDetail(tool: ToolDetailUiState) {
        initiateOwnerToolDetailEditingUiState(tool)
        isEditingToolInfo.value = false
    }

}
