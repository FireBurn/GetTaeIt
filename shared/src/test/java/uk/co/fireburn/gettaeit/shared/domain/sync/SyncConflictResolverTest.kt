package uk.co.fireburn.gettaeit.shared.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.data.TaskTombstone
import uk.co.fireburn.gettaeit.shared.domain.sync.TaskRecord.Deleted
import uk.co.fireburn.gettaeit.shared.domain.sync.TaskRecord.Live
import java.util.UUID

class SyncConflictResolverTest {

    private val id = UUID.fromString("00000000-0000-0000-0000-00000000000a")
    private val base = TaskEntity(id = id, title = "Phone the dentist", updatedAt = 1_000L)

    @Test
    fun `offline edit made later wins over the older cloud copy`() {
        val offlineEdit = base.copy(title = "Phone the dentist before 5", updatedAt = 5_000L)
        val cloudCopy = base.copy(updatedAt = 2_000L)

        val plan = SyncConflictResolver.plan(local = listOf(Live(offlineEdit)), remote = listOf(Live(cloudCopy)))

        assertTrue(plan.local.isEmpty)
        assertEquals(listOf(Live(offlineEdit)), plan.pushToRemote)
    }

    @Test
    fun `newer remote edit replaces the stale local copy`() {
        val stale = base.copy(updatedAt = 2_000L)
        val newer = base.copy(priority = 1, updatedAt = 3_000L)

        val plan = SyncConflictResolver.plan(local = listOf(Live(stale)), remote = listOf(Live(newer)))

        assertEquals(listOf(newer), plan.local.upserts)
        assertTrue(plan.pushToRemote.isEmpty())
    }

    @Test
    fun `deletion after an edit removes the task everywhere`() {
        val edited = base.copy(title = "Edited on the watch", updatedAt = 2_000L)
        val deletion = TaskTombstone(id, deletedAt = 3_000L)

        val plan = SyncConflictResolver.plan(local = listOf(Live(edited)), remote = listOf(Deleted(deletion)))

        assertEquals(listOf(deletion), plan.local.deletions)
        assertTrue(plan.local.upserts.isEmpty())
    }

    @Test
    fun `edit made after a deletion brings the task back`() {
        val deletion = TaskTombstone(id, deletedAt = 3_000L)
        val laterEdit = base.copy(title = "Actually still need this", updatedAt = 4_000L)

        val plan = SyncConflictResolver.plan(local = listOf(Deleted(deletion)), remote = listOf(Live(laterEdit)))

        assertEquals(listOf(laterEdit), plan.local.upserts)
    }

    @Test
    fun `an older copy cannot resurrect a deleted task`() {
        val deletion = TaskTombstone(id, deletedAt = 3_000L)

        val plan = SyncConflictResolver.plan(local = listOf(Deleted(deletion)), remote = listOf(Live(base)))

        assertTrue(plan.local.isEmpty)
        assertEquals(listOf(Deleted(deletion)), plan.pushToRemote)
    }

    @Test
    fun `completing on two devices at the same instant agrees on the completed copy`() {
        val completed = base.copy(isCompleted = true, completedAt = 7_000L, updatedAt = 7_000L)
        val snoozed = base.copy(isSnoozed = true, snoozedUntil = 9_000L, updatedAt = 7_000L)

        assertEquals(Live(completed), SyncConflictResolver.winner(Live(completed), Live(snoozed)))
        assertEquals(Live(completed), SyncConflictResolver.winner(Live(snoozed), Live(completed)))
    }

    @Test
    fun `same-instant ties pick the same winner whichever side asks`() {
        val a = base.copy(title = "A", updatedAt = 7_000L)
        val b = base.copy(title = "B", updatedAt = 7_000L)
        val deletion = Deleted(TaskTombstone(id, deletedAt = 7_000L))

        assertEquals(SyncConflictResolver.winner(Live(a), Live(b)), SyncConflictResolver.winner(Live(b), Live(a)))
        assertEquals(deletion, SyncConflictResolver.winner(Live(a), deletion))
        assertEquals(deletion, SyncConflictResolver.winner(deletion, Live(a)))
    }

    @Test
    fun `replaying records that already won changes nothing`() {
        val other = TaskEntity(title = "Bins", updatedAt = 1_500L)
        val remote = listOf(Live(base.copy(updatedAt = 4_000L)), Live(other))

        val first = SyncConflictResolver.plan(local = listOf(Live(base)), remote = remote)
        val local = listOf(Live(base.copy(updatedAt = 4_000L)), Live(other))
        val replay = SyncConflictResolver.plan(local = local, remote = remote)

        assertEquals(2, first.local.upserts.size)
        assertTrue(replay.local.isEmpty)
        assertTrue(replay.pushToRemote.isEmpty())
    }

    @Test
    fun `tasks only one side knows about flow to the other`() {
        val localOnly = TaskEntity(title = "Made offline", updatedAt = 1L)
        val remoteOnly = TaskEntity(title = "Made on the other phone", updatedAt = 2L)
        val remoteDeletion = TaskTombstone(UUID.randomUUID(), deletedAt = 3L)

        val plan = SyncConflictResolver.plan(
            local = listOf(Live(localOnly)),
            remote = listOf(Live(remoteOnly), Deleted(remoteDeletion))
        )

        assertEquals(listOf(remoteOnly), plan.local.upserts)
        assertEquals(listOf(remoteDeletion), plan.local.deletions)
        assertEquals(listOf(Live(localOnly)), plan.pushToRemote)
    }
}
