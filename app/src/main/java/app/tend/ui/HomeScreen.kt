package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.window.Dialog
import app.tend.health.HealthConnect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.tend.core.Avatar
import app.tend.core.Budget
import app.tend.core.BudgetConfig
import app.tend.core.DayState
import app.tend.core.Moods
import app.tend.core.TaskDef
import app.tend.core.Tasks
import app.tend.core.Verification
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    avatar: Avatar,
    cfg: BudgetConfig,
    day: DayState,
    plantCount: Int,
    isEvening: Boolean,
    missCount: Int,
    permissionsMissing: Boolean,
    health: HealthConnect.Status?,
    focusSecondsLeft: Long?,
    onFixPermissions: () -> Unit,
    onConnectHealth: () -> Unit,
    onStartFocus: (TaskDef) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGarden: () -> Unit,
    onOpenRecap: () -> Unit,
) {
    val remaining = Budget.remainingMinutes(cfg, day)
    Column(
        Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hey there!", style = MaterialTheme.typography.headlineMedium)
                Text(day.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), style = MaterialTheme.typography.bodyMedium)
            }
            Pill("Garden · $plantCount 🌱", modifier = Modifier.clickable(onClick = onOpenGarden))
        }

        if (isEvening) Card {
            Text("TONIGHT'S RECAP", style = MaterialTheme.typography.labelSmall)
            Text("See what today gave back", style = MaterialTheme.typography.titleMedium)
            PrimaryButton("Open recap", onOpenRecap, modifier = Modifier.padding(top = 8.dp))
        }

        if (permissionsMissing) Card {
            Text("${avatar.displayName} can't see app opens right now", style = MaterialTheme.typography.titleMedium)
            Text("A permission was switched off, so there's no pause before scrolling.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onFixPermissions) { Text("Fix it →") }
        }

        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CharacterView(avatar, Moods.of(cfg, day), 96.dp)
                Spacer(Modifier.padding(6.dp))
                Text(Moods.line(cfg, day), style = MaterialTheme.typography.titleMedium)
            }
        }

        Card {
            Text("SCREEN TIME TODAY", style = MaterialTheme.typography.labelSmall)
            Text(
                if (remaining > 0) "$remaining min left" else "Budget's empty",
                style = MaterialTheme.typography.headlineLarge,
                color = if (remaining > 0) Brand.Moss else Brand.Coral,
            )
            Text("Up to ${Budget.earnableMinutes(cfg, day)} more to earn", style = MaterialTheme.typography.bodyLarge)
            Text("What you don't use tonight grows the garden.", style = MaterialTheme.typography.bodyMedium)
        }

        Text("TODAY", style = MaterialTheme.typography.labelSmall)
        if (health == HealthConnect.Status.NEEDS_PERMISSION || health == HealthConnect.Status.NEEDS_INSTALL) Card {
            Text("Count walks and workouts automatically", style = MaterialTheme.typography.titleMedium)
            Text(
                "Health Connect shares today's steps and exercise with tend. Read only, stays on your phone.",
                style = MaterialTheme.typography.bodyMedium,
            )
            PrimaryButton(
                if (health == HealthConnect.Status.NEEDS_INSTALL) "Get Health Connect" else "Connect Health Connect",
                onConnectHealth, modifier = Modifier.padding(top = 8.dp),
            )
        }
        Tasks.all.forEach { task -> TaskRow(task, day, health, focusSecondsLeft, onStartFocus) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (missCount == 0) "Blocking: no misses logged" else "Blocking misses logged: $missCount",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskDef,
    day: DayState,
    health: HealthConnect.Status?,
    focusSecondsLeft: Long?,
    onStartFocus: (TaskDef) -> Unit,
) = Card {
    val done = task.id in day.completedTasks
    val progress = Tasks.progress(task, day)
    val measured = task.verification == Verification.AUTOMATIC && health == HealthConnect.Status.READY
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                when {
                    task.verification == Verification.FOCUS_TIMER -> task.subtitle
                    measured && progress != null -> progressLabel(task, progress)
                    health == HealthConnect.Status.UNSUPPORTED -> "Needs Health Connect, not available on this phone"
                    else -> "${task.subtitle} · connect Health Connect"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        when {
            done -> Pill("✓ +${task.rewardMinutes} min")
            task.verification == Verification.FOCUS_TIMER -> Button(onClick = { onStartFocus(task) }) {
                Text(focusSecondsLeft?.let { "Continue · ${it / 60}:%02d".format(it % 60) } ?: "Start", color = Color.White)
            }
            else -> Pill("+${task.rewardMinutes} min")
        }
    }
    if (measured && progress != null && !done) {
        LinearProgressIndicator(
            progress = { (progress.toFloat() / task.target).coerceIn(0f, 1f) },
            color = Brand.Moss, trackColor = Brand.Sage,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(RoundedCornerShape(50)),
        )
    }
}

private fun progressLabel(task: TaskDef, progress: Long): String = when (task.id) {
    Tasks.OUTSIDE.id -> "%,d of %,d steps".format(Locale.ENGLISH, progress, task.target)
    else -> "$progress of ${task.target} min of exercise"
}

/** Short moment of joy when a task completes: the character goes outside, minutes are credited. */
@Composable
fun Celebration(avatar: Avatar, tasks: List<TaskDef>, onDismiss: () -> Unit) = Dialog(onDismissRequest = onDismiss) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brand.Card).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BobbingCharacter(avatar, app.tend.core.Mood.OUTSIDE, 140.dp)
        Text("+${tasks.sumOf { it.rewardMinutes }} min", style = MaterialTheme.typography.headlineLarge, color = Brand.Moss)
        Text(tasks.joinToString(" · ") { it.title }, style = MaterialTheme.typography.titleMedium)
        Text("${avatar.displayName} loved that. Minutes are yours for today.", style = MaterialTheme.typography.bodyMedium)
        PrimaryButton("Nice", onDismiss, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun Card(content: @Composable () -> Unit) = Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.Card).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
) { content() }

@Composable
fun Pill(text: String, color: Color = Brand.Sage, modifier: Modifier = Modifier) = Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = Brand.Moss,
    modifier = modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 12.dp, vertical = 6.dp),
)

@Composable
fun FocusTimerScreen(avatar: Avatar, task: TaskDef, secondsLeft: Long, onCancel: () -> Unit) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
) {
    CharacterView(avatar, app.tend.core.Mood.HAPPY, 160.dp)
    Spacer(Modifier.height(16.dp))
    Text(task.title, style = MaterialTheme.typography.titleLarge)
    Text("%d:%02d".format(secondsLeft / 60, secondsLeft % 60), style = MaterialTheme.typography.headlineLarge, color = Brand.Moss)
    Text("${avatar.displayName} is reading with you. +${task.rewardMinutes} min when the timer ends.", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(24.dp))
    TextButton(onClick = onCancel) { Text("Stop for now") }
}

