package uk.co.fireburn.gettaeit.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import dagger.hilt.android.AndroidEntryPoint
import uk.co.fireburn.gettaeit.shared.data.TaskEntity

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp(
    viewModel: WearViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        if (tasks.isEmpty()) {
            item {
                Text("Nae bother, chill out.")
            }
        } else {
            items(tasks) { task ->
                TaskChip(
                    task = task,
                    onCompletedChange = { isCompleted ->
                        viewModel.setTaskCompleted(task, isCompleted)
                    }
                )
            }
        }
    }
}

@Composable
fun TaskChip(
    task: TaskEntity,
    onCompletedChange: (Boolean) -> Unit
) {
    Chip(
        onClick = { onCompletedChange(!task.isCompleted) },
        label = { Text(task.title) },
        enabled = !task.isCompleted
    )
}
