package app.tend.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.tend.core.Avatar
import app.tend.core.Mood

/** Where the tour's targets are on screen, keyed by name. */
class TourTargets {
    val bounds = mutableStateMapOf<String, Rect>()
}

fun Modifier.tourTarget(targets: TourTargets, key: String) =
    onGloballyPositioned { targets.bounds[key] = it.boundsInWindow() }

class TourStep(val key: String, val title: String, val body: String)

/**
 * First-run tour of the home screen: everything dims except one thing at a time, and Bo in his lab
 * coat explains it. Steps whose target isn't on screen are skipped.
 */
@Composable
fun TourOverlay(avatar: Avatar, targets: TourTargets, steps: List<TourStep>, onDone: () -> Unit) {
    val shown = steps.filter { it.key in targets.bounds }
    if (shown.isEmpty()) return
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = shown[index.coerceAtMost(shown.lastIndex)]
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val pad = with(density) { 8.dp.toPx() }
    val target = targets.bounds.getValue(step.key).translate(-origin).inflate(pad)
    val l by animateFloatAsState(target.left, tween(350), label = "l")
    val t by animateFloatAsState(target.top, tween(350), label = "t")
    val r by animateFloatAsState(target.right, tween(350), label = "r")
    val b by animateFloatAsState(target.bottom, tween(350), label = "b")
    val last = index >= shown.lastIndex

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            // Taps on the dimmed screen do nothing; the tour moves on with its own buttons.
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Brand.Shade.copy(alpha = 0.72f))
            val radius = CornerRadius(24.dp.toPx())
            drawRoundRect(Color.Black, Offset(l, t), Size(r - l, b - t), radius, blendMode = BlendMode.Clear)
            drawRoundRect(Brand.Mint, Offset(l, t), Size(r - l, b - t), radius, style = Stroke(2.dp.toPx()))
        }
        // The explanation goes under the target, or above it when the target is low on screen.
        val below = (t + b) / 2 < constraints.maxHeight / 2
        val gap = with(density) { 12.dp.toPx() }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).layout { measurable, constraints ->
                val p = measurable.measure(constraints.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
                val y = if (below) b + gap else t - gap - p.height
                layout(p.width, p.height) { p.place(0, y.toInt().coerceAtLeast(0)) }
            },
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                CharacterView(avatar, Mood.HAPPY, 84.dp, scientist = true)
                Column(
                    Modifier.weight(1f).surface(radius = 22.dp).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AnimatedContent(step, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "tour") { s ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(s.title, style = MaterialTheme.typography.titleMedium)
                            Text(brandTend(s.body), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                        Text("${index + 1} of ${shown.size}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        if (!last) Text(
                            "Skip", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).tappable(onClick = onDone).padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        ChipButton(if (last) "Got it" else "Next", { if (last) onDone() else index++ })
                    }
                }
            }
        }
    }
}
