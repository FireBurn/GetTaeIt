package uk.co.fireburn.gettaeit.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.notifications.ReminderScheduler
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
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

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
    val timesPerDay: Int = 1,
    val suggestedSubtasks: List<String> = emptyList(),
    val suggestedSubtaskMinutes: List<Int?> = emptyList(),
    val isGeneratingSubtasks: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val contextManager: ContextManager,
    private val hybridTaskService: HybridTaskService,
    private val dataLayerSync: DataLayerSync,
    private val reminderScheduler: ReminderScheduler,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    // ── Streams ───────────────────────────────────────────────────────────────

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTopLevelTasksForCurrentMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appMode: StateFlow<AppMode> = contextManager.appMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppMode.PERSONAL)

    /** All active top-level tasks — used for the dependency/blocker picker. */
    val allTasks: StateFlow<List<TaskEntity>> = taskRepository.getAllActiveToplevelTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Add task sheet state ──────────────────────────────────────────────────

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

    // ── AI subtask + scheduling suggestions ──────────────────────────────────

    fun requestSubtaskBreakdown() {
        val title = _addTaskState.value.title.ifBlank { return }
        viewModelScope.launch {
            _addTaskState.value = _addTaskState.value.copy(isGeneratingSubtasks = true)
            try {
                val results = hybridTaskService.generateSubtasks(title)
                val detectedContext = hybridTaskService.detectContext(title)
                _addTaskState.value = _addTaskState.value.copy(
                    suggestedSubtasks = results.map { it.title },
                    suggestedSubtaskMinutes = results.map { it.estimatedMinutes },
                    context = if (_addTaskState.value.context == TaskContext.ANY)
                        detectedContext else _addTaskState.value.context
                )
            } finally {
                _addTaskState.value = _addTaskState.value.copy(isGeneratingSubtasks = false)
            }
        }
    }

    /**
     * Auto-fills scheduling fields from the title using the AI template.
     * Called when the user finishes typing a title and hasn't set a schedule yet.
     */
    fun autoScheduleFromTitle() {
        val title = _addTaskState.value.title.ifBlank { return }
        viewModelScope.launch {
            val parsed = hybridTaskService.parsePrompt(title).firstOrNull() ?: return@launch
            val rec = parsed.suggestedRecurrence ?: return@launch
            // Only auto-fill if user hasn't already set a recurrence
            if (_addTaskState.value.recurrenceType != RecurrenceType.NONE) return@launch
            _addTaskState.value = _addTaskState.value.copy(
                recurrenceType = rec.type,
                recurrenceInterval = rec.interval,
                recurrenceDaysOfWeek = rec.daysOfWeek,
                missedBehaviour = rec.missedBehaviour,
                preferredTimeOfDayMinutes = rec.preferredTimeOfDayMinutes
                    ?: _addTaskState.value.preferredTimeOfDayMinutes,
                timesPerDay = rec.timesPerDay
            )
        }
    }

    fun removeSuggestedSubtask(index: Int) {
        _addTaskState.value = _addTaskState.value.copy(
            suggestedSubtasks = _addTaskState.value.suggestedSubtasks.toMutableList()
                .also { it.removeAt(index) },
            suggestedSubtaskMinutes = _addTaskState.value.suggestedSubtaskMinutes.toMutableList()
                .also { if (index < it.size) it.removeAt(index) }
        )
    }

    // ── Save task ─────────────────────────────────────────────────────────────

    fun saveTask() {
        val state = _addTaskState.value
        if (state.title.isBlank()) return
        viewModelScope.launch {
            val recurrenceConfig = RecurrenceConfig(
                type = state.recurrenceType,
                interval = state.recurrenceInterval,
                daysOfWeek = state.recurrenceDaysOfWeek,
                missedBehaviour = state.missedBehaviour,
                preferredTimeOfDayMinutes = state.preferredTimeOfDayMinutes,
                timesPerDay = state.timesPerDay
            )
            val parentId = UUID.randomUUID()
            // Learning loop: check if we have historical actuals for this title
            val historicalTasks = taskRepository.getCompletedByTitle(state.title.trim())
            val learnedMinutes = hybridTaskService.improvedEstimate(historicalTasks)

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
                isSubtask = state.parentId != null,
                estimatedMinutes = learnedMinutes
            )
            taskRepository.addTask(parentTask)
            if (state.suggestedSubtasks.isNotEmpty()) {
                taskRepository.addAll(state.suggestedSubtasks.mapIndexed { idx, title ->
                    TaskEntity(
                        title = title,
                        context = state.context,
                        priority = state.priority,
                        parentId = parentId,
                        isSubtask = true,
                        estimatedMinutes = state.suggestedSubtaskMinutes.getOrNull(idx),
                        dependencyIds = emptyList()
                    )
                })
            }
            // Schedule reminders for recurring tasks
            if (parentTask.recurrence.type != RecurrenceType.NONE) {
                reminderScheduler.scheduleTask(appContext, parentTask)
            }
            resetAddTaskState()
        }
    }

    // ── Task actions ──────────────────────────────────────────────────────────

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.completeTask(task)
            dataLayerSync.sendTaskUpdate(task.id, true)
            // Cancel today's remaining alarms; recurring tasks will be rescheduled by BootReceiver/next launch
            reminderScheduler.cancelTask(appContext, task)
        }
    }

    /**
     * Complete a subtask, then auto-complete the parent if all siblings are done.
     */
    fun completeSubtask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.completeTask(task)
            dataLayerSync.sendTaskUpdate(task.id, true)
            task.parentId?.let { taskRepository.autoCompleteParentIfDone(it) }
        }
    }

    fun completeTaskWithTime(task: TaskEntity, actualMinutes: Int?) {
        viewModelScope.launch {
            taskRepository.completeTask(task.copy(actualMinutes = actualMinutes))
            dataLayerSync.sendTaskUpdate(task.id, true)
            // If it's a subtask, check parent too
            task.parentId?.let { taskRepository.autoCompleteParentIfDone(it) }
        }
    }

    fun uncompleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.uncompleteTask(task)
            dataLayerSync.sendTaskUpdate(task.id, false)
        }
    }

    fun snoozeTask(task: TaskEntity, hoursAhead: Long = 2L) {
        viewModelScope.launch {
            val untilMs = System.currentTimeMillis() + hoursAhead * 3_600_000L
            taskRepository.snoozeTask(task, untilMs)
        }
    }

    fun snoozeTomorrow(task: TaskEntity) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            taskRepository.snoozeTask(task, cal.timeInMillis)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.deleteTask(task) }
    }

    /**
     * Loads an existing task into the add/edit sheet state so the user can modify it.
     * Call this before navigating to AddTaskScreen in "edit mode".
     */
    fun loadTaskForEditing(task: TaskEntity) {
        _addTaskState.value = AddTaskUiState(
            title = task.title,
            description = task.description ?: "",
            context = task.context,
            priority = task.priority,
            recurrenceType = task.recurrence.type,
            recurrenceInterval = task.recurrence.interval,
            recurrenceDaysOfWeek = task.recurrence.daysOfWeek,
            missedBehaviour = task.recurrence.missedBehaviour,
            preferredTimeOfDayMinutes = task.recurrence.preferredTimeOfDayMinutes,
            dueDate = task.dueDate,
            parentId = task.parentId,
            dependencyIds = task.dependencyIds,
            timesPerDay = task.recurrence.timesPerDay
        )
        _editingTaskId.value = task.id
    }

    /** Non-null while an existing task is being edited (vs. a new task being created). */
    private val _editingTaskId = MutableStateFlow<UUID?>(null)
    val editingTaskId: StateFlow<UUID?> = _editingTaskId.asStateFlow()

    /**
     * Saves changes to an existing task. Called instead of [saveTask] when editing.
     */
    fun saveTaskEdits() {
        val taskId = _editingTaskId.value ?: run { saveTask(); return }
        val state = _addTaskState.value
        if (state.title.isBlank()) return
        viewModelScope.launch {
            val existing = taskRepository.getTaskById(taskId) ?: return@launch
            val recurrenceConfig = RecurrenceConfig(
                type = state.recurrenceType,
                interval = state.recurrenceInterval,
                daysOfWeek = state.recurrenceDaysOfWeek,
                missedBehaviour = state.missedBehaviour,
                preferredTimeOfDayMinutes = state.preferredTimeOfDayMinutes,
                timesPerDay = state.timesPerDay
            )
            val updated = existing.copy(
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                context = state.context,
                priority = state.priority,
                dueDate = state.dueDate,
                recurrence = recurrenceConfig,
                dependencyIds = state.dependencyIds
            )
            taskRepository.updateTask(updated)
            if (updated.recurrence.type != RecurrenceType.NONE) {
                reminderScheduler.scheduleTask(appContext, updated)
            }
            _editingTaskId.value = null
            resetAddTaskState()
        }
    }

    /** Call when the edit sheet is dismissed without saving. */
    fun cancelEdit() {
        _editingTaskId.value = null
        resetAddTaskState()
    }

    fun getSubtasks(parentId: UUID): Flow<List<TaskEntity>> = taskRepository.getSubtasks(parentId)

    // ── Voice input ───────────────────────────────────────────────────────────

    val isParsingVoice = MutableStateFlow(false)

    /**
     * Parses a voice/text prompt using the full AI template (supports multi-task expansion,
     * auto-scheduling, and subtasks). Saves all resulting tasks and calls [onComplete].
     */
    fun addTasksFromVoice(prompt: String, dueDate: Long? = null, onComplete: () -> Unit = {}) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            isParsingVoice.value = true
            try {
                val parsedList = hybridTaskService.parsePrompt(prompt)
                parsedList.forEach { parsed ->
                    val context = when {
                        parsed.suggestedContext != TaskContext.ANY -> parsed.suggestedContext
                        appMode.value == AppMode.WORK -> TaskContext.WORK
                        else -> TaskContext.PERSONAL
                    }
                    val recurrence = parsed.suggestedRecurrence
                        ?: RecurrenceConfig(type = RecurrenceType.NONE)

                    val parentId = UUID.randomUUID()
                    taskRepository.addTask(
                        TaskEntity(
                            id = parentId,
                            title = parsed.title,
                            context = context,
                            priority = 3,
                            recurrence = recurrence,
                            estimatedMinutes = parsed.estimatedMinutes,
                            // Use the user-selected due date from the "When?" sheet.
                            // If the template already inferred a preferred time, prefer that
                            // over the sheet's generic timestamp — but only if no explicit
                            // due date was provided.
                            dueDate = dueDate
                        )
                    )
                    if (parsed.subtasks.isNotEmpty()) {
                        taskRepository.addAll(parsed.subtasks.map { result ->
                            TaskEntity(
                                title = result.title,
                                context = context,
                                priority = (3 + result.priorityOffset).coerceIn(1, 5),
                                parentId = parentId,
                                isSubtask = true,
                                recurrence = RecurrenceConfig(type = RecurrenceType.NONE),
                                estimatedMinutes = result.estimatedMinutes
                            )
                        })
                    }
                }
            } finally {
                isParsingVoice.value = false
                onComplete()
            }
        }
    }

    /** Re-schedule all active recurring tasks (called e.g. after returning from Settings). */
    fun rescheduleAllReminders() {
        viewModelScope.launch {
            taskRepository.getAllActiveToplevelTasks().collect { tasks ->
                tasks.filter { it.recurrence.type != RecurrenceType.NONE }
                    .forEach { reminderScheduler.scheduleTask(appContext, it) }
                return@collect // only process first emission
            }
        }
    }
}
