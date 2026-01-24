package uk.co.fireburn.gettaeit.shared.domain

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<FirebaseUser?>
    suspend fun signOut()
}
