package uk.co.fireburn.gettaeit.shared.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.data.MissedBehaviour
import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.Calendar

class RecurrenceEngineTest {
    private val engine = RecurrenceEngine()

    @Test
    fun `one-off task has no next occurrence`() {
        val task = TaskEntity(title = "One off")
        assertNull(engine.calculateNextOccurrence(task, 1_000L))
    }

    @Test
    fun `persistent daily task advances from completion`() {
        val completedAt = calendar(2026, Calendar.AUGUST, 10, 14, 25).timeInMillis
        val task = recurringTask(RecurrenceType.DAILY, MissedBehaviour.PERSISTENT)

        val next = engine.calculateNextOccurrence(task, completedAt)

        assertEquals(calendar(2026, Calendar.AUGUST, 11, 14, 25).timeInMillis, next)
    }

    @Test
    fun `scheduled recurrence snaps to its preferred time`() {
        val completedAt = calendar(2026, Calendar.AUGUST, 10, 23, 45).timeInMillis
        val task = recurringTask(
            type = RecurrenceType.DAILY,
            behaviour = MissedBehaviour.IGNORABLE,
            preferredTime = 8 * 60
        )

        assertEquals(
            calendar(2026, Calendar.AUGUST, 11, 8, 0).timeInMillis,
            engine.calculateNextOccurrence(task, completedAt)
        )
    }

    @Test
    fun `empty custom days does not crash and advances by interval`() {
        val completedAt = calendar(2026, Calendar.AUGUST, 10, 9, 0).timeInMillis
        val task = recurringTask(RecurrenceType.CUSTOM_DAYS, MissedBehaviour.PERSISTENT)

        assertEquals(
            calendar(2026, Calendar.AUGUST, 11, 9, 0).timeInMillis,
            engine.calculateNextOccurrence(task, completedAt)
        )
    }

    @Test
    fun `due occurrence resets to active state`() {
        val task = recurringTask(RecurrenceType.DAILY, MissedBehaviour.PERSISTENT).copy(
            isCompleted = true,
            completedAt = 50L,
            nextOccurrenceAt = 100L
        )

        assertFalse(engine.isOccurrenceDue(task, 99L))
        assertEquals(null, engine.resetForNextOccurrence(task).nextOccurrenceAt)
        assertFalse(engine.resetForNextOccurrence(task).isCompleted)
    }

    private fun recurringTask(
        type: RecurrenceType,
        behaviour: MissedBehaviour,
        preferredTime: Int? = null
    ) = TaskEntity(
        title = "Recurring",
        recurrence = RecurrenceConfig(
            type = type,
            missedBehaviour = behaviour,
            preferredTimeOfDayMinutes = preferredTime
        )
    )

    private fun calendar(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }
}
