package uk.co.fireburn.gettaeit.shared

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.di.DataLayerEntryPoint
import java.util.UUID

class DataLayerListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path.startsWith(TASK_UPDATE_PATH)) {
            val taskIdString = messageEvent.path.substringAfterLast("/")
            val taskId = UUID.fromString(taskIdString)
            val isCompleted = messageEvent.data[0].toInt() == 1

            // Get repository from Hilt Entry Point
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                DataLayerEntryPoint::class.java
            )
            val taskRepository = entryPoint.taskRepository()

            serviceScope.launch {
                val task =
                    taskRepository.getTaskById(taskId) // Note: This function doesn't exist yet!
                task?.let {
                    if (it.isCompleted != isCompleted) {
                        taskRepository.updateTask(it.copy(isCompleted = isCompleted))
                    }
                }
            }
        }
    }

    companion object {
        const val TASK_UPDATE_PATH = "/task-update"
    }
}
