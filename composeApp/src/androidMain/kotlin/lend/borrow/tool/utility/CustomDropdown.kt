package lend.borrow.tool.utility

import User
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import lend.borrow.tool.BorrowLendAppScreen
import lend.borrow.tool.ToolDetailViewModel
import lend.borrow.tool.userprofile.UserProfileViewModel

@Composable
fun DropDownMenu(toolId: String?, toolDetailViewModel: ToolDetailViewModel, navController: NavController, isOwner: Boolean) {
    val requestSentForThisTool by toolDetailViewModel.requestsReceivedForThisTool.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (requestSentForThisTool.isNotEmpty())
                DropdownMenuItem(
                    text = { Text("Received requests") },
                    onClick = {
                        navController.navigate("${BorrowLendAppScreen.REQUESTS.name}/${toolId}/${false}")
                        expanded = false
                    },
                    trailingIcon = {
                        if (requestSentForThisTool.filter { !it.isRead }.isNotEmpty())
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.Red, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${requestSentForThisTool.filter { !it.isRead }.size}",
                                    color = Color.White
                                )
                            }
                    }
                )
            DropdownMenuItem(
                text = { Text("Conversations") },
                onClick = {
                    navController.navigate(BorrowLendAppScreen.REQUESTS.name)
                    expanded = false
                }
            )
            if (isOwner)
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        toolDetailViewModel.isEditingToolInfo.value = true
                        expanded = false
                    }
                )
        }
    }
}

@Composable
fun DropDownMenu(loggedInUser: User, userProfileViewModel: UserProfileViewModel, navController: NavController) {
    val requestSentByThisUser by userProfileViewModel.requestsSentByThisUser.collectAsState()
    val requestSentToThisUser by userProfileViewModel.requestsSentToThisUser.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val tool = null
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (requestSentByThisUser.isNotEmpty())
                DropdownMenuItem(
                    text = { Text("Sent requests") },
                    onClick = {
                        navController.navigate("${BorrowLendAppScreen.REQUESTS.name}/${tool}/${true}")
                        expanded = false
                    }
                )

            if (requestSentToThisUser.isNotEmpty())
                DropdownMenuItem(
                    text = { Text("Received requests") },
                    onClick = {
                        navController.navigate("${BorrowLendAppScreen.REQUESTS.name}/${tool}/${false}")
                        expanded = false
                    },
                    trailingIcon = {
                        if (requestSentToThisUser.values.filter { !it.isRead }.isNotEmpty())
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color.Red, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${requestSentToThisUser.values.filter { !it.isRead }.size}",
                                    color = Color.White
                                )
                            }
                    }
                )

            DropdownMenuItem(
                text = { Text("Conversations") },
                onClick = {
                    navController.navigate(BorrowLendAppScreen.REQUESTS.name)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    userProfileViewModel.isEditingUserProfile.value = true
                    expanded = false
                }
            )
        }
    }
}