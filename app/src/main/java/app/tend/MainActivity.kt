package app.tend

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import app.tend.core.Budget
import app.tend.core.Patterns
import app.tend.core.Recap
import app.tend.reminder.RecapReminder
import app.tend.reminder.WeeklyLetter
import java.time.LocalDate
import app.tend.core.TaskDef
import app.tend.ui.RecapScreen
import java.time.LocalTime
import app.tend.core.Tasks
import app.tend.health.HealthConnect
import app.tend.health.TaskSync
import app.tend.ui.Celebration
import androidx.health.connect.client.PermissionController
import app.tend.data.TendStore
import app.tend.monitor.MonitorService
import app.tend.ui.AppCatalog
import app.tend.ui.drawClearing
import app.tend.ui.FocusTimerScreen
import app.tend.ui.HomeScreen
import app.tend.ui.Onboarding
import app.tend.ui.PermissionActions
import app.tend.ui.PermissionLock
import app.tend.ui.Permissions
import app.tend.ui.PermissionsScreen
import app.tend.ui.SettingsScreen
import app.tend.ui.TendTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val store by lazy { TendStore(this) }
    private val handler = Handler(Looper.getMainLooper())

    /** Bumped whenever we come back to the app, so the UI re-reads the store. */
    private var refresh by mutableIntStateOf(0)

    /** What the companion wears now, and what waits on the shelf. */
    private var shelf by mutableStateOf<Pair<Set<app.tend.core.Accessory>, Set<app.tend.core.Accessory>>>(emptySet<app.tend.core.Accessory>() to emptySet())

    /** The user went to a settings page and came back without the permission. */
    private var usageAttempted = false
    private var overlayAttempted = false
    /** What the permission flow last opened, so coming back can take the next step by itself. */
    private var launched = Launched.NONE
    /** Health Connect here can read in the background: asked together with the rest. */
    private var healthBackground = false
    /** Android has refused a switch once (fallback when the lock state can't be read). */
    private var tripped = false

    private enum class Launched { NONE, TRIP, APP_INFO, OVERLAY, USAGE }

    private enum class Screen { HOME, SETTINGS, PERMISSIONS, CLEARING, RIVER, RECAP, SLEEP, INSIGHTS, PLUS }

    /** Set when the evening notification was tapped. */
    private var openRecap by mutableStateOf(false)
    private var openClearing by mutableStateOf(false)
    private var openActivities by mutableStateOf(false)
    /** Opened from "Anna put her phone down": join her at the fire. */
    private var joinFire by mutableStateOf(false)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (store.reminderPrompted) store.reminderEnabled = granted // asked for the evening reminder, not for nudges
        RecapReminder.schedule(this)
        // Allowed while a timer is already running: its countdown shows from now.
        if (granted && store.focusStartedAt > 0) app.tend.health.TimerAlarm.showRunning(this, store.focusTask, store.focusStartedAt + store.focusTask.target * 60_000L)
        refresh++
    }

    private val healthPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // tend is always light: dark status-bar icons even when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        loadCatalog()
        RecapReminder.schedule(this)
        app.tend.reminder.WeeklyLetter.schedule(this)
        app.tend.reminder.MorningNote.schedule(this)
        app.tend.reminder.DayClock.schedule(this)
        handleIntent(intent)
        setContent {
            TendTheme {
                androidx.compose.runtime.CompositionLocalProvider(app.tend.ui.LocalAccessories provides shelf.first) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) { catalog?.let { Content(it) } }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.getStringExtra(RecapReminder.EXTRA_OPEN)) {
            RecapReminder.OPEN_RECAP -> openRecap = true
            app.tend.reminder.WeeklyLetter.OPEN_CLEARING -> openClearing = true
            app.tend.data.Friends.OPEN_JOIN -> joinFire = true
            RecapReminder.OPEN_ACTIVITIES -> openActivities = true
        }
        // A friend's invite: keep the code (it waits through onboarding or logging in) and open the clearing.
        app.tend.data.Friends.codeFrom(intent?.dataString)?.let { code ->
            app.tend.data.Friends.setPendingCode(this, code)
            if (store.onboarded) openClearing = true
        }
    }

    private fun setReminder(enabled: Boolean) {
        store.reminderPrompted = true
        if (enabled && !RecapReminder.canNotify(this) && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        store.reminderEnabled = enabled
        RecapReminder.schedule(this)
        refresh++
    }

    /** The camp card for this week, to the share sheet. */
    private fun shareCamp() {
        val week = Patterns.week(LocalDate.now(), store.daySummaries(), store.answers())
        app.tend.data.Stats.log(this, "share_garden")
        app.tend.ui.ShareCard.share(
            this,
            app.tend.ui.ShareCard.Week(
                store.avatar, store.placed, store.accessories().first,
                gotBackMinutes = week.gotBackMinutes, activeDays = week.days.count { it.tasksDone > 0 }, letGo = week.letGo,
                level = app.tend.core.Progress.level(store.totalXp()).level,
                layout = campLayout(),
            ),
        )
    }

    /**
     * Google Play's review sheet, once, at a good moment: saying good night after a day with
     * something done, from the third day on. Play itself decides whether it shows.
     */
    private fun maybeAskReview() {
        if (store.reviewAsked || store.daysTended < 3 || store.today().completedTasks.isEmpty()) return
        store.reviewAsked = true
        app.tend.data.Stats.log(this, "review_asked")
        Review.ask(this)
    }

    /** An e-mail to the support address with the version and phone, nothing else. */
    private fun sendFeedback(mail: String) {
        val body = "\n\n—\ntend ${BuildConfig.VERSION_NAME} · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
        open(
            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_EMAIL, arrayOf(mail))
                .putExtra(Intent.EXTRA_SUBJECT, "tend feedback").putExtra(Intent.EXTRA_TEXT, body),
        )
    }

    /** Settings → Start over: everything on this phone goes, and onboarding begins again. */
    private fun startOver() {
        TaskSync.stopTimer(this)
        stopService(Intent(this, MonitorService::class.java))
        for (name in listOf("tend", "tend_monitor", "tend_ui", app.tend.data.Stats.PREFS, app.tend.data.Account.PREFS_NAME)) getSharedPreferences(name, MODE_PRIVATE).edit().clear().commit()
        filesDir.listFiles()?.forEach { it.deleteRecursively() }
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        app.tend.widget.Widgets.refresh(this, force = true)
        app.tend.ui.ThemeChoice.mode = null // read again from the (now empty) store
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    /** After a restore: stop what runs on the old data, then start again on the restored one. */
    private fun restartFresh() {
        TaskSync.stopTimer(this)
        stopService(Intent(this, MonitorService::class.java))
        app.tend.widget.Widgets.refresh(this, force = true)
        app.tend.ui.ThemeChoice.mode = null
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun openPlayListing() = open(
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")),
        Intent(Intent.ACTION_VIEW, Uri.parse(app.tend.ui.ShareCard.playUrl(this))),
    )

    /** Asks Android to let tend run in the background: one "Allow" in a system dialog. */
    private fun keepAwake() {
        store.monitorSlept = false
        if (BuildConfig.PLAY) {
            // Play build: the system list, where the user picks tend and "Don't optimise".
            hint("Find tend and choose \"Don't optimise\"")
            open(highlightTend(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)))
        } else {
            @android.annotation.SuppressLint("BatteryLife") // side-loaded build; the pause depends on it
            val ask = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            open(ask, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        refresh++
    }

    /** From the end of onboarding: notes on, the recap reminder included, asking Android if needed. */
    private fun allowNotes() {
        store.eveningPause = true
        store.weeklyLetter = true
        WeeklyLetter.schedule(this)
        store.reminderPrompted = true
        store.reminderEnabled = true
        store.notesMorning = true
        store.notesNudges = true
        if (!RecapReminder.canNotify(this) && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        RecapReminder.schedule(this)
        refresh++
    }

    /** A settings switch for one kind of note; turning one on asks Android first if needed. */
    private fun setNotes(kind: String, on: Boolean) {
        when (kind) {
            "morning" -> store.notesMorning = on
            "nudges" -> store.notesNudges = on
        }
        if (on && !RecapReminder.canNotify(this) && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        refresh++
    }

    private fun setWeeklyLetter(on: Boolean) {
        store.weeklyLetter = on
        if (on && !RecapReminder.canNotify(this) && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        WeeklyLetter.schedule(this)
        refresh++
    }

    /** Shares everything tend logged on this phone as one text, for testing. */
    private fun exportTestData() {
        val text = buildString {
            appendLine("tend test data · ${java.time.LocalDateTime.now()}")
            appendLine("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("app ${packageManager.getPackageInfo(packageName, 0).versionName}")
            appendLine("activities: ${store.selectedTasks.joinToString { it.id }} · evening from ${store.eveningFrom}")
            for (name in listOf("answers.csv", "history.csv", "misses.csv", "last_crash.txt")) {
                val f = java.io.File(filesDir, name)
                appendLine()
                appendLine("== $name ==")
                // Last lines only: a share intent carries well under 1 MB.
                appendLine(if (f.exists()) f.readLines().takeLast(1500).joinToString("\n") else "(none)")
            }
        }
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "tend test data")
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, "Export test data"))
    }

    /** Installed apps and, with usage access, how much each is used; read again once access is given. */
    private var catalog by mutableStateOf<AppCatalog?>(null)
    private var catalogHasUsage = false
    private var catalogLoading = false

    private fun loadCatalog() {
        catalogLoading = true
        val fresh = AppCatalog(this)
        fresh.preload { c ->
            val hasUsage = c.dailyMinutes.isNotEmpty()
            runOnUiThread {
                // Before setup is done, the picker starts from what's recommended: the social apps and
                // everything used a lot. Only while the user hasn't changed the pick themselves.
                val before = catalog?.let(AppCatalog::recommended).orEmpty()
                if (!store.onboarded && (store.blockedPackages.isEmpty() || store.blockedPackages == before)) store.blockedPackages = AppCatalog.recommended(c)
                catalog = c
                catalogHasUsage = hasUsage
                catalogLoading = false
                refresh++
            }
        }
        if (catalog == null) catalog = fresh
    }

    override fun onResume() {
        super.onResume()
        // Usage access just given (during setup, or later): read the apps again to suggest the right ones.
        if (!catalogLoading && !catalogHasUsage && permissions().usageAccess) loadCatalog()
        handler.removeCallbacksAndMessages(null)
        // Back from App info: when the lock can't be read, trust the visit and move on.
        if (launched == Launched.APP_INFO && restrictedMode() == null) store.restrictedUnlocked = true
        launched = Launched.NONE
        val nowMs = System.currentTimeMillis()
        if (store.onboarded && store.lastOpenedAt > 0 && nowMs - store.lastOpenedAt > 2 * DAY_MS) store.welcomeBackOn = LocalDate.now().toString()
        store.lastOpenedAt = nowMs
        if (store.onboarded) app.tend.data.Stats.appOpen(this)
        app.tend.data.Stats.flush(this)
        refresh++
        // Usage access is enough to count minutes; without the overlay the monitor nudges by notification.
        if (store.onboarded) lifecycleScope.launch { app.tend.data.Backup.uploadIfDue(this@MainActivity) }
        // What friends see (this week's activities count toward the together goal), and what they did to your camp.
        if (store.onboarded) saveProfile()
        if (store.onboarded) lifecycleScope.launch { if (app.tend.data.Friends.syncInbox(this@MainActivity)) { refresh++; app.tend.widget.Widgets.refresh(this@MainActivity) } }
        if (store.onboarded && permissions().usageAccess) {
            MonitorService.start(this)
            store.learnNormal()
        }
    }

    override fun onStop() {
        super.onStop()
        app.tend.data.Stats.flush(this)
    }

    @Composable
    private fun Content(catalog: AppCatalog) {
        refresh // read so recomposition follows onResume
        // Status and navigation bar icons follow the theme in use.
        val dark = app.tend.ui.Brand.dark
        LaunchedEffect(dark) {
            val bars = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        }
        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
        // tend Plus: where it was opened from (for the counts), and where to go back to.
        var plusFrom by rememberSaveable { mutableStateOf(app.tend.ui.PlusFrom.SETTINGS) }
        var plusBack by rememberSaveable { mutableStateOf(Screen.HOME) }
        val openPlus: (app.tend.ui.PlusFrom) -> Unit = { from ->
            plusFrom = from
            plusBack = if (screen == Screen.PLUS) Screen.HOME else screen
            app.tend.data.Stats.log(this, "plus_shown", mapOf("from" to from.name.lowercase(), "trial_days_left" to store.trialDaysLeft))
            screen = Screen.PLUS
        }
        // The trial just ended: Plus once, with a way to go on free.
        LaunchedEffect(store.onboarded, refresh) {
            if (store.onboarded && !store.isPlus && !store.trialEndShown) { store.trialEndShown = true; openPlus(app.tend.ui.PlusFrom.TRIAL_END) }
        }
        var riverFocus by rememberSaveable { mutableStateOf<String?>(null) }
        // A first step tapped: open the camp (the shop), or with the invite open.
        var campInvite by rememberSaveable { mutableStateOf(false) }
        // The animal tapped on home: into the camp, and greet it there.
        var greetOnOpen by rememberSaveable { mutableStateOf(false) }
        var activitiesFocus by remember { mutableIntStateOf(0) }
        var goodNightAcorns by remember { mutableIntStateOf(0) }
        val goToStep: (app.tend.core.Step) -> Unit = { step ->
            when (step) {
                app.tend.core.Step.ACTIVITY -> { screen = Screen.HOME; activitiesFocus++ }
                app.tend.core.Step.PLACE -> screen = Screen.CLEARING
                app.tend.core.Step.USE, app.tend.core.Step.MOVE -> screen = Screen.CLEARING
                app.tend.core.Step.NOT_NOW -> {
                    // Open a paused app: the pause comes up, and "Not now" is right there.
                    val launch = store.blockedPackages.firstNotNullOfOrNull { packageManager.getLaunchIntentForPackage(it) }
                    if (launch != null && permissions().allGranted) startActivity(launch)
                    else Toast.makeText(this, "Open one of your paused apps, then tap \"Not now\".", Toast.LENGTH_LONG).show()
                }
                app.tend.core.Step.WIDGET -> if (!pinWidget()) screen = Screen.SETTINGS
                app.tend.core.Step.FRIEND, app.tend.core.Step.TOGETHER -> { campInvite = step == app.tend.core.Step.FRIEND; screen = Screen.CLEARING }
                app.tend.core.Step.THREE_DAYS, app.tend.core.Step.TEN_ACTIVITIES -> { screen = Screen.HOME; activitiesFocus++ }
                app.tend.core.Step.EARLY_NIGHT -> if (app.tend.core.Budget.isEvening(store.eveningFrom, LocalTime.now().hour)) screen = Screen.RECAP
                    else Toast.makeText(this, "In the evening, open the recap and tap Good night.", Toast.LENGTH_LONG).show()
                else -> screen = Screen.CLEARING
            }
        }
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var health by remember { mutableStateOf<HealthConnect.Status?>(null) }
        var celebrate by remember { mutableStateOf<TaskSync.Done?>(null) }
        // After an activity is done: the companion acts it out in the camp on the home page.
        var reaction by remember { mutableStateOf<String?>(null) }
        var synced by remember { mutableIntStateOf(0) }
        synced // read so recomposition follows a sync

        suspend fun sync() {
            val hc = HealthConnect(this@MainActivity, store.selectedTasks)
            health = hc.status()
            healthBackground = hc.backgroundAvailable()
            val done = TaskSync.sync(this@MainActivity)
            if (done.tasks.isNotEmpty()) celebrate = done
            synced++
            // A newly earned accessory gets a little moment.
            store.takeNewAccessories().firstOrNull()?.let {
                Toast.makeText(this@MainActivity, "${store.avatar.displayName} got a ${it.title.lowercase()}!", Toast.LENGTH_LONG).show()
            }
            shelf = store.accessories()
        }
        LaunchedEffect(refresh) { if (store.onboarded) sync() }
        LaunchedEffect(Unit) {
            var seconds = 0
            while (true) {
                delay(1000)
                now = System.currentTimeMillis()
                // Steps every minute while tend is open; the reading timer the moment it ends.
                if (store.onboarded && (++seconds % 60 == 0 || TaskSync.focusSecondsLeft(store, now) == 0L)) sync()
            }
        }
        // System back always leads home from inner pages.
        androidx.activity.compose.BackHandler(enabled = store.onboarded && screen != Screen.HOME) {
            if (screen == Screen.CLEARING) campInvite = false
            screen = when (screen) { Screen.RIVER -> Screen.CLEARING; Screen.PLUS -> plusBack; else -> Screen.HOME }
        }
        LaunchedEffect(openRecap) { if (openRecap) { screen = Screen.RECAP; openRecap = false } }
        LaunchedEffect(openClearing) { if (openClearing) { screen = Screen.CLEARING; openClearing = false } }
        // From a pause: home, with the activities in view.
        LaunchedEffect(openActivities) { if (openActivities) { screen = Screen.HOME; activitiesFocus++; openActivities = false } }
        celebrate?.let { done ->
            // Only a single hand check-off can be undone; measured tasks are facts.
            val undoable = done.tasks.singleOrNull()?.takeIf { it.manual }
            val together = remember(done) { store.togetherWith.also { store.togetherWith = "" } }
            Celebration(
                store.avatar, done.tasks, done.minutes,
                onUndo = undoable?.let { t -> { TaskSync.undo(store, t); celebrate = null; refresh++ } },
                together = together,
            ) {
                celebrate = null
                val r = app.tend.ui.Uses.reactionFor(done.tasks.first().id)
                reaction = if (r == "campfire" && store.placed[0] == null) "act_cheer" else r
                // With a thing from the camp (the guitar), the sound starts once they're there.
                reaction?.let { r -> app.tend.ui.Sounds.forUse(this@MainActivity, r, if (r != "campfire" && r in store.placed.values) (app.tend.ui.Uses.WALK * 1000).toLong() else 0L) }
            }
        }
        val toggleTask: (TaskDef) -> Unit = { store.toggleTask(it); refresh++ }

        // The town chosen: saved, and its weather fetched at once.
        val onTown: (app.tend.data.WeatherService.Place?) -> Unit = { p ->
            store.setWeatherPlace(p); refresh++
            lifecycleScope.launch {
                if (kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { app.tend.data.WeatherService.refresh(this@MainActivity, force = true) }) {
                    refresh++; app.tend.widget.Widgets.refresh(this@MainActivity)
                }
            }
        }
        // A little about you: the town, an age range, a gender.
        val aboutYou: @Composable () -> Unit = {
            @Suppress("UNUSED_VARIABLE") val changed = refresh // redraw after each choice
            app.tend.ui.AboutYou(
                store.weatherPlace, store.ageBand, store.gender,
                onPlace = onTown,
                onAge = { store.ageBand = it; refresh++ },
                onGender = { store.gender = it; refresh++ },
            )
        }

        // The shared campfire: who's phone-free now (heard every minute while tend is open), and joining them.
        var atFire by remember { mutableStateOf(app.tend.data.Friends.atFire(this)) }
        LaunchedEffect(Unit) {
            while (true) {
                if (app.tend.data.Account.user(this@MainActivity) != null) runCatching { app.tend.data.Friends.list(this@MainActivity) }
                // The town's weather, at most hourly.
                if (kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { app.tend.data.WeatherService.refresh(this@MainActivity) }) {
                    refresh++; app.tend.widget.Widgets.refresh(this@MainActivity)
                }
                val now = app.tend.data.Friends.atFire(this@MainActivity)
                if (now != atFire) { atFire = now; app.tend.widget.Widgets.refresh(this@MainActivity) }
                kotlinx.coroutines.delay(60_000)
            }
        }
        val join: (app.tend.data.Friends.AtFire) -> Unit = { f ->
            // Theirs if you have it, else one not done yet today, else any (done again for the company, not the minutes).
            val timed = store.selectedTasks.filter { it.verification.timed }
            val open = timed.filter { it.id !in store.today().completedTasks }
            val task = open.firstOrNull { it.id == f.task } ?: open.firstOrNull() ?: timed.firstOrNull { it.id == f.task } ?: timed.firstOrNull()
            if (task == null) {
                android.widget.Toast.makeText(this, "Pick a phone-free activity first (like a hobby or reading), then join ${f.name}.", android.widget.Toast.LENGTH_LONG).show()
                screen = Screen.SETTINGS
            } else if (store.focusStartedAt == 0L) {
                app.tend.data.Stats.log(this, "fire_join")
                TaskSync.startTimer(this, task)
                screen = Screen.HOME
                refresh++
            }
        }
        LaunchedEffect(joinFire, atFire) { if (joinFire) { joinFire = false; atFire.firstOrNull()?.let(join) } }
        val fireGuests = atFire.map { (runCatching { app.tend.core.Avatar.valueOf(it.companion) }.getOrDefault(app.tend.core.Avatar.BO)) to it.name }

        val toggleApp: (String) -> Unit = { pkg ->
            val b = store.blockedPackages
            // Strict mode: apps can be added, not taken off.
            if (pkg in b && store.isStrict) strictNo()
            else { store.blockedPackages = if (pkg in b) b - pkg else b + pkg; refresh++ }
        }

        val page = when {
            !store.onboarded -> "setup"
            screen != Screen.HOME -> screen.name
            (TaskSync.focusSecondsLeft(store, now) ?: 0) > 0 -> "focus"
            else -> "HOME"
        }
        // Each page (as a function, so `this` inside stays the activity).
        val pageContent: @Composable (String) -> Unit = { p ->
        when {
            p == "setup" -> {
                val finish: () -> Unit = {
                    store.onboarded = true
                    store.onboardedAt = System.currentTimeMillis()
                    store.tourDone = true // the camp was shown at the end of setup
                    app.tend.data.Stats.log(this, "onboarding_done", mapOf("tasks" to store.selectedTasks.size))
                    if (permissions().usageAccess) MonitorService.start(this)
                    if (!permissions().overlay && Build.VERSION.SDK_INT >= 33 && !RecapReminder.canNotify(this)) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) // for the fallback nudge
                    }
                    saveProfile()
                    screen = Screen.HOME
                    refresh++
                }
                Onboarding(
                    catalog = catalog,
                    avatar = store.avatar,
                    cfg = store.config,
                    blocked = store.blockedPackages,
                    perms = permissionActions(),
                    onAvatar = { store.avatar = it; refresh++ },
                    onConfig = { store.config = it; refresh++ },
                    onToggleApp = toggleApp,
                    signedIn = app.tend.data.Account.user(this) != null,
                    tasks = store.selectedTasks,
                    onToggleTask = toggleTask,
                    onDefaultTasks = { reset -> if (reset || !store.tasksChosen) { store.useDefaultTasks(); refresh++ } },
                    // Logged in: straight into the app (after a restore offer, if there's a backup).
                    account = { done -> app.tend.ui.AccountCard(onChanged = { refresh++ }, onRestored = ::restartFresh, onSignedIn = done) },
                    placed = store.placed,
                    acorns = store.acorns,
                    // The camp starts empty: the first thing (the campfire) is bought, guided, at the end of setup.
                    onBuy = { thing -> store.buy(thing).also { if (it) refresh++ } },
                    onUsed = { thing -> store.markUsed(thing); refresh++ },
                    about = aboutYou,
                    onFinish = finish,
                )
            }

            p == "SETTINGS" -> SettingsScreen(
                catalog = catalog,
                avatar = store.avatar,
                cfg = store.config,
                blocked = store.blockedPackages,
                onAvatar = { store.avatar = it; saveProfile(); refresh++ },
                onAccountChanged = { saveProfile(); refresh++ },
                onRestored = ::restartFresh,
                onConfig = {
                    val now = store.config
                    if (store.isStrict && (it.freeMinutes > now.freeMinutes || it.totalMinutes > now.totalMinutes)) strictNo() else { store.config = it; refresh++ }
                },
                onToggleApp = toggleApp,
                tasks = store.selectedTasks,
                onToggleTask = toggleTask,
                missCount = store.missCount(),
                reminderEnabled = store.reminderEnabled,
                onReminder = ::setReminder,
                eveningFrom = store.eveningFrom,
                onEvening = { store.eveningFrom = it; RecapReminder.schedule(this); app.tend.reminder.DayClock.schedule(this); refresh++ },
                eveningPause = store.eveningPause,
                onEveningPause = { store.eveningPause = it; refresh++ },
                weeklyLetter = store.weeklyLetter,
                onWeeklyLetter = ::setWeeklyLetter,
                onExport = if (BuildConfig.PLAY) null else ::exportTestData,
                onRate = if (BuildConfig.PLAY) ::openPlayListing else null,
                onPrivacy = { startActivity(Intent(this, app.tend.health.PrivacyActivity::class.java)) },
                onFeedback = getString(R.string.support_email).takeIf { it.isNotBlank() }?.let { mail -> { sendFeedback(mail) } },
                onStartOver = ::startOver,
                onAddWidget = ::pinWidget,
                onAddTile = if (Build.VERSION.SDK_INT >= 33) ({
                    getSystemService(android.app.StatusBarManager::class.java).requestAddTileService(
                        android.content.ComponentName(this, app.tend.monitor.PhoneDownTile::class.java), "Phone down",
                        android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_t_hourglass), mainExecutor,
                    ) { }
                }) else null,
                version = BuildConfig.VERSION_NAME,
                shareStats = app.tend.data.Stats.consent(this) == true,
                onShareStats = { app.tend.data.Stats.setConsent(this, it); refresh++ },
                notesMorning = store.notesMorning,
                notesNudges = store.notesNudges,
                onNotes = ::setNotes,
                theme = app.tend.ui.ThemeChoice.mode ?: store.themeMode,
                onTheme = { store.themeMode = it; app.tend.ui.ThemeChoice.mode = it },
                lockSchedules = store.lockSchedules,
                onLockSchedule = { new ->
                    val was = store.lockSchedules.firstOrNull { it.label == new.label }
                    // Strict mode: a lock can be turned on, not off or shortened.
                    if (store.isStrict && was?.on == true) strictNo() else { store.setLockSchedule(new); refresh++ }
                },
                pausedUntil = store.pausedUntil,
                onPause = { if (it == 0) store.resume() else if (store.isStrict) strictNo() else store.pause(it); refresh++ },
                appLimits = store.appLimits,
                appSecondsToday = store.appSecondsToday(),
                onAppLimit = { pkg, m -> if (store.setAppLimit(pkg, m)) refresh++ else strictNo() },
                strict = store.isStrict,
                strictEndsAt = store.strictEndsAt,
                onStrict = { store.setStrict(it); refresh++ },
                plus = store.isPlus,
                trialDaysLeft = store.trialDaysLeft.takeIf { store.plusTrialRunning },
                plusPaid = store.plusPaid,
                onPlus = openPlus,
                onTestTrial = if (BuildConfig.PLAY) null else { days -> store.setTrialStart(days); refresh++ },
                peekOn = store.peekOn,
                aboutYou = aboutYou,
                townName = store.weatherPlace?.name,
                onPeek = { store.peekOn = it; refresh++ },
                onDone = { screen = Screen.HOME },
            )

            p == "PLUS" -> app.tend.ui.PlusScreen(
                store.avatar,
                trialDaysLeft = store.trialDaysLeft.takeIf { store.plusTrialRunning },
                paid = store.plusPaid,
                wonBack = remember(refresh) {
                    app.tend.core.InsightsOf.of(store.daySummaries(), store.normalMinutes, 0, LocalDate.now()).wonBack.takeIf { store.normalMinutes != null }
                },
                included = !TendStore.PLUS_ON_SALE,
                canBuy = false, // Google Play billing: next
                onBuy = {},
                onClose = {
                    if (!store.isPlus) app.tend.data.Stats.log(this, "plus_free", mapOf("from" to plusFrom.name.lowercase()))
                    screen = plusBack
                },
            )

            p == "INSIGHTS" -> {
                val insights = remember(refresh) {
                    app.tend.core.InsightsOf.of(
                        store.daySummaries(), store.normalMinutes,
                        store.answers().count { it.kind == "pause" && it.answer == "not_now" }, LocalDate.now(),
                    )
                }
                val apps = remember(refresh) { store.appMinutes().map { (pkg, before, now) -> app.tend.ui.AppTime(catalog.label(pkg), catalog.icon(pkg), before, now) } }
                app.tend.ui.InsightsScreen(insights, apps, plus = store.isPlus, onPlus = { openPlus(app.tend.ui.PlusFrom.INSIGHTS) }) { screen = Screen.HOME }
            }

            p == "SLEEP" -> app.tend.ui.SleepScreen(store.avatar, store.placed, shelf.first, goodNightAcorns, campLayout()) { screen = Screen.HOME }

            p == "RECAP" -> {
                LaunchedEffect(Unit) { store.recapSeenOn = LocalDate.now().toString() }
                RecapScreen(
                    avatar = store.avatar,
                    recap = Recap.of(store.config, store.today(), store.worthItToday(), store.normalMinutes),
                    placed = store.placed,
                    worn = shelf.first,
                    reminderHour = store.eveningFrom.takeIf { !store.reminderPrompted },
                    onReminder = ::setReminder,
                    onGoodNight = { goodNightAcorns = store.goodNight(); screen = Screen.SLEEP; maybeAskReview() },
                    onShare = ::shareCamp,
                )
            }

            p == "PERMISSIONS" -> PermissionsScreen(store.avatar, permissionActions()) {
                MonitorService.start(this)
                screen = Screen.HOME
            }

            p == "CLEARING" -> app.tend.ui.ClearingScreen(
                avatar = store.avatar,
                placed = store.placed,
                acorns = store.acorns,
                earnedToday = store.acornsToday(),
                lastNight = store.acornsLastNight(),
                worn = shelf.first, onShelf = shelf.second,
                night = app.tend.core.Budget.isEvening(store.eveningFrom, LocalTime.now().hour),
                onBuy = { thing -> store.buy(thing).also { if (it) { app.tend.data.Stats.log(this, "bought", mapOf("thing" to thing)); saveProfile(); refresh++ } } },
                onSell = { thing -> store.sell(thing); saveProfile(); refresh++ },
                layout = campLayout(),
                onPlace = { spot, at -> if (store.place(spot, at.x, at.y)) { saveProfile(); refresh++ } },
                plus = store.isPlus,
                onPlus = { openPlus(app.tend.ui.PlusFrom.SEASON) },
                guests = fireGuests,
                minutes = store.today().let { d -> Triple(app.tend.core.Budget.remainingMinutes(store.config, d), (store.config.freeMinutes + d.earnedMinutes).coerceAtLeast(1), app.tend.core.Budget.earnableFrom(store.config, d, store.selectedTasks)) },
                cheers = remember(refresh) { app.tend.data.Friends.cheersToShow(this@MainActivity) },
                onCheersShown = { app.tend.data.Friends.markCheersShown(this@MainActivity) },
                outfits = store.outfits,
                outfitWorn = store.outfitWorn,
                onBuyOutfit = { a -> store.buyOutfit(a).also { if (it) { saveProfile(); refresh++ } } },
                onWearOutfit = { a -> store.wearOutfit(a); shelf = store.accessories(); refresh++ },
                earned = remember(refresh, shelf) { store.accessoriesEarned },
                onToggleAccessory = { a -> store.toggleAccessory(a); shelf = store.accessories(); refresh++ },
                onOpenSettings = { screen = Screen.SETTINGS },
                onOpenRiver = { id -> riverFocus = id; screen = Screen.RIVER },
                paper = app.tend.data.Friends.paperOnMe(this).isNotEmpty(),
                bridge = store.bridgeBuilt,
                steps = remember(refresh) { firstSteps() },
                onClaimStep = { store.claimStep(it); refresh++ },
                onUsed = { thing -> if (thing !in store.usedThings) { store.markUsed(thing); refresh++ } },
                rest = store.restSpot,
                onRest = { store.restSpot = it; refresh++ },
                visitor = remember(refresh, LocalTime.now().hour) { store.visitorWaiting() },
                guestbook = remember(refresh) { store.guestbook },
                onGreet = { store.greetVisitor().also { refresh++ } },
                greetOnOpen = greetOnOpen,
                onGreetedOnOpen = { greetOnOpen = false },
                onFriendsChanged = { saveProfile(); refresh++ },
                ducks = app.tend.data.Friends.ducks(this).take(3),
                onUnroll = { lifecycleScope.launch { app.tend.data.Friends.clearPaper(this@MainActivity) }; refresh++ },
                onCatchDuck = { n -> app.tend.data.Friends.catchDuck(this, n); refresh++; store.ducksCaught },
                onBack = { campInvite = false; screen = Screen.HOME },
                onShare = ::shareCamp,
                // No town yet (set up before it was asked): once, a card for the real weather.
                askTown = store.weatherPlace == null && !store.townAsked,
                onTown = onTown,
                onTownLater = { store.townAsked = true; refresh++ },
                gifts = remember(refresh) { app.tend.data.Friends.giftsToOpen(this@MainActivity) },
                totals = remember(refresh) { store.totals() },
                milestonesSeen = store.milestonesSeen,
                onMilestonesSeen = { store.milestonesSeen = store.milestonesSeen + it; refresh++ },
                onOpenGifts = {
                    val n = app.tend.data.Friends.giftsToOpen(this@MainActivity).size
                    if (n > 0) store.addAcorns(n * app.tend.data.Friends.GIFT_ACORNS, "gift")
                    app.tend.data.Friends.markGiftsOpened(this@MainActivity); refresh++
                },
                mood = app.tend.core.Moods.atNight(app.tend.core.Moods.of(store.config, store.today()), app.tend.core.Budget.isEvening(store.eveningFrom, LocalTime.now().hour)),
                openInvite = campInvite,
                onStep = goToStep,
            )

            p == "RIVER" -> app.tend.ui.RiverScreen(
                me = app.tend.data.Friends.me(this),
                worn = shelf.first,
                night = app.tend.core.Budget.isEvening(store.eveningFrom, LocalTime.now().hour),
                focus = riverFocus,
                onBack = { screen = Screen.CLEARING },
                myPaper = app.tend.data.Friends.paperOnMe(this).isNotEmpty(),
                myDucks = app.tend.data.Friends.ducks(this).size.coerceAtMost(3),
            )

            p == "focus" -> FocusTimerScreen(
                // Still drawn for a moment while the page fades out after the timer stops: 0 then, never a crash.
                store.avatar, store.focusTask, TaskSync.focusSecondsLeft(store, now) ?: 0L,
                screenOnSeconds = remember(now / 5000) { TaskSync.screenOnSeconds(this@MainActivity) },
                onCancel = { TaskSync.stopTimer(this@MainActivity); refresh++ },
            )

            else -> {
              // Levels and the weekly challenge: read from the history once per change, not every second.
              val done = store.today().completedTasks
              val progress = remember(refresh, done) { homeProgress() }
              HomeScreen(
                avatar = store.avatar,
                cfg = store.config,
                day = store.today(),
                tasks = store.selectedTasks,
                acorns = store.acorns,
                isEvening = app.tend.core.Budget.isEvening(store.eveningFrom, LocalTime.now().hour),
                permissions = permissions(),
                // Test builds only: Google Play collects crashes by itself.
                onShareCrash = TendApp.crashFile(this).takeIf { !BuildConfig.PLAY && it.exists() }?.let { file ->
                    {
                        val report = file.readText()
                        file.delete()
                        startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_SUBJECT, "tend crash report").putExtra(Intent.EXTRA_TEXT, report),
                                "Share crash report",
                            ),
                        )
                        refresh++
                    }
                },
                health = health,
                focusSecondsLeft = TaskSync.focusSecondsLeft(store, now),
                focusTaskId = store.focusTaskId,
                onFixPermissions = { screen = Screen.PERMISSIONS },
                onConnectHealth = {
                    if (health == HealthConnect.Status.NEEDS_INSTALL) {
                        open(
                            HealthConnect.installIntent(),
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${HealthConnect.PROVIDER}")),
                        )
                    } else {
                        healthPermissions.launch(
                            HealthConnect.permissionsFor(store.selectedTasks) + if (healthBackground) setOf(HealthConnect.BACKGROUND) else emptySet(),
                        )
                    }
                },
                onStartFocus = { task ->
                    TaskSync.startTimer(this@MainActivity, task)
                    // The moment notes make sense: "Bo will tell you when it's done". Asked once.
                    // Asked again at later timers too: the time left counts down in a notification.
                    if (!RecapReminder.canNotify(this@MainActivity)) {
                        if (!store.reminderPrompted) allowNotes() else if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    refresh++
                },
                onCheckOff = { task ->
                    val done = TaskSync.checkOff(store, task)
                    if (done.tasks.isNotEmpty()) celebrate = done
                    refresh++
                },
                onUndo = { TaskSync.undo(store, it); refresh++ },
                onOpenSettings = { screen = Screen.SETTINGS },
                onOpenClearing = { screen = Screen.CLEARING },
                visitor = remember(refresh, LocalTime.now().hour) { store.visitorWaiting() },
                onGreetVisitor = { greetOnOpen = true; screen = Screen.CLEARING },
                steps = remember(refresh) { firstSteps() },
                onStep = goToStep,
                // Bo's quest for today (once something's picked to do instead).
                quest = remember(refresh, LocalDate.now()) { app.tend.core.Quests.of(LocalDate.now(), store.selectedTasks, store.config.freeMinutes).takeIf { store.selectedTasks.isNotEmpty() } },
                questDone = remember(refresh, LocalTime.now().hour) {
                    app.tend.core.Quests.done(app.tend.core.Quests.of(LocalDate.now(), store.selectedTasks, store.config.freeMinutes), store.today(), store.notNowsToday, LocalTime.now().hour)
                },
                questClaimed = store.questClaimedOn == LocalDate.now().toString(),
                // The week's story: from Sunday 17:00 through Monday, until shared or put away.
                weekStory = remember(refresh, LocalTime.now().hour) {
                    val today = LocalDate.now()
                    val sunday = when (today.dayOfWeek) {
                        java.time.DayOfWeek.SUNDAY -> today.takeIf { LocalTime.now().hour >= 17 }
                        java.time.DayOfWeek.MONDAY -> today.minusDays(1)
                        else -> null
                    }
                    sunday?.takeIf { store.weekStoryDone != it.toString() && store.daysTended >= 3 }?.let { sun ->
                        val week = Patterns.week(sun, store.daySummaries(), store.answers())
                        Triple(week.gotBackMinutes, week.days.count { it.tasksDone > 0 }, week.letGo)
                    }
                },
                onShareWeek = { shareCamp() },
                onWeekStoryDone = {
                    val today = LocalDate.now()
                    store.weekStoryDone = (if (today.dayOfWeek == java.time.DayOfWeek.MONDAY) today.minusDays(1) else today).toString(); refresh++
                },
                onClaimQuest = {
                    if (store.questClaimedOn != LocalDate.now().toString()) {
                        store.questClaimedOn = LocalDate.now().toString()
                        store.addAcorns(app.tend.core.Quests.of(LocalDate.now(), store.selectedTasks, store.config.freeMinutes).acorns, "quest")
                        app.tend.ui.Sounds.acorns(this@MainActivity); refresh++
                    }
                },
                focusActivities = activitiesFocus,
                sleeping = now < store.sleepUntil,
                onWakeUp = { store.wakeUp(); refresh++ },
                // Until something has been moved: how (on the camp, small).
                campTip = remember(refresh) {
                    // Only what still applies: nothing moved yet, no widget yet, no town yet.
                    store.tipToday(buildList {
                        if (store.layout.isEmpty() && store.placed.values.any { id -> app.tend.core.Things.byId(id)?.let(app.tend.core.Clearing::movable) == true }) add(app.tend.core.Tip.MOVE)
                        if (widgetPlaced() == false) add(app.tend.core.Tip.WIDGET)
                        if (store.weatherPlace == null) add(app.tend.core.Tip.TOWN)
                    })
                },
                onCampTip = { tip ->
                    when (tip) {
                        app.tend.core.Tip.MOVE -> screen = Screen.CLEARING
                        app.tend.core.Tip.WIDGET -> if (!pinWidget()) screen = Screen.SETTINGS
                        app.tend.core.Tip.TOWN -> screen = Screen.SETTINGS
                    }
                },
                camp = {
                    val night = app.tend.core.Budget.isEvening(store.eveningFrom, LocalTime.now().hour)
                    val mood = app.tend.core.Moods.atNight(app.tend.core.Moods.of(store.config, store.today()), night)
                    val paper = remember(refresh) { app.tend.data.Friends.paperOnMe(this@MainActivity).isNotEmpty() }
                    val ducks = remember(refresh) { app.tend.data.Friends.ducks(this@MainActivity).size.coerceAtMost(3) }
                    val visitor = remember(refresh, LocalTime.now().hour) { store.visitorWaiting() }
                    // Where the companion stays (shared with the camp page through the store).
                    var restSpot by remember(refresh) { mutableStateOf(store.restSpot) }
                    // The reaction's clock: frame by frame while it plays, then gone.
                    var start by remember { mutableLongStateOf(-1L) }
                    var frame by remember { mutableLongStateOf(0L) }
                    // At night, asleep in the shelter: out with a yawn first, and back to bed after.
                    var yawn by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                    var backToBed by remember { mutableIntStateOf(0) }
                    LaunchedEffect(reaction) {
                        val r = reaction ?: return@LaunchedEffect
                        start = -1L
                        yawn = if (night && app.tend.ui.bedOf(store.placed)?.let { it == store.restSpot } == true) app.tend.ui.Uses.YAWN else 0f
                        while (true) {
                            androidx.compose.runtime.withFrameNanos { frame = it; if (start < 0) start = it }
                            // Acted out where they usually stand; they stay there.
                            if ((frame - start) / 1e9f > yawn + app.tend.ui.Uses.WALK + app.tend.ui.Uses.seconds(r)) {
                                reaction = null
                                if (night) backToBed++
                                // Used a thing of the camp: they stay by it. Acted out where they stood: they stay there.
                                val at = if (r == "campfire") 0 else store.placed.entries.firstOrNull { it.value == r }?.key
                                if (at != null) { store.restSpot = at; restSpot = at }
                                // At night, acted out at the shelter's door: not in bed yet, so they walk back in.
                                else if (night && yawn > 0f) { store.restSpot = null; restSpot = null }
                                break
                            }
                        }
                    }
                    val nightClock = app.tend.ui.rememberNightClock(night, backToBed, asleep = restSpot != null && restSpot == app.tend.ui.bedOf(store.placed))
                    // Read once per change, not on every frame of the camp's life.
                    val placed = remember(refresh) { store.placed }
                    val layout = remember(refresh) { campLayout() }
                    val appear = app.tend.ui.rememberAppear(placed)
                    LaunchedEffect(nightClock > app.tend.ui.Uses.BED_WALK + 2f) {
                        val bed = app.tend.ui.bedOf(store.placed)
                        if (night && nightClock > app.tend.ui.Uses.BED_WALK + 2f && bed != null && store.restSpot != bed) { store.restSpot = bed; restSpot = bed }
                    }
                    val ambient = app.tend.ui.rememberAmbientClock()
                    val unlocked = remember(refresh) { app.tend.core.Milestone.reached(store.totals()) }
                    val idle = app.tend.ui.rememberIdleLife(placed, restSpot, enabled = reaction == null && !night, layout = layout) { store.restSpot = it; restSpot = it }
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize().then(app.tend.ui.sharedCamp() ?: Modifier)) {
                        // An activity done: acted out where they stand, or with the thing from the camp where it is (over to it first).
                        val use = reaction?.takeIf { start >= 0 }?.let { r ->
                            val at = placed.entries.firstOrNull { it.value == r && r != "campfire" }?.key
                            app.tend.ui.InUse(at ?: -1, r, (frame - start) / 1e9f, yawn + if (r == "campfire" || at != null) app.tend.ui.Uses.WALK else 0f, reward = true)
                        } ?: idle.value
                        drawClearing(store.avatar, placed, mood, shelf.first, night, paper = if (paper) 1f else 0f, ducks = ducks, bridge = store.bridgeBuilt, use = use, visitor = visitor, rest = restSpot, nightClock = nightClock, appear = appear, layout = layout, ambient = ambient.value, guests = fireGuests, unlocked = unlocked)
                    }
                },
                onOpenRecap = { screen = Screen.RECAP },
                // Right after the tour; after a "Not now", the important ones once more a week later.
                showNextSteps = store.tourDone && (!store.widgetTipDone || store.nextStepsAgainAt in 1..now),
                // Coming back a week after "Not now": only what keeps tend working (battery, Health Connect).
                nextStepsImportantOnly = store.widgetTipDone,
                widgetPlaced = widgetPlaced(),
                onNextStepsDone = {
                    // The first "Not now" lets the important steps come back once, a week later.
                    store.nextStepsAgainAt = if (store.widgetTipDone) Long.MAX_VALUE else System.currentTimeMillis() + 7 * DAY_MS
                    store.widgetTipDone = true
                    refresh++
                },
                welcomeBack = store.welcomeBackOn == LocalDate.now().toString(),
                batteryRisk = !getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
                monitorSlept = store.monitorSlept,
                onKeepAwake = ::keepAwake,
                // One "calm evenings" step: the recap and notes, sleepy pauses, the Sunday letter.
                notesOff = !store.eveningPause || !store.weeklyLetter ||
                    (!store.reminderPrompted && !(store.reminderEnabled && RecapReminder.canNotify(this@MainActivity))),
                onAllowNotes = ::allowNotes,
                onAddWidget = ::pinWidget,
                exerciseTracked = store.exerciseTracked,
                sleepEstimated = store.sleepEstimatedOn == LocalDate.now().toString(),
                brokenTimer = Tasks.presets.firstOrNull { it.id == store.timerBroken },
                onDismissBroken = { store.timerBroken = ""; refresh++ },
                level = progress.level,
                skills = progress.skills,
                challenge = progress.challenge,
                history = progress.days,
                pausedUntil = store.pausedUntil,
                onResume = { store.resume(); refresh++ },
                onOpenInsights = { screen = Screen.INSIGHTS },
                atFire = atFire,
                onJoin = join,
                voice = remember(refresh, atFire, done, java.time.LocalTime.now().hour) {
                    val now = java.time.LocalDateTime.now()
                    val days = store.daySummaries()
                    app.tend.core.Voice.Moment(
                        store.avatar.displayName, now, store.config, store.today(), store.selectedTasks,
                        yesterday = days.firstOrNull { it.date == now.toLocalDate().minusDays(1) }?.done.orEmpty(),
                        streak = app.tend.core.Voice.streak(days, now.toLocalDate()),
                        weather = app.tend.core.LiveWeather.fresh() ?: app.tend.core.Weather.of(now.toLocalDate(), now.hour),
                        visitor = store.visitorWaiting(),
                        atFire = atFire.map { it.name },
                        welcomeBack = store.welcomeBackOn == LocalDate.now().toString(),
                        canClose = permissions().overlay,
                    )
                },
                strict = store.isStrict,
                plusDaysLeft = store.trialDaysLeft.takeIf { store.plusTrialRunning },
                onPlus = { openPlus(app.tend.ui.PlusFrom.SETTINGS) },
                askStats = app.tend.data.Stats.consent(this@MainActivity) == null && store.daysTended >= 1,
                onStats = { app.tend.data.Stats.setConsent(this@MainActivity, it); refresh++ },
                levelUp = progress.level.level.takeIf { it > store.levelSeen && store.tourDone },
                onLevelUpSeen = {
                    store.addAcorns(app.tend.core.Acorns.LEVEL_UP * (progress.level.level - store.levelSeen).coerceAtLeast(0), "level")
                    store.levelSeen = progress.level.level
                    refresh++
                },
            )
            }
        }
        }
        // The camp scene glides and grows from home into the camp page (and back); the rest fades.
        androidx.compose.animation.SharedTransitionLayout {
            androidx.compose.animation.AnimatedContent(
                page, label = "page",
                transitionSpec = {
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(320, delayMillis = 60)) togetherWith
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220))
                },
            ) { p ->
                androidx.compose.runtime.CompositionLocalProvider(app.tend.ui.LocalCampShare provides app.tend.ui.CampShare(this@SharedTransitionLayout, this@AnimatedContent)) { pageContent(p) }
            }
        }
    }

    private fun strictNo() =
        android.widget.Toast.makeText(this, "Strict mode is on: things can only get tighter until it ends.", android.widget.Toast.LENGTH_LONG).show()

    /** Where the user put things in the camp, for drawing it. */
    private fun campLayout() = store.layout.mapValues { androidx.compose.ui.geometry.Offset(it.value.first, it.value.second) }

    /** Name, companion and level on the server, when signed in (friends will see them). */
    private fun saveProfile() {
        if (app.tend.data.Account.user(this) == null) return
        lifecycleScope.launch { app.tend.data.Account.saveProfile(this@MainActivity) }
    }

    private class HomeProgress(val level: app.tend.core.Level, val skills: Map<app.tend.core.Skill, app.tend.core.Level>, val challenge: app.tend.core.Challenge, val days: List<app.tend.core.DaySummary>)

    /** XP from all activities, the week's challenge (banked once when finished), and the levels. */
    /** The first steps: what's done (from what's in the store and on the home screen) and what's claimed. */
    private fun firstSteps(): List<app.tend.core.StepState> {
        val claimed = store.stepsClaimed
        if (claimed.size == app.tend.core.Step.entries.size) return app.tend.core.FirstSteps.of(emptySet(), claimed)
        val days = store.daySummaries()
        val placed = store.placed
        val kinds = placed.values.mapNotNull { app.tend.core.Things.byId(it)?.kind }.toSet()
        val book = store.guestbook
        val widgets = android.appwidget.AppWidgetManager.getInstance(this).let { m ->
            listOf(app.tend.widget.TendWidget::class.java, app.tend.widget.TendWidgetFace::class.java, app.tend.widget.TendWidgetGarden::class.java)
                .sumOf { m.getAppWidgetIds(android.content.ComponentName(this, it)).size }
        }
        val done = buildSet {
            if (store.placed.isNotEmpty()) add(app.tend.core.Step.PLACE)
            if (store.usedThings.isNotEmpty()) add(app.tend.core.Step.USE)
            if (store.layout.isNotEmpty()) add(app.tend.core.Step.MOVE)
            if (days.any { it.tasksDone > 0 }) add(app.tend.core.Step.ACTIVITY)
            if (book.isNotEmpty()) add(app.tend.core.Step.GREET)
            if (store.usedThings.size >= 3) add(app.tend.core.Step.THREE_THINGS)
            if (days.count { it.tasksDone > 0 } >= 3) add(app.tend.core.Step.THREE_DAYS)
            if (app.tend.core.SpotKind.TREES in kinds) add(app.tend.core.Step.TREES)
            if (placed[0] != null && placed[0] != "campfire") add(app.tend.core.Step.BETTER_FIRE)
            if (store.hadEarlyNight) add(app.tend.core.Step.EARLY_NIGHT)
            if (book.size >= 5) add(app.tend.core.Step.FIVE_ANIMALS)
            if (days.sumOf { it.tasksDone } >= 10) add(app.tend.core.Step.TEN_ACTIVITIES)
            if (app.tend.core.SpotKind.SHELTER in kinds) add(app.tend.core.Step.SHELTER)
            if (app.tend.core.SpotKind.WATER in kinds) add(app.tend.core.Step.ON_THE_RIVER)
            if (store.togetherDone.isNotEmpty()) add(app.tend.core.Step.TOGETHER)
            if (book.keys.any { it.rare }) add(app.tend.core.Step.RARE)
            if (store.answers().any { it.kind == "pause" && it.answer == "not_now" }) add(app.tend.core.Step.NOT_NOW)
            if (widgets > 0) add(app.tend.core.Step.WIDGET)
            if (store.invited) add(app.tend.core.Step.FRIEND)
        }
        return app.tend.core.FirstSteps.of(done, claimed)
    }

    private fun homeProgress(): HomeProgress {
        val days = store.daySummaries()
        val challenge = app.tend.core.Progress.challenge(LocalDate.now(), store.selectedTasks.map { it.id }, days)
        if (challenge.done && challenge.weekKey !in store.challengesDone) {
            store.challengesDone = store.challengesDone + challenge.weekKey
            store.addAcorns(app.tend.core.Acorns.CHALLENGE, "challenge")
            app.tend.data.Stats.log(this, "challenge_done", mapOf("target" to challenge.target))
        }
        val skillXp = app.tend.core.Progress.skillXp(days)
        val total = skillXp.values.sum() + store.challengesDone.size * app.tend.core.Progress.XP_PER_CHALLENGE
        return HomeProgress(
            app.tend.core.Progress.level(total),
            skillXp.mapValues { app.tend.core.Progress.level(it.value) },
            challenge,
            days,
        )
    }

    private fun permissionActions(): PermissionActions {
        val p = permissions()
        return PermissionActions(
            permissions = p,
            usageBlocked = usageAttempted && !p.usageAccess,
            overlayBlocked = overlayAttempted && !p.overlay,
            requestUsage = ::requestUsage,
            requestOverlay = ::requestOverlay,
            openAppInfo = ::openUnlock,
            lock = lock(),
            startUnlock = ::tripLock,
            unlockAppInfo = ::openUnlock,
            skipUnlock = { store.restrictedUnlocked = true; refresh++ },
        )
    }

    private fun requestUsage() {
        usageAttempted = true
        launched = Launched.USAGE
        returnWhenGranted { permissions().usageAccess }
        hint("Switch on usage access for tend")
        // Straight to tend's own switch where the phone supports it, else the list scrolled to tend.
        open(highlightTend(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName"))), highlightTend(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
    }

    private fun requestOverlay() {
        overlayAttempted = true
        launched = Launched.OVERLAY
        returnWhenGranted { Settings.canDrawOverlays(this) }
        hint("Find tend in the list and switch it on")
        open(highlightTend(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))), highlightTend(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)))
    }

    /**
     * Android only offers "Allow restricted settings" after it has refused a switch once, so the
     * unlock starts by letting it refuse: tend's own overlay switch, one tap, "OK".
     */
    private fun tripLock() {
        tripped = true
        launched = Launched.TRIP
        // Back as soon as Android has said no (or yes, on phones without the lock), where Android lets us.
        returnWhenGranted("Done! Press back to return to tend") {
            restrictedMode() == AppOpsManager.MODE_IGNORED || permissions().let { it.overlay || it.usageAccess }
        }
        hint("Tap tend's switch. Android will say it's restricted: tap OK")
        // tend's own usage-access switch where the phone supports it, else the list scrolled to tend.
        open(
            highlightTend(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName"))),
            highlightTend(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)),
        )
    }

    private fun openUnlock() {
        launched = Launched.APP_INFO
        returnWhenGranted("Unlocked! Press back to return to tend") { restrictedMode() == AppOpsManager.MODE_ALLOWED }
        hint("Tap ⋮ in the top right, then \"Allow restricted settings\"")
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    /**
     * Android 13+ "restricted settings": side-loaded apps can't get these switches until the user
     * allows it in App info, and that option only appears after Android has refused once. Read from
     * the app op itself where the phone allows it, so setup knows exactly which step is next.
     */
    private fun lock(): PermissionLock {
        val p = permissions()
        if (p.overlay || p.usageAccess) return PermissionLock.NONE // a switch worked, so nothing is locked
        return when (restrictedMode()) {
            AppOpsManager.MODE_ERRORED -> PermissionLock.LOCKED
            AppOpsManager.MODE_IGNORED -> PermissionLock.TRIPPED
            null -> when {
                store.restrictedUnlocked || !installedOutsideStore() -> PermissionLock.NONE
                tripped -> PermissionLock.TRIPPED
                else -> PermissionLock.LOCKED
            }
            else -> PermissionLock.NONE
        }
    }

    /** The restricted-settings op for tend, or null when this phone doesn't have it or won't say. */
    private fun restrictedMode(): Int? {
        if (Build.VERSION.SDK_INT < 33) return AppOpsManager.MODE_ALLOWED
        return runCatching {
            getSystemService(AppOpsManager::class.java).unsafeCheckOpRawNoThrow("android:access_restricted_settings", Process.myUid(), packageName)
        }.getOrNull()
    }

    /**
     * While the user is on a system settings page, watch for the switch and bring tend back
     * as soon as it flips, so they don't have to find their way back.
     */
    private fun returnWhenGranted(doneHint: String? = null, granted: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 120_000
        handler.removeCallbacksAndMessages(null)
        handler.post(object : Runnable {
            override fun run() = when {
                granted() -> {
                    // Before the overlay is on, Android may not let tend come forward by itself:
                    // then a short note on top of Settings says the step is done and how to get back.
                    doneHint?.let { hint(it) }
                    startActivity(
                        Intent(this@MainActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    )
                }
                SystemClock.uptimeMillis() < deadline -> { handler.postDelayed(this, 500); Unit }
                else -> Unit
            }
        })
    }

    /** Asks Settings to scroll to and flash tend's row, where the phone supports it. */
    private fun highlightTend(intent: Intent) = intent
        .putExtra(":settings:fragment_args_key", packageName)
        .putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", packageName) })

    /** A short note that stays visible on top of the Settings page. */
    private fun hint(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    /**
     * Android 13+ locks some switches for side-loaded apps until the user allows "restricted
     * settings" in App info. Anything not installed by an app store counts as side-loaded.
     */
    private fun installedOutsideStore(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        val source = runCatching { packageManager.getInstallSourceInfo(packageName).packageSource }.getOrNull()
        return source != android.content.pm.PackageInstaller.PACKAGE_SOURCE_STORE
    }

    private fun open(vararg intents: Intent) {
        for (intent in intents) {
            try { startActivity(intent); return } catch (_: ActivityNotFoundException) { }
        }
    }

    /** Null when the launcher can't pin widgets from inside the app. */
    private fun widgetPlaced() = app.tend.ui.WidgetKind.anyPlaced(this)

    /**
     * Asks the launcher to place a widget (its own dialog): the 1x1 Bo, which can stand in for the
     * app icon, else the 2x1. False when neither can be placed from the app.
     */
    private fun pinWidget(): Boolean {
        val manager = android.appwidget.AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) return false
        // The 1x1 Bo first: it can take the app icon's place. Where the launcher won't, the 2x1.
        return listOf(app.tend.ui.WidgetKind.FACE, app.tend.ui.WidgetKind.WIDE).any { kind ->
            runCatching { manager.requestPinAppWidget(android.content.ComponentName(this, kind.provider), null, null) }.getOrDefault(false)
        }
    }

    private fun permissions(): Permissions {
        val ops = getSystemService(AppOpsManager::class.java)
        val usage = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } == AppOpsManager.MODE_ALLOWED
        return Permissions(usage, Settings.canDrawOverlays(this))
    }

    private companion object {
        const val DAY_MS = 24 * 3_600_000L
    }
}
