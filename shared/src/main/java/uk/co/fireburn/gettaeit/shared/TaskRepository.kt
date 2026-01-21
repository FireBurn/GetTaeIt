package uk.co.fireburn.gettaeit.shared

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.co.fireburn.gettaeit.shared.data.TaskDao
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val contextManager: ContextManager
) {

    fun getTasksForCurrentMode(): Flow<List<TaskEntity>> {
        return combine(
            taskDao.getAllTasks(),
            contextManager.appContext
        ) { tasks, appContext ->
            when (appContext) {
                AppContext.WORK -> tasks.filter { it.context == TaskContext.WORK || it.context == TaskContext.ANY }
                AppContext.PERSONAL -> tasks.filter { it.context == TaskContext.PERSONAL || it.context == TaskContext.ANY }
                AppContext.COMMUTE -> tasks.filter { it.context == TaskContext.ANY } // Or some other logic for commute
            }
        }
    }
}
