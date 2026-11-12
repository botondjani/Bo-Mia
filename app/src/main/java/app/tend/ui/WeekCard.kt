package app.tend.ui

import app.tend.core.tr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * The last seven days: a bar of minutes used per day, a dashed mark for that day's minutes
 * (free + earned), and the average against the week before.
 */
@Composable
fun WeekCard(view: WeekView, onOpen: (() -> Unit)? = null) = Card(if (onOpen != null) Modifier.tappable(onClick = onOpen) else Modifier) {
    Row {
        Text(tr("YOUR WEEK", "A HETED"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        view.average?.let { Text(tr("avg $it min a day", "átlag napi $it perc"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss) }
    }
    // A little headroom above the tallest bar or mark.
    val top = 1.15f * (view.days.filterNotNull().maxOfOrNull { maxOf(it.usedMinutes, it.usedMinutes + it.unusedMinutes) }?.coerceAtLeast(30) ?: 30)
    Canvas(Modifier.fillMaxWidth().height(76.dp)) {
        val slot = size.width / 7
        val bar = slot * 0.46f
        view.days.forEachIndexed { i, d ->
            val x = slot * i + (slot - bar) / 2
            // An empty day: a faint stub, so the gap reads as "nothing logged", not "zero".
            if (d == null) {
                drawRoundRect(Brand.Muted.copy(alpha = 0.18f), Offset(x, size.height - 3.dp.toPx()), Size(bar, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
                return@forEachIndexed
            }
            val budget = d.usedMinutes + d.unusedMinutes
            val over = d.unusedMinutes == 0 && d.usedMinutes > 0
            val h = (size.height * d.usedMinutes / top).coerceAtLeast(3.dp.toPx())
            drawRoundRect(
                (if (over) Brand.Coral else Brand.Moss).copy(alpha = if (i == 6) 1f else 0.8f),
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
    Text(
        when {
            a == null -> ""
            b == null -> tr("Dashes: each day's minutes. Red: a day they ran out.", "Vonalak: az egyes napok percei. Piros: aznap elfogytak.")
            abs(a - b) < 5 -> tr("About the same as the week before.", "Nagyjából ugyanannyi, mint az előző héten.")
            a < b -> tr("${b - a} min a day less than the week before.", "Napi ${b - a} perccel kevesebb, mint az előző héten.")
            else -> tr("${a - b} min a day more than the week before.", "Napi ${a - b} perccel több, mint az előző héten.")
        },
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
    )
    if (onOpen != null) Text(tr("See all your time →", "Az összes időd →"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
}
