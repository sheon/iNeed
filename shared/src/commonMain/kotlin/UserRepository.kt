import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import lend.borrow.tool.AuthenticationService


class UserRepository(private val authService: AuthenticationService = AuthenticationService(Firebase.auth)) {
    val db: FirebaseFirestore = Firebase.firestore
    val dbUsers: CollectionReference = db.collection("Users")

    private val _currentUser = MutableStateFlow(runBlocking { getCurrentUser() })
    val currentUser = _currentUser.asStateFlow()

    suspend fun createUser(email: String, password: String) {
        authService.createUser(email, password).user?.let {
            val signedUpUser = User(
                it.uid,
                "",
                ""
            )
            dbUsers.document(it.uid).set(signedUpUser)
            _currentUser.value = signedUpUser
        }
    }

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
    }

    suspend fun updateUserFavoriteTools(user: User) {
        dbUsers.document(user.id).update("favoriteTools" to user.favoriteTools)
        fetchUser(user.id)
    }

    suspend fun updateUserInfo(user: User) {
        dbUsers.document(user.id).update(user)
        fetchUser(user.id)
    }

    suspend fun getUserInfo(userID: String): User?{
        return try {
            dbUsers.document(userID).get().data<User>()
        } catch (e: Exception) {
            null
        }
    }
}