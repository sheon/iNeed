import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

class ToolsRepository {
    val db: FirebaseFirestore = Firebase.firestore
    val dbTools: CollectionReference = db.collection("Tools")
    val dbUser: CollectionReference = db.collection("Users")


    suspend fun getAvailableTools(retrievedData: (List<ToolInApp>) -> Unit, userRepository: UserRepository) {
        val toolList = mutableListOf<ToolInApp>()
        dbTools.get().let { queryDocumentSnapshots ->
            if (queryDocumentSnapshots.documents.isNotEmpty()) {
                val list = queryDocumentSnapshots.documents
                for (d in list) {
                    val c: ToolInFireStore = d.data<ToolInFireStore>()

                    val transformedTool =  c.toToolInApp(userRepo = userRepository)
                    println("Ehsan: ToolRepo ToolInApp is: $transformedTool")
                    toolList.add(transformedTool.copy(id = d.id)) // This ID will be used only in the app after the tool is fetched and there is not need to store it on FireStore.
                }
                retrievedData.invoke(toolList)
            } else {
                println("Ehsan: No data found in Database")
            }
        }
    }
}