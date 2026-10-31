package app.tend.ui

import androidx.compose.animation.AnimatedVisibility
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
import app.tend.core.TaskDef
import app.tend.core.Tasks

/** Pick up to [Tasks.MAX_SELECTED] things to do instead of scrolling: suggestions plus your own. */
@Composable
fun ActivityPicker(selected: List<TaskDef>, onToggle: (TaskDef) -> Unit) {
    val full = selected.size >= Tasks.MAX_SELECTED
    Text("${selected.size} of ${Tasks.MAX_SELECTED} picked", style = MaterialTheme.typography.labelSmall)
    Tasks.presets.forEach { task -> ActivityRow(task, task in selected, enabled = task in selected || !full) { onToggle(task) } }
    selected.filter { Tasks.isCustom(it) }.forEach { task -> ActivityRow(task, true, enabled = true) { onToggle(task) } }
    if (!full) OwnActivity { onToggle(it) }
}

@Composable
private fun ActivityRow(task: TaskDef, on: Boolean, enabled: Boolean, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .background(if (on) Brand.Mint.copy(alpha = 0.3f) else Brand.Card)
        .border(2.dp, if (on) Brand.Moss else Color.Transparent, RoundedCornerShape(20.dp))
        .clickable(enabled = enabled, onClick = onClick)
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
private fun OwnActivity(onAdd: (TaskDef) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf(EMOJIS.first()) }
    TextButton(onClick = { open = !open }) { Text(if (open) "Cancel" else "＋ Add your own") }
    AnimatedVisibility(open) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brand.Card).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
            PrimaryButton("Add", {
                onAdd(Tasks.custom(name, emoji)); name = ""; open = false
            }, enabled = name.isNotBlank())
        }
    }
}
