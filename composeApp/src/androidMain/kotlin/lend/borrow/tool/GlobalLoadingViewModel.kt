package lend.borrow.tool

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class GlobalLoadingViewModel: ViewModel() {
    private val _state = MutableStateFlow(GlobalLoadingState())
    val state: StateFlow<GlobalLoadingState> = _state

    fun loadingInProgress() {
        _state.update { it.copy(loading = true) }
    }
    fun loadingFinished() {
        _state.update { it.copy(loading = false) }
    }
}