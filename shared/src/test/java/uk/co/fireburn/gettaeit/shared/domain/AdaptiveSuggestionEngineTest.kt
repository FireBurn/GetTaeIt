package uk.co.fireburn.gettaeit.shared.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.data.EffortLevel
import uk.co.fireburn.gettaeit.shared.data.TaskEntity

class AdaptiveSuggestionEngineTest {
    @Test
    fun `prefers an explicit low effort task and explains deferred tasks`() {
        val demanding = TaskEntity(title = "Phone the bank", estimatedMinutes = 5, effortLevel = EffortLevel.HIGH)
        val easy = TaskEntity(title = "Fill water bottle", estimatedMinutes = 20, effortLevel = EffortLevel.LOW)
        val deferred = TaskEntity(title = "Laundry", isSnoozed = true)

        val result = AdaptiveSuggestionEngine.suggest(listOf(demanding, easy), emptyList(), listOf(deferred))

        assertEquals(easy.id, result?.taskId)
        assertTrue(result?.explanation?.contains("parked for later") == true)
    }
}
