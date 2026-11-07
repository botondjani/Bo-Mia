package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Box
import app.tend.core.TaskDef
import app.tend.core.Tasks

/**
 * Pick up to [Tasks.MAX_SELECTED] things to do instead of scrolling: suggestions as quick tiles,
 * one-tap ideas for your own, or write one. Minutes are shared out from the daily budget.
 */
@Composable
fun ActivityPicker(selected: List<TaskDef>, onToggle: (TaskDef) -> Unit) {
    val full = selected.size >= Tasks.MAX_SELECTED
    var writing by rememberSaveable { mutableStateOf(false) }
    PickCounter(selected.size)

    Text("SUGGESTIONS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    // Two equal columns; tiles in a row share one height, whatever wraps.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Tasks.presets.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { preset ->
                    val picked = selected.firstOrNull { it == preset }
                    PresetTile(preset, picked, enabled = picked != null || !full, modifier = Modifier.weight(1f).fillMaxHeight()) { onToggle(preset) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    Text("YOUR OWN", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    selected.filter { Tasks.isCustom(it) }.forEach { task -> OwnRow(task) { onToggle(task) } }
    val ideas = Tasks.ownIdeas.filter { (name) -> selected.none { it.title.equals(name, ignoreCase = true) } }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ideas.forEach { (name, icon, minutes) ->
            IdeaChip(name, icon, enabled = !full) { onToggle(Tasks.custom(name, icon, minutes)) }
        }
        IdeaChip("Write your own", "pen", enabled = !full, filled = true) { writing = true }
    }
    if (writing) OwnActivityDialog(onDismiss = { writing = false }) { onToggle(it); writing = false }
}

@Composable
private fun PresetTile(task: TaskDef, picked: TaskDef?, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) = Row(
    modifier.surface(radius = 18.dp, color = if (picked != null) Brand.Picked else Brand.Card)
        .border(1.5.dp, if (picked != null) Brand.Moss else Color.Transparent, RoundedCornerShape(18.dp))
        .then(if (enabled) Modifier.tappable(onClick = onClick) else Modifier.graphicsLayer { alpha = 0.5f })
        .padding(horizontal = 10.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    TendIcons.Badge(task.icon, 34.dp)
    Spacer(Modifier.width(8.dp))
    Column(Modifier.weight(1f)) {
        Text(task.title, style = MaterialTheme.typography.labelLarge, maxLines = 3)
        Text(
            picked?.let { "+${it.rewardMinutes} min" } ?: task.verification.label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = if (picked != null) Brand.Moss else Brand.Muted,
        )
    }
}

@Composable
private fun OwnRow(task: TaskDef, onRemove: () -> Unit) = Row(
    Modifier.fillMaxWidth().surface(radius = 18.dp, color = Brand.Picked)
        .border(1.5.dp, Brand.Moss, RoundedCornerShape(18.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    TendIcons.Badge(task.icon, 34.dp)
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
        Text(task.title, style = MaterialTheme.typography.labelLarge)
        Text(
            (if (task.manual) "Tick off" else "${task.target} min, phone away") + " · +${task.rewardMinutes} min",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Moss,
        )
    }
    Text(
        "Remove", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable(onClick = onRemove).padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun IdeaChip(label: String, icon: String, enabled: Boolean, filled: Boolean = false, onClick: () -> Unit) = Row(
    Modifier.clip(RoundedCornerShape(50))
        .background(if (filled) Brand.Moss else Brand.Card)
        .border(1.dp, if (filled) Color.Transparent else Brand.Mint.copy(alpha = 0.6f), RoundedCornerShape(50))
        .then(if (enabled) Modifier.tappable(onClick = onClick) else Modifier.graphicsLayer { alpha = 0.5f })
        .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    TendIcons.Glyph(icon, 18.dp, tint = if (filled) Color.White else Brand.Moss)
    Spacer(Modifier.width(6.dp))
    Text(if (filled) label else "+ $label", style = MaterialTheme.typography.labelLarge, color = if (filled) Color.White else Brand.Moss)
}

/**
 * Settings view: the picked activities as one compact card; "Change" opens the full picker in place.
 */
@Composable
fun ActivitySummary(selected: List<TaskDef>, onToggle: (TaskDef) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    if (editing) {
        ActivityPicker(selected, onToggle)
        PrimaryButton("Done", { editing = false }, enabled = selected.isNotEmpty())
        return
    }
    Card {
        if (selected.isEmpty()) Text("Nothing picked yet.", style = MaterialTheme.typography.bodyLarge)
        selected.forEach { task ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                TendIcons.Badge(task.icon, 32.dp)
                Spacer(Modifier.width(10.dp))
                Text(task.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text("+${task.rewardMinutes}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text("${selected.size} of ${Tasks.MAX_SELECTED} picked", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            ChipButton("Change", { editing = true })
        }
    }
}

/** "3 of 10 picked" with a thin bar, and the reassurance that nothing here is final. */
@Composable
private fun PickCounter(count: Int) = Column(
    Modifier.fillMaxWidth().surface(radius = 20.dp).padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
) {
    Text("$count of ${Tasks.MAX_SELECTED} picked", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Brand.Mint.copy(alpha = 0.25f))) {
        Box(Modifier.fillMaxWidth(count / Tasks.MAX_SELECTED.toFloat()).height(6.dp).clip(CircleShape).background(Brand.Moss))
    }
    Text(
        if (count >= Tasks.MAX_SELECTED) "That's the max. Tap one to swap it out."
        else "Your daily extra minutes are shared between them: bigger efforts earn more. Change any time in settings.",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun OwnActivityDialog(onDismiss: () -> Unit, onAdd: (TaskDef) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(TendIcons.ownChoices.first()) }
    var minutes by rememberSaveable { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brand.Card).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Your own activity", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(40) },
                placeholder = { Text("e.g. Play guitar") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TendIcons.ownChoices.forEach { key ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(if (key == icon) Brand.Mint.copy(alpha = 0.45f) else Brand.Sage)
                            .border(1.5.dp, if (key == icon) Brand.Moss else Color.Transparent, CircleShape)
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) { TendIcons.Glyph(key, 22.dp) }
                }
            }
            Text("HOW IT COUNTS", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "Tick off", 10 to "10 min", 20 to "20 min", 30 to "30 min").forEach { (m, label) ->
                    val on = m == minutes
                    Text(
                        label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1,
                        color = if (on) Color.White else Brand.Moss,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Brand.Moss else Brand.Sage)
                            .clickable { minutes = m }.padding(vertical = 10.dp),
                    )
                }
            }
            Text(
                if (minutes == 0) "Tick it off when it's done." else "A $minutes-minute timer with the phone put away.",
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryButton("Add", { onAdd(Tasks.custom(name, icon, minutes)) }, enabled = name.isNotBlank())
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
        }
    }
}
