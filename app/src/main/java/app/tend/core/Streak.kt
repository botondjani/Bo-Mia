package app.tend.core

import java.time.LocalDate

/**
 * Good days in a row, kindly counted: one missed day a week doesn't end the run (the companion
 * keeps the fire going that day). A missed day isn't counted, only bridged; two in a row end it.
 */
object Streak {
    /** [days]: the run's length; [kept]: yesterday was missed and bridged; [spare]: a missed day could still be bridged now. */
    data class State(val days: Int, val kept: Boolean, val spare: Boolean)

    /** From the [active] days (something was done), as of [today] (which counts once something's done, and never breaks the run). */
    fun of(active: Set<LocalDate>, today: LocalDate): State {
        var n = if (today in active) 1 else 0
        var bridged: LocalDate? = null // the last day bridged (the nearest one to today)
        var first: LocalDate? = null
        var d = today.minusDays(1)
        while (true) {
            if (d in active) { n++; d = d.minusDays(1); continue }
            // One gap a week, and only a single day wide: the day before it must be a good one.
            val free = bridged == null || java.time.temporal.ChronoUnit.DAYS.between(d, bridged) >= WEEK
            if (free && d.minusDays(1) in active) { if (first == null) first = d; bridged = d; d = d.minusDays(1); continue }
            break
        }
        val kept = first == today.minusDays(1)
        val spare = first == null || java.time.temporal.ChronoUnit.DAYS.between(first, today) >= WEEK
        return State(n, kept && n > 0, spare)
    }

    private const val WEEK = 7L
}
