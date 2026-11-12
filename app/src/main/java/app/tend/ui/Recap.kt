package app.tend.ui

import app.tend.core.tr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Avatar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.asImageBitmap
import app.tend.core.Accessory
import androidx.compose.material.icons.filled.Share
import app.tend.core.Mood
import app.tend.core.Recap

/**
 * What the companion wears, earned by doing. The page shows what's
 * there now; "See all" opens the whole list with how to get each.
 */
@Composable
internal fun Things(avatar: Avatar, worn: Set<Accessory>, onShelf: Set<Accessory>, earned: Set<Accessory> = worn, onToggle: ((Accessory) -> Unit)? = null) {
    var all by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("${avatar.displayName.uppercase()}'S THINGS", "${avatar.displayName.uppercase()} HOLMIJA"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(
                tr("See all ${Accessory.habits.size}", "Mind a ${Accessory.habits.size}"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { all = true }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        if (earned.isEmpty()) Text(
            tr("Do any activity and ${avatar.displayName} gets a lucky leaf. More things come with a few good days.", "Csinálj meg bármilyen tevékenységet, és ${avatar.displayName} kap egy szerencselevelet. Pár jó nap után jön a többi."),
            style = MaterialTheme.typography.bodyMedium,
        ) else androidx.compose.foundation.layout.FlowRow(
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            val mine = Accessory.habits.filter { it in earned }
            mine.forEach { ThingTile(it, worn, onShelf, Modifier.weight(1f), earned, onToggle) }
            repeat((3 - mine.size % 3) % 3) { Spacer(Modifier.weight(1f)) }
        }
    }
    if (all) androidx.compose.ui.window.Dialog(onDismissRequest = { all = false }) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brand.Card).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(tr("${avatar.displayName}'s things", "${avatar.displayName} holmija"), style = MaterialTheme.typography.titleLarge)
            Text(tr("Earned by doing, kept while the habit holds. Tap one to put it on or take it off. Faded ones wait on the shelf.", "Tettekkel szerzi, és megmarad, amíg a szokás tart. Koppintással adhatod rá vagy veheted le. Ami kikopott, a polcon vár."), style = MaterialTheme.typography.bodyMedium)
            androidx.compose.foundation.layout.FlowRow(
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Accessory.habits.forEach { ThingTile(it, worn, onShelf, Modifier.weight(1f), earned, onToggle) }
                repeat((3 - Accessory.habits.size % 3) % 3) { Spacer(Modifier.weight(1f)) }
            }
            PrimaryButton(tr("Close", "Bezárás"), { all = false })
        }
    }
}

@Composable
private fun ThingTile(a: Accessory, worn: Set<Accessory>, onShelf: Set<Accessory>, modifier: Modifier, earned: Set<Accessory> = worn, onToggle: ((Accessory) -> Unit)? = null) =
    Column(
        // Earned ones go on and off with a tap.
        if (onToggle != null && a in earned) modifier.clip(RoundedCornerShape(14.dp)).tappable { onToggle(a) }.padding(vertical = 4.dp) else modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                // Light chips in both themes, so the ink-drawn glasses stay visible.
                .background(if (a in worn) Brand.Mint.copy(alpha = 0.35f) else if (Brand.dark) Color(0xFFDCE2DD) else Brand.Sage),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                CharacterPainter.accessoryIcon(a, 96).asImageBitmap(), a.title,
                Modifier.size(40.dp).graphicsLayer { alpha = if (a in worn) 1f else if (a in earned || a in onShelf) 0.6f else 0.3f },
            )
        }
        Text(
            a.title.removePrefix("Reading ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            maxLines = 1, textAlign = TextAlign.Center, color = if (a in worn) Brand.Ink else Brand.Muted, modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            when (a) { in worn -> tr("Wearing it", "Rajta van"); in earned -> tr("Tap to wear", "Koppints: felveszi"); in onShelf -> tr("On the shelf", "A polcon"); else -> a.short },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 15.sp), textAlign = TextAlign.Center,
        )
    }

@Composable
fun RecapScreen(
    avatar: Avatar,
    recap: Recap,
    placed: Map<Int, String>,
    worn: Set<Accessory>,
    reminderHour: Int?,
    onReminder: (Boolean) -> Unit,
    onGoodNight: () -> Unit,
    onShare: () -> Unit = {},
) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Text(tr("TONIGHT'S RECAP", "A MA ESTI ÖSSZEFOGLALÓ"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    Text(
        if (recap.gotBackMinutes > 0) tr("You got back ${duration(recap.gotBackMinutes)}", "Ennyit nyertél vissza: ${duration(recap.gotBackMinutes)}") else tr("Today used it all", "A mai nap mindet elvitte"),
        style = MaterialTheme.typography.headlineLarge,
    )
    Text(
        if (recap.tonightAcorns > 0) tr("Minutes you could have scrolled, and didn't. +${recap.tonightAcorns} acorns at midnight.", "Percek, amiket elgörgethettél volna, de nem tetted. Éjfélkor +${recap.tonightAcorns} makk.")
        else tr("That happens. Tomorrow's a fresh start.", "Előfordul. Holnap tiszta lappal indulunk."),
        style = MaterialTheme.typography.bodyLarge, color = Brand.Muted,
    )

    // The camp in the evening light, whatever it holds today.
    // A fixed shape, like everywhere else, so nothing stretches on tall phones.
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(26.dp))) {
        drawClearing(avatar, placed, mood = Mood.HAPPY, worn = worn, night = true)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat("%,d".format(recap.steps), tr("steps", "lépés"), Modifier.weight(1f))
        Stat(tr("${recap.exerciseMinutes} min", "${recap.exerciseMinutes} perc"), tr("exercise", "mozgás"), Modifier.weight(1f))
        Stat(tr("${recap.readingMinutes} min", "${recap.readingMinutes} perc"), tr("reading", "olvasás"), Modifier.weight(1f))
    }

    if (recap.worthItAnswers > 0) Card {
        Text(tr("WORTH IT? TODAY", "MEGÉRTE? MA"), style = MaterialTheme.typography.labelSmall)
        Text(
            if (recap.notWorthIt == 0) tr("Every scroll you rated felt worth it. Nice.", "Minden görgetés, amit értékeltél, megérte. Szép.")
            else tr("${recap.notWorthIt} of ${recap.worthItAnswers} scrolls didn't feel worth it.", "${recap.worthItAnswers} görgetésből ${recap.notWorthIt} nem érte meg.") +
                (recap.notWorthAfterHour?.let { tr(" All of them after ${hour(it)}.", " Mind ${hour(it)} után.") } ?: ""),
            style = MaterialTheme.typography.bodyLarge,
        )
    }

    if (reminderHour != null) Card {
        Text(tr("A nudge tomorrow evening?", "Szóljunk holnap este?"), style = MaterialTheme.typography.titleMedium)
        Text(tr("${avatar.displayName} can send one quiet note at ${hour(reminderHour)} when the recap is ready.", "${avatar.displayName} küld egy halk üzenetet ${hour(reminderHour)}-kor, amikor kész az összefoglaló."), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            ChipButton(tr("Yes, remind me", "Igen, szólj"), { onReminder(true) })
            ChipButton(tr("No thanks", "Köszi, nem"), { onReminder(false) }, filled = false)
        }
    }
    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        SecondaryButton(tr("Share", "Megosztás"), onShare, modifier = Modifier.weight(0.4f))
        PrimaryButton(tr("Good night, ${avatar.displayName}", "Jó éjt, ${avatar.displayName}"), onGoodNight, modifier = Modifier.weight(0.6f))
    }
}

/**
 * After "Good night": the camp at night, the companion asleep in the shelter (or by the fire), and
 * what it means: the paused apps rest until 7:00, and an early night's acorns. A tap ends it.
 */
@Composable
fun SleepScreen(avatar: Avatar, placed: Map<Int, String>, worn: Set<Accessory>, earned: Int, layout: Map<Int, androidx.compose.ui.geometry.Offset> = emptyMap(), onDone: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { kotlinx.coroutines.delay(7000); onDone() }
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "night")
    val clock by t.animateFloat(0f, 30f, androidx.compose.animation.core.infiniteRepeatable(tween(30_000, easing = androidx.compose.animation.core.LinearEasing)), label = "clock")
    val shelter = app.tend.core.Clearing.spots.filter { it.kind == app.tend.core.SpotKind.SHELTER }.firstOrNull { placed[it.id] != null }
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C2B47), Color(0xFF2B4A4A))))
            .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onDone)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(28.dp))) {
            // In the shelter: they've gone in, Zs drift out of the roof. No shelter: asleep by the fire.
            val use = shelter?.let { InUse(it.id, placed.getValue(it.id), 2f + clock) }
            drawClearing(avatar, placed, Mood.SLEEPING, worn, night = true, use = use, layout = layout)
        }
        Text(tr("Good night", "Jó éjt"), style = MaterialTheme.typography.headlineMedium, color = Color(0xFFF3EBC8))
        Text(
            tr("Your paused apps rest until 7:00. Only \"Not now\" until then.", "A szüneteltetett appjaid 7:00-ig pihennek. Addig csak „Most nem” van.") + if (earned > 0) tr("\n+$earned acorns for an early night.", "\n+$earned makk a korai lefekvésért.") else "",
            style = MaterialTheme.typography.bodyLarge, color = Color(0xFFF3EBC8).copy(alpha = 0.8f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
internal fun Stat(value: String, label: String, modifier: Modifier) = Column(
    modifier.clip(RoundedCornerShape(20.dp)).background(Brand.Card).padding(vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text(value, style = MaterialTheme.typography.titleLarge, color = Brand.Moss)
    Text(label, style = MaterialTheme.typography.bodyMedium)
}

private fun duration(minutes: Int) = if (minutes >= 60) tr("${minutes / 60} h ${minutes % 60} min", "${minutes / 60} ó ${minutes % 60} p") else tr("$minutes min", "$minutes perc")

private fun hour(h: Int) = if (app.tend.core.Lang.hu) "$h:00" else when {
    h == 0 -> "midnight"
    h < 12 -> "$h am"
    h == 12 -> "noon"
    else -> "${h - 12} pm"
}
