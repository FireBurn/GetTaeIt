package uk.co.fireburn.gettaeit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.draw.rotate
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

// ─── Accent colours ───────────────────────────────────────────────────────────
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

    Column(modifier = Modifier.fillMaxSize()) {
        ContextBanner(mode = appMode)

        if (tasks.isEmpty()) {
            EmptyState(mode = appMode)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        recurrenceEngine = recurrenceEngine,
                        onComplete = { viewModel.completeTask(task) },
                        onCompleteWithTime = { mins -> viewModel.completeTaskWithTime(task, mins) },
                        onSnooze = { viewModel.snoozeTask(task) },
                        onSnoozeTomorrow = { viewModel.snoozeTomorrow(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        getSubtasks = { viewModel.getSubtasks(task.id) },
                        onCompleteSubtask = { sub -> viewModel.completeTask(sub) },
                        onCompleteSubtaskWithTime = { sub, mins ->
                            viewModel.completeTaskWithTime(
                                sub,
                                mins
                            )
                        }
                    )
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}

// ─── Context banner — uses MaterialTheme so it works in dark mode ─────────────

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

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 0.dp) {
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
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Task card ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: TaskEntity,
    recurrenceEngine: RecurrenceEngine,
    onComplete: () -> Unit,
    onCompleteWithTime: (Int?) -> Unit,
    onSnooze: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onDelete: () -> Unit,
    getSubtasks: () -> kotlinx.coroutines.flow.StateFlow<List<TaskEntity>>,
    onCompleteSubtask: (TaskEntity) -> Unit,
    onCompleteSubtaskWithTime: (TaskEntity, Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showTimeDlg by remember { mutableStateOf(false) }
    val subtasks by getSubtasks().collectAsState()

    val accentColor = if (task.context == TaskContext.WORK) WorkPrimary else PersonalPrimary
    val hasSubtasks = subtasks.isNotEmpty()
    val blockedCount = task.dependencyIds.size
    val doneCount = subtasks.count { it.isCompleted }
    val arrowAngle by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")

    if (showTimeDlg) {
        CompletionTimeDialog(
            estimatedMinutes = task.estimatedMinutes,
            onConfirm = { mins -> onCompleteWithTime(mins); showTimeDlg = false },
            onSkip = { onComplete(); showTimeDlg = false },
            onDismiss = { showTimeDlg = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)) {
                // Accent strip — fillMaxHeight works because Row uses IntrinsicSize.Min
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .background(accentColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            task.description?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Complete button
                        IconButton(
                            onClick = {
                                if (!task.isCompleted) {
                                    if (task.estimatedMinutes != null) showTimeDlg = true
                                    else onComplete()
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = "Complete",
                                tint = if (task.isCompleted) accentColor
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        if (task.recurrence.type != RecurrenceType.NONE) {
                            MetaChip(
                                Icons.Filled.Repeat,
                                recurrenceEngine.describeRecurrence(task.recurrence),
                                accentColor
                            )
                        }
                        if (task.priority <= 2) {
                            MetaChip(Icons.Filled.PriorityHigh, "Urgent", Color(0xFFD32F2F))
                        }
                        task.estimatedMinutes?.let {
                            MetaChip(Icons.Filled.Timer, formatMinutes(it), accentColor)
                        }
                        if (blockedCount > 0) {
                            MetaChip(
                                Icons.Filled.Lock,
                                "Blocked by $blockedCount",
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

                    // Subtask progress bar + expand trigger
                    if (hasSubtasks) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(vertical = 2.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { if (subtasks.isEmpty()) 0f else doneCount.toFloat() / subtasks.size },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = accentColor,
                                trackColor = accentColor.copy(alpha = 0.15f)
                            )
                            Text(
                                "$doneCount/${subtasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(arrowAngle),
                                tint = accentColor
                            )
                        }
                    }
                }

                // ⋮ menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Filled.MoreVert, contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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

            // Subtask list
            AnimatedVisibility(
                visible = expanded && hasSubtasks,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subtasks.forEach { sub ->
                        SubtaskRow(
                            subtask = sub,
                            accent = accentColor,
                            onComplete = { if (!sub.isCompleted) onCompleteSubtask(sub) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Subtask row — prominent ──────────────────────────────────────────────────

@Composable
private fun SubtaskRow(subtask: TaskEntity, accent: Color, onComplete: () -> Unit) {
    val done = subtask.isCompleted
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (done) accent.copy(alpha = 0.07f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
            .clickable { if (!done) onComplete() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (done) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
            textDecoration = if (done) TextDecoration.LineThrough else null,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        subtask.estimatedMinutes?.let {
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatMinutes(it),
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = if (done) 0.4f else 0.85f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Completion time dialog ───────────────────────────────────────────────────

@Composable
private fun CompletionTimeDialog(
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
                        "We guessed ${formatMinutes(estimatedMinutes)}. Help us get better!",
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
        confirmButton = {
            TextButton(onClick = { onConfirm(input.toIntOrNull()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    )
}

// ─── Meta chip ────────────────────────────────────────────────────────────────

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: (() -> Unit)? = null
) {
    val mod = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = mod
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(10.dp), tint = tint)
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
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(32.dp), contentAlignment = Alignment.Center) {
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
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d
