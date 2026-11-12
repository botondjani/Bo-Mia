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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Avatar
import app.tend.core.BudgetConfig
import app.tend.core.TaskDef
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

/** Settings after onboarding: the same pickers, on one page. */
@Composable
fun SettingsScreen(
    catalog: AppCatalog,
    avatar: Avatar,
    cfg: BudgetConfig,
    blocked: Set<String>,
    onAvatar: (Avatar) -> Unit,
    /** After signing in or out: save the profile, redraw. */
    onAccountChanged: () -> Unit = {},
    /** After a backup was restored: start the app again on the restored data. */
    onRestored: () -> Unit = {},
    onConfig: (BudgetConfig) -> Unit,
    onToggleApp: (String) -> Unit,
    tasks: List<TaskDef>,
    onToggleTask: (TaskDef) -> Unit,
    missCount: Int,
    reminderEnabled: Boolean,
    onReminder: (enabled: Boolean) -> Unit,
    eveningFrom: Int,
    onEvening: (Int) -> Unit,
    eveningPause: Boolean,
    onEveningPause: (Boolean) -> Unit,
    weeklyLetter: Boolean,
    onWeeklyLetter: (Boolean) -> Unit,
    /** Side-loaded test builds: share all logs. Null on the Play build. */
    onExport: (() -> Unit)?,
    /** Play build: rate tend on Google Play. */
    onRate: (() -> Unit)? = null,
    /** Opens the privacy page. */
    onPrivacy: () -> Unit = {},
    /** Mail to the support address; null when none is set. */
    onFeedback: (() -> Unit)? = null,
    /** Asks the launcher to place the widget in its own dialog; false when it can't. */
    onAddWidget: () -> Boolean = { false },
    /** Asks to add the "Phone down" tile to the quick settings (Android 13+); null where it can't. */
    onAddTile: (() -> Unit)? = null,
    /** Deletes everything and starts onboarding again. */
    onStartOver: () -> Unit = {},
    version: String = "",
    /** Anonymous usage counts, off until the user says yes. */
    shareStats: Boolean = false,
    onShareStats: (Boolean) -> Unit = {},
    notesMorning: Boolean,
    notesNudges: Boolean,
    onNotes: (kind: String, on: Boolean) -> Unit,
    theme: String,
    onTheme: (String) -> Unit,
    lockSchedules: List<app.tend.core.LockSchedule> = emptyList(),
    onLockSchedule: (app.tend.core.LockSchedule) -> Unit = {},
    /** A break from all limits until this moment (epoch ms); in the past = none. */
    pausedUntil: Long = 0,
    /** Starts a break of that many days; 0 ends it. */
    onPause: (Int) -> Unit = {},
    /** Each paused app's own daily limit (minutes), and its seconds today. */
    appLimits: Map<String, Int> = emptyMap(),
    appSecondsToday: Map<String, Long> = emptyMap(),
    /** Sets or (null) removes an app's own limit. */
    onAppLimit: (String, Int?) -> Unit = { _, _ -> },
    strict: Boolean = false,
    /** Strict mode was turned off and ends then (epoch ms); 0 = it isn't ending. */
    strictEndsAt: Long = 0,
    onStrict: (Boolean) -> Unit = {},
    /** Plus is on (bought or in the trial), and the trial's days left (null when over or bought). */
    plus: Boolean = true,
    trialDaysLeft: Int? = null,
    plusPaid: Boolean = false,
    onPlus: (PlusFrom) -> Unit = {},
    /** Test builds only: move the trial's start this many days back (0 restarts it). */
    onTestTrial: ((Int) -> Unit)? = null,
    /** The companion peeking in after ten minutes in a row in paused apps. */
    peekOn: Boolean = true,
    onPeek: (Boolean) -> Unit = {},
    /** Your town (for the camp's weather), age range and gender, and the town's name for the row. */
    aboutYou: @Composable () -> Unit = {},
    townName: String? = null,
    onDone: () -> Unit,
) {
    // One short list; each part opens in place, below its row (one at a time; back closes it).
    var page by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    androidx.activity.compose.BackHandler(enabled = page != null) { page = null }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scroll = rememberScrollState()

    @Composable
    fun Section(key: String) {
        when (key) {
        "companion" -> CompanionPicker(avatar, onAvatar, compact = true)
        // The apps, each with its own limit, in one list; the full picker opens below it.
        "apps" -> {
            AppsCard(catalog, blocked, onToggleApp, appLimits, appSecondsToday, strict, plus, onAppLimit) { onPlus(PlusFrom.APP_LIMITS) }
            ShortsGuardCard(plus) { onPlus(PlusFrom.SETTINGS) }
        }
        // Minutes: the day's budget, strict mode and a break, together.
        "minutes" -> {
            Card {
                BudgetSteppers(cfg, onConfig)
                Divider()
                if (plus) StrictRow(strict, strictEndsAt, onStrict)
                else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable { onPlus(PlusFrom.STRICT) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Strict mode", style = MaterialTheme.typography.bodyLarge)
                        Text("No shortcuts, for the days you need it", style = MaterialTheme.typography.bodyMedium)
                    }
                    PlusMark()
                }
                Divider()
                BreakRows(pausedUntil, strict, onPause)
            }
        }
        "activities" -> ActivitySummary(tasks, onToggleTask)
        // Everything about the evening and the notes in one place.
        "evening" -> {
            // Everything about the evening in one place: when it starts, the pause, the recap, the lock.
            val bedtime = lockSchedules.firstOrNull { it.label == app.tend.core.LockSchedule.BEDTIME.label }
            val work = lockSchedules.firstOrNull { it.label == app.tend.core.LockSchedule.WORK.label }
            // Notes and the evening in one place.
            Card {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Evening starts at", style = MaterialTheme.typography.bodyLarge)
                        Text("Until 5:00", style = MaterialTheme.typography.bodyMedium)
                    }
                    EveningStepper(eveningFrom, onEvening)
                }
                SwitchRow("Sleepy pauses", "The pause waits 15 s and ${avatar.displayName} gets sleepy", eveningPause, onEveningPause)
                SwitchRow("Evening recap", "A note when the evening starts", reminderEnabled, onReminder)
                bedtime?.let { LockRow(it, "Bedtime lock", onLockSchedule) }
                Divider()
                SwitchRow("Morning plan", "8:30: today's minutes and what's worth the most", notesMorning) { onNotes("morning", it) }
                SwitchRow("Gentle nudges", "When 10 minutes are left", notesNudges) { onNotes("nudges", it) }
                SwitchRow("${avatar.displayName} peeks in", "After 10 minutes in a row in a paused app, from the edge of the screen", peekOn, onPeek)
                SwitchRow("Sunday letter", "Your week, on Sunday evening", weeklyLetter, onWeeklyLetter)
                work?.let { LockRow(it, "Work hours lock", onLockSchedule) }
                Text("At most 3 notes a day. A lock keeps the picked apps closed; only \"Not now\" or something better.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
            }
        }
        "sounds" -> Card {
            // Each kind of sound on its own.
            Sounds.Kind.entries.forEach { kind ->
                var on by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Sounds.on(ctx, kind)) }
                SwitchRow(kind.title, kind.about, on) { on = it; Sounds.set(ctx, kind, it) }
            }
        }
        // The account, your town and you, and the rest: one part.
        else -> {
            AccountCard(onAccountChanged, onRestored)
            Text("YOUR TOWN & YOU", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            Card { aboutYou() }
            Text("MORE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            var confirmReset by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
            Card {
                LanguagePicker()
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
                                .clickable { onTheme(key) }.padding(vertical = 10.dp),
                        )
                    }
                }
                WidgetButton(avatar, onAddWidget)
        onAddTile?.let { add ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable(onClick = add).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Phone down, from the quick settings", style = MaterialTheme.typography.bodyLarge)
                    Text("Swipe down, one tap: your next activity starts", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Add", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
            }
        }
                SwitchRow("Share anonymous stats", "No names, apps or health data", shareStats, onShareStats)
                // Small things, two to a row, all the same width.
                val links = listOfNotNull(
                    onRate?.let { "Rate tend" to it },
                    "Privacy" to onPrivacy,
                    onFeedback?.let { "Feedback" to it },
                    onExport?.let { "Export data" to it },
                    "Start over" to { confirmReset = true },
                )
                links.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (label, action) ->
                            Text(
                                label, style = MaterialTheme.typography.labelLarge, color = Brand.Moss, textAlign = TextAlign.Center, maxLines = 1,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Brand.Sage).clickable(onClick = action).padding(vertical = 10.dp),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                // Testers only: the Play build doesn't show it.
                val diagnostics = if (app.tend.BuildConfig.PLAY) "" else if (missCount == 0) " · no blocking misses" else " · $missCount blocking misses"
                Text("tend $version$diagnostics", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
            }
            if (confirmReset) androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmReset = false },
                title = { Text("Start over?") },
                text = { Text("This deletes your minutes, activities, camp and settings on this phone. It can't be undone.") },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false; onStartOver() }) { Text("Delete everything", color = Brand.Coral) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false }) { Text("Keep") } },
                containerColor = Brand.Card,
            )
        }
        }
    }

    Column(
        Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(scroll).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Space.l, bottom = 2.dp)) {
            IconButtonSoft(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back", onDone)
            Spacer(Modifier.width(Space.m))
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        PlusCard(plus, trialDaysLeft, plusPaid, onTestTrial) { onPlus(PlusFrom.SETTINGS) }
        val soundsOff = Sounds.Kind.entries.filter { !Sounds.on(ctx, it) }
        val summaries = mapOf(
            "companion" to avatar.displayName,
            "apps" to if (blocked.isEmpty()) "None yet" else "${blocked.size} app${if (blocked.size == 1) "" else "s"}" + if (appLimits.isNotEmpty()) " · ${appLimits.size} with a limit" else "",
            "minutes" to "${cfg.freeMinutes} free a day" + if (strict) " · strict" else "",
            "activities" to "${tasks.size} picked",
            "evening" to "From %d:00".format(eveningFrom) + if (lockSchedules.any { it.on }) " · a lock on" else "",
            "sounds" to when (soundsOff.size) { 0 -> "All on"; Sounds.Kind.entries.size -> "Off"; else -> soundsOff.joinToString(", ") { it.title } + " off" },
            "account" to "Sign in, backup · " + (townName?.let { "weather from $it" } ?: "your town") + " · theme",
        )
        SETTINGS_PAGES.forEach { (key, title, icon) ->
            val open = page == key
            Box(Modifier.surface(radius = 20.dp).padding(horizontal = 14.dp, vertical = 2.dp)) {
                run {
                    MenuRow(icon, title, summaries[key] ?: "", open) {
                        page = if (open) null else key
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                open,
                enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 420f)) +
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, delayMillis = 60)),
                exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)),
            ) {
                Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Section(key) }
            }
        }
        // Testers only: the Play build doesn't show it.
        val diagnostics = if (app.tend.BuildConfig.PLAY) "" else if (missCount == 0) " · no blocking misses" else " · $missCount blocking misses"
        Text("tend $version$diagnostics", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.padding(bottom = 24.dp))
    }
}

/**
 * Optional (Plus): Reels and Shorts closed, the rest of those apps open. Turning it on says plainly
 * what it does and doesn't look at, then leads to Android's switch for it.
 */
@Composable
private fun ShortsGuardCard(plus: Boolean, onPlus: () -> Unit) = Card {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember { app.tend.data.TendStore(ctx) }
    var on by remember { androidx.compose.runtime.mutableStateOf(store.shortsGuardOn) }
    var asking by remember { androidx.compose.runtime.mutableStateOf(false) }
    // Back from Android's settings: whether its switch is on now.
    var running by remember { androidx.compose.runtime.mutableStateOf(app.tend.monitor.ShortsGuard.enabled(ctx)) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val o = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) running = app.tend.monitor.ShortsGuard.enabled(ctx) }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    fun openAndroid() = ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Close only Reels & Shorts", style = MaterialTheme.typography.bodyLarge)
            Text("Instagram, YouTube and Facebook stay open; the endless videos don't", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(12.dp))
        if (plus) Switch(checked = on, onCheckedChange = { want -> if (want) asking = true else { on = false; store.shortsGuardOn = false } })
        else Box(Modifier.clip(RoundedCornerShape(10.dp)).tappable(onClick = onPlus).padding(6.dp)) { PlusMark() }
    }
    if (on && !running) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable { openAndroid() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("One more step: switch on \u201ctend: Reels and Shorts\u201d in Android's accessibility settings", style = MaterialTheme.typography.bodyMedium, color = Brand.Coral, modifier = Modifier.weight(1f))
        Text("Open", style = MaterialTheme.typography.labelLarge, color = Brand.Moss, modifier = Modifier.padding(start = 8.dp))
    }
    if (on && running) Text(
        store.shortsClosedToday.let { if (it > 0) "Closed $it time${if (it == 1) "" else "s"} today" else "On. Nothing to close yet today." },
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
    )
    if (asking) androidx.compose.material3.AlertDialog(
        onDismissRequest = { asking = false },
        title = { Text("Close Reels & Shorts?") },
        text = {
            Text(
                "tend uses Android's accessibility service for this. In Instagram, YouTube and Facebook it only checks whether the screen " +
                    "showing is the short-video player; if it is, it goes back and ${store.avatar.displayName} says so. Nothing on your screen is read, " +
                    "kept or sent anywhere, and no other app is looked at. You can turn it off here or in Android's settings any time.",
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                asking = false; on = true; store.shortsGuardOn = true
                if (!app.tend.monitor.ShortsGuard.enabled(ctx)) openAndroid()
            }) { Text("Turn on") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { asking = false }) { Text("Not now") } },
        containerColor = Brand.Card,
    )
}

/** The settings menu: key, title, icon. */
private val SETTINGS_PAGES = listOf(
    Triple("companion", "Companion", androidx.compose.material.icons.Icons.Filled.Face),
    Triple("apps", "Apps to pause", androidx.compose.material.icons.Icons.Filled.Lock),
    Triple("minutes", "Minutes", androidx.compose.material.icons.Icons.Filled.DateRange),
    Triple("activities", "Instead of scrolling", androidx.compose.material.icons.Icons.Filled.Favorite),
    Triple("evening", "Evening & notes", androidx.compose.material.icons.Icons.Filled.Notifications),
    Triple("sounds", "Sounds", androidx.compose.material.icons.Icons.Filled.PlayArrow),
    Triple("account", "Account & more", androidx.compose.material.icons.Icons.Filled.AccountCircle),
)

/** One row of the settings menu: an icon, the title, how it's set now, and an arrow (down while open). */
@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, summary: String, open: Boolean, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable(onClick = onClick).padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Brand.Sage), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(icon, null, tint = Brand.Moss, modifier = Modifier.size(19.dp))
    }
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(summary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Brand.Muted, maxLines = 1)
    }
    // Pointing down while open.
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 90f else 0f, androidx.compose.animation.core.spring(stiffness = 500f), label = "chevron")
    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Brand.Muted, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = turn })
}

/** A lock as one switch; its hours show while it's on. */
@Composable
private fun LockRow(lock: app.tend.core.LockSchedule, title: String, onLockSchedule: (app.tend.core.LockSchedule) -> Unit) {
    SwitchRow(title, "%d:00 – %d:00 · %s".format(lock.from, lock.to, if (lock.weekdaysOnly) "weekdays" else "every day"), lock.on) { onLockSchedule(lock.copy(on = it)) }
    if (lock.on) Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        HourStepper("From", lock.from, Modifier.weight(1f)) { onLockSchedule(lock.copy(from = it)) }
        HourStepper("To", lock.to, Modifier.weight(1f)) { onLockSchedule(lock.copy(to = it)) }
    }
}

/** Any hour from 16:00 round to 3:00, one step at a time. */
@Composable
private fun EveningStepper(hour: Int, onChange: (Int) -> Unit) {
    val hours = app.tend.core.Budget.EVENING_HOURS
    val i = hours.indexOf(hour).let { if (it < 0) hours.indexOf(22) else it }
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Brand.Sage).padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("−", style = MaterialTheme.typography.titleLarge, color = if (i > 0) Brand.Moss else Brand.Muted.copy(alpha = 0.4f),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = i > 0) { onChange(hours[i - 1]) }.padding(horizontal = 10.dp, vertical = 2.dp))
        Text("%d:00".format(hours[i]), style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
        Text("+", style = MaterialTheme.typography.titleLarge, color = if (i < hours.lastIndex) Brand.Moss else Brand.Muted.copy(alpha = 0.4f),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = i < hours.lastIndex) { onChange(hours[i + 1]) }.padding(horizontal = 10.dp, vertical = 2.dp))
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, modifier: Modifier, onChange: (Int) -> Unit) =
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Brand.Sage).padding(horizontal = 6.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), maxLines = 1)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("−", style = MaterialTheme.typography.titleLarge, color = Brand.Moss,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onChange((hour + 23) % 24) }.padding(horizontal = 10.dp, vertical = 2.dp))
        Text("%d:00".format(hour), style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
        Text("+", style = MaterialTheme.typography.titleLarge, color = Brand.Moss,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onChange((hour + 1) % 24) }.padding(horizontal = 10.dp, vertical = 2.dp))
      }
    }

@Composable
private fun SwitchRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) =
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
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

/** A holiday, a sick day: every limit off for a while, and those days don't count against you. */
@Composable
private fun BreakRows(pausedUntil: Long, strict: Boolean, onPause: (Int) -> Unit) {
    if (pausedUntil > System.currentTimeMillis()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("On a break until ${breakEnd(pausedUntil)}", style = MaterialTheme.typography.bodyLarge)
                Text("No pauses, locks or counting", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Come back", style = MaterialTheme.typography.labelLarge, color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss).tappable { onPause(0) }.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    } else {
        Text("Take a break", style = MaterialTheme.typography.bodyLarge)
        Text(
            if (strict) "Not while strict mode is on." else "Holiday or a sick day: every limit off, and those days don't count.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!strict) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            listOf(1 to "Today", 3 to "3 days", 7 to "A week").forEach { (days, label) ->
                Text(
                    label, style = MaterialTheme.typography.labelLarge, color = Brand.Moss, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Brand.Sage).tappable { onPause(days) }.padding(vertical = 10.dp),
                )
            }
        }
    }
}

/** "tomorrow morning" or "Friday": the midnight a break ends at, as people say it. */
fun breakEnd(until: Long): String {
    val end = java.time.Instant.ofEpochMilli(until).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val last = end.minusDays(1)
    return if (last == java.time.LocalDate.now()) "tonight" else "the end of " + last.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
}

/** Picked apps, each with its own daily limit (or none): tap one to set it. */
@Composable
private fun AppsCard(
    catalog: AppCatalog, blocked: Set<String>, onToggleApp: (String) -> Unit, limits: Map<String, Int>, secondsToday: Map<String, Long>, strict: Boolean,
    plus: Boolean, onAppLimit: (String, Int?) -> Unit, onPlus: () -> Unit,
) = Card {
    var editing by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var picking by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(blocked.isEmpty()) }
    if (blocked.isNotEmpty()) Text("Tap one to give it its own minutes a day.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    blocked.sortedBy { catalog.label(it).lowercase() }.forEach { pkg ->
        val limit = limits[pkg]
        val used = ((secondsToday[pkg] ?: 0L) / 60).toInt()
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable { if (plus) editing = pkg else onPlus() }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = remember(pkg) { catalog.icon(pkg) }
            if (icon != null) androidx.compose.foundation.Image(icon, null, Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)))
            else Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Brand.Mint))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(catalog.label(pkg), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                if (limit != null) Text(
                    if (used >= limit) "Used up today" else "${limit - used} of $limit min left today",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = if (used >= limit) Brand.Coral else Brand.Muted,
                )
            }
            if (!plus) PlusMark()
            else Text(
                limit?.let { "$it min" } ?: "No limit", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = if (limit != null) Color.White else Brand.Moss,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (limit != null) Brand.Moss else Brand.Sage).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
    // Adding or taking off apps: the full picker, folded away until wanted.
    Text(
        if (picking) "Done" else "Add or remove apps", style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { picking = !picking }.padding(vertical = 6.dp),
    )
    androidx.compose.animation.AnimatedVisibility(picking) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { AppPicker(catalog, blocked, onToggleApp) }
    }
    editing?.let { pkg ->
        val now = limits[pkg]
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(catalog.label(pkg)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Minutes a day, at most:", style = MaterialTheme.typography.bodyMedium)
                    // Strict mode: only tighter.
                    val options = listOf<Int?>(null, 10, 15, 20, 30, 45, 60, 90)
                    options.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { m ->
                                val allowed = !strict || (m != null && (now == null || m <= now))
                                val on = m == now
                                Text(
                                    m?.let { "$it" } ?: "None", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                                    color = when { on -> Color.White; allowed -> Brand.Moss; else -> Brand.Muted.copy(alpha = 0.4f) },
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Brand.Moss else Brand.Sage)
                                        .clickable(enabled = allowed) { onAppLimit(pkg, m); editing = null }.padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                    if (strict) Text("Strict mode is on: a limit can only get tighter.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { editing = null }) { Text("Done", color = Brand.Moss) } },
            containerColor = Brand.Card,
        )
    }
}

/** No shortcuts, and loosening waits until tomorrow: for the days you don't trust yourself. */
@Composable
private fun StrictRow(strict: Boolean, endsAt: Long, onStrict: (Boolean) -> Unit) {
    SwitchRow(
        "Strict mode",
        when {
            endsAt > 0 -> "Turned off: it ends at midnight."
            strict -> "On. Turning it off takes effect tomorrow."
            else -> "No 1-minute looks, no 5 minutes for acorns, no waking the apps, no breaks; limits only get tighter."
        },
        strict && endsAt == 0L, onStrict,
    )
}

/** Plus: in the trial (days left), bought, or not; a tap shows what it is. */
@Composable
private fun PlusCard(plus: Boolean, daysLeft: Int?, paid: Boolean, onTestTrial: ((Int) -> Unit)?, onOpen: () -> Unit) = Card(Modifier.tappable(onClick = onOpen)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("tend Plus", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    paid -> "Yours. Thank you!"
                    plus && daysLeft != null -> if (daysLeft <= 1) "Free trial: last day" else "Free trial: $daysLeft day${if (daysLeft == 1) "" else "s"} left"
                    plus -> "Yours, free, while tend is new"
                    else -> "Your time in full, a limit for each app, strict mode, the season's things"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        PlusMark()
    }
    if (onTestTrial != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Test:", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
        listOf(0 to "restart trial", 11 to "3 days left", 14 to "end trial").forEach { (d, label) ->
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Brand.Moss, modifier = Modifier.clickable { onTestTrial(d) })
        }
    }
}

/** A thin line between groups inside one card. */
@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Brand.Sage))
