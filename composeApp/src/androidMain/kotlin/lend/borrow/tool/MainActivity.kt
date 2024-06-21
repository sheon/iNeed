package lend.borrow.tool

import BorrowLendApp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

class MainActivity : ComponentActivity() {
    val authService = AuthenticationService(auth = Firebase.auth)
    val loginViewModel by lazy {
        LoginViewModel(this.application, authService)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BorrowLendApp(loginViewModel = loginViewModel)
        }
    }
}
