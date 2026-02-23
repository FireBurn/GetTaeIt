package uk.co.fireburn.gettaeit.shared.domain.ai

import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import uk.co.fireburn.gettaeit.shared.data.MissedBehaviour
import uk.co.fireburn.gettaeit.shared.data.RecurrenceConfig
import uk.co.fireburn.gettaeit.shared.data.RecurrenceType
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import javax.inject.Inject

data class BreakdownResult(
    val title: String,
    val icon: String = "🔹",
    val priorityOffset: Int = 0,
    val estimatedMinutes: Int? = null
)

data class ParsedTask(
    val title: String,
    val suggestedContext: TaskContext,
    val subtasks: List<BreakdownResult> = emptyList(),
    val suggestedRecurrence: RecurrenceConfig? = null,
    val preferredTimeMinutes: Int? = null,
    val estimatedMinutes: Int? = null
)

interface TaskBreakerStrategy {
    suspend fun isAvailable(): Boolean
    suspend fun generate(prompt: String): List<BreakdownResult>
    suspend fun parsePrompt(prompt: String): List<ParsedTask>
}

// ─── Template strategy (offline fallback) ────────────────────────────────────

class TemplateStrategy @Inject constructor() : TaskBreakerStrategy {

    override suspend fun isAvailable(): Boolean = true

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

    // ── Named-time anchors (minutes since midnight) ─────────────────────────
    private val WAKE_UP = 7 * 60   // 07:00
    private val MID_MORN = 9 * 60   // 09:00
    private val LUNCH = 13 * 60  // 13:00
    private val AFTERNOON = 15 * 60  // 15:00
    private val EVENING = 18 * 60  // 18:00
    private val NIGHT = 21 * 60  // 21:00

    private fun cleanTitle(raw: String): String = raw
        .replace(
            Regex(
                """(every\s+)?(morning\s+and\s+(night|evening)|morning|night|evening|twice\s+a\s+day|twice\s+daily|2\s+times\s+a\s+day|when\s+i\s+(wake|get)\s+up|wake[\s-]?up|first\s+thing|at\s+lunch(time)?|midday|noon|with\s+dinner|at\s+dinner(time)?|dinnertime|before\s+bed(time)?|at\s+night|night\s+time|go(ing)?\s+to\s+(sleep|bed)|mid[\s-]?morning|in\s+the\s+(afternoon|evening))""",
                RegexOption.IGNORE_CASE
            ), ""
        )
        .trim()
        .trimEnd(',', ';', '-', '.')
        .trim()
        .replaceFirstChar { it.uppercase() }
        .ifBlank { raw.trim().replaceFirstChar { it.uppercase() } }

    private fun multiSlotConfig(slots: List<Int>): RecurrenceConfig = RecurrenceConfig(
        type = RecurrenceType.DAILY,
        missedBehaviour = MissedBehaviour.IGNORABLE,
        timesPerDay = slots.size,
        dailySlotMinutes = slots
    )

    override suspend fun parsePrompt(prompt: String): List<ParsedTask> {
        val p = prompt.lowercase()

        // ── Named-time slot detection ─────────────────────────────────────────
        // Build an explicit list of minute-of-day values from any time-of-day
        // phrases the user mentioned. These map directly to alarm times.
        val namedSlots = mutableListOf<Int>()

        // Detect each anchor — order doesn't matter, list is sorted later
        if (Regex("""(when i (wake|get) up|wake[- ]?up|first thing|get up|morning)""").containsMatchIn(
                p
            )
        )
            namedSlots += WAKE_UP
        if ("mid morning" in p || "mid-morning" in p) namedSlots += MID_MORN
        if (Regex("""(lunch|lunchtime|midday|noon|12 o)""").containsMatchIn(p))
            namedSlots += LUNCH
        if ("afternoon" in p) namedSlots += AFTERNOON
        if (Regex("""(evening|with dinner|at dinner|dinner time|dinnertime)""").containsMatchIn(p))
            namedSlots += EVENING
        if (Regex("""(before bed|bedtime|bed time|at night|night time|go to sleep|going? to bed)""").containsMatchIn(
                p
            )
        )
            namedSlots += NIGHT

        // "twice a day" / "morning and night" without specific names → morning + night
        val twiceADay = "twice a day" in p || "twice daily" in p || "2 times a day" in p
        val morningAndNight = "morning" in p && ("night" in p || "evening" in p)
        if ((twiceADay || morningAndNight) && namedSlots.size < 2) {
            namedSlots.clear()
            namedSlots += WAKE_UP
            namedSlots += NIGHT
        }

        // Deduplicate and sort
        val distinctSlots = namedSlots.distinct().sorted()

        // ── 3+ explicit slots (e.g. "4 times a day" or 4+ named anchors) ──────
        if (distinctSlots.size >= 3) {
            val title = cleanTitle(prompt)
            return listOf(
                ParsedTask(
                    title = title,
                    suggestedContext = detectContext(prompt),
                    estimatedMinutes = 2,
                    suggestedRecurrence = multiSlotConfig(distinctSlots)
                )
            )
        }

        // ── 2 explicit slots (morning + night, twice a day, etc.) ─────────────
        if (distinctSlots.size == 2) {
            val title = cleanTitle(prompt)
            return listOf(
                ParsedTask(
                    title = title,
                    suggestedContext = detectContext(prompt),
                    estimatedMinutes = 2,
                    suggestedRecurrence = multiSlotConfig(distinctSlots)
                )
            )
        }

        // ── "3 times a day" / "3x a day" without named anchors ───────────────
        val timesRegex = Regex("""(\d+)\s*(times?|x)\s*(a|per)\s*(day|daily)""")
        val timesMatch = timesRegex.find(p)
        if (timesMatch != null) {
            val n = timesMatch.groupValues[1].toIntOrNull() ?: 1
            if (n in 2..8) {
                // Spread evenly across waking hours 07:00-22:00
                val span = 15 * 60  // 900 mins
                val step = span / (n - 1).coerceAtLeast(1)
                val evenSlots = (0 until n).map { i -> (WAKE_UP + i * step).coerceAtMost(22 * 60) }
                return listOf(
                    ParsedTask(
                        title = cleanTitle(prompt),
                        suggestedContext = detectContext(prompt),
                        estimatedMinutes = 2,
                        suggestedRecurrence = multiSlotConfig(evenSlots)
                    )
                )
            }
        }

        // ── Single slot — all the original patterns below ─────────────────────

        if ("brush teeth" in p || "brush my teeth" in p || "teeth" in p) {
            return listOf(
                ParsedTask(
                    title = "Brush teeth 🦷",
                    suggestedContext = TaskContext.PERSONAL,
                    estimatedMinutes = 3,
                    suggestedRecurrence = RecurrenceConfig(
                        type = RecurrenceType.DAILY,
                        missedBehaviour = MissedBehaviour.IGNORABLE,
                        preferredTimeOfDayMinutes = WAKE_UP
                    )
                )
            )
        }

        if ("vitamin" in p || "vitamins" in p || "pill" in p || "pills" in p ||
            "medication" in p || "medicine" in p || "tablet" in p
        ) {
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
                        preferredTimeOfDayMinutes = WAKE_UP
                    )
                )
            )
        }

        if ("hoover" in p || "vacuum" in p || "clean" in p || "tidy" in p) {
            val isWeekly = "week" in p || "weekly" in p
            val isMonthly = "month" in p || "monthly" in p
            val (recurType, interval) = when {
                isMonthly -> RecurrenceType.MONTHLY to 1
                isWeekly -> RecurrenceType.WEEKLY to 1
                else -> RecurrenceType.WEEKLY to 1
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
                        preferredTimeOfDayMinutes = 10 * 60
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
            "teeth", "brush", "vitamin", "pill", "medication", "walk", "dog"
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

// ─── ML Kit GenAI (True On-Device) strategy ───────────────────────────────────

class GeminiNanoStrategy @Inject constructor(
    private val templateFallback: TemplateStrategy
) : TaskBreakerStrategy {

    private val TAG = "MLKitGenAIDebug"

    override suspend fun isAvailable(): Boolean {
        Log.i(TAG, "Checking ML Kit GenAI status...")
        return try {
            val model = Generation.getClient()
            val status = model.checkStatus()

            when (status) {
                FeatureStatus.AVAILABLE -> {
                    Log.i(TAG, "Nano is AVAILABLE! Ready to use.")
                    true
                }

                FeatureStatus.DOWNLOADABLE -> {
                    Log.i(
                        TAG,
                        "Nano is DOWNLOADABLE. Instructing Play Services to download it now..."
                    )
                    model.download().collect { dlStatus ->
                        Log.d(TAG, "Download progress: $dlStatus")
                    }
                    false
                }

                FeatureStatus.UNAVAILABLE -> {
                    Log.w(TAG, "Nano is UNAVAILABLE on this specific device model.")
                    false
                }

                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check status: ${e.message}", e)
            false
        }
    }

    override suspend fun generate(prompt: String): List<BreakdownResult> {
        Log.i(TAG, "Generating subtasks locally via ML Kit...")
        return try {
            val model = Generation.getClient()

            val promptText = """
                Task: "$prompt"
                Only break a task into steps if it has multiple distinct stages (e.g. clean the house, write a report, do the shopping). Single-action tasks (e.g. brush teeth, take medication, make a call) need no steps — output nothing for them.
                If steps are needed, list max 4 on separate lines using EXACTLY this format: Title|Minutes
                Example: Clear the desk|5
                Do not output any markdown, intro, or extra text.
            """.trimIndent()

            val response = model.generateContent(promptText)
            val responseText = response.candidates.firstOrNull()?.text ?: ""
            Log.i(TAG, "ML Kit raw response:\n$responseText")

            val results = mutableListOf<BreakdownResult>()

            // Fault-tolerant parsing: we process line by line. If it got cut off,
            // the last line might fail the parts.size == 2 check and just be ignored safely.
            responseText.lines().forEach { line ->
                val cleanLine = line.trim().removePrefix("-").removePrefix("*").trim()
                if (cleanLine.isNotBlank() && cleanLine.contains("|")) {
                    val parts = cleanLine.split("|")
                    if (parts.size >= 2) {
                        val title = parts[0].trim()
                        val mins = parts[1].trim().filter { it.isDigit() }.toIntOrNull()
                        if (title.isNotBlank()) {
                            results.add(
                                BreakdownResult(
                                    title = title,
                                    icon = "🔹",
                                    estimatedMinutes = mins
                                )
                            )
                        }
                    }
                }
            }
            results
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit generation failed: ${e.message}", e)
            emptyList()
        }
    }

    // ── Named-time anchors (minutes since midnight) ─────────────────────────
    override suspend fun parsePrompt(prompt: String): List<ParsedTask> {
        Log.i(TAG, "Parsing voice prompt locally via ML Kit...")
        return try {
            val model = Generation.getClient()

            val promptText = """
                Task: "$prompt"
                Only break a task into steps if it has multiple distinct stages (e.g. clean the house, write a report, do the shopping). Single-action tasks (e.g. brush teeth, take medication, make a call) need no steps — output nothing for them.
                Output EXACTLY in this format:
                MAIN: Task Title|Minutes
                SUB: Step one|Minutes
                SUB: Step two|Minutes
                Omit all SUB lines if the task is simple. Do not output any markdown or intro text.
            """.trimIndent()

            val response = model.generateContent(promptText)
            val responseText = response.candidates.firstOrNull()?.text ?: ""
            Log.i(TAG, "ML Kit parse response:\n$responseText")

            var mainTitle = "New Task"
            var mainContext = TaskContext.PERSONAL
            var mainMins: Int? = null
            val parsedSubtasks = mutableListOf<BreakdownResult>()

            // Resilient line-by-line parsing
            responseText.lines().forEach { line ->
                val cleanLine = line.trim().removePrefix("-").removePrefix("*").trim()

                if (cleanLine.startsWith("MAIN:", ignoreCase = true)) {
                    val content = cleanLine.substring(5).trim()
                    val parts = content.split("|")
                    if (parts.isNotEmpty()) mainTitle = parts[0].trim()
                    if (parts.size > 1) {
                        mainContext = if (parts[1].trim()
                                .equals("WORK", true)
                        ) TaskContext.WORK else TaskContext.PERSONAL
                    }
                    if (parts.size > 2) {
                        mainMins = parts[2].trim().filter { it.isDigit() }.toIntOrNull()
                    }
                } else if (cleanLine.startsWith("SUB:", ignoreCase = true)) {
                    val content = cleanLine.substring(4).trim()
                    val parts = content.split("|")
                    if (parts.isNotEmpty()) {
                        val subTitle = parts[0].trim()
                        val subMins = if (parts.size > 1) parts[1].trim().filter { it.isDigit() }
                            .toIntOrNull() else null
                        if (subTitle.isNotBlank()) {
                            parsedSubtasks.add(
                                BreakdownResult(
                                    title = subTitle,
                                    icon = "🔹",
                                    estimatedMinutes = subMins
                                )
                            )
                        }
                    }
                }
            }

            // Let the template decide recurrence — it reliably handles all patterns
            // including explicit named-time slots (dailySlotMinutes).
            val templateRule = templateFallback.parsePrompt(prompt).firstOrNull()
            listOf(
                ParsedTask(
                    title = mainTitle,
                    suggestedContext = mainContext,
                    estimatedMinutes = mainMins,
                    subtasks = parsedSubtasks,
                    suggestedRecurrence = templateRule?.suggestedRecurrence,
                    preferredTimeMinutes = templateRule?.preferredTimeMinutes
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit parse failed, falling back to template: ${e.message}", e)
            templateFallback.parsePrompt(prompt)
        }
    }
}

// ─── Hybrid service ───────────────────────────────────────────────────────────

class HybridTaskService @Inject constructor(
    private val template: TemplateStrategy,
    private val nano: GeminiNanoStrategy
) {
    suspend fun generateSubtasks(prompt: String): List<BreakdownResult> =
        if (nano.isAvailable()) nano.generate(prompt) else template.generate(prompt)

    suspend fun parsePrompt(prompt: String): List<ParsedTask> =
        if (nano.isAvailable()) nano.parsePrompt(prompt) else template.parsePrompt(prompt)

    fun detectContext(prompt: String): TaskContext = template.detectContext(prompt)

    fun improvedEstimate(historicalTasks: List<TaskEntity>): Int? {
        val samples = historicalTasks
            .mapNotNull { it.actualMinutes }
            .filter { it > 0 }
        if (samples.size < 2) return null

        val recent = samples.takeLast(samples.size / 2 + 1)
        val older = samples.dropLast(recent.size)
        val weightedSum = recent.sum() * 2 + older.sum()
        val weightedCount = recent.size * 2 + older.size
        return (weightedSum.toDouble() / weightedCount).toInt().coerceAtLeast(1)
    }
}
