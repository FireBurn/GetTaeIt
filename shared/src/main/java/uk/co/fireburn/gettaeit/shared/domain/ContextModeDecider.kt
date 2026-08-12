package uk.co.fireburn.gettaeit.shared.domain

import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import java.util.Calendar

/** Pure, testable policy for the Work/Personal boundary. */
object ContextModeDecider {
    fun decide(
        preferences: UserPreferences,
        isAtWorkLocation: Boolean,
        currentSsid: String?,
        isCarMode: Boolean,
        now: Calendar
    ): AppMode {
        // Vacation is an explicit boundary. Location and Wi-Fi must not pull a
        // user back into work mode while it is enabled.
        if (preferences.isVacationMode) return AppMode.PERSONAL

        if (isCarMode) return AppMode.COMMUTE

        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val isWorkDay = dayOfWeek in preferences.workSchedule.workingDays
        val isWorkHours = isWorkDay &&
            currentHour in preferences.workSchedule.startHour until preferences.workSchedule.endHour
        val isOnWorkWifi = preferences.workSsid != null &&
            currentSsid?.equals(preferences.workSsid, ignoreCase = true) == true
        val isCommuteHour = isWorkDay && !isAtWorkLocation && !isOnWorkWifi && (
            currentHour == preferences.workSchedule.startHour - 1 ||
                currentHour == preferences.workSchedule.endHour
            )

        return when {
            isAtWorkLocation || isOnWorkWifi || isWorkHours -> AppMode.WORK
            isCommuteHour -> AppMode.COMMUTE
            else -> AppMode.PERSONAL
        }
    }
}
