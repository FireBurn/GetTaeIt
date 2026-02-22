package uk.co.fireburn.gettaeit.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.EntryPointAccessors
import uk.co.fireburn.gettaeit.shared.di.DataLayerEntryPoint

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null.")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence Error with code: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        Log.i(TAG, "Geofence transition detected: $transition")

        // Wire the transition into GeofenceManager so ContextManager reacts
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DataLayerEntryPoint::class.java
            )
            val geofenceManager = entryPoint.geofenceManager()
            val isEntering = transition == Geofence.GEOFENCE_TRANSITION_ENTER
            geofenceManager.updateWorkLocationStatus(isEntering)
            Log.i(TAG, "Work location status updated: isAtWork=$isEntering")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update GeofenceManager: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
