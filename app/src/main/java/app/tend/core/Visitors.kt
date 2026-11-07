package app.tend.core

import java.time.LocalDate

/**
 * Each day an animal comes to the camp and waits to be greeted. Common ones most days; after a
 * good day (within the minutes, or something done) a rare one may come. Greeting it adds it to
 * the guestbook, with a few acorns.
 */
enum class Visitor(val title: String, val rare: Boolean, val x: Float, val y: Float) {
    DEER("Deer", false, 0.47f, 0.7f),
    FOX("Fox", false, 0.735f, 0.745f),
    RABBIT("Rabbit", false, 0.6f, 0.885f),
    SQUIRREL("Squirrel", false, 0.957f, 0.6f),
    HERON("Heron", true, 0.25f, 0.69f),
    OWL("Owl", true, 0.883f, 0.47f);
}

object Visitors {
    const val ACORNS = 5

    /** Today's visitor: the same all day; [goodDay] (yesterday) opens the door to a rare one. */
    fun of(date: LocalDate, goodDay: Boolean): Visitor {
        val rnd = java.util.Random(date.toEpochDay() * 7919)
        val common = Visitor.entries.filter { !it.rare }
        val rare = Visitor.entries.filter { it.rare }
        return if (goodDay && rnd.nextInt(3) == 0) rare[rnd.nextInt(rare.size)] else common[rnd.nextInt(common.size)]
    }

    /** The guestbook as saved ("DEER:3,FOX:1"), parsed; unknown names are dropped. */
    fun parse(saved: String): Map<Visitor, Int> = saved.split(',').mapNotNull { part ->
        val (name, n) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
        val v = runCatching { Visitor.valueOf(name) }.getOrNull() ?: return@mapNotNull null
        v to (n.toIntOrNull() ?: return@mapNotNull null)
    }.toMap()

    fun format(book: Map<Visitor, Int>): String = book.entries.joinToString(",") { "${it.key.name}:${it.value}" }
}
