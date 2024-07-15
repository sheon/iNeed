package lend.borrow.tool

import BorrowLendAppScreen
import User
import android.app.Activity
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor
import androidx.navigation.NavController
import dev.gitlive.firebase.firestore.GeoPoint
import lend.borrow.tool.utility.GenericWarningDialog

@Composable
fun UserProfile(
    signedInUser: User,
    viewModel: GlobalLoadingViewModel = GlobalLoadingViewModel(),
    loginViewModel: LoginViewModel,
    navController: NavController,
    isEditingUserProfile: Boolean = false
) {
    val context = LocalContext.current

    var isEditingUserProfile by remember {
        mutableStateOf(isEditingUserProfile)
    }

    val userViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        UserViewModel((context as Activity).application)
    }
    val uploadInProgress by userViewModel.uploadInProgress.collectAsState()
    val user by userViewModel.currentUser.collectAsState(signedInUser) // if we have made it this far, the user should not be null. If it is null, then there is something wrong!


    val openAddToolDialog = remember { mutableStateOf(false) }
    val openAddressRequiredDialog = remember {
        mutableStateOf( user!!.address.isEmpty())
    }
    val openDeleteAccountDialog = remember { mutableStateOf(false) }

    var userName by remember {
        mutableStateOf(user!!.name)
    }

    var userAddress by remember {
        mutableStateOf(user!!.address)
    }


    var userAvailability: Boolean by remember {
        mutableStateOf(user!!.availableAtTheMoment)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(15.dp)
    ) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isEditingUserProfile.not()) Arrangement.SpaceBetween else Arrangement.End ) {
                if (isEditingUserProfile.not()) {
                    Switch(checked = userAvailability, onCheckedChange = {
                        userAvailability = it
                    })
                    IconButton(onClick = {
                        isEditingUserProfile = !isEditingUserProfile
                    }) {
                        Column {
                            Icon(Icons.Filled.Edit, "Edit user profile data.")
                            Text(text = "Edit")
                        }

                    }
                } else {
                    IconButton(onClick = {
                        val geoPoint = when {
                            userAddress.isNotEmpty() && userAddress.equals(user!!.address, true).not() || user!!.geoPoint == null -> {
                                Geocoder(context).getFromLocationName(userAddress, 1)?.let {
                                    if (it.size != 0)
                                        GeoPoint(it.first().latitude, it.first().longitude)
                                    else {
                                        userAddress = ""
                                        null
                                    }
                                }
                            }

                            userAddress.equals(
                                user!!.address,
                                true
                            ) -> user!!.geoPoint // No need to run the geoCoder again
                            else -> null
                        }

                        geoPoint?.let {
                            userViewModel.updateUserInfo(
                                user!!.copy(
                                    address = userAddress,
                                    name = userName,
                                    geoPoint = it,
                                    latitude = it.latitude,
                                    longitude = it.longitude
                                ),
                                user!!
                            )
                            isEditingUserProfile = !isEditingUserProfile
                        } ?: run {
                            userViewModel.provideAddressMessage =
                                context.getString(R.string.bad_address_message)
                            openAddressRequiredDialog.value = true
                        }


                    }) {
                        Column {
                            Icon(Icons.Filled.Done, stringResource(R.string.save_user_profile_data))
                            Text(text = "Save")
                        }

                    }
                }
            }



        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Name",
            Modifier.fillMaxWidth(),
            fontWeight = FontWeight.ExtraBold)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditingUserProfile)
                OutlinedTextField(
                    value = userName,
                    label = {
                        Text("User name")
                    },
                    onValueChange = {
                        userName = it //This update should be handled by updating the user in the repository
                    }
                )
            else {
                Text(text = "${userName.ifEmpty { "Unknown user" }} (is ${if (user!!.availableAtTheMoment) "available" else "not available"})")
            }
        }

        Text(text = "Address",
            Modifier.fillMaxWidth(),
            fontWeight = FontWeight.ExtraBold)

        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (isEditingUserProfile)
                OutlinedTextField(
                    value = userAddress,
                    label = {
                        Text("User address")
                    },
                    onValueChange = {
                        userAddress = it //This update should be handled by updating the user in the repository
                    }
                )
            else {
                Text(
                    text = userAddress.ifEmpty { "Unknown" }, Modifier
                        .fillMaxWidth()
                )
            }
        }

        Text(text = "Email",
            Modifier.fillMaxWidth(),
            fontWeight = FontWeight.ExtraBold)
        Text(
            text = user!!.email,
            Modifier
                .fillMaxWidth()
                .padding(10.dp)
        )

        Text(text = "Subscription",
            Modifier.fillMaxWidth(),
            fontWeight = FontWeight.ExtraBold)

        Text(
            text = user!!.subscription,
            Modifier
                .fillMaxWidth()
                .padding(10.dp)
        )

        if (isEditingUserProfile.not()) {

            Spacer(modifier = Modifier.height(64.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ClickableText(text = AnnotatedString("Sign out")) {
                    loginViewModel.signOut()
                    navController.navigate(BorrowLendAppScreen.LOGIN.name)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ClickableText(text = AnnotatedString("Delete account"), style = TextStyle(color = Color.Red, fontWeight = FontWeight.ExtraBold)) {
                    openDeleteAccountDialog.value = true
                }
            }
        }



        Row (Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom) {
            SmallFloatingActionButton(
                onClick = {
                    openAddToolDialog.value = true
                },
                Modifier
                    .wrapContentSize(),
                containerColor = Color(getColor(LocalContext.current, lend.borrow.tool.shared.R.color.primary))
            ) {
                Row(
                    Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(text = "Add tool", color = Color.White)
                    Icon(Icons.Filled.Add, "Small floating action button.", tint = Color.White)
                }
            }
        }

    }

    if (openAddressRequiredDialog.value) {
        GenericWarningDialog(
            userViewModel.provideAddressMessage,
            onNegativeClick = {
                loginViewModel.signOut()
                openAddressRequiredDialog.value = false
            },
            onPositiveClick = {
                openAddressRequiredDialog.value = false
            })
    } else if (openAddToolDialog.value)
        CustomDialogWithResult({
            openAddToolDialog.value = false
        }, {
            openAddToolDialog.value = false
        }, { toolName, toolDescription, tags, images ->
            viewModel.loadingInProgress()
            openAddToolDialog.value = false
            userViewModel.uploadTool(toolName, toolDescription, tags = tags.toMutableList(), images = images.toMutableList(), ownerId = user!!.id)
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
                loginViewModel.deleteAccount(user!!)
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
