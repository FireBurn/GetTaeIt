package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import android.content.Intent
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import uk.co.fireburn.gettaeit.R
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.data.EffortLevel
import uk.co.fireburn.gettaeit.shared.domain.AppMode
import uk.co.fireburn.gettaeit.shared.domain.DependencyGraph
import uk.co.fireburn.gettaeit.shared.domain.RecurrenceEngine
import uk.co.fireburn.gettaeit.shared.domain.TaskNowFilter
import uk.co.fireburn.gettaeit.ui.theme.MonoLabelStyle

/** Personal tasks read as thistle, work tasks as loch — same trio as the rest of the app. */
private val AppMode.contextAccent: androidx.compose.ui.graphics.Color
    @Composable get() = when (this) {
        AppMode.WORK -> MaterialTheme.colorScheme.secondary
        AppMode.PERSONAL -> MaterialTheme.colorScheme.primary
        AppMode.COMMUTE -> MaterialTheme.colorScheme.tertiary
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: MainViewModel = hiltViewModel(),
    recurrenceEngine: RecurrenceEngine = hiltViewModel<MainViewModel>().let { RecurrenceEngine() },
    onAddTaskClicked: () -> Unit,
    onGoblinModeClicked: () -> Unit,
    onWeeklyReviewClicked: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val appMode by viewModel.appMode.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val completedToday by viewModel.completedToday.collectAsState()
    val completionCelebration by viewModel.completionCelebration.collectAsState()
    var availableMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    var lowEnergyOnly by rememberSaveable { mutableStateOf(false) }
    val visibleTasks = remember(tasks, availableMinutes, lowEnergyOnly) {
        TaskNowFilter.filter(tasks, availableMinutes, lowEnergyOnly)
    }

    // Direct + transitive: a task blocking a chain of three outranks one blocking a single task.
    val unblocksCount = remember(allTasks) {
        allTasks.associate { it.id to DependencyGraph.transitiveUnblockCount(it.id, allTasks) }
    }

    // Best currently-active streak, for a wee bit of bragging rights in the header.
    val bestStreak = remember(tasks) { tasks.maxOfOrNull { it.streakCount } ?: 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars) // Push content below the status bar!
    ) {
        BrandHeader(
            streak = bestStreak,
            onGoblinModeClicked = onGoblinModeClicked,
            onWeeklyReviewClicked = onWeeklyReviewClicked
        )
        ContextBanner(mode = appMode)
        completionCelebration?.let { celebration ->
            CompletionCelebrationCard(
                celebration = celebration,
                onDismiss = viewModel::clearCompletionCelebration
            )
        }
        WinsSummary(
            completedCount = completedToday.size,
            totalXp = completedToday.sumOf { it.xpValue },
            bestStreak = bestStreak
        )
        TaskNowFilterBar(
            availableMinutes = availableMinutes,
            lowEnergyOnly = lowEnergyOnly,
            onMinutesChanged = { availableMinutes = it },
            onLowEnergyChanged = { lowEnergyOnly = it }
        )
        if (visibleTasks.isEmpty()) {
            EmptyState(mode = appMode)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visibleTasks, key = { it.id }) { task ->
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
                        onArchive = { viewModel.archiveTask(task) },
                        onMakeSmaller = { viewModel.makeTaskSmaller(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onEdit = {
                            viewModel.loadTaskForEditing(task)
                            onAddTaskClicked()
                        }
                    )
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}

@Composable
private fun CompletionCelebrationCard(
    celebration: CompletionCelebration,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Nice — +${celebration.xp} XP for ${celebration.taskTitle}", modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    }
}

@Composable
private fun WinsSummary(completedCount: Int, totalXp: Int, bestStreak: Int) {
    if (completedCount == 0 && bestStreak < 2) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = if (completedCount == 1) "One win today · $totalXp XP" else "$completedCount wins today · $totalXp XP",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (bestStreak > 1) {
                Text(
                    "Routine streak ×$bestStreak. A day off is allowed — life happens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskNowFilterBar(
    availableMinutes: Int?,
    lowEnergyOnly: Boolean,
    onMinutesChanged: (Int?) -> Unit,
    onLowEnergyChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(10, 20, 45).forEach { minutes ->
            AssistChip(
                onClick = { onMinutesChanged(if (availableMinutes == minutes) null else minutes) },
                label = { Text("$minutes min") }
            )
        }
        AssistChip(
            onClick = { onLowEnergyChanged(!lowEnergyOnly) },
            label = { Text(if (lowEnergyOnly) "Low energy ✓" else "Low energy") }
        )
    }
}

// ─── Brand header ─────────────────────────────────────────────────────────────

@Composable
private fun BrandHeader(
    streak: Int,
    onGoblinModeClicked: () -> Unit,
    onWeeklyReviewClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(
                painter = painterResource(R.drawable.coo_mark),
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
            )
            Text(
                "Get Tae It",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onWeeklyReviewClicked) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = "Open weekly review",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onGoblinModeClicked) {
                Icon(
                    Icons.Filled.Visibility,
                    contentDescription = "Enter Goblin Mode",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (streak > 1) Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = "Best streak",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "×$streak",
                    style = MonoLabelStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

// ─── Context banner ───────────────────────────────────────────────────────────

@Composable
private fun ContextBanner(mode: AppMode) {
    val accent = mode.contextAccent
    val (icon, label, sub) = when (mode) {
        AppMode.WORK -> Triple(Icons.Filled.Work, "Work Mode", "Showing your work tasks")
        AppMode.PERSONAL -> Triple(Icons.Filled.Home, "Home Mode", "Your personal tasks")
        AppMode.COMMUTE -> Triple(
            Icons.Filled.DirectionsCar,
            "On The Move",
            "Top 3 tasks for the road"
        )
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    sub, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Task group: subtask cards stacked, parent footer beneath ────────────────

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
    onArchive: () -> Unit,
    onMakeSmaller: () -> Unit,
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
        onArchive = onArchive,
        onMakeSmaller = onMakeSmaller,
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
    onArchive: () -> Unit,
    onMakeSmaller: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val subtasks by remember(task.id) {
        viewModel.getSubtasks(task.id)
    }.collectAsState(initial = emptyList())

    val accent =
        if (task.context == TaskContext.WORK) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.primary
    val blockers =
        task.dependencyIds.mapNotNull { depId -> allTasks.firstOrNull { it.id == depId } }
    val isBlocked = blockers.any { !it.isCompleted }

    val doneCount = subtasks.count { it.isCompleted }
    val totalCount = subtasks.size

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        subtasks.forEach { sub ->
            SubtaskCard(
                subtask = sub,
                accent = accent,
                onComplete = { onCompleteSubtask(sub) },
                onCompleteWithTime = { mins -> onCompleteSubtaskWithTime(sub, mins) }
            )
        }
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
            onArchive = onArchive,
            onMakeSmaller = onMakeSmaller,
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
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (done) "Done" else "Complete",
                tint = if (done) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

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
    onArchive: () -> Unit,
    onMakeSmaller: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showGentleReentry by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val errorColor = MaterialTheme.colorScheme.error
    val tertiary = MaterialTheme.colorScheme.tertiary

    if (showGentleReentry) {
        GentleReentryDialog(
            task = task,
            onMakeSmaller = { onMakeSmaller(); showGentleReentry = false },
            onSnoozeTomorrow = { onSnoozeTomorrow(); showGentleReentry = false },
            onAskForHelp = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Could you help me get started with “${task.title}”? Even a quick check-in would help."
                    )
                }
                context.startActivity(Intent.createChooser(intent, "Ask for help"))
                showGentleReentry = false
            },
            onArchive = { onArchive(); showGentleReentry = false },
            onDismiss = { showGentleReentry = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = if (totalCount > 0) 0.dp else 17.dp,
            topEnd = if (totalCount > 0) 0.dp else 17.dp,
            bottomStart = 17.dp,
            bottomEnd = 17.dp
        ),
        border = if (unblocksCount > 0 && !isBlocked) BorderStroke(
            1.dp,
            tertiary.copy(alpha = 0.4f)
        ) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isBlocked) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isBlocked) Modifier.alpha(0.72f) else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(if (totalCount > 0) 64.dp else 72.dp)
                    .background(
                        if (isBlocked) accent.copy(alpha = 0.3f) else accent,
                        RoundedCornerShape(bottomStart = 17.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = task.title,
                        style = if (totalCount > 0) MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.titleMedium,
                        color = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // The one bold element on the card — everything else stays quiet.
                    if (unblocksCount > 0 && !isBlocked) {
                        UnlocksBadge(count = unblocksCount, color = tertiary)
                    }
                    if (isBlocked) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Blocked",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

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
                            style = MonoLabelStyle,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

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
                        MetaChip(Icons.Filled.PriorityHigh, "Urgent", errorColor)
                    }
                    task.estimatedMinutes?.let { mins ->
                        if (totalCount == 0) MetaChip(
                            Icons.Filled.Timer,
                            formatMinutes(mins),
                            accent
                        )
                    }
                    if (task.effortLevel != EffortLevel.MEDIUM) {
                        MetaChip(
                            Icons.Filled.WbSunny,
                            if (task.effortLevel == EffortLevel.LOW) "Low effort" else "High effort",
                            accent
                        )
                    }
                    if (task.streakCount > 1) {
                        MetaChip(
                            Icons.Filled.LocalFireDepartment,
                            "×${task.streakCount}",
                            tertiary
                        )
                    }
                }

                if (isBlocked) {
                    val blockerNames =
                        blockers.filter { !it.isCompleted }.joinToString(" · ") { it.title }
                    Text(
                        "Waiting on $blockerNames",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert, "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Gentle reset") },
                        leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) },
                        onClick = { showGentleReentry = true; showMenu = false }
                    )
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
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun GentleReentryDialog(
    task: TaskEntity,
    onMakeSmaller: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onAskForHelp: () -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stuck is information") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("“${task.title}” has been hanging about. That is not a failure.")
                task.frictionNote?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        "Last time you noted: $note",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Pick the kindest next move.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onMakeSmaller) { Text("Make it smaller") }
                TextButton(onClick = onSnoozeTomorrow) { Text("Snooze without guilt") }
                TextButton(onClick = onAskForHelp) { Text("Ask for help") }
                TextButton(onClick = onArchive) { Text("Archive it") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
    )
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
        Text(label, style = MonoLabelStyle, fontSize = 10.5.sp, color = tint)
    }
}

// ─── Unlocks badge ────────────────────────────────────────────────────────────
// The one loud element on a card — a task holding up N others jumps the queue,
// and this is why. Everything else on the card stays quiet by comparison.

@Composable
private fun UnlocksBadge(count: Int, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Filled.Key,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
        Text(
            "Unlocks $count",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 12.sp,
            color = Color.White
        )
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
