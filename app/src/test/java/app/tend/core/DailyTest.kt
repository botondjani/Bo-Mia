package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyTest {
    @Test fun visitorIsTheSameAllDayAndRareOnlyAfterAGoodDay() {
        val day = LocalDate.of(2026, 11, 7)
        assertEquals(Visitors.of(day, true), Visitors.of(day, true))
        val year = (0L until 365L).map { day.plusDays(it) }
        assertTrue(year.none { Visitors.of(it, goodDay = false).rare })
        val rare = year.count { Visitors.of(it, goodDay = true).rare }
        assertTrue("rare $rare", rare in 45..105) // about a fifth of good days
        val common = year.count { Visitors.of(it, false).rarity == Rarity.COMMON }
        assertTrue("common $common", common in 220..290) // most days
        assertEquals(Visitor.entries.filter { !it.rare }.toSet(), year.map { Visitors.of(it, false) }.toSet())
    }

    @Test fun guestbookRoundTrip() {
        val book = mapOf(Visitor.DEER to 3, Visitor.OWL to 1)
        assertEquals(book, Visitors.parse(Visitors.format(book)))
        assertEquals(mapOf(Visitor.FOX to 2), Visitors.parse("FOX:2,BEAR:1,junk"))
    }

    @Test fun firstSteps() {
        val s = FirstSteps.of(setOf(Step.PLACE), emptySet())
        assertTrue(s.first { it.step == Step.PLACE }.claimable)
        assertFalse(s.first { it.step == Step.USE }.done)
        assertTrue(FirstSteps.finished(FirstSteps.of(emptySet(), Step.entries.toSet())))
        // Rounds: the next one opens when this one is all claimed.
        assertEquals(0, FirstSteps.current(FirstSteps.of(emptySet(), emptySet())).first().step.round)
        assertEquals(1, FirstSteps.current(FirstSteps.of(emptySet(), Step.entries.filter { it.round == 0 }.toSet())).first().step.round)
        assertTrue(FirstSteps.current(FirstSteps.of(emptySet(), Step.entries.toSet())).isEmpty())
        // Day one: the start plus the first steps buys a campfire, a bench and more.
        assertTrue(Clearing.STARTING_ACORNS + Step.entries.filter { it.round == 0 }.sumOf { it.acorns } >= 40 + 50)
    }
}
