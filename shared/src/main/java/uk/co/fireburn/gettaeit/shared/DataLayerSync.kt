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

    suspend fun sendTaskUpdate(taskId: UUID, isCompleted: Boolean) {
        try {
            val nodes = capabilityClient
                .getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .awaitTask()
                .nodes

            nodes.firstOrNull()?.let { node ->
                val path = "$TASK_UPDATE_PATH/$taskId"
                val data = byteArrayOf(if (isCompleted) 1 else 0)
                messageClient.sendMessage(node.id, path, data).awaitTask()
            }
        } catch (_: Exception) {
            // No watch connected — silently ignore
        }
    }

    companion object {
        private const val WEAR_CAPABILITY = "get_tae_it_wear_app"
        private const val TASK_UPDATE_PATH = "/task-update"
    }
}

/**
 * Suspending bridge for Google's [com.google.android.gms.tasks.Task] without
 * requiring the kotlinx-coroutines-play-services artifact.
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { e -> cont.resumeWithException(e) }
    }
