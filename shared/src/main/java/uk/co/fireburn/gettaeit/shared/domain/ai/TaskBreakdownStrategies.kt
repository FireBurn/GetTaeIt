package uk.co.fireburn.gettaeit.shared.domain.ai

import javax.inject.Inject

data class BreakdownResult(val title: String, val icon: String = "🔹")

interface TaskBreakerStrategy {
    suspend fun isAvailable(): Boolean
    suspend fun generate(prompt: String): List<BreakdownResult>
}

class TemplateStrategy @Inject constructor() : TaskBreakerStrategy {
    override suspend fun isAvailable(): Boolean = true

    override suspend fun generate(prompt: String): List<BreakdownResult> {
        val lowerCasePrompt = prompt.lowercase()
        return when {
            "kitchen" in lowerCasePrompt || "clean" in lowerCasePrompt -> listOf(
                BreakdownResult("Empty Dishwasher"),
                BreakdownResult("Wipe Surfaces"),
                BreakdownResult("Sweep Floor")
            )

            "shopping" in lowerCasePrompt -> listOf(
                BreakdownResult("Check Fridge"),
                BreakdownResult("Write List"),
                BreakdownResult("Grab Bags")
            )

            else -> {
                prompt.split(" and ", ",").map { BreakdownResult(it.trim()) }
            }
        }
    }
}

class GeminiNanoStrategy @Inject constructor() : TaskBreakerStrategy {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun generate(prompt: String): List<BreakdownResult> = emptyList()
}

class HybridTaskService @Inject constructor(
    val template: TemplateStrategy,
    val nano: GeminiNanoStrategy
) {
    suspend fun generateSubtasks(prompt: String): List<BreakdownResult> {
        return if (nano.isAvailable()) {
            nano.generate(prompt)
        } else {
            template.generate(prompt)
        }
    }
}
