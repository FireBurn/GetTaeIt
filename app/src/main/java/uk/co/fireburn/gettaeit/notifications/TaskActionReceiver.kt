package uk.co.fireburn.gettaeit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Handles "Feenisht ✓" and "Stickit ✗" notification action button taps.
 *
 * Feenisht → marks the task complete in the DB (which triggers recurrence logic).
 * Stickit  → dismisses the notification silently; task stays active for next slot.
 *
 * Uses goAsync() so we can do DB work without ANR risk.
 */
@AndroidEntryPoint
class TaskActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskRepository: TaskRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TaskNotificationManager.EXTRA_TASK_ID) ?: return
        val slotIndex = intent.getIntExtra(TaskNotificationManager.EXTRA_SLOT_INDEX, 0)

        // Always dismiss the notification first (instant feedback)
        TaskNotificationManager.cancelNotification(context, taskId, slotIndex)

        when (intent.action) {
            TaskNotificationManager.ACTION_FEENISHT -> {
                // Mark done asynchronously
                val result = goAsync()
                scope.launch {
                    try {
                        val uuid = UUID.fromString(taskId)
                        val task = taskRepository.getTaskById(uuid) ?: return@launch
                        taskRepository.completeTask(task)
                        // If it's a subtask, auto-complete parent if all siblings done
                        task.parentId?.let { taskRepository.autoCompleteParentIfDone(it) }
                    } finally {
                        result.finish()
                    }
                }
            }

            TaskNotificationManager.ACTION_STICKIT -> {
                // Nothing to do — notification already cancelled above.
                // The task stays active; it'll fire again at the next scheduled slot.
            }
        }
    }
}
