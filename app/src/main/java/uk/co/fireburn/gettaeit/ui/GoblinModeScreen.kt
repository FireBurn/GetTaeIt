package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uk.co.fireburn.gettaeit.shared.data.TaskEntity

/**
 * A low-stimulation escape hatch for moments when choosing from a list is the
 * problem. It is intentionally reversible and never changes the user's plan.
 */
@Composable
fun GoblinModeScreen(
    task: TaskEntity?,
    focusSession: FocusSessionUiState,
    onComplete: (TaskEntity) -> Unit,
    onSnooze: (TaskEntity) -> Unit,
    onStartFocus: (Int) -> Unit,
    onStopFocus: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Goblin Mode",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "One wee thing. That's aw.",
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        if (task == null) {
            Text(
                text = "Nae urgent gubbins right now. Take the win.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Your next move", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    task.estimatedMinutes?.let { minutes ->
                        Text(
                            "About $minutes min",
                            modifier = Modifier.padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onComplete(task) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sorted") }
            OutlinedButton(
                onClick = { onSnooze(task) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Nope — snooze it") }
        }

        Spacer(Modifier.height(20.dp))
        FocusSessionCard(
            session = focusSession,
            onStart = onStartFocus,
            onStop = onStopFocus
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onExit) { Text("Back to the plan") }
    }
}

@Composable
private fun FocusSessionCard(
    session: FocusSessionUiState,
    onStart: (Int) -> Unit,
    onStop: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Focus buddy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            when (session.status) {
                FocusSessionStatus.IDLE -> {
                    Text(
                        "Pick a wee block. One breath, then just begin.",
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 15, 25).forEach { minutes ->
                            AssistChip(
                                onClick = { onStart(minutes) },
                                label = { Text("$minutes min") },
                                colors = AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }
                FocusSessionStatus.RUNNING -> {
                    val minutes = session.remainingSeconds / 60
                    val seconds = session.remainingSeconds % 60
                    Text(
                        text = "%d:%02d".format(minutes, seconds),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Stay with the one wee thing. I’m here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onStop, modifier = Modifier.padding(top = 8.dp)) {
                        Text("End focus block")
                    }
                }
                FocusSessionStatus.COMPLETED -> {
                    Text(
                        "Focus block complete. Stretch, drink, then choose your next move.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onStop, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
