package uk.co.fireburn.gettaeit.shared.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

data class WorkSchedule(
    val startHour: Int = 9,
    val endHour: Int = 17,
    val workingDays: List<Int> = listOf(2, 3, 4, 5, 6) // Monday to Friday
)

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1,
    @Embedded val workSchedule: WorkSchedule = WorkSchedule(),
    val workLocation: LatLng? = null,
    val workSsid: String? = null,
    val homeSsid: String? = null,
    val isVacationMode: Boolean = false,
    val geminiModel: String? = null
)
