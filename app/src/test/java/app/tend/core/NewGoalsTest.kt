package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NewGoalsTest {
    private val cfg = BudgetConfig()
    private val day = DayState(LocalDate.of(2026, 10, 31))
    private val min = 60_000L

    @Test fun `screen-on time adds up the on stretches`() {
        // on from start, off at 2 min, on 10..11 min, off at 11, still off at 30 min
        val t = listOf(2 * min to false, 10 * min to true, 11 * min to false)
        assertEquals(3 * min, PhoneFree.screenOnMillis(0, 30 * min, t))
        // never locked: all of it counts
        assertEquals(30 * min, PhoneFree.screenOnMillis(0, 30 * min, emptyList()))
    }

    @Test fun `phone-free session is done, running or broken`() {
        assertEquals(PhoneFree.Outcome.DONE, PhoneFree.outcome(Tasks.FRIENDS, 0, 30 * min, 2 * min))
        assertEquals(PhoneFree.Outcome.RUNNING, PhoneFree.outcome(Tasks.FRIENDS, 0, 20 * min, 2 * min))
        assertEquals(PhoneFree.Outcome.BROKEN, PhoneFree.outcome(Tasks.FRIENDS, 0, 20 * min, 4 * min))
    }

    @Test fun `phone-free tasks are measured, not capped check-offs`() {
        assertTrue(!Tasks.FRIENDS.manual && Tasks.FRIENDS.verification.timed)
        val d = Budget.credit(cfg.copy(manualCapMinutes = 0), day, Tasks.HOBBY.id, 15, manual = Tasks.HOBBY.manual)
        assertEquals(15, d.earnedMinutes)
    }

    @Test fun `seven hours of sleep completes the sleep goal`() {
        var d = Budget.applyActivity(cfg, day, null, null, listOf(Tasks.SLEEP), sleepMinutes = 400)
        assertEquals(emptySet<String>(), d.completedTasks)
        d = Budget.applyActivity(cfg, d, null, null, listOf(Tasks.SLEEP), sleepMinutes = 430)
        assertEquals(setOf("sleep"), d.completedTasks)
        assertEquals(20, d.earnedMinutes)
    }

    @Test fun `accessories follow a week of habits`() {
        val week = listOf(setOf("move"), setOf("move", "read"), setOf("read"), setOf("move"), setOf("move", "friends"), setOf("hobby"), setOf("friends"))
        assertEquals(setOf(Accessory.LEAF, Accessory.BOWTIE, Accessory.HEADBAND, Accessory.FLOWER, Accessory.BUNTING), Accessories.active(week))
        assertEquals(emptySet<Accessory>(), Accessories.active(week.map { emptySet() }))
        // Quick ones for new users: the leaf after one activity, the bow tie after two active days.
        assertEquals(setOf(Accessory.LEAF), Accessories.active(listOf(setOf("read"))))
        // Sleep earns a nightcap after 2 nights and lanterns for the garden after 4.
        val nights = List(4) { setOf("sleep") }
        assertTrue(Accessory.NIGHTCAP in Accessories.active(nights.take(2)) && Accessory.LANTERNS !in Accessories.active(nights.take(3)))
        assertTrue(Accessory.LANTERNS in Accessories.active(nights) && Accessory.LANTERNS.garden)
        assertEquals(setOf(Accessory.LEAF, Accessory.BOWTIE), Accessories.active(listOf(setOf("custom:journal"), emptySet(), setOf("read"))))
    }
}

class PauseAndEveningTest {
    private val today = java.time.LocalDate.of(2026, 11, 1)

    @Test fun `pause decisions are counted for the week`() {
        fun pause(d: Long, a: String) = Answer(today.minusDays(d).atTime(21, 0), "pause", "insta", a)
        val p = Patterns.week(today, emptyList(), listOf(pause(0, "open"), pause(1, "not_now"), pause(2, "instead"), pause(3, "open"), pause(9, "not_now")))
        assertEquals(4, p.pauses)
        assertEquals(2, p.letGo)
    }

    @Test fun `evening mode waits longer and runs past midnight`() {
        val cfg = BudgetConfig()
        val day = DayState(today, opensToday = 1)
        assertEquals(6, Budget.openDelaySeconds(cfg, day))
        assertEquals(15, Budget.openDelaySeconds(cfg, day, evening = true))
        assertTrue(Budget.isEvening(22, 23) && Budget.isEvening(22, 1))
        assertTrue(!Budget.isEvening(22, 12) && !Budget.isEvening(-1, 23))
    }
}

class FallbackTrackingTest {
    @org.junit.Test fun longestOffStretchStandsInForSleep() {
        val h = 3_600_000L
        // On at 20:00 (0), off 23:00, on 23:10, off 23:30, on 06:45 -> longest 7h15m.
        val t = listOf(3 * h to false, 3 * h + 600_000 to true, 3 * h + 1_800_000 to false, 10 * h + 2_700_000 to true)
        org.junit.Assert.assertEquals(7 * h + 15 * 60_000, PhoneFree.longestOffMillis(0, 16 * h, t))
        // Still off at the end of the window: counts up to it.
        org.junit.Assert.assertEquals(2 * h, PhoneFree.longestOffMillis(0, 5 * h, listOf(3 * h to false)))
        org.junit.Assert.assertEquals(0L, PhoneFree.longestOffMillis(0, 5 * h, emptyList()))
    }

    @org.junit.Test fun workoutWithoutTrackerIsStrictlyPhoneOff() {
        org.junit.Assert.assertTrue(PhoneFree.runsPhoneFree(Tasks.MOVE))
        org.junit.Assert.assertFalse(PhoneFree.runsPhoneFree(Tasks.READ))
        val start = 0L; val end = 30 * 60_000L
        org.junit.Assert.assertEquals(PhoneFree.Outcome.DONE, PhoneFree.outcome(Tasks.MOVE, start, end, 50_000))
        org.junit.Assert.assertEquals(PhoneFree.Outcome.BROKEN, PhoneFree.outcome(Tasks.MOVE, start, end, 90_000))
        // Friends & family keeps its 3 minutes.
        org.junit.Assert.assertEquals(PhoneFree.Outcome.DONE, PhoneFree.outcome(Tasks.FRIENDS, start, end, 90_000))
    }
}

class SharedRewardsTest {
    @org.junit.Test fun earnableMinutesAreSharedByEffort() {
        val cfg = BudgetConfig() // 45 free, 180 max: 135 to share
        val few = Tasks.withRewards(cfg, listOf(Tasks.MOVE, Tasks.CHORES))
        org.junit.Assert.assertEquals(listOf(100, 35), few.map { it.rewardMinutes }) // 30:10 weights of 135, rounded to 5
        val many = Tasks.withRewards(cfg, Tasks.presets + Tasks.custom("Journal", "pen"))
        org.junit.Assert.assertTrue(many.sumOf { it.rewardMinutes } in 125..145)
        org.junit.Assert.assertTrue(many.all { it.rewardMinutes >= 5 && it.rewardMinutes % 5 == 0 })
        // A copy with another reward is still the same activity.
        org.junit.Assert.assertTrue(Tasks.MOVE in few)
    }

    @org.junit.Test fun ownActivityCanBeATimer() {
        val t = Tasks.custom("Play music", "music", 30)
        org.junit.Assert.assertEquals(Verification.PHONE_FREE, t.verification)
        org.junit.Assert.assertEquals(30, t.target)
        org.junit.Assert.assertEquals(Verification.CHECK_OFF, Tasks.custom("Journal", "pen").verification)
    }

    @org.junit.Test fun automaticTasksCreditTheSharedReward() {
        val picked = Tasks.withRewards(BudgetConfig(), listOf(Tasks.MOVE, Tasks.OUTSIDE))
        val done = completedAutomaticTasks(steps = 5000, exerciseMinutes = 0, selected = picked)
        org.junit.Assert.assertEquals(picked.first { it == Tasks.OUTSIDE }.rewardMinutes, done.single().rewardMinutes)
    }
}

class EveningMoodTest {
    @org.junit.Test fun contentCompanionIsSleepyAtNight() {
        org.junit.Assert.assertEquals(Mood.SLEEPY, Moods.atNight(Mood.HAPPY, night = true))
        org.junit.Assert.assertEquals(Mood.SLEEPY, Moods.atNight(Mood.OUTSIDE, night = true))
        org.junit.Assert.assertEquals(Mood.SCROLLING, Moods.atNight(Mood.SCROLLING, night = true)) // still says what's going on
        org.junit.Assert.assertEquals(Mood.HAPPY, Moods.atNight(Mood.HAPPY, night = false))
        org.junit.Assert.assertEquals("Bo is sleepy", Moods.status("Bo", Mood.SLEEPY))
    }
}

class ScheduleTest {
    private fun at(day: Int, hour: Int) = java.time.LocalDateTime.of(2026, 11, day, hour, 30) // Nov 2 2026 is a Monday

    @org.junit.Test fun bedtimeWrapsPastMidnight() {
        val bed = LockSchedule.BEDTIME.copy(on = true)
        org.junit.Assert.assertTrue(bed.activeAt(at(2, 23)))
        org.junit.Assert.assertTrue(bed.activeAt(at(3, 6)))
        org.junit.Assert.assertFalse(bed.activeAt(at(3, 7)))
        org.junit.Assert.assertFalse(bed.activeAt(at(3, 22)))
        org.junit.Assert.assertFalse(LockSchedule.BEDTIME.activeAt(at(2, 23))) // off by default
    }

    @org.junit.Test fun workHoursOnWeekdaysOnly() {
        val work = LockSchedule.WORK.copy(on = true)
        org.junit.Assert.assertTrue(work.activeAt(at(2, 10)))   // Monday
        org.junit.Assert.assertFalse(work.activeAt(at(2, 17)))  // 17:30, after
        org.junit.Assert.assertFalse(work.activeAt(at(7, 10)))  // Saturday
        org.junit.Assert.assertEquals("Work hours", LockSchedule.activeOf(listOf(LockSchedule.BEDTIME, work), at(4, 11))?.label)
    }

    @org.junit.Test fun timeBoxEnds() {
        org.junit.Assert.assertEquals(300_000L, TimeBox.endsAt(0, 5))
        org.junit.Assert.assertEquals(Long.MAX_VALUE, TimeBox.endsAt(0, 0))
    }
}
