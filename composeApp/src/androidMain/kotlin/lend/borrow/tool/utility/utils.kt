package lend.borrow.tool.utility

import ToolDetailUiState
import ToolInApp
import ToolInFireStore
import User
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lend.borrow.tool.ToolsRepository
import lend.borrow.tool.UserRepository
import lend.borrow.tool.shared.R

suspend fun ToolInFireStore.toToolInApp(
    owner: User,
    userRepo: UserRepository,
    toolsRepo: ToolsRepository
): ToolInApp { // This may be avoided if we have one data class for tool.
    return ToolInApp(name = name, id = id, description = description, imageRefUrlMap = imageReferences.associateWith {
        toolsRepo.storage.reference(it).getDownloadUrl()
    }, tags = tags, available = available, owner = owner, borrower = borrower?.let { userRepo.getUserInfo(it) }, instruction = instruction)
}

fun ToolInApp.toToolInFireStore(): ToolInFireStore { // This may be avoided if we have one data class for tool.
    return ToolInFireStore(id = id, name = name, description = description, imageReferences = imageRefUrlMap.keys.toList(), tags = tags, available = available, owner = owner.id, borrower = borrower?.id, instruction = instruction)
}

fun ToolDetailUiState.toToolInApp() = defaultTool.copy(name = name, description = description?.ifEmpty { null }, instruction = instruction?.ifEmpty { null }, imageRefUrlMap = images, tags = tags).also {
    it.newImages.addAll(this.newImages)
    it.deletedImages.addAll(this.deletedImages)
}
fun ToolInApp.toToolDetailUi() = ToolDetailUiState(id = id, name = name, description = description , instruction = instruction, images = imageRefUrlMap, tags = tags.toMutableList(), owner = owner, borrower = borrower, isAvailable =available, defaultTool = this)

fun GeoPoint.distanceToOtherPoint(point: GeoPoint): Float {
    val user1 = Location("user1")
    user1.latitude = latitude
    user1.longitude = longitude
    val user2 = Location("user2")
    user2.latitude = point.latitude
    user2.longitude = point.longitude
    return user1.distanceTo(user2)/1000 // The return unit should be Kilometer
}

@Composable
fun GenericAlertDialog(
    message: String,
    positiveText: String = stringResource(lend.borrow.tool.R.string.ok),
    positiveColor: Color = LocalContext.current.primaryColor,
    onNegativeClick: () -> Unit = {},
    onPositiveClick: () -> Unit = {}
) {
    Dialog(onDismissRequest = {},
        DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(15.dp)) {
                Text(modifier = Modifier.padding(5.dp),
                    text = message,
                    textAlign = TextAlign.Justify)
                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    CustomButton(text = "Cancel", color = Color.Gray, onClick = onNegativeClick)

                    Spacer(modifier = Modifier.width(4.dp))

                    CustomButton (text = positiveText, color = positiveColor, filled = true) {
                        onPositiveClick()
                    }
                }
            }
        }
    }
}

@Composable
fun GenericWarningDialog(
    message: String,
    positiveText: String = stringResource(lend.borrow.tool.R.string.ok),
    positiveColor: Color = LocalContext.current.warningColor,
    onNegativeClick: () -> Unit = {},
    onPositiveClick: () -> Unit = {}
) {
    Dialog(onDismissRequest = {},
        DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(15.dp)) {
                Text(modifier = Modifier.padding(5.dp),
                    text = message,
                    textAlign = TextAlign.Justify)
                // Buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    CustomButton(text = "Cancel", color = Color.Gray, onClick = onNegativeClick)


                    CustomButton (text = positiveText, color = positiveColor) {
                        onPositiveClick()
                    }
                }
            }
        }
    }
}

fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
fun getCurrentLocation(context: Context, callback: suspend (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val long = location.longitude
                CoroutineScope(Dispatchers.IO).launch {
                    callback(lat, long)
                }
            }
        }
        .addOnFailureListener { exception ->
            exception.printStackTrace()
        }
}


@Composable
fun CustomDialogWithResult(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss,
        DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        content
    )
}


val Context.primaryColor: Color
    get() = Color(this.getColor(R.color.primary))

val Context.secondaryColor: Color
    get() = Color(this.getColor(R.color.secondary))

val Context.warningColor: Color
    get() = Color(this.getColor(android.R.color.holo_red_light))