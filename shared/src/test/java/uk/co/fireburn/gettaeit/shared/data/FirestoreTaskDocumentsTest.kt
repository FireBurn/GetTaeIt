package uk.co.fireburn.gettaeit.shared.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.domain.sync.TaskRecord
import java.util.UUID

class FirestoreTaskDocumentsTest {

    @Test
    fun `task round-trips through its document`() {
        val task = TaskEntity(title = "Renew bus pass", context = TaskContext.WORK, updatedAt = 12L)

        val document = FirestoreTaskDocuments.toDocument(TaskRecord.Live(task))

        assertEquals(12L, document["updatedAt"])
        assertEquals(TaskRecord.Live(task), FirestoreTaskDocuments.fromDocument(task.id.toString(), document))
    }

    @Test
    fun `tombstone round-trips through its document`() {
        val tombstone = TaskTombstone(UUID.randomUUID(), deletedAt = 99L)

        val document = FirestoreTaskDocuments.toDocument(TaskRecord.Deleted(tombstone))

        assertEquals(mapOf("deleted" to true, "deletedAt" to 99L), document)
        assertEquals(TaskRecord.Deleted(tombstone), FirestoreTaskDocuments.fromDocument(tombstone.id.toString(), document))
    }

    @Test
    fun `document from before tombstones still reads`() {
        val task = TaskEntity(title = "Legacy")
        val legacy = mapOf<String, Any?>("json" to TaskJson.toJson(task))

        assertEquals(TaskRecord.Live(task), FirestoreTaskDocuments.fromDocument(task.id.toString(), legacy))
    }

    @Test
    fun `mismatched or malformed documents are skipped`() {
        val task = TaskEntity(title = "Moved")
        val document = FirestoreTaskDocuments.toDocument(TaskRecord.Live(task))

        assertNull(FirestoreTaskDocuments.fromDocument(UUID.randomUUID().toString(), document))
        assertNull(FirestoreTaskDocuments.fromDocument("not-a-uuid", document))
        assertNull(FirestoreTaskDocuments.fromDocument(task.id.toString(), mapOf("deleted" to true)))
        assertNull(FirestoreTaskDocuments.fromDocument(task.id.toString(), null))
    }
}
