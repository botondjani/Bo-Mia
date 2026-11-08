package app.tend.core

/**
 * The clearing on the riverbank: acorns earned by doing, and things bought with them for fixed
 * spots around a camp. Pure rules here; storage is in TendStore, drawing in ui.
 */

/** Where a thing can go: the fire pit, a seat by it, a shelter, the trees, the river, overhead, or a small spot. */
enum class SpotKind { FIRE, SEAT, SHELTER, TREES, WATER, ABOVE, SMALL }

/** A spot in the clearing; [id] is stable (it's what's saved). */
data class Spot(val id: Int, val kind: SpotKind)

/** The camp's seasons (as the months go, north of the equator): the look changes, and each brings its own things. */
enum class Season(val title: String) {
    SPRING("Spring"), SUMMER("Summer"), AUTUMN("Autumn"), WINTER("Winter");

    companion object {
        fun of(date: java.time.LocalDate): Season = when (date.monthValue) { 3, 4, 5 -> SPRING; 6, 7, 8 -> SUMMER; 9, 10, 11 -> AUTUMN; else -> WINTER }
    }
}

/**
 * [spot]: the one spot this thing always goes to (the hammock between two trees, the swing under the branch).
 * [season]: sold only in that season (once bought, it stays).
 */
data class Thing(val id: String, val name: String, val price: Int, val kind: SpotKind, val spot: Int? = null, val season: Season? = null)

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
        Thing("kettle", "Fire and kettle", 120, SpotKind.FIRE),
        Thing("pizza_oven", "Pizza oven", 250, SpotKind.FIRE),
        // Seats by the fire.
        Thing("log_bench", "Log bench", 50, SpotKind.SEAT),
        Thing("picnic_table", "Picnic table", 90, SpotKind.SEAT),
        Thing("rocking_chair", "Rocking chair", 120, SpotKind.SEAT),
        // Shelters at the back, for the night.
        Thing("tent", "Tent", 150, SpotKind.SHELTER),
        Thing("bell_tent", "Canvas tent", 300, SpotKind.SHELTER),
        Thing("cabin", "Cabin", 700, SpotKind.SHELTER),
        // In the trees.
        Thing("swing", "Swing", 70, SpotKind.TREES, spot = 6),
        Thing("hammock", "Hammock", 90, SpotKind.TREES, spot = 5),
        Thing("treehouse", "Treehouse", 300, SpotKind.TREES, spot = 6),
        // On the river.
        Thing("raft", "Raft", 150, SpotKind.WATER, spot = 7),
        Thing("canoe", "Canoe", 200, SpotKind.WATER, spot = 7),
        Thing("dock", "Dock", 400, SpotKind.WATER, spot = 8),
        // Strung between the trees over the camp.
        Thing("wind_chimes", "Wind chimes", 40, SpotKind.ABOVE),
        Thing("bird_feeder", "Bird feeder", 50, SpotKind.ABOVE),
        Thing("string_lights", "String lights", 60, SpotKind.ABOVE),
        // Each season brings three of its own, sold only while it lasts.
        Thing("tulips", "Tulips", 20, SpotKind.SMALL, season = Season.SPRING),
        Thing("watering_can", "Watering can", 25, SpotKind.SMALL, season = Season.SPRING),
        Thing("birdhouse", "Birdhouse", 45, SpotKind.SMALL, season = Season.SPRING),
        Thing("beach_ball", "Beach ball", 15, SpotKind.SMALL, season = Season.SUMMER),
        Thing("watermelon", "Watermelon", 20, SpotKind.SMALL, season = Season.SUMMER),
        Thing("deck_chair", "Deck chair", 50, SpotKind.SMALL, season = Season.SUMMER),
        Thing("leaf_pile", "Leaf pile", 20, SpotKind.SMALL, season = Season.AUTUMN),
        Thing("pumpkins", "Pumpkins", 30, SpotKind.SMALL, season = Season.AUTUMN),
        Thing("scarecrow", "Scarecrow", 60, SpotKind.SMALL, season = Season.AUTUMN),
        Thing("gift_boxes", "Presents", 25, SpotKind.SMALL, season = Season.WINTER),
        Thing("sled", "Sled", 30, SpotKind.SMALL, season = Season.WINTER),
        Thing("snowman", "Snowman", 40, SpotKind.SMALL, season = Season.WINTER),
    )

    /** Things no longer sold, with what they cost: whoever had one gets it back in acorns. */
    val retired: Map<String, Int> = mapOf(
        "mushrooms" to 10, "backpack" to 15, "camp_chairs" to 70, "workshop" to 500, "bike" to 120, "boat" to 600, "bunting" to 40, "grill" to 80,
    )

    fun byId(id: String): Thing? = all.firstOrNull { it.id == id }

    /** What the shop always has for [kind] (not the seasons' own). */
    fun forKind(kind: SpotKind): List<Thing> = all.filter { it.kind == kind && it.season == null }.sortedBy { it.price }

    /** The season's own things, cheapest first. */
    fun forSeason(season: Season): List<Thing> = all.filter { it.season == season }.sortedBy { it.price }
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
        thing.spot?.let { return it }
        val ofKind = spots.filter { it.kind == thing.kind }
        ofKind.firstOrNull { it.id !in placed }?.let { return it.id }
        return ofKind.minByOrNull { Things.byId(placed.getValue(it.id))?.price ?: 0 }?.id
    }

    /** Things that can stand anywhere on the meadow: small things, seats and shelters. The fire, trees, river and overhead keep their place. */
    fun movable(thing: Thing): Boolean = thing.spot == null && thing.kind in setOf(SpotKind.SMALL, SpotKind.SEAT, SpotKind.SHELTER)

    /**
     * Where a thing of [kind] may stand, nearest to (x, y) (scene fractions; [ratio] is the scene's width
     * over its height): on the meadow, off the fire pit and the tree trunks. Shelters stay towards the back.
     */
    fun keepOnMeadow(kind: SpotKind, x: Float, y: Float, ratio: Float = 1.8f): Pair<Float, Float> {
        val (x0, x1, y0, y1) = when (kind) {
            SpotKind.SHELTER -> listOf(0.46f, 0.93f, 0.6f, 0.8f)
            SpotKind.SEAT -> listOf(0.43f, 0.95f, 0.68f, 0.94f)
            else -> listOf(0.41f, 0.97f, 0.64f, 0.965f)
        }
        var px = x.coerceIn(x0, x1)
        var py = y.coerceIn(y0, y1)
        // Bigger things keep farther off: half widths in scene heights, turned into fractions.
        val pad = when (kind) { SpotKind.SHELTER -> 0.09f; SpotKind.SEAT -> 0.04f; else -> 0.015f }
        // Centred a little below the pit: things stand up from their feet, so one just in front would cover the stones.
        for ((cx, cy, r) in listOf(Triple(0.625f, 0.805f, 0.13f), Triple(0.875f, 0.725f, 0.035f), Triple(0.975f, 0.76f, 0.035f))) {
            val rx = (r + pad) / ratio
            val ry = (r + pad) * 0.42f
            val dx = (px - cx) / rx
            val dy = (py - cy) / ry
            val d = kotlin.math.sqrt(dx * dx + dy * dy)
            if (d < 1f) {
                // Out to the edge, the shortest way (straight down when right on the centre).
                val (ux, uy) = if (d < 1e-4f) 0f to 1f else dx / d to dy / d
                px = cx + ux * rx
                py = cy + uy * ry
            }
        }
        return px.coerceIn(x0, x1) to py.coerceIn(y0, y1)
    }

    /** Moved things as saved: "spot:x:y,…" (scene fractions). */
    fun formatLayout(layout: Map<Int, Pair<Float, Float>>): String =
        layout.entries.joinToString(",") { "${it.key}:%.4f:%.4f".format(java.util.Locale.ROOT, it.value.first, it.value.second) }

    /**
     * Moved things from [saved], for what's at each spot in [placed]: only movable things, each kept to
     * where it may stand (so a friend's camp can't put a tent in the river).
     */
    fun parseLayout(saved: String, placed: Map<Int, String>): Map<Int, Pair<Float, Float>> = saved.split(',').mapNotNull { part ->
        val p = part.split(':').takeIf { it.size == 3 } ?: return@mapNotNull null
        val spot = p[0].toIntOrNull() ?: return@mapNotNull null
        val thing = placed[spot]?.let(Things::byId)?.takeIf(::movable) ?: return@mapNotNull null
        val x = p[1].toFloatOrNull()?.takeIf { it.isFinite() } ?: return@mapNotNull null
        val y = p[2].toFloatOrNull()?.takeIf { it.isFinite() } ?: return@mapNotNull null
        spot to keepOnMeadow(thing.kind, x, y)
    }.toMap()

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
    /** Out of minutes, once a day: five minutes now, not counted, for this many acorns. */
    const val BORROW = 20
    const val BORROW_MINUTES = 5

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
