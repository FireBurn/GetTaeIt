package uk.co.fireburn.gettaeit.shared.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val geofenceManager: GeofenceManager,
    @param:ApplicationContext private val context: Context
) {
    private val _isOnWorkWifi = MutableStateFlow(false)

    init {
        registerWifiCallback()
    }

    /** Registers a network callback to detect when the device joins/leaves the work WiFi. */
    private fun registerWifiCallback() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    checkWifiSsid(connectivityManager)
                }

                override fun onAvailable(network: Network) {
                    checkWifiSsid(connectivityManager)
                }

                override fun onLost(network: Network) {
                    _isOnWorkWifi.value = false
                }
            })
    }

    private fun checkWifiSsid(connectivityManager: ConnectivityManager) {
        // Fire-and-forget: best-effort SSID check. Requires ACCESS_FINE_LOCATION on API 29+.
        try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

            @Suppress("DEPRECATION")
            val rawSsid = wifiManager.connectionInfo?.ssid?.trim('"') ?: return
            userPreferencesRepository.getUserPreferences().let { flow ->
                // We can't collect a flow here synchronously, so we read the last-known pref
                // via a blocking call isn't possible in a callback. Instead we stash the SSID
                // and let the combine() below pick it up.
                _lastKnownWifiSsid.value = rawSsid
            }
        } catch (_: Exception) {
            // Permission or hardware not available — silently ignore
        }
    }

    private val _lastKnownWifiSsid = MutableStateFlow<String?>(null)

    /**
     * Emits the current AppMode based on time, location, WiFi, and other signals.
     */
    val appMode: Flow<AppMode> = combine(
        userPreferencesRepository.getUserPreferences(),
        geofenceManager.isAtWorkLocation,
        _lastKnownWifiSsid
    ) { prefs, isAtWork, currentSsid ->
        determineMode(prefs, isAtWork, currentSsid)
    }

    private fun determineMode(
        prefs: UserPreferences,
        isAtWork: Boolean,
        currentSsid: String?
    ): AppMode {
        val now = Calendar.getInstance()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        val isWorkDay = prefs.workSchedule.workingDays.contains(dayOfWeek)
        val isWorkHours =
            isWorkDay && currentHour in prefs.workSchedule.startHour until prefs.workSchedule.endHour

        // WiFi SSID match
        val isOnWorkWifi = prefs.workSsid != null &&
                currentSsid != null &&
                currentSsid.equals(prefs.workSsid, ignoreCase = true)

        // Commute window: within 30 min of work start/end and NOT at work location
        val isCommuteHour = isWorkDay && !isAtWork && !isOnWorkWifi && (
                currentHour == prefs.workSchedule.startHour - 1 ||
                        currentHour == prefs.workSchedule.endHour
                )

        return when {
            isAtWork || isOnWorkWifi || isWorkHours -> AppMode.WORK
            isCommuteHour -> AppMode.COMMUTE
            else -> AppMode.PERSONAL
        }
    }
}
