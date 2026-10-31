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
    reminderHour: Int,
    onReminder: (enabled: Boolean, hour: Int) -> Unit,
    eveningFrom: Int,
    onEvening: (Int) -> Unit,
    weeklyLetter: Boolean,
    onWeeklyLetter: (Boolean) -> Unit,
    onExport: () -> Unit,
    onReplayTour: () -> Unit,
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
    ActivityPicker(tasks, onToggleTask)
    Text("DAILY BUDGET", style = MaterialTheme.typography.labelSmall)
    BudgetPicker(cfg, onConfig)
    Text("EVENING RECAP", style = MaterialTheme.typography.labelSmall)
    Card {
        SwitchRow("Evening reminder", "One quiet note when tonight's recap is ready", reminderEnabled) { onReminder(it, reminderHour) }
        if (reminderEnabled) HourChips(listOf(20, 21, 22), reminderHour) { onReminder(true, it) }
        SwitchRow("Sunday letter", "A short note on Sunday evening about your week", weeklyLetter, onWeeklyLetter)
    }
    Text("EVENING MODE", style = MaterialTheme.typography.labelSmall)
    Card {
        SwitchRow("Slower evenings", "Late at night the pause waits 15 s and ${avatar.displayName} gets sleepy", eveningFrom >= 0) {
            onEvening(if (it) 22 else -1)
        }
        if (eveningFrom >= 0) HourChips(listOf(21, 22, 23), eveningFrom, onEvening)
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
