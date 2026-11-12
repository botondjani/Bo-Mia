package app.tend.ui

import app.tend.core.tr

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Insights
import app.tend.core.InsightsOf
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** One picked app, before tend and now (minutes a day). */
class AppTime(val label: String, val icon: ImageBitmap?, val before: Int, val now: Int)

/**
 * What tend has given back: the time won back (the headline), before against now, the weeks, each
 * app, and what was done instead.
 */
@Composable
fun InsightsScreen(insights: Insights, apps: List<AppTime>, plus: Boolean = true, onPlus: () -> Unit = {}, onBack: () -> Unit) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = Space.xs, bottom = Space.xxl),
    verticalArrangement = Arrangement.spacedBy(Space.m),
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButtonSoft(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, tr("Back", "Vissza"), onBack)
        Spacer(Modifier.width(Space.m))
        Column {
            Text(tr("What tend gave back", "Mit adott vissza a tend"), style = MaterialTheme.typography.bodyMedium)
            Text(tr("Your time", "Az időd"), style = MaterialTheme.typography.headlineMedium)
        }
    }

    // The headline: time won back.
    Card {
        Text(tr("WON BACK", "VISSZANYERVE"), style = MaterialTheme.typography.labelSmall)
        if (insights.normal == null) {
            Text(tr("Soon", "Hamarosan"), style = MaterialTheme.typography.headlineLarge, color = Brand.Moss)
            Text(tr("After your first week tend knows your normal, and counts every minute below it.", "Az első heted után a tend ismeri a megszokott idődet, és minden percet számol, amivel alatta maradsz."), style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(InsightsOf.hours(insights.wonBack), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp), color = Brand.Moss)
            Text(
                listOfNotNull(
                    insights.since?.let { tr("since ${it.format(DateTimeFormatter.ofPattern("MMMM d", app.tend.core.Lang.locale))}", "${it.format(DateTimeFormatter.ofPattern("MMMM d.", app.tend.core.Lang.locale))} óta") },
                    InsightsOf.likeWhat(insights.wonBack)?.let { tr("that's $it", "ez $it") },
                ).joinToString(" · ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    // Before tend against the last week.
    val normal = insights.normal
    val last = insights.lastWeek
    if (normal != null && last != null) Card {
        Text(tr("A DAY IN THE PICKED APPS", "EGY NAP A KIVÁLASZTOTT APPOKBAN"), style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(tr("Before tend", "A tend előtt"), style = MaterialTheme.typography.bodyMedium)
                Text(InsightsOf.hours(normal), style = MaterialTheme.typography.titleLarge, color = Brand.Muted)
            }
            Column(Modifier.weight(1f)) {
                Text(tr("Last 7 days", "Az elmúlt 7 nap"), style = MaterialTheme.typography.bodyMedium)
                Text(InsightsOf.hours(last), style = MaterialTheme.typography.titleLarge, color = Brand.Moss)
            }
            if (normal > 0) Change(normal, last)
        }
    }

    // Without Plus: the headline and before/now stay; the weeks and the apps wait behind one card.
    if (!plus) Card(Modifier.tappable(onClick = onPlus)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("EVERY WEEK, AND EACH APP", "MINDEN HÉT, MINDEN APP"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            PlusMark()
        }
        Text(tr("See how each week went, and how much less each app takes now.", "Nézd meg, hogy ment egy-egy hét, és mennyivel kevesebb időt visz el most egy-egy app."), style = MaterialTheme.typography.bodyMedium)
        Text(tr("See tend Plus →", "Megnézem a tend Plust →"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
    }

    // The weeks: minutes a day, against the normal.
    if (plus && insights.weeks.any { it.second != null }) Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("MINUTES A DAY, BY WEEK", "NAPI PERCEK, HETENTE"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            PlusMark()
        }
        WeeksChart(insights)
        Text(tr("Tap a week to see it. The dashed line is your normal before tend.", "Koppints egy hétre a részletekért. A szaggatott vonal a tend előtti megszokott időd."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }

    // Each app, before and now.
    if (plus && apps.isNotEmpty()) Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("BY APP, MINUTES A DAY", "APPONKÉNT, NAPI PERCEKBEN"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            PlusMark()
        }
        apps.take(6).forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                if (a.icon != null) Image(a.icon, null, Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)))
                else Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Brand.Mint))
                Spacer(Modifier.width(12.dp))
                Text(a.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f))
                Text("${a.before} → ${a.now}", style = MaterialTheme.typography.labelLarge, color = Brand.Ink)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.width(52.dp), contentAlignment = Alignment.CenterEnd) { if (a.before > 0) Change(a.before, a.now, small = true) }
            }
        }
    }

    // What was done instead.
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile("${insights.activities}", tr("activities done", "elvégzett tevékenység"), Modifier.weight(1f))
        Tile("${insights.activeDays}", tr("active days", "aktív nap"), Modifier.weight(1f))
        Tile("${insights.notNows}", tr("times \"Not now\"", "alkalommal „Most nem”"), Modifier.weight(1f))
    }
}

/** "−48%" in moss when it went down, "+12%" in muted ink when it went up. */
@Composable
private fun Change(before: Int, now: Int, small: Boolean = false) {
    val pct = ((now - before) * 100f / before).roundToInt()
    Text(
        if (pct <= 0) "−${-pct}%" else "+$pct%",
        style = if (small) MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp) else MaterialTheme.typography.titleMedium,
        color = if (pct <= 0) Brand.Moss else Brand.Muted,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (pct <= 0) Brand.Sage else Brand.Card).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier) = Column(
    modifier.surface(radius = 20.dp).padding(horizontal = 12.dp, vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text(value, style = MaterialTheme.typography.headlineMedium, color = Brand.Moss)
    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, textAlign = TextAlign.Center)
}

/**
 * One bar a week (average minutes a day), the normal as a dashed line. The last week, and a tapped
 * one, carry their value; the rest stay quiet.
 */
@Composable
private fun WeeksChart(insights: Insights) {
    // From the first week with tend: empty weeks before it would only take room.
    val weeks = insights.weeks.dropWhile { it.second == null }
    var picked by remember { mutableIntStateOf(weeks.lastIndex) }
    val top = (listOfNotNull(insights.normal) + weeks.mapNotNull { it.second }).maxOrNull()?.coerceAtLeast(30)?.times(1.2f) ?: 30f
    val ink = Brand.Ink
    val muted = Brand.Muted
    val moss = Brand.Moss
    val faint = Brand.Mint
    Canvas(
        Modifier.fillMaxWidth().height(150.dp).pointerInput(weeks.size) {
            detectTapGestures { at -> picked = (at.x / (size.width / weeks.size)).toInt().coerceIn(0, weeks.lastIndex) }
        },
    ) {
        val labelH = 18.dp.toPx()
        val plotH = size.height - labelH
        val slot = size.width / weeks.size
        val bar = (slot * 0.56f).coerceAtMost(28.dp.toPx())
        val text = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 11.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
        // Baseline only: the grid stays out of the way.
        drawLine(muted.copy(alpha = 0.35f), Offset(0f, plotH), Offset(size.width, plotH), 1.dp.toPx())
        weeks.forEachIndexed { i, (start, avg) ->
            val cx = slot * i + slot / 2
            val on = i == picked
            if (avg != null) {
                val h = (plotH * avg / top).coerceAtLeast(3.dp.toPx())
                // Rounded at the data end, square on the baseline.
                val r = 4.dp.toPx().coerceAtMost(h / 2)
                drawRoundRect(if (on) moss else faint, Offset(cx - bar / 2, plotH - h), Size(bar, h), CornerRadius(r))
                drawRect(if (on) moss else faint, Offset(cx - bar / 2, plotH - r), Size(bar, r))
                if (on) drawIntoCanvas {
                    text.color = ink.toArgb()
                    text.isFakeBoldText = true
                    it.nativeCanvas.drawText(tr("$avg min", "$avg perc"), cx, plotH - h - 6.dp.toPx(), text)
                }
            }
            drawIntoCanvas {
                text.color = (if (on) ink else muted).toArgb()
                text.isFakeBoldText = false
                it.nativeCanvas.drawText(start.format(DateTimeFormatter.ofPattern(tr("MMM d", "MMM d."), app.tend.core.Lang.locale)), cx, size.height - 3.dp.toPx(), text)
            }
        }
        insights.normal?.let { n ->
            val y = plotH - plotH * n / top
            drawLine(muted, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
            drawIntoCanvas {
                text.color = muted.toArgb()
                text.textAlign = android.graphics.Paint.Align.RIGHT
                it.nativeCanvas.drawText(tr("before tend $n", "a tend előtt $n"), size.width, y - 5.dp.toPx(), text)
                text.textAlign = android.graphics.Paint.Align.CENTER
            }
        }
    }
}
