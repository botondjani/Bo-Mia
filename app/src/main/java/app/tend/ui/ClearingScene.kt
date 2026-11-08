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
private val SkyTop = Color(0xFFB9D3DF)
private val SkyLow = Color(0xFFF4E3BC)
private val Haze1 = Color(0xFFA7BFA9)
private val Haze2 = Color(0xFF7FA184)
private val PineDark = Color(0xFF26432F)
private val PineMid = Color(0xFF335A3E)
private val PineLit = Color(0xFF4F7A48)
private val LeafDark = Color(0xFF2E5236)
private val LeafMid = Color(0xFF41704A)
private val LeafLit = Color(0xFF7C9E55)
private val Meadow = Color(0xFF86A85E)
private val MeadowDeep = Color(0xFF5F8547)
private val Dirt = Color(0xFFA88C66)
private val RiverFar = Color(0xFF9DB7B4)
private val RiverNear = Color(0xFF557A7E)
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
) {
    val w = size.width
    val h = size.height
    val u = h // things scale with the scene's height
    val horizon = h * 0.5f
    val lights = mutableListOf<Light>()
    fun at(id: Int) = spotPos(id, layout).let { Offset(it.x * w, it.y * h) }

    // Sky, with the low sun's warmth near the horizon.
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyLow), endY = horizon), size = Size(w, horizon + 2))
    drawCircle(Brush.radialGradient(listOf(Color(0x66FFF1C9), Color.Transparent), center = Offset(w * 0.2f, h * 0.18f), radius = h * 0.55f), h * 0.55f, Offset(w * 0.2f, h * 0.18f))

    // Snow-capped mountains far behind, pale with distance.
    mountains(horizon)

    // Two hazy forest walls far away: lighter and bluer the farther they are.
    forestWall(horizon + h * 0.02f, h * 0.2f, Haze1, seed = 3)
    forestWall(horizon + h * 0.06f, h * 0.26f, Haze2, seed = 7)

    // The meadow, warm where the light falls; its far edge rolls gently into the forest.
    drawPath(Path().apply {
        moveTo(0f, horizon + h * 0.02f)
        cubicTo(w * 0.3f, horizon - h * 0.005f, w * 0.55f, horizon + h * 0.035f, w * 0.78f, horizon + h * 0.01f)
        cubicTo(w * 0.88f, horizon, w * 0.95f, horizon + h * 0.02f, w, horizon + h * 0.012f)
        lineTo(w, h); lineTo(0f, h); close()
    }, Brush.verticalGradient(listOf(Color(0xFF9DB874), Meadow, MeadowDeep), startY = horizon, endY = h))
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

    // Everything standing in the clearing, back to front, the companion among them (or using something).
    // Walking over first (from where they stay), then using it there.
    // At night they go to bed: walk to the shelter (if there is one) and sleep inside.
    val bed = if (night && use == null) bedOf(placed) else null
    val act = use ?: bed?.let { InUse(it, placed.getValue(it), nightClock, if (rest == it) 0f else Uses.WALK) }
    // Their place, and (just after a thing moved) where it was a moment ago.
    val restTo = restPoint(rest, placed, layout)
    val restWas = restFrom?.let { restPoint(rest, placed, it) }?.takeIf { (it - restTo).getDistance() > u * 0.01f }
    val strolling = act == null && restWas != null && restClock in 0.001f..Uses.WALK
    val restAt = when {
        restWas == null || restClock >= Uses.WALK -> restTo
        restClock <= 0f -> restWas
        else -> { val k = (restClock / Uses.WALK).let { it * it * (3 - 2 * it) }; restWas + (restTo - restWas) * k }
    }
    val target = act?.let { useAnchor(it.arrived(), placed, restAt, layout) }
    val walking = act?.walking == true
    val anchor = when {
        target == null -> restAt
        walking -> {
            val k = (act!!.t / act.walk).let { it * it * (3 - 2 * it) }
            Offset(restAt.x + (target.x - restAt.x) * k, restAt.y + (target.y - restAt.y) * k)
        }
        else -> target
    }
    var companionDrawn = false
    val cs = u * 0.17f * depthScale(anchor.y / h)
    fun drawActor(glows: MutableList<Light>) {
        if (act != null && walking) { drawWalker(avatar, mood, worn, anchor, act.t, target!!.x >= restAt.x); return }
        if (strolling) { drawWalker(avatar, mood, worn, anchor, restClock, restTo.x >= restWas!!.x); return }
        if (act != null) { drawUse(act.arrived(), avatar, mood, worn, placed, anchor, glows, lastStrum, layout); return }
        shadow(anchor, cs * 0.5f, u)
        drawIntoCanvas {
            it.nativeCanvas.save()
            it.nativeCanvas.translate(anchor.x - cs / 2, anchor.y - cs * 0.95f)
            // At night with no shelter: asleep where they stand.
            CharacterPainter.draw(it.nativeCanvas, avatar, if (night) Mood.SLEEPING else mood, cs, accessories = worn)
            it.nativeCanvas.restore()
        }
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
    for (spot in Clearing.spots) {
        if (spot.kind == SpotKind.ABOVE) continue
        val p = at(spot.id)
        ground += p.y to {
            if (spot.kind == SpotKind.FIRE) firePitStones(p, u)
            val thing = placed[spot.id]
            if (thing != null && spot.id != held && !(act?.spot == spot.id && !walking && thing in Uses.redrawn)) {
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

    // Night: a blue veil over everything, then the lights.
    if (night) {
        drawRect(NightVeil.copy(alpha = 0.6f), size = size)
        for ((sx, sy) in listOf(0.08f to 0.08f, 0.22f to 0.15f, 0.35f to 0.05f, 0.52f to 0.12f, 0.7f to 0.06f, 0.86f to 0.14f, 0.95f to 0.04f))
            drawCircle(Color.White.copy(alpha = 0.8f), h * 0.004f, Offset(w * sx, h * sy))
        drawCircle(Color(0xFFF3EBC8), h * 0.035f, Offset(w * 0.16f, h * 0.12f))
        // The companion stays readable: drawn again over the veil, cooled down to match the night.
        drawIntoCanvas {
            val paint = android.graphics.Paint().apply { colorFilter = android.graphics.LightingColorFilter(0xFF9DA6C4.toInt(), 0x101410) }
            it.nativeCanvas.saveLayer(null, paint)
        }
        drawActor(mutableListOf())
        drawIntoCanvas { it.nativeCanvas.restore() }
        lights.forEach { l ->
            drawCircle(Brush.radialGradient(listOf(Glow.copy(alpha = 0.55f * l.strength), Color.Transparent), center = l.at, radius = l.radius), l.radius, l.at)
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
        drawLine(if (rnd.nextBoolean()) MeadowDeep else Color(0xFF4D7139), Offset(x, y), Offset(x + lean, y - len), strokeWidth = h * 0.004f, cap = StrokeCap.Round)
    }
    repeat(14) {
        val c = Offset(w * (0.33f + 0.66f * rnd.nextFloat()), h * (0.94f + 0.05f * rnd.nextFloat()))
        drawCircle(listOf(Color.White, Color(0xFFC9A6E3), Color(0xFFF2D27A))[it % 3], h * 0.005f, c)
    }
}

/** A soft contact shadow, falling a little to the right (the sun is low on the left). */
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
private fun thingWidth(id: String): Float = when (id) {
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
): android.graphics.Bitmap {
    val image = androidx.compose.ui.graphics.ImageBitmap(widthPx, heightPx)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    val size = Size(widthPx.toFloat(), heightPx.toFloat())
    canvas.clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset.Zero, size), CornerRadius(widthPx * 0.05f))) })
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, canvas, size,
    ) { drawClearing(avatar, placed, worn = worn, night = night, paper = if (paper) 1f else 0f, ducks = ducks, bridge = bridge, visitor = visitor, layout = layout) }
    return image.asAndroidBitmap()
}

/** Rough size of each thing (width, height) in scene heights, to fit it into an icon. */
private fun thingSize(id: String): Pair<Float, Float> = when (id) {
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
