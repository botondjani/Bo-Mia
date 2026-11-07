package app.tend.core

/**
 * The clearing on the riverbank: acorns earned by doing, and things bought with them for fixed
 * spots. Pure rules here; storage is in TendStore, drawing in ui.
 */

/** Where a thing can stand. */
enum class SpotKind { FIRE, TREES, BANK, WATER, BACK, SMALL }

/** A spot in the clearing; [id] is stable (it's what's saved). */
data class Spot(val id: Int, val kind: SpotKind)

data class Thing(val id: String, val name: String, val price: Int, val kind: SpotKind)

object Things {
    val all: List<Thing> = listOf(
        // Small things: always something to buy today.
        Thing("flowers", "Wildflowers", 10, SpotKind.SMALL),
        Thing("mushrooms", "Mushrooms", 10, SpotKind.SMALL),
        Thing("stones", "River stones", 10, SpotKind.SMALL),
        Thing("sunflowers", "Sunflowers", 15, SpotKind.SMALL),
        Thing("lantern", "Lantern", 15, SpotKind.SMALL),
        Thing("fireflies", "Fireflies", 15, SpotKind.SMALL),
        // The fire pit.
        Thing("campfire", "Campfire", 40, SpotKind.FIRE),
        Thing("grill", "Grill", 80, SpotKind.FIRE),
        Thing("pizza_oven", "Pizza oven", 250, SpotKind.FIRE),
        // Under the trees.
        Thing("log_bench", "Log bench", 50, SpotKind.TREES),
        Thing("swing", "Swing", 70, SpotKind.TREES),
        Thing("hammock", "Hammock", 90, SpotKind.TREES),
        Thing("tent", "Tent", 150, SpotKind.TREES),
        Thing("treehouse", "Treehouse", 300, SpotKind.TREES),
        // On the bank.
        Thing("fishing_chair", "Fishing chair", 60, SpotKind.BANK),
        Thing("bike", "Bike", 120, SpotKind.BANK),
        // On the water.
        Thing("canoe", "Canoe", 200, SpotKind.WATER),
        Thing("boat", "Boat", 600, SpotKind.WATER),
        Thing("dock", "Dock", 900, SpotKind.WATER),
        // At the back.
        Thing("flag", "Flag", 40, SpotKind.BACK),
        Thing("workshop", "Workshop", 500, SpotKind.BACK),
        Thing("cabin", "Cabin", 800, SpotKind.BACK),
    )

    fun byId(id: String): Thing? = all.firstOrNull { it.id == id }

    fun forKind(kind: SpotKind): List<Thing> = all.filter { it.kind == kind }.sortedBy { it.price }
}

object Clearing {
    /** 10 big spots and 20 small ones. Ids never change: placements are saved by id. */
    val spots: List<Spot> = buildList {
        add(Spot(0, SpotKind.FIRE))
        repeat(3) { add(Spot(1 + it, SpotKind.TREES)) }
        repeat(2) { add(Spot(4 + it, SpotKind.BANK)) }
        repeat(2) { add(Spot(6 + it, SpotKind.WATER)) }
        repeat(2) { add(Spot(8 + it, SpotKind.BACK)) }
        repeat(20) { add(Spot(10 + it, SpotKind.SMALL)) }
    }

    fun spot(id: Int): Spot? = spots.firstOrNull { it.id == id }

    /** What a new user starts with: enough for a first small thing. */
    const val STARTING_ACORNS = 25

    /** Selling gives half back. */
    fun sellValue(thing: Thing): Int = thing.price / 2

    /** Placements as saved: "spot:thing,spot:thing". Unknown spots or things are dropped. */
    fun parse(saved: String): Map<Int, String> = saved.split(',').mapNotNull { part ->
        val (spot, thing) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
        val s = spot.toIntOrNull()?.let(::spot) ?: return@mapNotNull null
        val t = Things.byId(thing) ?: return@mapNotNull null
        if (t.kind != s.kind) null else s.id to t.id
    }.toMap()

    fun format(placed: Map<Int, String>): String = placed.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }

    /** Old garden plants and base logs, turned into acorns once. */
    fun acornsFor(oldPlants: List<Plant>): Int = oldPlants.sumOf {
        when (it) { Plant.SPROUT -> 15; Plant.TULIP -> 25; Plant.SUNFLOWER -> 40 }
    }
}

/** How acorns are earned. Never by budget size: the same day is worth the same with any budget. */
object Acorns {
    const val PER_ACTIVITY = 10
    const val ACTIVITY_CAP = 50
    const val PER_NOT_NOW = 2
    const val NOT_NOW_CAP = 10
    const val WITHIN_BUDGET = 10
    const val BELOW_NORMAL_CAP = 20
    const val CHALLENGE = 60
    const val LEVEL_UP = 30

    fun forActivities(done: Int): Int = (done * PER_ACTIVITY).coerceIn(0, ACTIVITY_CAP)

    fun forNotNows(count: Int): Int = (count * PER_NOT_NOW).coerceIn(0, NOT_NOW_CAP)

    /**
     * At day close: +10 for staying within the day's budget, and +1 per 10 minutes below your own
     * normal (null while it isn't known yet).
     */
    fun atDayClose(usedMinutes: Int, availableMinutes: Int, normalMinutes: Int?): Int {
        val within = if (usedMinutes <= availableMinutes) WITHIN_BUDGET else 0
        val below = normalMinutes?.let { ((it - usedMinutes) / 10).coerceIn(0, BELOW_NORMAL_CAP) } ?: 0
        return within + below
    }

    /** Your normal from days with tend, when nothing from before is known: needs a week. */
    fun normalFrom(usedMinutesPerDay: List<Int>): Int? =
        usedMinutesPerDay.takeIf { it.size >= 7 }?.take(7)?.average()?.toInt()
}
