package uk.co.fireburn.gettaeit.shared.domain.scheduling

import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class RecipeType {
    SLOW_COOKER_STEW,
    ROAST_CHICKEN,
    PASTA_BAKE
}

@Singleton
class SmartMealService @Inject constructor() {

    fun generatePrepTasks(
        mainEvent: TaskEntity,
        recipeType: RecipeType = RecipeType.SLOW_COOKER_STEW
    ): List<TaskEntity> {
        if (mainEvent.dueDate == null) return emptyList()

        return when (recipeType) {
            RecipeType.SLOW_COOKER_STEW -> listOf(
                TaskEntity(
                    title = "Take meat out of freezer",
                    description = "For ${mainEvent.title}",
                    context = mainEvent.context,
                    parentId = mainEvent.id,
                    isSubtask = true,
                    offsetReferenceId = mainEvent.id,
                    offsetDuration = TimeUnit.HOURS.toMillis(8),
                    dueDate = mainEvent.dueDate - TimeUnit.HOURS.toMillis(8)
                ),
                TaskEntity(
                    title = "Chop veg & start Slow Cooker",
                    description = "For ${mainEvent.title}",
                    context = mainEvent.context,
                    parentId = mainEvent.id,
                    isSubtask = true,
                    offsetReferenceId = mainEvent.id,
                    offsetDuration = TimeUnit.HOURS.toMillis(4),
                    dueDate = mainEvent.dueDate - TimeUnit.HOURS.toMillis(4)
                ),
                TaskEntity(
                    title = "Put leftovers in fridge",
                    description = "From ${mainEvent.title}",
                    context = mainEvent.context,
                    parentId = mainEvent.id,
                    isSubtask = true,
                    offsetReferenceId = mainEvent.id,
                    offsetDuration = -TimeUnit.HOURS.toMillis(2),
                    dueDate = mainEvent.dueDate + TimeUnit.HOURS.toMillis(2)
                )
            )

            RecipeType.ROAST_CHICKEN -> listOf(
                TaskEntity(
                    title = "Take chicken out of fridge",
                    description = "For ${mainEvent.title} — let it come to room temp",
                    context = mainEvent.context,
                    parentId = mainEvent.id,
                    isSubtask = true,
                    offsetReferenceId = mainEvent.id,
                    offsetDuration = TimeUnit.HOURS.toMillis(1),
                    dueDate = mainEvent.dueDate - TimeUnit.HOURS.toMillis(1)
                ),
                TaskEntity(
                    title = "Preheat oven & prep chicken",
                    description = "For ${mainEvent.title}",
                    context = mainEvent.context,
                    parentId = mainEvent.id,
                    isSubtask = true,
                    offsetReferenceId = mainEvent.id,
                    offsetDuration = TimeUnit.MINUTES.toMillis(90),
                    dueDate = mainEvent.dueDate - TimeUnit.MINUTES.toMillis(90)
                )
            )

            RecipeType.PASTA_BAKE -> listOf(
                TaskEntity(
                    title = "Boil pasta & make sauce",
                    description = "For ${mainEvent.title}",
                    context = mainEvent.context,
                    parentId = mainEvent.id,
                    isSubtask = true,
                    offsetReferenceId = mainEvent.id,
                    offsetDuration = TimeUnit.MINUTES.toMillis(45),
                    dueDate = mainEvent.dueDate - TimeUnit.MINUTES.toMillis(45)
                )
            )
        }
    }
}
