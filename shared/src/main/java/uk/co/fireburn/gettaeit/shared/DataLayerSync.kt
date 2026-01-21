package uk.co.fireburn.gettaeit.shared

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataLayerSync @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }

    suspend fun sendTaskUpdate(taskId: UUID, isCompleted: Boolean) {
        try {
            val nodes = capabilityClient
                .getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes

            nodes.firstOrNull()?.let { node ->
                val path = "$TASK_UPDATE_PATH/$taskId"
                val data = byteArrayOf(if (isCompleted) 1 else 0)
                messageClient.sendMessage(node.id, path, data).await()
            }
        } catch (e: Exception) {
            // Handle exception
        }
    }

    companion object {
        private const val WEAR_CAPABILITY = "get_tae_it_wear_app"
        private const val TASK_UPDATE_PATH = "/task-update"
    }
}
