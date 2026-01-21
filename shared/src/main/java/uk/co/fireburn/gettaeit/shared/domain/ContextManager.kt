package uk.co.fireburn.gettaeit.shared.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val userPreferencesRepository: UserPreferencesRepository
) {

    /**
     * Emits the current AppMode based on time, location, and other signals.
     * For now, it only checks the time against the user's work schedule.
     */
    val appMode: Flow<AppMode> = userPreferencesRepository.getUserPreferences().map { prefs ->
        determineMode(prefs)
    }

    private fun determineMode(prefs: UserPreferences): AppMode {
        val now = Calendar.getInstance()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)

        val isWorkDay = prefs.workSchedule.workingDays.contains(dayOfWeek)
        if (!isWorkDay) {
            return AppMode.PERSONAL
        }

        val startHour = prefs.workSchedule.startHour
        val endHour = prefs.workSchedule.endHour
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        return if (currentHour in startHour until endHour) {
            AppMode.WORK
        } else {
            AppMode.PERSONAL
        }
    }
}
