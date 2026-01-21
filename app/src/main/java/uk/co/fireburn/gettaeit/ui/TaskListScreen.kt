package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TaskListScreen(viewModel: MainViewModel = hiltViewModel()) {
    val tasks by viewModel.tasks.collectAsState()

    LazyColumn {
        items(tasks) { task ->
            Text(text = task.title)
        }
    }
}
