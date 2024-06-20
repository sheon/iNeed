package lend.borrow.tool

import User
import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.auth.AuthResult
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await

class AuthenticationService(
    val auth: FirebaseAuth,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) :
    AuthenticationServiceInterface {
    override val currentUserId: String
        get() = auth.currentUser?.uid.toString()
    override val isAuthenticated: Boolean
        get() = auth.currentUser != null && auth.currentUser?.isAnonymous == false

    suspend fun getCurrentUser(): User? = auth.currentUser?.let {
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        val dbUsers: CollectionReference = db.collection("Users")
        val result = dbUsers.document(it.uid).get().await()
        result.data?.let {
            return result.toObject(User::class.java)
        }
    }


    override suspend fun authenticate(email: String, password: String, callBack: (AuthResult) -> Unit) {
        scope.async {
            val result = auth.signInWithEmailAndPassword(email, password)
            callBack.invoke(result)
        }.await()
    }

    override suspend fun createUser(email: String, password: String, callBack: (AuthResult) -> Unit) {
        scope.async {
            val result = auth.createUserWithEmailAndPassword(email, password)
            callBack.invoke(result)
        }.await()
    }

    override suspend fun signOut() {
        scope.async {
            if (auth.currentUser?.isAnonymous == true) {
                auth.currentUser?.delete()
            }
            auth.signOut()
            Log.v("Ehsan1", "curretnUser on FB: ${auth.currentUser}")
        }.await()
    }
}