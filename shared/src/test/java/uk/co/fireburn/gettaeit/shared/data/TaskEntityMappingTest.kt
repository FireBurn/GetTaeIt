package uk.co.fireburn.gettaeit.shared.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

class TaskEntityMappingTest {

    private val gson = Gson()

    @Test
    fun `TaskEntity serializes to JSON correctly`() {
        val task = TaskEntity(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            title = "Test Task",
            description = "Description here",
            context = TaskContext.WORK,
            priority = 1,
            effortLevel = EffortLevel.HIGH,
            isCompleted = true,
            isArchived = false,
            estimatedMinutes = 15
        )

        val json = gson.toJson(task)
        
        // Assert that the JSON contains expected fields
        assert(json.contains("00000000-0000-0000-0000-000000000001"))
        assert(json.contains("Test Task"))
        assert(json.contains("WORK"))
        assert(json.contains("HIGH"))
    }

    @Test
    fun `TaskEntity deserializes from JSON correctly`() {
        val json = """
            {
              "id": "00000000-0000-0000-0000-000000000002",
              "title": "Remote Task",
              "context": "PERSONAL",
              "priority": 2,
              "effortLevel": "LOW",
              "isCompleted": false,
              "isArchived": false,
              "recurrence": {
                "type": "NONE",
                "interval": 1,
                "daysOfWeek": [],
                "missedBehaviour": "IGNORABLE",
                "timesPerDay": 1,
                "dailySlotMinutes": []
              },
              "dependencyIds": [],
              "isSubtask": false,
              "xpValue": 10,
              "streakCount": 0
            }
        """.trimIndent()

        val parsedTask = gson.fromJson(json, TaskEntity::class.java)

        assertNotNull(parsedTask)
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000002"), parsedTask.id)
        assertEquals("Remote Task", parsedTask.title)
        assertEquals(TaskContext.PERSONAL, parsedTask.context)
        assertEquals(EffortLevel.LOW, parsedTask.effortLevel)
        assertEquals(2, parsedTask.priority)
    }
}
