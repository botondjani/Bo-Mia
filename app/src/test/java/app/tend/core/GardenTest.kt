package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class GardenTest {
    private val cfg = BudgetConfig(totalMinutes = 180, freeMinutes = 45)
    private val day = DayState(LocalDate.of(2026, 10, 31))

    @Test fun `plants grow with unused minutes`() {
        assertNull(Garden.plantFor(0))
        assertEquals(Plant.SPROUT, Garden.plantFor(10))
        assertEquals(Plant.TULIP, Garden.plantFor(20))
        assertEquals(Plant.SUNFLOWER, Garden.plantFor(42))
    }

    @Test fun `home grows weekly`() {
        assertEquals(0, Garden.homeStage(0))
        assertEquals(1, Garden.homeStage(7))
        assertEquals(2, Garden.homeStage(21))
    }

    @Test fun `recap counts unused minutes and activity`() {
        val d = Budget.credit(cfg, day.copy(usedSeconds = 30 * 60L, steps = 6240, exerciseMinutes = 48), "read", 15, false)
        val r = Recap.of(cfg, d, emptyList())
        assertEquals(30, r.gotBackMinutes)
        assertEquals(Plant.SUNFLOWER, r.tonightsPlant)
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
