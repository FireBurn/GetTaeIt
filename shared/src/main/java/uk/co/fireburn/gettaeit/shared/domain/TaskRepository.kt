package uk.co.fireburn.gettaeit.shared.domain

import kotlinx.coroutines.flow.Flow
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.UUID

interface TaskRepository {
    /** All active tasks for the current Work/Personal mode, filtered and sorted. */
    fun getTasksForCurrentMode(): Flow<List<TaskEntity>>

    /** Top-level tasks only (no subtasks). */
    fun getTopLevelTasksForCurrentMode(): Flow<List<TaskEntity>>

    /** All active top-level tasks, regardless of mode (for Alarms/Widgets). */
    fun getAllActiveToplevelTasks(): Flow<List<TaskEntity>>

    /** Subtasks belonging to a given parent. */
    fun getSubtasks(parentId: UUID): Flow<List<TaskEntity>>

    suspend fun getTaskById(id: UUID): TaskEntity?

    /** Add a task. If it has subtasks they must be added separately via addAll. */
    suspend fun addTask(task: TaskEntity)
    suspend fun addAll(tasks: List<TaskEntity>)

    /**
     * Mark a task as done. Handles recurrence automatically:
     * – IGNORABLE: schedules next occurrence at next calendar slot
     * – PERSISTENT: schedules next occurrence from now + interval
     * – Non-recurring: just marks isCompleted = true
     */
    suspend fun completeTask(task: TaskEntity)

    /** Undo a completion (within the same session). */
    suspend fun uncompleteTask(task: TaskEntity)

    /** Snooze a task until [untilMs]. */
    suspend fun snoozeTask(task: TaskEntity, untilMs: Long)

    suspend fun updateTask(task: TaskEntity)
    suspend fun deleteTask(task: TaskEntity)

    /** Check if all subtasks are done, and if so, mark the parent as done. */
    suspend fun autoCompleteParentIfDone(parentId: UUID)

    /** Reset any recurring tasks whose nextOccurrenceAt has passed. Called by WorkManager. */
    suspend fun resetDueRecurrences()
}
