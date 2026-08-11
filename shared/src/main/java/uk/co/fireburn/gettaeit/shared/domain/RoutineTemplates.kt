package uk.co.fireburn.gettaeit.shared.domain

import uk.co.fireburn.gettaeit.shared.data.TaskContext

data class RoutineTemplate(
    val id: String,
    val label: String,
    val context: TaskContext,
    val steps: List<String>
)

/** Small, editable starting points—not prescriptive productivity plans. */
object RoutineTemplates {
    val all = listOf(
        RoutineTemplate(
            id = "morning_reset",
            label = "Morning reset",
            context = TaskContext.PERSONAL,
            steps = listOf("Drink some water", "Get dressed", "Check today's one main thing")
        ),
        RoutineTemplate(
            id = "leaving_home",
            label = "Leaving home",
            context = TaskContext.PERSONAL,
            steps = listOf("Keys", "Phone", "Wallet", "Check the door")
        ),
        RoutineTemplate(
            id = "work_shutdown",
            label = "Work shutdown",
            context = TaskContext.WORK,
            steps = listOf("Write tomorrow's first step", "Close work tabs", "Leave work at work")
        )
    )
}
