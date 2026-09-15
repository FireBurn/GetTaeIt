package uk.co.fireburn.gettaeit.shared.domain

import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import uk.co.fireburn.gettaeit.shared.data.WorkSchedule
import java.util.Calendar

/** Pure, testable policy for the Work/Personal boundary. */
object ContextModeDecider {
    private const val MINUTES_PER_DAY = 24 * 60
    private const val COMMUTE_WINDOW_MINUTES = 60

    fun decide(
        preferences: UserPreferences,
        isAtWorkLocation: Boolean,
        currentSsid: String?,
        isCarMode: Boolean,
        now: Calendar
    ): AppMode {
        // Vacation is an explicit boundary. Location and Wi-Fi must not pull a
        // user back into work mode while it is enabled.
        if (preferences.isVacationMode) return AppMode.PERSONAL

        if (isCarMode) return AppMode.COMMUTE

        val schedule = preferences.workSchedule
        val today = now.get(Calendar.DAY_OF_WEEK)
        val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val isOnWorkWifi = preferences.workSsid != null &&
            currentSsid?.equals(preferences.workSsid, ignoreCase = true) == true
        val isWorkHours = schedule.isWorkingAt(today, minuteOfDay)
        val isCommuteTime = !isAtWorkLocation && !isOnWorkWifi && schedule.isCommutingAt(today, minuteOfDay)

        return when {
            isAtWorkLocation || isOnWorkWifi || isWorkHours -> AppMode.WORK
            isCommuteTime -> AppMode.COMMUTE
            else -> AppMode.PERSONAL
        }
    }

    /**
     * How long a shift lasts. An end at or before the start runs past midnight, and a shift
     * belongs to the day it starts on: a Friday 22:00–06:00 night shift is still work at
     * 03:00 on Saturday. Equal start and end means no set hours.
     */
    private val WorkSchedule.shiftMinutes: Int
        get() = Math.floorMod(endMinuteOfDay - startMinuteOfDay, MINUTES_PER_DAY)

    private fun WorkSchedule.isWorkingAt(today: Int, minuteOfDay: Int): Boolean =
        shiftMinutes > 0 && (-1..0).any { dayOffset ->
            shiftsOn(today, dayOffset) && minutesIntoShift(minuteOfDay, dayOffset) in 0 until shiftMinutes
        }

    /** The hour before a shift starts and the hour after it ends. */
    private fun WorkSchedule.isCommutingAt(today: Int, minuteOfDay: Int): Boolean =
        shiftMinutes > 0 && (-1..1).any { dayOffset ->
            val intoShift = minutesIntoShift(minuteOfDay, dayOffset)
            shiftsOn(today, dayOffset) && (
                intoShift in -COMMUTE_WINDOW_MINUTES until 0 ||
                    intoShift in shiftMinutes until shiftMinutes + COMMUTE_WINDOW_MINUTES
                )
        }

    /** Minutes since the start of the shift that begins [dayOffset] days from today. */
    private fun WorkSchedule.minutesIntoShift(minuteOfDay: Int, dayOffset: Int): Int =
        minuteOfDay - (startMinuteOfDay + dayOffset * MINUTES_PER_DAY)

    private fun WorkSchedule.shiftsOn(today: Int, dayOffset: Int): Boolean =
        Math.floorMod(today - Calendar.SUNDAY + dayOffset, 7) + Calendar.SUNDAY in workingDays
}
