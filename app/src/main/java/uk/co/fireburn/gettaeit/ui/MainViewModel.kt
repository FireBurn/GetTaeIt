package uk.co.fireburn.gettaeit.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.notifications.ReminderScheduler
import uk.co.fireburn.gettaeit.shared.DataLayerSync
import uk.co.fireburn.gettaeit.shared.data.ActiveUsersSync
import uk.co.fireburn.gettaeit.shared.data.MissedBehaviour
import uk.co.fireburn.gettaeit.shared.data.EffortLevel
import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.AppMode
import uk.co.fireburn.gettaeit.shared.domain.ContextManager
import uk.co.fireburn.gettaeit.shared.domain.DependencyGraph
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.RoutineTemplate
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import uk.co.fireburn.gettaeit.shared.domain.AdaptiveSuggestion
import uk.co.fireburn.gettaeit.shared.domain.AdaptiveSuggestionEngine
import uk.co.fireburn.gettaeit.shared.domain.ai.HybridTaskService
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class AddTaskUiState(
    val title: String = "",
    val description: String = "",
    val frictionNote: String = "",
    val context: TaskContext = TaskContext.PERSONAL,
    val priority: Int = 3,
    val effortLevel: EffortLevel = EffortLevel.MEDIUM,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: List<Int> = emptyList(),
    val missedBehaviour: MissedBehaviour = MissedBehaviour.IGNORABLE,
    val preferredTimeOfDayMinutes: Int? = null,
    val dueDate: Long? = null,
    val parentId: UUID? = null,
    val dependencyIds: List<UUID> = emptyList(),
    val dependencyCycleWarning: String? = null,
    val timesPerDay: Int = 1,
    val suggestedSubtasks: List<String> = emptyList(),
    val suggestedSubtaskMinutes: List<Int?> = emptyList(),
    val manualSubtasks: List<String> = emptyList(),
    val existingSubtasks: List<TaskEntity> = emptyList(),
    val isGeneratingSubtasks: Boolean = false
)

/** The small amount of state needed for an optional, private focus block. */
data class FocusSessionUiState(
    val status: FocusSessionStatus = FocusSessionStatus.IDLE,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val activeUsersCount: Int = 0
)

enum class FocusSessionStatus { IDLE, RUNNING, COMPLETED }

data class CompletionCelebration(val taskTitle: String, val xp: Int)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val contextManager: ContextManager,
    private val hybridTaskService: HybridTaskService,
    private val dataLayerSync: DataLayerSync,
    private val activeUsersSync: ActiveUsersSync,
    private val reminderScheduler: ReminderScheduler,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    // ── Streams ───────────────────────────────────────────────────────────────

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTopLevelTasksForCurrentMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appMode: StateFlow<AppMode> = contextManager.appMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppMode.PERSONAL)

    val allTasks: StateFlow<List<TaskEntity>> = taskRepository.getAllActiveToplevelTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userRoutineTemplates: StateFlow<List<RoutineTemplate>> =
        userPreferencesRepository.getUserRoutineTemplates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences = userPreferencesRepository.getUserPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), uk.co.fireburn.gettaeit.shared.data.UserPreferences())

    val completedToday: StateFlow<List<TaskEntity>> = taskRepository.getCompletedToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reviewTasks: StateFlow<List<TaskEntity>> = taskRepository.getReviewableTopLevelTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archivedTasks: StateFlow<List<TaskEntity>> = taskRepository.getArchivedTopLevelTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dismissedSuggestionId = MutableStateFlow<UUID?>(null)
    private val _chainedSuggestion = MutableStateFlow<AdaptiveSuggestion?>(null)
    
    val adaptiveSuggestion: StateFlow<AdaptiveSuggestion?> = combine(
        tasks, completedToday, reviewTasks, _dismissedSuggestionId, _chainedSuggestion
    ) { active, completed, review, dismissedId, chained ->
        if (chained != null && chained.taskId != dismissedId) return@combine chained
        AdaptiveSuggestionEngine.suggest(active, completed, review.filter { it.isSnoozed })
            ?.takeIf { it.taskId != dismissedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun dismissAdaptiveSuggestion(taskId: UUID) {
        _dismissedSuggestionId.value = taskId
        if (_chainedSuggestion.value?.taskId == taskId) {
            _chainedSuggestion.value = null
        }
    }

    private val _completionCelebration = MutableStateFlow<CompletionCelebration?>(null)
    val completionCelebration: StateFlow<CompletionCelebration?> = _completionCelebration.asStateFlow()

    fun clearCompletionCelebration() {
        _completionCelebration.value = null
    }

    private val _isGoblinMode = MutableStateFlow(false)
    /** A reversible focus state: show one useful task, not an overwhelming plan. */
    val isGoblinMode: StateFlow<Boolean> = _isGoblinMode.asStateFlow()

    fun setGoblinMode(enabled: Boolean) {
        _isGoblinMode.value = enabled
    }

    private var focusSessionJob: Job? = null
    private val _focusSession = MutableStateFlow(FocusSessionUiState())
    /** An optional body-doubling timer. It is local, private, and never changes tasks. */
    val focusSession: StateFlow<FocusSessionUiState> = _focusSession.asStateFlow()

    fun startFocusSession(minutes: Int) {
        val totalSeconds = (minutes.coerceIn(1, 90)) * 60
        val endTimeMs = System.currentTimeMillis() + totalSeconds * 1_000L
        focusSessionJob?.cancel()
        _focusSession.value = FocusSessionUiState(
            status = FocusSessionStatus.RUNNING,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds
        )
        
        activeUsersSync.registerFocusSession(totalSeconds)
        viewModelScope.launch {
            val count = activeUsersSync.getActiveFocusersCount()
            _focusSession.value = _focusSession.value.copy(activeUsersCount = count)
        }

        focusSessionJob = viewModelScope.launch {
            while (isActive) {
                val remaining = ((endTimeMs - System.currentTimeMillis() + 999L) / 1_000L)
                    .toInt().coerceAtLeast(0)
                if (remaining == 0) {
                    _focusSession.value = FocusSessionUiState(
                        status = FocusSessionStatus.COMPLETED,
                        totalSeconds = totalSeconds
                    )
                    activeUsersSync.clearFocusSession()
                    break
                }
                _focusSession.value = _focusSession.value.copy(
                    status = FocusSessionStatus.RUNNING,
                    totalSeconds = totalSeconds,
                    remainingSeconds = remaining
                )
                kotlinx.coroutines.delay(250)
            }
        }
    }

    fun stopFocusSession() {
        focusSessionJob?.cancel()
        focusSessionJob = null
        _focusSession.value = FocusSessionUiState()
    }

    /**
     * Low-friction capture for an ADHD brain: save the thought before it
     * disappears, then let the user organise it later if they want to.
     */
    fun quickCapture(title: String) {
        val cleanedTitle = title.trim()
        if (cleanedTitle.isEmpty()) return
        viewModelScope.launch {
            taskRepository.addTask(
                TaskEntity(
                    title = cleanedTitle,
                    context = TaskContext.ANY,
                    priority = 3
                )
            )
        }
    }

    fun addRoutineTemplate(template: RoutineTemplate) {
        viewModelScope.launch {
            taskRepository.addAll(template.steps.map { step ->
                TaskEntity(title = step, context = template.context, priority = 3)
            })
        }
    }

    fun saveUserRoutineTemplate(label: String, steps: List<String>) {
        val cleanedSteps = steps.map(String::trim).filter(String::isNotBlank)
        if (label.isBlank() || cleanedSteps.isEmpty()) return
        viewModelScope.launch {
            userPreferencesRepository.saveUserRoutineTemplate(
                RoutineTemplate(
                    id = "user_${UUID.randomUUID()}",
                    label = label.trim(),
                    context = TaskContext.PERSONAL,
                    steps = cleanedSteps
                )
            )
        }
    }

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

    fun autoScheduleFromTitle() {
        val title = _addTaskState.value.title.ifBlank { return }
        viewModelScope.launch {
            val parsed = hybridTaskService.parsePrompt(title).firstOrNull() ?: return@launch
            val rec = parsed.suggestedRecurrence ?: return@launch
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

    // ── Subtask management ────────────────────────────────────────────────────

    fun removeSuggestedSubtask(index: Int) {
        _addTaskState.value = _addTaskState.value.copy(
            suggestedSubtasks = _addTaskState.value.suggestedSubtasks.toMutableList()
                .also { it.removeAt(index) },
            suggestedSubtaskMinutes = _addTaskState.value.suggestedSubtaskMinutes.toMutableList()
                .also { if (index < it.size) it.removeAt(index) }
        )
    }

    fun addManualSubtask(title: String) {
        if (title.isBlank()) return
        updateAddTaskState { copy(manualSubtasks = manualSubtasks + title.trim()) }
    }

    fun updateManualSubtaskTitle(index: Int, newTitle: String) {
        updateAddTaskState {
            val list = manualSubtasks.toMutableList()
            list[index] = newTitle
            copy(manualSubtasks = list)
        }
    }

    fun removeManualSubtask(index: Int) {
        updateAddTaskState {
            copy(manualSubtasks = manualSubtasks.toMutableList().also { it.removeAt(index) })
        }
    }

    fun updateExistingSubtaskTitle(id: UUID, newTitle: String) {
        updateAddTaskState {
            copy(existingSubtasks = existingSubtasks.map {
                if (it.id == id) it.copy(title = newTitle) else it
            })
        }
    }

    fun removeExistingSubtask(task: TaskEntity) {
        updateAddTaskState { copy(existingSubtasks = existingSubtasks.filter { it.id != task.id }) }
    }

    // ── Dependency picker ─────────────────────────────────────────────────────

    /**
     * Toggles [depId] as a blocker for the task being created/edited. Refuses to add
     * it if doing so would create a loop (A blocks B blocks A) and surfaces a warning
     * instead — silently allowing that would let a task go permanently un-doable.
     */
    fun toggleDependency(depId: UUID) {
        val state = _addTaskState.value
        if (depId in state.dependencyIds) {
            updateAddTaskState {
                copy(dependencyIds = dependencyIds - depId, dependencyCycleWarning = null)
            }
            return
        }
        val wouldCycle = DependencyGraph.wouldCreateCycle(
            taskId = _editingTaskId.value,
            proposedDependencyId = depId,
            allTasks = allTasks.value
        )
        if (wouldCycle) {
            updateAddTaskState {
                copy(dependencyCycleWarning = "Cannae dae that — it'd loop back on itself.")
            }
        } else {
            updateAddTaskState {
                copy(dependencyIds = dependencyIds + depId, dependencyCycleWarning = null)
            }
        }
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
            val historicalTasks = taskRepository.getCompletedByTitle(state.title.trim())
            val learnedMinutes = hybridTaskService.improvedEstimate(historicalTasks)

            val parentTask = TaskEntity(
                id = parentId,
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                frictionNote = state.frictionNote.trim().ifBlank { null },
                context = state.context,
                priority = state.priority,
                effortLevel = state.effortLevel,
                dueDate = state.dueDate,
                recurrence = recurrenceConfig,
                parentId = state.parentId,
                dependencyIds = state.dependencyIds,
                isSubtask = state.parentId != null,
                estimatedMinutes = learnedMinutes
            )
            taskRepository.addTask(parentTask)

            val allNewSubs = state.suggestedSubtasks + state.manualSubtasks
            if (allNewSubs.isNotEmpty()) {
                taskRepository.addAll(allNewSubs.mapIndexed { idx, title ->
                    TaskEntity(
                        title = title,
                        context = state.context,
                        priority = state.priority,
                        parentId = parentId,
                        isSubtask = true,
                        estimatedMinutes = if (idx < state.suggestedSubtasks.size) state.suggestedSubtaskMinutes.getOrNull(
                            idx
                        ) else null,
                        dependencyIds = emptyList()
                    )
                })
            }
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
            userPreferencesRepository.addXp(task.xpValue)
            _completionCelebration.value = CompletionCelebration(task.title, task.xpValue)
            dataLayerSync.sendTaskUpdate(task.id, true)
            reminderScheduler.cancelTask(appContext, task)
            
            // Routine Chaining check
            val allTasks = taskRepository.getAllActiveToplevelTasks().firstOrNull() ?: emptyList()
            val nextTask = allTasks.firstOrNull { it.dependencyIds.contains(task.id) }
            if (nextTask != null) {
                _chainedSuggestion.value = AdaptiveSuggestion(
                    taskId = nextTask.id,
                    message = "Up next: ${nextTask.title}",
                    explanation = "This is the next step in your routine."
                )
            }
        }
    }

    fun completeSubtask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.completeTask(task)
            userPreferencesRepository.addXp(task.xpValue)
            _completionCelebration.value = CompletionCelebration(task.title, task.xpValue)
            dataLayerSync.sendTaskUpdate(task.id, true)
            task.parentId?.let { taskRepository.autoCompleteParentIfDone(it) }
        }
    }

    fun completeTaskWithTime(task: TaskEntity, actualMinutes: Int?) {
        viewModelScope.launch {
            taskRepository.completeTask(task.copy(actualMinutes = actualMinutes))
            userPreferencesRepository.addXp(task.xpValue)
            _completionCelebration.value = CompletionCelebration(task.title, task.xpValue)
            dataLayerSync.sendTaskUpdate(task.id, true)
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

    fun archiveTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.archiveTask(task) }
    }

    /** Turn a stuck task into concrete next moves using the on-device-first AI service. */
    fun makeTaskSmaller(task: TaskEntity) {
        viewModelScope.launch {
            val steps = hybridTaskService.generateSubtasks(task.title)
            if (steps.isEmpty()) return@launch
            taskRepository.addAll(steps.map { step ->
                TaskEntity(
                    title = step.title,
                    context = task.context,
                    priority = task.priority,
                    effortLevel = task.effortLevel,
                    parentId = task.id,
                    isSubtask = true,
                    estimatedMinutes = step.estimatedMinutes
                )
            })
        }
    }

    fun unarchiveTask(task: TaskEntity) {
        viewModelScope.launch { taskRepository.unarchiveTask(task) }
    }

    fun deleteTaskById(id: UUID) {
        viewModelScope.launch {
            taskRepository.getTaskById(id)?.let { taskRepository.deleteTask(it) }
        }
    }

    fun loadTaskForEditing(task: TaskEntity) {
        viewModelScope.launch {
            val existingSubs = taskRepository.getSubtasks(task.id).first()
            _addTaskState.value = AddTaskUiState(
                title = task.title,
                description = task.description ?: "",
                frictionNote = task.frictionNote ?: "",
                context = task.context,
                priority = task.priority,
                effortLevel = task.effortLevel,
                recurrenceType = task.recurrence.type,
                recurrenceInterval = task.recurrence.interval,
                recurrenceDaysOfWeek = task.recurrence.daysOfWeek,
                missedBehaviour = task.recurrence.missedBehaviour,
                preferredTimeOfDayMinutes = task.recurrence.preferredTimeOfDayMinutes,
                dueDate = task.dueDate,
                parentId = task.parentId,
                dependencyIds = task.dependencyIds,
                timesPerDay = task.recurrence.timesPerDay,
                existingSubtasks = existingSubs
            )
            _editingTaskId.value = task.id
        }
    }

    private val _editingTaskId = MutableStateFlow<UUID?>(null)
    val editingTaskId: StateFlow<UUID?> = _editingTaskId.asStateFlow()

    fun saveTaskEdits() {
        val taskId = _editingTaskId.value ?: run { saveTask(); return }
        val state = _addTaskState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            val existing = taskRepository.getTaskById(taskId) ?: return@launch

            val dbSubtasks = taskRepository.getSubtasks(taskId).first()
            val keptIds = state.existingSubtasks.map { it.id }
            dbSubtasks.filter { it.id !in keptIds }.forEach { taskRepository.deleteTask(it) }

            state.existingSubtasks.forEach { updatedSub ->
                taskRepository.updateTask(updatedSub)
            }

            val newSubs = state.suggestedSubtasks + state.manualSubtasks
            if (newSubs.isNotEmpty()) {
                taskRepository.addAll(newSubs.mapIndexed { idx, title ->
                    TaskEntity(
                        title = title,
                        context = state.context,
                        priority = state.priority,
                        parentId = taskId,
                        isSubtask = true,
                        estimatedMinutes = if (idx < state.suggestedSubtasks.size) state.suggestedSubtaskMinutes.getOrNull(
                            idx
                        ) else null
                    )
                })
            }

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
                frictionNote = state.frictionNote.trim().ifBlank { null },
                context = state.context,
                priority = state.priority,
                effortLevel = state.effortLevel,
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

    fun cancelEdit() {
        _editingTaskId.value = null
        resetAddTaskState()
    }

    fun getSubtasks(parentId: UUID): Flow<List<TaskEntity>> = taskRepository.getSubtasks(parentId)

    // ── Voice input ───────────────────────────────────────────────────────────

    val isParsingVoice = MutableStateFlow(false)

    /**
     * Holds the AI-inferred recurrence suggestion for the current voice prompt.
     * Null means the AI found no repeating schedule (one-off task).
     * Emitted before the "How often?" sheet is shown so the UI can offer it.
     */
    private val _voiceScheduleSuggestion = MutableStateFlow<RecurrenceConfig?>(null)
    val voiceScheduleSuggestion: StateFlow<RecurrenceConfig?> =
        _voiceScheduleSuggestion.asStateFlow()

    /**
     * Calls Gemini Nano (or the template fallback) to figure out whether the
     * voice prompt implies a recurring schedule, then exposes the result via
     * [voiceScheduleSuggestion].  The caller should await the coroutine (or
     * collect the flow) before presenting the "How often?" confirmation sheet.
     */
    fun parseVoiceForSchedule(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            isParsingVoice.value = true
            try {
                val parsed = hybridTaskService.parsePrompt(prompt).firstOrNull()
                _voiceScheduleSuggestion.value = parsed?.suggestedRecurrence
                    ?.takeIf { it.type != RecurrenceType.NONE }
            } finally {
                isParsingVoice.value = false
            }
        }
    }

    /** Clear the schedule suggestion once the user has acted on it. */
    fun clearVoiceScheduleSuggestion() {
        _voiceScheduleSuggestion.value = null
    }

    fun addTasksFromVoice(
        prompt: String,
        dueDate: Long? = null,
        recurrenceOverride: RecurrenceConfig? = null,
        onComplete: () -> Unit = {}
    ) {
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
                    // Use the user-confirmed override first, then the AI suggestion, then NONE
                    val recurrence = recurrenceOverride
                        ?: parsed.suggestedRecurrence
                        ?: RecurrenceConfig(type = RecurrenceType.NONE)

                    val parentId = UUID.randomUUID()

                    val parentTask = TaskEntity(
                        id = parentId,
                        title = parsed.title,
                        context = context,
                        priority = 3,
                        recurrence = recurrence,
                        estimatedMinutes = parsed.estimatedMinutes,
                        dueDate = dueDate
                    )

                    taskRepository.addTask(parentTask)

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

                    // CRITICAL FIX: Ensure background alarms are scheduled for voice tasks!
                    if (parentTask.recurrence.type != RecurrenceType.NONE) {
                        reminderScheduler.scheduleTask(appContext, parentTask)
                    }
                }
            } finally {
                isParsingVoice.value = false
                onComplete()
            }
        }
    }

    fun rescheduleAllReminders() {
        viewModelScope.launch {
            taskRepository.getAllActiveToplevelTasks().collect { tasks ->
                tasks.filter { it.recurrence.type != RecurrenceType.NONE }
                    .forEach { reminderScheduler.scheduleTask(appContext, it) }
                return@collect // only process first emission
            }
        }
    }

    fun setDailySpoons(spoons: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateSpoons(spoons)
        }
    }
}
