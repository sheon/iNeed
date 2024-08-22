package lend.borrow.tool.utility

import ToolInApp
import ToolInFireStore
import User
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.gitlive.firebase.firestore.GeoPoint
import lend.borrow.tool.UserRepository
import lend.borrow.tool.shared.R

suspend fun ToolInFireStore.toToolInApp(owner: User, userRepo: UserRepository): ToolInApp { // This may be avoided if we have one data class for tool.
    return ToolInApp(name, "", description, imageReferences, imageUrls, tags, available, owner, borrower?.let { userRepo.getUserInfo(it) })
}

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
fun GenericWarningDialog(
    message: String,
    positiveText: String = stringResource(lend.borrow.tool.R.string.ok),
    positiveColor: Color = Color(LocalContext.current.getColor(R.color.primary)),
    onNegativeClick: () -> Unit = {},
    onPositiveClick: () -> Unit = {}
) {
    Dialog(onDismissRequest = {},
        DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = Color.LightGray
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

                    TextButton(modifier = Modifier.padding(5.dp),
                        colors = ButtonDefaults.buttonColors(Color.LightGray, Color.Black),
                        shape = RoundedCornerShape(2.dp),
                        onClick = onNegativeClick) {
                        Text(text = "CANCEL")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(modifier = Modifier.padding(5.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.textButtonColors(
                            positiveColor,
                            Color.White
                        ),
                        onClick = {
                            onPositiveClick()
                        }) {
                        Text(text = positiveText)
                    }
                }
            }
        }
    }
}