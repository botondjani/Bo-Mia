package app.tend.core

/**
 * Little things the companion wears when a habit has held for a week. Never body shape, only
 * accessories; when the habit fades they go to the shelf and come back when it returns.
 */
enum class Accessory(
    val title: String, val how: String, val short: String, val tasks: Set<String>, val days: Int,
    /** Lives in the garden rather than on the companion. */
    val garden: Boolean = false,
) {
    // Quick ones for the first days: any activity counts (an empty set means any).
    LEAF("Lucky leaf", "Any activity, once", "1 activity", emptySet(), 1),
    BOWTIE("Bow tie", "Activities on 2 days in a week", "2 active days", emptySet(), 2),
    HEADBAND("Sweatband", "4 workout days in a week", "4 workout days", setOf("move"), 4),
    GLASSES("Reading glasses", "4 days of reading or learning in a week", "4 reading days", setOf("read", "learn"), 4),
    HAT("Sun hat", "4 days outside in a week", "4 days outside", setOf("outside"), 4),
    FLOWER("Flower", "3 days with friends or a hobby in a week", "3 social days", setOf("friends", "hobby"), 3),
    // One more for each automatic activity, and the garden gets some too.
    NIGHTCAP("Nightcap", "2 good nights in a week", "2 good nights", setOf("sleep"), 2),
    BUNTING("Bunting", "3 workout days in a week: flags over the garden", "3 workout days", setOf("move"), 3, garden = true),
    BIRDHOUSE("Birdhouse", "3 days outside in a week: a home for the birds", "3 days outside", setOf("outside"), 3, garden = true),
    LANTERNS("Lanterns", "4 good nights in a week: lights in the garden", "4 good nights", setOf("sleep"), 4, garden = true),
}

object Accessories {
    /** Worn right now, from the done tasks of the last 7 days (today included). */
    fun active(lastDays: List<Set<String>>): Set<Accessory> {
        val week = lastDays.takeLast(7)
        return Accessory.entries.filter { a -> week.count { day -> day.any { a.tasks.isEmpty() || it in a.tasks } } >= a.days }.toSet()
    }
}
