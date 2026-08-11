package uk.co.fireburn.gettaeit.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import uk.co.fireburn.gettaeit.MainActivity
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.di.DataLayerEntryPoint

/**
 * A deliberately small widget: ADHD-friendly means showing the next few useful
 * things, rather than duplicating the full planner on the home screen.
 */
internal class TaskListWidget(
    private val contextFilter: TaskContext,
    private val title: String,
    private val emptyMessage: String,
    private val background: ColorProvider
) : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DataLayerEntryPoint::class.java
        ).taskRepository()

        val tasks = repository.getAllActiveToplevelTasks().first()
            .asSequence()
            .filter { it.context == contextFilter || it.context == TaskContext.ANY }
            .sortedWith(compareBy<TaskEntity> { it.priority }.thenBy { it.dueDate ?: Long.MAX_VALUE })
            .take(MAX_VISIBLE_TASKS)
            .toList()

        provideContent {
            TaskWidgetContent(title, emptyMessage, background, tasks)
        }
    }

    companion object {
        private const val MAX_VISIBLE_TASKS = 3
    }
}

@Composable
private fun TaskWidgetContent(
    title: String,
    emptyMessage: String,
    background: ColorProvider,
    tasks: List<TaskEntity>
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title,
            modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
            style = TextStyle(
                color = ColorProvider(android.graphics.Color.WHITE),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text(
                text = emptyMessage,
                style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE))
            )
        } else {
            tasks.forEach { task ->
                Text(
                    text = "• ${task.title}",
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(
                            actionRunCallback<CompleteTaskWidgetAction>(
                                actionParametersOf(TaskIdKey to task.id.toString())
                            )
                        )
                        .padding(vertical = 2.dp),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE))
                )
            }
        }
    }
}

private val TaskIdKey = ActionParameters.Key<String>("task_id")

/** Completes a task locally; repository sync and widget refresh follow as normal. */
class CompleteTaskWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey]?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() }
            ?: return
        val repository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DataLayerEntryPoint::class.java
        ).taskRepository()
        repository.getTaskById(taskId)?.let { task -> repository.completeTask(task) }
        refreshTaskWidgets(context)
    }
}

internal val workTasksWidget = TaskListWidget(
        contextFilter = TaskContext.WORK,
        title = "Get Tae It · Work",
        emptyMessage = "Cracking job, go hame.",
        background = ColorProvider(Color(0xFF1D4E89))
)

internal val personalTasksWidget = TaskListWidget(
        contextFilter = TaskContext.PERSONAL,
        title = "Get Tae It · Personal",
        emptyMessage = "Nae bother, chill out.",
        background = ColorProvider(Color(0xFF7C3E73))
)

/** Refresh installed widgets whenever the local task stream changes. */
suspend fun refreshTaskWidgets(context: Context) {
    workTasksWidget.updateAll(context)
    personalTasksWidget.updateAll(context)
}

class WorkTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = workTasksWidget
}

class PersonalTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = personalTasksWidget
}
