package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MilestonesTest {
    @Test fun `milestones come with days, activities and hours back`() {
        assertTrue(Milestone.reached(Totals(0, 0, 0)).isEmpty())
        assertEquals(setOf(Milestone.FLAG), Milestone.reached(Totals(7, 3, 2)))
        assertEquals(setOf(Milestone.FLAG, Milestone.PATH_LIGHTS, Milestone.GARDEN), Milestone.reached(Totals(12, 30, 11)))
        assertEquals(Milestone.entries.toSet(), Milestone.reached(Totals(30, 100, 10)))
        assertEquals("4 of 7 days", Milestone.FLAG.label(Totals(4, 0, 0)))
        assertEquals("7 of 7 days", Milestone.FLAG.label(Totals(40, 0, 0)))
    }
}
