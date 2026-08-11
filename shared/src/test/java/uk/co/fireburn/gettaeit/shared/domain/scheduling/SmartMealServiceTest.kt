package uk.co.fireburn.gettaeit.shared.domain.scheduling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.concurrent.TimeUnit

class SmartMealServiceTest {
    private val service = SmartMealService()

    @Test
    fun `slow cooker plan produces preparation and leftovers at correct offsets`() {
        val meal = TaskEntity(title = "Stew", context = TaskContext.PERSONAL, dueDate = 1_000_000L)

        val tasks = service.generatePrepTasks(meal, RecipeType.SLOW_COOKER_STEW)

        assertEquals(3, tasks.size)
        assertTrue(tasks.all { it.parentId == meal.id && it.offsetReferenceId == meal.id })
        assertEquals(meal.dueDate!! - TimeUnit.HOURS.toMillis(8), tasks[0].dueDate)
        assertEquals(meal.dueDate!! + TimeUnit.HOURS.toMillis(2), tasks[2].dueDate)
        assertEquals(-TimeUnit.HOURS.toMillis(2), tasks[2].offsetDuration)
    }

    @Test
    fun `meal without a time has no prep tasks`() {
        assertTrue(service.generatePrepTasks(TaskEntity(title = "Stew")).isEmpty())
    }
}
