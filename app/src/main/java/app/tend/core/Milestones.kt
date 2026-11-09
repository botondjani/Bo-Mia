package app.tend.core

/** How far you've come: days at the camp, activities done, hours got back from scrolling. */
data class Totals(val days: Int, val activities: Int, val hoursBack: Int)

/**
 * Milestones over the months: each one reached adds something to the camp for good, so it grows
 * with you: a flag, lights along the path, a garden, a hut up the mountain, shooting stars.
 */
enum class Milestone(val title: String, val adds: String, val need: Int, private val unit: String) {
    FLAG("A week at the camp", "a flag over the camp", 7, "days"),
    PATH_LIGHTS("25 activities", "lamps along the river path", 25, "activities"),
    GARDEN("10 hours back", "a vegetable garden", 10, "hours back"),
    HUT("A month at the camp", "a hut up the mountain", 30, "days"),
    SHOOTING_STARS("100 activities", "shooting stars at night", 100, "activities"),
    ;

    /** Where you are toward it, out of [need]. */
    fun progress(t: Totals): Int = when (unit) {
        "days" -> t.days
        "activities" -> t.activities
        else -> t.hoursBack
    }.coerceAtMost(need)

    fun reached(t: Totals) = progress(t) >= need

    /** "4 of 7 days" */
    fun label(t: Totals) = "${progress(t)} of $need $unit"

    companion object {
        fun reached(t: Totals): Set<Milestone> = entries.filter { it.reached(t) }.toSet()
    }
}
