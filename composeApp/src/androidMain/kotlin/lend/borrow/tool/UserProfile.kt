package lend.borrow.tool

import BorrowLendAppScreen
import Tool
import User
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun UserProfile(
    user: User,
    viewModel: GlobalLoadingViewModel = GlobalLoadingViewModel(),
    loginViewModel: LoginViewModel,
    navController: NavController,
    isEditingUserProfile: Boolean = false
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val openDialog = remember { mutableStateOf(false) }
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val dbTools: CollectionReference = db.collection("Tools")

    var isEditingUserProfile by remember {
        mutableStateOf(isEditingUserProfile)
    }

    var userName by remember {
        mutableStateOf(user.name)
    }

    var userAddress by remember {
        mutableStateOf(user.address)
    }

    val userViewModel by lazy {
        UserViewModel((context as Activity).application)
    }

    var userAvailability: Boolean by remember {
        mutableStateOf(user.availableAtTheMoment)
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
                        user.availableAtTheMoment = it
                        userViewModel.updateUserInfo(user)
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
                        // Save the changes
                        isEditingUserProfile = !isEditingUserProfile
                    }) {
                        Column {
                            Icon(Icons.Filled.Done, "Save user profile data.")
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
                        user.name = it
                        userViewModel.updateUserInfo(user)
                        userName = it //This update should be handled by updating the user in the repository
                    }
                )
            else {
                Text(text = "${user.name.ifEmpty { "Unknown user" }} (is ${if (user.availableAtTheMoment) "available" else "not available"})")
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
                        user.address = it
                        userViewModel.updateUserInfo(user)
                        userAddress = it //This update should be handled by updating the user in the repository
                    }
                )
            else {
                Text(
                    text = user.address.ifEmpty { "Unknown" }, Modifier
                        .fillMaxWidth()
                )
            }
        }

        Text(text = "Email",
            Modifier.fillMaxWidth(),
            fontWeight = FontWeight.ExtraBold)
        Text(
            text = user.email,
            Modifier
                .fillMaxWidth()
                .padding(10.dp)
        )

        Text(text = "Subscription",
            Modifier.fillMaxWidth(),
            fontWeight = FontWeight.ExtraBold)

        Text(
            text = user.subscription,
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
                    openDialog.value = true
                },
                Modifier
                    .wrapContentSize()
            ) {
                Row(
                    Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(text = "Add tool")
                    Icon(Icons.Filled.Add, "Small floating action button.")
                }
            }
        }

    }


    if (openDialog.value)
        CustomDialogWithResult({
            openDialog.value = false
        }, {
            openDialog.value = false
        }, { toolName, toolDescription, tags, images ->
            viewModel.loadingInProgress()
            openDialog.value = false
            val tempTool = Tool(toolName, "", toolDescription, tags = tags.toMutableList(), images = images.toMutableList(), owner = user.id)
            GlobalScope.launch {// The scope should be fixed later
                uploadTool(tempTool, dbTools, context, viewModel)
            }

        })
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
suspend fun uploadTool(
    tool: Tool,
    dbTools: CollectionReference,
    context: Context,
    viewModel: GlobalLoadingViewModel
) {
    val storage = Firebase.storage
    val uploadedImagesNameWithSuffix = mutableListOf<String>()
    tool.images.forEach { imageUUID ->
        context.contentResolver.openInputStream(Uri.parse(imageUUID))?.use {
            val data = it.readAllBytes()
            val imageNameWithSuffix = "${UUID.randomUUID()}.png"
            val path = "tools/$imageNameWithSuffix"
            val imageRef = storage.getReference(path)
            val uploadTask = imageRef.putBytes(data)
            uploadTask.continueWithTask {
                if (!it.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Fail to upload the image \n$it",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                imageRef.downloadUrl
            }.addOnSuccessListener { url ->
                uploadedImagesNameWithSuffix.add(url.toString())
                Toast.makeText(
                    context,
                    "Image uploaded successfully ${url}",
                    Toast.LENGTH_SHORT
                ).show()
            }.await()
            uploadTask.await()
        }
    }
    tool.images.clear()
    tool.images.addAll(uploadedImagesNameWithSuffix)
    dbTools.add(tool).addOnSuccessListener {
        // after the data addition is successful
        // we are displaying a success toast message.
        Toast.makeText(
            context,
            "Your tool has been added to Firebase Firestore",
            Toast.LENGTH_SHORT
        ).show()
        viewModel.loadingFinished()
    }.addOnFailureListener { e ->
        Toast.makeText(context, "Fail to add course \n$e", Toast.LENGTH_SHORT)
            .show()
        viewModel.loadingFinished()
    }
}