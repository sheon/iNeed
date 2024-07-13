package lend.borrow.tool

import AddressRequiredWarningDialog
import BorrowLendAppScreen
import ToolInFireStore
import User
import android.app.Activity
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor
import androidx.navigation.NavController
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.GeoPoint
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.File
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun UserProfile(
    signedInUser: User,
    viewModel: GlobalLoadingViewModel = GlobalLoadingViewModel(),
    loginViewModel: LoginViewModel,
    navController: NavController,
    isEditingUserProfile: Boolean = false
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val db: FirebaseFirestore = Firebase.firestore
    val dbTools: CollectionReference = db.collection("Tools")

    var isEditingUserProfile by remember {
        mutableStateOf(isEditingUserProfile)
    }

    val userViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        UserViewModel((context as Activity).application)
    }
    val user by userViewModel.currentUser.collectAsState(signedInUser) // if we have made it this far, the user should not be null. If it is null, then there is something wrong!


    val openAddToolDialog = remember { mutableStateOf(false) }
    val openAddressRequiredDialog = remember {
        mutableStateOf( user!!.address.isEmpty())
    }
    var userName by remember {
        mutableStateOf(user!!.name)
    }

    var userAddress by remember {
        mutableStateOf(user!!.address)
    }


    var userAvailability: Boolean by remember {
        mutableStateOf(user!!.availableAtTheMoment)
    }
    if (state.loading)
        CircularProgressIndicator(modifier = Modifier.wrapContentSize())
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
                                    geoPoint = it
                                )
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

        Spacer(modifier = Modifier.height(64.dp))

        if (isEditingUserProfile.not())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ClickableText(text = AnnotatedString("Sign out")) {
                    loginViewModel.onSignOut()
                    navController.navigate(BorrowLendAppScreen.LOGIN.name)
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
        AddressRequiredWarningDialog(
            userViewModel.provideAddressMessage,
            onNegativeClick = {
                loginViewModel.onSignOut()
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
            val tempTool = ToolInFireStore(toolName, "", toolDescription, tags = tags.toMutableList(), images = images.toMutableList(), owner = user!!.id)
            GlobalScope.launch {// The scope should be fixed later
                uploadTool(tempTool, dbTools, viewModel)
            }

        })
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
suspend fun uploadTool(
    tool: ToolInFireStore,
    dbTools: CollectionReference,
    viewModel: GlobalLoadingViewModel
) {
    val storage = Firebase.storage
    val uploadedImagesNameWithSuffix = mutableListOf<String>()
    tool.images.forEach { imageUUID ->
        val imageNameWithSuffix = "${UUID.randomUUID()}.png"
        val path = "tools/$imageNameWithSuffix"
        val imageRef = storage.reference(path)
        imageRef.putFile(File(Uri.parse(imageUUID)))
    }
    tool.images.clear()
    tool.images.addAll(uploadedImagesNameWithSuffix)
    dbTools.add(tool)
    viewModel.loadingFinished()
}