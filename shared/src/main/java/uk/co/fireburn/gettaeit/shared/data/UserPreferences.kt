package uk.co.fireburn.gettaeit.shared.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

data class WorkSchedule(
    val startHour: Int = 9,
    val endHour: Int = 17,
    val workingDays: List<Int> = listOf(2, 3, 4, 5, 6) // Calendar.MONDAY..FRIDAY
)

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1,
    @Embedded val workSchedule: WorkSchedule = WorkSchedule(),
    /** Stored as "lat,lng" e.g. "55.86,-4.25" */
    val workLocationString: String? = null,
    val workLocationRadius: Float = 100f, // metres
    val workSsid: String? = null,
    val homeSsid: String? = null,
    val isVacationMode: Boolean = false
) {
    val workLocationLatLng: Pair<Double, Double>?
        get() = workLocationString?.split(',')?.let {
            val lat = it[0].toDoubleOrNull() ?: return@let null
            val lng = it[1].toDoubleOrNull() ?: return@let null
            lat to lng
        }
}
