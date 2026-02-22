package uk.co.fireburn.gettaeit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.AppMode
import uk.co.fireburn.gettaeit.shared.domain.RecurrenceEngine

// ─── Context banner colours ──────────────────────────────────────────────────
private val WorkPrimary = Color(0xFF0065BD)     // Loch Blue
private val WorkSurface = Color(0xFFE8F1FB)
private val PersonalPrimary = Color(0xFF8D5CA5) // Thistle Purple
private val PersonalSurface = Color(0xFFF5EEF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: MainViewModel = hiltViewModel(),
    recurrenceEngine: RecurrenceEngine = hiltViewModel<MainViewModel>()
        .let { RecurrenceEngine() },
    onAddTaskClicked: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val appMode by viewModel.appMode.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Context mode banner ──────────────────────────────────────────────
        ContextBanner(mode = appMode)

        // ── Task list ────────────────────────────────────────────────────────
        if (tasks.isEmpty()) {
            EmptyState(mode = appMode)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        recurrenceEngine = recurrenceEngine,
                        onComplete = { viewModel.completeTask(task) },
                        onSnooze = { viewModel.snoozeTask(task) },
                        onSnoozeTomorrow = { viewModel.snoozeTomorrow(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        getSubtasks = { viewModel.getSubtasks(task.id) },
                        onCompleteSubtask = { sub -> viewModel.completeTask(sub) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) } // room for FAB
            }
        }
    }
}

// ─── Context banner ───────────────────────────────────────────────────────────

@Composable
private fun ContextBanner(mode: AppMode) {
    val (bg, icon, label, sub) = when (mode) {
        AppMode.WORK -> Quad(WorkSurface, Icons.Filled.Work, "Work Mode", "Showing your work tasks")
        AppMode.PERSONAL -> Quad(
            PersonalSurface,
            Icons.Filled.Home,
            "Home Mode",
            "Your personal tasks"
        )

        AppMode.COMMUTE -> Quad(
            Color(0xFFFFF3E0),
            Icons.Filled.DirectionsCar,
            "On The Move",
            "Top 3 tasks for the road"
        )
    }

    Surface(color = bg, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (mode == AppMode.WORK) WorkPrimary else PersonalPrimary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    sub, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    onSnooze: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onDelete: () -> Unit,
    getSubtasks: () -> kotlinx.coroutines.flow.StateFlow<List<TaskEntity>>,
    onCompleteSubtask: (TaskEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val subtasks by getSubtasks().collectAsState()

    val accentColor = if (task.context == TaskContext.WORK) WorkPrimary else PersonalPrimary
    val hasSubtasks = subtasks.isNotEmpty()
    val blockedCount = task.dependencyIds.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // ── Left accent bar + main row ────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                // Context colour accent strip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accentColor)
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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

                        // Complete checkbox
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { if (!task.isCompleted) onComplete() },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                    }

                    // ── Metadata chips row ───────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        // Recurrence chip
                        if (task.recurrence.type != RecurrenceType.NONE) {
                            MetaChip(
                                icon = Icons.Filled.Repeat,
                                label = recurrenceEngine.describeRecurrence(task.recurrence),
                                tint = accentColor
                            )
                        }

                        // Priority pip
                        if (task.priority <= 2) {
                            MetaChip(
                                icon = Icons.Filled.PriorityHigh,
                                label = "Urgent",
                                tint = Color(0xFFD32F2F)
                            )
                        }

                        // Subtasks indicator
                        if (hasSubtasks) {
                            val doneCount = subtasks.count { it.isCompleted }
                            MetaChip(
                                icon = Icons.Filled.AccountTree,
                                label = "$doneCount/${subtasks.size}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { expanded = !expanded }
                            )
                        }

                        // Blocked-by indicator
                        if (blockedCount > 0) {
                            MetaChip(
                                icon = Icons.Filled.Lock,
                                label = "Blocked by $blockedCount",
                                tint = Color(0xFFE65100)
                            )
                        }

                        // Streak
                        if (task.streakCount > 1) {
                            MetaChip(
                                icon = Icons.Filled.LocalFireDepartment,
                                label = "×${task.streakCount}",
                                tint = Color(0xFFFF6F00)
                            )
                        }
                    }
                }

                // ── Context menu ─────────────────────────────────────────────
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

            // ── Subtask expansion ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded && hasSubtasks,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(start = 20.dp, end = 16.dp, bottom = 8.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subtasks.forEach { sub ->
                        SubtaskRow(subtask = sub, onComplete = { onCompleteSubtask(sub) })
                    }
                }
            }
        }
    }
}

// ─── Subtask row ──────────────────────────────────────────────────────────────

@Composable
private fun SubtaskRow(subtask: TaskEntity, onComplete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!subtask.isCompleted) onComplete() }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            if (subtask.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (subtask.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
            color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── Meta chip ────────────────────────────────────────────────────────────────

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = modifier
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
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

// ── Helper to destructure a 4-tuple ──────────────────────────────────────────
private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d
