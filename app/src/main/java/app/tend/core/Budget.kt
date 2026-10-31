package app.tend.core

import java.time.LocalDate

/** User-set limits. All minutes. */
data class BudgetConfig(
    /** Daily total that can be reached with base + earned minutes. */
    val totalMinutes: Int = 180,
    /** Free base budget, available without doing anything. */
    val freeMinutes: Int = 45,
    /** Internal cap on minutes earned by manual check-offs (not shown to the user). */
    val manualCapMinutes: Int = 45,
    /** Delay before the first open of a blocked app. */
    val baseDelaySeconds: Int = 3,
    /** Every further open today waits this much longer. */
    val delayStepSeconds: Int = 5,
)

/** Everything that resets at midnight. Earned minutes expire with it. */
data class DayState(
    val date: LocalDate,
    val earnedMinutes: Int = 0,
    val manualEarnedMinutes: Int = 0,
    val usedSeconds: Long = 0,
    val opensToday: Int = 0,
    val completedTasks: Set<String> = emptySet(),
    /** Today's totals from Health Connect, as of the last sync. */
    val steps: Long = 0,
    val exerciseMinutes: Int = 0,
)

object Budget {

    /** Minutes still usable today. Can go negative (overuse). */
    fun remainingMinutes(cfg: BudgetConfig, day: DayState): Int =
        cfg.freeMinutes + day.earnedMinutes - usedMinutes(day)

    /** Minutes that can still be earned today. */
    fun earnableMinutes(cfg: BudgetConfig, day: DayState): Int =
        (cfg.totalMinutes - cfg.freeMinutes - day.earnedMinutes).coerceAtLeast(0)

    /**
     * What the selected tasks can still bring today: their rewards, with check-offs limited by
     * the manual cap, and everything by the daily total.
     */
    fun earnableFrom(cfg: BudgetConfig, day: DayState, selected: List<TaskDef>): Int {
        val open = selected.filter { it.id !in day.completedTasks }
        val measured = open.filterNot { it.manual }.sumOf { it.rewardMinutes }
        val manual = minOf(open.filter { it.manual }.sumOf { it.rewardMinutes }, (cfg.manualCapMinutes - day.manualEarnedMinutes).coerceAtLeast(0))
        return minOf(measured + manual, earnableMinutes(cfg, day))
    }

    /** Blocked apps stay closed until a task brings the balance back above zero. */
    fun isLocked(cfg: BudgetConfig, day: DayState): Boolean = remainingMinutes(cfg, day) <= 0

    /** Wait before the next open; grows with every open today. */
    fun openDelaySeconds(cfg: BudgetConfig, day: DayState): Int =
        cfg.baseDelaySeconds + cfg.delayStepSeconds * day.opensToday

    /**
     * Credits a completed task. Each task counts once per day; the credit is clamped
     * to what is still earnable, and manual credits additionally to [BudgetConfig.manualCapMinutes].
     */
    fun credit(cfg: BudgetConfig, day: DayState, taskId: String, minutes: Int, manual: Boolean): DayState {
        if (taskId in day.completedTasks) return day
        var granted = minOf(minutes, earnableMinutes(cfg, day))
        if (manual) granted = minOf(granted, (cfg.manualCapMinutes - day.manualEarnedMinutes).coerceAtLeast(0))
        return day.copy(
            earnedMinutes = day.earnedMinutes + granted,
            manualEarnedMinutes = day.manualEarnedMinutes + if (manual) granted else 0,
            completedTasks = day.completedTasks + taskId,
        )
    }

    /**
     * Applies today's activity totals and credits the automatic tasks they complete.
     * Totals never go down within a day (a source can briefly report less while syncing).
     */
    fun applyActivity(cfg: BudgetConfig, day: DayState, steps: Long?, exerciseMinutes: Int?, selected: List<TaskDef>): DayState {
        var d = day.copy(
            steps = maxOf(day.steps, steps ?: 0),
            exerciseMinutes = maxOf(day.exerciseMinutes, exerciseMinutes ?: 0),
        )
        for (task in completedAutomaticTasks(d.steps, d.exerciseMinutes, selected)) {
            d = credit(cfg, d, task.id, task.rewardMinutes, manual = false)
        }
        return d
    }

    /** Unused minutes at the end of the day grow the garden. Overuse never carries over. */
    fun gardenMinutesAtClose(cfg: BudgetConfig, day: DayState): Int =
        remainingMinutes(cfg, day).coerceAtLeast(0)

    private fun usedMinutes(day: DayState): Int = ((day.usedSeconds + 59) / 60).toInt()
}
