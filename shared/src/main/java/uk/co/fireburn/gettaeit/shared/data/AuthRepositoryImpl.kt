package uk.co.fireburn.gettaeit.shared.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository
import uk.co.fireburn.gettaeit.shared.domain.AuthUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AuthRepository {

    override val isConfigured: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    private val auth: FirebaseAuth? by lazy { if (isConfigured) FirebaseAuth.getInstance() else null }

    private val _currentUser = MutableStateFlow(auth?.currentUser?.toAuthUser())
    override val currentUser = _currentUser.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toAuthUser()
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> {
        val firebaseAuth = auth
            ?: return Result.failure(IllegalStateException("Firebase isn't set up yet."))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = firebaseAuth.signInWithCredential(credential).await().user
                ?: return Result.failure(IllegalStateException("Signed in but got no user back"))
            Result.success(user.toAuthUser())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth?.signOut()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString()
    )
}
