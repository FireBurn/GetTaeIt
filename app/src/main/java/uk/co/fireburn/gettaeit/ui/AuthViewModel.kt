package uk.co.fireburn.gettaeit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel() {

    // This flow emits the current user whenever the auth state changes
    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun onSignInClick() {
        // TODO: Implement sign in logic, e.g., using GoogleSignInClient
    }
}
