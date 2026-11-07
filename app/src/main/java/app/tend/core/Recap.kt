package app.tend.core

import java.time.LocalTime

/** The old garden's nightly plants: kept only to turn them into acorns once (see Clearing.acornsFor). */
enum class Plant(val displayName: String) { SPROUT("sprout"), TULIP("tulip"), SUNFLOWER("sunflower") }

/** A "Worth it?" answer from today. */
data class WorthIt(val time: LocalTime, val answer: String) {
    val notWorth get() = answer == "Not really"
}

/** Tonight's recap: what the day gave back. */
data class Recap(
    val gotBackMinutes: Int,
    /** Acorns tonight's day close will add if the day ends like this. */
    val tonightAcorns: Int,
    val steps: Long,
    val exerciseMinutes: Int,
    val readingMinutes: Int,
    val worthItAnswers: Int,
    val notWorthIt: Int,
    /** Set when every "not really" came at or after this hour (and there were at least two). */
    val notWorthAfterHour: Int?,
) {
    companion object {
        private const val LATE_FROM_HOUR = 18

        fun of(cfg: BudgetConfig, day: DayState, answers: List<WorthIt>, normalMinutes: Int? = null): Recap {
            val unused = Budget.unusedMinutesAtClose(cfg, day)
            val not = answers.filter { it.notWorth }
            val earliestNot = not.minOfOrNull { it.time.hour }
            return Recap(
                gotBackMinutes = unused,
                tonightAcorns = Budget.usedMinutes(day).let { used -> Acorns.atDayClose(used, Budget.remainingMinutes(cfg, day) + used, normalMinutes) },
                steps = day.steps,
                exerciseMinutes = day.exerciseMinutes,
                readingMinutes = listOf(Tasks.READ, Tasks.LEARN).filter { it.id in day.completedTasks }.sumOf { it.target },
                worthItAnswers = answers.size,
                notWorthIt = not.size,
                notWorthAfterHour = earliestNot?.takeIf { not.size >= 2 && it >= LATE_FROM_HOUR },
            )
        }
    }
}
