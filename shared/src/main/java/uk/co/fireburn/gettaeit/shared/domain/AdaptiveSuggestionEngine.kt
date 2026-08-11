package uk.co.fireburn.gettaeit.shared.domain

import uk.co.fireburn.gettaeit.shared.data.EffortLevel
import uk.co.fireburn.gettaeit.shared.data.TaskEntity

data class AdaptiveSuggestion(
    val taskId: java.util.UUID,
    val message: String,
    val explanation: String
)

/**
 * A deliberately small, explainable local suggestion engine. It reads only task
 * state already on-device and never changes schedules or task properties.
 */
object AdaptiveSuggestionEngine {
    fun suggest(
        activeTasks: List<TaskEntity>,
        completedToday: List<TaskEntity>,
        deferredTasks: List<TaskEntity>
    ): AdaptiveSuggestion? {
        val candidate = activeTasks
            .filter { !it.isCompleted && !it.isSnoozed }
            .sortedWith(
                compareBy<TaskEntity> { it.effortLevel != EffortLevel.LOW }
                    .thenBy { it.estimatedMinutes ?: Int.MAX_VALUE }
                    .thenBy { it.priority }
            )
            .firstOrNull() ?: return null

        val explanation = when {
            deferredTasks.isNotEmpty() ->
                "You have a few things parked for later, so this is a gentler next move."
            completedToday.isNotEmpty() ->
                "You’ve already made progress today; this is one manageable next move."
            candidate.effortLevel == EffortLevel.LOW ->
                "Marked low effort, so it may fit a lower-energy moment."
            else -> "Chosen locally from your available tasks; you can ignore it."
        }
        return AdaptiveSuggestion(candidate.id, "Try: ${candidate.title}", explanation)
    }
}
