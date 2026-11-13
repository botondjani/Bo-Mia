package app.tend.ui

import androidx.compose.foundation.layout.size
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

/** Good days in a row, and how a missed one is forgiven. */
@Composable
fun StreakDialog(name: String, streak: app.tend.core.Streak.State, onDismiss: () -> Unit) = Sheet(tr("${streak.days} days in a row", "${streak.days} nap egymás után"), onDismiss) {
    Para(tr("A day counts when you did at least one of your activities.", "Egy nap akkor számít, ha legalább egy tevékenységed megvolt."))
    Para(tr(
        "Miss a day and $name keeps the fire going for you: once a week the run carries on. Two missed days in a row start a new one, and that's fine too.",
        "Ha kimarad egy nap, $name őrzi helyetted a tüzet: hetente egyszer a sorozat megy tovább. Két kihagyott nap után új kezdődik, és az is rendben van.",
    ))
    Para(
        if (streak.kept) tr("Yesterday was that day: the fire's still burning.", "A tegnap volt ez a nap: a tűz még ég.")
        else if (streak.spare) tr("This week's spare day is still there.", "Az e heti tartaléknapod még megvan.")
        else tr("This week's spare day is used; a new one comes in a few days.", "Az e heti tartaléknapot már felhasználtad; pár nap múlva jön az új."),
    )
}

/** A small flame, for the streak. */
@Composable
fun FlameIcon(size: androidx.compose.ui.unit.Dp) = androidx.compose.foundation.Canvas(androidx.compose.ui.Modifier.size(size)) {
    val w = this.size.width; val h = this.size.height
    fun flame(scale: Float, color: androidx.compose.ui.graphics.Color) = drawPath(androidx.compose.ui.graphics.Path().apply {
        val cx = w / 2; val b = h * 0.98f; val top = b - h * 0.95f * scale; val r = w * 0.42f * scale
        moveTo(cx, top)
        cubicTo(cx + r * 0.2f, top + (b - top) * 0.35f, cx + r * 1.15f, top + (b - top) * 0.45f, cx + r, b - r * 0.75f)
        cubicTo(cx + r, b + r * 0.1f, cx - r, b + r * 0.1f, cx - r, b - r * 0.75f)
        cubicTo(cx - r * 1.1f, top + (b - top) * 0.55f, cx - r * 0.35f, top + (b - top) * 0.5f, cx, top)
        close()
    }, color)
    flame(1f, androidx.compose.ui.graphics.Color(0xFFE8963F))
    flame(0.55f, androidx.compose.ui.graphics.Color(0xFFF6D57A))
}
