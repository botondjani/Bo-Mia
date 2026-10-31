package app.tend.health

import android.content.Context
import app.tend.core.Budget
import app.tend.core.TaskDef
import app.tend.core.Tasks
import app.tend.core.Verification
import app.tend.data.TendStore

/** Brings today's tasks up to date: Health Connect totals and a finished reading timer. */
object TaskSync {

    /** What a sync or a check-off completed: for the celebration. */
    data class Done(val tasks: List<TaskDef>, val minutes: Int)

    suspend fun sync(context: Context): Done {
        val store = TendStore(context)
        val before = store.today()
        val selected = store.selectedTasks

        if (selected.any { it.verification == Verification.AUTOMATIC }) {
            val hc = HealthConnect(context)
            val steps = hc.stepsToday()
            val exercise = hc.exerciseMinutesToday()
            if (steps != null || exercise != null) {
                val cfg = store.config
                store.update { Budget.applyActivity(cfg, it, steps, exercise, selected) }
            }
        }
        finishFocusIfDue(store)
        return diff(before, store.today(), selected)
    }

    /** One tap: the user did a check-off activity. */
    fun checkOff(store: TendStore, task: TaskDef): Done {
        val before = store.today()
        store.credit(task.id, task.rewardMinutes, manual = true)
        return diff(before, store.today(), store.selectedTasks)
    }

    fun undo(store: TendStore, task: TaskDef) = store.update { Budget.undo(it, task.id, task.manual) }

    private fun diff(before: app.tend.core.DayState, after: app.tend.core.DayState, selected: List<TaskDef>) = Done(
        selected.filter { it.id in after.completedTasks && it.id !in before.completedTasks },
        after.earnedMinutes - before.earnedMinutes,
    )

    fun focusSecondsLeft(store: TendStore, now: Long = System.currentTimeMillis()): Long? {
        val start = store.focusStartedAt.takeIf { it > 0 } ?: return null
        return (store.focusTask.target * 60L - (now - start) / 1000).coerceAtLeast(0)
    }

    private fun finishFocusIfDue(store: TendStore) {
        if (focusSecondsLeft(store) != 0L) return
        store.focusStartedAt = 0
        store.focusTask.let { store.credit(it.id, it.rewardMinutes, manual = false) }
    }
}
