package app.tend.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * What tend has given back, from the days logged: time won back against your normal (the picked
 * apps' daily average before tend), how the weeks went, and what you did instead.
 */
data class Insights(
    /** Minutes a day in the picked apps before tend; null until known. */
    val normal: Int?,
    /** Minutes won back on closed days: below the normal, never negative, breaks left out. */
    val wonBack: Int,
    /** Since this day (the first logged one). */
    val since: LocalDate?,
    /** The last 7 closed days' average use; null without any. */
    val lastWeek: Int?,
    /** Weeks (Monday first), oldest first: average minutes a day, null for a week with no closed days. */
    val weeks: List<Pair<LocalDate, Int?>>,
    val activities: Int,
    val activeDays: Int,
    /** Times a paused app was left closed ("Not now"). */
    val notNows: Int,
)

object InsightsOf {
    /** From [days] (closed days and today, as logged), as of [today], with [weeks] weeks in the chart. */
    fun of(days: List<DaySummary>, normal: Int?, notNows: Int, today: LocalDate, weeks: Int = 8): Insights {
        val closed = days.filter { it.date < today && !it.paused }
        val wonBack = if (normal == null) 0 else closed.sumOf { (normal - it.usedMinutes).coerceAtLeast(0) }
        val lastWeek = closed.filter { it.date >= today.minusDays(7) }.takeIf { it.isNotEmpty() }?.let { l -> l.sumOf { it.usedMinutes } / l.size }
        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val chart = (weeks - 1 downTo 0).map { back ->
            val start = thisMonday.minusWeeks(back.toLong())
            val inWeek = closed.filter { it.date >= start && it.date < start.plusDays(7) }
            start to inWeek.takeIf { it.isNotEmpty() }?.let { l -> l.sumOf { it.usedMinutes } / l.size }
        }
        return Insights(
            normal = normal,
            wonBack = wonBack,
            since = days.minOfOrNull { it.date },
            lastWeek = lastWeek,
            weeks = chart,
            activities = days.sumOf { maxOf(it.done.size, it.tasksDone) },
            activeDays = days.count { it.done.isNotEmpty() || it.tasksDone > 0 },
            notNows = notNows,
        )
    }

    /** "31 h 20 min", "45 min". */
    fun hours(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }

    /** What the time won back is worth, said plainly: walks, films or books. */
    fun likeWhat(minutes: Int): String? = when {
        minutes < 90 -> null
        minutes < 8 * 60 -> "about ${minutes / 45} walks in the woods"
        minutes < 30 * 60 -> "about ${minutes / 110} films"
        else -> "about ${minutes / 360} books"
    }
}
