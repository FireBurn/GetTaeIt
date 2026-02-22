package uk.co.fireburn.gettaeit.shared

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.di.DataLayerEntryPoint
import java.util.UUID

class DataLayerListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DataLayerEntryPoint::class.java
        )

        when {
            // ── Task completion sync (watch → phone or phone → watch) ────────
            messageEvent.path.startsWith(DataLayerSync.TASK_UPDATE_PATH) -> {
                val taskIdString = messageEvent.path.substringAfterLast("/")
                val taskId = UUID.fromString(taskIdString)
                val isCompleted = messageEvent.data[0].toInt() == 1

                val taskRepository = entryPoint.taskRepository()
                serviceScope.launch {
                    val task = taskRepository.getTaskById(taskId)
                    task?.let {
                        if (it.isCompleted != isCompleted) {
                            taskRepository.updateTask(it.copy(isCompleted = isCompleted))
                        }
                    }
                }
            }

            // ── Voice task from watch → parse on phone and save ──────────────
            messageEvent.path == DataLayerSync.VOICE_TASK_PATH -> {
                val text = messageEvent.data.toString(Charsets.UTF_8)
                if (text.isBlank()) return

                val taskRepository = entryPoint.taskRepository()
                val hybridService = entryPoint.hybridTaskService()

                serviceScope.launch {
                    // Parse the voice text using the same AI template as the phone app
                    val parsedList = hybridService.parsePrompt(text)
                    parsedList.forEach { parsed ->
                        val parentId = UUID.randomUUID()
                        taskRepository.addTask(
                            TaskEntity(
                                id = parentId,
                                title = parsed.title,
                                context = parsed.suggestedContext,
                                recurrence = parsed.suggestedRecurrence
                                    ?: uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig(),
                                estimatedMinutes = parsed.estimatedMinutes
                            )
                        )
                        if (parsed.subtasks.isNotEmpty()) {
                            taskRepository.addAll(parsed.subtasks.map { result ->
                                TaskEntity(
                                    title = result.title,
                                    context = parsed.suggestedContext,
                                    priority = (3 + result.priorityOffset).coerceIn(1, 5),
                                    parentId = parentId,
                                    isSubtask = true,
                                    estimatedMinutes = result.estimatedMinutes
                                )
                            })
                        }
                    }
                }
            }
        }
    }
}
