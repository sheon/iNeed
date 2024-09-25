
import android.app.Activity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import lend.borrow.tool.AuthenticationService
import lend.borrow.tool.BorrowLendAppScreen
import lend.borrow.tool.LoginScreen
import lend.borrow.tool.LoginViewModel
import lend.borrow.tool.RegisteredToolsScreen
import lend.borrow.tool.ToolDetailScreen
import lend.borrow.tool.chatroom.ChatroomScreen
import lend.borrow.tool.conversations.ConversationsScreen
import lend.borrow.tool.requests.RequestsScreen
import lend.borrow.tool.userprofile.UserProfileScreen


@Composable
fun BorrowLendApp(navController: NavHostController = rememberNavController()) {

    val context = LocalContext.current
    val authService = AuthenticationService(auth = Firebase.auth)

    val currentScreenName = rememberSaveable {
        mutableStateOf(BorrowLendAppScreen.TOOLS)
    }
    val loginViewModel = viewModel {
        LoginViewModel((context as Activity).application, authService)
    }

    val user: State<User?> = loginViewModel.currentUser.collectAsState()

    val shouldEditUserProfile = user.value?.address?.isEmpty() == true


    MaterialTheme {

        Scaffold(
            topBar = {
                if(currentScreenName.value != BorrowLendAppScreen.LOGIN)
                BorrowLendAppBar(
                    currentScreenName = currentScreenName.value,
                    navController = navController,
                    user
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (user.value == null)
                    BorrowLendAppScreen.LOGIN.name
                else if (user.value?.address?.isEmpty() == true)
                    BorrowLendAppScreen.USER.name
                else
                    BorrowLendAppScreen.TOOLS.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                enterTransition = {
                    fadeIn()
                },
                exitTransition = {
                    fadeOut()
                }
            ) {
                composable(BorrowLendAppScreen.LOGIN.name) {
                    currentScreenName.value = BorrowLendAppScreen.LOGIN
                        LoginScreen(
                            Modifier
                                .fillMaxSize(),
                            loginViewModel,
                            navController
                        )
                }
                composable(BorrowLendAppScreen.TOOLS.name) {
                    currentScreenName.value = BorrowLendAppScreen.TOOLS
                    RegisteredToolsScreen(user.value?.id, navController)
                }

                composable(
                    "${BorrowLendAppScreen.TOOL_DETAIL.name}/{toolId}",
                    arguments = listOf(navArgument("toolId") { type = NavType.StringType })
                    ) {
                    currentScreenName.value = BorrowLendAppScreen.TOOL_DETAIL
                    it.arguments?.getString("toolId")?.let {toolId ->
                        if(it.lifecycle.currentState == Lifecycle.State.RESUMED)
                            ToolDetailScreen(toolId, user.value, navController)
                    }
                }

                composable(BorrowLendAppScreen.USER.name) {
                    currentScreenName.value = BorrowLendAppScreen.USER
                    if (it.lifecycle.currentState == Lifecycle.State.RESUMED)
                        user.value?.let {
                            UserProfileScreen(
                                it,
                                navController = navController,
                                isEditingUserProfile = shouldEditUserProfile
                            )
                        }
                }

                composable(
                    "${BorrowLendAppScreen.REQUESTS.name}/{toolId}/{isSentByUser}",
                    arguments = listOf(navArgument("toolId") {
                        type = NavType.StringType
                        nullable = true
                    },
                        navArgument("isSentByUser") {
                            type = NavType.BoolType
                        })
                ) {
                    currentScreenName.value = BorrowLendAppScreen.REQUESTS
                    if (it.lifecycle.currentState == Lifecycle.State.RESUMED)
                        user.value?.let { loggedInUser ->
                            RequestsScreen(it.arguments?.getString("toolId"), loggedInUser, it.arguments?.getBoolean("isSentByUser"), navController)
                        }
                }

                composable(BorrowLendAppScreen.CONVERSATIONS.name) {
                    user.value?.let { loggedInUser ->
                        currentScreenName.value = BorrowLendAppScreen.CONVERSATIONS
                        ConversationsScreen(loggedInUser, navController)
                    }
                }

                composable("${BorrowLendAppScreen.CHAT.name}/{conversationId}/{requesterId}",
                    arguments = listOf(navArgument("conversationId") {
                        type = NavType.StringType
                    },
                        navArgument("requesterId") {
                            type = NavType.StringType
                        }
                    )
                ) {
                    user.value?.let { loggedInUser ->
                        if (it.lifecycle.currentState == Lifecycle.State.RESUMED)
                            it.arguments?.getString("conversationId")?.let { conversationId ->
                                it.arguments?.getString("requesterId")?.let { toUserId ->
                                    currentScreenName.value = BorrowLendAppScreen.CHAT
                                    ChatroomScreen(conversationId, loggedInUser, toUserId)
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun BorrowLendAppBar(
    currentScreenName: BorrowLendAppScreen,
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
                    user.value == null && currentScreenName.name == BorrowLendAppScreen.LOGIN.name -> {
                        null
                    }

                    user.value == null && currentScreenName.name == BorrowLendAppScreen.TOOLS.name -> {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }

                    user.value != null && currentScreenName.name == BorrowLendAppScreen.TOOLS.name -> {
                        IconButton(onClick = {
                            navController.navigate(BorrowLendAppScreen.USER.name)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile Btn"
                            )
                        }
                    }

                    currentScreenName.name == BorrowLendAppScreen.USER.name || currentScreenName.name == BorrowLendAppScreen.TOOL_DETAIL.name-> {
                        if (navController.currentBackStackEntry?.arguments?.getBoolean("isEditingUserProfile") == false)
                            IconButton(onClick = {
                                navController.popBackStack()
                            }) {
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
                    currentScreenName.title,
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W900
                )
            }
        }
    }
}
