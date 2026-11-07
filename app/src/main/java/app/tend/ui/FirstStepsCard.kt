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

/** The first steps on the clearing page: each with how to do it, and its acorns to claim. */
@Composable
fun FirstStepsCard(steps: List<StepState>, onClaim: (Step) -> Unit) {
    if (steps.all { it.claimed }) return
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FIRST STEPS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("${steps.count { it.claimed }} / ${steps.size}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
        }
        steps.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Check(s.done)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.step.title, style = MaterialTheme.typography.titleSmall, color = if (s.claimed) Brand.Muted else MaterialTheme.colorScheme.onSurface)
                    if (!s.done) Text(s.step.how, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
                }
                when {
                    s.claimable -> Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss).tappable { onClaim(s.step) }.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("+${s.step.acorns}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Spacer(Modifier.width(4.dp))
                        AcornIcon(14.dp)
                    }
                    s.claimed -> Text("+${s.step.acorns}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Brand.Muted)
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("+${s.step.acorns}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Brand.Muted)
                        Spacer(Modifier.width(3.dp))
                        AcornIcon(12.dp)
                    }
                }
            }
        }
    }
}

/** On the home page: how far the first steps are, and acorns waiting; opens the clearing. */
@Composable
fun FirstStepsStrip(steps: List<StepState>, onOpen: () -> Unit) {
    if (steps.all { it.claimed }) return
    val waiting = steps.filter { it.claimable }.sumOf { it.step.acorns }
    Row(
        Modifier.fillMaxWidth().surface(radius = 22.dp).tappable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("FIRST STEPS · ${steps.count { it.claimed }} OF ${steps.size}", style = MaterialTheme.typography.labelSmall)
            val next = steps.firstOrNull { !it.done }?.step
            Text(
                if (waiting > 0) "$waiting acorns waiting for you in the camp" else next?.let { "Next: ${it.title.replaceFirstChar { c -> c.lowercase() }}" } ?: "",
                style = MaterialTheme.typography.titleSmall,
            )
            if (waiting == 0 && next != null) Text(next.how, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Sage)) {
                Box(Modifier.fillMaxWidth(steps.count { it.claimed } / steps.size.toFloat()).height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Moss))
            }
        }
        Spacer(Modifier.width(12.dp))
        AcornIcon(28.dp)
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
