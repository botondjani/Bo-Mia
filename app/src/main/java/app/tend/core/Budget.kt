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
    val baseDelaySeconds: Int = 5,
    /** Every further open today waits this much longer. */
    val delayStepSeconds: Int = 1,
    /** The wait never grows beyond this. */
    val maxDelaySeconds: Int = 10,
    /** In evening mode every open waits this long: late scrolls are the ones we regret most. */
    val eveningDelaySeconds: Int = 10,
    /** Kinds of app ([AppKinds]) whose time also counts toward the day's minutes, with no pause. */
    val counted: Set<String> = emptySet(),
) {
    /** With the weekend's extra hour taken today: [WEEKEND_PLUS] more free minutes, and as many more at most. */
    fun withWeekendExtra(): BudgetConfig = copy(freeMinutes = freeMinutes + WEEKEND_PLUS, totalMinutes = totalMinutes + WEEKEND_PLUS)

    companion object {
        const val WEEKEND_PLUS = 60

        fun isWeekend(date: java.time.LocalDate) = date.dayOfWeek.value >= 6
    }
}

/** Kinds of app that count toward the day's minutes without a pause: only the picked apps get one. */
object AppKinds {
    const val SOCIAL = "social"
    const val VIDEO = "video"
    const val GAMES = "games"
    val ALL = setOf(SOCIAL, VIDEO, GAMES)
}

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
    /** Last night's sleep, from Health Connect. */
    val sleepMinutes: Int = 0,
    /** Minutes actually granted per task today, so a check-off can be undone exactly. */
    val grants: Map<String, Int> = emptyMap(),
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

    /**
     * Wait before an open: the base for the first two in an hour, then a second more for each one
     * after ([earlierThisHour]: opens in the last hour before this one), up to the most; in the
     * evening at least the evening's.
     */
    /**
     * The Megállító's wait before an app can be opened, by how strict tend is: Easy 3 s, Balanced 5 s,
     * Strict 10 s; in the evening 5 s more. From the third open within an hour, a second more for each,
     * up to 10, 12 or 20 s.
     */
    fun openDelaySeconds(level: StrictLevel, earlierThisHour: Int, evening: Boolean = false): Int {
        val (base, cap) = when (level) { StrictLevel.EASY -> 3 to 10; StrictLevel.BALANCED -> 5 to 12; StrictLevel.STRICT -> 10 to 20 }
        val start = base + if (evening) EVENING_EXTRA_SECONDS else 0
        return minOf(start + (earlierThisHour - 1).coerceAtLeast(0), maxOf(cap, start))
    }

    /** What the evening adds to the Megállító's wait. */
    const val EVENING_EXTRA_SECONDS = 5

    /** Evening mode runs from [fromHour] until 5 am; a negative hour means it's off. */
    /** From [fromHour] until 5:00. A start after midnight (0–4) means only those small hours. */
    fun isEvening(fromHour: Int, hour: Int): Boolean = when {
        fromHour < 0 -> false
        fromHour < 5 -> hour in fromHour until 5
        else -> hour >= fromHour || hour < 5
    }

    /** The hours the evening can start at: from 16:00 round to 3:00. */
    val EVENING_HOURS: List<Int> = (16..23).toList() + (0..3).toList()

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
            grants = day.grants + (taskId to granted),
        )
    }

    /** Key in [DayState.grants] for what "phone down" gave today. */
    const val PHONE_DOWN = "phone_down"

    /** The most "phone down" can give in a day: half of what can be earned. */
    fun phoneDownCap(cfg: BudgetConfig): Int = (cfg.totalMinutes - cfg.freeMinutes).coerceAtLeast(0) / 2

    /**
     * A plain "phone down" of [minutes] finished: as many minutes back, up to [phoneDownCap] for the
     * day (and the day's total). It's not an activity: nothing is ticked off.
     */
    fun creditPhoneDown(cfg: BudgetConfig, day: DayState, minutes: Int): DayState {
        val already = day.grants[PHONE_DOWN] ?: 0
        val granted = minOf(minutes, phoneDownCap(cfg) - already, earnableMinutes(cfg, day)).coerceAtLeast(0)
        if (granted == 0) return day
        return day.copy(earnedMinutes = day.earnedMinutes + granted, grants = day.grants + (PHONE_DOWN to already + granted))
    }

    /** Takes back a credited task (a mistaken check-off), exactly what it granted. */
    fun undo(day: DayState, taskId: String, manual: Boolean): DayState {
        if (taskId !in day.completedTasks) return day
        val granted = day.grants[taskId] ?: 0
        return day.copy(
            earnedMinutes = day.earnedMinutes - granted,
            manualEarnedMinutes = day.manualEarnedMinutes - if (manual) granted else 0,
            completedTasks = day.completedTasks - taskId,
            grants = day.grants - taskId,
        )
    }

    /**
     * Applies today's activity totals and credits the automatic tasks they complete.
     * Totals never go down within a day (a source can briefly report less while syncing).
     */
    fun applyActivity(
        cfg: BudgetConfig, day: DayState, steps: Long?, exerciseMinutes: Int?, selected: List<TaskDef>, sleepMinutes: Int? = null,
    ): DayState {
        var d = day.copy(
            steps = maxOf(day.steps, steps ?: 0),
            exerciseMinutes = maxOf(day.exerciseMinutes, exerciseMinutes ?: 0),
            sleepMinutes = maxOf(day.sleepMinutes, sleepMinutes ?: 0),
        )
        for (task in completedAutomaticTasks(d.steps, d.exerciseMinutes, selected, d.sleepMinutes)) {
            d = credit(cfg, d, task.id, task.rewardMinutes, manual = false)
        }
        return d
    }

    /** Minutes not used by the end of the day ("got back"). Overuse never carries over. */
    fun unusedMinutesAtClose(cfg: BudgetConfig, day: DayState): Int =
        remainingMinutes(cfg, day).coerceAtLeast(0)

    fun usedMinutes(day: DayState): Int = ((day.usedSeconds + 59) / 60).toInt()
}
