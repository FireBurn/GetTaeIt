package uk.co.fireburn.gettaeit.shared.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.domain.AppMode
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository
import uk.co.fireburn.gettaeit.shared.domain.ContextManager
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val contextManager: ContextManager,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : TaskRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        repositoryScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    syncTasksFromFirestore(user.uid)
                } else {
                    // Handle user signed out: maybe clear local tasks?
                }
            }
        }
    }

    override fun getTasksForCurrentMode(): Flow<List<TaskEntity>> {
        return authRepository.currentUser.flatMapLatest { user ->
            val localTasksFlow = if (user != null) {
                taskDao.getAllTasksForUser(user.uid) // Needs to be added to DAO
            } else {
                taskDao.getAllLocalTasks() // Needs to be added to DAO (for logged-out state)
            }

            combine(localTasksFlow, contextManager.appMode) { tasks, appMode ->
                when (appMode) {
                    AppMode.WORK -> tasks.filter { it.context == TaskContext.WORK || it.context == TaskContext.ANY }
                    AppMode.PERSONAL -> tasks.filter { it.context == TaskContext.PERSONAL || it.context == TaskContext.ANY }
                    else -> tasks
                }
            }
        }
    }

    override suspend fun getTaskById(id: UUID): TaskEntity? {
        return taskDao.getTaskByIdOnce(id)
    }

    override suspend fun addTask(task: TaskEntity) {
        val user = authRepository.currentUser.first()
        val taskWithUser = task.copy(userId = user?.uid)

        taskDao.insert(taskWithUser)
        user?.let {
            firestore.collection("users").document(it.uid).collection("tasks")
                .document(taskWithUser.id.toString()).set(taskWithUser)
        }
    }

    override suspend fun updateTask(task: TaskEntity) {
        taskDao.update(task)
        task.userId?.let { userId ->
            firestore.collection("users").document(userId).collection("tasks")
                .document(task.id.toString()).set(task)
        }
    }

    private fun syncTasksFromFirestore(userId: String) {
        firestore.collection("users").document(userId).collection("tasks")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { doc ->
                    val task = doc.toObject(TaskEntity::class.java)
                    task?.let {
                        repositoryScope.launch {
                            taskDao.insert(it) // Insert or update from remote
                        }
                    }
                }
            }
    }
}
