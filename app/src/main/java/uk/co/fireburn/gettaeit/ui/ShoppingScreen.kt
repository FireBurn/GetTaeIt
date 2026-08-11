package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import uk.co.fireburn.gettaeit.shared.data.ShoppingItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(viewModel: ShoppingViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Other") }
    var supermarket by rememberSaveable { mutableStateOf("") }
    val categories = listOf("Fruit & veg", "Chilled", "Cupboard", "Household", "Other")

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Shopping") }, actions = {
            if (items.any { it.isBought }) TextButton(onClick = viewModel::clearBought) { Text("Clear bought") }
        })
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Add an item") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { value -> FilterChip(selected = category == value, onClick = { category = value }, label = { Text(value) }) }
            }
            OutlinedTextField(supermarket, { supermarket = it }, Modifier.fillMaxWidth(), label = { Text("Supermarket (optional)") }, singleLine = true)
            Button(onClick = { viewModel.add(title, category, supermarket); title = "" }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Add to list") }
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
