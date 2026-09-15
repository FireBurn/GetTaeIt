package uk.co.fireburn.gettaeit.wear.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import dagger.hilt.android.AndroidEntryPoint
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Icon
import uk.co.fireburn.gettaeit.wear.R

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp() }
    }
}

data class PendingAction(
    val type: String,
    val task: TaskEntity,
    val color: Color,
    val onExecute: () -> Unit
)

@Composable
fun WearApp(viewModel: WearViewModel = hiltViewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val isSendingVoice by viewModel.isSendingVoice.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    var voiceStatusMsg by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            val capitalized = spokenText.replaceFirstChar { it.uppercase() }
            viewModel.sendVoiceTaskToPhone(capitalized) { success ->
                voiceStatusMsg = if (success) "Sent to phone ✓" else "Saved on the watch — it'll reach your phone later"
            }
        }
    }

    fun startVoiceInput() {
        viewModel.startHaptic()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you need to get tae?")
        }
        voiceLauncher.launch(intent)
    }

    MaterialTheme {
        if (pendingAction != null) {
            var timeLeft by remember { mutableIntStateOf(3) }
            androidx.compose.runtime.LaunchedEffect(pendingAction) {
                timeLeft = 3
                while (timeLeft > 0) {
                    delay(1000)
                    timeLeft--
                }
                pendingAction?.onExecute?.invoke()
                pendingAction = null
            }
            
            Box(
                modifier = Modifier.fillMaxSize().background(pendingAction!!.color),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val text = when (pendingAction!!.type) {
                        "complete" -> "Completing in $timeLeft"
                        "snooze" -> "Snoozing (2h) in $timeLeft"
                        "delete" -> "Deleting in $timeLeft"
                        else -> ""
                    }
                    Text(text, style = MaterialTheme.typography.title3, color = Color.White)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { pendingAction = null },
                        modifier = Modifier.size(56.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Cancel", style = MaterialTheme.typography.caption3, color = Color.White)
                }
            }
        } else {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.coo_mark),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Get Tae It", style = MaterialTheme.typography.title3)
                    }
                }
                item {
                    // A chip rather than a fixed-height button, so it grows with the font size.
                    Chip(
                        onClick = { startVoiceInput() },
                        enabled = !isSendingVoice,
                        label = { Text(if (isSendingVoice) "Sending…" else "Add task") },
                        icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                        colors = ChipDefaults.primaryChipColors(backgroundColor = Color(0xFF6200EE)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                voiceStatusMsg?.let { msg ->
                    item { Text(msg, style = MaterialTheme.typography.caption3, color = if (msg.contains("✓")) Color(0xFF4CAF50) else Color(0xFFFF5722), modifier = Modifier.padding(horizontal = 8.dp)) }
                }

                if (tasks.isEmpty()) {
                    item { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) { Text("Chill oot. Yer list is empty.", modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center) } }
                } else {
                    items(tasks.take(8)) { task ->
                        WearTaskCard(
                            task = task,
                            onComplete = { 
                                pendingAction = PendingAction("complete", task, Color(0xFF4CAF50)) { viewModel.setTaskCompleted(task, true) }
                            },
                            onSnooze = { 
                                pendingAction = PendingAction("snooze", task, Color(0xFFFFA000)) { viewModel.snoozeTask(task) }
                            },
                            onDelete = { 
                                pendingAction = PendingAction("delete", task, Color(0xFFD32F2F)) { viewModel.deleteTask(task) }
                            }
                        )
                    }
                }

                item {
                    ToggleChip(
                        checked = hapticsEnabled,
                        onCheckedChange = { viewModel.toggleHaptics() },
                        label = { Text("Haptics") },
                        toggleControl = {
                            Icon(
                                imageVector = ToggleChipDefaults.switchIcon(hapticsEnabled),
                                contentDescription = if (hapticsEnabled) "On" else "Off"
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WearTaskCard(task: TaskEntity, onComplete: () -> Unit, onSnooze: () -> Unit, onDelete: () -> Unit) {
    val dueDateText = task.dueDate?.let {
        val now = System.currentTimeMillis()
        val diffHrs = (it - now) / 3_600_000
        when {
            diffHrs < 0 -> "Overdue"
            diffHrs < 1 -> "Due now"
            diffHrs < 24 -> "Due in ${diffHrs}h"
            else -> SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(it))
        }
    }

    TitleCard(
        onClick = onComplete,
        title = {
            Text(
                task.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (task.priority <= 2) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (dueDateText != null) {
                Text(
                    dueDateText,
                    style = MaterialTheme.typography.caption3,
                    color = if (dueDateText == "Overdue") Color(0xFFFF4444) else Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            if (task.estimatedMinutes != null) {
                Text(
                    "~${task.estimatedMinutes}min",
                    style = MaterialTheme.typography.caption3,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Green (Complete)
                Button(
                    onClick = onComplete,
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                ) { Icon(Icons.Filled.Check, contentDescription = "Complete", tint = Color.White, modifier = Modifier.size(20.dp)) }
                // Amber (Snooze)
                Button(
                    onClick = onSnooze,
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFA000))
                ) { Icon(Icons.Filled.Snooze, contentDescription = "Snooze", tint = Color.White, modifier = Modifier.size(20.dp)) }
                // Red (Delete)
                Button(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
                ) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
