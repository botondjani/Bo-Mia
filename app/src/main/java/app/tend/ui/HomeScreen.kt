package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    tasks: List<TaskDef>,
    plantCount: Int,
    isEvening: Boolean,
    missCount: Int,
    permissionsMissing: Boolean,
    health: HealthConnect.Status?,
    focusSecondsLeft: Long?,
    onFixPermissions: () -> Unit,
    onConnectHealth: () -> Unit,
    onStartFocus: (TaskDef) -> Unit,
    onCheckOff: (TaskDef) -> Unit,
    onUndo: (TaskDef) -> Unit,
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
                Text(Moods.line(cfg, day, tasks), style = MaterialTheme.typography.titleMedium)
            }
        }

        Card {
            Text("SCREEN TIME TODAY", style = MaterialTheme.typography.labelSmall)
            Text(
                if (remaining > 0) "$remaining min left" else "Budget's empty",
                style = MaterialTheme.typography.headlineLarge,
                color = if (remaining > 0) Brand.Moss else Brand.Coral,
            )
            Text("Up to ${Budget.earnableFrom(cfg, day, tasks)} more to earn", style = MaterialTheme.typography.bodyLarge)
            Text("What you don't use tonight grows the garden.", style = MaterialTheme.typography.bodyMedium)
        }

        Text("TODAY", style = MaterialTheme.typography.labelSmall)
        val needsHealth = tasks.any { it.verification == Verification.AUTOMATIC }
        if (needsHealth && (health == HealthConnect.Status.NEEDS_PERMISSION || health == HealthConnect.Status.NEEDS_INSTALL)) Card {
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
        if (tasks.isEmpty()) Card {
            Text("No activities picked yet", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onOpenSettings) { Text("Pick what you'd rather do →") }
        }
        tasks.forEach { task -> TaskRow(task, day, health, focusSecondsLeft, onStartFocus, onCheckOff, onUndo) }

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
    onCheckOff: (TaskDef) -> Unit,
    onUndo: (TaskDef) -> Unit,
) = Card {
    val done = task.id in day.completedTasks
    val progress = Tasks.progress(task, day)
    val measured = task.verification == Verification.AUTOMATIC && health == HealthConnect.Status.READY
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${task.emoji}  ${task.title}", style = MaterialTheme.typography.bodyLarge)
            Text(
                when {
                    task.verification != Verification.AUTOMATIC -> task.subtitle
                    measured && progress != null -> progressLabel(task, progress)
                    health == HealthConnect.Status.UNSUPPORTED -> "Needs Health Connect, not available on this phone"
                    else -> "${task.subtitle} · connect Health Connect"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        when {
            done && task.manual -> Column(horizontalAlignment = Alignment.End) {
                Pill(day.grants[task.id]?.takeIf { it > 0 }?.let { "✓ +$it min" } ?: "✓")
                TextButton(onClick = { onUndo(task) }) { Text("Undo") }
            }
            done -> Pill("✓ +${day.grants[task.id] ?: task.rewardMinutes} min")
            task.verification == Verification.FOCUS_TIMER -> Button(onClick = { onStartFocus(task) }) {
                Text(focusSecondsLeft?.let { "Continue · ${it / 60}:%02d".format(it % 60) } ?: "Start", color = Color.White)
            }
            task.manual -> Button(onClick = { onCheckOff(task) }) { Text("Done", color = Color.White) }
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
fun Celebration(avatar: Avatar, tasks: List<TaskDef>, minutes: Int, onUndo: (() -> Unit)?, onDismiss: () -> Unit) = Dialog(onDismissRequest = onDismiss) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brand.Card).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BobbingCharacter(avatar, app.tend.core.Mood.OUTSIDE, 140.dp)
        // The manual cap is an internal rule: past it, no minutes, but the moment still counts.
        if (minutes > 0) Text("+$minutes min", style = MaterialTheme.typography.headlineLarge, color = Brand.Moss)
        Text(tasks.joinToString(" · ") { "${it.emoji} ${it.title}" }, style = MaterialTheme.typography.titleMedium)
        Text(
            if (minutes > 0) "${avatar.displayName} loved that. Minutes are yours for today." else "${avatar.displayName} loved that.",
            style = MaterialTheme.typography.bodyMedium,
        )
        PrimaryButton("Nice", onDismiss, modifier = Modifier.padding(top = 8.dp))
        if (onUndo != null) TextButton(onClick = onUndo) { Text("Oops, undo") }
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
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
) {
    Text("READING TOGETHER", style = MaterialTheme.typography.labelSmall)
    Box(contentAlignment = Alignment.Center) {
        val total = task.target * 60f
        androidx.compose.foundation.Canvas(Modifier.size(280.dp)) {
            val w = 14.dp.toPx()
            val inset = w / 2
            val arc = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            val style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawArc(Brand.Mint.copy(alpha = 0.3f), 0f, 360f, false, topLeft, arc, style = style)
            drawArc(Brand.Moss, -90f, 360f * (1f - secondsLeft / total), false, topLeft, arc, style = style)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CharacterView(avatar, app.tend.core.Mood.HAPPY, 150.dp, reading = true)
            Text("%d:%02d".format(secondsLeft / 60, secondsLeft % 60), style = MaterialTheme.typography.headlineLarge, color = Brand.Moss)
        }
    }
    Text(task.title, style = MaterialTheme.typography.titleLarge)
    Text(
        "${avatar.displayName} is reading with you. +${task.rewardMinutes} min when the timer ends. " +
            "You can leave tend; opening a paused app ends the session.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    androidx.compose.material3.OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(20.dp)) { Text("Stop for now") }
}

