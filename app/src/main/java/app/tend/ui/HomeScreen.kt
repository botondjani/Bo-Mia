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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.window.Dialog
import app.tend.health.HealthConnect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    /** Acorns to spend in the clearing. */
    acorns: Int,
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
    /** The companion's level and the three skills; null hides the strip. */
    level: app.tend.core.Level? = null,
    skills: Map<app.tend.core.Skill, app.tend.core.Level> = emptyMap(),
    challenge: app.tend.core.Challenge? = null,
    /** Logged days, for the week's chart (today is drawn live from [day]). */
    history: List<app.tend.core.DaySummary> = emptyList(),
    /** On a break until then (epoch ms); in the past = none. */
    pausedUntil: Long = 0,
    onResume: () -> Unit = {},
    /** A new level to celebrate, once. */
    levelUp: Int? = null,
    onLevelUpSeen: () -> Unit = {},
    onOpenClearing: () -> Unit,
    /** The camp scene for the top of the page (drawn by the caller, which knows what's in it). */
    camp: @Composable () -> Unit = {},
    steps: List<app.tend.core.StepState> = emptyList(),
    onStep: (app.tend.core.Step) -> Unit = {},
    /** Bumped when 'Do one activity' is tapped: the list comes into view with a glow. */
    focusActivities: Int = 0,
    /** "Good night" was pressed and it's not 7:00 yet. */
    sleeping: Boolean = false,
    onWakeUp: () -> Unit = {},
    onOpenRecap: () -> Unit,
    /** Show the one-time widget tip; called with true when it was handled. */
    showNextSteps: Boolean = false,
    widgetPlaced: Boolean = false,
    /** A phone-free session the phone interrupted, shown once. */
    brokenTimer: TaskDef? = null,
    onDismissBroken: () -> Unit = {},
    onNextStepsDone: () -> Unit = {},
    /** The week-later return of the next steps: only battery and Health Connect. */
    nextStepsImportantOnly: Boolean = false,
    /** Back after a few days away: Bo says welcome back. */
    welcomeBack: Boolean = false,
    /** The phone may stop tend in the background (battery optimisation is on for it). */
    batteryRisk: Boolean = false,
    /** It did stop it for a while: say so on home, with the fix. */
    monitorSlept: Boolean = false,
    onKeepAwake: () -> Unit = {},
    /** Notes haven't been offered yet: the last of the next steps. */
    notesOff: Boolean = false,
    onAllowNotes: () -> Unit = {},
    /** Not asked yet about anonymous usage stats: the last, optional step. */
    /** Asks the launcher to place the widget in its own dialog; false when it can't. */
    onAddWidget: () -> Boolean = { false },
    /** Something writes workouts to Health Connect; otherwise the workout is a phone-off timer. */
    exerciseTracked: Boolean = true,
    /** Last night's sleep came from the screen, not a tracker. */
    sleepEstimated: Boolean = false,
) {
    // The first days: one clear thing to do (the first steps), and nothing that's still empty.
    val firstDays = steps.isNotEmpty() && !steps.all { it.claimed }
    var widgetGuide by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (widgetGuide) WidgetGuideDialog(avatar) { widgetGuide = false }
    var explain by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    if (explain == "minutes") MinutesDialog(cfg, tasks, canClose = permissions.overlay) { explain = "" }
    if (explain == "acorns") AcornsDialog { explain = "" }
    val remaining = Budget.remainingMinutes(cfg, day)
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(Modifier.fillMaxSize()) {
    // One page that scrolls: the camp, the activities, what's done.
    Column(
        Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        // Header: greeting + settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(day.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)), style = MaterialTheme.typography.bodyMedium)
                Text(greeting(), style = MaterialTheme.typography.headlineMedium)
            }
            // Today's minutes, next to the greeting: what's left, a small bar, what's still to earn.
            Column(Modifier.padding(end = 12.dp).width(96.dp).tappable { explain = "minutes" }, horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (remaining > 0) "$remaining" else "0", style = MaterialTheme.typography.headlineMedium, color = if (remaining > 0) Brand.Moss else Brand.Coral)
                    Spacer(Modifier.width(4.dp))
                    Text("min left", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Ink, modifier = Modifier.padding(bottom = 4.dp))
                }
                val available = (cfg.freeMinutes + day.earnedMinutes).coerceAtLeast(1)
                val left by animateFloatAsState((remaining.toFloat() / available).coerceIn(0f, 1f), tween(700), label = "left")
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Brand.Card)) {
                    Box(Modifier.fillMaxWidth(left).height(5.dp).clip(RoundedCornerShape(50)).background(if (remaining > 0) Brand.Mint else Brand.Coral))
                }
                Text("+${Budget.earnableFrom(cfg, day, tasks)} to earn", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted)
            }
            IconButtonSoft(Icons.Filled.Settings, "Settings", onOpenSettings)
        }

        // Hero: the camp, with the companion living in it; what they say, today's minutes, and the
        // acorns to spend. Tapping it opens the camp.
        Box(
            // Edge to edge: the camp as wide as the screen.
            Modifier.fillMaxWidth().layout { m, c ->
                val extra = 20.dp.roundToPx()
                val p = m.measure(c.copy(minWidth = c.maxWidth + 2 * extra, maxWidth = c.maxWidth + 2 * extra))
                layout(c.maxWidth, p.height) { p.place(-extra, 0) }
            }.aspectRatio(1.6f).campEntrance().tappable(onClick = onOpenClearing),
        ) {
            camp()
            // What the companion says: one small bubble, so the camp shows.
            Text(
                Moods.line(cfg, day, tasks, welcomeBack, canClose = permissions.overlay),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp), color = Brand.Ink, maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp).fillMaxWidth(0.58f)
                    .clip(RoundedCornerShape(14.dp)).background(Color(0xE6FFFFFF)).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Row(
                Modifier.align(Alignment.TopEnd).padding(16.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xF0FFFFFF)).tappable { explain = "acorns" }.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AcornIcon(18.dp)
                Spacer(Modifier.width(5.dp))
                Text("$acorns", style = MaterialTheme.typography.titleMedium, color = Color(0xFF3D2A1C))
            }
            // The way in, plainly: a button to the camp, where it's bigger and everything happens.
            Row(
                Modifier.align(Alignment.BottomEnd).padding(14.dp).clip(RoundedCornerShape(50)).background(Brand.Moss)
                    .tappable(onClick = onOpenClearing).padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${avatar.displayName}'s camp", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        FirstStepsStrip(steps, onOpenClearing, onStep)
        if (level != null && !(firstDays && level.level == 1 && level.into == 0)) ProgressStrip(level, skills, challenge)
        if (levelUp != null) LevelUpDialog(avatar, levelUp, onLevelUpSeen)

        if (onShareCrash != null) Notice("alert", "tend crashed last time", "Sorry! Tap to share the crash report.", onShareCrash)
        if (brokenTimer != null) Notice(
            brokenTimer.icon, "The phone came back a little early",
            "${brokenTimer.title} needs ${brokenTimer.target} phone-free minutes. Tap to try again later.", onDismissBroken,
        )
        if (pausedUntil > System.currentTimeMillis()) Notice("sun", "On a break until ${breakEnd(pausedUntil)}", "No pauses, locks or counting. Tap to come back.", onResume)
        // After "Good night": the paused apps rest until morning, with a way back.
        else if (sleeping) Notice("moon", "Good night: your apps rest until 7:00", "Up again? Tap to wake them", onWakeUp)
        else if (isEvening) {
            val seen = app.tend.data.TendStore(context).recapSeenOn == day.date.toString()
            Notice("moon", if (seen) "Tonight's recap" else "Tonight's recap is ready", if (seen) "See it again" else "See what today gave back", onOpenRecap)
        }
        when {
            !permissions.usageAccess -> Notice("alert", "Limited mode", "${avatar.displayName} can't see screen time. Tap to turn it on.", onFixPermissions)
            !permissions.overlay -> Notice(
                "alert", if (remaining <= 0) "Out of minutes, but your apps are open" else "No pause before scrolling",
                if (remaining <= 0) "tend can only notify you, not close them. Tap to fix." else "Only a notification for now. Tap to fix.", onFixPermissions,
            )
        }
        val needsHealth = tasks.any { it.verification == Verification.AUTOMATIC } &&
            (health == HealthConnect.Status.NEEDS_PERMISSION || health == HealthConnect.Status.NEEDS_INSTALL)
        if (monitorSlept && batteryRisk) Notice("alert", "${avatar.displayName} fell asleep in the background", "Your phone paused tend for a while. Tap to keep ${avatar.displayName} awake.", onKeepAwake)
        // In the first days the first steps ask for the widget; here only what can't wait.
        val importantOnly = nextStepsImportantOnly || firstDays
        val nextSteps = showNextSteps && (needsHealth || batteryRisk || (!importantOnly && (!widgetPlaced || notesOff)))
        if (nextSteps) NextSteps(
            avatar,
            widget = !widgetPlaced && !importantOnly, onWidget = { if (!onAddWidget()) widgetGuide = true },
            health = if (needsHealth) health else null, onHealth = onConnectHealth,
            notes = notesOff && !importantOnly, onNotes = onAllowNotes,
            battery = batteryRisk && !monitorSlept, onBattery = onKeepAwake,
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
        if (active.isNotEmpty()) {
            // One at a time, swiped left and right; the next one peeks in from the side.
            val pager = androidx.compose.foundation.pager.rememberPagerState { active.size }
            // "Do one activity" tapped: back to the first one, and a short glow around it.
            val glow = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
            androidx.compose.runtime.LaunchedEffect(focusActivities) {
                if (focusActivities == 0) return@LaunchedEffect
                pager.animateScrollToPage(0)
                repeat(2) { glow.animateTo(1f, tween(300)); glow.animateTo(0f, tween(400)) }
            }
            androidx.compose.foundation.pager.HorizontalPager(
                pager, Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(end = if (active.size > 1) 36.dp else 0.dp),
                pageSpacing = 12.dp, verticalAlignment = Alignment.Top,
            ) { i ->
                val task = active[i]
                Box(Modifier.padding(vertical = 4.dp).border(2.dp, Brand.Moss.copy(alpha = if (i == 0) glow.value else 0f), RoundedCornerShape(22.dp))) {
                    TaskRow(task, day, health, focusSecondsLeft?.takeIf { task.id == focusTaskId }, onStartFocus, onCheckOff, onUndo, exerciseTracked, sleepEstimated)
                }
            }
            if (active.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(active.size) { i ->
                    val on = i == pager.currentPage
                    Box(Modifier.padding(horizontal = 3.dp).size(if (on) 8.dp else 6.dp).clip(CircleShape).background(if (on) Brand.Moss else Brand.Mint.copy(alpha = 0.45f)))
                }
            }
        } else if (done.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().surface(radius = 20.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                TendIcons.Badge("sparkle", 40.dp)
                Spacer(Modifier.width(12.dp))
                Text("Everything done today. ${avatar.displayName} is proud of you.", style = MaterialTheme.typography.titleMedium)
            }
        }
        // "Done today" right under the activities.
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
            Column(
                Modifier.padding(bottom = Space.xl),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) { done.forEach { DoneRow(it, day, onUndo) } }
        }
        val today = app.tend.core.DaySummary(day.date, Budget.usedMinutes(day), remaining.coerceAtLeast(0), day.completedTasks.size)
        val week = app.tend.core.WeekOverview.of(history.filter { it.date != day.date } + today, day.date)
        if (week.days.count { it != null } >= 2) Box(Modifier.padding(bottom = Space.xl)) { WeekCard(week) }
    }
    }
}

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

/**
 * A little after onboarding: what makes tend better from here, one card per step, swiped through.
 * Each card does its thing in one tap; the widget, Health Connect and the rest stay in settings.
 */
@Composable
private fun NextSteps(
    avatar: Avatar,
    widget: Boolean, onWidget: () -> Unit,
    health: HealthConnect.Status?, onHealth: () -> Unit,
    notes: Boolean, onNotes: () -> Unit,
    battery: Boolean, onBattery: () -> Unit,
    onDismiss: () -> Unit,
) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    class Step(val icon: String, val title: String, val body: String, val cta: String, val action: () -> Unit)
    val name = avatar.displayName
    val steps = listOfNotNull(
        // First, so the pause keeps working: one "Allow" in a system dialog.
        if (battery) Step("sparkle", "Keep $name awake", "Keeps the pause working in the background.", "Allow", onBattery) else null,
        health?.let {
            Step(
                "heart", "Health Connect",
                "Walks, workouts and sleep count by themselves.", if (it == HealthConnect.Status.NEEDS_INSTALL) "Install" else "Connect", onHealth,
            )
        },
        if (widget) Step("sun", "Home screen widget", "$name at a glance, next to your apps.", "Add", onWidget) else null,
        if (notes) Step("moon", "Calm evenings", "An evening recap, sleepy pauses at night and a Sunday letter.", "Turn on", onNotes) else null,
    )
    val pager = androidx.compose.foundation.pager.rememberPagerState { steps.size }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (steps.size > 1) "NEXT STEPS · ${pager.currentPage + 1} OF ${steps.size}" else "NEXT STEP",
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),
        )
        Text(
            "Not now", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable(onClick = onDismiss).padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
    // The next card peeks in from the right, so it's clear there's more to swipe.
    androidx.compose.foundation.pager.HorizontalPager(
        pager, Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(end = if (steps.size > 1) 28.dp else 0.dp),
        pageSpacing = 10.dp, verticalAlignment = Alignment.Top,
    ) { i ->
        val step = steps[i]
        // One slim row: icon, two lines of text, and a small pill that does it. A soft mint wash
        // and a hairline edge set it apart from the activity cards below.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Brand.Card, Brand.Mint.copy(alpha = 0.22f))))
                .border(1.dp, Brand.Mint.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                .tappable(onClick = step.action)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TendIcons.Badge(step.icon, 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(step.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(
                    step.body, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Brand.Muted,
                    minLines = 2, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                step.cta, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = Color.White, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss).padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
    if (steps.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(steps.size) { i ->
            val on = i == pager.currentPage
            Box(Modifier.padding(horizontal = 3.dp).size(if (on) 8.dp else 6.dp).clip(CircleShape).background(if (on) Brand.Moss else Brand.Mint.copy(alpha = 0.45f)))
        }
    }
}

/**
 * Level and skills in one slim card, with this week's goal under them. Every activity is XP
 * for Body, Mind or Rest; a reached weekly goal adds a bonus.
 */
@Composable
private fun ProgressStrip(level: app.tend.core.Level, skills: Map<app.tend.core.Skill, app.tend.core.Level>, challenge: app.tend.core.Challenge?) = Column(
    Modifier.fillMaxWidth().surface(radius = 22.dp).padding(horizontal = 14.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Level badge: a ring that fills toward the next level.
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                drawArc(Brand.Mint.copy(alpha = 0.35f), 0f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawArc(Brand.Moss, -90f, 360f * level.fraction, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LV", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Brand.Muted)
                Text("${level.level}", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
            }
        }
        Spacer(Modifier.width(12.dp))
        app.tend.core.Skill.entries.forEach { skill ->
            val l = skills[skill] ?: app.tend.core.Level(1, 0, 40)
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text("${skill.label} ${l.level}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), maxLines = 1)
                Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Sage)) {
                    Box(Modifier.fillMaxWidth(l.fraction.coerceIn(0.04f, 1f)).height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Moss))
                }
            }
        }
    }
    if (challenge != null) Row(verticalAlignment = Alignment.CenterVertically) {
        TendIcons.Badge(if (challenge.done) "sparkle" else "sun", 32.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("WEEKLY GOAL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.Muted)
            Text(challenge.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Text(
            if (challenge.done) "Done · +${app.tend.core.Progress.XP_PER_CHALLENGE} XP" else "${challenge.progress.coerceAtMost(challenge.target)} / ${challenge.target}",
            style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
        )
    }
}

@Composable
private fun LevelUpDialog(avatar: Avatar, level: Int, onDone: () -> Unit) = androidx.compose.ui.window.Dialog(onDismissRequest = onDone) {
    Column(
        Modifier.fillMaxWidth().surface(radius = 28.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BobbingCharacter(avatar, app.tend.core.Mood.OUTSIDE, 120.dp)
        Text("Level $level!", style = MaterialTheme.typography.headlineMedium, color = Brand.Moss)
        Text(
            "${avatar.displayName} levelled up. Every activity is XP for Body, Mind or Rest, and the weekly goal gives a bonus.",
            style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        PrimaryButton("Nice", onDone)
    }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
internal fun TaskRow(
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
                    else -> "+${task.rewardMinutes} min"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            // How it counts, said once and plainly: a tracker, a timer, or a tick.
            val auto = task.verification == Verification.AUTOMATIC
            val needsHealth = auto && !measured && task.id != Tasks.SLEEP.id && !workoutTimer
            if (!done && !(task.id == Tasks.SLEEP.id && !measured)) Text(
                when {
                    workoutTimer && exerciseTracked && health == HealthConnect.Status.READY -> "Counted by Health Connect, or use the timer"
                    workoutTimer || task.verification.timed -> "A timer: keep the phone down"
                    task.manual -> "You tick it off"
                    estimated -> "Estimated from your screen at night"
                    needsHealth -> "Needs Health Connect to count"
                    else -> "Counted by Health Connect, nothing to do"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
                color = if (needsHealth) Brand.Coral else Brand.Muted, modifier = Modifier.padding(top = 2.dp),
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

