package uk.co.fireburn.gettaeit.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import uk.co.fireburn.gettaeit.R
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import uk.co.fireburn.gettaeit.shared.data.ShoppingItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(viewModel: ShoppingViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Other") }
    var supermarket by rememberSaveable { mutableStateOf("") }
    val categories = listOf("Fruit & veg", "Chilled", "Cupboard", "Household", "Other")
    val canDictate = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    // The system recogniser handles the microphone itself, so no RECORD_AUDIO prompt here.
    // Dictation only fills the box: the category and shop are still one tap away.
    val dictation = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { spoken -> title = spoken.replaceFirstChar { it.uppercase() } }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.coo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(50))
                )
                Text("Shopping")
            }
        }, actions = {
            if (items.any { it.isBought }) TextButton(onClick = viewModel::clearBought) { Text("Clear bought") }
        })
        Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Add an item") },
                        singleLine = true
                    )
                    if (canDictate) {
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "What do ye need?")
                                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                            }
                            try {
                                dictation.launch(intent)
                            } catch (_: ActivityNotFoundException) {
                                // Recogniser vanished since we checked; typing still works.
                            }
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = "Say an item")
                        }
                    }
                }

                Text("Pick an aisle so similar things stay together and you're no' doubling back.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { value -> FilterChip(selected = category == value, onClick = { category = value }, label = { Text(value) }) }
                }
                OutlinedTextField(supermarket, { supermarket = it }, Modifier.fillMaxWidth(), label = { Text("Supermarket (optional)") }, singleLine = true)
                Button(onClick = { viewModel.add(title, category, supermarket); title = "" }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Add to list") }
            }
        }
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Add the things before they vanish from your brain.") }
        } else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(items, key = { it.id }) { item -> ShoppingRow(item, viewModel::toggle, viewModel::delete) }
        }
    }
}

@Composable private fun ShoppingRow(item: ShoppingItemEntity, onToggle: (ShoppingItemEntity) -> Unit, onDelete: (ShoppingItemEntity) -> Unit) {
    Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onToggle(item) }) { Icon(if (item.isBought) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, if (item.isBought) "Mark unbought" else "Mark bought") }
        Column(Modifier.weight(1f)) {
            Text(item.title, textDecoration = if (item.isBought) TextDecoration.LineThrough else null)
            Text(listOf(item.category, item.supermarket).filterNotNull().joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { onDelete(item) }) { Text("Remove") }
    } }
}
