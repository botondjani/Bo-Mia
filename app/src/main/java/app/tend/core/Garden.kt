package app.tend.core

import java.time.LocalTime

/** One plant per day, grown from that day's unused minutes. */
enum class Plant(val displayName: String) { SPROUT("sprout"), TULIP("tulip"), SUNFLOWER("sunflower") }

/**
 * Where the unused minutes go: a garden that blooms, or a base camp in the woods that gets built
 * up (a tent first, then a cabin, then a workshop). Same days, same rules, a different picture.
 */
enum class World(val label: String, val place: String) {
    GARDEN("Garden", "garden"),
    BASE("Base", "base");

    /** What a night's unused minutes become here. */
    fun itemName(p: Plant): String = when (this) {
        GARDEN -> p.displayName
        BASE -> when (p) { Plant.SPROUT -> "log"; Plant.TULIP -> "log pile"; Plant.SUNFLOWER -> "supply crate" }
    }

    /** "A new sunflower grows tonight" / "A supply crate arrives tonight". */
    fun tonight(p: Plant): String = when (this) {
        GARDEN -> "A new ${p.displayName} grows tonight"
        BASE -> "A new ${itemName(p)} arrives tonight"
    }
}

object Garden {
    fun plantFor(unusedMinutes: Int): Plant? = when {
        unusedMinutes <= 0 -> null
        unusedMinutes < 15 -> Plant.SPROUT
        unusedMinutes < 30 -> Plant.TULIP
        else -> Plant.SUNFLOWER
    }

    /** The home grows weekly: 0 first sprouts, 1 lit window and bench (week 2), 2 a tree of your own (week 4). */
    /** The garden is drawn at night from the evening hour until 5:00, whatever the theme. */
    fun isNight(eveningFrom: Int, hour: Int = java.time.LocalTime.now().hour): Boolean = Budget.isEvening(eveningFrom, hour)

    fun homeStage(daysTended: Int): Int = when {
        daysTended < 7 -> 0
        daysTended < 21 -> 1
        else -> 2
    }
}

/** A "Worth it?" answer from today. */
data class WorthIt(val time: LocalTime, val answer: String) {
    val notWorth get() = answer == "Not really"
}

/** Tonight's recap: what the day gave back. */
data class Recap(
    val gotBackMinutes: Int,
    val tonightsPlant: Plant?,
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

        fun of(cfg: BudgetConfig, day: DayState, answers: List<WorthIt>): Recap {
            val unused = Budget.gardenMinutesAtClose(cfg, day)
            val not = answers.filter { it.notWorth }
            val earliestNot = not.minOfOrNull { it.time.hour }
            return Recap(
                gotBackMinutes = unused,
                tonightsPlant = Garden.plantFor(unused),
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
