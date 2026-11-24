package app.tend.flows

import app.tend.core.*
import app.tend.data.TendStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each setting, changed, does what it says where it matters. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsWorkTest {
    private val ctx = org.robolectric.RuntimeEnvironment.getApplication()
    private val s = TendStore(ctx)

    @Test fun strictnessSetsTheWait() {
        s.strictLevel = StrictLevel.EASY
        assertEquals(3, Budget.openDelaySeconds(s.effectiveStrict, 1))
        s.strictLevel = StrictLevel.BALANCED
        assertEquals(5, Budget.openDelaySeconds(s.effectiveStrict, 1))
        // Strict without Plus: Balanced.
        s.strictLevel = StrictLevel.STRICT
        if (!s.isPlus) assertEquals(StrictLevel.BALANCED, s.effectiveStrict)
    }

    @Test fun eveningModeClosesOrSlows() {
        s.eveningMode = EveningMode.LOCK
        assertTrue(s.lockSchedules.first { it.id == "bed" }.on)
        s.eveningMode = EveningMode.SLOW
        assertFalse(s.lockSchedules.first { it.id == "bed" }.on); assertTrue(s.eveningSlows)
        s.eveningMode = EveningMode.OFF
        assertFalse(s.eveningSlows)
    }

    @Test fun appModes() {
        s.blockedPackages = setOf("a.b")
        assertTrue(s.pausesOnOpen("a.b"))
        s.setAppMode("a.b", AppMode.COUNT)
        assertFalse(s.pausesOnOpen("a.b"))
        s.setAppMode("a.b", AppMode.STOP)
        assertTrue(s.pausesOnOpen("a.b"))
    }

    @Test fun notesLevel() {
        s.applyNotesLevel(NotesLevel.QUIET)
        assertFalse(s.notesMorning || s.notesNudges || s.reminderEnabled || s.weeklyLetter)
        s.applyNotesLevel(NotesLevel.NORMAL)
        assertTrue(s.notesMorning && s.notesNudges && s.reminderEnabled && s.weeklyLetter)
    }

    @Test fun locksAndDayOff() {
        val h = java.time.LocalTime.now().hour
        s.setLockSchedule(LockSchedule.WORK.copy(on = true, from = h, to = (h + 1) % 24, days = LockSchedule.ALL_DAYS, apps = setOf("a.b")))
        assertNotNull(s.activeLock("a.b")); assertNull(s.activeLock("c.d"))
        s.pause(1)
        assertNull(s.activeLock("a.b"))
    }

    @Test fun budget() {
        s.baseConfig = s.baseConfig.copy(freeMinutes = 30)
        assertEquals(30, s.config.freeMinutes)
    }
}
