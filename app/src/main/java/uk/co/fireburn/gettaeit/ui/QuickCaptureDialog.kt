package uk.co.fireburn.gettaeit.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import uk.co.fireburn.gettaeit.shared.domain.RoutineTemplate
import uk.co.fireburn.gettaeit.shared.domain.RoutineTemplates

@Composable
fun QuickCaptureDialog(
    onSave: (String) -> Unit,
    onTemplate: (RoutineTemplate) -> Unit,
    userTemplates: List<RoutineTemplate>,
    onSaveTemplate: (String, List<String>) -> Unit,
    onPlanInstead: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var templateName by remember { mutableStateOf("") }
    var templateSteps by remember { mutableStateOf("") }
    var creatingTemplate by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catch it before it scarpers") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What's on your mind?") },
                    singleLine = false,
                    minLines = 2
                )
                Text("Or start with a wee routine")
                (RoutineTemplates.all + userTemplates).chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { template ->
                            AssistChip(
                                onClick = { onTemplate(template) },
                                label = { Text(template.label) }
                            )
                        }
                    }
                }
                TextButton(onClick = { creatingTemplate = !creatingTemplate }) {
                    Text(if (creatingTemplate) "Cancel new routine" else "Create a routine")
                }
                if (creatingTemplate) {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Routine name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = templateSteps,
                        onValueChange = { templateSteps = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Steps — one per line") },
                        minLines = 3
                    )
                    TextButton(
                        enabled = templateName.isNotBlank() && templateSteps.lines().any(String::isNotBlank),
                        onClick = {
                            onSaveTemplate(templateName, templateSteps.lines())
                            templateName = ""
                            templateSteps = ""
                            creatingTemplate = false
                        }
                    ) { Text("Save routine") }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    onSave(text)
                    focusManager.clearFocus()
                }
            ) { Text("Saved. Sorted.") }
        },
        dismissButton = { TextButton(onClick = onPlanInstead) { Text("Plan it properly") } }
    )
}
