package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AddTaskScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onTaskAdded: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val isBreakingDownTask by viewModel.isBreakingDownTask.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("What needs tae be done?") },
            enabled = !isBreakingDownTask
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isBreakingDownTask) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    viewModel.breakdownAndAddTask(prompt)
                    onTaskAdded()
                },
                enabled = prompt.isNotBlank()
            ) {
                Text("Get it done!")
            }
        }
    }
}
