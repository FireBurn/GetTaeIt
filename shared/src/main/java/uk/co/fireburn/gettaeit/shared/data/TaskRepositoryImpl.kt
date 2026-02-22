package uk.co.fireburn.gettaeit.shared.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.co.fireburn.gettaeit.shared.domain.AppMode
import uk.co.fireburn.gettaeit.shared.domain.ContextManager
import uk.co.fireburn.gettaeit.shared.domain.RecurrenceEngine
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val contextManager: ContextManager,
    private val recurrenceEngine: RecurrenceEngine
) : TaskRepository {

    // ─── Active task stream ─────────────────────────────────────────────────

    override fun getTasksForCurrentMode(): Flow<List<TaskEntity>> {
        val now = System.currentTimeMillis()
        return combine(
            taskDao.getActiveTasks(now),
            contextManager.appMode
        ) { tasks, mode ->
            tasks.filterByMode(mode).sortedByDisplayOrder()
        }
    }

    override fun getTopLevelTasksForCurrentMode(): Flow<List<TaskEntity>> {
        val now = System.currentTimeMillis()
        return combine(
            taskDao.getActiveTasks(now),
            contextManager.appMode
        ) { tasks, mode ->
            tasks.filter { it.parentId == null }.filterByMode(mode).sortedByDisplayOrder()
        }
    }

    override fun getSubtasks(parentId: UUID): Flow<List<TaskEntity>> =
        taskDao.getSubtasks(parentId)

    // ─── Single task ────────────────────────────────────────────────────────

    override suspend fun getTaskById(id: UUID): TaskEntity? = taskDao.getTaskById(id)

    // ─── Writes ─────────────────────────────────────────────────────────────

    override suspend fun addTask(task: TaskEntity) = taskDao.insert(task)

    override suspend fun addAll(tasks: List<TaskEntity>) = taskDao.insertAll(tasks)

    override suspend fun updateTask(task: TaskEntity) = taskDao.update(task)

    override suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteSubtasksOf(task.id)
        taskDao.delete(task)
    }

    // ─── Completion with recurrence ─────────────────────────────────────────

    override suspend fun completeTask(task: TaskEntity) {
        val now = System.currentTimeMillis()
        val nextOccurrence = recurrenceEngine.calculateNextOccurrence(task, now)

        val updated = task.copy(
            isCompleted = true,
            completedAt = now,
            isSnoozed = false,
            snoozedUntil = null,
            nextOccurrenceAt = nextOccurrence,
            // Streak: award if completed today and streak was maintained
            streakCount = calculateStreak(task, now),
            lastStreakDate = now
        )
        taskDao.update(updated)
    }

    override suspend fun uncompleteTask(task: TaskEntity) {
        taskDao.update(
            task.copy(
                isCompleted = false,
                completedAt = null,
                nextOccurrenceAt = null
            )
        )
    }

    // ─── Snooze ─────────────────────────────────────────────────────────────

    override suspend fun snoozeTask(task: TaskEntity, untilMs: Long) {
        taskDao.update(task.copy(isSnoozed = true, snoozedUntil = untilMs))
    }

    // ─── Recurrence reset (called by WorkManager) ───────────────────────────

    override suspend fun resetDueRecurrences() {
        val now = System.currentTimeMillis()
        val due = taskDao.getRecurrencesDue(now)
        due.forEach { task ->
            taskDao.update(recurrenceEngine.resetForNextOccurrence(task))
        }
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private fun List<TaskEntity>.filterByMode(mode: AppMode): List<TaskEntity> = filter {
        when (mode) {
            AppMode.WORK -> it.context == TaskContext.WORK || it.context == TaskContext.ANY
            AppMode.PERSONAL -> it.context == TaskContext.PERSONAL || it.context == TaskContext.ANY
            AppMode.COMMUTE -> true
        }
    }

    private fun List<TaskEntity>.sortedByDisplayOrder(): List<TaskEntity> = sortedWith(
        compareBy(
            { it.priority },      // 1 = most urgent
            { it.dueDate ?: Long.MAX_VALUE },
            { it.title }
        )
    )

    private fun calculateStreak(task: TaskEntity, now: Long): Int {
        val oneDayMs = 86_400_000L
        val lastDate = task.lastStreakDate ?: return 1
        // Was last completed within the past 2 days? (allow a little slack for ADHD)
        return if (now - lastDate <= oneDayMs * 2) task.streakCount + 1 else 1
    }
}
