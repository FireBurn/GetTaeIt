package uk.co.fireburn.gettaeit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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
    }
}
