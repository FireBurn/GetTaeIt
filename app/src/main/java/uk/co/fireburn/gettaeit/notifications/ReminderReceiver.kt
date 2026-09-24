package uk.co.fireburn.gettaeit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.DataLayerSync
import uk.co.fireburn.gettaeit.shared.di.DataLayerEntryPoint

/**
 * Fired by AlarmManager when a task reminder slot is due.
 * Simply posts the notification — all DB logic is deferred to [TaskActionReceiver].
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TaskNotificationManager.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(TaskNotificationManager.EXTRA_TASK_TITLE) ?: return
        val slotIndex = intent.getIntExtra(TaskNotificationManager.EXTRA_SLOT_INDEX, 0)

        TaskNotificationManager.showReminder(context, taskId, taskTitle, slotIndex)

        // A Wear notification may be muted or delayed by the system. Ask a paired,
        // reachable watch for its dedicated reminder pattern as well.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                EntryPointAccessors.fromApplication(context.applicationContext, DataLayerEntryPoint::class.java)
                    .dataLayerSync()
                    .sendWearHaptic(DataLayerSync.HAPTIC_REMINDER)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
