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
            // Only nights that began after setup count: no sleep credit for the night before installing.
            val night = java.time.LocalDate.now().minusDays(1).atTime(20, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val sleepCounts = store.onboardedAt < night
            var sleep = if (sleepCounts) hc.sleepMinutesLastNight() else null
            // No sleep tracker (or nothing written last night): the longest screen-off stretch stands in.
            if (sleepCounts && Tasks.SLEEP in selected && (sleep == null || sleep == 0)) {
                sleep = (estimatedSleepMillis(context) / 60_000).toInt().takeIf { it > 0 }
                if (sleep != null) store.sleepEstimatedOn = java.time.LocalDate.now().toString()
            }
            if (Tasks.MOVE in selected) store.exerciseTracked = (hc.exerciseMinutesLately() ?: 0) > 0
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
        val outcome = when {
            PhoneFree.runsPhoneFree(task) -> PhoneFree.outcome(task, start, now, screenOnMillis(context, start, now))
            now - start >= task.target * 60_000L -> PhoneFree.Outcome.DONE
            else -> PhoneFree.Outcome.RUNNING
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

    /** Last night's longest screen-off stretch, 8 pm to noon. */
    private fun estimatedSleepMillis(context: Context): Long {
        val zone = java.time.ZoneId.systemDefault()
        val from = java.time.LocalDate.now().minusDays(1).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
        val noon = java.time.LocalDate.now().atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val to = minOf(System.currentTimeMillis(), noon)
        return PhoneFree.longestOffMillis(from, to, screenTransitions(context, from, to) ?: return 0)
    }

    /** Screen on/off transitions from the system's usage events (Android 9+). */
    private fun screenTransitions(context: Context, start: Long, now: Long): List<Pair<Long, Boolean>>? {
        if (android.os.Build.VERSION.SDK_INT < 28) return null
        val usm = context.getSystemService(android.app.usage.UsageStatsManager::class.java)
        val events = runCatching { usm.queryEvents(start, now) }.getOrNull() ?: return null
        val e = android.app.usage.UsageEvents.Event()
        val transitions = mutableListOf<Pair<Long, Boolean>>()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE -> transitions += e.timeStamp to true
                android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> transitions += e.timeStamp to false
            }
        }
        return transitions
    }

    private fun screenOnMillis(context: Context, start: Long, now: Long): Long =
        screenTransitions(context, start, now)?.let { PhoneFree.screenOnMillis(start, now, it) } ?: 0
}
