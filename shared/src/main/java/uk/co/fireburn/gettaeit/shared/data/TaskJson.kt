package uk.co.fireburn.gettaeit.shared.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * The one JSON shape a [TaskEntity] travels in, for Firestore and the watch alike.
 *
 * Gson bypasses Kotlin constructors, so a copy written by an older app version (before
 * effort levels existed, say) would arrive with nulls in non-null fields and blow up on
 * the Room insert. [fromJson] fills those gaps with the same defaults a fresh task gets.
 */
object TaskJson {
    private val gson = Gson()

    fun toJson(task: TaskEntity): String = gson.toJson(task)

    fun fromJson(json: String): TaskEntity? =
        runCatching { gson.fromJson(json, TaskEntity::class.java) }.getOrNull()?.withMissingFieldsDefaulted()

    internal fun toTree(task: TaskEntity) = gson.toJsonTree(task)

    internal fun fromTree(tree: com.google.gson.JsonElement): TaskEntity? =
        runCatching { gson.fromJson(tree, TaskEntity::class.java) }.getOrNull()?.withMissingFieldsDefaulted()

    private fun TaskEntity.withMissingFieldsDefaulted(): TaskEntity? {
        if ((id as UUID?) == null || (title as String?) == null) return null
        val recurrence = (recurrence as RecurrenceConfig?)?.let {
            it.copy(
                type = it.type.orIfMissing(RecurrenceType.NONE),
                daysOfWeek = it.daysOfWeek.orIfMissing(emptyList()),
                missedBehaviour = it.missedBehaviour.orIfMissing(MissedBehaviour.IGNORABLE),
                dailySlotMinutes = it.dailySlotMinutes.orIfMissing(emptyList())
            )
        } ?: RecurrenceConfig()
        return copy(
            context = context.orIfMissing(TaskContext.PERSONAL),
            effortLevel = effortLevel.orIfMissing(EffortLevel.MEDIUM),
            recurrence = recurrence,
            dependencyIds = dependencyIds.orIfMissing(emptyList())
        )
    }

    // Generic so the null check survives compilation even though the declared type is non-null.
    private fun <T> T.orIfMissing(default: T): T = (this as T?) ?: default
}

/**
 * A device's tasks as sent over the Wear Data Layer.
 *
 * [mergedPeerSnapshots] is only filled in by the phone: for each watch node, the
 * [generatedAt] of the newest watch snapshot it has merged. The watch uses that to
 * tell "the phone dropped this because it's old news" from "the phone hasn't seen it yet".
 */
data class TaskSnapshot(
    val generatedAt: Long,
    val tasks: List<TaskEntity>,
    val tombstones: List<TaskTombstone>,
    val mergedPeerSnapshots: Map<String, Long> = emptyMap()
)

object TaskSnapshotCodec {
    private val gson = Gson()

    fun encode(snapshot: TaskSnapshot): ByteArray {
        val bytes = ByteArrayOutputStream()
        GZIPOutputStream(bytes).use { it.write(toJson(snapshot).toByteArray(Charsets.UTF_8)) }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): TaskSnapshot? = runCatching {
        val json = GZIPInputStream(bytes.inputStream()).use { it.readBytes().toString(Charsets.UTF_8) }
        val root = JsonParser.parseString(json).asJsonObject
        TaskSnapshot(
            generatedAt = root.get("generatedAt").asLong,
            tasks = root.getAsJsonArray("tasks").mapNotNull { TaskJson.fromTree(it) },
            tombstones = root.getAsJsonArray("tombstones").mapNotNull { element ->
                val obj = element.asJsonObject
                runCatching {
                    TaskTombstone(UUID.fromString(obj.get("id").asString), obj.get("deletedAt").asLong)
                }.getOrNull()
            },
            mergedPeerSnapshots = root.getAsJsonObject("mergedPeerSnapshots")
                ?.entrySet()?.associate { (node, at) -> node to at.asLong }
                .orEmpty()
        )
    }.getOrNull()

    /** Identifies the content, ignoring when it was generated, so unchanged data isn't resent. */
    fun fingerprint(snapshot: TaskSnapshot): String = toJson(snapshot.copy(generatedAt = 0L))

    private fun toJson(snapshot: TaskSnapshot): String {
        val root = JsonObject()
        root.addProperty("generatedAt", snapshot.generatedAt)
        root.add("tasks", JsonArray().apply {
            snapshot.tasks.sortedBy { it.id }.forEach { add(TaskJson.toTree(it)) }
        })
        root.add("tombstones", JsonArray().apply {
            snapshot.tombstones.sortedBy { it.id }.forEach { tombstone ->
                add(JsonObject().apply {
                    addProperty("id", tombstone.id.toString())
                    addProperty("deletedAt", tombstone.deletedAt)
                })
            }
        })
        root.add("mergedPeerSnapshots", JsonObject().apply {
            snapshot.mergedPeerSnapshots.toSortedMap().forEach { (node, at) -> addProperty(node, at) }
        })
        return gson.toJson(root)
    }
}
