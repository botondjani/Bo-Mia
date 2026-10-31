package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
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
    permissions: Permissions,
    health: HealthConnect.Status?,
    focusSecondsLeft: Long?,
    focusTaskId: String,
    onShareCrash: (() -> Unit)?,
    onFixPermissions: () -> Unit,
    onConnectHealth: () -> Unit,
    onStartFocus: (TaskDef) -> Unit,
    onCheckOff: (TaskDef) -> Unit,
    onUndo: (TaskDef) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGarden: () -> Unit,
    onOpenRecap: () -> Unit,
    weekNote: String?,
    /** Show the one-time widget tip; called with true when it was handled. */
    showWidgetTip: Boolean = false,
    /** A phone-free session the phone interrupted, shown once. */
    brokenTimer: TaskDef? = null,
    onDismissBroken: () -> Unit = {},
    onWidgetTipDone: () -> Unit = {},
) {
    var widgetGuide by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (widgetGuide) WidgetGuideDialog(avatar) { widgetGuide = false; onWidgetTipDone() }
    val remaining = Budget.remainingMinutes(cfg, day)
    val mood = Moods.of(cfg, day)
    Column(
        Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = Space.xs, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        // Header: greeting + settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(day.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), style = MaterialTheme.typography.bodyMedium)
                Text(greeting(), style = MaterialTheme.typography.headlineMedium)
            }
            // The garden is one tap away, right at the top.
            Row(
                Modifier.surface(radius = 22.dp).tappable(onClick = onOpenGarden).padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TendIcons.Glyph("sprout", 18.dp)
                Spacer(Modifier.width(6.dp))
                Text("Garden", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
            }
            Spacer(Modifier.width(Space.s))
            IconButtonSoft(Icons.Filled.Settings, "Settings", onOpenSettings)
        }

        // Hero: the companion and today's minutes
        Column(Modifier.fillMaxWidth().surface(radius = 28.dp).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CharacterView(avatar, mood, 88.dp)
                Spacer(Modifier.width(Space.l))
                Column(Modifier.weight(1f)) {
                    Text(Moods.status(avatar.displayName, mood).uppercase(), style = MaterialTheme.typography.labelSmall, color = Brand.Moss)
                    Text(Moods.line(cfg, day, tasks), style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(if (remaining > 0) "$remaining" else "0", style = MaterialTheme.typography.displayLarge, color = if (remaining > 0) Brand.Moss else Brand.Coral)
                Spacer(Modifier.width(Space.s))
                Text("min left today", style = MaterialTheme.typography.titleMedium, color = Brand.Muted, modifier = Modifier.padding(bottom = 10.dp))
            }
            val available = (cfg.freeMinutes + day.earnedMinutes).coerceAtLeast(1)
            val left by animateFloatAsState((remaining.toFloat() / available).coerceIn(0f, 1f), tween(700), label = "left")
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Brand.Sage)) {
                Box(Modifier.fillMaxWidth(left).height(8.dp).clip(RoundedCornerShape(50)).background(if (remaining > 0) Brand.Mint else Brand.Coral))
            }
            Row {
                Text("+${Budget.earnableFrom(cfg, day, tasks)} min still to earn", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.tappable(onClick = onOpenGarden)) {
                    TendIcons.Glyph("sprout", 16.dp)
                    Spacer(Modifier.width(Space.xs))
                    Text("$plantCount in the garden", style = MaterialTheme.typography.bodyMedium, color = Brand.Moss)
                }
            }
        }

        if (onShareCrash != null) Notice("alert", "tend crashed last time", "Sorry! Tap to share the crash report.", onShareCrash)
        if (brokenTimer != null) Notice(
            brokenTimer.icon, "The phone came back a little early",
            "${brokenTimer.title} needs ${brokenTimer.target} phone-free minutes. Tap to try again later.", onDismissBroken,
        )
        if (showWidgetTip) Notice("sun", "${avatar.displayName} has a widget", "See ${avatar.displayName} and your minutes on your home screen.", { widgetGuide = true })
        if (isEvening) Notice("moon", "Tonight's recap is ready", "See what today gave back", onOpenRecap)
        when {
            !permissions.usageAccess -> Notice("alert", "Limited mode", "${avatar.displayName} can't see screen time. Tap to turn it on.", onFixPermissions)
            !permissions.overlay -> Notice("alert", "No pause before scrolling", "Only a notification for now. Tap to fix.", onFixPermissions)
        }
        val needsHealth = tasks.any { it.verification == Verification.AUTOMATIC }
        if (needsHealth && (health == HealthConnect.Status.NEEDS_PERMISSION || health == HealthConnect.Status.NEEDS_INSTALL)) {
            Notice(
                "heart", if (health == HealthConnect.Status.NEEDS_INSTALL) "Get Health Connect" else "Connect Health Connect",
                "Count walks and workouts automatically", onConnectHealth,
            )
        }

        SectionHeader("Instead of scrolling", "${tasks.count { it.id in day.completedTasks }} of ${tasks.size} done")
        if (tasks.isEmpty()) Notice("sparkle", "Pick what you'd rather do", "A few activities that give minutes back", onOpenSettings)
        Tasks.ordered(day, tasks).forEach { task ->
            TaskRow(task, day, health, focusSecondsLeft?.takeIf { task.id == focusTaskId }, onStartFocus, onCheckOff, onUndo)
        }

        SectionHeader("Garden & week")
        Notice(
            "sprout",
            if (plantCount == 0) "Your garden is waiting" else "$plantCount ${if (plantCount == 1) "plant" else "plants"} in the garden",
            weekNote ?: "Your home grows with you. See how the week went.",
            onOpenGarden,
        )
    }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
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
) = Column(Modifier.fillMaxWidth().surface().padding(horizontal = 16.dp, vertical = 14.dp)) {
    val done = task.id in day.completedTasks
    val progress = Tasks.progress(task, day)
    val measured = task.verification == Verification.AUTOMATIC && health == HealthConnect.Status.READY
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (done) Box(Modifier.size(44.dp).clip(CircleShape).background(Brand.Moss), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, "Done", tint = Color.White, modifier = Modifier.size(22.dp))
        } else TendIcons.Badge(task.icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge, color = if (done) Brand.Muted else Brand.Ink)
            Text(
                when {
                    done -> day.grants[task.id]?.takeIf { it > 0 }?.let { "+$it min earned" } ?: "Done for today"
                    task.verification != Verification.AUTOMATIC -> "+${task.rewardMinutes} min · ${task.subtitle.substringAfter("· ")}"
                    measured && progress != null -> progressLabel(task, progress)
                    health == HealthConnect.Status.UNSUPPORTED -> "Health Connect isn't available on this phone"
                    else -> "+${task.rewardMinutes} min · counts automatically"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        when {
            done && task.manual -> Text(
                "Undo", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
                modifier = Modifier.tappable { onUndo(task) }.padding(8.dp),
            )
            done -> Unit
            task.verification.timed ->
                ChipButton(focusSecondsLeft?.let { "%d:%02d".format(it / 60, it % 60) } ?: "Start", { onStartFocus(task) })
            task.manual -> ChipButton("Done", { onCheckOff(task) })
            else -> Pill("+${task.rewardMinutes}")
        }
    }
    if (measured && progress != null && !done) {
        val p by animateFloatAsState((progress.toFloat() / task.target).coerceIn(0f, 1f), tween(700), label = "progress")
        Box(Modifier.fillMaxWidth().padding(top = Space.m).height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Sage)) {
            Box(Modifier.fillMaxWidth(p).height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Moss))
        }
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
        Modifier.fillMaxWidth().surface(radius = 28.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BobbingCharacter(avatar, app.tend.core.Mood.OUTSIDE, 140.dp)
        // The manual cap is an internal rule: past it, no minutes, but the moment still counts.
        if (minutes > 0) Text("+$minutes min", style = MaterialTheme.typography.headlineLarge, color = Brand.Moss)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            tasks.forEach { TendIcons.Badge(it.icon, 32.dp) }
            Text(tasks.joinToString(" · ") { it.title }, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            if (minutes > 0) "${avatar.displayName} loved that. Minutes are yours for today." else "${avatar.displayName} loved that.",
            style = MaterialTheme.typography.bodyMedium,
        )
        PrimaryButton("Nice", onDismiss, modifier = Modifier.padding(top = 8.dp))
        if (onUndo != null) Text(
            "Oops, undo", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
            modifier = Modifier.tappable(onClick = onUndo).padding(8.dp),
        )
    }
}

@Composable
fun FocusTimerScreen(avatar: Avatar, task: TaskDef, secondsLeft: Long, screenOnSeconds: Long, onCancel: () -> Unit) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
) {
    val phoneFree = task.verification == Verification.PHONE_FREE
    Text(
        when {
            phoneFree -> "PHONE-FREE TIME"
            task.id == Tasks.LEARN.id -> "LEARNING TOGETHER"
            else -> "READING TOGETHER"
        },
        style = MaterialTheme.typography.labelSmall,
    )
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
            CharacterView(avatar, app.tend.core.Mood.HAPPY, 150.dp, reading = !phoneFree)
            Text("%d:%02d".format(secondsLeft / 60, secondsLeft % 60), style = MaterialTheme.typography.headlineLarge, color = Brand.Moss)
        }
    }
    Text(task.title, style = MaterialTheme.typography.titleLarge)
    if (phoneFree) {
        Text(
            "Lock your phone and put it away. ${avatar.displayName} keeps time and lets you know when it's done: +${task.rewardMinutes} min.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        val left = (app.tend.core.PhoneFree.GRACE_SECONDS - screenOnSeconds).coerceAtLeast(0)
        Pill("Screen time allowed: %d:%02d left".format(left / 60, left % 60), if (left < 60) Brand.Coral.copy(alpha = 0.2f) else Brand.Sage)
    } else Text(
        "${avatar.displayName} is ${if (task.id == Tasks.LEARN.id) "learning" else "reading"} with you. +${task.rewardMinutes} min when the timer ends. " +
            "You can leave tend; opening a paused app ends the session.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    SecondaryButton("Stop for now", onCancel, modifier = Modifier.padding(horizontal = Space.xxl))
}

