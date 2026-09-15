package uk.co.fireburn.gettaeit.shared.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.co.fireburn.gettaeit.shared.domain.sync.SyncConflictResolver
import uk.co.fireburn.gettaeit.shared.domain.sync.TaskRecord
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Room side of sync. Hands out this device's copies and applies merge results in a
 * single transaction, so a half-applied merge can never leave tasks missing.
 */
@Singleton
class TaskSyncStore @Inject constructor(private val database: AppDatabase) {

    private val taskDao get() = database.taskDao()
    private val tombstoneDao get() = database.taskTombstoneDao()

    /** Emits whenever any task or tombstone changes. */
    fun changes(): Flow<Unit> = combine(taskDao.getAllTasks(), tombstoneDao.observeAll()) { _, _ -> }

    /**
     * Merges [remote] into Room and returns the plan; its `pushToRemote` lists copies the
     * other side is missing or holds older. When [remote] is only part of the picture (an
     * incremental listener event, or a watch that only carries recent tasks) pass
     * [includeLocalOnly] = false so tasks it doesn't mention aren't treated as missing.
     */
    suspend fun merge(
        remote: Collection<TaskRecord>,
        includeLocalOnly: Boolean = true
    ): SyncConflictResolver.Plan = database.withTransaction {
        val remoteIds = remote.mapTo(HashSet()) { it.id }
        val local = readRecords().let { all ->
            if (includeLocalOnly) all else all.filter { it.id in remoteIds }
        }
        SyncConflictResolver.plan(local, remote).also { apply(it.local) }
    }

    /** Deletes tasks here and leaves tombstones so other devices follow suit. */
    suspend fun deleteLocally(ids: Collection<UUID>, deletedAt: Long) = database.withTransaction {
        ids.forEach { taskDao.deleteById(it) }
        tombstoneDao.insertAll(ids.map { TaskTombstone(it, deletedAt) })
    }

    /** What the phone sends the watch: everything still in play, recent history, and deletions. */
    suspend fun phoneSnapshot(now: Long, mergedPeerSnapshots: Map<String, Long>): TaskSnapshot =
        database.withTransaction {
            val historyCutoff = now - WATCH_HISTORY_MS
            TaskSnapshot(
                generatedAt = now,
                tasks = taskDao.getAllTasksOnce().filter {
                    (!it.isCompleted && !it.isArchived) || it.updatedAt >= historyCutoff
                },
                tombstones = tombstoneDao.getAll(),
                mergedPeerSnapshots = mergedPeerSnapshots
            )
        }

    /** What the watch sends back: every copy it holds. */
    suspend fun fullSnapshot(now: Long): TaskSnapshot = database.withTransaction {
        TaskSnapshot(generatedAt = now, tasks = taskDao.getAllTasksOnce(), tombstones = tombstoneDao.getAll())
    }

    /**
     * Watch housekeeping after a phone snapshot: forget tasks the phone no longer sends, but
     * only copies the phone has already seen (changed no later than [seenUpTo]). Anything newer
     * is a change made on the watch that is still on its way to the phone.
     */
    suspend fun forgetTasksMissingFrom(snapshot: TaskSnapshot, seenUpTo: Long) = database.withTransaction {
        val stillSent = snapshot.tasks.mapTo(HashSet()) { it.id }
        taskDao.getAllTasksOnce()
            .filter { it.id !in stillSent && it.updatedAt <= seenUpTo }
            .forEach { taskDao.deleteById(it.id) }
    }

    suspend fun pruneTombstones(now: Long) = tombstoneDao.deleteOlderThan(now - TOMBSTONE_RETENTION_MS)

    private suspend fun readRecords(): List<TaskRecord> =
        taskDao.getAllTasksOnce().map { TaskRecord.Live(it) } +
            tombstoneDao.getAll().map { TaskRecord.Deleted(it) }

    private suspend fun apply(changes: SyncConflictResolver.LocalChanges) {
        if (changes.upserts.isNotEmpty()) {
            taskDao.insertAll(changes.upserts)
            changes.upserts.forEach { tombstoneDao.deleteById(it.id) }
        }
        if (changes.deletions.isNotEmpty()) {
            changes.deletions.forEach { taskDao.deleteById(it.id) }
            tombstoneDao.insertAll(changes.deletions)
        }
    }

    companion object {
        /** Completed or archived tasks the watch still hears about, so undo and streaks line up. */
        const val WATCH_HISTORY_MS = 14L * 24 * 60 * 60 * 1000

        /** Long enough for a device left in a drawer for a couple of months to catch up. */
        const val TOMBSTONE_RETENTION_MS = 90L * 24 * 60 * 60 * 1000
    }
}
