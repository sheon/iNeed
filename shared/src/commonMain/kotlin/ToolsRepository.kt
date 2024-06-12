import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class ToolsRepository {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val dbTools: CollectionReference = db.collection("Tools")


    suspend fun getAvailableTools(retrieveData: (List<ToolDownloadedFromFireBase>) -> Unit) {
        val toolList = mutableListOf<ToolDownloadedFromFireBase>()
        dbTools.get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                if (!queryDocumentSnapshots.isEmpty) {
                    // if the snapshot is not empty we are
                    // hiding our progress bar and adding
                    // our data in a list.
                    // loadingPB.setVisibility(View.GONE)
                    val list = queryDocumentSnapshots.documents
                    for (d in list) {
                        // after getting this list we are passing that
                        // list to our object class.
                        val c: ToolDownloadedFromFireBase? = d.toObject(ToolDownloadedFromFireBase::class.java)
                        // and we will pass this object class inside
                        // our arraylist which we have created for list view.
                        c?.let { toolList.add(it.copy(id = d.id)) }
                        println("Ehsan: ID: ${d.id}")
                        println("Ehsan: Reference: ${d.reference}")

                    }
                    retrieveData.invoke(toolList)
                    println("Ehsan: $toolList")
                } else {
                    // if the snapshot is empty we are displaying
                    // a toast message.
                    println("Ehsan: No data found in Database")
                }
            }
            // if we don't get any data or any error
            // we are displaying a toast message
            // that we do not get any data
            .addOnFailureListener {
                println("Ehsan: Fail to get the data.")
            }
    }
}