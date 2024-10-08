package lend.borrow.tool

import BorrowRequest
import Conversation
import Message
import ToolInApp
import User
import android.app.Application
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.FieldValue
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
    val db: FirebaseFirestore = Firebase.firestore
    val dbUsers: CollectionReference = db.collection("Users")
    val dbRequests: CollectionReference = db.collection("Requests")
    val dbConversations: CollectionReference = db.collection("Conversations")
    val dbMessages: CollectionReference = db.collection("Messages")


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


    suspend fun fetchRequestsSentByUser(borrowerId: String, requestsCallBack: suspend (sentRequest: List<BorrowRequest>) -> Unit) {
        val sentRequestsResult = dbRequests.where {
            "requesterId".equalTo(borrowerId)
        }.get()

        val tmpListOfRequestsSent = mutableListOf<BorrowRequest>()
        sentRequestsResult.documents.forEach { dataSnapShot ->
            dataSnapShot.data<BorrowRequest>().let { request ->
                tmpListOfRequestsSent.add(request)
            }
        }
        requestsCallBack(tmpListOfRequestsSent)
    }

    suspend fun fetchRequestsSentToUser(borrowerId: String, requestsCallBack: suspend (receivedRequests: List<BorrowRequest>) -> Unit) {
        val receivedRequestsResult = dbRequests.where {
            "ownerId".equalTo(borrowerId)
        }.get()

        val tmpListOfRequestsReceived = mutableListOf<BorrowRequest>()
        receivedRequestsResult.documents.forEach { dataSnapShot ->
            dataSnapShot.data<BorrowRequest>().let { request ->
                tmpListOfRequestsReceived.add(request)
            }
        }

        requestsCallBack(tmpListOfRequestsReceived)

    }

    suspend fun onRequestReadUpdated(request: BorrowRequest, callback: suspend () -> Unit) {
        dbRequests.document(request.requestId).update(request)
        callback()
    }

    suspend fun onRequestAccepted(request: BorrowRequest, callback: suspend () -> Unit) {
        val newConversation = dbConversations.document
        newConversation.set(Conversation(conversationId = newConversation.id, messages = emptyList()))
        dbUsers.document(request.requesterId).update(Pair("conversation", FieldValue.arrayUnion(newConversation.id)))
        dbUsers.document(request.ownerId).update(Pair("conversation", FieldValue.arrayUnion(newConversation.id)))
        dbRequests.document(request.requestId).update(request.copy(conversationId = newConversation.id))
        callback()
    }


    suspend fun fetchReceivedRequestsForToolAndUser(toolId: String, requestForUserId: String? = null): List<BorrowRequest> {
        val result = if (requestForUserId == null)
            dbRequests
            .orderBy("toolId")
            .where {
                "toolId".equalTo(toolId)
            }.get()
        else
            dbRequests
                .orderBy("requesterId")
                .where{
                    "requesterId".equalTo(requestForUserId)
                }
                .where{
                    "toolId".equalTo(toolId)
                }.get()
        val tmpListOfRequests = mutableListOf<BorrowRequest>()
        result.documents.forEach { dataSnapShot ->
            dataSnapShot.data<BorrowRequest>().let { request ->
                tmpListOfRequests.add(request)
            }
        }

        return tmpListOfRequests
    }

    suspend fun fetchARequest(requestId: String): BorrowRequest {
        val result = dbRequests.document(requestId).get()
        return result.data<BorrowRequest>()
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

    suspend fun sendMessage(message: String, senderId: String, conversationId: String) {
        val newMessage = dbMessages.document
        newMessage.set(Message(message = message, fromUserId = senderId, messageId = newMessage.id))
        dbConversations.document(conversationId).update("messages" to FieldValue.arrayUnion(newMessage.id))
    }

    suspend fun getMessage(messageId: String): Message {
        val messageRef = dbMessages.document(messageId).get()
        return messageRef.data<Message>()
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