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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
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
    /** Notes haven't been offered yet: the last of the next steps. */
    notesOff: Boolean = false,
    onAllowNotes: () -> Unit = {},
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
                    Modifier.padding(bottom = 6.dp).tourTarget(tour, "garden").clip(RoundedCornerShape(50)).background(Brand.Mint.copy(alpha = 0.32f))
                        .border(1.dp, Brand.Mint, RoundedCornerShape(50))
                        .tappable(onClick = onOpenGarden).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TendIcons.Glyph("sprout", 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(if (plantCount > 0) "Garden · $plantCount" else "Garden", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
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
        val nextSteps = showNextSteps && (needsHealth || !widgetPlaced || notesOff)
        if (nextSteps) NextSteps(
            avatar,
            widget = !widgetPlaced, onWidget = { widgetGuide = true },
            health = if (needsHealth) health else null, onHealth = onConnectHealth,
            notes = notesOff, onNotes = onAllowNotes,
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
        // Three or more open: an endless loop, so the list goes round and round.
        val n = active.size
        val loop = n >= 3
        val count = if (loop) n * 1000 else n
        val start = if (loop) count / 2 - (count / 2) % n else 0
        val list = androidx.compose.runtime.remember(n) { androidx.compose.foundation.lazy.LazyListState(if (loop) start - 1 else 0) }
        val centered by androidx.compose.runtime.remember(list) {
            androidx.compose.runtime.derivedStateOf {
                val info = list.layoutInfo
                val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
                info.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - mid) }?.index ?: 0
            }
        }
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val reserve = when {
                done.isNotEmpty() -> 60.dp + 50.dp * minOf(done.size, 3)
                active.isNotEmpty() -> 116.dp
                else -> 0.dp
            }
            // The open ones fill the space down to "Done today": about three cards on a typical phone.
            val listMax = (maxHeight - reserve - if (loop) 18.dp else 0.dp).coerceAtLeast(80.dp)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                EdgeToEdge(Modifier.heightIn(max = listMax)) {
                androidx.compose.foundation.lazy.LazyColumn(
                    Modifier.fillMaxWidth(),
                    state = list,
                    flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(
                        list, androidx.compose.foundation.gestures.snapping.SnapPosition.Center,
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    items(count) { idx ->
                        val task = active[idx % n]
                        Box(
                            Modifier
                                .then(if (idx == start) Modifier.tourTarget(tour, "activity") else Modifier)
                                .graphicsLayer {
                                    if (!loop) return@graphicsLayer
                                    // Sharp in the middle; cards fade and shrink towards the top and bottom.
                                    val info = list.layoutInfo
                                    val item = info.visibleItemsInfo.firstOrNull { it.index == idx } ?: return@graphicsLayer
                                    val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                                    val half = ((info.viewportEndOffset - info.viewportStartOffset) / 2f).coerceAtLeast(1f)
                                    val d = (kotlin.math.abs(item.offset + item.size / 2f - mid) / half).coerceIn(0f, 1f)
                                    alpha = 1f - 0.75f * d * d
                                    scaleX = 1f - 0.07f * d
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
                // One dot per open activity: there are more than the ones in view.
                if (loop) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(n) { i ->
                        val on = i == centered % n
                        Box(
                            Modifier.padding(horizontal = 3.dp).size(if (on) 8.dp else 6.dp).clip(CircleShape)
                                .background(if (on) Brand.Moss else Brand.Mint.copy(alpha = 0.45f)),
                        )
                    }
                }
            // "Done today" sits at the bottom of the screen; the open ones get the space above it.
            Spacer(Modifier.weight(1f))
            if (done.isEmpty() && active.isNotEmpty()) {
                // Before the first one: an empty, dashed spot that says where finished things go.
                SectionHeader("Done today", "0 min")
                Row(
                    Modifier.fillMaxWidth().padding(bottom = Space.s).drawBehind {
                        drawRoundRect(
                            Brand.Muted.copy(alpha = 0.45f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                            ),
                        )
                    }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, Brand.Muted.copy(alpha = 0.5f), CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Nothing yet. Your first one lands here.",
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
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
    notes: Boolean, onNotes: () -> Unit,
    onDismiss: () -> Unit,
) = Column(Modifier.fillMaxWidth().surface(radius = 24.dp).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("NEXT STEP", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text(
            "Not now", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable(onClick = onDismiss).padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
    // One step at a time: Health Connect, then the widget, then notes.
    class Step(val icon: String, val title: String, val body: String, val action: () -> Unit)
    val steps = listOfNotNull(
        health?.let {
            Step(
                "heart", if (it == HealthConnect.Status.NEEDS_INSTALL) "Get Health Connect" else "Connect Health Connect",
                "Walks count by themselves. Got a smartwatch? Then sleep and workouts do too.", onHealth,
            )
        },
        if (widget) Step("sun", "Put ${avatar.displayName} on your home screen", "Your minutes at a glance, without opening tend.", onWidget) else null,
        if (notes) Step("moon", "Little notes from ${avatar.displayName}", "A morning plan, a heads-up at 10 minutes left, tonight's recap. At most 3 a day.", onNotes) else null,
    )
    steps.firstOrNull()?.let { StepRow(it.icon, it.title, it.body, it.action) }
    if (steps.size > 1) Text("1 of ${steps.size} · then: ${steps[1].title}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp))
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
    // A workout can always be done as a 30-minute phone-off session; a tracker counts it too.
    val workoutTimer = task.id == Tasks.MOVE.id
    val estimated = task.id == Tasks.SLEEP.id && sleepEstimated
    val measured = !(workoutTimer && !exerciseTracked) && task.verification == Verification.AUTOMATIC && (health == HealthConnect.Status.READY || estimated)
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
                    workoutTimer && !(measured && progress != null) -> "+${task.rewardMinutes} min · ${task.target} min, phone off"
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

/** Three copies of an activity's icon drifting up around the character, e.g. notes for music. */
@Composable
private fun FloatingIcons(icon: String) {
    val painter = TendIcons.painterFor(icon) ?: return
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "float")
    val phase by t.animateFloat(
        0f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(3000, easing = androidx.compose.animation.core.LinearEasing)), label = "phase",
    )
    val tint = androidx.compose.ui.graphics.ColorFilter.tint(Brand.Moss)
    androidx.compose.foundation.Canvas(Modifier.size(230.dp)) {
        val icon = 26.dp.toPx()
        for ((x, offset, scale) in listOf(Triple(-0.36f, 0f, 1f), Triple(0.36f, 0.33f, 0.8f), Triple(-0.12f, 0.66f, 0.9f))) {
            val p = (phase + offset) % 1f
            val cx = size.width / 2 + x * size.width + kotlin.math.sin(p * 6.28f) * 6.dp.toPx()
            val cy = size.height / 2 + (0.3f - p) * size.height * 0.85f
            val s = icon * scale
            translate(cx - s / 2, cy - s / 2) {
                rotate((p - 0.5f) * 24f, pivot = androidx.compose.ui.geometry.Offset(s / 2, s / 2)) {
                    with(painter) { draw(androidx.compose.ui.geometry.Size(s, s), alpha = kotlin.math.sin(p * 3.14f), colorFilter = tint) }
                }
            }
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
fun FocusTimerScreen(avatar: Avatar, task: TaskDef, secondsLeft: Long, screenOnSeconds: Long, onCancel: () -> Unit) {
    // A short start: the screen settles in, the ring draws itself, Bo pops in and says "Here we go!".
    val appear = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    var hello by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        appear.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 220f))
        kotlinx.coroutines.delay(900)
        hello = false
    }
    FocusTimerContent(avatar, task, secondsLeft, screenOnSeconds, onCancel, appear.value, hello)
}

@Composable
private fun FocusTimerContent(
    avatar: Avatar, task: TaskDef, secondsLeft: Long, screenOnSeconds: Long, onCancel: () -> Unit, appear: Float, hello: Boolean,
) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).graphicsLayer {
        alpha = appear.coerceIn(0f, 1f)
        scaleX = 0.94f + 0.06f * appear
        scaleY = scaleX
    }.padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
) {
    val phoneFree = app.tend.core.PhoneFree.runsPhoneFree(task)
    Text(
        when {
            hello -> "HERE WE GO!"
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
            drawArc(Brand.Moss, -90f, 360f * (1f - secondsLeft / total) * appear.coerceIn(0f, 1f), false, topLeft, arc, style = style)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Each activity gets its own look: a dumbbell and sweatband, hearts, a palette, a broom, a chef's hat.
            val prop = when (task.id) {
                Tasks.MOVE.id -> Prop.DUMBBELL
                Tasks.FRIENDS.id -> Prop.HEARTS
                Tasks.HOBBY.id -> Prop.PALETTE
                Tasks.CHORES.id -> Prop.BROOM
                Tasks.COOK.id -> Prop.CHEF_HAT
                else -> Prop.NONE
            }
            val extra = when (task.id) {
                Tasks.MOVE.id -> setOf(app.tend.core.Accessory.HEADBAND)
                Tasks.LEARN.id -> setOf(app.tend.core.Accessory.GLASSES)
                else -> emptySet()
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer {
                val pop = 0.6f + 0.4f * appear
                scaleX = pop; scaleY = pop
            }) {
                // Activities without a prop of their own (like your own ones) get their icon floating around.
                if (prop == Prop.NONE && phoneFree) FloatingIcons(task.icon)
                androidx.compose.runtime.CompositionLocalProvider(LocalAccessories provides LocalAccessories.current + extra) {
                    CharacterView(avatar, app.tend.core.Mood.HAPPY, 150.dp, reading = !phoneFree, prop = prop)
                }
            }
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

