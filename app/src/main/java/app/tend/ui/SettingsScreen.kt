package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Avatar
import app.tend.core.BudgetConfig
import app.tend.core.TaskDef

/** Settings after onboarding: the same pickers, on one page. */
@Composable
fun SettingsScreen(
    catalog: AppCatalog,
    avatar: Avatar,
    cfg: BudgetConfig,
    blocked: Set<String>,
    onAvatar: (Avatar) -> Unit,
    /** After signing in or out: save the profile, redraw. */
    onAccountChanged: () -> Unit = {},
    /** After a backup was restored: start the app again on the restored data. */
    onRestored: () -> Unit = {},
    onConfig: (BudgetConfig) -> Unit,
    onToggleApp: (String) -> Unit,
    tasks: List<TaskDef>,
    onToggleTask: (TaskDef) -> Unit,
    missCount: Int,
    reminderEnabled: Boolean,
    onReminder: (enabled: Boolean) -> Unit,
    eveningFrom: Int,
    onEvening: (Int) -> Unit,
    eveningPause: Boolean,
    onEveningPause: (Boolean) -> Unit,
    weeklyLetter: Boolean,
    onWeeklyLetter: (Boolean) -> Unit,
    /** Side-loaded test builds: share all logs. Null on the Play build. */
    onExport: (() -> Unit)?,
    /** Play build: rate tend on Google Play. */
    onRate: (() -> Unit)? = null,
    /** Opens the privacy page. */
    onPrivacy: () -> Unit = {},
    /** Mail to the support address; null when none is set. */
    onFeedback: (() -> Unit)? = null,
    /** Asks the launcher to place the widget in its own dialog; false when it can't. */
    onAddWidget: () -> Boolean = { false },
    /** Deletes everything and starts onboarding again. */
    onStartOver: () -> Unit = {},
    version: String = "",
    /** Anonymous usage counts, off until the user says yes. */
    shareStats: Boolean = false,
    onShareStats: (Boolean) -> Unit = {},
    onReplayTour: () -> Unit,
    notesMorning: Boolean,
    notesNudges: Boolean,
    onNotes: (kind: String, on: Boolean) -> Unit,
    theme: String,
    onTheme: (String) -> Unit,
    lockSchedules: List<app.tend.core.LockSchedule> = emptyList(),
    onLockSchedule: (app.tend.core.LockSchedule) -> Unit = {},
    onDone: () -> Unit,
) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Space.l)) {
        IconButtonSoft(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back", onDone)
        Spacer(Modifier.width(Space.m))
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
    }
    Text("COMPANION", style = MaterialTheme.typography.labelSmall)
    CompanionPicker(avatar, onAvatar, compact = true)
    Text("APPS TO PAUSE", style = MaterialTheme.typography.labelSmall)
    AppPicker(catalog, blocked, onToggleApp)
    Text("INSTEAD OF SCROLLING", style = MaterialTheme.typography.labelSmall)
    ActivitySummary(tasks, onToggleTask)
    Text("DAILY BUDGET", style = MaterialTheme.typography.labelSmall)
    BudgetPicker(cfg, onConfig)

    // Everything about the evening in one place: when it starts, the pause, the recap, the lock.
    val bedtime = lockSchedules.firstOrNull { it.label == app.tend.core.LockSchedule.BEDTIME.label }
    val work = lockSchedules.firstOrNull { it.label == app.tend.core.LockSchedule.WORK.label }
    Text("EVENING", style = MaterialTheme.typography.labelSmall)
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Evening starts at", style = MaterialTheme.typography.bodyLarge)
                Text("Until 5:00", style = MaterialTheme.typography.bodyMedium)
            }
            EveningStepper(eveningFrom, onEvening)
        }
        SwitchRow("Sleepy pauses", "The pause waits 15 s and ${avatar.displayName} gets sleepy", eveningPause, onEveningPause)
        SwitchRow("Evening recap", "A note when the evening starts", reminderEnabled, onReminder)
        bedtime?.let { LockRow(it, "Bedtime lock", onLockSchedule) }
    }
    Text("DURING THE DAY", style = MaterialTheme.typography.labelSmall)
    Card {
        SwitchRow("Morning plan", "8:30: today's minutes and what's worth the most", notesMorning) { onNotes("morning", it) }
        SwitchRow("Gentle nudges", "When 10 minutes are left", notesNudges) { onNotes("nudges", it) }
        SwitchRow("Sunday letter", "Your week, on Sunday evening", weeklyLetter, onWeeklyLetter)
        work?.let { LockRow(it, "Work hours lock", onLockSchedule) }
        Text("At most 3 notes a day. A lock keeps the picked apps closed; only \"Not now\" or something better.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }

    Text("ACCOUNT", style = MaterialTheme.typography.labelSmall)
    AccountCard(onAccountChanged, onRestored)

    Text("MORE", style = MaterialTheme.typography.labelSmall)
    var confirmReset by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("light" to "Light", "dark" to "Dark", "system" to "Auto").forEach { (key, label) ->
                val on = key == theme
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) Color.White else Brand.Moss,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (on) Brand.Moss else Brand.Sage)
                        .clickable { onTheme(key) }.padding(vertical = 10.dp),
                )
            }
        }
        WidgetButton(avatar, onAddWidget)
        val ctx = androidx.compose.ui.platform.LocalContext.current
        var sounds by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Sounds.enabled(ctx)) }
        SwitchRow("Sounds", "The guitar, the fire, the river: when ${avatar.displayName} uses things", sounds) { sounds = it; Sounds.setEnabled(ctx, it) }
        SwitchRow("Share anonymous stats", "No names, apps or health data", shareStats, onShareStats)
        // Small things, two to a row, all the same width.
        val links = listOfNotNull(
            "Tour" to onReplayTour,
            onRate?.let { "Rate tend" to it },
            "Privacy" to onPrivacy,
            onFeedback?.let { "Feedback" to it },
            onExport?.let { "Export data" to it },
            "Start over" to { confirmReset = true },
        )
        links.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (label, action) ->
                    Text(
                        label, style = MaterialTheme.typography.labelLarge, color = Brand.Moss, textAlign = TextAlign.Center, maxLines = 1,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Brand.Sage).clickable(onClick = action).padding(vertical = 10.dp),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        // Testers only: the Play build doesn't show it.
        val diagnostics = if (app.tend.BuildConfig.PLAY) "" else if (missCount == 0) " · no blocking misses" else " · $missCount blocking misses"
        Text("tend $version$diagnostics", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }
    if (confirmReset) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Start over?") },
        text = { Text("This deletes your minutes, activities, camp and settings on this phone. It can't be undone.") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false; onStartOver() }) { Text("Delete everything", color = Brand.Coral) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false }) { Text("Keep") } },
        containerColor = Brand.Card,
    )
    Spacer(Modifier.padding(bottom = 24.dp))
}

/** A lock as one switch; its hours show while it's on. */
@Composable
private fun LockRow(lock: app.tend.core.LockSchedule, title: String, onLockSchedule: (app.tend.core.LockSchedule) -> Unit) {
    SwitchRow(title, "%d:00 – %d:00 · %s".format(lock.from, lock.to, if (lock.weekdaysOnly) "weekdays" else "every day"), lock.on) { onLockSchedule(lock.copy(on = it)) }
    if (lock.on) Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        HourStepper("From", lock.from, Modifier.weight(1f)) { onLockSchedule(lock.copy(from = it)) }
        HourStepper("To", lock.to, Modifier.weight(1f)) { onLockSchedule(lock.copy(to = it)) }
    }
}

/** Any hour from 16:00 round to 3:00, one step at a time. */
@Composable
private fun EveningStepper(hour: Int, onChange: (Int) -> Unit) {
    val hours = app.tend.core.Budget.EVENING_HOURS
    val i = hours.indexOf(hour).let { if (it < 0) hours.indexOf(22) else it }
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Brand.Sage).padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("−", style = MaterialTheme.typography.titleLarge, color = if (i > 0) Brand.Moss else Brand.Muted.copy(alpha = 0.4f),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = i > 0) { onChange(hours[i - 1]) }.padding(horizontal = 10.dp, vertical = 2.dp))
        Text("%d:00".format(hours[i]), style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
        Text("+", style = MaterialTheme.typography.titleLarge, color = if (i < hours.lastIndex) Brand.Moss else Brand.Muted.copy(alpha = 0.4f),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = i < hours.lastIndex) { onChange(hours[i + 1]) }.padding(horizontal = 10.dp, vertical = 2.dp))
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, modifier: Modifier, onChange: (Int) -> Unit) =
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Brand.Sage).padding(horizontal = 6.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), maxLines = 1)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("−", style = MaterialTheme.typography.titleLarge, color = Brand.Moss,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onChange((hour + 23) % 24) }.padding(horizontal = 10.dp, vertical = 2.dp))
        Text("%d:00".format(hour), style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
        Text("+", style = MaterialTheme.typography.titleLarge, color = Brand.Moss,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onChange((hour + 1) % 24) }.padding(horizontal = 10.dp, vertical = 2.dp))
      }
    }

@Composable
private fun SwitchRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }

/** Shown when a permission was revoked after onboarding. */
@Composable
fun PermissionsScreen(avatar: Avatar, perms: PermissionActions, onDone: () -> Unit) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Text("Let ${avatar.displayName} notice again", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 20.dp))
    Text("A permission was turned off, so the pause can't appear right now.", style = MaterialTheme.typography.bodyLarge, color = Brand.Muted)
    PermissionSteps(perms)
    PrimaryButton("Done", onDone, enabled = perms.permissions.allGranted, modifier = Modifier.padding(vertical = 20.dp))
}
