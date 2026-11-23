package app.tend.core

import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * A scheduled lock: the picked apps stay closed between [from] and [to] (hours, may wrap past
 * midnight), every day or on weekdays only. Off unless the user turns it on.
 */
data class LockSchedule(
    val on: Boolean, val from: Int, val to: Int,
    /** The days it locks on (for a window past midnight, the day it starts). */
    val days: Set<DayOfWeek>,
    val label: String,
    /** Which one it is: "bed", "work", "night", or "c0".."c2" for your own. */
    val id: String = "",
    /** The picked apps it closes; empty = all of them. */
    val apps: Set<String> = emptySet(),
) {
    val weekdaysOnly: Boolean get() = days == WEEKDAYS

    fun activeAt(t: LocalDateTime): Boolean {
        if (!on || from == to) return false
        val h = t.hour
        val inWindow = if (from < to) h in from until to else h >= from || h < to
        if (!inWindow) return false
        // For a window past midnight, the early hours belong to the day it started.
        val day = if (from > to && h < to) t.minusDays(1).dayOfWeek else t.dayOfWeek
        return day in days
    }

    /** Whether it closes [pkg] (one of the picked apps). */
    fun covers(pkg: String) = apps.isEmpty() || pkg in apps

    companion object {
        val ALL_DAYS: Set<DayOfWeek> = DayOfWeek.entries.toSet()
        val WEEKDAYS: Set<DayOfWeek> = ALL_DAYS - DayOfWeek.SATURDAY - DayOfWeek.SUNDAY
        val BEDTIME = LockSchedule(false, 23, 7, ALL_DAYS, tr("Bedtime", "Lefekvésidő"), "bed")
        val WORK = LockSchedule(false, 9, 17, WEEKDAYS, tr("Work hours", "Munkaidő"), "work")
        /** After "Good night", until 7:00: the picked apps rest like at bedtime. */
        val GOOD_NIGHT = LockSchedule(true, 0, 7, ALL_DAYS, tr("Good night", "Jó éjt"), "night")
        /** Your own times (Plus), at most [CUSTOM_MAX]. */
        const val CUSTOM_MAX = 3
        fun custom(i: Int) = LockSchedule(true, 17, 19, ALL_DAYS, tr("My time", "Saját időszak"), "c$i")

        /** The schedule locking right now, if any (for [pkg]: one that closes it). */
        fun activeOf(schedules: List<LockSchedule>, t: LocalDateTime = LocalDateTime.now(), pkg: String? = null) =
            schedules.firstOrNull { it.activeAt(t) && (pkg == null || it.covers(pkg)) }
    }
}

/** "Open for 5 min": how long a chosen open lasts before tend checks in again. */
object TimeBox {
    val CHOICES = listOf(5, 15)

    fun endsAt(start: Long, minutes: Int): Long = if (minutes > 0) start + minutes * 60_000L else Long.MAX_VALUE
}
