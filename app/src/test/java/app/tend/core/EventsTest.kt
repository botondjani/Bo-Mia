package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EventsTest {
    @Test fun `events are on their days only, with days left counted`() {
        assertEquals(CampEvent.LANTERNS, CampEvent.on(LocalDate.of(2026, 11, 9)))
        assertEquals(4, CampEvent.LANTERNS.daysLeft(LocalDate.of(2026, 11, 9)))
        assertEquals(1, CampEvent.LANTERNS.daysLeft(LocalDate.of(2026, 11, 12)))
        assertNull(CampEvent.on(LocalDate.of(2026, 11, 13)))
        assertEquals(CampEvent.ADVENT, CampEvent.on(LocalDate.of(2026, 12, 24)))
        assertEquals(CampEvent.PUMPKINS, CampEvent.on(LocalDate.of(2026, 10, 31)))
    }

    @Test fun `each event has its own thing, kept out of the everyday shop`() {
        CampEvent.entries.forEach { e -> assertEquals(e.thing, Things.forEvent(e).single().id) }
        assertTrue(Things.forKind(SpotKind.SMALL).none { it.event != null })
        assertTrue(CampEvent.entries.all { e -> Things.forEvent(e).all { Clearing.movable(it) } })
    }
}
