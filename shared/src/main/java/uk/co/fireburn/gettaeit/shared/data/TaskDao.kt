package uk.co.fireburn.gettaeit.shared.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TaskDao {

    // ── Read ─────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM tasks ORDER BY priority ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /** Active tasks = not completed AND (not snoozed OR snooze expired) AND nextOccurrence is null or past */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0
          AND (isSnoozed = 0 OR snoozedUntil <= :nowMs)
          AND (nextOccurrenceAt IS NULL OR nextOccurrenceAt <= :nowMs)
        ORDER BY priority ASC, dueDate ASC
    """
    )
    fun getActiveTasks(nowMs: Long): Flow<List<TaskEntity>>

    /** Tasks whose next recurrence is due but the task has been marked complete */
    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 1
          AND nextOccurrenceAt IS NOT NULL
          AND nextOccurrenceAt <= :nowMs
    """
    )
    suspend fun getRecurrencesDue(nowMs: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: UUID): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: UUID): TaskEntity?

    @Query("SELECT * FROM tasks WHERE parentId = :parentId ORDER BY priority ASC")
    fun getSubtasks(parentId: UUID): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL ORDER BY priority ASC, dueDate ASC")
    fun getTopLevelTasks(): Flow<List<TaskEntity>>

    /** All top-level tasks including completed, for dependency picker. */
    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND isCompleted = 0 ORDER BY title ASC")
    fun getAllActiveToplevelTasks(): Flow<List<TaskEntity>>

    /** Get all subtasks for a parent to check if all complete. */
    @Query("SELECT * FROM tasks WHERE parentId = :parentId")
    suspend fun getAllSubtasks(parentId: UUID): List<TaskEntity>

    /**
     * Completed tasks with a title matching [titlePattern] (SQLite LIKE).
     * Used by the learning loop to improve future time estimates.
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND actualMinutes IS NOT NULL AND title LIKE :titlePattern ORDER BY completedAt DESC LIMIT 20")
    suspend fun getCompletedByTitle(titlePattern: String): List<TaskEntity>

    // ── Write ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM tasks WHERE parentId = :parentId")
    suspend fun deleteSubtasksOf(parentId: UUID)
}
