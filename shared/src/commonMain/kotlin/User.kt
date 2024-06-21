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
    val name: String = "",
    val address: String = "",
    val email: String = "",
    var availableAtTheMoment: Boolean = true,
    var subscription: String = "free",
    val favoriteTools: MutableList<String> = mutableListOf<String>(),
    val ownTools: MutableList<String> = mutableListOf<String>(),
    val borrowedTools: MutableList<String> = mutableListOf<String>(),
    val lentTools: MutableList<String> = mutableListOf<String>()
) // The initial values are for the serializable in order to parse FirebaseUser to User directly.
