package lend.borrow.tool
import User
import kotlinx.coroutines.flow.Flow
interface AuthenticationServiceInterface {
    val currentUserId: String
    val isAuthenticated: Boolean

    val currentUser: Flow<User?>

    suspend fun authenticate(email: String, password: String)
    suspend fun createUser(email: String, password: String)

    suspend fun signOut()
}