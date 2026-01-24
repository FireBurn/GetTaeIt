package uk.co.fireburn.gettaeit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.scheduling.SmartMealService
import javax.inject.Inject

@HiltViewModel
class KitchenViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val smartMealService: SmartMealService
) : ViewModel() {

    fun scheduleMeal(
        title: String,
        description: String?,
        timestamp: Long
    ) {
        viewModelScope.launch {
            val mainMealEvent = TaskEntity(
                title = title,
                description = description,
                dueDate = timestamp,
                locationTrigger = null,
                wifiTrigger = null,
                offsetReferenceId = null,
                offsetDuration = null
                // Other fields will be set to defaults
            )
            taskRepository.addTask(mainMealEvent)

            val prepTasks = smartMealService.generatePrepTasks(mainMealEvent)
            prepTasks.forEach { task ->
                taskRepository.addTask(task)
            }
        }
    }
}
