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
data class User(val id: String, val name: String, val address: String, var availableAtTheMoment: Boolean = true, val profilePic: Int? = null, var subscription: String = "free", var availableDays: UserSchedule = UserSchedule.EVERYDAY, val availableTime: Long = 0, var isAnonymous: Boolean = true ) {
    val isAvailable: Boolean
        get() = availableAtTheMoment // And checks for more detailed schedules
    val favoriteTools = mutableListOf<String>()
    val ownTools = mutableListOf<String>()
    val borrowedTools = mutableListOf<String>()
    val lentTools = mutableListOf<String>()
}
