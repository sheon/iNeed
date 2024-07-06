
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import lend.borrow.tool.AuthenticationService
import lend.borrow.tool.LoginScreen
import lend.borrow.tool.LoginViewModel
import lend.borrow.tool.RegisteredToolsScreen
import lend.borrow.tool.UserProfile


enum class BorrowLendAppScreen(val title: String, modifier: Modifier = Modifier) {
    LOGIN(title = "Log in"),
    TOOLS(title = "Items to borrow"),
    USER(title = "User profile")
}

@Composable
fun BorrowLendApp(navController: NavHostController = rememberNavController()) {

    val context = LocalContext.current
    val authService = AuthenticationService(auth = Firebase.auth)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = BorrowLendAppScreen.valueOf(
        backStackEntry?.destination?.route ?: BorrowLendAppScreen.TOOLS.name
    )
    val loginViewModel = viewModel {
        LoginViewModel((context as Activity).application, authService)
    }

    val user: State<User?> = loginViewModel.currentUser.collectAsState()

    val shouldEditUserProfile = user.value?.address?.isEmpty() == true
    val openDialog = remember {
        mutableStateOf( user.value?.address?.isEmpty() == true)
    }


    MaterialTheme {

        Scaffold(
            topBar = {
                BorrowLendAppBar(
                    currentScreen = currentScreen,
                    navController = navController,
                    user
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (user.value == null) BorrowLendAppScreen.LOGIN.name
                else if (user.value?.address?.isEmpty() == true)
                    BorrowLendAppScreen.USER.name
                else
                    BorrowLendAppScreen.TOOLS.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(BorrowLendAppScreen.LOGIN.name) {
                    LoginScreen(
                        Modifier
                            .fillMaxSize(),
                        loginViewModel,
                        navController
                    )
                }
                composable(BorrowLendAppScreen.TOOLS.name) {
                    RegisteredToolsScreen(user.value)
                }

                composable(BorrowLendAppScreen.USER.name) {


                    if (openDialog.value) {
                        AddressRequiredWarningDialog(
                            onNegativeClick = {
                                loginViewModel.onSignOut()
                                openDialog.value = false
                            },
                            onPositiveClick = {
                                openDialog.value = false
                            })
                    }
                    user.value?.let {
                        UserProfile(
                            loginViewModel = loginViewModel,
                            navController = navController,
                            isEditingUserProfile = shouldEditUserProfile
                        )
                    }

                }

            }
        }
    }
}

@Composable
fun BorrowLendAppBar(
    currentScreen: BorrowLendAppScreen,
    navController: NavController,
    user: State<User?>
) {
    TopAppBar(
        backgroundColor = Color(getColor(LocalContext.current, lend.borrow.tool.shared.R.color.primary)),
        contentColor = Color.White
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.wrapContentSize(), verticalAlignment = Alignment.CenterVertically) {
                when {
                    user.value == null && navController.currentDestination?.route == BorrowLendAppScreen.LOGIN.name -> {
                        null
                    }

                    user.value == null && navController.currentDestination?.route == BorrowLendAppScreen.TOOLS.name -> {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }

                    user.value != null && navController.currentDestination?.route == BorrowLendAppScreen.TOOLS.name -> {
                        IconButton(onClick = {
                            navController.navigate(BorrowLendAppScreen.USER.name)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile Btn"
                            )
                        }
                    }

                    navController.currentDestination?.route == BorrowLendAppScreen.USER.name -> {
                        if (navController.currentBackStackEntry?.arguments?.getBoolean("isEditingUserProfile") == false)
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                    }
                }
            }
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    currentScreen.title,
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W900
                )
            }
        }
    }
}


@Composable
fun AddressRequiredWarningDialog(
    onNegativeClick: () -> Unit,
    onPositiveClick: () -> Unit
) {
    Dialog(onDismissRequest = {},
        DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color.LightGray
        ) {
            Column(Modifier.padding(15.dp)) {
                Text(modifier = Modifier.padding(5.dp),
                    text = "You need to provide your address so other users nearby can find the tools you can lend them. Your address is not visible to any other user and only used to proximate your location.",
                    textAlign = TextAlign.Justify)
                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    TextButton(modifier = Modifier.padding(5.dp),
                        colors = ButtonDefaults.buttonColors(Color.LightGray, Color.Black),
                        shape = RoundedCornerShape(2.dp),
                        onClick = onNegativeClick) {
                        Text(text = "CANCEL")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(modifier = Modifier.padding(5.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.textButtonColors(Color(LocalContext.current.getColor(lend.borrow.tool.shared.R.color.primary)), Color.White),
                        onClick = {
                            onPositiveClick()
                        }) {
                        Text(text = "OK")
                    }
                }
            }
        }
    }
}
