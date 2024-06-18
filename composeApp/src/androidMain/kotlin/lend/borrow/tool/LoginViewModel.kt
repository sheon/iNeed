package lend.borrow.tool

import User
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class LoginViewModel(
    private val authService: AuthenticationService
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _emailError = MutableStateFlow(false)
    val emailError = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow(false)
    val passwordError = _passwordError.asStateFlow()

    private val _confirmPasswordError = MutableStateFlow(false)
    val confirmPasswordError = _confirmPasswordError.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isSigningUp = MutableStateFlow(false)
    val isSigningUp = _isSigningUp.asStateFlow()

    private val isButtonEnabled: StateFlow<Boolean> = combine(uiState) { states ->
        val state = states.first()
        state.email.isNotBlank() && state.password.isNotBlank()
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    init {

        launchWithCatchingException {
            authService.currentUser.collect {
                _currentUser.value = it
            }
        }

    }

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue) }
        //reset error
        if (newValue.isNotBlank()) _emailError.value = false
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue) }
        //reset error
        if (newValue.isNotBlank()) _passwordError.value = false
    }

    fun onConfirmPasswordChange(newValue: String) {
        _uiState.update { it.copy(confirmPassword = newValue) }
        //reset error
        _confirmPasswordError.value = false
    }

    fun onSignInClick() {

        if (_uiState.value.email.isEmpty()) {
            _emailError.value = true
            return
        }

        if (_uiState.value.password.isEmpty()) {
            _passwordError.value = true
            return
        }

        launchWithCatchingException {
            _isProcessing.value = true
            authService.authenticate(_uiState.value.email, _uiState.value.password)
            _isProcessing.value = false
        }

    }

    fun onSignUpClick() {

        if (_uiState.value.email.isEmpty()) {
            _emailError.value = true
            return
        }

        if (_uiState.value.password.isEmpty()) {
            _passwordError.value = true
            return
        }

        if (_uiState.value.confirmPassword.isEmpty()) {
            _confirmPasswordError.value = true
            return
        }

        launchWithCatchingException {
            _isProcessing.value = true
            authService.createUser(_uiState.value.email, _uiState.value.password)
            _isProcessing.value = false
        }

    }

    fun onSignOut() {
        launchWithCatchingException {
            authService.signOut()
        }
    }

}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)