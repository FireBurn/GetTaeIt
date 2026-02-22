package uk.co.fireburn.gettaeit.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.DataLayerSync
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import javax.inject.Inject

@HiltViewModel
class WearViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val dataLayerSync: DataLayerSync
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksForCurrentMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            if (completed) taskRepository.completeTask(task) else taskRepository.uncompleteTask(task)
            dataLayerSync.sendTaskUpdate(task.id, completed)
        }
    }
}
