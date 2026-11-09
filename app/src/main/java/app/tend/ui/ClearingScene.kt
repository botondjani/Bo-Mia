package app.tend.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import app.tend.core.Accessory
import app.tend.core.Avatar
import app.tend.core.Clearing
import app.tend.core.Mood
import app.tend.core.SpotKind

/*
 * The riverside clearing, drawn like an evening-lit camp: a river winding into the forest on the
 * left, pines and birches framing a meadow, a stone fire pit in the middle with seats around it,
 * shelters behind, lights strung between the trees. Golden-hour light by day; at night everything
 * sinks under a blue veil and only the fire, lanterns, string lights and windows glow.
 */

// Palette: warm greens, soft wood, canvas; small bright details only.
// The colours that change with the season (summer's here; see [applySeason]).
private var SkyTop = Color(0xFFB9D3DF)
private var SkyLow = Color(0xFFF4E3BC)
private var Haze1 = Color(0xFFA7BFA9)
private var Haze2 = Color(0xFF7FA184)
private val PineDark = Color(0xFF26432F)
private val PineMid = Color(0xFF335A3E)
private var PineLit = Color(0xFF4F7A48)
private var LeafDark = Color(0xFF2E5236)
private var LeafMid = Color(0xFF41704A)
private var LeafLit = Color(0xFF7C9E55)
private var MeadowFar = Color(0xFF9DB874)
private var Meadow = Color(0xFF86A85E)
private var MeadowDeep = Color(0xFF5F8547)
private var GrassDark = Color(0xFF4D7139)
private val Dirt = Color(0xFFA88C66)
private var RiverFar = Color(0xFF9DB7B4)
private var RiverNear = Color(0xFF557A7E)
private var season = app.tend.core.Season.SUMMER

/** The scene's colours for [s]: fresh greens and blossom, summer, warm leaves, or snow. */
private fun applySeason(s: app.tend.core.Season) {
    if (s == season) return
    season = s
    SkyTop = Color(if (s == app.tend.core.Season.WINTER) 0xFFC9D8E2 else 0xFFB9D3DF)
    SkyLow = Color(when (s) { app.tend.core.Season.WINTER -> 0xFFEDEFEF; app.tend.core.Season.AUTUMN -> 0xFFF6DDB4; else -> 0xFFF4E3BC })
    Haze1 = Color(when (s) { app.tend.core.Season.WINTER -> 0xFFC6D2D2; app.tend.core.Season.AUTUMN -> 0xFFBCB894; else -> 0xFFA7BFA9 })
    Haze2 = Color(when (s) { app.tend.core.Season.WINTER -> 0xFF9DB0AE; app.tend.core.Season.AUTUMN -> 0xFF9A9A6C; else -> 0xFF7FA184 })
    PineLit = Color(if (s == app.tend.core.Season.WINTER) 0xFFE4ECEC else 0xFF4F7A48)
    LeafDark = Color(when (s) { app.tend.core.Season.SPRING -> 0xFF3B6A3E; app.tend.core.Season.AUTUMN -> 0xFF8E4A22; app.tend.core.Season.WINTER -> 0xFF5B6F68; else -> 0xFF2E5236 })
    LeafMid = Color(when (s) { app.tend.core.Season.SPRING -> 0xFF5E9A4E; app.tend.core.Season.AUTUMN -> 0xFFC66E2C; app.tend.core.Season.WINTER -> 0xFFA9BAB7; else -> 0xFF41704A })
    LeafLit = Color(when (s) { app.tend.core.Season.SPRING -> 0xFFA6C96A; app.tend.core.Season.AUTUMN -> 0xFFE3A440; app.tend.core.Season.WINTER -> 0xFFF4F7F7; else -> 0xFF7C9E55 })
    MeadowFar = Color(when (s) { app.tend.core.Season.SPRING -> 0xFFA3C77A; app.tend.core.Season.AUTUMN -> 0xFFAAB06C; app.tend.core.Season.WINTER -> 0xFFF2F5F5; else -> 0xFF9DB874 })
    Meadow = Color(when (s) { app.tend.core.Season.SPRING -> 0xFF8CB662; app.tend.core.Season.AUTUMN -> 0xFF95A05A; app.tend.core.Season.WINTER -> 0xFFE8EEEF; else -> 0xFF86A85E })
    MeadowDeep = Color(when (s) { app.tend.core.Season.SPRING -> 0xFF64904A; app.tend.core.Season.AUTUMN -> 0xFF6F7C42; app.tend.core.Season.WINTER -> 0xFFC9D5D8; else -> 0xFF5F8547 })
    GrassDark = Color(when (s) { app.tend.core.Season.AUTUMN -> 0xFF6E6A36; app.tend.core.Season.WINTER -> 0xFF9AAEA6; else -> 0xFF4D7139 })
    RiverFar = Color(if (s == app.tend.core.Season.WINTER) 0xFFB9CDD0 else 0xFF9DB7B4)
    RiverNear = Color(if (s == app.tend.core.Season.WINTER) 0xFF6D8D93 else 0xFF557A7E)
}
private val Pebble = Color(0xFFB4ADA0)
private val PebbleDark = Color(0xFF8C8578)
private val Bark = Color(0xFF6A4A33)
private val BarkDark = Color(0xFF452F21)
private val Wood = Color(0xFF9C6B45)
private val WoodLight = Color(0xFFC99766)
private val WoodPale = Color(0xFFE2C49A)
private val CanvasCloth = Color(0xFFEDE3CC)
private val CanvasShade = Color(0xFFD3C5A6)
private val Olive = Color(0xFF7B8150)
private val OliveDark = Color(0xFF5E6440)
private val Stone = Color(0xFF9A958C)
private val StoneDark = Color(0xFF746F67)
private val Metal = Color(0xFF34393D)
private val Flame = Color(0xFFF2A341)
private val FlameCore = Color(0xFFFFE08A)
private val Glow = Color(0xFFFFD27A)
private val NightVeil = Color(0xFF0B1A33)

/** Something that glows at night, drawn over the night veil. */
class Light(val at: Offset, val radius: Float, val strength: Float = 1f)

/**
 * Where each spot is, as a fraction of the scene (x, y = where the thing stands on the ground;
 * for the overhead spot, the middle of the string).
 */
val SpotPositions: Map<Int, Offset> = mapOf(
    0 to Offset(0.625f, 0.79f), // fire pit
    1 to Offset(0.5f, 0.81f), // seat, left of the fire
    2 to Offset(0.765f, 0.84f), // seat, right of the fire
    3 to Offset(0.535f, 0.63f), // shelter, middle
    4 to Offset(0.8f, 0.62f), // shelter, right
    5 to Offset(0.925f, 0.75f), // trees, front right (between the birch and the oak)
    6 to Offset(0.43f, 0.62f), // trees, back left (by the pine)
    7 to Offset(0.15f, 0.73f), // river
    8 to Offset(0.3f, 0.93f), // shore
    9 to Offset(0.7f, 0.44f), // overhead, between the trees
    10 to Offset(0.565f, 0.93f), // small: front, middle
    11 to Offset(0.685f, 0.67f), // small: between the shelters
    12 to Offset(0.875f, 0.87f), // small: front right
    13 to Offset(0.96f, 0.94f), // small: corner
    14 to Offset(0.46f, 0.95f), // small: front left
    15 to Offset(0.665f, 0.95f), // small: front
    16 to Offset(0.9f, 0.64f), // small: by the right shelter
    17 to Offset(0.79f, 0.96f), // small: front right, low
)

/** Where a spot's thing stands: where the user put it ([layout], scene fractions), else its usual place. */
fun spotPos(id: Int, layout: Map<Int, Offset>): Offset = layout[id] ?: SpotPositions.getValue(id)

/** A moved thing's size against its usual place: nearer looks bigger, farther back smaller. */
fun spotScale(id: Int, layout: Map<Int, Offset>): Float {
    val moved = layout[id] ?: return 1f
    return (depthScale(moved.y) / depthScale(SpotPositions.getValue(id).y)).coerceIn(0.7f, 1.35f)
}

/**
 * Draws the clearing. [placed] maps spot ids to thing ids.
 */
fun DrawScope.drawClearing(
    avatar: Avatar,
    placed: Map<Int, String>,
    mood: Mood = Mood.HAPPY,
    worn: Set<Accessory> = emptySet(),
    night: Boolean = false,
    paper: Float = 0f,
    ducks: Int = 0,
    bridge: Boolean = false,
    use: InUse? = null,
    lastStrum: Float = 9f,
    visitor: app.tend.core.Visitor? = null,
    /** The spot the companion stays at, after using what's there. */
    rest: Int? = null,
    /** Seconds since the camp came into view at night: walking to bed, then asleep (static pictures: long asleep). */
    nightClock: Float = 100f,
    /** Things popping into the camp just now: spot to how far along (0..1). */
    appear: Map<Int, Float> = emptyMap(),
    /** Sparkle bursts (where, as scene fractions, and how far along 0..1): an animal greeted, a duck caught. */
    bursts: List<Pair<Offset, Float>> = emptyList(),
    /** The time of year: how the trees and the meadow look. */
    season: app.tend.core.Season = app.tend.core.Season.of(java.time.LocalDate.now()),
    /** The hour now (with minutes as a fraction): dawn, day, the golden hour and dusk colour the sky. */
    hour: Float = java.time.LocalTime.now().let { it.hour + it.minute / 60f },
    /** Today's weather. */
    weather: app.tend.core.Weather = app.tend.core.LiveWeather.fresh() ?: app.tend.core.Weather.of(java.time.LocalDate.now(), hour.toInt(), season),
    /** Seconds of camp life, for what moves by itself: clouds, rain, snow, fireflies (0 for a still picture). */
    ambient: Float = 0f,
    /** Where the user moved things to (spot to scene fraction). */
    layout: Map<Int, Offset> = emptyMap(),
    /** A thing picked up: its spot and how high (1 = held up; below 0 = squashing as it lands). */
    lifted: Pair<Int, Float>? = null,
    /** A puff of dust where a thing was just put down: its spot and how far along (0..1). */
    dust: Pair<Int, Float>? = null,
    /**
     * The layout the companion's place was worked out from before a thing moved, and seconds since:
     * they wait there (0), then walk over to where that place is now.
     */
    restFrom: Map<Int, Offset>? = null,
    restClock: Float = 100f,
    /** Friends phone-free right now: their companions sit across the fire (companion, name). */
    guests: List<Pair<Avatar, String>> = emptyList(),
    /** Told where the companion stands (scene fractions, feet), each time it's drawn: for tapping them. */
    onCompanion: ((Offset) -> Unit)? = null,
    /** A thing being moved by the companion ([layout] still has it where it was). */
    carry: Carry? = null,
    /** Presents from friends waiting to be opened. */
    gifts: Int = 0,
    /** Milestones reached: what they've added to the camp for good. */
    unlocked: Set<app.tend.core.Milestone> = emptySet(),
) {
    applySeason(season)
    val w = size.width
    val h = size.height
    val u = h // things scale with the scene's height
    val horizon = h * 0.5f
    val lights = mutableListOf<Light>()
    fun at(id: Int) = spotPos(id, layout).let { Offset(it.x * w, it.y * h) }

    // Sky, with the low sun's warmth near the horizon.
    val sky = skyAt(hour, weather)
    drawRect(Brush.verticalGradient(listOf(sky.first, sky.second), endY = horizon), size = Size(w, horizon + 2))
    if (weather == app.tend.core.Weather.CLEAR || weather == app.tend.core.Weather.FOG)
        drawCircle(Brush.radialGradient(listOf(Color(0x66FFF1C9), Color.Transparent), center = Offset(w * 0.2f, h * 0.18f), radius = h * 0.55f), h * 0.55f, Offset(w * 0.2f, h * 0.18f))
    clouds(weather, ambient)

    // Snow-capped mountains far behind, pale with distance.
    mountains(horizon)
    // A month at the camp: a hut up the mountain, its chimney smoking, its window lit after dark.
    if (app.tend.core.Milestone.HUT in unlocked) mountainHut(Offset(w * 0.735f, h * 0.33f), u, ambient, lights)

    // Two hazy forest walls far away: lighter and bluer the farther they are.
    forestWall(horizon + h * 0.02f, h * 0.2f, Haze1, seed = 3)
    forestWall(horizon + h * 0.06f, h * 0.26f, Haze2, seed = 7)

    // The meadow, warm where the light falls; its far edge rolls gently into the forest.
    drawPath(Path().apply {
        moveTo(0f, horizon + h * 0.02f)
        cubicTo(w * 0.3f, horizon - h * 0.005f, w * 0.55f, horizon + h * 0.035f, w * 0.78f, horizon + h * 0.01f)
        cubicTo(w * 0.88f, horizon, w * 0.95f, horizon + h * 0.02f, w, horizon + h * 0.012f)
        lineTo(w, h); lineTo(0f, h); close()
    }, Brush.verticalGradient(listOf(MeadowFar, Meadow, MeadowDeep), startY = horizon, endY = h))
    drawOval(Brush.radialGradient(listOf(Color(0x40F7E7A8), Color.Transparent), center = Offset(w * 0.66f, h * 0.78f), radius = w * 0.35f), Offset(w * 0.3f, h * 0.55f), Size(w * 0.72f, h * 0.45f))

    // Smaller trees and bushes along the meadow's far edge, hazier the smaller (and farther) they are.
    distantTrees(horizon)

    // The river, winding out of the forest on the left and widening towards us.
    val river = Path().apply {
        moveTo(0f, h * 0.6f)
        cubicTo(w * 0.12f, h * 0.56f, w * 0.22f, h * 0.53f, w * 0.31f, h * 0.535f)
        cubicTo(w * 0.36f, h * 0.55f, w * 0.3f, h * 0.68f, w * 0.27f, h * 0.76f)
        cubicTo(w * 0.24f, h * 0.84f, w * 0.27f, h * 0.93f, w * 0.31f, h)
        lineTo(0f, h); close()
    }
    drawPath(river, Brush.verticalGradient(listOf(RiverFar, RiverNear), startY = h * 0.53f, endY = h))
    // Sky in the water, and a few long glints.
    drawPath(river, Brush.linearGradient(listOf(Color(0x55FFF4D6), Color.Transparent), start = Offset(w * 0.25f, h * 0.55f), end = Offset(w * 0.05f, h * 0.85f)))
    for ((x0, y0, len) in listOf(Triple(0.05f, 0.66f, 0.08f), Triple(0.13f, 0.62f, 0.06f), Triple(0.02f, 0.78f, 0.1f), Triple(0.16f, 0.86f, 0.06f), Triple(0.22f, 0.58f, 0.04f), Triple(0.07f, 0.94f, 0.09f)))
        drawLine(Color(0x99F6F1E4), Offset(w * x0, h * y0), Offset(w * (x0 + len), h * (y0 - 0.004f)), strokeWidth = h * 0.004f, cap = StrokeCap.Round)
    // Grassy far bank on the left, with its own trees.
    drawPath(Path().apply {
        moveTo(0f, h * 0.6f); cubicTo(w * 0.12f, h * 0.56f, w * 0.22f, h * 0.53f, w * 0.31f, h * 0.535f)
        lineTo(w * 0.31f, horizon); lineTo(0f, horizon); close()
    }, Brush.verticalGradient(listOf(Haze2, Color(0xFF6E9466)), startY = horizon, endY = h * 0.6f))
    // Pebbles along our bank.
    shore(w, h)
    // Across the bridge, a trail leads off into the forest, towards friends' camps.
    if (bridge) farTrail(w, h)

    // Trees and bushes around the clearing, drawn back to front (by where they stand), so a bush
    // in front of a trunk covers it and one behind is covered.
    val greenery: List<Pair<Float, () -> Unit>> = listOf(
        h * 0.6f to { pine(Offset(w * 0.035f, h * 0.6f), h * 0.44f) },
        h * 0.578f to { pine(Offset(w * 0.07f, h * 0.578f), h * 0.36f) },
        h * 0.55f to { pine(Offset(w * 0.2f, h * 0.55f), h * 0.28f) },
        h * 0.575f to { roundTree(Offset(w * 0.505f, h * 0.575f), h * 0.2f, seed = 1) },
        h * 0.6f to { pine(Offset(w * 0.335f, h * 0.6f), h * 0.25f) },
        // The swing's tree: a strong branch reaches out to the left, over the swing's spot.
        h * 0.6f to { roundTree(Offset(w * 0.44f, h * 0.6f), h * 0.42f, seed = 5, branch = SpotPositions.getValue(6).x * w) },
        h * 0.578f to { roundTree(Offset(w * 0.6f, h * 0.578f), h * 0.3f, seed = 2) },
        h * 0.57f to { pine(Offset(w * 0.69f, h * 0.57f), h * 0.34f) },
        h * 0.583f to { roundTree(Offset(w * 0.76f, h * 0.583f), h * 0.26f, seed = 3) },
        // Bushes on open ground, beside the trunks rather than on them.
        h * 0.605f to { bush(Offset(w * 0.385f, h * 0.605f), h * 0.022f) },
        h * 0.59f to { bush(Offset(w * 0.655f, h * 0.59f), h * 0.02f) },
    )
    greenery.sortedBy { it.first }.forEach { it.second() }

    // The bridge built together with friends, over the narrow part of the river.
    if (bridge) bridge(Offset(w * 0.1f, h * 0.555f), Offset(w * 0.348f, h * 0.555f), u, night, lights)

    // Something strung between the trees, over the camp.
    placed[9]?.let { overhead(it, Offset(w * 0.445f, h * 0.4f), Offset(w * 0.955f, h * 0.38f), u, lights) }
    appear[9]?.let { sparkles(Offset(w * 0.7f, h * 0.44f), u, it) }

    // The worn ground of the fire pit is always there.
    val pit = at(0)
    drawOval(Dirt.copy(alpha = 0.55f), Offset(pit.x - u * 0.2f, pit.y - u * 0.06f), Size(u * 0.4f, u * 0.13f))
    // Autumn: fallen leaves on the meadow.
    if (season == app.tend.core.Season.AUTUMN) {
        val rnd = java.util.Random(17L)
        repeat(60) {
            val c = Offset(w * (0.38f + 0.62f * rnd.nextFloat()), h * (0.6f + 0.38f * rnd.nextFloat()))
            rotate(rnd.nextFloat() * 180f, c) {
                drawOval(listOf(Color(0xFFD9822B), Color(0xFFE0A341), Color(0xFFB5502E))[it % 3].copy(alpha = 0.8f), c - Offset(u * 0.006f, u * 0.003f), Size(u * 0.012f, u * 0.006f))
            }
        }
    }

    // Everything standing in the clearing, back to front, the companion among them (or using something).
    // Walking over first (from where they stay), then using it there.
    // At night they go to bed: walk to the shelter (if there is one) and sleep inside.
    val bed = if (night && use == null) bedOf(placed) else null
    val act = use ?: bed?.let { InUse(it, placed.getValue(it), nightClock, if (rest == it) 0f else Uses.WALK) }
    // Their place (or where they stopped, part way through something), and (just after a thing moved) where it was a moment ago.
    // (Going to bed doesn't count: in the morning they're back where they were.)
    Whereabouts.frame(carry?.let { "carry ${it.spot}" } ?: use?.let { "${it.spot} ${it.thing} ${it.walk}" })
    val standing = Whereabouts.standing
    val restTo = standing?.let { Offset(it.x * w, it.y * h) } ?: restPoint(rest, placed, layout)
    val restWas = restFrom?.takeIf { standing == null }?.let { restPoint(rest, placed, it) }?.takeIf { (it - restTo).getDistance() > u * 0.01f }
    val strolling = act == null && restWas != null && restClock in 0.001f..Uses.WALK
    // Asleep in the shelter and woken for something: out of the shelter (a yawn first), and from there.
    val bedSpot = if (night) bedOf(placed) else null
    val fromBed = use != null && carry == null && bedSpot != null && rest == bedSpot && use.spot != bedSpot && use.walk >= Uses.YAWN
    val restAt = when {
        fromBed -> useAnchor(InUse(bedSpot!!, placed.getValue(bedSpot), 100f), placed, layout = layout)
        restWas == null || restClock >= Uses.WALK -> restTo
        restClock <= 0f -> restWas
        else -> { val k = (restClock / Uses.WALK).let { it * it * (3 - 2 * it) }; restWas + (restTo - restWas) * k }
    }
    val target = act?.let { useAnchor(it.arrived(), placed, restAt, layout) }
    val walking = act?.walking == true
    // Done with it: the last moments, stepping off to where they'll stay (out of the hammock, off the swing).
    val stepK = act?.takeIf { !walking && it.end > 0f && it.spot >= 0 }?.let { (it.arrived().t - (it.end - Uses.STEP)) / Uses.STEP }?.takeIf { it >= 0f }
    val stepTo = stepK?.let { restPoint(act.spot, placed, layout) }?.takeIf { (it - target!!).getDistance() > u * 0.01f }
    // Moving a thing: over to it, picked up (big ones taken hold of), carried over (big ones dragged
    // behind), put down; then on to their usual place by it.
    val carrying = carry?.takeIf { placed[it.spot] != null }
    var carryAt: Offset? = null
    var carryThing = Offset.Zero
    var carryLift = 0f
    var carryWalk = -1f // the walker's clock, or below 0: standing or crouching
    var carryCrouch = 0f
    var carryRight = true
    var carryGhost = false
    var carrySparkle = -1f
    if (carrying != null) {
        val c = carrying
        val from = Offset(c.from.x * w, c.from.y * h)
        val to = Offset(c.to.x * w, c.to.y * h)
        val side = if (c.to.x >= c.from.x) 1f else -1f
        val id = placed.getValue(c.spot)
        val big = app.tend.core.Things.byId(id)?.kind.let { it == SpotKind.SEAT || it == SpotKind.SHELTER }
        // From the companion's feet to the thing: in front (in their hands), or behind (dragged, clear of it).
        fun reach(y: Float) = Offset(side * u * (if (big) -(thingWidth(id) / 2f + 0.03f) else 0.06f) * depthScale(y / h), u * 0.01f)
        fun held(bo: Offset) = bo + Offset(reach(bo.y).x, -u * 0.01f)
        val b1 = from - reach(from.y)
        val b2 = to - reach(to.y)
        fun ease(k: Float) = k.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
        val t1 = c.walk
        val t2 = t1 + Carry.PICK
        val t3 = t2 + c.move
        val t4 = t3 + Carry.DOWN
        val up = if (big) 0f else u * 0.05f
        carryGhost = c.t < t3
        when {
            c.t < t1 -> { carryAt = restAt + (b1 - restAt) * ease(c.t / t1); carryWalk = c.t; carryRight = b1.x >= restAt.x; carryThing = from }
            c.t < t2 -> {
                val k = (c.t - t1) / Carry.PICK
                carryAt = b1; carryCrouch = k; carryRight = side > 0f
                carryThing = from + (held(b1) - from) * ease(k); carryLift = up * ease(k)
            }
            c.t < t3 -> {
                val bo = b1 + (b2 - b1) * ease((c.t - t2) / c.move)
                carryAt = bo; carryWalk = c.t; carryRight = side > 0f
                carryThing = held(bo)
                carryLift = up * (1f + 0.08f * kotlin.math.sin(c.t * 22f)) + if (big) kotlin.math.abs(kotlin.math.sin(c.t * 11f)) * u * 0.004f else 0f
            }
            c.t < t4 -> {
                val k = (c.t - t3) / Carry.DOWN
                carryAt = b2; carryCrouch = k; carryRight = side > 0f
                carryThing = held(b2) + (to - held(b2)) * ease(k); carryLift = up * (1f - ease(k))
            }
            else -> {
                // Put down. Then (nothing else to move) round it, behind it (so it stays in view), to the other side, and a look at it.
                val k = (c.t - t4) / Carry.ROUND
                carryThing = to; carrySparkle = (c.t - t4) / 1f
                val rx = (thingWidth(id) / 2f + 0.06f) * u * depthScale(to.y / h)
                val ry = u * 0.035f
                val startsLeft = b2.x < to.x
                fun round(k: Float): Offset {
                    val a = Math.PI.toFloat() * (if (startsLeft) 1f + ease(k) else -ease(k))
                    return to + Offset(kotlin.math.cos(a) * rx, kotlin.math.sin(a) * ry + u * 0.01f)
                }
                when {
                    !c.admire || k <= 0f -> { carryAt = b2; carryRight = side > 0f }
                    k < 1f -> {
                        // From where they put it down onto the circle round it, then along it.
                        val on = round(k)
                        val p = if (k < 0.15f) b2 + (round(0.15f) - b2) * ease(k / 0.15f) else on
                        carryAt = p; carryWalk = c.t; carryRight = round((k + 0.02f).coerceAtMost(1f)).x >= round(k).x
                    }
                    else -> { carryAt = round(1f); carryRight = to.x >= round(1f).x }
                }
            }
        }
    }
    val anchor = when {
        carryAt != null -> carryAt
        target == null -> restAt
        walking -> {
            val k = (if (fromBed) ((act!!.t - Uses.YAWN) / (act.walk - Uses.YAWN).coerceAtLeast(0.01f)).coerceIn(0f, 1f) else act!!.t / act.walk).let { it * it * (3 - 2 * it) }
            Offset(restAt.x + (target.x - restAt.x) * k, restAt.y + (target.y - restAt.y) * k)
        }
        // Out of the canoe: a hop onto the bank.
        stepTo != null -> target + (stepTo - target) * stepK.coerceAtMost(1f).let { it * it * (3 - 2 * it) } -
            Offset(0f, if (act.thing == "canoe" || act.thing == "raft") kotlin.math.sin(stepK.coerceAtMost(1f) * Math.PI.toFloat()) * u * 0.07f else 0f)
        else -> target
    }
    var companionDrawn = false
    Whereabouts.drawn = Offset(anchor.x / w, anchor.y / h)
    onCompanion?.invoke(Offset(anchor.x / w, anchor.y / h))
    val cs = u * 0.17f * depthScale(anchor.y / h)
    // The umbrella in a shower (0 put away, 0.5 out but closed, 1 open): from behind the back, up over
    // the head and open; folded and back behind them when the shower's over. Not while busy with something.
    // Wanted while it rains (a moment after a shower starts), unless they're busy, under a tree, or
    // nearly at what they're walking to (folded on the way).
    val showering = weather == app.tend.core.Weather.RAIN && (ambient <= 0f || showerOf((ambient - 0.5f).coerceAtLeast(0f)) > 0.25f)
    val free = carryAt == null && (act == null && (rest ?: 0) > -20 || act != null && walking && act.t < act.walk - 0.9f || stepTo != null || strolling)
    val umbrella = Whereabouts.umbrellaTo(showering && !night && free, ambient)
    fun umbrellaPart(front: Boolean) {
        if (umbrella <= 0f || front != (umbrella >= 0.5f)) return
        val hand = anchor + Offset(cs * 0.3f, -cs * 0.38f)
        val top = Offset(anchor.x + cs * 0.12f, anchor.y - cs * 1.18f)
        val shaft = Color(0xFF3A2414)
        val coral = Color(0xFFE07A5F)
        if (umbrella < 0.5f) {
            // Closed, swinging up from behind their back (drawn before them, so they hide most of it).
            val e = (umbrella / 0.5f).let { it * it * (3 - 2 * it) }
            val b = anchor + Offset(-cs * 0.12f, -cs * 0.32f) + (hand - (anchor + Offset(-cs * 0.12f, -cs * 0.32f))) * e
            val tip = anchor + Offset(-cs * 0.62f, -cs * 0.1f) + (top - (anchor + Offset(-cs * 0.62f, -cs * 0.1f))) * e
            drawLine(shaft, b, tip, strokeWidth = cs * 0.025f, cap = StrokeCap.Round)
            val dir = (tip - b) / (tip - b).getDistance().coerceAtLeast(1f)
            val side = Offset(-dir.y, dir.x) * cs * 0.05f
            val m = tip - dir * cs * 0.4f
            drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(m.x + side.x, m.y + side.y); lineTo(m.x - side.x, m.y - side.y); close() }, coral)
            return
        }
        // Up over the head, and opening.
        val o = ((umbrella - 0.5f) / 0.5f).let { 0.1f + 0.9f * it * it * (3 - 2 * it) }
        drawLine(shaft, hand, top, strokeWidth = cs * 0.025f, cap = StrokeCap.Round)
        drawArc(shaft, 0f, 180f, false, hand + Offset(-cs * 0.06f, -cs * 0.04f), Size(cs * 0.06f, cs * 0.08f), style = Stroke(cs * 0.022f))
        val hem = top.y + cs * (0.2f + 0.2f * (1f - o))
        val canopy = Path().apply {
            moveTo(top.x - cs * 0.48f * o, hem)
            quadraticTo(top.x, top.y - cs * 0.22f * o, top.x + cs * 0.48f * o, hem)
            // A scalloped hem.
            for (k in 0 until 4) quadraticTo(top.x + cs * (0.36f - k * 0.24f) * o, hem - cs * 0.1f, top.x + cs * (0.24f - k * 0.24f) * o, hem)
            close()
        }
        drawPath(canopy, coral)
        drawPath(Path().apply { moveTo(top.x, top.y - cs * 0.01f); quadraticTo(top.x - cs * 0.06f * o, top.y + cs * 0.1f, top.x - cs * 0.12f * o, hem); lineTo(top.x + cs * 0.12f * o, hem); quadraticTo(top.x + cs * 0.06f * o, top.y + cs * 0.1f, top.x, top.y - cs * 0.01f); close() }, Color(0xFFF2D27A))
    }
    fun drawActor(glows: MutableList<Light>) {
        if (carryAt != null) {
            if (carryWalk >= 0f) drawWalker(avatar, mood, worn, anchor, carryWalk, carryRight)
            else drawCrouch(avatar, mood, worn, anchor, carryCrouch, if (carryRight) 0.8f else -0.8f)
            return
        }
        // Walking about: the umbrella comes along.
        fun walker(t: Float, right: Boolean) { umbrellaPart(false); drawWalker(avatar, mood, worn, anchor, t, right); umbrellaPart(true) }
        if (act != null && walking && fromBed && act.t < Uses.YAWN) { drawYawn(avatar, worn, anchor, act.t / Uses.YAWN); return }
        if (act != null && walking) { walker(act.t, target!!.x >= restAt.x); return }
        if (stepTo != null) { walker(stepK!! * 2f, stepTo.x >= target!!.x); return }
        if (strolling) { walker(restClock, restTo.x >= restWas!!.x); return }
        if (act != null) { drawUse(act.arrived(), avatar, mood, worn, placed, anchor, glows, lastStrum, layout); return }
        umbrellaPart(false)
        shadow(anchor, cs * 0.5f, u)
        drawIntoCanvas {
            it.nativeCanvas.save()
            it.nativeCanvas.translate(anchor.x - cs / 2, anchor.y - cs * 0.95f)
            // At night with no shelter: asleep where they stand.
            // Standing about: breathing, and a blink every few seconds.
            val pose = if (night) CharacterPainter.Pose() else CharacterPainter.Pose(
                blink = if (ambient % 4.3f > 4.16f) 1f else 0f, breathe = kotlin.math.sin(ambient * 2.2f) * 0.6f,
            )
            CharacterPainter.draw(it.nativeCanvas, avatar, if (night) Mood.SLEEPING else mood, cs, pose, accessories = worn)
            it.nativeCanvas.restore()
        }
        umbrellaPart(true)
    }
    // A thing on the ground, at its size for where it stands. Glows it adds are moved and sized with it.
    fun thingAt(thing: String, p: Offset, k: Float, spot: Int, sx: Float = 1f, sy: Float = 1f, lift: Float = 0f) {
        val from = lights.size
        withTransform({ translate(0f, -lift); scale(k * sx, k * sy, pivot = p) }) {
            if (spot != 7) shadow(p + Offset(0f, lift), u * thingWidth(thing) * 0.5f, u)
            drawThing(thing, p, u, lights)
        }
        for (i in from until lights.size) lights[i] = lights[i].let { Light(p + (it.at - p) * k + Offset(0f, -lift), it.radius * k, it.strength) }
    }
    val held = lifted?.first
    // Back to front by where they stand: things, the companion, and the trees and bushes on the meadow
    // (so a tent behind the oak is behind it, and a lantern in front of it is in front).
    val ground = mutableListOf<Pair<Float, () -> Unit>>(
        h * 0.725f to { roundTree(Offset(w * 0.875f, h * 0.725f), h * 0.46f, seed = 4) },
        h * 0.76f to { broadleaf(Offset(w * 0.975f, h * 0.76f), h * 0.56f) },
        h * 0.66f to { bush(Offset(w * 0.825f, h * 0.66f), h * 0.022f) },
        h * 0.78f to { bush(Offset(w * 0.935f, h * 0.78f), h * 0.026f) },
    )
    // Friends at the fire: on the far side of it, facing us, a name over each (two Bos look alike).
    val pit0 = at(0)
    guests.take(3).forEachIndexed { i, (who, name) ->
        val seat = pit0 + Offset(u * (listOf(-0.11f, 0.12f, 0.0f)[i]), -u * (listOf(0.06f, 0.055f, 0.085f)[i]))
        ground += seat.y to {
            val gs = u * 0.17f * depthScale(seat.y / h) * 0.92f
            shadow(seat, gs * 0.45f, u)
            val bob = kotlin.math.sin(ambient * 1.3f + i * 2f) * gs * 0.01f
            drawIntoCanvas {
                it.nativeCanvas.save()
                it.nativeCanvas.translate(seat.x - gs / 2, seat.y - gs * 0.95f + bob)
                CharacterPainter.draw(it.nativeCanvas, who, if (night) Mood.SLEEPY else Mood.HAPPY, gs)
                it.nativeCanvas.restore()
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = u * 0.026f; color = 0xFF1D2A23.toInt(); textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }
                val label = name.take(12)
                val tw = paint.measureText(label)
                val top = seat.y - gs * 1.02f
                val pill = android.graphics.RectF(seat.x - tw / 2 - u * 0.012f, top - u * 0.034f, seat.x + tw / 2 + u * 0.012f, top)
                val bg = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6FFFFFF.toInt() }
                it.nativeCanvas.drawRoundRect(pill, u * 0.017f, u * 0.017f, bg)
                it.nativeCanvas.drawText(label, seat.x, top - u * 0.009f, paint)
            }
        }
    }
    // Milestones: a flag over the camp, lamps along the path, a garden between the tent and the cabin.
    if (app.tend.core.Milestone.FLAG in unlocked) Offset(w * 0.37f, h * 0.665f).let { p -> ground += p.y to { flag(p, u, ambient) } }
    if (app.tend.core.Milestone.PATH_LIGHTS in unlocked) for (f in listOf(Offset(0.345f, 0.69f), Offset(0.325f, 0.79f), Offset(0.305f, 0.885f))) {
        val p = Offset(f.x * w, f.y * h)
        ground += p.y to { pathLamp(p, u, lights) }
    }
    if (app.tend.core.Milestone.GARDEN in unlocked) Offset(w * 0.63f, h * 0.605f).let { p -> ground += p.y to { garden(p, u) } }
    // A present from a friend, waiting by the fire: a little hop now and then, so it's noticed.
    if (gifts > 0) {
        val p = Offset(GiftSpot.x * w, GiftSpot.y * h)
        ground += p.y to {
            val k = (ambient % 3f) / 0.5f
            val hop = if (k < 1f) kotlin.math.sin(k * Math.PI.toFloat()) * u * 0.02f else 0f
            withTransform({ translate(0f, -hop); scale(1.7f, 1.7f, pivot = p) }) { drawThing("gift_boxes", p, u, lights) }
            if (k < 1f) sparkles(p + Offset(0f, -u * 0.05f), u, k)
        }
    }
    // Where a thing being moved is going: a faint outline, until it's there.
    if (carrying != null && carryGhost) {
        val p = Offset(carrying.to.x * w, carrying.to.y * h)
        drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), androidx.compose.ui.graphics.Paint().apply { alpha = 0.35f })
        thingAt(placed.getValue(carrying.spot), p, (depthScale(p.y / h) / depthScale(SpotPositions.getValue(carrying.spot).y)).coerceIn(0.7f, 1.35f), carrying.spot)
        drawContext.canvas.restore()
    }
    for (spot in Clearing.spots) {
        if (spot.kind == SpotKind.ABOVE) continue
        if (spot.id == carrying?.spot) {
            // In the companion's hands (in front of them), or dragged along the ground.
            val p = carryThing
            ground += (if (carryLift > u * 0.01f) p.y + u * 0.03f else p.y) to {
                thingAt(placed.getValue(spot.id), p, (depthScale(p.y / h) / depthScale(SpotPositions.getValue(spot.id).y)).coerceIn(0.7f, 1.35f), spot.id, lift = carryLift)
                if (carrySparkle in 0f..0.7f) sparkles(p + Offset(0f, -u * 0.04f), u, carrySparkle / 0.7f)
            }
            continue
        }
        val p = at(spot.id)
        ground += p.y to {
            if (spot.kind == SpotKind.FIRE) firePitStones(p, u)
            val thing = placed[spot.id]
            if (thing != null && spot.id != held && !(act?.spot == spot.id && !walking && stepTo == null && thing in Uses.redrawn)) {
                val k = spotScale(spot.id, layout)
                val a = appear[spot.id]
                // Popping up from the ground with a little overshoot, and a burst of sparkles.
                if (a != null) { thingAt(thing, p, k, spot.id, easeOutBack(a), easeOutBack(a)); sparkles(p + Offset(0f, -u * 0.04f), u, a) }
                else thingAt(thing, p, k, spot.id)
            }
        }
    }
    for ((y, draw) in ground.sortedBy { it.first }) {
        if (!companionDrawn && y > anchor.y) { drawActor(lights); companionDrawn = true }
        draw()
    }
    if (!companionDrawn) drawActor(lights)

    // Put down just now: a soft puff of dust at its feet.
    dust?.let { (id, k) -> if (placed[id] != null) dustPuff(at(id), u * spotScale(id, layout), k) }
    // Picked up: held above everything, a little bigger; landing, it squashes and springs back.
    lifted?.let { (id, lift) ->
        if (id == carrying?.spot) return@let
        val thing = placed[id] ?: return@let
        val p = at(id)
        val up = lift.coerceAtLeast(0f)
        val sx = if (lift >= 0f) 1f + 0.06f * lift else 1f - 0.14f * lift
        val sy = if (lift >= 0f) 1f + 0.06f * lift else 1f + 0.18f * lift
        thingAt(thing, p, spotScale(id, layout), id, sx, sy, lift = u * 0.05f * up)
    }

    // Today's visitor, waiting to be greeted.
    visitor?.let { drawVisitor(it, Offset(it.x * w, it.y * h), u * VISITOR_SCALE) }

    bursts.forEach { (at, k) -> sparkles(Offset(at.x * w, at.y * h), u * 1.3f, k) }

    // What friends did: toilet paper over the trees on the right, a rubber duck for each visit.
    if (paper > 0f) toiletPaper(paper)
    DuckSpots.take(ducks).forEach { rubberDuck(Offset(it.x * w, it.y * h), u) }

    // Grass and a few flowers along the bottom edge frame the picture.
    foreground(w, h)
    // The weather over everything: rain, snow, morning fog; and the light of the hour.
    when (weather) {
        app.tend.core.Weather.RAIN -> rain(ambient, u, if (ambient > 0f) showerOf(ambient) else 1f)
        app.tend.core.Weather.SNOW -> snowfall(ambient, u)
        app.tend.core.Weather.FOG -> drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.18f)), startY = horizon - h * 0.12f, endY = h * 0.85f), size = size)
        else -> {}
    }
    if (!night) hourTint(hour, weather)

    // After dark (by the real sunset and sunrise), and at bedtime: a blue veil over everything, then the lights.
    val dark = if (night) 1f else darkOf(hour)
    if (dark > 0f) {
        drawRect(NightVeil.copy(alpha = 0.6f * dark), size = size)
        if (weather != app.tend.core.Weather.RAIN && weather != app.tend.core.Weather.SNOW) {
            for ((sx, sy) in listOf(0.08f to 0.08f, 0.22f to 0.15f, 0.35f to 0.05f, 0.52f to 0.12f, 0.7f to 0.06f, 0.86f to 0.14f, 0.95f to 0.04f))
                drawCircle(Color.White.copy(alpha = 0.8f * dark), h * 0.004f, Offset(w * sx, h * sy))
            drawCircle(Color(0xFFF3EBC8).copy(alpha = dark), h * 0.035f, Offset(w * 0.16f, h * 0.12f))
        }
        // The companion stays readable: drawn again over the veil, cooled down to match the night.
        drawIntoCanvas {
            val paint = android.graphics.Paint().apply { colorFilter = android.graphics.LightingColorFilter(0xFF9DA6C4.toInt(), 0x101410) }
            it.nativeCanvas.saveLayer(null, paint)
        }
        drawActor(mutableListOf())
        drawIntoCanvas { it.nativeCanvas.restore() }
        // Summer nights: fireflies over the meadow, each blinking in its own time.
        if (season == app.tend.core.Season.SUMMER && weather != app.tend.core.Weather.RAIN) {
            val rnd = java.util.Random(31L)
            repeat(16) { i ->
                val base = Offset(w * (0.4f + 0.58f * rnd.nextFloat()), h * (0.55f + 0.38f * rnd.nextFloat()))
                val at = base + Offset(kotlin.math.sin(ambient * 0.6f + i) * w * 0.025f, kotlin.math.cos(ambient * 0.45f + i * 1.3f) * h * 0.03f)
                val on = ((kotlin.math.sin(ambient * 1.8f + i * 1.7f) + 1f) / 2f).let { it * it }
                drawCircle(Color(0xFFF7F3A0).copy(alpha = (0.3f + 0.7f * on) * dark), u * 0.0035f, at)
                lights += Light(at, u * 0.035f, 0.5f * on)
            }
        }
        lights.forEach { l ->
            drawCircle(Brush.radialGradient(listOf(Glow.copy(alpha = 0.55f * l.strength * dark), Color.Transparent), center = l.at, radius = l.radius), l.radius, l.at)
        }
        // A hundred activities: now and then a shooting star across the dark.
        if (app.tend.core.Milestone.SHOOTING_STARS in unlocked && dark > 0.6f && weather != app.tend.core.Weather.RAIN && weather != app.tend.core.Weather.SNOW) {
            val k = (ambient % 9f) / 0.9f
            if (k < 1f) {
                val n = (ambient / 9f).toInt()
                val from = Offset(w * (0.3f + 0.5f * ((n * 37) % 10) / 10f), h * (0.05f + 0.04f * (n % 3)))
                val head = from + Offset(w * 0.18f * k, h * 0.08f * k)
                drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f * (1f - k))), start = head - Offset(w * 0.06f, h * 0.027f), end = head),
                    head - Offset(w * 0.06f, h * 0.027f), head, strokeWidth = u * 0.003f, cap = StrokeCap.Round)
            }
        }
    }
}

// ---- Scenery ----

private fun DrawScope.mountains(horizon: Float) {
    val w = size.width
    val h = size.height
    val rock = Color(0xFFA9B9C6)
    val rockShade = Color(0xFF93A5B4)
    val snow = Color(0xFFF7F7F2)
    // (peak x, peak y, half width) as fractions of the scene.
    for ((px, py, half) in listOf(Triple(0.18f, 0.2f, 0.2f), Triple(0.47f, 0.13f, 0.25f), Triple(0.78f, 0.19f, 0.22f), Triple(0.98f, 0.25f, 0.14f))) {
        val peak = Offset(w * px, h * py)
        val base = horizon + h * 0.04f
        drawPath(Path().apply { moveTo(peak.x - w * half, base); lineTo(peak.x, peak.y); lineTo(peak.x + w * half, base); close() }, rock)
        // The shaded right face.
        drawPath(Path().apply { moveTo(peak.x, peak.y); lineTo(peak.x + w * half, base); lineTo(peak.x + w * half * 0.15f, base); close() }, rockShade)
        // Snow on top, with a ragged lower edge.
        val drop = (base - peak.y) * 0.3f
        val sw = w * half * 0.3f
        drawPath(Path().apply {
            moveTo(peak.x, peak.y)
            lineTo(peak.x + sw, peak.y + drop); lineTo(peak.x + sw * 0.55f, peak.y + drop * 0.8f)
            lineTo(peak.x + sw * 0.2f, peak.y + drop * 1.05f); lineTo(peak.x - sw * 0.2f, peak.y + drop * 0.78f)
            lineTo(peak.x - sw * 0.6f, peak.y + drop * 0.95f); lineTo(peak.x - sw, peak.y + drop)
            close()
        }, snow)
    }
    // Haze over their feet.
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, SkyLow.copy(alpha = 0.7f)), startY = h * 0.25f, endY = horizon + h * 0.04f), Offset(0f, h * 0.25f), Size(w, horizon - h * 0.21f))
}

private fun DrawScope.forestWall(base: Float, height: Float, color: Color, seed: Int) {
    val w = size.width
    val path = Path().apply {
        moveTo(0f, base)
        var x = 0f
        var i = seed
        while (x <= w) {
            val peak = height * (0.55f + 0.45f * (((i * 53) % 17) / 16f))
            val half = w * (0.018f + 0.012f * (((i * 29) % 7) / 6f))
            lineTo(x + half, base - peak); lineTo(x + half * 2, base - peak * 0.35f)
            x += half * 2; i++
        }
        lineTo(w, base); close()
    }
    drawPath(path, color)
}

private fun DrawScope.shore(w: Float, h: Float) {
    // A band of pebbles where our meadow meets the water.
    drawPath(Path().apply {
        moveTo(w * 0.31f, h * 0.535f)
        cubicTo(w * 0.39f, h * 0.6f, w * 0.33f, h * 0.72f, w * 0.315f, h * 0.8f)
        cubicTo(w * 0.305f, h * 0.88f, w * 0.34f, h * 0.95f, w * 0.38f, h)
        lineTo(w * 0.31f, h)
        cubicTo(w * 0.27f, h * 0.93f, w * 0.24f, h * 0.84f, w * 0.27f, h * 0.76f)
        cubicTo(w * 0.3f, h * 0.68f, w * 0.36f, h * 0.55f, w * 0.31f, h * 0.535f)
        close()
    }, Color(0xFFC9BFA9))
    // Pebbles only on the band: between the water's edge and the meadow, at each height.
    fun bez(t: Float, a: Float, b: Float, c: Float, d: Float): Float { val m = 1 - t; return m * m * m * a + 3 * m * m * t * b + 3 * m * t * t * c + t * t * t * d }
    fun edge(segments: List<FloatArray>): List<Pair<Float, Float>> = segments.flatMap { q -> (0..20).map { i -> val t = i / 20f; bez(t, q[1], q[3], q[5], q[7]) to bez(t, q[0], q[2], q[4], q[6]) } }
    val water = edge(listOf(floatArrayOf(0.31f, 0.535f, 0.36f, 0.55f, 0.3f, 0.68f, 0.27f, 0.76f), floatArrayOf(0.27f, 0.76f, 0.24f, 0.84f, 0.27f, 0.93f, 0.31f, 1f)))
    val meadow = edge(listOf(floatArrayOf(0.31f, 0.535f, 0.39f, 0.6f, 0.33f, 0.72f, 0.315f, 0.8f), floatArrayOf(0.315f, 0.8f, 0.305f, 0.88f, 0.34f, 0.95f, 0.38f, 1f)))
    fun xAt(line: List<Pair<Float, Float>>, y: Float) = line.minBy { kotlin.math.abs(it.first - y) }.second
    val rnd = java.util.Random(5L)
    repeat(46) {
        val t = rnd.nextFloat()
        val fy = 0.58f + 0.42f * t
        val r = h * (0.006f + 0.008f * t * rnd.nextFloat())
        val left = w * xAt(water, fy) + r * 1.2f
        val right = w * xAt(meadow, fy) - r * 1.2f
        if (right <= left) return@repeat
        val x = left + (right - left) * rnd.nextFloat()
        drawOval(if (rnd.nextBoolean()) Pebble else PebbleDark, Offset(x - r, h * fy - r * 0.6f), Size(r * 2, r * 1.2f))
    }
}

private fun DrawScope.pine(base: Offset, height: Float) {
    shadow(base, height * 0.12f, size.height)
    val trunkW = height * 0.035f
    drawRect(Bark, Offset(base.x - trunkW / 2, base.y - height), Size(trunkW, height))
    // Tiers of needles, widest at the bottom; the sun side (left) is lighter.
    val tiers = 7
    for (i in 0 until tiers) {
        val t = i / (tiers - 1f)
        val y = base.y - height * (0.28f + 0.68f * t)
        val half = height * (0.2f - 0.15f * t)
        val tierH = height * 0.2f
        val tier = Path().apply {
            moveTo(base.x, y - tierH)
            lineTo(base.x - half, y); lineTo(base.x - half * 0.55f, y - tierH * 0.12f); lineTo(base.x - half * 0.3f, y + tierH * 0.02f)
            lineTo(base.x, y - tierH * 0.08f)
            lineTo(base.x + half * 0.3f, y + tierH * 0.02f); lineTo(base.x + half * 0.55f, y - tierH * 0.12f); lineTo(base.x + half, y)
            close()
        }
        drawPath(tier, if (i % 2 == 0) PineDark else PineMid)
        drawPath(Path().apply { moveTo(base.x, y - tierH); lineTo(base.x - half, y); lineTo(base.x - half * 0.3f, y + tierH * 0.02f); lineTo(base.x, y - tierH * 0.08f); close() }, PineLit.copy(alpha = 0.35f))
    }
}

/**
 * A leafy tree with a rounded crown: a tapering trunk that forks, and clumps of leaves in three
 * tones, lit from the upper left, with a soft shadow under the crown. [seed] varies the clumps.
 */
private fun DrawScope.roundTree(base: Offset, height: Float, seed: Int, branch: Float? = null) {
    shadow(base, height * 0.16f, size.height)
    val rnd = java.util.Random(seed * 31L)
    val trunkW = height * 0.07f
    val forkY = base.y - height * 0.45f
    drawPath(Path().apply {
        moveTo(base.x - trunkW, base.y)
        quadraticTo(base.x - trunkW * 0.5f, base.y - height * 0.25f, base.x - trunkW * 0.35f, forkY)
        lineTo(base.x - trunkW * 1.4f, forkY - height * 0.16f); lineTo(base.x - trunkW * 0.9f, forkY - height * 0.17f)
        lineTo(base.x, forkY - height * 0.05f)
        lineTo(base.x + trunkW * 1.1f, forkY - height * 0.18f); lineTo(base.x + trunkW * 1.5f, forkY - height * 0.15f)
        lineTo(base.x + trunkW * 0.35f, forkY)
        quadraticTo(base.x + trunkW * 0.5f, base.y - height * 0.25f, base.x + trunkW, base.y); close()
    }, Bark)
    // The shaded side of the trunk.
    drawPath(Path().apply { moveTo(base.x + trunkW * 0.2f, base.y); quadraticTo(base.x + trunkW * 0.3f, base.y - height * 0.25f, base.x + trunkW * 0.3f, forkY); lineTo(base.x + trunkW * 0.35f, forkY); quadraticTo(base.x + trunkW * 0.5f, base.y - height * 0.25f, base.x + trunkW, base.y); close() }, BarkDark.copy(alpha = 0.5f))
    // A strong side branch out to [branch] (x), level enough for a swing to hang from.
    branch?.let { bx ->
        val y0 = base.y - height * 0.38f
        val end = Offset(bx - height * 0.03f, y0 + height * 0.005f)
        drawPath(Path().apply {
            moveTo(base.x, y0 - trunkW * 0.5f)
            quadraticTo((base.x + end.x) / 2, y0 - height * 0.03f, end.x, end.y - trunkW * 0.25f)
            lineTo(end.x, end.y + trunkW * 0.25f)
            quadraticTo((base.x + end.x) / 2, y0 - height * 0.005f, base.x, y0 + trunkW * 0.6f); close()
        }, Bark)
        drawCircle(LeafMid, height * 0.06f, end + Offset(-height * 0.02f, -height * 0.03f))
        drawCircle(LeafLit, height * 0.035f, end + Offset(-height * 0.03f, -height * 0.05f))
    }
    val c = Offset(base.x, base.y - height * 0.7f)
    val r = height * 0.3f
    // Clumps: dark ones low and to the right, then the middle, then the lit tops on the left.
    val clumps = (0 until 7).map { k ->
        val a = (k / 7f) * 6.283f + rnd.nextFloat() * 0.6f
        Offset(c.x + kotlin.math.cos(a) * r * 0.62f, c.y + kotlin.math.sin(a) * r * 0.5f) to r * (0.5f + 0.18f * rnd.nextFloat())
    }
    drawOval(Color.Black.copy(alpha = 0.1f), Offset(c.x - r * 1.1f, c.y + r * 0.35f), Size(r * 2.2f, r * 0.55f))
    clumps.forEach { (p, cr) -> drawCircle(LeafDark, cr, p + Offset(r * 0.08f, r * 0.1f)) }
    drawCircle(LeafMid, r * 0.78f, c)
    clumps.filter { it.first.y < c.y + r * 0.15f }.forEach { (p, cr) -> drawCircle(LeafMid, cr * 0.9f, p) }
    clumps.filter { it.first.y < c.y && it.first.x < c.x + r * 0.2f }.forEach { (p, cr) -> drawCircle(LeafLit, cr * 0.55f, p + Offset(-cr * 0.2f, -cr * 0.25f)) }
    drawCircle(LeafLit.copy(alpha = 0.6f), r * 0.32f, c + Offset(-r * 0.35f, -r * 0.4f))
    // Spring: blossom all over the crown.
    if (season == app.tend.core.Season.SPRING) repeat(22) {
        val a = rnd.nextFloat() * 6.283f
        val d = kotlin.math.sqrt(rnd.nextFloat())
        drawCircle(if (it % 3 == 0) Color.White else Color(0xFFF4B8C8), r * 0.06f, Offset(c.x + kotlin.math.cos(a) * r * 0.85f * d, c.y + kotlin.math.sin(a) * r * 0.7f * d))
    }
}


private class Blob(val dx: Float, val dy: Float, val r: Float, val color: Color)

private fun DrawScope.broadleaf(base: Offset, height: Float) {
    shadow(base, height * 0.14f, size.height)
    val trunkW = height * 0.05f
    drawPath(Path().apply {
        moveTo(base.x - trunkW, base.y); lineTo(base.x - trunkW * 0.45f, base.y - height * 0.6f)
        lineTo(base.x + trunkW * 0.45f, base.y - height * 0.6f); lineTo(base.x + trunkW, base.y); close()
    }, Bark)
    listOf(
        Blob(-0.12f, 0.72f, 0.16f, LeafDark), Blob(0.08f, 0.7f, 0.17f, LeafDark), Blob(-0.02f, 0.86f, 0.18f, LeafMid),
        Blob(-0.16f, 0.88f, 0.12f, LeafMid), Blob(0.12f, 0.9f, 0.13f, LeafMid), Blob(-0.08f, 0.98f, 0.11f, LeafLit),
    ).forEach { drawCircle(it.color, height * it.r, Offset(base.x + height * it.dx, base.y - height * it.dy)) }
}

/** Visitors are drawn a little larger than life, so they're easy to see and tap. */
const val VISITOR_SCALE = 1.4f

// ---- Built together ----

/** On the far bank: a dirt trail from the bridge up into the trees, and a little signpost. */
private fun DrawScope.farTrail(w: Float, h: Float) {
    // From where the bridge lands on the far bank, left past the signpost and up into the forest.
    drawPath(Path().apply {
        moveTo(w * 0.106f, h * 0.552f)
        cubicTo(w * 0.09f, h * 0.548f, w * 0.076f, h * 0.54f, w * 0.066f, h * 0.527f)
        quadraticTo(w * 0.06f, h * 0.518f, w * 0.057f, h * 0.506f)
        lineTo(w * 0.066f, h * 0.506f)
        quadraticTo(w * 0.07f, h * 0.516f, w * 0.078f, h * 0.524f)
        cubicTo(w * 0.088f, h * 0.534f, w * 0.098f, h * 0.539f, w * 0.11f, h * 0.544f)
        close()
    }, Brush.verticalGradient(listOf(Dirt.copy(alpha = 0.5f), Dirt), startY = h * 0.508f, endY = h * 0.556f))
    for ((x, y) in listOf(0.088f to 0.546f, 0.07f to 0.53f)) drawOval(Stone, Offset(w * x - h * 0.004f, h * y - h * 0.002f), Size(h * 0.008f, h * 0.005f))
    // The signpost beside the bridge's end, pointing up the trail.
    val post = Offset(w * 0.088f, h * 0.547f)
    drawLine(BarkDark, post, post + Offset(0f, -h * 0.05f), strokeWidth = h * 0.005f, cap = StrokeCap.Round)
    val b = post + Offset(0f, -h * 0.045f)
    drawPath(Path().apply {
        moveTo(b.x + h * 0.014f, b.y - h * 0.007f); lineTo(b.x - h * 0.012f, b.y - h * 0.007f); lineTo(b.x - h * 0.02f, b.y)
        lineTo(b.x - h * 0.012f, b.y + h * 0.007f); lineTo(b.x + h * 0.014f, b.y + h * 0.007f); close()
    }, WoodLight)
    drawLine(Wood, b + Offset(-h * 0.009f, 0f), b + Offset(h * 0.009f, 0f), strokeWidth = h * 0.0025f)
}


/** A wooden footbridge arching from [a] (the far bank) to [b] (ours), with posts, rails and a lantern. */
private fun DrawScope.bridge(a: Offset, b: Offset, u: Float, night: Boolean, lights: MutableList<Light>) {
    val rise = u * 0.06f
    fun deck(t: Float): Offset {
        val m = 1 - t
        val c = Offset((a.x + b.x) / 2, (a.y + b.y) / 2 - rise * 2)
        return Offset(m * m * a.x + 2 * m * t * c.x + t * t * b.x, m * m * a.y + 2 * m * t * c.y + t * t * b.y)
    }
    val n = 30
    val pts = (0..n).map { deck(it / n.toFloat()) }
    fun curve(dy: Float) = Path().apply { moveTo(pts[0].x, pts[0].y + dy); pts.drop(1).forEach { lineTo(it.x, it.y + dy) } }
    // Its reflection, faint in the water below.
    drawPath(Path().apply { pts.take(n * 7 / 10).forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y + rise * 2f) else lineTo(p.x, p.y + rise * 2f) } }, Color.Black.copy(alpha = 0.04f), style = Stroke(u * 0.014f, cap = StrokeCap.Round))
    // Stone feet on each bank.
    for (p in listOf(a, b)) drawOval(Color(0xFF8E8A80), Offset(p.x - u * 0.025f, p.y - u * 0.008f), Size(u * 0.05f, u * 0.022f))
    // The deck: a dark underside, then the planks.
    drawPath(curve(u * 0.012f), BarkDark, style = Stroke(u * 0.016f, cap = StrokeCap.Round))
    drawPath(curve(0f), Wood, style = Stroke(u * 0.02f, cap = StrokeCap.Round))
    for (i in 1 until n) {
        val p = pts[i]
        drawLine(if (i % 2 == 0) WoodLight else BarkDark.copy(alpha = 0.5f), p + Offset(0f, -u * 0.009f), p + Offset(0f, u * 0.009f), strokeWidth = u * 0.003f)
    }
    // Posts and two rails.
    val posts = listOf(0, 6, 12, 18, 24, 30).map { pts[it] }
    val postH = u * 0.05f
    posts.forEach { p -> drawLine(BarkDark, p, p + Offset(0f, -postH), strokeWidth = u * 0.008f, cap = StrokeCap.Round) }
    drawPath(curve(-postH), Wood, style = Stroke(u * 0.007f, cap = StrokeCap.Round))
    drawPath(curve(-postH * 0.5f), WoodLight, style = Stroke(u * 0.004f, cap = StrokeCap.Round))
    // A lantern on the middle post, lit at night.
    val top = pts[18] + Offset(0f, -postH)
    drawRoundRect(BarkDark, Offset(top.x - u * 0.008f, top.y - u * 0.022f), Size(u * 0.016f, u * 0.022f), CornerRadius(u * 0.003f))
    drawRect(if (night) Glow else Color(0xFFF3E3B0), Offset(top.x - u * 0.005f, top.y - u * 0.018f), Size(u * 0.01f, u * 0.013f))
    lights += Light(top + Offset(0f, -u * 0.01f), u * 0.08f, 0.8f)
}

// ---- What friends do ----

/** Where visitors' rubber ducks bob in the river, as fractions of the scene; the first ones fill first. */
/** Where presents from friends wait (scene fractions): in front, by the fire. */
val GiftSpot = Offset(0.53f, 0.93f)

val DuckSpots = listOf(Offset(0.12f, 0.885f), Offset(0.045f, 0.8f), Offset(0.2f, 0.965f))

/** The toilet-papered trees on the right, for taps: anything right of this and above [PAPER_BOTTOM]. */
const val PAPER_LEFT = 0.82f
const val PAPER_BOTTOM = 0.78f

private val Paper = Color(0xFFFBFAF6)
private val PaperShadow = Color(0xFFC9C5B8)
private val PaperFold = Color(0xFFE6E2D7)

/**
 * A ribbon of toilet paper along a curve: it twists (its width breathes), throws a soft shadow,
 * and has faint perforations. [t] (0..1) is how much of it is still there, from the start.
 */
private fun DrawScope.ribbon(a: Offset, c1: Offset, c2: Offset, b: Offset, band: Float, twists: Float, t: Float = 1f, alpha: Float = 1f) {
    if (t <= 0f) return
    val n = 48
    fun at(k: Float): Offset {
        val m = 1 - k
        return Offset(
            m * m * m * a.x + 3 * m * m * k * c1.x + 3 * m * k * k * c2.x + k * k * k * b.x,
            m * m * m * a.y + 3 * m * m * k * c1.y + 3 * m * k * k * c2.y + k * k * k * b.y,
        )
    }
    val pts = (0..n).map { at(it / n.toFloat() * t) }
    val left = ArrayList<Offset>(); val right = ArrayList<Offset>(); val widths = ArrayList<Float>()
    for (i in pts.indices) {
        val p0 = pts[maxOf(0, i - 1)]; val p1 = pts[minOf(pts.size - 1, i + 1)]
        val d = p1 - p0
        val len = kotlin.math.sqrt(d.x * d.x + d.y * d.y).coerceAtLeast(0.001f)
        val nx = -d.y / len; val ny = d.x / len
        val k = i / n.toFloat() * t
        val wdt = band * (0.4f + 0.6f * kotlin.math.abs(kotlin.math.cos(k * twists * Math.PI.toFloat())))
        widths += wdt
        left += Offset(pts[i].x + nx * wdt / 2, pts[i].y + ny * wdt / 2)
        right += Offset(pts[i].x - nx * wdt / 2, pts[i].y - ny * wdt / 2)
    }
    fun strip(dx: Float, dy: Float) = Path().apply {
        moveTo(left[0].x + dx, left[0].y + dy)
        left.drop(1).forEach { lineTo(it.x + dx, it.y + dy) }
        right.asReversed().forEach { lineTo(it.x + dx, it.y + dy) }
        close()
    }
    drawPath(strip(band * 0.25f, band * 0.35f), Color.Black.copy(alpha = 0.12f * alpha)) // shadow on what's behind
    drawPath(strip(0f, 0f), PaperShadow.copy(alpha = alpha))
    drawPath(strip(-band * 0.06f, -band * 0.08f), Paper.copy(alpha = alpha))
    // Where it turns edge-on, the fold shows.
    for (i in 1 until pts.size) if (widths[i] < band * 0.5f) drawLine(PaperFold.copy(alpha = alpha), left[i], right[i], strokeWidth = band * 0.18f)
    // Perforations every few sheets.
    for (i in 4 until pts.size step 6) drawLine(PaperShadow.copy(alpha = 0.7f * alpha), left[i], right[i], strokeWidth = band * 0.05f)
}

/** A roll of toilet paper, as seen from the side, at [c]. */
private fun DrawScope.paperRoll(c: Offset, r: Float, angle: Float) {
    rotate(angle, c) {
        drawRoundRect(PaperShadow, Offset(c.x - r * 1.1f, c.y - r), Size(r * 2.2f, r * 2f), CornerRadius(r * 0.35f))
        drawRoundRect(Paper, Offset(c.x - r * 1.1f, c.y - r), Size(r * 2.0f, r * 1.85f), CornerRadius(r * 0.35f))
        drawOval(PaperFold, Offset(c.x + r * 0.55f, c.y - r), Size(r * 0.7f, r * 2f))
        drawOval(Color(0xFFB98F63), Offset(c.x + r * 0.72f, c.y - r * 0.45f), Size(r * 0.36f, r * 0.9f)) // cardboard core
    }
}

/**
 * Toilet paper thrown over the birch and the oak on the right: loops over the crowns, long strands
 * hanging down and drifting, a roll stuck in the branches and another on the grass. [amount] (0..1)
 * unrolls it: the strands pull back up and fade.
 */
private fun DrawScope.toiletPaper(amount: Float) {
    val w = size.width
    val h = size.height
    fun o(x: Float, y: Float) = Offset(w * x, h * y)
    val band = h * 0.019f
    val a = amount.coerceIn(0f, 1f)
    val fade = (a * 1.6f).coerceAtMost(1f)
    // Loops over the crowns (drawn first: they're behind the strands).
    ribbon(o(0.845f, 0.27f), o(0.88f, 0.36f), o(0.93f, 0.36f), o(0.965f, 0.22f), band, 1.1f, a, fade)
    ribbon(o(0.88f, 0.2f), o(0.92f, 0.3f), o(0.98f, 0.31f), o(1.02f, 0.2f), band, 0.9f, a, fade)
    ribbon(o(0.86f, 0.36f), o(0.9f, 0.44f), o(0.97f, 0.45f), o(1.01f, 0.35f), band, 1.3f, a, fade)
    // A long garland from the birch to the oak.
    ribbon(o(0.875f, 0.31f), o(0.9f, 0.52f), o(0.95f, 0.53f), o(0.985f, 0.4f), band, 3f, a, fade)
    // Strands hanging down, swaying; the longest nearly reach the grass.
    ribbon(o(0.852f, 0.33f), o(0.86f, 0.42f), o(0.84f, 0.5f), o(0.855f, 0.6f), band, 1.7f, a, fade)
    ribbon(o(0.905f, 0.42f), o(0.92f, 0.52f), o(0.895f, 0.6f), o(0.912f, 0.7f), band, 1.9f, a, fade)
    ribbon(o(0.945f, 0.44f), o(0.93f, 0.5f), o(0.955f, 0.56f), o(0.94f, 0.63f), band, 1.3f, a, fade)
    ribbon(o(0.99f, 0.4f), o(1.0f, 0.5f), o(0.975f, 0.6f), o(0.99f, 0.74f), band, 2.1f, a, fade)
    // A roll caught in the oak, its tail fluttering.
    if (a > 0.5f) paperRoll(o(0.93f, 0.25f), h * 0.022f, -25f)
    // And one that rolled away over the grass, unrolling as it went.
    if (a > 0.3f) {
        ribbon(o(0.99f, 0.77f), o(0.98f, 0.8f), o(0.96f, 0.79f), o(0.945f, 0.815f), band * 0.9f, 1f, a, fade)
        paperRoll(o(0.935f, 0.81f), h * 0.02f, 8f)
    }
}

/** A yellow rubber duck bobbing in the river. */
private fun DrawScope.rubberDuck(p: Offset, u: Float) {
    val yellow = Color(0xFFFFD43B)
    val shade = Color(0xFFE9B21C)
    val s = u * 0.045f
    drawOval(Color.White.copy(alpha = 0.35f), Offset(p.x - s * 1.3f, p.y - s * 0.12f), Size(s * 2.6f, s * 0.4f)) // ripple
    drawPath(Path().apply { // body, tail up on the right
        moveTo(p.x - s, p.y - s * 0.35f)
        cubicTo(p.x - s * 1.05f, p.y + s * 0.1f, p.x + s * 0.9f, p.y + s * 0.15f, p.x + s, p.y - s * 0.4f)
        lineTo(p.x + s * 1.15f, p.y - s * 0.85f)
        cubicTo(p.x + s * 0.6f, p.y - s * 0.6f, p.x - s * 0.2f, p.y - s * 0.75f, p.x - s, p.y - s * 0.35f); close()
    }, yellow)
    drawOval(shade, Offset(p.x - s * 0.1f, p.y - s * 0.55f), Size(s * 0.75f, s * 0.35f)) // wing
    val head = Offset(p.x - s * 0.55f, p.y - s * 1.0f)
    drawCircle(yellow, s * 0.45f, head)
    drawCircle(Color.White.copy(alpha = 0.5f), s * 0.12f, head + Offset(s * 0.12f, -s * 0.2f)) // shine
    drawPath(Path().apply { moveTo(head.x - s * 0.35f, head.y - s * 0.02f); lineTo(head.x - s * 0.85f, head.y + s * 0.08f); lineTo(head.x - s * 0.35f, head.y + s * 0.22f); close() }, Color(0xFFF07F2B))
    drawCircle(Color(0xFF1D2A23), s * 0.07f, head + Offset(-s * 0.12f, -s * 0.1f))
}

/** A row of small trees at the meadow's far edge: little pines and round trees, blended into the haze. */
private fun DrawScope.distantTrees(horizon: Float) {
    val w = size.width
    val h = size.height
    // (x, height, round?) — bases sit on the meadow's edge, a little lower the closer they are.
    val trees = listOf(
        Triple(0.34f, 0.07f, false), Triple(0.37f, 0.1f, false), Triple(0.395f, 0.06f, true), Triple(0.48f, 0.08f, true),
        Triple(0.5f, 0.12f, false), Triple(0.53f, 0.07f, false), Triple(0.565f, 0.09f, true), Triple(0.6f, 0.06f, false),
        Triple(0.645f, 0.11f, false), Triple(0.72f, 0.075f, true), Triple(0.745f, 0.1f, false), Triple(0.775f, 0.065f, false),
        Triple(0.82f, 0.12f, false), Triple(0.85f, 0.07f, true), Triple(0.9f, 0.09f, false), Triple(0.93f, 0.06f, true),
    )
    for ((x, th, round) in trees.sortedBy { it.second }) {
        val haze = (0.12f - th) / 0.06f * 0.5f + 0.25f // smaller: more haze
        val base = Offset(w * x, horizon + h * (0.012f + th * 0.12f))
        val height = h * th
        val dark = androidx.compose.ui.graphics.lerp(PineMid, Haze2, haze.coerceIn(0f, 0.85f))
        val lit = androidx.compose.ui.graphics.lerp(PineLit, Haze1, haze.coerceIn(0f, 0.85f))
        if (round) {
            drawRect(androidx.compose.ui.graphics.lerp(Bark, Haze2, 0.5f), Offset(base.x - height * 0.03f, base.y - height * 0.5f), Size(height * 0.06f, height * 0.5f))
            drawCircle(dark, height * 0.34f, Offset(base.x, base.y - height * 0.62f))
            drawCircle(dark, height * 0.26f, Offset(base.x + height * 0.2f, base.y - height * 0.48f))
            drawCircle(lit, height * 0.22f, Offset(base.x - height * 0.12f, base.y - height * 0.72f))
        } else {
            for (i in 0 until 3) {
                val y = base.y - height * (0.15f + 0.28f * i)
                val half = height * (0.24f - 0.06f * i)
                drawPath(Path().apply { moveTo(base.x, y - height * 0.4f); lineTo(base.x - half, y); lineTo(base.x + half, y); close() }, dark)
                drawPath(Path().apply { moveTo(base.x, y - height * 0.4f); lineTo(base.x - half, y); lineTo(base.x - half * 0.2f, y); close() }, lit)
            }
        }
    }
    // Low bushes along the edge, between the trees.
    for ((x, r) in listOf(0.33f to 0.018f, 0.43f to 0.02f, 0.58f to 0.016f, 0.69f to 0.02f, 0.8f to 0.017f, 0.96f to 0.02f))
        bush(Offset(w * x, horizon + h * 0.03f), h * r, haze = 0.45f)
}

/** A round bush: a few overlapping clumps, lit from the left. */
private fun DrawScope.bush(base: Offset, r: Float, haze: Float = 0f) {
    val dark = androidx.compose.ui.graphics.lerp(LeafMid, Haze2, haze)
    val mid = androidx.compose.ui.graphics.lerp(androidx.compose.ui.graphics.lerp(LeafMid, LeafLit, 0.35f), Haze2, haze)
    val lit = androidx.compose.ui.graphics.lerp(LeafLit, Haze1, haze)
    if (haze == 0f) drawOval(Color.Black.copy(alpha = 0.08f), Offset(base.x - r * 1.5f, base.y - r * 0.2f), Size(r * 3f, r * 0.5f))
    drawCircle(dark, r * 0.9f, Offset(base.x + r * 0.7f, base.y - r * 0.6f))
    drawCircle(mid, r, Offset(base.x - r * 0.5f, base.y - r * 0.7f))
    drawCircle(mid, r * 1.05f, Offset(base.x + r * 0.15f, base.y - r * 1.05f))
    drawCircle(lit, r * 0.5f, Offset(base.x - r * 0.45f, base.y - r * 1.15f))
}

private fun DrawScope.foreground(w: Float, h: Float) {
    val rnd = java.util.Random(9L)
    repeat(70) {
        val x = w * (0.3f + 0.72f * rnd.nextFloat())
        val y = h * (0.965f + 0.04f * rnd.nextFloat())
        val len = h * (0.025f + 0.035f * rnd.nextFloat())
        val lean = (rnd.nextFloat() - 0.5f) * len * 0.6f
        drawLine(if (rnd.nextBoolean()) MeadowDeep else GrassDark, Offset(x, y), Offset(x + lean, y - len), strokeWidth = h * 0.004f, cap = StrokeCap.Round)
    }
    // Flowers in the grass (not under the snow).
    if (season != app.tend.core.Season.WINTER) repeat(14) {
        val c = Offset(w * (0.33f + 0.66f * rnd.nextFloat()), h * (0.94f + 0.05f * rnd.nextFloat()))
        drawCircle(listOf(Color.White, Color(0xFFC9A6E3), Color(0xFFF2D27A))[it % 3], h * 0.005f, c)
    }
}

/** A soft contact shadow, falling a little to the right (the sun is low on the left). */
/** The sky's two colours at [hour] in [weather]: lavender dawn, blue day, a warm golden hour, dusk; greyer when it's cloudy. */
private fun skyAt(hour: Float, weather: app.tend.core.Weather): Pair<Color, Color> {
    var top = SkyTop
    var low = SkyLow
    fun bump(from: Float, peak: Float, to: Float) = when { hour <= from || hour >= to -> 0f; hour < peak -> (hour - from) / (peak - from); else -> (to - hour) / (to - peak) }
    val grey = when (weather) { app.tend.core.Weather.RAIN -> 0.6f; app.tend.core.Weather.CLOUDY -> 0.35f; app.tend.core.Weather.SNOW -> 0.3f; else -> 0f }
    top = androidx.compose.ui.graphics.lerp(top, Color(0xFFA9B5BD), grey)
    low = androidx.compose.ui.graphics.lerp(low, Color(0xFFD5D9D8), grey)
    val clear = 1f - grey
    // Around the real sunrise and sunset when a city is set (the usual hours otherwise).
    val rise = app.tend.core.LiveWeather.sunrise
    val set = app.tend.core.LiveWeather.sunset
    bump(rise - 1.5f, rise, rise + 2f).let { k -> top = androidx.compose.ui.graphics.lerp(top, Color(0xFFC6B8D8), k * 0.7f * clear); low = androidx.compose.ui.graphics.lerp(low, Color(0xFFF7C6A6), k * 0.75f * clear) }
    bump(set - 2.1f, set, set + 1f).let { k -> top = androidx.compose.ui.graphics.lerp(top, Color(0xFFE6CFAE), k * 0.45f * clear); low = androidx.compose.ui.graphics.lerp(low, Color(0xFFF6B56C), k * 0.7f * clear) }
    bump(set + 0.4f, set + 1.9f, set + 4.4f).let { k -> top = androidx.compose.ui.graphics.lerp(top, Color(0xFF8292C2), k * 0.7f); low = androidx.compose.ui.graphics.lerp(low, Color(0xFFE5A28C), k * 0.65f * clear) }
    return top to low
}

/**
 * How dark it is at [hour] (0 day, 1 night), by the real sunset and sunrise when a town is set:
 * falling over the hour or so after sunset, lifting in the hour before sunrise.
 */
internal fun darkOf(hour: Float): Float {
    val rise = app.tend.core.LiveWeather.sunrise
    val set = app.tend.core.LiveWeather.sunset
    fun ramp(x: Float, from: Float, to: Float) = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return when {
        hour >= set -> ramp(hour, set + 0.4f, set + 1.5f)
        hour < rise -> 1f - ramp(hour, rise - 1.2f, rise - 0.1f)
        else -> 0f
    }
}

/** The light of the hour over the whole camp: rosy at dawn, golden late in the day, violet at dusk; dimmer in the rain. */
private fun DrawScope.hourTint(hour: Float, weather: app.tend.core.Weather) {
    fun bump(from: Float, peak: Float, to: Float) = when { hour <= from || hour >= to -> 0f; hour < peak -> (hour - from) / (peak - from); else -> (to - hour) / (to - peak) }
    val wet = weather == app.tend.core.Weather.RAIN
    if (wet) drawRect(Color(0xFF6E7C86).copy(alpha = 0.14f), size = size)
    val k = if (wet) 0.4f else 1f
    val rise = app.tend.core.LiveWeather.sunrise
    val set = app.tend.core.LiveWeather.sunset
    bump(rise - 1.5f, rise, rise + 2f).takeIf { it > 0f }?.let { drawRect(Color(0xFFFFB49A).copy(alpha = 0.07f * it * k), size = size) }
    bump(set - 2.1f, set, set + 1f).takeIf { it > 0f }?.let { drawRect(Color(0xFFFFA347).copy(alpha = 0.1f * it * k), size = size) }
    bump(set + 0.4f, set + 1.9f, set + 4.4f).takeIf { it > 0f }?.let { drawRect(Color(0xFF5E4FA0).copy(alpha = 0.16f * it), size = size) }
}

/** Soft clouds drifting slowly across: a couple on a clear day, more and greyer when it's cloudy. */
private fun DrawScope.clouds(weather: app.tend.core.Weather, t: Float) {
    val w = size.width
    val h = size.height
    val many = weather != app.tend.core.Weather.CLEAR && weather != app.tend.core.Weather.FOG
    val color = if (weather == app.tend.core.Weather.RAIN) Color(0xFFE3E7E9) else Color.White
    val rnd = java.util.Random(13L)
    repeat(if (many) 6 else 3) { i ->
        val x0 = rnd.nextFloat()
        val y = h * (0.06f + 0.24f * rnd.nextFloat())
        val r = h * (if (many) 0.05f else 0.035f) * (0.8f + 0.5f * rnd.nextFloat())
        val speed = 0.004f + 0.003f * rnd.nextFloat()
        val x = (((x0 + t * speed) % 1.4f) - 0.2f) * w
        val alpha = if (many) 0.85f else 0.7f
        drawCircle(color.copy(alpha = alpha), r, Offset(x, y))
        drawCircle(color.copy(alpha = alpha), r * 0.75f, Offset(x - r * 0.9f, y + r * 0.25f))
        drawCircle(color.copy(alpha = alpha), r * 0.85f, Offset(x + r * 0.95f, y + r * 0.2f))
        drawRoundRect(color.copy(alpha = alpha), Offset(x - r * 1.6f, y + r * 0.1f), Size(r * 3.3f, r * 0.75f), CornerRadius(r * 0.4f))
    }
}

/** Rain falling, slanting a little, and rings on the river. */
private fun DrawScope.rain(t: Float, u: Float, k: Float = 1f) {
    if (k <= 0f) return
    val w = size.width
    val h = size.height
    val rnd = java.util.Random(41L)
    repeat(110) { i ->
        val x0 = rnd.nextFloat()
        val y0 = rnd.nextFloat()
        val speed = 0.9f + 0.4f * rnd.nextFloat()
        val y = ((y0 + t * speed) % 1.1f - 0.05f) * h
        val x = x0 * w + y * 0.06f
        // Fewer drops as a shower fades in or out.
        if (i < 110 * k) drawLine(Color(0xFFDCE6EC).copy(alpha = 0.55f), Offset(x, y), Offset(x + h * 0.0025f, y + h * 0.028f), strokeWidth = u * 0.0022f, cap = StrokeCap.Round)
    }
    repeat(7) { i ->
        val c = Offset(w * (0.04f + 0.22f * rnd.nextFloat()), h * (0.64f + 0.3f * rnd.nextFloat()))
        val r = ((t * 0.7f + i / 7f) % 1f)
        drawOval(Color.White.copy(alpha = 0.45f * (1f - r) * k), Offset(c.x - u * 0.03f * r, c.y - u * 0.006f * r), Size(u * 0.06f * r, u * 0.012f * r), style = Stroke(u * 0.0018f))
    }
}

/** Snow coming down, swaying as it falls. */
private fun DrawScope.snowfall(t: Float, u: Float) {
    val w = size.width
    val h = size.height
    val rnd = java.util.Random(23L)
    repeat(80) { i ->
        val x0 = rnd.nextFloat()
        val y0 = rnd.nextFloat()
        val r = u * (0.002f + 0.003f * rnd.nextFloat())
        val speed = 0.03f + 0.03f * rnd.nextFloat()
        val y = ((y0 + t * speed) % 1f) * h
        val x = (x0 + kotlin.math.sin(t * 0.6f + i) * 0.008f) * w
        drawCircle(Color.White.copy(alpha = 0.6f + 0.35f * rnd.nextFloat()), r, Offset(x, y))
    }
}

/** A small hut up the mountain: a roof, a lit window after dark, smoke curling up. */
private fun DrawScope.mountainHut(p: Offset, u: Float, t: Float, lights: MutableList<Light>) {
    val s = u * 0.028f
    drawRect(Color(0xFF7A5A43), Offset(p.x - s, p.y - s * 1.1f), Size(s * 2f, s * 1.1f))
    drawPath(Path().apply { moveTo(p.x - s * 1.3f, p.y - s * 1.05f); lineTo(p.x, p.y - s * 2f); lineTo(p.x + s * 1.3f, p.y - s * 1.05f); close() }, Color(0xFF4A3528))
    drawRect(Color(0xFF4A3528), Offset(p.x + s * 0.45f, p.y - s * 2f), Size(s * 0.3f, s * 0.6f))
    drawRect(Glow, Offset(p.x - s * 0.45f, p.y - s * 0.75f), Size(s * 0.45f, s * 0.4f))
    lights += Light(p + Offset(-s * 0.22f, -s * 0.55f), u * 0.03f, 0.6f)
    for (i in 0 until 3) {
        val k = ((t * 0.25f + i / 3f) % 1f)
        drawCircle(Color.White.copy(alpha = 0.45f * (1f - k)), s * (0.18f + 0.3f * k), Offset(p.x + s * 0.6f + kotlin.math.sin(k * 5f + i) * s * 0.4f, p.y - s * 2.1f - k * s * 2.2f))
    }
}

/** A pennant flag on a pole, flapping in the breeze. */
private fun DrawScope.flag(p: Offset, u: Float, t: Float) {
    val top = p + Offset(0f, -u * 0.13f)
    shadow(p, u * 0.01f, u)
    drawLine(Color(0xFF6B4A33), p, top, strokeWidth = u * 0.004f, cap = StrokeCap.Round)
    drawCircle(Color(0xFFF2C14E), u * 0.004f, top)
    val wave = kotlin.math.sin(t * 3.2f) * u * 0.006f
    drawPath(Path().apply {
        moveTo(top.x, top.y + u * 0.004f)
        quadraticTo(top.x + u * 0.025f, top.y + u * 0.008f + wave, top.x + u * 0.05f, top.y + u * 0.016f + wave * 1.4f)
        quadraticTo(top.x + u * 0.025f, top.y + u * 0.026f + wave, top.x, top.y + u * 0.032f)
        close()
    }, Color(0xFFE07A5F))
}

/** A little lamp on a post by the path; lit after dark. */
private fun DrawScope.pathLamp(p: Offset, u: Float, lights: MutableList<Light>) {
    val k = depthScale(p.y / size.height)
    val top = p + Offset(0f, -u * 0.045f * k)
    drawLine(Color(0xFF4A3B2E), p, top, strokeWidth = u * 0.003f * k, cap = StrokeCap.Round)
    drawRoundRect(Color(0xFF4A3B2E), top + Offset(-u * 0.007f * k, -u * 0.012f * k), Size(u * 0.014f * k, u * 0.004f * k), CornerRadius(u * 0.002f))
    drawRoundRect(Glow, top + Offset(-u * 0.005f * k, -u * 0.008f * k), Size(u * 0.01f * k, u * 0.009f * k), CornerRadius(u * 0.002f))
    lights += Light(top + Offset(0f, -u * 0.004f * k), u * 0.06f * k, 0.8f)
}

/** A small vegetable garden: a raised bed, rows of greens, a few carrots and a pumpkin. */
private fun DrawScope.garden(p: Offset, u: Float) = withTransform({ scale(1.3f, 1.3f, pivot = p) }) {
    val w = u * 0.09f
    shadow(p, w * 0.55f, u)
    drawRoundRect(Color(0xFF8B6446), Offset(p.x - w / 2, p.y - u * 0.016f), Size(w, u * 0.018f), CornerRadius(u * 0.004f))
    drawRoundRect(Color(0xFF5E4232), Offset(p.x - w / 2 + u * 0.003f, p.y - u * 0.016f), Size(w - u * 0.006f, u * 0.006f), CornerRadius(u * 0.003f))
    for (i in 0 until 6) {
        val x = p.x - w / 2 + u * 0.009f + i * (w - u * 0.018f) / 5f
        val y = p.y - u * 0.017f
        if (i % 3 == 1) drawCircle(Color(0xFFE88E3C), u * 0.006f, Offset(x, y - u * 0.003f))
        else {
            for (l in listOf(-1f, 0f, 1f)) drawOval(Color(0xFF6E9A4A), Offset(x + l * u * 0.003f - u * 0.0025f, y - u * 0.012f), Size(u * 0.005f, u * 0.012f))
        }
    }
}

/** Dust kicked up where something lands: little puffs rolling out both ways and fading. */
private fun DrawScope.dustPuff(p: Offset, u: Float, k: Float) {
    if (k <= 0f || k >= 1f) return
    val e = 1f - (1f - k) * (1f - k)
    for (i in 0 until 8) {
        val a = Math.PI.toFloat() * (0.04f + 0.92f * i / 7f)
        val d = u * (0.025f + 0.07f * e)
        val c = p + Offset(kotlin.math.cos(a) * d * 1.5f, -kotlin.math.sin(a) * d * 0.3f - u * 0.004f)
        drawCircle(Color(0xFFE6DCBE).copy(alpha = 0.5f * (1f - k) * (1f - k)), u * (0.005f + 0.008f * e) * (if (i % 2 == 0) 1f else 0.7f), c)
    }
}

private fun DrawScope.shadow(at: Offset, halfWidth: Float, u: Float) {
    val c = at + Offset(halfWidth * 0.25f, 0f)
    drawOval(Color.Black.copy(alpha = 0.07f), Offset(c.x - halfWidth * 1.25f, c.y - u * 0.016f), Size(halfWidth * 2.5f, u * 0.034f))
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(c.x - halfWidth * 0.95f, c.y - u * 0.011f), Size(halfWidth * 1.9f, u * 0.022f))
}

private fun DrawScope.firePitStones(c: Offset, u: Float) {
    val rnd = java.util.Random(13L)
    for (i in 0 until 11) {
        val a = Math.PI * 2 * i / 11
        val p = c + Offset((u * 0.075f * Math.cos(a)).toFloat(), (u * 0.024f * Math.sin(a)).toFloat())
        val r = u * (0.014f + 0.005f * rnd.nextFloat())
        drawOval(if (i % 2 == 0) Stone else StoneDark, Offset(p.x - r, p.y - r * 0.75f), Size(r * 2, r * 1.5f))
    }
}

/** How wide a thing is, in scene heights, for its shadow. */
internal fun thingWidth(id: String): Float = when (id) {
    "leaf_pile", "sled", "deck_chair", "scarecrow" -> 0.05f
    "cabin" -> 0.22f
    "bell_tent", "tent" -> 0.16f
    "picnic_table", "log_bench" -> 0.11f
    "rocking_chair" -> 0.07f
    "raft" -> 0.16f
    "canoe" -> 0.16f
    "campfire", "kettle", "pizza_oven" -> 0.1f
    "hammock", "swing", "treehouse" -> 0.08f
    else -> 0.04f
}

// ---- Things ----

/** Strung between two trees: string lights in a soft curve, or a line with wind chimes or a bird feeder hanging from its middle. */
private fun DrawScope.overhead(id: String, a: Offset, b: Offset, u: Float, lights: MutableList<Light>) {
    val sag = u * 0.07f
    fun point(t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t + sag * 4 * t * (1 - t))
    drawPath(Path().apply {
        moveTo(a.x, a.y); quadraticTo((a.x + b.x) / 2, (a.y + b.y) / 2 + sag * 2, b.x, b.y)
    }, BarkDark.copy(alpha = 0.7f), style = Stroke(u * 0.003f))
    when (id) {
        "wind_chimes", "bird_feeder" -> drawThing(id, point(0.5f), u, lights)
        "string_lights" -> for (i in 0 until 20) {
            val p = point((i + 0.5f) / 20f) + Offset(0f, u * 0.008f)
            drawCircle(Glow.copy(alpha = 0.35f), u * 0.012f, p)
            drawCircle(Color(0xFFFFF1C2), u * 0.005f, p)
            lights += Light(p, u * 0.045f, 0.7f)
        }
    }
}

/**
 * Draws one thing standing at [p] (its base); [u] is the scene height, so things scale with it.
 * Glowing parts are added to [lights] for the night. Overhead things are drawn by the scene.
 */
fun DrawScope.drawThing(id: String, p: Offset, u: Float, lights: MutableList<Light> = mutableListOf()) {
    when (id) {
        // ---- The events' own ----
        "jack_o_lantern" -> {
            // A carved pumpkin with a grin, lit from inside.
            val c = p + Offset(0f, -u * 0.016f)
            drawOval(Color(0xFFD9772B), Offset(c.x - u * 0.022f, c.y - u * 0.016f), Size(u * 0.044f, u * 0.032f))
            drawOval(Color(0xFFE88E3C), Offset(c.x - u * 0.012f, c.y - u * 0.016f), Size(u * 0.024f, u * 0.032f))
            drawLine(Color(0xFF4D7139), c + Offset(0f, -u * 0.016f), c + Offset(u * 0.004f, -u * 0.024f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            for (side in listOf(-1f, 1f)) drawPath(Path().apply { moveTo(c.x + side * u * 0.011f, c.y - u * 0.007f); lineTo(c.x + side * u * 0.005f, c.y - u * 0.001f); lineTo(c.x + side * u * 0.015f, c.y - u * 0.001f); close() }, Glow)
            drawArc(Glow, 10f, 160f, true, Offset(c.x - u * 0.012f, c.y - u * 0.006f), Size(u * 0.024f, u * 0.014f))
            lights += Light(c, u * 0.07f, 0.9f)
        }
        "paper_lanterns" -> {
            // Three paper lanterns on a crooked stick, red, yellow and blue, glowing softly.
            val top = p + Offset(-u * 0.006f, -u * 0.075f)
            drawLine(BarkDark, p, top, strokeWidth = u * 0.003f, cap = StrokeCap.Round)
            drawLine(BarkDark, top, top + Offset(u * 0.04f, -u * 0.006f), strokeWidth = u * 0.0025f, cap = StrokeCap.Round)
            listOf(Color(0xFFE5677A), Color(0xFFF2C14E), Color(0xFF6FA8DC)).forEachIndexed { i, col ->
                val hook = top + Offset(u * (0.008f + i * 0.014f), -u * 0.0015f * i)
                val at = hook + Offset(0f, u * (0.012f + (i % 2) * 0.006f))
                drawLine(Color(0xFF6B5A4A), hook, at, strokeWidth = u * 0.001f)
                drawOval(col, Offset(at.x - u * 0.0065f, at.y), Size(u * 0.013f, u * 0.016f))
                drawOval(Color.White.copy(alpha = 0.35f), Offset(at.x - u * 0.003f, at.y + u * 0.003f), Size(u * 0.005f, u * 0.008f))
                lights += Light(at + Offset(0f, u * 0.008f), u * 0.04f, 0.7f)
            }
        }
        "xmas_tree" -> {
            // A little fir in three tiers, a star on top, and lights hung in it.
            drawRect(Wood, Offset(p.x - u * 0.004f, p.y - u * 0.012f), Size(u * 0.008f, u * 0.012f))
            for (k in 0 until 3) {
                val base = p.y - u * (0.01f + k * 0.022f)
                val half = u * (0.032f - k * 0.008f)
                drawPath(Path().apply { moveTo(p.x - half, base); lineTo(p.x, base - u * 0.032f); lineTo(p.x + half, base); close() }, if (k % 2 == 0) Color(0xFF2E5E3E) else Color(0xFF356B47))
            }
            val star = p + Offset(0f, -u * 0.088f)
            drawCircle(Color(0xFFF2C14E), u * 0.006f, star)
            lights += Light(star, u * 0.05f, 0.8f)
            listOf(Offset(-0.014f, -0.022f), Offset(0.012f, -0.03f), Offset(-0.006f, -0.048f), Offset(0.009f, -0.06f), Offset(-0.018f, -0.012f)).forEachIndexed { i, o ->
                val at = p + Offset(o.x * u, o.y * u)
                drawCircle(listOf(Color(0xFFE5677A), Color(0xFFF2C14E), Color(0xFF6FA8DC))[i % 3], u * 0.0035f, at)
                lights += Light(at, u * 0.025f, 0.5f)
            }
        }
        "flower_arch" -> {
            // Two posts with a garland of flowers between them.
            val l = p + Offset(-u * 0.03f, 0f)
            val r = p + Offset(u * 0.03f, 0f)
            for (b in listOf(l, r)) drawLine(Wood, b, b + Offset(0f, -u * 0.07f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            for (k in 0..8) {
                val t = k / 8f
                val at = Offset(l.x + (r.x - l.x) * t, l.y - u * 0.07f + kotlin.math.sin(t * Math.PI.toFloat()) * u * 0.018f)
                drawCircle(Color(0xFF6E9A4A), u * 0.005f, at + Offset(0f, u * 0.002f))
                drawCircle(listOf(Color(0xFFF2A0B8), Color(0xFFF2C14E), Color.White, Color(0xFFE5677A))[k % 4], u * 0.0045f, at)
            }
        }
        // ---- The seasons' own ----
        "tulips" -> for (k in 0 until 5) {
            val dx = (k - 2) * u * 0.009f
            val top = p + Offset(dx * 1.2f, -u * (0.032f + 0.008f * ((k * 7) % 3)))
            drawLine(Color(0xFF4D7139), p + Offset(dx, 0f), top, strokeWidth = u * 0.0032f)
            drawOval(Color(0xFF6E9A4A), top + Offset(-u * 0.008f, u * 0.008f), Size(u * 0.007f, u * 0.016f)) // a leaf
            val c = listOf(Color(0xFFE5677A), Color(0xFFF2C14E), Color(0xFFE07A5F), Color(0xFFF2A0B8), Color(0xFFE5677A))[k]
            drawRoundRect(c, top + Offset(-u * 0.006f, -u * 0.012f), Size(u * 0.012f, u * 0.014f), CornerRadius(u * 0.005f))
            drawPath(Path().apply { moveTo(top.x - u * 0.006f, top.y - u * 0.008f); lineTo(top.x - u * 0.003f, top.y - u * 0.016f); lineTo(top.x, top.y - u * 0.009f); lineTo(top.x + u * 0.003f, top.y - u * 0.016f); lineTo(top.x + u * 0.006f, top.y - u * 0.008f); close() }, c)
        }
        "watering_can" -> {
            val body = Color(0xFF6E9FB5)
            drawRoundRect(body, p + Offset(-u * 0.016f, -u * 0.03f), Size(u * 0.032f, u * 0.03f), CornerRadius(u * 0.004f))
            drawRoundRect(Color.White.copy(alpha = 0.25f), p + Offset(-u * 0.013f, -u * 0.027f), Size(u * 0.006f, u * 0.022f), CornerRadius(u * 0.003f))
            drawLine(body, p + Offset(u * 0.014f, -u * 0.012f), p + Offset(u * 0.034f, -u * 0.036f), strokeWidth = u * 0.0045f, cap = StrokeCap.Round) // spout
            drawOval(body, p + Offset(u * 0.03f, -u * 0.041f), Size(u * 0.01f, u * 0.008f)) // rose
            drawArc(body, 180f, 180f, false, p + Offset(-u * 0.012f, -u * 0.044f), Size(u * 0.024f, u * 0.026f), style = Stroke(u * 0.0035f)) // handle
        }
        "birdhouse" -> {
            drawLine(Wood, p, p + Offset(0f, -u * 0.06f), strokeWidth = u * 0.005f)
            val box = p + Offset(0f, -u * 0.06f)
            drawRect(Color(0xFFE9C46A), box + Offset(-u * 0.016f, -u * 0.026f), Size(u * 0.032f, u * 0.028f))
            drawPath(Path().apply { moveTo(box.x - u * 0.022f, box.y - u * 0.024f); lineTo(box.x, box.y - u * 0.044f); lineTo(box.x + u * 0.022f, box.y - u * 0.024f); close() }, Color(0xFFC0533B))
            drawCircle(Color(0xFF3A2414), u * 0.005f, box + Offset(0f, -u * 0.014f))
            drawLine(Wood, box + Offset(-u * 0.004f, -u * 0.006f), box + Offset(u * 0.004f, -u * 0.006f), strokeWidth = u * 0.002f) // perch
        }
        "beach_ball" -> {
            val c = p + Offset(0f, -u * 0.016f)
            val r = u * 0.016f
            for ((i, col) in listOf(Color(0xFFE5677A), Color.White, Color(0xFF5FA8D3), Color.White, Color(0xFFF2C14E), Color.White).withIndex())
                drawArc(col, i * 60f - 90f, 60f, true, c - Offset(r, r), Size(r * 2, r * 2))
            drawCircle(Color.White, r * 0.25f, c)
            drawCircle(Color.Black.copy(alpha = 0.12f), r, c, style = Stroke(u * 0.0015f))
        }
        "watermelon" -> {
            drawArc(Color(0xFF4E8A3E), 0f, 180f, true, p + Offset(-u * 0.026f, -u * 0.02f), Size(u * 0.052f, u * 0.04f)) // a half, rind down
            drawArc(Color(0xFFF2F0D8), 0f, 180f, true, p + Offset(-u * 0.023f, -u * 0.018f), Size(u * 0.046f, u * 0.034f))
            drawArc(Color(0xFFE5525E), 0f, 180f, true, p + Offset(-u * 0.021f, -u * 0.017f), Size(u * 0.042f, u * 0.031f))
            for (k in 0 until 4) drawOval(Color(0xFF2B1C14), p + Offset((k - 1.5f) * u * 0.008f - u * 0.0015f, -u * 0.001f + (k % 2) * u * 0.004f), Size(u * 0.003f, u * 0.004f))
            drawRect(Color(0xFF4E8A3E), p + Offset(-u * 0.026f, -u * 0.0035f), Size(u * 0.052f, u * 0.0035f))
        }
        "deck_chair" -> {
            drawLine(Wood, p + Offset(-u * 0.026f, 0f), p + Offset(u * 0.014f, -u * 0.05f), strokeWidth = u * 0.0035f, cap = StrokeCap.Round)
            drawLine(Wood, p + Offset(u * 0.022f, 0f), p + Offset(-u * 0.012f, -u * 0.026f), strokeWidth = u * 0.0035f, cap = StrokeCap.Round)
            // Striped canvas, from the top of the back down to the seat.
            val canvas = Path().apply { moveTo(p.x + u * 0.012f, p.y - u * 0.048f); lineTo(p.x - u * 0.004f, p.y - u * 0.012f); lineTo(p.x - u * 0.022f, p.y - u * 0.016f); lineTo(p.x - u * 0.003f, p.y - u * 0.052f); close() }
            drawPath(canvas, Color(0xFFF2F0E6))
            for (k in 0 until 3) drawLine(Color(0xFF5FA8D3), Offset(p.x - u * (0.0005f - k * 0.0055f), p.y - u * 0.052f), Offset(p.x - u * (0.02f - k * 0.0055f), p.y - u * 0.015f), strokeWidth = u * 0.0028f)
        }
        "leaf_pile" -> {
            val rnd = java.util.Random(5L)
            val leaves = listOf(Color(0xFFD9822B), Color(0xFFE0A341), Color(0xFFB5502E), Color(0xFFC9A03A))
            drawOval(Color(0xFFC2702F), p + Offset(-u * 0.04f, -u * 0.02f), Size(u * 0.08f, u * 0.026f))
            for (k in 0 until 26) {
                val a = rnd.nextFloat() * 3.14f
                val r = rnd.nextFloat()
                val c = p + Offset(kotlin.math.cos(a) * u * 0.036f * r, -kotlin.math.sin(a) * u * 0.022f * r - u * 0.006f)
                rotate(rnd.nextFloat() * 180f, c) { drawOval(leaves[k % 4], c + Offset(-u * 0.006f, -u * 0.0035f), Size(u * 0.012f, u * 0.007f)) }
            }
        }
        "pumpkins" -> {
            fun pumpkin(c: Offset, r: Float) {
                drawOval(Color(0xFFD9822B), c + Offset(-r * 1.15f, -r), Size(r * 2.3f, r * 1.9f))
                drawOval(Color(0xFFE89A3C), c + Offset(-r * 0.45f, -r), Size(r * 0.9f, r * 1.9f))
                drawOval(Color(0xFFB5651D).copy(alpha = 0.5f), c + Offset(-r * 1.15f, -r), Size(r * 2.3f, r * 1.9f), style = Stroke(r * 0.08f))
                drawLine(Color(0xFF5E6B2E), c + Offset(0f, -r * 0.9f), c + Offset(r * 0.18f, -r * 1.35f), strokeWidth = r * 0.22f, cap = StrokeCap.Round)
            }
            pumpkin(p + Offset(-u * 0.013f, -u * 0.012f), u * 0.013f)
            pumpkin(p + Offset(u * 0.014f, -u * 0.009f), u * 0.009f)
        }
        "scarecrow" -> {
            drawLine(Wood, p, p + Offset(0f, -u * 0.095f), strokeWidth = u * 0.004f)
            drawLine(Wood, p + Offset(-u * 0.03f, -u * 0.07f), p + Offset(u * 0.03f, -u * 0.07f), strokeWidth = u * 0.004f)
            // Shirt, straw hands, a sack head and a hat.
            drawPath(Path().apply { moveTo(p.x - u * 0.026f, p.y - u * 0.074f); lineTo(p.x + u * 0.026f, p.y - u * 0.074f); lineTo(p.x + u * 0.014f, p.y - u * 0.035f); lineTo(p.x - u * 0.014f, p.y - u * 0.035f); close() }, Color(0xFF7B8FB8))
            drawLine(Color(0xFF5E709A), p + Offset(-u * 0.006f, -u * 0.074f), p + Offset(-u * 0.004f, -u * 0.036f), strokeWidth = u * 0.002f)
            for (side in listOf(-1f, 1f)) for (k in -1..1) drawLine(Color(0xFFE2C26B), p + Offset(side * u * 0.03f, -u * 0.07f), p + Offset(side * u * 0.038f, -u * (0.07f - k * 0.006f)), strokeWidth = u * 0.0018f)
            val head = p + Offset(0f, -u * 0.086f)
            drawCircle(Color(0xFFE6D3A3), u * 0.012f, head)
            drawCircle(Color(0xFF3A2414), u * 0.0018f, head + Offset(-u * 0.004f, -u * 0.001f))
            drawCircle(Color(0xFF3A2414), u * 0.0018f, head + Offset(u * 0.004f, -u * 0.001f))
            drawArc(Color(0xFF3A2414), 20f, 140f, false, head + Offset(-u * 0.004f, 0f), Size(u * 0.008f, u * 0.005f), style = Stroke(u * 0.0012f))
            drawRect(Color(0xFF6B4A2B), head + Offset(-u * 0.017f, -u * 0.012f), Size(u * 0.034f, u * 0.003f))
            drawRoundRect(Color(0xFF6B4A2B), head + Offset(-u * 0.009f, -u * 0.022f), Size(u * 0.018f, u * 0.011f), CornerRadius(u * 0.002f))
        }
        "gift_boxes" -> {
            fun box(o: Offset, w: Float, h: Float, c: Color, ribbon: Color) {
                drawRect(c, o + Offset(-w / 2, -h), Size(w, h))
                drawRect(ribbon, o + Offset(-w * 0.08f, -h), Size(w * 0.16f, h))
                drawRect(ribbon, o + Offset(-w / 2, -h * 0.58f), Size(w, h * 0.14f))
                drawOval(ribbon, o + Offset(-w * 0.3f, -h - w * 0.16f), Size(w * 0.28f, w * 0.18f))
                drawOval(ribbon, o + Offset(w * 0.02f, -h - w * 0.16f), Size(w * 0.28f, w * 0.18f))
            }
            box(p + Offset(-u * 0.01f, 0f), u * 0.03f, u * 0.026f, Color(0xFFC0533B), Color(0xFFF2D27A))
            box(p + Offset(u * 0.017f, 0f), u * 0.022f, u * 0.018f, Color(0xFF3F7A5A), Color(0xFFF7F3E8))
            box(p + Offset(-u * 0.006f, -u * 0.026f), u * 0.018f, u * 0.015f, Color(0xFF5FA8D3), Color(0xFFE5677A))
        }
        "sled" -> {
            drawRoundRect(Color(0xFFB5502E), p + Offset(-u * 0.03f, -u * 0.016f), Size(u * 0.056f, u * 0.008f), CornerRadius(u * 0.002f))
            for (k in 0 until 4) drawLine(Color(0xFF8E3B22), p + Offset(-u * 0.022f + k * u * 0.013f, -u * 0.016f), p + Offset(-u * 0.022f + k * u * 0.013f, -u * 0.008f), strokeWidth = u * 0.0018f)
            // The runner curls up at the front.
            drawPath(Path().apply { moveTo(p.x - u * 0.03f, p.y - u * 0.002f); lineTo(p.x + u * 0.026f, p.y - u * 0.002f); quadraticTo(p.x + u * 0.038f, p.y - u * 0.002f, p.x + u * 0.036f, p.y - u * 0.014f) }, Metal, style = Stroke(u * 0.0025f, cap = StrokeCap.Round))
            drawLine(Metal, p + Offset(-u * 0.022f, -u * 0.002f), p + Offset(-u * 0.022f, -u * 0.008f), strokeWidth = u * 0.002f)
            drawLine(Metal, p + Offset(u * 0.018f, -u * 0.002f), p + Offset(u * 0.018f, -u * 0.008f), strokeWidth = u * 0.002f)
        }
        "snowman" -> {
            val snow = Color(0xFFF7F9FA)
            val shade = Color(0xFFD6E0E6)
            drawCircle(shade, u * 0.022f, p + Offset(u * 0.002f, -u * 0.02f))
            drawCircle(snow, u * 0.021f, p + Offset(0f, -u * 0.021f))
            drawCircle(shade, u * 0.016f, p + Offset(u * 0.0015f, -u * 0.052f))
            drawCircle(snow, u * 0.015f, p + Offset(0f, -u * 0.053f))
            val head = p + Offset(0f, -u * 0.077f)
            drawCircle(snow, u * 0.011f, head)
            drawCircle(Color(0xFF2B2B2B), u * 0.0016f, head + Offset(-u * 0.004f, -u * 0.002f))
            drawCircle(Color(0xFF2B2B2B), u * 0.0016f, head + Offset(u * 0.004f, -u * 0.002f))
            drawPath(Path().apply { moveTo(head.x, head.y + u * 0.0005f); lineTo(head.x + u * 0.013f, head.y + u * 0.003f); lineTo(head.x, head.y + u * 0.004f); close() }, Color(0xFFF07F2B)) // carrot
            drawRect(Color(0xFFC0533B), head + Offset(-u * 0.011f, u * 0.008f), Size(u * 0.022f, u * 0.004f)) // scarf
            drawRect(Color(0xFFC0533B), head + Offset(u * 0.004f, u * 0.01f), Size(u * 0.004f, u * 0.012f))
            for (k in 0 until 3) drawCircle(Color(0xFF2B2B2B), u * 0.0017f, p + Offset(0f, -u * (0.045f + k * 0.007f)))
            drawLine(Wood, p + Offset(-u * 0.014f, -u * 0.056f), p + Offset(-u * 0.03f, -u * 0.068f), strokeWidth = u * 0.0018f, cap = StrokeCap.Round) // stick arms
            drawLine(Wood, p + Offset(u * 0.014f, -u * 0.056f), p + Offset(u * 0.03f, -u * 0.07f), strokeWidth = u * 0.0018f, cap = StrokeCap.Round)
        }
        "wildflowers" -> {
            for (k in 0 until 7) {
                val dx = (k - 3) * u * 0.008f
                val top = p + Offset(dx * 1.4f, -u * (0.025f + 0.012f * ((k * 5) % 3)))
                drawLine(Color(0xFF4D7139), p + Offset(dx, 0f), top, strokeWidth = u * 0.003f)
                drawCircle(listOf(Color.White, Color(0xFFC9A6E3), Color(0xFFF2D27A), Color(0xFFE9A0A0))[k % 4], u * 0.006f, top)
            }
        }
        "lantern" -> {
            val c = p + Offset(0f, -u * 0.026f)
            drawRoundRect(Metal, c + Offset(-u * 0.012f, -u * 0.03f), Size(u * 0.024f, u * 0.006f), CornerRadius(u * 0.002f))
            drawRoundRect(Glow, c + Offset(-u * 0.01f, -u * 0.024f), Size(u * 0.02f, u * 0.024f), CornerRadius(u * 0.006f))
            drawRoundRect(Metal, c + Offset(-u * 0.012f, 0f), Size(u * 0.024f, u * 0.006f), CornerRadius(u * 0.002f))
            drawArc(Metal, 180f, 180f, false, c + Offset(-u * 0.007f, -u * 0.04f), Size(u * 0.014f, u * 0.016f), style = Stroke(u * 0.002f))
            lights += Light(c + Offset(0f, -u * 0.012f), u * 0.07f)
        }
        "woodpile" -> {
            val rnd = java.util.Random(21L)
            for (row in 0 until 3) for (k in 0 until 4 - row) {
                val c = p + Offset((k - (3 - row) / 2f) * u * 0.016f, -u * (0.008f + row * 0.013f))
                drawCircle(WoodPale, u * 0.0085f, c)
                drawCircle(Wood, u * 0.0085f, c, style = Stroke(u * 0.0022f))
                drawCircle(WoodLight, u * 0.0025f * (0.7f + rnd.nextFloat() * 0.6f), c)
            }
        }
        "guitar" -> rotate(-14f, p) {
            drawLine(BarkDark, p + Offset(0f, -u * 0.04f), p + Offset(0f, -u * 0.085f), strokeWidth = u * 0.006f, cap = StrokeCap.Round)
            drawRoundRect(BarkDark, p + Offset(-u * 0.005f, -u * 0.095f), Size(u * 0.01f, u * 0.014f), CornerRadius(u * 0.003f))
            drawOval(Color(0xFFC98B4A), p + Offset(-u * 0.018f, -u * 0.035f), Size(u * 0.036f, u * 0.034f))
            drawOval(Color(0xFFC98B4A), p + Offset(-u * 0.014f, -u * 0.054f), Size(u * 0.028f, u * 0.026f))
            drawCircle(Color(0xFF3A2414), u * 0.006f, p + Offset(0f, -u * 0.033f))
        }
        "fishing_rod" -> {
            // A rod resting on a forked stick, its line out to a red-and-white float, and a small tackle box.
            val fork = p + Offset(u * 0.012f, 0f)
            drawLine(Bark, fork, fork + Offset(0f, -u * 0.03f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            drawLine(Bark, fork + Offset(0f, -u * 0.026f), fork + Offset(-u * 0.006f, -u * 0.036f), strokeWidth = u * 0.003f, cap = StrokeCap.Round)
            drawLine(Bark, fork + Offset(0f, -u * 0.026f), fork + Offset(u * 0.006f, -u * 0.036f), strokeWidth = u * 0.003f, cap = StrokeCap.Round)
            val butt = p + Offset(u * 0.035f, -u * 0.004f)
            val tip = p + Offset(-u * 0.045f, -u * 0.09f)
            drawLine(BarkDark, butt, tip, strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            drawLine(Color(0xFF2B2B2B), butt, butt + (tip - butt) * 0.22f, strokeWidth = u * 0.006f, cap = StrokeCap.Round) // grip
            drawCircle(Metal, u * 0.006f, butt + (tip - butt) * 0.16f + Offset(0f, u * 0.006f)) // reel
            val float = p + Offset(-u * 0.06f, -u * 0.012f)
            drawPath(Path().apply { moveTo(tip.x, tip.y); quadraticTo(tip.x - u * 0.02f, tip.y + u * 0.05f, float.x, float.y) }, Color.White.copy(alpha = 0.6f), style = Stroke(u * 0.0012f))
            drawArc(Color(0xFFD64541), 180f, 180f, true, float + Offset(-u * 0.005f, -u * 0.005f), Size(u * 0.01f, u * 0.01f))
            drawArc(Color.White, 0f, 180f, true, float + Offset(-u * 0.005f, -u * 0.005f), Size(u * 0.01f, u * 0.01f))
            drawRoundRect(Color(0xFF3F6E5A), p + Offset(u * 0.02f, -u * 0.014f), Size(u * 0.024f, u * 0.014f), CornerRadius(u * 0.003f))
            drawLine(Color(0xFF2E5244), p + Offset(u * 0.02f, -u * 0.009f), p + Offset(u * 0.044f, -u * 0.009f), strokeWidth = u * 0.0015f)
        }
        "telescope" -> {
            // A brass telescope on a wooden tripod, pointing up at the sky.
            val head = p + Offset(0f, -u * 0.06f)
            for (dx in listOf(-0.022f, 0f, 0.022f)) drawLine(Wood, head, p + Offset(u * dx, 0f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            rotate(-32f, head) {
                drawRoundRect(Color(0xFFB8893B), head + Offset(-u * 0.035f, -u * 0.008f), Size(u * 0.07f, u * 0.016f), CornerRadius(u * 0.004f))
                drawRoundRect(Color(0xFFD9AE5C), head + Offset(-u * 0.035f, -u * 0.008f), Size(u * 0.07f, u * 0.005f), CornerRadius(u * 0.003f))
                drawRoundRect(Color(0xFF8C6526), head + Offset(u * 0.03f, -u * 0.011f), Size(u * 0.012f, u * 0.022f), CornerRadius(u * 0.003f))
                drawRoundRect(Color(0xFF8C6526), head + Offset(-u * 0.045f, -u * 0.005f), Size(u * 0.012f, u * 0.01f), CornerRadius(u * 0.002f))
            }
            drawCircle(BarkDark, u * 0.004f, head)
        }
        "rocking_chair" -> {
            // Side on: curved rockers, a slatted seat and back, armrests.
            drawArc(Wood, 20f, 140f, false, Offset(p.x - u * 0.04f, p.y - u * 0.03f), Size(u * 0.08f, u * 0.034f), style = Stroke(u * 0.005f, cap = StrokeCap.Round))
            for (dx in listOf(-0.018f, 0.018f)) drawLine(Wood, p + Offset(u * dx, -u * 0.01f), p + Offset(u * dx, -u * 0.035f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            drawRoundRect(WoodLight, p + Offset(-u * 0.024f, -u * 0.04f), Size(u * 0.048f, u * 0.007f), CornerRadius(u * 0.003f))
            drawLine(Wood, p + Offset(u * 0.02f, -u * 0.037f), p + Offset(u * 0.03f, -u * 0.085f), strokeWidth = u * 0.005f, cap = StrokeCap.Round)
            drawLine(Wood, p + Offset(u * 0.012f, -u * 0.037f), p + Offset(u * 0.02f, -u * 0.085f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            for (k in 0 until 3) drawLine(WoodLight, p + Offset(u * (0.013f + k * 0.0035f), -u * (0.05f + k * 0.01f)), p + Offset(u * (0.025f + k * 0.0015f), -u * (0.05f + k * 0.01f)), strokeWidth = u * 0.003f)
            drawLine(Wood, p + Offset(-u * 0.022f, -u * 0.052f), p + Offset(u * 0.022f, -u * 0.054f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            drawLine(Wood, p + Offset(-u * 0.02f, -u * 0.04f), p + Offset(-u * 0.022f, -u * 0.052f), strokeWidth = u * 0.003f)
            drawRoundRect(Color(0xFF2A6F6B), p + Offset(-u * 0.016f, -u * 0.046f), Size(u * 0.03f, u * 0.006f), CornerRadius(u * 0.003f)) // a cushion
        }
        "raft" -> raftAt(p, u)
        "wind_chimes" -> {
            // A wooden disc on strings, metal tubes of different lengths, a clapper in the middle.
            drawLine(BarkDark, p, p + Offset(0f, u * 0.02f), strokeWidth = u * 0.0015f)
            val top = p + Offset(0f, u * 0.02f)
            drawOval(Wood, Offset(top.x - u * 0.016f, top.y - u * 0.003f), Size(u * 0.032f, u * 0.007f))
            for ((k, len) in listOf(0.03f, 0.042f, 0.05f, 0.038f, 0.028f).withIndex()) {
                val x = top.x + (k - 2) * u * 0.0065f
                drawLine(Color(0xFF7A7F84), Offset(x, top.y + u * 0.003f), Offset(x, top.y + u * 0.008f), strokeWidth = u * 0.0008f)
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFDDE2E6), Color(0xFF9AA3AB)), startX = x - u * 0.0018f, endX = x + u * 0.0018f), Offset(x - u * 0.0018f, top.y + u * 0.008f), Size(u * 0.0036f, u * len), CornerRadius(u * 0.0015f))
            }
            drawCircle(Wood, u * 0.004f, top + Offset(0f, u * 0.05f))
            drawLine(BarkDark, top + Offset(0f, u * 0.003f), top + Offset(0f, u * 0.046f), strokeWidth = u * 0.0008f)
        }
        "bird_feeder" -> {
            // A little house on a string: a roof, a tray of seeds, and a bird on the edge.
            drawLine(BarkDark, p, p + Offset(0f, u * 0.018f), strokeWidth = u * 0.0015f)
            val top = p + Offset(0f, u * 0.018f)
            drawPath(Path().apply { moveTo(top.x - u * 0.022f, top.y + u * 0.014f); lineTo(top.x, top.y); lineTo(top.x + u * 0.022f, top.y + u * 0.014f); close() }, Color(0xFFB5523B))
            for (dx in listOf(-0.014f, 0.014f)) drawLine(Wood, top + Offset(u * dx, u * 0.012f), top + Offset(u * dx, u * 0.034f), strokeWidth = u * 0.002f)
            drawRoundRect(WoodLight, top + Offset(-u * 0.018f, u * 0.032f), Size(u * 0.036f, u * 0.006f), CornerRadius(u * 0.002f))
            for (k in 0 until 7) drawCircle(Color(0xFFD9B26A), u * 0.0016f, top + Offset(u * (-0.012f + k * 0.004f), u * 0.031f))
            bird(top + Offset(u * 0.016f, u * 0.027f), u, Color(0xFF5D8FB8))
        }
        "campfire" -> campfire(p, u, lights)
        "kettle" -> {
            campfire(p, u, lights)
            val top = p + Offset(0f, -u * 0.11f)
            drawLine(Metal, p + Offset(-u * 0.05f, u * 0.005f), top, strokeWidth = u * 0.004f)
            drawLine(Metal, p + Offset(u * 0.05f, u * 0.005f), top, strokeWidth = u * 0.004f)
            drawLine(Metal, top, top + Offset(0f, u * 0.03f), strokeWidth = u * 0.002f)
            drawRoundRect(Metal, top + Offset(-u * 0.013f, u * 0.03f), Size(u * 0.026f, u * 0.02f), CornerRadius(u * 0.006f))
            drawCircle(Color.White.copy(alpha = 0.25f), u * 0.012f, top + Offset(u * 0.02f, -u * 0.01f))
        }
        "pizza_oven" -> {
            drawRect(StoneDark, p + Offset(-u * 0.05f, -u * 0.03f), Size(u * 0.1f, u * 0.03f))
            drawArc(Color(0xFFC07A4C), 180f, 180f, true, p + Offset(-u * 0.046f, -u * 0.09f), Size(u * 0.092f, u * 0.12f))
            drawArc(Color(0xFFD99566), 200f, 60f, false, p + Offset(-u * 0.04f, -u * 0.084f), Size(u * 0.08f, u * 0.1f), style = Stroke(u * 0.005f))
            drawArc(Color(0xFF2A1A10), 180f, 180f, true, p + Offset(-u * 0.02f, -u * 0.06f), Size(u * 0.04f, u * 0.054f))
            drawArc(Flame, 180f, 180f, true, p + Offset(-u * 0.012f, -u * 0.046f), Size(u * 0.024f, u * 0.03f))
            drawRect(StoneDark, p + Offset(u * 0.02f, -u * 0.12f), Size(u * 0.012f, u * 0.04f))
            lights += Light(p + Offset(0f, -u * 0.04f), u * 0.08f, 0.8f)
        }
        "log_bench" -> {
            drawRoundRect(Bark, p + Offset(-u * 0.055f, -u * 0.03f), Size(u * 0.11f, u * 0.03f), CornerRadius(u * 0.015f))
            drawRoundRect(Wood, p + Offset(-u * 0.055f, -u * 0.03f), Size(u * 0.11f, u * 0.01f), CornerRadius(u * 0.006f))
            drawOval(WoodPale, p + Offset(u * 0.045f, -u * 0.03f), Size(u * 0.016f, u * 0.03f))
            drawOval(WoodLight, p + Offset(u * 0.049f, -u * 0.022f), Size(u * 0.008f, u * 0.014f), style = Stroke(u * 0.0015f))
            // A striped blanket over it.
            drawRect(Color(0xFFB5523B), p + Offset(-u * 0.025f, -u * 0.031f), Size(u * 0.035f, u * 0.024f))
            for (k in 0 until 4) drawRect(listOf(Color(0xFFE9C46A), Color(0xFF2A9D8F), Color(0xFFF1E6D3), Color(0xFF264653))[k], p + Offset(-u * 0.025f + k * u * 0.009f, -u * 0.031f), Size(u * 0.003f, u * 0.024f))
        }
        "picnic_table" -> {
            // Seen a little from above: the top, two benches either side, A-frame legs.
            val top = p + Offset(0f, -u * 0.048f)
            drawRoundRect(Bark, Offset(p.x - u * 0.06f, p.y - u * 0.034f), Size(u * 0.12f, u * 0.007f), CornerRadius(u * 0.002f)) // far bench
            for (dx in listOf(-0.04f, 0.04f)) {
                drawLine(Bark, top + Offset(u * dx - u * 0.01f, 0f), p + Offset(u * dx - u * 0.016f, 0f), strokeWidth = u * 0.004f)
                drawLine(Bark, top + Offset(u * dx + u * 0.01f, 0f), p + Offset(u * dx + u * 0.016f, 0f), strokeWidth = u * 0.004f)
            }
            drawPath(Path().apply { moveTo(top.x - u * 0.055f, top.y - u * 0.012f); lineTo(top.x + u * 0.055f, top.y - u * 0.012f); lineTo(top.x + u * 0.062f, top.y + u * 0.004f); lineTo(top.x - u * 0.062f, top.y + u * 0.004f); close() }, Wood)
            for (k in 1..3) drawLine(Bark.copy(alpha = 0.5f), top + Offset(-u * 0.058f, -u * 0.012f + k * u * 0.004f), top + Offset(u * 0.058f, -u * 0.012f + k * u * 0.004f), strokeWidth = u * 0.0008f)
            drawRect(WoodLight, Offset(top.x - u * 0.062f, top.y + u * 0.004f), Size(u * 0.124f, u * 0.003f))
            drawRoundRect(Wood, Offset(p.x - u * 0.066f, p.y - u * 0.02f), Size(u * 0.132f, u * 0.008f), CornerRadius(u * 0.002f)) // near bench
            drawRect(WoodLight, Offset(p.x - u * 0.066f, p.y - u * 0.02f), Size(u * 0.132f, u * 0.002f))
            // Two enamel mugs and a flask.
            drawRoundRect(Color(0xFF2A6F6B), top + Offset(-u * 0.03f, -u * 0.02f), Size(u * 0.01f, u * 0.012f), CornerRadius(u * 0.002f))
            drawRoundRect(Color(0xFFB5523B), top + Offset(-u * 0.012f, -u * 0.019f), Size(u * 0.01f, u * 0.012f), CornerRadius(u * 0.002f))
            drawRoundRect(Color(0xFF8A9096), top + Offset(u * 0.015f, -u * 0.03f), Size(u * 0.01f, u * 0.024f), CornerRadius(u * 0.003f))
        }
        "tent" -> {
            // A ridge tent at a slight angle: the sunlit side, the shaded end, an open door flap,
            // a ridge pole, and guy ropes pegged out.
            val tw = u * 0.15f
            val th = u * 0.11f
            val apex = p + Offset(-tw * 0.08f, -th)
            val back = p + Offset(tw * 0.38f, -th * 0.96f)
            for ((a, b) in listOf(apex to p + Offset(-tw * 0.62f, u * 0.004f), back to p + Offset(tw * 0.72f, u * 0.002f)))
                drawLine(Color(0xFFD8C7A6), a, b, strokeWidth = u * 0.0015f)
            for (x in listOf(-tw * 0.62f, tw * 0.72f)) drawLine(BarkDark, p + Offset(x, u * 0.006f), p + Offset(x, -u * 0.004f), strokeWidth = u * 0.003f)
            drawPath(Path().apply { moveTo(p.x - tw / 2, p.y); lineTo(apex.x, apex.y); lineTo(back.x, back.y); lineTo(p.x + tw / 2 + tw * 0.08f, p.y); close() },
                Brush.horizontalGradient(listOf(Olive, OliveDark), startX = p.x - tw / 2, endX = p.x + tw * 0.6f))
            drawPath(Path().apply { moveTo(p.x - tw / 2, p.y); lineTo(apex.x, apex.y); lineTo(p.x + tw * 0.34f, p.y); close() }, Olive)
            // The doorway, dark inside, with its flap tied back.
            drawPath(Path().apply { moveTo(apex.x, apex.y + th * 0.08f); lineTo(p.x + tw * 0.06f, p.y); lineTo(p.x - tw * 0.22f, p.y); close() }, Color(0xFF2E2A1E))
            drawPath(Path().apply { moveTo(apex.x, apex.y + th * 0.08f); lineTo(p.x - tw * 0.22f, p.y); lineTo(p.x - tw * 0.32f, p.y); close() }, Color(0xFF969B67))
            drawLine(Color(0xFFD8C7A6), p + Offset(-tw * 0.29f, -th * 0.4f), p + Offset(-tw * 0.24f, -th * 0.4f), strokeWidth = u * 0.002f)
            drawLine(WoodLight, apex, back, strokeWidth = u * 0.0025f)
            drawLine(Wood, apex, p + Offset(-tw * 0.08f, 0f), strokeWidth = u * 0.003f)
            lights += Light(p + Offset(-tw * 0.08f, -th * 0.3f), u * 0.04f, 0.4f)
        }
        "bell_tent" -> {
            val tw = u * 0.18f
            val th = u * 0.15f
            drawPath(Path().apply { moveTo(p.x - tw / 2, p.y); lineTo(p.x - tw * 0.42f, p.y - th * 0.35f); lineTo(p.x, p.y - th); lineTo(p.x + tw * 0.42f, p.y - th * 0.35f); lineTo(p.x + tw / 2, p.y); close() }, CanvasCloth)
            drawPath(Path().apply { moveTo(p.x, p.y - th); lineTo(p.x + tw * 0.42f, p.y - th * 0.35f); lineTo(p.x + tw / 2, p.y); lineTo(p.x + tw * 0.15f, p.y); close() }, CanvasShade)
            drawPath(Path().apply { moveTo(p.x - tw * 0.12f, p.y); lineTo(p.x, p.y - th * 0.62f); lineTo(p.x + tw * 0.12f, p.y); close() }, Color(0xFF6E4E2E))
            drawPath(Path().apply { moveTo(p.x - tw * 0.06f, p.y); lineTo(p.x, p.y - th * 0.45f); lineTo(p.x + tw * 0.06f, p.y); close() }, Glow.copy(alpha = 0.7f))
            drawLine(Wood, p + Offset(0f, -th), p + Offset(0f, -th - u * 0.02f), strokeWidth = u * 0.004f)
            drawLine(Color(0xFF8A7F68), p + Offset(tw * 0.42f, -th * 0.35f), p + Offset(tw * 0.62f, 0f), strokeWidth = u * 0.0015f)
            lights += Light(p + Offset(0f, -th * 0.2f), u * 0.06f, 0.6f)
        }
        "cabin" -> {
            val cw = u * 0.22f
            val ch = u * 0.14f
            val l = p.x - cw / 2
            drawRect(Wood, Offset(l, p.y - ch), Size(cw, ch))
            var y = p.y - ch + ch / 7
            while (y < p.y - 1) { drawLine(Bark.copy(alpha = 0.6f), Offset(l, y), Offset(l + cw, y), strokeWidth = u * 0.0025f); y += ch / 7 }
            for (k in 0 until 7) { drawCircle(WoodPale, ch / 15, Offset(l, p.y - ch + ch / 14 + k * ch / 7)); drawCircle(WoodPale, ch / 15, Offset(l + cw, p.y - ch + ch / 14 + k * ch / 7)) }
            drawPath(Path().apply { moveTo(l - cw * 0.1f, p.y - ch + u * 0.004f); lineTo(p.x, p.y - ch - u * 0.085f); lineTo(l + cw * 1.1f, p.y - ch + u * 0.004f); close() }, Color(0xFF3A4A3E))
            drawPath(Path().apply { moveTo(p.x, p.y - ch - u * 0.085f); lineTo(l + cw * 1.1f, p.y - ch + u * 0.004f); lineTo(p.x + cw * 0.2f, p.y - ch + u * 0.004f); close() }, Color(0xFF2E3B31))
            drawRect(StoneDark, Offset(l + cw * 0.72f, p.y - ch - u * 0.09f), Size(cw * 0.08f, u * 0.05f))
            drawRect(BarkDark, Offset(l + cw * 0.58f, p.y - ch * 0.66f), Size(cw * 0.18f, ch * 0.66f))
            val win = Offset(l + cw * 0.14f, p.y - ch * 0.72f)
            drawRect(Glow.copy(alpha = 0.9f), win, Size(cw * 0.26f, ch * 0.34f))
            drawLine(BarkDark, win + Offset(cw * 0.13f, 0f), win + Offset(cw * 0.13f, ch * 0.34f), strokeWidth = u * 0.003f)
            drawRect(BarkDark, win, Size(cw * 0.26f, ch * 0.34f), style = Stroke(u * 0.003f))
            lights += Light(win + Offset(cw * 0.13f, ch * 0.17f), u * 0.08f, 0.8f)
        }
        "swing" -> {
            // Hangs from the branch of the tree beside it (see the scene: 0.38 of a 0.42-high tree at 0.6).
            val top = p.y - (p.y - (u * 0.6f - u * 0.42f * 0.38f)).coerceAtLeast(u * 0.12f)
            drawLine(Color(0xFFD8C7A6), Offset(p.x - u * 0.026f, top), p + Offset(-u * 0.026f, -u * 0.04f), strokeWidth = u * 0.0025f)
            drawLine(Color(0xFFD8C7A6), Offset(p.x + u * 0.014f, top), p + Offset(u * 0.014f, -u * 0.04f), strokeWidth = u * 0.0025f)
            drawRoundRect(Wood, p + Offset(-u * 0.034f, -u * 0.044f), Size(u * 0.056f, u * 0.009f), CornerRadius(u * 0.003f))
        }
        "hammock" -> {
            val a = p + Offset(-u * 0.065f, -u * 0.085f) // the birch
            val b = p + Offset(u * 0.07f, -u * 0.09f) // the oak
            drawPath(Path().apply {
                moveTo(a.x, a.y); quadraticTo(p.x, p.y + u * 0.01f, b.x, b.y)
                quadraticTo(p.x, p.y - u * 0.035f, a.x, a.y); close()
            }, Color(0xFFE7DCC4))
            for (k in 1..3) drawPath(Path().apply { moveTo(a.x, a.y); quadraticTo(p.x, p.y + u * 0.01f - k * u * 0.011f, b.x, b.y) }, Color(0xFFB5523B).copy(alpha = 0.7f), style = Stroke(u * 0.003f))
        }
        "treehouse" -> {
            // Built in the tree beside it, under the crown (like the swing's branch), with a rope
            // ladder down to the ground.
            val floorY = p.y - (p.y - (u * 0.6f - u * 0.42f * 0.4f)).coerceAtLeast(u * 0.14f)
            val c = Offset(p.x + u * 0.018f, floorY)
            for (dx in listOf(-0.005f, 0.005f)) drawLine(Color(0xFFD8C7A6), Offset(p.x - u * 0.03f + u * dx * 2, floorY), p + Offset(-u * 0.03f + u * dx * 2, 0f), strokeWidth = u * 0.0018f)
            var y = floorY + u * 0.02f
            while (y < p.y - u * 0.008f) { drawLine(Wood, Offset(p.x - u * 0.04f, y), Offset(p.x - u * 0.02f, y), strokeWidth = u * 0.003f); y += u * 0.024f }
            drawLine(Bark, c + Offset(-u * 0.05f, u * 0.004f), c + Offset(-u * 0.02f, u * 0.035f), strokeWidth = u * 0.004f) // braces
            drawLine(Bark, c + Offset(u * 0.05f, u * 0.004f), c + Offset(u * 0.02f, u * 0.035f), strokeWidth = u * 0.004f)
            drawRect(Wood, Offset(c.x - u * 0.06f, c.y - u * 0.004f), Size(u * 0.12f, u * 0.01f))
            drawRect(WoodLight, Offset(c.x - u * 0.06f, c.y - u * 0.004f), Size(u * 0.12f, u * 0.003f))
            for (k in 0..6) drawLine(WoodLight, Offset(c.x - u * 0.058f + k * u * 0.019f, c.y - u * 0.004f), Offset(c.x - u * 0.058f + k * u * 0.019f, c.y - u * 0.02f), strokeWidth = u * 0.002f) // railing
            drawLine(WoodLight, Offset(c.x - u * 0.06f, c.y - u * 0.02f), Offset(c.x + u * 0.06f, c.y - u * 0.02f), strokeWidth = u * 0.0025f)
            drawRect(Brush.horizontalGradient(listOf(WoodLight, Wood), startX = c.x - u * 0.035f, endX = c.x + u * 0.035f), Offset(c.x - u * 0.035f, c.y - u * 0.062f), Size(u * 0.07f, u * 0.058f))
            for (k in 1..4) drawLine(Wood.copy(alpha = 0.6f), Offset(c.x - u * 0.035f, c.y - u * 0.062f + k * u * 0.012f), Offset(c.x + u * 0.035f, c.y - u * 0.062f + k * u * 0.012f), strokeWidth = u * 0.0008f)
            drawPath(Path().apply { moveTo(c.x - u * 0.046f, c.y - u * 0.06f); lineTo(c.x, c.y - u * 0.1f); lineTo(c.x + u * 0.046f, c.y - u * 0.06f); close() }, Color(0xFF8E3B26))
            drawRoundRect(Glow.copy(alpha = 0.9f), Offset(c.x - u * 0.012f, c.y - u * 0.05f), Size(u * 0.024f, u * 0.022f), CornerRadius(u * 0.004f))
            drawLine(Wood, c + Offset(0f, -u * 0.05f), c + Offset(0f, -u * 0.028f), strokeWidth = u * 0.0018f)
            lights += Light(c + Offset(0f, -u * 0.04f), u * 0.06f, 0.7f)
        }
        "canoe" -> {
            drawPath(Path().apply {
                moveTo(p.x - u * 0.1f, p.y - u * 0.04f)
                quadraticTo(p.x - u * 0.06f, p.y + u * 0.004f, p.x, p.y + u * 0.004f)
                quadraticTo(p.x + u * 0.06f, p.y + u * 0.004f, p.x + u * 0.1f, p.y - u * 0.04f)
                quadraticTo(p.x, p.y - u * 0.022f, p.x - u * 0.1f, p.y - u * 0.04f); close()
            }, Color(0xFF8E5A36))
            drawPath(Path().apply { moveTo(p.x - u * 0.092f, p.y - u * 0.036f); quadraticTo(p.x, p.y - u * 0.018f, p.x + u * 0.092f, p.y - u * 0.036f) }, WoodLight, style = Stroke(u * 0.004f))
            for (dx in listOf(-0.04f, 0f, 0.04f)) drawLine(WoodPale, p + Offset(u * dx, -u * 0.026f), p + Offset(u * dx, -u * 0.012f), strokeWidth = u * 0.003f)
        }
        "dock" -> {
            // A plank jetty from our bank out over the water: a deck in perspective, posts, a bollard.
            val land = p + Offset(u * 0.12f, -u * 0.012f)
            val tip = p + Offset(-u * 0.07f, u * 0.012f)
            val half = u * 0.014f
            for (t in listOf(0.15f, 0.5f, 0.85f)) {
                val q = Offset(land.x + (tip.x - land.x) * t, land.y + (tip.y - land.y) * t)
                for (side in listOf(-1f, 1f)) drawRect(BarkDark, Offset(q.x - u * 0.003f, q.y + side * half * 0.6f), Size(u * 0.006f, u * 0.03f))
            }
            val n = 12
            for (k in 0 until n) {
                val a = k / n.toFloat(); val b = (k + 0.85f) / n
                val qa = Offset(land.x + (tip.x - land.x) * a, land.y + (tip.y - land.y) * a)
                val qb = Offset(land.x + (tip.x - land.x) * b, land.y + (tip.y - land.y) * b)
                drawPath(Path().apply { moveTo(qa.x, qa.y - half); lineTo(qb.x, qb.y - half); lineTo(qb.x, qb.y + half); lineTo(qa.x, qa.y + half); close() }, if (k % 2 == 0) WoodLight else Wood)
            }
            drawLine(Wood.copy(alpha = 0.7f), Offset(land.x, land.y + half), Offset(tip.x, tip.y + half), strokeWidth = u * 0.003f)
            drawRoundRect(BarkDark, Offset(tip.x + u * 0.006f, tip.y - half - u * 0.012f), Size(u * 0.008f, u * 0.014f), CornerRadius(u * 0.003f))
            drawCircle(Color.White.copy(alpha = 0.35f), u * 0.012f, tip + Offset(0f, u * 0.022f), style = Stroke(u * 0.0015f))
        }
    }
}

private fun DrawScope.campfire(p: Offset, u: Float, lights: MutableList<Light>) {
    val c = p + Offset(0f, -u * 0.012f)
    drawLine(BarkDark, c + Offset(-u * 0.035f, u * 0.006f), c + Offset(u * 0.035f, -u * 0.01f), strokeWidth = u * 0.012f, cap = StrokeCap.Round)
    drawLine(Bark, c + Offset(-u * 0.035f, -u * 0.01f), c + Offset(u * 0.035f, u * 0.006f), strokeWidth = u * 0.012f, cap = StrokeCap.Round)
    val f = c + Offset(0f, -u * 0.006f)
    drawPath(Path().apply { moveTo(f.x - u * 0.022f, f.y); quadraticTo(f.x - u * 0.024f, f.y - u * 0.035f, f.x - u * 0.004f, f.y - u * 0.062f); quadraticTo(f.x + u * 0.004f, f.y - u * 0.04f, f.x + u * 0.01f, f.y - u * 0.05f); quadraticTo(f.x + u * 0.026f, f.y - u * 0.025f, f.x + u * 0.022f, f.y); close() }, Flame)
    drawPath(Path().apply { moveTo(f.x - u * 0.011f, f.y); quadraticTo(f.x - u * 0.01f, f.y - u * 0.022f, f.x, f.y - u * 0.036f); quadraticTo(f.x + u * 0.012f, f.y - u * 0.018f, f.x + u * 0.011f, f.y); close() }, FlameCore)
    // A wisp of smoke.
    drawPath(Path().apply { moveTo(f.x, f.y - u * 0.06f); cubicTo(f.x - u * 0.02f, f.y - u * 0.1f, f.x + u * 0.03f, f.y - u * 0.14f, f.x + u * 0.005f, f.y - u * 0.2f) }, Color.White.copy(alpha = 0.18f), style = Stroke(u * 0.012f, cap = StrokeCap.Round))
    lights += Light(f + Offset(0f, -u * 0.02f), u * 0.16f, 1f)
}

/** The clearing as a bitmap with rounded corners, for the widget and the share card. */
fun clearingBitmap(
    avatar: Avatar, placed: Map<Int, String>, worn: Set<Accessory>, widthPx: Int, heightPx: Int, night: Boolean = false, paper: Boolean = false, ducks: Int = 0, bridge: Boolean = false, visitor: app.tend.core.Visitor? = null,
    layout: Map<Int, Offset> = emptyMap(),
    guests: List<Pair<Avatar, String>> = emptyList(),
): android.graphics.Bitmap {
    val image = androidx.compose.ui.graphics.ImageBitmap(widthPx, heightPx)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    val size = Size(widthPx.toFloat(), heightPx.toFloat())
    canvas.clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset.Zero, size), CornerRadius(widthPx * 0.05f))) })
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, canvas, size,
    ) { drawClearing(avatar, placed, worn = worn, night = night, paper = if (paper) 1f else 0f, ducks = ducks, bridge = bridge, visitor = visitor, layout = layout, guests = guests) }
    return image.asAndroidBitmap()
}

/**
 * The 1x1 widget's badge for something from a friend: a white disc with a coral ring, holding the
 * camp's own rubber duck for a visit, or an emoji ([emoji]) for the rest.
 */
fun noteBadge(sizePx: Int, duck: Boolean, emoji: String): android.graphics.Bitmap {
    val image = androidx.compose.ui.graphics.ImageBitmap(sizePx, sizePx)
    val d = sizePx.toFloat()
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(image), Size(d, d),
    ) {
        val c = Offset(d / 2, d / 2)
        drawCircle(Color.Black.copy(alpha = 0.18f), d * 0.46f, c + Offset(0f, d * 0.03f))
        drawCircle(Color.White, d * 0.45f, c)
        drawCircle(Color(0xFFE07A5F), d * 0.43f, c, style = Stroke(d * 0.06f))
        if (duck) {
            val s = d * 0.26f
            rubberDuck(Offset(c.x + s * 0.12f, c.y + s * 0.62f), s / 0.045f)
        } else drawIntoCanvas {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = d * 0.5f; textAlign = android.graphics.Paint.Align.CENTER }
            val m = paint.fontMetrics
            it.nativeCanvas.drawText(emoji, c.x, c.y - (m.ascent + m.descent) / 2, paint)
        }
    }
    return image.asAndroidBitmap()
}

/** Rough size of each thing (width, height) in scene heights, to fit it into an icon. */
private fun thingSize(id: String): Pair<Float, Float> = when (id) {
    "jack_o_lantern" -> 0.05f to 0.05f
    "paper_lanterns" -> 0.06f to 0.095f
    "xmas_tree" -> 0.07f to 0.1f
    "flower_arch" -> 0.08f to 0.095f
    "scarecrow" -> 0.08f to 0.11f
    "snowman" -> 0.06f to 0.09f
    "birdhouse" -> 0.05f to 0.11f
    "deck_chair", "sled", "watermelon", "leaf_pile", "gift_boxes" -> 0.08f to 0.06f
    "cabin" -> 0.25f to 0.24f
    "bell_tent" -> 0.2f to 0.18f
    "tent" -> 0.17f to 0.12f
    "treehouse" -> 0.14f to 0.32f
    "swing" -> 0.07f to 0.13f
    "hammock" -> 0.15f to 0.1f
    "canoe" -> 0.21f to 0.05f
    "dock" -> 0.2f to 0.07f
    "kettle" -> 0.11f to 0.13f
    "rocking_chair" -> 0.07f to 0.08f
    "raft" -> 0.18f to 0.05f
    "campfire" -> 0.08f to 0.09f
    "pizza_oven" -> 0.11f to 0.13f
    "picnic_table" -> 0.13f to 0.08f
    "log_bench" -> 0.12f to 0.035f
    "guitar" -> 0.04f to 0.1f
    "fishing_rod" -> 0.15f to 0.1f
    "telescope" -> 0.09f to 0.1f
    else -> 0.06f to 0.05f
}

/** One thing as an icon, filling this canvas, standing on its bottom. */
fun DrawScope.drawThingIcon(id: String) {
    val s = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    if (id == "wind_chimes" || id == "bird_feeder") {
        // Hanging from a short bit of line at the top.
        drawLine(BarkDark, Offset(c.x - s * 0.3f, c.y - s * 0.42f), Offset(c.x + s * 0.3f, c.y - s * 0.42f), strokeWidth = s * 0.02f)
        drawThing(id, Offset(c.x, c.y - s * 0.42f), s * 9f)
        return
    }
    if (id == "string_lights") {
        // As an icon: a drooping wire with warm bulbs on dark sockets, so it reads on a light card.
        val a = Offset(c.x - s * 0.45f, c.y - s * 0.2f)
        val b = Offset(c.x + s * 0.45f, c.y - s * 0.2f)
        drawPath(Path().apply { moveTo(a.x, a.y); quadraticTo(c.x, c.y + s * 0.2f, b.x, b.y) }, BarkDark, style = Stroke(s * 0.02f))
        for (i in 0 until 7) {
            val t = (i + 0.5f) / 7f
            val p = Offset(a.x + (b.x - a.x) * t, a.y + s * 0.4f * 4 * t * (1 - t) * 0.5f)
            drawRect(BarkDark, Offset(p.x - s * 0.018f, p.y), Size(s * 0.036f, s * 0.03f))
            drawCircle(Glow.copy(alpha = 0.45f), s * 0.07f, p + Offset(0f, s * 0.07f))
            drawCircle(Color(0xFFFFE7A3), s * 0.04f, p + Offset(0f, s * 0.07f))
        }
        return
    }
    val (tw, th) = thingSize(id)
    val u = minOf(s * 0.85f / tw, s * 0.8f / th)
    // Some things reach out to one side of where they stand: centre what's drawn.
    val shift = when (id) { "dock" -> -0.03f; "fishing_rod" -> 0.006f; else -> 0f }
    drawThing(id, Offset(c.x + u * shift, c.y + s * if (id == "dock") 0.15f else 0.4f), u)
}

/** A small songbird perched (or, with [flying], mid-flap), facing left. */
fun DrawScope.bird(at: Offset, u: Float, color: Color, flying: Boolean = false, flap: Float = 0f) {
    drawOval(color, Offset(at.x - u * 0.006f, at.y - u * 0.007f), Size(u * 0.012f, u * 0.008f))
    drawCircle(color, u * 0.0035f, at + Offset(-u * 0.005f, -u * 0.007f))
    drawOval(Color(0xFFF2E3C6), Offset(at.x - u * 0.005f, at.y - u * 0.004f), Size(u * 0.006f, u * 0.004f)) // breast
    drawPath(Path().apply { moveTo(at.x - u * 0.008f, at.y - u * 0.0075f); lineTo(at.x - u * 0.012f, at.y - u * 0.0065f); lineTo(at.x - u * 0.008f, at.y - u * 0.0055f); close() }, Color(0xFFE0A93E))
    drawCircle(Color(0xFF1D2A23), u * 0.0009f, at + Offset(-u * 0.006f, -u * 0.008f))
    drawPath(Path().apply { moveTo(at.x + u * 0.005f, at.y - u * 0.004f); lineTo(at.x + u * 0.011f, at.y - u * 0.007f); lineTo(at.x + u * 0.01f, at.y - u * 0.002f); close() }, color) // tail
    val wingY = if (flying) -u * 0.008f * (0.5f + flap) else -u * 0.002f
    drawPath(Path().apply { moveTo(at.x - u * 0.002f, at.y - u * 0.005f); quadraticTo(at.x + u * 0.004f, at.y - u * 0.005f + wingY, at.x + u * 0.007f, at.y - u * 0.004f); close() }, androidx.compose.ui.graphics.lerp(color, Color.Black, 0.25f), style = Stroke(u * 0.002f, cap = StrokeCap.Round))
}

/** A raft of tied logs floating at [p], with a small pole and pennant. */
fun DrawScope.raftAt(p: Offset, u: Float) {
    drawOval(Color.White.copy(alpha = 0.3f), Offset(p.x - u * 0.085f, p.y - u * 0.004f), Size(u * 0.17f, u * 0.012f))
    for (k in 0 until 5) {
        val y = p.y - u * 0.012f - k * u * 0.0055f
        val inset = k * u * 0.004f
        drawRoundRect(if (k % 2 == 0) Wood else WoodLight, Offset(p.x - u * 0.075f + inset, y - u * 0.004f), Size(u * 0.15f - inset * 2, u * 0.008f), CornerRadius(u * 0.004f))
    }
    for (dx in listOf(-0.05f, 0.05f)) drawLine(Color(0xFFD8C7A6), p + Offset(u * dx, -u * 0.04f), p + Offset(u * dx, -u * 0.012f), strokeWidth = u * 0.0025f)
    drawLine(BarkDark, p + Offset(u * 0.045f, -u * 0.035f), p + Offset(u * 0.05f, -u * 0.1f), strokeWidth = u * 0.003f, cap = StrokeCap.Round)
    drawPath(Path().apply { moveTo(p.x + u * 0.05f, p.y - u * 0.1f); lineTo(p.x + u * 0.075f, p.y - u * 0.092f); lineTo(p.x + u * 0.049f, p.y - u * 0.084f); close() }, Color(0xFFE07A5F))
}
