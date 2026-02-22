package uk.co.fireburn.gettaeit.shared.domain

/**
 * Auth is intentionally minimal for now — the app is offline-first and
 * does not require a Google account. This interface exists for future
 * optional cloud backup.
 */
interface AuthRepository {
    val isSignedIn: Boolean
    suspend fun signOut()
}
