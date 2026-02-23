package uk.co.fireburn.gettaeit.shared.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// ─── Context: Work vs Personal ───────────────────────────────────────────────

enum class TaskContext { WORK, PERSONAL, ANY }

// ─── Recurrence: how often a task repeats ────────────────────────────────────

/**
 * How a task recurs.
 *
 * NONE          – one-shot, never repeats
 * DAILY         – every N days (e.g. "brush teeth" = every 1 day)
 * WEEKLY        – every N weeks (e.g. "change sheets" = every 1 week)
 * MONTHLY       – every N months
 * CUSTOM_DAYS   – specific days of the week (Calendar.MONDAY etc.)
 */
enum class RecurrenceType { NONE, DAILY, WEEKLY, MONTHLY, CUSTOM_DAYS }

/**
 * What happens when a recurring task notification fires and the user misses it.
 *
 * IGNORABLE    – missed instances are silently skipped (e.g. "brush teeth tonight" – too late now)
 * PERSISTENT   – reminder keeps showing up until the user actually does it, then the next
 *                recurrence is calculated from the completion date (e.g. "change sheets")
 */
enum class MissedBehaviour { IGNORABLE, PERSISTENT }

data class RecurrenceConfig(
    val type: RecurrenceType = RecurrenceType.NONE,
    val interval: Int = 1,                       // every N days/weeks/months
    val daysOfWeek: List<Int> = emptyList(),      // for CUSTOM_DAYS; Calendar.MONDAY etc.
    val missedBehaviour: MissedBehaviour = MissedBehaviour.IGNORABLE,
    /** Preferred time of day (minutes since midnight), e.g. 480 = 08:00 */
    val preferredTimeOfDayMinutes: Int? = null,
    /**
     * How many times per day to remind. Default 1.
     * e.g. 2 = morning + evening, 4 = morning/lunch/evening/bedtime.
     * When [dailySlotMinutes] is non-empty this field is ignored — the explicit
     * slots take precedence. Used only as a fallback for even-spacing.
     */
    val timesPerDay: Int = 1,
    /**
     * Explicit per-slot times (minutes since midnight), one entry per alarm.
     * When non-empty these are used directly instead of even-spacing.
     * e.g. listOf(7*60, 13*60, 18*60, 22*60) = wake / lunch / dinner / bedtime.
     * Populated by voice parsing when the user names specific times of day.
     */
    val dailySlotMinutes: List<Int> = emptyList()
)

// ─── Task Entity ──────────────────────────────────────────────────────────────

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),

    // Core
    val title: String,
    val description: String? = null,
    val context: TaskContext = TaskContext.PERSONAL,
    val priority: Int = 3,                       // 1 = urgent, 5 = someday

    // Status
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,               // epoch ms of last completion
    val isSnoozed: Boolean = false,
    val snoozedUntil: Long? = null,              // epoch ms; null = not snoozed

    // Scheduling
    val dueDate: Long? = null,                   // epoch ms; null = inbox/anytime
    val recurrence: RecurrenceConfig = RecurrenceConfig(),

    /**
     * For PERSISTENT recurring tasks: when is the *next* instance due?
     * Calculated as completedAt + recurrence interval after each completion.
     * Null means the task is currently active/overdue.
     */
    val nextOccurrenceAt: Long? = null,

    // Hierarchy & dependencies
    val parentId: UUID? = null,                  // null = top-level task
    val dependencyIds: List<UUID> = emptyList(), // task IDs that must be done first
    val isSubtask: Boolean = false,

    // Location / context triggers
    val locationTrigger: String? = null,         // "lat,lng,radiusM" e.g. "55.86,-4.25,100"
    val wifiTrigger: String? = null,             // SSID

    // Meal-prep backwards scheduling
    val offsetReferenceId: UUID? = null,
    val offsetDuration: Long? = null,            // ms before (negative) or after the reference

    // Time estimation
    /** AI/template estimate of how long this task takes (minutes). Null = unknown. */
    val estimatedMinutes: Int? = null,
    /** Actual time recorded by user on completion (minutes). Used to improve future estimates. */
    val actualMinutes: Int? = null,

    // Gamification
    val xpValue: Int = 10,
    val streakCount: Int = 0,
    val lastStreakDate: Long? = null
)
