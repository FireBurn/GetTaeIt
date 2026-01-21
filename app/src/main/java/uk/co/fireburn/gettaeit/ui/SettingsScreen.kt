package uk.co.fireburn.gettaeit.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel = hiltViewModel()) {
    val appContext by viewModel.appContext.collectAsState()

    Text(text = "Current Context: ${appContext.name}")
}
