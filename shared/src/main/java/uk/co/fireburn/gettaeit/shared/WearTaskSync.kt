package uk.co.fireburn.gettaeit.shared

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.co.fireburn.gettaeit.shared.data.FirestoreTaskSync
import uk.co.fireburn.gettaeit.shared.data.TaskSnapshot
import uk.co.fireburn.gettaeit.shared.data.TaskSnapshotCodec
import uk.co.fireburn.gettaeit.shared.data.TaskSyncStore
import uk.co.fireburn.gettaeit.shared.domain.sync.TaskRecord
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the phone and the watch holding the same tasks.
 *
 * Each side publishes a snapshot of its tasks and tombstones as a Data Layer item once its
 * database settles, and merges the other side's snapshot with the same last-write-wins rules
 * as the cloud backup. Play services queues data items, so anything done while the watch is
 * out of range arrives when it reconnects — nothing is ever replaced wholesale.
 *
 * The phone sends everything still in play plus recent history; the watch sends back every
 * copy it holds. The watch only forgets a task the phone has stopped sending once the phone
 * has merged a watch snapshot that already contained that version of it.
 */
@Singleton
class WearTaskSync @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: TaskSyncStore,
    private val dataLayer: DataLayerSync,
    private val firestoreTaskSync: FirestoreTaskSync
) {
    private val tag = "WearTaskSync"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isWatch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    private val mergedWatchSnapshots = context.getSharedPreferences("wear_task_sync", Context.MODE_PRIVATE)
    private val publishLock = Mutex()

    @Volatile
    private var lastPublishedFingerprint: String? = null

    /** Call once at app startup. */
    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            runCatching { store.pruneTombstones(System.currentTimeMillis()) }
            store.changes().debounce(PUBLISH_DEBOUNCE_MS).collect { publishIfChanged() }
        }
    }

    /** Merges a snapshot published by another node. Called from [DataLayerListenerService]. */
    suspend fun onPeerSnapshot(sourceNodeId: String, snapshot: TaskSnapshot) {
        val remote = snapshot.tasks.map { TaskRecord.Live(it) } +
            snapshot.tombstones.map { TaskRecord.Deleted(it) }
        // Neither side sends its whole history, so a missing task never means "deleted".
        val plan = store.merge(remote, includeLocalOnly = false)

        if (isWatch) {
            snapshot.mergedPeerSnapshots[dataLayer.localNodeId()]?.let { seenUpTo ->
                store.forgetTasksMissingFrom(snapshot, seenUpTo - IN_FLIGHT_MARGIN_MS)
            }
        } else {
            // Changes made on the watch reach the cloud backup by way of the phone.
            plan.local.upserts.forEach(firestoreTaskSync::push)
            plan.local.deletions.forEach(firestoreTaskSync::delete)
            if (snapshot.generatedAt > mergedWatchSnapshots.getLong(sourceNodeId, Long.MIN_VALUE)) {
                mergedWatchSnapshots.edit().putLong(sourceNodeId, snapshot.generatedAt).apply()
            }
            // The acknowledgement is news for the watch even when no task changed.
            publishIfChanged()
        }
    }

    private suspend fun publishIfChanged() = publishLock.withLock {
        try {
            if (!isWatch && !dataLayer.hasWatchInstalled()) return@withLock
            val now = System.currentTimeMillis()
            val snapshot = if (isWatch) {
                store.fullSnapshot(now)
            } else {
                store.phoneSnapshot(now, mergedWatchSnapshots.all.mapNotNull { (node, at) ->
                    (at as? Long)?.let { node to it }
                }.toMap())
            }
            val fingerprint = TaskSnapshotCodec.fingerprint(snapshot)
            if (fingerprint == lastPublishedFingerprint) return@withLock
            dataLayer.publishSnapshot(snapshot)
            lastPublishedFingerprint = fingerprint
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No Wear API on this phone, or Play services is busy: try again on the next change.
            Log.w(tag, "Couldn't publish tasks to the Data Layer: ${e.message}")
        }
    }

    private companion object {
        const val PUBLISH_DEBOUNCE_MS = 1_500L

        /** Slack for a write stamped just before a snapshot but committed just after it. */
        const val IN_FLIGHT_MARGIN_MS = 10_000L
    }
}
