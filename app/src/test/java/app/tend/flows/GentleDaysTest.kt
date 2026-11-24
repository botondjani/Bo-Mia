package app.tend.flows

import app.tend.core.*
import app.tend.data.TendStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GentleDaysTest {
    @Test fun firstTwoDaysAreGentle() {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication()
        val s = TendStore(ctx)
        // A lock that covers every hour, every day.
        s.setLockSchedule(LockSchedule.WORK.copy(on = true, from = java.time.LocalTime.now().hour, to = (java.time.LocalTime.now().hour + 1) % 24, days = LockSchedule.ALL_DAYS))
        val day = 24 * 3_600_000L
        s.onboardedAt = System.currentTimeMillis()
        assertEquals(1, s.dayOfUse); assertTrue(s.gentleDays); assertNull(s.activeLock("x"))
        s.onboardedAt = System.currentTimeMillis() - day
        assertEquals(2, s.dayOfUse); assertTrue(s.gentleDays); assertNull(s.activeLock("x"))
        s.onboardedAt = System.currentTimeMillis() - 2 * day
        assertEquals(3, s.dayOfUse); assertFalse(s.gentleDays); assertNotNull(s.activeLock("x"))
        // Installs from before this was kept: never gentle.
        s.onboardedAt = 0
        assertEquals(0, s.dayOfUse); assertFalse(s.gentleDays)
    }
}
