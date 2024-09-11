package lend.borrow.tool.userprofile

import User
import android.app.Activity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import lend.borrow.tool.AddToolDialog
import lend.borrow.tool.BorrowLendAppScreen
import lend.borrow.tool.GlobalLoadingViewModel
import lend.borrow.tool.LoginViewModel
import lend.borrow.tool.R
import lend.borrow.tool.utility.DropDownMenu
import lend.borrow.tool.utility.GenericWarningDialog

@Composable
fun UserProfileScreen(
    loggedInUser: User,
    viewModel: GlobalLoadingViewModel = GlobalLoadingViewModel(),
    loginViewModel: LoginViewModel,
    navController: NavController,
    isEditingUserProfile: Boolean = false
) {
    val application = (LocalContext.current as Activity).application
    val userProfileViewModel: UserProfileViewModel =
        viewModel(key = UserProfileViewModel::class.java.name) {
            UserProfileViewModel(loggedInUser, application)
        }

    val uploadInProgress by userProfileViewModel.uploadInProgress.collectAsState()
    val user by userProfileViewModel.currentUser.collectAsState(loggedInUser) // if we have made it this far, the user should not be null. If it is null, then there is something wrong!

    val isEditing by userProfileViewModel.isEditingUserProfile.collectAsState(isEditingUserProfile)
    val openAddToolDialog = remember { mutableStateOf(false) }
    val openAddressRequiredDialog = remember {
        mutableStateOf( user!!.address.isEmpty())
    }
    val openDeleteAccountDialog = remember { mutableStateOf(false) }

    val userProfileUiState by userProfileViewModel.userProfileUiState.collectAsState()
    val scrollState = rememberScrollState()
    Column (Modifier.fillMaxSize()) {
        if (!isEditing)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                DropDownMenu(loggedInUser = loggedInUser, userProfileViewModel, navController)
            }
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isEditing) {
                Row(
                    Modifier
                        .wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Unavailable")
                    Switch(checked = userProfileUiState.isAvailable, onCheckedChange = {
                        userProfileViewModel.onAvailabilityUpdated(it)
                    })
                    Text(text = "Available")
                }
            }




            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Name: ",
                Modifier.fillMaxWidth(),
                fontWeight = FontWeight.ExtraBold
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing)
                    OutlinedTextField(
                        value = userProfileUiState.name,
                        label = {
                            Text("User name")
                        },
                        onValueChange = {
                            userProfileViewModel.onUserNameUpdated(it)
                        }
                    )
                else {
                    Text(text = "${userProfileUiState.name.ifEmpty { "Unknown user" }} (is ${if (user!!.availableAtTheMoment) "available" else "not available"})")
                }
            }

            Text(
                text = "Address: ",
                Modifier.fillMaxWidth(),
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (isEditing)
                    OutlinedTextField(
                        value = userProfileUiState.address,
                        label = {
                            Text("User address")
                        },
                        onValueChange = {
                            userProfileViewModel.onAddressUpdated(it)
                        }
                    )
                else {
                    Text(
                        text = userProfileUiState.address.ifEmpty { "Unknown" }, Modifier
                            .fillMaxWidth()
                    )
                }
            }


            Text(
                text = "Search radius: ",
                Modifier.fillMaxWidth(),
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                user?.let {
                    Column {
                        Text(
                            text = "${userProfileUiState.searchRadius} km", Modifier
                                .fillMaxWidth()
                        )
                        if (isEditing)
                            Slider(
                                value = userProfileUiState.searchRadius.toFloat(),
                                onValueChange = {
                                    userProfileViewModel.onSearchRadiusUpdated(it.toInt())
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.secondary,
                                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                                steps = 8,
                                valueRange = 1f..10f
                            )
                    }
                }
            }



            Text(
                text = "Email: ",
                Modifier.fillMaxWidth(),
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = user!!.email,
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
                text = user!!.subscription,
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            )

            if (isEditing.not()) {

                Spacer(modifier = Modifier.height(64.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    ClickableText(text = AnnotatedString("Sign out")) {
                        loginViewModel.signOut()
                        navController.navigate(BorrowLendAppScreen.LOGIN.name)
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            if (isEditing.not())
                Row(
                    Modifier.fillMaxSize(),
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
            else
                Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    onClick = {
                        userProfileViewModel.discardChangesInUserInfo()
                    }, colors = ButtonDefaults.buttonColors(
                        Color.Gray, Color.White
                    )
                ) {
                    Text(text = AnnotatedString("Discard changes"))
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    onClick = {
                        user?.let { loggedInUser ->
                            userProfileViewModel.geocodeAddress(
                                userProfileUiState.address,
                                loggedInUser
                            ) { geoPoint ->
                                if (geoPoint != null) {
                                    userProfileViewModel.updateUserInfo(
                                        loggedInUser.copy(
                                            address = userProfileUiState.address,
                                            name = userProfileUiState.name,
                                            searchRadius = userProfileUiState.searchRadius,
                                            geoPoint = geoPoint,
                                            latitude = geoPoint.latitude,
                                            longitude = geoPoint.longitude
                                        ),
                                        loggedInUser
                                    )
                                } else {
                                    userProfileViewModel.provideAddressMessage =
                                        application.getString(R.string.bad_address_message)
                                    openAddressRequiredDialog.value = true
                                }
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(
                        Color(LocalContext.current.resources.getColor(lend.borrow.tool.shared.R.color.primary)),
                        Color.White
                    )
                ) {
                    Text(text = AnnotatedString("Save changes"))
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    onClick = {
                        openDeleteAccountDialog.value = true
                    }, colors = ButtonDefaults.buttonColors(
                        Color.Red, Color.White
                    )
                ) {
                    Text(text = AnnotatedString("Delete my profile"))
                }
            }

        }
    }


    if (openAddressRequiredDialog.value) {
        GenericWarningDialog(
            userProfileViewModel.provideAddressMessage,
            onNegativeClick = {
                loginViewModel.signOut()
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
            viewModel.loadingInProgress()
            openAddToolDialog.value = false
            userProfileViewModel.uploadTool(toolName, toolDescription, tags = tags.toMutableList(), images = images.toMutableList(), ownerId = user!!.id)
        })
    else if (openDeleteAccountDialog.value)
        GenericWarningDialog(
            "Are you sure that you want to delete the account: \n${user!!.email}",
            positiveText = stringResource(R.string.delete),
            positiveColor = Color.Red,
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
                Text(text = stringResource(R.string.uploading),
                    fontWeight = FontWeight.Bold,
                    color = Color(
                        getColor(
                            LocalContext.current,
                            lend.borrow.tool.shared.R.color.primary
                        )
                    ))
            }
        }
    }
}
