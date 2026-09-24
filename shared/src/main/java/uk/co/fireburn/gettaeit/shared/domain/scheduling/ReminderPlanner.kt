package uk.co.fireburn.gettaeit.shared.domain.scheduling

import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.Calendar

/**
 * Decides when a recurring task's reminders should next go off. Pure, so it's tested on the
 * JVM; the app's ReminderScheduler turns the answers into alarms.
 *
 * Each slot keeps its index (morning 0, evening 1, …) so its alarm can be replaced or
 * cancelled. Asking again just after a slot fires gives the next day's, which is how
 * reminders keep coming without the app being opened.
 */
object ReminderPlanner {
    /** Waking day for evenly spread reminders: 07:00 to 22:00. */
    private const val WAKE_START_MINUTES = 7 * 60
    private const val WAKE_END_MINUTES = 22 * 60

    /** More than the editor allows, so a task with fewer slots than before still clears old alarms. */
    const val MAX_SLOTS = 8

    fun slotMinutes(config: RecurrenceConfig): List<Int> {
        if (config.dailySlotMinutes.isNotEmpty()) return config.dailySlotMinutes.sorted().take(MAX_SLOTS)
        val count = config.timesPerDay.coerceIn(1, MAX_SLOTS)
        val first = config.preferredTimeOfDayMinutes ?: WAKE_START_MINUTES
        if (count == 1) return listOf(first)
        val step = (WAKE_END_MINUTES - WAKE_START_MINUTES) / (count - 1)
        return (0 until count).map { (first + it * step).coerceAtMost(WAKE_END_MINUTES) }
    }

    /** Whether a reminder going off at [nowMs] should show, given how the task looks now. */
    fun shouldNotify(task: TaskEntity, nowMs: Long): Boolean =
        task.recurrence.type != RecurrenceType.NONE &&
            !task.isArchived &&
            (!task.isCompleted || (task.nextOccurrenceAt ?: Long.MAX_VALUE) <= nowMs) &&
            !(task.isSnoozed && (task.snoozedUntil ?: Long.MAX_VALUE) > nowMs)

    /** When each slot should next fire, strictly after [nowMs]; empty when no reminders apply. */
    fun nextTriggerTimes(task: TaskEntity, nowMs: Long): List<Long> {
        val config = task.recurrence
        if (config.type == RecurrenceType.NONE || task.isArchived) return emptyList()
        if (task.isCompleted && task.nextOccurrenceAt == null) return emptyList()

        // Nothing before a finished task's next occurrence, or before a snooze ends.
        val notBefore = maxOf(
            nowMs,
            task.nextOccurrenceAt?.takeIf { task.isCompleted } ?: Long.MIN_VALUE,
            task.snoozedUntil?.takeIf { task.isSnoozed } ?: Long.MIN_VALUE
        )
        return slotMinutes(config).map { minuteOfDay -> nextSlot(minuteOfDay, notBefore, nowMs, config) }
    }

    private fun nextSlot(minuteOfDay: Int, notBefore: Long, nowMs: Long, config: RecurrenceConfig): Long {
        val slot = Calendar.getInstance().apply {
            timeInMillis = notBefore
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // A day and a week is always enough to pass the limits and land on an allowed weekday.
        repeat(9) {
            if (slot.timeInMillis >= notBefore && slot.timeInMillis > nowMs && config.allowsDay(slot)) {
                return slot.timeInMillis
            }
            slot.add(Calendar.DAY_OF_YEAR, 1)
        }
        return slot.timeInMillis
    }

    private fun RecurrenceConfig.allowsDay(day: Calendar): Boolean =
        type != RecurrenceType.CUSTOM_DAYS || daysOfWeek.isEmpty() || day.get(Calendar.DAY_OF_WEEK) in daysOfWeek
}
