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

    @Test fun `opens wait 5 s, one second more each time, at most 10 s`() {
        assertEquals(5, Budget.openDelaySeconds(cfg, day))
        assertEquals(8, Budget.openDelaySeconds(cfg, day.copy(opensToday = 3)))
        assertEquals(10, Budget.openDelaySeconds(cfg, day.copy(opensToday = 40)))
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
        val both = listOf(Tasks.MOVE, Tasks.OUTSIDE)
        assertEquals(listOf(Tasks.OUTSIDE), completedAutomaticTasks(steps = 4200, exerciseMinutes = 10, both))
        assertEquals(emptyList<TaskDef>(), completedAutomaticTasks(null, null, both))
        assertEquals(emptyList<TaskDef>(), completedAutomaticTasks(steps = 9000, exerciseMinutes = 60, listOf(Tasks.READ)))
    }

    @Test fun `activity sync credits automatic tasks once and never lowers totals`() {
        var d = Budget.applyActivity(cfg, day, steps = 3100, exerciseMinutes = 0, selected = Tasks.legacyDefault)
        assertEquals(3100, d.steps)
        assertEquals(emptySet<String>(), d.completedTasks)
        d = Budget.applyActivity(cfg, d, steps = 4100, exerciseMinutes = 35, selected = Tasks.legacyDefault)
        assertEquals(setOf("outside", "move"), d.completedTasks)
        assertEquals(50, d.earnedMinutes)
        d = Budget.applyActivity(cfg, d, steps = 200, exerciseMinutes = null, selected = Tasks.legacyDefault)
        assertEquals(4100, d.steps)
        assertEquals(50, d.earnedMinutes)
    }

    @Test fun `earnable counts only the user's picks, check-offs within the cap`() {
        val picks = listOf(Tasks.OUTSIDE, Tasks.FRIENDS, Tasks.HOBBY, Tasks.CHORES, Tasks.custom("Guitar", "music"))
        assertEquals(20 + 45, Budget.earnableFrom(cfg.copy(manualCapMinutes = 45), day, picks))
        val d = Budget.credit(cfg, day, Tasks.OUTSIDE.id, 20, manual = false)
        assertEquals(45, Budget.earnableFrom(cfg.copy(manualCapMinutes = 45), d, picks))
        assertEquals(0, Budget.earnableFrom(cfg, day, emptyList()))
    }

    @Test fun `custom activities get a clean id and a fixed reward`() {
        val t = Tasks.custom("  Play guitar, loud | ", "music")
        assertEquals("custom:play-guitar-loud", t.id)
        assertEquals("Play guitar loud", t.title)
        assertEquals(Tasks.CUSTOM_REWARD, t.rewardMinutes)
        assertTrue(t.manual)
    }

    @Test fun `companion line looks back at what we did today`() {
        val picks = listOf(Tasks.MOVE, Tasks.LEARN, Tasks.HOBBY)
        var d = Budget.credit(cfg, day, Tasks.MOVE.id, 30, manual = false)
        assertEquals("Today we already moved. Nice start!", Moods.line(cfg, d, picks))
        d = Budget.credit(cfg, d, Tasks.LEARN.id, 20, manual = false)
        assertEquals("Today we already moved and learned something. Look at us!", Moods.line(cfg, d, picks))
        d = Budget.credit(cfg, d, Tasks.HOBBY.id, 15, manual = true)
        assertEquals("Today we already moved, learned something and played with a hobby. Everything's done. Proud of us.", Moods.line(cfg, d, picks))
    }

    @Test fun `finished tasks sink to the bottom`() {
        val picks = listOf(Tasks.MOVE, Tasks.READ, Tasks.HOBBY)
        val d = Budget.credit(cfg, day, Tasks.MOVE.id, 30, manual = false)
        assertEquals(listOf(Tasks.READ, Tasks.HOBBY, Tasks.MOVE), Tasks.ordered(d, picks))
    }

    @Test fun `companion line follows the user's own picks`() {
        assertEquals("What shall we do today? Hobby sounds nice.", Moods.line(cfg, day, listOf(Tasks.HOBBY)))
        assertEquals("We haven't been outside in a while. Quick walk?", Moods.line(cfg, day, listOf(Tasks.OUTSIDE)))
    }

    @Test fun `undo takes back exactly what a check-off granted`() {
        val small = cfg.copy(manualCapMinutes = 20)
        var d = Budget.credit(small, day, "friends", 15, manual = true)
        d = Budget.credit(small, d, "hobby", 15, manual = true) // capped: only 5 granted
        assertEquals(20, d.manualEarnedMinutes)
        d = Budget.undo(d, "hobby", manual = true)
        assertEquals(15, d.earnedMinutes)
        assertEquals(15, d.manualEarnedMinutes)
        assertFalse("hobby" in d.completedTasks)
        assertEquals(d, Budget.undo(d, "hobby", manual = true))
    }

    @Test fun `status names the companion's mood`() {
        assertEquals("Bo is happy", Moods.status("Bo", Mood.HAPPY))
        assertEquals("Mia is sleepy", Moods.status("Mia", Mood.TIRED))
    }
}
