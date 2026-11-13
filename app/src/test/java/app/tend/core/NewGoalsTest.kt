package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NewGoalsTest {
    private val cfg = BudgetConfig()
    private val day = DayState(LocalDate.of(2026, 10, 31))
    private val min = 60_000L

    @Test fun `only time in other apps counts, not a screen that is just on`() {
        val open = setOf("app.tend", "launcher", "com.strava")
        val fg = PhoneFree.Kind.FOREGROUND; val bg = PhoneFree.Kind.BACKGROUND; val off = PhoneFree.Kind.SCREEN_OFF
        val t = listOf(
            PhoneFree.Event(1 * min, off),                    // locked
            PhoneFree.Event(5 * min, fg, "launcher"),         // screen on, home screen: nothing
            PhoneFree.Event(6 * min, fg, "com.strava"),       // the tracker: nothing
            PhoneFree.Event(7 * min, bg, "com.strava"),
            PhoneFree.Event(7 * min, fg, "insta"),            // 2 min of scrolling
            PhoneFree.Event(9 * min, off),
            PhoneFree.Event(20 * min, fg, "insta"),           // still open at the end: 5 min more
        )
        assertEquals(7 * min, PhoneFree.appUseMillis(0, 25 * min, t, open))
        assertEquals(0L, PhoneFree.appUseMillis(0, 25 * min, emptyList(), open))
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
        assertEquals(setOf(Accessory.LEAF, Accessory.BOWTIE, Accessory.HEADBAND, Accessory.FLOWER), Accessories.active(week))
        assertEquals(emptySet<Accessory>(), Accessories.active(week.map { emptySet() }))
        // Quick ones for new users: the leaf after one activity, the bow tie after two active days.
        assertEquals(setOf(Accessory.LEAF), Accessories.active(listOf(setOf("read"))))
        // Sleep earns a nightcap after 2 nights.
        val nights = List(2) { setOf("sleep") }
        assertTrue(Accessory.NIGHTCAP in Accessories.active(nights) && Accessory.NIGHTCAP !in Accessories.active(nights.take(1)))
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

    @org.junit.Test fun workoutWithoutTrackerIsPhoneFree() {
        org.junit.Assert.assertTrue(PhoneFree.runsPhoneFree(Tasks.MOVE))
        org.junit.Assert.assertFalse(PhoneFree.runsPhoneFree(Tasks.READ))
        val start = 0L; val end = 30 * 60_000L
        // A workout gets the same 3 minutes as the rest, and its trackers don't count at all.
        org.junit.Assert.assertEquals(PhoneFree.Outcome.DONE, PhoneFree.outcome(Tasks.MOVE, start, end, 150_000))
        org.junit.Assert.assertEquals(PhoneFree.Outcome.BROKEN, PhoneFree.outcome(Tasks.MOVE, start, end, 200_000))
        org.junit.Assert.assertTrue("com.strava" in app.tend.monitor.FocusApps.extra(Tasks.MOVE))
        org.junit.Assert.assertTrue(app.tend.monitor.FocusApps.extra(Tasks.READ).isEmpty())
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

class WelcomeBackTest {
    @org.junit.Test fun backAfterAFewDaysStartsSmall() {
        val day = DayState(java.time.LocalDate.of(2026, 11, 1))
        val line = Moods.line(BudgetConfig(), day, listOf(Tasks.READ), welcomeBack = true)
        org.junit.Assert.assertTrue(line, line.startsWith("Welcome back!") && "read" in line)
        // Once something is done, the usual praise takes over.
        val done = day.copy(completedTasks = setOf("read"))
        org.junit.Assert.assertTrue(Moods.line(BudgetConfig(), done, listOf(Tasks.READ), welcomeBack = true).startsWith("Today we already"))
    }

    @Test fun `accessories go on and off, one at a time on top of the head`() {
        val earned = setOf(Accessory.HAT, Accessory.NIGHTCAP, Accessory.LEAF, Accessory.GLASSES)
        // Nothing chosen yet: the hat is on top, the glasses with it.
        assertEquals(setOf(Accessory.HAT, Accessory.GLASSES), Accessories.worn(earned, emptySet()))
        // Tap the nightcap: it takes the hat's place.
        var off = Accessories.toggle(Accessory.NIGHTCAP, earned, emptySet())
        assertEquals(setOf(Accessory.NIGHTCAP, Accessory.GLASSES), Accessories.worn(earned, off))
        // Tap it again: off, and nothing else jumps on by itself.
        off = Accessories.toggle(Accessory.NIGHTCAP, earned, off)
        assertEquals(setOf(Accessory.GLASSES), Accessories.worn(earned, off))
        // Glasses off and on again.
        off = Accessories.toggle(Accessory.GLASSES, earned, off)
        assertTrue(Accessories.worn(earned, off).isEmpty())
        off = Accessories.toggle(Accessory.GLASSES, earned, off)
        assertEquals(setOf(Accessory.GLASSES), Accessories.worn(earned, off))
        // One not earned can't be put on.
        assertEquals(off, Accessories.toggle(Accessory.BOWTIE, earned, off))
    }

    @Test fun `tips take turns, every other day, a few days each`() {
        val all = listOf(Tip.MOVE, Tip.WIDGET, Tip.TOWN)
        assertEquals(null, Tip.today(all, emptyMap(), 101))          // an odd day: none
        assertEquals(Tip.TOWN, Tip.today(all, emptyMap(), 100))      // 100 / 2 = 50, 50 % 3 = 2: the third
        val seen = (0L until 12 step 2).mapNotNull { Tip.today(all, emptyMap(), it) }
        assertEquals(setOf(Tip.MOVE, Tip.WIDGET, Tip.TOWN), seen.toSet())
        // Shown enough: it steps aside; nothing left: no tip.
        assertTrue(Tip.today(all, mapOf(Tip.MOVE to 3), 0) != Tip.MOVE)
        assertEquals(null, Tip.today(all, all.associateWith { 3 }, 0))
        assertEquals(null, Tip.today(emptyList(), emptyMap(), 0))
    }

    @Test fun `a streak forgives one missed day a week, not two in a row`() {
        val t = LocalDate.of(2026, 11, 20)
        fun days(vararg ago: Int) = ago.map { t.minusDays(it.toLong()) }.toSet()
        // Five days running, today not done yet: still five.
        assertEquals(Streak.State(5, kept = false, spare = true), Streak.of(days(1, 2, 3, 4, 5), t))
        // Today done too.
        assertEquals(6, Streak.of(days(0, 1, 2, 3, 4, 5), t).days)
        // Yesterday missed: kept alive, the missed day isn't counted, and the spare is used.
        assertEquals(Streak.State(3, kept = true, spare = false), Streak.of(days(2, 3, 4), t))
        // Two missed in a row: over.
        assertEquals(0, Streak.of(days(3, 4, 5), t).days)
        // Two single gaps within a week: only the nearer one is bridged.
        assertEquals(2, Streak.of(days(1, 3, 5, 6), t).days)
        // Gaps a week apart are both bridged.
        assertEquals(10, Streak.of(days(0, 1, 2, 4, 5, 6, 7, 8, 9, 11), t).days)
        // Nothing at all.
        assertEquals(Streak.State(0, kept = false, spare = true), Streak.of(emptySet(), t))
    }
}
