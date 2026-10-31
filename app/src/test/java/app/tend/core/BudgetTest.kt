package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetTest {
    private val cfg = BudgetConfig(totalMinutes = 180, freeMinutes = 45, manualCapMinutes = 30)
    private val day = DayState(LocalDate.of(2026, 10, 26))

    @Test fun `fresh day shows free budget and everything else earnable`() {
        assertEquals(45, Budget.remainingMinutes(cfg, day))
        assertEquals(135, Budget.earnableMinutes(cfg, day))
        assertFalse(Budget.isLocked(cfg, day))
    }

    @Test fun `usage rounds up to whole minutes`() {
        val d = day.copy(usedSeconds = 61)
        assertEquals(43, Budget.remainingMinutes(cfg, d))
    }

    @Test fun `overuse goes negative and locks until a task lifts it above zero`() {
        var d = day.copy(usedSeconds = 50 * 60L)
        assertEquals(-5, Budget.remainingMinutes(cfg, d))
        assertTrue(Budget.isLocked(cfg, d))
        d = Budget.credit(cfg, d, "outside", 20, manual = false)
        assertEquals(15, Budget.remainingMinutes(cfg, d))
        assertFalse(Budget.isLocked(cfg, d))
    }

    @Test fun `a task credits only once per day`() {
        val once = Budget.credit(cfg, day, "move", 30, manual = false)
        val twice = Budget.credit(cfg, once, "move", 30, manual = false)
        assertEquals(30, twice.earnedMinutes)
    }

    @Test fun `credit is clamped to the daily total`() {
        val small = cfg.copy(totalMinutes = 60)
        val d = Budget.credit(small, day, "move", 30, manual = false)
        assertEquals(15, d.earnedMinutes)
        assertEquals(0, Budget.earnableMinutes(small, d))
    }

    @Test fun `manual credits are capped`() {
        var d = Budget.credit(cfg, day, "a", 20, manual = true)
        d = Budget.credit(cfg, d, "b", 20, manual = true)
        assertEquals(30, d.manualEarnedMinutes)
        d = Budget.credit(cfg, d, "c", 20, manual = false)
        assertEquals(50, d.earnedMinutes)
    }

    @Test fun `each open waits five seconds longer`() {
        assertEquals(3, Budget.openDelaySeconds(cfg, day))
        assertEquals(18, Budget.openDelaySeconds(cfg, day.copy(opensToday = 3)))
    }

    @Test fun `only unused minutes grow the garden, overuse does not carry over`() {
        assertEquals(45, Budget.gardenMinutesAtClose(cfg, day))
        assertEquals(0, Budget.gardenMinutesAtClose(cfg, day.copy(usedSeconds = 60 * 60L)))
    }

    @Test fun `mood follows the day`() {
        assertEquals(Mood.HAPPY, Moods.of(cfg, day))
        assertEquals(Mood.OUTSIDE, Moods.of(cfg, Budget.credit(cfg, day, "outside", 20, false)))
        assertEquals(Mood.TIRED, Moods.of(cfg, day.copy(usedSeconds = 40 * 60L)))
    }

    @Test fun `automatic tasks complete from activity numbers`() {
        assertEquals(listOf(Tasks.OUTSIDE), completedAutomaticTasks(steps = 4200, exerciseMinutes = 10))
        assertEquals(emptyList<TaskDef>(), completedAutomaticTasks(null, null))
    }

    @Test fun `activity sync credits automatic tasks once and never lowers totals`() {
        var d = Budget.applyActivity(cfg, day, steps = 3100, exerciseMinutes = 0)
        assertEquals(3100, d.steps)
        assertEquals(emptySet<String>(), d.completedTasks)
        d = Budget.applyActivity(cfg, d, steps = 4100, exerciseMinutes = 35)
        assertEquals(setOf("outside", "move"), d.completedTasks)
        assertEquals(50, d.earnedMinutes)
        d = Budget.applyActivity(cfg, d, steps = 200, exerciseMinutes = null)
        assertEquals(4100, d.steps)
        assertEquals(50, d.earnedMinutes)
    }
}
