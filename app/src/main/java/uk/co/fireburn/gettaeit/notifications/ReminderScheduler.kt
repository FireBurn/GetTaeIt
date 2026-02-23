package uk.co.fireburn.gettaeit.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels AlarmManager alarms for task reminders.
 *
 * For tasks with timesPerDay > 1, multiple alarms are scheduled — one per slot.
 * Slots are spread evenly across waking hours (07:00 – 22:00).
 *
 * Uses setExactAndAllowWhileIdle so alarms fire even in Doze mode.
 * On Android 12+ this requires SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM permission.
 *
 * Each alarm triggers [ReminderReceiver] which then posts the notification.
 * After each alarm fires, it is NOT automatically rescheduled — the ViewModel
 * calls [scheduleTask] again when the task resets for the next recurrence.
 */
@Singleton
class ReminderScheduler @Inject constructor() {

    /** Waking day window: 07:00 to 22:00 = 900 minutes span */
    private val WAKE_START_MINS = 7 * 60   // 07:00
    private val WAKE_END_MINS = 22 * 60  // 22:00
    private val WAKE_SPAN_MINS = WAKE_END_MINS - WAKE_START_MINS

    /**
     * Schedule all reminder slots for [task].
     * Safe to call on update — cancels existing alarms first.
     */
    fun scheduleTask(context: Context, task: TaskEntity) {
        if (task.recurrence.type == RecurrenceType.NONE) return
        if (task.isCompleted) {
            cancelTask(context, task); return
        }

        cancelTask(context, task) // clear old alarms
        val slotTimes = computeSlotTimesMs(task.recurrence)
        slotTimes.forEachIndexed { slotIndex, triggerAtMs ->
            if (triggerAtMs > System.currentTimeMillis()) {
                scheduleAlarm(context, task, slotIndex, triggerAtMs)
            }
        }
    }

    /** Cancel all alarms for [task]. */
    fun cancelTask(context: Context, task: TaskEntity) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val slotCount = if (task.recurrence.dailySlotMinutes.isNotEmpty())
            task.recurrence.dailySlotMinutes.size
        else
            task.recurrence.timesPerDay.coerceAtLeast(1)
        repeat(slotCount) { slotIndex ->
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

    /**
     * Computes the epoch-ms trigger time for each daily slot.
     *
     * Priority order:
     *   1. [RecurrenceConfig.dailySlotMinutes] — explicit named times (e.g. wake/lunch/dinner/bed).
     *   2. Even-spread across waking hours based on [RecurrenceConfig.timesPerDay],
     *      anchored at [RecurrenceConfig.preferredTimeOfDayMinutes] if set.
     *
     * Slots already past today are pushed to tomorrow.
     */
    private fun computeSlotTimesMs(config: RecurrenceConfig): List<Long> {
        val now = Calendar.getInstance()

        val slotMinutes: List<Int> = when {
            // 1. Explicit slot times — use as-is
            config.dailySlotMinutes.isNotEmpty() -> config.dailySlotMinutes.sorted()

            // 2. Even-spread fallback
            else -> {
                val n = config.timesPerDay.coerceAtLeast(1)
                if (n == 1) {
                    listOf(config.preferredTimeOfDayMinutes ?: WAKE_START_MINS)
                } else {
                    val firstMins = config.preferredTimeOfDayMinutes ?: WAKE_START_MINS
                    val step = WAKE_SPAN_MINS / (n - 1).coerceAtLeast(1)
                    (0 until n).map { i ->
                        (firstMins + i * step).coerceAtMost(WAKE_END_MINS)
                    }
                }
            }
        }

        return slotMinutes.map { mins ->
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, mins / 60)
                set(Calendar.MINUTE, mins % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If this slot has already passed today, push to tomorrow
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
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
