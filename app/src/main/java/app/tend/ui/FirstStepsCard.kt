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
 * On the home page, one slim line: the next challenge (tap: go do it), or acorns waiting to be
 * claimed in the camp (tap: open it).
 */
@Composable
fun FirstStepsStrip(steps: List<StepState>, onOpen: () -> Unit, onGo: (Step) -> Unit = {}) {
    val round = app.tend.core.FirstSteps.current(steps)
    if (round.isEmpty()) return
    val waiting = round.filter { it.claimable }.sumOf { it.step.acorns }
    val next = round.firstOrNull { !it.done }?.step
    Row(
        Modifier.fillMaxWidth().surface(radius = 18.dp).tappable { if (waiting > 0 || next == null) onOpen() else onGo(next) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(Brand.Sage), contentAlignment = Alignment.Center) {
            Text("${round.count { it.claimed }}/${round.size}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.Moss)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("CHALLENGE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.Muted)
            Text(if (waiting > 0) "$waiting acorns to claim in the camp" else next?.title ?: "", style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
        Text("+${if (waiting > 0) waiting else next?.acorns ?: 0}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
        Spacer(Modifier.width(4.dp))
        AcornIcon(16.dp)
    }
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
