package uk.co.fireburn.gettaeit.shared.domain.ai

import uk.co.fireburn.gettaeit.shared.data.MissedBehaviour
import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import javax.inject.Inject

data class BreakdownResult(
    val title: String,
    val icon: String = "🔹",
    val priorityOffset: Int = 0,
    val estimatedMinutes: Int? = null
)

/**
 * A fully parsed task suggestion from a voice or text prompt.
 * May expand into multiple top-level tasks (e.g. "brush teeth" → morning + evening).
 */
data class ParsedTask(
    val title: String,
    val suggestedContext: TaskContext,
    val subtasks: List<BreakdownResult> = emptyList(),
    /** AI-suggested recurrence. Null = one-off. */
    val suggestedRecurrence: RecurrenceConfig? = null,
    /**
     * Minutes from midnight for the preferred start time of this task instance.
     * e.g. 480 = 08:00, 1200 = 20:00
     */
    val preferredTimeMinutes: Int? = null,
    val estimatedMinutes: Int? = null
)

// ─── Strategy interface ───────────────────────────────────────────────────────

interface TaskBreakerStrategy {
    suspend fun isAvailable(): Boolean
    suspend fun generate(prompt: String): List<BreakdownResult>
}

// ─── Template strategy (offline, always available) ───────────────────────────

class TemplateStrategy @Inject constructor() : TaskBreakerStrategy {

    override suspend fun isAvailable(): Boolean = true

    // ── Subtask breakdown (called for manual add / AI breakdown button) ───────

    override suspend fun generate(prompt: String): List<BreakdownResult> {
        val p = prompt.lowercase()
        return when {
            "clean kitchen" in p || "tidy kitchen" in p -> listOf(
                BreakdownResult("Clear the worktops", "🧹", estimatedMinutes = 5),
                BreakdownResult("Empty and reload dishwasher", "🍽️", estimatedMinutes = 10),
                BreakdownResult("Wipe down surfaces and hob", "🧽", estimatedMinutes = 10),
                BreakdownResult("Sweep or mop the floor", "🫧", estimatedMinutes = 10)
            )

            "clean" in p || "tidy" in p || "hoover" in p || "vacuum" in p -> listOf(
                BreakdownResult("Clear any clutter first", "📦", estimatedMinutes = 10),
                BreakdownResult("Dust surfaces", "🪣", estimatedMinutes = 10),
                BreakdownResult("Hoover carpets / mop floors", "🧹", estimatedMinutes = 20),
                BreakdownResult("Put things back properly", "✅", estimatedMinutes = 5)
            )

            "laundry" in p || "washing" in p -> listOf(
                BreakdownResult("Sort clothes into piles", "👕", estimatedMinutes = 5),
                BreakdownResult("Put a wash on", "🫧", estimatedMinutes = 5),
                BreakdownResult("Hang or tumble-dry when done", "🏠", estimatedMinutes = 10),
                BreakdownResult("Put clothes away", "📦", estimatedMinutes = 10)
            )

            "shopping" in p || "groceries" in p -> listOf(
                BreakdownResult("Check fridge and cupboards", "🔍", estimatedMinutes = 5),
                BreakdownResult("Write the shopping list", "📝", estimatedMinutes = 5),
                BreakdownResult("Grab bags and head out", "🛍️", estimatedMinutes = 30),
                BreakdownResult("Put shopping away when back", "🏠", estimatedMinutes = 10)
            )

            "garden" in p || "gardening" in p -> listOf(
                BreakdownResult("Mow the lawn", "🌿", estimatedMinutes = 20),
                BreakdownResult("Pull weeds", "🌱", estimatedMinutes = 15),
                BreakdownResult("Water plants", "💧", estimatedMinutes = 10),
                BreakdownResult("Tidy tools away", "🔧", estimatedMinutes = 5)
            )

            "report" in p || "presentation" in p -> listOf(
                BreakdownResult("Gather data / source material", "📊", -1, estimatedMinutes = 30),
                BreakdownResult("Draft outline / structure", "📝", estimatedMinutes = 20),
                BreakdownResult("Write first draft", "✍️", estimatedMinutes = 60),
                BreakdownResult("Review and edit", "👀", estimatedMinutes = 30),
                BreakdownResult("Final proofread and send", "📤", estimatedMinutes = 15)
            )

            "email" in p || "reply" in p || "respond" in p -> listOf(
                BreakdownResult("Re-read the original message", "📧", -1, estimatedMinutes = 3),
                BreakdownResult("Draft your reply", "✍️", estimatedMinutes = 10),
                BreakdownResult("Check tone and send", "📤", estimatedMinutes = 2)
            )

            "meeting" in p -> listOf(
                BreakdownResult("Check the agenda", "📋", -1, estimatedMinutes = 5),
                BreakdownResult("Prep any notes or slides", "📝", estimatedMinutes = 20),
                BreakdownResult("Join / arrive on time", "🗓️", estimatedMinutes = 60),
                BreakdownResult("Write up action points after", "✅", estimatedMinutes = 15)
            )

            "budget" in p || "finance" in p || "invoice" in p -> listOf(
                BreakdownResult("Collect all receipts / data", "🧾", -1, estimatedMinutes = 10),
                BreakdownResult("Update the spreadsheet", "📊", estimatedMinutes = 20),
                BreakdownResult("Review totals", "🔍", estimatedMinutes = 10),
                BreakdownResult("Submit or file", "📤", estimatedMinutes = 5)
            )

            " and " in p -> prompt.split(" and ", ", ").map { BreakdownResult(it.trim()) }
            else -> emptyList()
        }
    }

    // ── Full parse: returns one or more ParsedTasks ───────────────────────────
    // Used by voice input and smart scheduling.

    suspend fun parsePrompt(prompt: String): List<ParsedTask> {
        val p = prompt.lowercase()

        // ── Habits: expand into multiple timed tasks ──────────────────────────

        if ("brush teeth" in p || "brush my teeth" in p || "teeth" in p) {
            return listOf(
                ParsedTask(
                    title = "Brush teeth 🦷",
                    suggestedContext = TaskContext.PERSONAL,
                    estimatedMinutes = 3,
                    suggestedRecurrence = RecurrenceConfig(
                        type = RecurrenceType.DAILY,
                        missedBehaviour = MissedBehaviour.IGNORABLE,
                        preferredTimeOfDayMinutes = 8 * 60,  // first reminder at 08:00
                        timesPerDay = 2                       // morning + evening
                    )
                )
            )
        }

        if ("vitamin" in p || "vitamins" in p || "pill" in p || "pills" in p ||
            "medication" in p || "medicine" in p || "tablet" in p
        ) {
            val threeADay = "3" in p || "three" in p || "times a day" in p || "tds" in p
            val timesPerDay = if (threeADay) 3 else 4
            val noun = when {
                "vitamin" in p || "vitamins" in p -> "vitamins"
                "tablet" in p -> "tablet"
                "pill" in p || "pills" in p -> "pills"
                else -> "medication"
            }
            return listOf(
                ParsedTask(
                    title = "Take $noun 💊",
                    suggestedContext = TaskContext.PERSONAL,
                    estimatedMinutes = 1,
                    suggestedRecurrence = RecurrenceConfig(
                        type = RecurrenceType.DAILY,
                        missedBehaviour = MissedBehaviour.IGNORABLE,
                        preferredTimeOfDayMinutes = 8 * 60,  // first reminder at 08:00
                        timesPerDay = timesPerDay
                    )
                )
            )
        }

        // ── Recurring household chores ────────────────────────────────────────

        if ("hoover" in p || "vacuum" in p || "clean" in p || "tidy" in p) {
            val isWeekly = "week" in p || "weekly" in p
            val isMonthly = "month" in p || "monthly" in p
            val (recurType, interval) = when {
                isMonthly -> RecurrenceType.MONTHLY to 1
                isWeekly -> RecurrenceType.WEEKLY to 1
                else -> RecurrenceType.WEEKLY to 1 // sensible default
            }
            return listOf(
                ParsedTask(
                    title = prompt.trim().replaceFirstChar { it.uppercase() },
                    suggestedContext = TaskContext.PERSONAL,
                    estimatedMinutes = 45,
                    subtasks = generate(prompt),
                    suggestedRecurrence = RecurrenceConfig(
                        type = recurType,
                        interval = interval,
                        missedBehaviour = MissedBehaviour.PERSISTENT,
                        preferredTimeOfDayMinutes = 10 * 60 // 10am
                    )
                )
            )
        }

        if ("laundry" in p || "washing" in p) {
            return listOf(
                ParsedTask(
                    title = prompt.trim().replaceFirstChar { it.uppercase() },
                    suggestedContext = TaskContext.PERSONAL,
                    estimatedMinutes = 30,
                    subtasks = generate(prompt),
                    suggestedRecurrence = RecurrenceConfig(
                        type = RecurrenceType.WEEKLY,
                        interval = 1,
                        missedBehaviour = MissedBehaviour.PERSISTENT
                    )
                )
            )
        }

        if ("shopping" in p || "groceries" in p) {
            return listOf(
                ParsedTask(
                    title = prompt.trim().replaceFirstChar { it.uppercase() },
                    suggestedContext = TaskContext.PERSONAL,
                    estimatedMinutes = 50,
                    subtasks = generate(prompt),
                    suggestedRecurrence = RecurrenceConfig(
                        type = RecurrenceType.WEEKLY,
                        interval = 1,
                        missedBehaviour = MissedBehaviour.PERSISTENT
                    )
                )
            )
        }

        // ── Work tasks — one-off with sensible priority ───────────────────────

        val isWork = detectContext(prompt) == TaskContext.WORK
        val subtasks = generate(prompt)
        val totalMins =
            subtasks.mapNotNull { it.estimatedMinutes }.takeIf { it.isNotEmpty() }?.sum()

        return listOf(
            ParsedTask(
                title = prompt.trim().replaceFirstChar { it.uppercase() },
                suggestedContext = if (isWork) TaskContext.WORK else TaskContext.PERSONAL,
                subtasks = subtasks,
                estimatedMinutes = totalMins
            )
        )
    }

    // ── Context detection ─────────────────────────────────────────────────────

    fun detectContext(prompt: String): TaskContext {
        val p = prompt.lowercase()
        val workWords = listOf(
            "report", "meeting", "email", "client", "presentation", "invoice",
            "budget", "deadline", "office", "colleague", "boss", "project", "sprint",
            "ticket", "pr", "pull request", "review", "deploy", "standup", "slack",
            "ansible", "jbpm", "playbook", "service", "upgrade", "rebuild", "template"
        )
        val personalWords = listOf(
            "clean", "laundry", "shopping", "garden", "dentist", "doctor",
            "gym", "cook", "dinner", "hoover", "vacuum", "tidy", "bins",
            "prescription", "kids", "school", "car", "mot", "plumber",
            "teeth", "brush", "vitamin", "pill", "medication"
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

// ─── Gemini Nano strategy ─────────────────────────────────────────────────────

class GeminiNanoStrategy @Inject constructor() : TaskBreakerStrategy {
    override suspend fun isAvailable(): Boolean = false // TODO: check AICore availability
    override suspend fun generate(prompt: String): List<BreakdownResult> = emptyList()
}

// ─── Hybrid service ───────────────────────────────────────────────────────────

class HybridTaskService @Inject constructor(
    private val template: TemplateStrategy,
    private val nano: GeminiNanoStrategy
) {
    suspend fun generateSubtasks(prompt: String): List<BreakdownResult> =
        if (nano.isAvailable()) nano.generate(prompt) else template.generate(prompt)

    /** Fully parse a voice/text prompt into one or more scheduled tasks. */
    suspend fun parsePrompt(prompt: String): List<ParsedTask> = template.parsePrompt(prompt)

    fun detectContext(prompt: String): TaskContext = template.detectContext(prompt)
}
