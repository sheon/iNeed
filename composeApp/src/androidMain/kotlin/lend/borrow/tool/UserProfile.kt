package lend.borrow.tool

import BorrowLendAppScreen
import Tool
import User
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    navController: NavController
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val openDialog = remember { mutableStateOf(false) }
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val dbTools: CollectionReference = db.collection("Tools")
    if (state.loading)
        CircularProgressIndicator(modifier = Modifier.wrapContentSize())
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${user.name} (is ${if (user.availableAtTheMoment) "available" else "not available"})")
            Switch(checked = user.availableAtTheMoment, onCheckedChange = {
                user.availableAtTheMoment = it
            })
        }
        Text(
            text = user.address, Modifier
                .fillMaxWidth()
                .padding(15.dp)
        )
        Text(
            text = user.subscription,
            Modifier
                .fillMaxWidth()
                .padding(15.dp)
        )

        ClickableText(text = AnnotatedString("Sign out")) {
            loginViewModel.onSignOut()
            navController.navigate(BorrowLendAppScreen.LOGIN.name)
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