import kotlinx.serialization.Serializable

@Serializable
data class ToolToBeUploadedToFireBase(
    val name: String,
    val description: String,
    var images: List<String> = emptyList(), // Drawable IDs
    val tags: List<String>,
//    val owner: User = User("", ""),
    var favorite: Boolean = false,
    var available: Boolean = true,
    var borrower: User? = null
)

@Serializable
data class ToolDownloadedFromFireBase(
    val name: String,
    val id: String,
    val description: String,
    val images: List<String>, // Image name stored on Firebase storage
    val tags: List<String>,
    var available: Boolean = true,
    var borrower: User? = null,
) {
    constructor() : this("", "", "", emptyList(), emptyList(), available = false)
}