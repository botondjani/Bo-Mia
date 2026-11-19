package app.tend.ui

import app.tend.core.tr

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.alpha
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
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipPath
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    /** What the companion knows for today's line (time, weather, yesterday, who's here); null: the plain line. */
    voice: app.tend.core.Voice.Moment? = null,
    /** Friends phone-free right now, and joining them (a phone-free activity of your own). */
    atFire: List<app.tend.data.Friends.AtFire> = emptyList(),
    onJoin: (app.tend.data.Friends.AtFire) -> Unit = {},
    /** Days left of the Plus trial (null when bought or over), and opening Plus. */
    plusDaysLeft: Int? = null,
    onPlus: () -> Unit = {},
    /** Strict mode: good night can't be undone before 7:00. */
    strict: Boolean = false,
    /** Opens the full picture: time won back, the weeks, each app. */
    onOpenInsights: () -> Unit = {},
    /** Ask once whether anonymous usage counts may be shared; answered with true or false. */
    askStats: Boolean = false,
    onStats: (Boolean) -> Unit = {},
    /** A new level to celebrate, once. */
    levelUp: Int? = null,
    onLevelUpSeen: () -> Unit = {},
    onOpenClearing: () -> Unit,
    /** The camp scene for the top of the page (drawn by the caller, which knows what's in it). */
    camp: @Composable () -> Unit = {},
    /** The animal waiting in the camp (tapped on home: into the camp to greet it). */
    visitor: app.tend.core.Visitor? = null,
    onGreetVisitor: () -> Unit = {},
    /** A small tip on the camp (how to move things), until it's been tried. */
    /** The length of the last plain "phone down" (15 to 120 minutes), and saying a new one. */
    timerMinutes: Int = 25,
    onTimerMinutes: (Int) -> Unit = {},
    /** Claiming a finished challenge, opening a friend's camp, and friends having changed. */
    onClaimStep: (app.tend.core.Step) -> Unit = {},
    onOpenRiver: (String?) -> Unit = {},
    onFriendsChanged: () -> Unit = {},
    /** Logged in (else home offers an account once, among the next steps: setup doesn't ask). */
    signedIn: Boolean = true,
    onOpenAccount: () -> Unit = {},
    /** Good days in a row (one missed day a week is forgiven). */
    streak: app.tend.core.Streak.State? = null,
    /** A friend calling you to the campfire just now (their name). */
    fireCall: String? = null,
    onAnswerCall: () -> Unit = {},
    onDismissCall: () -> Unit = {},
    /** Friends who did something today, with a cheer for each (once a day). */
    friendsToday: List<app.tend.data.Friends.Friend> = emptyList(),
    cheered: Set<String> = emptySet(),
    onCheer: (app.tend.data.Friends.Friend) -> Unit = {},
    campTip: app.tend.core.Tip? = null,
    onCampTip: (app.tend.core.Tip) -> Unit = {},
    steps: List<app.tend.core.StepState> = emptyList(),
    /** Bo's quest for today: what, whether it's done and claimed, and claiming it. */
    quest: app.tend.core.Quest? = null,
    questDone: Boolean = false,
    questClaimed: Boolean = false,
    onClaimQuest: () -> Unit = {},
    /** Sunday evening and Monday: the week (minutes got back, active days, scrolls let go), to share or put away. */
    weekStory: Triple<Int, Int, Int>? = null,
    /** Saturday or Sunday with the minutes gone: one more hour on offer, for today only. */
    weekendExtra: Boolean = false,
    onWeekendExtra: () -> Unit = {},
    /** The phone's whole screen time in minutes, the last 7 days, today last (empty without usage access). */
    screenTime: List<Int?> = emptyList(),
    onShareWeek: () -> Unit = {},
    onWeekStoryDone: () -> Unit = {},
    onStep: (app.tend.core.Step) -> Unit = {},
    /** Bumped when 'Do one activity' is tapped: the list comes into view with a glow. */
    focusActivities: Int = 0,
    /** "Good night" was pressed and it's not 7:00 yet. */
    sleeping: Boolean = false,
    onWakeUp: () -> Unit = {},
    onOpenRecap: () -> Unit,
    /** Show the one-time widget tip; called with true when it was handled. */
    showNextSteps: Boolean = false,
    /** No app asks for a pause yet: suggested among the next steps, after the account. */
    pauseSuggested: Boolean = false,
    onPauseSetup: () -> Unit = {},
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
    if (explain == "streak" && streak != null) StreakDialog(avatar.displayName, streak) { explain = "" }
    val remaining = Budget.remainingMinutes(cfg, day)
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(Modifier.fillMaxSize()) {
    // One page that scrolls: the camp, the activities, what's done.
    Column(
        Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        TodayHeader(day.date, Triple(remaining, (cfg.freeMinutes + day.earnedMinutes).coerceAtLeast(1), Budget.earnableFrom(cfg, day, tasks)), { explain = "minutes" }) {
            IconButtonSoft(Icons.Filled.Settings, tr("Settings", "Beállítások"), onOpenSettings)
        }

        // Hero: the camp, with the companion living in it; what they say, today's minutes, and the
        // acorns to spend. Tapping it opens the camp.
        Box(
            // Edge to edge: the camp as wide as the screen.
            Modifier.fillMaxWidth().layout { m, c ->
                val extra = 20.dp.roundToPx()
                val p = m.measure(c.copy(minWidth = c.maxWidth + 2 * extra, maxWidth = c.maxWidth + 2 * extra))
                layout(c.maxWidth, p.height) { p.place(-extra, 0) }
            }.aspectRatio(1.6f).campEntrance().pointerInput(visitor) {
                // Tapped: into the camp; on the animal waiting there, in to greet it.
                detectTapGestures { at ->
                    val v = visitor
                    val near = v != null && kotlin.math.abs(v.x - at.x / size.width) * size.width < size.height * 0.1f &&
                        (at.y / size.height) in (v.y - 0.18f)..(v.y + 0.04f)
                    if (near) onGreetVisitor() else onOpenClearing()
                }
            },
        ) {
            camp()
            // What the companion says: one small bubble, so the camp shows.
            Text(
                voice?.let(app.tend.core.Voice::home) ?: Moods.line(cfg, day, tasks, welcomeBack, canClose = permissions.overlay),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp), color = Brand.InkOnPill, maxLines = 3,
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
            // Good days in a row, from the second one on: a small flame under the acorns.
            if (streak != null && streak.days >= 2) Row(
                Modifier.align(Alignment.TopEnd).padding(top = 54.dp, end = 16.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xF0FFFFFF)).tappable { explain = "streak" }.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlameIcon(15.dp)
                Spacer(Modifier.width(4.dp))
                Text(tr("${streak.days} days", "${streak.days} nap"), style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = Color(0xFF3D2A1C))
            }
            // Along the bottom: a tip on the left (wrapping, never under the button), and the way in, plainly:
            // a button to the camp, where it's bigger and everything happens.
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom,
            ) {
                campTip?.let {
                    Text(
                        it.text(avatar.displayName), style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 15.sp), color = Brand.MossOnPill, maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                            .clip(RoundedCornerShape(14.dp)).background(Color(0xE6FFFFFF)).tappable { onCampTip(it) }.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                } ?: Spacer(Modifier.width(0.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss)
                        .tappable(onClick = onOpenClearing).padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tr("${avatar.displayName}'s camp", "${avatar.displayName} tábora"), style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Everything under the camp rises in as the page opens (the camp itself glides).
        Column(Modifier.pageBody(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        // The evening: tonight's recap, right under the camp.
        if (isEvening && !sleeping && pausedUntil <= System.currentTimeMillis()) {
            val seen = app.tend.data.TendStore(context).recapSeenOn == day.date.toString()
            Notice("moon", if (seen) tr("Tonight's recap", "A ma esti összefoglaló") else tr("Tonight's recap is ready", "Kész a ma esti összefoglaló"), if (seen) tr("See it again", "Megnézem újra") else tr("See what today gave back", "Nézd meg, mit adott vissza a mai nap"), onOpenRecap)
        }
        if (levelUp != null) LevelUpDialog(avatar, levelUp, onLevelUpSeen)

        if (onShareCrash != null) Notice("alert", tr("tend crashed last time", "A tend legutóbb összeomlott"), tr("Sorry! Tap to share the crash report.", "Bocsánat! Koppints, és megoszthatod a hibajelentést."), onShareCrash)
        if (brokenTimer != null) Notice(
            brokenTimer.icon, tr("The phone came back a little early", "A telefon kicsit korán került elő"),
            tr("${brokenTimer.title} needs ${brokenTimer.target} phone-free minutes. Tap to try again later.", "Ehhez ${brokenTimer.target} telefonmentes perc kell: ${brokenTimer.title.lowercase()}. Koppints, és később újra megpróbálhatod."), onDismissBroken,
        )
        // A friend at the fire: join them (unless a timer of your own is already going).
        if (focusSecondsLeft == null) atFire.firstOrNull()?.let { f ->
            val until = java.time.Instant.ofEpochMilli(f.until).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
            Notice(
                "friends", tr("${f.name}${if (atFire.size > 1) " and ${atFire.size - 1} more" else ""} at your campfire", "${f.name}${if (atFire.size > 1) " és még ${atFire.size - 1} barátod" else ""} a tábortüzednél ül"),
                tr("Phone-free until ${"%d:%02d".format(until.hour, until.minute)}. Put yours down too: +${app.tend.core.Acorns.TOGETHER_FIRE} acorns each", "${"%d:%02d".format(until.hour, until.minute)}-ig telefon nélkül. Tedd le te is: fejenként +${app.tend.core.Acorns.TOGETHER_FIRE} makk"), { onJoin(f) },
            )
        }
        // A friend calling to the fire (nobody's there yet): one tap puts your phone down too.
        if (focusSecondsLeft == null && atFire.isEmpty() && fireCall != null) Column(
            Modifier.fillMaxWidth().surface(radius = 20.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(tr("$fireCall calls you to the campfire", "$fireCall a tábortűzhöz hív"), style = MaterialTheme.typography.titleMedium)
            Text(tr("Put your phones down together: +${app.tend.core.Acorns.TOGETHER_FIRE} acorns each.", "Tegyétek le együtt a telefont: fejenként +${app.tend.core.Acorns.TOGETHER_FIRE} makk."), style = MaterialTheme.typography.bodyMedium, color = Brand.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipButton(tr("Join", "Csatlakozom"), onAnswerCall)
                ChipButton(tr("Not now", "Most nem"), onDismissCall, filled = false)
            }
        }
        // The trial's last days: said once a day on home, never in the way.
        if (plusDaysLeft != null && plusDaysLeft <= 3) Notice(
            "sparkle", if (plusDaysLeft <= 1) tr("Last day of tend Plus", "A tend Plus utolsó napja") else tr("$plusDaysLeft more day${if (plusDaysLeft == 1) "" else "s"} of tend Plus", "Még $plusDaysLeft nap tend Plus"),
            tr("See what stays free and what Plus keeps", "Nézd meg, mi marad ingyen, és mit tart meg a Plus"), onPlus,
        )
        if (pausedUntil > System.currentTimeMillis()) Notice("sun", tr("On a break until ${breakEnd(pausedUntil)}", "Szünet eddig: ${breakEnd(pausedUntil)}"), tr("No pauses, locks or counting. Tap to come back.", "Nincs szünet-képernyő, zárás, számolás. Koppints, ha visszajönnél."), onResume)
        // After "Good night": the paused apps rest until morning, with a way back.
        else if (sleeping) {
            // Waking them takes a moment's thought: a dialog, and a button held for a few seconds.
            var waking by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
            GoodNightCard(avatar, strict) { waking = true }
            if (waking) androidx.compose.material3.AlertDialog(
                onDismissRequest = { waking = false },
                icon = { CharacterView(avatar, app.tend.core.Mood.SLEEPING, 72.dp, lightweight = true) },
                title = { Text(tr("Wake the apps?", "Felébreszted az appokat?")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(tr("You said good night, and ${avatar.displayName} is asleep. The apps rest until 7:00. Do you really need them now?", "Jó éjt kívántál, és ${avatar.displayName} már alszik. Az appok 7:00-ig pihennek. Tényleg most kellenek?"))
                        HoldToConfirm(tr("Hold to wake the apps", "Tartsd nyomva az ébresztéshez"), seconds = 7f) { waking = false; onWakeUp() }
                    }
                },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { waking = false }) { Text(tr("Let them rest", "Hadd pihenjenek")) } },
                containerColor = Brand.Card,
            )
        }

        when {
            !permissions.usageAccess -> Notice("alert", tr("Limited mode", "Korlátozott mód"), tr("${avatar.displayName} can't see screen time. Tap to turn it on.", "${avatar.displayName} nem látja a képernyőidőt. Koppints, és bekapcsolhatod."), onFixPermissions)
            !permissions.overlay -> Notice(
                "alert", if (remaining <= 0) tr("Out of minutes, but your apps are open", "Elfogytak a percek, de az appjaid nyitva vannak") else tr("No pause before scrolling", "Nincs szünet görgetés előtt"),
                if (remaining <= 0) tr("tend can only notify you, not close them. Tap to fix.", "A tend csak értesíteni tud, bezárni nem. Koppints, és megjavítjuk.") else tr("Only a notification for now. Tap to fix.", "Egyelőre csak értesítés jön. Koppints, és megjavítjuk."), onFixPermissions,
            )
        }
        var weekendAsk by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        if (weekendExtra) Notice("sun", tr("It's the weekend: one more hour?", "Hétvége van: kérsz még egy órát?"), tr("Today's minutes are gone. Tap to see how it works.", "A mai keret elfogyott. Koppints, és megnézheted, hogy működik."), { weekendAsk = true })
        if (weekendAsk) WeekendExtraDialog(onTake = { weekendAsk = false; onWeekendExtra() }, onDismiss = { weekendAsk = false })
        val needsHealth = tasks.any { it.verification == Verification.AUTOMATIC } &&
            (health == HealthConnect.Status.NEEDS_PERMISSION || health == HealthConnect.Status.NEEDS_INSTALL)
        if (monitorSlept && batteryRisk) Notice("alert", tr("${avatar.displayName} fell asleep in the background", "${avatar.displayName} elaludt a háttérben"), tr("Your phone paused tend for a while. Tap to keep ${avatar.displayName} awake.", "A telefonod egy időre leállította a tendet. Koppints, hogy ${avatar.displayName} ébren maradjon."), onKeepAwake)
        // In the first days the first steps ask for the widget; here only what can't wait.
        val importantOnly = nextStepsImportantOnly || firstDays
        // Signing up counts from day one (a new phone shouldn't lose the camp); the rest waits for the first days to pass.
        val nextSteps = showNextSteps && (needsHealth || batteryRisk || !signedIn || (!importantOnly && (!widgetPlaced || notesOff || pauseSuggested)))
        if (nextSteps) NextSteps(
            avatar,
            widget = !widgetPlaced && !importantOnly, onWidget = { if (!onAddWidget()) widgetGuide = true },
            health = if (needsHealth) health else null, onHealth = onConnectHealth,
            notes = notesOff && !importantOnly, onNotes = onAllowNotes,
            battery = batteryRisk && !monitorSlept, onBattery = onKeepAwake,
            account = !signedIn, onAccount = onOpenAccount,
            pause = pauseSuggested && !importantOnly, onPause = onPauseSetup,
            onDismiss = onNextStepsDone,
        ) else if (needsHealth) {
            Notice(
                "heart", if (health == HealthConnect.Status.NEEDS_INSTALL) tr("Get Health Connect", "Health Connect telepítése") else tr("Connect Health Connect", "Health Connect összekötése"),
                tr("Count walks and workouts automatically", "A séták és edzések maguktól beszámítanak"), onConnectHealth,
            )
        }

        if (askStats && !nextSteps && !firstDays) StatsAsk(onStats)

        // Phone down for as long as you like (15 to 120 minutes), and every app waits: first, as a card of its own.
        if (focusSecondsLeft == null) {
            var minutes by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(Tasks.clampTimer(timerMinutes)) }
            fun nudge(d: Int) { minutes = Tasks.clampTimer(minutes + d); onTimerMinutes(minutes) }
            // Minus and plus drawn, not typed: a font's glyphs sit off-centre in a circle.
            @Composable
            fun Nudge(plus: Boolean, on: Boolean, onTap: () -> Unit) = Box(
                Modifier.size(34.dp).clip(CircleShape).background(Brand.Mint.copy(alpha = if (on) 0.25f else 0.1f)).then(if (on) Modifier.tappable(onClick = onTap) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
                    val col = if (on) Brand.Moss else Brand.Muted.copy(alpha = 0.4f)
                    val w = 2.2.dp.toPx()
                    drawLine(col, androidx.compose.ui.geometry.Offset(0f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width, size.height / 2), w, androidx.compose.ui.graphics.StrokeCap.Round)
                    if (plus) drawLine(col, androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width / 2, size.height), w, androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
            @Composable
            fun Title(modifier: Modifier) = Column(modifier) {
                Text(tr("Phone down", "Telefon le"), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(tr("Earns minutes too", "Perceket is ad"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, maxLines = 1)
            }
            @Composable
            fun Stepper() {
                Nudge(false, minutes > Tasks.TIMER_MIN) { nudge(-Tasks.TIMER_STEP) }
                Text(tr("$minutes min", "$minutes perc"), style = MaterialTheme.typography.titleMedium, color = Brand.Moss, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1, modifier = Modifier.width(72.dp))
                Nudge(true, minutes < Tasks.TIMER_MAX) { nudge(Tasks.TIMER_STEP) }
            }
            // On a narrow screen in two lines (the title never squeezed), otherwise in one.
            val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 400
            if (narrow) Column(Modifier.fillMaxWidth().surface(radius = 20.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Title(Modifier.weight(1f))
                    ChipButton(tr("Start", "Indítás"), { onStartFocus(Tasks.timer(minutes)) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) { Stepper() }
            } else Row(Modifier.fillMaxWidth().surface(radius = 20.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Title(Modifier.weight(1f))
                Stepper()
                ChipButton(tr("Start", "Indítás"), { onStartFocus(Tasks.timer(minutes)) })
            }
        }
        val active = tasks.filter { it.id !in day.completedTasks }
        val done = tasks.filter { it.id in day.completedTasks }
        SectionHeader(tr("Instead of scrolling", "Görgetés helyett"), tr("${done.size} of ${tasks.size} done", "${done.size} / ${tasks.size} megvan"))
        if (tasks.isEmpty()) Notice("sparkle", tr("Pick what you'd rather do", "Válaszd ki, mit csinálnál szívesebben"), tr("A few activities that give minutes back", "Pár tevékenység, ami perceket ad vissza"), onOpenSettings)
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
            // Every card as tall as the tallest one (laid out unseen underneath): nothing jumps when swiping.
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().padding(end = if (active.size > 1) 36.dp else 0.dp).alpha(0f).clearAndSetSemantics { }) {
                    active.forEach { task ->
                        TaskRow(task, day, health, focusSecondsLeft?.takeIf { task.id == focusTaskId }, {}, {}, {}, exerciseTracked, sleepEstimated, Modifier.padding(vertical = 4.dp))
                    }
                }
                androidx.compose.foundation.pager.HorizontalPager(
                    pager, Modifier.matchParentSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(end = if (active.size > 1) 36.dp else 0.dp),
                    pageSpacing = 12.dp, verticalAlignment = Alignment.Top,
                ) { i ->
                    val task = active[i]
                    Box(Modifier.fillMaxHeight().padding(vertical = 4.dp).border(2.dp, Brand.Moss.copy(alpha = if (i == 0) glow.value else 0f), RoundedCornerShape(22.dp))) {
                        TaskRow(task, day, health, focusSecondsLeft?.takeIf { task.id == focusTaskId }, onStartFocus, onCheckOff, onUndo, exerciseTracked, sleepEstimated, Modifier.fillMaxHeight())
                    }
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
                Text(tr("Everything done today. ${avatar.displayName} is proud of you.", "Mára minden megvan. ${avatar.displayName} büszke rád."), style = MaterialTheme.typography.titleMedium)
            }
        }
        // Friends today: what they did, and a cheer for it, one tap each. Cheered: gone from here (still in Friends).
        // Kept small: only what they did in the last few hours, two at a time, and no more once you've
        // cheered three today or put the card away for the day.
        val uiPrefs = context.getSharedPreferences("tend_ui", android.content.Context.MODE_PRIVATE)
        val todayKey = java.time.LocalDate.now().toString()
        var cheerHidden by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(uiPrefs.getString("cheer_hidden", "") == todayKey) }
        val fresh = System.currentTimeMillis() - CHEER_FRESH_MS
        val toCheer = if (cheerHidden || cheered.size >= CHEERS_A_DAY) emptyList() else friendsToday.filter { it.id !in cheered && (it.lastDoneAt ?: 0) > fresh }
        if (toCheer.isNotEmpty()) Column(Modifier.fillMaxWidth().surface(radius = 20.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("FRIENDS TODAY", "A BARÁTAID MA"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text(
                    "\u00D7", style = MaterialTheme.typography.titleMedium, color = Brand.Muted,
                    modifier = Modifier.clip(CircleShape).tappable { cheerHidden = true; uiPrefs.edit().putString("cheer_hidden", todayKey).apply() }.padding(horizontal = 8.dp),
                )
            }
            toCheer.take(2).forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Face(f.companion, 32)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${f.name} ${app.tend.core.Tasks.doneShown(f.lastDone.orEmpty())}", style = MaterialTheme.typography.bodyMedium, color = Brand.Ink,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    val done = f.id in cheered
                    Text(
                        if (done) tr("Cheered", "Megtapsolva") else tr("\uD83D\uDC4F Cheer", "👏 Taps"), style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = if (done) Brand.Muted else Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (done) Brand.Sage else Brand.Moss)
                            .then(if (done) Modifier else Modifier.tappable { onCheer(f) }).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        // The extras, after what matters: challenges, Bo's quest and the level, as three small tiles.
        // Two buttons: everything to do for acorns in one place, and friends.
        var hubOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        var friendsOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        val waiting = app.tend.core.FirstSteps.current(steps).filter { it.claimable }.sumOf { it.step.acorns } + (quest?.takeIf { questDone && !questClaimed }?.acorns ?: 0)
        var claimed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
        var claimKey by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
        Box(Modifier.fillMaxWidth()) {
            HubButtons(waiting, friendsToday.size, onChallenges = {
                // Acorns waiting: tapped, they're taken at once (the list opens when there's nothing to take).
                if (waiting == 0) hubOpen = true
                else {
                    app.tend.core.FirstSteps.current(steps).filter { it.claimable }.forEach { onClaimStep(it.step) }
                    if (quest != null && questDone && !questClaimed) onClaimQuest()
                    app.tend.ui.Sounds.acorns(context)
                    claimed = waiting; claimKey++
                }
            }, onFriends = { friendsOpen = true })
            AcornsClaimed(claimed, claimKey, Modifier.align(Alignment.CenterStart).fillMaxWidth(0.5f))
        }
        if (hubOpen) ChallengesSheet(
            steps, onClaimStep, { step -> hubOpen = false; onStep(step) }, quest, questDone, questClaimed, onClaimQuest,
            level, challenge, { hubOpen = false; onOpenInsights() },
        ) { hubOpen = false }
        if (friendsOpen) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { friendsOpen = false }, containerColor = Brand.Sage, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.sheetBody()) {
                FriendsCard(onOpenSettings, { id -> friendsOpen = false; onOpenRiver(id) }, onFriendsChanged)
            }
        }
        weekStory?.let { (back, days, letGo) -> WeekStoryCard(back, days, letGo, onShareWeek, onWeekStoryDone) }
        // "Done today" right under the activities.
        if (done.isEmpty() && active.isNotEmpty()) {
            // Before the first one: an empty, dashed spot that says where finished things go.
            SectionHeader(tr("Done today", "Ma megvan"), tr("0 min", "0 perc"))
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
                    tr("Nothing yet. Your first one lands here.", "Még semmi. Ide kerül az első."),
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        if (done.isNotEmpty()) {
            SectionHeader(tr("Done today", "Ma megvan"), tr("+${done.sumOf { day.grants[it.id] ?: 0 }} min", "+${done.sumOf { day.grants[it.id] ?: 0 }} perc"))
            // Thin rows; more than three scroll inside their own small box.
            Column(
                Modifier.padding(bottom = Space.xl),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) { done.forEach { DoneRow(it, day, onUndo) } }
        }
        screenTime.lastOrNull()?.let { ScreenTimeRow(it, screenTime.getOrNull(screenTime.size - 2), onOpenInsights) }
        val today = app.tend.core.DaySummary(day.date, Budget.usedMinutes(day), remaining.coerceAtLeast(0), day.completedTasks.size)
        val week = app.tend.core.WeekOverview.of(history.filter { it.date != day.date } + today, day.date)
        if (week.days.count { it != null } >= 2) Box(Modifier.padding(bottom = Space.xl)) { WeekCard(week, onOpenInsights, screenTime) }
        }
    }
    }
}

/** Once: may tend send anonymous counts? Both answers are one tap, and it can be changed in settings. */
@Composable
private fun StatsAsk(onStats: (Boolean) -> Unit) = Column(
    Modifier.fillMaxWidth().surface().padding(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
) {
    Text(tr("Help make tend better?", "Segítesz jobbá tenni a tendet?"), style = MaterialTheme.typography.titleMedium)
    Text(tr("Share anonymous counts, like which screens get used. Never your apps, name or health data.", "Névtelen számok megosztása, például hogy mely képernyőket használják. Az appjaid, a neved és az egészségadataid soha."), style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(
            tr("Sure", "Persze"), style = MaterialTheme.typography.labelLarge, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Brand.Moss).tappable { onStats(true) }.padding(vertical = 10.dp),
        )
        Text(
            tr("No thanks", "Köszi, nem"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Brand.Sage).tappable { onStats(false) }.padding(vertical = 10.dp),
        )
    }
}

/** A finished activity: one thin, quiet line. */
@Composable
private fun DoneRow(task: TaskDef, day: DayState, onUndo: (TaskDef) -> Unit) = Row(
    Modifier.fillMaxWidth().graphicsLayer { alpha = 0.75f }.surface(radius = 16.dp).padding(horizontal = 14.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(Modifier.size(24.dp).clip(CircleShape).background(Brand.Moss), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Check, tr("Done", "Kész"), tint = Color.White, modifier = Modifier.size(14.dp))
    }
    Spacer(Modifier.width(10.dp))
    Text(
        task.title, style = MaterialTheme.typography.bodyLarge, color = Brand.Muted, maxLines = 1,
        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, modifier = Modifier.weight(1f),
    )
    day.grants[task.id]?.takeIf { it > 0 }?.let { Text("+$it", style = MaterialTheme.typography.labelLarge, color = Brand.Moss) }
    if (task.manual) Text(
        tr("Undo", "Visszavonás"), style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
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
    account: Boolean = false, onAccount: () -> Unit = {},
    pause: Boolean = false, onPause: () -> Unit = {},
    onDismiss: () -> Unit,
) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    class Step(val icon: String, val title: String, val body: String, val cta: String, val action: () -> Unit)
    val name = avatar.displayName
    val steps = listOfNotNull(
        // First, so the pause keeps working: one "Allow" in a system dialog.
        if (battery) Step("sparkle", tr("Keep $name awake", "Maradjon ébren: $name"), tr("Keeps the pause working in the background.", "Így a szünet a háttérben is működik."), tr("Allow", "Engedélyezem"), onBattery) else null,
        health?.let {
            Step(
                "heart", "Health Connect",
                tr("Walks, workouts and sleep count by themselves.", "A séták, az edzések és az alvás maguktól beszámítanak."), if (it == HealthConnect.Status.NEEDS_INSTALL) tr("Install", "Telepítés") else tr("Connect", "Összekötés"), onHealth,
            )
        },
        if (widget) Step("sun", tr("Home screen widget", "Widget a kezdőképernyőre"), tr("$name at a glance, next to your apps.", "$name egy pillantásra, az appjaid mellett."), tr("Add", "Hozzáadás"), onWidget) else null,
        if (notes) Step("moon", tr("Calm evenings", "Nyugodt esték"), tr("An evening recap, sleepy pauses at night and a Sunday letter.", "Esti összefoglaló, álmos szünetek éjjel és egy vasárnapi levél."), tr("Turn on", "Bekapcsolom"), onNotes) else null,
        // Last: an account keeps the camp safe and lets friends in (setup doesn't ask for one).
        if (account) Step("friends", tr("Keep your camp safe", "Tartsd biztonságban a táborod"), tr("A backup, and friends at your fire.", "Mentés, és barátok a tüzednél."), tr("Log in", "Belépés"), onAccount) else null,
        // Then: a pause before the apps that pull hardest (only the minutes count until then).
        if (pause) Step("sparkle", tr("A pause before an app", "Szünet egy app előtt"), tr("A few seconds before, say, Instagram opens.", "Pár másodperc, mielőtt mondjuk az Instagram megnyílik."), tr("Set up", "Beállítom"), onPause) else null,
    )
    val pager = androidx.compose.foundation.pager.rememberPagerState { steps.size }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (steps.size > 1) tr("NEXT STEPS · ${pager.currentPage + 1} OF ${steps.size}", "KÖVETKEZŐ LÉPÉSEK · ${pager.currentPage + 1} / ${steps.size}") else tr("NEXT STEP", "KÖVETKEZŐ LÉPÉS"),
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),
        )
        Text(
            tr("Not now", "Most nem"), style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
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
/**
 * Three small tiles side by side, the same height: the challenges (the round's ring and what's
 * next), Bo's quest (claim it right here once done), and the level with the weekly goal.
 */
/** Challenges and friends, side by side: each a big, calm button with what's waiting. */
@Composable
private fun HubButtons(waiting: Int, friendsToday: Int, onChallenges: () -> Unit, onFriends: () -> Unit) =
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        @Composable
        fun Button(icon: String, title: String, note: String?, highlight: Boolean, onClick: () -> Unit) = Row(
            Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(if (highlight) Brand.Moss else Brand.Card).tappable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TendIcons.Badge(icon, 34.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = if (highlight) Color.White else Brand.Ink, maxLines = 1)
                note?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = if (highlight) Color.White.copy(alpha = 0.85f) else Brand.Muted, maxLines = 1) }
            }
        }
        Button("star", tr("Challenges", "Kihívások"), if (waiting > 0) tr("$waiting acorns waiting", "$waiting makk vár") else tr("Quest and goals", "Küldetés és célok"), waiting > 0, onChallenges)
        Button("friends", tr("Friends", "Barátok"), if (friendsToday > 0) tr("$friendsToday did something today", "$friendsToday ma tett valamit") else tr("Your camps together", "Közös táborok"), false, onFriends)
    }

/** Everything for acorns in one place: today's quest, this week's goal and the level, and the challenges. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ChallengesSheet(
    steps: List<app.tend.core.StepState>, onClaimStep: (app.tend.core.Step) -> Unit, onGo: (app.tend.core.Step) -> Unit,
    quest: app.tend.core.Quest?, questDone: Boolean, questClaimed: Boolean, onClaimQuest: () -> Unit,
    level: app.tend.core.Level?, challenge: app.tend.core.Challenge?, onOpenInsights: () -> Unit,
    onDismiss: () -> Unit,
) = androidx.compose.material3.ModalBottomSheet(
    onDismissRequest = onDismiss, containerColor = Brand.Sage,
    // Open all the way at once: everything in view, no second pull.
    sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        Modifier.sheetBody(0.62f),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Text(tr("Challenges", "Kihívások"), style = MaterialTheme.typography.headlineSmall)
        // Today's quest.
        quest?.let { q ->
            val claimable = questDone && !questClaimed
            Row(Modifier.fillMaxWidth().surface(radius = 20.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("TODAY'S QUEST", "MAI KÜLDETÉS"), style = MaterialTheme.typography.labelSmall)
                    Text(q.title, style = MaterialTheme.typography.titleMedium)
                    Text(q.how, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Brand.Muted)
                }
                Spacer(Modifier.width(10.dp))
                when {
                    questClaimed -> Text(tr("Done", "Megvan"), style = MaterialTheme.typography.labelLarge, color = Brand.Muted)
                    claimable -> ChipButton(tr("+${q.acorns} acorns", "+${q.acorns} makk"), { onClaimQuest(); app.tend.ui.Sounds.acorns(context) })
                    else -> Text("+${q.acorns}", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
                }
            }
        }
        // This week's goal and the level.
        if (challenge != null || level != null) Column(Modifier.fillMaxWidth().surface(radius = 20.dp).tappable(onClick = onOpenInsights).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            challenge?.let { c ->
                Text(tr("THIS WEEK", "A HÉTEN"), style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(if (c.done) "\u2713" else "${c.progress.coerceAtMost(c.target)}/${c.target}", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Sage)) {
                    Box(Modifier.fillMaxWidth((c.progress / c.target.toFloat()).coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Mint))
                }
            }
            level?.let { l ->
                Text(tr("Level ${l.level} · ${l.into}/${l.needed} XP", "${l.level}. szint · ${l.into}/${l.needed} XP"), style = MaterialTheme.typography.bodyMedium, color = Brand.Muted, modifier = Modifier.padding(top = 4.dp))
                // Where XP comes from, most first.
                Text(
                    tr("XP: minutes left at the end of the day (up to ${app.tend.core.Progress.XP_UNUSED_DAY} a day) · an activity ${app.tend.core.Progress.XP_PER_ACTIVITY} · the weekly challenge ${app.tend.core.Progress.XP_PER_CHALLENGE} · today's quest ${app.tend.core.Progress.XP_QUEST} · phone down 1 per 5 min · a first step ${app.tend.core.Progress.XP_STEP}",
                        "XP: a nap végén megmaradt keret (napi akár ${app.tend.core.Progress.XP_UNUSED_DAY}) · tevékenység ${app.tend.core.Progress.XP_PER_ACTIVITY} · heti kihívás ${app.tend.core.Progress.XP_PER_CHALLENGE} · mai küldetés ${app.tend.core.Progress.XP_QUEST} · telefon le 5 percenként 1 · első lépés ${app.tend.core.Progress.XP_STEP}"),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted,
                )
            }
        }
        // The challenges, round by round.
        if (steps.isNotEmpty()) FirstStepsCard(steps, onClaim = { onClaimStep(it); app.tend.ui.Sounds.acorns(context) }, onGo = onGo)
    }
}


@Composable
private fun LevelUpDialog(avatar: Avatar, level: Int, onDone: () -> Unit) = androidx.compose.ui.window.Dialog(onDismissRequest = onDone) {
    Column(
        Modifier.fillMaxWidth().surface(radius = 28.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BobbingCharacter(avatar, app.tend.core.Mood.OUTSIDE, 120.dp)
        Text(tr("Level $level!", "$level. szint!"), style = MaterialTheme.typography.headlineMedium, color = Brand.Moss)
        // What the level brings: a new home, things in the shop, a perk on round levels.
        val news = app.tend.core.Perks.newAt(level)
        if (news.size <= 1) Text(
            tr("${avatar.displayName} levelled up: ${news.firstOrNull().orEmpty()}.", "${avatar.displayName} szintet lépett: ${news.firstOrNull().orEmpty()}."),
            style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        else Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brand.Sage).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            news.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = Brand.Ink) }
        }
        app.tend.core.Home.TIERS.firstOrNull { it.first > level }?.let { (at, id) ->
            Text(tr("Next home at level $at: ${app.tend.core.Things.byId(id)?.name?.lowercase()}", "Következő otthon a(z) $at. szinten: ${app.tend.core.Things.byId(id)?.name?.lowercase()}"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        PrimaryButton(tr("Nice", "Szuper"), onDone)
    }
}

/**
 * The header, the same on home and in the camp (so nothing moves between them): the date, a
 * greeting, today's minutes (left, available, still to earn), and one round button: [button].
 */
@Composable
internal fun TodayHeader(date: java.time.LocalDate, minutes: Triple<Int, Int, Int>?, onMinutes: () -> Unit, button: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).then(sharedCamp("header-text") ?: Modifier)) {
            Text(date.format(DateTimeFormatter.ofPattern(tr("EEEE, MMMM d", "MMMM d., EEEE"), app.tend.core.Lang.locale)), style = MaterialTheme.typography.bodyMedium)
            Text(greeting(), style = MaterialTheme.typography.headlineMedium)
        }
        // Today's minutes, next to the greeting: what's left, a small bar, what's still to earn.
        minutes?.let { (left, available, toEarn) ->
            MinutesSummary(left, available, toEarn, Modifier.padding(end = 12.dp).then(sharedCamp("header-minutes") ?: Modifier), onMinutes)
        }
        button()
    }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> tr("Good morning", "Jó reggelt")
    in 12..17 -> tr("Good afternoon", "Szép napot")
    else -> tr("Good evening", "Szép estét")
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
    modifier: Modifier = Modifier,
) = Column(
    modifier.fillMaxWidth().graphicsLayer { alpha = if (task.id in day.completedTasks) 0.55f else 1f }
        .surface().padding(horizontal = 14.dp, vertical = 10.dp),
    verticalArrangement = Arrangement.Center,
) {
    val done = task.id in day.completedTasks
    val progress = Tasks.progress(task, day)
    // Without a workout tracker the workout is a 30-minute phone-off timer; without a sleep tracker sleep is estimated.
    // A workout can always be done as a 30-minute phone-off session; a tracker counts it too.
    val workoutTimer = task.id == Tasks.MOVE.id
    val estimated = task.id == Tasks.SLEEP.id && sleepEstimated
    val measured = !(workoutTimer && !exerciseTracked) && task.verification == Verification.AUTOMATIC && (health == HealthConnect.Status.READY || estimated)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (done) Box(Modifier.size(38.dp).clip(CircleShape).background(Brand.Moss), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, tr("Done", "Kész"), tint = Color.White, modifier = Modifier.size(20.dp))
        } else TendIcons.Badge(task.icon, 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title, style = MaterialTheme.typography.bodyLarge, color = if (done) Brand.Muted else Brand.Ink,
                textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            )
            // One short line: what to do (how long, how many steps); what it gives sits on the right, apart.
            val needsHealth = task.verification == Verification.AUTOMATIC && !measured && task.id != Tasks.SLEEP.id && !workoutTimer
            Text(
                when {
                    done -> day.grants[task.id]?.takeIf { it > 0 }?.let { tr("+$it min earned", "+$it perc megszerezve") } ?: tr("Done for today", "Mára megvan")
                    estimated && progress != null -> tr(
                        "About " + progressLabel(task, progress).replace(" sleep last night", ", from your screen"),
                        "Kb. " + progressLabel(task, progress).replace(" tegnap éjjel", ", a képernyőd alapján"),
                    )
                    measured && progress != null -> progressLabel(task, progress)
                    workoutTimer || task.verification.timed -> tr("${task.target} min, phone down", "${task.target} perc, telefon nélkül")
                    task.manual -> tr("Tick it off when done", "Pipáld ki, ha megvan")
                    task.id == Tasks.SLEEP.id -> tr("${task.target / 60} h of sleep", "${task.target / 60} óra alvás")
                    health == HealthConnect.Status.UNSUPPORTED -> tr("Not available on this phone", "Ezen a telefonon nem elérhető")
                    needsHealth -> tr("Needs Health Connect", "Health Connect kell hozzá")
                    else -> task.subtitle.substringAfter("· ")
                },
                style = MaterialTheme.typography.bodyMedium, color = if (needsHealth && !done) Brand.Coral else Brand.Muted,
            )
        }
        Spacer(Modifier.width(8.dp))
        // What it gives, then the button: the reward always in the same place, in the same words.
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!done && task.rewardMinutes > 0) Text(
                tr("+${task.rewardMinutes} min", "+${task.rewardMinutes} perc"),
                style = MaterialTheme.typography.titleSmall, color = Brand.Moss,
            )
            when {
                done && task.manual -> Text(
                    tr("Undo", "Visszavonás"), style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
                    modifier = Modifier.tappable { onUndo(task) }.padding(8.dp),
                )
                done -> Unit
                task.verification.timed || workoutTimer ->
                    ChipButton(focusSecondsLeft?.let { "%d:%02d".format(it / 60, it % 60) } ?: tr("Start", "Indítás"), { onStartFocus(task) })
                task.manual -> ChipButton(tr("Done", "Kész"), { onCheckOff(task) })
            }
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
    Tasks.OUTSIDE.id -> tr("%,d of %,d steps", "%,d / %,d lépés").format(app.tend.core.Lang.locale, progress, task.target)
    Tasks.SLEEP.id -> tr("%d h %02d of %d h sleep last night", "%d ó %02d p alvás a %d órából tegnap éjjel").format(app.tend.core.Lang.locale, progress / 60, progress % 60, task.target / 60)
    else -> tr("$progress of ${task.target} min of exercise", "$progress / ${task.target} perc mozgás")
}

/** Short moment of joy when a task completes: the character goes outside, minutes are credited. */
@Composable
fun Celebration(avatar: Avatar, tasks: List<TaskDef>, minutes: Int, onUndo: (() -> Unit)?, together: String = "", onDismiss: () -> Unit) = Dialog(onDismissRequest = onDismiss) {
    Column(
        Modifier.fillMaxWidth().surface(radius = 28.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BobbingCharacter(avatar, app.tend.core.Mood.OUTSIDE, 140.dp)
        // The manual cap is an internal rule: past it, no minutes, but the moment still counts.
        if (minutes > 0) Text(tr("+$minutes min", "+$minutes perc"), style = MaterialTheme.typography.headlineLarge, color = Brand.Moss)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically) {
            tasks.forEach { TendIcons.Badge(it.icon, 32.dp) }
            Text(tasks.joinToString(" · ") { it.title }, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            if (minutes > 0) tr("${avatar.displayName} loved that. Minutes are yours for today.", "${avatar.displayName} imádta. A percek ma a tieid.") else tr("${avatar.displayName} loved that.", "${avatar.displayName} imádta."),
            style = MaterialTheme.typography.bodyMedium,
        )
        // Done while a friend was phone-free too.
        if (together.isNotBlank()) Row(
            Modifier.clip(RoundedCornerShape(50)).background(Brand.Sage).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tr("Together with $together at the fire: +${app.tend.core.Acorns.TOGETHER_FIRE}", "Együtt a tűznél: $together veled tartott, +${app.tend.core.Acorns.TOGETHER_FIRE}"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
            Spacer(Modifier.width(4.dp))
            AcornIcon(14.dp)
        }
        PrimaryButton(tr("Nice", "Szuper"), onDismiss, modifier = Modifier.padding(top = 8.dp))
        if (onUndo != null) Text(
            tr("Oops, undo", "Hoppá, visszavonom"), style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
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
            hello -> tr("HERE WE GO!", "KEZDJÜK!")
            task.id == Tasks.MOVE.id -> tr("WORKOUT, PHONE OFF", "EDZÉS, TELEFON NÉLKÜL")
            task.id == Tasks.TIMER_ID -> tr("PHONE DOWN", "TELEFON LE")
            phoneFree -> tr("PHONE-FREE TIME", "TELEFONMENTES IDŐ")
            task.id == Tasks.LEARN.id -> tr("LEARNING TOGETHER", "EGYÜTT TANULUNK")
            else -> tr("READING TOGETHER", "EGYÜTT OLVASUNK")
        },
        style = MaterialTheme.typography.labelSmall,
    )
    Box(contentAlignment = Alignment.Center) {
        val total = task.target * 60f
        // Wide enough that the companion and the time sit inside the ring with room to spare.
        androidx.compose.foundation.Canvas(Modifier.size(300.dp)) {
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
                    CharacterView(avatar, app.tend.core.Mood.HAPPY, 128.dp, reading = !phoneFree && task.id != Tasks.TIMER_ID, prop = prop)
                }
                // Phone down: the phone lies face down next to the companion, asleep.
                if (task.id == Tasks.TIMER_ID) FaceDownPhone(Modifier.align(Alignment.BottomEnd).offset(x = 70.dp, y = 2.dp))
            }
            Text("%d:%02d".format(secondsLeft / 60, secondsLeft % 60), style = MaterialTheme.typography.headlineLarge, color = Brand.Moss)
        }
    }
    Text(task.title, style = MaterialTheme.typography.titleLarge)
    if (phoneFree) {
        Text(
            tr("Lock your phone and put it away. ${avatar.displayName} keeps time and lets you know when it's done: +${task.rewardMinutes} min.", "Zárd le a telefonod, és tedd félre. ${avatar.displayName} méri az időt, és szól, ha megvan: +${task.rewardMinutes} perc."),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        val left = (app.tend.core.PhoneFree.GRACE_SECONDS - screenOnSeconds).coerceAtLeast(0)
        Pill(tr("Other apps allowed: %d:%02d left", "Más appokra még: %d:%02d").format(left / 60, left % 60), if (left < 60) Brand.Coral.copy(alpha = 0.2f) else Brand.Sage)
    } else if (task.id == Tasks.TIMER_ID) Text(
        tr("Every app waits until the timer ends, and it gives minutes back. ${avatar.displayName} keeps time with you.", "Minden app megvárja a végét, és perceket ad vissza. ${avatar.displayName} veled méri az időt."),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    ) else Text(
        tr("${avatar.displayName} is ${if (task.id == Tasks.LEARN.id) "learning" else "reading"} with you. +${task.rewardMinutes} min when the timer ends. ", "${avatar.displayName} veled ${if (task.id == Tasks.LEARN.id) "tanul" else "olvas"}. +${task.rewardMinutes} perc, amikor lejár az időzítő. ") +
            tr("You can leave tend; opening a paused app ends the session.", "Kiléphetsz a tendből; ha megnyitsz egy szüneteltetett appot, az véget vet neki."),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
    SecondaryButton(tr("Stop for now", "Most abbahagyom"), onCancel, modifier = Modifier.padding(horizontal = Space.xxl))
}


/**
 * A phone lying face down on the ground, seen from a little above: the back up (camera bump, lenses,
 * flash, a soft sheen), its thickness showing below, a shadow under it, and two small z's rising.
 */
@Composable
internal fun FaceDownPhone(modifier: Modifier = Modifier) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "zz")
    val phase by t.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.LinearEasing)), label = "z")
    androidx.compose.foundation.Canvas(modifier.size(92.dp, 70.dp)) {
        val u = size.width / 120f // one unit of the phone's own measure (it's 100 long, 48 wide)
        val cx = size.width * 0.46f
        val cy = size.height * 0.72f
        val tilt = -16f
        val squash = 0.42f
        val thick = 5.5f * u
        val body = androidx.compose.ui.geometry.RoundRect(-50f * u, -24f * u, 50f * u, 24f * u, androidx.compose.ui.geometry.CornerRadius(10f * u))
        fun flat(dy: Float = 0f, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) =
            withTransform({ translate(cx, cy + dy); scale(1f, squash, androidx.compose.ui.geometry.Offset.Zero); rotate(tilt, androidx.compose.ui.geometry.Offset.Zero) }, block)
        val path = androidx.compose.ui.graphics.Path().apply { addRoundRect(body) }
        // The shadow, soft and a little to the right.
        flat(thick + 2f * u) {
            drawPath(androidx.compose.ui.graphics.Path().apply { addRoundRect(body.copy(left = body.left - 1f * u, right = body.right + 5f * u, top = body.top + 1f * u, bottom = body.bottom + 5f * u)) }, Color.Black.copy(alpha = 0.10f))
        }
        // The side: the same outline, stacked down, darker toward the bottom.
        val steps = 8
        for (i in steps downTo 1) flat(thick * i / steps) { drawPath(path, androidx.compose.ui.graphics.lerp(Color(0xFF3C434B), Color(0xFF1C2024), i / steps.toFloat())) }
        // A side button, a sliver on the near edge.
        flat(thick * 0.5f) { drawRoundRect(Color(0xFF59626C), androidx.compose.ui.geometry.Offset(8f * u, 22.5f * u), androidx.compose.ui.geometry.Size(16f * u, 2.2f * u), androidx.compose.ui.geometry.CornerRadius(1f * u)) }
        flat {
            // The back: graphite, a touch lighter at the far end.
            drawPath(path, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF4A525B), Color(0xFF2E343A)), androidx.compose.ui.geometry.Offset(-50f * u, -24f * u), androidx.compose.ui.geometry.Offset(50f * u, 24f * u)))
            drawPath(path, Color(0xFF6B747E), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f * u))
            // A soft sheen across it.
            clipPath(path) {
                drawPath(androidx.compose.ui.graphics.Path().apply {
                    moveTo(-8f * u, -30f * u); lineTo(10f * u, -30f * u); lineTo(-10f * u, 30f * u); lineTo(-28f * u, 30f * u); close()
                }, Color.White.copy(alpha = 0.10f))
            }
            // The camera bump in the far corner: lenses with a glint, and the flash.
            val bump = androidx.compose.ui.geometry.RoundRect(-44f * u, -19f * u, -18f * u, 7f * u, androidx.compose.ui.geometry.CornerRadius(7f * u))
            drawPath(androidx.compose.ui.graphics.Path().apply { addRoundRect(bump.copy(top = bump.top + 1.5f * u, bottom = bump.bottom + 1.5f * u)) }, Color.Black.copy(alpha = 0.25f))
            drawPath(androidx.compose.ui.graphics.Path().apply { addRoundRect(bump) }, Color(0xFF23282D))
            drawPath(androidx.compose.ui.graphics.Path().apply { addRoundRect(bump) }, Color(0xFF59626C), style = androidx.compose.ui.graphics.drawscope.Stroke(1.1f * u))
            for (c in listOf(androidx.compose.ui.geometry.Offset(-37f * u, -12f * u), androidx.compose.ui.geometry.Offset(-37f * u, 0f), androidx.compose.ui.geometry.Offset(-25f * u, -12f * u))) {
                drawCircle(Color(0xFF7A848F), 5f * u, c)
                drawCircle(Color(0xFF0D0F12), 4f * u, c)
                drawCircle(Color(0xFF2B3A55), 2.2f * u, c)
                drawCircle(Color.White.copy(alpha = 0.75f), 0.9f * u, c + androidx.compose.ui.geometry.Offset(-1.2f * u, -1.2f * u))
            }
            drawCircle(Color(0xFFF4E7B9), 2f * u, androidx.compose.ui.geometry.Offset(-25f * u, 0f))
        }
        // Two z's rising from it, one after the other: it's asleep.
        for (k in 0..1) {
            val p = (phase + k * 0.5f) % 1f
            val zs = (5f + 3f * k) * u * (0.7f + 0.3f * p)
            val zx = cx + 22f * u + 10f * u * p + k * 8f * u
            val zy = cy - 12f * u - 34f * u * p
            val a = kotlin.math.sin(p * kotlin.math.PI.toFloat()).coerceAtLeast(0f)
            drawPath(androidx.compose.ui.graphics.Path().apply {
                moveTo(zx - zs / 2, zy - zs / 2); lineTo(zx + zs / 2, zy - zs / 2); lineTo(zx - zs / 2, zy + zs / 2); lineTo(zx + zs / 2, zy + zs / 2)
            }, Brand.Moss.copy(alpha = 0.8f * a), style = androidx.compose.ui.graphics.drawscope.Stroke(2.4f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

/**
 * After "Good night": a small night sky (deep blue, a crescent and a few stars) with the companion
 * asleep, the apps resting until 7:00, and (unless strict) a quiet way to wake them.
 */
@Composable
private fun GoodNightCard(avatar: Avatar, strict: Boolean, onWake: () -> Unit) = Box(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1E2A4A), Color(0xFF34406B)))),
) {
    // The sky: a crescent and stars, drawn behind.
    androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
        val stars = listOf(0.62f to 0.18f, 0.74f to 0.42f, 0.86f to 0.22f, 0.55f to 0.7f, 0.93f to 0.62f, 0.68f to 0.84f, 0.8f to 0.08f)
        stars.forEachIndexed { i, (x, y) -> drawCircle(Color.White.copy(alpha = if (i % 2 == 0) 0.75f else 0.4f), (if (i % 3 == 0) 1.8f else 1.2f) * density, androidx.compose.ui.geometry.Offset(x * size.width, y * size.height)) }
        val m = androidx.compose.ui.geometry.Offset(size.width - 26.dp.toPx(), 24.dp.toPx())
        val r = 10.dp.toPx()
        drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFFF4E7B9).copy(alpha = 0.22f), Color.Transparent), m, r * 2.6f), r * 2.6f, m)
        // A crescent: the moon with a bite of sky taken out of it.
        val moon = androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(m, r)) }
        val bite = androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(m + androidx.compose.ui.geometry.Offset(r * 0.5f, -r * 0.35f), r * 0.85f)) }
        drawPath(androidx.compose.ui.graphics.Path.combine(androidx.compose.ui.graphics.PathOperation.Difference, moon, bite), Color(0xFFF4E7B9))
    }
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CharacterView(avatar, app.tend.core.Mood.SLEEPING, 64.dp, lightweight = true)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(end = 34.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tr("Good night", "Jó éjt"), style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(tr("Your apps rest until 7:00.", "Az appjaid reggel 7:00-ig pihennek."), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            if (strict) Text(tr("Strict mode: they wake at 7:00", "Szigorú mód: 7:00-kor ébrednek"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Color.White.copy(alpha = 0.6f))
            else Text(
                tr("Need them?", "Kellenek?"), style = MaterialTheme.typography.labelLarge, color = Color.White,
                modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f)).tappable(onClick = onWake).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** What friends did in the last this-many hours is offered for a cheer on home. */
private const val CHEER_FRESH_MS = 4 * 3_600_000L

/** After this many cheers sent in a day, home stops offering more (the friends list still can). */
private const val CHEERS_A_DAY = 3

/** The phone's whole screen time today, every app, next to yesterday's. */
@Composable
private fun ScreenTimeRow(today: Int, yesterday: Int?, onOpen: () -> Unit) = Row(
    // Tapped: the full picture of your time.
    Modifier.fillMaxWidth().padding(bottom = Space.xl).surface(radius = 16.dp).tappable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    fun hm(m: Int) = if (m >= 60) tr("${m / 60} h ${m % 60} min", "${m / 60} ó ${m % 60} p") else tr("$m min", "$m perc")
    Column(Modifier.weight(1f)) {
        Text(tr("Screen time today", "Képernyőidő ma"), style = MaterialTheme.typography.bodyMedium)
        Text(tr("All apps", "Az összes app"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted)
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(hm(today), style = MaterialTheme.typography.titleMedium, color = Brand.Ink)
        if (yesterday != null) Text(tr("yesterday ${hm(yesterday)}", "tegnap ${hm(yesterday)}"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted)
    }
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Brand.Muted, modifier = Modifier.padding(start = 6.dp).size(20.dp))
}

/** Today's minutes in a header: what's left, a small bar of it, and what's still to earn. Home and the camp. */
@Composable
fun MinutesSummary(remaining: Int, available: Int, toEarn: Int, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
    // A little wider in Hungarian: "perc maradt" is longer than "min left".
    Column(modifier.width(if (app.tend.core.Lang.hu) 118.dp else 96.dp).tappable(onClick = onClick), horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(if (remaining > 0) "$remaining" else "0", style = MaterialTheme.typography.headlineMedium, color = if (remaining > 0) Brand.Moss else Brand.Coral)
            Spacer(Modifier.width(4.dp))
            Text(tr("min left", "perc maradt"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Ink, modifier = Modifier.padding(bottom = 4.dp))
        }
        val left by animateFloatAsState((remaining.toFloat() / available.coerceAtLeast(1)).coerceIn(0f, 1f), tween(700), label = "left")
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Brand.Card)) {
            Box(Modifier.fillMaxWidth(left).height(5.dp).clip(RoundedCornerShape(50)).background(if (remaining > 0) Brand.Mint else Brand.Coral))
        }
        Text(tr("+$toEarn to earn", "+$toEarn szerezhető"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted)
    }

/** What the weekend's extra hour is, before taking it. */
@Composable
fun WeekendExtraDialog(onTake: () -> Unit, onDismiss: () -> Unit) = androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(tr("Weekend hour", "Hétvégi óra")) },
    text = {
        Text(
            tr(
                "Weekends have more free time, so on Saturday and Sunday, once today's minutes are gone, you can take one more hour: +${BudgetConfig.WEEKEND_PLUS} minutes now, and the day's max goes up as much.\n\nIt's for today only, once a day: tomorrow starts with your usual minutes. Not in strict mode.",
                "Hétvégén több a szabadidő, ezért szombaton és vasárnap, ha elfogyott a mai keret, kérhetsz még egy órát: most +${BudgetConfig.WEEKEND_PLUS} perc, és a napi maximum is ennyivel nő.\n\nCsak mára szól, naponta egyszer: holnap a szokásos kerettel indulsz. Szigorú módban nem kérhető.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    },
    confirmButton = { androidx.compose.material3.TextButton(onClick = onTake) { Text(tr("Take the hour", "Kérem az órát"), color = Brand.Moss) } },
    dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(tr("Not now", "Most nem")) } },
    containerColor = Brand.Card,
)
