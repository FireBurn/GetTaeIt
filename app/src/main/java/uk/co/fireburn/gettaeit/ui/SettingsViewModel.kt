package uk.co.fireburn.gettaeit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import uk.co.fireburn.gettaeit.shared.domain.GeofenceManager
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> =
        userPreferencesRepository.getUserPreferences().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

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
}
