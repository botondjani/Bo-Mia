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
    /** Months, oldest first: average minutes a day, null for a month with no closed days. */
    val months: List<Pair<java.time.YearMonth, Int?>> = emptyList(),
    /** This week so far and the week before (Monday first): average minutes a day and activities done. */
    val thisWeek: WeekSummary? = null,
    val prevWeek: WeekSummary? = null,
    /** The closed day with the least time in the picked apps, of the last 30. */
    val bestDay: DaySummary? = null,
    /** The activity done most often (its id), and how many times. */
    val favourite: Pair<String, Int>? = null,
)

/** A week's average minutes a day (closed days only; null without any) and activities done. */
data class WeekSummary(val avg: Int?, val activities: Int)

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
        val months = (5 downTo 0).map { back ->
            val m = java.time.YearMonth.from(today).minusMonths(back.toLong())
            m to closed.filter { java.time.YearMonth.from(it.date) == m }.takeIf { it.isNotEmpty() }?.let { l -> l.sumOf { it.usedMinutes } / l.size }
        }
        fun week(start: LocalDate): WeekSummary? {
            val all = days.filter { it.date >= start && it.date < start.plusDays(7) && it.date <= today }
            if (all.isEmpty()) return null
            val shut = all.filter { it.date < today && !it.paused }
            return WeekSummary(shut.takeIf { it.isNotEmpty() }?.let { l -> l.sumOf { it.usedMinutes } / l.size }, all.sumOf { maxOf(it.done.size, it.tasksDone) })
        }
        val favourite = days.flatMap { it.done }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.toPair()?.takeIf { it.second >= 2 }
        return Insights(
            months = months,
            thisWeek = week(thisMonday),
            prevWeek = week(thisMonday.minusWeeks(1)),
            bestDay = closed.filter { it.date >= today.minusDays(30) }.takeIf { it.size >= 3 }?.minByOrNull { it.usedMinutes },
            favourite = favourite,
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

    /** "31 h 20 min", "45 min": never broken over two lines (no-break spaces). */
    fun hours(minutes: Int): String = when {
        minutes < 60 -> tr("$minutes min", "$minutes perc")
        minutes % 60 == 0 -> tr("${minutes / 60} h", "${minutes / 60} óra")
        else -> tr("${minutes / 60} h ${minutes % 60} min", "${minutes / 60} ó ${minutes % 60} p")
    }.replace(' ', '\u00A0')

    /** What the time won back is worth, said plainly: walks, films or books. */
    fun likeWhat(minutes: Int): String? = when {
        minutes < 90 -> null
        minutes < 8 * 60 -> tr("about ${minutes / 45} walks in the woods", "nagyjából ${minutes / 45} erdei séta")
        minutes < 30 * 60 -> tr("about ${minutes / 110} films", "nagyjából ${minutes / 110} film")
        else -> tr("about ${minutes / 360} books", "nagyjából ${minutes / 360} könyv")
    }
}
