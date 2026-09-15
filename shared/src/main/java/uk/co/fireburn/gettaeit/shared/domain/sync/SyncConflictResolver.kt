package uk.co.fireburn.gettaeit.shared.domain.sync

import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.data.TaskTombstone
import java.util.UUID

/** One device's copy of a task: the task itself, or the fact that it was deleted. */
sealed interface TaskRecord {
    val id: UUID
    val changedAt: Long

    data class Live(val task: TaskEntity) : TaskRecord {
        override val id: UUID get() = task.id
        override val changedAt: Long get() = task.updatedAt
    }

    data class Deleted(val tombstone: TaskTombstone) : TaskRecord {
        override val id: UUID get() = tombstone.id
        override val changedAt: Long get() = tombstone.deletedAt
    }
}

/**
 * The conflict rules shared by Firestore backup and the watch.
 *
 * 1. **Offline edits** — whichever copy changed most recently (`updatedAt`, or
 *    `deletedAt` for a deletion) wins, however long a device was offline.
 * 2. **Delete vs edit** — deletions are timestamped tombstones, so an edit made
 *    after a delete brings the task back, and an older edit can't resurrect it.
 * 3. **Same-instant ties** — a deletion beats an edit, a completed copy beats an
 *    uncompleted one (two devices completing together agree), and anything left is
 *    broken on content so every device picks the same winner.
 * 4. **Repeated delivery** — merging a record that has already won changes nothing,
 *    so replayed listener events and duplicate Data Layer items are harmless.
 */
object SyncConflictResolver {

    fun winner(a: TaskRecord?, b: TaskRecord?): TaskRecord? {
        if (a == null) return b
        if (b == null) return a
        if (a.changedAt != b.changedAt) return if (a.changedAt > b.changedAt) a else b
        return when {
            a is TaskRecord.Deleted -> a
            b is TaskRecord.Deleted -> b
            else -> {
                val taskA = (a as TaskRecord.Live).task
                val taskB = (b as TaskRecord.Live).task
                when {
                    taskA.isCompleted != taskB.isCompleted -> if (taskA.isCompleted) a else b
                    else -> if (taskA.toString() >= taskB.toString()) a else b
                }
            }
        }
    }

    /** What has to change on this device so it holds the winning copy of every task. */
    data class LocalChanges(
        val upserts: List<TaskEntity>,
        val deletions: List<TaskTombstone>
    ) {
        val isEmpty: Boolean get() = upserts.isEmpty() && deletions.isEmpty()
    }

    /** [local] changes to apply here, and records the other side is missing or holds older. */
    data class Plan(
        val local: LocalChanges,
        val pushToRemote: List<TaskRecord>
    )

    fun plan(local: Collection<TaskRecord>, remote: Collection<TaskRecord>): Plan {
        val localById = local.newestById()
        val remoteById = remote.newestById()
        val upserts = mutableListOf<TaskEntity>()
        val deletions = mutableListOf<TaskTombstone>()
        val push = mutableListOf<TaskRecord>()

        for (id in localById.keys + remoteById.keys) {
            val mine = localById[id]
            val theirs = remoteById[id]
            val winner = winner(mine, theirs) ?: continue
            if (winner != mine) {
                when (winner) {
                    is TaskRecord.Live -> upserts += winner.task
                    is TaskRecord.Deleted -> deletions += winner.tombstone
                }
            }
            if (winner != theirs) push += winner
        }
        return Plan(LocalChanges(upserts, deletions), push)
    }

    private fun Collection<TaskRecord>.newestById(): Map<UUID, TaskRecord> =
        groupBy { it.id }.mapValues { (_, copies) -> copies.reduce { a, b -> winner(a, b)!! } }
}
