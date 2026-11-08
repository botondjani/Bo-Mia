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
import androidx.compose.ui.graphics.nativeCanvas
import app.tend.core.Accessory
import app.tend.core.Avatar
import app.tend.core.Mood
import kotlin.math.PI
import kotlin.math.sin

/** The companion using a thing in the camp: which spot, which thing, and [t] seconds since it started. */
data class InUse(val spot: Int, val thing: String, val t: Float, val walk: Float = 0f) {
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
        "grill" -> "$name grills sausages."
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

    fun seconds(thing: String): Float = when (thing) {
        "act_read", "act_run", "act_walk", "act_stretch", "act_hearts", "act_tidy", "act_cheer" -> 8f
        "guitar" -> 12f
        "canoe", "fishing_rod", "hammock", "tent", "bell_tent", "cabin" -> 9f
        else -> 7f
    }

    /** Things drawn differently while used (they move, or the companion holds them). */
    val redrawn = setOf("guitar", "fishing_rod", "swing", "canoe")
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

/**
 * Where the companion stays after using the thing at [spot]: standing beside it, on the shore
 * for the river, at the land end of the dock. Null spot, or nothing there: the usual place.
 */
fun DrawScope.restPoint(spot: Int?, placed: Map<Int, String>): Offset {
    val w = size.width
    val h = size.height
    val u = h
    val home = Offset(w * 0.395f, h * 0.86f)
    if (spot == null || placed[spot] == null) return home
    val p = SpotPositions.getValue(spot).let { Offset(it.x * w, it.y * h) }
    return when (spot) {
        7 -> Offset(w * 0.33f, h * 0.8f) // the river: on our bank
        8 -> p + Offset(u * 0.13f, -u * 0.01f) // the dock: where it meets the shore
        9 -> home // the lights overhead
        0 -> p + Offset(-u * 0.15f, u * 0.03f) // the fire: a step away from the flames
        else -> p + Offset(-u * (if (SpotPositions.getValue(spot).y < 0.7f) 0.07f else 0.09f), u * 0.012f)
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
fun DrawScope.useAnchor(use: InUse, placed: Map<Int, String>): Offset {
    val w = size.width
    val h = size.height
    val u = h
    val home = Offset(w * 0.395f, h * 0.86f)
    val p = SpotPositions[use.spot]?.let { Offset(it.x * w, it.y * h) } ?: home
    val fire = SpotPositions.getValue(0).let { Offset(it.x * w, it.y * h) }
    val seat = SpotPositions.getValue(1).let { Offset(it.x * w, it.y * h) }
    return when (use.thing) {
        // Activities done in real life, acted out where the companion stands.
        "act_run" -> Offset(w * (0.5f + 0.12f * sin(use.t * 0.9f)), h * 0.84f)
        "act_walk" -> Offset(w * (0.36f + 0.03f * sin(use.t * 0.4f)), h * (0.8f + 0.05f * sin(use.t * 0.4f)))
        "act_read", "act_stretch", "act_hearts", "act_tidy", "act_cheer" -> home
        "guitar", "campfire" -> if (use.spot < 0) (if (use.thing == "campfire") fire + Offset(-u * 0.12f, u * 0.035f) else home) else p
        "campfire", "grill", "kettle", "pizza_oven", "woodpile" ->
            if (placed[1] != null && use.thing != "woodpile") seat + Offset(0f, -u * 0.012f) else fire + Offset(-u * 0.12f, u * 0.035f)
        "fishing_rod" -> Offset(w * 0.335f, h * 0.84f)
        "wildflowers", "lantern" -> p + Offset(u * 0.055f, u * 0.004f)
        "telescope" -> p + Offset(-u * 0.05f, u * 0.004f)
        "log_bench", "picnic_table" -> p + Offset(0f, -u * 0.012f)
        "tent", "bell_tent", "cabin" -> p + Offset(0f, u * 0.01f)
        "hammock" -> p + Offset(0f, -u * 0.03f)
        "swing" -> p + Offset(-u * 0.01f, -u * 0.04f)
        "treehouse" -> p + Offset(0f, -u * 0.2f)
        "canoe" -> p + Offset(sin(use.t * 0.7f) * w * 0.04f, -u * 0.02f)
        "dock" -> p + Offset(-u * 0.05f, u * 0.0f)
        "string_lights" -> Offset(w * 0.6f, h * 0.7f)
        else -> p
    }
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

private fun DrawScope.shadowAt(p: Offset, half: Float) =
    drawOval(Color.Black.copy(alpha = 0.14f), Offset(p.x - half, p.y - half * 0.12f), Size(half * 2, half * 0.24f))

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
    val c = Ink.copy(alpha = alpha * 0.8f)
    drawPath(Path().apply { moveTo(at.x - s / 2, at.y - s / 2); lineTo(at.x + s / 2, at.y - s / 2); lineTo(at.x - s / 2, at.y + s / 2); lineTo(at.x + s / 2, at.y + s / 2) }, c, style = Stroke(s * 0.18f, cap = StrokeCap.Round))
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
) {
    val w = size.width
    val h = size.height
    val u = h
    val t = use.t
    val s = u * 0.17f * depthScale(base.y / h)
    val p = SpotPositions[use.spot]?.let { Offset(it.x * w, it.y * h) } ?: base
    // An activity just done: acorns pop out and fly up to the corner.
    if (use.spot < 0 && t < 1.6f) acornBurst(base + Offset(0f, -s * 0.6f), t / 1.6f, u)
    val fire = SpotPositions.getValue(0).let { Offset(it.x * w, it.y * h) }
    when (use.thing) {
        "guitar" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, bounce = sin(t * 7f) * 0.6f, look = 0.3f)
            guitarHeld(base + Offset(s * 0.05f, -s * 0.28f), s, lastStrum)
            rising(t, base + Offset(s * 0.45f, -s * 0.85f), s * 0.07f, every = 1.4f) { at, a -> note(at, s * 0.07f, a) }
        }
        "campfire", "grill", "kettle", "pizza_oven" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, bounce = sin(t * 2f) * 0.3f, look = 0.8f)
            val hand = base + Offset(s * 0.38f, -s * 0.4f)
            when (use.thing) {
                "campfire" -> {
                    val end = fire + Offset(-u * 0.008f, -u * 0.07f)
                    drawLine(BarkDark, hand, end, strokeWidth = u * 0.003f, cap = StrokeCap.Round)
                    val toast = (t / 6f).coerceIn(0f, 1f)
                    drawRoundRect(androidx.compose.ui.graphics.lerp(Marshmallow, Toasted, toast), end + Offset(-u * 0.011f, -u * 0.01f), Size(u * 0.022f, u * 0.02f), CornerRadius(u * 0.006f))
                }
                "grill" -> {
                    for (dx in listOf(-0.012f, 0.012f)) {
                        val c = fire + Offset(u * dx, -u * 0.07f)
                        drawRoundRect(androidx.compose.ui.graphics.lerp(Color(0xFFD9826B), Color(0xFF8E3B26), (t / 6f).coerceIn(0f, 1f)), c + Offset(-u * 0.004f, -u * 0.012f), Size(u * 0.008f, u * 0.026f), CornerRadius(u * 0.004f))
                    }
                    rising(t, fire + Offset(0f, -u * 0.09f), u * 0.01f, every = 0.7f) { at, a -> drawCircle(Color.White.copy(alpha = 0.3f * a), u * 0.008f, at) }
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
        "woodpile" -> {
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
        "wildflowers" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = -0.8f, bounce = sin(t * 1.5f) * 0.4f)
            rising(t, p + Offset(0f, -u * 0.05f), u * 0.012f, every = 1.2f) { at, a -> heart(at, u * 0.014f, a) }
        }
        "lantern" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = -0.8f)
            val glow = p + Offset(0f, -u * 0.038f)
            val pulse = 0.5f + 0.5f * sin(t * 4f)
            drawCircle(GlowWarm.copy(alpha = 0.25f + 0.2f * pulse), u * (0.03f + 0.01f * pulse), glow)
            lights += Light(glow, u * 0.14f, 1f)
        }
        "fishing_rod" -> {
            shadowAt(base, s * 0.45f)
            actor(avatar, mood, worn, base, s, look = -1f)
            val hand = base + Offset(-s * 0.3f, -s * 0.45f)
            val tip = hand + Offset(-u * 0.09f, -u * 0.09f)
            drawLine(BarkDark, hand, tip, strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            val float = Offset(w * 0.2f, h * 0.8f + sin(t * 3f) * u * 0.003f)
            drawLine(Color.White.copy(alpha = 0.7f), tip, float, strokeWidth = u * 0.0012f)
            drawCircle(Color(0xFFD64541), u * 0.005f, float)
            drawOval(Color.White.copy(alpha = 0.3f), Offset(float.x - u * 0.015f, float.y + u * 0.002f), Size(u * 0.03f, u * 0.006f))
            // A fish jumps, then it's caught.
            if (t in 4f..5.2f) {
                val k = (t - 4f) / 1.2f
                val at = float + Offset(-u * 0.04f + k * u * 0.06f, -sin(k * PI.toFloat()) * u * 0.07f)
                fish(at, u, k * 180f - 90f)
            } else if (t > 5.2f) fish(hand + Offset(-u * 0.01f, u * 0.01f), u, 80f)
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
            else rising(t, p + Offset(u * 0.03f, -u * 0.12f), u * 0.014f, every = 1.5f) { at, a -> zee(at, u * 0.016f, a) }
        }
        "hammock" -> {
            val sway = sin(t * 1.6f) * 4f
            actor(avatar, mood, worn, base + Offset(0f, s * 0.1f), s * 0.9f, rotation = -78f + sway, sleepy = true)
            rising(t, base + Offset(s * 0.2f, -s * 0.3f), u * 0.012f, every = 1.5f) { at, a -> zee(at, u * 0.014f, a) }
        }
        "swing" -> {
            // The swing goes back and forth with the companion on the seat.
            val bough = p + Offset(u * 0.01f, -u * 0.2f)
            drawLine(BarkDark, bough + Offset(-u * 0.07f, u * 0.01f), bough + Offset(u * 0.02f, 0f), strokeWidth = u * 0.008f, cap = StrokeCap.Round)
            val angle = sin(t * 2.6f) * 24f
            val pivot = bough + Offset(-u * 0.03f, 0f)
            rotate(angle, pivot) {
                val seat = pivot + Offset(0f, u * 0.16f)
                drawLine(Rope, pivot + Offset(-u * 0.022f, 0f), seat + Offset(-u * 0.022f, 0f), strokeWidth = u * 0.0025f)
                drawLine(Rope, pivot + Offset(u * 0.022f, 0f), seat + Offset(u * 0.022f, 0f), strokeWidth = u * 0.0025f)
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
            drawOval(Color.White.copy(alpha = 0.35f), Offset(base.x - u * 0.12f, base.y + u * 0.02f), Size(u * 0.24f, u * 0.01f))
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
