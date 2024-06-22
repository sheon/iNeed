package lend.borrow.tool

import BorrowLendAppScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FixedScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Composable
fun LoginScreen(modifier: Modifier = Modifier, loginViewModel: LoginViewModel, navController: NavController) {

    val uiState by loginViewModel.uiState.collectAsState()
    val isProcessing by loginViewModel.isProcessing.collectAsState()
    val loginError by loginViewModel.loginErrorMessage.collectAsState()


    LoginScreenContent(
        uiState = uiState,
        onEmailChange = loginViewModel::onEmailChange,
        onPasswordChange = loginViewModel::onPasswordChange,
        onConfirmPasswordChange = loginViewModel::onConfirmPasswordChange,
        isProcessing = isProcessing,
        errorMessage = loginError,
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
            Image(painterResource(
                R.drawable.toolbox_svgrepo_com),
                contentDescription = "",
                contentScale = FixedScale(0.2F),
                modifier = Modifier.fillMaxSize()
            )
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
                    },
                    colors = ButtonDefaults.buttonColors(Color(LocalContext.current.getColor(lend.borrow.tool.shared.R.color.primary)), Color.Black),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text(if (isSigningUp) "SIGN UP" else "SIGN IN", color = Color.White )
                }


                    HorizontalDivider(Modifier.padding(vertical = 16.dp, horizontal = 40.dp), thickness = 1.dp, color = Color.LightGray)

                    ClickableText(
                        text = AnnotatedString(if (isSigningUp.not()) "Sign up" else "Sign in") ,
                        onClick = {
                            isSigningUp = !isSigningUp
                        })

                    HorizontalDivider(Modifier.padding(vertical = 16.dp, horizontal = 40.dp), thickness = 1.dp, color = Color.LightGray)

                    ClickableText(
                        text = AnnotatedString("Continue as a guest") ,
                        onClick = {
                            navController.navigate(BorrowLendAppScreen.TOOLS.name)
                        })


            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(errorMessage != null) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    errorMessage?.let {
                        Text( it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

        }
    }

}