package app.tend.core

/** How far you've come: days at the camp, activities done, hours got back from scrolling. */
data class Totals(val days: Int, val activities: Int, val hoursBack: Int)

/**
 * Milestones over the months: each one reached adds something to the camp for good, so it grows
 * with you: a flag, lights along the path, a garden, a hut up the mountain, shooting stars.
 */
enum class Milestone(val title: String, val adds: String, val need: Int, private val unit: String) {
    FLAG(tr("A week at the camp", "Egy hét a táborban"), tr("a flag over the camp", "zászló a tábor fölött"), 7, "days"),
    PATH_LIGHTS(tr("25 activities", "25 tevékenység"), tr("lamps along the river path", "lámpák a folyóparti ösvényen"), 25, "activities"),
    GARDEN(tr("10 hours back", "10 visszanyert óra"), tr("a vegetable garden", "veteményeskert"), 10, "hours back"),
    HUT(tr("A month at the camp", "Egy hónap a táborban"), tr("a hut up the mountain", "kunyhó fent a hegyen"), 30, "days"),
    SHOOTING_STARS(tr("100 activities", "100 tevékenység"), tr("shooting stars at night", "hullócsillagok éjszaka"), 100, "activities"),
    ;

    /** Where you are toward it, out of [need]. */
    fun progress(t: Totals): Int = when (unit) {
        "days" -> t.days
        "activities" -> t.activities
        else -> t.hoursBack
    }.coerceAtMost(need)

    fun reached(t: Totals) = progress(t) >= need

    /** "4 of 7 days" */
    fun label(t: Totals) = tr(
        "${progress(t)} of $need $unit",
        "${progress(t)} / $need ${when (unit) { "days" -> "nap"; "activities" -> "tevékenység"; else -> "visszanyert óra" }}",
    )

    companion object {
        fun reached(t: Totals): Set<Milestone> = entries.filter { it.reached(t) }.toSet()
    }
}
