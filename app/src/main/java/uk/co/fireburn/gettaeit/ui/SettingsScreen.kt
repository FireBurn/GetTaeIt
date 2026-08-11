package uk.co.fireburn.gettaeit.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.auth.GoogleSignInHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var workSsidInput by remember(prefs.workSsid) { mutableStateOf(prefs.workSsid ?: "") }

    // Work hours state
    var startHour by remember(prefs.workSchedule.startHour) {
        mutableStateOf(prefs.workSchedule.startHour.toFloat())
    }
    var endHour by remember(prefs.workSchedule.endHour) {
        mutableStateOf(prefs.workSchedule.endHour.toFloat())
    }

    val dayLabels = listOf(
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat",
        Calendar.SUNDAY to "Sun"
    )

    // On Android 11+, Background Location must be requested separately from Foreground
    val foregroundLocationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val bgLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Gubbins",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        SettingsCard(
            title = "🏖️ Vacation Mode",
            subtitle = "Keep work tasks out of sight, even at the office"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (prefs.isVacationMode) "Aye, protect your time." else "Off — work context can switch on normally.",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = prefs.isVacationMode,
                    onCheckedChange = viewModel::setVacationMode
                )
            }
        }

        // ── Work schedule ────────────────────────────────────────────────────
        SettingsCard(title = "⏰ Work Hours", subtitle = "When should Work Mode kick in?") {
            val sched = prefs.workSchedule

            Text(
                "Start: ${startHour.toInt()}:00 → End: ${endHour.toInt()}:00",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text("Start hour", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = startHour,
                onValueChange = { startHour = it },
                onValueChangeFinished = {
                    viewModel.setWorkHours(startHour.toInt(), endHour.toInt())
                },
                valueRange = 0f..23f,
                steps = 22
            )

            Text("End hour", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = endHour,
                onValueChange = { if (it > startHour) endHour = it },
                onValueChangeFinished = {
                    viewModel.setWorkHours(startHour.toInt(), endHour.toInt())
                },
                valueRange = 0f..23f,
                steps = 22
            )

            Text("Working days", style = MaterialTheme.typography.labelSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                dayLabels.forEach { (calDay, label) ->
                    val selected = sched.workingDays.contains(calDay)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val newDays = if (selected)
                                sched.workingDays - calDay
                            else
                                sched.workingDays + calDay
                            viewModel.setWorkingDays(newDays.sorted())
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Work location ────────────────────────────────────────────────────
        SettingsCard(
            title = "📍 Work Location",
            subtitle = "Auto-switch to Work Mode when you arrive"
        ) {
            if (prefs.workLocationString != null) {
                val (lat, lng) = prefs.workLocationLatLng!!
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Set: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}",
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearWorkLocation() }) { Text("Clear") }
                }
            } else {
                Text(
                    "No work location set.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    when {
                        foregroundLocationPermissions.allPermissionsGranted ->
                            viewModel.captureCurrentLocationAsWork()

                        else -> foregroundLocationPermissions.launchMultiplePermissionRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.MyLocation, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (prefs.workLocationString == null)
                        "Use Current Location as Work"
                    else
                        "Update Work Location"
                )
            }

            if (!foregroundLocationPermissions.allPermissionsGranted) {
                Text(
                    "📍 Grant location permission to enable auto work-mode switching.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Warning for missing background location
            if (foregroundLocationPermissions.allPermissionsGranted && bgLocationPermission?.status?.isGranted == false) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Geofencing requires 'Allow all the time' location access to detect when you arrive at work in the background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { bgLocationPermission.launchPermissionRequest() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Fix Background Permission")
                        }
                    }
                }
            }
        }

        // ── Work WiFi ────────────────────────────────────────────────────────
        SettingsCard(
            title = "📶 Work WiFi",
            subtitle = "Switch to Work Mode when connected to your office network"
        ) {
            OutlinedTextField(
                value = workSsidInput,
                onValueChange = { workSsidInput = it },
                label = { Text("Office WiFi network name (SSID)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (workSsidInput != (prefs.workSsid ?: "")) {
                        TextButton(onClick = { viewModel.setWorkSsid(workSsidInput) }) {
                            Text("Save")
                        }
                    }
                }
            )
            if (prefs.workSsid != null) {
                Text(
                    "Active: \"${prefs.workSsid}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Note: on Android 8+, SSID detection requires Location permission to be granted.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ── Permissions summary ──────────────────────────────────────────────
        SettingsCard(title = "🔐 Permissions", subtitle = null) {
            val perms = listOf(
                "Location (Fine)" to foregroundLocationPermissions.allPermissionsGranted,
                "Background Location" to (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                        bgLocationPermission?.status?.isGranted == true)
            )
            perms.forEach { (label, granted) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (granted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        null,
                        tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (!foregroundLocationPermissions.allPermissionsGranted || bgLocationPermission?.status?.isGranted == false) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open App Settings") }
            }
        }

        // ── Backup & Sync ────────────────────────────────────────────────────
        SettingsCard(title = "☁️ Backup & Sync", subtitle = null) {
            val currentUser by authViewModel.currentUser.collectAsState()
            val isSigningIn by authViewModel.isSigningIn.collectAsState()
            val signInError by authViewModel.signInError.collectAsState()
            val coroutineScope = rememberCoroutineScope()

            when {
                !authViewModel.isConfigured -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CloudOff, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Not set up on this build yet — your tasks stay on this device only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                currentUser != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.CloudDone, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                currentUser?.displayName ?: currentUser?.email ?: "Signed in",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Backed up automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = { authViewModel.signOut() }) { Text("Sign out") }
                }

                else -> {
                    Text(
                        "Sign in so a lost or reset phone doesn't cost you your list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            authViewModel.onSignInStarting()
                            coroutineScope.launch {
                                val token = GoogleSignInHelper.requestIdToken(context)
                                authViewModel.onGoogleIdToken(token)
                            }
                        },
                        enabled = !isSigningIn,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sign in with Google")
                        }
                    }
                    signInError?.let { err ->
                        Text(
                            err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // ── About ────────────────────────────────────────────────────────────
        SettingsCard(title = "🏴󠁧󠁢󠁳󠁣󠁴󠁿 Get Tae It", subtitle = null) {
            Text(
                "Version 1.0.0 · fireburn.co.uk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Built for braw folk wi' ADHD wha just want tae get stuff done.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}
