package app.tend.core

import java.time.LocalDate

/**
 * Each day an animal comes to the camp and waits to be greeted. Common ones most days; after a
 * good day (within the minutes, or something done) a rare one may come. Greeting it adds it to
 * the guestbook, with a few acorns.
 */
/** How often an animal comes, and what greeting it is worth. */
enum class Rarity(val label: String, val acorns: Int) { COMMON("Common", 5), UNCOMMON("Uncommon", 10), RARE("Rare", 20) }

enum class Visitor(val title: String, val rarity: Rarity, val x: Float, val y: Float) {
    RABBIT("Rabbit", Rarity.COMMON, 0.6f, 0.885f),
    SQUIRREL("Squirrel", Rarity.COMMON, 0.957f, 0.6f),
    HEDGEHOG("Hedgehog", Rarity.COMMON, 0.73f, 0.905f),
    DEER("Deer", Rarity.UNCOMMON, 0.365f, 0.675f),
    FOX("Fox", Rarity.UNCOMMON, 0.735f, 0.745f),
    BEAVER("Beaver", Rarity.UNCOMMON, 0.215f, 0.83f),
    HERON("Heron", Rarity.RARE, 0.25f, 0.69f),
    OWL("Owl", Rarity.RARE, 0.883f, 0.47f);

    /** "a fox", "an owl". */
    val aName: String get() = (if (title.first().lowercaseChar() in "aeiou") "an " else "a ") + title.lowercase()

    val rare: Boolean get() = rarity == Rarity.RARE
}

object Visitors {
    /**
     * Today's visitor, the same all day: mostly common ones, sometimes an uncommon one; a rare
     * one only after a good day (within the minutes, or something done).
     */
    fun of(date: LocalDate, goodDay: Boolean, feeder: Boolean = false): Visitor {
        val rnd = java.util.Random(date.toEpochDay() * 7919)
        val roll = rnd.nextInt(100)
        // A bird feeder in the camp draws more of the shy ones.
        val rare = when { goodDay && feeder -> 30; goodDay -> 20; feeder -> 8; else -> 0 }
        val rarity = when {
            roll < rare -> Rarity.RARE
            roll < rare + (if (goodDay) 35 else 30) + (if (feeder) 10 else 0) -> Rarity.UNCOMMON
            else -> Rarity.COMMON
        }
        val pool = Visitor.entries.filter { it.rarity == rarity }
        return pool[rnd.nextInt(pool.size)]
    }

    /** The guestbook as saved ("DEER:3,FOX:1"), parsed; unknown names are dropped. */
    fun parse(saved: String): Map<Visitor, Int> = saved.split(',').mapNotNull { part ->
        val (name, n) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
        val v = runCatching { Visitor.valueOf(name) }.getOrNull() ?: return@mapNotNull null
        v to (n.toIntOrNull() ?: return@mapNotNull null)
    }.toMap()

    fun format(book: Map<Visitor, Int>): String = book.entries.joinToString(",") { "${it.key.name}:${it.value}" }
}
