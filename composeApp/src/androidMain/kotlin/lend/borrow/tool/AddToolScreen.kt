package lend.borrow.tool

import ToolToBeUploadedToFireBase
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.util.Objects
import java.util.UUID

@Composable
fun CustomDialogWithResult(
    onDismiss: () -> Unit,
    onNegativeClick: () -> Unit,
    onPositiveClick: (ToolToBeUploadedToFireBase) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val context = LocalContext.current
        var toolName by rememberSaveable { mutableStateOf("") }
        var toolDescription by rememberSaveable { mutableStateOf("") }
        var toolTags: String? by rememberSaveable { mutableStateOf(null) }
        var toolValue by rememberSaveable { mutableStateOf(0) }
        var takingPics by rememberSaveable { mutableStateOf(false) }
        val file = context.createImageFile()
        val uri = FileProvider.getUriForFile(
            Objects.requireNonNull(context),
            BuildConfig.APPLICATION_ID + ".provider", file
        )

        var capturedImageUri by remember {
            mutableStateOf<Uri?>(null)
        }

        val cameraLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
                if (it) {
                    takingPics = false
                    capturedImageUri = uri
                }
            }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
        val permissionCheckResult =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)


        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {

            Column(modifier = Modifier.padding(8.dp)) {

                Text(
                    text = if (takingPics) "Take a picture" else "Add tool",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))


                if (takingPics) {
                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(uri)
                    } else {
                        // Request a permission
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                Column {
                    Text(text = "Tool name:")
                    TextField(
                        value = toolName,
                        onValueChange = {
                            toolName = it
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Description:")
                    TextField(
                        value = toolDescription,
                        onValueChange = {
                            toolDescription = it
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Tags:")
                    TextField(
                        value = if (toolTags != null) toolTags!! else "",
                        onValueChange = {
                            toolTags = it
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Value:")
                    TextField(
                        value = toolValue.toString(),
                        onValueChange = {
                            if (it.isNotBlank())
                                toolValue = it.toInt()
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AsyncImage(
                                    model = if (capturedImageUri?.path?.isNotEmpty() == true) capturedImageUri else context.getDrawable(R.drawable.baseline_image_24),
                                    modifier = Modifier
                                        .padding(16.dp, 8.dp)
                                        .alpha(if (capturedImageUri?.path?.isNotEmpty() == true) 1f else 0.2f),
                                    contentDescription = null
                                )
                            }
                        IconButton(onClick = {
                            takingPics = true
                        }) {
                            Icon(
                                painter = painterResource(lend.borrow.tool.R.drawable.baseline_camera_alt_24),
                                contentDescription = "Profile Btn"
                            )
                        }
                    }

                }


                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    TextButton(onClick = onNegativeClick) {
                        Text(text = "CANCEL")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = {
                        onPositiveClick(
                            ToolToBeUploadedToFireBase(
                                toolName,
                                toolDescription,
                                tags = toolTags?.split(",") ?: emptyList(),
                                images = capturedImageUri?.let {
                                    listOf(it.toString())
                                }?: emptyList()
                            )
                        )
                    }) {
                        Text(text = "OK")
                    }
                }
            }
        }
    }
}

fun Context.createImageFile(): File {
    // Create an image file name
    val uuid = UUID.randomUUID().toString()
    val image = File.createTempFile(
        uuid,
        ".jpg",
        externalCacheDir
    )
    return image
}