package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
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
    /** Show the one-time widget tip; called with true when it was handled. */
    showNextSteps: Boolean = false,
    widgetPlaced: Boolean = false,
    /** A phone-free session the phone interrupted, shown once. */
    brokenTimer: TaskDef? = null,
    onDismissBroken: () -> Unit = {},
    onNextStepsDone: () -> Unit = {},
    /** First run: Bo walks through the screen once. */
    showTour: Boolean = false,
    onTourDone: () -> Unit = {},
    /** Something writes workouts to Health Connect; otherwise the workout is a phone-off timer. */
    exerciseTracked: Boolean = true,
    /** Last night's sleep came from the screen, not a tracker. */
    sleepEstimated: Boolean = false,
) {
    val tour = androidx.compose.runtime.remember { TourTargets() }
    var widgetGuide by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (widgetGuide) WidgetGuideDialog(avatar) { widgetGuide = false }
    val remaining = Budget.remainingMinutes(cfg, day)
    val mood = Moods.of(cfg, day)
    Box(Modifier.fillMaxSize()) {
    // The page itself stays put; only the activity list scrolls, in the space that's left.
    Column(
        Modifier.fillMaxSize().background(Brand.Sage)
            .padding(horizontal = 20.dp).padding(top = Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        // Header: greeting + settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(day.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), style = MaterialTheme.typography.bodyMedium)
                Text(greeting(), style = MaterialTheme.typography.headlineMedium)
            }
            Box(Modifier.tourTarget(tour, "settings")) { IconButtonSoft(Icons.Filled.Settings, "Settings", onOpenSettings) }
        }

        // Hero: the companion and today's minutes
        Column(Modifier.fillMaxWidth().tourTarget(tour, "minutes").surface(radius = 28.dp).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(Space.s)) {
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
                Text("min left today", style = MaterialTheme.typography.titleMedium, color = Brand.Muted, modifier = Modifier.weight(1f).padding(bottom = 10.dp))
                // The garden is one tap away, next to today's minutes.
                Row(
                    Modifier.padding(bottom = 6.dp).tourTarget(tour, "garden").clip(RoundedCornerShape(50)).background(Brand.Sage)
                        .tappable(onClick = onOpenGarden).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TendIcons.Glyph("sprout", 18.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(if (plantCount > 0) "Garden · $plantCount" else "Garden", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
                }
            }
            val available = (cfg.freeMinutes + day.earnedMinutes).coerceAtLeast(1)
            val left by animateFloatAsState((remaining.toFloat() / available).coerceIn(0f, 1f), tween(700), label = "left")
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Brand.Sage)) {
                Box(Modifier.fillMaxWidth(left).height(8.dp).clip(RoundedCornerShape(50)).background(if (remaining > 0) Brand.Mint else Brand.Coral))
            }
            Text("+${Budget.earnableFrom(cfg, day, tasks)} min still to earn", style = MaterialTheme.typography.bodyMedium)
        }

        if (onShareCrash != null) Notice("alert", "tend crashed last time", "Sorry! Tap to share the crash report.", onShareCrash)
        if (brokenTimer != null) Notice(
            brokenTimer.icon, "The phone came back a little early",
            "${brokenTimer.title} needs ${brokenTimer.target} phone-free minutes. Tap to try again later.", onDismissBroken,
        )
        if (isEvening) Notice("moon", "Tonight's recap is ready", "See what today gave back", onOpenRecap)
        when {
            !permissions.usageAccess -> Notice("alert", "Limited mode", "${avatar.displayName} can't see screen time. Tap to turn it on.", onFixPermissions)
            !permissions.overlay -> Notice("alert", "No pause before scrolling", "Only a notification for now. Tap to fix.", onFixPermissions)
        }
        val needsHealth = tasks.any { it.verification == Verification.AUTOMATIC } &&
            (health == HealthConnect.Status.NEEDS_PERMISSION || health == HealthConnect.Status.NEEDS_INSTALL)
        val nextSteps = showNextSteps && (needsHealth || !widgetPlaced)
        if (nextSteps) NextSteps(
            avatar,
            widget = !widgetPlaced, onWidget = { widgetGuide = true },
            health = if (needsHealth) health else null, onHealth = onConnectHealth,
            onDismiss = onNextStepsDone,
        ) else if (needsHealth) {
            Notice(
                "heart", if (health == HealthConnect.Status.NEEDS_INSTALL) "Get Health Connect" else "Connect Health Connect",
                "Count walks and workouts automatically", onConnectHealth,
            )
        }

        val active = tasks.filter { it.id !in day.completedTasks }
        val done = tasks.filter { it.id in day.completedTasks }
        SectionHeader("Instead of scrolling", "${done.size} of ${tasks.size} done")
        if (tasks.isEmpty()) Notice("sparkle", "Pick what you'd rather do", "A few activities that give minutes back", onOpenSettings)
        // The open activities share the space that's left and scroll on their own; the page never does.
        // Cards fade and shrink a little as they slide in or out at the edges.
        val list = androidx.compose.foundation.lazy.rememberLazyListState()
        // About three cards tall (less if the screen is short); the rest scroll in.
        EdgeToEdge(Modifier.weight(1f, fill = false).heightIn(max = 300.dp)) {
            androidx.compose.foundation.lazy.LazyColumn(
                Modifier.fillMaxWidth(),
                state = list,
                flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(list),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                itemsIndexed(active, key = { _, t -> t.id }) { i, task ->
                    Box(
                        Modifier.animateItem()
                            .then(if (i == 0) Modifier.tourTarget(tour, "activity") else Modifier)
                            .graphicsLayer {
                                val info = list.layoutInfo
                                val item = info.visibleItemsInfo.firstOrNull { it.key == task.id } ?: return@graphicsLayer
                                val size = item.size.coerceAtLeast(1).toFloat()
                                val inBottom = ((info.viewportEndOffset - item.offset) / size).coerceIn(0f, 1f)
                                val inTop = ((item.offset + item.size - info.viewportStartOffset) / size).coerceIn(0f, 1f)
                                val shown = minOf(inBottom, inTop)
                                alpha = 0.2f + 0.8f * shown * shown
                                scaleX = 0.92f + 0.08f * shown
                                scaleY = scaleX
                            },
                    ) {
                        TaskRow(task, day, health, focusSecondsLeft?.takeIf { task.id == focusTaskId }, onStartFocus, onCheckOff, onUndo, exerciseTracked, sleepEstimated)
                    }
                }
                if (active.isEmpty() && done.isNotEmpty()) item {
                    Row(Modifier.fillMaxWidth().surface(radius = 20.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        TendIcons.Badge("sparkle", 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Everything done today. ${avatar.displayName} is proud of you.", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        if (done.isNotEmpty()) {
            SectionHeader("Done today", "+${done.sumOf { day.grants[it.id] ?: 0 }} min")
            // Thin rows; more than three scroll inside their own small box.
            EdgeToEdge(Modifier.heightIn(max = 160.dp).padding(bottom = Space.s)) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) { done.forEach { DoneRow(it, day, onUndo) } }
            }
        }
    }
    if (showTour) TourOverlay(avatar, tour, homeTour(avatar.displayName, cfg.freeMinutes), onTourDone)
    }
}

/** Lets a list reach the screen edges, so the cards' shadows aren't cut off at the sides. */
@Composable
private fun EdgeToEdge(modifier: Modifier, content: @Composable () -> Unit) = Box(
    modifier.fillMaxWidth().layout { m, c ->
        val extra = 20.dp.roundToPx()
        val p = m.measure(c.copy(minWidth = c.minWidth + 2 * extra, maxWidth = c.maxWidth + 2 * extra))
        layout(c.maxWidth, p.height) { p.place(-extra, 0) }
    },
) { content() }

/** A finished activity: one thin, quiet line. */
@Composable
private fun DoneRow(task: TaskDef, day: DayState, onUndo: (TaskDef) -> Unit) = Row(
    Modifier.fillMaxWidth().graphicsLayer { alpha = 0.75f }.surface(radius = 16.dp).padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(Modifier.size(24.dp).clip(CircleShape).background(Brand.Moss), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Check, "Done", tint = Color.White, modifier = Modifier.size(14.dp))
    }
    Spacer(Modifier.width(10.dp))
    Text(
        task.title, style = MaterialTheme.typography.bodyLarge, color = Brand.Muted, maxLines = 1,
        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, modifier = Modifier.weight(1f),
    )
    day.grants[task.id]?.takeIf { it > 0 }?.let { Text("+$it", style = MaterialTheme.typography.labelLarge, color = Brand.Moss) }
    if (task.manual) Text(
        "Undo", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
        modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(8.dp)).tappable { onUndo(task) }.padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun homeTour(name: String, free: Int) = listOf(
    TourStep("minutes", "Your minutes for today", "Every app you picked draws from here. You start with $free, and I'll pause you before each open."),
    TourStep("activity", "Do one, earn minutes", "Finish one of these and I add minutes back. Walks and workouts count by themselves; the rest you start or tick off here."),
    TourStep("garden", "Our garden", "Minutes you don't use grow into a plant at midnight. Come visit, and see how the week went."),
    TourStep("settings", "Make it yours", "Apps, activities, your budget and the evening mode live here. Change them any time."),
)

/**
 * A little after onboarding: what makes tend better from here. Shown until both are done or it's
 * closed; the widget and Health Connect stay reachable from settings and the home notice.
 */
@Composable
private fun NextSteps(
    avatar: Avatar,
    widget: Boolean, onWidget: () -> Unit,
    health: HealthConnect.Status?, onHealth: () -> Unit,
    onDismiss: () -> Unit,
) = Column(Modifier.fillMaxWidth().surface(radius = 24.dp).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("NEXT STEPS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(
            "Not now", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable(onClick = onDismiss).padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
    if (health != null) StepRow(
        "heart",
        if (health == HealthConnect.Status.NEEDS_INSTALL) "Get Health Connect" else "Connect Health Connect",
        "Walks count by themselves. Got a smartwatch? Then sleep and workouts do too.",
        onHealth,
    )
    if (widget) StepRow("sun", "Put ${avatar.displayName} on your home screen", "Your minutes at a glance, without opening tend.", onWidget)
}

@Composable
private fun StepRow(icon: String, title: String, body: String, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).tappable(onClick = onClick).padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    TendIcons.Badge(icon, 40.dp)
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Brand.Muted)
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
    exerciseTracked: Boolean = true,
    sleepEstimated: Boolean = false,
) = Column(
    Modifier.fillMaxWidth().graphicsLayer { alpha = if (task.id in day.completedTasks) 0.55f else 1f }
        .surface().padding(horizontal = 16.dp, vertical = 14.dp),
) {
    val done = task.id in day.completedTasks
    val progress = Tasks.progress(task, day)
    // Without a workout tracker the workout is a 30-minute phone-off timer; without a sleep tracker sleep is estimated.
    val workoutTimer = task.id == Tasks.MOVE.id && !exerciseTracked
    val estimated = task.id == Tasks.SLEEP.id && sleepEstimated
    val measured = !workoutTimer && task.verification == Verification.AUTOMATIC && (health == HealthConnect.Status.READY || estimated)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (done) Box(Modifier.size(44.dp).clip(CircleShape).background(Brand.Moss), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, "Done", tint = Color.White, modifier = Modifier.size(22.dp))
        } else TendIcons.Badge(task.icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title, style = MaterialTheme.typography.bodyLarge, color = if (done) Brand.Muted else Brand.Ink,
                textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            )
            Text(
                when {
                    done -> day.grants[task.id]?.takeIf { it > 0 }?.let { "+$it min earned" } ?: "Done for today"
                    workoutTimer -> "+${task.rewardMinutes} min · ${task.target} min, phone off"
                    task.verification != Verification.AUTOMATIC -> "+${task.rewardMinutes} min · ${task.subtitle.substringAfter("· ")}"
                    estimated && progress != null -> "About " + progressLabel(task, progress).replace(" sleep last night", ", from your screen")
                    measured && progress != null -> progressLabel(task, progress)
                    task.id == Tasks.SLEEP.id -> "+${task.rewardMinutes} min · from your tracker or your screen at night"
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
            task.verification.timed || workoutTimer ->
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
    Tasks.SLEEP.id -> "%d h %02d of %d h sleep last night".format(Locale.ENGLISH, progress / 60, progress % 60, task.target / 60)
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
    val phoneFree = app.tend.core.PhoneFree.runsPhoneFree(task)
    Text(
        when {
            task.id == Tasks.MOVE.id -> "WORKOUT, PHONE OFF"
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
        val left = (app.tend.core.PhoneFree.graceSeconds(task) - screenOnSeconds).coerceAtLeast(0)
        Pill("Screen time allowed: %d:%02d left".format(left / 60, left % 60), if (left < 60) Brand.Coral.copy(alpha = 0.2f) else Brand.Sage)
    } else Text(
        "${avatar.displayName} is ${if (task.id == Tasks.LEARN.id) "learning" else "reading"} with you. +${task.rewardMinutes} min when the timer ends. " +
            "You can leave tend; opening a paused app ends the session.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    SecondaryButton("Stop for now", onCancel, modifier = Modifier.padding(horizontal = Space.xxl))
}

