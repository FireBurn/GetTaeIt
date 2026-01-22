package uk.co.fireburn.gettaeit.shared.domain

import kotlinx.coroutines.flow.Flow
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.UUID

interface TaskRepository {
    fun getTasksForCurrentMode(): Flow<List<TaskEntity>>
    suspend fun getTaskById(id: UUID): TaskEntity?
    suspend fun addTask(task: TaskEntity)
    suspend fun updateTask(task: TaskEntity)
}
