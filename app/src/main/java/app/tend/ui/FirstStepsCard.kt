package app.tend.ui

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
fun FirstStepsCard(steps: List<StepState>, onClaim: (Step) -> Unit, onGo: (Step) -> Unit = {}) {
    val round = app.tend.core.FirstSteps.current(steps)
    if (round.isEmpty()) return
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("CHALLENGES · ROUND ${app.tend.core.FirstSteps.roundOf(steps)} OF ${app.tend.core.FirstSteps.rounds}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("${round.count { it.claimed }} / ${round.size}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
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

/**
 * On the home page, the round at a glance: how many are done (a ring), what's next or waiting to be
 * claimed, and how much is still there to earn. Tap: go do the next one, or claim in the camp.
 */
@Composable
fun FirstStepsStrip(steps: List<StepState>, onOpen: () -> Unit, onGo: (Step) -> Unit = {}) {
    val round = app.tend.core.FirstSteps.current(steps)
    if (round.isEmpty()) return
    val waiting = round.filter { it.claimable }.sumOf { it.step.acorns }
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
            Text("CHALLENGES · ROUND ${app.tend.core.FirstSteps.roundOf(steps)}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.Muted)
            Text(if (waiting > 0) "$waiting acorns ready to claim" else next?.title ?: "All done!", style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                if (toEarn > 0) "$toEarn more to earn this round" else "Claim them in the camp",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, maxLines = 1,
            )
        }
        Text("+${if (waiting > 0) waiting else next?.acorns ?: 0}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
        Spacer(Modifier.width(4.dp))
        AcornIcon(16.dp)
    }
}

/**
 * Bo's quest for today, under the challenges: what it is, how, and its acorns; once done, a tap
 * claims them (and it says so for the rest of the day).
 */
@Composable
fun QuestCard(quest: app.tend.core.Quest, done: Boolean, claimed: Boolean, name: String, onClaim: () -> Unit) = Row(
    Modifier.fillMaxWidth().surface(radius = 18.dp).then(if (done && !claimed) Modifier.tappable(onClick = onClaim) else Modifier)
        .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    TendIcons.Badge("star", 38.dp)
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
        Text("${name.uppercase()}'S QUEST TODAY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.Muted)
        Text(quest.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, color = if (claimed) Brand.Muted else Brand.Ink)
        Text(
            when { claimed -> "Done. A new one tomorrow."; done -> "Done! Tap to claim."; else -> quest.how },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, maxLines = 1,
        )
    }
    if (done && !claimed) Text(
        "+${quest.acorns}", style = MaterialTheme.typography.labelLarge, color = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss).padding(horizontal = 12.dp, vertical = 6.dp),
    ) else {
        Text(if (claimed) "\u2713" else "+${quest.acorns}", style = MaterialTheme.typography.labelLarge, color = if (claimed) Brand.Moss else Brand.Muted)
        if (!claimed) { Spacer(Modifier.width(4.dp)); AcornIcon(16.dp) }
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
        Text("YOUR WEEK", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
        Text("\u2715", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.clip(CircleShape).tappable(onClick = onDone).padding(6.dp))
    }
    val h = gotBackMinutes / 60
    val m = gotBackMinutes % 60
    Text(
        if (gotBackMinutes <= 0) "A week at the camp" else (if (h > 0) "$h h ${if (m > 0) "$m min " else ""}" else "$m min ") + "back from scrolling",
        style = MaterialTheme.typography.headlineSmall, color = Color.White,
    )
    Text(
        "$activeDays day${if (activeDays == 1) "" else "s"} with something done" + if (letGo > 0) " · $letGo scroll${if (letGo == 1) "" else "s"} let go" else "",
        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
    )
    Text(
        "Share your week", style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
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
