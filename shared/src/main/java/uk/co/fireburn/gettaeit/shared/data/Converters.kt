package uk.co.fireburn.gettaeit.shared.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class Converters {

    private val gson = Gson()

    // ── UUID List ────────────────────────────────────────────────────────────
    @TypeConverter
    fun fromUUIDList(v: List<UUID>?): String? = v?.let { gson.toJson(it) }

    @TypeConverter
    fun toUUIDList(v: String?): List<UUID>? =
        v?.let { gson.fromJson(it, object : TypeToken<List<UUID>>() {}.type) }

    // ── UUID (single, nullable) ──────────────────────────────────────────────
    @TypeConverter
    fun fromUUID(v: UUID?): String? = v?.toString()

    @TypeConverter
    fun toUUID(v: String?): UUID? = v?.let { UUID.fromString(it) }

    // ── Int List ─────────────────────────────────────────────────────────────
    @TypeConverter
    fun fromIntList(v: List<Int>?): String? = v?.let { gson.toJson(it) }

    @TypeConverter
    fun toIntList(v: String?): List<Int>? =
        v?.let { gson.fromJson(it, object : TypeToken<List<Int>>() {}.type) }

    // ── RecurrenceConfig ─────────────────────────────────────────────────────
    @TypeConverter
    fun fromRecurrenceConfig(v: RecurrenceConfig?): String? = v?.let { gson.toJson(it) }

    @TypeConverter
    fun toRecurrenceConfig(v: String?): RecurrenceConfig? =
        v?.let { gson.fromJson(it, RecurrenceConfig::class.java) }

    // ── TaskContext enum ─────────────────────────────────────────────────────
    @TypeConverter
    fun fromTaskContext(v: TaskContext?): String? = v?.name

    @TypeConverter
    fun toTaskContext(v: String?): TaskContext? = v?.let { TaskContext.valueOf(it) }
}
