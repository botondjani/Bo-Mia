package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeatherTest {
    @Test fun `the same all day, fog only before ten`() {
        val d = LocalDate.of(2026, 11, 8)
        assertEquals(Weather.of(d, 11), Weather.of(d, 20))
        (0 until 365).map { LocalDate.of(2026, 1, 1).plusDays(it.toLong()) }.forEach { day ->
            assertTrue(Weather.of(day, 14) != Weather.FOG)
        }
    }

    @Test fun `snow only in winter, rain never in winter, and every season has clear days`() {
        val year = (0 until 365).map { LocalDate.of(2026, 1, 1).plusDays(it.toLong()) }
        year.forEach { day ->
            val w = Weather.of(day, 8)
            if (Season.of(day) != Season.WINTER) assertTrue(w != Weather.SNOW)
            else assertTrue(w != Weather.RAIN)
        }
        Season.entries.forEach { s -> assertTrue(year.filter { Season.of(it) == s }.any { Weather.of(it, 12) == Weather.CLEAR }) }
        // Not all one thing: a mix over the year.
        assertTrue(year.map { Weather.of(it, 8) }.toSet().size == Weather.entries.size)
    }
}
