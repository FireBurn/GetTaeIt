package uk.co.fireburn.gettaeit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import uk.co.fireburn.gettaeit.shared.data.MissedBehaviour
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onTaskAdded: () -> Unit
) {
    val state by viewModel.addTaskState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Task", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.resetAddTaskState(); onTaskAdded() }) {
                        Icon(Icons.Filled.Close, "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveTask(); onTaskAdded() },
                        enabled = state.title.isNotBlank(),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Save") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Title ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.updateAddTaskState { copy(title = it) } },
                label = { Text("What needs tae be done?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3,
                trailingIcon = {
                    if (state.title.length > 5) {
                        IconButton(onClick = { viewModel.requestSubtaskBreakdown() }) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = "Break it doon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )

            // ── Description ──────────────────────────────────────────────────
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.updateAddTaskState { copy(description = it) } },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // ── Context selector ─────────────────────────────────────────────
            SectionLabel("Context")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContextChip(
                    label = "🏠 Personal",
                    selected = state.context == TaskContext.PERSONAL,
                    selectedColor = Color(0xFF8D5CA5),
                    onClick = { viewModel.updateAddTaskState { copy(context = TaskContext.PERSONAL) } }
                )
                ContextChip(
                    label = "💼 Work",
                    selected = state.context == TaskContext.WORK,
                    selectedColor = Color(0xFF0065BD),
                    onClick = { viewModel.updateAddTaskState { copy(context = TaskContext.WORK) } }
                )
                ContextChip(
                    label = "🔁 Both",
                    selected = state.context == TaskContext.ANY,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.updateAddTaskState { copy(context = TaskContext.ANY) } }
                )
            }

            // ── Priority ─────────────────────────────────────────────────────
            SectionLabel("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    1 to "🔴 Urgent",
                    2 to "🟠 High",
                    3 to "🟡 Normal",
                    4 to "🟢 Low",
                    5 to "⚪ Someday"
                )
                    .forEach { (p, label) ->
                        FilterChip(
                            selected = state.priority == p,
                            onClick = { viewModel.updateAddTaskState { copy(priority = p) } },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
            }

            // ── Recurrence ───────────────────────────────────────────────────
            SectionLabel("Repeat")
            RecurrenceSection(
                recurrenceType = state.recurrenceType,
                interval = state.recurrenceInterval,
                missedBehaviour = state.missedBehaviour,
                daysOfWeek = state.recurrenceDaysOfWeek,
                preferredTimeMinutes = state.preferredTimeOfDayMinutes,
                onTypeChange = {
                    viewModel.updateAddTaskState {
                        copy(
                            recurrenceType = it,
                            recurrenceDaysOfWeek = emptyList()
                        )
                    }
                },
                onIntervalChange = { viewModel.updateAddTaskState { copy(recurrenceInterval = it) } },
                onMissedBehaviourChange = { viewModel.updateAddTaskState { copy(missedBehaviour = it) } },
                onDayToggle = { day ->
                    viewModel.updateAddTaskState {
                        val newDays = if (day in recurrenceDaysOfWeek)
                            recurrenceDaysOfWeek - day else recurrenceDaysOfWeek + day
                        copy(recurrenceDaysOfWeek = newDays)
                    }
                },
                onTimeChange = { viewModel.updateAddTaskState { copy(preferredTimeOfDayMinutes = it) } }
            )

            // ── AI subtask suggestions ───────────────────────────────────────
            AnimatedVisibility(
                visible = state.isGeneratingSubtasks || state.suggestedSubtasks.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SubtaskPreviewSection(
                    isLoading = state.isGeneratingSubtasks,
                    subtasks = state.suggestedSubtasks,
                    onRemove = { idx -> viewModel.removeSuggestedSubtask(idx) }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Recurrence section ───────────────────────────────────────────────────────

@Composable
private fun RecurrenceSection(
    recurrenceType: RecurrenceType,
    interval: Int,
    missedBehaviour: MissedBehaviour,
    daysOfWeek: List<Int>,
    preferredTimeMinutes: Int?,
    onTypeChange: (RecurrenceType) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onMissedBehaviourChange: (MissedBehaviour) -> Unit,
    onDayToggle: (Int) -> Unit,
    onTimeChange: (Int?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Type row
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                RecurrenceType.NONE to "Once",
                RecurrenceType.DAILY to "Daily",
                RecurrenceType.WEEKLY to "Weekly",
                RecurrenceType.MONTHLY to "Monthly",
                RecurrenceType.CUSTOM_DAYS to "Custom"
            ).forEach { (type, label) ->
                FilterChip(
                    selected = recurrenceType == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Interval + days of week
        if (recurrenceType != RecurrenceType.NONE && recurrenceType != RecurrenceType.CUSTOM_DAYS) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Every", style = MaterialTheme.typography.bodyMedium)
                IconButton(
                    onClick = { if (interval > 1) onIntervalChange(interval - 1) },
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Filled.Remove, null, modifier = Modifier.size(16.dp)) }
                Text(
                    "$interval",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onIntervalChange(interval + 1) },
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp)) }
                Text(
                    when (recurrenceType) {
                        RecurrenceType.DAILY -> if (interval == 1) "day" else "days"
                        RecurrenceType.WEEKLY -> if (interval == 1) "week" else "weeks"
                        RecurrenceType.MONTHLY -> if (interval == 1) "month" else "months"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (recurrenceType == RecurrenceType.CUSTOM_DAYS) {
            val dayLabels = listOf(
                Calendar.MONDAY to "Mo", Calendar.TUESDAY to "Tu",
                Calendar.WEDNESDAY to "We", Calendar.THURSDAY to "Th",
                Calendar.FRIDAY to "Fr", Calendar.SATURDAY to "Sa", Calendar.SUNDAY to "Su"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dayLabels.forEach { (day, label) ->
                    FilterChip(
                        selected = day in daysOfWeek,
                        onClick = { onDayToggle(day) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Missed behaviour — only show for recurring tasks
        if (recurrenceType != RecurrenceType.NONE) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "If I miss it…",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = missedBehaviour == MissedBehaviour.IGNORABLE,
                            onClick = { onMissedBehaviourChange(MissedBehaviour.IGNORABLE) },
                            label = {
                                Text(
                                    "Let it go 👋",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                        FilterChip(
                            selected = missedBehaviour == MissedBehaviour.PERSISTENT,
                            onClick = { onMissedBehaviourChange(MissedBehaviour.PERSISTENT) },
                            label = {
                                Text(
                                    "Keep nagging me 🔔",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                    Text(
                        text = if (missedBehaviour == MissedBehaviour.IGNORABLE)
                            "Missed reminders disappear. No guilt."
                        else
                            "Keeps showing until you do it, then resets the timer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Subtask preview section ─────────────────────────────────────────────────

@Composable
private fun SubtaskPreviewSection(
    isLoading: Boolean,
    subtasks: List<String>,
    onRemove: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Suggested subtasks",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text("Working it oot…", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(
                    "These will be saved as subtasks. Tap × to remove any.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                subtasks.forEachIndexed { idx, title ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.DragHandle, null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemove(idx) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ContextChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedColor.copy(alpha = 0.15f),
            selectedLabelColor = selectedColor
        )
    )
}
