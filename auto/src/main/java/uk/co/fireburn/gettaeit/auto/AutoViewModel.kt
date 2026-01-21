package uk.co.fireburn.gettaeit.auto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import javax.inject.Inject

@HiltViewModel
class AutoViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksForCurrentMode()
        .stateIn(
            // While a car screen is visible
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
