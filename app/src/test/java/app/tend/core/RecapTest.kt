package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RecapTest {
    private val cfg = BudgetConfig(totalMinutes = 180, freeMinutes = 45)
    private val day = DayState(LocalDate.of(2026, 10, 31))

    @Test fun `recap counts unused minutes and activity`() {
        val d = Budget.credit(cfg, day.copy(usedSeconds = 30 * 60L, steps = 6240, exerciseMinutes = 48), "read", 15, false)
        val r = Recap.of(cfg, d, emptyList())
        assertEquals(30, r.gotBackMinutes)
        assertEquals("within budget, normal unknown", Acorns.WITHIN_BUDGET, r.tonightAcorns)
        assertEquals(Acorns.WITHIN_BUDGET + 3, Recap.of(cfg, d, emptyList(), normalMinutes = 60).tonightAcorns)
        assertEquals(6240, r.steps)
        assertEquals(20, r.readingMinutes)
    }

    @Test fun `recap spots late regrets`() {
        val answers = listOf(
            WorthIt(LocalTime.of(9, 0), "Yes"),
            WorthIt(LocalTime.of(22, 10), "Not really"),
            WorthIt(LocalTime.of(23, 5), "Not really"),
        )
        val r = Recap.of(cfg, day, answers)
        assertEquals(2, r.notWorthIt)
        assertEquals(22, r.notWorthAfterHour)
        assertNull(Recap.of(cfg, day, answers + WorthIt(LocalTime.of(12, 0), "Not really")).notWorthAfterHour)
    }
}
