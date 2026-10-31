package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.tend.core.TaskDef
import app.tend.core.Tasks

/** Pick up to [Tasks.MAX_SELECTED] things to do instead of scrolling: suggestions plus your own. */
@Composable
fun ActivityPicker(selected: List<TaskDef>, onToggle: (TaskDef) -> Unit) {
    val full = selected.size >= Tasks.MAX_SELECTED
    var adding by rememberSaveable { mutableStateOf(false) }
    PickCounter(selected.size)
    if (!full) SecondaryButton("＋ Add your own", { adding = true })
    selected.filter { Tasks.isCustom(it) }.forEach { task -> ActivityRow(task, true, enabled = true) { onToggle(task) } }
    Tasks.presets.forEach { task -> ActivityRow(task, task in selected, enabled = task in selected || !full) { onToggle(task) } }
    if (adding) OwnActivityDialog(onDismiss = { adding = false }) { onToggle(it); adding = false }
}

/** "3 of 5" with five dots, so the limit is obvious before anyone hits it. */
@Composable
private fun PickCounter(count: Int) = Row(
    Modifier.fillMaxWidth().surface(radius = 20.dp).padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(Modifier.weight(1f)) {
        Text("$count of ${Tasks.MAX_SELECTED} picked", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
        Text(
            if (count >= Tasks.MAX_SELECTED) "That's the max. Tap one to swap it out." else "You can pick up to ${Tasks.MAX_SELECTED}.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(Tasks.MAX_SELECTED) { i ->
            Spacer(Modifier.size(12.dp).clip(CircleShape).background(if (i < count) Brand.Moss else Brand.Mint.copy(alpha = 0.3f)))
        }
    }
}

@Composable
private fun ActivityRow(task: TaskDef, on: Boolean, enabled: Boolean, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().surface(radius = 20.dp, color = if (on) Color(0xFFE8F5EE) else Brand.Card)
        .border(1.5.dp, if (on) Brand.Moss else Color.Transparent, RoundedCornerShape(20.dp))
        .then(if (enabled) Modifier.tappable(onClick = onClick) else Modifier)
        .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(task.emoji, fontSize = 26.sp)
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
        Text(task.title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) Brand.Ink else Brand.Muted)
        Text("${task.verification.label} · +${task.rewardMinutes} min", style = MaterialTheme.typography.bodyMedium)
    }
    Text(
        if (on) "✓" else "",
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.size(26.dp).clip(CircleShape).background(if (on) Brand.Moss else Brand.Sage).padding(top = 3.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

private val EMOJIS = listOf("🎸", "🧘", "✍️", "🐶", "🌱", "🧩", "🎹", "🚲")

@Composable
private fun OwnActivityDialog(onDismiss: () -> Unit, onAdd: (TaskDef) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf(EMOJIS.first()) }
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
                EMOJIS.forEach { e ->
                    Text(
                        e, fontSize = 22.sp,
                        modifier = Modifier.clip(CircleShape).background(if (e == emoji) Brand.Mint.copy(alpha = 0.4f) else Brand.Sage)
                            .clickable { emoji = e }.padding(8.dp),
                    )
                }
            }
            Text("Check off when done · +${Tasks.CUSTOM_REWARD} min", style = MaterialTheme.typography.bodyMedium)
            PrimaryButton("Add", { onAdd(Tasks.custom(name, emoji)) }, enabled = name.isNotBlank())
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
        }
    }
}
