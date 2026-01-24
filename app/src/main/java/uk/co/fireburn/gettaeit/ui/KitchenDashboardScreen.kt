package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Calendar

@Composable
fun KitchenDashboardScreen(
    viewModel: KitchenViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("Dinner") }
    var description by remember { mutableStateOf("Slow Cooker Stew") }

    // Simple date/time entry for demonstration
    val calendar = Calendar.getInstance()
    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR).toString()) }
    var month by remember { mutableStateOf((calendar.get(Calendar.MONTH) + 1).toString()) }
    var day by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH).toString()) }
    var hour by remember { mutableStateOf("19") }
    var minute by remember { mutableStateOf("00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Schedule a Meal")
        Spacer(modifier = Modifier.height(16.dp))
        TextField(value = title, onValueChange = { title = it }, label = { Text("Meal Name") })
        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") })
        TextField(value = hour, onValueChange = { hour = it }, label = { Text("Hour (24h)") })
        TextField(value = minute, onValueChange = { minute = it }, label = { Text("Minute") })
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            viewModel.scheduleMeal(
                title = title,
                description = description,
                year = year.toInt(),
                month = month.toInt() - 1, // Calendar month is 0-indexed
                day = day.toInt(),
                hour = hour.toInt(),
                minute = minute.toInt()
            )
        }) {
            Text("Schedule Meal")
        }
    }
}
