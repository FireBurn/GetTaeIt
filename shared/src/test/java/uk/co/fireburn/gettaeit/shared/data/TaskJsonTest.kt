package uk.co.fireburn.gettaeit.shared.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class TaskJsonTest {

    @Test
    fun `copy from an older app version gets current defaults instead of nulls`() {
        val json = """{"id":"00000000-0000-0000-0000-000000000003","title":"Old task","priority":2}"""

        val task = TaskJson.fromJson(json)

        assertNotNull(task)
        assertEquals(EffortLevel.MEDIUM, task!!.effortLevel)
        assertEquals(TaskContext.PERSONAL, task.context)
        assertEquals(RecurrenceConfig(), task.recurrence)
        assertEquals(emptyList<UUID>(), task.dependencyIds)
        assertEquals(2, task.priority)
    }

    @Test
    fun `json without an id or title is rejected`() {
        assertNull(TaskJson.fromJson("""{"title":"No id"}"""))
        assertNull(TaskJson.fromJson("""{"id":"00000000-0000-0000-0000-000000000004"}"""))
        assertNull(TaskJson.fromJson("not json"))
    }

    @Test
    fun `snapshot survives the trip over the Data Layer`() {
        val snapshot = TaskSnapshot(
            generatedAt = 42L,
            tasks = listOf(
                TaskEntity(title = "Hoover", effortLevel = EffortLevel.LOW, dependencyIds = listOf(UUID.randomUUID())),
                TaskEntity(title = "Timesheet", context = TaskContext.WORK, isCompleted = true)
            ),
            tombstones = listOf(TaskTombstone(UUID.randomUUID(), 40L)),
            mergedPeerSnapshots = mapOf("watch-node" to 41L)
        )

        val decoded = TaskSnapshotCodec.decode(TaskSnapshotCodec.encode(snapshot))

        assertEquals(snapshot.generatedAt, decoded!!.generatedAt)
        assertEquals(snapshot.tasks.toSet(), decoded.tasks.toSet())
        assertEquals(snapshot.tombstones, decoded.tombstones)
        assertEquals(snapshot.mergedPeerSnapshots, decoded.mergedPeerSnapshots)
    }

    @Test
    fun `fingerprint ignores generation time but not content`() {
        val task = TaskEntity(title = "Hoover")
        val snapshot = TaskSnapshot(1L, listOf(task), emptyList())

        assertEquals(
            TaskSnapshotCodec.fingerprint(snapshot),
            TaskSnapshotCodec.fingerprint(snapshot.copy(generatedAt = 99L))
        )
        assertNotEquals(
            TaskSnapshotCodec.fingerprint(snapshot),
            TaskSnapshotCodec.fingerprint(snapshot.copy(tasks = listOf(task.copy(isCompleted = true))))
        )
    }

    @Test
    fun `corrupt snapshot bytes are ignored rather than crashing`() {
        assertNull(TaskSnapshotCodec.decode(byteArrayOf(1, 2, 3)))
    }
}
