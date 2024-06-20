package lend.borrow.tool

import Tool
import ToolsRepository
import User
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking

class ToolsViewModel(private val application: Application): AndroidViewModel(application) {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val dbUsers: CollectionReference = db.collection("Users")
    val toolsRepo by lazy {
        ToolsRepository(User("","","")) // Should be fixed
    }

    fun getToolsFromRemote(callback: (List<Tool>)-> Unit) = runBlocking {
        toolsRepo.getAvailableTools(callback)
    }

    fun updateUserFavoriteTools(user: User?, updateUI: () -> Unit) {
        user?.let {
            dbUsers.document(it.id).update("favoriteTools", it.favoriteTools).addOnSuccessListener {
                updateUI()
            }
        }
    }
}