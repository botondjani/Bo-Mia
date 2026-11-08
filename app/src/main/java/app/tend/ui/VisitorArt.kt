package app.tend.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import app.tend.core.Visitor

private val Eye = Color(0xFF1D2A23)
private val Cream = Color(0xFFF4E9D6)

/** An animal standing at [p] (its feet), [u] the scene height. */
fun DrawScope.drawVisitor(v: Visitor, p: Offset, u: Float) {
    if (v != Visitor.SQUIRREL && v != Visitor.OWL && v != Visitor.BEAVER) drawOval(Color.Black.copy(alpha = 0.13f), Offset(p.x - u * 0.035f, p.y - u * 0.006f), Size(u * 0.07f, u * 0.012f))
    when (v) {
        Visitor.DEER -> deer(p, u)
        Visitor.FOX -> fox(p, u)
        Visitor.RABBIT -> rabbit(p, u)
        Visitor.SQUIRREL -> squirrel(p, u)
        Visitor.HERON -> heron(p, u)
        Visitor.OWL -> owl(p, u)
        Visitor.HEDGEHOG -> hedgehog(p, u)
        Visitor.BEAVER -> beaver(p, u)
    }
}

/** One animal as an icon filling this canvas, standing on its bottom; [seen] false draws a silhouette. */
fun DrawScope.drawVisitorIcon(v: Visitor, seen: Boolean = true) {
    if (!seen) {
        drawIntoCanvas { it.nativeCanvas.saveLayer(null, android.graphics.Paint().apply { colorFilter = android.graphics.PorterDuffColorFilter(0x33000000, android.graphics.PorterDuff.Mode.SRC_IN) }) }
        drawVisitorIcon(v, true)
        drawIntoCanvas { it.nativeCanvas.restore() }
        return
    }
    val s = size.minDimension
    val (w, h) = when (v) { Visitor.DEER -> 0.09f to 0.12f; Visitor.HERON -> 0.07f to 0.12f; Visitor.SQUIRREL -> 0.05f to 0.058f; Visitor.OWL -> 0.07f to 0.07f; Visitor.FOX -> 0.075f to 0.08f; Visitor.RABBIT -> 0.05f to 0.07f; Visitor.HEDGEHOG -> 0.085f to 0.045f; Visitor.BEAVER -> 0.1f to 0.045f }
    val u = minOf(s * 0.85f / w, s * 0.8f / h)
    val base = Offset(size.width / 2, size.height / 2 + s * 0.4f)
    when (v) {
        Visitor.SQUIRREL -> squirrel(base + Offset(0f, -u * 0.01f), u, onTrunk = false)
        Visitor.OWL -> owl(base, u)
        else -> drawVisitor(v, base, u)
    }
}

/** A glossy eye: dark, with a small white catchlight. */
private fun DrawScope.eye(at: Offset, r: Float) {
    drawCircle(Eye, r, at)
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.38f, at + Offset(-r * 0.3f, -r * 0.35f))
}

/** Soft shading: lighter on the upper left, darker on the lower right, over a shape's fill. */
private fun shade(base: Color, center: Offset, r: Float) = Brush.radialGradient(
    listOf(androidx.compose.ui.graphics.lerp(base, Color.White, 0.18f), base, androidx.compose.ui.graphics.lerp(base, Color.Black, 0.18f)),
    center = center + Offset(-r * 0.4f, -r * 0.5f), radius = r * 1.6f,
)

/** A young deer, standing side on and turning its head to us. */
private fun DrawScope.deer(p: Offset, u: Float) {
    val coat = Color(0xFFB9824F)
    val dark = Color(0xFF7E5431)
    val belly = Color(0xFFF1E2C8)
    // Far legs, a little darker, then the body, then the near legs.
    for (dx in listOf(-0.019f, 0.03f)) leg(p + Offset(u * dx, 0f), u, dark)
    val body = Path().apply {
        moveTo(p.x - u * 0.03f, p.y - u * 0.045f)
        cubicTo(p.x - u * 0.034f, p.y - u * 0.068f, p.x - u * 0.005f, p.y - u * 0.072f, p.x + u * 0.025f, p.y - u * 0.068f)
        cubicTo(p.x + u * 0.042f, p.y - u * 0.066f, p.x + u * 0.045f, p.y - u * 0.05f, p.x + u * 0.04f, p.y - u * 0.04f)
        cubicTo(p.x + u * 0.03f, p.y - u * 0.033f, p.x - u * 0.02f, p.y - u * 0.032f, p.x - u * 0.03f, p.y - u * 0.045f)
        close()
    }
    drawPath(body, shade(coat, p + Offset(0f, -u * 0.055f), u * 0.04f))
    drawPath(Path().apply { moveTo(p.x - u * 0.022f, p.y - u * 0.036f); quadraticTo(p.x + u * 0.005f, p.y - u * 0.03f, p.x + u * 0.032f, p.y - u * 0.036f); quadraticTo(p.x + u * 0.005f, p.y - u * 0.042f, p.x - u * 0.022f, p.y - u * 0.036f); close() }, belly)
    // Fawn spots along the back.
    for ((dx, dy) in listOf(-0.012f to 0.062f, 0.0f to 0.065f, 0.012f to 0.063f, 0.024f to 0.06f, -0.004f to 0.056f, 0.016f to 0.055f)) drawCircle(belly.copy(alpha = 0.9f), u * 0.0022f, p + Offset(u * dx, -u * dy))
    // A white tail flick.
    drawOval(belly, Offset(p.x + u * 0.036f, p.y - u * 0.07f), Size(u * 0.008f, u * 0.012f))
    for (dx in listOf(-0.024f, 0.024f)) leg(p + Offset(u * dx, 0f), u, coat)
    // Neck and head.
    drawPath(Path().apply {
        moveTo(p.x - u * 0.03f, p.y - u * 0.05f); quadraticTo(p.x - u * 0.042f, p.y - u * 0.075f, p.x - u * 0.036f, p.y - u * 0.098f)
        lineTo(p.x - u * 0.022f, p.y - u * 0.096f); quadraticTo(p.x - u * 0.022f, p.y - u * 0.07f, p.x - u * 0.012f, p.y - u * 0.062f); close()
    }, coat)
    drawPath(Path().apply { moveTo(p.x - u * 0.034f, p.y - u * 0.072f); quadraticTo(p.x - u * 0.03f, p.y - u * 0.06f, p.x - u * 0.022f, p.y - u * 0.058f); lineTo(p.x - u * 0.026f, p.y - u * 0.07f); close() }, belly)
    val head = p + Offset(-u * 0.03f, -u * 0.104f)
    for (side in listOf(-1f, 1f)) rotate(side * 40f, head + Offset(side * u * 0.012f, -u * 0.006f)) {
        drawOval(coat, Offset(head.x + side * u * 0.013f - u * 0.008f, head.y - u * 0.011f), Size(u * 0.016f, u * 0.009f))
        drawOval(Color(0xFFE9B9A2), Offset(head.x + side * u * 0.013f - u * 0.005f, head.y - u * 0.009f), Size(u * 0.01f, u * 0.005f))
    }
    drawOval(shade(coat, head, u * 0.012f), Offset(head.x - u * 0.011f, head.y - u * 0.01f), Size(u * 0.022f, u * 0.021f))
    drawOval(belly, Offset(head.x - u * 0.007f, head.y + u * 0.002f), Size(u * 0.014f, u * 0.011f))
    eye(head + Offset(-u * 0.005f, -u * 0.002f), u * 0.0026f)
    eye(head + Offset(u * 0.005f, -u * 0.002f), u * 0.0026f)
    drawOval(Eye, Offset(head.x - u * 0.0028f, head.y + u * 0.006f), Size(u * 0.0056f, u * 0.004f))
}

private fun DrawScope.leg(top: Offset, u: Float, color: Color) {
    drawLine(color, top + Offset(0f, -u * 0.042f), top + Offset(0f, -u * 0.004f), strokeWidth = u * 0.005f, cap = StrokeCap.Round)
    drawOval(Color(0xFF3B2A20), Offset(top.x - u * 0.003f, top.y - u * 0.004f), Size(u * 0.006f, u * 0.004f))
}

/** A fox sitting up, tail wrapped round its paws. */
private fun DrawScope.fox(p: Offset, u: Float) {
    val fur = Color(0xFFE2763A)
    val dark = Color(0xFF3B2A20)
    val white = Color(0xFFFBF3E6)
    // Tail curling round the front, white tip.
    val tail = Path().apply {
        moveTo(p.x + u * 0.008f, p.y - u * 0.006f)
        cubicTo(p.x + u * 0.04f, p.y - u * 0.002f, p.x + u * 0.05f, p.y - u * 0.03f, p.x + u * 0.034f, p.y - u * 0.042f)
        cubicTo(p.x + u * 0.04f, p.y - u * 0.024f, p.x + u * 0.024f, p.y - u * 0.012f, p.x + u * 0.004f, p.y - u * 0.014f)
        close()
    }
    drawPath(tail, shade(fur, p + Offset(u * 0.03f, -u * 0.02f), u * 0.025f))
    drawCircle(white, u * 0.0065f, p + Offset(u * 0.036f, -u * 0.04f))
    // Body: a pear shape, with a white bib.
    val body = Path().apply {
        moveTo(p.x - u * 0.016f, p.y)
        cubicTo(p.x - u * 0.022f, p.y - u * 0.02f, p.x - u * 0.014f, p.y - u * 0.045f, p.x, p.y - u * 0.046f)
        cubicTo(p.x + u * 0.014f, p.y - u * 0.045f, p.x + u * 0.022f, p.y - u * 0.02f, p.x + u * 0.016f, p.y)
        close()
    }
    drawPath(body, shade(fur, p + Offset(0f, -u * 0.025f), u * 0.025f))
    drawPath(Path().apply { moveTo(p.x - u * 0.008f, p.y - u * 0.04f); quadraticTo(p.x - u * 0.012f, p.y - u * 0.012f, p.x, p.y - u * 0.006f); quadraticTo(p.x + u * 0.012f, p.y - u * 0.012f, p.x + u * 0.008f, p.y - u * 0.04f); close() }, white)
    for (dx in listOf(-0.007f, 0.007f)) drawRoundRect(dark, Offset(p.x + u * dx - u * 0.003f, p.y - u * 0.012f), Size(u * 0.006f, u * 0.012f), androidx.compose.ui.geometry.CornerRadius(u * 0.003f))
    // Head: wide cheeks, a pointed snout, big dark-backed ears.
    val head = p + Offset(0f, -u * 0.054f)
    for (side in listOf(-1f, 1f)) {
        drawPath(Path().apply { moveTo(head.x + side * u * 0.006f, head.y - u * 0.008f); lineTo(head.x + side * u * 0.016f, head.y - u * 0.028f); lineTo(head.x + side * u * 0.019f, head.y - u * 0.004f); close() }, dark)
        drawPath(Path().apply { moveTo(head.x + side * u * 0.009f, head.y - u * 0.008f); lineTo(head.x + side * u * 0.0155f, head.y - u * 0.022f); lineTo(head.x + side * u * 0.0165f, head.y - u * 0.006f); close() }, fur)
    }
    drawPath(Path().apply {
        moveTo(head.x - u * 0.02f, head.y - u * 0.004f); quadraticTo(head.x, head.y - u * 0.016f, head.x + u * 0.02f, head.y - u * 0.004f)
        quadraticTo(head.x + u * 0.012f, head.y + u * 0.006f, head.x, head.y + u * 0.016f); quadraticTo(head.x - u * 0.012f, head.y + u * 0.006f, head.x - u * 0.02f, head.y - u * 0.004f); close()
    }, shade(fur, head, u * 0.02f))
    drawPath(Path().apply { moveTo(head.x - u * 0.016f, head.y + u * 0.0f); quadraticTo(head.x - u * 0.006f, head.y + u * 0.002f, head.x, head.y + u * 0.016f); quadraticTo(head.x + u * 0.006f, head.y + u * 0.002f, head.x + u * 0.016f, head.y); quadraticTo(head.x, head.y + u * 0.006f, head.x - u * 0.016f, head.y); close() }, white)
    eye(head + Offset(-u * 0.0075f, -u * 0.003f), u * 0.0026f)
    eye(head + Offset(u * 0.0075f, -u * 0.003f), u * 0.0026f)
    drawCircle(dark, u * 0.0024f, head + Offset(0f, u * 0.015f))
}

/** A wild rabbit, side on, ears up. */
private fun DrawScope.rabbit(p: Offset, u: Float) {
    val fur = Color(0xFFA48F7A)
    val light = Color(0xFFE9DFD2)
    val body = Path().apply {
        moveTo(p.x - u * 0.016f, p.y)
        cubicTo(p.x - u * 0.022f, p.y - u * 0.02f, p.x - u * 0.004f, p.y - u * 0.03f, p.x + u * 0.012f, p.y - u * 0.026f)
        cubicTo(p.x + u * 0.024f, p.y - u * 0.022f, p.x + u * 0.024f, p.y - u * 0.004f, p.x + u * 0.016f, p.y)
        close()
    }
    drawPath(body, shade(fur, p + Offset(0f, -u * 0.015f), u * 0.022f))
    drawCircle(Color.White, u * 0.006f, p + Offset(u * 0.022f, -u * 0.012f)) // tail
    drawOval(fur, Offset(p.x + u * 0.002f, p.y - u * 0.006f), Size(u * 0.016f, u * 0.006f)) // hind foot
    drawOval(light, Offset(p.x - u * 0.014f, p.y - u * 0.012f), Size(u * 0.012f, u * 0.01f)) // chest
    val head = p + Offset(-u * 0.014f, -u * 0.03f)
    for ((dx, tilt) in listOf(0.002f to 8f, -0.004f to -6f)) rotate(tilt, head + Offset(u * dx, -u * 0.008f)) {
        drawOval(fur, Offset(head.x + u * dx - u * 0.0035f, head.y - u * 0.034f), Size(u * 0.007f, u * 0.028f))
        drawOval(Color(0xFFE7B1AC), Offset(head.x + u * dx - u * 0.0016f, head.y - u * 0.03f), Size(u * 0.0032f, u * 0.019f))
    }
    drawCircle(shade(fur, head, u * 0.011f), u * 0.011f, head)
    drawOval(light, Offset(head.x - u * 0.011f, head.y + u * 0.0f), Size(u * 0.01f, u * 0.008f))
    eye(head + Offset(-u * 0.004f, -u * 0.002f), u * 0.0026f)
    drawCircle(Color(0xFFD98E8E), u * 0.0016f, head + Offset(-u * 0.011f, u * 0.002f))
}

/** A red squirrel, sitting up with an acorn, its big tail curled behind. */
private fun DrawScope.squirrel(p: Offset, u: Float, onTrunk: Boolean = true) {
    val fur = Color(0xFFB4562C)
    val light = Color(0xFFF2D9BE)
    val at = p
    val tail = Path().apply {
        moveTo(at.x + u * 0.004f, at.y)
        cubicTo(at.x + u * 0.04f, at.y - u * 0.004f, at.x + u * 0.036f, at.y - u * 0.05f, at.x + u * 0.014f, at.y - u * 0.05f)
        cubicTo(at.x + u * 0.002f, at.y - u * 0.05f, at.x + u * 0.004f, at.y - u * 0.04f, at.x + u * 0.012f, at.y - u * 0.042f)
        cubicTo(at.x + u * 0.026f, at.y - u * 0.04f, at.x + u * 0.024f, at.y - u * 0.014f, at.x + u * 0.002f, at.y - u * 0.012f)
        close()
    }
    drawPath(tail, shade(fur, at + Offset(u * 0.025f, -u * 0.03f), u * 0.025f))
    drawPath(tail, Color(0xFFD9824F).copy(alpha = 0.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(u * 0.0025f))
    drawOval(shade(fur, at + Offset(0f, -u * 0.015f), u * 0.014f), Offset(at.x - u * 0.011f, at.y - u * 0.027f), Size(u * 0.02f, u * 0.027f))
    drawOval(light, Offset(at.x - u * 0.008f, at.y - u * 0.021f), Size(u * 0.01f, u * 0.017f))
    val head = at + Offset(-u * 0.004f, -u * 0.032f)
    for (dx in listOf(-0.004f, 0.004f)) {
        drawPath(Path().apply { moveTo(head.x + u * dx * 1.4f - u * 0.003f, head.y - u * 0.005f); lineTo(head.x + u * dx * 1.6f, head.y - u * 0.0135f); lineTo(head.x + u * dx * 1.4f + u * 0.003f, head.y - u * 0.005f); close() }, fur)
        drawLine(Color(0xFF7A3418), head + Offset(u * dx * 1.6f, -u * 0.013f), head + Offset(u * dx * 1.7f, -u * 0.0165f), strokeWidth = u * 0.0016f, cap = StrokeCap.Round) // tufts
    }
    drawCircle(shade(fur, head, u * 0.009f), u * 0.009f, head)
    drawOval(light, Offset(head.x - u * 0.008f, head.y + u * 0.0f), Size(u * 0.008f, u * 0.006f))
    eye(head + Offset(-u * 0.004f, -u * 0.002f), u * 0.0022f)
    // The acorn in its paws.
    drawOval(Color(0xFFB9783F), Offset(at.x - u * 0.013f, at.y - u * 0.019f), Size(u * 0.007f, u * 0.008f))
    drawArc(Color(0xFF6B4A2B), 180f, 180f, true, Offset(at.x - u * 0.0135f, at.y - u * 0.0215f), Size(u * 0.008f, u * 0.005f))
}

/** A grey heron standing in the shallows, neck in an S, a crest plume behind. */
private fun DrawScope.heron(p: Offset, u: Float) {
    val grey = Color(0xFFA4AEB6)
    val dark = Color(0xFF4F5962)
    val pale = Color(0xFFE9EEF1)
    drawOval(Color.White.copy(alpha = 0.35f), Offset(p.x - u * 0.02f, p.y - u * 0.003f), Size(u * 0.04f, u * 0.007f))
    for (dx in listOf(0f, 0.006f)) drawLine(Color(0xFFC79A4A), p + Offset(u * dx, 0f), p + Offset(u * dx * 0.5f, -u * 0.045f), strokeWidth = u * 0.0024f)
    // Body: a long teardrop, wings folded, dark flight feathers at the back.
    val body = Path().apply {
        moveTo(p.x - u * 0.016f, p.y - u * 0.058f)
        cubicTo(p.x - u * 0.004f, p.y - u * 0.07f, p.x + u * 0.024f, p.y - u * 0.06f, p.x + u * 0.034f, p.y - u * 0.05f)
        cubicTo(p.x + u * 0.02f, p.y - u * 0.042f, p.x - u * 0.004f, p.y - u * 0.04f, p.x - u * 0.016f, p.y - u * 0.058f)
        close()
    }
    drawPath(body, shade(grey, p + Offset(u * 0.005f, -u * 0.055f), u * 0.025f))
    drawPath(Path().apply { moveTo(p.x + u * 0.012f, p.y - u * 0.054f); quadraticTo(p.x + u * 0.026f, p.y - u * 0.052f, p.x + u * 0.036f, p.y - u * 0.048f); quadraticTo(p.x + u * 0.024f, p.y - u * 0.046f, p.x + u * 0.012f, p.y - u * 0.054f); close() }, dark)
    // The S of the neck, pale in front with a dark streak.
    val neck = Path().apply { moveTo(p.x - u * 0.012f, p.y - u * 0.06f); cubicTo(p.x - u * 0.028f, p.y - u * 0.072f, p.x - u * 0.004f, p.y - u * 0.086f, p.x - u * 0.016f, p.y - u * 0.104f) }
    drawPath(neck, pale, style = androidx.compose.ui.graphics.drawscope.Stroke(u * 0.0075f, cap = StrokeCap.Round))
    drawPath(neck, dark.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(u * 0.0015f, cap = StrokeCap.Round))
    val head = p + Offset(-u * 0.016f, -u * 0.108f)
    drawCircle(pale, u * 0.0065f, head)
    drawLine(Color(0xFFE0A93E), head + Offset(-u * 0.004f, u * 0.001f), head + Offset(-u * 0.028f, u * 0.005f), strokeWidth = u * 0.0032f, cap = StrokeCap.Round)
    drawLine(Eye, head + Offset(-u * 0.001f, -u * 0.003f), head + Offset(u * 0.008f, -u * 0.003f), strokeWidth = u * 0.0022f, cap = StrokeCap.Round)
    drawPath(Path().apply { moveTo(head.x + u * 0.004f, head.y - u * 0.003f); quadraticTo(head.x + u * 0.016f, head.y - u * 0.004f, head.x + u * 0.022f, head.y + u * 0.002f) }, Eye, style = androidx.compose.ui.graphics.drawscope.Stroke(u * 0.0012f, cap = StrokeCap.Round)) // plume
    eye(head + Offset(-u * 0.002f, -u * 0.0005f), u * 0.0017f)
}

/** A tawny owl on a branch: round, with tufts, big amber eyes and a speckled chest. */
private fun DrawScope.owl(p: Offset, u: Float) {
    val feather = Color(0xFF8C6A4A)
    val dark = Color(0xFF5E4430)
    val chest = Color(0xFFDCC5A3)
    drawLine(Color(0xFF452F21), p + Offset(-u * 0.032f, u * 0.002f), p + Offset(u * 0.032f, -u * 0.003f), strokeWidth = u * 0.006f, cap = StrokeCap.Round)
    drawCircle(Color(0xFF41704A), u * 0.006f, p + Offset(u * 0.03f, -u * 0.006f))
    val body = Path().apply {
        moveTo(p.x - u * 0.015f, p.y)
        cubicTo(p.x - u * 0.022f, p.y - u * 0.02f, p.x - u * 0.018f, p.y - u * 0.046f, p.x, p.y - u * 0.047f)
        cubicTo(p.x + u * 0.018f, p.y - u * 0.046f, p.x + u * 0.022f, p.y - u * 0.02f, p.x + u * 0.015f, p.y)
        close()
    }
    drawPath(body, shade(feather, p + Offset(0f, -u * 0.025f), u * 0.025f))
    // Wings folded at the sides.
    for (side in listOf(-1f, 1f)) drawPath(Path().apply { moveTo(p.x + side * u * 0.016f, p.y - u * 0.03f); quadraticTo(p.x + side * u * 0.02f, p.y - u * 0.01f, p.x + side * u * 0.01f, p.y - u * 0.001f); quadraticTo(p.x + side * u * 0.012f, p.y - u * 0.016f, p.x + side * u * 0.016f, p.y - u * 0.03f); close() }, dark)
    drawOval(chest, Offset(p.x - u * 0.009f, p.y - u * 0.026f), Size(u * 0.018f, u * 0.024f))
    for ((dx, dy) in listOf(-0.004f to 0.02f, 0.003f to 0.017f, -0.002f to 0.012f, 0.005f to 0.009f, -0.005f to 0.007f)) {
        drawPath(Path().apply { moveTo(p.x + u * dx - u * 0.002f, p.y - u * dy); lineTo(p.x + u * dx, p.y - u * dy + u * 0.002f); lineTo(p.x + u * dx + u * 0.002f, p.y - u * dy) }, dark, style = androidx.compose.ui.graphics.drawscope.Stroke(u * 0.0012f))
    }
    // Facial disc, tufts, eyes and beak.
    val face = p + Offset(0f, -u * 0.036f)
    for (side in listOf(-1f, 1f)) drawPath(Path().apply { moveTo(face.x + side * u * 0.008f, face.y - u * 0.008f); lineTo(face.x + side * u * 0.016f, face.y - u * 0.018f); lineTo(face.x + side * u * 0.016f, face.y - u * 0.006f); close() }, feather)
    drawOval(Color(0xFFC9A47E), Offset(face.x - u * 0.016f, face.y - u * 0.01f), Size(u * 0.032f, u * 0.019f))
    for (side in listOf(-1f, 1f)) {
        val e = face + Offset(side * u * 0.0075f, 0f)
        drawCircle(Color(0xFFF7EBD2), u * 0.0072f, e)
        drawCircle(Color(0xFFE8A93A), u * 0.0052f, e)
        eye(e, u * 0.003f)
    }
    drawPath(Path().apply { moveTo(face.x - u * 0.0022f, face.y + u * 0.003f); lineTo(face.x + u * 0.0022f, face.y + u * 0.003f); lineTo(face.x, face.y + u * 0.009f); close() }, Color(0xFFC98A3A))
    for (dx in listOf(-0.006f, 0.006f)) drawLine(Color(0xFFC98A3A), p + Offset(u * dx, -u * 0.001f), p + Offset(u * dx, u * 0.002f), strokeWidth = u * 0.0025f, cap = StrokeCap.Round)
}

/** A hedgehog trundling along: a round body of spines, a pale pointed face and a little nose. */
private fun DrawScope.hedgehog(p: Offset, u: Float) {
    val spines = Color(0xFF6E5741)
    val tips = Color(0xFFCDB89C)
    val face = Color(0xFFE6D2B5)
    val back = Path().apply {
        moveTo(p.x - u * 0.016f, p.y - u * 0.002f)
        cubicTo(p.x - u * 0.02f, p.y - u * 0.03f, p.x + u * 0.022f, p.y - u * 0.034f, p.x + u * 0.026f, p.y - u * 0.002f)
        close()
    }
    drawPath(back, shade(spines, p + Offset(u * 0.004f, -u * 0.016f), u * 0.022f))
    // Spines: short light-tipped strokes fanning back over the body.
    for (k in 0 until 14) {
        val a = 3.4f + k / 13f * 2.6f
        val c = p + Offset(u * 0.004f, -u * 0.004f)
        val r0 = u * 0.012f; val r1 = u * 0.026f
        val from = c + Offset(kotlin.math.cos(a) * r0, kotlin.math.sin(a) * r0 * 0.9f)
        val to = c + Offset(kotlin.math.cos(a + 0.25f) * r1, kotlin.math.sin(a + 0.25f) * r1 * 0.85f)
        drawLine(spines, from, to, strokeWidth = u * 0.0026f, cap = StrokeCap.Round)
        drawCircle(tips, u * 0.0011f, to)
    }
    // The face, pointing left.
    drawPath(Path().apply {
        moveTo(p.x - u * 0.012f, p.y - u * 0.02f); quadraticTo(p.x - u * 0.03f, p.y - u * 0.012f, p.x - u * 0.033f, p.y - u * 0.006f)
        quadraticTo(p.x - u * 0.024f, p.y - u * 0.001f, p.x - u * 0.01f, p.y - u * 0.002f); close()
    }, face)
    drawCircle(Eye, u * 0.0028f, p + Offset(-u * 0.034f, -u * 0.0065f))
    eye(p + Offset(-u * 0.02f, -u * 0.012f), u * 0.0022f)
    drawCircle(spines, u * 0.0035f, p + Offset(-u * 0.014f, -u * 0.017f)) // ear
    for (dx in listOf(-0.012f, 0.012f)) drawOval(Color(0xFF4A3A2C), Offset(p.x + u * dx - u * 0.003f, p.y - u * 0.003f), Size(u * 0.006f, u * 0.004f))
}

/** A beaver swimming by the shore: head and back above the water, a V of ripples behind, its flat tail. */
private fun DrawScope.beaver(p: Offset, u: Float) {
    val fur = Color(0xFF7A5232)
    val dark = Color(0xFF4E321E)
    // Ripples spreading behind it.
    for (k in 0 until 2) {
        val back = p + Offset(u * (0.03f + k * 0.018f), u * 0.002f)
        drawLine(Color.White.copy(alpha = 0.45f - k * 0.15f), back + Offset(-u * 0.012f, 0f), back + Offset(u * 0.01f, -u * 0.007f), strokeWidth = u * 0.0018f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.45f - k * 0.15f), back + Offset(-u * 0.012f, 0f), back + Offset(u * 0.01f, u * 0.007f), strokeWidth = u * 0.0018f, cap = StrokeCap.Round)
    }
    drawOval(Color.White.copy(alpha = 0.3f), Offset(p.x - u * 0.03f, p.y - u * 0.002f), Size(u * 0.06f, u * 0.008f))
    // The flat, scaly tail just breaking the surface.
    drawOval(dark, Offset(p.x + u * 0.02f, p.y - u * 0.004f), Size(u * 0.018f, u * 0.006f))
    // Back and head above the water line.
    drawPath(Path().apply {
        moveTo(p.x - u * 0.024f, p.y); cubicTo(p.x - u * 0.02f, p.y - u * 0.016f, p.x + u * 0.012f, p.y - u * 0.018f, p.x + u * 0.022f, p.y); close()
    }, shade(fur, p + Offset(0f, -u * 0.008f), u * 0.02f))
    val head = p + Offset(-u * 0.022f, -u * 0.008f)
    drawCircle(shade(fur, head, u * 0.01f), u * 0.0095f, head)
    drawCircle(fur, u * 0.003f, head + Offset(u * 0.004f, -u * 0.008f)) // ear
    drawOval(Color(0xFF9C7350), Offset(head.x - u * 0.013f, head.y - u * 0.001f), Size(u * 0.01f, u * 0.007f))
    drawCircle(Eye, u * 0.0022f, head + Offset(-u * 0.012f, u * 0.0f))
    eye(head + Offset(-u * 0.003f, -u * 0.003f), u * 0.002f)
    drawRect(Color(0xFFF1E9D8), Offset(head.x - u * 0.011f, head.y + u * 0.005f), Size(u * 0.003f, u * 0.0025f)) // teeth
}
