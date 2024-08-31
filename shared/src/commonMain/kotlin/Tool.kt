import kotlinx.serialization.Serializable


@Serializable
data class ToolInFireStore(
    val name: String,
    val description: String,
    val imageReferences: List<String>, // Document references for Images stored on Firebase storage
    val imageUrls: List<String>, // Downloadable Urls for Images stored on Firebase storage
    val tags: List<String> = emptyList(),
    var available: Boolean = true,
    val owner: String, // User Id
    var borrower: String? = null, // User Id
) {
    constructor() : this("", "", mutableListOf(), mutableListOf(), mutableListOf(), available = false, "")
}

@Serializable
data class ToolInApp(
    val name: String,
    var id: String,
    val description: String,
    val imageReferences: List<String>, // Document references for Images stored on Firebase storage
    val imageUrls: List<String>, // Downloadable Urls for Images stored on Firebase storage
    val tags: List<String> = emptyList(),
    val available: Boolean = true,
    val owner: User, // To avoid requesting FireStore everytime the item is loaded on a view
    val borrower: User? = null, // To avoid requesting FireStore everytime the item is loaded on a view
    val instruction: String = "dnfvöjknfdkjövndklfjnbökjnvsfgökjnbösdkjnfbökndflmnflkbnlijnfblkdnflbnldfnvöaeälsjdvädaflökväöldf'äå'pldfgopkergmkntrugnöednb.va,md .mxv andfökfjkngvörekngömndföm, bvmfdn bjn"
) {
    val newImages = mutableListOf<String>()
    constructor() : this("", "", "", mutableListOf(), mutableListOf(), mutableListOf(), available = false, User(), null)
}


