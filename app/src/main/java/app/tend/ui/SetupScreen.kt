package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.tend.core.Avatar
import app.tend.core.BudgetConfig
import app.tend.core.Mood

data class InstalledApp(val pkg: String, val label: String)

data class Permissions(val usageAccess: Boolean, val overlay: Boolean, val notifications: Boolean) {
    val allGranted get() = usageAccess && overlay && notifications
}

@Composable
fun SetupScreen(
    avatar: Avatar,
    cfg: BudgetConfig,
    apps: List<InstalledApp>,
    blocked: Set<String>,
    permissions: Permissions,
    onAvatar: (Avatar) -> Unit,
    onConfig: (BudgetConfig) -> Unit,
    onToggleApp: (String) -> Unit,
    onRequestUsage: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onDone: () -> Unit,
) = LazyColumn(
    Modifier.fillMaxSize().background(Brand.Sage).padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    item { Text("Pick your companion", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 20.dp)) }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar.entries.forEach { a ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(Brand.Card)
                        .border(3.dp, if (a == avatar) Brand.Moss else Color.Transparent, RoundedCornerShape(22.dp))
                        .clickable { onAvatar(a) }.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CharacterView(a, Mood.HAPPY, 96.dp)
                    Text(a.displayName, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
    item {
        Card {
            Text("DAILY BUDGET", style = MaterialTheme.typography.labelSmall)
            Stepper("Free every day", cfg.freeMinutes) { onConfig(cfg.copy(freeMinutes = it.coerceIn(0, cfg.totalMinutes))) }
            Stepper("Daily total (with earned)", cfg.totalMinutes) { onConfig(cfg.copy(totalMinutes = it.coerceAtLeast(cfg.freeMinutes))) }
        }
    }
    item {
        Card {
            Text("PERMISSIONS", style = MaterialTheme.typography.labelSmall)
            PermissionRow("Usage access", "To measure screen time and notice opens", permissions.usageAccess, onRequestUsage)
            PermissionRow("Display over other apps", "To show the pause screen when you open an app", permissions.overlay, onRequestOverlay)
            PermissionRow("Notifications", "Keeps the companion running in the background", permissions.notifications, onRequestNotifications)
        }
    }
    item { Text("APPS TO PAUSE", style = MaterialTheme.typography.labelSmall) }
    items(apps, key = { it.pkg }) { app ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card).clickable { onToggleApp(app.pkg) }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = app.pkg in blocked, onCheckedChange = { onToggleApp(app.pkg) })
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
        }
    }
    item {
        Button(
            onClick = onDone,
            enabled = blocked.isNotEmpty() && permissions.allGranted,
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        ) { Text(if (permissions.allGranted) "Done" else "Grant the permissions above first") }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) =
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { onChange(value - 15) }) { Text("−") }
        Text("$value min", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
        OutlinedButton(onClick = { onChange(value + 15) }) { Text("+") }
    }

@Composable
private fun PermissionRow(title: String, why: String, granted: Boolean, onRequest: () -> Unit) =
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(why, style = MaterialTheme.typography.bodyMedium)
        }
        if (granted) Pill("✓") else Button(onClick = onRequest) { Text("Allow") }
    }
