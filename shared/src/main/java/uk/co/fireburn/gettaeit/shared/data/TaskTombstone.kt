package uk.co.fireburn.gettaeit.shared.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Remembers that a task was deleted and when, so an older copy still sitting on the
 * watch or in the cloud can't quietly bring it back. Holds no task content.
 */
@Entity(tableName = "task_tombstones")
data class TaskTombstone(
    @PrimaryKey val id: UUID,
    val deletedAt: Long
)

@Dao
interface TaskTombstoneDao {
    @Query("SELECT * FROM task_tombstones")
    suspend fun getAll(): List<TaskTombstone>

    @Query("SELECT * FROM task_tombstones")
    fun observeAll(): Flow<List<TaskTombstone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tombstones: List<TaskTombstone>)

    @Query("DELETE FROM task_tombstones WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM task_tombstones WHERE deletedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
