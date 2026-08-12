package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    onMakeSmaller: (TaskEntity) -> Unit,
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
            var showWallOfAwful by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
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
            
            if (!showWallOfAwful) {
                Button(
                    onClick = { onComplete(task) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sorted") }
                OutlinedButton(
                    onClick = { showWallOfAwful = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Why is this hard? (Wall of Awful)") }
            } else {
                Text(
                    "What's stopping you?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedButton(
                    onClick = { onMakeSmaller(task); showWallOfAwful = false },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Too many steps -> Break it down (AI)") }
                OutlinedButton(
                    onClick = { onSnooze(task); showWallOfAwful = false },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("I'm exhausted -> Snooze it") }
                OutlinedButton(
                    onClick = { onStartFocus(5); showWallOfAwful = false },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("I'm anxious -> 5 Min Focus Dash") }
                TextButton(
                    onClick = { showWallOfAwful = false },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) { Text("Cancel") }
            }
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
                    
                    if (session.activeUsersCount > 0) {
                        var showLobby by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                        
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showLobby = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.Group,
                                contentDescription = "Lobby",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${session.activeUsersCount} other ${if (session.activeUsersCount == 1) "person is" else "people are"} focusing right now.\nTap to enter Body Doubling Lobby.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        if (showLobby) {
                            BodyDoublingLobbyDialog(
                                activeUsersCount = session.activeUsersCount,
                                onDismiss = { showLobby = false },
                                onStartFocus = { 
                                    showLobby = false
                                    onStart(it) 
                                }
                            )
                        }
                    }

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
                    val progress = if (session.totalSeconds > 0) session.remainingSeconds.toFloat() / session.totalSeconds.toFloat() else 0f
                    
                    var isNoisePlaying by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    val noiseGenerator = androidx.compose.runtime.remember { uk.co.fireburn.gettaeit.shared.audio.NoiseGenerator() }
                    
                    androidx.compose.runtime.DisposableEffect(Unit) {
                        onDispose { noiseGenerator.stop() }
                    }

                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp).size(120.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                        Text(
                            text = "%d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Stay with the one wee thing. I’m here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { 
                            onStop()
                            noiseGenerator.stop() 
                        }) {
                            Text("End focus block")
                        }
                        AssistChip(
                            onClick = { 
                                isNoisePlaying = !isNoisePlaying 
                                if (isNoisePlaying) noiseGenerator.start() else noiseGenerator.stop()
                            },
                            label = { Text(if (isNoisePlaying) "Stop Brown Noise" else "Play Brown Noise") }
                        )
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BodyDoublingLobbyDialog(
    activeUsersCount: Int,
    onDismiss: () -> Unit,
    onStartFocus: (Int) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(androidx.compose.material.icons.Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text("Body Doubling Lobby")
            }
        },
        text = {
            Column {
                Text(
                    "Knowing others are working right now can help your brain engage. You are not alone!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                
                // Generate some fake users for the lobby feel
                val fakeNames = androidx.compose.runtime.remember { listOf("Alex", "Sam", "Jordan", "Casey", "Taylor", "Morgan", "Riley", "Drew").shuffled().take(minOf(activeUsersCount, 4).coerceAtLeast(1)) }
                
                fakeNames.forEach { name ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                        Text(name, fontWeight = FontWeight.Medium)
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        Text("Focusing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                if (activeUsersCount > fakeNames.size) {
                    Text(
                        "...and ${activeUsersCount - fakeNames.size} others",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, start = 44.dp)
                    )
                }
                
                androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
                Text("Join them:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 15, 25).forEach { minutes ->
                        AssistChip(
                            onClick = { onStartFocus(minutes) },
                            label = { Text("$minutes m") },
                            colors = AssistChipDefaults.assistChipColors()
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
