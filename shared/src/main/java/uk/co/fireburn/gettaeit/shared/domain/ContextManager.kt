package uk.co.fireburn.gettaeit.shared.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

enum class AppMode {
    WORK,
    PERSONAL,
    COMMUTE
}

@Singleton
class ContextManager @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geofenceManager: GeofenceManager
) {

    /**
     * Emits the current AppMode based on time, location, and other signals.
     */
    val appMode: Flow<AppMode> = combine(
        userPreferencesRepository.getUserPreferences(),
        geofenceManager.isAtWorkLocation
    ) { prefs, isAtWork ->
        determineMode(prefs, isAtWork)
    }

    private fun determineMode(prefs: UserPreferences, isAtWork: Boolean): AppMode {
        val now = Calendar.getInstance()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        val isWorkDay = prefs.workSchedule.workingDays.contains(dayOfWeek)
        val isWorkHours = isWorkDay && currentHour in prefs.workSchedule.startHour until prefs.workSchedule.endHour

        // If the user is inside the work geofence OR it's currently work hours,
        // they are in Work Mode.
        return if (isAtWork || isWorkHours) {
            AppMode.WORK
        } else {
            AppMode.PERSONAL
        }
    }
}
