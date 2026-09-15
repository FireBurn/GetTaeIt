package uk.co.fireburn.gettaeit.shared.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import uk.co.fireburn.gettaeit.shared.data.WorkSchedule
import java.util.Calendar

class ContextModeDeciderTest {
    @Test
    fun `work hours select work mode`() {
        assertEquals(
            AppMode.WORK,
            ContextModeDecider.decide(UserPreferences(), false, null, false, at(Calendar.MONDAY, 10))
        )
    }

    @Test
    fun `work wifi selects work mode outside schedule`() {
        assertEquals(
            AppMode.WORK,
            ContextModeDecider.decide(
                UserPreferences(workSsid = "Office"), false, "office", false, at(Calendar.SUNDAY, 20)
            )
        )
    }

    @Test
    fun `vacation always protects personal mode`() {
        assertEquals(
            AppMode.PERSONAL,
            ContextModeDecider.decide(
                UserPreferences(isVacationMode = true, workSsid = "Office"),
                isAtWorkLocation = true,
                currentSsid = "Office",
                isCarMode = true, // Vacation beats everything
                now = at(Calendar.MONDAY, 10)
            )
        )
    }

    @Test
    fun `hour before work is commute mode`() {
        assertEquals(
            AppMode.COMMUTE,
            ContextModeDecider.decide(UserPreferences(), false, null, false, at(Calendar.MONDAY, 8))
        )
    }

    @Test
    fun `hour after work is commute mode then personal`() {
        assertEquals(AppMode.COMMUTE, decide(UserPreferences(), at(Calendar.MONDAY, 17, 30)))
        assertEquals(AppMode.PERSONAL, decide(UserPreferences(), at(Calendar.MONDAY, 18)))
    }

    @Test
    fun `car mode overrides personal mode outside commute hours`() {
        assertEquals(
            AppMode.COMMUTE,
            ContextModeDecider.decide(UserPreferences(), false, null, true, at(Calendar.SATURDAY, 15))
        )
    }

    @Test
    fun `start minutes are respected`() {
        val prefs = UserPreferences(workSchedule = WorkSchedule(startHour = 8, startMinute = 45, endHour = 16, endMinute = 15))

        assertEquals(AppMode.COMMUTE, decide(prefs, at(Calendar.TUESDAY, 8, 40)))
        assertEquals(AppMode.WORK, decide(prefs, at(Calendar.TUESDAY, 8, 45)))
        assertEquals(AppMode.WORK, decide(prefs, at(Calendar.TUESDAY, 16, 14)))
        assertEquals(AppMode.COMMUTE, decide(prefs, at(Calendar.TUESDAY, 16, 15)))
    }

    @Test
    fun `overnight shift belongs to the day it starts`() {
        val nights = UserPreferences(
            workSchedule = WorkSchedule(startHour = 22, endHour = 6, workingDays = listOf(Calendar.FRIDAY))
        )

        assertEquals(AppMode.COMMUTE, decide(nights, at(Calendar.FRIDAY, 21, 30)))
        assertEquals(AppMode.WORK, decide(nights, at(Calendar.FRIDAY, 23)))
        assertEquals(AppMode.WORK, decide(nights, at(Calendar.SATURDAY, 3)))
        assertEquals(AppMode.COMMUTE, decide(nights, at(Calendar.SATURDAY, 6, 30)))
        assertEquals(AppMode.PERSONAL, decide(nights, at(Calendar.THURSDAY, 23)))
        assertEquals(AppMode.PERSONAL, decide(nights, at(Calendar.FRIDAY, 3)))
    }

    @Test
    fun `sunday night shift carries into monday morning`() {
        val sundayNights = UserPreferences(
            workSchedule = WorkSchedule(startHour = 23, endHour = 7, workingDays = listOf(Calendar.SUNDAY))
        )

        assertEquals(AppMode.WORK, decide(sundayNights, at(Calendar.MONDAY, 2)))
    }

    @Test
    fun `shift work with no fixed days leaves location and wifi in charge`() {
        val shiftWorker = UserPreferences(workSchedule = WorkSchedule(workingDays = emptyList()), workSsid = "Ward 7")

        assertEquals(AppMode.PERSONAL, decide(shiftWorker, at(Calendar.MONDAY, 10)))
        assertEquals(
            AppMode.WORK,
            ContextModeDecider.decide(shiftWorker, false, "Ward 7", false, at(Calendar.SUNDAY, 3))
        )
    }

    private fun decide(prefs: UserPreferences, now: Calendar) =
        ContextModeDecider.decide(prefs, isAtWorkLocation = false, currentSsid = null, isCarMode = false, now = now)

    private fun at(day: Int, hour: Int, minute: Int = 0): Calendar = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.AUGUST, 10, hour, minute)
        while (get(Calendar.DAY_OF_WEEK) != day) add(Calendar.DAY_OF_YEAR, 1)
    }
}
