package lend.borrow.tool.utility

import ToolInApp
import ToolInFireStore
import User
import android.location.Location
import dev.gitlive.firebase.firestore.GeoPoint
import lend.borrow.tool.UserRepository
import kotlin.math.roundToInt

suspend fun ToolInFireStore.toToolInApp(owner: User, userRepo: UserRepository): ToolInApp {
    return ToolInApp(name, id, description, images, tags, available, owner, borrower?.let { userRepo.getUserInfo(it) })
}

fun GeoPoint.distanceToOtherPoint(point: GeoPoint): Int {
    val user1 = Location("user1")
    user1.latitude = latitude
    user1.longitude = longitude
    val user2 = Location("user2")
    user2.latitude = point.latitude
    user2.longitude = point.longitude
    return user1.distanceTo(user2).roundToInt()/1000 // The return unit should be Kilometer
}