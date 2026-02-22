package uk.co.fireburn.gettaeit.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import uk.co.fireburn.gettaeit.shared.domain.GeofenceManager
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geofenceManager: GeofenceManager,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> =
        userPreferencesRepository.getUserPreferences().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    /** Capture the device's current GPS position and save it as the work location. */
    @SuppressLint("MissingPermission")
    fun captureCurrentLocationAsWork() {
        viewModelScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
                val cts = CancellationTokenSource()
                val location: Location? = suspendCancellableCoroutine { cont ->
                    cont.invokeOnCancellation { cts.cancel() }
                    fusedClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc: Location? -> cont.resume(loc) }
                        .addOnFailureListener { e -> cont.resumeWithException(e) }
                }
                if (location != null) {
                    setWorkLocation(location.latitude, location.longitude)
                }
            } catch (_: Exception) {
                // Permission not granted or location unavailable — silently swallow
            }
        }
    }

    fun setWorkLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val updated = userPreferences.first().copy(
                workLocationString = "$latitude,$longitude"
            )
            userPreferencesRepository.updateUserPreferences(updated)
            geofenceManager.addWorkGeofence(latitude, longitude)
        }
    }

    fun clearWorkLocation() {
        viewModelScope.launch {
            val updated = userPreferences.first().copy(workLocationString = null)
            userPreferencesRepository.updateUserPreferences(updated)
            geofenceManager.removeWorkGeofence()
        }
    }

    fun setWorkSsid(ssid: String) {
        viewModelScope.launch {
            val updated = userPreferences.first().copy(workSsid = ssid.ifBlank { null })
            userPreferencesRepository.updateUserPreferences(updated)
        }
    }

    fun setWorkHours(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            val current = userPreferences.first()
            val updated = current.copy(
                workSchedule = current.workSchedule.copy(
                    startHour = startHour,
                    endHour = endHour
                )
            )
            userPreferencesRepository.updateUserPreferences(updated)
        }
    }

    fun setWorkingDays(days: List<Int>) {
        viewModelScope.launch {
            val current = userPreferences.first()
            val updated = current.copy(
                workSchedule = current.workSchedule.copy(workingDays = days)
            )
            userPreferencesRepository.updateUserPreferences(updated)
        }
    }
}
