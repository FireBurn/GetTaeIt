package uk.co.fireburn.gettaeit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.DataLayerSync
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.AppMode
import uk.co.fireburn.gettaeit.shared.domain.ContextManager
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.ai.GeminiService
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val contextManager: ContextManager,
    private val geminiService: GeminiService,
    private val dataLayerSync: DataLayerSync
) : ViewModel() {

    private val _isBreakingDownTask = MutableStateFlow(false)
    val isBreakingDownTask: StateFlow<Boolean> = _isBreakingDownTask.asStateFlow()

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksForCurrentMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val appMode: StateFlow<AppMode> = contextManager.appMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppMode.PERSONAL
        )

    fun breakdownAndAddTask(prompt: String) {
        viewModelScope.launch {
            _isBreakingDownTask.value = true
            try {
                val subTasks = geminiService.breakdownTask(prompt)
                val parentTaskId = UUID.randomUUID()
                val parentTask = TaskEntity(
                    id = parentTaskId,
                    title = prompt,
                    description = "Broken down by AI",
                    context = if (appMode.value == AppMode.WORK) TaskContext.WORK else TaskContext.PERSONAL,
                    dependencyIds = emptyList(),
                    locationTrigger = null,
                    wifiTrigger = null,
                    offsetReferenceId = null,
                    dueDate = null
                )
                taskRepository.addTask(parentTask)

                subTasks.forEach { subTask ->
                    val newTask = TaskEntity(
                        title = subTask.title,
                        description = subTask.description,
                        context = if (appMode.value == AppMode.WORK) TaskContext.WORK else TaskContext.PERSONAL,
                        dependencyIds = listOf(parentTaskId),
                        locationTrigger = null,
                        wifiTrigger = null,
                        offsetReferenceId = null,
                        dueDate = null
                    )
                    taskRepository.addTask(newTask)
                }
            } finally {
                _isBreakingDownTask.value = false
            }
        }
    }

    fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(isCompleted = completed))
            dataLayerSync.sendTaskUpdate(task.id, completed)
        }
    }
}
