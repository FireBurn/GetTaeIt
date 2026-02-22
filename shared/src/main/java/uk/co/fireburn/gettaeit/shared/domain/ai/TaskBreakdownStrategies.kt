package uk.co.fireburn.gettaeit.shared.domain.ai

import uk.co.fireburn.gettaeit.shared.data.TaskContext
import javax.inject.Inject

data class BreakdownResult(
    val title: String,
    val icon: String = "🔹",
    /** Suggested relative priority offset from parent (0 = same, -1 = more urgent) */
    val priorityOffset: Int = 0
)

data class ParsedTask(
    val title: String,
    val suggestedContext: TaskContext,
    val subtasks: List<BreakdownResult>
)

// ─── Strategy interface ───────────────────────────────────────────────────────

interface TaskBreakerStrategy {
    suspend fun isAvailable(): Boolean
    suspend fun generate(prompt: String): List<BreakdownResult>
}

// ─── Template strategy (offline, always available) ───────────────────────────

/**
 * Keyword-driven task breakdown. Good enough for common household/work tasks,
 * and the guaranteed fallback when Gemini Nano isn't available.
 */
class TemplateStrategy @Inject constructor() : TaskBreakerStrategy {

    override suspend fun isAvailable(): Boolean = true

    override suspend fun generate(prompt: String): List<BreakdownResult> {
        val p = prompt.lowercase()
        return when {
            // ── Household ─────────────────────────────────────────────────────
            "clean kitchen" in p || "tidy kitchen" in p -> listOf(
                BreakdownResult("Clear the worktops", "🧹"),
                BreakdownResult("Empty and reload dishwasher", "🍽️"),
                BreakdownResult("Wipe down surfaces and hob", "🧽"),
                BreakdownResult("Sweep or mop the floor", "🫧")
            )

            "clean" in p || "tidy" in p || "hoover" in p || "vacuum" in p -> listOf(
                BreakdownResult("Clear any clutter first", "📦"),
                BreakdownResult("Dust surfaces", "🪣"),
                BreakdownResult("Hoover carpets / mop floors", "🧹"),
                BreakdownResult("Put things back properly", "✅")
            )

            "laundry" in p || "washing" in p -> listOf(
                BreakdownResult("Sort clothes into piles", "👕"),
                BreakdownResult("Put a wash on", "🫧"),
                BreakdownResult("Hang or tumble-dry when done", "🏠"),
                BreakdownResult("Put clothes away", "📦")
            )

            "shopping" in p || "groceries" in p -> listOf(
                BreakdownResult("Check fridge and cupboards", "🔍"),
                BreakdownResult("Write the shopping list", "📝"),
                BreakdownResult("Grab bags and head out", "🛍️"),
                BreakdownResult("Put shopping away when back", "🏠")
            )

            "garden" in p || "gardening" in p -> listOf(
                BreakdownResult("Mow the lawn", "🌿"),
                BreakdownResult("Pull weeds", "🌱"),
                BreakdownResult("Water plants", "💧"),
                BreakdownResult("Tidy tools away", "🔧")
            )

            // ── Work ──────────────────────────────────────────────────────────
            "report" in p || "presentation" in p -> listOf(
                BreakdownResult("Gather data / source material", "📊", -1),
                BreakdownResult("Draft outline / structure", "📝"),
                BreakdownResult("Write first draft", "✍️"),
                BreakdownResult("Review and edit", "👀"),
                BreakdownResult("Final proofread and send", "📤")
            )

            "email" in p || "reply" in p || "respond" in p -> listOf(
                BreakdownResult("Re-read the original message", "📧", -1),
                BreakdownResult("Draft your reply", "✍️"),
                BreakdownResult("Check tone and send", "📤")
            )

            "meeting" in p -> listOf(
                BreakdownResult("Check the agenda", "📋", -1),
                BreakdownResult("Prep any notes or slides", "📝"),
                BreakdownResult("Join / arrive on time", "🗓️"),
                BreakdownResult("Write up action points after", "✅")
            )

            "budget" in p || "finance" in p || "invoice" in p -> listOf(
                BreakdownResult("Collect all receipts / data", "🧾", -1),
                BreakdownResult("Update the spreadsheet", "📊"),
                BreakdownResult("Review totals", "🔍"),
                BreakdownResult("Submit or file", "📤")
            )

            // ── Generic: split on "and" or commas ─────────────────────────────
            " and " in p -> prompt.split(" and ", ", ").map { BreakdownResult(it.trim()) }

            // ── Truly unknown: single task, no breakdown ──────────────────────
            else -> emptyList()
        }
    }

    /**
     * Guesses context from keywords. Used when the user hasn't explicitly chosen.
     */
    fun detectContext(prompt: String): TaskContext {
        val p = prompt.lowercase()
        val workWords = listOf(
            "report", "meeting", "email", "client", "presentation", "invoice",
            "budget", "deadline", "office", "colleague", "boss", "project", "sprint",
            "ticket", "pr", "pull request", "review", "deploy", "standup", "slack"
        )
        val personalWords = listOf(
            "clean", "laundry", "shopping", "garden", "dentist", "doctor",
            "gym", "cook", "dinner", "hoover", "vacuum", "tidy", "bins",
            "prescription", "kids", "school", "car", "MOT", "plumber"
        )
        val workScore = workWords.count { it in p }
        val personalScore = personalWords.count { it in p }
        return when {
            workScore > personalScore -> TaskContext.WORK
            personalScore > workScore -> TaskContext.PERSONAL
            else -> TaskContext.ANY
        }
    }
}

// ─── Gemini Nano strategy (on-device, no API key, Android 14+ Pixel/Samsung) ──

/**
 * Uses the Android AICore / Gemini Nano on-device model.
 * Falls back gracefully when hardware isn't supported.
 *
 * To fully enable: add "com.google.ai.edge.aicore:aicore:0.0.1-alpha02" to :shared deps
 * and implement isAvailable() via GenerativeAIRuntime.getAIFeatureStatus().
 */
class GeminiNanoStrategy @Inject constructor() : TaskBreakerStrategy {

    override suspend fun isAvailable(): Boolean {
        // TODO: check GenerativeAIRuntime.getAIFeatureStatus(context, AIFeature.TEXT_GENERATION)
        //       == AIFeatureStatus.AVAILABLE
        return false
    }

    override suspend fun generate(prompt: String): List<BreakdownResult> {
        // TODO: use GenerativeAIRuntime.getGenerativeModel() + sendMessage() with JSON prompt
        return emptyList()
    }
}

// ─── Hybrid service (Nano → Template) ────────────────────────────────────────

class HybridTaskService @Inject constructor(
    private val template: TemplateStrategy,
    private val nano: GeminiNanoStrategy
) {
    /**
     * Generates subtasks for a given prompt.
     * Returns an empty list if the task is atomic and shouldn't be split.
     */
    suspend fun generateSubtasks(prompt: String): List<BreakdownResult> =
        if (nano.isAvailable()) nano.generate(prompt) else template.generate(prompt)

    /** Best-guess context classification. */
    fun detectContext(prompt: String): TaskContext = template.detectContext(prompt)
}
