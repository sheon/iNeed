import kotlinx.serialization.Serializable


@Serializable
data class Tool(
    val name: String,
    var id: String,
    val description: String,
    val images: MutableList<String>, // Image name stored on Firebase storage
    val tags: MutableList<String>?,
    var available: Boolean = true,
    val owner: String, // User Id
    var borrower: String? = null, // User Id
) {
    constructor() : this("", "", "", mutableListOf(), mutableListOf(), available = false, "")
}