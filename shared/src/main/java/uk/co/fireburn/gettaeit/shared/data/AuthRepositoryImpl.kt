package uk.co.fireburn.gettaeit.shared.data

import uk.co.fireburn.gettaeit.shared.domain.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local auth stub. No Firebase required.
 * The app is fully offline-first; auth is reserved for future optional backup.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {
    override val isSignedIn: Boolean = false
    override suspend fun signOut() { /* no-op */
    }
}
