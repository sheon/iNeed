package lend.borrow.tool

import dev.gitlive.firebase.auth.AuthResult
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class AuthenticationService(
    val auth: FirebaseAuth,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) :
    AuthenticationServiceInterface {
    override val currentUserId: String
        get() = auth.currentUser?.uid.toString()
    override val isAuthenticated: Boolean
        get() = auth.currentUser != null && auth.currentUser?.isAnonymous == false

    override suspend fun authenticate(email: String, password: String): AuthResult {
        return auth.signInWithEmailAndPassword(email, password)
    }

    override suspend fun createUser(email: String, password: String): AuthResult {
        return auth.createUserWithEmailAndPassword(email, password)
    }

    override suspend fun signOut() {
        scope.async {
            if (auth.currentUser?.isAnonymous == true) {
                auth.currentUser?.delete()
            }
            auth.signOut()
        }.await()
    }

    override suspend fun deleteAccount() {
        scope.async {
            auth.currentUser?.delete()
        }.await()
    }
}