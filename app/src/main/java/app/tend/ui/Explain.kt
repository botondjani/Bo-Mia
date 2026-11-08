package app.tend.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tend.core.Acorns
import app.tend.core.BudgetConfig
import app.tend.core.Rarity
import app.tend.core.TaskDef

/** Two currencies, kept apart: minutes are for your apps, acorns are for the camp. */

@Composable
fun MinutesDialog(cfg: BudgetConfig, tasks: List<TaskDef>, canClose: Boolean = true, onDismiss: () -> Unit) = Sheet("Your minutes", onDismiss) {
    Para("Time in the apps you picked. ${cfg.freeMinutes} min are free every day; activities win more back, up to ${cfg.totalMinutes} a day.")
    tasks.forEach { Line(it.title, "+${it.rewardMinutes} min") }
    Para(
        if (canClose) "At 0, the picked apps stay closed for the rest of the day. Do an activity to open them again. Midnight starts a fresh day."
        else "At 0 you get a notification, but the apps stay open: without \"display over other apps\" tend can't close them. Turn it on in Settings.",
    )
    Para("Acorns never turn into minutes.")
}

@Composable
fun AcornsDialog(onDismiss: () -> Unit) = Sheet("Acorns", onDismiss) {
    Para("For the camp: spend them in the shop. They never buy minutes.")
    Line("An activity", "+${Acorns.PER_ACTIVITY} (up to ${Acorns.ACTIVITY_CAP} a day)")
    Line("\"Not now\" on a paused app", "+${Acorns.PER_NOT_NOW} (up to ${Acorns.NOT_NOW_CAP})")
    Line("A day within your minutes", "+${Acorns.WITHIN_BUDGET}")
    Line("Below your normal", "+1 per 10 min")
    Line("A visitor in the camp", "+${Rarity.COMMON.acorns}–${Rarity.RARE.acorns}")
    Line("A challenge", "+${app.tend.core.Step.entries.minOf { it.acorns }}–${app.tend.core.Step.entries.maxOf { it.acorns }}")
    Line("The weekly goal", "+${Acorns.CHALLENGE}")
    Line("A new level", "+${Acorns.LEVEL_UP}")
}

@Composable
private fun Sheet(title: String, onDismiss: () -> Unit, body: @Composable () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { body() } },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Got it", color = Brand.Moss) } },
    containerColor = Brand.Card,
)

@Composable
private fun Para(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium)

@Composable
private fun Line(what: String, worth: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Text(what, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Spacer(Modifier.width(8.dp))
    Text(worth, style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
}
