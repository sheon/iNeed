import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class ToolsRepository(user: User) {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val dbTools: CollectionReference = db.collection("Tools")


    suspend fun getAvailableTools(retrieveData: (List<Tool>) -> Unit) {
        val toolList = mutableListOf<Tool>()
        dbTools.get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                if (!queryDocumentSnapshots.isEmpty) {
                    val list = queryDocumentSnapshots.documents
                    for (d in list) {
                        val c: Tool? = d.toObject(Tool::class.java)
                        c?.let { toolList.add(it.copy(id = d.id)) } // This ID will be used only in the app after the tool is fetched and there is not need to store it on FireStore.
                        println("Ehsan: ID: ${d.id}")
                        println("Ehsan: Reference: ${d.reference}")

                    }
                    retrieveData.invoke(toolList)
                    println("Ehsan: $toolList")
                } else {
                    println("Ehsan: No data found in Database")
                }
            }
            .addOnFailureListener {
                println("Ehsan: Fail to get the data.")
            }
    }
}