package app.tend.core

/**
 * The clearing on the riverbank: acorns earned by doing, and things bought with them for fixed
 * spots around a camp. Pure rules here; storage is in TendStore, drawing in ui.
 */

/** Where a thing can go: the fire pit, a seat by it, a shelter, the trees, the river, overhead, or a small spot. */
enum class SpotKind { FIRE, SEAT, SHELTER, TREES, WATER, ABOVE, SMALL }

/** A spot in the clearing; [id] is stable (it's what's saved). */
data class Spot(val id: Int, val kind: SpotKind)

data class Thing(val id: String, val name: String, val price: Int, val kind: SpotKind)

object Things {
    val all: List<Thing> = listOf(
        // Small things around the camp: always something to buy today, and each one gets used.
        Thing("wildflowers", "Wildflowers", 10, SpotKind.SMALL),
        Thing("lantern", "Lantern", 15, SpotKind.SMALL),
        Thing("woodpile", "Firewood", 15, SpotKind.SMALL),
        Thing("guitar", "Guitar", 30, SpotKind.SMALL),
        Thing("fishing_rod", "Fishing rod", 40, SpotKind.SMALL),
        Thing("telescope", "Telescope", 80, SpotKind.SMALL),
        // The fire pit: each one cooks something else.
        Thing("campfire", "Campfire", 40, SpotKind.FIRE),
        Thing("grill", "Grill", 80, SpotKind.FIRE),
        Thing("kettle", "Fire and kettle", 120, SpotKind.FIRE),
        Thing("pizza_oven", "Pizza oven", 250, SpotKind.FIRE),
        // Seats by the fire.
        Thing("log_bench", "Log bench", 50, SpotKind.SEAT),
        Thing("picnic_table", "Picnic table", 90, SpotKind.SEAT),
        // Shelters at the back, for the night.
        Thing("tent", "Tent", 150, SpotKind.SHELTER),
        Thing("bell_tent", "Canvas tent", 300, SpotKind.SHELTER),
        Thing("cabin", "Cabin", 700, SpotKind.SHELTER),
        // In the trees.
        Thing("swing", "Swing", 70, SpotKind.TREES),
        Thing("hammock", "Hammock", 90, SpotKind.TREES),
        Thing("treehouse", "Treehouse", 300, SpotKind.TREES),
        // On the river.
        Thing("canoe", "Canoe", 200, SpotKind.WATER),
        Thing("dock", "Dock", 400, SpotKind.WATER),
        // Strung between the trees over the camp.
        Thing("string_lights", "String lights", 60, SpotKind.ABOVE),
    )

    /** Things no longer sold, with what they cost: whoever had one gets it back in acorns. */
    val retired: Map<String, Int> = mapOf(
        "mushrooms" to 10, "backpack" to 15, "camp_chairs" to 70, "workshop" to 500, "bike" to 120, "boat" to 600, "bunting" to 40,
    )

    fun byId(id: String): Thing? = all.firstOrNull { it.id == id }

    fun forKind(kind: SpotKind): List<Thing> = all.filter { it.kind == kind }.sortedBy { it.price }
}

object Clearing {
    /** 10 big spots and 8 small ones around the camp. Ids never change: placements are saved by id. */
    val spots: List<Spot> = listOf(
        Spot(0, SpotKind.FIRE),
        Spot(1, SpotKind.SEAT), Spot(2, SpotKind.SEAT),
        Spot(3, SpotKind.SHELTER), Spot(4, SpotKind.SHELTER),
        Spot(5, SpotKind.TREES), Spot(6, SpotKind.TREES),
        Spot(7, SpotKind.WATER), Spot(8, SpotKind.WATER),
        Spot(9, SpotKind.ABOVE),
    ) + (0 until 8).map { Spot(10 + it, SpotKind.SMALL) }

    fun spot(id: Int): Spot? = spots.firstOrNull { it.id == id }

    /** What a new user starts with: a campfire and some flowers on day one. */
    const val STARTING_ACORNS = 50

    /**
     * Where a bought [thing] goes: a free spot of its kind, else the spot whose thing is cheapest
     * (it's swapped out). Null when it's already in the camp.
     */
    fun spotFor(placed: Map<Int, String>, thing: Thing): Int? {
        if (thing.id in placed.values) return null
        val ofKind = spots.filter { it.kind == thing.kind }
        ofKind.firstOrNull { it.id !in placed }?.let { return it.id }
        return ofKind.minByOrNull { Things.byId(placed.getValue(it.id))?.price ?: 0 }?.id
    }

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
