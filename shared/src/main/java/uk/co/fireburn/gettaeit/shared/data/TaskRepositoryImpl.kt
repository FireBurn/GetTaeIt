package uk.co.fireburn.gettaeit.shared.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.co.fireburn.gettaeit.shared.domain.AppMode
import uk.co.fireburn.gettaeit.shared.domain.ContextManager
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val contextManager: ContextManager
) : TaskRepository {

    override fun getTasksForCurrentMode(): Flow<List<TaskEntity>> {
        return combine(taskDao.getAllTasks(), contextManager.appMode) { tasks, appMode ->
            when (appMode) {
                AppMode.WORK -> tasks.filter { it.context == TaskContext.WORK || it.context == TaskContext.ANY }
                AppMode.PERSONAL -> tasks.filter { it.context == TaskContext.PERSONAL || it.context == TaskContext.ANY }
                else -> tasks
            }
        }
    }

    override suspend fun getTaskById(id: UUID): TaskEntity? {
        return taskDao.getTaskByIdOnce(id)
    }

    override suspend fun addTask(task: TaskEntity) {
        taskDao.insert(task)
    }

    override suspend fun updateTask(task: TaskEntity) {
        taskDao.update(task)
    }
}
