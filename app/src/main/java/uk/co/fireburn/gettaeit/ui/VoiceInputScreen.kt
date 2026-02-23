package uk.co.fireburn.gettaeit.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.ui.theme.ThistlePurple
import java.util.Calendar

/**
 * Quick scheduling option shown in the "When?" bottom sheet after voice recognition.
 */
private sealed class WhenOption(val label: String) {
    object Now : WhenOption("Right now")
    object ThisEvening : WhenOption("This evening (6pm)")
    object Tomorrow : WhenOption("Tomorrow 9am")
    object ThisWeek : WhenOption("This week")
    object NoDate : WhenOption("No date")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun VoiceInputScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var editableText by remember { mutableStateOf("") }
    var hasResult by remember { mutableStateOf(false) }
    val isParsing by viewModel.isParsingVoice.collectAsState()
    rememberCoroutineScope()

    // Shared pending voice text — set once speech finishes, read by both sheets
    var pendingVoiceText by remember { mutableStateOf("") }

    // AI schedule suggestion from Gemini Nano / template
    val aiScheduleSuggestion by viewModel.voiceScheduleSuggestion.collectAsState()

    // Which sheet to show — only one is ever visible at a time
    var showScheduleSheet by remember { mutableStateOf(false) }  // "How often?" (recurring)
    var showWhenSheet by remember { mutableStateOf(false) }      // "When?" (one-off)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Set true when we fire a parse; prevents the LaunchedEffect routing to a
    // sheet spuriously on first composition (when isParsing is already false).
    var awaitingScheduleRoute by remember { mutableStateOf(false) }

    // aiScheduleSuggestion is written before isParsing flips to false in the
    // ViewModel, so by the time this fires both values are stable.
    LaunchedEffect(isParsing, aiScheduleSuggestion) {
        if (awaitingScheduleRoute && !isParsing) {
            awaitingScheduleRoute = false
            if (aiScheduleSuggestion != null) {
                showScheduleSheet = true
            } else {
                showWhenSheet = true
            }
        }
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    fun startListening() {
        recognizedText = ""
        editableText = ""
        hasResult = false
        speechRecognizer.startListening(recognizerIntent)
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    recognizedText = matches[0]
                    editableText = matches[0]
                    hasResult = true
                    pendingVoiceText = matches[0]
                    awaitingScheduleRoute = true
                    viewModel.parseVoiceForSchedule(matches[0])
                }
                isListening = false
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty()) editableText = partial[0]
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose { speechRecognizer.destroy() }
    }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) startListening()
        else permissionState.launchPermissionRequest()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700)), label = "scale"
    )

    // "When?" sheet — only shown for tasks Gemini Nano did NOT flag as recurring
    if (showWhenSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWhenSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "When do you want to do it?",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "\"$pendingVoiceText\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val whenOptions = listOf(
                    WhenOption.Now,
                    WhenOption.ThisEvening,
                    WhenOption.Tomorrow,
                    WhenOption.ThisWeek,
                    WhenOption.NoDate
                )

                whenOptions.forEach { option ->
                    Button(
                        onClick = {
                            showWhenSheet = false
                            val dueMs = when (option) {
                                WhenOption.Now -> System.currentTimeMillis()
                                WhenOption.ThisEvening -> Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 18)
                                    set(Calendar.MINUTE, 0)
                                }.timeInMillis

                                WhenOption.Tomorrow -> Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 9)
                                    set(Calendar.MINUTE, 0)
                                }.timeInMillis

                                WhenOption.ThisWeek -> Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 3)
                                    set(Calendar.HOUR_OF_DAY, 9)
                                    set(Calendar.MINUTE, 0)
                                }.timeInMillis

                                WhenOption.NoDate -> null
                            }
                            viewModel.addTasksFromVoice(
                                pendingVoiceText,
                                dueMs
                            ) { onNavigateBack() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (option == WhenOption.Now)
                                ThistlePurple else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (option == WhenOption.Now)
                                androidx.compose.ui.graphics.Color.White
                            else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(option.label)
                    }
                }
            }
        }
    }

    // "How often?" sheet — only shown when Gemini Nano detects a recurring task
    if (showScheduleSheet && aiScheduleSuggestion != null) {
        val suggestion = aiScheduleSuggestion!!
        val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        fun minsToTime(mins: Int): String {
            val h = mins / 60
            val m = mins % 60
            val suffix = if (h < 12) "am" else "pm"
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            return if (m == 0) "$h12$suffix" else "$h12:${m.toString().padStart(2, '0')}$suffix"
        }

        fun humanReadable(cfg: RecurrenceConfig): String = when (cfg.type) {
            RecurrenceType.DAILY -> when {
                cfg.dailySlotMinutes.size >= 2 ->
                    "Every day at ${
                        cfg.dailySlotMinutes.sorted().joinToString(", ") { minsToTime(it) }
                    }"

                cfg.timesPerDay >= 2 -> "Every day, ${cfg.timesPerDay} times a day"
                cfg.interval == 1 -> "Every day"
                else -> "Every ${cfg.interval} days"
            }

            RecurrenceType.WEEKLY -> if (cfg.interval == 1) "Every week" else "Every ${cfg.interval} weeks"
            RecurrenceType.MONTHLY -> if (cfg.interval == 1) "Every month" else "Every ${cfg.interval} months"
            RecurrenceType.CUSTOM_DAYS -> "On specific days"
            RecurrenceType.NONE -> "Just once"
        }

        ModalBottomSheet(
            onDismissRequest = {
                showScheduleSheet = false
                viewModel.clearVoiceScheduleSuggestion()
                viewModel.addTasksFromVoice(
                    pendingVoiceText,
                    recurrenceOverride = RecurrenceConfig(type = RecurrenceType.NONE)
                ) { onNavigateBack() }
            },
            sheetState = scheduleSheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "🤖 Gemini reckons this repeats",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "\"$pendingVoiceText\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Suggested schedule: ${humanReadable(suggestion)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThistlePurple
                )

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        showScheduleSheet = false
                        viewModel.clearVoiceScheduleSuggestion()
                        viewModel.addTasksFromVoice(
                            pendingVoiceText,
                            recurrenceOverride = suggestion
                        ) { onNavigateBack() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ThistlePurple)
                ) {
                    Text("✅  Aye, ${humanReadable(suggestion).lowercase()}")
                }

                Button(
                    onClick = {
                        showScheduleSheet = false
                        viewModel.clearVoiceScheduleSuggestion()
                        // Fall through to "When?" for a one-off date
                        showWhenSheet = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Naw, just the once")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blether at it") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isParsing -> {
                    CircularProgressIndicator(
                        color = ThistlePurple,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "AI's working on it...", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (editableText.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "\"$editableText\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                isListening -> {
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .size(96.dp)
                            .background(ThistlePurple.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(ThistlePurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mic, null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Listening...",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThistlePurple
                    )
                    if (editableText.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            editableText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Say your tasks out loud...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                hasResult -> {
                    Text(
                        "Got it! Ye said:", style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editableText, onValueChange = { editableText = it },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        label = { Text("Edit if needed") }
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            pendingVoiceText = editableText
                            viewModel.clearVoiceScheduleSuggestion()
                            awaitingScheduleRoute = true
                            viewModel.parseVoiceForSchedule(editableText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThistlePurple)
                    ) { Text("Schedule Task") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { startListening() }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Try again")
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MicNone, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (permissionState.status.isGranted) startListening()
                            else permissionState.launchPermissionRequest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThistlePurple)
                    ) { Text("Tap to speak") }
                }
            }
        }
    }
}
