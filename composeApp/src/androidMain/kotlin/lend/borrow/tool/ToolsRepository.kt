package lend.borrow.tool


import ToolInApp
import ToolInFireStore
import android.app.Application
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import lend.borrow.tool.utility.toToolInApp


class ToolsRepository(val application: Application) {
    val db: FirebaseFirestore = Firebase.firestore
    val dbTools: CollectionReference = db.collection("Tools")

    companion object {
        private lateinit var instance: ToolsRepository
        fun getInstance(application: Application): ToolsRepository {
            if (::instance.isInitialized.not()) {
                instance = ToolsRepository(application)
            }
            return instance
        }
    }
    suspend fun getAvailableTools(retrievedData: (List<ToolInApp>) -> Unit, userRepository: UserRepository) {
        userRepository.getNearByOwners { nearByOwners ->
            dbTools.get().let { queryDocumentSnapshots ->
                val tmpOwnerIds = nearByOwners.associateBy { it.id }
                val toolList = mutableListOf<ToolInApp>()
                if (queryDocumentSnapshots.documents.isNotEmpty()) {
                    val list = queryDocumentSnapshots.documents
                    for (d in list) {
                        val c: ToolInFireStore = d.data<ToolInFireStore>()
                        tmpOwnerIds[c.owner]?.let {
                            val transformedTool = c.toToolInApp(it, userRepo = userRepository)
                            toolList.add(transformedTool.copy(id = d.id)) // This ID will be used only in the app after the tool is fetched and there is not need to store it on FireStore.
                        }
                    }
                } else {
                    println("Ehsan: No data found in Database")
                }
                retrievedData.invoke(toolList)
            }
        }

    }
}