package uk.co.fireburn.gettaeit.shared

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextManager @Inject constructor(
    @ApplicationContext private val context: Context,
    // private val userPreferencesRepository: UserPreferencesRepository, // To be added later
    // private val locationManager: LocationManager // To be added later
) {

    private val _androidAutoConnectionState = MutableStateFlow(false)

    fun setAndroidAutoConnected(isConnected: Boolean) {
        _androidAutoConnectionState.value = isConnected
    }

    // This will eventually be driven by the UserPreferencesRepository
    private val userPreferencesFlow = MutableStateFlow(UserPreferences(workSsid = null, officeLocation = null, homeLocation = null))

    val appContext: Flow<AppContext> = combine(
        userPreferencesFlow,
        _androidAutoConnectionState.asStateFlow(),
        // locationManager.isAtWork // To be added later
    ) { prefs, isAutoConnected ->
        determineContext(prefs, isAutoConnected, false) // isAtWork is false for now
    }

    private fun determineContext(
        prefs: UserPreferences,
        isAutoConnected: Boolean,
        isAtWork: Boolean
    ): AppContext {
        if (isAutoConnected) {
            return AppContext.COMMUTE
        }

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val isWorkHours =
            dayOfWeek in prefs.workSchedule.workingDays && hourOfDay in prefs.workSchedule.startHour until prefs.workSchedule.endHour

        // In the future, we will also check for work wifi and geofence
        if (isWorkHours || isAtWork) {
            return AppContext.WORK
        }

        return AppContext.PERSONAL
    }
}
