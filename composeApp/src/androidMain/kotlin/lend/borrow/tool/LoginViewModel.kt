package lend.borrow.tool

import User
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

class LoginViewModel(
    private val application: Application,
    private val authService: AuthenticationService
) : BaseViewModel(application) {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val dbUsers: CollectionReference = db.collection("Users")
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _emailError = MutableStateFlow(false)
    val emailError = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow(false)
    val passwordError = _passwordError.asStateFlow()

    private val _confirmPasswordError = MutableStateFlow(false)
    val confirmPasswordError = _confirmPasswordError.asStateFlow()

    private val _currentUser = MutableStateFlow(runBlocking { authService.getCurrentUser() })
    val currentUser = _currentUser.asStateFlow()

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

        if (_uiState.value.email.isEmpty() || _uiState.value.password.isEmpty()) {
            _latestErrorMessage.value = "Error in email or password!"
            return
        }

        launchWithCatchingException {
            _isProcessing.value = true
            authService.authenticate(_uiState.value.email, _uiState.value.password) {
                it.user?.let { fireBaseUser ->
                    dbUsers.document(fireBaseUser.uid).get().addOnSuccessListener { dataSnapShot ->
                        dataSnapShot.data?.let {
                            val user_info = dataSnapShot.toObject(User::class.java)
                            _currentUser.value = user_info
                            _latestErrorMessage.value = null
                            _uiState.value = LoginUiState()
                            _isProcessing.value = false
                        } ?:run {
                            _latestErrorMessage.value = "User not found"
                            _uiState.value = LoginUiState()
                            _isProcessing.value = false
                            onSignOut()
                        }

                    }.addOnFailureListener {
                        _latestErrorMessage.value = "Something went wrong"
                        _uiState.value = LoginUiState()
                        _isProcessing.value = false
                        onSignOut()
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
            authService.createUser(_uiState.value.email, _uiState.value.password) {
                it.user?.let {
                    val signedUpUser = User(
                        it.uid,
                        "",
                        ""
                    )
                    dbUsers.document(it.uid).set(signedUpUser).addOnSuccessListener {
                        _currentUser.value = signedUpUser
                        _latestErrorMessage.value = null
                    }.addOnFailureListener { e ->
                        Toast.makeText(application, "Fail to add the user \n$e", Toast.LENGTH_SHORT)
                            .show()
                    }
                }


            }
            _isProcessing.value = false
        }

    }

    fun onSignOut() {
        launchWithCatchingException {
            authService.signOut()
            _currentUser.value = null
        }
    }

}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)