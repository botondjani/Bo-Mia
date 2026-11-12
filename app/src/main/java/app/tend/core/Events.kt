package app.tend.core

import java.time.LocalDate
import java.time.MonthDay

/**
 * A few days in the year the camp celebrates, each with a thing of its own, sold only then (and
 * kept): pumpkin night, St. Martin's lantern night, Advent, Midsummer.
 */
enum class CampEvent(val title: String, val thing: String, private val from: MonthDay, private val to: MonthDay, val line: String) {
    PUMPKINS(tr("Pumpkin night", "Töklámpás-este"), "jack_o_lantern", MonthDay.of(10, 27), MonthDay.of(11, 1), tr("Carved pumpkins glow on the meadow.", "Faragott tökök világítanak a réten.")),
    LANTERNS(tr("Lantern night", "Lampionos este"), "paper_lanterns", MonthDay.of(11, 9), MonthDay.of(11, 12), tr("St. Martin's: lanterns for the walk at dusk.", "Márton-nap: lampionok az alkonyi sétához.")),
    ADVENT(tr("Advent", "Advent"), "xmas_tree", MonthDay.of(12, 1), MonthDay.of(12, 26), tr("A little fir tree for the camp, lit up at night.", "Kis fenyőfa a táborba, este kivilágítva.")),
    MIDSUMMER(tr("Midsummer", "Szent Iván-éj"), "flower_arch", MonthDay.of(6, 19), MonthDay.of(6, 24), tr("The longest days: a garland of flowers.", "A leghosszabb napok: egy füzér virág.")),
    ;

    fun on(date: LocalDate): Boolean = MonthDay.from(date).let { !it.isBefore(from) && !it.isAfter(to) }

    /** Days left, today counted (1 on the last day). */
    fun daysLeft(date: LocalDate): Int = (to.atYear(date.year).toEpochDay() - date.toEpochDay() + 1).toInt()

    companion object {
        /** What's on at [date], if anything. */
        fun on(date: LocalDate): CampEvent? = entries.firstOrNull { it.on(date) }
    }
}
