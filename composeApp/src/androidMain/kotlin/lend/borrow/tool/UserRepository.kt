package lend.borrow.tool

import BorrowRequest
import ToolInApp
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
import lend.borrow.tool.requests.BorrowRequestUiState
import lend.borrow.tool.utility.distanceToOtherPoint
import kotlin.math.cos


class UserRepository(val application: Application) {
    private val authService: AuthenticationService = AuthenticationService(
        Firebase.auth
    )
    val db: FirebaseFirestore = Firebase.firestore
    val dbUsers: CollectionReference = db.collection("Users")
    val dbRequests: CollectionReference = db.collection("Requests")


    val requests = MutableStateFlow<List<BorrowRequestUiState>>(emptyList())
    companion object {
    private lateinit var instance: UserRepository
        fun getInstance(application: Application): UserRepository {
            if (::instance.isInitialized.not()) {
                instance = UserRepository(application)
            }
            return instance
        }
    }


    private val _currentUser = MutableStateFlow(runBlocking { getCurrentUser() })
    val currentUser = _currentUser.asStateFlow()
    suspend fun createUser(email: String, password: String) {
        authService.createUser(email, password).user?.let {
            val signedUpUser = User(
                it.uid,
                "",
                "",
                it.email ?: ""
            )
            dbUsers.document(it.uid).set(signedUpUser)
            _currentUser.value = signedUpUser
        }
    }

    private val _nearByOwners = mutableListOf<User>()
    val nearByOwners = _nearByOwners

    suspend fun fetchUser(id: String?, callBack: () -> Unit = {}) {
        id?.let {
            dbUsers.document(id).get().let { dataSnapShot ->
                dataSnapShot.data<User>().let { userInfo ->
                    _currentUser.value = userInfo
                    callBack()
                }
            }
        } ?: callBack()
    }


    suspend fun fetchAllRequestsForUser(borrowerId: String, requestsCallBack: (receivedRequests: List<BorrowRequestUiState>, sentRequest: List<BorrowRequestUiState>) -> Unit) {
        val sentRequestsResult = dbRequests.where {
            "requesterId".equalTo(borrowerId)
        }.get()

        val tmpListOfRequestsSent = mutableListOf<BorrowRequest>()
        sentRequestsResult.documents.forEach { dataSnapShot ->
            dataSnapShot.data<BorrowRequest>().let { request ->
                tmpListOfRequestsSent.add(request)
            }
        }


        val receivedRequestsResult = dbRequests.where {
            "ownerId".equalTo(borrowerId)
        }.get()

        val tmpListOfRequestsReceived = mutableListOf<BorrowRequest>()
        receivedRequestsResult.documents.forEach { dataSnapShot ->
            dataSnapShot.data<BorrowRequest>().let { request ->
                tmpListOfRequestsReceived.add(request)
            }
        }


        val toolRepo = ToolsRepository.getInstance(application)

        val tmpSentRequestsUiStates = mutableListOf<BorrowRequestUiState>()
        tmpListOfRequestsSent.forEach { request ->
            toolRepo.getTool(request.toolId, this) { tool ->
                getUserInfo(request.requesterId)?.let { borrower ->
                    tmpSentRequestsUiStates.add(
                        BorrowRequestUiState(
                            tool,
                            borrower,
                            isAccepted = request.isAccepted,
                            isRead = request.isRead,
                            initialRequest = request
                        )
                    )
                }
            }
        }

        val tmpReceivedRequestsUiStates = mutableListOf<BorrowRequestUiState>()
        tmpListOfRequestsReceived.forEach { request ->
            toolRepo.getTool(request.toolId, this) { tool ->
                getUserInfo(request.requesterId)?.let { borrower ->
                    tmpReceivedRequestsUiStates.add(
                        BorrowRequestUiState(
                            tool,
                            borrower,
                            isAccepted = request.isAccepted,
                            isRead = request.isRead,
                            initialRequest = request
                        )
                    )
                }
            }
        }

        requestsCallBack(tmpReceivedRequestsUiStates, tmpSentRequestsUiStates)

    }

    suspend fun updateRequest(request: BorrowRequest, callback: () -> Unit) {
        dbRequests.document(request.requestId).update(request)
        callback()
    }


    suspend fun fetchReceivedRequestsForTool(tool: ToolInApp): List<BorrowRequestUiState> {
        val result = dbRequests.where {
            "toolId".equalTo(tool.id)
        }.get()

        val tmpListOfRequests = mutableListOf<BorrowRequest>()
        result.documents.forEach { dataSnapShot ->
            dataSnapShot.data<BorrowRequest>().let { request ->
                tmpListOfRequests.add(request)
            }
        }

        // This transformation should be done in the ViewModel but there is currently not such a functionality for StateFlow
        val tmpRequestsUiStates = mutableListOf<BorrowRequestUiState>()
        tmpListOfRequests.forEach { request ->
            getUserInfo(request.requesterId)?.let { borrower ->
                tmpRequestsUiStates.add(
                    BorrowRequestUiState(
                        tool,
                        borrower,
                        isAccepted = request.isAccepted,
                        isRead = request.isRead,
                        initialRequest = request
                    )
                )
            }
        }


        return tmpRequestsUiStates
    }


    private suspend fun getCurrentUser(): User? = authService.auth.currentUser?.let {
        val result = dbUsers.document(it.uid).get()
        return result.data<User?>()
    }

    suspend fun signOut() {
        authService.signOut()
        _currentUser.value = getCurrentUser()
        _nearByOwners.clear()
    }

    suspend fun deleteAccount(user: User) {
        if (user.borrowedTools.isEmpty() && user.lentTools.isEmpty()) {
            val toolRepo = ToolsRepository.getInstance(application)
            toolRepo.deleteTools(user.ownTools.toList())
            dbUsers.document(user.id).delete()
            authService.deleteAccount()
            authService.signOut()
            _currentUser.value = getCurrentUser()
            _nearByOwners.clear()
        }

    }

    suspend fun updateUserFavoriteTools(user: User) {
        dbUsers.document(user.id).update("favoriteTools" to user.favoriteTools)
        fetchUser(user.id)
    }

    suspend fun onRequestToBorrow(borrower: User, tool: ToolInApp, callBack: () -> Unit) {
        val tmpRef = dbRequests.document
        val tmpRequest = BorrowRequest(requestId = tmpRef.id, requesterId = borrower.id, ownerId = tool.owner.id, toolId = tool.id)
        tmpRef.set(tmpRequest)
        callBack()
    }

    suspend fun updateUserInfo(newUserInfo: User, oldUserInfo: User, progressCallBack: () -> Unit) {
        dbUsers.document(newUserInfo.id).update(newUserInfo)
        if (newUserInfo.address != oldUserInfo.address || newUserInfo.searchRadius != oldUserInfo.searchRadius) {
            _nearByOwners.clear()
            nearByOwners.clear()
        }
        fetchUser(newUserInfo.id, progressCallBack)
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
        location: GeoPoint? = null,
        distance: Int = 2, // Kilometer
        callback: suspend (List<User>) -> Unit
    ) {
        val possibleLocation = location ?: currentUser.value?.geoPoint
        possibleLocation?.let { availableLocation ->
                val searchDistance = currentUser.value?.searchRadius ?: distance
                val tempListOfOwnerNearBy = mutableListOf<User>()
                val collectionRef = Firebase.firestore.collection("Users")
                val corners = calculateBoundingBoxCoordinates(availableLocation, searchDistance)
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
                    val tmpUser = it.data<User>()
                    tmpUser.geoPoint?.let {
                        if (it.distanceToOtherPoint(availableLocation) <= searchDistance)
                            tempListOfOwnerNearBy.add(tmpUser)
                    }
                }
                _nearByOwners.addAll(tempListOfOwnerNearBy)
                callback(tempListOfOwnerNearBy)
            }
    }

    fun refreshData() {
        _nearByOwners.clear()
        nearByOwners.clear()
    }
}