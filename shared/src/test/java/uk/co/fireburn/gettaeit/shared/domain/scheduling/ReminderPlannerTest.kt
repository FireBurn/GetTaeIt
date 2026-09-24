package uk.co.fireburn.gettaeit.shared.domain.scheduling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.Calendar

class ReminderPlannerTest {

    private val brushTeeth = TaskEntity(
        title = "Brush teeth",
        recurrence = RecurrenceConfig(type = RecurrenceType.DAILY, preferredTimeOfDayMinutes = 8 * 60)
    )

    @Test
    fun `after a daily reminder fires the next one is tomorrow`() {
        val firedAt = on(Calendar.MONDAY, 8, 0) + 3_000L

        assertEquals(listOf(on(Calendar.TUESDAY, 8, 0)), ReminderPlanner.nextTriggerTimes(brushTeeth, firedAt))
    }

    @Test
    fun `a slot still to come today stays today`() {
        assertEquals(
            listOf(on(Calendar.MONDAY, 8, 0)),
            ReminderPlanner.nextTriggerTimes(brushTeeth, on(Calendar.MONDAY, 7, 30))
        )
    }

    @Test
    fun `finished weekly task waits for its next occurrence instead of nagging daily`() {
        val nextWeek = on(Calendar.MONDAY, 8, 0) + 7 * DAY_MS
        val sheets = TaskEntity(
            title = "Change sheets",
            recurrence = RecurrenceConfig(type = RecurrenceType.WEEKLY, preferredTimeOfDayMinutes = 8 * 60),
            isCompleted = true,
            nextOccurrenceAt = nextWeek
        )

        assertEquals(listOf(nextWeek), ReminderPlanner.nextTriggerTimes(sheets, on(Calendar.MONDAY, 9, 0)))
    }

    @Test
    fun `slots inside a snooze move on`() {
        val pills = TaskEntity(
            title = "Pills",
            recurrence = RecurrenceConfig(type = RecurrenceType.DAILY, dailySlotMinutes = listOf(20 * 60, 8 * 60)),
            isSnoozed = true,
            snoozedUntil = on(Calendar.MONDAY, 15, 0)
        )

        assertEquals(
            listOf(on(Calendar.TUESDAY, 8, 0), on(Calendar.MONDAY, 20, 0)),
            ReminderPlanner.nextTriggerTimes(pills, on(Calendar.MONDAY, 9, 0))
        )
    }

    @Test
    fun `custom days only remind on the chosen days`() {
        val gym = TaskEntity(
            title = "Gym",
            recurrence = RecurrenceConfig(
                type = RecurrenceType.CUSTOM_DAYS,
                daysOfWeek = listOf(Calendar.MONDAY, Calendar.WEDNESDAY),
                preferredTimeOfDayMinutes = 18 * 60
            )
        )

        assertEquals(
            listOf(on(Calendar.WEDNESDAY, 18, 0)),
            ReminderPlanner.nextTriggerTimes(gym, on(Calendar.MONDAY, 19, 0))
        )
    }

    @Test
    fun `several reminders spread across the waking day`() {
        assertEquals(
            listOf(7 * 60, 14 * 60 + 30, 22 * 60),
            ReminderPlanner.slotMinutes(RecurrenceConfig(type = RecurrenceType.DAILY, timesPerDay = 3))
        )
    }

    @Test
    fun `one-off, archived and finished-for-good tasks get no reminders`() {
        val now = on(Calendar.MONDAY, 9, 0)

        assertTrue(ReminderPlanner.nextTriggerTimes(TaskEntity(title = "Once"), now).isEmpty())
        assertTrue(ReminderPlanner.nextTriggerTimes(brushTeeth.copy(isArchived = true), now).isEmpty())
        assertTrue(ReminderPlanner.nextTriggerTimes(brushTeeth.copy(isCompleted = true), now).isEmpty())
    }

    @Test
    fun `stale alarms stay quiet`() {
        val now = on(Calendar.MONDAY, 9, 0)

        assertTrue(ReminderPlanner.shouldNotify(brushTeeth, now))
        assertFalse(ReminderPlanner.shouldNotify(brushTeeth.copy(isCompleted = true, nextOccurrenceAt = now + DAY_MS), now))
        assertTrue(ReminderPlanner.shouldNotify(brushTeeth.copy(isCompleted = true, nextOccurrenceAt = now - 1), now))
        assertFalse(ReminderPlanner.shouldNotify(brushTeeth.copy(isSnoozed = true, snoozedUntil = now + 60_000L), now))
        assertFalse(ReminderPlanner.shouldNotify(brushTeeth.copy(isArchived = true), now))
        assertFalse(ReminderPlanner.shouldNotify(TaskEntity(title = "Once"), now))
    }

    private fun on(day: Int, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.SEPTEMBER, 14, hour, minute)
        while (get(Calendar.DAY_OF_WEEK) != day) add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
