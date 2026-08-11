package uk.co.fireburn.gettaeit.shared.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineTemplatesTest {
    @Test
    fun `templates have unique identifiers and actionable steps`() {
        assertEquals(RoutineTemplates.all.size, RoutineTemplates.all.map { it.id }.toSet().size)
        assertTrue(RoutineTemplates.all.all { it.steps.isNotEmpty() && it.steps.all(String::isNotBlank) })
    }

    @Test
    fun `shopping and cleaning resets are available`() {
        assertTrue(RoutineTemplates.all.any { it.id == "shopping_reset" })
        assertTrue(RoutineTemplates.all.any { it.id == "tiny_tidy" })
    }
}
