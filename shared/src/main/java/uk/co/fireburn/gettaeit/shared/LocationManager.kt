package uk.co.fireburn.gettaeit.shared

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    private val _isAtWork = MutableStateFlow(false)
    val isAtWork = _isAtWork.asStateFlow()

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // Use FLAG_IMMUTABLE for security best practices
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    @SuppressLint("MissingPermission") // Permissions will be handled at the UI layer before calling this
    fun addWorkGeofence(workLocation: LatLng) {
        val geofence = Geofence.Builder()
            .setRequestId("WORK_GEOFENCE")
            .setCircularRegion(workLocation.latitude, workLocation.longitude, 100f) // 100-meter radius
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                // Successfully added
            }
            addOnFailureListener {
                // Failed to add
            }
        }
    }

    fun removeWorkGeofence() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }

    fun updateGeofenceState(isEnter: Boolean) {
        _isAtWork.value = isEnter
    }
}
