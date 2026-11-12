package app.tend.ui

import app.tend.core.tr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Step
import app.tend.core.StepState

/**
 * The challenges, compact: this round's few, each a single line with its acorns. Tap one not
 * done yet to go where it's done; tap the acorns of a done one to claim them.
 */
@Composable
fun FirstStepsCard(steps: List<StepState>, onClaim: (Step) -> Unit, onGo: (Step) -> Unit = {}, onFold: (() -> Unit)? = null) {
    val round = app.tend.core.FirstSteps.current(steps)
    if (round.isEmpty()) return
    Card {
        // Tapped (where it folds), the header folds it back into the strip.
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).then(if (onFold != null) Modifier.tappable(onClick = onFold) else Modifier), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("CHALLENGES · ROUND ${app.tend.core.FirstSteps.roundOf(steps)} OF ${app.tend.core.FirstSteps.rounds}", "KIHÍVÁSOK · ${app.tend.core.FirstSteps.roundOf(steps)}. KÖR A ${app.tend.core.FirstSteps.rounds}-BŐL"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("${round.count { it.claimed }} / ${round.size}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
            if (onFold != null) Text(" \u2303", style = MaterialTheme.typography.labelLarge, color = Brand.Muted)
        }
        val next = round.firstOrNull { !it.done }
        round.forEach { s ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).then(if (!s.done) Modifier.tappable { onGo(s.step) } else Modifier).padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Check(s.done)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.step.title, style = MaterialTheme.typography.labelLarge, color = if (s.claimed) Brand.Muted else MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    if (s == next) Text(s.step.how, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted, maxLines = 1)
                }
                when {
                    s.claimable -> Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss).tappable { onClaim(s.step) }.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("+${s.step.acorns}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = Color.White)
                        Spacer(Modifier.width(3.dp))
                        AcornIcon(12.dp)
                    }
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("+${s.step.acorns}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Brand.Muted)
                        Spacer(Modifier.width(3.dp))
                        AcornIcon(11.dp)
                    }
                }
            }
        }
    }
}

/** Challenges done and waiting: a row each, claimed right there (above the folded strip). */
@Composable
fun ClaimableSteps(steps: List<StepState>, onClaim: (Step) -> Unit) {
    app.tend.core.FirstSteps.current(steps).filter { it.claimable }.forEach { s ->
        Row(
            Modifier.fillMaxWidth().surface(radius = 18.dp).tappable { onClaim(s.step) }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Check(true)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.step.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(tr("Done! Tap to claim", "Megvan! Koppints, és a tiéd"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted, maxLines = 1)
            }
            Row(Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("+${s.step.acorns}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = Color.White)
                Spacer(Modifier.width(3.dp))
                AcornIcon(12.dp)
            }
        }
    }
}

/**
 * On the home page, the round at a glance: how many are done (a ring), what's next or waiting to be
 * claimed, and how much is still there to earn. Tap: go do the next one, or claim in the camp.
 */
@Composable
fun FirstStepsStrip(steps: List<StepState>, onOpen: () -> Unit, onGo: (Step) -> Unit = {}, showWaiting: Boolean = true) {
    val round = app.tend.core.FirstSteps.current(steps)
    if (round.isEmpty()) return
    val waiting = if (showWaiting) round.filter { it.claimable }.sumOf { it.step.acorns } else 0
    val next = round.firstOrNull { !it.done }?.step
    val done = round.count { it.done }
    val toEarn = round.filter { !it.done }.sumOf { it.step.acorns }
    Row(
        Modifier.fillMaxWidth().surface(radius = 18.dp).tappable { if (waiting > 0 || next == null) onOpen() else onGo(next) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The round's progress: a ring filling up, done over all.
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            val k = done / round.size.toFloat()
            Canvas(Modifier.size(38.dp)) {
                val w = 3.5.dp.toPx()
                drawArc(Brand.Sage, 0f, 360f, false, Offset(w / 2, w / 2), androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = androidx.compose.ui.graphics.drawscope.Stroke(w))
                if (k > 0f) drawArc(Brand.Moss, -90f, 360f * k, false, Offset(w / 2, w / 2), androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = StrokeCap.Round))
            }
            Text("$done/${round.size}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.Moss)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("CHALLENGES · ROUND ${app.tend.core.FirstSteps.roundOf(steps)}", "KIHÍVÁSOK · ${app.tend.core.FirstSteps.roundOf(steps)}. KÖR"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.Muted)
            Text(if (waiting > 0) tr("$waiting acorns ready to claim", "$waiting makk vár rád") else next?.title ?: tr("All done!", "Mind megvan!"), style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                if (toEarn > 0) tr("$toEarn more to earn this round", "Ebben a körben még $toEarn makk szerezhető") else tr("Claim them in the camp", "A táborban veheted át"),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, maxLines = 1,
            )
        }
        Text("+${if (waiting > 0) waiting else next?.acorns ?: 0}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
        Spacer(Modifier.width(4.dp))
        AcornIcon(16.dp)
    }
}

/**
 * The week, told on Sunday evening and Monday: minutes got back, days with something done, and
 * the scrolls let go; shared as a card (the camp, the numbers, the way to tend), or put away.
 */
@Composable
fun WeekStoryCard(gotBackMinutes: Int, activeDays: Int, letGo: Int, onShare: () -> Unit, onDone: () -> Unit) = Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.Moss).padding(horizontal = 18.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(tr("YOUR WEEK", "A HETED"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
        Text("\u2715", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.clip(CircleShape).tappable(onClick = onDone).padding(6.dp))
    }
    val h = gotBackMinutes / 60
    val m = gotBackMinutes % 60
    Text(
        if (gotBackMinutes <= 0) tr("A week at the camp", "Egy hét a táborban") else (if (h > 0) tr("$h h ${if (m > 0) "$m min " else ""}", "$h ó ${if (m > 0) "$m p " else ""}") else tr("$m min ", "$m perc ")) + tr("back from scrolling", "visszanyerve a görgetéstől"),
        style = MaterialTheme.typography.headlineSmall, color = Color.White,
    )
    Text(
        tr("$activeDays day${if (activeDays == 1) "" else "s"} with something done", "$activeDays napon volt meg valami") + if (letGo > 0) tr(" · $letGo scroll${if (letGo == 1) "" else "s"} let go", " · $letGo elengedett görgetés") else "",
        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
    )
    Text(
        tr("Share your week", "Oszd meg a heted"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
        modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(Color.White).tappable(onClick = onShare).padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
private fun Check(done: Boolean) = Box(
    Modifier.size(22.dp).clip(CircleShape).background(if (done) Brand.Moss else Brand.Sage),
    contentAlignment = Alignment.Center,
) {
    if (done) Canvas(Modifier.size(12.dp)) {
        val s = size.minDimension
        drawLine(Color.White, Offset(s * 0.1f, s * 0.55f), Offset(s * 0.4f, s * 0.82f), strokeWidth = s * 0.18f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(s * 0.4f, s * 0.82f), Offset(s * 0.92f, s * 0.2f), strokeWidth = s * 0.18f, cap = StrokeCap.Round)
    }
}
