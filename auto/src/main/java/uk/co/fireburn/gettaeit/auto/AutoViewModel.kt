package uk.co.fireburn.gettaeit.auto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.ai.HybridTaskService
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AutoViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val hybridTaskService: HybridTaskService
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksForCurrentMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.completeTask(task)
        }
    }

    fun addTasksFromVoice(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            val parsedList = hybridTaskService.parsePrompt(prompt)
            parsedList.forEach { parsed ->
                val parentId = UUID.randomUUID()

                taskRepository.addTask(
                    TaskEntity(
                        id = parentId,
                        title = parsed.title,
                        context = parsed.suggestedContext,
                        priority = 3, // Default normal priority
                        estimatedMinutes = parsed.estimatedMinutes
                    )
                )

                if (parsed.subtasks.isNotEmpty()) {
                    taskRepository.addAll(parsed.subtasks.map { sub ->
                        TaskEntity(
                            title = sub.title,
                            context = parsed.suggestedContext,
                            priority = (3 + sub.priorityOffset).coerceIn(1, 5),
                            parentId = parentId,
                            isSubtask = true,
                            estimatedMinutes = sub.estimatedMinutes
                        )
                    })
                }
            }
        }
    }
}
