package lend.borrow.tool

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

class ToolDetailViewModel(private val application: Application, val toolId: String) : BaseViewModel() {
    var isSavingChanges = MutableStateFlow(false)
    val isFetchingTool = MutableStateFlow(false)

    private lateinit var _toolDetailUiState: MutableStateFlow<OwnerToolDetailUi>
    val toolDetailUiState by lazy {
        _toolDetailUiState.asStateFlow()
    }
    val toolsRepo by lazy {
        ToolsRepository.getInstance(application)
    }

    val userRepo by lazy {
        UserRepository.getInstance(application)
    }
    val favorites: MutableStateFlow<List<String>>
        get() = MutableStateFlow( userRepo.currentUser.value?.favoriteTools?: emptyList())

    var tool = MutableStateFlow<ToolInApp?>(null)
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
            getTool(toolId)
    }



    fun updateUserFavoriteTools(user: User) {
        launchWithCatchingException {
            userRepo.updateUserFavoriteTools(user)
        }
    }


    private fun getTool(toolId: String) {
        if (!isFetchingTool.value)
            viewModelScope.launch {
                isFetchingTool.update {true}
                try {
                    _latestErrorMessage.value = null
                    toolsRepo.getTool(toolId, {
                        tool.value = it
                        initiateOwnerToolDetailEditingUiState(it)
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
    fun onToolImagesChanged(newValue: List<String>){
        _toolDetailUiState.update { it.copy(images = newValue) }
    }

    fun initiateOwnerToolDetailEditingUiState(tool: ToolInApp){
        if (::_toolDetailUiState.isInitialized)
            _toolDetailUiState.update { OwnerToolDetailUi(tool.id, tool.name, tool.description, tool.instruction, tool.imageUrls, tool.tags.joinToString(), tool.owner, tool.borrower, tool.available, tool) }
        else
            _toolDetailUiState = MutableStateFlow(OwnerToolDetailUi(tool.id, tool.name, tool.description, tool.instruction, tool.imageUrls, tool.tags.joinToString(), tool.owner, tool.borrower, tool.available, tool))
    }

    fun deleteTool() {
        if (isProcessing.value.not())
            viewModelScope.launch {
                _isProcessing.value = true
                toolsRepo.deleteTool(toolId) {
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

    fun discardChangesInToolDetail(tool: ToolInApp) {
        initiateOwnerToolDetailEditingUiState(tool)
        isEditingToolInfo.value = false
    }
}

data class OwnerToolDetailUi(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val instruction: String = "",
    val images: List<String> = emptyList(),
    val tags: String? = null,
    val owner: User = User(),
    val borrower: User? = null,
    val isAvailable: Boolean = true,
    val toolToShow: ToolInApp
)


fun OwnerToolDetailUi.toToolInApp() = toolToShow.copy(name = name, description = description, imageUrls = images, tags =tags?.replace(" ", "")?.split(",")?.filterNot { it == "" } ?: emptyList())
