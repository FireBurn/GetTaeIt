package uk.co.fireburn.gettaeit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import uk.co.fireburn.gettaeit.shared.AppContext
import uk.co.fireburn.gettaeit.shared.ContextManager
import uk.co.fireburn.gettaeit.shared.TaskRepository
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
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppContext.PERSONAL
        )
}
