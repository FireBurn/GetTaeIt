package uk.co.fireburn.gettaeit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import javax.inject.Inject

/**
 * Reschedules all active recurring task alarms after device reboot.
 * AlarmManager alarms don't survive reboots — this receiver fires on BOOT_COMPLETED
 * and reinstates any tasks that have active recurrence.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var userPreferencesRepository: uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository

    @Inject
    lateinit var geofenceManager: uk.co.fireburn.gettaeit.shared.domain.GeofenceManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON" &&
            intent.action != android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) return

        val result = goAsync()
        scope.launch {
            try {
                val tasks = taskRepository.getAllActiveToplevelTasks().first()
                tasks.filter { it.recurrence.type != uk.co.fireburn.gettaeit.shared.data.RecurrenceType.NONE }
                    .forEach { reminderScheduler.scheduleTask(context, it) }

                val prefs = userPreferencesRepository.getUserPreferences().first()
                prefs.workLocationLatLng?.let { (lat, lng) ->
                    geofenceManager.addWorkGeofence(lat, lng, prefs.workLocationRadius)
                }
            } finally {
                result.finish()
            }
        }
    }
}
