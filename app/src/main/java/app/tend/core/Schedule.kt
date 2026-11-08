package app.tend.core

import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * A scheduled lock: the picked apps stay closed between [from] and [to] (hours, may wrap past
 * midnight), every day or on weekdays only. Off unless the user turns it on.
 */
data class LockSchedule(val on: Boolean, val from: Int, val to: Int, val weekdaysOnly: Boolean, val label: String) {
    fun activeAt(t: LocalDateTime): Boolean {
        if (!on || from == to) return false
        val h = t.hour
        val inWindow = if (from < to) h in from until to else h >= from || h < to
        if (!inWindow) return false
        if (!weekdaysOnly) return true
        // For a window past midnight, the early hours belong to the day it started.
        val day = if (from > to && h < to) t.minusDays(1).dayOfWeek else t.dayOfWeek
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
    }

    companion object {
        val BEDTIME = LockSchedule(false, 23, 7, false, "Bedtime")
        val WORK = LockSchedule(false, 9, 17, true, "Work hours")
        /** After "Good night", until 7:00: the paused apps rest like at bedtime. */
        val GOOD_NIGHT = LockSchedule(true, 0, 7, false, "Good night")

        /** The schedule locking right now, if any. */
        fun activeOf(schedules: List<LockSchedule>, t: LocalDateTime = LocalDateTime.now()) = schedules.firstOrNull { it.activeAt(t) }
    }
}

/** "Open for 5 min": how long a chosen open lasts before tend checks in again. */
object TimeBox {
    val CHOICES = listOf(5, 15)

    fun endsAt(start: Long, minutes: Int): Long = if (minutes > 0) start + minutes * 60_000L else Long.MAX_VALUE
}
