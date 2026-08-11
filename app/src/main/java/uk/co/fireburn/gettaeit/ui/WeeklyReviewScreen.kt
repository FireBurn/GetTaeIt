package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.Calendar

/**
 * A short, kind weekly reset. It deliberately avoids scores and overdue warnings:
 * the only choices are to keep a task moving, archive it, or restore it later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val tasks by viewModel.reviewTasks.collectAsState()
    val archived by viewModel.archivedTasks.collectAsState()
    val now = System.currentTimeMillis()
    val weekStart = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val wins = remember(tasks, weekStart) { tasks.filter { (it.completedAt ?: 0L) >= weekStart } }
    val deferred = remember(tasks) { tasks.filter { !it.isCompleted && it.isSnoozed } }
    val stale = remember(tasks, now) {
        tasks.filter { !it.isCompleted && !it.isSnoozed && it.dueDate?.let { due -> due < now } == true }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Weekly review") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "A wee reset, not a report card. Keep what matters; let the rest go.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { ReviewSection("Wins this week", wins, emptyText = "No wins logged yet — there’s still time.") }
            item {
                ReviewActionSection(
                    title = "Parked for later",
                    tasks = deferred,
                    emptyText = "Nothing is waiting in the snooze pile.",
                    onTomorrow = viewModel::snoozeTomorrow,
                    onArchive = viewModel::archiveTask
                )
            }
            item {
                ReviewActionSection(
                    title = "Could use a decision",
                    tasks = stale,
                    emptyText = "Nothing needs your attention here.",
                    onTomorrow = viewModel::snoozeTomorrow,
                    onArchive = viewModel::archiveTask
                )
            }
            if (archived.isNotEmpty()) {
                item {
                    ReviewActionSection(
                        title = "Archived",
                        tasks = archived,
                        emptyText = "",
                        onTomorrow = viewModel::unarchiveTask,
                        onArchive = viewModel::unarchiveTask,
                        tomorrowLabel = "Restore",
                        archiveLabel = null
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewSection(title: String, tasks: List<TaskEntity>, emptyText: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (tasks.isEmpty()) Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else tasks.take(8).forEach { Text("• ${it.title}") }
        }
    }
}

@Composable
private fun ReviewActionSection(
    title: String,
    tasks: List<TaskEntity>,
    emptyText: String,
    onTomorrow: (TaskEntity) -> Unit,
    onArchive: (TaskEntity) -> Unit,
    tomorrowLabel: String = "Tomorrow",
    archiveLabel: String? = "Archive"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (tasks.isEmpty()) {
                Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                tasks.take(8).forEach { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(task.title, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onTomorrow(task) }) { Text(tomorrowLabel) }
                        if (archiveLabel != null) {
                            Spacer(Modifier.width(2.dp))
                            TextButton(onClick = { onArchive(task) }) { Text(archiveLabel) }
                        }
                    }
                }
            }
        }
    }
}
