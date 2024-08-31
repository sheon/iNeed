package lend.borrow.tool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {
    open val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    internal val _progressMessage = MutableStateFlow<String?>("")
    val progressMessage = _progressMessage.asStateFlow()

    internal val _latestErrorMessage = MutableStateFlow<String?>(null)
    val latestErrorMessage = _latestErrorMessage.asStateFlow()

    val coroutineContext = SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        _latestErrorMessage.update {
            throwable.message
        }
        println("BaseViewModel: Error: ${throwable.message}")
        //show error message using snackbar for all errors
    }

    private var job: Job? =  null

    fun launchWithCatchingException(block: suspend CoroutineScope.() -> Unit) {
        job = viewModelScope.launch(
            context = coroutineContext,
            block = block
        )
        _isProcessing.value = false
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }

}