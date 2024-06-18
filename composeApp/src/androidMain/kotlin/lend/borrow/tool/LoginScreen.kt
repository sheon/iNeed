package lend.borrow.tool

import BorrowLendAppScreen
import User
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Composable
fun LoginScreen(modifier: Modifier = Modifier, loginViewModel: LoginViewModel, navController: NavController) {

    val uiState by loginViewModel.uiState.collectAsState()
    val emailError by loginViewModel.emailError.collectAsState()
    val passwordError by loginViewModel.passwordError.collectAsState()
    val isProcessing by loginViewModel.isProcessing.collectAsState()
    val isButtonEnabled by loginViewModel.isProcessing.collectAsState()
    val currentUser by loginViewModel.currentUser.collectAsState()
    val loginError by loginViewModel.loginErrorMessage.collectAsState()


    LoginScreenContent(
        uiState = uiState,
        onEmailChange = loginViewModel::onEmailChange,
        onPasswordChange = loginViewModel::onPasswordChange,
        onConfirmPasswordChange = loginViewModel::onConfirmPasswordChange,
        isProcessing = isProcessing,
        currentUser = currentUser,
        errorMessage = if (emailError || passwordError) "Error in email or password!" else if(loginError != null) loginError else null,
        loginViewModel = loginViewModel,
        navController = navController
    )

}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LoginScreenContent(
    modifier: Modifier = Modifier,
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    isProcessing: Boolean,
    currentUser: User?,
    errorMessage: String?,
    loginViewModel: LoginViewModel,
    navController: NavController
) {
    var isSigningUp by remember {
        mutableStateOf(false)
    }


    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        val width = this.maxWidth
        val finalModifier = if (width >= 780.dp) modifier.width(400.dp) else modifier.fillMaxWidth()
        Column(
            modifier = finalModifier
                .padding(16.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.email,
                label = {
                    Text("Email")
                },
                onValueChange = onEmailChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.password,
                visualTransformation = PasswordVisualTransformation(),
                label = {
                    Text("Password")
                },
                onValueChange = onPasswordChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isSigningUp) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.confirmPassword,
                    visualTransformation = PasswordVisualTransformation(),
                    label = {
                        Text("Confirm password")
                    },
                    onValueChange = onConfirmPasswordChange
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isProcessing) {
                CircularProgressIndicator()
            } else {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp), onClick = {
                            if (isSigningUp) loginViewModel.onSignUpClick() else loginViewModel.onSignInClick()
                    }
                ) {
                    Text(if (isSigningUp) "SIGN UP" else "SIGN IN")
                }
                if (isSigningUp.not()){

                    HorizontalDivider(Modifier.padding(vertical = 16.dp, horizontal = 40.dp), thickness = 1.dp, color = Color.LightGray)

                    ClickableText(
                        text = AnnotatedString("Sign up") ,
                        onClick = {
                            isSigningUp = true
                        })

                    HorizontalDivider(Modifier.padding(vertical = 16.dp, horizontal = 40.dp), thickness = 1.dp, color = Color.LightGray)

                    ClickableText(
                        text = AnnotatedString("Continue as a guest") ,
                        onClick = {
                            navController.navigate(BorrowLendAppScreen.TOOLS.name)
                        })
                }


            }

            Spacer(modifier = Modifier.height(16.dp))

            //This is just for example, Ideally user will go to some other screen after login
            if(currentUser != null && !currentUser.isAnonymous) {
                navController.navigate(BorrowLendAppScreen.TOOLS.name)
            }

            AnimatedVisibility(errorMessage != null) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text(errorMessage!! , color = MaterialTheme.colorScheme.error)
                }
            }

        }
    }

}