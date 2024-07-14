package lend.borrow.tool


import ToolInApp
import ToolInFireStore
import android.app.Application
import android.net.Uri
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.File
import dev.gitlive.firebase.storage.storage
import lend.borrow.tool.utility.toToolInApp
import java.util.UUID


class ToolsRepository(val application: Application) {
    val db: FirebaseFirestore = Firebase.firestore
    val dbTools: CollectionReference = db.collection("Tools")
    val storage = Firebase.storage

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

    suspend fun deleteTools(tools: List<String>) { // Tool IDs
        for (tool in tools) {
            val toolRef = dbTools.document(tool)
            val tempTool = toolRef.get().data<ToolInFireStore>()
            tempTool.imageReferences.forEach {
                storage.reference(it).delete() // This should remove all the images for this tool before deleting the tool
            }
            toolRef.delete()
        }
    }

    suspend fun uploadTool(toolName: String, toolDescription: String, tags: List<String>, images: List<String>, ownerId: String): DocumentReference {
        val uploadedImagesDownloadableUrl = mutableListOf<String>()
        val uploadedImagesStoragePath = mutableListOf<String>()
        images.forEach { imageUUID ->
            val imageNameWithSuffix = "${UUID.randomUUID()}.png"
            val path = "tools/$imageNameWithSuffix"
            val imageRef = storage.reference(path)
            imageRef.putFile(File(Uri.parse(imageUUID)))
            uploadedImagesDownloadableUrl.add(imageRef.getDownloadUrl())
            uploadedImagesStoragePath.add(path)
        }
        val tempTool = ToolInFireStore(toolName, toolDescription, tags = tags.toMutableList(), imageReferences = uploadedImagesStoragePath, imageUrls = uploadedImagesDownloadableUrl, owner = ownerId)
        return dbTools.add(tempTool)
    }

}