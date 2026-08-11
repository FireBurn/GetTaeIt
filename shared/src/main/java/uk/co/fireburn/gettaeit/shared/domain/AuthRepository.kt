package uk.co.fireburn.gettaeit.shared.domain

import kotlinx.coroutines.flow.StateFlow

data class AuthUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

/**
 * Google sign-in + cloud backup. The app is offline-first and fully usable signed out —
 * this only exists so tasks survive a lost or reset phone.
 */
interface AuthRepository {
    /** Null when signed out, or when Firebase hasn't been configured yet (no google-services.json). */
    val currentUser: StateFlow<AuthUser?>

    /** False until a real google-services.json has been dropped into :app (Firebase console). */
    val isConfigured: Boolean

    /** Exchanges a Google ID token (from Credential Manager, in the UI layer) for a Firebase session. */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    suspend fun signOut()
}
