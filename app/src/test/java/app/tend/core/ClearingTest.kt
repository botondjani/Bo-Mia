package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearingTest {
    @Test fun catalogue() {
        assertEquals("ids are unique", Things.all.size, Things.all.map { it.id }.toSet().size)
        // Something is always in reach: at least three small things a day's activity pays for.
        assertTrue(Things.all.count { it.price <= Acorns.PER_ACTIVITY + 5 } >= 3)
        // Every kind of spot has things, and every thing has a spot.
        SpotKind.entries.forEach { k -> assertTrue("$k has things", Things.forKind(k).isNotEmpty()) }
        assertTrue(Things.all.all { t -> Clearing.spots.any { it.kind == t.kind } })
        assertEquals(10, Clearing.spots.count { it.kind != SpotKind.SMALL })
        assertEquals(8, Clearing.spots.count { it.kind == SpotKind.SMALL })
        assertEquals(Clearing.spots.size, Clearing.spots.map { it.id }.toSet().size)
    }

    @Test fun placementsRoundTrip() {
        val placed = mapOf(0 to "grill", 5 to "hammock", 10 to "wildflowers", 11 to "wildflowers")
        assertEquals(placed, Clearing.parse(Clearing.format(placed)))
        // Unknown things, unknown spots and things in the wrong kind of spot are dropped.
        assertEquals(mapOf(0 to "grill"), Clearing.parse("0:grill,1:boat,99:flowers,2:nonsense,junk"))
    }

    @Test fun acornsDontDependOnBudgetSize() {
        // Two people, same day (60 min used, normal 120): one with a 2-hour budget, one with 4 hours.
        assertEquals(Acorns.atDayClose(60, 120, 120), Acorns.atDayClose(60, 240, 120))
        assertEquals(10 + 6, Acorns.atDayClose(60, 120, 120))
        // Over budget, above normal: nothing at day close.
        assertEquals(0, Acorns.atDayClose(200, 120, 150))
        // Way below normal: capped.
        assertEquals(10 + Acorns.BELOW_NORMAL_CAP, Acorns.atDayClose(0, 60, 600))
        // Normal not known yet: only the budget part.
        assertEquals(10, Acorns.atDayClose(30, 60, null))
    }

    @Test fun dailyCaps() {
        assertEquals(30, Acorns.forActivities(3))
        assertEquals(Acorns.ACTIVITY_CAP, Acorns.forActivities(9))
        assertEquals(6, Acorns.forNotNows(3))
        assertEquals(Acorns.NOT_NOW_CAP, Acorns.forNotNows(40))
    }

    @Test fun normalNeedsAWeek() {
        assertNull(Acorns.normalFrom(listOf(60, 70, 80)))
        assertEquals(70, Acorns.normalFrom(listOf(70, 70, 70, 70, 70, 70, 70, 10)))
    }

    @Test fun oldPlantsBecomeAcorns() {
        assertEquals(15 + 25 + 40, Clearing.acornsFor(listOf(Plant.SPROUT, Plant.TULIP, Plant.SUNFLOWER)))
        assertTrue(Things.forKind(SpotKind.FIRE).first().price <= Clearing.STARTING_ACORNS + Acorns.ACTIVITY_CAP)
    }

    @Test fun shopPlacesThingsByItself() {
        val fire = Things.byId("campfire")!!
        assertEquals(0, Clearing.spotFor(emptyMap(), fire))
        assertEquals(null, Clearing.spotFor(mapOf(0 to "campfire"), fire)) // already there
        assertEquals(0, Clearing.spotFor(mapOf(0 to "campfire"), Things.byId("grill")!!)) // the one fire pit: swapped
        val tent = Things.byId("tent")!!
        assertEquals(4, Clearing.spotFor(mapOf(3 to "cabin"), tent)) // a free shelter
        assertEquals(3, Clearing.spotFor(mapOf(3 to "tent", 4 to "cabin"), Things.byId("bell_tent")!!)) // swaps the cheaper
        assertEquals(10, Clearing.spotFor(emptyMap(), Things.byId("lantern")!!))
    }
}
