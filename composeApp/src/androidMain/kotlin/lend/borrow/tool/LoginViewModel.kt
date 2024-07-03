package lend.borrow.tool

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class LoginViewModel(
    application: Application,
    val authService: AuthenticationService
) : BaseViewModel() {

    val userRepo by lazy {
        UserRepository.getInstance(application)
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    val currentUser = userRepo.currentUser

    private val _isSigningUp = MutableStateFlow(false)
    val isSigningUp = _isSigningUp.asStateFlow()

    private val isButtonEnabled: StateFlow<Boolean> = combine(uiState) { states ->
        val state = states.first()
        state.email.isNotBlank() && state.password.isNotBlank()
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue) }
    }

    fun onConfirmPasswordChange(newValue: String) {
        _uiState.update { it.copy(confirmPassword = newValue) }
    }

    fun onSignInClick() {

        if (_uiState.value.email.isEmpty() || _uiState.value.password.isEmpty()) {
            _latestErrorMessage.value = "Error in email or password!"
            return
        }

        launchWithCatchingException {
            _isProcessing.value = true
            authService.authenticate(_uiState.value.email, _uiState.value.password).let {
                it.user?.let { fireBaseUser ->
                    userRepo.fetchUser(fireBaseUser.uid).let {
                            _latestErrorMessage.value = null
                            _uiState.value = LoginUiState()
                            _isProcessing.value = false
                        }
                    }
                }
            }
    }

    fun onSignUpClick() {

        if (_uiState.value.email.isEmpty() || _uiState.value.password.isEmpty() || _uiState.value.confirmPassword.isEmpty()) {
            _latestErrorMessage.value = "Error in email or password!"
            return
        }

        launchWithCatchingException {
            _isProcessing.value = true
            userRepo.createUser(_uiState.value.email, _uiState.value.password)
            _isProcessing.value = false
        }

    }

    fun onSignOut() {
        launchWithCatchingException {
            userRepo.signOut()
        }
    }

}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)