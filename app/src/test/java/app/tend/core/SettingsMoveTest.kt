package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsMoveTest {
    @Test fun `the old switches become the new settings`() {
        val strict = SettingsMove.from(strict = true, bedtimeLock = true, sleepyPauses = true, morning = true, nudges = true, recap = true, letter = true, pauseOff = setOf("a"))
        assertEquals(StrictLevel.STRICT, strict.strict)
        assertEquals(EveningMode.LOCK, strict.evening)
        assertEquals(NotesLevel.NORMAL, strict.notes)
        assertEquals(WeekendMode.ASK, strict.weekend)
        assertEquals(setOf("a"), strict.countOnly)

        val easy = SettingsMove.from(strict = false, bedtimeLock = false, sleepyPauses = true, morning = false, nudges = false, recap = false, letter = false, pauseOff = emptySet())
        assertEquals(StrictLevel.BALANCED, easy.strict)
        assertEquals(EveningMode.SLOW, easy.evening)
        assertEquals(NotesLevel.QUIET, easy.notes)

        assertEquals(EveningMode.OFF, SettingsMove.from(false, false, false, true, false, false, false, emptySet()).evening)
    }
}
