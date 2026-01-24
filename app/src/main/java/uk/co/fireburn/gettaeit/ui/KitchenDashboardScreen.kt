package uk.co.fireburn.gettaeit.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class QuickRecipe(
    val id: String,
    val title: String,
    val description: String
)

private val quickRecipes = listOf(
    QuickRecipe("stew", "Slow Cooker Stew", "A hearty, slow-cooked beef stew."),
    QuickRecipe("roast", "Roast Chicken", "Classic Sunday roast chicken with all the trimmings."),
    QuickRecipe("pasta", "Pasta Bake", "Cheesy and delicious pasta bake.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenDashboardScreen(
    viewModel: KitchenViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedRecipeId by remember { mutableStateOf("") }
    val calendar by remember { mutableStateOf(Calendar.getInstance()) }
    var formattedTime by remember {
        mutableStateOf(
            SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(calendar.time)
        )
    }
    val context = LocalContext.current

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            formattedTime =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true // 24-hour view
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Schedule a Meal")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Quick Recipes")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickRecipes) { recipe ->
                FilterChip(
                    selected = selectedRecipeId == recipe.id,
                    onClick = {
                        selectedRecipeId = recipe.id
                        title = recipe.title
                        description = recipe.description
                    },
                    label = { Text(recipe.title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(value = title, onValueChange = { title = it }, label = { Text("Meal Name") })
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") })

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Eating at: $formattedTime")
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { timePickerDialog.show() }) {
                Text("Pick Time")
            }
        }


        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            viewModel.scheduleMeal(
                title = title,
                description = description,
                timestamp = calendar.timeInMillis
            )
        }, enabled = title.isNotBlank() && description.isNotBlank()) {
            Text("Schedule Meal")
        }
    }
}
