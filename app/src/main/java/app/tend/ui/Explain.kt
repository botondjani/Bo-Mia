package app.tend.ui

import app.tend.core.tr

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
fun MinutesDialog(cfg: BudgetConfig, tasks: List<TaskDef>, canClose: Boolean = true, onDismiss: () -> Unit) = Sheet(tr("Your minutes", "A perceid"), onDismiss) {
    Para(tr("Time in the apps you picked. ${cfg.freeMinutes} min are free every day; activities win more back, up to ${cfg.totalMinutes} a day.", "Ennyi időd van a kiválasztott appokra. Naponta ${cfg.freeMinutes} perc jár alapból; a tevékenységekkel többet nyerhetsz vissza, legfeljebb napi ${cfg.totalMinutes} percet."))
    tasks.forEach { Line(it.title, tr("+${it.rewardMinutes} min", "+${it.rewardMinutes} perc")) }
    Para(
        if (canClose) tr("At 0, the picked apps stay closed for the rest of the day. Do an activity to open them again. Midnight starts a fresh day.", "Nullánál a kiválasztott appok a nap végéig zárva maradnak. Egy tevékenységgel újra kinyithatod őket. Éjfélkor új nap indul.")
        else tr("At 0 you get a notification, but the apps stay open: without \"display over other apps\" tend can't close them. Turn it on in Settings.", "Nullánál értesítést kapsz, de az appok nyitva maradnak: a „megjelenítés más appok fölött” engedély nélkül a tend nem tudja bezárni őket. A Beállításokban kapcsolhatod be."),
    )
    Para(tr("An app with its own limit (Plus) closes when its own minutes are used, even with minutes left for the rest.", "Ha egy appnak saját limitje van (Plus), akkor is bezár, amikor a saját percei elfogytak, ha a többire még maradt."))
    Para(tr("Once a day, if it really can't wait: 5 minutes for ${app.tend.core.Acorns.BORROW} acorns, from the pause screen (not in strict mode). Otherwise acorns never turn into minutes.", "Naponta egyszer, ha tényleg nem várhat: 5 perc ${app.tend.core.Acorns.BORROW} makkért, a szünet képernyőjéről (szigorú módban nem). Máskülönben a makkból sosem lesz perc."))
}

@Composable
fun AcornsDialog(onDismiss: () -> Unit) = Sheet(tr("Acorns", "Makk"), onDismiss) {
    Para(tr("For the camp: spend them in the shop. They don't buy minutes, except one 5-minute emergency a day.", "A táborra való: a boltban költheted el. Percet nem lehet venni rajta, kivéve napi egy 5 perces vészhelyzetet."))
    Line(tr("An activity", "Egy tevékenység"), tr("+${Acorns.PER_ACTIVITY} (up to ${Acorns.ACTIVITY_CAP} a day)", "+${Acorns.PER_ACTIVITY} (naponta legfeljebb ${Acorns.ACTIVITY_CAP})"))
    Line(tr("\"Not now\" on a paused app", "„Most nem” egy szüneteltetett appnál"), tr("+${Acorns.PER_NOT_NOW} (up to ${Acorns.NOT_NOW_CAP})", "+${Acorns.PER_NOT_NOW} (legfeljebb ${Acorns.NOT_NOW_CAP})"))
    Line(tr("A day within your minutes", "Egy nap a perceiden belül"), "+${Acorns.WITHIN_BUDGET}")
    Line(tr("Below your normal", "A megszokottnál kevesebb"), tr("+1 per 10 min", "10 percenként +1"))
    Line(tr("A visitor in the camp", "Látogató a táborban"), "+${Rarity.COMMON.acorns}–${Rarity.RARE.acorns}")
    Line(tr("A challenge", "Egy kihívás"), "+${app.tend.core.Step.entries.minOf { it.acorns }}–${app.tend.core.Step.entries.maxOf { it.acorns }}")
    Line(tr("The weekly goal", "A heti cél"), "+${Acorns.CHALLENGE}")
    Line(tr("A new level", "Új szint"), "+${Acorns.LEVEL_UP}")
}

@Composable
private fun Sheet(title: String, onDismiss: () -> Unit, body: @Composable () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { body() } },
    confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Got it", "Értem"), color = Brand.Moss) } },
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
