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
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.channels.awaitClose

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

    private val isCarModeFlow: Flow<Boolean> = kotlinx.coroutines.flow.callbackFlow {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isInitiallyCarMode = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR
        trySend(isInitiallyCarMode)

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: android.content.Intent) {
                if (intent.action == android.app.UiModeManager.ACTION_ENTER_CAR_MODE) {
                    trySend(true)
                } else if (intent.action == android.app.UiModeManager.ACTION_EXIT_CAR_MODE) {
                    trySend(false)
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.app.UiModeManager.ACTION_ENTER_CAR_MODE)
            addAction(android.app.UiModeManager.ACTION_EXIT_CAR_MODE)
        }
        // In modern Android we should export it or not, but for system broadcasts it's fine.
        context.registerReceiver(receiver, filter)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    /**
     * Emits the current AppMode based on time, location, WiFi, and other signals.
     */
    val appMode: Flow<AppMode> = combine(
        userPreferencesRepository.getUserPreferences(),
        geofenceManager.isAtWorkLocation,
        _lastKnownWifiSsid,
        isCarModeFlow
    ) { prefs, isAtWork, currentSsid, isCarMode ->
        ContextModeDecider.decide(
            preferences = prefs,
            isAtWorkLocation = isAtWork,
            currentSsid = currentSsid,
            isCarMode = isCarMode,
            now = java.util.Calendar.getInstance()
        )
    }
}
