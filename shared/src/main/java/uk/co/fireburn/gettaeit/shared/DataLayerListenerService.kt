package uk.co.fireburn.gettaeit.shared

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.di.DataLayerEntryPoint
import java.util.UUID

class DataLayerListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, DataLayerEntryPoint::class.java)
    }

    // ── Task snapshots (phone ⇄ watch) ───────────────────────────────────────
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // The buffer is released when this returns, so take frozen copies first.
        val items = dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == DataLayerSync.SNAPSHOT_PATH }
            .map { it.dataItem.freeze() }
        if (items.isEmpty()) return

        val dataLayer = entryPoint.dataLayerSync()
        val wearTaskSync = entryPoint.wearTaskSync()
        // Already off the main thread; blocking keeps the service alive until the merge lands.
        runBlocking {
            val localNodeId = runCatching { dataLayer.localNodeId() }.getOrNull()
            items.forEach { item ->
                val sourceNodeId = item.uri.host ?: return@forEach
                if (sourceNodeId == localNodeId) return@forEach
                try {
                    dataLayer.readSnapshot(item)?.let { wearTaskSync.onPeerSnapshot(sourceNodeId, it) }
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't merge tasks from $sourceNodeId: ${e.message}")
                }
            }
        }
    }

    // ── Voice task from watch → parse on phone and save ──────────────────────
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != DataLayerSync.VOICE_TASK_PATH) return
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

    private companion object {
        const val TAG = "DataLayerListener"
    }
}
