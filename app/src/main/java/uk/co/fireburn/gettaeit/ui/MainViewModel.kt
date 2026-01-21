package uk.co.fireburn.gettaeit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.AppContext
import uk.co.fireburn.gettaeit.shared.ContextManager
import uk.co.fireburn.gettaeit.shared.TaskRepository
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val contextManager: ContextManager
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksForCurrentMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val appContext: StateFlow<AppContext> = contextManager.appContext
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSub-scribed(5000),
            initialValue = AppContext.PERSONAL
        )

    fun addTask(title: String, description: String?) {
        viewModelScope.launch {
            val newTask = TaskEntity(
                title = title,
                description = description,
                context = if (appContext.value == AppContext.WORK) TaskContext.WORK else TaskContext.PERSONAL,
                locationTrigger = null,
                wifiTrigger = null,
                offsetReferenceId = null,
                dueDate = null
            )
            taskRepository.insertTask(newTask)
        }
    }
}
