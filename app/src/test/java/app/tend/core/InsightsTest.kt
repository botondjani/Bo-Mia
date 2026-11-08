package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class InsightsTest {
    private val today = LocalDate.of(2026, 11, 11) // a Wednesday
    private fun d(ago: Long, used: Int, tasks: Int = 0, paused: Boolean = false) = DaySummary(today.minusDays(ago), used, 0, tasks, paused = paused)

    @Test fun `won back counts closed days below the normal, not today, not breaks`() {
        val i = InsightsOf.of(listOf(d(3, 60), d(2, 150), d(1, 0, paused = true), d(0, 10)), normal = 120, notNows = 4, today = today)
        assertEquals(60, i.wonBack) // 120-60, the 150 day adds nothing, the break and today don't count
        assertEquals(today.minusDays(3), i.since)
        assertEquals(105, i.lastWeek) // (60 + 150) / 2
        assertEquals(4, i.notNows)
    }

    @Test fun `without a normal nothing is claimed`() {
        assertEquals(0, InsightsOf.of(listOf(d(2, 30)), normal = null, notNows = 0, today = today).wonBack)
    }

    @Test fun `weeks start on Monday, oldest first, empty ones stay empty`() {
        val i = InsightsOf.of(listOf(d(9, 100), d(8, 80), d(1, 40)), normal = 120, notNows = 0, today = today, weeks = 3)
        assertEquals(listOf(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9)), i.weeks.map { it.first })
        assertNull(i.weeks[0].second)
        assertEquals(90, i.weeks[1].second)
        assertEquals(40, i.weeks[2].second)
    }

    @Test fun `activities and active days`() {
        val i = InsightsOf.of(listOf(d(2, 0, tasks = 2), d(1, 0), d(0, 0, tasks = 1)), normal = 60, notNows = 0, today = today)
        assertEquals(3, i.activities)
        assertEquals(2, i.activeDays)
    }

    @Test fun `hours and what they are like`() {
        assertEquals("45 min", InsightsOf.hours(45))
        assertEquals("2 h", InsightsOf.hours(120))
        assertEquals("31 h 20 min", InsightsOf.hours(1880))
        assertNull(InsightsOf.likeWhat(60))
        assertEquals("about 4 walks in the woods", InsightsOf.likeWhat(180))
        assertEquals("about 5 books", InsightsOf.likeWhat(1880))
    }
}
