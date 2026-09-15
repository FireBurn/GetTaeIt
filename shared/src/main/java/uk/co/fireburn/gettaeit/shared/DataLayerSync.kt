package uk.co.fireburn.gettaeit.shared

import android.content.Context
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import uk.co.fireburn.gettaeit.shared.data.TaskSnapshot
import uk.co.fireburn.gettaeit.shared.data.TaskSnapshotCodec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Thin wrapper over the Wear Data Layer for the phone and watch apps. */
@Singleton
class DataLayerSync @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }
    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }

    /**
     * Replaces this node's task snapshot. Play services holds on to it and delivers it to the
     * other side whenever that's reachable, so this succeeds offline too. The payload is an
     * asset, which has no practical size limit, rather than a data map field capped at 100 KB.
     */
    suspend fun publishSnapshot(snapshot: TaskSnapshot) {
        val request = PutDataMapRequest.create(SNAPSHOT_PATH).apply {
            dataMap.putAsset(KEY_SNAPSHOT, Asset.createFromBytes(TaskSnapshotCodec.encode(snapshot)))
            dataMap.putLong(KEY_GENERATED_AT, snapshot.generatedAt)
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).awaitTask()
    }

    /** Reads the snapshot out of a changed data item, or null if it is unreadable. */
    suspend fun readSnapshot(item: DataItem): TaskSnapshot? {
        val asset = DataMapItem.fromDataItem(item).dataMap.getAsset(KEY_SNAPSHOT) ?: return null
        val bytes = dataClient.getFdForAsset(asset).awaitTask().inputStream.use { it.readBytes() }
        return TaskSnapshotCodec.decode(bytes)
    }

    /** Whether any paired watch has Get Tae It installed, whether or not it's in range. */
    suspend fun hasWatchInstalled(): Boolean =
        capabilityClient.getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_ALL)
            .awaitTask()
            .nodes
            .isNotEmpty()

    suspend fun localNodeId(): String = nodeClient.localNode.awaitTask().id

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
        const val SNAPSHOT_PATH = "/tasks-snapshot"
        const val VOICE_TASK_PATH = "/voice-task"
        private const val KEY_SNAPSHOT = "snapshot"
        private const val KEY_GENERATED_AT = "generatedAt"
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
