package lend.borrow.tool
import dev.gitlive.firebase.auth.AuthResult

interface AuthenticationServiceInterface {
    val currentUserId: String
    val isAuthenticated: Boolean

    suspend fun authenticate(email: String, password: String): AuthResult
    suspend fun createUser(email: String, password: String): AuthResult

    suspend fun signOut()
}