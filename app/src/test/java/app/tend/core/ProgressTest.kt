package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    @Test fun levels() {
        // Skills: level 2 after 40 XP, then 20 more each.
        assertEquals(Level(1, 20, 40), Progress.skillLevel(20))
        assertEquals(Level(3, 0, 80), Progress.skillLevel(100))
        // Overall: 20 x (level² - 1) in all.
        assertEquals(Level(1, 0, 60), Progress.level(0))
        assertEquals(Level(2, 0, 100), Progress.level(60))
        assertEquals(10, Progress.level(1980).level)
        assertEquals(19, Progress.level(7979).level)
        assertEquals(1, Progress.level(-5).level)
        // The unused share of a day is worth the most: a whole day's minutes left, 100; half, 50.
        assertEquals(100, Progress.xpUnused(DaySummary(java.time.LocalDate.now(), 0, 45, 0)))
        assertEquals(50, Progress.xpUnused(DaySummary(java.time.LocalDate.now(), 30, 30, 0)))
        assertEquals(0, Progress.xpUnused(DaySummary(java.time.LocalDate.now(), 30, 30, 0, paused = true)))
        assertEquals(24, Progress.xpPhoneDown(240))
    }

    @Test fun skillsFromDoneTasks() {
        val d = LocalDate.of(2026, 11, 2)
        val xp = Progress.skillXp(listOf(
            DaySummary(d, 0, 0, 3, setOf("move", "read", "sleep")),
            DaySummary(d.plusDays(1), 0, 0, 2, setOf("outside", "custom:Guitar")),
        ))
        assertEquals(40, xp[Skill.BODY])
        assertEquals(40, xp[Skill.MIND])
        assertEquals(20, xp[Skill.REST])
    }

    @Test fun challengeCountsThisWeekOnly() {
        val monday = LocalDate.of(2026, 11, 2)
        val sunday = monday.plusDays(6)
        val days = listOf(
            DaySummary(monday.minusDays(1), 0, 0, 1, setOf("move")), // last week
            DaySummary(monday, 0, 0, 1, setOf("move")),
            DaySummary(monday.plusDays(2), 0, 0, 2, setOf("move", "read")),
        )
        val c = Progress.challenge(sunday, listOf("move"), days)
        assertEquals(Progress.weekKey(sunday), c.weekKey)
        assertTrue(c.progress in 0..c.target + 10)
        // Same week, same challenge, whichever day it is asked.
        assertEquals(c.title, Progress.challenge(monday, listOf("move"), days).title)
        // Only what's this week counts: 2 workout days, 3 activities.
        when (c.title) {
            "3 workout days" -> assertEquals(2, c.progress)
            "10 activities this week" -> assertEquals(3, c.progress)
            "Something done on 5 days" -> assertEquals(2, c.progress)
        }
    }

    @Test fun togetherCountsThisWeeksActivities() {
        val wed = LocalDate.of(2026, 11, 4) // a Wednesday
        val days = listOf(
            DaySummary(wed.minusDays(3), 0, 0, 5), // last Sunday: not this week
            DaySummary(wed.minusDays(2), 0, 0, 2), // Monday
            DaySummary(wed, 0, 0, 3),
        )
        assertEquals(5, Together.activities(wed, days))
        assertEquals(24, Together.target(3))
        assertEquals(8, Together.target(0))
        // Steps: with two camps, 6 is bronze, 10 silver, 16 gold.
        assertEquals(null, Together.tier(5, 2))
        assertEquals(Together.Tier.BRONZE, Together.tier(6, 2))
        assertEquals(Together.Tier.SILVER, Together.next(9, 2))
        assertEquals(Together.Tier.GOLD, Together.tier(16, 2))
        assertEquals(null, Together.next(16, 2))
    }
}
