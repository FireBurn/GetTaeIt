package uk.co.fireburn.gettaeit.shared.domain

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.co.fireburn.gettaeit.shared.GeofenceBroadcastReceiver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private val _isAtWorkLocation = MutableStateFlow(false)
    val isAtWorkLocation = _isAtWorkLocation.asStateFlow()

    fun updateWorkLocationStatus(isAtWork: Boolean) {
        _isAtWorkLocation.value = isAtWork
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    fun addWorkGeofence(latitude: Double, longitude: Double) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions not granted. We'll need to handle this in the UI.
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId("work_geofence")
            .setCircularRegion(latitude, longitude, 100f) // 100-meter radius
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                // Geofence added successfully
            }
            .addOnFailureListener {
                // Failed to add geofence
            }
    }

    fun removeWorkGeofence() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }
}
