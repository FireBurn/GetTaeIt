package uk.co.fireburn.gettaeit.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.DataLayerSync
import uk.co.fireburn.gettaeit.shared.domain.scheduling.ReminderPlanner
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import java.util.UUID

/**
 * Fired by AlarmManager when a task reminder slot is due. Reads the current task so stale
 * alarms cannot notify after completion, deletion, archival or snoozing.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TaskNotificationManager.EXTRA_TASK_ID) ?: return
        val slotIndex = intent.getIntExtra(TaskNotificationManager.EXTRA_SLOT_INDEX, 0)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReminderEntryPoint::class.java
                )
                val repository = entryPoint.taskRepository()
                val uuid = runCatching { UUID.fromString(taskId) }.getOrNull() ?: return@launch
                val now = System.currentTimeMillis()
                var task = repository.getTaskById(uuid)
                if (task?.isCompleted == true && task.nextOccurrenceAt?.let { it <= now } == true) {
                    repository.uncompleteTask(task)
                    task = repository.getTaskById(uuid)
                }
                if (task != null && ReminderPlanner.shouldNotify(task, now)) {
                    TaskNotificationManager.showReminder(context, taskId, task.title, slotIndex)
                    entryPoint.dataLayerSync().sendWearHaptic(DataLayerSync.HAPTIC_REMINDER)
                }
                task?.let { entryPoint.reminderScheduler().scheduleTask(context, it) }
            } catch (error: Exception) {
                Log.e(TAG, "Couldn't process task reminder for $taskId", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun taskRepository(): TaskRepository
    fun dataLayerSync(): DataLayerSync
    fun reminderScheduler(): ReminderScheduler
}

private const val TAG = "ReminderReceiver"
