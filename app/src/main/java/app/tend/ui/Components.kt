package app.tend.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Spacing scale: every gap in the app is one of these. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** White surface with a soft, low shadow instead of borders. */
fun Modifier.surface(radius: Dp = 24.dp, color: Color = Brand.Card): Modifier = this
    .shadow(10.dp, RoundedCornerShape(radius), ambientColor = Brand.Shade.copy(alpha = 0.05f), spotColor = Brand.Shade.copy(alpha = 0.08f))
    .clip(RoundedCornerShape(radius))
    .background(color)

/** Tap with a gentle press-in and a light haptic tick. */
@Composable
fun Modifier.tappable(haptic: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 600f), label = "press")
    val haptics = LocalHapticFeedback.current
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(source, indication = null) {
            if (haptic) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) = Column(
    modifier.fillMaxWidth().surface().padding(horizontal = 20.dp, vertical = 18.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
) { content() }

@Composable
fun Pill(text: String, color: Color = Brand.Sage, modifier: Modifier = Modifier) = Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = Brand.Moss,
    modifier = modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 12.dp, vertical = 6.dp),
)

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) = Box(
    modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp))
        .background(if (enabled) Brand.Moss else Brand.Moss.copy(alpha = 0.25f))
        .then(if (enabled) Modifier.tappable(onClick = onClick) else Modifier),
    contentAlignment = Alignment.Center,
) { Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White) }

/** Quiet alternative to the primary action: tonal, never grey outlines. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) = Box(
    modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(18.dp))
        .background(Brand.Mint.copy(alpha = if (enabled) 0.22f else 0.1f))
        .then(if (enabled) Modifier.tappable(onClick = onClick) else Modifier),
    contentAlignment = Alignment.Center,
) { Text(text, style = MaterialTheme.typography.titleMedium, color = if (enabled) Brand.Moss else Brand.Muted) }

/** Small pill-shaped action at the end of a row ("Done", "Start"). */
@Composable
fun ChipButton(text: String, onClick: () -> Unit, filled: Boolean = true) = Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = if (filled) Color.White else Brand.Moss,
    textAlign = TextAlign.Center,
    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (filled) Brand.Moss else Brand.Mint.copy(alpha = 0.22f))
        .tappable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
)

@Composable
fun IconButtonSoft(icon: ImageVector, description: String, onClick: () -> Unit) = Box(
    Modifier.size(44.dp).surface(radius = 22.dp).tappable(onClick = onClick),
    contentAlignment = Alignment.Center,
) { Icon(icon, description, tint = Brand.Ink, modifier = Modifier.size(22.dp)) }

/** One-line heads-up with an action: recap ready, permission off, connect Health Connect. */
@Composable
fun Notice(icon: String, title: String, body: String, onClick: () -> Unit, tint: Color = TendIcons.accentOf(icon)) = Row(
    Modifier.fillMaxWidth().surface().tappable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    TendIcons.Badge(icon, accent = tint)
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Brand.Muted)
}

/** Small caps section header with an optional right-hand note. */
@Composable
fun SectionHeader(title: String, note: String? = null) = Row(
    Modifier.fillMaxWidth().padding(top = Space.s, start = 4.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
    if (note != null) Text(note, style = MaterialTheme.typography.labelSmall, color = Brand.Moss)
}

/** Every "tend" in a line of copy, in the brand green: the app's name hiding in plain sight. */
fun brandTend(text: String): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    var at = 0
    Regex("""\btend\b""", RegexOption.IGNORE_CASE).findAll(text).forEach { m ->
        append(text.substring(at, m.range.first))
        pushStyle(androidx.compose.ui.text.SpanStyle(color = Brand.Moss, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)); append(m.value); pop()
        at = m.range.last + 1
    }
    append(text.substring(at))
}
