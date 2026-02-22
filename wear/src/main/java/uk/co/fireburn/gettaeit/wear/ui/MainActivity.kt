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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard
import dagger.hilt.android.AndroidEntryPoint
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp() }
    }
}

@Composable
fun WearApp(viewModel: WearViewModel = hiltViewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    val isSendingVoice by viewModel.isSendingVoice.collectAsState()
    var voiceStatusMsg by remember { mutableStateOf<String?>(null) }

    // Voice input launcher — opens the watch's built-in speech recognition
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            viewModel.sendVoiceTaskToPhone(spokenText) { success ->
                voiceStatusMsg = if (success) "Sent to phone ✓" else "Phone not reachable"
            }
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you need to get tae?")
        }
        voiceLauncher.launch(intent)
    }

    MaterialTheme {
        ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    "Get Tae It",
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Mic button at top for quick voice task capture
            item {
                Button(
                    onClick = { startVoiceInput() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF6200EE)
                    ),
                    enabled = !isSendingVoice
                ) {
                    Text(
                        if (isSendingVoice) "Sending..." else "🎤 Add task",
                        fontSize = 14.sp
                    )
                }
            }

            voiceStatusMsg?.let { msg ->
                item {
                    Text(
                        msg,
                        style = MaterialTheme.typography.caption3,
                        color = if (msg.contains("✓")) Color(0xFF4CAF50) else Color(0xFFFF5722),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        Text("Nae bother, all done! 🎉", modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                items(tasks.take(8)) { task ->
                    WearTaskCard(
                        task = task,
                        onComplete = { viewModel.setTaskCompleted(task, true) },
                        onSnooze = { viewModel.snoozeTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun WearTaskCard(task: TaskEntity, onComplete: () -> Unit, onSnooze: () -> Unit) {
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
                fontWeight = if (task.priority <= 2) FontWeight.Bold else FontWeight.Normal
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Column {
            if (dueDateText != null) {
                Text(
                    dueDateText,
                    style = MaterialTheme.typography.caption3,
                    color = if (dueDateText == "Overdue") Color(0xFFFF4444) else Color.Gray
                )
            }
            if (task.estimatedMinutes != null) {
                Text(
                    "~${task.estimatedMinutes}min",
                    style = MaterialTheme.typography.caption3,
                    color = Color.Gray
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.size(32.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
                ) { Text("✓", fontSize = 14.sp) }
                Button(
                    onClick = onSnooze,
                    modifier = Modifier.size(32.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF555555))
                ) { Text("z", fontSize = 12.sp) }
            }
        }
    }
}
