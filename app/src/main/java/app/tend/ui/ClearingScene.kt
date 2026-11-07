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

/**
 * Draws the clearing. [placed] maps spot ids to thing ids. With [showSpots], empty spots show a
 * soft "+" and [selected] gets a ring.
 */
fun DrawScope.drawClearing(
    avatar: Avatar,
    placed: Map<Int, String>,
    mood: Mood = Mood.HAPPY,
    worn: Set<Accessory> = emptySet(),
    night: Boolean = false,
    showSpots: Boolean = false,
    selected: Int? = null,
    paper: Float = 0f,
    ducks: Int = 0,
) {
    val w = size.width
    val h = size.height
    val u = h // things scale with the scene's height
    val horizon = h * 0.5f
    val lights = mutableListOf<Light>()
    fun at(id: Int) = SpotPositions.getValue(id).let { Offset(it.x * w, it.y * h) }

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

    // Trees around the clearing, back ones first.
    pine(Offset(w * 0.035f, h * 0.6f), h * 0.44f)
    pine(Offset(w * 0.105f, h * 0.575f), h * 0.36f)
    pine(Offset(w * 0.2f, h * 0.55f), h * 0.28f)
    pine(Offset(w * 0.44f, h * 0.6f), h * 0.42f)
    birch(Offset(w * 0.62f, h * 0.58f), h * 0.36f)
    pine(Offset(w * 0.69f, h * 0.57f), h * 0.34f)
    birch(Offset(w * 0.88f, h * 0.73f), h * 0.5f)
    broadleaf(Offset(w * 0.975f, h * 0.76f), h * 0.56f)

    // Something strung between the trees, over the camp.
    placed[9]?.let { overhead(it, Offset(w * 0.445f, h * 0.4f), Offset(w * 0.955f, h * 0.38f), u, lights) }
    if (showSpots && placed[9] == null) emptySpot(at(9), u, SpotKind.ABOVE, selected == 9)

    // The worn ground of the fire pit is always there.
    val pit = at(0)
    drawOval(Dirt.copy(alpha = 0.55f), Offset(pit.x - u * 0.2f, pit.y - u * 0.06f), Size(u * 0.4f, u * 0.13f))

    // Everything standing in the clearing, back to front, the companion among them.
    val companion = Offset(w * 0.395f, h * 0.86f)
    var companionDrawn = false
    val cs = u * 0.17f
    fun drawCompanion() {
        shadow(companion, cs * 0.5f, u)
        drawIntoCanvas {
            it.nativeCanvas.save()
            it.nativeCanvas.translate(companion.x - cs / 2, companion.y - cs * 0.95f)
            CharacterPainter.draw(it.nativeCanvas, avatar, mood, cs, accessories = worn)
            it.nativeCanvas.restore()
        }
    }
    val ground = Clearing.spots.filter { it.kind != SpotKind.ABOVE }.sortedBy { SpotPositions.getValue(it.id).y }
    for (spot in ground) {
        val p = at(spot.id)
        if (!companionDrawn && p.y > companion.y) { drawCompanion(); companionDrawn = true }
        if (spot.kind == SpotKind.FIRE) firePitStones(p, u)
        val thing = placed[spot.id]
        if (thing != null) {
            if (spot.id != 7) shadow(p, u * thingWidth(thing) * 0.5f, u)
            drawThing(thing, p, u, lights)
        } else if (showSpots) emptySpot(p, u, spot.kind, selected == spot.id)
    }
    if (!companionDrawn) drawCompanion()

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
            it.nativeCanvas.translate(companion.x - cs / 2, companion.y - cs * 0.95f)
            CharacterPainter.draw(it.nativeCanvas, avatar, mood, cs, accessories = worn)
            it.nativeCanvas.restore()
        }
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
    val rnd = java.util.Random(5L)
    repeat(46) {
        val t = rnd.nextFloat()
        val y = h * (0.6f + 0.4f * t)
        val x = w * (0.285f + 0.06f * t * t + 0.035f * rnd.nextFloat())
        val r = h * (0.006f + 0.008f * t * rnd.nextFloat())
        drawOval(if (rnd.nextBoolean()) Pebble else PebbleDark, Offset(x - r, y - r * 0.6f), Size(r * 2, r * 1.2f))
    }
}

private fun DrawScope.pine(base: Offset, height: Float) {
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

private fun DrawScope.birch(base: Offset, height: Float) {
    val trunkW = height * 0.03f
    drawRect(Color(0xFFEFEBE2), Offset(base.x - trunkW / 2, base.y - height), Size(trunkW, height))
    val rnd = java.util.Random((base.x * 31).toLong())
    repeat(9) {
        val y = base.y - height * rnd.nextFloat() * 0.95f
        drawRect(Color(0xFF3C3A35), Offset(base.x - trunkW / 2, y), Size(trunkW * (0.3f + 0.5f * rnd.nextFloat()), height * 0.008f))
    }
    for ((dx, dy, r) in listOf(Triple(-0.06f, 0.9f, 0.1f), Triple(0.05f, 0.95f, 0.11f), Triple(0f, 1.02f, 0.1f), Triple(-0.02f, 0.82f, 0.08f)))
        drawCircle(if (dy > 0.9f) LeafMid else LeafDark, height * r, Offset(base.x + height * dx, base.y - height * dy))
    drawCircle(LeafLit.copy(alpha = 0.5f), height * 0.06f, Offset(base.x - height * 0.06f, base.y - height * 0.97f))
}

private class Blob(val dx: Float, val dy: Float, val r: Float, val color: Color)

private fun DrawScope.broadleaf(base: Offset, height: Float) {
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

private fun DrawScope.shadow(at: Offset, halfWidth: Float, u: Float) =
    drawOval(Color.Black.copy(alpha = 0.16f), Offset(at.x - halfWidth, at.y - u * 0.012f), Size(halfWidth * 2, u * 0.026f))

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
    "cabin", "workshop" -> 0.22f
    "bell_tent", "tent" -> 0.16f
    "picnic_table", "log_bench", "camp_chairs" -> 0.11f
    "canoe" -> 0.16f
    "campfire", "kettle", "grill", "pizza_oven" -> 0.1f
    "hammock", "swing", "bike", "treehouse" -> 0.08f
    else -> 0.04f
}

private fun DrawScope.emptySpot(p: Offset, u: Float, kind: SpotKind, selected: Boolean) {
    val r = u * if (kind == SpotKind.SMALL) 0.02f else 0.034f
    val c = p + Offset(0f, -r * 1.2f)
    drawCircle(Color.White.copy(alpha = if (selected) 0.95f else 0.6f), r, c)
    drawCircle(if (selected) Brand.Moss else Color.White, r, c, style = Stroke(u * if (selected) 0.006f else 0.003f))
    drawLine(Brand.Moss, c + Offset(-r * 0.45f, 0f), c + Offset(r * 0.45f, 0f), strokeWidth = u * 0.005f, cap = StrokeCap.Round)
    drawLine(Brand.Moss, c + Offset(0f, -r * 0.45f), c + Offset(0f, r * 0.45f), strokeWidth = u * 0.005f, cap = StrokeCap.Round)
}

// ---- Things ----

/** Strung between two trees: bunting or string lights, hanging in a soft curve. */
private fun DrawScope.overhead(id: String, a: Offset, b: Offset, u: Float, lights: MutableList<Light>) {
    val sag = u * 0.07f
    fun point(t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t + sag * 4 * t * (1 - t))
    drawPath(Path().apply {
        moveTo(a.x, a.y); quadraticTo((a.x + b.x) / 2, (a.y + b.y) / 2 + sag * 2, b.x, b.y)
    }, BarkDark.copy(alpha = 0.7f), style = Stroke(u * 0.003f))
    when (id) {
        "bunting" -> {
            val colors = listOf(Color(0xFFE07A5F), Color(0xFFF2CC8F), Color(0xFF81B29A), Color(0xFF3D405B), Color(0xFFE9C46A), Color(0xFFB5838D))
            for (i in 0 until 16) {
                val p = point((i + 0.5f) / 16f)
                drawPath(Path().apply { moveTo(p.x - u * 0.012f, p.y); lineTo(p.x + u * 0.012f, p.y); lineTo(p.x, p.y + u * 0.032f); close() }, colors[i % colors.size])
            }
        }
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
        "mushrooms" -> for ((dx, s) in listOf(-0.012f to 1f, 0.01f to 0.7f, 0.024f to 0.5f)) {
            val b = p + Offset(u * dx, 0f)
            drawRoundRect(Color(0xFFF1E6D3), b + Offset(-u * 0.004f * s, -u * 0.022f * s), Size(u * 0.008f * s, u * 0.022f * s), CornerRadius(u * 0.004f))
            drawArc(Color(0xFFC0502F), 180f, 180f, true, b + Offset(-u * 0.015f * s, -u * 0.034f * s), Size(u * 0.03f * s, u * 0.026f * s))
            drawCircle(Color(0xFFF7EBD8), u * 0.0028f * s, b + Offset(-u * 0.005f * s, -u * 0.027f * s))
            drawCircle(Color(0xFFF7EBD8), u * 0.0022f * s, b + Offset(u * 0.005f * s, -u * 0.03f * s))
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
        "backpack" -> {
            drawRoundRect(Olive, p + Offset(-u * 0.016f, -u * 0.042f), Size(u * 0.032f, u * 0.042f), CornerRadius(u * 0.01f))
            drawRoundRect(OliveDark, p + Offset(-u * 0.016f, -u * 0.044f), Size(u * 0.032f, u * 0.014f), CornerRadius(u * 0.008f))
            drawRoundRect(OliveDark, p + Offset(-u * 0.01f, -u * 0.02f), Size(u * 0.02f, u * 0.014f), CornerRadius(u * 0.003f))
            drawLine(BarkDark, p + Offset(-u * 0.004f, -u * 0.033f), p + Offset(-u * 0.004f, -u * 0.026f), strokeWidth = u * 0.002f)
        }
        "guitar" -> rotate(-14f, p) {
            drawLine(BarkDark, p + Offset(0f, -u * 0.04f), p + Offset(0f, -u * 0.085f), strokeWidth = u * 0.006f, cap = StrokeCap.Round)
            drawRoundRect(BarkDark, p + Offset(-u * 0.005f, -u * 0.095f), Size(u * 0.01f, u * 0.014f), CornerRadius(u * 0.003f))
            drawOval(Color(0xFFC98B4A), p + Offset(-u * 0.018f, -u * 0.035f), Size(u * 0.036f, u * 0.034f))
            drawOval(Color(0xFFC98B4A), p + Offset(-u * 0.014f, -u * 0.054f), Size(u * 0.028f, u * 0.026f))
            drawCircle(Color(0xFF3A2414), u * 0.006f, p + Offset(0f, -u * 0.033f))
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
        "grill" -> {
            drawLine(Metal, p + Offset(-u * 0.022f, 0f), p + Offset(-u * 0.01f, -u * 0.035f), strokeWidth = u * 0.004f)
            drawLine(Metal, p + Offset(u * 0.022f, 0f), p + Offset(u * 0.01f, -u * 0.035f), strokeWidth = u * 0.004f)
            drawArc(Metal, 0f, 180f, true, p + Offset(-u * 0.032f, -u * 0.064f), Size(u * 0.064f, u * 0.056f))
            drawArc(Color(0xFF4A5056), 180f, 180f, true, p + Offset(-u * 0.032f, -u * 0.082f), Size(u * 0.064f, u * 0.046f))
            drawLine(Color(0xFF8A9096), p + Offset(-u * 0.036f, -u * 0.036f), p + Offset(u * 0.036f, -u * 0.036f), strokeWidth = u * 0.003f)
            for ((dx, dy, r) in listOf(Triple(0.01f, 0.1f, 0.01f), Triple(0.02f, 0.125f, 0.014f), Triple(0.012f, 0.155f, 0.017f)))
                drawCircle(Color.White.copy(alpha = 0.22f), u * r, p + Offset(u * dx, -u * dy))
            lights += Light(p + Offset(0f, -u * 0.04f), u * 0.06f, 0.5f)
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
        "camp_chairs" -> for ((dx, cloth) in listOf(-0.025f to Color(0xFF2A6F6B), 0.025f to Color(0xFFB5523B))) {
            val b = p + Offset(u * dx, 0f)
            drawLine(Wood, b + Offset(-u * 0.016f, 0f), b + Offset(u * 0.012f, -u * 0.03f), strokeWidth = u * 0.003f)
            drawLine(Wood, b + Offset(u * 0.016f, 0f), b + Offset(-u * 0.012f, -u * 0.03f), strokeWidth = u * 0.003f)
            drawRect(CanvasCloth, b + Offset(-u * 0.018f, -u * 0.034f), Size(u * 0.036f, u * 0.008f))
            drawRect(cloth, b + Offset(-u * 0.016f, -u * 0.064f), Size(u * 0.032f, u * 0.026f))
            drawLine(Wood, b + Offset(-u * 0.018f, -u * 0.066f), b + Offset(-u * 0.018f, -u * 0.03f), strokeWidth = u * 0.003f)
            drawLine(Wood, b + Offset(u * 0.018f, -u * 0.066f), b + Offset(u * 0.018f, -u * 0.03f), strokeWidth = u * 0.003f)
        }
        "picnic_table" -> {
            drawRect(Wood, p + Offset(-u * 0.055f, -u * 0.05f), Size(u * 0.11f, u * 0.01f))
            drawRect(WoodLight, p + Offset(-u * 0.055f, -u * 0.05f), Size(u * 0.11f, u * 0.003f))
            for (dx in listOf(-0.04f, 0.035f)) drawRect(Bark, p + Offset(u * dx, -u * 0.04f), Size(u * 0.006f, u * 0.04f))
            drawRect(Wood, p + Offset(-u * 0.065f, -u * 0.022f), Size(u * 0.13f, u * 0.007f))
            // Two enamel mugs and a flask.
            drawRoundRect(Color(0xFF2A6F6B), p + Offset(-u * 0.03f, -u * 0.063f), Size(u * 0.01f, u * 0.013f), CornerRadius(u * 0.002f))
            drawRoundRect(Color(0xFFB5523B), p + Offset(-u * 0.012f, -u * 0.063f), Size(u * 0.01f, u * 0.013f), CornerRadius(u * 0.002f))
            drawRoundRect(Color(0xFF8A9096), p + Offset(u * 0.015f, -u * 0.074f), Size(u * 0.01f, u * 0.024f), CornerRadius(u * 0.003f))
        }
        "tent" -> {
            val tw = u * 0.15f
            val th = u * 0.11f
            drawPath(Path().apply { moveTo(p.x - tw / 2, p.y); lineTo(p.x - tw * 0.08f, p.y - th); lineTo(p.x + tw * 0.38f, p.y - th * 0.96f); lineTo(p.x + tw / 2 + tw * 0.08f, p.y); close() }, OliveDark)
            drawPath(Path().apply { moveTo(p.x - tw / 2, p.y); lineTo(p.x - tw * 0.08f, p.y - th); lineTo(p.x + tw * 0.34f, p.y); close() }, Olive)
            drawPath(Path().apply { moveTo(p.x - tw * 0.08f, p.y - th); lineTo(p.x + tw * 0.05f, p.y); lineTo(p.x - tw * 0.2f, p.y); close() }, Color(0xFF2E2A1E))
            drawLine(Wood, p + Offset(-tw * 0.08f, -th), p + Offset(-tw * 0.08f, 0f), strokeWidth = u * 0.003f)
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
        "workshop" -> {
            val sw = u * 0.18f
            val sh = u * 0.12f
            val l = p.x - sw / 2
            drawRect(WoodLight, Offset(l, p.y - sh), Size(sw, sh))
            for (k in 1 until 8) drawLine(Wood.copy(alpha = 0.5f), Offset(l + sw * k / 8f, p.y - sh), Offset(l + sw * k / 8f, p.y), strokeWidth = u * 0.0015f)
            drawPath(Path().apply { moveTo(l - sw * 0.06f, p.y - sh); lineTo(l + sw * 1.06f, p.y - sh - u * 0.04f); lineTo(l + sw * 1.06f, p.y - sh - u * 0.03f); lineTo(l - sw * 0.06f, p.y - sh + u * 0.012f); close() }, Color(0xFF3F4A45))
            drawRect(Color(0xFF2A1F17), Offset(l + sw * 0.1f, p.y - sh * 0.78f), Size(sw * 0.42f, sh * 0.78f))
            drawLine(Color(0xFFBFC6CC), Offset(l + sw * 0.2f, p.y - sh * 0.64f), Offset(l + sw * 0.2f, p.y - sh * 0.3f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            drawRect(Color(0xFFBFC6CC), Offset(l + sw * 0.32f, p.y - sh * 0.62f), Size(sw * 0.13f, sh * 0.08f))
            drawRoundRect(Glow.copy(alpha = 0.85f), Offset(l + sw * 0.64f, p.y - sh * 0.72f), Size(sw * 0.22f, sh * 0.3f), CornerRadius(u * 0.003f))
            lights += Light(Offset(l + sw * 0.75f, p.y - sh * 0.57f), u * 0.07f, 0.7f)
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
            val bough = p + Offset(u * 0.01f, -u * 0.2f)
            drawLine(BarkDark, bough + Offset(-u * 0.07f, u * 0.01f), bough + Offset(u * 0.02f, 0f), strokeWidth = u * 0.008f, cap = StrokeCap.Round)
            drawLine(Color(0xFFD8C7A6), bough + Offset(-u * 0.055f, 0f), p + Offset(-u * 0.028f, -u * 0.04f), strokeWidth = u * 0.0025f)
            drawLine(Color(0xFFD8C7A6), bough + Offset(-u * 0.01f, 0f), p + Offset(u * 0.012f, -u * 0.04f), strokeWidth = u * 0.0025f)
            drawRoundRect(Wood, p + Offset(-u * 0.034f, -u * 0.044f), Size(u * 0.052f, u * 0.009f), CornerRadius(u * 0.003f))
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
        "bike" -> {
            val r = u * 0.022f
            val c = p + Offset(0f, -r)
            val back = c + Offset(-r * 1.25f, 0f)
            val front = c + Offset(r * 1.25f, 0f)
            drawCircle(Metal, r, back, style = Stroke(r * 0.15f))
            drawCircle(Metal, r, front, style = Stroke(r * 0.15f))
            val seat = c + Offset(-r * 0.45f, -r * 1.1f)
            val bar = c + Offset(r * 0.8f, -r * 1.25f)
            val crank = c + Offset(-r * 0.1f, 0f)
            for ((x1, x2) in listOf(back to crank, crank to seat, seat to back, crank to bar, seat to bar, bar to front))
                drawLine(Color(0xFF2A6F6B), x1, x2, strokeWidth = r * 0.14f, cap = StrokeCap.Round)
            drawLine(BarkDark, seat + Offset(-r * 0.25f, -r * 0.1f), seat + Offset(r * 0.25f, -r * 0.1f), strokeWidth = r * 0.2f, cap = StrokeCap.Round)
        }
        "treehouse" -> {
            val floor = p + Offset(0f, -u * 0.2f)
            drawLine(Bark, p + Offset(-u * 0.045f, 0f), floor + Offset(-u * 0.045f, 0f), strokeWidth = u * 0.005f)
            drawLine(Bark, p + Offset(u * 0.045f, 0f), floor + Offset(u * 0.045f, 0f), strokeWidth = u * 0.005f)
            for (k in 0 until 6) drawLine(Wood, p + Offset(-u * 0.03f, -u * 0.02f - k * u * 0.03f), p + Offset(-u * 0.012f, -u * 0.02f - k * u * 0.03f), strokeWidth = u * 0.004f)
            drawRect(Wood, floor + Offset(-u * 0.065f, 0f), Size(u * 0.13f, u * 0.012f))
            drawRect(WoodLight, floor + Offset(-u * 0.05f, -u * 0.065f), Size(u * 0.1f, u * 0.065f))
            drawPath(Path().apply { moveTo(floor.x - u * 0.062f, floor.y - u * 0.065f); lineTo(floor.x, floor.y - u * 0.11f); lineTo(floor.x + u * 0.062f, floor.y - u * 0.065f); close() }, Color(0xFF3A4A3E))
            drawRect(Glow.copy(alpha = 0.85f), floor + Offset(-u * 0.014f, -u * 0.05f), Size(u * 0.028f, u * 0.03f))
            lights += Light(floor + Offset(0f, -u * 0.035f), u * 0.05f, 0.6f)
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
        "boat" -> {
            drawOval(Color.White.copy(alpha = 0.25f), p + Offset(-u * 0.11f, -u * 0.006f), Size(u * 0.22f, u * 0.014f))
            drawPath(Path().apply {
                moveTo(p.x - u * 0.1f, p.y - u * 0.036f); lineTo(p.x + u * 0.1f, p.y - u * 0.036f)
                quadraticTo(p.x + u * 0.085f, p.y, p.x + u * 0.05f, p.y)
                lineTo(p.x - u * 0.075f, p.y); quadraticTo(p.x - u * 0.095f, p.y - u * 0.01f, p.x - u * 0.1f, p.y - u * 0.036f); close()
            }, Color(0xFFE9E1CF))
            drawRect(Color(0xFF2A6F6B), p + Offset(-u * 0.098f, -u * 0.036f), Size(u * 0.196f, u * 0.008f))
            drawLine(Wood, p + Offset(-u * 0.04f, -u * 0.05f), p + Offset(-u * 0.12f, u * 0.01f), strokeWidth = u * 0.003f, cap = StrokeCap.Round)
            drawLine(Wood, p + Offset(u * 0.02f, -u * 0.05f), p + Offset(u * 0.1f, u * 0.012f), strokeWidth = u * 0.003f, cap = StrokeCap.Round)
        }
        "dock" -> {
            // Planks out from our bank into the river, on posts.
            val start = p + Offset(u * 0.12f, -u * 0.02f)
            val end = p + Offset(-u * 0.06f, u * 0.01f)
            for (k in 0 until 9) {
                val t = k / 9f
                val x = start.x + (end.x - start.x) * t
                val y = start.y + (end.y - start.y) * t
                drawRect(if (k % 2 == 0) WoodLight else Wood, Offset(x - u * 0.011f, y - u * 0.012f), Size(u * 0.022f, u * 0.014f))
            }
            for (t in listOf(0.2f, 0.55f, 0.9f)) {
                val x = start.x + (end.x - start.x) * t
                val y = start.y + (end.y - start.y) * t
                drawRect(BarkDark, Offset(x - u * 0.003f, y), Size(u * 0.006f, u * 0.03f))
            }
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
    avatar: Avatar, placed: Map<Int, String>, worn: Set<Accessory>, widthPx: Int, heightPx: Int, night: Boolean = false, paper: Boolean = false, ducks: Int = 0,
): android.graphics.Bitmap {
    val image = androidx.compose.ui.graphics.ImageBitmap(widthPx, heightPx)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    val size = Size(widthPx.toFloat(), heightPx.toFloat())
    canvas.clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset.Zero, size), CornerRadius(widthPx * 0.05f))) })
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, canvas, size,
    ) { drawClearing(avatar, placed, worn = worn, night = night, paper = if (paper) 1f else 0f, ducks = ducks) }
    return image.asAndroidBitmap()
}

/** Rough size of each thing (width, height) in scene heights, to fit it into an icon. */
private fun thingSize(id: String): Pair<Float, Float> = when (id) {
    "cabin" -> 0.25f to 0.24f
    "workshop" -> 0.2f to 0.17f
    "bell_tent" -> 0.2f to 0.18f
    "tent" -> 0.17f to 0.12f
    "treehouse" -> 0.14f to 0.32f
    "swing" -> 0.1f to 0.21f
    "hammock" -> 0.15f to 0.1f
    "bike" -> 0.1f to 0.05f
    "canoe" -> 0.21f to 0.05f
    "boat" -> 0.24f to 0.07f
    "dock" -> 0.2f to 0.05f
    "kettle" -> 0.11f to 0.13f
    "grill" -> 0.08f to 0.11f
    "campfire" -> 0.08f to 0.09f
    "pizza_oven" -> 0.11f to 0.13f
    "picnic_table" -> 0.13f to 0.08f
    "camp_chairs" -> 0.09f to 0.07f
    "log_bench" -> 0.12f to 0.035f
    "guitar" -> 0.04f to 0.1f
    else -> 0.06f to 0.05f
}

/** One thing as an icon, filling this canvas, standing on its bottom. */
fun DrawScope.drawThingIcon(id: String) {
    val s = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    if (id == "bunting" || id == "string_lights") {
        overhead(id, Offset(c.x - s * 0.45f, c.y - s * 0.15f), Offset(c.x + s * 0.45f, c.y - s * 0.15f), s * 0.9f, mutableListOf())
        return
    }
    val (tw, th) = thingSize(id)
    val u = minOf(s * 0.85f / tw, s * 0.8f / th)
    drawThing(id, Offset(c.x, c.y + s * 0.4f), u)
}
