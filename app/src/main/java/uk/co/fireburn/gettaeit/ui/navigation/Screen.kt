package uk.co.fireburn.gettaeit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object TaskList : Screen("tasks", "The List", Icons.Filled.Checklist)
    object KitchenDashboard : Screen("kitchen", "Scran", Icons.Filled.Kitchen)
    object Settings : Screen("settings", "Gubbins", Icons.Filled.Settings)
}
