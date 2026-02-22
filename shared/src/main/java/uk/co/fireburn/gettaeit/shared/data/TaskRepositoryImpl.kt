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

    override fun getAllActiveToplevelTasks(): Flow<List<TaskEntity>> =
        taskDao.getAllActiveToplevelTasks()

    override suspend fun autoCompleteParentIfDone(parentId: UUID) {
        val subtasks = taskDao.getAllSubtasks(parentId)
        if (subtasks.isNotEmpty() && subtasks.all { it.isCompleted }) {
            val parent = taskDao.getTaskById(parentId) ?: return
            if (!parent.isCompleted) completeTask(parent)
        }
    }

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

    /**
     * Smart sort order:
     * 1. Blocked tasks (have unfulfilled dependencies) sink — you can't do them anyway
     * 2. High-dependency-count tasks float up (unblocking them unlocks others)
     * 3. Tasks recently completed on a recurring schedule sink (not due for a while)
     * 4. Within same urgency: quick tasks (low estimatedMinutes) first
     * 5. Fallback: explicit priority, then due date
     */
    private fun List<TaskEntity>.sortedByDisplayOrder(): List<TaskEntity> {
        map { it.id }.toSet()
        // Count how many tasks each task blocks (i.e. how many others depend on it)
        val unblocksCount = mutableMapOf<UUID, Int>()
        forEach { task ->
            task.dependencyIds.forEach { depId ->
                unblocksCount[depId] = (unblocksCount[depId] ?: 0) + 1
            }
        }
        val now = System.currentTimeMillis()

        return sortedWith(
            compareBy(
                // 1. Blocked tasks last (dependencies not yet complete)
                { task ->
                    val hasUnmetDeps = task.dependencyIds.any { depId ->
                        val dep = firstOrNull { it.id == depId }
                        dep != null && !dep.isCompleted
                    }
                    if (hasUnmetDeps) 1 else 0
                },
                // 2. Tasks that unblock many others float up (negate to sort descending)
                { task -> -(unblocksCount[task.id] ?: 0) },
                // 3. Recently-completed recurring tasks sink — next occurrence is far away
                { task ->
                    val nextOcc = task.nextOccurrenceAt
                    if (nextOcc != null && nextOcc > now) {
                        // How far in the future is the next occurrence? Bucket it.
                        val hoursAway = (nextOcc - now) / 3_600_000L
                        when {
                            hoursAway > 24 * 7 -> 3   // weekly+ away: sink far
                            hoursAway > 24 -> 2   // tomorrow-ish
                            else -> 1   // later today
                        }
                    } else 0
                },
                // 4. Explicit priority (1=urgent, 5=someday)
                { task -> task.priority },
                // 5. Due date urgency
                { task -> task.dueDate ?: Long.MAX_VALUE },
                // 6. Quick wins first (shorter estimated time = float up)
                { task -> task.estimatedMinutes ?: Int.MAX_VALUE },
                // 7. Alpha tiebreak
                { task -> task.title }
            ))
    }

    private fun calculateStreak(task: TaskEntity, now: Long): Int {
        val oneDayMs = 86_400_000L
        val lastDate = task.lastStreakDate ?: return 1
        // Was last completed within the past 2 days? (allow a little slack for ADHD)
        return if (now - lastDate <= oneDayMs * 2) task.streakCount + 1 else 1
    }
}
