package uk.co.fireburn.gettaeit.shared.data

import com.google.android.gms.maps.model.LatLng

data class WorkSchedule(
    val startHour: Int = 9,
    val endHour: Int = 17,
    val workingDays: List<Int> = listOf(1, 2, 3, 4, 5) // Monday to Friday
)

data class UserPreferences(
    val workSchedule: WorkSchedule = WorkSchedule(),
    val officeLocation: LatLng?,
    val homeLocation: LatLng?,
    val workSsid: String?
)
