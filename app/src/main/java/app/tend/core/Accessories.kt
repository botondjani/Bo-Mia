package app.tend.core

/**
 * Little things the companion wears when a habit has held for a week. Never body shape, only
 * accessories; when the habit fades they go to the shelf and come back when it returns.
 */
enum class Accessory(
    val title: String, val how: String, val short: String, val tasks: Set<String>, val days: Int,
    /** A season's outfit, bought in the shop (with Plus) and worn by choice, not earned by a habit. */
    val outfit: Season? = null,
) {
    // Quick ones for the first days: any activity counts (an empty set means any).
    LEAF(tr("Lucky leaf", "Szerencselevél"), tr("Any activity, once", "Bármilyen tevékenység, egyszer"), tr("1 activity", "1 tevékenység"), emptySet(), 1),
    BOWTIE(tr("Bow tie", "Csokornyakkendő"), tr("Activities on 2 days in a week", "Egy héten 2 napon tevékenység"), tr("2 active days", "2 aktív nap"), emptySet(), 2),
    HEADBAND(tr("Sweatband", "Homlokpánt"), tr("4 workout days in a week", "Egy héten 4 edzős nap"), tr("4 workout days", "4 edzős nap"), setOf("move"), 4),
    GLASSES(tr("Reading glasses", "Olvasószemüveg"), tr("4 days of reading or learning in a week", "Egy héten 4 nap olvasás vagy tanulás"), tr("4 reading days", "4 olvasós nap"), setOf("read", "learn"), 4),
    HAT(tr("Sun hat", "Szalmakalap"), tr("4 days outside in a week", "Egy héten 4 nap a szabadban"), tr("4 days outside", "4 nap a szabadban"), setOf("outside"), 4),
    FLOWER(tr("Flower", "Virág"), tr("3 days with friends or a hobby in a week", "Egy héten 3 nap barátokkal vagy hobbival"), tr("3 social days", "3 társas nap"), setOf("friends", "hobby"), 3),
    // One more for the automatic sleep goal.
    NIGHTCAP(tr("Nightcap", "Hálósapka"), tr("2 good nights in a week", "Egy héten 2 jó éjszaka"), tr("2 good nights", "2 jó éjszaka"), setOf("sleep"), 2),

    // The seasons' outfits.
    FLOWER_CROWN(tr("Flower crown", "Virágkoszorú"), tr("This spring, in the shop", "Idén tavasszal a boltban"), "spring", emptySet(), 0, Season.SPRING),
    SUNGLASSES(tr("Sunglasses", "Napszemüveg"), tr("This summer, in the shop", "Idén nyáron a boltban"), "summer", emptySet(), 0, Season.SUMMER),
    SCARF(tr("Knitted scarf", "Kötött sál"), tr("This autumn, in the shop", "Idén ősszel a boltban"), "autumn", emptySet(), 0, Season.AUTUMN),
    BOBBLE_HAT(tr("Bobble hat", "Bojtos sapka"), tr("This winter, in the shop", "Idén télen a boltban"), "winter", emptySet(), 0, Season.WINTER),
    ;

    companion object {
        /** The ones earned by habits (the outfits are bought). */
        val habits: List<Accessory> get() = entries.filter { it.outfit == null }

        fun outfitFor(season: Season): Accessory = entries.first { it.outfit == season }

        /** What an outfit costs in acorns. */
        const val OUTFIT_PRICE = 60
    }
}

object Accessories {
    /** Worn right now, from the done tasks of the last 7 days (today included). */
    fun active(lastDays: List<Set<String>>): Set<Accessory> {
        val week = lastDays.takeLast(7)
        return Accessory.habits.filter { a -> week.count { day -> day.any { a.tasks.isEmpty() || it in a.tasks } } >= a.days }.toSet()
    }

    /** On top of the head there's room for one: the hat first, then the nightcap, then the leaf. */
    val TOP = listOf(Accessory.HAT, Accessory.NIGHTCAP, Accessory.LEAF)

    /** What's actually on, of the [earned] ones: not the ones taken [off], and one thing on top of the head. */
    fun worn(earned: Set<Accessory>, off: Set<Accessory>): Set<Accessory> {
        val on = earned - off
        val top = TOP.firstOrNull { it in on }
        return on.filter { it !in TOP || it == top }.toSet()
    }

    /** Tapping [a] in the collection: off if it's on; on if it isn't (taking its place on the head, if that's where it goes). */
    fun toggle(a: Accessory, earned: Set<Accessory>, off: Set<Accessory>): Set<Accessory> = when {
        a !in earned -> off
        a in worn(earned, off) -> off + a
        a in TOP -> off - a + TOP.filter { it != a && it in earned }
        else -> off - a
    }
}
