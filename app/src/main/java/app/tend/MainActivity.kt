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
import app.tend.ui.WeekSections
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

    private enum class Screen { HOME, SETTINGS, PERMISSIONS, CLEARING, RECAP, SLEEP }

    /** Set when the evening notification was tapped. */
    private var openRecap by mutableStateOf(false)
    private var openClearing by mutableStateOf(false)

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (store.reminderPrompted) store.reminderEnabled = granted // asked for the evening reminder, not for nudges
        RecapReminder.schedule(this)
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
        val catalog = AppCatalog(this)
        catalog.preload { c ->
            if (!store.onboarded && store.blockedPackages.isEmpty()) {
                store.blockedPackages = c.suggested.map { it.pkg }.filter { it in AppCatalog.SOCIAL }.toSet()
                runOnUiThread { refresh++ } // the app picker must show the pre-selection, not a stale empty set
            }
        }
        RecapReminder.schedule(this)
        app.tend.reminder.WeeklyLetter.schedule(this)
        app.tend.reminder.MorningNote.schedule(this)
        app.tend.reminder.DayClock.schedule(this)
        handleIntent(intent)
        setContent {
            TendTheme {
                androidx.compose.runtime.CompositionLocalProvider(app.tend.ui.LocalAccessories provides shelf.first) {
                    Box(Modifier.fillMaxSize().safeDrawingPadding()) { Content(catalog) }
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

    override fun onResume() {
        super.onResume()
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
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var health by remember { mutableStateOf<HealthConnect.Status?>(null) }
        var celebrate by remember { mutableStateOf<TaskSync.Done?>(null) }
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
        androidx.activity.compose.BackHandler(enabled = store.onboarded && screen != Screen.HOME) { screen = Screen.HOME }
        LaunchedEffect(openRecap) { if (openRecap) { screen = Screen.RECAP; openRecap = false } }
        LaunchedEffect(openClearing) { if (openClearing) { screen = Screen.CLEARING; openClearing = false } }
        celebrate?.let { done ->
            // Only a single hand check-off can be undone; measured tasks are facts.
            val undoable = done.tasks.singleOrNull()?.takeIf { it.manual }
            Celebration(
                store.avatar, done.tasks, done.minutes,
                onUndo = undoable?.let { t -> { TaskSync.undo(store, t); celebrate = null; refresh++ } },
            ) { celebrate = null }
        }
        val toggleTask: (TaskDef) -> Unit = { store.toggleTask(it); refresh++ }

        val toggleApp: (String) -> Unit = { pkg ->
            val b = store.blockedPackages
            store.blockedPackages = if (pkg in b) b - pkg else b + pkg
            refresh++
        }

        when {
            !store.onboarded -> Onboarding(
                catalog = catalog,
                avatar = store.avatar,
                cfg = store.config,
                blocked = store.blockedPackages,
                perms = permissionActions(),
                onAvatar = { store.avatar = it; refresh++ },
                onConfig = { store.config = it; refresh++ },
                onToggleApp = toggleApp,
                tasks = store.selectedTasks,
                onToggleTask = toggleTask,
                onFinish = {
                    store.onboarded = true
                    store.onboardedAt = System.currentTimeMillis()
                    app.tend.data.Stats.log(this, "onboarding_done", mapOf("tasks" to store.selectedTasks.size))
                    if (permissions().usageAccess) MonitorService.start(this)
                    if (!permissions().overlay && Build.VERSION.SDK_INT >= 33 && !RecapReminder.canNotify(this)) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) // for the fallback nudge
                    }
                    refresh++
                },
            )

            screen == Screen.SETTINGS -> SettingsScreen(
                catalog = catalog,
                avatar = store.avatar,
                cfg = store.config,
                blocked = store.blockedPackages,
                onAvatar = { store.avatar = it; saveProfile(); refresh++ },
                onAccountChanged = { saveProfile(); refresh++ },
                onRestored = ::restartFresh,
                onConfig = { store.config = it; refresh++ },
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
                version = BuildConfig.VERSION_NAME,
                shareStats = app.tend.data.Stats.consent(this) == true,
                onShareStats = { app.tend.data.Stats.setConsent(this, it); refresh++ },
                onReplayTour = { store.tourDone = false; screen = Screen.HOME },
                notesMorning = store.notesMorning,
                notesNudges = store.notesNudges,
                onNotes = ::setNotes,
                theme = app.tend.ui.ThemeChoice.mode ?: store.themeMode,
                onTheme = { store.themeMode = it; app.tend.ui.ThemeChoice.mode = it },
                lockSchedules = store.lockSchedules,
                onLockSchedule = { store.setLockSchedule(it); refresh++ },
                onDone = { screen = Screen.HOME },
            )

            screen == Screen.SLEEP -> app.tend.ui.SleepScreen(store.avatar) { screen = Screen.HOME }

            screen == Screen.RECAP -> {
                LaunchedEffect(Unit) { store.recapSeenOn = LocalDate.now().toString() }
                RecapScreen(
                    avatar = store.avatar,
                    recap = Recap.of(store.config, store.today(), store.worthItToday(), store.normalMinutes),
                    placed = store.placed,
                    worn = shelf.first,
                    reminderHour = store.eveningFrom.takeIf { !store.reminderPrompted },
                    onReminder = ::setReminder,
                    onGoodNight = { screen = Screen.SLEEP; maybeAskReview() },
                    onShare = ::shareCamp,
                )
            }

            screen == Screen.PERMISSIONS -> PermissionsScreen(store.avatar, permissionActions()) {
                MonitorService.start(this)
                screen = Screen.HOME
            }

            screen == Screen.CLEARING -> app.tend.ui.ClearingScreen(
                avatar = store.avatar,
                placed = store.placed,
                acorns = store.acorns,
                earnedToday = store.acornsToday(),
                lastNight = store.acornsLastNight(),
                worn = shelf.first, onShelf = shelf.second,
                night = app.tend.core.Budget.isEvening(store.eveningFrom, LocalTime.now().hour),
                onBuy = { spot, thing -> store.buy(spot, thing).also { if (it) { app.tend.data.Stats.log(this, "bought", mapOf("thing" to thing)); refresh++ } } },
                onSell = { spot -> store.sell(spot); refresh++ },
                onBack = { screen = Screen.HOME },
                onShare = ::shareCamp,
            ) {
                WeekSections(
                    store.avatar,
                    Patterns.week(LocalDate.now(), store.daySummaries(), store.answers()),
                    appLabel = { pkg -> runCatching { packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString() }.getOrDefault(pkg) },
                )
            }

            (TaskSync.focusSecondsLeft(store, now) ?: 0) > 0 -> FocusTimerScreen(
                store.avatar, store.focusTask, TaskSync.focusSecondsLeft(store, now)!!,
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
                onShareCrash = TendApp.crashFile(this).takeIf { it.exists() }?.let { file ->
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
                    if (!store.reminderPrompted && !RecapReminder.canNotify(this@MainActivity)) allowNotes()
                    refresh++
                },
                onCheckOff = { task ->
                    val done = TaskSync.checkOff(store, task)
                    if (done.tasks.isNotEmpty()) celebrate = done
                    refresh++
                },
                onUndo = { TaskSync.undo(store, it); refresh++ },
                onOpenSettings = { screen = Screen.SETTINGS },
                onShare = ::shareCamp,
                onOpenClearing = { screen = Screen.CLEARING },
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
                showTour = !store.tourDone,
                exerciseTracked = store.exerciseTracked,
                sleepEstimated = store.sleepEstimatedOn == LocalDate.now().toString(),
                onTourDone = { store.tourDone = true; refresh++ },
                brokenTimer = Tasks.presets.firstOrNull { it.id == store.timerBroken },
                onDismissBroken = { store.timerBroken = ""; refresh++ },
                level = progress.level,
                skills = progress.skills,
                challenge = progress.challenge,
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

    /** Name, companion and level on the server, when signed in (friends will see them). */
    private fun saveProfile() {
        if (app.tend.data.Account.user(this) == null) return
        val level = app.tend.core.Progress.level(store.totalXp()).level
        lifecycleScope.launch { app.tend.data.Account.saveProfile(this@MainActivity, store.avatar.name, level) }
    }

    private class HomeProgress(val level: app.tend.core.Level, val skills: Map<app.tend.core.Skill, app.tend.core.Level>, val challenge: app.tend.core.Challenge)

    /** XP from all activities, the week's challenge (banked once when finished), and the levels. */
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
     * Asks the launcher to place the 2x1 widget (its own dialog); false when it can't. The 2x1 is
     * the one launchers place reliably from an app; the 1x1 is in the phone's widget list.
     */
    private fun pinWidget(): Boolean {
        val manager = android.appwidget.AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return runCatching {
            manager.requestPinAppWidget(android.content.ComponentName(this, app.tend.ui.WidgetKind.WIDE.provider), null, null)
        }.getOrDefault(false)
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
