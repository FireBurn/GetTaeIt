package uk.co.fireburn.gettaeit.shared.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository
import uk.co.fireburn.gettaeit.shared.domain.sync.TaskRecord
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors Room to Firestore under users/{uid}/tasks while signed in.
 *
 * Room stays the source of truth: every write lands there first and this runs afterwards,
 * fire-and-forget, with Firestore's own offline queue holding pushes made without signal.
 * Incoming documents go through [TaskSyncStore.merge], so offline edits, deletions and
 * repeated listener events all settle by the rules in SyncConflictResolver.
 *
 * Only snapshots confirmed by the server trigger corrective pushes. A stale local cache
 * must never overwrite something newer that another device has already uploaded.
 */
@Singleton
class FirestoreTaskSync @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val syncStore: TaskSyncStore
) {
    private val tag = "FirestoreTaskSync"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val firestore: FirebaseFirestore?
        get() = if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseFirestore.getInstance() else null

    private fun tasksCollection(uid: String): CollectionReference? =
        firestore?.collection("users")?.document(uid)?.collection("tasks")

    /** Call once at app startup. Follows sign-in and sign-out for the life of the process. */
    fun start() {
        scope.launch {
            authRepository.currentUser
                .map { it?.uid }
                .distinctUntilChanged()
                .collectLatest { uid ->
                    val collection = uid?.let(::tasksCollection) ?: return@collectLatest
                    try {
                        follow(collection)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(tag, "Cloud sync stopped, carrying on offline: ${e.message}")
                    }
                }
        }
    }

    private suspend fun follow(collection: CollectionReference) {
        var reconciled = false
        snapshots(collection).collect { snapshot ->
            val fromServer = !snapshot.metadata.isFromCache
            // The first server snapshot is the whole collection: the moment to push anything
            // the cloud has never seen. After that only changed documents need looking at.
            val fullPicture = fromServer && !reconciled
            val records = if (fullPicture) {
                snapshot.documents.mapNotNull { FirestoreTaskDocuments.fromDocument(it.id, it.data) }
            } else {
                snapshot.documentChanges
                    .filter { it.type != DocumentChange.Type.REMOVED }
                    .mapNotNull { FirestoreTaskDocuments.fromDocument(it.document.id, it.document.data) }
            }
            val plan = syncStore.merge(records, includeLocalOnly = fullPicture)
            if (fromServer) plan.pushToRemote.forEach { upload(collection, it) }
            if (fullPicture) reconciled = true
        }
    }

    private fun snapshots(collection: CollectionReference): Flow<QuerySnapshot> = callbackFlow {
        // Metadata changes too, or a cache snapshot that matches the server never reports
        // that it has been confirmed and the first reconcile would never happen.
        val registration = collection.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            when {
                error != null -> close(error)
                snapshot != null -> trySend(snapshot)
            }
        }
        awaitClose { registration.remove() }
    }.buffer(Channel.UNLIMITED)

    /** Fire-and-forget upload; call after every local add/update/complete. */
    fun push(task: TaskEntity) = upload(TaskRecord.Live(task))

    /** Fire-and-forget tombstone; call after every local delete. */
    fun delete(tombstone: TaskTombstone) = upload(TaskRecord.Deleted(tombstone))

    private fun upload(record: TaskRecord) {
        val uid = authRepository.currentUser.value?.uid ?: return
        upload(tasksCollection(uid) ?: return, record)
    }

    private fun upload(collection: CollectionReference, record: TaskRecord) {
        // Not awaited: offline, Firestore queues the write and sends it when signal returns.
        collection.document(record.id.toString())
            .set(FirestoreTaskDocuments.toDocument(record))
            .addOnFailureListener { Log.w(tag, "Upload failed for ${record.id}: ${it.message}") }
    }

    /**
     * Removes this account's whole task backup. Needs a connection, so it returns false when
     * the cloud copy couldn't be confirmed gone. Returns true when there's nothing to remove.
     */
    suspend fun deleteRemoteBackup(): Boolean {
        val uid = authRepository.currentUser.value?.uid ?: return true
        val collection = tasksCollection(uid) ?: return true
        return withTimeoutOrNull(15_000L) {
            try {
                collection.get(Source.SERVER).await().documents.chunked(400).forEach { chunk ->
                    val batch = collection.firestore.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(tag, "Couldn't delete the cloud backup: ${e.message}")
                false
            }
        } ?: false
    }
}
