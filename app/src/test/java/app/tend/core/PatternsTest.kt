package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PatternsTest {
    private val today = LocalDate.of(2026, 10, 31)
    private fun at(daysAgo: Long, hour: Int) = today.minusDays(daysAgo).atTime(hour, 15)
    private fun worth(t: LocalDateTime, a: String, pkg: String = "insta") = Answer(t, "worth_it", pkg, a)

    @Test fun `day parts split the clock`() {
        assertEquals(DayPart.MORNING, DayPart.of(7))
        assertEquals(DayPart.AFTERNOON, DayPart.of(12))
        assertEquals(DayPart.EVENING, DayPart.of(21))
        assertEquals(DayPart.LATE, DayPart.of(23))
        assertEquals(DayPart.LATE, DayPart.of(2))
    }

    @Test fun `week keeps the last seven days and sums what was got back`() {
        val days = (0L..9L).map { DaySummary(today.minusDays(it), 30, 10, 1) }
        val p = Patterns.week(today, days, emptyList())
        assertEquals(7, p.days.size)
        assertEquals(today.minusDays(6), p.days.first().date)
        assertEquals(70, p.gotBackMinutes)
    }

    @Test fun `late night regrets become the insight`() {
        val answers = listOf(
            worth(at(0, 23), "Not really"), worth(at(1, 23), "Not really"), worth(at(2, 0), "Not really"),
            worth(at(3, 23), "Yes"), worth(at(1, 9), "Yes"), worth(at(10, 23), "Not really"),
        )
        val p = Patterns.week(today, emptyList(), answers)
        assertEquals(WorthSplit(yes = 1, not = 3), p.byDayPart[DayPart.LATE])
        assertEquals("Late night scrolls felt worth it least: 3 of 4 were \"not really\".", p.insight)
    }

    @Test fun `no insight from too few answers`() {
        assertNull(Patterns.week(today, emptyList(), listOf(worth(at(0, 23), "Not really"))).insight)
    }

    @Test fun `apps ranked by answers, looking-for counted`() {
        val answers = listOf(
            worth(at(0, 10), "Yes", "yt"), worth(at(0, 11), "Not really", "insta"), worth(at(0, 12), "Kind of", "insta"),
            Answer(at(0, 13), "looking_for", "insta", "Just bored"), Answer(at(1, 13), "looking_for", "insta", "Just bored"),
        )
        val p = Patterns.week(today, emptyList(), answers)
        assertEquals(listOf("insta", "yt"), p.byApp.map { it.first })
        assertEquals(mapOf("Just bored" to 2), p.lookingFor)
    }

    @Test fun `the week has seven days with today last, gaps kept, averaged against the week before`() {
        fun d(ago: Long, used: Int) = DaySummary(today.minusDays(ago), used, 0, 0)
        val v = WeekOverview.of(listOf(d(13, 100), d(8, 80), d(5, 60), d(1, 40), d(0, 20)), today)
        assertEquals(7, v.days.size)
        assertEquals(today, v.days.last()!!.date)
        assertNull(v.days[0])
        assertEquals(50, v.average)
        assertEquals(90, v.before)
        assertNull(WeekOverview.of(emptyList(), today).average)
    }

    @Test fun `days on a break are left out of the week`() {
        val v = WeekOverview.of(listOf(DaySummary(today.minusDays(1), 0, 45, 0, paused = true), DaySummary(today.minusDays(2), 60, 0, 0)), today)
        assertNull(v.days[5])
        assertEquals(60, v.average)
    }
}
