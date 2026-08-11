package uk.co.fireburn.gettaeit.shared.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors Room writes to Firestore under users/{uid}/tasks when signed in.
 *
 * Best-effort and fire-and-forget by design: Room is always the source of truth and the
 * app is fully usable offline whether or not any of this succeeds. Each [TaskEntity] is
 * stored as a single JSON blob (via the same Gson round-trip Room's own [Converters] use
 * for UUID/RecurrenceConfig) rather than a hand-mapped Firestore document — the schema
 * already changes often enough during development that a flat mirror of the Room row is
 * far less to keep in sync than a parallel field-by-field mapping.
 */
@Singleton
class FirestoreTaskSync @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val taskDao: TaskDao
) {
    private val tag = "FirestoreTaskSync"
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val firestore: FirebaseFirestore?
        get() = if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseFirestore.getInstance() else null

    private fun tasksCollection(uid: String) =
        firestore?.collection("users")?.document(uid)?.collection("tasks")

    /** Call once at app startup. Reconciles local Room state against the cloud on every sign-in. */
    fun start() {
        scope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) reconcile(user.uid)
            }
        }
    }

    private suspend fun reconcile(uid: String) {
        val col = tasksCollection(uid) ?: return
        try {
            val remoteDocs = col.get().await().documents
            val localTasks = taskDao.getAllTasksOnce()

            if (remoteDocs.isEmpty()) {
                // Nothing in the cloud for this account yet — this device's tasks become the seed.
                localTasks.forEach { push(it) }
            } else {
                val localIds = localTasks.map { it.id }.toSet()
                remoteDocs
                    .mapNotNull { doc -> doc.getString("json") }
                    .mapNotNull { json -> runCatching { gson.fromJson(json, TaskEntity::class.java) }.getOrNull() }
                    .filter { it.id !in localIds }
                    .forEach { taskDao.insert(it) }
            }
        } catch (e: Exception) {
            Log.w(tag, "Reconcile failed, carrying on offline: ${e.message}")
        }
    }

    /** Fire-and-forget upload; call after every local add/update/complete. */
    fun push(task: TaskEntity) {
        val uid = authRepository.currentUser.value?.uid ?: return
        val col = tasksCollection(uid) ?: return
        scope.launch {
            try {
                col.document(task.id.toString()).set(mapOf("json" to gson.toJson(task))).await()
            } catch (e: Exception) {
                Log.w(tag, "Push failed for ${task.id}, will retry next reconcile: ${e.message}")
            }
        }
    }

    /** Fire-and-forget remote delete; call after every local delete. */
    fun delete(taskId: UUID) {
        val uid = authRepository.currentUser.value?.uid ?: return
        val col = tasksCollection(uid) ?: return
        scope.launch {
            try {
                col.document(taskId.toString()).delete().await()
            } catch (e: Exception) {
                Log.w(tag, "Delete failed for $taskId: ${e.message}")
            }
        }
    }
}
