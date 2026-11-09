package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuestsTest {
    private val day = LocalDate.of(2026, 11, 9)
    private val mine = listOf(Tasks.READ, Tasks.OUTSIDE)

    @Test fun `the same quest all day, and different ones over the days`() {
        assertEquals(Quests.of(day, mine, 45), Quests.of(day, mine, 45))
        assertTrue((0L until 20L).map { Quests.of(day.plusDays(it), mine, 45).id }.toSet().size >= 3)
    }

    @Test fun `each kind is done by what it asks for`() {
        val read = Quest("do:read", "", "", 15)
        assertTrue(Quests.done(read, DayState(day, completedTasks = setOf("read")), 0, 10))
        assertFalse(Quests.done(read, DayState(day, completedTasks = setOf("outside")), 0, 10))
        assertTrue(Quests.done(Quest("two", "", "", 20), DayState(day, completedTasks = setOf("read", "outside")), 0, 10))
        assertTrue(Quests.done(Quest("notnow", "", "", 10), DayState(day), 2, 10))
        val under = Quest("under:45", "", "", 20)
        assertFalse("not before 20:00", Quests.done(under, DayState(day, usedSeconds = 600), 0, 19))
        assertTrue(Quests.done(under, DayState(day, usedSeconds = 600), 0, 20))
        assertFalse(Quests.done(under, DayState(day, usedSeconds = 50 * 60L), 0, 21))
    }
}
