package app.tend.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asAndroidBitmap
import app.tend.core.Accessory
import app.tend.core.Avatar
import app.tend.core.Clearing
import app.tend.core.Mood
import app.tend.core.SpotKind

private val Bark = Color(0xFF6B4A33)
private val BarkDark = Color(0xFF4E3424)
private val Plank = Color(0xFFB98059)
private val PlankLight = Color(0xFFD9A876)
private val Leaf = Color(0xFF3F7A57)
private val LeafDark = Color(0xFF2F5D45)
private val Water = Color(0xFF7CB9D6)
private val WaterDeep = Color(0xFF5E9FC2)
private val Sand = Color(0xFFE6D6B0)
private val Metal = Color(0xFF3A3F44)

/**
 * Where each spot is, as a fraction of the scene (x, y = where the thing stands). Big spots first,
 * then the 20 small ones along the bank, under the trees and in the meadow.
 */
val SpotPositions: Map<Int, Offset> = buildMap {
    put(0, Offset(0.56f, 0.66f)) // fire pit
    put(1, Offset(0.14f, 0.63f)) // under the left tree
    put(2, Offset(0.36f, 0.57f)) // under the middle tree
    put(3, Offset(0.9f, 0.62f)) // under the right tree
    put(4, Offset(0.24f, 0.77f)) // bank, left
    put(5, Offset(0.78f, 0.77f)) // bank, right
    put(6, Offset(0.36f, 0.91f)) // water, left
    put(7, Offset(0.74f, 0.92f)) // water, right
    put(8, Offset(0.62f, 0.45f)) // back, middle
    put(9, Offset(0.84f, 0.45f)) // back, right
    val small = listOf(
        0.05f to 0.72f, 0.1f to 0.5f, 0.2f to 0.68f, 0.27f to 0.6f, 0.31f to 0.72f,
        0.44f to 0.75f, 0.68f to 0.52f, 0.5f to 0.47f, 0.42f to 0.45f, 0.66f to 0.73f,
        0.7f to 0.6f, 0.74f to 0.47f, 0.88f to 0.72f, 0.96f to 0.5f, 0.8f to 0.57f,
        0.03f to 0.57f, 0.53f to 0.77f, 0.94f to 0.74f, 0.25f to 0.5f, 0.66f to 0.66f,
    )
    small.forEachIndexed { i, (x, y) -> put(10 + i, Offset(x, y)) }
}

/**
 * The riverside clearing: a meadow among trees, a river in front. [placed] maps spot ids to thing
 * ids. With [showSpots], empty spots show a soft "+"; [selected] gets a ring.
 */
fun DrawScope.drawClearing(
    avatar: Avatar,
    placed: Map<Int, String>,
    mood: Mood = Mood.HAPPY,
    worn: Set<Accessory> = emptySet(),
    night: Boolean = false,
    showSpots: Boolean = false,
    selected: Int? = null,
) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.38f
    drawSky(night, horizon)

    // Far forest along the horizon.
    val far = if (night) Color(0xFF1E3A2E) else Color(0xFF5E9C78)
    var x = -w * 0.02f
    var i = 0
    while (x < w * 1.05f) {
        val th = h * (0.1f + 0.05f * ((i * 37) % 5) / 4f)
        drawPath(Path().apply { moveTo(x - w * 0.04f, horizon + 2); lineTo(x, horizon - th); lineTo(x + w * 0.04f, horizon + 2); close() }, far)
        x += w * 0.055f; i++
    }

    // Meadow down to the bank.
    val grass = if (night) Color(0xFF2F5F47) else Color(0xFF7CC294)
    val grassDeep = if (night) Color(0xFF285240) else Color(0xFF6DB58A)
    drawRect(Brush.verticalGradient(listOf(grass, grassDeep), startY = horizon, endY = h * 0.8f), Offset(0f, horizon), Size(w, h - horizon))

    // Things are drawn from the back to the front, so nearer ones overlap farther ones.
    fun at(id: Int) = SpotPositions.getValue(id).let { Offset(it.x * w, it.y * h) }
    val unit = h
    val order = Clearing.spots.sortedBy { SpotPositions.getValue(it.id).y }
    val treeSpots = listOf(1, 2, 3)

    // The three big trees always stand; things under them come after their trunks.
    fun tree(id: Int, scale: Float) {
        val p = at(id)
        val trunkX = p.x + w * 0.035f
        drawRect(Bark, Offset(trunkX - unit * 0.018f, p.y - unit * 0.3f * scale), Size(unit * 0.036f, unit * 0.3f * scale))
        val crown = if (night) Color(0xFF244A38) else Leaf
        val crownDark = if (night) Color(0xFF1C3B2C) else LeafDark
        drawCircle(crownDark, unit * 0.13f * scale, Offset(trunkX, p.y - unit * 0.36f * scale))
        drawCircle(crown, unit * 0.11f * scale, Offset(trunkX - unit * 0.07f * scale, p.y - unit * 0.3f * scale))
        drawCircle(crown, unit * 0.1f * scale, Offset(trunkX + unit * 0.08f * scale, p.y - unit * 0.31f * scale))
        drawCircle(crown, unit * 0.09f * scale, Offset(trunkX, p.y - unit * 0.45f * scale))
    }

    // The river along the bottom, with a sandy bank.
    val bankY = h * 0.8f
    fun river() {
        drawPath(Path().apply {
            moveTo(0f, bankY - h * 0.015f)
            cubicTo(w * 0.3f, bankY + h * 0.02f, w * 0.6f, bankY - h * 0.04f, w, bankY - h * 0.005f)
            lineTo(w, h); lineTo(0f, h); close()
        }, Sand)
        val waterTop = bankY + h * 0.03f
        drawPath(Path().apply {
            moveTo(0f, waterTop)
            cubicTo(w * 0.3f, waterTop + h * 0.02f, w * 0.6f, waterTop - h * 0.03f, w, waterTop + h * 0.01f)
            lineTo(w, h); lineTo(0f, h); close()
        }, Brush.verticalGradient(listOf(if (night) Color(0xFF2E5770) else Water, if (night) Color(0xFF203F55) else WaterDeep), startY = waterTop, endY = h))
        val ripple = Color.White.copy(alpha = if (night) 0.18f else 0.45f)
        for ((rx, ry, rl) in listOf(Triple(0.12f, 0.9f, 0.08f), Triple(0.5f, 0.86f, 0.06f), Triple(0.6f, 0.95f, 0.1f), Triple(0.9f, 0.88f, 0.07f), Triple(0.25f, 0.97f, 0.05f)))
            drawLine(ripple, Offset(w * rx, h * ry), Offset(w * (rx + rl), h * ry), strokeWidth = h * 0.006f, cap = StrokeCap.Round)
    }

    var riverDrawn = false
    var companionDrawn = false
    val companionAt = Offset(w * 0.46f, h * 0.64f)
    fun companion() {
        val cs = h * 0.22f
        drawIntoCanvas {
            it.nativeCanvas.save()
            it.nativeCanvas.translate(companionAt.x - cs / 2, companionAt.y - cs * 0.92f)
            CharacterPainter.draw(it.nativeCanvas, avatar, mood, cs, accessories = worn)
            it.nativeCanvas.restore()
        }
    }

    for (spot in order) {
        val p = at(spot.id)
        if (!riverDrawn && p.y > bankY - h * 0.05f) { river(); riverDrawn = true }
        if (!companionDrawn && p.y > companionAt.y) { companion(); companionDrawn = true }
        if (spot.id in treeSpots) tree(spot.id, if (spot.id == 2) 0.7f else 0.82f)
        val thing = placed[spot.id]
        // Small things are drawn a bit larger in the scene, so they read at a glance.
        if (thing != null) drawThing(thing, p, unit * if (spot.kind == SpotKind.SMALL) 1.5f else 1f, night)
        else if (showSpots) emptySpot(p, unit, spot.kind)
        if (showSpots && selected == spot.id) drawCircle(Brand.Sunny, unit * if (spot.kind == SpotKind.SMALL) 0.045f else 0.08f, p + Offset(0f, -unit * 0.02f), style = Stroke(unit * 0.008f))
    }
    if (!riverDrawn) river()
    if (!companionDrawn) companion()
}

/** A soft "+" where something could go. */
private fun DrawScope.emptySpot(p: Offset, unit: Float, kind: SpotKind) {
    val r = unit * if (kind == SpotKind.SMALL) 0.022f else 0.04f
    val c = p + Offset(0f, -r)
    drawCircle(Color.White.copy(alpha = 0.55f), r, c)
    drawCircle(Color.White, r, c, style = Stroke(unit * 0.004f))
    drawLine(Brand.Moss, c + Offset(-r * 0.45f, 0f), c + Offset(r * 0.45f, 0f), strokeWidth = unit * 0.006f, cap = StrokeCap.Round)
    drawLine(Brand.Moss, c + Offset(0f, -r * 0.45f), c + Offset(0f, r * 0.45f), strokeWidth = unit * 0.006f, cap = StrokeCap.Round)
}

/** Draws one thing standing at [p] (its base); [u] is the scene height, so things scale with it. */
fun DrawScope.drawThing(id: String, p: Offset, u: Float, night: Boolean = false) {
    when (id) {
        "flowers" -> for ((dx, c) in listOf(-0.018f to Brand.Coral, 0f to Brand.Sunny, 0.018f to Color(0xFFB79AE0), 0.008f to Color.White)) {
            val top = p + Offset(u * dx, -u * (0.03f + if (dx == 0.008f) 0.012f else 0f))
            drawLine(LeafDark, p + Offset(u * dx * 0.6f, 0f), top, strokeWidth = u * 0.004f)
            drawCircle(c, u * 0.009f, top)
        }
        "mushrooms" -> for ((dx, s) in listOf(-0.012f to 1f, 0.012f to 0.75f)) {
            val b = p + Offset(u * dx, 0f)
            drawRoundRect(Color(0xFFF4EBDD), b + Offset(-u * 0.005f * s, -u * 0.022f * s), Size(u * 0.01f * s, u * 0.022f * s), CornerRadius(u * 0.004f))
            drawArc(Brand.Coral, 180f, 180f, true, b + Offset(-u * 0.016f * s, -u * 0.036f * s), Size(u * 0.032f * s, u * 0.028f * s))
            drawCircle(Color.White, u * 0.003f * s, b + Offset(-u * 0.005f * s, -u * 0.028f * s))
        }
        "stones" -> {
            drawOval(Color(0xFF9AA3A8), p + Offset(-u * 0.026f, -u * 0.018f), Size(u * 0.034f, u * 0.02f))
            drawOval(Color(0xFFB7BEC2), p + Offset(-u * 0.002f, -u * 0.014f), Size(u * 0.026f, u * 0.016f))
            drawOval(Color(0xFF7F888D), p + Offset(-u * 0.012f, -u * 0.03f), Size(u * 0.02f, u * 0.014f))
        }
        "sunflowers" -> {
            drawPlant(app.tend.core.Plant.SUNFLOWER, p + Offset(-u * 0.012f, 0f), u * 0.07f)
            drawPlant(app.tend.core.Plant.SUNFLOWER, p + Offset(u * 0.014f, 0f), u * 0.055f)
        }
        "lantern" -> {
            drawLine(BarkDark, p, p + Offset(0f, -u * 0.07f), strokeWidth = u * 0.006f)
            drawLine(BarkDark, p + Offset(0f, -u * 0.07f), p + Offset(u * 0.016f, -u * 0.07f), strokeWidth = u * 0.005f)
            val c = p + Offset(u * 0.016f, -u * 0.055f)
            drawCircle(Brand.Sunny.copy(alpha = if (night) 0.45f else 0.25f), u * 0.03f, c)
            drawRoundRect(Brand.Sunny, c + Offset(-u * 0.007f, -u * 0.01f), Size(u * 0.014f, u * 0.02f), CornerRadius(u * 0.004f))
        }
        "fireflies" -> for ((dx, dy) in listOf(-0.02f to 0.04f, 0.01f to 0.06f, 0.025f to 0.03f, -0.005f to 0.085f)) {
            val c = p + Offset(u * dx, -u * dy)
            drawCircle(Color(0xFFF7F3A1).copy(alpha = if (night) 0.5f else 0.25f), u * 0.012f, c)
            drawCircle(Color(0xFFFFF7B0), u * 0.004f, c)
        }
        "campfire" -> campfireAt(p, u * 0.06f, night)
        "grill" -> {
            drawLine(Metal, p + Offset(-u * 0.025f, 0f), p + Offset(-u * 0.012f, -u * 0.04f), strokeWidth = u * 0.005f)
            drawLine(Metal, p + Offset(u * 0.025f, 0f), p + Offset(u * 0.012f, -u * 0.04f), strokeWidth = u * 0.005f)
            drawArc(Metal, 0f, 180f, true, p + Offset(-u * 0.035f, -u * 0.07f), Size(u * 0.07f, u * 0.06f))
            drawArc(Color(0xFF52585E), 180f, 180f, true, p + Offset(-u * 0.035f, -u * 0.09f), Size(u * 0.07f, u * 0.05f))
            drawLine(Metal, p + Offset(-u * 0.04f, -u * 0.04f), p + Offset(u * 0.04f, -u * 0.04f), strokeWidth = u * 0.004f)
            drawCircle(Color.White.copy(alpha = 0.35f), u * 0.012f, p + Offset(u * 0.01f, -u * 0.11f))
            drawCircle(Color.White.copy(alpha = 0.25f), u * 0.016f, p + Offset(u * 0.022f, -u * 0.135f))
        }
        "pizza_oven" -> {
            drawRect(Color(0xFF8C8C8C), p + Offset(-u * 0.055f, -u * 0.035f), Size(u * 0.11f, u * 0.035f))
            drawArc(Color(0xFFC8693F), 180f, 180f, true, p + Offset(-u * 0.05f, -u * 0.1f), Size(u * 0.1f, u * 0.13f))
            drawArc(if (night) Brand.Sunny else Color(0xFF3A2A1E), 180f, 180f, true, p + Offset(-u * 0.022f, -u * 0.066f), Size(u * 0.044f, u * 0.06f))
            drawRect(Color(0xFF8C8C8C), p + Offset(u * 0.022f, -u * 0.13f), Size(u * 0.014f, u * 0.04f))
        }
        "log_bench" -> {
            drawRoundRect(Bark, p + Offset(-u * 0.05f, -u * 0.035f), Size(u * 0.1f, u * 0.022f), CornerRadius(u * 0.011f))
            drawCircle(PlankLight, u * 0.011f, p + Offset(u * 0.05f, -u * 0.024f))
            drawRect(BarkDark, p + Offset(-u * 0.04f, -u * 0.014f), Size(u * 0.014f, u * 0.014f))
            drawRect(BarkDark, p + Offset(u * 0.026f, -u * 0.014f), Size(u * 0.014f, u * 0.014f))
        }
        "swing" -> {
            val branch = p + Offset(u * 0.035f, -u * 0.17f)
            drawLine(BarkDark, branch + Offset(-u * 0.06f, 0f), branch + Offset(u * 0.02f, 0f), strokeWidth = u * 0.01f, cap = StrokeCap.Round)
            drawLine(Color(0xFFD8C7A6), branch + Offset(-u * 0.045f, 0f), p + Offset(-u * 0.025f, -u * 0.035f), strokeWidth = u * 0.003f)
            drawLine(Color(0xFFD8C7A6), branch + Offset(-u * 0.005f, 0f), p + Offset(u * 0.015f, -u * 0.035f), strokeWidth = u * 0.003f)
            drawRoundRect(Plank, p + Offset(-u * 0.032f, -u * 0.04f), Size(u * 0.054f, u * 0.01f), CornerRadius(u * 0.003f))
        }
        "hammock" -> {
            val a = p + Offset(u * 0.035f, -u * 0.09f) // on the trunk
            val b = p + Offset(-u * 0.1f, -u * 0.09f) // on a post
            drawLine(BarkDark, b + Offset(0f, u * 0.09f), b + Offset(0f, -u * 0.01f), strokeWidth = u * 0.008f)
            drawPath(Path().apply {
                moveTo(a.x, a.y); quadraticTo((a.x + b.x) / 2, a.y + u * 0.09f, b.x, b.y)
                quadraticTo((a.x + b.x) / 2, a.y + u * 0.05f, a.x, a.y); close()
            }, Brand.Coral)
            drawPath(Path().apply { moveTo(a.x, a.y); quadraticTo((a.x + b.x) / 2, a.y + u * 0.07f, b.x, b.y) }, Color.White.copy(alpha = 0.5f), style = Stroke(u * 0.004f))
        }
        "tent" -> {
            val tw = u * 0.14f
            val th = u * 0.11f
            drawPath(Path().apply { moveTo(p.x - tw / 2, p.y); lineTo(p.x, p.y - th); lineTo(p.x + tw / 2, p.y); close() }, Brand.Coral)
            drawPath(Path().apply { moveTo(p.x, p.y - th); lineTo(p.x + tw * 0.12f, p.y); lineTo(p.x, p.y); close() }, Color.Black.copy(alpha = 0.12f))
            drawPath(Path().apply { moveTo(p.x - tw * 0.1f, p.y); lineTo(p.x, p.y - th * 0.55f); lineTo(p.x + tw * 0.1f, p.y); close() }, BarkDark)
        }
        "treehouse" -> {
            val base = p + Offset(u * 0.035f, -u * 0.2f)
            drawRect(Plank, base + Offset(-u * 0.07f, 0f), Size(u * 0.14f, u * 0.014f))
            drawRect(PlankLight, base + Offset(-u * 0.05f, -u * 0.07f), Size(u * 0.1f, u * 0.07f))
            drawPath(Path().apply { moveTo(base.x - u * 0.065f, base.y - u * 0.07f); lineTo(base.x, base.y - u * 0.115f); lineTo(base.x + u * 0.065f, base.y - u * 0.07f); close() }, Brand.Coral)
            drawRoundRect(if (night) Brand.Sunny else Color(0xFF3A2A1E), base + Offset(-u * 0.015f, -u * 0.05f), Size(u * 0.03f, u * 0.035f), CornerRadius(u * 0.004f))
            for (k in 0 until 6) drawLine(Plank, base + Offset(-u * 0.06f, u * 0.02f + k * u * 0.03f), base + Offset(-u * 0.04f, u * 0.02f + k * u * 0.03f), strokeWidth = u * 0.005f)
            drawLine(BarkDark, base + Offset(-u * 0.065f, u * 0.01f), base + Offset(-u * 0.065f, u * 0.2f), strokeWidth = u * 0.004f)
            drawLine(BarkDark, base + Offset(-u * 0.035f, u * 0.01f), base + Offset(-u * 0.035f, u * 0.2f), strokeWidth = u * 0.004f)
        }
        "fishing_chair" -> {
            drawLine(Metal, p + Offset(-u * 0.02f, 0f), p + Offset(u * 0.015f, -u * 0.035f), strokeWidth = u * 0.004f)
            drawLine(Metal, p + Offset(u * 0.02f, 0f), p + Offset(-u * 0.015f, -u * 0.035f), strokeWidth = u * 0.004f)
            drawRoundRect(Brand.Sky, p + Offset(-u * 0.022f, -u * 0.04f), Size(u * 0.044f, u * 0.012f), CornerRadius(u * 0.003f))
            drawRoundRect(Brand.Sky, p + Offset(-u * 0.022f, -u * 0.075f), Size(u * 0.01f, u * 0.037f), CornerRadius(u * 0.003f))
            // The rod, its line dipping into the river.
            val tip = p + Offset(u * 0.11f, -u * 0.12f)
            drawLine(BarkDark, p + Offset(u * 0.01f, -u * 0.04f), tip, strokeWidth = u * 0.004f, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.8f), tip, tip + Offset(u * 0.01f, u * 0.17f), strokeWidth = u * 0.0015f)
            drawCircle(Brand.Coral, u * 0.006f, tip + Offset(u * 0.01f, u * 0.17f))
        }
        "bike" -> bikeAt(p + Offset(0f, -u * 0.035f), u * 0.032f)
        "canoe" -> {
            // A long hull seen from the side, its pointed ends lifted, a paddle across.
            drawPath(Path().apply {
                moveTo(p.x - u * 0.1f, p.y - u * 0.045f)
                quadraticTo(p.x - u * 0.06f, p.y + u * 0.005f, p.x, p.y + u * 0.005f)
                quadraticTo(p.x + u * 0.06f, p.y + u * 0.005f, p.x + u * 0.1f, p.y - u * 0.045f)
                quadraticTo(p.x, p.y - u * 0.02f, p.x - u * 0.1f, p.y - u * 0.045f); close()
            }, Brand.Coral)
            drawPath(Path().apply {
                moveTo(p.x - u * 0.085f, p.y - u * 0.035f); quadraticTo(p.x, p.y - u * 0.012f, p.x + u * 0.085f, p.y - u * 0.035f)
            }, Color(0xFFA9452A), style = Stroke(u * 0.006f))
            drawLine(BarkDark, p + Offset(-u * 0.035f, -u * 0.075f), p + Offset(u * 0.04f, u * 0.005f), strokeWidth = u * 0.005f, cap = StrokeCap.Round)
            drawOval(BarkDark, p + Offset(u * 0.03f, -u * 0.004f), Size(u * 0.022f, u * 0.012f))
        }
        "boat" -> {
            drawPath(Path().apply {
                moveTo(p.x - u * 0.11f, p.y - u * 0.045f); lineTo(p.x + u * 0.12f, p.y - u * 0.045f)
                quadraticTo(p.x + u * 0.1f, p.y + u * 0.01f, p.x + u * 0.06f, p.y + u * 0.01f)
                lineTo(p.x - u * 0.08f, p.y + u * 0.01f); close()
            }, Color.White)
            drawRect(Brand.Sky, p + Offset(-u * 0.1f, -u * 0.045f), Size(u * 0.21f, u * 0.008f))
            drawRect(PlankLight, p + Offset(-u * 0.04f, -u * 0.09f), Size(u * 0.07f, u * 0.045f))
            drawRect(if (night) Brand.Sunny else Brand.Sky, p + Offset(-u * 0.03f, -u * 0.08f), Size(u * 0.02f, u * 0.02f))
            drawRect(if (night) Brand.Sunny else Brand.Sky, p + Offset(u * 0.0f, -u * 0.08f), Size(u * 0.02f, u * 0.02f))
        }
        "dock" -> {
            for (k in 0 until 6) drawRect(if (k % 2 == 0) Plank else PlankLight, p + Offset(-u * 0.09f + k * u * 0.03f, -u * 0.13f), Size(u * 0.03f, u * 0.025f))
            for (dx in listOf(-0.085f, -0.02f, 0.06f)) drawRect(BarkDark, p + Offset(u * dx, -u * 0.105f), Size(u * 0.01f, u * 0.08f))
        }
        "flag" -> {
            drawLine(BarkDark, p, p + Offset(0f, -u * 0.17f), strokeWidth = u * 0.006f)
            drawPath(Path().apply { moveTo(p.x, p.y - u * 0.17f); lineTo(p.x + u * 0.07f, p.y - u * 0.145f); lineTo(p.x, p.y - u * 0.12f); close() }, Brand.Coral)
        }
        "workshop" -> {
            val sw = u * 0.18f
            val sh = u * 0.12f
            val l = p.x - sw / 2
            drawRect(PlankLight, Offset(l, p.y - sh), Size(sw, sh))
            drawPath(Path().apply { moveTo(l - sw * 0.06f, p.y - sh); lineTo(l + sw * 1.06f, p.y - sh - u * 0.04f); lineTo(l + sw * 1.06f, p.y - sh + u * 0.008f); lineTo(l - sw * 0.06f, p.y - sh + u * 0.016f); close() }, Color(0xFF4B5563))
            drawRect(Color(0xFF3A2A1E), Offset(l + sw * 0.12f, p.y - sh * 0.75f), Size(sw * 0.45f, sh * 0.75f))
            drawLine(Color(0xFFCBD5E1), Offset(l + sw * 0.24f, p.y - sh * 0.62f), Offset(l + sw * 0.24f, p.y - sh * 0.3f), strokeWidth = u * 0.005f, cap = StrokeCap.Round)
            drawRect(Color(0xFFCBD5E1), Offset(l + sw * 0.36f, p.y - sh * 0.6f), Size(sw * 0.14f, sh * 0.09f))
            drawRoundRect(if (night) Brand.Sunny else Brand.Sky, Offset(l + sw * 0.66f, p.y - sh * 0.7f), Size(sw * 0.22f, sh * 0.3f), CornerRadius(u * 0.004f))
        }
        "cabin" -> {
            val cw = u * 0.22f
            val ch = u * 0.15f
            val l = p.x - cw / 2
            drawRoundRect(Plank, Offset(l, p.y - ch), Size(cw, ch), CornerRadius(u * 0.006f))
            var y = p.y - ch + ch / 6
            while (y < p.y - 2) { drawLine(BarkDark.copy(alpha = 0.4f), Offset(l + 3, y), Offset(l + cw - 3, y), strokeWidth = u * 0.003f); y += ch / 6 }
            drawPath(Path().apply { moveTo(l - cw * 0.08f, p.y - ch); lineTo(p.x, p.y - ch - u * 0.09f); lineTo(l + cw * 1.08f, p.y - ch); close() }, if (night) Color(0xFF243A30) else Color(0xFF35584A))
            drawRoundRect(BarkDark, Offset(l + cw * 0.6f, p.y - ch * 0.62f), Size(cw * 0.2f, ch * 0.62f), CornerRadius(u * 0.004f))
            val win = Offset(l + cw * 0.14f, p.y - ch * 0.72f)
            if (night) drawCircle(Brand.Sunny.copy(alpha = 0.35f), cw * 0.2f, win + Offset(cw * 0.13f, ch * 0.18f))
            drawRoundRect(if (night) Brand.Sunny else Color(0xFFF3E3B5), win, Size(cw * 0.26f, ch * 0.36f), CornerRadius(u * 0.004f))
        }
    }
}

private fun DrawScope.campfireAt(c: Offset, s: Float, night: Boolean) {
    if (night) drawCircle(Brand.Sunny.copy(alpha = 0.22f), s * 1.8f, c + Offset(0f, -s * 0.3f))
    drawLine(BarkDark, c + Offset(-s * 0.6f, 0f), c + Offset(s * 0.6f, -s * 0.3f), strokeWidth = s * 0.22f, cap = StrokeCap.Round)
    drawLine(Bark, c + Offset(-s * 0.6f, -s * 0.3f), c + Offset(s * 0.6f, 0f), strokeWidth = s * 0.22f, cap = StrokeCap.Round)
    val f = c + Offset(0f, -s * 0.2f)
    drawPath(Path().apply { moveTo(f.x - s * 0.35f, f.y); quadraticTo(f.x - s * 0.3f, f.y - s * 0.6f, f.x, f.y - s * 1.0f); quadraticTo(f.x + s * 0.35f, f.y - s * 0.5f, f.x + s * 0.35f, f.y); close() }, Brand.Coral)
    drawPath(Path().apply { moveTo(f.x - s * 0.18f, f.y); quadraticTo(f.x - s * 0.12f, f.y - s * 0.4f, f.x + s * 0.02f, f.y - s * 0.62f); quadraticTo(f.x + s * 0.2f, f.y - s * 0.3f, f.x + s * 0.18f, f.y); close() }, Brand.Sunny)
}

private fun DrawScope.bikeAt(c: Offset, r: Float) {
    val wheel = Color(0xFF2B2B2B)
    val back = c + Offset(-r * 1.2f, 0f)
    val front = c + Offset(r * 1.2f, 0f)
    drawCircle(wheel, r, back, style = Stroke(r * 0.18f))
    drawCircle(wheel, r, front, style = Stroke(r * 0.18f))
    val seat = c + Offset(-r * 0.4f, -r * 1.1f)
    val bar = c + Offset(r * 0.8f, -r * 1.25f)
    val crank = c + Offset(-r * 0.1f, 0f)
    for ((a, b) in listOf(back to crank, crank to seat, seat to back, crank to bar, seat to bar, bar to front))
        drawLine(Brand.Coral, a, b, strokeWidth = r * 0.16f, cap = StrokeCap.Round)
    drawLine(wheel, seat + Offset(-r * 0.25f, -r * 0.1f), seat + Offset(r * 0.25f, -r * 0.1f), strokeWidth = r * 0.2f, cap = StrokeCap.Round)
}

/** The clearing as a bitmap with rounded corners, for the widget and the share card. */
fun clearingBitmap(
    avatar: Avatar, placed: Map<Int, String>, worn: Set<Accessory>, widthPx: Int, heightPx: Int, night: Boolean = false,
): android.graphics.Bitmap {
    val image = androidx.compose.ui.graphics.ImageBitmap(widthPx, heightPx)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    val size = Size(widthPx.toFloat(), heightPx.toFloat())
    canvas.clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size), CornerRadius(widthPx * 0.06f))) })
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, canvas, size,
    ) { drawClearing(avatar, placed, worn = worn, night = night) }
    return image.asAndroidBitmap()
}
