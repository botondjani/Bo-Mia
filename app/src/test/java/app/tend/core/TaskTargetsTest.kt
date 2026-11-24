package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskTargetsTest {
    @Test fun goalIsKeptInRangeAndSaid() {
        val t = Tasks.withTarget(Tasks.OUTSIDE, 99999)
        assertEquals(15000, t.target)
        assertTrue(t.subtitle, t.subtitle.contains("15"))
        assertEquals(510, Tasks.withTarget(Tasks.SLEEP, 510).target)
        assertNull(Tasks.targetRange(Tasks.CHORES.copy(id = "custom:x")))
    }

    @Test fun healthCheckUsesThePickedGoal() {
        val picked = listOf(Tasks.withTarget(Tasks.OUTSIDE, 6000), Tasks.withTarget(Tasks.MOVE, 15))
        val done = completedAutomaticTasks(steps = 5000, exerciseMinutes = 20, selected = picked, sleepMinutes = null).map { it.id }
        assertEquals(listOf(Tasks.MOVE.id), done)
    }
}
