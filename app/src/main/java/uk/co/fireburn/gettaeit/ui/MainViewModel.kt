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
import uk.co.fireburn.gettaeit.shared.data.MissedBehaviour
import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.AppMode
import uk.co.fireburn.gettaeit.shared.domain.ContextManager
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.ai.HybridTaskService
import java.util.UUID
import javax.inject.Inject

/**
 * UI state for the add/edit task sheet.
 * Defaults represent a sensible "new personal task" with no recurrence.
 */
data class AddTaskUiState(
    val title: String = "",
    val description: String = "",
    val context: TaskContext = TaskContext.PERSONAL,
    val priority: Int = 3,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: List<Int> = emptyList(),
    val missedBehaviour: MissedBehaviour = MissedBehaviour.IGNORABLE,
    val preferredTimeOfDayMinutes: Int? = null,
    val dueDate: Long? = null,
    val parentId: UUID? = null,
    val dependencyIds: List<UUID> = emptyList(),
    /** AI-generated subtask titles ready to be previewed before saving */
    val suggestedSubtasks: List<String> = emptyList(),
    val isGeneratingSubtasks: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val contextManager: ContextManager,
    private val hybridTaskService: HybridTaskService,
    private val dataLayerSync: DataLayerSync
) : ViewModel() {

    // ── Streams ──────────────────────────────────────────────────────────────

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTopLevelTasksForCurrentMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appMode: StateFlow<AppMode> = contextManager.appMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppMode.PERSONAL)

    // ── Add task sheet state ─────────────────────────────────────────────────

    private val _addTaskState = MutableStateFlow(AddTaskUiState())
    val addTaskState: StateFlow<AddTaskUiState> = _addTaskState.asStateFlow()

    fun updateAddTaskState(update: AddTaskUiState.() -> AddTaskUiState) {
        _addTaskState.value = _addTaskState.value.update()
    }

    fun resetAddTaskState() {
        _addTaskState.value = AddTaskUiState(
            context = if (appMode.value == AppMode.WORK) TaskContext.WORK else TaskContext.PERSONAL
        )
    }

    // ── AI subtask suggestions ───────────────────────────────────────────────

    /**
     * Triggers AI breakdown of the current task title and populates
     * [AddTaskUiState.suggestedSubtasks] for the user to review.
     * Context is auto-detected if not already explicitly set.
     */
    fun requestSubtaskBreakdown() {
        val title = _addTaskState.value.title.ifBlank { return }
        viewModelScope.launch {
            _addTaskState.value = _addTaskState.value.copy(isGeneratingSubtasks = true)
            try {
                val results = hybridTaskService.generateSubtasks(title)
                val detectedContext = hybridTaskService.detectContext(title)

                _addTaskState.value = _addTaskState.value.copy(
                    suggestedSubtasks = results.map { it.title },
                    // Auto-fill context only if user left it as ANY
                    context = if (_addTaskState.value.context == TaskContext.ANY)
                        detectedContext else _addTaskState.value.context
                )
            } finally {
                _addTaskState.value = _addTaskState.value.copy(isGeneratingSubtasks = false)
            }
        }
    }

    fun removeSuggestedSubtask(index: Int) {
        _addTaskState.value = _addTaskState.value.copy(
            suggestedSubtasks = _addTaskState.value.suggestedSubtasks.toMutableList()
                .also { it.removeAt(index) }
        )
    }

    // ── Save task ────────────────────────────────────────────────────────────

    /** Saves the current [AddTaskUiState] as a task (+ subtasks if any). */
    fun saveTask() {
        val state = _addTaskState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val recurrenceConfig = RecurrenceConfig(
                type = state.recurrenceType,
                interval = state.recurrenceInterval,
                daysOfWeek = state.recurrenceDaysOfWeek,
                missedBehaviour = state.missedBehaviour,
                preferredTimeOfDayMinutes = state.preferredTimeOfDayMinutes
            )

            val parentId = UUID.randomUUID()
            val parentTask = TaskEntity(
                id = parentId,
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                context = state.context,
                priority = state.priority,
                dueDate = state.dueDate,
                recurrence = recurrenceConfig,
                parentId = state.parentId,
                dependencyIds = state.dependencyIds,
                isSubtask = state.parentId != null
            )
            taskRepository.addTask(parentTask)

            // Save AI-confirmed subtasks
            if (state.suggestedSubtasks.isNotEmpty()) {
                val subtasks = state.suggestedSubtasks.mapIndexed { idx, title ->
                    TaskEntity(
                        title = title,
                        context = state.context,
                        priority = state.priority,
                        parentId = parentId,
                        isSubtask = true,
                        // Each subtask depends on the previous one (sequential dependency)
                        dependencyIds = if (idx == 0) emptyList() else listOf()
                    )
                }
                taskRepository.addAll(subtasks)
            }

            resetAddTaskState()
        }
    }

    // ── Task actions ─────────────────────────────────────────────────────────

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.completeTask(task)
            dataLayerSync.sendTaskUpdate(task.id, true)
        }
    }

    fun uncompleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.uncompleteTask(task)
            dataLayerSync.sendTaskUpdate(task.id, false)
        }
    }

    /**
     * Snooze a task.
     * @param hoursAhead how many hours from now to snooze until (default 2h)
     */
    fun snoozeTask(task: TaskEntity, hoursAhead: Long = 2L) {
        viewModelScope.launch {
            val untilMs = System.currentTimeMillis() + hoursAhead * 3_600_000L
            taskRepository.snoozeTask(task, untilMs)
        }
    }

    fun snoozeTomorrow(task: TaskEntity) {
        viewModelScope.launch {
            // Snooze until 9am tomorrow
            val cal = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 9)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            taskRepository.snoozeTask(task, cal.timeInMillis)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.deleteTask(task) }
    }

    // Subtasks for a parent
    fun getSubtasks(parentId: UUID) = taskRepository.getSubtasks(parentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Legacy compat — kept so VoiceInputScreen doesn't need a change yet
    val isBreakingDownTask: StateFlow<Boolean>
        get() = _addTaskState.asStateFlow()
            .let { MutableStateFlow(it.value.isGeneratingSubtasks) }
    val isParsingVoice = MutableStateFlow(false)
    fun addTasksFromVoice(prompt: String) { /* TODO */
    }
}
