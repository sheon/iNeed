import kotlinx.serialization.Serializable


@Serializable
data class ToolInFireStore(
    val name: String,
    val description: String? = null,
    val imageReferences: List<String>, // Document references for Images stored on Firebase storage
    val tags: List<String> = emptyList(),
    val available: Boolean = true,
    val owner: String, // User Id
    val borrower: String? = null, // User Id
    val instruction: String? = null
) {
    constructor() : this(name = "", imageReferences =  emptyList(), owner = "")
}

@Serializable
data class ToolInApp(
    val name: String,
    val id: String,
    val description: String? = null,
    val imageRefUrlMap: Map<String, String>, // Document references for Images stored on Firebase storage
    val tags: List<String> = emptyList(),
    val available: Boolean = true,
    val owner: User, // To avoid requesting FireStore everytime the item is loaded on a view
    val borrower: User? = null, // To avoid requesting FireStore everytime the item is loaded on a view
    val instruction: String? = null
) {
    val newImages = mutableListOf<String>()
    val deletedImages = mutableListOf<String>()
    constructor() : this(name = "", id = "", imageRefUrlMap = mapOf(), available = false, owner = User())
}

data class ToolDetailUiState(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val instruction: String = "",
    val images: Map<String, String> = mutableMapOf(),
    val tags: String? = null,
    val owner: User = User(),
    val borrower: User? = null,
    val isAvailable: Boolean = true,
    val defaultTool: ToolInApp,
    val somethingIsChanging: Boolean = false
) {
    val newImages = mutableListOf<String>()
    val deletedImages = mutableListOf<String>()
    val imageUrlsRefMap: Map<String, String>
        get() {
            val tmp = images.entries.associateBy({it.value}) { it.key } +
                    newImages.associateWith { it } // For new images there is no downloadable Firebase Url but the local address
            return tmp
        }
}


