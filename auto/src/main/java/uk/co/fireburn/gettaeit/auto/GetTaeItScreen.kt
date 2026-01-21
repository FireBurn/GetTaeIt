package uk.co.fireburn.gettaeit.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.data.TaskEntity

class GetTaeItScreen(
    carContext: CarContext,
    private val viewModel: AutoViewModel
) : Screen(carContext) {

    private var tasks: List<TaskEntity> = emptyList()

    init {
        lifecycleScope.launch {
            viewModel.tasks.collect { newTasks ->
                tasks = newTasks
                invalidate() // This tells the car screen to refresh
            }
        }
    }

    override fun onGetTemplate(): Template {
        val taskList = ItemList.Builder().apply {
            tasks.filter { !it.isCompleted }.take(3).forEach { task ->
                addItem(
                    Row.Builder()
                        .setTitle(task.title)
                        .build()
                )
            }
        }.build()

        return ListTemplate.Builder()
            .setSingleList(taskList)
            .setTitle("Your Morning Briefing")
            .setHeaderAction(androidx.car.app.model.Action.APP_ICON)
            .build()
    }
}
