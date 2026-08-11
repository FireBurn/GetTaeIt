package uk.co.fireburn.gettaeit.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.DataLayerSync
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WearViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val dataLayerSync: DataLayerSync,
    private val preferences: UserPreferencesRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val hapticsEnabled: StateFlow<Boolean> = preferences.getUserPreferences()
        .map { it.wearHapticsEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleHaptics() = viewModelScope.launch { preferences.updateUserPreferences(preferences.getUserPreferences().first().copy(wearHapticsEnabled = !hapticsEnabled.value)) }
    fun startHaptic() = vibrate(longArrayOf(0, 40), intArrayOf(180, 0))
    private fun vibrate(pattern: LongArray, amplitudes: IntArray) { if (hapticsEnabled.value) (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1)) }

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.getTasksForCurrentMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSendingVoice = MutableStateFlow(false)

    fun getSubtasks(parentId: UUID): Flow<List<TaskEntity>> =
        taskRepository.getSubtasks(parentId)

    fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            if (completed) taskRepository.completeTask(task) else taskRepository.uncompleteTask(task)
            if (completed) vibrate(longArrayOf(0, 35, 55, 60), intArrayOf(160, 0, 220, 0))
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
            vibrate(longArrayOf(0, 70), intArrayOf(100, 0))
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
