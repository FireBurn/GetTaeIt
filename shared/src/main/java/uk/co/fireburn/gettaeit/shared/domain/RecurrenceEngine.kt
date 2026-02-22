package uk.co.fireburn.gettaeit.shared.domain

import uk.co.fireburn.gettaeit.shared.data.MissedBehaviour
import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The RecurrenceEngine is the heart of Get Tae It's scheduling logic.
 *
 * It answers two questions:
 *   1. Is this task visible/active right now?
 *   2. After a task is completed, when should it next appear?
 *
 * ─── Behaviour models ────────────────────────────────────────────────────────
 *
 * IGNORABLE (e.g. "brush teeth"):
 *   - If you miss it, the reminder disappears. No guilt.
 *   - Next occurrence fires at the *scheduled* time (not from completion).
 *   - So if you brush teeth at 11pm instead of 8pm, tomorrow's reminder is still 8pm.
 *
 * PERSISTENT (e.g. "change sheets"):
 *   - If you miss it, it stays visible and keeps nudging you.
 *   - Next occurrence is calculated from *when you actually did it*.
 *   - So if you change sheets on Wednesday instead of Sunday, the next reminder
 *     is Wednesday + 1 week, not Sunday + 1 week.
 */
@Singleton
class RecurrenceEngine @Inject constructor() {

    /**
     * Called when the user marks a task complete.
     * Returns the epoch-ms timestamp of when the task should next appear,
     * or null if it doesn't recur.
     */
    fun calculateNextOccurrence(task: TaskEntity, completedAtMs: Long): Long? {
        val config = task.recurrence
        if (config.type == RecurrenceType.NONE) return null

        return when (config.missedBehaviour) {
            MissedBehaviour.IGNORABLE -> nextFromSchedule(task, completedAtMs)
            MissedBehaviour.PERSISTENT -> nextFromCompletion(config, completedAtMs)
        }
    }

    /**
     * IGNORABLE: next occurrence is the next scheduled slot after the completion time.
     * Preserves the preferred time of day.
     */
    private fun nextFromSchedule(task: TaskEntity, completedAtMs: Long): Long {
        val config = task.recurrence
        val cal = Calendar.getInstance().apply { timeInMillis = completedAtMs }

        // Advance to the next interval
        when (config.type) {
            RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, config.interval)
            RecurrenceType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, config.interval)
            RecurrenceType.MONTHLY -> cal.add(Calendar.MONTH, config.interval)
            RecurrenceType.CUSTOM_DAYS -> {
                // Find the next day-of-week in the list
                val todayDow = cal.get(Calendar.DAY_OF_WEEK)
                val sorted = config.daysOfWeek.sorted()
                val nextDow = sorted.firstOrNull { it > todayDow } ?: sorted.first()
                var daysAhead = nextDow - todayDow
                if (daysAhead <= 0) daysAhead += 7
                cal.add(Calendar.DAY_OF_YEAR, daysAhead)
            }

            RecurrenceType.NONE -> { /* shouldn't reach here */
            }
        }

        // Snap to preferred time of day if set
        config.preferredTimeOfDayMinutes?.let { mins ->
            cal.set(Calendar.HOUR_OF_DAY, mins / 60)
            cal.set(Calendar.MINUTE, mins % 60)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        return cal.timeInMillis
    }

    /**
     * PERSISTENT: next occurrence is simply "interval after I actually did it".
     * The time of day is preserved from the completion moment (or preferred time if set).
     */
    private fun nextFromCompletion(config: RecurrenceConfig, completedAtMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = completedAtMs }

        when (config.type) {
            RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, config.interval)
            RecurrenceType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, config.interval)
            RecurrenceType.MONTHLY -> cal.add(Calendar.MONTH, config.interval)
            RecurrenceType.CUSTOM_DAYS -> {
                // For PERSISTENT + CUSTOM_DAYS: advance to the next matching day from completion
                val sorted = config.daysOfWeek.sorted()
                val todayDow = cal.get(Calendar.DAY_OF_WEEK)
                val nextDow = sorted.firstOrNull { it > todayDow } ?: sorted.first()
                var daysAhead = nextDow - todayDow
                if (daysAhead <= 0) daysAhead += 7
                cal.add(Calendar.DAY_OF_YEAR, daysAhead)
            }

            RecurrenceType.NONE -> { /* no-op */
            }
        }

        config.preferredTimeOfDayMinutes?.let { mins ->
            cal.set(Calendar.HOUR_OF_DAY, mins / 60)
            cal.set(Calendar.MINUTE, mins % 60)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        return cal.timeInMillis
    }

    /**
     * Returns true if a recurring task that was previously completed should now
     * be "reset" (shown again) because its next occurrence is due.
     */
    fun isOccurrenceDue(task: TaskEntity, nowMs: Long): Boolean {
        if (task.recurrence.type == RecurrenceType.NONE) return false
        val next = task.nextOccurrenceAt ?: return task.isCompleted.not()
        return nowMs >= next
    }

    /**
     * Resets a task for its next occurrence (clears isCompleted, updates nextOccurrenceAt to null
     * meaning "currently active"). The repo should call this when [isOccurrenceDue] returns true.
     */
    fun resetForNextOccurrence(task: TaskEntity): TaskEntity =
        task.copy(isCompleted = false, completedAt = null, nextOccurrenceAt = null)

    /**
     * Human-readable summary of recurrence for display in the UI.
     * e.g. "Every day", "Every week (persistent)", "Mon, Wed, Fri"
     */
    fun describeRecurrence(config: RecurrenceConfig): String {
        if (config.type == RecurrenceType.NONE) return "Once"

        val intervalStr = when {
            config.interval == 1 -> ""
            else -> " (every ${config.interval})"
        }

        val base = when (config.type) {
            RecurrenceType.DAILY -> "Daily$intervalStr"
            RecurrenceType.WEEKLY -> "Weekly$intervalStr"
            RecurrenceType.MONTHLY -> "Monthly$intervalStr"
            RecurrenceType.CUSTOM_DAYS -> {
                val dayNames = mapOf(
                    Calendar.MONDAY to "Mon", Calendar.TUESDAY to "Tue",
                    Calendar.WEDNESDAY to "Wed", Calendar.THURSDAY to "Thu",
                    Calendar.FRIDAY to "Fri", Calendar.SATURDAY to "Sat",
                    Calendar.SUNDAY to "Sun"
                )
                config.daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
            }

            RecurrenceType.NONE -> "Once"
        }

        return if (config.missedBehaviour == MissedBehaviour.PERSISTENT) "$base ↩ persistent" else base
    }
}
