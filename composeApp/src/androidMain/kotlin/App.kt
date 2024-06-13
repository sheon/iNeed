
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import lend.borrow.tool.RegisteredToolsScreen
import lend.borrow.tool.UserProfile
import org.jetbrains.compose.ui.tooling.preview.Preview


enum class BorrowLendAppScreen(val title: String, modifier: Modifier = Modifier) {
    ITEMS(title = "Items to borrow"),
    USER(title = "User profile")
}


@Composable
@Preview
fun BorrowLendApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = BorrowLendAppScreen.valueOf(
        backStackEntry?.destination?.route ?: BorrowLendAppScreen.ITEMS.name
    )
    MaterialTheme {

        Scaffold(
            topBar = {
                BorrowLendAppBar(
                    currentScreen = currentScreen,
                    canNavigateBack = navController.previousBackStackEntry != null,
                    navController = navController
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BorrowLendAppScreen.ITEMS.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable("ITEMS") {
                    RegisteredToolsScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 15.dp)
                    )
                }
                composable("USER") {
                    UserProfile(
                        user1,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BorrowLendAppBar(
    currentScreen: BorrowLendAppScreen,
    canNavigateBack: Boolean,
    navController: NavController
) {
    TopAppBar(
        title = { Text(currentScreen.title) },
        backgroundColor = Color.Green,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            } else
                IconButton(onClick = {
                    navController.navigate("user")
                }) {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile Btn")
                }
        }
    )
}


@androidx.compose.ui.tooling.preview.Preview
@Composable
fun AppAndroidPreview() {
    BorrowLendApp(navController = rememberNavController())
}

val user1 = User("Ehsan", "Bagaregate 23, Nykopping", true)
val user2 = User("Jack", "Bagaregate 21, Nykopping", false)


//        listOf(
//            ToolToBeCreated(
//                "drilling machine",
//                "It has hammer function",
//                listOf(
//                    R.drawable.hammer_drill,
//                    R.drawable.hammer_drill_usecase1,
//                    R.drawable.hammer_drill_parts
//                ),
//                tags = listOf("Electronic", "Heavy duty"),
//                //owner = user1
//            ),
//            ToolToBeCreated(
//                "Handsaw",
//                "It is a classic wood saw",
//                listOf(
//                    R.drawable.saw,
//                    R.drawable.saw_wood_usecase1,
//                    R.drawable.saw_woods_usecase2
//                ),
//                tags = listOf("Manual"),
//                //owner = user2
//            ),
//            ToolToBeCreated(
//                "Hammer",
//                "To drive/draw a nail into/from a wall or wood.",
//                listOf(
//                    R.drawable.hammer,
//                    R.drawable.hammer_usecase1
//                ),
//                tags = listOf("Manual"),
//                //owner = user1
//            )
//        )
