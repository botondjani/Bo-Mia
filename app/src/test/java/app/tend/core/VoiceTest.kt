package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VoiceTest {
    private val tasks = listOf(Tasks.MOVE, Tasks.OUTSIDE, Tasks.READ)
    private val tuesday = LocalDateTime.of(2026, 11, 10, 15, 0)
    private fun m(at: LocalDateTime = tuesday, done: Set<String> = emptySet(), used: Long = 0, vararg extra: (Voice.Moment) -> Voice.Moment): Voice.Moment {
        var x = Voice.Moment("Bo", at, BudgetConfig(), DayState(at.toLocalDate(), completedTasks = done, usedSeconds = used), tasks)
        extra.forEach { x = it(x) }
        return x
    }

    @Test fun `out of minutes comes first, then friends, then visitors`() {
        assertTrue(Voice.home(m(used = 99_999)).startsWith("Out of minutes"))
        assertTrue(Voice.home(m(extra = arrayOf({ it.copy(atFire = listOf("Anna"), visitor = Visitor.FOX) }))).contains("Anna"))
        assertTrue(Voice.home(m(extra = arrayOf({ it.copy(visitor = Visitor.FOX) }))).contains("fox"))
    }

    @Test fun `the same line for a few hours, then maybe another`() {
        val a = Voice.home(m(at = tuesday))
        assertEquals(a, Voice.home(m(at = tuesday.plusMinutes(50))))
        val day = (0 until 8).map { Voice.home(m(at = tuesday.withHour(0).plusHours(it * 3L))) }.toSet()
        assertTrue("varies through the day: $day", day.size > 1)
    }

    @Test fun `it remembers yesterday and a good run, and says what was done today`() {
        val week = (0 until 60).map { Voice.home(m(at = tuesday.plusHours(it * 3L), extra = arrayOf({ it.copy(yesterday = setOf("read"), streak = 4) }))) }
        assertTrue(week.any { it.contains("Yesterday we read") })
        assertTrue(week.any { it.contains("4 good days in a row") })
        assertTrue(Voice.home(m(done = setOf("move"))).startsWith("Today we already moved."))
        assertTrue(Voice.home(m(done = setOf("move", "outside", "read"))).startsWith("Today we already moved, went outside and read."))
    }

    @Test fun `the weather shows up`() {
        val rainy = (0 until 40).map { Voice.home(m(at = tuesday.plusHours(it * 3L), extra = arrayOf({ it.copy(weather = Weather.RAIN) }))) }
        assertTrue(rainy.any { it.contains("Rain on the tent") })
    }

    @Test fun `a run of days counts back from yesterday`() {
        val today = LocalDate.of(2026, 11, 10)
        val days = listOf(3L, 2L, 1L).map { DaySummary(today.minusDays(it), 0, 0, 1) } + DaySummary(today.minusDays(5), 0, 0, 1)
        assertEquals(3, Voice.streak(days, today))
        assertEquals(0, Voice.streak(listOf(DaySummary(today.minusDays(2), 0, 0, 1)), today))
    }
}
