package app.tend.core

import java.time.LocalDate
import java.time.MonthDay

/**
 * A few days in the year the camp celebrates, each with a thing of its own, sold only then (and
 * kept): pumpkin night, St. Martin's lantern night, Advent, Midsummer.
 */
enum class CampEvent(val title: String, val thing: String, private val from: MonthDay, private val to: MonthDay, val line: String) {
    PUMPKINS("Pumpkin night", "jack_o_lantern", MonthDay.of(10, 27), MonthDay.of(11, 1), "Carved pumpkins glow on the meadow."),
    LANTERNS("Lantern night", "paper_lanterns", MonthDay.of(11, 9), MonthDay.of(11, 12), "St. Martin's: lanterns for the walk at dusk."),
    ADVENT("Advent", "xmas_tree", MonthDay.of(12, 1), MonthDay.of(12, 26), "A little fir tree for the camp, lit up at night."),
    MIDSUMMER("Midsummer", "flower_arch", MonthDay.of(6, 19), MonthDay.of(6, 24), "The longest days: a garland of flowers."),
    ;

    fun on(date: LocalDate): Boolean = MonthDay.from(date).let { !it.isBefore(from) && !it.isAfter(to) }

    /** Days left, today counted (1 on the last day). */
    fun daysLeft(date: LocalDate): Int = (to.atYear(date.year).toEpochDay() - date.toEpochDay() + 1).toInt()

    companion object {
        /** What's on at [date], if anything. */
        fun on(date: LocalDate): CampEvent? = entries.firstOrNull { it.on(date) }
    }
}
