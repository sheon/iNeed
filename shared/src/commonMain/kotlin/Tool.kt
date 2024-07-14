import kotlinx.serialization.Serializable


@Serializable
data class ToolInFireStore(
    val name: String,
    val description: String,
    val imageReferences: MutableList<String>, // Document references for Images stored on Firebase storage
    val imageUrls: MutableList<String>, // Downloadable Urls for Images stored on Firebase storage
    val tags: MutableList<String>?,
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
    val imageReferences: MutableList<String>, // Document references for Images stored on Firebase storage
    val imageUrls: MutableList<String>, // Downloadable Urls for Images stored on Firebase storage
    val tags: MutableList<String>?,
    var available: Boolean = true,
    var owner: User?, // To avoid requesting FireStore everytime the item is loaded on a view
    var borrower: User? = null, // To avoid requesting FireStore everytime the item is loaded on a view
) {
    constructor() : this("", "", "", mutableListOf(), mutableListOf(), mutableListOf(), available = false, null, null)
}


