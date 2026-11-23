package app.tend.monitor

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerPickTest {
    @Test fun lastLengthStaysOrTakesNearestPlace() {
        assertEquals(listOf(15, 25, 45, 60), TimerPickActivity.lengths(25))
        assertEquals(listOf(20, 25, 45, 60), TimerPickActivity.lengths(20))
        assertEquals(listOf(15, 25, 45, 90), TimerPickActivity.lengths(90))
    }
}
