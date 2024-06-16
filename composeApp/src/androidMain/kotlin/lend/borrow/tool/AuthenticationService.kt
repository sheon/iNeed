package lend.borrow.tool

import User
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthenticationService(
    val auth: FirebaseAuth,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) :
    AuthenticationServiceInterface {
    override val currentUserId: String
        get() = auth.currentUser?.uid.toString()
    override val isAuthenticated: Boolean
        get() = auth.currentUser != null && auth.currentUser?.isAnonymous == false
    override val currentUser: Flow<User?> = auth.authStateChanged.map {
        it?.let {
            User(
                it.uid,
                "",
                "",
                isAnonymous = it.isAnonymous
            )
        }
    }

    override suspend fun authenticate(email: String, password: String) {
        scope.async {
            auth.signInWithEmailAndPassword(email, password)
        }.await()
    }

    override suspend fun createUser(email: String, password: String) {
        scope.async {
            auth.createUserWithEmailAndPassword(email, password)
        }.await()
    }

    override suspend fun signOut() {
        if (auth.currentUser?.isAnonymous == true) {
            auth.currentUser?.delete()
        }

        auth.signOut()

        //create  new user anonymous session
    }
}