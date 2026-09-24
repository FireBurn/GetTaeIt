package uk.co.fireburn.gettaeit.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.scheduling.ReminderPlanner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels AlarmManager alarms for task reminders.
 *
 * Each configured reminder slot gets its next alarm. When an alarm fires, the receiver
 * validates the current task and schedules the next occurrence for each slot.
 *
 * Uses setExactAndAllowWhileIdle so alarms fire even in Doze mode.
 * On Android 12+ this requires SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM permission.
 *
 * Each alarm triggers [ReminderReceiver] which then posts the notification.
 * The scheduler delegates date, recurrence and snooze rules to [ReminderPlanner].
 */
@Singleton
class ReminderScheduler @Inject constructor() {

    /**
     * Schedule all reminder slots for [task].
     * Safe to call on update — cancels existing alarms first.
     */
    fun scheduleTask(context: Context, task: TaskEntity) {
        cancelTask(context, task)
        ReminderPlanner.nextTriggerTimes(task, System.currentTimeMillis()).forEachIndexed { slotIndex, triggerAtMs ->
            scheduleAlarm(context, task, slotIndex, triggerAtMs)
        }
    }

    /** Cancel all alarms for [task]. */
    fun cancelTask(context: Context, task: TaskEntity) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Clear every supported slot so reducing the configured slot count cannot leave
        // an old alarm armed under an index that no longer exists.
        repeat(ReminderPlanner.MAX_SLOTS) { slotIndex ->
            am.cancel(buildPendingIntent(context, task.id.toString(), slotIndex))
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun scheduleAlarm(
        context: Context,
        task: TaskEntity,
        slotIndex: Int,
        triggerAtMs: Long
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = buildPendingIntent(context, task.id.toString(), slotIndex, task.title)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // Fall back to inexact alarm if exact permission not granted
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        } catch (e: SecurityException) {
            // Exact alarms not permitted — use inexact fallback
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    private fun buildPendingIntent(
        context: Context,
        taskId: String,
        slotIndex: Int,
        taskTitle: String = ""
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(TaskNotificationManager.EXTRA_TASK_ID, taskId)
            putExtra(TaskNotificationManager.EXTRA_TASK_TITLE, taskTitle)
            putExtra(TaskNotificationManager.EXTRA_SLOT_INDEX, slotIndex)
        }
        val requestCode = TaskNotificationManager.notificationId(taskId, slotIndex)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
