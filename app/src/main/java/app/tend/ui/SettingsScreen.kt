package app.tend.ui

import kotlinx.coroutines.launch
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.layout.heightIn
import app.tend.core.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
    /** Open on this part (e.g. "apps"), not the list. */
    openSection: String? = null,
) {
    // One short list; each part opens in place, below its row (one at a time; back closes it).
    // "widget": the account part, scrolled down to the widget button.
    var page by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(if (openSection == "widget") "account" else openSection) }
    androidx.activity.compose.BackHandler(enabled = page != null) { page = null }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scroll = rememberScrollState()
    if (openSection == "widget") androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        scroll.animateScrollTo(scroll.maxValue)
    }

    @Composable
    fun Section(key: String) {
        when (key) {
        // The apps, each with its own limit, in one list; the full picker opens below it.
        "apps" -> {
            AppsCard(catalog, blocked, onToggleApp, appLimits, appSecondsToday, strict, plus, onAppLimit) { onPlus(PlusFrom.APP_LIMITS) }
            Card { CountedKinds(cfg, onConfig) }
        }
        // The day's budget and the weekend, then how strict (and Bo's peeks): one page.
        "minutes" -> {
            Card {
                BudgetSteppers(cfg, onConfig)
                Divider()
                WeekendRow(strict)
            }
            StrictCard(plus, strict, if (plus || strict) strictEndsAt else 0, onStrict, { onPlus(PlusFrom.STRICT) }, avatar.displayName, peekOn, onPeek)
        }
        "periods" -> PeriodsCard(catalog, blocked, plus, { onPlus(PlusFrom.SETTINGS) }, strict, eveningFrom, onEvening, lockSchedules, onLockSchedule, onAccountChanged)
        "notes" -> NotesCard(avatar.displayName, reminderEnabled, onReminder, notesMorning, notesNudges, onNotes, weeklyLetter, onWeeklyLetter, onAccountChanged)
        "activities" -> ActivitySummary(tasks, onToggleTask)
        // The account, your town and you, and the rest: one part.
        else -> {
            var confirmReset by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
            AccountCard(onAccountChanged, onRestored, onStartOver = { confirmReset = true })
            Text(tr("COMPANION", "TÁRS"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            CompanionPicker(avatar, onAvatar, compact = true)
            Text(tr("WHAT FRIENDS SEE", "MIT LÁTNAK A BARÁTAID"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            FriendsPrivacy()
            Text(tr("YOUR TOWN & YOU", "A VÁROSOD ÉS TE"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            Card { aboutYou() }
            Text(tr("SOUNDS", "HANGOK"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            Card {
                Sounds.Kind.entries.forEach { kind ->
                    var on by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Sounds.on(ctx, kind)) }
                    SwitchRow(kind.title, kind.about, on) { on = it; Sounds.set(ctx, kind, it) }
                }
            }
            Text(tr("MORE", "EGYEBEK"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            Card {
                LanguagePicker(plain = Brand.Sage)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("light" to tr("Light", "Világos"), "dark" to tr("Dark", "Sötét"), "system" to tr("Auto", "Auto")).forEach { (key, label) ->
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
                    Text(tr("Phone down, from the quick settings", "Telefon le, a gyorsbeállításokból"), style = MaterialTheme.typography.bodyLarge)
                    Text(tr("Swipe down, one tap: your next activity starts", "Lehúzod, egy koppintás: indul a következő tevékenységed"), style = MaterialTheme.typography.bodyMedium)
                }
                Text(tr("Add", "Hozzáadás"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
            }
        }
                SwitchRow(tr("Share anonymous stats", "Névtelen statisztika megosztása"), tr("No names, apps or health data", "Se név, se appok, se egészségadat"), shareStats, onShareStats)
                // Small things, two to a row, all the same width.
                val links = listOfNotNull(
                    onRate?.let { tr("Rate tend", "Értékeld a tendet") to it },
                    tr("Privacy", "Adatvédelem") to onPrivacy,
                    onFeedback?.let { tr("Feedback", "Visszajelzés") to it },
                    onExport?.let { tr("Export data", "Adatok exportálása") to it },
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
            }
            if (confirmReset) androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmReset = false },
                title = { Text(tr("Start over?", "Újrakezded?")) },
                text = { Text(tr("This deletes your minutes, activities, camp and settings on this phone, and signs you out; you start again with setup. Your account and its backup stay: log in again any time to bring this camp back.", "Ez törli a perceidet, a tevékenységeidet, a táborodat és a beállításaidat erről a telefonról, és kijelentkeztet; újra a beállítással kezdesz. A fiókod és a mentésed megmarad: ha újra belépsz, bármikor visszahozhatod ezt a tábort.")) },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false; onStartOver() }) { Text(tr("Delete everything", "Mindent törlök"), color = Brand.Coral) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmReset = false }) { Text(tr("Keep", "Mégsem")) } },
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
            IconButtonSoft(TendIcons.vector("back"), tr("Back", "Vissza"), onDone)
            Spacer(Modifier.width(Space.m))
            Text(tr("Settings", "Beállítások"), style = MaterialTheme.typography.headlineMedium)
        }
        PlusCard(plus, trialDaysLeft, plusPaid, onTestTrial) { onPlus(PlusFrom.SETTINGS) }
        // What decides the picked apps right now, in one line; and a day off, one tap away.
        var dayOff by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        NowLine(onDayOff = { dayOff = true })
        if (dayOff) androidx.compose.material3.AlertDialog(
            onDismissRequest = { dayOff = false },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { BreakRows(pausedUntil, strict) { onPause(it); dayOff = false } } },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { dayOff = false }) { Text(tr("Close", "Bezárás"), color = Brand.Moss) } },
            containerColor = Brand.Card,
        )
        val store = remember { app.tend.data.TendStore(ctx) }
        val level = when { strict -> tr("Strict", "Szigorú"); store.strictLevel == app.tend.core.StrictLevel.EASY -> tr("Easy", "Laza"); else -> tr("Balanced", "Kiegyensúlyozott") }
        val evening = when (store.eveningMode) { app.tend.core.EveningMode.OFF -> tr("evening off", "este ki"); app.tend.core.EveningMode.SLOW -> tr("slower from %d:00", "%d:00-tól lassít").format(eveningFrom); app.tend.core.EveningMode.LOCK -> tr("closed at night", "éjjel zár") }
        val summaries = mapOf(
            "apps" to if (blocked.isEmpty()) tr("None yet", "Még egy sincs") else tr("${blocked.size} app${if (blocked.size == 1) "" else "s"}", "${blocked.size} app") + if (appLimits.isNotEmpty()) tr(" · ${appLimits.size} with a limit", " · ${appLimits.size} saját limittel") else "",
            "minutes" to tr("${cfg.freeMinutes} min a day", "napi ${cfg.freeMinutes} perc") + " · " + level,
            "periods" to evening + if (lockSchedules.any { it.on && it.id != "bed" }) tr(" · a lock on", " · zár bekapcsolva") else "",
            "activities" to tr("${tasks.size} picked", "${tasks.size} kiválasztva"),
            "notes" to when (store.notesLevel) { app.tend.core.NotesLevel.QUIET -> tr("Quiet", "Csendes"); app.tend.core.NotesLevel.NORMAL -> tr("Normal", "Normál"); app.tend.core.NotesLevel.CHATTY -> tr("Chatty", "Beszédes") },
            "account" to tr("Sign in, backup, ${avatar.displayName}, theme, sounds", "Belépés, mentés, ${avatar.displayName}, téma, hangok"),
        )
        // Opening a part scrolls it to the top; the one open before closes at once (no two animations fighting).
        val rowY = remember { mutableMapOf<String, Float>() }
        val density = androidx.compose.ui.platform.LocalDensity.current
        androidx.compose.runtime.LaunchedEffect(page) {
            val key = page ?: return@LaunchedEffect
            kotlinx.coroutines.delay(60)
            rowY[key]?.let { y -> scroll.animateScrollTo((y - with(density) { 12.dp.toPx() }).toInt().coerceAtLeast(0)) }
        }
        SETTINGS_PAGES.forEach { (key, title, icon) ->
            val open = page == key
            Box(Modifier.onGloballyPositioned { rowY[key] = it.positionInParent().y }.surface(radius = 20.dp).padding(horizontal = 14.dp, vertical = 2.dp)) {
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
                exit = androidx.compose.animation.ExitTransition.None,
            ) {
                Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Section(key) }
            }
        }
        // Testers only: the Play build doesn't show it.
        val diagnostics = if (app.tend.BuildConfig.PLAY) "" else if (missCount == 0) tr(" · no blocking misses", " · nem maradt el Megállító") else tr(" · $missCount blocking misses", " · $missCount elmaradt Megállító")
        Text("tend $version$diagnostics", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.padding(bottom = 24.dp))
    }
}

private val SETTINGS_PAGES = listOf(
    Triple("apps", tr("Picked apps", "Kiválasztott appok"), "lock"),
    Triple("minutes", tr("Minutes and strictness", "Keret és szigor"), "dial"),
    Triple("periods", tr("Times of day", "Időszakok"), "clock"),
    Triple("activities", tr("Instead of scrolling", "Görgetés helyett"), "heart"),
    Triple("notes", tr("Bo's notes", "Bo üzenetei"), "bell"),
    Triple("account", tr("Account & more", "Fiók és egyebek"), "person"),
)

/** One row of the settings menu: an icon, the title, how it's set now, and an arrow (down while open). */
@Composable
private fun MenuRow(icon: String, title: String, summary: String, open: Boolean, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable(onClick = onClick).padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Brand.Sage), contentAlignment = Alignment.Center) {
        TendIcons.Glyph(icon, 19.dp)
    }
    Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(summary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Brand.Muted, maxLines = 1)
    }
    // Pointing down while open.
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 90f else 0f, androidx.compose.animation.core.spring(stiffness = 500f), label = "chevron")
    androidx.compose.material3.Icon(TendIcons.vector("chevron"), null, tint = Brand.Muted, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = turn })
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
    Text(tr("Let ${avatar.displayName} notice again", "Hadd figyeljen újra ${avatar.displayName}"), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 20.dp))
    Text(tr("A permission was turned off, so the pause can't appear right now.", "Egy engedély ki lett kapcsolva, ezért a Megállító most nem tud megjelenni."), style = MaterialTheme.typography.bodyLarge, color = Brand.Muted)
    PermissionSteps(perms)
    PrimaryButton(tr("Done", "Kész"), onDone, enabled = perms.permissions.allGranted, modifier = Modifier.padding(vertical = 20.dp))
}

/** A holiday, a sick day: every limit off for a while, and those days don't count against you. */
@Composable
internal fun BreakRows(pausedUntil: Long, strict: Boolean, onPause: (Int) -> Unit) {
    if (pausedUntil > System.currentTimeMillis()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Day off until ${breakEnd(pausedUntil)}", "Szabadnap eddig: ${breakEnd(pausedUntil)}"), style = MaterialTheme.typography.bodyLarge)
                Text(tr("No pauses, locks or counting", "Nincs Megállító, zárás, számolás"), style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                tr("Come back", "Visszajövök"), style = MaterialTheme.typography.labelLarge, color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss).tappable { onPause(0) }.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    } else {
        Text(tr("Day off", "Szabadnap"), style = MaterialTheme.typography.bodyLarge)
        Text(
            if (strict) tr("Not while strict mode is on.", "Szigorú módban nem lehet.") else tr("Holiday or a sick day: every limit off, and those days don't count.", "Nyaralás vagy betegség: minden korlát kikapcsol, és ezek a napok nem számítanak."),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!strict) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            listOf(1 to tr("Today", "Ma"), 3 to tr("3 days", "3 nap"), 7 to tr("A week", "Egy hét")).forEach { (days, label) ->
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
    val weekday = last.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, app.tend.core.Lang.locale)
    return if (last == java.time.LocalDate.now()) tr("tonight", "ma éjfél") else tr("the end of $weekday", "$weekday éjfél")
}

/** Picked apps, each with its own daily limit (or none): tap one to set it. */
@Composable
private fun AppsCard(
    catalog: AppCatalog, blocked: Set<String>, onToggleApp: (String) -> Unit, limits: Map<String, Int>, secondsToday: Map<String, Long>, strict: Boolean,
    plus: Boolean, onAppLimit: (String, Int?) -> Unit, onPlus: () -> Unit,
) = Card {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember { app.tend.data.TendStore(ctx) }
    var editing by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var picking by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(blocked.isEmpty()) }
    // What each app does when opened: changes show at once.
    var modes by remember { androidx.compose.runtime.mutableStateOf(store.appModes) }
    var askShorts by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    fun modeLabel(m: app.tend.core.AppMode?) = when (m) {
        app.tend.core.AppMode.COUNT -> tr("Only counts", "Csak számol")
        app.tend.core.AppMode.SHORTS -> tr("Only Reels and Shorts closed", "Csak a Reels és a Shorts zárva")
        else -> tr("Opens with the Megállító", "Megállítóval nyílik")
    }
    if (blocked.isNotEmpty()) Text(tr("Their time comes off your daily minutes. Tap one to choose what opening it does, or to give it its own minutes a day.", "Az idejük a napi keretedből fogy. Koppints egyre: kiválaszthatod, mi történjen megnyitáskor, vagy saját napi perceket adhatsz neki."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    blocked.sortedBy { catalog.label(it).lowercase() }.forEach { pkg ->
        val limit = limits[pkg]
        val used = ((secondsToday[pkg] ?: 0L) / 60).toInt()
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable { editing = pkg }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = remember(pkg) { catalog.icon(pkg) }
            if (icon != null) androidx.compose.foundation.Image(icon, null, Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)))
            else Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Brand.Mint))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(catalog.label(pkg), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                val mode = if (strict) null else modes[pkg]
                Text(
                    modeLabel(mode), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = if (mode == null) Brand.Moss else Brand.Muted,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (limit != null) Text(
                    if (used >= limit) tr("Used up today", "Mára elfogyott") else tr("${limit - used} of $limit min left today", "Ma még ${limit - used} perc a $limit percből"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = if (used >= limit) Brand.Coral else Brand.Muted,
                )
            }
            if (plus || limit != null) Text(
                limit?.let { tr("$it min", "$it perc") } ?: tr("No limit", "Nincs limit"), style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = if (limit != null) Color.White else Brand.Moss,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (limit != null) Brand.Moss else Brand.Sage).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
    // Adding or taking off apps: the full picker, folded away until wanted.
    Text(
        if (picking) tr("Done", "Kész") else tr("Add or remove apps", "Appok hozzáadása vagy elvétele"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { picking = !picking }.padding(vertical = 6.dp),
    )
    androidx.compose.animation.AnimatedVisibility(picking) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { AppPicker(catalog, blocked, onToggleApp) }
    }
    // Only Reels and Shorts: it needs Android's accessibility switch for tend, said plainly first.
    askShorts?.let {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { askShorts = null },
            title = { Text(tr("One more step", "Még egy lépés")) },
            text = {
                Text(tr("tend uses Android's accessibility service for this. It only checks whether the short-video player is showing; if it is, it goes back. Nothing on your screen is read, kept or sent, and no other app is looked at. It also makes the Megállító quicker, and Android's \"over other apps\" note goes away.", "A tend ehhez az Android akadálymentesítési szolgáltatását használja. Csak azt nézi, hogy a rövidvideó-lejátszó látszik-e; ha igen, visszalép. Semmit nem olvas, nem tárol és nem küld el a képernyődről, más appba nem néz bele. A Megállító is gyorsabb lesz tőle, és az Android „other apps” értesítése eltűnik."))
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = {
                askShorts = null
                ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text(tr("Turn it on", "Bekapcsolom"), color = Brand.Moss) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { askShorts = null }) { Text(tr("Later", "Később"), color = Brand.Muted) } },
            containerColor = Brand.Card,
        )
    }
    editing?.let { pkg ->
        val now = limits[pkg]
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(catalog.label(pkg)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // What opening it does: the Megállító (the default), only counting, or only Reels and Shorts closed.
                    Text(tr("When it opens", "Megnyitáskor"), style = MaterialTheme.typography.bodyMedium)
                    val modeOptions = listOfNotNull(app.tend.core.AppMode.STOP, app.tend.core.AppMode.COUNT, app.tend.core.AppMode.SHORTS.takeIf { app.tend.monitor.ShortsGuard.supports(pkg) })
                    val current = modes[pkg] ?: app.tend.core.AppMode.STOP
                    modeOptions.forEach { m ->
                        OptionCard(
                            when (m) { app.tend.core.AppMode.STOP -> tr("Megállító", "Megállító"); app.tend.core.AppMode.COUNT -> tr("Only count", "Csak számol"); app.tend.core.AppMode.SHORTS -> tr("Only Reels and Shorts", "Csak Reels és Shorts") },
                            when (m) {
                                app.tend.core.AppMode.STOP -> tr("A few seconds before it opens: often enough to change your mind.", "Pár másodperc megnyitás előtt: sokszor ennyi is elég, hogy meggondold.")
                                app.tend.core.AppMode.COUNT -> tr("Opens straight away; its time comes off your minutes.", "Azonnal nyílik; az ideje a keretedből fogy.")
                                app.tend.core.AppMode.SHORTS -> tr("The app stays open; the endless videos close.", "Az app nyitva marad; a végtelen videók bezárnak.")
                            },
                            picked = !strict && current == m || strict && m == app.tend.core.AppMode.STOP,
                        ) {
                            if (strict) return@OptionCard
                            store.setAppMode(pkg, m); modes = store.appModes
                            if (m == app.tend.core.AppMode.SHORTS && !app.tend.monitor.ShortsGuard.enabled(ctx)) askShorts = pkg
                        }
                    }
                    if (strict) Text(tr("Strict: every picked app opens with the Megállító.", "Szigorú fokozatban minden kiválasztott app Megállítóval nyílik."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
                    Text(tr("Minutes a day, at most:", "Naponta legfeljebb ennyi perc:"), style = MaterialTheme.typography.bodyMedium)
                    if (!plus) Row(Modifier.clip(RoundedCornerShape(12.dp)).tappable { editing = null; onPlus() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Own limits for each app come with Plus ", "Appok saját limitje a Plusszal "), style = MaterialTheme.typography.bodyMedium); PlusMark()
                    }
                    else {
                    // Strict mode: only tighter.
                    val options = listOf<Int?>(null, 10, 15, 20, 30, 45, 60, 90)
                    options.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { m ->
                                val allowed = !strict || (m != null && (now == null || m <= now))
                                val on = m == now
                                Text(
                                    m?.let { "$it" } ?: tr("None", "Nincs"), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                                    color = when { on -> Color.White; allowed -> Brand.Moss; else -> Brand.Muted.copy(alpha = 0.4f) },
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Brand.Moss else Brand.Sage)
                                        .clickable(enabled = allowed) { onAppLimit(pkg, m); editing = null }.padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                    if (strict) Text(tr("Strict mode is on: a limit can only get tighter.", "Szigorú mód van: a limitet csak szigorítani lehet."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { editing = null }) { Text(tr("Done", "Kész"), color = Brand.Moss) } },
            containerColor = Brand.Card,
        )
    }
}

/** No shortcuts, and loosening waits until tomorrow: for the days you don't trust yourself. */
@Composable
private fun StrictRow(strict: Boolean, endsAt: Long, onStrict: (Boolean) -> Unit) {
    SwitchRow(
        tr("Strict mode", "Szigorú mód"),
        when {
            endsAt > 0 -> tr("Turned off: it ends at midnight.", "Kikapcsoltad: éjfélkor ér véget.")
            strict -> tr("On. Turning it off takes effect tomorrow.", "Bekapcsolva. Ha kikapcsolod, holnaptól érvényes.")
            else -> tr("A pause before every picked app; no quick looks, no 5 minutes for acorns, no waking the apps, no days off; limits only get tighter.", "Megállító minden kiválasztott appnál; nincs Gyors belesés, nincs 5 perc makkért, nincs appébresztés, nincs Szabadnap; a limitek csak szigorodhatnak.")
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
                    paid -> tr("Yours. Thank you!", "A tiéd. Köszönjük!")
                    plus && daysLeft != null -> if (daysLeft <= 1) tr("Free trial: last day", "Ingyenes próba: utolsó nap") else tr("Free trial: $daysLeft day${if (daysLeft == 1) "" else "s"} left", "Ingyenes próba: még $daysLeft nap")
                    plus -> tr("Yours, free, while tend is new", "A tiéd, ingyen, amíg a tend új")
                    else -> tr("Your time in full, a limit for each app, strict mode, the season's things", "Az időd részletesen, saját limit minden appnak, szigorú mód, az évszak tárgyai")
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

/** A choice among a few: one row of equal chips, the picked one filled. */
@Composable
private fun Choice(options: List<String>, picked: Int, modifier: Modifier = Modifier, onPick: (Int) -> Unit) =
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == picked
            Text(
                label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), textAlign = TextAlign.Center, maxLines = 1,
                color = if (on) Color.White else Brand.Moss,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Brand.Moss else Brand.Sage).tappable { onPick(i) }.padding(vertical = 9.dp),
            )
        }
    }

/** One option as a card: a title, what it means, ticked when picked. */
@Composable
private fun OptionCard(title: String, body: String, picked: Boolean, plusMark: Boolean = false, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().heightIn(min = 76.dp).clip(RoundedCornerShape(16.dp)).background(if (picked) Brand.Picked else Brand.Sage)
        .border(2.dp, if (picked) Brand.Moss else Color.Transparent, RoundedCornerShape(16.dp))
        .tappable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (picked) Brand.Moss else Brand.Ink)
            if (plusMark) { Spacer(Modifier.width(6.dp)); PlusMark() }
        }
        Text(body, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp), color = Brand.Muted)
    }
    // The tick's place is kept when it isn't picked, so the text doesn't reflow on a tap.
    Text("✓", style = MaterialTheme.typography.titleMedium, color = if (picked) Brand.Moss else Color.Transparent, modifier = Modifier.padding(start = 8.dp))
}

/**
 * "Now": what decides the picked apps at this moment, in one line (a lock and until when, the
 * minutes left, how strict), so it's never a mystery why something closed.
 */
@Composable
private fun NowLine(onDayOff: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember { app.tend.data.TendStore(ctx) }
    val lock = store.activeLock()
    val left = app.tend.core.Budget.remainingMinutes(store.config, store.today())
    val level = when (store.effectiveStrict) { app.tend.core.StrictLevel.EASY -> tr("easy", "laza"); app.tend.core.StrictLevel.BALANCED -> tr("balanced", "kiegyensúlyozott"); app.tend.core.StrictLevel.STRICT -> tr("strict", "szigorú") }
    val parts = listOfNotNull(
        if (store.isPaused) tr("a day off", "szabadnap") else null,
        lock?.let { tr("${it.label} until %d:00".format(it.to), "${it.label} %d:00-ig".format(it.to)) },
        if (left <= 0) tr("today's minutes are gone", "a mai keret elfogyott") else tr("$left min left", "$left perc maradt"),
        level,
    )
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand.Card).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(tr("NOW", "MOST"), style = MaterialTheme.typography.labelSmall, color = Brand.Moss)
        Spacer(Modifier.width(10.dp))
        Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, maxLines = 2, modifier = Modifier.weight(1f))
        Text(
            tr("Day off", "Szabadnap"), style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Brand.Moss, maxLines = 1,
            modifier = Modifier.padding(start = 8.dp).clip(RoundedCornerShape(50)).background(Brand.Sage).tappable(onClick = onDayOff).padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/** How strict tend is: three levels, each said plainly; Strict comes with Plus. Bo's peeks belong here too. */
@Composable
private fun StrictCard(plus: Boolean, strict: Boolean, strictEndsAt: Long, onStrict: (Boolean) -> Unit, onPlus: () -> Unit, avatarName: String, peekOn: Boolean, onPeek: (Boolean) -> Unit) = Card {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember { app.tend.data.TendStore(ctx) }
    var chosen by remember { androidx.compose.runtime.mutableStateOf(if (strict) app.tend.core.StrictLevel.STRICT else store.effectiveStrict) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OptionCard(tr("Easy", "Laza"), tr("A 3 s wait (8 s in the evening). $avatarName peeks in after 20 minutes. Every way in is there.", "3 mp várakozás (este 8). $avatarName 20 perc után néz be. Minden kiskapu megvan."), chosen == app.tend.core.StrictLevel.EASY) {
            if (strict && strictEndsAt == 0L) onStrict(false)
            store.strictLevel = app.tend.core.StrictLevel.EASY; chosen = store.strictLevel
        }
        OptionCard(tr("Balanced", "Kiegyensúlyozott"), tr("A 5 s wait (10 s in the evening). $avatarName peeks in after 15 minutes. Quick look 3 times a day.", "5 mp várakozás (este 10). $avatarName 15 perc után néz be. Gyors belesés naponta 3-szor."), chosen == app.tend.core.StrictLevel.BALANCED) {
            if (strict && strictEndsAt == 0L) onStrict(false)
            store.strictLevel = app.tend.core.StrictLevel.BALANCED; chosen = store.strictLevel
        }
        OptionCard(tr("Strict", "Szigorú"), tr("A 10 s wait, no quick look, no 5 minutes for acorns, no day off; limits only get tighter; off only from the next day.", "10 mp várakozás, nincs Gyors belesés, nincs 5 perc makkért, nincs Szabadnap; a limitek csak szigorodhatnak; kikapcsolni csak másnaptól lehet."), chosen == app.tend.core.StrictLevel.STRICT, plusMark = !plus) {
            if (!plus) onPlus()
            else { if (!strict) onStrict(true); store.strictLevel = app.tend.core.StrictLevel.STRICT; chosen = app.tend.core.StrictLevel.STRICT }
        }
        if (strict && strictEndsAt > 0) Text(
            tr("Strict until midnight, then ${if (store.strictLevel == app.tend.core.StrictLevel.EASY) "Easy" else "Balanced"}.", "Éjfélig szigorú, utána ${if (store.strictLevel == app.tend.core.StrictLevel.EASY) "laza" else "kiegyensúlyozott"}."),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Coral,
        )
        SwitchRow(tr("$avatarName peeks in", "$avatarName benéz"), tr("From the edge of the screen, after a while in a picked app (by the level above)", "A képernyő széléről, egy idő után egy kiválasztott appban (a fenti fokozat szerint)"), peekOn, onPeek)
    }
}

/** The weekend's extra hour: never, always, or offered when the minutes run out (Friday from 18:00 counts). */
@Composable
private fun WeekendRow(strict: Boolean) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember { app.tend.data.TendStore(ctx) }
    var mode by remember { androidx.compose.runtime.mutableStateOf(store.weekendMode) }
    Text(tr("Weekend", "Hétvége"), style = MaterialTheme.typography.bodyLarge)
    Text(
        if (strict) tr("Not in strict mode.", "Szigorú fokozatban nincs.") else tr("+${BudgetConfig.WEEKEND_PLUS} minutes on Saturday and Sunday (Friday from 18:00)", "+${BudgetConfig.WEEKEND_PLUS} perc szombaton és vasárnap (pénteken 18:00-tól)"),
        style = MaterialTheme.typography.bodyMedium,
    )
    val modes = listOf(app.tend.core.WeekendMode.OFF, app.tend.core.WeekendMode.ALWAYS, app.tend.core.WeekendMode.ASK)
    Choice(listOf(tr("Off", "Ki"), tr("Always", "Mindig"), tr("Ask me", "Kérdezzen")), modes.indexOf(mode)) { store.weekendMode = modes[it]; mode = modes[it] }
}

/** How much Bo says: three levels, the single notes under "Details". */
@Composable
private fun NotesCard(
    avatarName: String, reminderEnabled: Boolean, onReminder: (Boolean) -> Unit, notesMorning: Boolean, notesNudges: Boolean,
    onNotes: (String, Boolean) -> Unit, weeklyLetter: Boolean, onWeeklyLetter: (Boolean) -> Unit, onChanged: () -> Unit,
) = Card {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember { app.tend.data.TendStore(ctx) }
    var level by remember { androidx.compose.runtime.mutableStateOf(store.notesLevel) }
    val levels = app.tend.core.NotesLevel.entries
    Choice(listOf(tr("Quiet", "Csendes"), tr("Normal", "Normál"), tr("Chatty", "Beszédes")), levels.indexOf(level)) {
        store.applyNotesLevel(levels[it]); level = levels[it]
        // The evening recap and the Sunday letter are alarms: set again (or cancelled) for the new level.
        app.tend.reminder.RecapReminder.schedule(ctx); app.tend.reminder.WeeklyLetter.schedule(ctx)
        onChanged()
    }
    Text(
        when (level) {
            app.tend.core.NotesLevel.QUIET -> tr("Only what you start yourself (timers) and calls to the fire.", "Csak amit te indítasz (időzítők), és a tűzhöz hívás.")
            app.tend.core.NotesLevel.NORMAL -> tr("The morning plan, a heads-up at 10 minutes left, the evening recap, the Sunday letter. At most 3 a day.", "Reggeli terv, jelzés 10 percnél, esti összefoglaló, vasárnapi levél. Naponta legfeljebb 3.")
            app.tend.core.NotesLevel.CHATTY -> tr("All of those, and $avatarName may say one more a day. At most 4 a day.", "Mindez, és $avatarName naponta egyszer többet szólhat. Naponta legfeljebb 4.")
        },
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, minLines = 2,
    )
    var details by remember { androidx.compose.runtime.mutableStateOf(false) }
    Text(
        if (details) tr("Hide details", "Részletek elrejtése") else tr("Details", "Részletek"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { details = !details }.padding(vertical = 6.dp),
    )
    if (details) {
        SwitchRow(tr("Morning plan", "Reggeli terv"), tr("Today's minutes and what's worth the most", "A mai percek, és ami a legtöbbet éri"), notesMorning) { onNotes("morning", it) }
        SwitchRow(tr("Gentle nudges", "Finom emlékeztetők"), tr("When 10 minutes are left", "Amikor 10 perc maradt"), notesNudges) { onNotes("nudges", it) }
        SwitchRow(tr("Evening recap", "Esti összefoglaló"), tr("A note when the evening starts", "Üzenet, amikor kezdődik az este"), reminderEnabled, onReminder)
        SwitchRow(tr("Sunday letter", "Vasárnapi levél"), tr("Your week, on Sunday evening", "A heted, vasárnap este"), weeklyLetter, onWeeklyLetter)
    }
}

/** The day as a bar, midnight to midnight: when the evening slows things down and when locks close them. */
@Composable
private fun DayBar(eveningFrom: Int, evening: app.tend.core.EveningMode, locks: List<app.tend.core.LockSchedule>) {
    val today = java.time.LocalDate.now().dayOfWeek
    val moss = Brand.Moss
    val slow = Brand.Mint.copy(alpha = 0.55f)
    val track = Brand.Sage
    val muted = Brand.Muted
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(40.dp)) {
        val barH = 14.dp.toPx()
        val r = androidx.compose.ui.geometry.CornerRadius(barH / 2)
        fun x(h: Int) = size.width * h / 24f
        drawRoundRect(track, size = androidx.compose.ui.geometry.Size(size.width, barH), cornerRadius = r)
        fun span(from: Int, to: Int, c: Color) {
            if (from == to) return
            if (from < to) drawRect(c, androidx.compose.ui.geometry.Offset(x(from), 0f), androidx.compose.ui.geometry.Size(x(to) - x(from), barH))
            else { drawRect(c, androidx.compose.ui.geometry.Offset(x(from), 0f), androidx.compose.ui.geometry.Size(size.width - x(from), barH)); drawRect(c, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Size(x(to), barH)) }
        }
        if (evening != app.tend.core.EveningMode.OFF && eveningFrom >= 0) span(eveningFrom, 5, slow)
        locks.filter { it.on && today in it.days }.forEach { span(it.from, it.to, moss) }
        val text = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = 11.sp.toPx(); color = muted.toArgb(); textAlign = android.graphics.Paint.Align.CENTER }
        listOf(0, 6, 12, 18, 24).forEach { h ->
            text.textAlign = when (h) { 0 -> android.graphics.Paint.Align.LEFT; 24 -> android.graphics.Paint.Align.RIGHT; else -> android.graphics.Paint.Align.CENTER }
            drawContext.canvas.nativeCanvas.drawText("$h", x(h), size.height - 2.dp.toPx(), text)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(slow)); Text(tr("slower", "lassít"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(moss)); Text(tr("closed (today)", "zárva (ma)"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }
}

/** Which days a lock runs on: seven chips, Monday first. */
@Composable
private fun DayChips(days: Set<java.time.DayOfWeek>, onChange: (Set<java.time.DayOfWeek>) -> Unit) =
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        java.time.DayOfWeek.entries.forEach { d ->
            val on = d in days
            Text(
                d.getDisplayName(java.time.format.TextStyle.NARROW_STANDALONE, app.tend.core.Lang.locale).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), textAlign = TextAlign.Center,
                color = if (on) Color.White else Brand.Moss,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (on) Brand.Moss else Brand.Sage)
                    .tappable { val next = if (on) days - d else days + d; if (next.isNotEmpty()) onChange(next) }.padding(vertical = 8.dp),
            )
        }
    }

/** Which picked apps a lock closes: all of them, or the ones chosen. */
@Composable
private fun LockApps(catalog: AppCatalog, blocked: Set<String>, apps: Set<String>, onChange: (Set<String>) -> Unit) =
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val all = apps.isEmpty()
        Text(
            tr("All picked apps", "Minden kiválasztott app"), style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = if (all) Color.White else Brand.Moss,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (all) Brand.Moss else Brand.Sage).tappable { onChange(emptySet()) }.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        blocked.sortedBy { catalog.label(it).lowercase() }.forEach { pkg ->
            val on = !all && pkg in apps
            Text(
                catalog.label(pkg), style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), maxLines = 1,
                color = if (on) Color.White else Brand.Moss,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Brand.Moss else Brand.Sage)
                    .tappable { val next = if (on) apps - pkg else apps + pkg; onChange(next) }.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }

/** One lock: on or off, its hours, days and apps while on; a name for your own ones. */
@Composable
private fun LockEditor(catalog: AppCatalog, blocked: Set<String>, lock: app.tend.core.LockSchedule, title: String, onLock: (app.tend.core.LockSchedule) -> Unit, onRemove: (() -> Unit)? = null) {
    val daysText = when (lock.days) {
        app.tend.core.LockSchedule.ALL_DAYS -> tr("every day", "minden nap")
        app.tend.core.LockSchedule.WEEKDAYS -> tr("weekdays", "hétköznap")
        else -> tr("${lock.days.size} days a week", "heti ${lock.days.size} nap")
    }
    var editing by remember { androidx.compose.runtime.mutableStateOf(false) }
    SwitchRow(title, "%d:00 – %d:00 · %s".format(lock.from, lock.to, daysText) + if (lock.apps.isNotEmpty()) tr(" · ${lock.apps.size} apps", " · ${lock.apps.size} app") else "", lock.on) { editing = it; onLock(lock.copy(on = it)) }
    if (lock.on && editing) Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HourStepper(tr("From", "Ettől"), lock.from, Modifier.weight(1f)) { onLock(lock.copy(from = it)) }
            HourStepper(tr("To", "Eddig"), lock.to, Modifier.weight(1f)) { onLock(lock.copy(to = it)) }
        }
        DayChips(lock.days) { onLock(lock.copy(days = it)) }
        LockApps(catalog, blocked, lock.apps) { onLock(lock.copy(apps = it)) }
    }
    // Change it (hours, days, apps); your own ones can be renamed ("Studying") and removed too.
    if (lock.on || lock.id.startsWith("c")) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        var naming by remember { androidx.compose.runtime.mutableStateOf(false) }
        if (lock.on) Text(if (editing) tr("Done", "Kész") else tr("Change", "Módosítás"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { editing = !editing }.padding(vertical = 4.dp))
        if (lock.id.startsWith("c")) Text(tr("Rename", "Átnevezés"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { naming = true }.padding(vertical = 4.dp))
        onRemove?.let { remove ->
            Text(tr("Remove", "Törlés"), style = MaterialTheme.typography.labelLarge, color = Brand.Coral,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable(onClick = remove).padding(vertical = 4.dp))
        }
        if (naming) {
            var name by remember { androidx.compose.runtime.mutableStateOf(lock.label) }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { naming = false },
                title = { Text(tr("Name", "Név")) },
                text = { androidx.compose.material3.OutlinedTextField(name, { name = it.take(20) }, singleLine = true) },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { naming = false; if (name.isNotBlank()) onLock(lock.copy(label = name.trim())) }) { Text(tr("Save", "Mentés"), color = Brand.Moss) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { naming = false }) { Text(tr("Cancel", "Mégse"), color = Brand.Muted) } },
                containerColor = Brand.Card,
            )
        }
    }
}

/**
 * Times of the day, all in one place: the evening (off, slower, or closed), work hours, and your own
 * times (Plus, three at most), over a bar showing today.
 */
@Composable
private fun PeriodsCard(
    catalog: AppCatalog, blocked: Set<String>, plus: Boolean, onPlus: () -> Unit, strict: Boolean,
    eveningFrom: Int, onEvening: (Int) -> Unit, lockSchedules: List<app.tend.core.LockSchedule>, onLockSchedule: (app.tend.core.LockSchedule) -> Unit, onChanged: () -> Unit,
) = Card {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember { app.tend.data.TendStore(ctx) }
    var evening by remember { androidx.compose.runtime.mutableStateOf(store.eveningMode) }
    val bed = lockSchedules.first { it.id == "bed" }
    val work = lockSchedules.first { it.id == "work" }
    val customs = lockSchedules.filter { it.id.startsWith("c") }
    DayBar(eveningFrom, evening, lockSchedules)
    Text(tr("EVENING", "ESTE"), style = MaterialTheme.typography.labelSmall, color = Brand.Muted, modifier = Modifier.padding(top = 6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(tr("Evening starts at", "Az este kezdete"), style = MaterialTheme.typography.bodyLarge)
            Text(tr("Until 5:00", "5:00-ig tart"), style = MaterialTheme.typography.bodyMedium)
        }
        EveningStepper(eveningFrom, onEvening)
    }
    val modes = app.tend.core.EveningMode.entries
    Choice(listOf(tr("Off", "Ki"), tr("Slower", "Lassít"), tr("Closed", "Zár")), modes.indexOf(evening)) {
        if (strict && evening == app.tend.core.EveningMode.LOCK && modes[it] != app.tend.core.EveningMode.LOCK) return@Choice
        store.eveningMode = modes[it]; evening = modes[it]; onChanged()
    }
    Text(
        when (evening) {
            app.tend.core.EveningMode.OFF -> tr("The evening is like any other time.", "Az este olyan, mint bármikor máskor.")
            app.tend.core.EveningMode.SLOW -> tr("The Megállító waits 5 s longer and ${store.avatar.displayName} gets sleepy.", "A Megállító 5 mp-cel tovább vár, és ${store.avatar.displayName} elálmosodik.")
            app.tend.core.EveningMode.LOCK -> tr("Slower from the evening, and closed at bedtime:", "Estétől lassít, lefekvéskor pedig zár:")
        },
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
    )
    if (evening == app.tend.core.EveningMode.LOCK) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        HourStepper(tr("Bedtime", "Lefekvés"), bed.from, Modifier.weight(1f)) { onLockSchedule(bed.copy(from = it)) }
        HourStepper(tr("Until", "Eddig"), bed.to, Modifier.weight(1f)) { onLockSchedule(bed.copy(to = it)) }
    }
    Text(tr("WORK", "MUNKA"), style = MaterialTheme.typography.labelSmall, color = Brand.Muted, modifier = Modifier.padding(top = 6.dp))
    LockEditor(catalog, blocked, work, tr("Work hours", "Munkaidő"), onLockSchedule)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Text(tr("MY OWN TIMES", "SAJÁT IDŐSZAKOK"), style = MaterialTheme.typography.labelSmall, color = Brand.Muted, modifier = Modifier.weight(1f))
        if (!plus) PlusMark()
    }
    customs.forEach { c ->
        LockEditor(catalog, blocked, c, c.label, onLockSchedule, onRemove = if (strict && c.on) null else ({ store.removeLock(c.id); onChanged() }))
    }
    if (customs.size < app.tend.core.LockSchedule.CUSTOM_MAX) Text(
        tr("+ A time of my own (studying, training…)", "+ Saját időszak (tanulás, edzés…)"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable {
            if (!plus) onPlus()
            else {
                val free = (0 until app.tend.core.LockSchedule.CUSTOM_MAX).first { i -> customs.none { it.id == "c$i" } }
                store.setLockSchedule(app.tend.core.LockSchedule.custom(free)); onChanged()
            }
        }.padding(vertical = 6.dp),
    )
    Text(tr("A lock keeps the picked apps closed: only \"Not now\", something better, or once a day 5 minutes for ${app.tend.data.TendStore.LOCK_PASS_ACORNS} acorns.", "A zár csukva tartja a kiválasztott appokat: csak „Most nem”, valami jobb, vagy naponta egyszer 5 perc ${app.tend.data.TendStore.LOCK_PASS_ACORNS} makkért."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
}

/**
 * What friends see of you (never your screen time or apps), and whether pranks may land in your
 * camp. Kept here and sent to the server when there's an account.
 */
@Composable
private fun FriendsPrivacy() = Card {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val store = remember { app.tend.data.TendStore(ctx) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    fun save() { scope.launch { app.tend.data.Friends.saveSettings(ctx) } }
    var done by remember { androidx.compose.runtime.mutableStateOf(store.shareDone) }
    var level by remember { androidx.compose.runtime.mutableStateOf(store.shareLevel) }
    var fire by remember { androidx.compose.runtime.mutableStateOf(store.shareFire) }
    var pranks by remember { androidx.compose.runtime.mutableStateOf(store.pranksOn) }
    SwitchRow(tr("What you did last", "Amit legutóbb csináltál"), tr("\"Went for a walk\", for a day or two", "„Sétált egyet”, egy-két napig"), done) { done = it; store.shareDone = it; save() }
    SwitchRow(tr("Your level", "A szinted"), tr("Next to your name", "A neved mellett"), level) { level = it; store.shareLevel = it; save() }
    SwitchRow(tr("You at the fire", "Te a tűznél"), tr("While you're phone-free", "Amíg telefon nélkül vagy"), fire) { fire = it; store.shareFire = it; save() }
    SwitchRow(tr("Pranks", "Csínyek"), tr("Toilet paper on your oak", "Vécépapír a tölgyfádon"), pranks) { pranks = it; store.pranksOn = it; save() }
    Text(tr("Your minutes and apps are never shown.", "A perceid és az appjaid soha nem látszanak."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
}
