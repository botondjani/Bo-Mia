package app.tend.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import app.tend.core.Accessory
import app.tend.core.Avatar
import app.tend.core.Mood
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * The companion using a thing in the camp: which spot, which thing, and [t] seconds since it started
 * ([walk] of them getting there). With [end] (seconds of use, once there) known, the last moments
 * are stepping off to where they'll stay.
 */
data class InUse(val spot: Int, val thing: String, val t: Float, val walk: Float = 0f, val end: Float = 0f, val reward: Boolean = false) {
    /** Walking over to the thing: the first [walk] seconds. */
    val walking: Boolean get() = t < walk
    /** The use itself, its clock starting once there. */
    fun arrived() = copy(t = (t - walk).coerceAtLeast(0f), walk = 0f)
}

/** What the companion does with each thing: a line to show, and how long it lasts. */
object Uses {
    fun line(thing: String, name: String): String = when (thing) {
        "guitar" -> "$name plays the guitar. Tap again for another chord."
        "campfire" -> "$name roasts a marshmallow."
        "kettle" -> "$name makes a cup of tea."
        "pizza_oven" -> "$name bakes a pizza."
        "woodpile" -> "$name feeds the fire."
        "wildflowers" -> "$name smells the flowers."
        "lantern" -> "$name lights the lantern."
        "fishing_rod" -> "$name goes fishing."
        "telescope" -> "$name looks at the sky."
        "log_bench" -> "$name sits by the fire and hums."
        "picnic_table" -> "$name has a sandwich."
        "tent", "bell_tent", "cabin" -> "$name takes a nap."
        "swing" -> "$name swings."
        "hammock" -> "$name naps in the hammock."
        "treehouse" -> "$name waves from the treehouse."
        "canoe" -> "$name paddles the canoe."
        "dock" -> "$name dangles their feet off the dock."
        "string_lights" -> "$name dances under the lights."
        "rocking_chair" -> "$name rocks in the chair."
        "raft" -> "$name drifts on the raft."
        "wind_chimes" -> "$name listens to the chimes."
        "bird_feeder" -> "$name watches the birds at the feeder."
        "tulips" -> "$name smells the tulips."
        "watering_can" -> "$name waters the flowers."
        "birdhouse" -> "$name watches a little bird move in."
        "beach_ball" -> "$name plays with the beach ball."
        "watermelon" -> "$name has a slice of watermelon."
        "deck_chair" -> "$name soaks up the sun."
        "leaf_pile" -> "$name jumps into the leaves."
        "pumpkins" -> "$name carves a smile into a pumpkin."
        "scarecrow" -> "$name says hello to the scarecrow."
        "gift_boxes" -> "$name peeks into a present."
        "sled" -> "$name sits on the sled and dreams of hills."
        "snowman" -> "$name gives the snowman a hug."
        else -> "$name looks around."
    }

    /** How the companion acts out an activity done in real life, in the camp. */
    fun reactionFor(taskId: String): String = when (taskId) {
        "read", "learn" -> "act_read"
        "move" -> "act_run"
        "outside" -> "act_walk"
        "sleep" -> "act_stretch"
        "friends" -> "act_hearts"
        "hobby" -> "guitar"
        "chores" -> "act_tidy"
        "cook" -> "campfire"
        else -> "act_cheer"
    }

    /** Seconds to walk over to a thing before using it. */
    const val WALK = 1.2f

    /** Stepping off something at the end of a use (out of the hammock, off the swing). */
    const val STEP = 0.7f

    /** Picking a thing up where it lies (half a second), then carrying it over (a second). */
    const val FETCH = 1.5f

    fun seconds(thing: String): Float = when (thing) {
        "act_read", "act_run", "act_walk", "act_stretch", "act_hearts", "act_tidy", "act_cheer" -> 8f
        "guitar" -> 12f
        "canoe", "raft", "hammock", "tent", "bell_tent", "cabin" -> 9f
        // Fetched first (the log, the rod), then used where it's used.
        "fishing_rod" -> 9f + FETCH
        "woodpile" -> 7f + FETCH
        else -> 7f
    }

    /** Things drawn differently while used (they move, or the companion holds them). */
    val redrawn = setOf("guitar", "fishing_rod", "swing", "canoe", "raft", "rocking_chair")
}

/**
 * Where the companion really is, shared by home and the camp page (scene fractions): whenever what
 * they were doing ends (done, or something else took over part way), they stay just where they
 * were and go on from there. Never a jump back to some usual place.
 */
object Whereabouts {
    /** Where they were drawn last. */
    var drawn: Offset? = null
    /** Where they stand now; null: nothing happened yet (their usual place). */
    var standing: Offset? = null
    private var lastAct: String? = null

    /** The umbrella: 0 put away, 0.5 out but closed, 1 open. */
    var umbrella = 0f
        private set
    private var umbrellaClock = 0f

    /**
     * Each frame: whether they'd like the umbrella up now ([clock]: seconds of camp life). It comes
     * out from behind the back and opens over a second or so, and folds and goes back the same way.
     */
    internal fun umbrellaTo(want: Boolean, clock: Float): Float {
        val dt = (clock - umbrellaClock).coerceIn(0f, 0.1f)
        umbrellaClock = clock
        umbrella = if (clock <= 0f) (if (want) 1f else 0f) else (umbrella + (if (want) dt else -dt) / 1.1f).coerceIn(0f, 1f)
        return umbrella
    }

    /** Each frame, with what they're doing (null: nothing). */
    internal fun frame(act: String?) {
        if (act == lastAct) return
        if (lastAct != null) standing = drawn ?: standing
        lastAct = act
    }
}

/**
 * The companion moving a thing: [spot] from [from] to [to] (scene fractions), [t] seconds in: [walk]
 * getting to it, picked up (or taken hold of, if it's big), [move] carrying it over, put down; then,
 * with nothing else to move ([admire]), a little walk round it to see how it looks.
 */
data class Carry(val spot: Int, val from: Offset, val to: Offset, val t: Float, val walk: Float, val move: Float, val admire: Boolean = false) {
    companion object {
        const val PICK = 0.5f
        const val DOWN = 0.4f
        /** Round it (to the other side, past its front), then a look. */
        const val ROUND = 2.6f
        const val LOOK = 1.2f
        /** With more to move: just a breath before the next one. */
        const val NEXT = 0.3f
        fun length(walk: Float, move: Float, admire: Boolean) = walk + PICK + move + DOWN + if (admire) ROUND + LOOK else NEXT
    }
}

/**
 * On a rainy day the rain comes in showers: about 45 seconds of it, a dry spell of a quarter of a
 * minute, and again ([t]: seconds of camp life). How hard it rains now, 0..1 (fading in and out).
 */
fun showerOf(t: Float): Float {
    val p = t % 75f
    return when {
        p < 42f -> 1f
        p < 46f -> 1f - (p - 42f) / 4f
        p < 60f -> 0f
        p < 64f -> (p - 60f) / 4f
        else -> 1f
    }
}

/** Bending down (to pick something up, or put it down) and back up, [k] 0..1; at 0, just standing. */
fun DrawScope.drawCrouch(avatar: Avatar, mood: Mood, worn: Set<Accessory>, at: Offset, k: Float, look: Float) {
    val s = size.height * 0.17f * depthScale(at.y / size.height)
    shadowAt(at, s * 0.45f)
    val d = sin(k.coerceIn(0f, 1f) * PI.toFloat())
    actor(avatar, mood, worn, at + Offset(0f, d * s * 0.05f), s, look = look, bounce = -d)
}

private val Wood = Color(0xFF9C6B45)
private val WoodLight = Color(0xFFC99766)
private val BarkDark = Color(0xFF452F21)
private val Rope = Color(0xFFD8C7A6)
private val Ink = Color(0xFF1D2A23)
private val Marshmallow = Color(0xFFFBF6EC)
private val Toasted = Color(0xFFC98A4B)
private val GlowWarm = Color(0xFFFFD27A)

/** How big the companion is at [y] (a fraction of the scene): smaller farther back. */
internal fun depthScale(y: Float) = (0.55f + (y - 0.6f) * 1.6f).coerceIn(0.5f, 1f)

/** Things used standing (or sitting at the fire): afterwards they stay right there. */
private val STANDING = setOf(
    "guitar", "campfire", "kettle", "pizza_oven", "woodpile", "wildflowers", "lantern", "telescope", "fishing_rod", "wind_chimes", "bird_feeder",
    "tulips", "watering_can", "birdhouse", "beach_ball", "watermelon", "leaf_pile", "pumpkins", "scarecrow", "gift_boxes", "sled", "snowman",
)

/**
 * Where the companion stays after using the thing at [spot]: standing beside it, on the shore
 * for the river, at the land end of the dock. Null spot, or nothing there: the usual place.
 */
fun DrawScope.restPoint(spot: Int?, placed: Map<Int, String>, layout: Map<Int, Offset> = emptyMap()): Offset {
    val w = size.width
    val h = size.height
    val u = h
    val home = Offset(w * 0.395f, h * 0.86f)
    wanderAt(spot)?.let { return Offset(it.x * w, it.y * h) }
    if (spot == null || placed[spot] == null) return home
    val p = spotPos(spot, layout).let { Offset(it.x * w, it.y * h) }
    // After something used standing up (playing, cooking, fishing...), they stay just where they were.
    val thing = placed.getValue(spot)
    if (thing in STANDING) return useAnchor(InUse(spot, thing, 100f), placed, layout = layout) // where the use ends
    return when (spot) {
        7 -> Offset(w * 0.33f, h * 0.8f) // the river: on our bank
        8 -> p + Offset(u * 0.13f, -u * 0.01f) // the dock: where it meets the shore
        9 -> home // the lights overhead
        else -> p + Offset(-u * spotScale(spot, layout) * (if (spotPos(spot, layout).y < 0.7f) 0.07f else 0.09f), u * 0.012f)
    }
}

/** The companion walking across the camp: bobbing, leaning into each step, facing the way they go. */
fun DrawScope.drawWalker(avatar: Avatar, mood: Mood, worn: Set<Accessory>, at: Offset, t: Float, toRight: Boolean) {
    val s = size.height * 0.17f * depthScale(at.y / size.height)
    shadowAt(at, s * 0.42f)
    val step = kotlin.math.abs(sin(t * 11f))
    actor(avatar, mood, worn, at + Offset(0f, -step * s * 0.07f), s, rotation = if (toRight) 5f else -5f, look = if (toRight) 1f else -1f, bounce = step * 0.5f)
}

/** Where the companion stands for [use], as a fraction-free point in the scene. */
fun DrawScope.useAnchor(use: InUse, placed: Map<Int, String>, here: Offset? = null, layout: Map<Int, Offset> = emptyMap()): Offset {
    val w = size.width
    val h = size.height
    val u = h
    // Activities from real life are acted out where the companion is standing.
    val home = here ?: Offset(w * 0.395f, h * 0.86f)
    val p = SpotPositions[use.spot]?.let { spotPos(use.spot, layout) }?.let { Offset(it.x * w, it.y * h) } ?: home
    val k = if (use.spot >= 0) spotScale(use.spot, layout) else 1f
    val fire = SpotPositions.getValue(0).let { Offset(it.x * w, it.y * h) }
    val seat = spotPos(1, layout).let { Offset(it.x * w, it.y * h) }
    // The bench only counts as a seat by the fire while it's near the fire.
    val bench = placed[1] != null && (seat - fire).getDistance() < u * 0.3f
    wanderAt(use.spot)?.let { return Offset(it.x * w, it.y * h) }
    return when (use.thing) {
        // Activities done in real life, acted out where the companion stands.
        "act_run" -> home + Offset(sin(use.t * 0.9f) * u * 0.14f, 0f)
        "act_walk" -> home + Offset(sin(use.t * 0.4f) * u * 0.06f, (kotlin.math.cos(use.t * 0.4f) - 1f) * u * 0.03f)
        "act_read", "act_stretch", "act_hearts", "act_tidy", "act_cheer" -> home
        "guitar", "campfire" -> if (use.spot < 0) (if (use.thing == "campfire") fire + Offset(-u * 0.21f, u * 0.03f) else home) else
            if (use.thing == "campfire" && bench) seat + Offset(0f, -u * 0.012f) else if (use.thing == "campfire") fire + Offset(-u * 0.21f, u * 0.03f) else p
        "kettle", "pizza_oven" ->
            // Beside the fire pit, clear of the stones and the flames (or on the bench, if there is one).
            if (bench) seat + Offset(0f, -u * 0.012f) else fire + Offset(-u * 0.21f, u * 0.03f)
        // Fetched first: to the woodpile for a log, then over to the fire; to the rod, then to the bank.
        "woodpile" -> fetched(use.t, p + Offset(u * 0.05f * k, u * 0.004f), fire + Offset(-u * 0.21f, u * 0.03f))
        "fishing_rod" -> fetched(use.t, p + Offset(u * 0.05f * k, u * 0.004f), Offset(w * 0.335f, h * 0.84f))
        "wildflowers", "lantern", "tulips", "watering_can", "birdhouse", "beach_ball", "watermelon", "pumpkins", "scarecrow", "gift_boxes", "snowman" ->
            p + Offset(u * 0.055f * k, u * 0.004f)
        "leaf_pile", "sled", "deck_chair" -> p + Offset(0f, -u * 0.004f)
        "telescope" -> p + Offset(-u * 0.05f * k, u * 0.004f)
        "log_bench", "picnic_table" -> p + Offset(0f, -u * 0.012f * k)
        "tent", "bell_tent", "cabin" -> p + Offset(0f, u * 0.01f * k)
        "hammock" -> p + Offset(0f, -u * 0.03f)
        "swing" -> p + Offset(-u * 0.01f, -u * 0.04f)
        "treehouse" -> Offset(p.x + u * 0.03f, u * 0.6f - u * 0.42f * 0.4f - u * 0.004f) // on the platform, in the tree
        // Out on the water, and drifting back to the mooring before stepping off.
        "canoe", "raft" -> p + Offset(sin(use.t * 0.7f) * w * 0.04f * (if (use.end > 0f) ((use.end - Uses.STEP - use.t) / 1.5f).coerceIn(0f, 1f) else 1f), -u * 0.02f)
        "rocking_chair" -> p + Offset(0f, -u * 0.012f * k)
        "wind_chimes", "bird_feeder" -> Offset(w * 0.64f, h * 0.71f)
        "dock" -> p + Offset(-u * 0.05f, u * 0.0f)
        "string_lights" -> Offset(w * 0.6f, h * 0.7f)
        else -> p
    }
}

/** A log from the companion's hands into the fire (after fetching it), then sparks. */
private fun DrawScope.woodToFire(use: InUse, avatar: Avatar, mood: Mood, worn: Set<Accessory>, base: Offset, s: Float, u: Float, fire: Offset, lights: MutableList<Light>) {
    val t = use.t
    shadowAt(base, s * 0.45f)
    actor(avatar, mood, worn, base, s, look = 0.8f, bounce = if (t < 1.2f) sin(t * 6f) else 0f)
    if (t < 1.2f) {
        // A log flies from the companion's hands into the fire.
        val k = t / 1.2f
        val from = base + Offset(s * 0.3f, -s * 0.5f)
        val to = fire + Offset(0f, -u * 0.02f)
        val at = Offset(from.x + (to.x - from.x) * k, from.y + (to.y - from.y) * k - sin(k * PI.toFloat()) * u * 0.08f)
        rotate(k * 360f, at) { drawRoundRect(Wood, at + Offset(-u * 0.018f, -u * 0.005f), Size(u * 0.036f, u * 0.01f), CornerRadius(u * 0.004f)) }
    } else {
        rising(t, fire + Offset(0f, -u * 0.06f), u * 0.012f, every = 0.6f, drift = 2f) { at, a -> drawCircle(Color(0xFFFFB347).copy(alpha = a), u * 0.003f, at) }
        lights += Light(fire + Offset(0f, -u * 0.05f), u * 0.24f, 1f)
    }
}

/** Casting from the bank (after fetching the rod): the float flies, lands, bobs; a fish jumps and is caught. */
private fun DrawScope.fishing(use: InUse, avatar: Avatar, mood: Mood, worn: Set<Accessory>, base: Offset, s: Float, u: Float, w: Float, h: Float) {
    val t = use.t
    shadowAt(base, s * 0.45f)
    actor(avatar, mood, worn, base, s, look = -1f)
    val hand = base + Offset(-s * 0.3f, -s * 0.45f)
    val tip = hand + Offset(-u * 0.09f, -u * 0.09f)
    drawLine(BarkDark, hand, tip, strokeWidth = u * 0.004f, cap = StrokeCap.Round)
    // The cast: the float flies out in an arc, lands with a splash, then bobs.
    val target = Offset(w * 0.2f, h * 0.8f)
    val castK = (t / 0.9f).coerceIn(0f, 1f)
    val float = if (castK < 1f) Offset(tip.x + (target.x - tip.x) * castK, tip.y + (target.y - tip.y) * castK - sin(castK * PI.toFloat()) * u * 0.08f)
        else target + Offset(0f, sin(t * 3f) * u * 0.003f)
    drawLine(Color.White.copy(alpha = 0.7f), tip, float, strokeWidth = u * 0.0012f)
    drawCircle(Color(0xFFD64541), u * 0.005f, float)
    if (castK >= 1f) {
        drawOval(Color.White.copy(alpha = 0.3f), Offset(float.x - u * 0.015f, float.y + u * 0.002f), Size(u * 0.03f, u * 0.006f))
        for (r in 0 until 2) {
            val k = ((t - 0.9f) / 1.2f - r * 0.3f).coerceIn(0f, 1f)
            if (k in 0.01f..0.99f) drawOval(Color.White.copy(alpha = 0.6f * (1 - k)), Offset(target.x - u * 0.05f * k, target.y - u * 0.008f * k), Size(u * 0.1f * k, u * 0.016f * k), style = Stroke(u * 0.002f))
        }
    }
    // A fish jumps, then it's caught.
    if (t in 4f..5.2f) {
        val k = (t - 4f) / 1.2f
        val at = float + Offset(-u * 0.04f + k * u * 0.06f, -sin(k * PI.toFloat()) * u * 0.07f)
        fish(at, u, k * 180f - 90f)
        droplets(float + Offset(-u * 0.03f, 0f), u, k)
    } else if (t > 5.2f) fish(hand + Offset(-u * 0.01f, u * 0.01f), u, 80f)
}

/** Where the companion is while fetching something: at it, then carrying it over (eased), then there. */
private fun fetched(t: Float, at: Offset, to: Offset): Offset {
    val k = ((t - 0.5f) / (Uses.FETCH - 0.5f)).coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
    return at + (to - at) * k
}

/** Draws the companion at [base] (feet), [s] tall, optionally rotated, faded, or with eyes closed. */
private fun DrawScope.actor(
    avatar: Avatar, mood: Mood, worn: Set<Accessory>, base: Offset, s: Float,
    rotation: Float = 0f, alpha: Float = 1f, bounce: Float = 0f, sleepy: Boolean = false, look: Float = 0f, book: Boolean = false,
) = drawIntoCanvas {
    val c = it.nativeCanvas
    c.save()
    if (alpha < 1f) c.saveLayerAlpha(null, (alpha * 255).toInt())
    c.rotate(rotation, base.x, base.y - s * 0.45f)
    c.translate(base.x - s / 2, base.y - s * 0.95f)
    CharacterPainter.draw(c, avatar, mood, s, CharacterPainter.Pose(blink = if (sleepy) 1f else 0f, breathe = bounce, look = look, book = book), worn)
    if (alpha < 1f) c.restore()
    c.restore()
}

private fun DrawScope.shadowAt(p: Offset, half: Float) {
    val c = p + Offset(half * 0.25f, 0f)
    drawOval(Color.Black.copy(alpha = 0.06f), Offset(c.x - half * 1.25f, c.y - half * 0.16f), Size(half * 2.5f, half * 0.32f))
    drawOval(Color.Black.copy(alpha = 0.11f), Offset(c.x - half * 0.95f, c.y - half * 0.11f), Size(half * 1.9f, half * 0.22f))
}

/** A little music note. */
private fun DrawScope.note(at: Offset, s: Float, alpha: Float) {
    drawOval(Ink.copy(alpha = alpha), Offset(at.x - s * 0.5f, at.y - s * 0.3f), Size(s, s * 0.7f))
    drawLine(Ink.copy(alpha = alpha), at + Offset(s * 0.45f, 0f), at + Offset(s * 0.45f, -s * 2f), strokeWidth = s * 0.22f)
    drawLine(Ink.copy(alpha = alpha), at + Offset(s * 0.45f, -s * 2f), at + Offset(s * 1.1f, -s * 1.5f), strokeWidth = s * 0.22f, cap = StrokeCap.Round)
}

/** Things floating up and fading: notes, hearts, Zs, sparks. [every] seconds apart. */
private fun DrawScope.rising(t: Float, from: Offset, s: Float, every: Float = 0.9f, drift: Float = 1f, draw: DrawScope.(Offset, Float) -> Unit) {
    for (i in 0 until 3) {
        val phase = ((t / every + i / 3f) % 1f)
        val at = from + Offset(sin((phase + i) * PI.toFloat() * 2) * s * 0.6f * drift, -phase * s * 6f)
        draw(at, 1f - phase)
    }
}

private fun DrawScope.heart(at: Offset, s: Float, alpha: Float) {
    val c = Color(0xFFE5677A).copy(alpha = alpha)
    drawCircle(c, s * 0.3f, at + Offset(-s * 0.25f, 0f))
    drawCircle(c, s * 0.3f, at + Offset(s * 0.25f, 0f))
    drawPath(Path().apply { moveTo(at.x - s * 0.55f, at.y + s * 0.08f); lineTo(at.x, at.y + s * 0.6f); lineTo(at.x + s * 0.55f, at.y + s * 0.08f); close() }, c)
}

private fun DrawScope.zee(at: Offset, s: Float, alpha: Float) {
    // Light letters with a dark edge: they read on a sunny meadow and in the night.
    val path = Path().apply { moveTo(at.x - s / 2, at.y - s / 2); lineTo(at.x + s / 2, at.y - s / 2); lineTo(at.x - s / 2, at.y + s / 2); lineTo(at.x + s / 2, at.y + s / 2) }
    drawPath(path, Ink.copy(alpha = alpha * 0.5f), style = Stroke(s * 0.32f, cap = StrokeCap.Round))
    drawPath(path, Color(0xFFFFF6DA).copy(alpha = alpha), style = Stroke(s * 0.16f, cap = StrokeCap.Round))
}

private fun DrawScope.guitarHeld(center: Offset, s: Float, strum: Float) = rotate(-58f, center) {
    drawLine(BarkDark, center + Offset(0f, -s * 0.12f), center + Offset(0f, -s * 0.55f), strokeWidth = s * 0.045f, cap = StrokeCap.Round)
    drawRoundRect(BarkDark, center + Offset(-s * 0.035f, -s * 0.62f), Size(s * 0.07f, s * 0.09f), CornerRadius(s * 0.02f))
    drawOval(Color(0xFFC98B4A), center + Offset(-s * 0.13f, -s * 0.06f), Size(s * 0.26f, s * 0.24f))
    drawOval(Color(0xFFC98B4A), center + Offset(-s * 0.1f, -s * 0.2f), Size(s * 0.2f, s * 0.18f))
    drawCircle(Color(0xFF3A2414), s * 0.04f, center + Offset(0f, -s * 0.04f))
    // Strings shiver after each strum.
    for (k in -1..1) drawLine(Color(0xFFEDE3CC), center + Offset(k * s * 0.012f + sin(strum * 40f) * s * 0.004f * (1 - strum).coerceAtLeast(0f), s * 0.1f), center + Offset(k * s * 0.012f, -s * 0.55f), strokeWidth = s * 0.006f)
}

/**
 * The companion using [use]'s thing, with its props and effects; [base] comes from useAnchor.
 * Glowing parts go to [lights]. [lastStrum] is seconds since the guitar was last strummed.
 */
fun DrawScope.drawUse(
    use: InUse, avatar: Avatar, mood: Mood, worn: Set<Accessory>, placed: Map<Int, String>, base: Offset, lights: MutableList<Light>, lastStrum: Float = 9f,
    layout: Map<Int, Offset> = emptyMap(),
) {
    val w = size.width
    val h = size.height
    val u = h
    val t = use.t
    val s = u * 0.17f * depthScale(base.y / h)
    val p = SpotPositions[use.spot]?.let { spotPos(use.spot, layout) }?.let { Offset(it.x * w, it.y * h) } ?: base
    val k = if (use.spot >= 0) spotScale(use.spot, layout) else 1f
    // An activity just done: acorns pop out and fly up to the corner.
    if ((use.spot == -1 || use.reward) && t < 1.6f) acornBurst(base + Offset(0f, -s * 0.6f), t / 1.6f, u)
    val fire = SpotPositions.getValue(0).let { Offset(it.x * w, it.y * h) }
    when (use.thing) {
        "guitar" -> {
            shadowAt(base, s * 0.45f)
            // Swaying to the beat, the guitar with them; colourful notes drift up.
            rotate(sin(t * 2.2f) * 6f, base) {
                actor(avatar, mood, worn, base, s, bounce = sin(t * 7f) * 0.6f, look = 0.3f)
                guitarHeld(base + Offset(s * 0.05f, -s * 0.28f), s, lastStrum)
            }
            val colors = listOf(Color(0xFF316849), Color(0xFFE07A5F), Color(0xFFD9A440))
            for (k in 0 until 3) {
                val ph = ((t / 1.2f + k / 3f) % 1f)
                val at = base + Offset(s * (0.4f + 0.25f * sin((ph + k) * 6f)), -s * (0.9f + ph * 0.9f))
                noteColored(at, s * 0.09f, colors[k], 1f - ph)
            }
            if (lastStrum in 0f..0.5f) sparkles(base + Offset(s * 0.1f, -s * 0.3f), u, lastStrum / 0.5f)
        }
        "campfire", "kettle", "pizza_oven" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, bounce = sin(t * 2f) * 0.3f, look = 0.8f)
            val hand = base + Offset(s * 0.38f, -s * 0.4f)
            when (use.thing) {
                "campfire" -> {
                    val end = fire + Offset(-u * 0.008f, -u * 0.07f)
                    drawLine(BarkDark, hand, end, strokeWidth = u * 0.003f, cap = StrokeCap.Round)
                    val toast = (t / 4.5f).coerceIn(0f, 1f)
                    drawRoundRect(androidx.compose.ui.graphics.lerp(Marshmallow, Toasted, toast), end + Offset(-u * 0.011f, -u * 0.01f), Size(u * 0.022f, u * 0.02f), CornerRadius(u * 0.006f))
                    // Sparks fly up; at the end the marshmallow catches fire for a moment, and gets blown out.
                    rising(t, fire + Offset(0f, -u * 0.06f), u * 0.014f, every = 0.5f, drift = 2.5f) { at, a -> drawCircle(Color(0xFFFFB347).copy(alpha = a), u * 0.0035f, at) }
                    lights += Light(fire + Offset(0f, -u * 0.05f), u * (0.18f + 0.04f * sin(t * 9f)), 0.8f)
                    if (t in 4.6f..5.6f) {
                        val k = (t - 4.6f)
                        drawPath(Path().apply { val m = end + Offset(0f, -u * 0.012f); moveTo(m.x - u * 0.008f, m.y); quadraticTo(m.x, m.y - u * 0.035f * (1 - k), m.x + u * 0.008f, m.y); close() }, Color(0xFFF2A341).copy(alpha = 1f - k))
                        for (i in 0 until 3) drawCircle(Color.White.copy(alpha = 0.5f * (1 - k)), u * 0.004f * (1 + i), hand + Offset(-s * 0.05f + k * u * 0.05f * (i + 1), -s * 0.35f - i * u * 0.004f))
                    }
                }
                "kettle" -> {
                    drawRoundRect(Color(0xFF2A6F6B), hand + Offset(-u * 0.008f, -u * 0.014f), Size(u * 0.016f, u * 0.016f), CornerRadius(u * 0.003f))
                    drawArc(Color(0xFF2A6F6B), -90f, 180f, false, hand + Offset(u * 0.005f, -u * 0.01f), Size(u * 0.01f, u * 0.008f), style = Stroke(u * 0.002f))
                    rising(t, hand + Offset(0f, -u * 0.02f), u * 0.006f, every = 1.1f) { at, a -> drawCircle(Color.White.copy(alpha = 0.45f * a), u * 0.005f, at) }
                }
                "pizza_oven" -> {
                    drawPath(Path().apply { moveTo(hand.x - u * 0.01f, hand.y); lineTo(hand.x + u * 0.018f, hand.y - u * 0.008f); lineTo(hand.x + u * 0.006f, hand.y + u * 0.016f); close() }, Color(0xFFF2C14E))
                    drawCircle(Color(0xFFC0392B), u * 0.0025f, hand + Offset(u * 0.004f, u * 0.002f))
                    drawCircle(Color(0xFFC0392B), u * 0.0022f, hand + Offset(u * 0.009f, u * 0.006f))
                }
            }
        }
        "woodpile", "fishing_rod" -> if (t < Uses.FETCH) {
            // Fetching: a little crouch to pick it up, then carried over.
            val log = use.thing == "woodpile"
            if (t < 0.5f) {
                shadowAt(base, s * 0.45f)
                actor(avatar, mood, worn, base + Offset(0f, sin(t / 0.5f * PI.toFloat()) * s * 0.04f), s, look = 0.6f, bounce = -sin(t / 0.5f * PI.toFloat()))
            } else {
                val toRight = useAnchor(use.copy(t = 100f), placed, base, layout).x >= base.x
                drawWalker(avatar, mood, worn, base, t, toRight)
            }
            val hands = base + Offset(s * 0.28f, -s * 0.42f)
            if (t >= 0.25f) {
                if (log) drawRoundRect(Wood, hands + Offset(-u * 0.018f, -u * 0.005f), Size(u * 0.036f, u * 0.01f), CornerRadius(u * 0.004f))
                else drawLine(BarkDark, hands, hands + Offset(-u * 0.05f, -u * 0.07f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            }
        } else if (use.thing == "fishing_rod") fishing(use.copy(t = t - Uses.FETCH), avatar, mood, worn, base, s, u, w, h)
        else woodToFire(use.copy(t = t - Uses.FETCH), avatar, mood, worn, base, s, u, fire, lights)
        // Under a tree, out of the rain or snow: a good shake (drops flying), then a look out from under it.
        "shelter" -> {
            shadowAt(base, s * 0.45f)
            val shake = if (t < 1f) sin(t * 34f) * 9f * (1f - t) else 0f
            rotate(shake, base + Offset(0f, -s * 0.3f)) {
                actor(avatar, mood, worn, base, s, look = if (t < 1f) 0f else sin(t * 0.7f) * 0.9f, bounce = if (t < 1f) 0f else sin(t * 1.3f) * 0.2f)
            }
            if (t < 1.1f) for (side in listOf(-1f, 1f)) droplets(base + Offset(side * s * 0.35f, -s * 0.55f), u, t / 1.1f)
            // Now and then a peek up at the branches.
            if (t > 3f && (t % 4f) < 0.6f) drawCircle(Color.White.copy(alpha = 0.5f), u * 0.003f, base + Offset(s * 0.1f, -s * 1.15f - (t % 4f) * s * 0.2f))
        }
        // A stroll among the trees: looking about, a leaf coming down, a butterfly passing.
        "stroll" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = sin(t * 0.9f), bounce = sin(t * 1.4f) * 0.3f)
            leafFalling(base + Offset(s * 0.5f, -s * 1.3f), u, t)
            butterfly(base + Offset(-s * 0.6f + sin(t * 0.8f) * s * 0.5f, -s * (1.1f + 0.2f * sin(t * 1.7f))), u, t * 14f, Color(0xFFF2C14E))
        }
        // Down by the river: watching the water, then a pebble skipped across it.
        "river" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = -1f, bounce = if (t in 1.6f..2.1f) sin((t - 1.6f) * 12f) else sin(t * 1.2f) * 0.2f)
            val water = Offset(w * 0.13f, base.y - u * 0.02f)
            val throwK = ((t - 2f) / 0.8f).coerceIn(0f, 1f)
            val hand = base + Offset(-s * 0.3f, -s * 0.45f)
            if (throwK in 0.001f..0.999f) drawCircle(Color(0xFF8D8A82), u * 0.004f, hand + (water - hand) * throwK + Offset(0f, -sin(throwK * PI.toFloat()) * u * 0.06f))
            for (r in 0 until 3) {
                val k = ((t - 2.8f) / 1.4f - r * 0.25f).coerceIn(0f, 1f)
                if (k in 0.01f..0.99f) drawOval(Color.White.copy(alpha = 0.6f * (1 - k)), Offset(water.x - u * 0.05f * k, water.y - u * 0.008f * k), Size(u * 0.1f * k, u * 0.016f * k), style = Stroke(u * 0.002f))
            }
            if (t in 2.8f..3.4f) droplets(water, u, (t - 2.8f) / 0.6f)
        }
        "wildflowers" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = -0.8f, bounce = sin(t * 1.5f) * 0.4f)
            rising(t, p + Offset(0f, -u * 0.05f), u * 0.012f, every = 1.6f) { at, a -> heart(at, u * 0.012f, a) }
            // Two butterflies dance over the flowers.
            for (k in 0 until 2) {
                val a = t * (1.5f + k * 0.4f) + k * 3f
                butterfly(p + Offset(kotlin.math.cos(a) * u * 0.05f, -u * 0.06f + sin(a * 1.7f) * u * 0.025f), u, t * 14f + k, if (k == 0) Color(0xFFF2C14E) else Color(0xFF9FC4E6))
            }
        }
        "lantern" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = -0.8f)
            val glow = p + Offset(0f, -u * 0.038f)
            val pulse = 0.5f + 0.5f * sin(t * 4f)
            drawCircle(GlowWarm.copy(alpha = 0.25f + 0.2f * pulse), u * (0.03f + 0.01f * pulse), glow)
            lights += Light(glow, u * 0.14f, 1f)
            // Moths circling the light.
            for (k in 0 until 3) {
                val a = t * (3f + k) + k * 2.1f
                butterfly(glow + Offset(kotlin.math.cos(a) * u * 0.03f, sin(a) * u * 0.022f), u * 0.6f, t * 22f + k, Color(0xFFE8DCC4))
            }
        }
        "telescope" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = 1f, rotation = -6f)
            // A shooting star crosses the sky; stars twinkle.
            val k = (t % 3.5f) / 1.2f
            if (k < 1f) {
                val a = Offset(w * (0.25f + 0.4f * k), h * (0.06f + 0.1f * k))
                drawLine(Color.White.copy(alpha = 1f - k), a, a + Offset(-w * 0.06f, -h * 0.03f), strokeWidth = u * 0.003f, cap = StrokeCap.Round)
            }
            for ((sx, sy) in listOf(0.3f to 0.1f, 0.55f to 0.06f, 0.75f to 0.12f)) {
                val tw = 0.5f + 0.5f * sin(t * 5f + sx * 10f)
                drawCircle(Color.White.copy(alpha = 0.4f + 0.6f * tw), u * 0.004f * (1 + tw), Offset(w * sx, h * sy))
            }
        }
        "log_bench", "picnic_table" -> {
            actor(avatar, mood, worn, base, s, bounce = sin(t * 2.5f) * 0.4f, look = if (use.thing == "log_bench") 0.6f else 0f)
            if (use.thing == "log_bench") rising(t, base + Offset(s * 0.4f, -s * 0.85f), s * 0.07f, every = 1.6f) { at, a -> note(at, s * 0.07f, a) }
            else {
                val hand = base + Offset(s * 0.32f, -s * 0.42f)
                drawRoundRect(Color(0xFFF1E1B8), hand + Offset(-u * 0.01f, -u * 0.006f), Size(u * 0.02f, u * 0.012f), CornerRadius(u * 0.003f))
                drawRect(Color(0xFF7FA35B), hand + Offset(-u * 0.01f, -u * 0.001f), Size(u * 0.02f, u * 0.002f))
            }
        }
        "tent", "bell_tent", "cabin" -> {
            // Walks in and fades; then Zs drift out of the roof.
            if (t < 1.5f) actor(avatar, mood, worn, base, s * (1f - t / 4f), alpha = 1f - t / 1.5f)
            else rising(t, p + Offset(u * 0.03f, -u * 0.12f) * k, u * 0.014f, every = 1.5f) { at, a -> zee(at, u * 0.016f, a) }
        }
        "hammock" -> {
            val sway = sin(t * 1.6f) * 4f
            actor(avatar, mood, worn, base + Offset(0f, s * 0.1f), s * 0.9f, rotation = -78f + sway, sleepy = true)
            for (k in 0 until 3) leafFalling(Offset(base.x + u * (k - 1) * 0.06f, base.y - u * 0.25f), u, t + k * 1.7f)
            rising(t, base + Offset(s * 0.2f, -s * 0.3f), u * 0.012f, every = 1.5f) { at, a -> zee(at, u * 0.014f, a) }
        }
        "swing" -> {
            // The swing goes back and forth with the companion on the seat.
            // From the branch of the tree beside it (as the swing is drawn in the scene).
            val pivot = Offset(p.x - u * 0.006f, u * 0.6f - u * 0.42f * 0.38f)
            val angle = sin(t * 2.6f) * 34f
            for (k in 0 until 3) leafFalling(Offset(pivot.x + u * (k - 1) * 0.05f, pivot.y), u, t + k * 1.3f)
            rotate(angle, pivot) {
                val seat = pivot + Offset(0f, p.y - u * 0.04f - pivot.y)
                drawLine(Rope, pivot + Offset(-u * 0.02f, 0f), seat + Offset(-u * 0.02f, 0f), strokeWidth = u * 0.0025f)
                drawLine(Rope, pivot + Offset(u * 0.02f, 0f), seat + Offset(u * 0.02f, 0f), strokeWidth = u * 0.0025f)
                actor(avatar, mood, worn, seat + Offset(0f, u * 0.006f), u * 0.11f, bounce = -0.3f)
                drawRoundRect(Wood, seat + Offset(-u * 0.028f, 0f), Size(u * 0.056f, u * 0.009f), CornerRadius(u * 0.003f))
            }
        }
        "treehouse" -> {
            actor(avatar, mood, worn, base + Offset(0f, -u * 0.012f), u * 0.085f, rotation = sin(t * 6f) * 8f)
        }
        "canoe" -> {
            // Paddling up and down the river.
            actor(avatar, mood, worn, base + Offset(0f, u * 0.015f), s * 0.7f, look = if (kotlin.math.cos(t * 0.7f) > 0) 1f else -1f)
            canoeHull(base + Offset(0f, u * 0.02f), u)
            val side = if (sin(t * 3f) > 0) 1f else -1f
            drawLine(BarkDark, base + Offset(side * s * 0.15f, -s * 0.35f), base + Offset(side * s * 0.4f, u * 0.03f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            droplets(base + Offset(side * s * 0.4f, u * 0.03f), u, (t * 3f / PI.toFloat()) % 1f)
            drawOval(Color.White.copy(alpha = 0.35f), Offset(base.x - u * 0.12f, base.y + u * 0.02f), Size(u * 0.24f, u * 0.01f))
        }
        "raft" -> {
            // Sitting on the raft as it drifts, trailing a hand in the water.
            raftAt(base + Offset(0f, u * 0.02f), u)
            actor(avatar, mood, worn, base + Offset(0f, -u * 0.006f), s * 0.72f, bounce = sin(t * 1.4f) * 0.3f, look = -0.5f)
        }
        "rocking_chair" -> {
            // Rocking: chair and companion tip back and forth together on the rockers.
            // Sized like the chair where it stands (the companion keeps its own size).
            rotate(sin(t * 2.2f) * 7f, base + Offset(0f, u * 0.012f)) { scale(k, pivot = base + Offset(0f, u * 0.012f * k)) {
                val chair = base + Offset(0f, u * 0.012f)
                drawArc(Color(0xFF9C6B45), 20f, 140f, false, Offset(chair.x - u * 0.04f, chair.y - u * 0.03f), Size(u * 0.08f, u * 0.034f), style = Stroke(u * 0.005f, cap = StrokeCap.Round))
                drawLine(Color(0xFF9C6B45), chair + Offset(u * 0.02f, -u * 0.037f), chair + Offset(u * 0.03f, -u * 0.085f), strokeWidth = u * 0.005f, cap = StrokeCap.Round)
                actor(avatar, mood, worn, base, s * 0.82f / k, look = -0.3f, sleepy = t > 4f)
                for (dx in listOf(-0.018f, 0.018f)) drawLine(Color(0xFF9C6B45), chair + Offset(u * dx, -u * 0.01f), chair + Offset(u * dx, -u * 0.035f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            } }
        }
        "wind_chimes" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = 0.6f, bounce = sin(t * 2f) * 0.3f)
            rising(t, Offset(w * 0.7f, h * 0.55f), u * 0.01f, every = 0.9f, drift = 2f) { at, a -> note(at, u * 0.008f, a) }
        }
        "bird_feeder" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = 0.7f)
            // Birds come and go around the feeder.
            val feeder = Offset(w * 0.7f, h * 0.48f)
            for (k in 0 until 3) {
                val a = t * (1.6f + k * 0.4f) + k * 2.1f
                val at = feeder + Offset(kotlin.math.cos(a) * u * (0.05f + k * 0.015f), sin(a * 1.3f) * u * 0.03f - u * 0.01f)
                bird(at, u, listOf(Color(0xFFD9744A), Color(0xFF5D8FB8), Color(0xFFE0B84A))[k], flying = true, flap = (sin(t * 18f + k) + 1) / 2)
            }
        }
        "dock" -> {
            actor(avatar, mood, worn, base + Offset(0f, -u * 0.004f), s * 0.8f, bounce = sin(t * 2f) * 0.3f, look = -0.6f)
            for (k in 0 until 2) {
                val r = ((t * 0.5f + k * 0.5f) % 1f)
                drawOval(Color.White.copy(alpha = 0.4f * (1 - r)), Offset(base.x - u * 0.05f * r - u * 0.01f, base.y + u * 0.012f), Size(u * 0.1f * r + u * 0.02f, u * 0.012f * (r + 0.3f)), style = Stroke(u * 0.002f))
            }
        }
        "string_lights" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base + Offset(0f, -kotlin.math.abs(sin(t * 5f)) * u * 0.02f), s, rotation = sin(t * 5f) * 10f)
            rising(t, base + Offset(s * 0.4f, -s * 0.9f), s * 0.07f, every = 1f) { at, a -> note(at, s * 0.07f, a) }
        }
        "act_read" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, book = true, bounce = sin(t * 1.5f) * 0.2f)
        }
        "act_run" -> {
            shadowAt(base, s * 0.45f)
            val dir = kotlin.math.cos(t * 0.9f)
            actor(avatar, mood, worn, base + Offset(0f, -kotlin.math.abs(sin(t * 9f)) * u * 0.015f), s, rotation = if (dir > 0) 6f else -6f, look = if (dir > 0) 1f else -1f)
            if (sin(t * 3f) > 0.6f) drawCircle(Color(0xFF8EC9E6), u * 0.004f, base + Offset(-s * 0.35f * dir, -s * 0.85f)) // a drop of sweat
        }
        "act_walk" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base + Offset(0f, -kotlin.math.abs(sin(t * 4f)) * u * 0.006f), s, look = -0.6f)
            rising(t, base + Offset(-s * 0.4f, -s * 0.7f), u * 0.01f, every = 2f) { at, a -> heart(at, u * 0.01f, a * 0.6f) }
        }
        "act_stretch" -> {
            shadowAt(base, s * 0.45f)
            val k = (sin(t * 1.8f) + 1) / 2
            actor(avatar, mood, worn, base, s * (1f + 0.08f * k), sleepy = t < 2f, bounce = -k)
            for (i in 0 until 6) {
                val a = i / 6f * 2 * PI.toFloat() + t * 0.3f
                val c = base + Offset(0f, -s * 0.5f)
                drawLine(GlowWarm.copy(alpha = 0.5f), c + Offset(kotlin.math.cos(a) * s * 0.6f, sin(a) * s * 0.6f), c + Offset(kotlin.math.cos(a) * s * 0.8f, sin(a) * s * 0.8f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            }
        }
        "act_hearts" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, bounce = sin(t * 4f) * 0.5f)
            rising(t, base + Offset(0f, -s * 0.9f), u * 0.014f, every = 1f, drift = 1.5f) { at, a -> heart(at, u * 0.016f, a) }
        }
        "act_tidy" -> {
            shadowAt(base, s * 0.45f)
            val sweep = sin(t * 5f) * 18f
            actor(avatar, mood, worn, base, s, look = 0.5f)
            val grip = base + Offset(s * 0.35f, -s * 0.5f)
            rotate(sweep, grip) {
                drawLine(Wood, grip, grip + Offset(s * 0.1f, s * 0.48f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
                val b0 = grip + Offset(s * 0.1f, s * 0.48f)
                drawPath(Path().apply { moveTo(b0.x - u * 0.012f, b0.y + u * 0.012f); lineTo(b0.x + u * 0.012f, b0.y + u * 0.012f); lineTo(b0.x + u * 0.004f, b0.y - u * 0.004f); lineTo(b0.x - u * 0.004f, b0.y - u * 0.004f); close() }, Color(0xFFD9B26A))
            }
            rising(t, base + Offset(s * 0.5f, -s * 0.05f), u * 0.006f, every = 0.6f) { at, a -> drawCircle(Color(0xFFB79C7A).copy(alpha = 0.5f * a), u * 0.004f, at) }
        }
        "act_cheer" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base + Offset(0f, -kotlin.math.abs(sin(t * 4f)) * u * 0.04f), s, bounce = -0.5f)
            rising(t, base + Offset(0f, -s * 1f), u * 0.012f, every = 0.8f, drift = 2f) { at, a -> drawCircle(GlowWarm.copy(alpha = a), u * 0.005f, at) }
        }
        // The seasons' own: beside it, with a little something happening.
        "tulips", "watering_can", "birdhouse", "watermelon", "pumpkins", "scarecrow", "gift_boxes", "snowman" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = -0.8f, bounce = sin(t * 1.8f) * 0.4f)
            when (use.thing) {
                "watering_can" -> for (k in 0 until 4) { // drops falling on the ground in front
                    val ph = (t * 1.6f + k / 4f) % 1f
                    drawCircle(Color(0xFF8EC5E8).copy(alpha = 1f - ph), u * 0.003f, base + Offset(-s * 0.55f - k * u * 0.004f, -s * 0.5f + ph * s * 0.45f))
                }
                "birdhouse" -> {
                    val k = ((t % 4f) / 1.5f).coerceAtMost(1f) // a bird flies to the perch, then sits
                    val perch = p + Offset(0f, -u * 0.068f)
                    val from = perch + Offset(-u * 0.12f, -u * 0.08f)
                    val at = Offset(from.x + (perch.x - from.x) * k, from.y + (perch.y - from.y) * k - sin(k * PI.toFloat()) * u * 0.03f)
                    drawOval(Color(0xFF8C6A4A), at + Offset(-u * 0.007f, -u * 0.005f), Size(u * 0.014f, u * 0.009f))
                    drawCircle(Color(0xFF8C6A4A), u * 0.0045f, at + Offset(u * 0.006f, -u * 0.006f))
                    if (k < 1f) drawLine(Color(0xFF6B4F37), at, at + Offset(-u * 0.004f, -u * 0.008f * sin(t * 30f)), strokeWidth = u * 0.003f)
                }
                "gift_boxes", "snowman", "scarecrow" -> rising(t, base + Offset(-s * 0.35f, -s * 0.8f), u * 0.012f, every = 1.4f) { at, a -> heart(at, u * 0.011f, a) }
                else -> rising(t, p + Offset(0f, -u * 0.05f), u * 0.012f, every = 1.6f) { at, a -> heart(at, u * 0.012f, a) }
            }
        }
        "beach_ball" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = -0.6f, bounce = kotlin.math.abs(sin(t * 3f)) * 0.6f)
            // Bounced up and caught, again and again.
            val up = kotlin.math.abs(sin(t * 3f))
            val ball = base + Offset(-s * 0.45f, -s * (0.75f + up * 0.6f))
            drawCircle(Color(0xFFE5677A), u * 0.014f, ball)
            drawArc(Color.White, t * 200f, 60f, true, ball - Offset(u * 0.014f, u * 0.014f), Size(u * 0.028f, u * 0.028f))
            drawArc(Color(0xFF5FA8D3), t * 200f + 120f, 60f, true, ball - Offset(u * 0.014f, u * 0.014f), Size(u * 0.028f, u * 0.028f))
        }
        "leaf_pile" -> {
            // A jump, then leaves everywhere.
            val hop = (t % 2.4f) / 2.4f
            actor(avatar, mood, worn, base + Offset(0f, -sin(hop * PI.toFloat()).coerceAtLeast(0f) * u * 0.05f), s * 0.95f, bounce = 0.6f)
            for (k in 0 until 6) {
                val ph = ((t * 0.8f + k / 6f) % 1f)
                val at = base + Offset((k - 2.5f) * u * 0.015f * (1 + ph * 2), -s * 0.4f * sin(ph * PI.toFloat()) - u * 0.01f)
                rotate(ph * 400f + k * 50f, at) { drawOval(listOf(Color(0xFFD9822B), Color(0xFFE0A341), Color(0xFFB5502E))[k % 3].copy(alpha = 1f - ph * 0.6f), at - Offset(u * 0.006f, u * 0.0035f), Size(u * 0.012f, u * 0.007f)) }
            }
        }
        "sled" -> {
            actor(avatar, mood, worn, base + Offset(0f, -u * 0.008f), s * 0.85f, look = 1f, sleepy = t > 5f, bounce = sin(t * 2f) * 0.3f)
            for (k in 0 until 6) { // snow drifting down
                val ph = ((t * 0.3f + k / 6f) % 1f)
                drawCircle(Color.White.copy(alpha = 0.9f - ph * 0.5f), u * 0.0035f, base + Offset((k - 2.5f) * u * 0.025f + sin(t + k) * u * 0.006f, -s * 1.6f + ph * s * 1.5f))
            }
        }
        "deck_chair" -> {
            actor(avatar, mood, worn, base + Offset(-u * 0.004f, -u * 0.006f), s * 0.85f, rotation = -22f, sleepy = t > 2.5f)
            rising(t, base + Offset(s * 0.2f, -s * 0.8f), u * 0.012f, every = 1.5f) { at, a -> zee(at, u * 0.014f, a) }
        }
        else -> actor(avatar, mood, worn, base, s)
    }
}

private fun DrawScope.fish(at: Offset, u: Float, angle: Float) = rotate(angle, at) {
    drawOval(Color(0xFFE58F3C), Offset(at.x - u * 0.016f, at.y - u * 0.007f), Size(u * 0.032f, u * 0.014f))
    drawPath(Path().apply { moveTo(at.x + u * 0.014f, at.y); lineTo(at.x + u * 0.026f, at.y - u * 0.008f); lineTo(at.x + u * 0.026f, at.y + u * 0.008f); close() }, Color(0xFFE58F3C))
    drawCircle(Ink, u * 0.002f, at + Offset(-u * 0.009f, -u * 0.001f))
}

/** The canoe's hull alone, drawn over the companion sitting in it. */
private fun DrawScope.canoeHull(p: Offset, u: Float) {
    drawPath(Path().apply {
        moveTo(p.x - u * 0.1f, p.y - u * 0.04f)
        quadraticTo(p.x - u * 0.06f, p.y + u * 0.004f, p.x, p.y + u * 0.004f)
        quadraticTo(p.x + u * 0.06f, p.y + u * 0.004f, p.x + u * 0.1f, p.y - u * 0.04f)
        quadraticTo(p.x, p.y - u * 0.022f, p.x - u * 0.1f, p.y - u * 0.04f); close()
    }, Color(0xFF8E5A36))
    drawPath(Path().apply { moveTo(p.x - u * 0.092f, p.y - u * 0.036f); quadraticTo(p.x, p.y - u * 0.018f, p.x + u * 0.092f, p.y - u * 0.036f) }, WoodLight, style = Stroke(u * 0.004f))
}

/** Five acorns popping out of [from] and flying up to the top-right corner, [k] 0..1. */
private fun DrawScope.acornBurst(from: Offset, k: Float, u: Float) {
    val corner = Offset(size.width * 0.93f, size.height * 0.08f)
    for (i in 0 until 5) {
        val kk = ((k - i * 0.08f) / 0.7f).coerceIn(0f, 1f)
        if (kk <= 0f || kk >= 1f) continue
        val spread = (i - 2) * u * 0.03f
        val at = Offset(from.x + spread * (1 - kk) + (corner.x - from.x) * kk * kk, from.y - sin(kk * PI.toFloat()) * u * 0.12f + (corner.y - from.y) * kk * kk)
        val r = u * 0.012f
        drawOval(Color(0xFFB9783F), Offset(at.x - r * 0.8f, at.y - r * 0.6f), Size(r * 1.6f, r * 1.7f))
        drawArc(Color(0xFF6B4A2B), 180f, 180f, true, Offset(at.x - r, at.y - r * 1.1f), Size(r * 2f, r * 1.1f))
    }
}

/** The shelter the companion sleeps in at night (the first one in the camp), or null. */
fun bedOf(placed: Map<Int, String>): Int? =
    app.tend.core.Clearing.spots.filter { it.kind == app.tend.core.SpotKind.SHELTER }.firstOrNull { placed[it.id] != null }?.id

/**
 * Seconds since the camp came into view at night, counting while it's shown (for walking to bed
 * and the Zs); 100 by day, as if long asleep.
 */
@androidx.compose.runtime.Composable
fun rememberNightClock(night: Boolean): Float {
    var t by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(100f) }
    androidx.compose.runtime.LaunchedEffect(night) {
        if (!night) { t = 100f; return@LaunchedEffect }
        // Every frame while walking to bed; after that only the Zs drift, and a dozen frames a second is plenty.
        val start = System.nanoTime()
        while (true) {
            if (t < Uses.WALK + 2f) androidx.compose.runtime.withFrameNanos { t = (System.nanoTime() - start) / 1e9f }
            else { kotlinx.coroutines.delay(80); t = (System.nanoTime() - start) / 1e9f }
        }
    }
    return t
}

/**
 * Seconds of camp life while the camp is on screen, for what moves by itself (clouds, rain, snow,
 * fireflies). About 30 updates a second: smooth enough for drifting things, easy on the battery.
 */
@androidx.compose.runtime.Composable
fun rememberAmbientClock(): androidx.compose.runtime.State<Float> {
    // Returned as state, to be read only where it's drawn: then each tick redraws, nothing recomposes.
    val t = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val start = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            t.floatValue = (androidx.compose.runtime.withFrameNanos { it } - start) / 1e9f
            kotlinx.coroutines.delay(33)
        }
    }
    return t
}

/**
 * Places to go that aren't things, as rest codes (-10 and down): among the trees at the back,
 * and down on the river bank.
 */
private val WANDER = listOf(
    Offset(0.44f, 0.635f) to "stroll", Offset(0.68f, 0.625f) to "stroll", Offset(0.83f, 0.665f) to "stroll",
    Offset(0.355f, 0.72f) to "river", Offset(0.31f, 0.8f) to "river",
)
/** Under the trees, out of the rain or the snow (rest codes -20 and down): by the big tree on the right, under the oak. */
private val SHELTERS = listOf(Offset(0.83f, 0.745f), Offset(0.475f, 0.625f))
private fun wanderAt(spot: Int?): Offset? = when {
    spot == null -> null
    spot <= -20 -> SHELTERS.getOrNull(-20 - spot)
    spot <= -10 -> WANDER.getOrNull(-10 - spot)?.first
    else -> null
}

/** How much the companion likes using each thing on their own: favourites often, shelters and the lights now and then. */
private fun idleWeight(thing: String) = when (thing) {
    "guitar", "campfire", "fishing_rod", "swing", "hammock", "woodpile", "canoe", "raft" -> 3
    "tent", "bell_tent", "cabin", "treehouse", "wind_chimes", "bird_feeder", "string_lights" -> 1
    else -> 2
}

/** About where the companion is when staying at [rest], as scene fractions (to time a walk). */
internal fun restFraction(rest: Int?, layout: Map<Int, Offset>): Offset =
    wanderAt(rest) ?: rest?.takeIf { it in SpotPositions }?.let { spotPos(it, layout) } ?: Offset(0.395f, 0.86f)

/** A walk across [from] to [to] (scene fractions, at the camp's width to height): longer for farther. */
internal fun walkSeconds(from: Offset, to: Offset): Float =
    (0.7f + kotlin.math.hypot((to.x - from.x) * 1.6f, to.y - from.y) * 2.6f).coerceIn(1f, 3.2f)

/**
 * The companion getting on with camp life on their own: every so often they walk over to something
 * and use it (each thing now and then, favourites more often), quietly (no sound, no challenge
 * counted), or stroll among the trees or down to the river; then they stay there, and go on from
 * there to the next thing. Paused while [enabled] is false (something chosen by the user, the night,
 * a thing being moved). Now and then a stretch or a cheer where they stand.
 */
@androidx.compose.runtime.Composable
fun rememberIdleLife(
    placed: Map<Int, String>, rest: Int?, enabled: Boolean, layout: Map<Int, Offset> = emptyMap(), onRest: (Int) -> Unit,
): androidx.compose.runtime.State<InUse?> {
    // Returned as state, read only where it's drawn: the walk redraws the camp, nothing else recomposes.
    val current = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<InUse?>(null) }
    val placedNow by androidx.compose.runtime.rememberUpdatedState(placed)
    val restNow by androidx.compose.runtime.rememberUpdatedState(rest)
    val layoutNow by androidx.compose.runtime.rememberUpdatedState(layout)
    val on by androidx.compose.runtime.rememberUpdatedState(enabled)
    val onRestNow by androidx.compose.runtime.rememberUpdatedState(onRest)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val rnd = java.util.Random()
        while (true) {
            kotlinx.coroutines.delay(2_500L + rnd.nextInt(5_000))
            if (!on) continue
            // Everything in the camp, more or less often, and a stroll or the river now and then.
            // In rain or snow, under a tree is the favourite (a dash over there, a shake, and a look out).
            val wet = (app.tend.core.LiveWeather.fresh() ?: java.time.LocalDateTime.now().let { app.tend.core.Weather.of(it.toLocalDate(), it.hour) })
                .let { it == app.tend.core.Weather.RAIN || it == app.tend.core.Weather.SNOW }
            val options = placedNow.filter { (spot, _) -> spot != restNow && spot in SpotPositions }.map { (spot, thing) -> Triple(spot, thing, idleWeight(thing)) } +
                WANDER.indices.map { -10 - it }.filter { it != restNow }.map { Triple(it, WANDER[-10 - it].second, 1) } +
                (if (wet) SHELTERS.indices.map { -20 - it }.filter { it != restNow }.map { Triple(it, "shelter", 6) } else emptyList())
            val total = options.sumOf { it.third }
            // Now and then something where they stand: a stretch, a cheer (spot -2: nothing earned, no acorns fly).
            val pick = if (rnd.nextInt(100) < 15) null else rnd.nextInt(total).let { r -> var left = r; options.first { left -= it.third; left < 0 } }
            val (spot, thing) = pick?.let { it.first to it.second } ?: (-2 to listOf("act_stretch", "act_cheer", "act_hearts")[rnd.nextInt(3)])
            val walk = if (spot == -2) 0f else walkSeconds(Whereabouts.standing ?: restFraction(restNow, layoutNow), restFraction(spot, layoutNow)) *
                (if (thing == "shelter") 0.65f else 1f) // under a tree: a dash
            val end = minOf(Uses.seconds(thing), if (spot < 0 && spot > -10) 3.5f else 8.5f)
            val start = androidx.compose.runtime.withFrameNanos { it }
            var t = 0f
            while (on && t < walk + end) {
                t = (androidx.compose.runtime.withFrameNanos { it } - start) / 1e9f
                current.value = InUse(spot, thing, t, walk, end)
            }
            // Done: they stay (as after any use), and go on from there next time. Stopped part way: they stay where they got to.
            if (t >= walk + end && spot != -2) onRestNow(spot)
            current.value = null
        }
    }
    return current
}

/** Overshoots a little and settles: things pop into the camp rather than just appearing. */
fun easeOutBack(k: Float): Float {
    val c = 1.9f
    val x = k.coerceIn(0f, 1f) - 1f
    return 1f + (c + 1f) * x * x * x + c * x * x
}

/**
 * How far along each newly placed thing is in popping up (spot to 0..1). A spot that gets a new
 * thing animates; everything already there at first sight just is.
 */
@androidx.compose.runtime.Composable
fun rememberAppear(placed: Map<Int, String>): Map<Int, Float> {
    val seen = androidx.compose.runtime.remember { mutableMapOf<Int, String>().apply { putAll(placed) } }
    val anims = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateMapOf<Int, androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>() }
    androidx.compose.runtime.LaunchedEffect(placed) {
        placed.forEach { (spot, thing) ->
            if (seen[spot] != thing) {
                seen[spot] = thing
                val a = androidx.compose.animation.core.Animatable(0f)
                anims[spot] = a
                launch { a.animateTo(1f, androidx.compose.animation.core.tween(750, easing = androidx.compose.animation.core.LinearEasing)); anims.remove(spot) }
            }
        }
        seen.keys.retainAll(placed.keys)
    }
    return anims.mapValues { it.value.value }
}

/** Little four-point stars bursting out from [at] and fading, [k] 0..1. */
fun DrawScope.sparkles(at: Offset, u: Float, k: Float) {
    if (k <= 0f || k >= 1f) return
    for (i in 0 until 7) {
        val a = i / 7f * 6.283f + 0.4f
        val r = u * (0.03f + 0.07f * k)
        val c = at + Offset(kotlin.math.cos(a) * r, kotlin.math.sin(a) * r * 0.7f)
        val s = u * 0.008f * (1f - k) * (if (i % 2 == 0) 1.2f else 0.8f)
        val col = Color(0xFFFFE7A3).copy(alpha = 1f - k)
        drawPath(Path().apply { moveTo(c.x, c.y - s * 2); lineTo(c.x + s * 0.5f, c.y - s * 0.5f); lineTo(c.x + s * 2, c.y); lineTo(c.x + s * 0.5f, c.y + s * 0.5f); lineTo(c.x, c.y + s * 2); lineTo(c.x - s * 0.5f, c.y + s * 0.5f); lineTo(c.x - s * 2, c.y); lineTo(c.x - s * 0.5f, c.y - s * 0.5f); close() }, col)
    }
}

/** The camp floating in when a page opens: from a touch larger and transparent to in place. */
@androidx.compose.runtime.Composable
fun androidx.compose.ui.Modifier.campEntrance(): androidx.compose.ui.Modifier {
    // Coming from the other page, the scene glides over by itself: no fade-in on top of that.
    val gliding = LocalCampShare.current?.moving == true
    val k = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(if (gliding) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { k.animateTo(1f, androidx.compose.animation.core.tween(650, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
    return this.graphicsLayer { alpha = 0.3f + 0.7f * k.value; val s = 1.04f - 0.04f * k.value; scaleX = s; scaleY = s }
}

/** A coloured music note. */
private fun DrawScope.noteColored(at: Offset, s: Float, color: Color, alpha: Float) {
    val c = color.copy(alpha = alpha)
    drawOval(c, Offset(at.x - s * 0.5f, at.y - s * 0.3f), Size(s, s * 0.7f))
    drawLine(c, at + Offset(s * 0.45f, 0f), at + Offset(s * 0.45f, -s * 2f), strokeWidth = s * 0.22f)
    drawLine(c, at + Offset(s * 0.45f, -s * 2f), at + Offset(s * 1.1f, -s * 1.5f), strokeWidth = s * 0.22f, cap = StrokeCap.Round)
}

/** A butterfly (or moth) with flapping wings; [flap] is a phase. */
private fun DrawScope.butterfly(at: Offset, u0: Float, flap: Float, color: Color) {
    val u = u0 * 1.8f
    val open = 0.35f + 0.65f * kotlin.math.abs(sin(flap))
    for (side in listOf(-1f, 1f)) {
        drawOval(color, Offset(at.x + (if (side < 0) -u * 0.009f * open else 0f), at.y - u * 0.006f), Size(u * 0.009f * open, u * 0.008f))
        drawOval(color.copy(alpha = 0.85f), Offset(at.x + (if (side < 0) -u * 0.007f * open else 0f), at.y), Size(u * 0.007f * open, u * 0.006f))
    }
    drawLine(Ink, at + Offset(0f, -u * 0.006f), at + Offset(0f, u * 0.006f), strokeWidth = u * 0.0018f, cap = StrokeCap.Round)
}

/** A leaf drifting down from [from], swaying, over a few seconds and again. */
private fun DrawScope.leafFalling(from: Offset, u: Float, t: Float) {
    val k = (t % 3.5f) / 3.5f
    val at = from + Offset(sin(k * 9f) * u * 0.02f, k * u * 0.22f)
    rotate(sin(k * 9f) * 60f, at) {
        drawOval(Color(0xFF7C9E55).copy(alpha = 1f - k * k), Offset(at.x - u * 0.006f, at.y - u * 0.003f), Size(u * 0.012f, u * 0.006f))
    }
}

/** A few drops of water thrown up and falling back, [k] 0..1. */
private fun DrawScope.droplets(at: Offset, u: Float, k: Float) {
    if (k <= 0f || k >= 1f) return
    for (i in 0 until 4) {
        val dx = (i - 1.5f) * u * 0.008f
        val p = at + Offset(dx * (1 + k * 2), -sin(k * PI.toFloat()) * u * (0.025f + i % 2 * 0.012f))
        drawCircle(Color(0xFFDDEFF3).copy(alpha = 0.9f * (1 - k)), u * 0.0025f, p)
    }
}
