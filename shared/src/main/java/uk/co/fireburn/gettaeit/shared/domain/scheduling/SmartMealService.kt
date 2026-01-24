package uk.co.fireburn.gettaeit.shared.domain.scheduling

import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class RecipeType {
    SLOW_COOKER_STEW,
    ROAST_CHICKEN
}

@Singleton
class SmartMealService @Inject constructor() {

    fun generatePrepTasks(mainEvent: TaskEntity): List<TaskEntity> {
        val recipeType = RecipeType.SLOW_COOKER_STEW // This would be dynamic in a real app
        if (mainEvent.dueDate == null) return emptyList()

        return when (recipeType) {
            RecipeType.SLOW_COOKER_STEW -> {
                listOf(
                    TaskEntity(
                        title = "Take meat out of freezer",
                        description = "For ${mainEvent.title}",
                        userId = mainEvent.userId,
                        context = mainEvent.context,
                        offsetReferenceId = mainEvent.id,
                        offsetDuration = TimeUnit.HOURS.toMillis(8), // 8 hours before
                        dueDate = mainEvent.dueDate - TimeUnit.HOURS.toMillis(8),
                        locationTrigger = null,
                        wifiTrigger = null
                    ),
                    TaskEntity(
                        title = "Chop veg & turn on Slow Cooker",
                        description = "For ${mainEvent.title}",
                        userId = mainEvent.userId,
                        context = mainEvent.context,
                        offsetReferenceId = mainEvent.id,
                        offsetDuration = TimeUnit.HOURS.toMillis(4), // 4 hours before
                        dueDate = mainEvent.dueDate - TimeUnit.HOURS.toMillis(4),
                        locationTrigger = null,
                        wifiTrigger = null
                    ),
                    TaskEntity(
                        title = "Put leftovers in fridge",
                        description = "From ${mainEvent.title}",
                        userId = mainEvent.userId,
                        context = mainEvent.context,
                        offsetReferenceId = mainEvent.id,
                        offsetDuration = -TimeUnit.HOURS.toMillis(2), // 2 hours after
                        dueDate = mainEvent.dueDate + TimeUnit.HOURS.toMillis(2),
                        locationTrigger = null,
                        wifiTrigger = null
                    )
                )
            }
            // Other recipes would go here
            else -> emptyList()
        }
    }
}
