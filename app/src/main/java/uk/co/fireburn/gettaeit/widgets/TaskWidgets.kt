package uk.co.fireburn.gettaeit.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
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
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DataLayerEntryPoint::class.java
        )
        val now = System.currentTimeMillis()

        val tasks = entryPoint.taskRepository().getAllActiveToplevelTasks().first()
            .asSequence()
            .filter { it.context == contextFilter || it.context == TaskContext.ANY }
            // Same as the plan: snoozed tasks stay out of sight until their snooze ends.
            .filter { !it.isSnoozed || (it.snoozedUntil ?: Long.MAX_VALUE) <= now }
            .sortedWith(compareBy<TaskEntity> { it.priority }.thenBy { it.dueDate ?: Long.MAX_VALUE })
            .take(MAX_VISIBLE_TASKS)
            .toList()

        val prefs = entryPoint.userPreferencesRepository().getUserPreferences().first()

        provideContent {
            // Levels match the task list header: one every 100 XP.
            TaskWidgetContent(title, emptyMessage, background, tasks, prefs.xp / 100 + 1, prefs.dailySpoons)
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
    tasks: List<TaskEntity>,
    level: Int,
    spoons: Int
) {
    val white = ColorProvider(android.graphics.Color.WHITE)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = white, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Lvl $level • 🥄 $spoons",
                modifier = GlanceModifier.semantics { contentDescription = "Level $level, $spoons spoons" },
                style = TextStyle(color = white)
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text(text = emptyMessage, style = TextStyle(color = white))
        } else {
            tasks.forEach { task ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tapping the words opens the plan; only the tick marks it done, so a
                    // stray tap on the home screen can't finish something by accident.
                    Text(
                        text = task.title,
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(vertical = 6.dp)
                            .clickable(actionStartActivity<MainActivity>()),
                        maxLines = 1,
                        style = TextStyle(color = white)
                    )
                    Text(
                        text = "✓",
                        modifier = GlanceModifier
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clickable(
                                actionRunCallback<CompleteTaskWidgetAction>(
                                    actionParametersOf(TaskIdKey to task.id.toString())
                                )
                            )
                            .semantics { contentDescription = "Mark ${task.title} done" },
                        style = TextStyle(color = white, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    )
                }
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
        repository.getTaskById(taskId)?.takeUnless { it.isCompleted }?.let { task -> repository.completeTask(task) }
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
