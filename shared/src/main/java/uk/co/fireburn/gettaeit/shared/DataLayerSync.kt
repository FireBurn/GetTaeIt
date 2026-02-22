package uk.co.fireburn.gettaeit.shared

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class DataLayerSync @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }

    /** Send a task completion/uncompletion to the paired device. */
    suspend fun sendTaskUpdate(taskId: UUID, isCompleted: Boolean): Boolean {
        return try {
            val nodes = capabilityClient
                .getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .awaitTask()
                .nodes

            nodes.firstOrNull()?.let { node ->
                val path = "$TASK_UPDATE_PATH/$taskId"
                val data = byteArrayOf(if (isCompleted) 1 else 0)
                messageClient.sendMessage(node.id, path, data).awaitTask()
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Send a voice-dictated task string from the watch to the phone.
     * The phone's [DataLayerListenerService] receives it, parses it with the AI,
     * and saves the resulting tasks.
     * Returns true if the phone was reached, false if it wasn't connected.
     */
    suspend fun sendVoiceTask(text: String): Boolean {
        return try {
            val nodes = capabilityClient
                .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .awaitTask()
                .nodes

            nodes.firstOrNull()?.let { node ->
                val data = text.toByteArray(Charsets.UTF_8)
                messageClient.sendMessage(node.id, VOICE_TASK_PATH, data).awaitTask()
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val WEAR_CAPABILITY = "get_tae_it_wear_app"
        const val PHONE_CAPABILITY = "get_tae_it_phone_app"
        const val TASK_UPDATE_PATH = "/task-update"
        const val VOICE_TASK_PATH = "/voice-task"
    }
}

/**
 * Suspending bridge for Google's [com.google.android.gms.tasks.Task] without
 * requiring the kotlinx-coroutines-play-services artifact.
 */
internal suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { e -> cont.resumeWithException(e) }
    }
