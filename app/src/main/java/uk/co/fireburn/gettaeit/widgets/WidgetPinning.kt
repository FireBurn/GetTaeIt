package uk.co.fireburn.gettaeit.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import uk.co.fireburn.gettaeit.shared.data.TaskContext

/** Whether this launcher lets an app offer to place a widget, rather than making people hunt for it. */
fun canPinTaskWidgets(context: Context): Boolean =
    AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

/** Asks the launcher to add the Work or Personal widget; the person confirms in its own dialog. */
fun requestPinTaskWidget(context: Context, taskContext: TaskContext): Boolean {
    val receiver = if (taskContext == TaskContext.WORK) {
        WorkTasksWidgetReceiver::class.java
    } else {
        PersonalTasksWidgetReceiver::class.java
    }
    val manager = AppWidgetManager.getInstance(context)
    return manager.isRequestPinAppWidgetSupported &&
        manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
}
