package uk.co.fireburn.gettaeit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
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

    fun setWorkLocation(location: LatLng) {
        viewModelScope.launch {
            val currentPrefs = userPreferences.first()
            val updatedPrefs = currentPrefs.copy(workLocation = location)
            userPreferencesRepository.updateUserPreferences(updatedPrefs)
            geofenceManager.addWorkGeofence(location.latitude, location.longitude)
        }
    }

    fun clearWorkLocation() {
        viewModelScope.launch {
            val currentPrefs = userPreferences.first()
            val updatedPrefs = currentPrefs.copy(workLocation = null)
            userPreferencesRepository.updateUserPreferences(updatedPrefs)
            geofenceManager.removeWorkGeofence()
        }
    }
}
