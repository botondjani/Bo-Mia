package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
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
    reminderEnabled: Boolean,
    reminderHour: Int,
    onReminder: (enabled: Boolean, hour: Int) -> Unit,
    onDone: () -> Unit,
) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 20.dp))
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Evening reminder", style = MaterialTheme.typography.bodyLarge)
                Text("One quiet note when tonight's recap is ready", style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = reminderEnabled, onCheckedChange = { onReminder(it, reminderHour) })
        }
        if (reminderEnabled) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf(20, 21, 22).forEach { h ->
                val on = h == reminderHour
                Text(
                    "$h:00",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) Color.White else Brand.Moss,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (on) Brand.Moss else Brand.Sage)
                        .clickable { onReminder(true, h) }.padding(vertical = 12.dp),
                )
            }
        }
    }
    PrimaryButton("Done", onDone, enabled = blocked.isNotEmpty(), modifier = Modifier.padding(vertical = 20.dp))
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
