package lend.borrow.tool.userprofile

import User
import android.app.Activity
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import lend.borrow.tool.AddToolDialog
import lend.borrow.tool.BorrowLendAppScreen
import lend.borrow.tool.R
import lend.borrow.tool.utility.CustomButton
import lend.borrow.tool.utility.DropDownMenu
import lend.borrow.tool.utility.GenericAlertDialog
import lend.borrow.tool.utility.GenericWarningDialog
import lend.borrow.tool.utility.WarningButton
import lend.borrow.tool.utility.primaryColor
import lend.borrow.tool.utility.secondaryColor

@Composable
fun UserProfileScreen(
    loggedInUser: User,
    navController: NavController,
    isEditingUserProfile: Boolean = false
) {
    val application = (LocalContext.current as Activity).application
    val userProfileViewModel: UserProfileViewModel =
        viewModel(key = UserProfileViewModel::class.java.name) {
            UserProfileViewModel(loggedInUser, application)
        }

    LaunchedEffect(Unit) {
        userProfileViewModel.initiateUserProfileViewmodel()
    }

    val isEditing by userProfileViewModel.isEditingUserProfile.collectAsState(isEditingUserProfile)

    if (isEditing)
        EditingUserProfileScreen(
            loggedInUser = loggedInUser,
            userProfileViewModel = userProfileViewModel,
            navController = navController,
            application = application
        )
    else
        StaticUserProfileScreen(
            loggedInUser = loggedInUser,
            userProfileViewModel = userProfileViewModel,
            navController = navController
        )

}

@Composable
fun EditingUserProfileScreen(
    loggedInUser: User,
    userProfileViewModel: UserProfileViewModel,
    navController: NavController,
    application: Application
) {

    val uploadInProgress by userProfileViewModel.uploadInProgress.collectAsState()
    val user by userProfileViewModel.currentUser.collectAsState(loggedInUser) // if we have made it this far, the user should not be null. If it is null, then there is something wrong!

    val openAddToolDialog = remember { mutableStateOf(false) }
    val openAddressRequiredDialog = remember {
        mutableStateOf(user!!.address.isEmpty())
    }
    val openDeleteAccountDialog = remember { mutableStateOf(false) }

    val userProfileUiState by userProfileViewModel.userProfileUiState.collectAsState()
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(15.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier
                    .wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Unavailable")
                Switch(checked = userProfileUiState.isAvailable,
                    colors = SwitchDefaults.colors(
                        LocalContext.current.primaryColor,
                        LocalContext.current.secondaryColor
                    ),
                    onCheckedChange = {
                    userProfileViewModel.onAvailabilityUpdated(it)
                })
                Text(text = "Available")
            }


            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Name: ",
                Modifier.fillMaxWidth(),
                fontWeight = FontWeight.ExtraBold
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                value = userProfileUiState.name,
                label = {
                    Text("User name")
                },
                onValueChange = {
                    userProfileViewModel.onUserNameUpdated(it)
                }
            )


            Text(
                text = "Address: ",
                Modifier.fillMaxWidth(),
                fontWeight = FontWeight.ExtraBold
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                value = userProfileUiState.address,
                label = {
                    Text("User address")
                },
                onValueChange = {
                    userProfileViewModel.onAddressUpdated(it)
                }
            )

            Text(
                text = "Search radius: ",
                Modifier.fillMaxWidth(),
                fontWeight = FontWeight.ExtraBold
            )

            user?.let {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(
                        text = "${userProfileUiState.searchRadius} km", Modifier
                            .fillMaxWidth()
                    )
                    Slider(
                        value = userProfileUiState.searchRadius.toFloat(),
                        onValueChange = {
                            userProfileViewModel.onSearchRadiusUpdated(it.toInt())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = LocalContext.current.primaryColor,
                            activeTrackColor = LocalContext.current.secondaryColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        steps = 8,
                        valueRange = 1f..10f
                    )
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CustomButton(
                modifier = Modifier.fillMaxWidth(),
                "Save changes",
                onClick = {
                    user?.let { loggedInUser ->
                        userProfileViewModel.geocodeAddress(
                            userProfileUiState.address,
                            loggedInUser
                        ) { geoPoint ->
                            if (geoPoint != null) {
                                userProfileViewModel.updateUserInfo(geoPoint)
                            } else {
                                userProfileViewModel.provideAddressMessage =
                                    application.getString(R.string.bad_address_message)
                                openAddressRequiredDialog.value = true
                            }
                        }
                    }
                },
                filled = true
            )

            CustomButton(
                modifier = Modifier.fillMaxWidth(),
                "Discard changes",
                onClick = {
                    userProfileViewModel.discardChangesInUserInfo()
                },
                color = Color.Gray
            )
            WarningButton(Modifier.fillMaxWidth(), "Delete my profile") {
                openDeleteAccountDialog.value = true
            }
        }
    }


    if (openAddressRequiredDialog.value) {
        GenericAlertDialog(
            userProfileViewModel.provideAddressMessage,
            onNegativeClick = {
                userProfileViewModel.signOut()
                openAddressRequiredDialog.value = false
            },
            onPositiveClick = {
                openAddressRequiredDialog.value = false
            })
    } else if (openAddToolDialog.value)
        AddToolDialog({
            openAddToolDialog.value = false
        }, {
            openAddToolDialog.value = false
        }, { toolName, toolDescription, tags, images ->
            openAddToolDialog.value = false
            userProfileViewModel.uploadTool(
                toolName,
                toolDescription,
                tags = tags.toMutableList(),
                images = images.toMutableList(),
                ownerId = user!!.id
            )
        })
    else if (openDeleteAccountDialog.value)
        GenericWarningDialog(
            "Are you sure that you want to delete the account: \n${user!!.email}",
            positiveText = stringResource(R.string.delete),
            onNegativeClick = {
                openDeleteAccountDialog.value = false
            },
            onPositiveClick = {
                userProfileViewModel.deleteUser(loggedInUser)
                openDeleteAccountDialog.value = false
                navController.navigate(BorrowLendAppScreen.LOGIN.name)
            })

    if (uploadInProgress) {
        Box(Modifier
            .fillMaxSize()
            .background(Color.LightGray.copy(alpha = 0.8f))
            .clickable(
                indication = null, // disable ripple effect
                interactionSource = remember { MutableInteractionSource() },
                onClick = { }
            ), // This should prevent the layer under from catching the press event
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.wrapContentSize(),
                    color = Color(
                        getColor(
                            LocalContext.current,
                            lend.borrow.tool.shared.R.color.primary
                        )
                    )
                )
                Text(
                    text = stringResource(R.string.uploading),
                    fontWeight = FontWeight.Bold,
                    color = Color(
                        getColor(
                            LocalContext.current,
                            lend.borrow.tool.shared.R.color.primary
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun StaticUserProfileScreen(
    loggedInUser: User,
    userProfileViewModel: UserProfileViewModel,
    navController: NavController
) {
    val uploadInProgress by userProfileViewModel.uploadInProgress.collectAsState()
    val openAddToolDialog = remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.End
        ) {

            DropDownMenu(userProfileViewModel, navController)

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(15.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Name: ",
                    Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "${loggedInUser.name.ifEmpty { "Unknown user" }} (is ${if (loggedInUser.availableAtTheMoment) "available" else "not available"})",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )

                Text(
                    text = "Address: ",
                    Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = loggedInUser.address.ifEmpty { "Unknown" },
                    Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )

                Text(
                    text = "Search radius: ",
                    Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "${loggedInUser.searchRadius} km", Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )

                Text(
                    text = "Email: ",
                    Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = loggedInUser.email,
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )

                Text(
                    text = "Subscription: ",
                    Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = loggedInUser.subscription,
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )

            }
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(15.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            SmallFloatingActionButton(
                onClick = {
                    openAddToolDialog.value = true
                },
                Modifier
                    .wrapContentSize(),
                containerColor = Color(
                    getColor(
                        LocalContext.current,
                        lend.borrow.tool.shared.R.color.primary
                    )
                )
            ) {
                Row(
                    Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(text = "Add tool", color = Color.White)
                    Icon(
                        Icons.Filled.Add,
                        "Small floating action button.",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (openAddToolDialog.value)
        AddToolDialog({
            openAddToolDialog.value = false
        }, {
            openAddToolDialog.value = false
        }, { toolName, toolDescription, tags, images ->
            openAddToolDialog.value = false
            userProfileViewModel.uploadTool(
                toolName,
                toolDescription,
                tags = tags.toMutableList(),
                images = images.toMutableList(),
                ownerId = loggedInUser.id
            )
        })

    if (uploadInProgress) {
        Box(Modifier
            .fillMaxSize()
            .background(Color.LightGray.copy(alpha = 0.8f))
            .clickable(
                indication = null, // disable ripple effect
                interactionSource = remember { MutableInteractionSource() },
                onClick = { }
            ), // This should prevent the layer under from catching the press event
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.wrapContentSize(),
                    color = Color(
                        getColor(
                            LocalContext.current,
                            lend.borrow.tool.shared.R.color.primary
                        )
                    )
                )
                Text(
                    text = stringResource(R.string.uploading),
                    fontWeight = FontWeight.Bold,
                    color = Color(
                        getColor(
                            LocalContext.current,
                            lend.borrow.tool.shared.R.color.primary
                        )
                    )
                )
            }
        }
    }
}