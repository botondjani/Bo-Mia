package app.tend.core

import java.time.LocalDate
import java.time.LocalDateTime

/** A logged answer: "looking_for" from the pause screen or "worth_it" after a session. */
data class Answer(val time: LocalDateTime, val kind: String, val pkg: String, val answer: String)

/** One day, as it was closed (or today, so far). */
data class DaySummary(
    val date: LocalDate,
    val usedMinutes: Int,
    val unusedMinutes: Int,
    val tasksDone: Int,
    /** Ids of the tasks done that day (empty for days logged before this was kept). */
    val done: Set<String> = emptySet(),
)

enum class DayPart(val label: String) {
    MORNING("Morning"), AFTERNOON("Afternoon"), EVENING("Evening"), LATE("Late night");

    companion object {
        fun of(hour: Int) = when (hour) {
            in 5..11 -> MORNING
            in 12..17 -> AFTERNOON
            in 18..21 -> EVENING
            else -> LATE
        }
    }
}

data class WorthSplit(val yes: Int = 0, val kindOf: Int = 0, val not: Int = 0) {
    val total get() = yes + kindOf + not
    val notShare get() = if (total == 0) 0f else not.toFloat() / total

    fun add(answer: String) = when (answer) {
        "Yes" -> copy(yes = yes + 1)
        "Kind of" -> copy(kindOf = kindOf + 1)
        else -> copy(not = not + 1)
    }
}

data class WeeklyPatterns(
    /** The last 7 days, oldest first. */
    val days: List<DaySummary>,
    val gotBackMinutes: Int,
    val byDayPart: Map<DayPart, WorthSplit>,
    /** Package to split, most-answered first, at most [Patterns.TOP_APPS]. */
    val byApp: List<Pair<String, WorthSplit>>,
    val lookingFor: Map<String, Int>,
    /** One honest sentence about when scrolling felt worth it least (or most), if the data says so. */
    val insight: String?,
    /** Pause pop-ups this week, and how many ended without opening the app. */
    val pauses: Int = 0,
    val letGo: Int = 0,
)

object Patterns {
    const val TOP_APPS = 3
    private const val MIN_ANSWERS = 3

    fun week(today: LocalDate, days: List<DaySummary>, answers: List<Answer>): WeeklyPatterns {
        val from = today.minusDays(6)
        val week = days.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }.sortedBy { it.date }
        val recent = answers.filter { !it.time.toLocalDate().isBefore(from) }
        val worth = recent.filter { it.kind == "worth_it" }

        val byPart = DayPart.entries.associateWith { part ->
            worth.filter { DayPart.of(it.time.hour) == part }.fold(WorthSplit()) { s, a -> s.add(a.answer) }
        }
        val byApp = worth.groupBy { it.pkg }
            .map { (pkg, list) -> pkg to list.fold(WorthSplit()) { s, a -> s.add(a.answer) } }
            .sortedByDescending { it.second.total }
            .take(TOP_APPS)
        val lookingFor = recent.filter { it.kind == "looking_for" }.groupingBy { it.answer }.eachCount()

        val pauses = recent.filter { it.kind == "pause" }
        return WeeklyPatterns(
            week, week.sumOf { it.unusedMinutes }, byPart, byApp, lookingFor, insight(byPart),
            pauses = pauses.size, letGo = pauses.count { it.answer != "open" },
        )
    }

    private fun insight(byPart: Map<DayPart, WorthSplit>): String? {
        val enough = byPart.filterValues { it.total >= MIN_ANSWERS }
        val worst = enough.maxByOrNull { it.value.notShare }
        if (worst != null && worst.value.notShare >= 0.5f) {
            val (part, s) = worst
            return "${part.label} scrolls felt worth it least: ${s.not} of ${s.total} were \"not really\"."
        }
        val best = enough.minByOrNull { it.value.notShare }
        if (best != null && best.value.notShare <= 0.25f) {
            return "${best.key.label} scrolls mostly felt worth it. Nice balance."
        }
        return null
    }
}
