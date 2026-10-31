package app.tend.core

/**
 * Little things the companion wears when a habit has held for a week. Never body shape, only
 * accessories; when the habit fades they go to the shelf and come back when it returns.
 */
enum class Accessory(val title: String, val how: String, val short: String, val tasks: Set<String>, val days: Int) {
    HEADBAND("Sweatband", "4 workout days in a week", "4 workout days", setOf("move"), 4),
    GLASSES("Reading glasses", "4 days of reading or learning in a week", "4 reading days", setOf("read", "learn"), 4),
    HAT("Sun hat", "4 days outside in a week", "4 days outside", setOf("outside"), 4),
    FLOWER("Flower", "3 days with friends or a hobby in a week", "3 social days", setOf("friends", "hobby"), 3),
}

object Accessories {
    /** Worn right now, from the done tasks of the last 7 days (today included). */
    fun active(lastDays: List<Set<String>>): Set<Accessory> {
        val week = lastDays.takeLast(7)
        return Accessory.entries.filter { a -> week.count { day -> day.any { it in a.tasks } } >= a.days }.toSet()
    }
}
