package uk.co.fireburn.gettaeit.shared.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import java.util.UUID

enum class TaskContext {
    WORK,
    PERSONAL,
    ANY
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String?,
    val isCompleted: Boolean = false,
    val context: TaskContext = TaskContext.ANY,
    val priority: Int = 3, // 1 (Highest) to 5 (Lowest)
    val dependencyIds: List<UUID> = emptyList(),
    val locationTrigger: LatLng?,
    val wifiTrigger: String?, // SSID
    val offsetReferenceId: UUID?, // For linking prep-tasks to a main event
    val dueDate: Long? // Store as timestamp
)
