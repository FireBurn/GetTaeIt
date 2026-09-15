package uk.co.fireburn.gettaeit.shared.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uk.co.fireburn.gettaeit.shared.domain.sync.TaskRecord

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaskSyncStoreTest {

    private lateinit var database: AppDatabase
    private lateinit var store: TaskSyncStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        store = TaskSyncStore(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `local deletion leaves a tombstone that stops a stale copy coming back`() = runTest {
        val task = TaskEntity(title = "Bins", updatedAt = 1_000L)
        database.taskDao().insert(task)

        store.deleteLocally(listOf(task.id), deletedAt = 2_000L)
        val plan = store.merge(listOf(TaskRecord.Live(task)))

        assertNull(database.taskDao().getTaskById(task.id))
        assertEquals(listOf(TaskTombstone(task.id, 2_000L)), database.taskTombstoneDao().getAll())
        assertEquals(listOf(TaskRecord.Deleted(TaskTombstone(task.id, 2_000L))), plan.pushToRemote)
    }

    @Test
    fun `merge applies remote edits and deletions in one go`() = runTest {
        val edited = TaskEntity(title = "Ironing", updatedAt = 1_000L)
        val doomed = TaskEntity(title = "Old plan", updatedAt = 1_000L)
        database.taskDao().insertAll(listOf(edited, doomed))

        store.merge(
            listOf(
                TaskRecord.Live(edited.copy(isCompleted = true, updatedAt = 3_000L)),
                TaskRecord.Deleted(TaskTombstone(doomed.id, 3_000L))
            )
        )

        assertTrue(database.taskDao().getTaskById(edited.id)!!.isCompleted)
        assertNull(database.taskDao().getTaskById(doomed.id))
    }

    @Test
    fun `edit made after a deletion restores the task and clears its tombstone`() = runTest {
        val task = TaskEntity(title = "Dentist", updatedAt = 1_000L)
        store.deleteLocally(listOf(task.id), deletedAt = 2_000L)

        store.merge(listOf(TaskRecord.Live(task.copy(updatedAt = 3_000L))))

        assertNotNull(database.taskDao().getTaskById(task.id))
        assertTrue(database.taskTombstoneDao().getAll().isEmpty())
    }

    @Test
    fun `partial merge does not offer to push tasks it was not told about`() = runTest {
        database.taskDao().insert(TaskEntity(title = "Only on this device"))

        val plan = store.merge(emptyList(), includeLocalOnly = false)

        assertTrue(plan.pushToRemote.isEmpty())
    }

    @Test
    fun `watch forgets tasks the phone dropped only once the phone has seen them`() = runTest {
        val oldNews = TaskEntity(title = "Done last month", isCompleted = true, updatedAt = 1_000L)
        val notYetSent = TaskEntity(title = "Added on the watch", updatedAt = 5_000L)
        database.taskDao().insertAll(listOf(oldNews, notYetSent))

        store.forgetTasksMissingFrom(TaskSnapshot(6_000L, emptyList(), emptyList()), seenUpTo = 4_000L)

        assertNull(database.taskDao().getTaskById(oldNews.id))
        assertNotNull(database.taskDao().getTaskById(notYetSent.id))
    }

    @Test
    fun `phone snapshot carries active tasks and recent history only`() = runTest {
        val now = TaskSyncStore.WATCH_HISTORY_MS * 2
        val active = TaskEntity(title = "Active", updatedAt = 0L)
        val recentlyDone = TaskEntity(title = "Recently done", isCompleted = true, updatedAt = now - 1_000L)
        val longDone = TaskEntity(title = "Long done", isCompleted = true, updatedAt = 0L)
        database.taskDao().insertAll(listOf(active, recentlyDone, longDone))

        val snapshot = store.phoneSnapshot(now, mapOf("watch" to 7L))

        assertEquals(setOf(active.id, recentlyDone.id), snapshot.tasks.map { it.id }.toSet())
        assertEquals(mapOf("watch" to 7L), snapshot.mergedPeerSnapshots)
    }
}
