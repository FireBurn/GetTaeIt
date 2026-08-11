package uk.co.fireburn.gettaeit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository
import uk.co.fireburn.gettaeit.shared.domain.AuthUser
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<AuthUser?> = authRepository.currentUser
    val isConfigured: Boolean get() = authRepository.isConfigured

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError.asStateFlow()

    /** Called with the raw Google ID token once Credential Manager returns one (or null if cancelled). */
    fun onGoogleIdToken(idToken: String?) {
        if (idToken == null) {
            _isSigningIn.value = false
            return
        }
        viewModelScope.launch {
            _isSigningIn.value = true
            authRepository.signInWithGoogle(idToken)
                .onFailure { e -> _signInError.value = e.message ?: "Sign-in failed." }
            _isSigningIn.value = false
        }
    }

    fun onSignInStarting() {
        _signInError.value = null
        _isSigningIn.value = true
    }

    fun onSignInCancelled() {
        _isSigningIn.value = false
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun clearError() {
        _signInError.value = null
    }
}
