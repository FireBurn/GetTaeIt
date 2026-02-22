package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.AppMode
import uk.co.fireburn.gettaeit.shared.domain.RecurrenceEngine

private val WorkPrimary = Color(0xFF0065BD)
private val PersonalPrimary = Color(0xFF8D5CA5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: MainViewModel = hiltViewModel(),
    recurrenceEngine: RecurrenceEngine = hiltViewModel<MainViewModel>().let { RecurrenceEngine() },
    onAddTaskClicked: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val appMode by viewModel.appMode.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()

    // Build a map of id -> how many tasks depend on each task (for "unblocks N" badge)
    val unblocksCount = remember(allTasks) {
        val map = mutableMapOf<java.util.UUID, Int>()
        allTasks.forEach { t ->
            t.dependencyIds.forEach { depId ->
                map[depId] = (map[depId] ?: 0) + 1
            }
        }
        map
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ContextBanner(mode = appMode)
        if (tasks.isEmpty()) {
            EmptyState(mode = appMode)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskGroup(
                        task = task,
                        recurrenceEngine = recurrenceEngine,
                        unblocksCount = unblocksCount[task.id] ?: 0,
                        allTasks = allTasks,
                        onCompleteSubtask = { sub -> viewModel.completeSubtask(sub) },
                        onCompleteSubtaskWithTime = { sub, mins ->
                            viewModel.completeTaskWithTime(sub, mins)
                        },
                        onSnooze = { viewModel.snoozeTask(task) },
                        onSnoozeTomorrow = { viewModel.snoozeTomorrow(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onEdit = {
                            viewModel.loadTaskForEditing(task)
                            onAddTaskClicked() // navigate to the Add/Edit screen
                        }
                    )
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}

// ─── Context banner ───────────────────────────────────────────────────────────

@Composable
private fun ContextBanner(mode: AppMode) {
    val (icon, label, sub, accent) = when (mode) {
        AppMode.WORK -> Quad(Icons.Filled.Work, "Work Mode", "Showing your work tasks", WorkPrimary)
        AppMode.PERSONAL -> Quad(
            Icons.Filled.Home,
            "Home Mode",
            "Your personal tasks",
            PersonalPrimary
        )

        AppMode.COMMUTE -> Quad(
            Icons.Filled.DirectionsCar,
            "On The Move",
            "Top 3 tasks for the road",
            Color(0xFFE65100)
        )
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    sub, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Task group: subtask cards stacked, parent footer beneath ────────────────
//
// Layout:
//   ┌─────────────────────────────────┐
//   │  Subtask 1            5m    [ ] │  ← individual subtask card
//   ├─────────────────────────────────┤
//   │  Subtask 2           10m    [ ] │
//   ├─────────────────────────────────┤
//   │  ▏ Parent task title            │  ← parent footer (accent left border)
//   │  ▏ Daily  ·  🔑 Unblocks 2     │
//   └─────────────────────────────────┘
//
// If no subtasks, just the parent card is shown (slightly taller).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskGroup(
    task: TaskEntity,
    recurrenceEngine: RecurrenceEngine,
    unblocksCount: Int,
    allTasks: List<TaskEntity>,
    onCompleteSubtask: (TaskEntity) -> Unit,
    onCompleteSubtaskWithTime: (TaskEntity, Int?) -> Unit,
    onSnooze: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    TaskGroupStateful(
        task = task,
        recurrenceEngine = recurrenceEngine,
        unblocksCount = unblocksCount,
        allTasks = allTasks,
        onCompleteSubtask = onCompleteSubtask,
        onCompleteSubtaskWithTime = onCompleteSubtaskWithTime,
        onSnooze = onSnooze,
        onSnoozeTomorrow = onSnoozeTomorrow,
        onDelete = onDelete,
        onEdit = onEdit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskGroupStateful(
    task: TaskEntity,
    recurrenceEngine: RecurrenceEngine,
    unblocksCount: Int,
    allTasks: List<TaskEntity>,
    onCompleteSubtask: (TaskEntity) -> Unit,
    onCompleteSubtaskWithTime: (TaskEntity, Int?) -> Unit,
    onSnooze: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val subtasks by remember(task.id) {
        viewModel.getSubtasks(task.id)
    }.collectAsState(initial = emptyList())

    val accent = if (task.context == TaskContext.WORK) WorkPrimary else PersonalPrimary

    // Check if this task is blocked (any dependency not yet complete)
    val blockers =
        task.dependencyIds.mapNotNull { depId -> allTasks.firstOrNull { it.id == depId } }
    val isBlocked = blockers.any { !it.isCompleted }

    val doneCount = subtasks.count { it.isCompleted }
    val totalCount = subtasks.size

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

        // ── Subtask cards (shown first, most prominent) ────────────────────
        subtasks.forEach { sub ->
            SubtaskCard(
                subtask = sub,
                accent = accent,
                onComplete = { onCompleteSubtask(sub) },
                onCompleteWithTime = { mins -> onCompleteSubtaskWithTime(sub, mins) }
            )
        }

        // ── Parent footer card ─────────────────────────────────────────────
        ParentFooterCard(
            task = task,
            accent = accent,
            recurrenceEngine = recurrenceEngine,
            unblocksCount = unblocksCount,
            blockers = blockers,
            isBlocked = isBlocked,
            doneCount = doneCount,
            totalCount = totalCount,
            onSnooze = onSnooze,
            onSnoozeTomorrow = onSnoozeTomorrow,
            onDelete = onDelete,
            onEdit = onEdit
        )
    }
}

// ─── Individual subtask card ─────────────────────────────────────────────────

@Composable
private fun SubtaskCard(
    subtask: TaskEntity,
    accent: Color,
    onComplete: () -> Unit,
    onCompleteWithTime: (Int?) -> Unit
) {
    var showTimeDlg by remember { mutableStateOf(false) }
    val done = subtask.isCompleted

    if (showTimeDlg) {
        CompletionTimeDialog(
            estimatedMinutes = subtask.estimatedMinutes,
            onConfirm = { mins -> onCompleteWithTime(mins); showTimeDlg = false },
            onSkip = { onComplete(); showTimeDlg = false },
            onDismiss = { showTimeDlg = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (done)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !done) {
                    if (subtask.estimatedMinutes != null) showTimeDlg = true
                    else onComplete()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Completion icon
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (done) "Done" else "Complete",
                tint = if (done) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            // Title
            Text(
                text = subtask.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Time estimate
            subtask.estimatedMinutes?.let { mins ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Filled.Timer, null,
                        modifier = Modifier.size(11.dp),
                        tint = accent.copy(alpha = if (done) 0.4f else 0.8f)
                    )
                    Text(
                        formatMinutes(mins),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent.copy(alpha = if (done) 0.4f else 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─── Parent footer card ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentFooterCard(
    task: TaskEntity,
    accent: Color,
    recurrenceEngine: RecurrenceEngine,
    unblocksCount: Int,
    blockers: List<TaskEntity>,
    isBlocked: Boolean,
    doneCount: Int,
    totalCount: Int,
    onSnooze: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = if (totalCount > 0) 0.dp else 14.dp,
            topEnd = if (totalCount > 0) 0.dp else 14.dp,
            bottomStart = 14.dp,
            bottomEnd = 14.dp
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (totalCount > 0) 64.dp else 72.dp)
                    .background(
                        if (isBlocked) accent.copy(alpha = 0.3f) else accent,
                        RoundedCornerShape(bottomStart = 14.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = if (totalCount > 0) MaterialTheme.typography.bodyMedium
                        else MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Progress if has subtasks
                if (totalCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { doneCount.toFloat() / totalCount },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = accent,
                            trackColor = accent.copy(alpha = 0.15f)
                        )
                        Text(
                            "$doneCount/$totalCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Meta chips row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (task.recurrence.type != RecurrenceType.NONE) {
                        MetaChip(
                            Icons.Filled.Repeat,
                            recurrenceEngine.describeRecurrence(task.recurrence), accent
                        )
                    }
                    if (task.priority <= 2) {
                        MetaChip(Icons.Filled.PriorityHigh, "Urgent", Color(0xFFD32F2F))
                    }
                    task.estimatedMinutes?.let { mins ->
                        if (totalCount == 0) MetaChip(
                            Icons.Filled.Timer,
                            formatMinutes(mins),
                            accent
                        )
                    }
                    // 🔑 Unblocks badge — shows if other tasks depend on this one
                    if (unblocksCount > 0) {
                        MetaChip(
                            Icons.Filled.Key,
                            "Unblocks $unblocksCount",
                            Color(0xFF2E7D32)
                        )
                    }
                    // 🔒 Blocked badge — shows what's blocking this
                    if (isBlocked) {
                        MetaChip(
                            Icons.Filled.Lock,
                            "Blocked by ${blockers.count { !it.isCompleted }}",
                            Color(0xFFE65100)
                        )
                    }
                    if (task.streakCount > 1) {
                        MetaChip(
                            Icons.Filled.LocalFireDepartment,
                            "×${task.streakCount}",
                            Color(0xFFFF6F00)
                        )
                    }
                }

                // Blocker names (collapsed to one line if long)
                if (isBlocked) {
                    val blockerNames =
                        blockers.filter { !it.isCompleted }.joinToString(" · ") { it.title }
                    Text(
                        "Waiting on: $blockerNames",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100).copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ⋮ menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert, "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit task") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = { onEdit(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Snooze 2 hours") },
                        leadingIcon = { Icon(Icons.Filled.Snooze, null) },
                        onClick = { onSnooze(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Snooze till tomorrow") },
                        leadingIcon = { Icon(Icons.Filled.WbSunny, null) },
                        onClick = { onSnoozeTomorrow(); showMenu = false }
                    )
                    if (task.recurrence.type == RecurrenceType.NONE) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    null,
                                    tint = Color(0xFFD32F2F)
                                )
                            },
                            onClick = { onDelete(); showMenu = false }
                        )
                    }
                }
            }
        }
    }
}

// ─── Completion time dialog ───────────────────────────────────────────────────

@Composable
fun CompletionTimeDialog(
    estimatedMinutes: Int?,
    onConfirm: (Int?) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(estimatedMinutes?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How long did that take? ⏱️") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (estimatedMinutes != null) {
                    Text(
                        "Guessed ${formatMinutes(estimatedMinutes)}. Help us get better!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text("Minutes") },
                    suffix = { Text("min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(input.toIntOrNull()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onSkip) { Text("Skip") } }
    )
}

// ─── Meta chip ────────────────────────────────────────────────────────────────

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(10.dp), tint = tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(mode: AppMode = AppMode.PERSONAL) {
    val (emoji, headline, body) = when (mode) {
        AppMode.WORK -> Triple("🏆", "Cracking job, go hame!", "Ye've nothing left tae dae here.")
        AppMode.PERSONAL -> Triple("🛋️", "Nae bother!", "Chill oot. Yer list is empty.")
        AppMode.COMMUTE -> Triple("🚗", "Safe travels!", "Nothing urgent for the road.")
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.displayLarge)
            Text(
                headline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                body, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d
