package app.tend.health

import android.content.Context
import app.tend.core.Budget
import app.tend.core.TaskDef
import app.tend.core.Tasks
import app.tend.data.TendStore

/** Brings today's tasks up to date: Health Connect totals and a finished reading timer. */
object TaskSync {

    /** Returns the tasks completed by this sync, for the celebration. */
    suspend fun sync(context: Context): List<TaskDef> {
        val store = TendStore(context)
        val before = store.today().completedTasks

        val hc = HealthConnect(context)
        val steps = hc.stepsToday()
        val exercise = hc.exerciseMinutesToday()
        if (steps != null || exercise != null) {
            val cfg = store.config
            store.update { Budget.applyActivity(cfg, it, steps, exercise) }
        }
        finishFocusIfDue(store)

        val after = store.today().completedTasks
        return Tasks.all.filter { it.id in after && it.id !in before }
    }

    fun focusSecondsLeft(store: TendStore, now: Long = System.currentTimeMillis()): Long? {
        val start = store.focusStartedAt.takeIf { it > 0 } ?: return null
        return (Tasks.READ.target * 60L - (now - start) / 1000).coerceAtLeast(0)
    }

    private fun finishFocusIfDue(store: TendStore) {
        if (focusSecondsLeft(store) != 0L) return
        store.focusStartedAt = 0
        store.credit(Tasks.READ.id, Tasks.READ.rewardMinutes, manual = false)
    }
}
