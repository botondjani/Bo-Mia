package app.tend

import android.app.AppOpsManager
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
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
import androidx.compose.foundation.layout.Column
import app.tend.core.Budget
import app.tend.core.Patterns
import app.tend.core.Recap
import app.tend.reminder.RecapReminder
import app.tend.reminder.WeeklyLetter
import app.tend.ui.WeekSections
import java.time.LocalDate
import app.tend.core.TaskDef
import app.tend.ui.GardenScreen
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
import app.tend.widget.TendWidget
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
    /** Android has refused a switch once (fallback when the lock state can't be read). */
    private var tripped = false

    private enum class Launched { NONE, TRIP, APP_INFO, OVERLAY, USAGE }

    private enum class Screen { HOME, SETTINGS, PERMISSIONS, GARDEN, RECAP }

    /** Set when the evening notification was tapped. */
    private var openRecap by mutableStateOf(false)
    private var openGarden by mutableStateOf(false)

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
            if (!store.onboarded && store.blockedPackages.isEmpty()) store.blockedPackages = c.suggested.map { it.pkg }.toSet()
        }
        RecapReminder.schedule(this)
        app.tend.reminder.WeeklyLetter.schedule(this)
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
            app.tend.reminder.WeeklyLetter.OPEN_GARDEN -> openGarden = true
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
        continueSetup()
        refresh++
        // Usage access is enough to count minutes; without the overlay the monitor nudges by notification.
        if (store.onboarded && permissions().usageAccess) MonitorService.start(this)
    }

    @Composable
    private fun Content(catalog: AppCatalog) {
        refresh // read so recomposition follows onResume
        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var health by remember { mutableStateOf<HealthConnect.Status?>(null) }
        var celebrate by remember { mutableStateOf<TaskSync.Done?>(null) }
        var synced by remember { mutableIntStateOf(0) }
        synced // read so recomposition follows a sync

        suspend fun sync() {
            health = HealthConnect(this@MainActivity, store.selectedTasks).status()
            val done = TaskSync.sync(this@MainActivity)
            if (done.tasks.isNotEmpty()) celebrate = done
            synced++
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
        LaunchedEffect(openGarden) { if (openGarden) { screen = Screen.GARDEN; openGarden = false } }
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
                onAvatar = { store.avatar = it; refresh++ },
                onConfig = { store.config = it; refresh++ },
                onToggleApp = toggleApp,
                tasks = store.selectedTasks,
                onToggleTask = toggleTask,
                missCount = store.missCount(),
                onAddWidget = ::pinWidget,
                reminderEnabled = store.reminderEnabled,
                onReminder = ::setReminder,
                eveningFrom = store.eveningFrom,
                onEvening = { store.eveningFrom = it; RecapReminder.schedule(this); refresh++ },
                eveningPause = store.eveningPause,
                onEveningPause = { store.eveningPause = it; refresh++ },
                weeklyLetter = store.weeklyLetter,
                onWeeklyLetter = ::setWeeklyLetter,
                onExport = ::exportTestData,
                onReplayTour = { store.tourDone = false; screen = Screen.HOME },
                onDone = { screen = Screen.HOME },
            )

            screen == Screen.RECAP -> {
                LaunchedEffect(Unit) { store.recapSeenOn = LocalDate.now().toString() }
                RecapScreen(
                    avatar = store.avatar,
                    recap = Recap.of(store.config, store.today(), store.worthItToday()),
                    plants = store.plants,
                    daysTended = store.daysTended,
                    reminderHour = store.eveningFrom.takeIf { !store.reminderPrompted },
                    onReminder = ::setReminder,
                    onGoodNight = { screen = Screen.HOME },
                )
            }

            screen == Screen.PERMISSIONS -> PermissionsScreen(store.avatar, permissionActions()) {
                MonitorService.start(this)
                screen = Screen.HOME
            }

            screen == Screen.GARDEN -> GardenScreen(
                store.avatar, store.plants, store.daysTended, Budget.gardenMinutesAtClose(store.config, store.today()),
                worn = shelf.first, onShelf = shelf.second,
                onBack = { screen = Screen.HOME },
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

            else -> HomeScreen(
                avatar = store.avatar,
                cfg = store.config,
                day = store.today(),
                tasks = store.selectedTasks,
                plantCount = store.plants.size,
                isEvening = LocalTime.now().hour >= store.eveningFrom,
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
                        healthPermissions.launch(HealthConnect.permissionsFor(store.selectedTasks))
                    }
                },
                onStartFocus = { task ->
                    TaskSync.startTimer(this@MainActivity, task)
                    refresh++
                },
                onCheckOff = { task ->
                    val done = TaskSync.checkOff(store, task)
                    if (done.tasks.isNotEmpty()) celebrate = done
                    refresh++
                },
                onUndo = { TaskSync.undo(store, it); refresh++ },
                onOpenSettings = { screen = Screen.SETTINGS },
                onOpenGarden = { screen = Screen.GARDEN },
                onOpenRecap = { screen = Screen.RECAP },
                // Not during onboarding: from the second day on, until placed or seen.
                showWidgetTip = !store.widgetTipDone && store.daysTended >= 1 && !widgetPlaced(),
                onWidgetTipDone = { store.widgetTipDone = true; refresh++ },
                showTour = !store.tourDone,
                onTourDone = { store.tourDone = true; refresh++ },
                brokenTimer = Tasks.presets.firstOrNull { it.id == store.timerBroken },
                onDismissBroken = { store.timerBroken = ""; refresh++ },
            )
        }
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
        hint("Switch on \"Allow display over other apps\"")
        open(highlightTend(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))), highlightTend(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)))
    }

    /**
     * Android only offers "Allow restricted settings" after it has refused a switch once, so the
     * unlock starts by letting it refuse: tend's own overlay switch, one tap, "OK".
     */
    private fun tripLock() {
        tripped = true
        launched = Launched.TRIP
        returnWhenGranted { Settings.canDrawOverlays(this) } // phones without the lock just say yes
        hint("Tap the switch. Android will say it's restricted: tap OK, then come back")
        open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun openUnlock() {
        launched = Launched.APP_INFO
        hint("Tap ⋮ in the top right, then \"Allow restricted settings\"")
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    /** Back in tend after a system screen: open the next one by itself, so setup is a chain of switches. */
    private fun continueSetup() {
        val from = launched
        launched = Launched.NONE
        val p = permissions()
        val lock = lock()
        val next: (() -> Unit)? = when {
            from == Launched.NONE || from == Launched.USAGE -> null
            p.overlay && !p.usageAccess -> ::requestUsage
            from == Launched.TRIP && lock == PermissionLock.TRIPPED -> ::openUnlock
            from == Launched.APP_INFO && lock == PermissionLock.NONE && !p.overlay -> ::requestOverlay
            else -> null
        }
        // A short beat so the tick on the card is seen before the next screen opens.
        if (next != null) handler.postDelayed({ next() }, 650)
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
    private fun returnWhenGranted(granted: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 120_000
        handler.removeCallbacksAndMessages(null)
        handler.post(object : Runnable {
            override fun run() = when {
                granted() -> startActivity(
                    Intent(this@MainActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
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
    private fun widgetPlaced() = AppWidgetManager.getInstance(this)
        .getAppWidgetIds(ComponentName(this, TendWidget::class.java)).isNotEmpty()

    /** Asks the launcher to place the widget; false when it can't or won't (then the user adds it by hand). */
    private fun pinWidget(): Boolean {
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return runCatching { manager.requestPinAppWidget(ComponentName(this, TendWidget::class.java), null, null) }.getOrDefault(false)
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


}
