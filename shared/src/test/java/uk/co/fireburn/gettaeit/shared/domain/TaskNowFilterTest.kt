package uk.co.fireburn.gettaeit.shared.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.data.EffortLevel

class TaskNowFilterTest {
    private val quick = TaskEntity(title = "Reply", estimatedMinutes = 5)
    private val medium = TaskEntity(title = "Wash dishes", estimatedMinutes = 20)
    private val long = TaskEntity(title = "Report", estimatedMinutes = 60)
    private val unknown = TaskEntity(title = "Mystery")

    @Test
    fun `time filter keeps only estimated tasks that fit`() {
        assertEquals(
            listOf(quick, medium),
            TaskNowFilter.filter(listOf(quick, medium, long, unknown), 20, false)
        )
    }

    @Test
    fun `low energy filter caps work at fifteen minutes`() {
        assertEquals(
            listOf(quick),
            TaskNowFilter.filter(listOf(quick, medium, long), null, true)
        )
    }

    @Test
    fun `explicit effort overrides duration for low energy filtering`() {
        val longButEasy = TaskEntity(title = "Listen to an audiobook", estimatedMinutes = 45, effortLevel = EffortLevel.LOW)
        val quickButDemanding = TaskEntity(title = "Hard phone call", estimatedMinutes = 5, effortLevel = EffortLevel.HIGH)

        assertEquals(
            listOf(longButEasy),
            TaskNowFilter.filter(listOf(longButEasy, quickButDemanding), null, true)
        )
    }
}
