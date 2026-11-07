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
import androidx.compose.ui.graphics.nativeCanvas
import app.tend.core.Accessory
import app.tend.core.Avatar
import app.tend.core.Mood
import app.tend.core.Plant

private val Wood = Color(0xFF8A5A3B)
private val WoodLight = Color(0xFFB98059)
private val WoodDark = Color(0xFF5E3D28)
private val Pine = Color(0xFF2F5D45)
private val PineLight = Color(0xFF3F7558)
private val Dirt = Color(0xFFD9C7A3)

/**
 * The base camp: mountains, pines and a clearing. A tent and a campfire first; a log cabin from
 * week two; a workshop with tools from week four. Each night's unused minutes become supplies
 * stacked in the yard: a log, a log pile or a crate.
 */
fun DrawScope.drawBase(
    avatar: Avatar,
    items: List<Plant>,
    stage: Int,
    mood: Mood = Mood.HAPPY,
    preview: Plant? = null,
    accessories: Set<Accessory> = emptySet(),
    night: Boolean = false,
) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.5f
    drawSky(night, groundY)

    // Mountains far behind, with snow on top.
    val rock = if (night) Color(0xFF3B4C5E) else Color(0xFF9DB3C2)
    for ((cx, peak, half) in listOf(Triple(0.3f, 0.2f, 0.32f), Triple(0.72f, 0.14f, 0.36f))) {
        val top = Offset(w * cx, h * peak)
        drawPath(Path().apply { moveTo(top.x - w * half, groundY); lineTo(top.x, top.y); lineTo(top.x + w * half, groundY); close() }, rock)
        val snow = 0.22f
        drawPath(Path().apply {
            moveTo(top.x - w * half * snow, top.y + (groundY - top.y) * snow); lineTo(top.x, top.y)
            lineTo(top.x + w * half * snow, top.y + (groundY - top.y) * snow)
            lineTo(top.x + w * half * snow * 0.4f, top.y + (groundY - top.y) * snow * 0.75f)
            lineTo(top.x, top.y + (groundY - top.y) * snow); close()
        }, Color.White.copy(alpha = if (night) 0.55f else 0.9f))
    }

    // Forest floor and the clearing.
    val grass = if (night) Color(0xFF2F5F47) else Color(0xFF6DB58A)
    drawRect(grass, Offset(0f, groundY), Size(w, h - groundY))
    drawOval(Dirt.copy(alpha = if (night) 0.35f else 0.55f), Offset(w * 0.28f, groundY + h * 0.06f), Size(w * 0.5f, h * 0.2f))

    // Pines along the back and at both edges.
    pine(Offset(w * 0.5f, groundY + h * 0.01f), h * 0.2f, night)
    pine(Offset(w * 0.04f, groundY + h * 0.03f), h * 0.32f, night)
    pine(Offset(w * 0.96f, groundY + h * 0.04f), h * 0.3f, night)

    // Things earned with habits.
    if (Accessory.LANTERNS in accessories) { // string lights between the trees
        val y0 = groundY - h * 0.2f
        drawPath(Path().apply { moveTo(w * 0.04f, y0); quadraticTo(w * 0.27f, y0 + h * 0.07f, w * 0.5f, y0 + h * 0.02f) }, WoodDark, style = Stroke(2.5f))
        for (i in 0 until 7) {
            val t = (i + 0.5f) / 7f
            val p = Offset(w * 0.04f + w * 0.46f * t, y0 + h * 0.07f * 4 * t * (1 - t) * 0.5f + t * h * 0.02f + h * 0.015f)
            drawCircle(Brand.Sunny.copy(alpha = if (night) 0.45f else 0.25f), h * 0.028f, p)
            drawCircle(Brand.Sunny, h * 0.012f, p)
        }
    }
    if (Accessory.BUNTING in accessories) { // pennants on a pole
        val x = w * 0.2f
        drawRect(WoodDark, Offset(x, groundY - h * 0.3f), Size(w * 0.01f, h * 0.32f))
        drawPath(Path().apply { moveTo(x + w * 0.01f, groundY - h * 0.3f); lineTo(x + w * 0.09f, groundY - h * 0.27f); lineTo(x + w * 0.01f, groundY - h * 0.24f); close() }, Brand.Coral)
        drawPath(Path().apply { moveTo(x + w * 0.01f, groundY - h * 0.23f); lineTo(x + w * 0.07f, groundY - h * 0.205f); lineTo(x + w * 0.01f, groundY - h * 0.18f); close() }, Brand.Sunny)
    }

    // The shelter grows: tent, then a log cabin.
    val hx = w * 0.6f
    val hw = w * 0.3f
    val hh = h * 0.2f
    val doorX: Float
    if (stage == 0) {
        val tw = hw * 0.9f
        val th = h * 0.2f
        val left = hx + hw * 0.05f
        drawPath(Path().apply { moveTo(left, groundY); lineTo(left + tw / 2, groundY - th); lineTo(left + tw, groundY); close() }, Brand.Coral)
        drawPath(Path().apply { moveTo(left + tw / 2, groundY - th); lineTo(left + tw * 0.62f, groundY); lineTo(left + tw / 2, groundY); close() }, Color.Black.copy(alpha = 0.12f))
        drawPath(Path().apply { moveTo(left + tw * 0.4f, groundY); lineTo(left + tw / 2, groundY - th * 0.55f); lineTo(left + tw * 0.6f, groundY); close() }, WoodDark)
        drawLine(WoodDark, Offset(left + tw / 2, groundY - th), Offset(left + tw / 2, groundY - th - h * 0.03f), strokeWidth = 4f, cap = StrokeCap.Round)
        doorX = left + tw * 0.42f
    } else {
        // Log cabin: stacked logs, a dark roof, a lit window and a door.
        drawRoundRect(Wood, Offset(hx, groundY - hh), Size(hw, hh), CornerRadius(8f))
        var y = groundY - hh + hh / 6
        while (y < groundY - 2) { drawLine(WoodDark.copy(alpha = 0.45f), Offset(hx + 4, y), Offset(hx + hw - 4, y), strokeWidth = 2.5f); y += hh / 6 }
        for (ly in listOf(groundY - hh * 0.9f, groundY - hh * 0.55f, groundY - hh * 0.2f)) {
            drawCircle(WoodLight, hh * 0.06f, Offset(hx + 2, ly)); drawCircle(WoodLight, hh * 0.06f, Offset(hx + hw - 2, ly))
        }
        drawPath(Path().apply {
            moveTo(hx - hw * 0.1f, groundY - hh); lineTo(hx + hw / 2, groundY - hh - h * 0.13f); lineTo(hx + hw * 1.1f, groundY - hh); close()
        }, if (night) Color(0xFF243A30) else Color(0xFF35584A))
        drawRect(Color(0xFF6E6E6E), Offset(hx + hw * 0.72f, groundY - hh - h * 0.12f), Size(hw * 0.09f, h * 0.07f))
        if (!night) drawCircle(Color.White.copy(alpha = 0.6f), h * 0.018f, Offset(hx + hw * 0.78f, groundY - hh - h * 0.15f))
        doorX = hx + hw * 0.6f
        drawRoundRect(WoodDark, Offset(doorX, groundY - hh * 0.65f), Size(hw * 0.22f, hh * 0.65f), CornerRadius(6f))
        drawCircle(Brand.Sunny, hw * 0.018f, Offset(doorX + hw * 0.17f, groundY - hh * 0.32f))
        val win = Offset(hx + hw * 0.14f, groundY - hh * 0.75f)
        val winSize = Size(hw * 0.28f, hh * 0.4f)
        drawCircle(Brand.Sunny.copy(alpha = 0.35f), hw * 0.22f, Offset(win.x + winSize.width / 2, win.y + winSize.height / 2))
        drawRoundRect(Brand.Sunny, win, winSize, CornerRadius(5f))
        drawLine(WoodDark, Offset(win.x + winSize.width / 2, win.y), Offset(win.x + winSize.width / 2, win.y + winSize.height), 4f)
    }
    if (stage >= 2) { // the workshop: a shed with tools on the wall
        val sx = w * 0.36f
        val sw = w * 0.18f
        val sh = h * 0.14f
        drawRect(WoodLight, Offset(sx, groundY - sh), Size(sw, sh))
        drawPath(Path().apply { moveTo(sx - sw * 0.06f, groundY - sh); lineTo(sx + sw * 1.06f, groundY - sh - h * 0.05f); lineTo(sx + sw * 1.06f, groundY - sh + h * 0.01f); lineTo(sx - sw * 0.06f, groundY - sh + h * 0.02f); close() }, Color(0xFF4B5563))
        drawRect(Color(0xFF3A2A1E), Offset(sx + sw * 0.1f, groundY - sh * 0.75f), Size(sw * 0.5f, sh * 0.75f))
        // A wrench and a saw hanging inside.
        drawLine(Color(0xFFCBD5E1), Offset(sx + sw * 0.22f, groundY - sh * 0.62f), Offset(sx + sw * 0.22f, groundY - sh * 0.3f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFCBD5E1), sw * 0.04f, Offset(sx + sw * 0.22f, groundY - sh * 0.64f))
        drawRect(Color(0xFFCBD5E1), Offset(sx + sw * 0.36f, groundY - sh * 0.6f), Size(sw * 0.16f, sh * 0.1f))
        drawRect(Wood, Offset(sx + sw * 0.52f, groundY - sh * 0.62f), Size(sw * 0.05f, sh * 0.14f))
    }
    // Leaning by the cabin, out of the path.
    if (Accessory.BIRDHOUSE in accessories) bike(Offset(w * 0.565f, groundY - h * 0.01f), h * 0.045f)

    // Path from the door, then the campfire in the clearing.
    drawPath(Path().apply {
        moveTo(doorX, groundY); lineTo(doorX + hw * 0.22f, groundY)
        lineTo(doorX + hw * 0.5f, h); lineTo(doorX - hw * 0.25f, h); close()
    }, Dirt)
    campfire(Offset(w * 0.47f, groundY + h * 0.16f), h * 0.07f, night)
    if (stage >= 1) { // a log to sit on by the fire
        drawRoundRect(Wood, Offset(w * 0.33f, groundY + h * 0.14f), Size(w * 0.08f, h * 0.035f), CornerRadius(h * 0.02f))
        drawCircle(WoodLight, h * 0.017f, Offset(w * 0.41f, groundY + h * 0.1575f))
    }

    // Supplies stacked in the yard: 2 rows of 6, newest last.
    val perRow = 6
    val rows = 2
    val slots = perRow * rows
    val shown = items.takeLast(if (preview != null) slots - 1 else slots)
    val left = w * 0.04f
    val right = w * 0.5f
    val step = (right - left) / perRow
    for (i in 0 until slots) {
        val base = Offset(left + step * (i % perRow + 0.5f), groundY + (h - groundY) * (0.62f + (i / perRow) * 0.25f))
        val item = shown.getOrNull(i)
        when {
            item != null -> supply(item, base, h * 0.1f)
            i == shown.size && preview != null -> drawIntoCanvas {
                it.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), androidx.compose.ui.graphics.Paint().apply { alpha = 0.4f })
                supply(preview, base, h * 0.1f)
                it.restore()
            }
            else -> drawOval(Color.Black.copy(alpha = 0.06f), Offset(base.x - step * 0.3f, base.y - h * 0.01f), Size(step * 0.6f, h * 0.02f))
        }
    }

    // The companion in the clearing, in front of everything.
    val cs = h * 0.24f
    drawIntoCanvas {
        it.nativeCanvas.save()
        it.nativeCanvas.translate(w * 0.24f, groundY - cs * 0.88f)
        CharacterPainter.draw(it.nativeCanvas, avatar, mood, cs, accessories = accessories)
        it.nativeCanvas.restore()
    }
}

private fun DrawScope.pine(base: Offset, s: Float, night: Boolean) {
    drawRect(WoodDark, Offset(base.x - s * 0.04f, base.y - s * 0.2f), Size(s * 0.08f, s * 0.2f))
    val dark = if (night) Color(0xFF1F3F30) else Pine
    val light = if (night) Color(0xFF28503C) else PineLight
    for ((i, t) in listOf(0f, 0.28f, 0.52f).withIndex()) {
        val width = s * (0.42f - i * 0.1f)
        val bottom = base.y - s * (0.15f + t)
        drawPath(Path().apply { moveTo(base.x - width, bottom); lineTo(base.x, bottom - s * 0.42f); lineTo(base.x + width, bottom); close() }, if (i % 2 == 0) dark else light)
    }
}

private fun DrawScope.campfire(c: Offset, s: Float, night: Boolean) {
    if (night) drawCircle(Brand.Sunny.copy(alpha = 0.22f), s * 1.6f, c)
    drawLine(WoodDark, c + Offset(-s * 0.6f, s * 0.25f), c + Offset(s * 0.6f, -s * 0.05f), strokeWidth = s * 0.22f, cap = StrokeCap.Round)
    drawLine(Wood, c + Offset(-s * 0.6f, -s * 0.05f), c + Offset(s * 0.6f, s * 0.25f), strokeWidth = s * 0.22f, cap = StrokeCap.Round)
    drawPath(Path().apply { moveTo(c.x - s * 0.35f, c.y); quadraticTo(c.x - s * 0.3f, c.y - s * 0.6f, c.x, c.y - s * 1.0f); quadraticTo(c.x + s * 0.35f, c.y - s * 0.5f, c.x + s * 0.35f, c.y); close() }, Brand.Coral)
    drawPath(Path().apply { moveTo(c.x - s * 0.18f, c.y); quadraticTo(c.x - s * 0.12f, c.y - s * 0.4f, c.x + s * 0.02f, c.y - s * 0.62f); quadraticTo(c.x + s * 0.2f, c.y - s * 0.3f, c.x + s * 0.18f, c.y); close() }, Brand.Sunny)
}

private fun DrawScope.bike(c: Offset, r: Float) {
    val frame = Brand.Coral
    val wheel = Color(0xFF2B2B2B)
    val back = c + Offset(-r * 1.2f, 0f)
    val front = c + Offset(r * 1.2f, 0f)
    drawCircle(wheel, r, back, style = Stroke(r * 0.18f))
    drawCircle(wheel, r, front, style = Stroke(r * 0.18f))
    val seat = c + Offset(-r * 0.4f, -r * 1.1f)
    val bar = c + Offset(r * 0.8f, -r * 1.25f)
    val crank = c + Offset(-r * 0.1f, 0f)
    for ((a, b) in listOf(back to crank, crank to seat, seat to back, crank to bar, seat to bar, bar to front))
        drawLine(frame, a, b, strokeWidth = r * 0.16f, cap = StrokeCap.Round)
    drawLine(wheel, seat + Offset(-r * 0.25f, -r * 0.1f), seat + Offset(r * 0.25f, -r * 0.1f), strokeWidth = r * 0.2f, cap = StrokeCap.Round)
}

/** One night's supplies: a log, a log pile or a crate, standing at [base]; [s] is its size. */
fun DrawScope.supply(item: Plant, base: Offset, s: Float) {
    fun log(c: Offset, len: Float) {
        drawRoundRect(Wood, Offset(c.x - len / 2, c.y - s * 0.14f), Size(len, s * 0.28f), CornerRadius(s * 0.14f))
        drawCircle(WoodLight, s * 0.13f, Offset(c.x + len / 2 - s * 0.1f, c.y))
        drawCircle(WoodDark.copy(alpha = 0.5f), s * 0.05f, Offset(c.x + len / 2 - s * 0.1f, c.y))
    }
    when (item) {
        Plant.SPROUT -> log(base + Offset(0f, -s * 0.14f), s * 0.8f)
        Plant.TULIP -> {
            log(base + Offset(-s * 0.16f, -s * 0.14f), s * 0.7f)
            log(base + Offset(s * 0.16f, -s * 0.14f), s * 0.7f)
            log(base + Offset(0f, -s * 0.4f), s * 0.7f)
        }
        Plant.SUNFLOWER -> {
            val box = s * 0.62f
            val tl = Offset(base.x - box / 2, base.y - box)
            drawRoundRect(WoodLight, tl, Size(box, box), CornerRadius(s * 0.05f))
            drawRect(Wood, tl, Size(box, box), style = Stroke(s * 0.07f))
            drawLine(Wood, tl, tl + Offset(box, box), strokeWidth = s * 0.07f)
            drawLine(Wood, tl + Offset(box, 0f), tl + Offset(0f, box), strokeWidth = s * 0.07f)
        }
    }
}

/** A small icon of a night's item in [world], for lists and the recap. */
fun DrawScope.drawItem(world: app.tend.core.World, item: Plant, base: Offset, s: Float) =
    if (world == app.tend.core.World.BASE) supply(item, base, s * 0.8f) else drawPlant(item, base, s)
