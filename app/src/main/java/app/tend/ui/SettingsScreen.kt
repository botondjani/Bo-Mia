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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    onConfig: (BudgetConfig) -> Unit,
    onToggleApp: (String) -> Unit,
    tasks: List<TaskDef>,
    onToggleTask: (TaskDef) -> Unit,
    missCount: Int,
    onAddWidget: () -> Boolean,
    reminderEnabled: Boolean,
    onReminder: (enabled: Boolean) -> Unit,
    eveningFrom: Int,
    onEvening: (Int) -> Unit,
    eveningPause: Boolean,
    onEveningPause: (Boolean) -> Unit,
    weeklyLetter: Boolean,
    onWeeklyLetter: (Boolean) -> Unit,
    onExport: () -> Unit,
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
    CompanionPicker(avatar, onAvatar)
    Text("APPS TO PAUSE", style = MaterialTheme.typography.labelSmall)
    AppPicker(catalog, blocked, onToggleApp)
    Text("INSTEAD OF SCROLLING", style = MaterialTheme.typography.labelSmall)
    ActivitySummary(tasks, onToggleTask)
    Text("DAILY BUDGET", style = MaterialTheme.typography.labelSmall)
    BudgetPicker(cfg, onConfig)
    Text("EVENING", style = MaterialTheme.typography.labelSmall)
    Card {
        Text("Evening starts at", style = MaterialTheme.typography.bodyLarge)
        Text("Tonight's recap is ready from then.", style = MaterialTheme.typography.bodyMedium)
        HourChips(listOf(21, 22, 23), eveningFrom, onEvening)
        SwitchRow("Sleepy pauses", "From then until 5:00 the pause waits 15 s and ${avatar.displayName} gets sleepy", eveningPause, onEveningPause)
    }
    Text("SCHEDULED LOCK", style = MaterialTheme.typography.labelSmall)
    Card {
        Text("The picked apps stay closed, whatever the minutes say. Only \"Not now\" or something better.", style = MaterialTheme.typography.bodyMedium)
        lockSchedules.forEach { lock ->
            SwitchRow(
                lock.label,
                "%d:00 – %d:00 · %s".format(lock.from, lock.to, if (lock.weekdaysOnly) "weekdays" else "every day"),
                lock.on,
            ) { onLockSchedule(lock.copy(on = it)) }
            if (lock.on) Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                HourStepper("From", lock.from, Modifier.weight(1f)) { onLockSchedule(lock.copy(from = it)) }
                HourStepper("To", lock.to, Modifier.weight(1f)) { onLockSchedule(lock.copy(to = it)) }
            }
        }
    }
    Text("NOTIFICATIONS", style = MaterialTheme.typography.labelSmall)
    Card {
        Text("At most 3 a day, and none at night.", style = MaterialTheme.typography.bodyMedium)
        SwitchRow("Morning plan", "8:30: today's minutes and what's worth the most", notesMorning) { onNotes("morning", it) }
        SwitchRow("Gentle nudges", "When 10 minutes are left", notesNudges) { onNotes("nudges", it) }
        SwitchRow("Evening recap", "When the evening starts", reminderEnabled, onReminder)
        SwitchRow("Sunday letter", "Your week, on Sunday evening", weeklyLetter, onWeeklyLetter)
    }
    Text("APPEARANCE", style = MaterialTheme.typography.labelSmall)
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
                        .clickable { onTheme(key) }.padding(vertical = 12.dp),
                )
            }
        }
        Text("Auto follows your phone.", style = MaterialTheme.typography.bodyMedium)
    }
    Text("HOME SCREEN", style = MaterialTheme.typography.labelSmall)
    WidgetButton(avatar, onAddWidget)
    Text(
        if (missCount == 0) "Diagnostics · no blocking misses logged" else "Diagnostics · $missCount blocking misses logged",
        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Space.s),
    )
    SecondaryButton("Show the tour again", onReplayTour)
    SecondaryButton("Export test data", onExport)
    PrimaryButton("Done", onDone, enabled = blocked.isNotEmpty(), modifier = Modifier.padding(vertical = 20.dp))
}

@Composable
private fun HourStepper(label: String, hour: Int, modifier: Modifier, onChange: (Int) -> Unit) =
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(Brand.Sage).padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 6.dp).weight(1f))
        Text("−", style = MaterialTheme.typography.titleLarge, color = Brand.Moss,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onChange((hour + 23) % 24) }.padding(horizontal = 10.dp, vertical = 2.dp))
        Text("%d:00".format(hour), style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
        Text("+", style = MaterialTheme.typography.titleLarge, color = Brand.Moss,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onChange((hour + 1) % 24) }.padding(horizontal = 10.dp, vertical = 2.dp))
    }

@Composable
private fun SwitchRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }

@Composable
private fun HourChips(hours: List<Int>, selected: Int, onPick: (Int) -> Unit) =
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        hours.forEach { h ->
            val on = h == selected
            Text(
                "$h:00",
                style = MaterialTheme.typography.labelLarge,
                color = if (on) Color.White else Brand.Moss,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (on) Brand.Moss else Brand.Sage)
                    .clickable { onPick(h) }.padding(vertical = 12.dp),
            )
        }
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
