package uk.co.fireburn.gettaeit.shared.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import java.util.Calendar

class ContextModeDeciderTest {
    @Test
    fun `work hours select work mode`() {
        assertEquals(
            AppMode.WORK,
            ContextModeDecider.decide(UserPreferences(), false, null, at(Calendar.MONDAY, 10))
        )
    }

    @Test
    fun `work wifi selects work mode outside schedule`() {
        assertEquals(
            AppMode.WORK,
            ContextModeDecider.decide(
                UserPreferences(workSsid = "Office"), false, "office", at(Calendar.SUNDAY, 20)
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
                now = at(Calendar.MONDAY, 10)
            )
        )
    }

    @Test
    fun `hour before work is commute mode`() {
        assertEquals(
            AppMode.COMMUTE,
            ContextModeDecider.decide(UserPreferences(), false, null, at(Calendar.MONDAY, 8))
        )
    }

    private fun at(day: Int, hour: Int): Calendar = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.AUGUST, 10, hour, 0)
        while (get(Calendar.DAY_OF_WEEK) != day) add(Calendar.DAY_OF_YEAR, 1)
    }
}
