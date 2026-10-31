package app.tend.health

import android.content.Context
import app.tend.core.Budget
import app.tend.core.PhoneFree
import app.tend.core.TaskDef
import app.tend.core.Tasks
import app.tend.core.Verification
import app.tend.data.TendStore

/** Brings today's tasks up to date: Health Connect totals and finished timers. */
object TaskSync {

    /** What a sync or a check-off completed: for the celebration. */
    data class Done(val tasks: List<TaskDef>, val minutes: Int)

    suspend fun sync(context: Context): Done {
        val store = TendStore(context)
        val before = store.today()
        val selected = store.selectedTasks

        if (selected.any { it.verification == Verification.AUTOMATIC }) {
            val hc = HealthConnect(context, selected)
            val steps = hc.stepsToday()
            val exercise = hc.exerciseMinutesToday()
            val sleep = hc.sleepMinutesLastNight()
            if (steps != null || exercise != null || sleep != null) {
                val cfg = store.config
                store.update { Budget.applyActivity(cfg, it, steps, exercise, selected, sleep) }
            }
        }
        checkTimer(context)
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

    enum class TimerResult { DONE, BROKEN }

    /** Starts a reading/learning or phone-free timer; it also finishes in the background. */
    fun startTimer(context: Context, task: TaskDef) {
        val store = TendStore(context)
        if (store.focusStartedAt > 0) return
        store.timerBroken = ""
        store.focusTaskId = task.id
        store.focusStartedAt = System.currentTimeMillis()
        TimerAlarm.schedule(context, store.focusStartedAt + task.target * 60_000L + 2_000L)
    }

    fun stopTimer(context: Context) {
        TendStore(context).focusStartedAt = 0
        TimerAlarm.cancel(context)
    }

    /** Screen-on seconds since the running phone-free session started (0 when it can't be measured). */
    fun screenOnSeconds(context: Context): Long {
        val store = TendStore(context)
        val start = store.focusStartedAt.takeIf { it > 0 } ?: return 0
        return screenOnMillis(context, start, System.currentTimeMillis()) / 1000
    }

    /** Credits a timer that has run its course; ends a phone-free one the phone interrupted. */
    fun checkTimer(context: Context): TimerResult? {
        val store = TendStore(context)
        val start = store.focusStartedAt.takeIf { it > 0 } ?: return null
        val task = store.focusTask
        val now = System.currentTimeMillis()
        val outcome = when (task.verification) {
            Verification.PHONE_FREE -> PhoneFree.outcome(task, start, now, screenOnMillis(context, start, now))
            else -> if (now - start >= task.target * 60_000L) PhoneFree.Outcome.DONE else PhoneFree.Outcome.RUNNING
        }
        return when (outcome) {
            PhoneFree.Outcome.RUNNING -> null
            PhoneFree.Outcome.DONE -> {
                stopTimer(context)
                store.credit(task.id, task.rewardMinutes, manual = false)
                TimerResult.DONE
            }
            PhoneFree.Outcome.BROKEN -> {
                stopTimer(context)
                store.timerBroken = task.id
                TimerResult.BROKEN
            }
        }
    }

    /** Screen on/off transitions from the system's usage events (Android 9+). */
    private fun screenOnMillis(context: Context, start: Long, now: Long): Long {
        if (android.os.Build.VERSION.SDK_INT < 28) return 0
        val usm = context.getSystemService(android.app.usage.UsageStatsManager::class.java)
        val events = runCatching { usm.queryEvents(start, now) }.getOrNull() ?: return 0
        val e = android.app.usage.UsageEvents.Event()
        val transitions = mutableListOf<Pair<Long, Boolean>>()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE -> transitions += e.timeStamp to true
                android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> transitions += e.timeStamp to false
            }
        }
        return PhoneFree.screenOnMillis(start, now, transitions)
    }
}
