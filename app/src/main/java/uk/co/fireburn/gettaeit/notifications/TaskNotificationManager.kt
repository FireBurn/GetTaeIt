package uk.co.fireburn.gettaeit.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uk.co.fireburn.gettaeit.MainActivity

object TaskNotificationManager {

    const val CHANNEL_ID = "gettaeit_reminders"
    const val CHANNEL_NAME = "Task Reminders"
    const val CHANNEL_DESC = "Reminds you when tasks are due"

    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"
    const val EXTRA_SLOT_INDEX = "slot_index"   // which daily slot (0, 1, 2…) fired

    const val ACTION_FEENISHT = "uk.co.fireburn.gettaeit.ACTION_FEENISHT"   // done ✓
    const val ACTION_STICKIT = "uk.co.fireburn.gettaeit.ACTION_STICKIT"    // not done / skip ✗

    /** Call once at app startup (Application.onCreate). */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    /**
     * Show the reminder notification for [taskId] / [taskTitle].
     * Includes "Feenisht ✓" and "Stickit ✗" action buttons.
     */
    fun showReminder(context: Context, taskId: String, taskTitle: String, slotIndex: Int) {
        val notifId = notificationId(taskId, slotIndex)

        // Tap notification → open app
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Feenisht ✓" action
        val feenisht = actionPendingIntent(context, ACTION_FEENISHT, taskId, slotIndex, notifId)

        // "Stickit ✗" action (dismiss / skip this slot)
        val stickit = actionPendingIntent(context, ACTION_STICKIT, taskId, slotIndex, notifId)

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(taskTitle)
            .setContentText("Time tae get tae it! 👀")
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Feenisht ✓",
                feenisht
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Stickit ✗",
                stickit
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted — silently skip
        }
    }

    fun cancelNotification(context: Context, taskId: String, slotIndex: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId(taskId, slotIndex))
    }

    fun cancelAllForTask(context: Context, taskId: String, timesPerDay: Int) {
        val nm = NotificationManagerCompat.from(context)
        repeat(timesPerDay) { slot -> nm.cancel(notificationId(taskId, slot)) }
    }

    private fun actionPendingIntent(
        context: Context,
        action: String,
        taskId: String,
        slotIndex: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, TaskActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Stable notification ID: hash of taskId + slot so each slot gets its own notif. */
    fun notificationId(taskId: String, slotIndex: Int): Int =
        (taskId + slotIndex).hashCode() and 0x7FFFFFFF
}
