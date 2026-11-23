package app.tend.core

/**
 * Little things the companion wears: earned by a habit held for a week (kept for good once earned),
 * or bought in the shop. Never body shape, only accessories.
 */
enum class Accessory(
    val title: String, val how: String, val short: String, val tasks: Set<String>, val days: Int,
    /** A season's outfit, bought in the shop (with Plus) and worn by choice, not earned by a habit. */
    val outfit: Season? = null,
    /** An outfit sold all year round. */
    val always: Boolean = false,
    /** An outfit sold only during a camp event (a few days a year). */
    val event: CampEvent? = null,
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

    // Outfits for any time of year.
    BERET(tr("Beret", "Svájcisapka"), tr("In the shop", "A boltban"), "beret", emptySet(), 0, always = true),
    PARTY_HAT(tr("Party hat", "Csákó"), tr("In the shop", "A boltban"), "party", emptySet(), 0, always = true),
    BANDANA(tr("Bandana", "Kendő"), tr("In the shop", "A boltban"), "bandana", emptySet(), 0, always = true),
    HEADPHONES(tr("Headphones", "Fejhallgató"), tr("In the shop", "A boltban"), "headphones", emptySet(), 0, always = true),
    CROWN(tr("Little crown", "Kis korona"), tr("In the shop", "A boltban"), "crown", emptySet(), 0, always = true),
    HAIR_BOW(tr("Hair bow", "Masni"), tr("In the shop", "A boltban"), "bow", emptySet(), 0, always = true),
    CHEF_HAT(tr("Chef's hat", "Szakácssapka"), tr("In the shop", "A boltban"), "chef", emptySet(), 0, always = true),
    CAPTAIN_HAT(tr("Captain's cap", "Kapitánysapka"), tr("In the shop", "A boltban"), "captain", emptySet(), 0, always = true),
    COWBOY_HAT(tr("Cowboy hat", "Cowboykalap"), tr("In the shop", "A boltban"), "cowboy", emptySet(), 0, always = true),
    WIZARD_HAT(tr("Wizard hat", "Varázslósüveg"), tr("In the shop", "A boltban"), "wizard", emptySet(), 0, always = true),
    MUSTACHE(tr("Mustache", "Bajusz"), tr("In the shop", "A boltban"), "mustache", emptySet(), 0, always = true),
    MONOCLE(tr("Monocle", "Monokli"), tr("In the shop", "A boltban"), "monocle", emptySet(), 0, always = true),
    STAR_GLASSES(tr("Star glasses", "Csillagszemüveg"), tr("In the shop", "A boltban"), "stars", emptySet(), 0, always = true),

    // Events' outfits: in the shop only while the event is on.
    WITCH_HAT(tr("Witch's hat", "Boszorkánykalap"), tr("On pumpkin night, in the shop", "Töklámpás-estén a boltban"), "witch", emptySet(), 0, event = CampEvent.PUMPKINS),
    SANTA_HAT(tr("Santa hat", "Mikulássapka"), tr("In Advent, in the shop", "Adventkor a boltban"), "santa", emptySet(), 0, event = CampEvent.ADVENT),
    GARLAND(tr("Flower garland", "Virágfüzér"), tr("At Midsummer, in the shop", "Szent Iván-éjkor a boltban"), "garland", emptySet(), 0, event = CampEvent.MIDSUMMER),
    ;

    /** Bought and worn by choice (a season's, or any time's), not earned by a habit. */
    val isOutfit: Boolean get() = outfit != null || always || event != null

    /** For sale in the shop on [date]: all year, this season's, or this event's. */
    fun forSale(date: java.time.LocalDate): Boolean = always || (outfit != null && outfit == Season.of(date)) || event?.on(date) == true

    companion object {
        /** The ones earned by habits (the outfits are bought). */
        val habits: List<Accessory> get() = entries.filter { !it.isOutfit }

        /** The outfits sold all year round. */
        val anytime: List<Accessory> get() = entries.filter { it.always }

        fun outfitFor(season: Season): Accessory = entries.first { it.outfit == season }

        /** What an outfit costs in acorns. */
        const val OUTFIT_PRICE = 90
    }
}

/** Where on the companion a thing goes: one thing per place. */
enum class Slot(val title: String) {
    HEAD(tr("Head", "Fej")), EYES(tr("Eyes", "Szem")), NECK(tr("Neck", "Nyak")), DECOR(tr("Extra", "Dísz"));
}

val Accessory.slot: Slot get() = when (this) {
    Accessory.GLASSES, Accessory.SUNGLASSES, Accessory.MONOCLE, Accessory.STAR_GLASSES -> Slot.EYES
    Accessory.BOWTIE, Accessory.SCARF, Accessory.BANDANA, Accessory.GARLAND -> Slot.NECK
    Accessory.LEAF, Accessory.FLOWER, Accessory.HAIR_BOW, Accessory.MUSTACHE -> Slot.DECOR
    else -> Slot.HEAD
}

object Accessories {
    /** The habits held right now, from the done tasks of the last 7 days (today included): the ones that shine. */
    fun active(lastDays: List<Set<String>>): Set<Accessory> {
        val week = lastDays.takeLast(7)
        return Accessory.habits.filter { a -> week.count { day -> day.any { a.tasks.isEmpty() || it in a.tasks } } >= a.days }.toSet()
    }

    /** What actually shows of [all]: one thing per place, a bought one before an earned one (for older or friends' sets). */
    fun shown(all: Set<Accessory>): Set<Accessory> =
        all.groupBy { it.slot }.values.map { same -> same.firstOrNull { it.isOutfit } ?: same.first() }.toSet()

    /** The earlier versions' worn set, put in its places: what showed then shows now. */
    fun fromOld(worn: Set<Accessory>): Map<Slot, Accessory> {
        // Then the hat went before the nightcap and the leaf; any bought thing before an earned one.
        val order = listOf(Accessory.HAT, Accessory.NIGHTCAP, Accessory.HEADBAND, Accessory.LEAF)
        return shown(worn.sortedBy { if (it in order) order.indexOf(it) else -1 }.toCollection(LinkedHashSet())).associateBy { it.slot }
    }

    /**
     * Dressed by itself, from what's [owned]: a bobble hat and scarf in winter or snow, sunglasses
     * and the sun hat on a summer day, the flower crown in spring, the scarf in autumn. Where none of
     * those fits, what was chosen stays.
     */
    fun auto(owned: Set<Accessory>, chosen: Map<Slot, Accessory>, season: Season, weather: Weather?): Map<Slot, Accessory> {
        val out = chosen.toMutableMap()
        fun put(a: Accessory) { if (a in owned) out[a.slot] = a }
        val cold = season == Season.WINTER || weather == Weather.SNOW
        val sunny = weather == null || weather == Weather.CLEAR
        when {
            cold -> { put(Accessory.BOBBLE_HAT); put(Accessory.SCARF) }
            season == Season.SUMMER -> { put(Accessory.HAT); if (sunny) put(Accessory.SUNGLASSES) }
            season == Season.SPRING -> { put(Accessory.FLOWER_CROWN); put(Accessory.FLOWER) }
            season == Season.AUTUMN -> put(Accessory.SCARF)
        }
        // No sunglasses in the rain, whatever was chosen.
        if (out[Slot.EYES] == Accessory.SUNGLASSES && !sunny) out.remove(Slot.EYES)
        return out
    }

    fun encode(m: Map<Slot, Accessory>) = m.entries.joinToString(";") { "${it.key.name}=${it.value.name}" }
    fun decode(s: String): Map<Slot, Accessory> = s.split(';').mapNotNull { e ->
        val (k, v) = e.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        val slot = Slot.entries.firstOrNull { it.name == k } ?: return@mapNotNull null
        Accessory.entries.firstOrNull { it.name == v && it.slot == slot }?.let { slot to it }
    }.toMap()
}
