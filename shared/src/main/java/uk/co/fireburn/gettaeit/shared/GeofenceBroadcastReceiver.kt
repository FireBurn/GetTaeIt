package uk.co.fireburn.gettaeit.shared

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null.")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = "Geofence Error with code: ${geofencingEvent.errorCode}"
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.i(TAG, "Geofence transition detected: $geofenceTransition")

        // Here we would typically update a repository or send an event
        // to notify the app that the user has entered/exited a geofence.
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
