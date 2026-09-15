package uk.co.fireburn.gettaeit.ui

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Whether reminders can reach the person at all, and whether they can arrive on time. */
data class ReminderAccess(val notifications: Boolean, val exactAlarms: Boolean)

fun Context.reminderAccess() = ReminderAccess(
    notifications = NotificationManagerCompat.from(this).areNotificationsEnabled(),
    exactAlarms = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
)

/** Both are switched in system settings, so look again whenever the app comes back. */
@Composable
fun rememberReminderAccess(): ReminderAccess {
    val context = LocalContext.current
    var access by remember { mutableStateOf(context.reminderAccess()) }
    LifecycleResumeEffect(context) {
        access = context.reminderAccess()
        onPauseOrDispose { }
    }
    return access
}

/**
 * Turns notifications on the least fiddly way available: the system prompt while Android
 * will still show it, otherwise this app's notification settings.
 */
@Composable
fun rememberNotificationEnabler(): () -> Unit {
    val context = LocalContext.current
    val prompt = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val activity = context.findActivity()
        // Denied twice already: Android answers "no" without asking, so go to settings instead.
        if (!granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            context.openNotificationSettings()
        }
    }
    return {
        val canPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (canPrompt) prompt.launch(Manifest.permission.POST_NOTIFICATIONS) else context.openNotificationSettings()
    }
}

fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    startActivity(
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Shown on the plan while notifications are off, until turned on or waved away. */
@Composable
fun ReminderAccessBanner(onDismiss: () -> Unit) {
    val enableNotifications = rememberNotificationEnabler()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text("Reminders can't reach you yet", fontWeight = FontWeight.SemiBold)
            Text(
                "Notifications are off for Get Tae It, so nudges stay silent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Not now") }
                Button(onClick = enableNotifications) { Text("Turn on") }
            }
        }
    }
}

/** The Gubbins rows for notifications and on-time delivery. */
@Composable
fun ColumnScope.ReminderAccessRows() {
    val context = LocalContext.current
    val access = rememberReminderAccess()
    val enableNotifications = rememberNotificationEnabler()

    AccessRow("Notifications", access.notifications, "Turn on", enableNotifications)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AccessRow("On-time reminders", access.exactAlarms, "Allow") { context.openExactAlarmSettings() }
        if (!access.exactAlarms) {
            Text(
                "Without this, Android can hold reminders back to save battery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccessRow(label: String, granted: Boolean, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (granted) {
            Text("On", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        } else {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
