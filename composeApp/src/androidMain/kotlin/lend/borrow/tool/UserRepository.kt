package lend.borrow.tool

import User
import android.app.Application
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.GeoPoint
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import lend.borrow.tool.utility.distanceToOtherPoint
import kotlin.math.cos


class UserRepository(val application: Application) {
    private val authService: AuthenticationService = AuthenticationService(
        Firebase.auth
    )

    companion object {
    private lateinit var instance: UserRepository
        fun getInstance(application: Application): UserRepository {
            if (::instance.isInitialized.not()) {
                instance = UserRepository(application)
            }
            return instance
        }
    }

    val db: FirebaseFirestore = Firebase.firestore
    val dbUsers: CollectionReference = db.collection("Users")

    private val _currentUser = MutableStateFlow(runBlocking { getCurrentUser() })
    val currentUser = _currentUser.asStateFlow()
    suspend fun createUser(email: String, password: String) {
        authService.createUser(email, password).user?.let {
            val signedUpUser = User(
                it.uid,
                "",
                "",
                geoPoint = GeoPoint(0.0, 0.0)
            )
            dbUsers.document(it.uid).set(signedUpUser)
            _currentUser.value = signedUpUser
        }
    }

    private val _nearByOwners = mutableListOf<User>()
    val nearByOwners = _nearByOwners

    suspend fun fetchUser(id: String) {
        dbUsers.document(id).get().let { dataSnapShot ->
            dataSnapShot.data<User>().let { userInfo ->
                _currentUser.value = userInfo
            }
        }
    }

    private suspend fun getCurrentUser(): User? = authService.auth.currentUser?.let {
        val result = dbUsers.document(it.uid).get()
        return result.data<User>()
    }

    suspend fun signOut() {
        authService.signOut()
        _currentUser.value = getCurrentUser()
        _nearByOwners.clear()
    }

    suspend fun updateUserFavoriteTools(user: User) {
        dbUsers.document(user.id).update("favoriteTools" to user.favoriteTools)
        fetchUser(user.id)
    }

    suspend fun updateUserInfo(user: User) {
        dbUsers.document(user.id).update(user)
        fetchUser(user.id)
    }

    suspend fun getUserInfo(userID: String): User? {
        return try {
            dbUsers.document(userID).get().data<User>()
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateBoundingBoxCoordinates(center: GeoPoint, radius: Int): List<GeoPoint> {
        val lat = radius / 110.574
        val lon = radius / (111.320 * cos((center.latitude * Math.PI) / (180)))


        val x0: Double = center.latitude - lat  //top-left
        val y0: Double = center.longitude + lon

        val x1: Double = center.latitude + lat //bottom-right
        val y1: Double = center.longitude - lon

        val corner0 = GeoPoint(x0, y0)
        val corner1 = GeoPoint(x1, y1)
        return listOf(corner0, corner1)

    }

    suspend fun getNearByOwners(
        distance: Int = 1, // Kilometer
        callback: suspend (List<User>) -> Unit
    ) {
        if (this.nearByOwners.isNotEmpty())
            callback(this.nearByOwners)
        else
            currentUser.value?.geoPoint?.let { location ->
                val tempListOfOwnerNearBy = mutableListOf<User>()
                val collectionRef = Firebase.firestore.collection("Users")
                val corners = calculateBoundingBoxCoordinates(location, distance)
                val queryResult = collectionRef
                    .orderBy("longitude")
                    .where {
                        "longitude".lessThanOrEqualTo(corners[0].longitude)
                    }
                    .where {
                        "longitude".greaterThanOrEqualTo(corners[1].longitude)
                    }.where {
                        "latitude".greaterThanOrEqualTo(corners[0].latitude)
                    }.where {
                        "latitude".lessThanOrEqualTo(corners[1].latitude)
                    }.get()
                queryResult.documents.forEach {
                    if ((it.id != currentUser.value?.id)) {
                        val tmpUser = it.data<User>()
                        tmpUser.geoPoint?.let {
                            if(it.distanceToOtherPoint(location) <= distance)
                                tempListOfOwnerNearBy.add(tmpUser)
                        }
                    }
                }
                _nearByOwners.addAll(tempListOfOwnerNearBy)
                callback(tempListOfOwnerNearBy)
            }
    }

}