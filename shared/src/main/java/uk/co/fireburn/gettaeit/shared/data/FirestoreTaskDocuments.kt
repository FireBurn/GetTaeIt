package uk.co.fireburn.gettaeit.shared.data

import uk.co.fireburn.gettaeit.shared.domain.sync.TaskRecord
import java.util.UUID

/**
 * How a task lives in `users/{uid}/tasks/{taskId}`: either `{json, updatedAt}` or a
 * tombstone `{deleted: true, deletedAt}`. Kept apart from the Firestore SDK so the
 * mapping can be tested on the JVM. Documents written before tombstones existed only
 * carry `json`, which still reads back fine.
 */
internal object FirestoreTaskDocuments {

    fun toDocument(record: TaskRecord): Map<String, Any> = when (record) {
        is TaskRecord.Live -> mapOf(
            "json" to TaskJson.toJson(record.task),
            "updatedAt" to record.task.updatedAt
        )
        is TaskRecord.Deleted -> mapOf(
            "deleted" to true,
            "deletedAt" to record.tombstone.deletedAt
        )
    }

    fun fromDocument(documentId: String, data: Map<String, Any?>?): TaskRecord? {
        val id = runCatching { UUID.fromString(documentId) }.getOrNull() ?: return null
        if (data == null) return null
        if (data["deleted"] == true) {
            val deletedAt = (data["deletedAt"] as? Number)?.toLong() ?: return null
            return TaskRecord.Deleted(TaskTombstone(id, deletedAt))
        }
        val task = (data["json"] as? String)?.let(TaskJson::fromJson) ?: return null
        return if (task.id == id) TaskRecord.Live(task) else null
    }
}
