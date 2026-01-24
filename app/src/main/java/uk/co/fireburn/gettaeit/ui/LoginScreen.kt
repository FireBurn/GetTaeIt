package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository

// This is a simplified ViewModel just for the LoginScreen
@Composable
fun LoginScreen(
    authRepository: AuthRepository, // In a real app, this would be in a ViewModel
    onSignInClick: () -> Unit
) {
    val currentUser by authRepository.currentUser.collectAsState(initial = null)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentUser == null) {
            Button(onClick = onSignInClick) {
                Text("Sign in with Google")
            }
        } else {
            Text("Welcome, ${currentUser?.displayName}")
        }
    }
}
