package lend.borrow.tool


import BorrowRequest
import ToolInApp
import ToolInFireStore
import User
import android.app.Application
import android.net.Uri
import android.util.Log
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.GeoPoint
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.File
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import lend.borrow.tool.utility.toToolInApp
import lend.borrow.tool.utility.toToolInFireStore
import java.util.UUID


class ToolsRepository(val application: Application) {
    val db: FirebaseFirestore = Firebase.firestore
    val dbTools: CollectionReference = db.collection("Tools")
    val storage = Firebase.storage

    private val Tag = ToolsRepository::class.java.name

    companion object {
        private lateinit var instance: ToolsRepository
        fun getInstance(application: Application): ToolsRepository {
            if (::instance.isInitialized.not()) {
                instance = ToolsRepository(application)
            }
            return instance
        }
    }

    val toolValidityMap = mutableMapOf<String, Boolean>()
    suspend fun getAvailableTools(
        location: GeoPoint? = null,
        retrievedData: (List<ToolInApp>) -> Unit,
        userRepository: UserRepository,
        isRefreshed: Boolean = false
    ) {
        val userToolsMap = mutableMapOf<User, MutableList<ToolInApp>>()
        userRepository.getNearByOwners(location) { nearByOwners ->
            coroutineScope {
                nearByOwners.map { owner ->
                        userToolsMap[owner] = mutableListOf()
                        owner.ownTools.map {
                            async {
                                dbTools.document(it).get().let { toolDoc ->
                                    if (toolDoc.exists) {
                                        val imageUrlList = mutableListOf<String>()
                                        val c: ToolInFireStore = toolDoc.data<ToolInFireStore>()

                                        c.imageReferences.map {
                                            async {
                                                imageUrlList.add(
                                                    storage.reference(it).getDownloadUrl()
                                                )
                                            }
                                        }
                                        async {
                                            val transformedTool =  c.toToolInApp(
                                                owner,
                                                userRepository,
                                                this@ToolsRepository
                                            )
                                            userToolsMap[owner]?.add(transformedTool)
                                            toolValidityMap[transformedTool.id] = !isRefreshed
                                        }
                                    } else {
                                        Log.i(Tag, "No data found in Database")
                                    }
                                }
                            }
                        }.awaitAll()
                }
            }
            retrievedData.invoke(userToolsMap.flatMap { it.value })
        }
    }

    suspend fun getToolWithRequests(toolId: String, requestsForUserId: String?, userRepository: UserRepository, retrievedData: suspend (ToolInApp, List<BorrowRequest>) -> Unit = { _, _ ->}) {
        coroutineScope {
            val toolRef = dbTools.document(toolId)
            val tool = toolRef.get().data<ToolInFireStore>()
            lateinit var toolTransformationTask: Deferred<ToolInApp>
            userRepository.getUserInfo(tool.owner)?.let {
                toolTransformationTask = async {
                    tool.toToolInApp(it, userRepo = userRepository, this@ToolsRepository)
                }
                val borrowRequestsTask = async {
                    userRepository.fetchReceivedRequestsForToolAndUser(toolId, requestsForUserId)
                }
                val transformedTool = toolTransformationTask.await()
                val borrowRequests = borrowRequestsTask.await()
                retrievedData.invoke(transformedTool, borrowRequests)
                toolValidityMap[transformedTool.id] = true
            }


        }
    }

    suspend fun getTool(toolId: String, userRepository: UserRepository, retrievedData: suspend (ToolInApp) -> Unit = {}) {
        val toolRef = dbTools.document(toolId)
            val tool = toolRef.get().data<ToolInFireStore>()
            userRepository.getUserInfo(tool.owner)?.let {
                val transformedTool = tool.toToolInApp(it, userRepo = userRepository, this)
                retrievedData.invoke(transformedTool)
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
        coroutineScope {
            images.map { imageUUID ->
                async {
                    val imageNameWithSuffix = "${UUID.randomUUID()}.png"
                    val path = "tools/$imageNameWithSuffix"
                    val imageRef = storage.reference(path)
                    imageRef.putFile(File(Uri.parse(imageUUID)))
                    uploadedImagesDownloadableUrl.add(imageRef.getDownloadUrl())
                    uploadedImagesStoragePath.add(path)
                }
            }.awaitAll()
        }
        val tmpRef = dbTools.document
        val tempTool = ToolInFireStore(id = tmpRef.id, toolName, toolDescription, tags = tags.toMutableList(), imageReferences = uploadedImagesStoragePath,owner = ownerId)
        tmpRef.set(tempTool)
        return tmpRef
    }

    suspend fun updateToolDetail(newTool: ToolInApp, callBack: () -> Unit) {
        val uploadedImagesStoragePath = newTool.imageRefUrlMap.toMutableMap()
        coroutineScope {
            newTool.newImages.map { imageUUID ->
                async {
                    val imageNameWithSuffix = "${UUID.randomUUID()}.png"
                    val path = "tools/$imageNameWithSuffix"
                    val imageRef = storage.reference(path)
                    imageRef.putFile(File(Uri.parse(imageUUID)))
                    uploadedImagesStoragePath[path] = imageRef.getDownloadUrl()
                }
            }
        }
        deleteToolImages(newTool.deletedImages)
        val tempTool = newTool.copy(imageRefUrlMap = uploadedImagesStoragePath).toToolInFireStore()
        dbTools.document(newTool.id).update(tempTool)
        toolValidityMap[newTool.id] = false
        callBack()
    }
    suspend fun deleteTool(tool: ToolInApp, user: User, progressCallBack: () -> Unit) {
        deleteToolImages(tool.imageRefUrlMap.keys.toList())
        dbTools.document(tool.id).delete()
        UserRepository.getInstance(application).dbUsers.document(user.id).update(user)
        progressCallBack()
    }

    private suspend fun deleteToolImages(images: List<String>) {
        images.forEach {
            storage.reference(it).delete()
        }
    }

}