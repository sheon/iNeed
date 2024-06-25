
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat.getColor
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
fun BorrowLendApp(navController: NavHostController = rememberNavController(), loginViewModel: LoginViewModel = LoginViewModel(application = Application(),  AuthenticationService(auth = Firebase.auth))) {

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = BorrowLendAppScreen.valueOf(
        backStackEntry?.destination?.route ?: BorrowLendAppScreen.TOOLS.name
    )
    val user: State<User?> = loginViewModel.currentUser.collectAsState()
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
                    user.value?.let { user ->
                        UserProfile(
                            user,
                            loginViewModel = loginViewModel,
                            navController = navController
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

