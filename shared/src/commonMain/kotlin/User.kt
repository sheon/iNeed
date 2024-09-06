
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.serialization.Serializable

enum class UserSchedule{
    EVERYDAY,
    WEEKEND,
    WEEKDAYS,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

@Serializable
data class User(
    val id: String = "",
    var name: String = "",
    var address: String = "",
    val email: String = "",
    var availableAtTheMoment: Boolean = true,
    var searchRadius: Int = 1,
    var subscription: String = "free",
    val favoriteTools: MutableList<String> = mutableListOf<String>(),
    val ownTools: MutableList<String> = mutableListOf<String>(),
    val borrowedTools: MutableList<String> = mutableListOf<String>(),
    val lentTools: MutableList<String> = mutableListOf<String>(),
    var geoPoint: GeoPoint? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val borrowRequestSent: List<BorrowRequest> = emptyList(),
    val borrowRequestReceived: List<BorrowRequest> = emptyList()
)
