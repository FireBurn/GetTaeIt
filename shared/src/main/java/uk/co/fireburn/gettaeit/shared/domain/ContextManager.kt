package uk.co.fireburn.gettaeit.shared.domain

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
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
    /** The Wi-Fi network we're connected to right now, or null when not on (known) Wi-Fi. */
    private val currentSsid = MutableStateFlow<String?>(null)

    init {
        registerWifiCallback()
    }

    /** Ticks on every minute boundary so modes and snoozes change on time, not on the next edit. */
    val minuteTicks: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    private fun registerWifiCallback() {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ only reveals the SSID to callbacks that ask for location info (and
            // to apps holding location permission); WifiManager just says "<unknown ssid>".
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    currentSsid.value = (capabilities.transportInfo as? WifiInfo)?.ssid.toKnownSsid()
                }

                override fun onLost(network: Network) {
                    currentSsid.value = null
                }
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    currentSsid.value = legacySsid()
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    currentSsid.value = legacySsid()
                }

                override fun onLost(network: Network) {
                    currentSsid.value = null
                }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (_: SecurityException) {
            // Without network state access, Wi-Fi simply doesn't feed into the mode.
        }
    }

    @Suppress("DEPRECATION")
    private fun legacySsid(): String? = try {
        context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo?.ssid.toKnownSsid()
    } catch (_: SecurityException) {
        null
    }

    private fun String?.toKnownSsid(): String? =
        this?.trim('"')?.takeUnless { it.isBlank() || it == UNKNOWN_SSID }

    /** Android Auto and car docks switch the phone into car mode while driving. */
    private val isCarMode: Flow<Boolean> = callbackFlow {
        val uiModeManager = context.getSystemService(UiModeManager::class.java)
        trySend(uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_CAR)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    UiModeManager.ACTION_ENTER_CAR_MODE -> trySend(true)
                    UiModeManager.ACTION_EXIT_CAR_MODE -> trySend(false)
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(UiModeManager.ACTION_ENTER_CAR_MODE)
            addAction(UiModeManager.ACTION_EXIT_CAR_MODE)
        }
        // Only the system sends these, and it reaches non-exported receivers; other apps can't.
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    /**
     * Emits the current AppMode based on time, location, WiFi, and other signals.
     */
    val appMode: Flow<AppMode> = combine(
        userPreferencesRepository.getUserPreferences(),
        geofenceManager.isAtWorkLocation,
        currentSsid,
        isCarMode,
        minuteTicks
    ) { prefs, isAtWork, ssid, carMode, _ ->
        ContextModeDecider.decide(
            preferences = prefs,
            isAtWorkLocation = isAtWork,
            currentSsid = ssid,
            isCarMode = carMode,
            now = Calendar.getInstance()
        )
    }.distinctUntilChanged()

    private companion object {
        const val UNKNOWN_SSID = "<unknown ssid>"
    }
}
