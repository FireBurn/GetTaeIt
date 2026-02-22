package uk.co.fireburn.gettaeit.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.DataLayerSync
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WearViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val dataLayerSync: DataLayerSync
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksForCurrentMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSendingVoice = MutableStateFlow(false)

    fun getSubtasks(parentId: UUID): Flow<List<TaskEntity>> =
        taskRepository.getSubtasks(parentId)

    fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            if (completed) taskRepository.completeTask(task) else taskRepository.uncompleteTask(task)
            // Sync completion state to phone
            dataLayerSync.sendTaskUpdate(task.id, completed)
            // Auto-complete parent if all subtasks done
            if (completed && task.isSubtask) {
                task.parentId?.let { taskRepository.autoCompleteParentIfDone(it) }
            }
        }
    }

    fun snoozeTask(task: TaskEntity) {
        viewModelScope.launch {
            val untilMs = System.currentTimeMillis() + 2 * 3_600_000L
            taskRepository.snoozeTask(task, untilMs)
        }
    }

    /**
     * Sends a voice-dictated task string to the phone for AI parsing and saving.
     * Falls back to saving locally if phone is unreachable.
     */
    fun sendVoiceTaskToPhone(text: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isSendingVoice.value = true
            try {
                val sent = dataLayerSync.sendVoiceTask(text)
                if (!sent) {
                    // Phone not reachable — save locally with no date
                    taskRepository.addTask(
                        TaskEntity(title = text.replaceFirstChar { it.uppercase() })
                    )
                }
                onResult(sent)
            } catch (_: Exception) {
                onResult(false)
            } finally {
                isSendingVoice.value = false
            }
        }
    }
}
