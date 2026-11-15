package app.tend.ui

import app.tend.core.tr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.drawText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.WeekView
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * The last seven days: per day, a light bar of the phone's whole screen time ([total], when known)
 * with the minutes that counted (the picked apps and the counted kinds) in it, red on a day they ran
 * out; a dashed mark for that day's minutes (free + earned), and the average against the week before.
 */
@Composable
fun WeekCard(view: WeekView, onOpen: (() -> Unit)? = null, total: List<Int?> = emptyList()) = Card(if (onOpen != null) Modifier.tappable(onClick = onOpen) else Modifier) {
    // A day tapped: its own minutes in the corner instead of the average (tapped again: back).
    var picked by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    val all = view.days.indices.map { total.getOrNull(it - view.days.size + total.size) }
    Row {
        Text(tr("YOUR WEEK", "A HETED"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        val p = picked
        if (p != null) {
            val day = if (p == 6) tr("Today", "Ma") else java.time.LocalDate.now().minusDays((6 - p).toLong()).dayOfWeek.getDisplayName(TextStyle.FULL, app.tend.core.Lang.locale).replaceFirstChar { it.uppercase() }
            val used = view.days[p]?.usedMinutes
            val whole = all[p]
            Text(
                "$day: " + (used?.let { tr("$it min", "$it perc") } ?: "–") + (whole?.let { tr(" · all $it", " · összesen $it") } ?: ""),
                style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
            )
        } else view.average?.let { Text(tr("avg $it min a day", "átlag napi $it perc"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss) }
    }
    // A little headroom above the tallest bar or mark.
    val top = 1.15f * (view.days.indices.maxOfOrNull { i -> maxOf(all[i] ?: 0, view.days[i]?.let { it.usedMinutes + it.unusedMinutes } ?: 0) }?.coerceAtLeast(30) ?: 30)
    // Hour lines with their labels in a narrow gutter on the left, so the bars can be read.
    val gutter = 30.dp
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 9.sp, color = Brand.Muted)
    val step = when { top <= 150 -> 30; top <= 330 -> 60; top <= 660 -> 120; else -> 240 }
    Canvas(Modifier.fillMaxWidth().height(110.dp).pointerInput(Unit) {
        detectTapGestures { o -> val g = gutter.toPx(); val i = ((o.x - g) / ((size.width - g) / 7f)).toInt().coerceIn(0, 6); picked = if (picked == i) null else i }
    }) {
        val g = gutter.toPx()
        var m = step
        while (m < top) {
            val y = size.height - size.height * m / top
            drawLine(Brand.Muted.copy(alpha = 0.18f), Offset(g, y), Offset(size.width, y), 1.dp.toPx())
            val label = if (m % 60 == 0) tr("${m / 60} h", "${m / 60} ó") else tr("$m m", "$m p")
            val t = measurer.measure(label, labelStyle)
            drawText(t, topLeft = Offset(g - t.size.width - 4.dp.toPx(), y - t.size.height / 2f))
            m += step
        }
        val slot = (size.width - g) / 7
        val bar = slot * 0.46f
        view.days.forEachIndexed { i, d ->
            val x = g + slot * i + (slot - bar) / 2
            // The whole screen time behind: a light bar the counted minutes sit in.
            all[i]?.let { t ->
                val th = size.height * maxOf(t, d?.usedMinutes ?: 0) / top
                if (th > 0f) drawRoundRect(Brand.Mint.copy(alpha = 0.45f), Offset(x, size.height - th), Size(bar, th), CornerRadius(4.dp.toPx()))
            }
            // An empty day: a faint stub, so the gap reads as "nothing logged", not "zero".
            if (d == null) {
                drawRoundRect(Brand.Muted.copy(alpha = 0.18f), Offset(x, size.height - 3.dp.toPx()), Size(bar, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
                return@forEachIndexed
            }
            val budget = d.usedMinutes + d.unusedMinutes
            val over = d.unusedMinutes == 0 && d.usedMinutes > 0
            val h = (size.height * d.usedMinutes / top).coerceAtLeast(3.dp.toPx())
            drawRoundRect(
                (if (over) Brand.Coral else Brand.Moss).copy(alpha = if (i == (picked ?: 6)) 1f else 0.7f),
                Offset(x, size.height - h), Size(bar, h), CornerRadius(4.dp.toPx()),
            )
            if (!over) {
                val y = size.height - size.height * budget / top
                drawLine(
                    Brand.Muted.copy(alpha = 0.7f), Offset(x - 3.dp.toPx(), y), Offset(x + bar + 3.dp.toPx(), y), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())),
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Spacer(Modifier.width(gutter))
        view.days.indices.forEach { i ->
            val date = java.time.LocalDate.now().minusDays((6 - i).toLong())
            Text(
                if (i == 6) tr("Today", "Ma") else date.dayOfWeek.getDisplayName(TextStyle.SHORT, app.tend.core.Lang.locale),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp), color = Brand.Muted,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
        }
    }
    val a = view.average
    val b = view.before
    Spacer(Modifier.height(2.dp))
    if (a != null && b != null) Text(
        when {
            abs(a - b) < 5 -> tr("About the same as the week before.", "Nagyjából ugyanannyi, mint az előző héten.")
            a < b -> tr("${b - a} min a day less than the week before.", "Napi ${b - a} perccel kevesebb, mint az előző héten.")
            else -> tr("${a - b} min a day more than the week before.", "Napi ${a - b} perccel több, mint az előző héten.")
        },
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
    )
    // The key: a swatch for each colour, and the dashes.
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        @Composable
        fun Key(label: String, draw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) = Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Canvas(Modifier.size(12.dp), onDraw = draw)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted)
        }
        if (total.isNotEmpty()) Key(tr("All screen time", "Teljes képernyőidő")) { drawRoundRect(Brand.Mint.copy(alpha = 0.45f), cornerRadius = CornerRadius(3.dp.toPx())) }
        Key(tr("Picked apps", "Kiválasztott appok")) { drawRoundRect(Brand.Moss.copy(alpha = 0.8f), cornerRadius = CornerRadius(3.dp.toPx())) }
        Key(tr("Over the limit", "Túllépve")) { drawRoundRect(Brand.Coral, cornerRadius = CornerRadius(3.dp.toPx())) }
        Key(tr("Day's minutes", "Napi keret")) {
            drawLine(Brand.Muted.copy(alpha = 0.7f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 2.dp.toPx())))
        }
    }
    if (onOpen != null) Text(tr("See all your time →", "Az összes időd →"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
}
