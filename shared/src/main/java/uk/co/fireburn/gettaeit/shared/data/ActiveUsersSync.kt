package uk.co.fireburn.gettaeit.shared.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveUsersSync @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) {
    private val tag = "ActiveUsersSync"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val firestore: FirebaseFirestore?
        get() = if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseFirestore.getInstance() else null

    /** Registers this user as actively focusing for [durationSeconds]. */
    fun registerFocusSession(durationSeconds: Int) {
        val uid = authRepository.currentUser.value?.uid ?: return
        val db = firestore ?: return
        scope.launch {
            try {
                val expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L)
                db.collection("active_focus_sessions").document(uid).set(
                    mapOf("expiresAt" to expiresAt)
                ).await()
            } catch (e: Exception) {
                Log.w(tag, "Failed to register focus session: ${e.message}")
            }
        }
    }

    /** Clears the user's active focus session. */
    fun clearFocusSession() {
        val uid = authRepository.currentUser.value?.uid ?: return
        val db = firestore ?: return
        scope.launch {
            try {
                db.collection("active_focus_sessions").document(uid).delete().await()
            } catch (e: Exception) {
                Log.w(tag, "Failed to clear focus session: ${e.message}")
            }
        }
    }

    /** Returns the count of other users currently focusing. */
    suspend fun getActiveFocusersCount(): Int {
        val uid = authRepository.currentUser.value?.uid
        val db = firestore ?: return 0
        return try {
            val now = System.currentTimeMillis()
            val querySnapshot = db.collection("active_focus_sessions")
                .whereGreaterThan("expiresAt", now)
                .get()
                .await()
            // Count others
            querySnapshot.documents.count { it.id != uid }
        } catch (e: Exception) {
            Log.w(tag, "Failed to get active focusers: ${e.message}")
            0
        }
    }
}
