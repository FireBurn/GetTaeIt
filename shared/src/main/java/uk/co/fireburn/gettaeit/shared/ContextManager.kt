package uk.co.fireburn.gettaeit.shared

import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import uk.co.fireburn.gettaeit.shared.data.UserPreferencesRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationManager: LocationManager
) {

    private val _androidAutoConnectionState = MutableStateFlow(false)

    fun setAndroidAutoConnected(isConnected: Boolean) {
        _androidAutoConnectionState.value = isConnected
    }

    val appContext: Flow<AppContext> = combine(
        userPreferencesRepository.userPreferencesFlow,
        _androidAutoConnectionState.asStateFlow(),
        locationManager.isAtWork
    ) { prefs, isAutoConnected, isAtWork ->
        determineContext(prefs, isAutoConnected, isAtWork)
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

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo
        val currentSsid = if (connectionInfo != null && connectionInfo.ssid != null) connectionInfo.ssid.replace("\"", "") else null
        val isWorkWifi = currentSsid != null && currentSsid == prefs.workSsid

        if (isWorkHours || isAtWork || isWorkWifi) {
            return AppContext.WORK
        }

        return AppContext.PERSONAL
    }
}
