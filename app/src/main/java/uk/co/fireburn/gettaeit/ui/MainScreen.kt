package uk.co.fireburn.gettaeit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uk.co.fireburn.gettaeit.ui.navigation.Screen

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val isGoblinMode by viewModel.isGoblinMode.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val userRoutineTemplates by viewModel.userRoutineTemplates.collectAsState()
    val focusSession by viewModel.focusSession.collectAsState()
    var showQuickCapture by rememberSaveable { mutableStateOf(false) }
    val navItems = listOf(
        Screen.TaskList,
        Screen.KitchenDashboard,
        Screen.Shopping,
        Screen.Settings
    )

    if (isGoblinMode) {
        GoblinModeScreen(
            task = tasks.firstOrNull(),
            focusSession = focusSession,
            onComplete = viewModel::completeTask,
            onSnooze = viewModel::snoozeTask,
            onStartFocus = viewModel::startFocusSession,
            onStopFocus = viewModel::stopFocusSession,
            onExit = { viewModel.setGoblinMode(false) }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Smaller mic FAB
                SmallFloatingActionButton(
                    onClick = { navController.navigate("voice_add_task") },
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Add by voice",
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Primary add FAB
                FloatingActionButton(
                    onClick = { showQuickCapture = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Quick capture")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.TaskList.route,
            // Only pad the bottom to avoid double-padding the TopAppBar
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.TaskList.route) {
                TaskListScreen(
                    viewModel = viewModel, // Share ViewModel
                    onAddTaskClicked = { navController.navigate("add_task") },
                    onGoblinModeClicked = { viewModel.setGoblinMode(true) },
                    onWeeklyReviewClicked = { navController.navigate("weekly_review") }
                )
            }
            composable(Screen.KitchenDashboard.route) {
                KitchenDashboardScreen() // Uses its own specific ViewModel
            }
            composable(Screen.Shopping.route) { ShoppingScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen() // Uses its own specific ViewModel
            }
            composable("add_task") {
                AddTaskScreen(
                    viewModel = viewModel, // Share ViewModel
                    onTaskAdded = { navController.popBackStack() }
                )
            }
            composable("voice_add_task") {
                VoiceInputScreen(
                    viewModel = viewModel, // Share ViewModel
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("weekly_review") {
                WeeklyReviewScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showQuickCapture) {
        QuickCaptureDialog(
            onSave = {
                viewModel.quickCapture(it)
                showQuickCapture = false
            },
            onTemplate = {
                viewModel.addRoutineTemplate(it)
                showQuickCapture = false
            },
            userTemplates = userRoutineTemplates,
            onSaveTemplate = viewModel::saveUserRoutineTemplate,
            onPlanInstead = {
                showQuickCapture = false
                viewModel.cancelEdit()
                navController.navigate("add_task")
            },
            onDismiss = { showQuickCapture = false }
        )
    }
}
