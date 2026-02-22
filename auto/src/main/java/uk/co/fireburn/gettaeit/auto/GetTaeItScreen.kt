package uk.co.fireburn.gettaeit.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.Calendar

class GetTaeItScreen(
    carContext: CarContext,
    private val viewModel: AutoViewModel
) : Screen(carContext) {

    private var tasks: List<TaskEntity> = emptyList()

    init {
        lifecycleScope.launch {
            viewModel.tasks.collect { newTasks ->
                tasks = newTasks
                invalidate()
            }
        }
    }

    private fun contextualTitle(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "☀️ Morning briefing"
            hour < 17 -> "🕐 Afternoon tasks"
            else -> "🌙 Evening wrap-up"
        }
    }

    override fun onGetTemplate(): Template {
        val activeTasks = tasks.filter { !it.isCompleted }.take(6)

        val taskList = ItemList.Builder().apply {
            if (activeTasks.isEmpty()) {
                addItem(
                    Row.Builder()
                        .setTitle("Nae bother, all done! 🎉")
                        .build()
                )
            } else {
                activeTasks.forEach { task ->
                    val subtitleParts = mutableListOf<String>()
                    task.estimatedMinutes?.let { subtitleParts.add("~${it}min") }
                    if (task.dependencyIds.isNotEmpty()) subtitleParts.add("⚠ Has blockers")
                    val priority = when (task.priority) {
                        1 -> "🔴 Urgent"
                        2 -> "🟡 High"
                        else -> null
                    }
                    priority?.let { subtitleParts.add(0, it) }

                    val rowBuilder = Row.Builder()
                        .setTitle(task.title)
                        // Toggle to mark done from the car
                        .setToggle(
                            Toggle.Builder { isChecked ->
                                if (isChecked) viewModel.completeTask(task)
                            }.setChecked(false).build()
                        )

                    if (subtitleParts.isNotEmpty()) {
                        rowBuilder.addText(subtitleParts.joinToString(" · "))
                    }

                    addItem(rowBuilder.build())
                }
            }
        }.build()

        return ListTemplate.Builder()
            .setSingleList(taskList)
            .setTitle(contextualTitle())
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
