package app.tend.core

import java.time.LocalDate

/** User-set limits. All minutes. */
data class BudgetConfig(
    /** Daily total that can be reached with base + earned minutes. */
    val totalMinutes: Int = 180,
    /** Free base budget, available without doing anything. */
    val freeMinutes: Int = 45,
    /** Internal cap on minutes earned by manual check-offs (not shown to the user). */
    val manualCapMinutes: Int = 30,
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
)

object Budget {

    /** Minutes still usable today. Can go negative (overuse). */
    fun remainingMinutes(cfg: BudgetConfig, day: DayState): Int =
        cfg.freeMinutes + day.earnedMinutes - usedMinutes(day)

    /** Minutes that can still be earned today. */
    fun earnableMinutes(cfg: BudgetConfig, day: DayState): Int =
        (cfg.totalMinutes - cfg.freeMinutes - day.earnedMinutes).coerceAtLeast(0)

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

    /** Unused minutes at the end of the day grow the garden. Overuse never carries over. */
    fun gardenMinutesAtClose(cfg: BudgetConfig, day: DayState): Int =
        remainingMinutes(cfg, day).coerceAtLeast(0)

    private fun usedMinutes(day: DayState): Int = ((day.usedSeconds + 59) / 60).toInt()
}
