package app.tend.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    if (v != Visitor.SQUIRREL && v != Visitor.OWL) drawOval(Color.Black.copy(alpha = 0.13f), Offset(p.x - u * 0.035f, p.y - u * 0.006f), Size(u * 0.07f, u * 0.012f))
    when (v) {
        Visitor.DEER -> deer(p, u)
        Visitor.FOX -> fox(p, u)
        Visitor.RABBIT -> rabbit(p, u)
        Visitor.SQUIRREL -> squirrel(p, u)
        Visitor.HERON -> heron(p, u)
        Visitor.OWL -> owl(p, u)
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
    val (w, h) = when (v) { Visitor.DEER -> 0.1f to 0.13f; Visitor.HERON -> 0.07f to 0.12f; Visitor.SQUIRREL -> 0.05f to 0.05f; Visitor.OWL -> 0.045f to 0.06f; else -> 0.08f to 0.06f }
    val u = minOf(s * 0.85f / w, s * 0.8f / h)
    val base = Offset(size.width / 2, size.height / 2 + s * 0.4f)
    when (v) {
        Visitor.SQUIRREL -> squirrel(base + Offset(0f, -u * 0.01f), u, onTrunk = false)
        Visitor.OWL -> owl(base, u)
        else -> drawVisitor(v, base, u)
    }
}

private fun DrawScope.deer(p: Offset, u: Float) {
    val body = Color(0xFFB07A4A)
    val dark = Color(0xFF8A5B33)
    for ((dx, c) in listOf(-0.026f to dark, -0.018f to body, 0.018f to dark, 0.026f to body))
        drawLine(c, p + Offset(u * dx, -u * 0.04f), p + Offset(u * dx, 0f), strokeWidth = u * 0.005f, cap = StrokeCap.Round)
    drawOval(body, Offset(p.x - u * 0.036f, p.y - u * 0.062f), Size(u * 0.07f, u * 0.03f))
    drawOval(Cream, Offset(p.x - u * 0.02f, p.y - u * 0.042f), Size(u * 0.036f, u * 0.01f))
    for ((dx, dy) in listOf(-0.016f to 0.055f, -0.004f to 0.058f, 0.008f to 0.054f)) drawCircle(Cream, u * 0.0025f, p + Offset(u * dx, -u * dy))
    drawCircle(Cream, u * 0.005f, p + Offset(u * 0.034f, -u * 0.058f)) // tail
    // Neck and head, looking at us.
    drawPath(Path().apply { moveTo(p.x - u * 0.03f, p.y - u * 0.055f); lineTo(p.x - u * 0.04f, p.y - u * 0.092f); lineTo(p.x - u * 0.026f, p.y - u * 0.094f); lineTo(p.x - u * 0.016f, p.y - u * 0.058f); close() }, body)
    val head = p + Offset(-u * 0.035f, -u * 0.1f)
    drawOval(body, Offset(head.x - u * 0.011f, head.y - u * 0.009f), Size(u * 0.022f, u * 0.02f))
    drawOval(dark, Offset(head.x - u * 0.007f, head.y + u * 0.004f), Size(u * 0.014f, u * 0.01f))
    drawCircle(Eye, u * 0.0022f, head + Offset(-u * 0.005f, -u * 0.002f))
    drawCircle(Eye, u * 0.0022f, head + Offset(u * 0.005f, -u * 0.002f))
    drawCircle(Eye, u * 0.0025f, head + Offset(0f, u * 0.008f))
    for (side in listOf(-1f, 1f)) {
        rotate(side * 35f, head + Offset(side * u * 0.012f, -u * 0.006f)) {
            drawOval(body, Offset(head.x + side * u * 0.012f - u * 0.006f, head.y - u * 0.01f), Size(u * 0.012f, u * 0.007f))
        }
        // Small antlers.
        drawLine(dark, head + Offset(side * u * 0.004f, -u * 0.008f), head + Offset(side * u * 0.012f, -u * 0.028f), strokeWidth = u * 0.0025f, cap = StrokeCap.Round)
        drawLine(dark, head + Offset(side * u * 0.009f, -u * 0.02f), head + Offset(side * u * 0.017f, -u * 0.022f), strokeWidth = u * 0.002f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.fox(p: Offset, u: Float) {
    val fur = Color(0xFFE07B39)
    val dark = Color(0xFF3B2A20)
    // Sitting: a bushy tail curled around, a round body, a pointed head.
    drawPath(Path().apply { moveTo(p.x + u * 0.012f, p.y - u * 0.004f); quadraticTo(p.x + u * 0.05f, p.y, p.x + u * 0.045f, p.y - u * 0.03f); quadraticTo(p.x + u * 0.035f, p.y - u * 0.012f, p.x + u * 0.01f, p.y - u * 0.012f); close() }, fur)
    drawCircle(Cream, u * 0.006f, p + Offset(u * 0.044f, -u * 0.028f))
    drawOval(fur, Offset(p.x - u * 0.018f, p.y - u * 0.04f), Size(u * 0.036f, u * 0.04f))
    drawOval(Cream, Offset(p.x - u * 0.01f, p.y - u * 0.034f), Size(u * 0.016f, u * 0.024f))
    for (dx in listOf(-0.009f, 0.004f)) drawLine(dark, p + Offset(u * dx, -u * 0.008f), p + Offset(u * dx, 0f), strokeWidth = u * 0.004f, cap = StrokeCap.Round)
    val head = p + Offset(0f, -u * 0.05f)
    drawPath(Path().apply { moveTo(head.x - u * 0.016f, head.y - u * 0.006f); lineTo(head.x + u * 0.016f, head.y - u * 0.006f); lineTo(head.x, head.y + u * 0.014f); close() }, fur)
    drawPath(Path().apply { moveTo(head.x - u * 0.008f, head.y + u * 0.002f); lineTo(head.x + u * 0.008f, head.y + u * 0.002f); lineTo(head.x, head.y + u * 0.014f); close() }, Cream)
    for (side in listOf(-1f, 1f)) drawPath(Path().apply { moveTo(head.x + side * u * 0.016f, head.y - u * 0.006f); lineTo(head.x + side * u * 0.014f, head.y - u * 0.022f); lineTo(head.x + side * u * 0.004f, head.y - u * 0.006f); close() }, fur)
    drawCircle(Eye, u * 0.0022f, head + Offset(-u * 0.006f, -u * 0.001f))
    drawCircle(Eye, u * 0.0022f, head + Offset(u * 0.006f, -u * 0.001f))
    drawCircle(Eye, u * 0.002f, head + Offset(0f, u * 0.013f))
}

private fun DrawScope.rabbit(p: Offset, u: Float) {
    val fur = Color(0xFFA89B8C)
    drawOval(fur, Offset(p.x - u * 0.016f, p.y - u * 0.026f), Size(u * 0.032f, u * 0.026f))
    drawCircle(Color.White, u * 0.006f, p + Offset(u * 0.016f, -u * 0.016f))
    val head = p + Offset(-u * 0.014f, -u * 0.03f)
    drawCircle(fur, u * 0.01f, head)
    for (dx in listOf(-0.004f, 0.003f)) {
        drawOval(fur, Offset(head.x + u * dx - u * 0.003f, head.y - u * 0.03f), Size(u * 0.006f, u * 0.024f))
        drawOval(Color(0xFFE5B5B5), Offset(head.x + u * dx - u * 0.0015f, head.y - u * 0.026f), Size(u * 0.003f, u * 0.016f))
    }
    drawCircle(Eye, u * 0.002f, head + Offset(-u * 0.004f, -u * 0.001f))
    drawCircle(Color(0xFFD98E8E), u * 0.0016f, head + Offset(-u * 0.009f, u * 0.003f))
}

private fun DrawScope.squirrel(p: Offset, u: Float, onTrunk: Boolean = true) {
    val fur = Color(0xFFB4562C)
    val at = if (onTrunk) p else p
    // Big curled tail behind, a small body, holding an acorn.
    drawPath(Path().apply { moveTo(at.x + u * 0.004f, at.y); cubicTo(at.x + u * 0.035f, at.y - u * 0.005f, at.x + u * 0.03f, at.y - u * 0.05f, at.x + u * 0.012f, at.y - u * 0.045f); quadraticTo(at.x + u * 0.022f, at.y - u * 0.02f, at.x + u * 0.002f, at.y - u * 0.012f); close() }, fur)
    drawOval(fur, Offset(at.x - u * 0.012f, at.y - u * 0.026f), Size(u * 0.02f, u * 0.026f))
    drawOval(Cream, Offset(at.x - u * 0.008f, at.y - u * 0.02f), Size(u * 0.009f, u * 0.016f))
    val head = at + Offset(-u * 0.004f, -u * 0.031f)
    drawCircle(fur, u * 0.0085f, head)
    drawPath(Path().apply { moveTo(head.x - u * 0.002f, head.y - u * 0.006f); lineTo(head.x + u * 0.001f, head.y - u * 0.016f); lineTo(head.x + u * 0.005f, head.y - u * 0.005f); close() }, fur)
    drawCircle(Eye, u * 0.0018f, head + Offset(-u * 0.004f, -u * 0.001f))
    drawOval(Color(0xFFB9783F), Offset(at.x - u * 0.012f, at.y - u * 0.02f), Size(u * 0.006f, u * 0.007f))
}

private fun DrawScope.heron(p: Offset, u: Float) {
    val grey = Color(0xFF9AA5AD)
    val dark = Color(0xFF55606A)
    // Standing in the shallows on thin legs, neck in an S.
    drawLine(Color(0xFFC8A04A), p, p + Offset(0f, -u * 0.045f), strokeWidth = u * 0.0025f)
    drawLine(Color(0xFFC8A04A), p + Offset(u * 0.006f, 0f), p + Offset(u * 0.004f, -u * 0.045f), strokeWidth = u * 0.0025f)
    drawOval(Color.White.copy(alpha = 0.35f), Offset(p.x - u * 0.016f, p.y - u * 0.003f), Size(u * 0.034f, u * 0.006f))
    drawPath(Path().apply { moveTo(p.x - u * 0.018f, p.y - u * 0.05f); quadraticTo(p.x + u * 0.01f, p.y - u * 0.04f, p.x + u * 0.03f, p.y - u * 0.055f); quadraticTo(p.x + u * 0.01f, p.y - u * 0.075f, p.x - u * 0.018f, p.y - u * 0.05f); close() }, grey)
    drawPath(Path().apply { moveTo(p.x - u * 0.005f, p.y - u * 0.055f); quadraticTo(p.x + u * 0.01f, p.y - u * 0.05f, p.x + u * 0.026f, p.y - u * 0.056f) }, dark, style = Stroke(u * 0.002f))
    drawPath(Path().apply { moveTo(p.x - u * 0.014f, p.y - u * 0.058f); cubicTo(p.x - u * 0.03f, p.y - u * 0.075f, p.x - u * 0.005f, p.y - u * 0.09f, p.x - u * 0.016f, p.y - u * 0.105f) }, grey, style = Stroke(u * 0.006f, cap = StrokeCap.Round))
    val head = p + Offset(-u * 0.016f, -u * 0.108f)
    drawCircle(grey, u * 0.006f, head)
    drawLine(Color(0xFFD9A440), head + Offset(-u * 0.004f, 0f), head + Offset(-u * 0.026f, u * 0.004f), strokeWidth = u * 0.003f, cap = StrokeCap.Round)
    drawLine(dark, head + Offset(u * 0.002f, -u * 0.003f), head + Offset(u * 0.016f, -u * 0.001f), strokeWidth = u * 0.0018f, cap = StrokeCap.Round)
    drawCircle(Eye, u * 0.0016f, head + Offset(-u * 0.002f, -u * 0.001f))
}

private fun DrawScope.owl(p: Offset, u: Float) {
    val feather = Color(0xFF8C6A4A)
    val light = Color(0xFFD9C3A3)
    drawLine(Color(0xFF452F21), p + Offset(-u * 0.03f, 0f), p + Offset(u * 0.03f, -u * 0.004f), strokeWidth = u * 0.005f, cap = StrokeCap.Round) // branch
    drawOval(feather, Offset(p.x - u * 0.016f, p.y - u * 0.042f), Size(u * 0.032f, u * 0.042f))
    drawOval(light, Offset(p.x - u * 0.009f, p.y - u * 0.028f), Size(u * 0.018f, u * 0.024f))
    for (side in listOf(-1f, 1f)) {
        drawCircle(Color(0xFFF1E3C6), u * 0.0075f, p + Offset(side * u * 0.0075f, -u * 0.036f))
        drawCircle(Color(0xFFE8A93A), u * 0.0045f, p + Offset(side * u * 0.0075f, -u * 0.036f))
        drawCircle(Eye, u * 0.0025f, p + Offset(side * u * 0.0075f, -u * 0.036f))
        drawPath(Path().apply { moveTo(p.x + side * u * 0.008f, p.y - u * 0.044f); lineTo(p.x + side * u * 0.015f, p.y - u * 0.052f); lineTo(p.x + side * u * 0.016f, p.y - u * 0.04f); close() }, feather)
    }
    drawPath(Path().apply { moveTo(p.x - u * 0.002f, p.y - u * 0.032f); lineTo(p.x + u * 0.002f, p.y - u * 0.032f); lineTo(p.x, p.y - u * 0.026f); close() }, Color(0xFFC98A3A))
}
